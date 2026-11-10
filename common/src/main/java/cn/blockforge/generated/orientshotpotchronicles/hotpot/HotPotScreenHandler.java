package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * 筷子视角的火锅屏幕：九宫格 × 3x3 = 81 个食材槽，
 * 摆放为 9x9 的锅面，大格之间留分界。用法像熔炉：
 * 生的放进去，熟的直接取出来（取出时结算经验）。
 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class HotPotScreenHandler extends PlayerInventoryMenu {
    /** 大格间缝隙（像素）。 */
    public static final int GAP = 4;
    /** 锅面网格左上角（界面坐标）；每格 18 像素，物品画在格内向右下偏 1 像素处。 */
    public static final int GRID_X = 13;
    public static final int GRID_Y = 27;
    /** 横版界面：玩家背包在右侧状态卡片下方。 */
    public static final int INV_X = 198;
    public static final int INV_Y = 128;
    public static final int HOTBAR_Y = 186;

    private final Container pot;
    private final net.minecraft.world.level.Level world;
    private final HotPotBlockEntity serverBe;
    private final DataSlot[] properties;
    private final int[] clientData = new int[HotPotBlockEntity.PROP_COUNT];

    // 服务端构造
    public HotPotScreenHandler(int syncId, Inventory playerInventory, HotPotBlockEntity be) {
        this(syncId, playerInventory, (Container) be, be);
    }

    // 客户端构造（扩展数据里只带方块坐标）
    public HotPotScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, clientInventory(playerInventory, pos), null);
    }

    private HotPotScreenHandler(int syncId, Inventory playerInventory, Container pot,
                                HotPotBlockEntity be) {
        super(HotPotContent.HOT_POT_SCREEN, syncId);
        this.pot = pot;
        this.serverBe = be;
        this.world = playerInventory.player.level();
        // 客户端在服务端属性包到达前也应显示“无汤”，而不是误显示为第一种汤底。
        clientData[0] = -1;
        for (int i = HotPotBlockEntity.PROP_EXTRA_BASES; i < HotPotBlockEntity.PROP_COUNT; i++) {
            clientData[i] = -1;
        }
        pot.startOpen(playerInventory.player);

        // 9x9 锅面槽位：九宫格锅行/列每跨过 3 格让出 GAP 像素，分界线落在缝里；
        // 鸳鸯 / 田字锅 9x9 均匀排满锅面（四周留 PotLayout.GUI_INSET），压到隔板的格子不启用（用户 2026-10-24 定）
        boolean multi = pot instanceof HotPotBlockEntity potBe && potBe.layout().multiSoup();
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            int col = i % 9;
            int row = i / 9;
            int x = multi ? GRID_X + PotLayout.GUI_INSET + col * 18 + 1 : GRID_X + col * 18 + (col / 3) * GAP + 1;
            int y = multi ? GRID_Y + PotLayout.GUI_INSET + row * 18 + 1 : GRID_Y + row * 18 + (row / 3) * GAP + 1;
            addSlot(new PotSlot(pot, i, x, y));
        }
        // 玩家背包
        addPlayerInventory(playerInventory, INV_X, INV_Y, HOTBAR_Y);

        properties = bindData(clientData, be == null ? null : be::propertyAt);
    }

    private static Container clientInventory(Inventory playerInventory, BlockPos pos) {
        net.minecraft.world.level.Level world = playerInventory.player.level();
        if (world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            return be;
        }
        return new net.minecraft.world.SimpleContainer(HotPotBlockEntity.SLOTS);
    }

    // ------------------------------------------------------------------ UI 数据

    public int baseId() {
        return properties[0].get();
    }

    public boolean hasSoup() {
        for (int side = 0; side < PotLayout.MAX_SIDES; side++) {
            if (baseOfSide(side) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** 某一边的汤底编号（第 0 边 = 属性 0，其余在 PROP_EXTRA_BASES 起）。 */
    public int baseOfSide(int side) {
        if (side <= 0) {
            return baseId();
        }
        return side < PotLayout.MAX_SIDES ? properties[HotPotBlockEntity.PROP_EXTRA_BASES + side - 1].get() : -1;
    }

    /** 这口锅怎么分格（鸳鸯锅：哪些大格属于哪边，正中那格不用）。 */
    public PotLayout layout() {
        return potMaterial().layout();
    }

    /** 这一格在用的锅里有没有（鸳鸯锅正中那个大格没有）。 */
    public boolean slotInUse(int slot) {
        return layout().sideOfSlot(slot) != PotLayout.NONE;
    }

    /** 这一格泡在哪边的汤里。 */
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

    /** 该槽是否已经是煮好的熟食（等待取出）。 */
    public boolean isCooked(int slot) {
        if (slot < 0 || slot >= HotPotBlockEntity.SLOTS) {
            return false;
        }
        int word = 4 + HotPotBlockEntity.SLOTS + slot / HotPotBlockEntity.COOKED_WORD_BITS;
        int bit = slot % HotPotBlockEntity.COOKED_WORD_BITS;
        return (properties[word].get() & (1 << bit)) != 0;
    }

    public boolean isPotSlot(Slot slot) {
        // 不能用 inventory index 判断：玩家背包中的前 81 个索引也可能小于 81。
        return slot instanceof PotSlot;
    }

    /** 该槽属于第几个大格（0..8，行优先）。 */
    public int bigCellOf(int slot) {
        int col = slot % 9;
        int row = slot / 9;
        return (row / 3) * 3 + col / 3;
    }

    /** 预估该格还要多久（秒），-1 表示不可煮/已完成。 */
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
        SoupBase soup = SoupBase.byId(soupOfSlot(slot));
        float power;
        if (fuelId() == HotPotBlockEntity.FUEL_OPEN_FIRE) {
            power = HotPotBlockEntity.OPEN_FIRE_POWER;   // 篝火上的锅（代码检查 B6）
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

    /** 锅的材质：界面据此给锅沿、九宫格隔条上铜色或铁色；取不到时按铜锅。 */
    public HotPotMaterial potMaterial() {
        HotPotMaterial material = pot instanceof HotPotBlockEntity be ? be.potMaterial() : null;
        return material == null ? HotPotMaterial.COPPER : material;
    }

    // ------------------------------------------------------------------ 行为

    @Override
    public boolean stillValid(Player player) {
        return pot.stillValid(player);
    }

    /**
     * 双击“拾取全部”走原版逻辑：同类物品收集到光标上那一组（最多收满一组），
     * 锅里的熟食被收走时照常经 onTakeItem 结算经验。
     * 锅里只收已经熟了的：没有熔炉配方的食材煮前煮后是同一种物品，不能把还在煮的一起拿走。
     * 客户端松开第二下时会用空物品问“这一格能不能当双击起点”（HandledScreen.mouseReleased），
     * 那一格的熟食刚被第一下拿走、已不算熟，所以空物品的询问一律放行，否则在锅里双击无效。
     */
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot instanceof PotSlot) {
            // 鸳鸯 / 田字锅：双击只收和起点格子同一种汤底里的（用户定：同汤底才一键收，不同汤底的别收）
            if (!stack.isEmpty() && collecting && soupOfSlot(slot.getContainerSlot()) != collectSoup) {
                return false;
            }
            return stack.isEmpty() || isCooked(slot.getContainerSlot());
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    /** 双击收取进行中（起点在锅里的格子）：只收这种汤底里的熟食。 */
    private boolean collecting = false;
    private int collectSoup = -1;

    /** 冷锅吃东西反胃的概率和时长（时长同原版河豚的反胃 15 秒）。 */
    private static final float COLD_NAUSEA_CHANCE = 0.3F;
    private static final int COLD_NAUSEA_TICKS = 300;

    /** 光标拿着筷子、左键点锅里的熟食：直接夹起来一口吃掉（只认熟食，其它情况照原版处理）。 */
    @Override
    public void clicked(int slotId, int button, ClickType actionType, Player player) {
        if (actionType == ClickType.PICKUP && button == 0 && slotId >= 0 && slotId < slots.size()
            && slots.get(slotId) instanceof PotSlot potSlot && potSlot.hasItem()
            && isCooked(potSlot.getContainerSlot()) && getCarried().is(HotPotContent.CHOPSTICKS)) {
            if (serverBe != null) {
                eatWithChopsticks(player, potSlot);
            }
            return;
        }
        if (actionType == ClickType.PICKUP_ALL && slotId >= 0 && slotId < slots.size()
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

    /**
     * 一口吃掉：交给原版 Player.eatFood，饥饿值、饱和度、药水效果、统计、进度、
     * 咀嚼声和打嗝声都和正常吃这个食物完全一样；照常结算出锅经验，碗之类的容器还给玩家。
     */
    private void eatWithChopsticks(Player player, Slot slot) {
        ItemStack stack = slot.getItem();
        FoodProperties food = ConfiguredFoodItem.food(stack);
        if (food == null) {
            return;
        }
        // 饱食度满了也让吃（同 ChopsticksItem：兼容 Always Eat 这类模组）；饥饿条满时只加饱和度，最多到 20
        if (!player.canEat(true)) {
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.too_full")), true);
            return;
        }
        // mryt233 改（2026-11-10）：与筷子物品一致，接管时保留锅槽与经验。
        if (HotPotHooks.beforeEating(getCarried(), player)) {
            return;
        }
        int index = slot.getContainerSlot();
        ItemStack eaten = slot.remove(1);
        serverBe.grantTakeXp(player, index, 1);
        if (!slot.hasItem()) {
            serverBe.onSlotEmptied(index);
        }
        // 主手或副手拿着蘸料：吃之前自动蘸一口（r103，用户：两种都要；2026-10-25 主手也算，调整）
        DippingSauceItem.dipFromHands(player, eaten);
        HotPotHooks.onEating(world, player, eaten);
        player.eat(world, eaten.copy(), ConfiguredFoodItem.food(eaten));
        // 冷锅：没坐在烧着的灶台（或其他热源）上，吃了有概率反胃
        if (!serverBe.isHeated() && world.random.nextFloat() < COLD_NAUSEA_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, COLD_NAUSEA_TICKS, 0));
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.cold_pot")), true);
        }
        if (!player.hasInfiniteMaterials()) {
            food.usingConvertsTo().ifPresent(rest -> player.getInventory().placeItemBackInInventory(rest.copy()));
        }
        // 嘴边迸出一小撮食物碎屑，像一口咬下去
        if (world instanceof ServerLevel serverWorld) {
            serverWorld.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, eaten),
                player.getX(), player.getEyeY() - 0.15, player.getZ(), 8, 0.12, 0.06, 0.12, 0.05);
        }
        broadcastChanges();
    }

    /** shift 键：背包 → 锅里空格；锅里 → 背包，并按实际移动数量结算经验。 */
    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        return super.quickMoveStack(player, slotId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pot.stopOpen(player);
    }

    /** 锅内槽位：像熔炉一样，生进熟出，随时可拿。 */
    private final class PotSlot extends Slot {
        PotSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 每个物理槽是一个独立的“单份格”，绝不能把新堆叠并入已有格。
            // 这条 empty-only 约束同时覆盖普通点击、拖拽和 quickMove，
            // 从根上阻断 AbstractContainerMenu.insertItem 的自动合并路径。
            return !hasItem() && !isCooked(getContainerSlot()) && slotInUse(getContainerSlot())
                && HotPotCooking.acceptable(world, stack);
        }

        /** 鸳鸯锅正中那个大格不用：格子藏起来、点不到。 */
        @Override
        public boolean isActive() {
            return slotInUse(getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            // 一格只允许一份，不能因为旧存档或客户端异常包写入整组物品。
            return 1;
        }

        /**
         * 原版点击流程最终会走 Slot.insertStack；这里禁止它复用/合并已有物品，
         * 并把一次放入严格限制为一份。返回值是未放入的剩余物品，交还给光标，
         * 因而不会出现“整组物品被写进一个槽”或被挤到 Slot 0 的副作用。
         */
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
                // 先结算经验，再清理空槽的熟食标记。
                serverBe.grantTakeXp(player, getContainerSlot(), stack.getCount());
                if (getItem().isEmpty()) {
                    serverBe.onSlotEmptied(getContainerSlot());
                }
            }
        }

    }

    @Override
    protected int containerSlots() { return HotPotBlockEntity.SLOTS; }

    @Override
    protected boolean fromContainer(Slot slot, int slotId) { return slot instanceof PotSlot; }

    @Override
    protected boolean requireMovedCount() { return true; }

    @Override
    protected boolean quickMoveOutput(Slot slot, boolean fromContainer) {
        return fromContainer && isCooked(slot.getContainerSlot());
    }

    @Override
    protected void onQuickMoved(Player player, Slot slot, boolean fromContainer, int moved, boolean cookedOutput) {
        if (cookedOutput && serverBe != null) serverBe.grantTakeXp(player, slot.getContainerSlot(), moved);
    }

    @Override
    protected void onQuickEmptied(Slot slot, boolean fromContainer) {
        if (fromContainer && serverBe != null) serverBe.onSlotEmptied(slot.getContainerSlot());
    }

    @Override
    protected void afterQuickMove() { broadcastChanges(); }
}
