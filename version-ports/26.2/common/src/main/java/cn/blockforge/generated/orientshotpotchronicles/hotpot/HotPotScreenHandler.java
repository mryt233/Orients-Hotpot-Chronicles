// mryt233 改：26.2 原版 API；第三批 3F 合并重复实现，保留各入口原有行为。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;

/**
 * 筷子视角的火锅屏幕：九宫格 × 3x3 = 81 个食材槽，
 * 摆放为 9x9 的锅面，大格之间留分界。用法像熔炉：
 * 生的放进去，熟的直接取出来（取出时结算经验）。
 */
public class HotPotScreenHandler extends PlayerInventoryMenu {
    public static final int GAP = 4;
    public static final int GRID_X = 13;
    public static final int GRID_Y = 27;
    public static final int INV_X = 198;
    public static final int INV_Y = 128;
    public static final int HOTBAR_Y = 186;

    private final Container pot;
    private final Level world;
    private final HotPotBlockEntity serverBe;
    private final DataSlot[] properties;
    private final int[] clientData = new int[HotPotBlockEntity.PROP_COUNT];

    public HotPotScreenHandler(int syncId, Inventory playerInventory, HotPotBlockEntity be) {
        this(syncId, playerInventory, (Container) be, be);
    }

    public HotPotScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, clientInventory(playerInventory, pos), null);
    }

    private HotPotScreenHandler(int syncId, Inventory playerInventory, Container pot,
                                HotPotBlockEntity be) {
        super(HotPotContent.HOT_POT_SCREEN, syncId);
        this.pot = pot;
        this.serverBe = be;
        this.world = playerInventory.player.level();
        clientData[0] = -1;
        for (int i = HotPotBlockEntity.PROP_EXTRA_BASES; i < HotPotBlockEntity.PROP_COUNT; i++) {
            clientData[i] = -1;
        }
        pot.startOpen(playerInventory.player);

        boolean multi = pot instanceof HotPotBlockEntity potBe && potBe.layout().multiSoup();
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            int col = i % 9;
            int row = i / 9;
            int x = multi ? GRID_X + PotLayout.GUI_INSET + col * 18 + 1 : GRID_X + col * 18 + (col / 3) * GAP + 1;
            int y = multi ? GRID_Y + PotLayout.GUI_INSET + row * 18 + 1 : GRID_Y + row * 18 + (row / 3) * GAP + 1;
            addSlot(new PotSlot(pot, i, x, y));
        }
        addPlayerInventory(playerInventory, INV_X, INV_Y, HOTBAR_Y);

        properties = bindData(clientData, be == null ? null : be::propertyAt);
    }

    private static Container clientInventory(Inventory playerInventory, BlockPos pos) {
        Level world = playerInventory.player.level();
        if (world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            return be;
        }
        return new SimpleContainer(HotPotBlockEntity.SLOTS);
    }

    public int baseId() {
        return properties[0].get();
    }

    public boolean hasSoup() {
        for (int side = 0; side < PotLayout.MAX_SIDES; side++) {
            if (baseOfSide(side) != SoupIds.EMPTY) {
                return true;
            }
        }
        return false;
    }

    public int baseOfSide(int side) {
        if (side <= 0) {
            return baseId();
        }
        return side < PotLayout.MAX_SIDES ? properties[HotPotBlockEntity.PROP_EXTRA_BASES + side - 1].get() : -1;
    }

    public PotLayout layout() {
        return potMaterial().layout();
    }

    public boolean slotInUse(int slot) {
        return layout().sideOfSlot(slot) != PotLayout.NONE;
    }

    public int soupOfSlot(int slot) {
        int side = layout().sideOfSlot(slot);
        return side == PotLayout.NONE ? -1 : baseOfSide(side);
    }

    public int fuelId() {
        return properties[1].get();
    }

    public int burnTicks() {
        return properties[2].get();
    }

    public int burnTotal() {
        return properties[3].get();
    }

    public int doneness(int slot) {
        if (slot < 0 || slot >= HotPotBlockEntity.SLOTS) {
            return 0;
        }
        return Mth.clamp(properties[4 + slot].get(), 0, HotPotCooking.FULL_DONE);
    }

    public boolean isCooked(int slot) {
        if (slot < 0 || slot >= HotPotBlockEntity.SLOTS) {
            return false;
        }
        int word = 4 + HotPotBlockEntity.SLOTS + slot / HotPotBlockEntity.COOKED_WORD_BITS;
        int bit = slot % HotPotBlockEntity.COOKED_WORD_BITS;
        return (properties[word].get() & (1 << bit)) != 0;
    }

    public boolean isPotSlot(Slot slot) {
        return slot instanceof PotSlot;
    }

    public int bigCellOf(int slot) {
        int col = slot % 9;
        int row = slot / 9;
        return (row / 3) * 3 + col / 3;
    }

    public float secondsLeft(int slot) {
        if (isCooked(slot)) {
            return -1F;
        }
        int d = doneness(slot);
        if (d >= HotPotCooking.FULL_DONE) {
            return -1F;
        }
        ItemStack stack = pot.getItem(slot);
        if (stack.isEmpty()) {
            return -1F;
        }
        SoupDef soup = Soups.byIndex(soupOfSlot(slot));
        float power;
        if (fuelId() == HotPotBlockEntity.FUEL_OPEN_FIRE) {
            power = HotPotBlockEntity.OPEN_FIRE_POWER;
        } else {
            HotPotFuel fuel = HotPotFuel.byId(fuelId());
            if (fuel == null || burnTicks() <= 0) {
                return Float.POSITIVE_INFINITY;
            }
            power = fuel.power();
        }
        float mul = soup == null ? 1F : soup.speedMul();
        int perTick = HotPotCooking.progressPerTick(HotPotCooking.cookTicks(world, stack), power, mul);
        return (HotPotCooking.FULL_DONE - d) / (float) perTick / 20F;
    }

    public HotPotMaterial potMaterial() {
        HotPotMaterial material = pot instanceof HotPotBlockEntity be ? be.potMaterial() : null;
        return material == null ? HotPotMaterial.COPPER : material;
    }

    @Override
    public boolean stillValid(Player player) {
        return pot.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot instanceof PotSlot) {
            if (!stack.isEmpty() && collecting && soupOfSlot(slot.getContainerSlot()) != collectSoup) {
                return false;
            }
            return stack.isEmpty() || isCooked(slot.getContainerSlot());
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    private boolean collecting = false;
    private int collectSoup = -1;

    @Override
    public void clicked(int slotId, int button, ContainerInput actionType, Player player) {
        if (actionType == ContainerInput.PICKUP && button == 0 && slotId >= 0 && slotId < slots.size()
            && slots.get(slotId) instanceof PotSlot potSlot && potSlot.hasItem()
            && isCooked(potSlot.getContainerSlot()) && getCarried().is(HotPotContent.CHOPSTICKS)) {
            if (serverBe != null) {
                eatWithChopsticks(player, potSlot);
            }
            return;
        }
        if (actionType == ContainerInput.PICKUP_ALL && slotId >= 0 && slotId < slots.size()
            && slots.get(slotId) instanceof PotSlot start && layout().multiSoup()) {
            collecting = true;
            collectSoup = soupOfSlot(start.getContainerSlot());
            try {
                super.clicked(slotId, button, actionType, player);
            } finally {
                collecting = false;
            }
            return;
        }
        super.clicked(slotId, button, actionType, player);
    }

    private void eatWithChopsticks(Player player, Slot slot) {
        ItemStack stack = slot.getItem();
        FoodProperties food = ConfiguredFoodItem.food(stack);
        if (food == null) {
            return;
        }
        if (!player.canEat(true)) {
            player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.too_full")));
            return;
        }
        if (HotPotHooks.beforeEating(getCarried(), player)) {
            return;
        }
        int index = slot.getContainerSlot();
        ItemStack eaten = slot.remove(1);
        serverBe.grantTakeXp(player, index, 1);
        if (!slot.hasItem()) {
            serverBe.onSlotEmptied(index);
        }
        DippingSauceItem.dipFromHands(player, eaten);
        HotPotHooks.onEating(world, player, eaten);
        FoodConsumption.consume(world, player, eaten, ConfiguredFoodItem.food(eaten));
        int nauseaDuration = Config.coldPotNauseaDuration();
        if (!serverBe.isHeated() && nauseaDuration > 0 && world.getRandom().nextFloat() < Config.coldPotNauseaChance()) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nauseaDuration, 0));
            player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.cold_pot")));
        }
        if (!player.hasInfiniteMaterials()) {
            FoodConsumption.returnRemainder(eaten, player);
        }
        if (world instanceof ServerLevel serverWorld) {
            serverWorld.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(eaten)),
                player.getX(), player.getEyeY() - 0.15, player.getZ(), 8, 0.12, 0.06, 0.12, 0.05);
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        return super.quickMoveStack(player, slotId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pot.stopOpen(player);
    }

    private final class PotSlot extends Slot {
        PotSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !hasItem() && !isCooked(getContainerSlot()) && slotInUse(getContainerSlot())
                && HotPotCooking.acceptable(world, stack);
        }

        @Override
        public boolean isActive() {
            return slotInUse(getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int count) {
            if (stack.isEmpty() || hasItem() || !mayPlace(stack)) {
                return stack;
            }
            int moved = Math.min(Math.min(count, stack.getCount()), getMaxStackSize(stack));
            if (moved <= 0) {
                return stack;
            }
            set(stack.copyWithCount(moved));
            ItemStack remainder = stack.copy();
            remainder.shrink(moved);
            return remainder;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (serverBe != null) {
                serverBe.grantTakeXp(player, getContainerSlot(), stack.getCount());
                if (getItem().isEmpty()) {
                    serverBe.onSlotEmptied(getContainerSlot());
                }
            }
        }
    }

    @Override
    protected int containerSlots() {
        return HotPotBlockEntity.SLOTS;
    }

    @Override
    protected boolean fromContainer(Slot slot, int slotId) {
        return slot instanceof PotSlot;
    }

    @Override
    protected boolean requireMovedCount() {
        return true;
    }

    @Override
    protected boolean quickMoveOutput(Slot slot, boolean fromContainer) {
        return fromContainer && isCooked(slot.getContainerSlot());
    }

    @Override
    protected void onQuickMoved(Player player, Slot slot, boolean fromContainer, int moved, boolean cookedOutput) {
        if (cookedOutput && serverBe != null) {
            serverBe.grantTakeXp(player, slot.getContainerSlot(), moved);
        }
    }

    @Override
    protected void onQuickEmptied(Slot slot, boolean fromContainer) {
        if (fromContainer && serverBe != null) {
            serverBe.onSlotEmptied(slot.getContainerSlot());
        }
    }

    @Override
    protected void afterQuickMove() {
        broadcastChanges();
    }
}
