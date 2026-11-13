package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
import cn.blockforge.orient.framework.compat.PlayerCompat;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
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

/**
 * 火锅库存与烹饪数据。
 *
 * <p>独立摆放的锅使用本类；坐上灶台后的组合方块由
 * {@link StoveBlockEntity} 继承本类。因此两种形态共用完全相同的 81 格
 * 食材、汤底、烹饪进度和经验逻辑。81 格怎么分边、每边一种汤由锅的 {@link PotLayout} 决定
 * （九宫格锅一边；鸳鸯锅两边；田字锅四边，2026-10-23 起）。</p>
 */
public class HotPotBlockEntity extends SyncedBlockEntity
    implements Container {
    /** 9x9 = 81 个食材槽，每 3x3 为一个大格。 */
    public static final int SLOTS = 81;
    /** 屏幕属性包使用 short，熟食位图每组只占 15 位，避免高位被截断。 */
    public static final int COOKED_WORD_BITS = 15;
    public static final int COOKED_WORDS = (SLOTS + COOKED_WORD_BITS - 1) / COOKED_WORD_BITS;
    /** 0汤底 1燃料类型 2余燃 3总燃 4..84熟度 85..90熟食位图 91..93鸳鸯 / 田字锅其余几边的汤底。 */
    public static final int PROP_EXTRA_BASES = 4 + SLOTS + COOKED_WORDS;
    public static final int PROP_COUNT = PROP_EXTRA_BASES + PotLayout.MAX_SIDES - 1;

    // mryt233 改（2026-11-11）：81 格并行状态由同一个部件持有。
    final HotPotSlots slots = new HotPotSlots(this);
    /**
     * 每边一种汤底（空字符串 = 没汤，未知 ID 原样保留）：九宫格锅只用第 0 边；鸳鸯锅 0、1；田字锅 0..3（PotLayout）。
     * 汤底不和格局绑定，任何汤底（包括以后新加的）都能倒进任何一边。调整（原来是单个 base）。
     */
    final String[] bases = {"", "", "", ""};   // mryt233：持久化 ID，不在读档时解析。
    /**
     * mryt233 改（2026-11-05，清单 #116）：每边红汤的辣度档位（0=微辣 / 1=中辣 / 2=特辣 / 3=变态辣）。
     * 只有汤底是红汤（SoupBase.SPICY）的那一边才会用到；其余边保持原值不影响任何结算。
     * 刚倒进红汤默认 1 档（中辣，用户定）；拿辣椒右键升档、拿冰降档，见 HotPotBlock / StoveBlock。
     */
    final int[] spicyLevels = {1, 1, 1, 1};
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
    /** 这一刻里有改动要同步给客户端：一刻结束时只发一次，不再每熟一样菜就发一次整锅（代码检查 W3）。 */
    boolean syncPending = false;

    /** 独立火锅的方块实体构造器。 */
    public HotPotBlockEntity(BlockPos blockPos, BlockState state) {
        this(HotPotContent.HOT_POT_BE, blockPos, state);
    }

    /** 供组合灶台子类使用的构造器。 */
    protected HotPotBlockEntity(BlockEntityType<?> type, BlockPos blockPos, BlockState state) {
        super(type, blockPos, state);
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
        return baseOfSide(side) != SoupIds.EMPTY && clientHeat < 0.5F ? BOB_COLD_OMEGA : BOB_OMEGA;
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

    /** 供 HotPotStorage：读完存档后标记需要修复方块状态。 */
    void markNeedsVisualRepair() {
        needsVisualRepair = true;
    }

    /** 组合灶台的锅状态必须与库存实体保持一致，避免旧存档出现“有库存但无锅”状态。 */
    public void repairVisualState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.getValue(StoveBlock.POT);
            if (!pot.hasPot()) {
                // 未知 ID 必须保留，避免视觉修复写掉未来版本的数据。
                for (String id : bases) if (SoupIds.index(id) == SoupIds.UNKNOWN) return;
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
        HotPotSoupOps.clearSoupState(this);
    }

    /** 让锅的外观状态与汤底种类保持一致。 */
    protected void updateSoupVisual() {
        HotPotSoupOps.updateSoupVisual(this);
    }

    void updateStandaloneBoilingVisual(boolean boiling) {
        HotPotEffects.updateStandaloneBoiling(this, boiling);
    }

    // ------------------------------------------------------------------ 交互

    /** 筷子任何时候都能打开当前锅的面板。 */
    public void useWithChopsticks(Player player) {
        HotPotSoupOps.useWithChopsticks(this, player);
    }

    /**
     * 手里的东西能不能把汤底舀走：只认空桶（r99 起碗是盛汤 / 盛熟菜，见 serveWithBowl）。
     * 舀出水 / 牛奶 / 岩浆还回原来的桶；菌汤桶装不了，就是倒掉（用户定的）。
     * 每边一种汤（九宫格锅一边；鸳鸯 / 田字锅按准星那边，见 scoopSoup），食材可以先放，汤底只决定那边煮不煮。
     */
    public boolean canScoopWith(ItemStack stack) {
        return HotPotSoupOps.canScoopWith(this, stack);
    }

    /**
     * 用空容器把汤底舀出来：倒进去的是什么就还什么（水桶、牛奶桶、岩浆桶、蘑菇煲）。
     * 锅里还有食物的话，先全部掉到地上，再清空汤底。
     */
    public boolean scoopSoup(Player player, InteractionHand hand) {
        return HotPotSoupOps.scoopSoup(this, player, hand);
    }

    /** 鸳鸯锅只舀准星那一边的汤（那边的食材掉出来），另一边不动。 */
    public boolean scoopSoup(Player player, InteractionHand hand, int side) {
        return HotPotSoupOps.scoopSoup(this, player, hand, side);
    }

    /**
     * 拿空碗点锅（r99）：锅里有熟菜就盛进碗里（先盛准星对着那一大格的，最多 3 样，碗变成装着菜的碗）；
     * 没熟菜但有汤，就盛一碗汤（锅里的汤不会少，用户定的「盛不完」）。
     */
    public boolean serveWithBowl(Player player, InteractionHand hand, int bigCell) {
        return HotPotSoupOps.serveWithBowl(this, player, hand, bigCell);
    }

    public boolean pourSoup(Player player, InteractionHand hand) {
        return HotPotSoupOps.pourSoup(this, player, hand);
    }

    /** 鸳鸯锅倒进准星那一边；两边可以是任意两种汤底（也可以一样），以后加的新汤底也一样能倒。 */
    public boolean pourSoup(Player player, InteractionHand hand, int side) {
        return HotPotSoupOps.pourSoup(this, player, hand, side);
    }

    // ------------------------------------------------------------------ 红汤辣度（r136a，mryt233 2026-11-05）

    /** 辣度最高档：3 = 3 级特辣。 */
    public static final int SPICY_MAX = HotPotSpice.SPICY_MAX;

    /** 某一边的辣度档位（0 = 微辣）；不用 / 越界的边按 0 算。 */
    public int spicyOfSide(int side) {
        return HotPotSpice.spicyOfSide(this, side);
    }

    /** 把某一边的辣度档位夹到 0..3 后写进去（客户端只读快照，不调用）。 */
    public void setSpicyOfSide(int side, int level) {
        HotPotSpice.setSpicyOfSide(this, side, level);
    }

    /** 这档辣度的名字（0 微辣、1 中辣、2 特辣、3 变态辣），语言键由语言文件提供。 */
    public static Component spicyLevelText(int level) {
        return HotPotSpice.spicyLevelText(level);
    }

    /**
     * 手里这件是不是「调辣度的东西」——辣椒加辣，冰块 / 浮冰 / 蓝冰降辣。
     * HotPotBlock / StoveBlock 用它决定要不要先试调辣度，再考虑下锅当食材（清单 #116）。
     */
    public static boolean isSpicyModifier(ItemStack stack) {
        return HotPotSpice.isSpicyModifier(stack);
    }

    /**
     * 拿着辣椒 / 冰块点红汤锅：辣椒加 1 档、冰块降 1~3 档（清单 #116）。
     * 只对红汤边有效；别的汤底上拿着这两样会提示「只有红汤锅底才能调整辣度」并吃掉这次点击。
     * 返回 true = 这次点击已经处理掉了（调用方直接算成功，不要再当食材下锅）。
     */
    public boolean adjustSpiciness(Player player, InteractionHand hand, int side) {
        return HotPotSpice.adjustSpiciness(this, player, hand, side);
    }

    /** 加辣 / 降辣时锅面上的那几颗粒子：加辣火苗 + 岩浆，降辣白云 + 水花。 */
    void spawnSpicyParticles(boolean up) {
        HotPotSpice.spawnSpicyParticles(this, up);
    }

    /**
     * 用碗盛的这一碗汤实际来自哪一边：准星那格那一边有这种汤就取那边，没有就取第一处有这种汤的边。
     * （{@link HotPotSoup#bowlSoup} 的逻辑，这里要的是「哪一边」而不是「哪种汤」。）
     */
    int soupSideOf(int bigCell, int soupId) {
        return HotPotSoupOps.soupSideOf(this, bigCell, soupId);
    }

    /** 这一格所在那一边的辣度（九宫格锅就是唯一那一边）。 */
    public int spicyOfSlot(int slot) {
        return HotPotSpice.spicyOfSlot(this, slot);
    }

    /** 筷子上夹着的食物放进锅里指定的小格（熟食、汤底原料放不进）。 */
    public boolean insertHeld(Player player, ItemStack chopsticks, int slot) {
        return HotPotFoodOps.insertHeld(this, player, chopsticks, slot);
    }

    /**
     * 拿着装了菜的盘子点锅：能下锅的生食材随机倒进九个大格的空位，熟食和放不下的留在盘子里。
     * 一叠盘子只倒最上面那一个。
     */
    public boolean pourPlate(Player player, InteractionHand hand) {
        return HotPotFoodOps.pourPlate(this, player, hand);
    }

    public boolean insertIngredient(Player player, InteractionHand hand) {
        return HotPotFoodOps.insertIngredient(this, player, hand);
    }

    /** 将一份食材放入准星对应槽位；占用时不挪动旧物品。 */
    public boolean insertIngredient(Player player, InteractionHand hand, int slot) {
        return HotPotFoodOps.insertIngredient(this, player, hand, slot);
    }

    /**
     * 服务端原子取件：先从熟食槽移出整份，再尝试放入背包，余量安全掉在锅边。
     * 只有实际移出的数量会结算经验，调用者重复发包也不会复制物品。
     */
    public boolean collectCookedSlot(Player player, int slot) {
        return HotPotFoodOps.collectCookedSlot(this, player, slot);
    }

    /**
     * 空筷子直接点锅（2026-10-25 用户定，开发计划「筷子新交互」；新增）：从这一格夹一样熟食到筷子上，
     * 照常结算出锅经验。夹着的菜之后按住右键吃、或放进碗盘 / 另一格，见 ChopsticksItem。
     */
    public boolean pickWithChopsticks(Player player, ItemStack chopsticks, int slot) {
        return HotPotFoodOps.pickWithChopsticks(this, player, chopsticks, slot);
    }

    /**
     * 空筷子直接右键锅：夹客户端报来的「这一刻浮在这格上的那样」（ChopsticksAimPayload）；它还生着就提示、不夹。
     * 没报（或报的不在这格）就夹这格第一样熟的。返回 false = 这格没有能夹的熟食。新增（2026-10-25）。
     */
    public boolean pickShown(Player player, ItemStack chopsticks, int bigCell) {
        return HotPotFoodOps.pickShown(this, player, chopsticks, bigCell);
    }

    /** 客户端报来的「这一刻浮在这格上的那一小格」（ChopsticksAimPayload）：在这格里、有东西才算，否则 -1。用一次就清掉。 */
    int shownIn(Player player, int bigCell) {
        return HotPotFoodOps.shownIn(this, player, bigCell);
    }

    /**
     * 空手右键锅捞熟食：捞的是这一刻浮在这格上、你看到的那一样（用户 2026-10-26：和筷子一样）；它还生着就提示、不捞。
     * 没报就捞这格第一样熟的。返回 false = 这格没有能捞的熟食。新增
     */
    public boolean collectShown(Player player, int bigCell) {
        return HotPotFoodOps.collectShown(this, player, bigCell);
    }

    int transferCookedSlot(Player player, int slot) {
        return HotPotFoodOps.transferCookedSlot(this, player, slot);
    }

    void finishCollection() {
        HotPotFoodOps.finishCollection(this);
    }

    /** 取出成品时结算经验，经验球在锅的位置生成。逻辑在 {@link HotPotCookingRun}。 */
    public void grantTakeXp(Player player, int slot, int count) {
        HotPotFoodOps.grantTakeXp(this, player, slot, count);
    }

    // ------------------------------------------------------------------ 槽位管理

    void trackSlotChanges() {
        slots.trackSlotChanges();
    }

    /** 读档时 level 还没设好，查不到配方，烹饪时间只能用兜底值：第一次服务端 tick 时按配方重算（代码检查 B3）。 */
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

    /** 成品被取空后立即重置该槽，允许下一次放入食材。 */
    public void onSlotEmptied(int slot) {
        slots.onSlotEmptied(slot);
    }

    // ------------------------------------------------------------------ 查询

    public int base() {
        return HotPotSoup.base(this);
    }

    public SoupVisual soupVisual() {
        return HotPotSoupOps.soupVisual(this);
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
        return HotPotSoupOps.baseOfSide(this, side);
    }

    void setBaseOfSide(int side, int id) {
        HotPotSoupOps.setBaseOfSide(this, side, id);
    }

    /** 这一格泡在哪种汤里（不用的格子 / 那边没汤 = null）。 */
    public SoupBase soupForSlot(int slot) {   // mryt233 改：保留旧公开签名，仅内置汤底有枚举值
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

    /** 锅里是否还有没煮熟的食材，供灶台判断要不要继续添燃料。 */
    public boolean hasRawIngredient() {
        return slots.hasRawIngredient();
    }

    /**
     * 整盘倒菜用：每一样随机挑一个还有空位的大格（九宫格之一），放进那格的第一个空小格，
     * 所以每个大格分到几样是随机的（r97，用户要求）。锅满了返回 -1。
     */
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
            if (!slots.ingredients().getItem(i).isEmpty()) {
                total++;
            }
        }
        return Math.min(15, total * 15 / SLOTS + (total > 0 ? 1 : 0));
    }

    /** 一边汤底的名字（没汤 = 「无汤底」）。 */
    public static Component soupName(int id) {
        SoupDef soup = Soups.byIndex(id);
        return soup == null ? Component.translatable(key(id == SoupIds.UNKNOWN ? "soup.unknown" : "soup.none")) : Component.translatable(soup.nameKey());
    }

    /**
     * 汤底名字后面带上辣度，比如「红汤锅底（特辣）」。
     * mryt233 改（2026-11-05，r136a）：红汤是按档位加效果的，界面和提示里看得见档位才调得明白。
     */
    public static Component soupNameWithSpicy(int id, int spicy) {
        Component name = soupName(id);
        // mryt233 改（2026-11-10）：红汤判定改走 isSpicy() 属性
        SoupDef base = Soups.byIndex(id);
        if (base == null || !base.spicy() || spicy < 0) {
            return name;   // 不是红汤，或者调用方不知道辣度（<0）就不带后缀
        }
        // 照任务单的样子写「红汤锅底（特辣）」；这里只包中文全角括号，汤底名和辣度名都还能各自翻译
        return name.copy().append(Component.literal("（")).append(spicyLevelText(spicy))
            .append(Component.literal("）"));
    }

    /**
     * 汤底那一栏：九宫格锅一种；鸳鸯 / 田字锅按边列出来，用「/」隔开。
     * mryt233 改（2026-11-05）：多了辣度版重载 {@link #soupText(PotLayout, IntUnaryOperator, IntUnaryOperator)}，
     * 红汤边会在名字后面带档位；不知道辣度的地方（旧的两参版本）照旧只写汤底名。
     */
    public static Component soupText(PotLayout layout, IntUnaryOperator baseOfSide) {
        return soupText(layout, baseOfSide, side -> -1);
    }

    /** 带辣度档位的汤底那一栏（见 {@link #soupNameWithSpicy}）。 */
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
            ItemStack stack = slots.ingredients().getItem(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack);
                slots.ingredients().setItem(i, ItemStack.EMPTY);
                resetSlot(i, ItemStack.EMPTY);
            }
        }
        markDirtyAndSync();
    }

    // ------------------------------------------------------------------ 整锅打包 / 冷热

    /** 锅里有没有东西（汤底或食材）。 */
    public boolean hasPotContents() {
        return HotPotPack.hasPotContents(this);
    }

    /** 把整锅（汤底 + 食材 + 熟度）打包成一个锅物品；空锅就是普通锅物品。 */
    public ItemStack toPotItem(HotPotMaterial material) {
        return HotPotPack.toPotItem(this, material);
    }

    /** 摆出装着东西的锅时，把物品里打包的汤底和食材原样放回来。 */
    public void loadPotContents(ItemStack stack) {
        HotPotPack.loadPotContents(this, stack);
    }

    /** 清空锅里的数据（已经打包进物品之后调用），不碰方块状态。 */
    public void clearPotContents() {
        HotPotPack.clearPotContents(this);
    }

    /**
     * 锅现在是不是热的：坐在烧着的灶台上（含保温），或者摆在能加热的方块上。
     * 能加热的方块由 {@link HotPotHeat} 决定（标签 + 联动注册），以后农夫乐事的炉灶从那里接入。
     */
    public boolean isHeated() {
        return HotPotPack.isHeated(this);
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

    // ------------------------------------------------------------------ Container 委托

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
        return !isRemoved() && PlayerCompat.canInteractWithBlock(player, worldPosition, 5.0);
    }

    // 漏斗 / 其他模组走 Container 接口时的限制（用户 2026-11-01：漏斗接上，只收能煮的、只抽熟了的）。
    // 以前没有限制：漏斗能往锅里塞泥土这类煮不了的东西、塞进鸳鸯锅不用的格子，也能把生菜抽走。

    /** 一格只放一份：告诉往里塞东西的一方，免得它一次塞一整组、多出来的被 setItem 截掉吞没。 */
    @Override
    public int getMaxStackSize() {
        return slots.getMaxStackSize();
    }

    /** 只收能下锅的生食材，只进锅里在用、空着、还没煮好的格子；灶上没锅不收。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slots.canPlaceItem(slot, stack);
    }

    /** 只能抽走煮熟的。 */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slots.canTakeItem(target, slot, stack);
    }

    @Override
    public void clearContent() {
        slots.clearContent();
    }

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

    /** 保留公开入口，实际更新格式由公共基类提供。 */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        HotPotStorage.write(this, nbt);
    }

    /** 修复：抽出存档模块后仍须保留原版读取入口，供区块加载和客户端同步恢复锅内容。 */
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        HotPotStorage.read(this, nbt);
    }

    /** 清理锅存档标签中的新旧汤底与食材键名（供物品打包或测试调用）。 */
    public void removeComponentsFromTag(CompoundTag nbt) {
        HotPotPack.removeComponentsFromTag(nbt, HotPotStorage.STORAGE_KEYS);
    }
}
