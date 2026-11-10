package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * 火锅库存与烹饪数据。
 *
 * <p>独立摆放的锅使用本类；坐上灶台后的组合方块由
 * {@link StoveBlockEntity} 继承本类。因此两种形态共用完全相同的 81 格
 * 食材、汤底、烹饪进度和经验逻辑。81 格怎么分边、每边一种汤由锅的 {@link PotLayout} 决定
 * （九宫格锅一边；鸳鸯锅两边；田字锅四边，2026-10-23 起）。</p>
 */
public class HotPotBlockEntity extends BlockEntity
    implements Container {
    /** 9x9 = 81 个食材槽，每 3x3 为一个大格。 */
    public static final int SLOTS = 81;
    /** 屏幕属性包使用 short，熟食位图每组只占 15 位，避免高位被截断。 */
    public static final int COOKED_WORD_BITS = 15;
    public static final int COOKED_WORDS = (SLOTS + COOKED_WORD_BITS - 1) / COOKED_WORD_BITS;
    /** 0汤底 1燃料类型 2余燃 3总燃 4..84熟度 85..90熟食位图 91..93鸳鸯 / 田字锅其余几边的汤底。 */
    public static final int PROP_EXTRA_BASES = 4 + SLOTS + COOKED_WORDS;
    public static final int PROP_COUNT = PROP_EXTRA_BASES + PotLayout.MAX_SIDES - 1;

    final SimpleContainer ingredients = new SimpleContainer(SLOTS);
    final int[] doneness = new int[SLOTS];
    final int[] cookDur = new int[SLOTS];
    // mryt233 改（2026-11-10）：只在修订号变化时重算，doneness 始终保留千分比。
    long cookRevision = -1;
    final float[] xpValue = new float[SLOTS];
    final boolean[] cooked = new boolean[SLOTS];
    /**
     * mryt233 改（2026-11-04）：熟了以后还在锅里继续煮的刻数（豆腐煮过头碎成豆腐块用）。
     * 只对登记过 `HotPotCooking.overcook` 的熟食累加；煮熟那一刻清 0，取走 / 换掉时一起重置。存档里是 Overcook 数组。
     */
    final int[] overcook = new int[SLOTS];
    final ItemStack[] tracked = new ItemStack[SLOTS];
    float pendingXp = 0F;
    /**
     * 每边一种汤底（-1 = 没汤）：九宫格锅只用第 0 边；鸳鸯锅 0、1；田字锅 0..3（PotLayout）。
     * 汤底不和格局绑定，任何汤底（包括以后新加的）都能倒进任何一边。调整（原来是单个 base）。
     */
    final int[] bases = {-1, -1, -1, -1};
    /**
     * mryt233 改（2026-11-05）：每边的红汤辣度档位（r136a），和 {@link #bases} 一一对应：
     * 0 = 微辣（刚倒进红汤的档位）、1 = 中辣、2 = 特辣、3 = 3 级特辣。
     * 只有红汤边有意义；倒汤 / 舀汤 / 清空汤底时归 0。存档字段见 HotPotStorage.SPICY_KEYS。
     */
    final int[] spicyLevels = {0, 0, 0, 0};
    /** 客户端缓动热度，用于让食材起伏和蒸汽自然启停。 */
    private float clientHeat = 0F;
    /** 气泡累计相位（秒）：随热度连续累加，避免用「全局时间÷随热度变化的周期」造成相位跳变（2026-10-30）。 */
    private double bubbleSeconds;
    /** 食材浮动角速度（弧度 / 刻）：沸腾 / 冷锅两档，渲染器和这里共用。 */
    public static final float BOB_OMEGA = 0.22F;
    public static final float BOB_COLD_OMEGA = 0.05F;
    /**
     * 每边汤里「一格几样菜轮流显示」的累计相位（弧度）。以前用「世界总刻数 × 冷 / 沸两档速度」，点火 / 熄火时
     * 速度一换，几百万刻乘出来的结果直接跳，显示的菜突然换成另一样，筷子也可能夹错（mryt233 2026-11-01，代码检查 B4）。
     */
    private final double[] cycleAngle = new double[PotLayout.MAX_SIDES];
    /** 新旧存档首次进入服务端 tick 时修复新增的汤底方块状态。 */
    boolean needsVisualRepair = true;
    /** 玩家破坏回调与状态替换回调之间的一次性防重复标记。 */
    private boolean breakHandled = false;
    /**
     * 格子可能被外部动过（界面、漏斗、其他模组走 Container 接口，或原地改了物品数量后 setChanged）。
     * 只有它为 true 时服务端 tick 才逐格比较找变化，不再每刻扫 81 格（mryt233 2026-11-01，代码检查 W4）。
     */
    boolean slotsTouched = true;
    /** 这一刻里有改动要同步给客户端：一刻结束时只发一次，不再每熟一样菜就发一次整锅（代码检查 W3）。 */
    boolean syncPending = false;

    /** 独立火锅的方块实体构造器。 */
    public HotPotBlockEntity(BlockPos blockPos, BlockState state) {
        this(HotPotContent.HOT_POT_BE, blockPos, state);
    }

    /** 供组合灶台子类使用的构造器。 */
    protected HotPotBlockEntity(BlockEntityType<?> type, BlockPos blockPos, BlockState state) {
        super(type, blockPos, state);
        java.util.Arrays.fill(tracked, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Level level, BlockPos blockPos, BlockState state, HotPotBlockEntity be) {
        // 远距离传送/重生后，客户端可能残留已卸载区块的方块实体 ticker：
        // 位置上的实际方块已经不是锅或灶台（level.getBlockState 读出来是 void_air）。
        // 这种残留 ticker 不能继续跑，否则读取 lit 等属性时会抛异常（2026-11-01，崩溃报告 02:37:16）。
        BlockState current = level.getBlockState(blockPos);
        if (!StoveBlock.isStove(current) && !(current.getBlock() instanceof HotPotBlock)) {
            return;
        }
        if (level.isClientSide) {
            be.clientTick(level, blockPos, state);
            return;
        }
        be.serverTick(level);
    }

    /** 服务端烹饪：具体逻辑在 {@link HotPotCookingRun}。 */
    protected void serverTick(Level level) {
        HotPotCookingRun.tick(this, level);
    }

    /** 客户端粒子表现：具体逻辑在 {@link HotPotEffects}。 */
    protected void clientTick(Level level, BlockPos blockPos, BlockState state) {
        clientHeat = HotPotEffects.tick(this, level, blockPos, state, clientHeat);
        // 相位按「原始瞬时速度」积分：每 tick 增量 = (0.7 + 0.5*heat)/20；
        // 渲染端周期已去掉热度分母（见 BoilingEffects），两者相乘正好还原原来的速度，且连续不跳变
        bubbleSeconds += (1.0 / 20.0) * (0.7 + 0.5 * clientHeat);
        for (int side = 0; side < cycleAngle.length; side++) {
            cycleAngle[side] += cycleRate(side);
        }
    }

    /** 这一边轮换相位每刻走多少：那边有汤且锅是冷的走慢档，否则快档（同渲染器原来的判断）。 */
    private float cycleRate(int side) {
        return baseOfSide(side) >= 0 && clientHeat < 0.5F ? BOB_COLD_OMEGA : BOB_OMEGA;
    }

    /** 某一边轮换相位，partial = 这一刻已经过去的比例（渲染插值用）。 */
    public double cycleAngle(int side, float partial) {
        return side < 0 || side >= cycleAngle.length ? 0 : cycleAngle[side] + partial * cycleRate(side);
    }

    /** 世界气泡用的累计时间（秒），带这一刻内的插值：只按刻累加的话高帧率下气泡 20 次 / 秒地跳（代码检查 B5）。 */
    public double bubbleSeconds(float partial) {
        return bubbleSeconds + partial * (0.7 + 0.5 * clientHeat) / 20.0;
    }

    // ------------------------------------------------------------------ 灶台联动

    /**
     * 返回实际热源。组合灶台自身就是热源；独立火锅则查找正下方的组合灶台。
     */
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

    /** 当前方块是否承载一个锅。 */
    public boolean hasPot() {
        // 用方块实体自己记着的状态：这一格的方块状态一变，原版就会同步到这里（LevelChunk.setBlockState），
        // 不用每次去世界里查——烹饪每刻每格都要问锅的格局（mryt233 2026-11-01，代码检查 W2）
        BlockState state = getBlockState();
        if (StoveBlock.isStove(state)) {
            return state.getValue(StoveBlock.POT).hasPot();
        }
        return state.getBlock() instanceof HotPotBlock;
    }

    /** 组合灶台的锅状态必须与库存实体保持一致，避免旧存档出现“有库存但无锅”状态。 */
    /** 供 HotPotStorage：读完存档后标记需要修复方块状态。 */
    void markNeedsVisualRepair() {
        needsVisualRepair = true;
    }

    public void repairVisualState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.getValue(StoveBlock.POT);
            if (!pot.hasPot()) {
                // 旧版灶台没有锅库存，不能凭空猜材质；清理无法显示的幽灵内容与汤底。
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

    /** 当前锅材质；没有锅时返回 null。 */
    public HotPotMaterial potMaterial() {
        BlockState state = getBlockState();   // 同 hasPot（W2）
        if (StoveBlock.isStove(state)) {
            return state.getValue(StoveBlock.POT).material();
        }
        return state.getBlock() instanceof HotPotBlock pot ? pot.material() : null;
    }

    /** 篝火上煮的火力：同灶台烧煤炭 / 木炭（HotPotFuel.COAL）。 */
    public static final float OPEN_FIRE_POWER = 1.0F;
    /** 界面属性里「燃料类型」的特殊值：锅下面是篝火这类明火（代码检查 B6）。 */
    public static final int FUEL_OPEN_FIRE = -2;

    /** 独立锅下面是不是点着的篝火这类热源（{@link HotPotHeat}，标签 heat_sources）。 */
    public boolean heatSourceBelow() {
        return level != null && !(this instanceof StoveBlockEntity) && HotPotHeat.isHeatSource(level, worldPosition.below());
    }

    /** 灶火是否正旺。 */
    public boolean hasFire() {
        if (level == null) {
            return false;
        }
        if (this instanceof StoveBlockEntity) {
            // 只有实际方块仍是灶台时才读 LIT：区块卸载后这里会读到 void_air，没有 lit 属性（2026-11-01）
            BlockState state = level.getBlockState(worldPosition);
            return StoveBlock.isStove(state) && state.getValue(StoveBlock.LIT);
        }
        StoveBlockEntity stove = stoveBelow();
        return (stove != null && stove.burning()) || heatSourceBelow();
    }

    /** 清空汤底并同步空锅外观，拆锅或重新安装时使用。 */
    protected void clearSoupState() {
        java.util.Arrays.fill(bases, -1);
        java.util.Arrays.fill(spicyLevels, 0);   // mryt233 改（2026-11-05）：汤底没了，辣度一起归零（r136a）
        updateSoupVisual();
        markDirtyAndSync();
    }

    /** 让锅的外观状态与汤底种类保持一致。 */
    protected void updateSoupVisual() {
        if (level == null) {
            return;
        }
        SoupVisual visual = blockSoupVisual();
        BlockState state = level.getBlockState(worldPosition);
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.getValue(StoveBlock.POT);
            BlockState updated = state.setValue(StoveBlock.POT, pot.withSoup(hasSoup()))
                .setValue(StoveBlock.SOUP, visual);
            level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
        } else if (state.getBlock() instanceof HotPotBlock) {
            BlockState updated = state.setValue(HotPotBlock.HAS_SOUP, hasSoup())
                .setValue(HotPotBlock.SOUP, visual)
                .setValue(HotPotBlock.BOILING, hasFire() && hasSoup());
            level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
        }
    }

    void updateStandaloneBoilingVisual(boolean boiling) {
        HotPotEffects.updateStandaloneBoiling(this, boiling);
    }

    // ------------------------------------------------------------------ 交互

    /** 筷子任何时候都能打开当前锅的面板。 */
    public void useWithChopsticks(Player player) {
        if (level == null || level.isClientSide || !hasPot()) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            HotpotScreens.openPot(sp, this);
        }
    }

    /**
     * 手里的东西能不能把汤底舀走：只认空桶（r99 起碗是盛汤 / 盛熟菜，见 serveWithBowl）。
     * 舀出水 / 牛奶 / 岩浆还回原来的桶；菌汤桶装不了，就是倒掉（用户定的）。
     * 每边一种汤（九宫格锅一边；鸳鸯 / 田字锅按准星那边，见 scoopSoup），食材可以先放，汤底只决定那边煮不煮。
     */
    public boolean canScoopWith(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.BUCKET);
    }

    /**
     * 用空容器把汤底舀出来：倒进去的是什么就还什么（水桶、牛奶桶、岩浆桶、蘑菇煲）。
     * 锅里还有食物的话，先全部掉到地上，再清空汤底。
     */
    public boolean scoopSoup(Player player, InteractionHand hand) {
        return scoopSoup(player, hand, PotLayout.SIDE_A);
    }

    /** 鸳鸯锅只舀准星那一边的汤（那边的食材掉出来），另一边不动。 */
    public boolean scoopSoup(Player player, InteractionHand hand, int side) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        if (!layout().multiSoup()) {
            side = PotLayout.SIDE_A;
        }
        ItemStack stack = player.getItemInHand(hand);
        SoupBase soup = SoupBase.byId(baseOfSide(side));
        if (soup == null) {
            player.displayClientMessage(Component.translatable(key("msg.no_soup_to_scoop")), true);
            return false;
        }
        if (!stack.is(soup.remainder())) {
            // 菌汤：桶装不了蘑菇煲，就是把汤倒掉，桶还是空桶（r99）
            dropContents(side);
            setBaseOfSide(side, -1);
            setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：倒掉汤底，辣度归零（r136a）
            updateSoupVisual();
            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.0F);
            markDirtyAndSync();
            return true;
        }
        dropContents(side);
        setBaseOfSide(side, -1);
        setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：舀走汤底，辣度归零（r136a）
        updateSoupVisual();
        player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, new ItemStack(soup.source())));
        net.minecraft.sounds.SoundEvent sound = soup == SoupBase.SPICY ? SoundEvents.BUCKET_FILL_LAVA
            : soup.remainder() == net.minecraft.world.item.Items.BOWL ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        markDirtyAndSync();
        return true;
    }

    /**
     * 拿空碗点锅（r99）：锅里有熟菜就盛进碗里（先盛准星对着那一大格的，最多 3 样，碗变成装着菜的碗）；
     * 没熟菜但有汤，就盛一碗汤（锅里的汤不会少，用户定的「盛不完」）。
     */
    public boolean serveWithBowl(Player player, InteractionHand hand, int bigCell) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        java.util.List<ItemStack> scooped = new java.util.ArrayList<>();
        float xp = 0;
        java.util.List<Integer> order = new java.util.ArrayList<>();
        // 鸳鸯 / 田字锅只盛和准星那一边同一种汤底里的菜（两边汤底一样就都能盛；用户定）
        int soup = baseOfSide(layout().sideOfCell(Math.max(0, bigCell)));
        for (int i = 0; i < 9; i++) {
            int cell = (Math.max(0, bigCell) + i) % 9;
            if (layout().usesCell(cell) && (!layout().multiSoup() || baseOfSide(layout().sideOfCell(cell)) == soup)) {
                order.add(cell);
            }
        }
        // 先盛你看到浮在准星那格上的那一样（用户 2026-10-26：和筷子一样；它还生着就跳过），再照原来的顺序盛
        int shown = shownIn(player, bigCell);
        if (shown >= 0 && cooked[shown]) {
            ItemStack food = ingredients.getItem(shown).copy();
            xp += xpValue[shown] * food.getCount();
            ingredients.setItem(shown, ItemStack.EMPTY);
            resetSlot(shown, ItemStack.EMPTY);
            scooped.add(food.copyWithCount(1));
            if (food.getCount() > 1) {
                ingredients.setItem(shown, food.copyWithCount(food.getCount() - 1));
            }
        }
        outer:
        for (int cell : order) {
            int slot;
            while (scooped.size() < 3 && (slot = firstCookedSlotInCell(cell)) >= 0) {
                ItemStack food = ingredients.getItem(slot).copy();
                xp += xpValue[slot] * food.getCount();
                ingredients.setItem(slot, ItemStack.EMPTY);
                resetSlot(slot, ItemStack.EMPTY);
                // 一格里只有 1 个；万一有多个，多出来的放回去
                scooped.add(food.copyWithCount(1));
                if (food.getCount() > 1) {
                    ingredients.setItem(slot, food.copyWithCount(food.getCount() - 1));
                }
                if (scooped.size() >= 3) {
                    break outer;
                }
            }
        }
        ItemStack result;
        if (!scooped.isEmpty()) {
            HotPotCriterion.fire(player, "pot_cooked");
            // mryt233 改（2026-11-05，r136a）：碗里盛到的熟虾也算「煮虾」成就。
            // pot_cooked 之外再记一次；豆腐块是煮过头的，盛到碗里不再另记（免得重复触发）
            for (ItemStack food : scooped) {
                if (food.is(HotPotContent.COOKED_SHRIMP) || food.is(HotPotContent.COOKED_PEELED_SHRIMP)
                    || food.is(HotPotContent.COOKED_SHRIMP_PASTE)) {
                    HotPotCriterion.fire(player, "cook_shrimp");
                }
            }
            result = new ItemStack(net.minecraft.world.item.Items.BOWL);
            result.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(scooped));
            PlateItem.syncPlateModel(result);
            if (xp > 0) {
                net.minecraft.world.entity.ExperienceOrb.award((net.minecraft.server.level.ServerLevel) level,
                    Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), Math.max(1, Math.round(xp)));
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.2F);
        } else if (SoupBase.byId(bowlSoup(bigCell)) != null) {
            result = new ItemStack(HotPotContent.SOUP_BOWLS.get(bowlSoup(bigCell)));
            // mryt233 改（2026-11-05）：盛出来的红汤碗带着这一边的辣度档位，喝的时候按它给效果（r136a）
            if (bowlSoup(bigCell) == SoupBase.SPICY.ordinal()) {
                int spicy = spicyOfSide(soupSideOf(bigCell, bowlSoup(bigCell)));
                if (spicy > 0) {
                    result.set(HotPotContent.SPICY_LEVEL, spicy);
                }
            }
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable(key("msg.bowl_nothing")), true);
            return false;
        }
        player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(held, player, result));
        markDirtyAndSync();
        return true;
    }

    public boolean pourSoup(Player player, InteractionHand hand) {
        return pourSoup(player, hand, PotLayout.SIDE_A);
    }

    /** 鸳鸯锅倒进准星那一边；两边可以是任意两种汤底（也可以一样），以后加的新汤底也一样能倒。 */
    public boolean pourSoup(Player player, InteractionHand hand, int side) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        if (!layout().multiSoup()) {
            side = PotLayout.SIDE_A;
        }
        ItemStack stack = player.getItemInHand(hand);
        SoupBase soup = SoupBase.fromStack(stack);
        if (soup == null) {
            return false;
        }
        if (soup.isAdditive(stack)) {
            // 锅底料（番茄 / 三鲜）：只能加进清汤里，把这边的清汤变成这种汤；没汤先倒清汤，别的汤（含已经加过的）不能再加（用户 2026-10-26）
            int current = baseOfSide(side);
            if (current < 0) {
                player.displayClientMessage(Component.translatable(key("msg.base_needs_clear")), true);
                return false;
            }
            if (current != SoupBase.CLEAR.ordinal()) {
                player.displayClientMessage(Component.translatable(key("msg.base_only_clear")), true);
                return false;
            }
            setBaseOfSide(side, soup.ordinal());
            setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：换汤底，辣度回到 0 档（r136a）
            consumeHand(player, hand, net.minecraft.world.item.Items.AIR);
            updateSoupVisual();
            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.3F);
            markDirtyAndSync();
            return true;
        }
        if (baseOfSide(side) >= 0) {
            player.displayClientMessage(Component.translatable(key("msg.already_soup")), true);
            return false;
        }
        setBaseOfSide(side, soup.ordinal());
        setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：倒新汤，辣度回到 0 档（微辣）（r136a）
        consumeHand(player, hand, soup.remainder());
        updateSoupVisual();
        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY,
            SoundSource.BLOCKS, 1.0F, 1.15F);
        markDirtyAndSync();
        return true;
    }

    // ------------------------------------------------------------------ 红汤辣度（r136a，mryt233 2026-11-05）

    /** 辣度最高档：3 = 3 级特辣。 */
    public static final int SPICY_MAX = 3;

    /** 某一边的辣度档位（0 = 微辣）；不用 / 越界的边按 0 算。 */
    public int spicyOfSide(int side) {
        return side >= 0 && side < spicyLevels.length ? spicyLevels[side] : 0;
    }

    /** 把某一边的辣度档位夹到 0..3 后写进去（客户端只读快照，不调用）。 */
    public void setSpicyOfSide(int side, int level) {
        if (side >= 0 && side < spicyLevels.length) {
            spicyLevels[side] = Math.max(0, Math.min(SPICY_MAX, level));
        }
    }

    /** 这档辣度的名字（0 微辣、1 中辣、2 特辣、3 级特辣），语言键由语言文件提供。 */
    public static Component spicyLevelText(int level) {
        return Component.translatable(key("spicy." + Math.max(0, Math.min(SPICY_MAX, level))));
    }

    /**
     * mryt233 改（2026-11-05）：手里这件是不是「调辣度的东西」——辣椒加辣，冰块 / 浮冰 / 蓝冰降辣。
     * HotPotBlock / StoveBlock 用它决定要不要先试调辣度，再考虑下锅当食材。
     */
    public static boolean isSpicyModifier(ItemStack stack) {
        return stack.is(HotPotContent.CHILI_PEPPER) || stack.is(net.minecraft.world.item.Items.ICE)
            || stack.is(net.minecraft.world.item.Items.PACKED_ICE) || stack.is(net.minecraft.world.item.Items.BLUE_ICE);
    }

    /** 冰块降辣几档：普通冰 1 档、浮冰 2 档、蓝冰直接压到 0 档（用户定的，见任务单）。 */
    private static int iceDrop(ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.BLUE_ICE)) {
            return SPICY_MAX;
        }
        if (stack.is(net.minecraft.world.item.Items.PACKED_ICE)) {
            return 2;
        }
        return 1;
    }

    /**
     * 拿着辣椒 / 冰块点红汤锅：辣椒加 1 档、冰块降 1~3 档（r136a）。
     * 只对红汤边有效；别的汤底上拿着这两样会提示「只有红汤锅底才能调整辣度」并吃掉这次点击。
     * 返回 true = 这次点击已经处理掉了（调用方直接算成功，不要再当食材下锅）。
     */
    public boolean adjustSpiciness(Player player, InteractionHand hand, int side) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        if (!layout().multiSoup()) {
            side = PotLayout.SIDE_A;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean chili = stack.is(HotPotContent.CHILI_PEPPER);
        boolean ice = stack.is(net.minecraft.world.item.Items.ICE)
            || stack.is(net.minecraft.world.item.Items.PACKED_ICE)
            || stack.is(net.minecraft.world.item.Items.BLUE_ICE);
        if (!chili && !ice) {
            return false;
        }
        // 不是红汤（含这一边还没倒汤）：提示一句就吃掉这次点击，别让辣椒 / 冰块被当成食材塞进锅槽
        if (SoupBase.byId(baseOfSide(side)) != SoupBase.SPICY) {
            player.displayClientMessage(Component.translatable(key("msg.spicy_need_red_broth")), true);
            return true;
        }
        int spicy = spicyOfSide(side);
        if (chili) {
            if (spicy >= SPICY_MAX) {
                player.displayClientMessage(Component.translatable(key("msg.spicy_max")), true);
                return true;
            }
            setSpicyOfSide(side, spicy + 1);
            consumeHand(player, hand, net.minecraft.world.item.Items.AIR);
            level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5F, 1.2F);
            spawnSpicyParticles(true);
            player.displayClientMessage(Component.translatable(key("msg.spicy_up"), spicyLevelText(spicy + 1)), true);
            if (spicy + 1 >= SPICY_MAX) {
                HotPotCriterion.fire(player, "max_spicy");   // 成就：吃辣吃到 3 级特辣（r136d 里配进度）
            }
        } else {
            if (spicy <= 0) {
                player.displayClientMessage(Component.translatable(key("msg.spicy_min")), true);
                return true;
            }
            int next = Math.max(0, spicy - iceDrop(stack));
            setSpicyOfSide(side, next);
            consumeHand(player, hand, net.minecraft.world.item.Items.AIR);
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.3F);
            spawnSpicyParticles(false);
            player.displayClientMessage(Component.translatable(key("msg.spicy_down"), spicyLevelText(next)), true);
        }
        markDirtyAndSync();
        return true;
    }

    /** 加辣 / 降辣时锅面上的那几颗粒子：加辣火苗 + 岩浆，降辣白云 + 水花。 */
    private void spawnSpicyParticles(boolean up) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 at = Vec3.atCenterOf(worldPosition).add(0, 0.95, 0);
        if (up) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                at.x, at.y, at.z, 14, 0.38, 0.12, 0.38, 0.02);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                at.x, at.y, at.z, 3, 0.3, 0.1, 0.3, 0.0);
        } else {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                at.x, at.y, at.z, 14, 0.38, 0.12, 0.38, 0.02);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                at.x, at.y, at.z, 8, 0.3, 0.1, 0.3, 0.0);
        }
    }

    /**
     * 用碗盛的这一碗汤实际来自哪一边：准星那格那一边有这种汤就取那边，没有就取第一处有这种汤的边。
     * （{@link HotPotSoup#bowlSoup} 的逻辑，这里要的是「哪一边」而不是「哪种汤」。）
     */
    private int soupSideOf(int bigCell, int soupId) {
        PotLayout layout = layout();
        int aimed = layout.sideOfCell(Math.max(0, bigCell));
        if (aimed != PotLayout.NONE && baseOfSide(aimed) == soupId) {
            return aimed;
        }
        for (int side = 0; side < layout.sides(); side++) {
            if (baseOfSide(side) == soupId) {
                return side;
            }
        }
        return aimed;
    }

    /** 这一格所在那一边的辣度（九宫格锅就是唯一那一边）。 */
    public int spicyOfSlot(int slot) {
        int side = layout().sideOfSlot(slot);
        return side == PotLayout.NONE ? 0 : spicyOfSide(side);
    }

    /** 筷子上夹着的食物放进锅里指定的小格（熟食、汤底原料放不进）。 */
    public boolean insertHeld(Player player, ItemStack chopsticks, int slot) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        ItemStack food = Chopsticks.held(chopsticks);
        if (!HotPotCooking.acceptable(level, food)) {
            player.displayClientMessage(Component.translatable(key("msg.cannot_cook")), true);
            return false;
        }
        if (slot < 0 || slot >= SLOTS) {
            player.displayClientMessage(Component.translatable(key("msg.cell_full")), true);
            return false;
        }
        if (!ingredients.getItem(slot).isEmpty()) {
            player.displayClientMessage(Component.translatable(key("msg.slot_occupied")), true);
            return false;
        }
        ingredients.setItem(slot, food.copyWithCount(1));
        Chopsticks.hold(chopsticks, ItemStack.EMPTY);
        onSlotStackChanged(slot);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.35F);
        return true;
    }

    /**
     * 拿着装了菜的盘子点锅：能下锅的生食材随机倒进九个大格的空位，熟食和放不下的留在盘子里。
     * 一叠盘子只倒最上面那一个。
     */
    public boolean pourPlate(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        PlateBlock plateBlock = PlateItem.placedFor(held);
        if (plateBlock == null || PlateItem.keepsSelf(held)) {
            return false;
        }
        net.minecraft.world.item.component.ItemContainerContents contents = held.getOrDefault(
            net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
        NonNullList<ItemStack> foods = NonNullList.withSize(plateBlock.size().capacity(), ItemStack.EMPTY);
        contents.copyInto(foods);
        if (foods.stream().allMatch(ItemStack::isEmpty)) {
            player.displayClientMessage(Component.translatable(key("msg.plate_empty")), true);
            return false;
        }
        int moved = 0;
        for (int i = 0; i < foods.size(); i++) {
            ItemStack food = foods.get(i);
            if (food.isEmpty() || !HotPotCooking.acceptable(level, food)) {
                continue;
            }
            int slot = randomEmptySlot();
            if (slot < 0) {
                break;
            }
            ingredients.setItem(slot, food.copyWithCount(1));
            onSlotStackChanged(slot);
            foods.set(i, ItemStack.EMPTY);
            moved++;
        }
        if (moved == 0) {
            player.displayClientMessage(Component.translatable(key("msg.plate_nothing_to_pour")), true);
            return false;
        }
        ItemStack plate = held.getCount() > 1 ? held.split(1) : held;
        plate.set(net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.fromItems(foods));
        PlateItem.syncPlateModel(plate);
        if (plate != held && !player.getInventory().add(plate)) {
            player.drop(plate, false);
        }
        level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.4F);
        return true;
    }

    public boolean insertIngredient(Player player, InteractionHand hand) {
        int slot = firstEmptySlot();
        if (slot < 0) {
            player.displayClientMessage(Component.translatable(key("msg.pot_full")), true);
            return false;
        }
        return insertIngredient(player, hand, slot);
    }

    /** 将一份食材放入准星对应槽位；占用时不挪动旧物品。 */
    public boolean insertIngredient(Player player, InteractionHand hand, int slot) {
        if (level == null || level.isClientSide || !hasPot()) {
            return false;
        }
        ItemStack handStack = player.getItemInHand(hand);
        if (!HotPotCooking.acceptable(level, handStack)) {
            return false;
        }
        if (slot < 0 || slot >= SLOTS) {
            player.displayClientMessage(Component.translatable(key("msg.cell_full")), true);
            return false;
        }
        if (!ingredients.getItem(slot).isEmpty()) {
            player.displayClientMessage(Component.translatable(key("msg.slot_occupied")), true);
            return false;
        }
        ingredients.setItem(slot, handStack.copyWithCount(1));
        consumeHand(player, hand, net.minecraft.world.item.Items.AIR);
        onSlotStackChanged(slot);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP,
            SoundSource.BLOCKS, 0.35F, 1.35F);
        return true;
    }

    /**
     * 服务端原子取件：先从熟食槽移出整份，再尝试放入背包，余量安全掉在锅边。
     * 只有实际移出的数量会结算经验，调用者重复发包也不会复制物品。
     */
    public boolean collectCookedSlot(Player player, int slot) {
        // mryt233 改（2026-11-05，r136a）：捞起来的是熟虾 / 热豆腐块，记一条对应的成就
        ItemStack taking = slot >= 0 && slot < SLOTS ? ingredients.getItem(slot) : ItemStack.EMPTY;
        int delivered = transferCookedSlot(player, slot);
        if (delivered > 0) {
            HotPotCriterion.fire(player, "pot_cooked");
            HotPotCookingRun.fireTakeCriteria(player, taking);
            finishCollection();
        }
        return delivered > 0;
    }

    /**
     * 空筷子直接点锅（2026-10-25 用户定，开发计划「筷子新交互」；新增）：从这一格夹一样熟食到筷子上，
     * 照常结算出锅经验。夹着的菜之后按住右键吃、或放进碗盘 / 另一格，见 ChopsticksItem。
     */
    public boolean pickWithChopsticks(Player player, ItemStack chopsticks, int slot) {
        if (level == null || level.isClientSide || slot < 0 || slot >= SLOTS || !cooked[slot]
            || !Chopsticks.held(chopsticks).isEmpty()) {
            return false;
        }
        ItemStack stored = ingredients.getItem(slot);
        if (stored.isEmpty()) {
            return false;
        }
        float slotXp = xpValue[slot];
        // mryt233 改（2026-11-05，r136a）：夹起来的是熟虾 / 热豆腐块，记一条对应的成就
        HotPotCookingRun.fireTakeCriteria(player, stored);
        Chopsticks.hold(chopsticks, stored.copyWithCount(1));
        if (stored.getCount() > 1) {
            ItemStack rest = stored.copyWithCount(stored.getCount() - 1);
            ingredients.setItem(slot, rest);
            tracked[slot] = rest.copy();
        } else {
            ingredients.setItem(slot, ItemStack.EMPTY);
            resetSlot(slot, ItemStack.EMPTY);
        }
        HotPotCookingRun.grantExperience(this, slotXp, 1);
        HotPotCriterion.fire(player, "pot_cooked");
        finishCollection();
        return true;
    }

    /**
     * 空筷子直接右键锅：夹客户端报来的「这一刻浮在这格上的那样」（ChopsticksAimPayload）；它还生着就提示、不夹。
     * 没报（或报的不在这格）就夹这格第一样熟的。返回 false = 这格没有能夹的熟食。新增（2026-10-25）。
     */
    public boolean pickShown(Player player, ItemStack chopsticks, int bigCell) {
        int shown = shownIn(player, bigCell);
        if (shown >= 0) {
            if (!cooked[shown]) {
                player.displayClientMessage(Component.translatable(key("msg.not_cooked_yet")), true);
                return true;
            }
            return pickWithChopsticks(player, chopsticks, shown);
        }
        return pickWithChopsticks(player, chopsticks, firstCookedSlotInCell(bigCell));
    }

    /** 客户端报来的「这一刻浮在这格上的那一小格」（ChopsticksAimPayload）：在这格里、有东西才算，否则 -1。用一次就清掉。 */
    private int shownIn(Player player, int bigCell) {
        int shown = ChopsticksAim.take(player, worldPosition);
        return shown >= 0 && shown < SLOTS && firstSlotInCell(bigCell, slot -> slot == shown) == shown
            && !ingredients.getItem(shown).isEmpty() ? shown : -1;
    }

    /**
     * 空手右键锅捞熟食：捞的是这一刻浮在这格上、你看到的那一样（用户 2026-10-26：和筷子一样）；它还生着就提示、不捞。
     * 没报就捞这格第一样熟的。返回 false = 这格没有能捞的熟食。新增
     */
    public boolean collectShown(Player player, int bigCell) {
        int shown = shownIn(player, bigCell);
        if (shown >= 0) {
            if (!cooked[shown]) {
                player.displayClientMessage(Component.translatable(key("msg.not_cooked_yet")), true);
                return true;
            }
            return collectCookedSlot(player, shown);
        }
        return collectCookedSlot(player, firstCookedSlotInCell(bigCell));
    }

    private int transferCookedSlot(Player player, int slot) {
        if (level == null || level.isClientSide || slot < 0 || slot >= SLOTS || !cooked[slot]) {
            return 0;
        }
        ItemStack stored = ingredients.getItem(slot);
        if (stored.isEmpty()) {
            resetSlot(slot, ItemStack.EMPTY);
            return 0;
        }

        float slotXp = xpValue[slot];
        // 不用 copyAndEmpty()：它会把库存里那个 ItemStack 对象自己清成 0 个。
        // 只要该对象被别处共享（旧存档、第三方配方缓存等），就会连带抹掉其它槽位。
        // 这里只接管一份拷贝，再显式把锅格置空。
        ItemStack moved = stored.copy();
        ingredients.setItem(slot, ItemStack.EMPTY);
        resetSlot(slot, ItemStack.EMPTY);

        int total = moved.getCount();
        ItemStack remainder = moved.copy();
        player.getInventory().add(remainder);
        int delivered = total - remainder.getCount();
        if (!remainder.isEmpty()) {
            ItemEntity dropped = new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.15, worldPosition.getZ() + 0.5,
                remainder.copy(), 0.0, 0.16, 0.0);
            dropped.setDefaultPickUpDelay();
            dropped.setThrower(player);
            if (level.addFreshEntity(dropped)) {
                delivered += remainder.getCount();
            } else {
                ingredients.setItem(slot, remainder.copy());
                tracked[slot] = remainder.copy();
                cooked[slot] = true;
                doneness[slot] = HotPotCooking.FULL_DONE;
                xpValue[slot] = slotXp;
                cookDur[slot] = HotPotCooking.cookTicks(level, remainder);
            }
        }
        if (delivered > 0) {
            HotPotCookingRun.grantExperience(this, slotXp, delivered);
        }
        return delivered;
    }

    private void finishCollection() {
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP,
            SoundSource.BLOCKS, 0.35F, 1.2F);
        markDirtyAndSync();
    }

    /** 取出成品时结算经验，经验球在锅的位置生成。逻辑在 {@link HotPotCookingRun}。 */
    public void grantTakeXp(Player player, int slot, int count) {
        HotPotCookingRun.grantTakeXp(this, player, slot, count);
    }

    // ------------------------------------------------------------------ 槽位管理

    void trackSlotChanges() {
        if (!slotsTouched) {
            return;
        }
        slotsTouched = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getItem(i);
            ItemStack snapshot = tracked[i];
            if (snapshot == null || !ItemStack.isSameItemSameComponents(snapshot, stack)) {
                resetSlot(i, stack);
                syncPending = true;
            }
        }
    }

    /** 读档时 level 还没设好，查不到配方，烹饪时间只能用兜底值：第一次服务端 tick 时按配方重算（代码检查 B3）。 */
    void recomputeCookTimes() {
        cookRevision = HotPotCooking.cookRevision();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getItem(i);
            if (!stack.isEmpty() && !cooked[i]) {
                cookDur[i] = HotPotCooking.cookTicks(level, stack);
            }
        }
    }

    @Override
    public void setChanged() {
        slotsTouched = true;
        super.setChanged();
    }

    private void resetSlot(int slot, ItemStack stack) {
        tracked[slot] = stack.copy();
        doneness[slot] = 0;
        cooked[slot] = false;
        overcook[slot] = 0;
        xpValue[slot] = 0F;
        cookDur[slot] = stack.isEmpty() ? 0 : HotPotCooking.cookTicks(level, stack);
    }

    public void onSlotStackChanged(int slot) {
        if (slot >= 0 && slot < SLOTS) {
            resetSlot(slot, ingredients.getItem(slot));
            markDirtyAndSync();
        }
    }

    /** 成品被取空后立即重置该槽，允许下一次放入食材。 */
    public void onSlotEmptied(int slot) {
        if (slot >= 0 && slot < SLOTS && ingredients.getItem(slot).isEmpty()) {
            resetSlot(slot, ItemStack.EMPTY);
            markDirtyAndSync();
        }
    }

    // ------------------------------------------------------------------ 查询

    public int base() {
        return HotPotSoup.base(this);
    }

    public SoupVisual soupVisual() {
        return HotPotSoup.visual(this);
    }

    public float clientHeat() {
        return clientHeat;
    }

    /** 世界气泡用的累计时间（秒），不会因热度变化而跳相位。 */
    public double bubbleSeconds() {
        return bubbleSeconds;
    }

    /** 锅里有没有汤（鸳鸯 / 田字锅任一边有就算）。逻辑在 {@link HotPotSoup}。 */
    public boolean hasSoup() {
        return HotPotSoup.has(this);
    }

    /** 这口锅怎么分格（没有锅时按九宫格）。 */
    public PotLayout layout() {
        HotPotMaterial material = potMaterial();
        return material == null ? PotLayout.NINE_GRID : material.layout();
    }

    /** 某一边的汤底编号（-1 = 没汤）。逻辑在 {@link HotPotSoup}。 */
    public int baseOfSide(int side) {
        return HotPotSoup.baseOfSide(this, side);
    }

    private void setBaseOfSide(int side, int id) {
        HotPotSoup.setBaseOfSide(this, side, id);
    }

    /** 这一格泡在哪种汤里（不用的格子 / 那边没汤 = null）。 */
    public SoupBase soupForSlot(int slot) {
        return HotPotSoup.forSlot(this, slot);
    }

    private int bowlSoup(int bigCell) {
        return HotPotSoup.bowlSoup(this, bigCell);
    }

    private SoupVisual blockSoupVisual() {
        return HotPotSoup.blockVisual(this);
    }

    public int filledSlots() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!ingredients.getItem(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public int firstEmptySlot() {
        PotLayout layout = layout();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ingredients.getItem(slot).isEmpty() && layout.sideOfSlot(slot) != PotLayout.NONE) {
                return slot;
            }
        }
        return -1;
    }

    /** 锅里是否还有没煮熟的食材，供灶台判断要不要继续添燃料。 */
    public boolean hasRawIngredient() {
        for (int i = 0; i < SLOTS; i++) {
            if (!cooked[i] && !ingredients.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 整盘倒菜用：每一样随机挑一个还有空位的大格（九宫格之一），放进那格的第一个空小格，
     * 所以每个大格分到几样是随机的（r97，用户要求）。锅满了返回 -1。
     */
    private int randomEmptySlot() {
        int[] open = new int[9];
        int n = 0;
        for (int cell = 0; cell < 9; cell++) {
            if (firstEmptySlotInCell(cell) >= 0) {
                open[n++] = cell;
            }
        }
        return n == 0 ? -1 : firstEmptySlotInCell(open[level.random.nextInt(n)]);
    }

    public int firstEmptySlotInCell(int bigCell) {
        return firstSlotInCell(bigCell, slot -> ingredients.getItem(slot).isEmpty());
    }

    public int firstCookedSlotInCell(int bigCell) {
        return firstSlotInCell(bigCell, slot -> cooked[slot] && !ingredients.getItem(slot).isEmpty());
    }

    private int firstSlotInCell(int bigCell, java.util.function.IntPredicate matches) {
        PotLayout layout = layout();
        if (bigCell < 0 || bigCell > 8 || !layout.usesCell(bigCell)) {
            return -1;
        }
        if (layout.multiSoup()) {
            // 鸳鸯 / 田字锅：锅里这个位置轮流显示的是归到它的那些小格（PotLayout.spotOfSlot）
            for (int slot = 0; slot < SLOTS; slot++) {
                if (layout.spotOfSlot(slot) == bigCell && matches.test(slot)) {
                    return slot;
                }
            }
            return -1;
        }
        int baseRow = (bigCell / 3) * 3;
        int baseCol = (bigCell % 3) * 3;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = (baseRow + row) * 9 + baseCol + col;
                if (matches.test(slot)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    public int propertyAt(int index) {
        if (index == 0) {
            return bases[0];
        }
        if (index >= PROP_EXTRA_BASES && index < PROP_COUNT) {
            return bases[index - PROP_EXTRA_BASES + 1];
        }
        if (index >= 4 && index < 4 + SLOTS) {
            return doneness[index - 4];
        }
        int cookedStart = 4 + SLOTS;
        if (index >= cookedStart && index < PROP_COUNT) {
            int word = index - cookedStart;
            int bits = 0;
            for (int b = 0; b < COOKED_WORD_BITS; b++) {
                int slot = word * COOKED_WORD_BITS + b;
                if (slot < SLOTS && cooked[slot]) {
                    bits |= 1 << b;
                }
            }
            return bits;
        }
        StoveBlockEntity stove = stoveBelow();
        if (stove == null && heatSourceBelow()) {
            // 篝火上的锅也在煮：以前界面只看灶台燃料，显示「没有火」、火苗灭着（mryt233 2026-11-01，代码检查 B6）。
            // 明火不耗燃料，火苗和火力条按满的显示（同灶台空烧）。
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
            if (!ingredients.getItem(i).isEmpty()) {
                total++;
            }
        }
        return Math.min(15, total * 15 / SLOTS + (total > 0 ? 1 : 0));
    }

    /** 一边汤底的名字（没汤 = 「无汤底」）。 */
    public static Component soupName(int id) {
        SoupBase soup = SoupBase.byId(id);
        return soup == null ? Component.translatable(key("soup.none")) : Component.translatable(soup.nameKey());
    }

    /**
     * 汤底名字后面带上辣度，比如「红汤锅底（特辣）」。
     * mryt233 改（2026-11-05，r136a）：红汤是按档位加效果的，界面和提示里看得见档位才调得明白。
     */
    public static Component soupNameWithSpicy(int id, int spicy) {
        Component name = soupName(id);
        if (SoupBase.byId(id) != SoupBase.SPICY || spicy < 0) {
            return name;   // 不是红汤，或者调用方不知道辣度（<0）就不带后缀
        }
        // 照任务单的样子写「红汤锅底（特辣）」；这里只包中文全角括号，汤底名和辣度名都还能各自翻译
        return name.copy().append(Component.literal("（")).append(spicyLevelText(spicy))
            .append(Component.literal("）"));
    }

    /**
     * 汤底那一栏：九宫格锅一种；鸳鸯 / 田字锅按边列出来，用「/」隔开。
     * mryt233 改（2026-11-05）：多了辣度版重载 {@link #soupText(PotLayout, java.util.function.IntUnaryOperator, java.util.function.IntUnaryOperator)}，
     * 红汤边会在名字后面带档位；不知道辣度的地方（旧的两参版本）照旧只写汤底名。
     */
    public static Component soupText(PotLayout layout, java.util.function.IntUnaryOperator baseOfSide) {
        return soupText(layout, baseOfSide, side -> -1);
    }

    /** 带辣度档位的汤底那一栏（见 {@link #soupNameWithSpicy}）。 */
    public static Component soupText(PotLayout layout, java.util.function.IntUnaryOperator baseOfSide,
                                     java.util.function.IntUnaryOperator spicyOfSide) {
        net.minecraft.network.chat.MutableComponent text = Component.empty();
        for (int side = 0; side < layout.sides(); side++) {
            if (side > 0) {
                text.append(Component.literal(" / "));
            }
            text.append(soupNameWithSpicy(baseOfSide.applyAsInt(side), spicyOfSide.applyAsInt(side)));
        }
        return text;
    }

    public Component statusText() {
        Component soupText = soupText(layout(), this::baseOfSide, this::spicyOfSide);   // mryt233 改（2026-11-05）：红汤带辣度档位（r136a）
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

    /** 标记本次破坏已经由玩家回调处理，避免 onStateReplaced 重复掉落。 */
    public void markBreakHandled() {
        breakHandled = true;
    }

    /** 消费一次性破坏标记；没有标记时返回 false。 */
    public boolean consumeBreakHandled() {
        boolean handled = breakHandled;
        breakHandled = false;
        return handled;
    }

    /** 掉落所有食材，不改变锅/灶台本体状态。 */
    public void dropContents() {
        dropContents(PotLayout.NONE);
    }

    /** 只掉某一边的食材（{@link PotLayout#NONE} = 全部）。鸳鸯锅舀走一边汤时用。 */
    public void dropContents(int side) {
        if (level == null || level.isClientSide) {
            return;
        }
        PotLayout layout = layout();
        boolean all = side == PotLayout.NONE || !layout.multiSoup();
        for (int i = 0; i < SLOTS; i++) {
            if (!all && layout.sideOfSlot(i) != side) {
                continue;
            }
            ItemStack stack = ingredients.getItem(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack);
                ingredients.setItem(i, ItemStack.EMPTY);
                resetSlot(i, ItemStack.EMPTY);
            }
        }
        markDirtyAndSync();
    }

    // ------------------------------------------------------------------ 整锅打包 / 冷热

    /** 存进锅物品里的字段名，复制方块（Ctrl+中键）时要从方块实体数据里去掉，免得重复。 */
    private static final String[] POT_DATA_KEYS = {"Base", "BaseB", "BaseC", "BaseD",
        "Spicy", "SpicyB", "SpicyC", "SpicyD",   // mryt233 改（2026-11-05）：每边辣度档位也由 pot_contents 打包（r136a）
        "Doneness", "Cooked", "Overcook", "SlotXp", "PendingXp", "Items"};

    /** 锅里有没有东西（汤底或食材）。 */
    public boolean hasPotContents() {
        return hasSoup() || !ingredients.isEmpty();
    }

    /** 把整锅（汤底 + 食材 + 熟度）打包成一个锅物品；空锅就是普通锅物品。 */
    public ItemStack toPotItem(HotPotMaterial material) {
        ItemStack stack = new ItemStack(HotPotContent.itemFor(material));
        if (level != null && hasPotContents()) {
            CompoundTag data = new CompoundTag();
            HotPotStorage.write(this, data, level.registryAccess());
            stack.set(HotPotContent.POT_CONTENTS, CustomData.of(data));
        }
        return stack;
    }

    /** 摆出装着东西的锅时，把物品里打包的汤底和食材原样放回来。 */
    public void loadPotContents(ItemStack stack) {
        CustomData data = stack.get(HotPotContent.POT_CONTENTS);
        if (data == null || level == null || level.isClientSide) {
            return;
        }
        HotPotStorage.read(this, data.copyTag(), level.registryAccess());
        updateSoupVisual();
        markDirtyAndSync();
    }

    /** 清空锅里的数据（已经打包进物品之后调用），不碰方块状态。 */
    public void clearPotContents() {
        ingredients.clearContent();
        for (int i = 0; i < SLOTS; i++) {
            resetSlot(i, ItemStack.EMPTY);
        }
        pendingXp = 0F;
        java.util.Arrays.fill(bases, -1);
        java.util.Arrays.fill(spicyLevels, 0);   // mryt233 改（2026-11-05）：整锅打包后清空，辣度也清掉（r136a）
        setChanged();
    }

    /**
     * 锅现在是不是热的：坐在烧着的灶台上（含保温），或者摆在能加热的方块上。
     * 能加热的方块由 {@link HotPotHeat} 决定（标签 + 联动注册），以后农夫乐事的炉灶从那里接入。
     */
    public boolean isHeated() {
        if (level == null || !hasPot()) {
            return false;
        }
        StoveBlockEntity stove = stoveBelow();
        if (stove != null && stove.burning()) {
            return true;
        }
        return !(this instanceof StoveBlockEntity) && HotPotHeat.isHeatSource(level, worldPosition.below());
    }

    /** 方块被破坏掉落时（战利品表 copy_components），锅里的东西跟着锅物品走。 */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!(this instanceof StoveBlockEntity) && level != null && hasPotContents()) {
            CompoundTag data = new CompoundTag();
            HotPotStorage.write(this, data, level.registryAccess());
            builder.set(HotPotContent.POT_CONTENTS, CustomData.of(data));
        }
    }

    /** 直接摆出独立锅时，原版 BlockItem 会调用这里读物品组件。 */
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        CustomData data = components.get(HotPotContent.POT_CONTENTS);
        if (data != null && level != null && !(this instanceof StoveBlockEntity)) {
            HotPotStorage.read(this, data.copyTag(), level.registryAccess());
            markDirtyAndSync();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag nbt) {
        super.removeComponentsFromTag(nbt);
        for (String key : POT_DATA_KEYS) {
            nbt.remove(key);
        }
    }

    private static void consumeHand(Player player, InteractionHand hand, Item remainder) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getAbilities().instabuild) {
            return;
        }
        if (stack.getCount() > 1) {
            stack.shrink(1);
            if (remainder != net.minecraft.world.item.Items.AIR) {
                ItemStack back = new ItemStack(remainder);
                if (!player.getInventory().add(back)) {
                    player.drop(back, false);
                }
            }
        } else {
            player.setItemInHand(hand,
                remainder == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(remainder));
        }
    }

    public static String key(String name) {
        return "text." + HotPot.MOD_ID + "." + name;
    }

    // ------------------------------------------------------------------ Container 委托

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return ingredients.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ingredients.removeItem(slot, amount);
        if (!result.isEmpty()) {
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ingredients.removeItemNoUpdate(slot);
        if (!result.isEmpty()) {
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        // 面板的每个格子代表一份食材。无论调用方是普通点击、Shift
        // 快速移动还是旧存档恢复，都不允许把多份物品压进同一个物理槽。
        ItemStack single = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        ingredients.setItem(slot, single);
        onSlotStackChanged(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.canInteractWithBlock(worldPosition, 5.0);
    }

    // 漏斗 / 其他模组走 Container 接口时的限制（用户 2026-11-01：漏斗接上，只收能煮的、只抽熟了的）。
    // 以前没有限制：漏斗能往锅里塞泥土这类煮不了的东西、塞进鸳鸯锅不用的格子，也能把生菜抽走。

    /** 一格只放一份：告诉往里塞东西的一方，免得它一次塞一整组、多出来的被 setItem 截掉吞没。 */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** 只收能下锅的生食材，只进锅里在用、空着、还没煮好的格子；灶上没锅不收。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return level != null && slot >= 0 && slot < SLOTS && hasPot()
            && ingredients.getItem(slot).isEmpty() && !cooked[slot]
            && layout().sideOfSlot(slot) != PotLayout.NONE
            && HotPotCooking.acceptable(level, stack);
    }

    /** 只能抽走煮熟的。 */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOTS && cooked[slot];
    }

    @Override
    public void clearContent() {
        ingredients.clearContent();
        for (int i = 0; i < SLOTS; i++) {
            resetSlot(i, ItemStack.EMPTY);
        }
        markDirtyAndSync();
    }

    // ------------------------------------------------------------------ 屏幕工厂

    // ------------------------------------------------------------------ 同步与 NBT

    /** 保存并立即把当前方块实体 NBT 推送给附近客户端。 */
    protected void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            // ServerLevel.sendBlockUpdated 里已经调了 chunkSource.blockChanged，不用再调一次（代码检查 W3）
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        syncPending = false;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        HotPotStorage.write(this, nbt, registries);
    }

    /** 修复：抽出存档模块后仍须保留原版读取入口，供区块加载和客户端同步恢复锅内容。 */
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        HotPotStorage.read(this, nbt, registries);
    }

}
