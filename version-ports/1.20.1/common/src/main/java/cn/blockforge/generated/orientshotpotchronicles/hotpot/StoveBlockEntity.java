package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名与结构不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * 组合灶台的方块实体。
 *
 * <p>它继承火锅方块实体，因此燃料、锅内 81 格、汤底和烹饪数据都在同一个
 * 方块实体中保存；灶台未放锅时只运行燃料部分。</p>
 *
 * <p>燃料采用熔炉式交互：右键灶台底座打开灶膛界面，把煤、木柴等原版熔炉燃料
 * 放进专用槽位。可烧的物品与各自的燃烧时长完全取自原版熔炉数据，只有确实需要
 * 加热时才消耗下一份燃料，不会空烧。</p>
 */
public class StoveBlockEntity extends HotPotBlockEntity {
    /** 灶膛只留一个燃料槽，和熔炉的燃料槽一样。 */
    public static final int FUEL_SLOTS = 1;

    private final SimpleContainer fuel = new SimpleContainer(FUEL_SLOTS);
    private int burnTicks = 0;
    private int burnTotal = 0;
    private int fuelType = -1;
    /** 空烧：锅里有汤、灶膛里有燃料，但没有生食材要煮——火点着、汤沸腾，燃料一点不耗。 */
    private boolean idleFlame = false;
    private boolean attachedPotDropped = false;

    public StoveBlockEntity(BlockPos worldPosition, BlockState state) {
        super(HotPotContent.STOVE_BE, worldPosition, state);
        // 界面里放 / 取燃料只改这个小容器，以前不会让灶台存档：灶上没锅或没汤时燃料不消耗，
        // 重启后燃料可能消失（mryt233 2026-11-01，代码检查 B1）。照蘸料台的做法挂监听。
        fuel.addListener(c -> setChanged());
    }

    // ------------------------------------------------------------------ tick

