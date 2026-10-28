package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * 筷子视角的火锅屏幕：九宫格 × 3x3 = 81 个食材槽，
 * 摆放为 9x9 的锅面，大格之间留分界。用法像熔炉：
 * 生的放进去，熟的直接取出来（取出时结算经验）。
 */
public class HotPotScreenHandler extends ScreenHandler {
    /** 大格间缝隙（像素）。 */
    public static final int GAP = 4;
    /** 锅面网格左上角（界面坐标）；每格 18 像素，物品画在格内向右下偏 1 像素处。 */
    public static final int GRID_X = 13;
    public static final int GRID_Y = 27;
    /** 横版界面：玩家背包在右侧状态卡片下方。 */
    public static final int INV_X = 198;
    public static final int INV_Y = 128;
    public static final int HOTBAR_Y = 186;

    private final Inventory pot;
    private final net.minecraft.world.World world;
    private final HotPotBlockEntity serverBe;
    private final Property[] properties;
    private final int[] clientData = new int[HotPotBlockEntity.PROP_COUNT];

    // 服务端构造
    public HotPotScreenHandler(int syncId, PlayerInventory playerInventory, HotPotBlockEntity be) {
        this(syncId, playerInventory, (Inventory) be, be);
    }

    // 客户端构造（扩展数据里只带方块坐标）
    public HotPotScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, clientInventory(playerInventory, pos), null);
    }

    private HotPotScreenHandler(int syncId, PlayerInventory playerInventory, Inventory pot,
                                HotPotBlockEntity be) {
        super(HotPotContent.HOT_POT_SCREEN, syncId);
        this.pot = pot;
        this.serverBe = be;
        this.world = playerInventory.player.getWorld();
        // 客户端在服务端属性包到达前也应显示“无汤”，而不是误显示为第一种汤底。
        clientData[0] = -1;
        for (int i = HotPotBlockEntity.PROP_EXTRA_BASES; i < HotPotBlockEntity.PROP_COUNT; i++) {
            clientData[i] = -1;
        }
        pot.onOpen(playerInventory.player);

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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }

        properties = new Property[HotPotBlockEntity.PROP_COUNT];
        for (int i = 0; i < properties.length; i++) {
            final int index = i;
            if (be != null) {
                properties[i] = addProperty(new Property() {
                    @Override
                    public int get() {
                        return be.propertyAt(index);
                    }

                    @Override
                    public void set(int value) {
                    }
                });
            } else {
                properties[i] = addProperty(new Property() {
                    @Override
                    public int get() {
                        return clientData[index];
                    }

                    @Override
                    public void set(int value) {
                        clientData[index] = value;
                    }
                });
            }
        }
    }

    private static Inventory clientInventory(PlayerInventory playerInventory, BlockPos pos) {
        net.minecraft.world.World world = playerInventory.player.getWorld();
        if (world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            return be;
        }
        return new net.minecraft.inventory.SimpleInventory(HotPotBlockEntity.SLOTS);
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
        return MathHelper.clamp(properties[4 + slot].get(), 0, HotPotCooking.FULL_DONE);
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
        ItemStack stack = pot.getStack(slot);
        if (stack.isEmpty()) {
            return -1F;
        }
        HotPotFuel fuel = HotPotFuel.byId(fuelId());
        SoupBase soup = SoupBase.byId(soupOfSlot(slot));
        if (fuel == null || burnTicks() <= 0) {
            return Float.POSITIVE_INFINITY;
        }
        float mul = soup == null ? 1F : soup.speedMul();
        int perTick = HotPotCooking.progressPerTick(HotPotCooking.cookTicks(world, stack), fuel.power(), mul);
        return (HotPotCooking.FULL_DONE - d) / (float) perTick / 20F;
    }

    /** 锅的材质：界面据此给锅沿、九宫格隔条上铜色或铁色；取不到时按铜锅。 */
    public HotPotMaterial potMaterial() {
        HotPotMaterial material = pot instanceof HotPotBlockEntity be ? be.potMaterial() : null;
        return material == null ? HotPotMaterial.COPPER : material;
    }

    // ------------------------------------------------------------------ 行为

    @Override
    public boolean canUse(PlayerEntity player) {
        return pot.canPlayerUse(player);
    }

    /**
     * 双击“拾取全部”走原版逻辑：同类物品收集到光标上那一组（最多收满一组），
     * 锅里的熟食被收走时照常经 onTakeItem 结算经验。
     * 锅里只收已经熟了的：没有熔炉配方的食材煮前煮后是同一种物品，不能把还在煮的一起拿走。
     * 客户端松开第二下时会用空物品问“这一格能不能当双击起点”（HandledScreen.mouseReleased），
     * 那一格的熟食刚被第一下拿走、已不算熟，所以空物品的询问一律放行，否则在锅里双击无效。
     */
    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        if (slot instanceof PotSlot) {
            // 鸳鸯 / 田字锅：双击只收和起点格子同一种汤底里的（用户定：同汤底才一键收，不同汤底的别收）
            if (!stack.isEmpty() && collecting && soupOfSlot(slot.getIndex()) != collectSoup) {
                return false;
            }
            return stack.isEmpty() || isCooked(slot.getIndex());
        }
        return super.canInsertIntoSlot(stack, slot);
    }

    /** 双击收取进行中（起点在锅里的格子）：只收这种汤底里的熟食。 */
    private boolean collecting = false;
    private int collectSoup = -1;

    /** 冷锅吃东西反胃的概率和时长（时长同原版河豚的反胃 15 秒）。 */
    private static final float COLD_NAUSEA_CHANCE = 0.3F;
    private static final int COLD_NAUSEA_TICKS = 300;

    /** 光标拿着筷子、左键点锅里的熟食：直接夹起来一口吃掉（只认熟食，其它情况照原版处理）。 */
    @Override
    public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        if (actionType == SlotActionType.PICKUP && button == 0 && slotId >= 0 && slotId < slots.size()
            && slots.get(slotId) instanceof PotSlot potSlot && potSlot.hasStack()
            && isCooked(potSlot.getIndex()) && getCursorStack().isOf(HotPotContent.CHOPSTICKS)) {
            if (serverBe != null) {
                eatWithChopsticks(player, potSlot);
            }
            return;
        }
        if (actionType == SlotActionType.PICKUP_ALL && slotId >= 0 && slotId < slots.size()
            && slots.get(slotId) instanceof PotSlot start && layout().multiSoup()) {
            collecting = true;
            collectSoup = soupOfSlot(start.getIndex());
            try {
                super.onSlotClick(slotId, button, actionType, player);
            } finally {
                collecting = false;
            }
            return;
        }
        super.onSlotClick(slotId, button, actionType, player);
    }

    /**
     * 一口吃掉：交给原版 PlayerEntity.eatFood，饥饿值、饱和度、药水效果、统计、进度、
     * 咀嚼声和打嗝声都和正常吃这个食物完全一样；照常结算出锅经验，碗之类的容器还给玩家。
     */
    private void eatWithChopsticks(PlayerEntity player, Slot slot) {
        ItemStack stack = slot.getStack();
        FoodComponent food = stack.getItem().getFoodComponent();   // 1.20.1 移植（）
        if (food == null) {
            return;
        }
        if (!player.canConsume(food.isAlwaysEdible())) {
            player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.too_full")), true);
            return;
        }
        int index = slot.getIndex();
        ItemStack eaten = slot.takeStack(1);
        serverBe.grantTakeXp(player, index, 1);
        if (!slot.hasStack()) {
            serverBe.onSlotEmptied(index);
        }
        // 主手或副手拿着蘸料：吃之前自动蘸一口（r103，用户：两种都要；2026-10-25 主手也算，）
        DippingSauceItem.dipFromHands(player, eaten);
        player.eatFood(world, eaten.copy());
        // 冷锅：没坐在烧着的灶台（或其他热源）上，吃了有概率反胃
        if (!serverBe.isHeated() && world.random.nextFloat() < COLD_NAUSEA_CHANCE) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, COLD_NAUSEA_TICKS, 0));
            player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.cold_pot")), true);
        }
        // 1.20.1 移植（）：1.20 的食物没有 usingConvertsTo，碗装的食物（蘑菇煲之类）照原版还碗
        if (!player.getAbilities().creativeMode) {
            ItemStack rest = eaten.getItem() instanceof net.minecraft.item.StewItem
                || eaten.getItem() instanceof net.minecraft.item.SuspiciousStewItem
                ? new ItemStack(net.minecraft.item.Items.BOWL) : eaten.getItem().getRecipeRemainder(eaten);
            if (!rest.isEmpty()) {
                player.getInventory().offerOrDrop(rest);
            }
        }
        // 嘴边迸出一小撮食物碎屑，像一口咬下去
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, eaten),
                player.getX(), player.getEyeY() - 0.15, player.getZ(), 8, 0.12, 0.06, 0.12, 0.05);
        }
        sendContentUpdates();
    }

    /** shift 键：背包 → 锅里空格；锅里 → 背包，并按实际移动数量结算经验。 */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotId);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        int before = stack.getCount();
        boolean fromPot = slot instanceof PotSlot;
        boolean cookedOutput = fromPot && isCooked(slot.getIndex());

        if (fromPot) {
            if (!insertItem(stack, HotPotBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!insertItem(stack, 0, HotPotBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        int moved = before - stack.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        if (cookedOutput && serverBe != null) {
            serverBe.grantTakeXp(player, slot.getIndex(), moved);
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
            if (fromPot && serverBe != null) {
                serverBe.onSlotEmptied(slot.getIndex());
            }
        } else {
            slot.markDirty();
        }
        sendContentUpdates();
        return original;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        pot.onClose(player);
    }

    /** 锅内槽位：像熔炉一样，生进熟出，随时可拿。 */
    private final class PotSlot extends Slot {
        PotSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            // 每个物理槽是一个独立的“单份格”，绝不能把新堆叠并入已有格。
            // 这条 empty-only 约束同时覆盖普通点击、拖拽和 quickMove，
            // 从根上阻断 ScreenHandler.insertItem 的自动合并路径。
            return !hasStack() && !isCooked(getIndex()) && slotInUse(getIndex())
                && HotPotCooking.acceptable(world, stack);
        }

        /** 鸳鸯锅正中那个大格不用：格子藏起来、点不到（）。 */
        @Override
        public boolean isEnabled() {
            return slotInUse(getIndex());
        }

        @Override
        public int getMaxItemCount() {
            // 一格只允许一份，不能因为旧存档或客户端异常包写入整组物品。
            return 1;
        }

        /**
         * 原版点击流程最终会走 Slot.insertStack；这里禁止它复用/合并已有物品，
         * 并把一次放入严格限制为一份。返回值是未放入的剩余物品，交还给光标，
         * 因而不会出现“整组物品被写进一个槽”或被挤到 Slot 0 的副作用。
         */
        @Override
        public ItemStack insertStack(ItemStack stack, int count) {
            if (stack.isEmpty() || hasStack() || !canInsert(stack)) {
                return stack;
            }
            int moved = Math.min(Math.min(count, stack.getCount()), getMaxItemCount(stack));
            if (moved <= 0) {
                return stack;
            }
            setStack(stack.copyWithCount(moved));
            ItemStack remainder = stack.copy();
            remainder.decrement(moved);
            return remainder;
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            if (serverBe != null) {
                // 先结算经验，再清理空槽的熟食标记。
                serverBe.grantTakeXp(player, getIndex(), stack.getCount());
                if (getStack().isEmpty()) {
                    serverBe.onSlotEmptied(getIndex());
                }
            }
        }

    }
}
