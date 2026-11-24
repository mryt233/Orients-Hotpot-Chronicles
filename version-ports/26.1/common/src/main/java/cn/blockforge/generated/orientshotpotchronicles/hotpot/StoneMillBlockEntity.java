// mryt233 改：26.1 同代移植同步。
// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 小石磨方块实体：存放投入原料、湿磨储水次数、待接取槽成品及推动圈数，驱动上扇石磨同步旋转。
 * 作者：mryt233
 */
public class StoneMillBlockEntity extends SyncedBlockEntity {
    public static final int MAX_WATER_USES = 12;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack troughResult = ItemStack.EMPTY;
    private String expectedContainerId = "";
    private int troughColor = StoneMillRecipe.DEFAULT_COLOR;
    private int waterUses;
    private int turns;
    private int quarterTurns;

    private transient StoneMillMotion clientMotion;
    private boolean renderGrindWet;
    private boolean renderGrindSpill;
    private int renderGrindColor;

    public StoneMillMotion clientMotion() {
        if (clientMotion == null) clientMotion = new StoneMillMotion();
        return clientMotion;
    }

    public boolean renderGrindWet() { return renderGrindWet; }
    public boolean renderGrindSpill() { return renderGrindSpill; }
    public int renderGrindColor() { return renderGrindColor; }

