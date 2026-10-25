package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

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

    private final SimpleInventory fuel = new SimpleInventory(FUEL_SLOTS);
    private int burnTicks = 0;
    private int burnTotal = 0;
    private int fuelType = -1;
    /** 空烧：锅里有汤、灶膛里有燃料，但没有生食材要煮——火点着、汤沸腾，燃料一点不耗。 */
    private boolean idleFlame = false;
    private boolean attachedPotDropped = false;

    public StoveBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.STOVE_BE, pos, state);
    }

    // ------------------------------------------------------------------ tick

    /** 先消耗灶火，再让父类推进锅内烹饪。 */
    @Override
    protected void serverTick(World world) {
        boolean changed = false;
        // r83 放下的高灶台还没有「上半截」，补上（每秒查一次）
        if (world.getTime() % 20L == 0L && getCachedState().getBlock() instanceof StoveBlock stove) {
            stove.placeTop(world, pos);
        }
        // 只有真在煮生食材时才耗燃料。锅里有汤但没有生食材：正在烧的这份燃料暂停不减，灶火和沸腾照常
        if (burnTicks > 0 && !soupIdle()) {
            burnTicks--;
            if (burnTicks == 0) {
                fuelType = -1;
                burnTotal = 0;
                changed = true;
            }
        }
        // 像熔炉一样：只有锅里确实还有要煮的东西时才烧掉下一份燃料。
        if (burnTicks <= 0 && wantsHeat() && consumeFuel()) {
            changed = true;
        }
        // 灶膛有燃料、锅里有汤：哪怕一份燃料都没点，也直接点火沸腾，不消耗燃料
        boolean idle = burnTicks <= 0 && soupIdle() && HotPotFuel.burnTicksOf(fuel.getStack(0)) > 0;
        if (idle != idleFlame) {
            idleFlame = idle;
            changed = true;
        }
        boolean lit = burnTicks > 0 || idleFlame;
        BlockState state = world.getBlockState(pos);
        if (StoveBlock.isStove(state) && state.get(StoveBlock.LIT) != lit) {
            world.setBlockState(pos, state.with(StoveBlock.LIT, lit), Block.NOTIFY_LISTENERS);
        }
        if (changed) {
            markDirtyAndSync();
        } else if (burnTicks > 0 && world.getTime() % 20L == 0L) {
            markDirty();
        }
        if (lit && world.getTime() % WARMTH_INTERVAL == 0L && heatedPot() != null) {
            giveWarmth(world, pos);
        }
        super.serverTick(world);
    }

    /** 围锅回血：每 4 秒给范围内玩家续一次生命恢复 I，做法同原版信标（环境效果、粒子很淡）。 */
    static final long WARMTH_INTERVAL = 80L;
    private static final int WARMTH_RANGE = 6;
    private static final int WARMTH_DURATION = 200;

    /** 给 pos 周围的玩家续生命恢复；篝火上的独立锅（r102）也用这个。 */
    static void giveWarmth(World world, BlockPos pos) {
        Box area = new Box(pos).expand(WARMTH_RANGE);
        for (PlayerEntity player : world.getNonSpectatingEntities(PlayerEntity.class, area)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, WARMTH_DURATION, 0, true, true));
        }
    }

    /** 被这台灶台加热的锅：坐在灶里的锅就是自己，否则是正上方的旧式独立锅。 */
    @org.jetbrains.annotations.Nullable
    private HotPotBlockEntity heatedPot() {
        if (hasPot()) {
            return this;
        }
        if (world != null && world.getBlockEntity(pos.up()) instanceof HotPotBlockEntity pot) {
            return pot;
        }
        return null;
    }

    /** 锅里有汤但没有要煮的生食材（空汤或全是熟的）：火可以烧着、汤可以沸腾，但不耗燃料。 */
    private boolean soupIdle() {
        HotPotBlockEntity pot = heatedPot();
        return pot != null && pot.hasSoup() && !pot.hasRawIngredient();
    }

    /** 是否真的需要点火：锅里有汤，并且还有没煮熟的食材。 */
    private boolean wantsHeat() {
        if (hasPot()) {
            return hasSoup() && hasRawIngredient();
        }
        // 兼容旧式独立锅：它坐在灶台正上方，由这台灶台提供热源。
        if (world != null && world.getBlockEntity(pos.up()) instanceof HotPotBlockEntity pot) {
            return pot.hasSoup() && pot.hasRawIngredient();
        }
        return false;
    }

    /**
     * 从灶膛里取出下一份燃料点燃。燃烧时长直接取该物品的原版熔炉数值；
     * 桶类燃料烧完后退回空桶，和熔炉一致。
     */
    private boolean consumeFuel() {
        if (world == null) {
            return false;
        }
        ItemStack stack = fuel.getStack(0);
        int ticks = HotPotFuel.burnTicksOf(stack);
        if (ticks <= 0) {
            return false;
        }
        Item remainderItem = stack.getItem().getRecipeRemainder();
        stack.decrement(1);
        if (stack.isEmpty()) {
            fuel.setStack(0, remainderItem == null || remainderItem == Items.AIR
                ? ItemStack.EMPTY : new ItemStack(remainderItem));
        }
        burnTicks = ticks;
        burnTotal = ticks;
        HotPotFuel grade = HotPotFuel.gradeForTicks(ticks);
        fuelType = grade == null ? -1 : grade.ordinal();
        world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE,
            SoundCategory.BLOCKS, 0.7F, 0.8F);
        return true;
    }

    // ------------------------------------------------------------------ 锅状态

    /** 将锅材质写入灶台自身的 POT 状态，不创建第二个方块。 */
    public void attachPot(HotPotMaterial material, net.minecraft.util.math.Direction.Axis handleAxis) {
        if (world == null || world.isClient || material == null) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (!StoveBlock.isStove(state) || state.get(StoveBlock.POT).hasPot()) {
            return;
        }
        world.setBlockState(pos, state.with(StoveBlock.POT, StovePot.of(material))
            .with(StoveBlock.SOUP, SoupVisual.NONE)
            .with(StoveBlock.POT_AXIS, handleAxis), Block.NOTIFY_LISTENERS);
        markDirtyAndSync();
    }

    /**
     * 潜行右键拆锅：整锅（汤底 + 食材 + 熟度）打包成锅物品给玩家，灶台恢复为空灶。
     */
    public void detachPot(PlayerEntity player) {
        if (world == null || world.isClient || !hasPot()) {
            return;
        }
        HotPotMaterial material = potMaterial();
        if (material == null) {
            return;
        }
        ItemStack pot = toPotItem(material);
        removePot();
        if (!player.getInventory().insertStack(pot)) {
            player.dropItem(pot, false);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND,
            SoundCategory.BLOCKS, 0.25F, 0.75F);
        markDirtyAndSync();
    }

    /** 锅从灶上拿走后：清空锅数据，灶台变回空灶。 */
    private void removePot() {
        clearPotContents();
        attachedPotDropped = false;
        BlockState state = world.getBlockState(pos);
        world.setBlockState(pos, state.with(StoveBlock.POT, StovePot.NONE)
            .with(StoveBlock.SOUP, SoupVisual.NONE), Block.NOTIFY_LISTENERS);
        markDirtyAndSync();
    }

    /**
     * 对着锅挖：只挖下锅，灶台保留。效果照原版挖方块：锅的碎屑和破坏声、工具耐久、统计；
     * 能挖下来（石镐以上）就整锅打包掉落，挖不下来就只撒出食材。创造模式直接拿掉。
     */
    public void breakPot(PlayerEntity player) {
        if (world == null || world.isClient || !hasPot()) {
            return;
        }
        HotPotMaterial material = potMaterial();
        if (material == null) {
            return;
        }
        HotPotBlock potBlock = HotPotContent.blockFor(material);
        BlockState potState = potBlock.getDefaultState();
        world.syncWorldEvent(null, net.minecraft.world.WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(potState));
        if (!player.isCreative()) {
            if (player.canHarvest(potState)) {
                Block.dropStack(world, pos, toPotItem(material));
            } else {
                dropContents();
            }
            player.getMainHandStack().postMine(world, potState, pos, player);
            player.incrementStat(net.minecraft.stat.Stats.MINED.getOrCreateStat(potBlock));
            player.addExhaustion(0.005F);
        }
        removePot();
    }

    /**
     * 破坏灶台时掉落附着的锅（连汤带食材打包）。用标记防止玩家破坏回调和状态替换回调重复掉落。
     */
    public void dropAttachedPot(HotPotMaterial material) {
        if (attachedPotDropped || world == null || world.isClient) {
            return;
        }
        attachedPotDropped = true;
        if (material != null) {
            Block.dropStack(world, pos, toPotItem(material));
            clearPotContents();
        }
    }

    // ------------------------------------------------------------------ 灶膛交互

    /** 灶膛燃料槽，供灶台界面读取与操作。 */
    public Inventory fuelInventory() {
        return fuel;
    }

    /** 打开灶膛界面；只有服务端会真正发打开屏幕的包。 */
    public void openFuelScreen(PlayerEntity player) {
        if (world == null || world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity p) {
                return new StoveScreenHandler(syncId, playerInventory, StoveBlockEntity.this);
            }

            @Override
            public BlockPos getScreenOpeningData(ServerPlayerEntity sp) {
                return pos;
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable(HotPotBlockEntity.key("screen.stove"));
            }
        });
    }

    /** 破坏灶台时把灶膛里剩下的燃料原样掉出来。 */
    public void dropFuel() {
        if (world == null || world.isClient) {
            return;
        }
        ItemStack stack = fuel.getStack(0);
        if (!stack.isEmpty()) {
            Block.dropStack(world, pos, stack);
            fuel.setStack(0, ItemStack.EMPTY);
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

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("BurnTicks", burnTicks);
        nbt.putInt("BurnTotal", burnTotal);
        nbt.putInt("FuelType", fuelType);
        nbt.putBoolean("IdleFlame", idleFlame);
        // 灶膛燃料单独存进 "Fuel" 子标签，避开父类 81 格用的 "Items" 键。
        DefaultedList<ItemStack> list = DefaultedList.ofSize(FUEL_SLOTS, ItemStack.EMPTY);
        list.set(0, fuel.getStack(0));
        NbtCompound fuelNbt = new NbtCompound();
        Inventories.writeNbt(fuelNbt, list, registries);
        nbt.put("Fuel", fuelNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        burnTicks = nbt.contains("BurnTicks") ? nbt.getInt("BurnTicks") : 0;
        burnTotal = nbt.contains("BurnTotal") ? nbt.getInt("BurnTotal") : 0;
        fuelType = nbt.contains("FuelType") ? nbt.getInt("FuelType") : -1;
        idleFlame = nbt.getBoolean("IdleFlame");
        fuel.clear();
        if (nbt.contains("Fuel", NbtElement.COMPOUND_TYPE)) {
            DefaultedList<ItemStack> list = DefaultedList.ofSize(FUEL_SLOTS, ItemStack.EMPTY);
            Inventories.readNbt(nbt.getCompound("Fuel"), list, registries);
            fuel.setStack(0, list.get(0));
        }
    }
}
