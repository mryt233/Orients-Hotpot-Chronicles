package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * 蘸料台的「上半截」（2026-10-23 用户：1.5 格高肯定占 2 格）：看不见，占住蘸料台头顶那一格，
 * 别的方块放不进来、不会和台子穿插。做法同高灶台的 StoveTopBlock：形状 = 下面蘸料台的形状下移一格，
 * 点它（打蘸料、开补货界面）原样转给下面的蘸料台，挖它 = 挖蘸料台；下面不是蘸料台了它自己消失。
 *
 */
public class SauceStationTopBlock extends net.minecraft.block.Block implements PartOutline {
    public SauceStationTopBlock() {
        super(AbstractBlock.Settings.create()
            .mapColor(net.minecraft.block.MapColor.OAK_TAN)
            .strength(2.0F, 3.0F)
            .sounds(net.minecraft.sound.BlockSoundGroup.WOOD)
            .nonOpaque()
            .dynamicBounds()
            .dropsNothing()
            .pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK));
    }

    private static boolean stationBelow(BlockView world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() instanceof SauceStationBlock;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    private static VoxelShape shapeOf(BlockView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof SauceStationBlock)) {
            return VoxelShapes.empty();
        }
        return below.getOutlineShape(world, pos.down()).offset(0, -1, 0);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(world, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockState below = world.getBlockState(pos.down());
        return below.getBlock() instanceof SauceStationBlock
            ? below.getCollisionShape(world, pos.down()).offset(0, -1, 0) : VoxelShapes.empty();
    }

    /** 选中框照下面蘸料台的规则（只框对着的碗），挪到这一格的坐标。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockView world, BlockPos pos, PlayerEntity player) {
        BlockState below = world.getBlockState(pos.down());
        if (below.getBlock() instanceof SauceStationBlock station) {
            VoxelShape part = station.aimedPart(below, world, pos.down(), player);
            return part == null ? null : part.offset(0, -1, 0);
        }
        return null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return stationBelow(world, pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !stationBelow(world, pos)
            ? net.minecraft.block.Blocks.AIR.getDefaultState() : state;
    }

    /** 准星位置不变，只把「点到的方块」换成下面的蘸料台（SauceStationBlock.aimed 按准星位置算碗）。 */
    private static BlockHitResult toStation(BlockPos pos, BlockHitResult hit) {
        return new BlockHitResult(hit.getPos(), hit.getSide(), pos.down(), hit.isInsideBlock());
    }

    protected ItemActionResult useWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof SauceStationBlock)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ((SauceStationBlock) below.getBlock()).useWithItem(stack, below, world, pos.down(), player, hand, toStation(pos, hit));
    }

    protected ActionResult useWithoutItem(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof SauceStationBlock)) {
            return ActionResult.PASS;
        }
        return ((SauceStationBlock) below.getBlock()).useWithoutItem(below, world, pos.down(), player, toStation(pos, hit));
    }

    /** 挖的快慢按下面的蘸料台算（木头的用斧、铁的用镐）。 */
    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        return below.getBlock() instanceof SauceStationBlock
            ? below.calcBlockBreakingDelta(player, world, pos.down())
            : super.calcBlockBreakingDelta(state, player, world, pos);
    }

    /** 挖上半截 = 挖整个蘸料台（工具对才掉蘸料台，格子里的原料照常掉出来）。 */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockState below = world.getBlockState(pos.down());
        if (!world.isClient && below.getBlock() instanceof SauceStationBlock) {
            boolean drop = !player.isCreative() && player.canHarvest(below);
            world.breakBlock(pos.down(), drop, player);
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public ItemStack getPickStack(net.minecraft.world.BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(world.getBlockState(pos.down()).getBlock());
    }
    // ：沿用工程既有的 1.21 风格右键分发。
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return ItemActionResult.dispatch(state, player, hand,
            () -> useWithItem(player.getStackInHand(hand), state, world, pos, player, hand, hit),
            () -> useWithoutItem(state, world, pos, player, hit));
    }
}
