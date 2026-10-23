// 0.1.2 的 1.20.1 Fabric 接口迁移：。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * 砧板（2026-10-23 用户定：只做一种；生牛肉 / 生羊肉放上去，拿铁菜刀点一下切成 3 卷）。
 * 切出什么照切石机配方（用户：切石机也能切），所以以后加土豆片、藕片只要加一条切石机配方 + 放进物品标签
 * {@code cuttable}（能放上砧板的东西）。台面上放着的东西由 client/CuttingBoardRenderer 平躺着画出来。
 * 交互：拿能切的东西右键 = 放一个上去；拿菜刀右键 = 切（掉出切好的东西，菜刀掉 1 点耐久）；空手右键 = 拿回来。
 * 标签 {@code cut_piece_by_piece} 里的（土豆）一刀只掉 1 份，切满配方的份数才切完（用户：一个土豆切三下）；其他一刀切完。
 *
 */
public class CuttingBoardBlock extends Block implements BlockEntityProvider, PartOutline {
    public static final TagKey<net.minecraft.item.Item> CUTTABLE =
        TagKey.of(RegistryKeys.ITEM, cn.blockforge.generated.orientshotpotchronicles.GeneratedMod.id("cuttable"));
    /** 一刀一刀切的东西（用户：土豆切三下，每下掉一份）；不在这个标签里的一刀切完（肉卷）。 */
    public static final TagKey<net.minecraft.item.Item> CUT_PIECE_BY_PIECE =
        TagKey.of(RegistryKeys.ITEM, cn.blockforge.generated.orientshotpotchronicles.GeneratedMod.id("cut_piece_by_piece"));
    /** 圆木墩：直径 12、高 3 像素（同 tools/gen_cutting.py 的模型）。 */
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 3, 14);
    public static final float TOP = 3F / 16F;

    public CuttingBoardBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    /** 板上东西的选中范围：板面中间 8×8、高 1.5 像素（画的时候是半格大的平躺物品）。 */
    public static final VoxelShape ITEM_SHAPE = Block.createCuboidShape(4, 3, 4, 12, 4.5, 12);

    private static boolean hasItem(BlockView world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board && !board.item().isEmpty();
    }

    /** 板上有东西时，它也能被准星选中（2026-10-23 用户：和原版食材放上去一样有自己的选中框）。 */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return hasItem(world, pos) ? net.minecraft.util.shape.VoxelShapes.union(SHAPE, ITEM_SHAPE) : SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /** 准星对着板上的东西还是砧板本身（看命中点有没有高出板面、在不在东西的范围里）。 */
    private static boolean aimsAtItem(BlockPos pos, PlayerEntity player) {
        net.minecraft.util.hit.HitResult hit = player.raycast((player.isCreative() ? 5.0 : 4.5), 1.0F, false);
        if (!(hit instanceof BlockHitResult b) || !b.getBlockPos().equals(pos)) {
            return false;
        }
        net.minecraft.util.math.Vec3d p = b.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
        return p.y > TOP + 0.001;   // 板面以上只有板上的东西
    }

    /** 选中框只框准星对着的那样：板上的东西，或者砧板本身。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockView world, BlockPos pos, PlayerEntity player) {
        if (!hasItem(world, pos)) {
            return null;
        }
        return aimsAtItem(pos, player) ? ITEM_SHAPE : SHAPE;
    }

    /** 鼠标中键：对着板上的东西复制那样东西，否则复制砧板。 */
    @Override
    public ItemStack getPickStack(net.minecraft.world.BlockView world, BlockPos pos, BlockState state) {
        PlayerEntity player = PlateHolderBlock.pickPlayer.get();
        if (player != null && world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board && !board.item().isEmpty()
            && aimsAtItem(pos, player)) {
            return board.item().copyWithCount(1);
        }
        return super.getPickStack(world, pos, state);
    }

    /** 这件东西在砧板上切出什么（照切石机配方）；不能切返回空。 */
    public static ItemStack cutResult(World world, ItemStack stack) {
        if (stack.isEmpty() || !stack.isIn(CUTTABLE)) {
            return ItemStack.EMPTY;
        }
        return world.getRecipeManager()
            .getFirstMatch(RecipeType.STONECUTTING, new SimpleInventory(stack), world)
            .map(entry -> entry.getOutput(world.getRegistryManager()).copy())
            .orElse(ItemStack.EMPTY);
    }

    protected ItemActionResult useWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isOf(HotPotContent.ROLLING_PIN)) {
            return pound(stack, world, pos, player, hand, board);
        }
        if (stack.isOf(HotPotContent.KITCHEN_KNIFE)) {
            // 捣过的肉（已经捶碎 / 成浆）不能再切
            ItemStack result = Pounding.poundable(board.item()) && board.cuts() > 0 ? ItemStack.EMPTY : cutResult(world, board.item());
            if (result.isEmpty()) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!world.isClient) {
                ItemStack cut = board.item();
                if (cut.isIn(CUT_PIECE_BY_PIECE) && result.getCount() > 1) {
                    int done = board.cuts() + 1;
                    if (done >= result.getCount()) {
                        board.setItem(ItemStack.EMPTY);
                    } else {
                        board.setCuts(done);
                    }
                    result.setCount(1);
                } else {
                    board.setItem(ItemStack.EMPTY);
                }
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + TOP + 0.1, pos.getZ() + 0.5, result);
                stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                world.playSound(null, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 0.7F, 1.5F);
                ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, cut),
                    pos.getX() + 0.5, pos.getY() + TOP + 0.05, pos.getZ() + 0.5, 8, 0.15, 0.05, 0.15, 0.05);
            }
            return ItemActionResult.SUCCESS;
        }
        if (board.item().isEmpty() && (stack.isIn(CUTTABLE) || Pounding.poundable(stack))) {
            if (!world.isClient) {
                board.setItem(stack.copyWithCount(1));
                if (!player.isCreative()) stack.decrement(1);
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.6F, 1.2F);
            }
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 擀面杖捣一下（Pounding）：前两下砧板上的肉变样（捶碎 → 肉浆），第三下掉出 3 个生丸子；擀面杖掉 1 点耐久。 */
    private static ItemActionResult pound(ItemStack stack, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                          CuttingBoardBlockEntity board) {
        ItemStack meat = board.item();
        net.minecraft.item.Item ball = Pounding.result(meat);
        if (ball == null) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClient) {
            int hits = board.cuts() + 1;
            if (hits >= Pounding.HITS) {
                board.setItem(ItemStack.EMPTY);
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + TOP + 0.1, pos.getZ() + 0.5,
                    new ItemStack(ball, Pounding.RESULT_COUNT));
            } else {
                board.setCuts(hits);
            }
            stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 1.0F, 0.7F);
            world.playSound(null, pos, SoundEvents.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.BLOCKS, 0.6F, 0.9F);
            ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, meat),
                pos.getX() + 0.5, pos.getY() + TOP + 0.05, pos.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.04);
        }
        return ItemActionResult.SUCCESS;
    }

    /** 空手右键：把砧板上的东西拿回来。 */
    protected ActionResult useWithoutItem(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) || board.item().isEmpty()) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            player.getInventory().offerOrDrop(board.item());
            board.setItem(ItemStack.EMPTY);
        }
        return ActionResult.SUCCESS;
    }

    /** 挖掉时板上的东西掉出来。 */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + TOP, pos.getZ() + 0.5, board.item());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    // 沿用工程既有的 1.21 风格右键分发。
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return ItemActionResult.dispatch(state, player, hand,
            () -> useWithItem(player.getStackInHand(hand), state, world, pos, player, hand, hit),
            () -> useWithoutItem(state, world, pos, player, hit));
    }
}