    /**
     * 先消耗灶火，再让父类推进锅内烹饪。
     * mryt233 改（2026-11-07）：块状态取一次、锅取一次、81 格只扫一次。
     * 以前同一条「锅里还有没有生食材」每刻被问三遍（soupIdle / wantsHeat / soupIdle），
     * 每次都要 heatedPot() 各查一次方块实体并完整扫 81 格。
     */
    @Override
    protected void serverTick(Level world) {
        boolean changed = false;
        BlockState state = world.getBlockState(worldPosition);
        // r83 放下的高灶台还没有「上半截」，补上（每秒查一次）；矮灶台 placeTop 自己会直接返回，这里先判掉省一次调用
        if (world.getGameTime() % 20L == 0L && state.getBlock() instanceof StoveBlock stove && stove.lift() > 0) {
            stove.placeTop(world, worldPosition);
        }
        HotPotBlockEntity pot = heatedPot();
        boolean potHasSoup = pot != null && pot.hasSoup();
        boolean potHasRaw = potHasSoup && pot.hasRawIngredient();
        boolean soupIdle = potHasSoup && !potHasRaw;
        // 只有真在煮生食材时才耗燃料。锅里有汤但没有生食材：正在烧的这份燃料暂停不减，灶火和沸腾照常
        if (burnTicks > 0 && !soupIdle) {
            burnTicks--;
            if (burnTicks == 0) {
                fuelType = -1;
                burnTotal = 0;
                changed = true;
            }
        }
        // 像熔炉一样：只有锅里确实还有要煮的东西时才烧掉下一份燃料。
        if (burnTicks <= 0 && potHasRaw && consumeFuel()) {
            changed = true;
        }
        // 灶膛有燃料、锅里有汤：哪怕一份燃料都没点，也直接点火沸腾，不消耗燃料
        boolean idle = burnTicks <= 0 && soupIdle && HotPotFuel.burnTicksOf(fuel.getItem(0)) > 0;
        if (idle != idleFlame) {
            idleFlame = idle;
            changed = true;
        }
        boolean lit = burnTicks > 0 || idleFlame;
        if (StoveBlock.isStove(state) && state.getValue(StoveBlock.LIT) != lit) {
            world.setBlock(worldPosition, state.setValue(StoveBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
        if (changed) {
            markDirtyAndSync();
        } else if (burnTicks > 0 && world.getGameTime() % 20L == 0L) {
            setChanged();
        }
        int warmthDuration = Config.warmthDuration();
        if (lit && warmthDuration > 0 && world.getGameTime() % Math.max(1, Config.warmthIntervalTicks()) == 0L && pot != null) {
            giveWarmth(world, worldPosition, warmthDuration);
        }
        super.serverTick(world);
    }

    /** 围锅回血：默认每 4 秒给范围内玩家续一次生命恢复 I，做法同原版信标（环境效果、粒子很淡）；mryt233 改：参数读取 Config.warmth*()。 */
    static void giveWarmth(Level world, BlockPos worldPosition) {
        giveWarmth(world, worldPosition, Config.warmthDuration());
    }

    /** 给 pos 周围的玩家续生命恢复；duration <= 0 时直接跳过扫描与加效果。 */
    static void giveWarmth(Level world, BlockPos worldPosition, int duration) {
        if (duration <= 0) {
            return;
        }
        int range = Math.max(1, Config.warmthRange());
        int amplifier = Math.max(0, Config.warmthAmplifier());
        AABB area = new AABB(worldPosition).inflate(range);
        for (Player player : world.getEntitiesOfClass(Player.class, area)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, true, true));
        }
    }

    /** 被这台灶台加热的锅：坐在灶里的锅就是自己，否则是正上方的旧式独立锅。 */
    @Nullable
    private HotPotBlockEntity heatedPot() {
        if (hasPot()) {
            return this;
        }
        if (level != null && level.getBlockEntity(worldPosition.above()) instanceof HotPotBlockEntity pot) {
            return pot;
        }
        return null;
    }

    /** 锅里有汤但没有要煮的生食材（空汤或全是熟的）：火可以烧着、汤可以沸腾，但不耗燃料。 */
    private boolean soupIdle() {
        HotPotBlockEntity pot = heatedPot();
        return pot != null && pot.hasSoup() && !pot.hasRawIngredient();
    }

    /**
     * 从灶膛里取出下一份燃料点燃。燃烧时长直接取该物品的原版熔炉数值；
     * 桶类燃料烧完后退回空桶，和熔炉一致。
     */
    private boolean consumeFuel() {
        if (level == null) {
            return false;
        }
        ItemStack stack = fuel.getItem(0);
        int ticks = HotPotFuel.burnTicksOf(stack);
        if (ticks <= 0) {
            return false;
        }
        Item remainderItem = stack.getItem().getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            fuel.setItem(0, remainderItem == null || remainderItem == Items.AIR
                ? ItemStack.EMPTY : new ItemStack(remainderItem));
        }
        burnTicks = ticks;
        burnTotal = ticks;
        HotPotFuel grade = HotPotFuel.gradeForTicks(ticks);
        fuelType = grade == null ? -1 : grade.ordinal();
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE,
            SoundSource.BLOCKS, 0.7F, 0.8F);
        return true;
    }

    // ------------------------------------------------------------------ 锅状态

    /**
     * 将锅材质写入灶台自身的 POT 状态，不创建第二个方块。
     * mryt233 改（2026-11-07）：返回是否真的装上。调用方要据此决定要不要读锅内容、扣锅物品；
     * 以前是 void，装不上也照扣，等于把玩家一整锅东西吞掉。
     */
    public boolean attachPot(HotPotMaterial material, Direction.Axis handleAxis) {
        if (level == null || level.isClientSide || material == null) {
            return false;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!StoveBlock.isStove(state) || state.getValue(StoveBlock.POT).hasPot()) {
            return false;
        }
        level.setBlock(worldPosition, state.setValue(StoveBlock.POT, StovePot.of(material))
            .setValue(StoveBlock.SOUP, SoupVisual.NONE)
            .setValue(StoveBlock.POT_AXIS, handleAxis), Block.UPDATE_CLIENTS);
        markDirtyAndSync();
        return true;
    }

    /**
     * 潜行右键拆锅：整锅（汤底 + 食材 + 熟度）打包成锅物品给玩家，灶台恢复为空灶。
     */
    public void detachPot(Player player) {
        if (level == null || level.isClientSide || !hasPot()) {
            return;
        }
        HotPotMaterial material = potMaterial();
        if (material == null) {
            return;
        }
        ItemStack pot = toPotItem(material);
        HotPotCriterion.fire(player, "carry_pot");
        removePot();
        if (!player.getInventory().add(pot)) {
            player.drop(pot, false);
        }
        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND,
            SoundSource.BLOCKS, 0.25F, 0.75F);
        markDirtyAndSync();
    }

    /** 锅从灶上拿走后：清空锅数据，灶台变回空灶。 */
    private void removePot() {
        clearPotContents();
        attachedPotDropped = false;
        BlockState state = level.getBlockState(worldPosition);
        level.setBlock(worldPosition, state.setValue(StoveBlock.POT, StovePot.NONE)
            .setValue(StoveBlock.SOUP, SoupVisual.NONE), Block.UPDATE_CLIENTS);
        markDirtyAndSync();
    }

    /**
     * 对着锅挖：只挖下锅，灶台保留。效果照原版挖方块：锅的碎屑和破坏声、工具耐久、统计；
     * 能挖下来（石镐以上）就整锅打包掉落，挖不下来就只撒出食材。创造模式直接拿掉。
     */
    public void breakPot(Player player) {
        if (level == null || level.isClientSide || !hasPot()) {
            return;
        }
        HotPotMaterial material = potMaterial();
        if (material == null) {
            return;
        }
        HotPotBlock potBlock = HotPotContent.blockFor(material);
        BlockState potState = potBlock.defaultBlockState();
        level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, worldPosition, Block.getId(potState));
        if (!player.isCreative()) {
            if (player.hasCorrectToolForDrops(potState)) {
                Block.popResource(level, worldPosition, toPotItem(material));
            } else {
                dropContents();
            }
            player.getMainHandItem().mineBlock(level, potState, worldPosition, player);
            player.awardStat(Stats.BLOCK_MINED.get(potBlock));
            player.causeFoodExhaustion(0.005F);
        }
        removePot();
    }

    /**
     * 破坏灶台时掉落附着的锅（连汤带食材打包）。用标记防止玩家破坏回调和状态替换回调重复掉落。
     */
    public void dropAttachedPot(HotPotMaterial material) {
        if (attachedPotDropped || level == null || level.isClientSide) {
            return;
        }
        attachedPotDropped = true;
        if (material != null) {
            Block.popResource(level, worldPosition, toPotItem(material));
            clearPotContents();
        }
    }

    // ------------------------------------------------------------------ 灶膛交互

    /** 灶膛燃料槽，供灶台界面读取与操作。 */
    public Container fuelInventory() {
        return fuel;
    }

    /** 打开灶膛界面；只有服务端会真正发打开屏幕的包。 */
    public void openFuelScreen(Player player) {
        if (level == null || level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        HotpotScreens.openFuel(serverPlayer, this);
    }

    /** 破坏灶台时把灶膛里剩下的燃料原样掉出来。 */
    public void dropFuel() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack stack = fuel.getItem(0);
        if (!stack.isEmpty()) {
            Block.popResource(level, worldPosition, stack);
            fuel.setItem(0, ItemStack.EMPTY);
            markDirtyAndSync();
        }
    }

    // ------------------------------------------------------------------ 状态查询

    public boolean burning() {
        return burnTicks > 0 || idleFlame;
    }

    /** 正在空烧（有汤、有燃料、没生食材，不耗燃料）。 */
    public boolean idle() {
        return idleFlame && burnTicks <= 0;
    }

    /** 给界面看的燃烧刻数：空烧时没有在耗的那份燃料，火苗显示成满的。 */
    public int displayBurnTicks() {
        return idleFlame && burnTicks <= 0 ? 1 : burnTicks;
    }

    public int displayBurnTotal() {
        return idleFlame && burnTicks <= 0 ? 1 : burnTotal;
    }

    public int burnTicks() {
        return burnTicks;
    }

    public int burnTotal() {
        return burnTotal;
    }

    public int fuelType() {
        return fuelType;
    }

    public float power() {
        HotPotFuel fuel = HotPotFuel.byId(fuelType);
        return fuel == null ? 0F : fuel.power();
    }

    public int fuelComparatorLevel() {
        if (burnTotal <= 0) {
            return 0;
        }
        return (int) Math.min(15, Math.ceil(15.0 * burnTicks / burnTotal));
    }

    // ------------------------------------------------------------------ NBT

    // mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名、结构与兜底值不变。
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        BeIo.write(nbt, w -> {
            w.putInt("BurnTicks", burnTicks);
            w.putInt("BurnTotal", burnTotal);
            w.putInt("FuelType", fuelType);
            w.putBoolean("IdleFlame", idleFlame);
            // 灶膛燃料单独存进 "Fuel" 子标签，避开父类 81 格用的 "Items" 键。
            NonNullList<ItemStack> list = NonNullList.withSize(FUEL_SLOTS, ItemStack.EMPTY);
            list.set(0, fuel.getItem(0));
            w.child("Fuel").putItems(list);
        });
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        BeIo.read(nbt, r -> {
            burnTicks = r.getInt("BurnTicks", 0);
            burnTotal = r.getInt("BurnTotal", 0);
            fuelType = r.getInt("FuelType", -1);
            idleFlame = r.getBoolean("IdleFlame", false);
            fuel.clearContent();
            if (r.hasCompound("Fuel")) {
                NonNullList<ItemStack> list = NonNullList.withSize(FUEL_SLOTS, ItemStack.EMPTY);
                r.child("Fuel").loadItems(list);
                fuel.setItem(0, list.get(0));
            }
        });
    }
}
