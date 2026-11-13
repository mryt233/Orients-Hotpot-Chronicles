// mryt233 改：26.2 原版 API；第三批 3G 存读档适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * 组合灶台的方块实体。
 * 作者：mryt233
 */
public class StoveBlockEntity extends HotPotBlockEntity {
    public static final int FUEL_SLOTS = 1;

    private final SimpleContainer fuel = new SimpleContainer(FUEL_SLOTS) {
        @Override
        public void setChanged() {
            StoveBlockEntity.this.setChanged();
        }
    };
    private int burnTicks = 0;
    private int burnTotal = 0;
    private int fuelType = -1;
    private boolean idleFlame = false;
    private boolean attachedPotDropped = false;

    public StoveBlockEntity(BlockPos worldPosition, BlockState state) {
        super(HotPotContent.STOVE_BE, worldPosition, state);
    }

    @Override
    protected void serverTick(Level world) {
        boolean changed = false;
        BlockState state = world.getBlockState(worldPosition);
        if (world.getGameTime() % 20L == 0L && state.getBlock() instanceof StoveBlock stove && stove.lift() > 0) {
            stove.placeTop(world, worldPosition);
        }
        HotPotBlockEntity pot = heatedPot();
        boolean potHasSoup = pot != null && pot.hasSoup();
        boolean potHasRaw = potHasSoup && pot.hasRawIngredient();
        boolean soupIdle = potHasSoup && !potHasRaw;
        if (burnTicks > 0 && !soupIdle) {
            burnTicks--;
            if (burnTicks == 0) {
                fuelType = -1;
                burnTotal = 0;
                changed = true;
            }
        }
        if (burnTicks <= 0 && potHasRaw && consumeFuel()) {
            changed = true;
        }
        boolean idle = burnTicks <= 0 && soupIdle && HotPotFuel.burnTicksOf(level, fuel.getItem(0)) > 0;
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

    static void giveWarmth(Level world, BlockPos worldPosition) {
        giveWarmth(world, worldPosition, Config.warmthDuration());
    }

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

    private boolean consumeFuel() {
        if (level == null) {
            return false;
        }
        ItemStack stack = fuel.getItem(0);
        int ticks = HotPotFuel.burnTicksOf(level, stack);
        if (ticks <= 0) {
            return false;
        }
        var remainderItem = stack.getItem().getCraftingRemainder();
        stack.shrink(1);
        if (stack.isEmpty()) {
            fuel.setItem(0, remainderItem == null ? ItemStack.EMPTY : remainderItem.create());
        }
        burnTicks = ticks;
        burnTotal = ticks;
        HotPotFuel grade = HotPotFuel.gradeForTicks(ticks);
        fuelType = grade == null ? -1 : grade.ordinal();
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE,
            SoundSource.BLOCKS, 0.7F, 0.8F);
        return true;
    }

    public boolean attachPot(HotPotMaterial material, Direction.Axis handleAxis) {
        if (level == null || level.isClientSide() || material == null) {
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

    public void detachPot(Player player) {
        if (level == null || level.isClientSide() || !hasPot()) {
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

    private void removePot() {
        clearPotContents();
        attachedPotDropped = false;
        BlockState state = level.getBlockState(worldPosition);
        level.setBlock(worldPosition, state.setValue(StoveBlock.POT, StovePot.NONE)
            .setValue(StoveBlock.SOUP, SoupVisual.NONE), Block.UPDATE_CLIENTS);
        markDirtyAndSync();
    }

    public void breakPot(Player player) {
        if (level == null || level.isClientSide() || !hasPot()) {
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

    public void dropAttachedPot(HotPotMaterial material) {
        if (attachedPotDropped || level == null || level.isClientSide()) {
            return;
        }
        attachedPotDropped = true;
        if (material != null) {
            Block.popResource(level, worldPosition, toPotItem(material));
            clearPotContents();
        }
    }

    public Container fuelInventory() {
        return fuel;
    }

    public void openFuelScreen(Player player) {
        if (level == null || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        HotpotScreens.openFuel(serverPlayer, this);
    }

    public void dropFuel() {
        if (level == null || level.isClientSide()) {
            return;
        }
        ItemStack stack = fuel.getItem(0);
        if (!stack.isEmpty()) {
            Block.popResource(level, worldPosition, stack);
            fuel.setItem(0, ItemStack.EMPTY);
            markDirtyAndSync();
        }
    }

    public boolean burning() {
        return burnTicks > 0 || idleFlame;
    }

    public boolean idle() {
        return idleFlame && burnTicks <= 0;
    }

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

    @Override
    protected void saveAdditional(ValueOutput nbt) {
        super.saveAdditional(nbt);
        BeIo.write(nbt, w -> {
            w.putInt("BurnTicks", burnTicks);
            w.putInt("BurnTotal", burnTotal);
            w.putInt("FuelType", fuelType);
            w.putBoolean("IdleFlame", idleFlame);
            NonNullList<ItemStack> list = NonNullList.withSize(FUEL_SLOTS, ItemStack.EMPTY);
            list.set(0, fuel.getItem(0));
            w.child("Fuel").putItems(list);
        });
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !consumeBreakHandled()) {
            dropAttachedPot(state.getValue(StoveBlock.POT).material());
            dropContents();
            dropFuel();
        }
    }
}
