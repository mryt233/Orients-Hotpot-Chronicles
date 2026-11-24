// mryt233 改：26.2 移植同步。
// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;


import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 开口大木桶方块实体：存放豆浆与点卤物料、木压板覆盖态与计时进度。
 * 作者：mryt233
 */
public class WoodenVatBlockEntity extends SyncedBlockEntity {
    
    private String fluid = "";
    private int fluidUses;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private boolean hasLid;
    private int progress;
    private int totalTicks;
    private int color = WoodenVatRecipe.DEFAULT_COLOR;

    public WoodenVatBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.WOODEN_VAT_BE, pos, state);
    }

    public String fluid() {
        return fluid;
    }

    public int fluidUses() {
        return fluidUses;
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack output() {
        return output;
    }

    public boolean hasLid() {
        return hasLid;
    }

    public int progress() {
        return progress;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public int color() {
        return color;
    }

    public static final float LID_CLEARANCE = 0.15F;

    public float fillFraction() {
        if (fluid.isEmpty() || fluidUses <= 0) {
            return output.isEmpty() ? 0F : 0.45F;
        }
        return 0.9F;
    }

    public float progressRatio() {
        return progressRatio(0F);
    }

    public float progressRatio(float tickDelta) {
        if (totalTicks <= 0) {
            return 0F;
        }
        float delta = hasLid && output.isEmpty() && progress < totalTicks
            ? Math.max(0F, Math.min(1F, tickDelta))
            : 0F;
        return Math.max(0F, Math.min(1F, (progress + delta) / (float) totalTicks));
    }

    /** 根据是否成品与压制进度计算 3D 木压板底面高度（像素 0..16 坐标，mryt233 改）。 */
    public static float lidBottomY(boolean hasOutput, float press) {
        if (hasOutput) {
            return 9.8F;
        }
        float clamped = Math.max(0F, Math.min(1F, press));
        return 11.2F - clamped * 1.4F;
    }

    /** 盖上木压板后液面同步随压板底面下沉，恒保留 0.15 px 防穿模间隙（mryt233 改）。 */
    public static float effectiveLiquidY(float fill, boolean hasLid, boolean hasOutput, float press) {
        float raw = 3.8F + Math.max(0F, fill) * 8.7F;
        if (!hasLid) {
            return raw;
        }
        return Math.min(raw, lidBottomY(hasOutput, press) - LID_CLEARANCE);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState current = getBlockState();
            if (current.hasProperty(WoodenVatBlock.HAS_LID) && current.getValue(WoodenVatBlock.HAS_LID) != hasLid) {
                level.setBlock(worldPosition, current.setValue(WoodenVatBlock.HAS_LID, hasLid), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, WoodenVatBlockEntity vat) {
        if (!world.isClientSide() || !vat.hasLid || !vat.output.isEmpty()
            || vat.fluid.isEmpty() || vat.fluidUses <= 0 || vat.input.isEmpty() || vat.totalTicks <= 0) {
            return;
        }
        if (vat.progress < vat.totalTicks) {
            vat.progress++;
        }
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, WoodenVatBlockEntity vat) {
        if (world.isClientSide() || !vat.output.isEmpty() || vat.fluid.isEmpty() || vat.fluidUses <= 0 || vat.input.isEmpty()) {
            return;
        }
        WoodenVatRecipe recipe = HotPotRecipes.woodenVat(world, vat.fluid, vat.input);
        if (recipe == null) {
            return;
        }
        if (recipe.needsLid() && !vat.hasLid) {
            return;
        }
        vat.totalTicks = recipe.effectiveTicks();
        vat.progress++;
        if (vat.progress % 12 == 0 && world instanceof ServerLevel server) {
            server.sendParticles(recipe.needsLid() ? ParticleTypes.CLOUD : ParticleTypes.BUBBLE_POP,
                pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                2, 0.18, 0.04, 0.18, 0.01);
        }
        if (vat.progress >= vat.totalTicks) {
            vat.output = recipe.result().copy();
            vat.color = recipe.color();
            vat.input = ItemStack.EMPTY;
            vat.progress = 0;
            vat.totalTicks = 0;
            vat.fluid = "";
            vat.fluidUses = 0;
            world.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.45F, 1.35F);
            vat.sync();
        } else if (vat.progress % 8 == 0) {
            vat.sync();
        }
    }

    public boolean tryCollectOutput(Player player) {
        if (output.isEmpty()) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            giveOrDrop(player, output.copy());
            output = ItemStack.EMPTY;
            if (hasLid) {
                hasLid = false;
                giveOrDrop(player, new ItemStack(HotPotContent.WOODEN_PRESS_LID));
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.15F);
            sync();
            drainAdjacentMills();
        }
        return true;
    }

    // mryt233 加：允许紧挨小石磨出料口时自动接取现磨豆浆，并在取走成品腾空木桶后触发石磨续灌。
    public boolean canReceiveSoymilk() {
        return output.isEmpty() && input.isEmpty() && !hasLid && (fluid.isEmpty() || fluidUses <= 0);
    }

    public boolean receiveSoymilkFromMill(int millColor) {
        if (!canReceiveSoymilk()) {
            return false;
        }
        if (level != null && level.isClientSide()) {
            return false;
        }
        fluid = WoodenVatRecipe.FLUID_SOYMILK;
        fluidUses = 1;
        color = (millColor & 0xFFFFFF) | 0xFF000000;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.65F, 1.05F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, 6, 0.16, 0.06, 0.16, 0.02);
            }
            sync();
        }
        return true;
    }

    private void drainAdjacentMills() {
        if (level == null || level.isClientSide() || !canReceiveSoymilk()) {
            return;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof StoneMillBlockEntity mill
                && mill.spoutFacing() == dir.getOpposite()
                && mill.tryAutoDrainToAdjacentVat()) {
                return;
            }
        }
    }

    public boolean tryPourFluid(Player player, InteractionHand hand, ItemStack stack) {
        if (!output.isEmpty() || !input.isEmpty() || hasLid) {
            return false;
        }
        if (stack.is(HotPotContent.SOYMILK_BUCKET)) {
            if (!fluid.isEmpty()) {
                return false;
            }
            if (level != null && !level.isClientSide()) {
                fluid = WoodenVatRecipe.FLUID_SOYMILK;
                fluidUses = 1;
                color = 0xFFF3ECD8;
                consumeAndReturn(player, hand, stack, 1, new ItemStack(Items.BUCKET));
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7F, 1.0F);
                sync();
            }
            return true;
        }
        return false;
    }

    public boolean tryApplyLid(Player player, InteractionHand hand, ItemStack stack) {
        if (!stack.is(HotPotContent.WOODEN_PRESS_LID) || hasLid || !output.isEmpty()) {
            return false;
        }
        if (level == null) {
            return false;
        }
        WoodenVatRecipe recipe = HotPotRecipes.woodenVat(level, fluid, input);
        if (recipe == null || !recipe.needsLid()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.vat_need_coagulant"),
                    "请先往木桶倒入豆浆并加入点卤剂，再盖上木压板压制"));
            }
            return true;
        }
        if (!level.isClientSide()) {
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            hasLid = true;
            progress = 0;
            totalTicks = recipe.effectiveTicks();
            level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.75F, 0.95F);
            sync();
        }
        return true;
    }

    public boolean tryInsertItem(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null || !input.isEmpty() || !output.isEmpty() || hasLid || stack.isEmpty()) {
            return false;
        }
        if (fluid.isEmpty() || fluidUses <= 0) {
            return false;
        }
        WoodenVatRecipe recipe = HotPotRecipes.woodenVat(level, fluid, stack);
        if (recipe == null) {
            return false;
        }
        if (stack.getCount() < recipe.ingredientCount()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.vat_need_more"),
                    "原料数量不足，需要 %s 个", recipe.ingredientCount()));
            }
            return true;
        }
        if (!level.isClientSide()) {
            ItemStackTemplate remTpl = stack.getItem().getCraftingRemainder();
            ItemStack remainder = remTpl != null ? remTpl.create() : ItemStack.EMPTY;
            if (!remainder.isEmpty()) {
                remainder.setCount(remainder.getCount() * recipe.ingredientCount());
            }
            input = stack.copyWithCount(recipe.ingredientCount());
            consumeAndReturn(player, hand, stack, recipe.ingredientCount(), remainder);
            progress = 0;
            totalTicks = recipe.effectiveTicks();
            color = recipe.color();
            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.45F, 1.25F);
            sync();
        }
        return true;
    }

    public boolean tryInteractEmpty(Player player) {
        if (tryCollectOutput(player)) {
            return true;
        }
        if (hasLid) {
            if (level != null && !level.isClientSide()) {
                hasLid = false;
                progress = 0;
                giveOrDrop(player, new ItemStack(HotPotContent.WOODEN_PRESS_LID));
                level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.65F, 1.1F);
                sync();
            }
            return true;
        }
        if (!input.isEmpty() && input.getItem().getCraftingRemainder() == null) {
            if (level != null && !level.isClientSide()) {
                giveOrDrop(player, input.copy());
                input = ItemStack.EMPTY;
                progress = 0;
                totalTicks = 0;
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 0.95F);
                sync();
            }
            return true;
        }
        return false;
    }

    public void dropContents() {
        if (level == null || level.isClientSide()) {
            return;
        }
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.6;
        double z = worldPosition.getZ() + 0.5;
        if (!input.isEmpty() && input.getItem().getCraftingRemainder() == null) {
            Containers.dropItemStack(level, x, y, z, input.copy());
            input = ItemStack.EMPTY;
        }
        if (!output.isEmpty()) {
            Containers.dropItemStack(level, x, y, z, output.copy());
            output = ItemStack.EMPTY;
        }
        if (hasLid) {
            Containers.dropItemStack(level, x, y, z, new ItemStack(HotPotContent.WOODEN_PRESS_LID));
            hasLid = false;
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropContents();
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
                player.drop(remainder, false, net.minecraft.util.Prediction.SERVER_ONLY); // mryt233 改：26.3 服务端掉落
            }
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY); // mryt233 改：26.3 服务端掉落
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        BeIo.write(out, writer -> {
            writer.putString("Fluid", fluid);
            writer.putInt("FluidUses", fluidUses);
            writer.putItem("Input", input);
            writer.putItem("Output", output);
            writer.putBoolean("HasLid", hasLid);
            writer.putInt("Progress", progress);
            writer.putInt("TotalTicks", totalTicks);
            writer.putInt("Color", color);
        });
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        BeIo.read(in, reader -> {
            fluid = reader.getString("Fluid", "");
            fluidUses = Math.max(0, Math.min(1, reader.getInt("FluidUses", 0)));
            input = reader.getItem("Input");
            output = reader.getItem("Output");
            hasLid = reader.getBoolean("HasLid", false);
            progress = Math.max(0, reader.getInt("Progress", 0));
            totalTicks = Math.max(0, reader.getInt("TotalTicks", 0));
            color = reader.getInt("Color", WoodenVatRecipe.DEFAULT_COLOR);
            // mryt233 改：旧桶的清水不再占槽，保留已有物料和成品。
            if (!fluid.isEmpty() && !WoodenVatRecipe.FLUID_SOYMILK.equals(fluid)) {
                fluid = "";
                fluidUses = 0;
                progress = 0;
                totalTicks = 0;
            }
        });
    }
}