    public StoneMillBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.STONE_MILL_BE, pos, state);
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack troughResult() {
        return troughResult;
    }

    public String expectedContainerId() {
        return expectedContainerId;
    }

    public int troughColor() {
        return troughColor;
    }

    public int waterUses() {
        return waterUses;
    }

    public int turns() {
        return turns;
    }

    public int quarterTurns() {
        return quarterTurns;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean tryAddWater(Player player, InteractionHand hand, ItemStack stack) {
        if (waterUses >= MAX_WATER_USES) {
            return false;
        }
        int gain = 0;
        ItemStack remainder = ItemStack.EMPTY;
        if (stack.is(Items.WATER_BUCKET)) {
            gain = Config.millWaterUses();
            remainder = new ItemStack(Items.BUCKET);
        } else if (stack.is(Items.POTION)
            && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
            gain = 1;
            remainder = new ItemStack(Items.GLASS_BOTTLE);
        }
        if (gain <= 0) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            waterUses = Math.min(MAX_WATER_USES, waterUses + gain);
            consumeAndReturn(player, hand, stack, 1, remainder);
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7F, 1.15F);
            spawnFluidParticles(ParticleTypes.SPLASH, 6);
            sync();
        }
        return true;
    }

    public boolean tryCollectTrough(Player player, InteractionHand hand, ItemStack stack) {
        if (troughResult.isEmpty()) {
            return false;
        }
        boolean hasExpectedContainer = expectedContainerId != null && !expectedContainerId.isEmpty();
        Item expected = hasExpectedContainer
            ? BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(expectedContainerId)).orElse(Items.AIR)
            : Items.AIR;
        if (expected != Items.AIR && !stack.is(expected)) {
            if (level != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.stone_mill_need_container"),
                    "磨槽已盛满成品，请拿 %s 盛出",
                    new ItemStack(expected).getHoverName()));
            }
            return true;
        }
        if (level != null && !level.isClientSide()) {
            ItemStack out = troughResult.copy();
            if (expected != Items.AIR) {
                consumeAndReturn(player, hand, stack, 1, out);
            } else {
                giveOrDrop(player, out);
            }
            troughResult = ItemStack.EMPTY;
            expectedContainerId = "";
            turns = 0;
            clearGrindEffect();
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7F, 1.1F);
            sync();
        }
        return true;
    }

    public boolean tryInsertInput(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null || !troughResult.isEmpty() || stack.isEmpty()) {
            return false;
        }
        StoneMillRecipe recipe = HotPotRecipes.stoneMill(level, stack);
        if (recipe == null) {
            return false;
        }
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) {
            return false;
        }
        int needed = recipe.ingredientCount() - input.getCount();
        if (needed <= 0) {
            return false;
        }
        if (!level.isClientSide()) {
            int move = Math.min(needed, stack.getCount());
            if (input.isEmpty()) {
                input = stack.copyWithCount(move);
                turns = 0;
            } else {
                input.grow(move);
            }
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(move);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.1F);
            sync();
        }
        return true;
    }

    public boolean tryWithdrawInput(Player player) {
        if (input.isEmpty() || !troughResult.isEmpty()) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            giveOrDrop(player, input.copy());
            input = ItemStack.EMPTY;
            turns = 0;
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.45F, 0.9F);
            sync();
        }
        return true;
    }

    public boolean tryGrind(Player player, Direction facing) {
        if (level == null) {
            return false;
        }
        if (!troughResult.isEmpty()) {
            if (tryAutoDrainToAdjacentVat(facing, false)) return true;
            return tryCollectTrough(player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        }
        if (input.isEmpty()) {
            return false;
        }
        StoneMillRecipe recipe = HotPotRecipes.stoneMill(level, input);
        if (recipe == null || input.getCount() < recipe.ingredientCount()) {
            if (!level.isClientSide()) {
                int req = recipe != null ? recipe.ingredientCount() : 1;
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.stone_mill_need_more"),
                    "原料数量不足（当前 %s / %s）",
                    input.getCount(), req));
            }
            return true;
        }
        if (recipe.requiresWater() && waterUses <= 0) {
            if (!level.isClientSide()) {
                level.playSound(null, worldPosition, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.6F, 0.85F);
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.stone_mill_need_water"),
                    "磨制该配方需要先倒入一桶水或水瓶"));
            }
            return true;
        }
        if (!level.isClientSide()) {
            turns++;
            quarterTurns++;
            renderGrindWet = recipe.requiresWater();
            renderGrindSpill = false;
            renderGrindColor = recipe.color();
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS,
                0.65F, 0.9F + level.getRandom().nextFloat() * 0.25F);
            if (level instanceof ServerLevel server) {
                // mryt233 改（2026-11-20）：传入独立单份快照，避免后续 input.shrink() 在异步编码粒子包前把原栈减空引发断线。
                ItemParticleOption grit = itemParticle(input);
                if (grit != null) {
                    server.sendParticles(grit,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 1.22, worldPosition.getZ() + 0.5,
                        6, 0.16, 0.06, 0.16, 0.04);
                }
                if (recipe.requiresWater()) {
                    spawnFluidParticles(ParticleTypes.SPLASH, 5);
                }
            }
            int goal = recipe.effectiveTurns();
            if (turns >= goal) {
                input.shrink(recipe.ingredientCount());
                if (input.isEmpty()) {
                    input = ItemStack.EMPTY;
                }
                if (recipe.requiresWater()) {
                    waterUses = Math.max(0, waterUses - 1);
                }
                turns = 0;
                if (recipe.container().isPresent()) {
                    troughResult = recipe.result().copy();
                    troughColor = recipe.color();
                    renderGrindSpill = renderGrindWet;
                    expectedContainerId = BuiltInRegistries.ITEM.getKey(recipe.container().get()).toString();
                    level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.2F);
                    tryAutoDrainToAdjacentVat(facing, true);
                } else {
                    double dx = worldPosition.getX() + 0.5 + facing.getStepX() * 0.45;
                    double dy = worldPosition.getY() + 0.95;
                    double dz = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.45;
                    Containers.dropItemStack(level, dx, dy, dz, recipe.result().copy());
                    level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.15F);
                }
            }
            sync();
        }
        return true;
    }

    public Direction spoutFacing() {
        BlockState state = getBlockState();
        return state != null && state.hasProperty(StoneMillBlock.FACING)
            ? state.getValue(StoneMillBlock.FACING) : Direction.SOUTH;
    }

    public boolean tryAutoDrainToAdjacentVat() {
        return tryAutoDrainToAdjacentVat(spoutFacing(), false);
    }

    private boolean tryAutoDrainToAdjacentVat(Direction facing, boolean fromGrind) {
        if (level == null || level.isClientSide() || troughResult.isEmpty()) {
            return false;
        }
        if (!troughResult.is(HotPotContent.SOYMILK_BUCKET)) {
            return false;
        }
        if (!(level.getBlockEntity(worldPosition.relative(facing)) instanceof WoodenVatBlockEntity vat)
            || !vat.canReceiveSoymilk()) {
            return false;
        }
        if (!vat.receiveSoymilkFromMill(troughColor)) {
            return false;
        }
        troughResult = ItemStack.EMPTY;
        expectedContainerId = "";
        turns = 0;
        if (!fromGrind) {
            clearGrindEffect();
            sync();
        }
        return true;
    }

    // mryt233 改：不要让一次研磨的表现粘到补桶或取浆动作上。
    private void clearGrindEffect() {
        renderGrindWet = false;
        renderGrindSpill = false;
    }

    // mryt233 改：26.1 的粒子载荷接收单份独立的 ItemStackTemplate。
    static ItemParticleOption itemParticle(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : new ItemParticleOption(ParticleTypes.ITEM,
            ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1)));
    }

    public void dropContents() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!input.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                worldPosition.getZ() + 0.5, input.copy());
            input = ItemStack.EMPTY;
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropContents();
    }

    private void spawnFluidParticles(net.minecraft.core.particles.SimpleParticleType type, int count) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(type, worldPosition.getX() + 0.5, worldPosition.getY() + 1.15,
                worldPosition.getZ() + 0.5, count, 0.22, 0.05, 0.22, 0.02);
        }
    }

    private static void consumeAndReturn(Player player, InteractionHand hand, ItemStack stack, int count,
                                         ItemStack remainder) {
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(count);
        }
        if (!remainder.isEmpty()) {
            if (stack.isEmpty() && !player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, remainder);
            } else if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (renderGrindWet) {
            tag.putInt("RenderGrindColor", renderGrindColor);
            tag.putBoolean("RenderGrindSpill", renderGrindSpill);
        }
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        BeIo.write(out, writer -> {
            writer.putItem("Input", input);
            writer.putItem("TroughResult", troughResult);
            writer.putString("ContainerId", expectedContainerId);
            writer.putInt("TroughColor", troughColor);
            writer.putInt("WaterUses", waterUses);
            writer.putInt("Turns", turns);
            writer.putInt("QuarterTurns", quarterTurns);
        });
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        var visualColor = in.getInt("RenderGrindColor");
        renderGrindWet = visualColor.isPresent();
        renderGrindSpill = renderGrindWet && in.getBooleanOr("RenderGrindSpill", false);
        renderGrindColor = visualColor.orElse(0);
        BeIo.read(in, reader -> {
            input = reader.getItem("Input");
            troughResult = reader.getItem("TroughResult");
            expectedContainerId = reader.getString("ContainerId", "");
            troughColor = reader.getInt("TroughColor", StoneMillRecipe.DEFAULT_COLOR);
            waterUses = Math.max(0, Math.min(MAX_WATER_USES, reader.getInt("WaterUses", 0)));
            turns = Math.max(0, reader.getInt("Turns", 0));
            quarterTurns = Math.max(0, reader.getInt("QuarterTurns", 0));
        });
    }
}
