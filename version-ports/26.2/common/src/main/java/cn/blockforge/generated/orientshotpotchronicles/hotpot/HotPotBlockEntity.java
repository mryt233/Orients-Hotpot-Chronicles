// mryt233 改：26.2 原版 API；第三批 3F/3G 拆分逻辑委派与存读档适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 火锅库存与烹饪数据。
 * 独立摆放的锅使用本类；坐上灶台后的组合方块由 {@link StoveBlockEntity} 继承本类。
 */
public class HotPotBlockEntity extends SyncedBlockEntity implements Container {
    public static final int SLOTS = 81;
    public static final int COOKED_WORD_BITS = 15;
    public static final int COOKED_WORDS = (SLOTS + COOKED_WORD_BITS - 1) / COOKED_WORD_BITS;
    public static final int PROP_EXTRA_BASES = 4 + SLOTS + COOKED_WORDS;
    public static final int PROP_COUNT = PROP_EXTRA_BASES + PotLayout.MAX_SIDES - 1;

    final HotPotSlots slots = new HotPotSlots(this);
    final String[] bases = {"", "", "", ""};
    final int[] spicyLevels = {0, 0, 0, 0};

    private float clientHeat = 0F;
    private double bubbleSeconds;
    public static final float BOB_OMEGA = 0.22F;
    public static final float BOB_COLD_OMEGA = 0.05F;
    private final double[] cycleAngle = new double[PotLayout.MAX_SIDES];
    boolean needsVisualRepair = true;
    private boolean breakHandled = false;
    boolean syncPending = false;

    public HotPotBlockEntity(BlockPos blockPos, BlockState state) {
        this(HotPotContent.HOT_POT_BE, blockPos, state);
    }

    protected HotPotBlockEntity(BlockEntityType<?> type, BlockPos blockPos, BlockState state) {
        super(type, blockPos, state);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState state, HotPotBlockEntity be) {
        BlockState current = level.getBlockState(blockPos);
        if (!StoveBlock.isStove(current) && !(current.getBlock() instanceof HotPotBlock)) {
            return;
        }
        if (level.isClientSide()) {
            be.clientTick(level, blockPos, state);
            return;
        }
        be.serverTick(level);
    }

    protected void serverTick(Level level) {
        HotPotCookingRun.tick(this, level);
    }

    protected void clientTick(Level level, BlockPos blockPos, BlockState state) {
        clientHeat = HotPotEffects.tick(this, level, blockPos, state, clientHeat);
        bubbleSeconds += (1.0 / 20.0) * (0.7 + 0.5 * clientHeat);
        for (int side = 0; side < cycleAngle.length; side++) {
            cycleAngle[side] += cycleRate(side);
        }
    }

    private float cycleRate(int side) {
        return baseOfSide(side) != SoupIds.EMPTY && clientHeat < 0.5F ? BOB_COLD_OMEGA : BOB_OMEGA;
    }

    public double cycleAngle(int side, float partial) {
        return side < 0 || side >= cycleAngle.length ? 0 : cycleAngle[side] + partial * cycleRate(side);
    }

    public double bubbleSeconds(float partial) {
        return bubbleSeconds + partial * (0.7 + 0.5 * clientHeat) / 20.0;
    }

    public StoveBlockEntity stoveBelow() {
        if (this instanceof StoveBlockEntity stove) {
            return stove;
        }
        if (level == null) {
            return null;
        }
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        return below instanceof StoveBlockEntity stove ? stove : null;
    }

    public boolean hasPot() {
        BlockState state = getBlockState();
        if (StoveBlock.isStove(state)) {
            return state.getValue(StoveBlock.POT).hasPot();
        }
        return state.getBlock() instanceof HotPotBlock;
    }

    void markNeedsVisualRepair() {
        needsVisualRepair = true;
    }

