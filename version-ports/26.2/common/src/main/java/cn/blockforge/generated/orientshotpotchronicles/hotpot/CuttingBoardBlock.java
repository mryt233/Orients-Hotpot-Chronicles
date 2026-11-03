// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * 砧板（2026-10-23 用户定：只做一种；生牛肉 / 生羊肉放上去，拿铁菜刀点一下切成 3 卷）。
 * 切出什么照切石机配方（用户：切石机也能切），所以以后加土豆片、藕片只要加一条切石机配方 + 放进物品标签
 * {@code cuttable}（能放上砧板的东西）。台面上放着的东西由 client/CuttingBoardRenderer 平躺着画出来。
 * 交互：拿能切的东西右键 = 放一个上去；拿菜刀右键 = 切（掉出切好的东西，菜刀掉 1 点耐久）；空手右键 = 拿回来。
 * 标签 {@code cut_piece_by_piece} 里的（土豆）一刀只掉 1 份，切满配方的份数才切完（用户：一个土豆切三下）；其他一刀切完。
 * 作者：mryt233 */
public class CuttingBoardBlock extends Block implements EntityBlock, PartOutline {
    public static final TagKey<net.minecraft.world.item.Item> CUTTABLE =
        TagKey.create(Registries.ITEM, cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.id("cuttable"));
    /** 一刀一刀切的东西（用户：土豆切三下，每下掉一份）；不在这个标签里的一刀切完（肉卷）。 */
    public static final TagKey<net.minecraft.world.item.Item> CUT_PIECE_BY_PIECE =
        TagKey.create(Registries.ITEM, cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.id("cut_piece_by_piece"));
    /** 圆木墩：直径 12、高 3 像素（同 tools/gen_cutting.py 的模型）。 */
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 3, 14);
    public static final float TOP = 3F / 16F;

    /** 朝向 = 放的人站的那一边（2026-10-23 用户：砧板要有朝向，板上的东西朝着玩家）。 */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    public CuttingBoardBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    /** 板上东西的选中范围：板面中间 8×8、高 1.5 像素（画的时候是半格大的平躺物品）。 */
    public static final VoxelShape ITEM_SHAPE = Block.box(4, 3, 4, 12, 4.5, 12);

    private static boolean hasItem(BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board && !board.item().isEmpty();
    }

    /** 板上有东西时，它也能被准星选中（2026-10-23 用户：和原版食材放上去一样有自己的选中框）。 */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return hasItem(world, pos) ? Shapes.or(SHAPE, ITEM_SHAPE) : SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 准星对着板上的东西还是砧板本身（看命中点有没有高出板面、在不在东西的范围里）。 */
    public static boolean aimsAtItem(BlockPos pos, Player player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult b) || !b.getBlockPos().equals(pos)) {
            return false;
        }
        Vec3 p = b.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return p.y > TOP + 0.001;   // 板面以上只有板上的东西
    }

    /** 选中框只框准星对着的那样：板上的东西，或者砧板本身。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        if (!hasItem(world, pos)) {
            return null;
        }
        return aimsAtItem(pos, player) ? ITEM_SHAPE : SHAPE;
    }

    /** 鼠标中键：对着板上的东西复制那样东西，否则复制砧板。 */
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        Player player = PlateHolderBlock.pickPlayer.get();
        if (player != null && world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board && !board.item().isEmpty()
            && aimsAtItem(pos, player)) {
            return board.item().copyWithCount(1);
        }
        return super.getCloneItemStack(world, pos, state, includeData);
    }

    /**
     * 砧板自带的切配表（玩家反馈：数据包里食材的切石机配方被删掉后，砧板就切不了东西了）。
     * 正常情况仍以数据包里的切石机配方为准（整合包想改产物照旧改配方即可），配方查不到时用这张表兜底，
     * 保证砧板本身不依赖任何数据包内容。表里的产物和 tools/gen_sausage.py 等生成器写的切石机配方保持一致。
     * 放在内部类里延迟初始化，避免和 {@link HotPotContent} 的静态注册抢初始化顺序。
     */
    private static final class BuiltinCuts {
        // Map.of 最多 10 对，改用 ofEntries（mryt233 2026-11-01，内容不变）
        static final java.util.Map<Item, ItemStack> TABLE = java.util.Map.ofEntries(
            java.util.Map.entry(Items.BEEF, new ItemStack(HotPotContent.FATTY_BEEF_ROLL, 3)),
            java.util.Map.entry(Items.MUTTON, new ItemStack(HotPotContent.MUTTON_ROLL, 3)),
            java.util.Map.entry(Items.POTATO, new ItemStack(HotPotContent.POTATO_SLICE, 3)),
            java.util.Map.entry(Items.PORKCHOP, new ItemStack(HotPotContent.MINCED_PORK, 1)),
            java.util.Map.entry(HotPotContent.BEEF_LOIN, new ItemStack(HotPotContent.BEEF_LOIN_SLICE, 3)),
            java.util.Map.entry(HotPotContent.TOFU, new ItemStack(HotPotContent.TOFU_CUBE, 3)),
            java.util.Map.entry(HotPotContent.SHRIMP, new ItemStack(HotPotContent.PEELED_SHRIMP, 1)),
            java.util.Map.entry(HotPotContent.HAM_SAUSAGE, new ItemStack(HotPotContent.HAM_SAUSAGE_SLICE, 3))
            // ==== tools/new_item.py：新的砧板切配插在这行上面（别删这行）====
        );

        private BuiltinCuts() {
        }

        static ItemStack of(Item item) {
            ItemStack stack = TABLE.get(item);
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }
    }

    /** 这件东西在砧板上切出什么（照切石机配方，数据包里没有就用内置表）；不能切返回空。 */
    public static ItemStack cutResult(Level world, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(CUTTABLE)) {
            return ItemStack.EMPTY;
        }
        return RecipeSync.find(world, RecipeType.STONECUTTING, stack)
            .map(entry -> entry.value().assemble(new SingleRecipeInput(stack)).copy())
            .orElseGet(() -> BuiltinCuts.of(stack.getItem()));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(HotPotContent.ROLLING_PIN)) {
            return pound(stack, world, pos, player, hand, board);
        }
        if (stack.is(HotPotContent.KITCHEN_KNIFE)) {
            // 捣过的肉（已经捶碎 / 成浆）不能再切
            ItemStack result = Pounding.poundable(board.item()) && board.cuts() > 0 ? ItemStack.EMPTY : cutResult(world, board.item());
            if (result.isEmpty()) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!world.isClientSide()) {
                ItemStack cut = board.item();
                if (cut.is(CUT_PIECE_BY_PIECE) && result.getCount() > 1) {
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
                Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + TOP + 0.1, pos.getZ() + 0.5, result);
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                world.playSound((Player) null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.7F, 1.5F);
                ((ServerLevel) world).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(cut)),
                    pos.getX() + 0.5, pos.getY() + TOP + 0.05, pos.getZ() + 0.5, 8, 0.15, 0.05, 0.15, 0.05);
            }
            return InteractionResult.SUCCESS;
        }
        if (board.item().isEmpty() && (stack.is(CUTTABLE) || Pounding.poundable(stack))) {
            if (!world.isClientSide()) {
                // 食物朝向单独记：放的人站在哪边，食物就朝哪边（和砧板自己的朝向无关，用户要分开）
                board.setItem(stack.copyWithCount(1), player.getDirection().getOpposite());
                stack.consume(1, player);
                world.playSound((Player) null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** 擀面杖捣一下（Pounding）：前两下砧板上的肉变样（捶碎 → 肉浆），第三下掉出 3 个生丸子；擀面杖掉 1 点耐久。 */
    private static InteractionResult pound(ItemStack stack, Level world, BlockPos pos, Player player, InteractionHand hand,
                                          CuttingBoardBlockEntity board) {
        ItemStack meat = board.item();
        Item ball = Pounding.result(meat);
        if (ball == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!world.isClientSide()) {
            int hits = board.cuts() + 1;
            if (hits >= Pounding.HITS) {
                board.setItem(ItemStack.EMPTY);
                Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + TOP + 0.1, pos.getZ() + 0.5,
                    new ItemStack(ball, Pounding.resultCount(meat)));
            } else {
                board.setCuts(hits);
            }
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            world.playSound((Player) null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.7F);
            world.playSound((Player) null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.6F, 0.9F);
            ((ServerLevel) world).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(meat)),
                pos.getX() + 0.5, pos.getY() + TOP + 0.05, pos.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.04);
        }
        return InteractionResult.SUCCESS;
    }

    /** 对着板上的东西挖：一下就挖掉（只挖东西，砧板留着，见 HotPotContent 的破坏事件）；否则按砧板自己的硬度。 */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (hasItem(world, pos) && aimsAtItem(pos, player)) {
            return 1.0F;
        }
        return super.getDestroyProgress(state, player, world, pos);
    }

    /** 挖掉板上的东西：掉出来，砧板留着（0.1.2-alpha 后用户报：挖板上的食物把砧板也打掉了）。 */
    public static void breakItem(Level world, BlockPos pos, CuttingBoardBlockEntity board, Player player) {
        ItemStack item = board.item();
        if (item.isEmpty()) {
            return;
        }
        board.setItem(ItemStack.EMPTY);
        if (!player.isCreative()) {
            Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + TOP + 0.1, pos.getZ() + 0.5, item);
        }
        world.playSound((Player) null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    /** 空手右键：把砧板上的东西拿回来。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) || board.item().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            player.getInventory().placeItemBackInInventory(board.item());
            board.setItem(ItemStack.EMPTY);
        }
        return InteractionResult.SUCCESS;
    }

    /** 挖掉时板上的东西掉出来。 */

}