    public void repairVisualState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.getValue(StoveBlock.POT);
            if (!pot.hasPot()) {
                for (String id : bases) {
                    if (SoupIds.index(id) == SoupIds.UNKNOWN) {
                        return;
                    }
                }
                if (filledSlots() > 0) {
                    clearContent();
                }
                if (hasSoup()) {
                    clearSoupState();
                }
                return;
            }
            SoupVisual visual = blockSoupVisual();
            if (pot.hasSoup() != hasSoup() || state.getValue(StoveBlock.SOUP) != visual) {
                level.setBlock(worldPosition, state.setValue(StoveBlock.POT, pot.withSoup(hasSoup()))
                    .setValue(StoveBlock.SOUP, visual), Block.UPDATE_CLIENTS);
            }
        } else if (state.getBlock() instanceof HotPotBlock) {
            SoupVisual visual = blockSoupVisual();
            if (state.getValue(HotPotBlock.SOUP) != visual
                || state.getValue(HotPotBlock.HAS_SOUP) != hasSoup()) {
                level.setBlock(worldPosition, state.setValue(HotPotBlock.HAS_SOUP, hasSoup())
                    .setValue(HotPotBlock.SOUP, visual), Block.UPDATE_CLIENTS);
            }
        }
    }

    public HotPotMaterial potMaterial() {
        BlockState state = getBlockState();
        if (StoveBlock.isStove(state)) {
            return state.getValue(StoveBlock.POT).material();
        }
        return state.getBlock() instanceof HotPotBlock pot ? pot.material() : null;
    }

    public static final float OPEN_FIRE_POWER = 1.0F;
    public static final int FUEL_OPEN_FIRE = -2;

    public boolean heatSourceBelow() {
        return level != null && !(this instanceof StoveBlockEntity) && HotPotHeat.isHeatSource(level, worldPosition.below());
    }

    public boolean hasFire() {
        if (level == null) {
            return false;
        }
        if (this instanceof StoveBlockEntity) {
            BlockState state = level.getBlockState(worldPosition);
            return StoveBlock.isStove(state) && state.getValue(StoveBlock.LIT);
        }
        StoveBlockEntity stove = stoveBelow();
        return (stove != null && stove.burning()) || heatSourceBelow();
    }

    protected void clearSoupState() {
        HotPotSoupOps.clearSoupState(this);
    }

    protected void updateSoupVisual() {
        HotPotSoupOps.updateSoupVisual(this);
    }

    void updateStandaloneBoilingVisual(boolean boiling) {
        HotPotEffects.updateStandaloneBoiling(this, boiling);
    }

    public void useWithChopsticks(Player player) {
        HotPotSoupOps.useWithChopsticks(this, player);
    }

    public boolean canScoopWith(ItemStack stack) {
        return HotPotSoupOps.canScoopWith(this, stack);
    }

    public boolean scoopSoup(Player player, InteractionHand hand) {
        return HotPotSoupOps.scoopSoup(this, player, hand);
    }

    public boolean scoopSoup(Player player, InteractionHand hand, int side) {
        return HotPotSoupOps.scoopSoup(this, player, hand, side);
    }

    public boolean serveWithBowl(Player player, InteractionHand hand, int bigCell) {
        return HotPotSoupOps.serveWithBowl(this, player, hand, bigCell);
    }

    public boolean pourSoup(Player player, InteractionHand hand) {
        return HotPotSoupOps.pourSoup(this, player, hand);
    }

    public boolean pourSoup(Player player, InteractionHand hand, int side) {
        return HotPotSoupOps.pourSoup(this, player, hand, side);
    }

    public static final int SPICY_MAX = HotPotSpice.SPICY_MAX;

    public int spicyOfSide(int side) {
        return HotPotSpice.spicyOfSide(this, side);
    }

    public void setSpicyOfSide(int side, int level) {
        HotPotSpice.setSpicyOfSide(this, side, level);
    }

    public static Component spicyLevelText(int level) {
        return HotPotSpice.spicyLevelText(level);
    }

    public static boolean isSpicyModifier(ItemStack stack) {
        return HotPotSpice.isSpicyModifier(stack);
    }

    private static int iceDrop(ItemStack stack) {
        return HotPotSpice.iceDrop(stack);
    }

    public boolean adjustSpiciness(Player player, InteractionHand hand, int side) {
        return HotPotSpice.adjustSpiciness(this, player, hand, side);
    }

    void spawnSpicyParticles(boolean up) {
        HotPotSpice.spawnSpicyParticles(this, up);
    }

    int soupSideOf(int bigCell, int soupId) {
        return HotPotSoupOps.soupSideOf(this, bigCell, soupId);
    }

    public int spicyOfSlot(int slot) {
        return HotPotSpice.spicyOfSlot(this, slot);
    }

    public boolean insertHeld(Player player, ItemStack chopsticks, int slot) {
        return HotPotFoodOps.insertHeld(this, player, chopsticks, slot);
    }

    public boolean pourPlate(Player player, InteractionHand hand) {
        return HotPotFoodOps.pourPlate(this, player, hand);
    }

    public boolean insertIngredient(Player player, InteractionHand hand) {
        return HotPotFoodOps.insertIngredient(this, player, hand);
    }

    public boolean insertIngredient(Player player, InteractionHand hand, int slot) {
        return HotPotFoodOps.insertIngredient(this, player, hand, slot);
    }

    public boolean collectCookedSlot(Player player, int slot) {
        return HotPotFoodOps.collectCookedSlot(this, player, slot);
    }

    public boolean pickWithChopsticks(Player player, ItemStack chopsticks, int slot) {
        return HotPotFoodOps.pickWithChopsticks(this, player, chopsticks, slot);
    }

    public boolean pickShown(Player player, ItemStack chopsticks, int bigCell) {
        return HotPotFoodOps.pickShown(this, player, chopsticks, bigCell);
    }

    int shownIn(Player player, int bigCell) {
        return HotPotFoodOps.shownIn(this, player, bigCell);
    }

    public boolean collectShown(Player player, int bigCell) {
        return HotPotFoodOps.collectShown(this, player, bigCell);
    }

    int transferCookedSlot(Player player, int slot) {
        return HotPotFoodOps.transferCookedSlot(this, player, slot);
    }

    void finishCollection() {
        HotPotFoodOps.finishCollection(this);
    }

    public void grantTakeXp(Player player, int slot, int count) {
        HotPotFoodOps.grantTakeXp(this, player, slot, count);
    }

    void trackSlotChanges() {
        slots.trackSlotChanges();
    }

    void recomputeCookTimes() {
        slots.recomputeCookTimes();
    }

    @Override
    public void setChanged() {
        slots.touch();
        super.setChanged();
    }

    void resetSlot(int slot, ItemStack stack) {
        slots.resetSlot(slot, stack);
    }

    public void onSlotStackChanged(int slot) {
        slots.onSlotStackChanged(slot);
    }

    public void onSlotEmptied(int slot) {
        slots.onSlotEmptied(slot);
    }

    public int base() {
        return HotPotSoup.base(this);
    }

    public SoupVisual soupVisual() {
        return HotPotSoupOps.soupVisual(this);
    }

    public float clientHeat() {
        return clientHeat;
    }

    public double bubbleSeconds() {
        return bubbleSeconds;
    }

    public boolean hasSoup() {
        return HotPotSoup.has(this);
    }

    public PotLayout layout() {
        HotPotMaterial material = potMaterial();
        return material == null ? PotLayout.NINE_GRID : material.layout();
    }

    public int baseOfSide(int side) {
        return HotPotSoupOps.baseOfSide(this, side);
    }

    void setBaseOfSide(int side, int id) {
        HotPotSoupOps.setBaseOfSide(this, side, id);
    }

    public SoupBase soupForSlot(int slot) {
        SoupDef soup = soupDefForSlot(slot);
        return soup == null ? null : SoupBase.byId(Soups.indexOf(soup));
    }

    public SoupDef soupDefForSlot(int slot) {
        return HotPotSoupOps.soupForSlot(this, slot);
    }

    int bowlSoup(int bigCell) {
        return HotPotSoupOps.bowlSoup(this, bigCell);
    }

    SoupVisual blockSoupVisual() {
        return HotPotSoupOps.blockSoupVisual(this);
    }

    public int filledSlots() {
        return slots.filledSlots();
    }

    public int firstEmptySlot() {
        return slots.firstEmptySlot();
    }

    public boolean hasRawIngredient() {
        return slots.hasRawIngredient();
    }

    int randomEmptySlot() {
        return slots.randomEmptySlot();
    }

    public int firstEmptySlotInCell(int bigCell) {
        return slots.firstEmptySlotInCell(bigCell);
    }

    public int firstCookedSlotInCell(int bigCell) {
        return slots.firstCookedSlotInCell(bigCell);
    }

    int firstSlotInCell(int bigCell, IntPredicate matches) {
        return slots.firstSlotInCell(bigCell, matches);
    }

    public int propertyAt(int index) {
        if (index == 0) {
            return SoupIds.index(bases[0]);
        }
        if (index >= PROP_EXTRA_BASES && index < PROP_COUNT) {
            return SoupIds.index(bases[index - PROP_EXTRA_BASES + 1]);
        }
        if (index >= 4 && index < 4 + SLOTS) {
            return slots.doneness()[index - 4];
        }
        int cookedStart = 4 + SLOTS;
        if (index >= cookedStart && index < PROP_COUNT) {
            int word = index - cookedStart;
            int bits = 0;
            for (int b = 0; b < COOKED_WORD_BITS; b++) {
                int slot = word * COOKED_WORD_BITS + b;
                if (slot < SLOTS && slots.cooked()[slot]) {
                    bits |= 1 << b;
                }
            }
            return bits;
        }
        StoveBlockEntity stove = stoveBelow();
        if (stove == null && heatSourceBelow()) {
            return index == 1 ? FUEL_OPEN_FIRE : 1;
        }
        return switch (index) {
            case 1 -> stove == null ? -1 : stove.fuelType();
            case 2 -> stove == null ? 0 : stove.displayBurnTicks();
            default -> stove == null ? 0 : stove.displayBurnTotal();
        };
    }

    public int comparatorLevel() {
        int total = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!slots.ingredients().getItem(i).isEmpty()) {
                total++;
            }
        }
        return Math.min(15, total * 15 / SLOTS + (total > 0 ? 1 : 0));
    }

    public static Component soupName(int id) {
        SoupDef soup = Soups.byIndex(id);
        return soup == null ? Component.translatable(key(id == SoupIds.UNKNOWN ? "soup.unknown" : "soup.none"))
            : Component.translatable(soup.nameKey());
    }

    public static Component soupNameWithSpicy(int id, int spicy) {
        Component name = soupName(id);
        SoupDef base = Soups.byIndex(id);
        if (base == null || !base.spicy() || spicy < 0) {
            return name;
        }
        return name.copy().append(Component.literal("（")).append(spicyLevelText(spicy))
            .append(Component.literal("）"));
    }

    public static Component soupText(PotLayout layout, IntUnaryOperator baseOfSide) {
        return soupText(layout, baseOfSide, side -> -1);
    }

    public static Component soupText(PotLayout layout, IntUnaryOperator baseOfSide,
                                     IntUnaryOperator spicyOfSide) {
        MutableComponent text = Component.empty();
        for (int side = 0; side < layout.sides(); side++) {
            if (side > 0) {
                text.append(Component.literal(" / "));
            }
            text.append(soupNameWithSpicy(baseOfSide.applyAsInt(side), spicyOfSide.applyAsInt(side)));
        }
        return text;
    }

    public Component statusText() {
        Component soupText = soupText(layout(), this::baseOfSide, this::spicyOfSide);
        StoveBlockEntity stove = stoveBelow();
        Component fireText;
        if (stove == null && heatSourceBelow()) {
            fireText = Component.translatable(key("heat.open_fire"));
        } else if (stove == null) {
            fireText = Component.translatable(key("stove.none"));
        } else if (stove.idle()) {
            fireText = Component.translatable(key("fuel.idle"));
        } else if (stove.burning()) {
            HotPotFuel fuel = HotPotFuel.byId(stove.fuelType());
            fireText = Component.translatable(fuel == null ? key("fuel.none") : fuel.nameKey())
                .append(Component.literal(" " + (stove.burnTicks() / 20) + "s"));
        } else {
            fireText = Component.translatable(key("fuel.none"));
        }
        return Component.translatable(key("msg.status"), soupText, filledSlots(), fireText);
    }

    public void markBreakHandled() {
        breakHandled = true;
    }

    public boolean consumeBreakHandled() {
        boolean handled = breakHandled;
        breakHandled = false;
        return handled;
    }

    public void dropContents() {
        dropContents(PotLayout.NONE);
    }

    public void dropContents(int side) {
        if (level == null || level.isClientSide()) {
            return;
        }
        PotLayout layout = layout();
        boolean all = side == PotLayout.NONE || !layout.multiSoup();
        for (int i = 0; i < SLOTS; i++) {
            if (!all && layout.sideOfSlot(i) != side) {
                continue;
            }
            ItemStack stack = slots.ingredients().getItem(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack);
                slots.ingredients().setItem(i, ItemStack.EMPTY);
                resetSlot(i, ItemStack.EMPTY);
            }
        }
        markDirtyAndSync();
    }

    private static final String[] POT_DATA_KEYS = {"Soup", "SoupB", "SoupC", "SoupD", "Base", "BaseB", "BaseC", "BaseD",
        "Spicy", "SpicyB", "SpicyC", "SpicyD",
        "Doneness", "Cooked", "Overcook", "SlotXp", "PendingXp", "Items"};

    public boolean hasPotContents() {
        return HotPotPack.hasPotContents(this);
    }

    public ItemStack toPotItem(HotPotMaterial material) {
        return HotPotPack.toPotItem(this, material);
    }

    public void loadPotContents(ItemStack stack) {
        HotPotPack.loadPotContents(this, stack);
    }

    public void clearPotContents() {
        HotPotPack.clearPotContents(this);
    }

    public boolean isHeated() {
        return HotPotPack.isHeated(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        HotPotPack.collectImplicitComponents(this, builder);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        HotPotPack.applyImplicitComponents(this, components.get(HotPotContent.POT_CONTENTS));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput nbt) {
        super.removeComponentsFromTag(nbt);
        HotPotPack.removeComponentsFromTag(nbt, POT_DATA_KEYS);
    }

    static void consumeHand(Player player, InteractionHand hand, Item remainder) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getAbilities().instabuild) {
            return;
        }
        if (stack.getCount() > 1) {
            stack.shrink(1);
            if (remainder != Items.AIR) {
                ItemStack back = new ItemStack(remainder);
                if (!player.getInventory().add(back)) {
                    player.drop(back, false);
                }
            }
        } else {
            player.setItemInHand(hand,
                remainder == Items.AIR ? ItemStack.EMPTY : new ItemStack(remainder));
        }
    }

    public static String key(String name) {
        return "text." + HotPot.MOD_ID + "." + name;
    }

    @Override
    public int getContainerSize() {
        return slots.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return slots.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slots.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slots.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slots.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        slots.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return slots.stillValid(player);
    }

    @Override
    public int getMaxStackSize() {
        return slots.getMaxStackSize();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slots.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slots.canTakeItem(target, slot, stack);
    }

    @Override
    public void clearContent() {
        slots.clearContent();
    }

    protected void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        syncPending = false;
    }

    @Override
    protected void saveAdditional(ValueOutput nbt) {
        super.saveAdditional(nbt);
        HotPotStorage.write(this, nbt);
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
        HotPotStorage.read(this, nbt);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    /** 掉落表负责整锅打包，禁止原版 Container 默认提前散落食材。 */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }
}
