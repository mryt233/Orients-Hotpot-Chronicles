package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
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
 * 高灶台的「上半截」：看不见，占住高灶台上面那一格。
 *
 * <p>高灶台的锅有一大半伸进了上面一格，而 MC 判断准星点到什么是一格一格查的：站着往锅里看，
 * 视线常常只穿过上面那一格（原来是空气）就离开了，根本查不到灶台，倒汤、放菜都点不到（r83 的 bug）。
 * 这一格放上这个方块后，它的形状就是灶台伸上来的那部分，点它的所有操作都原样转给下面的灶台（准星位置不变），
 * 挖它就是挖灶台（对着锅挖则只挖锅，见 HotPotContent 的破坏事件）。下面不是高灶台了它自己消失。</p>
 *
 * 作者：Claude
 */
public class StoveTopBlock extends Block {
    public StoveTopBlock() {
        super(AbstractBlock.Settings.create()
            .mapColor(net.minecraft.block.MapColor.RED)
            .strength(2.0F, 6.0F)
            .sounds(net.minecraft.sound.BlockSoundGroup.STONE)
            .nonOpaque()
            .dynamicBounds()
            .dropsNothing()
            .pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK));
    }

    private static boolean tallStoveBelow(BlockView world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() instanceof StoveBlock stove && stove.lift() > 0;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    /** 形状 = 下面灶台的形状往下挪一格（只有伸进这一格的部分在这一格里有效）。 */
    private static VoxelShape shapeOf(BlockView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof StoveBlock)) {
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
        return shapeOf(world, pos);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return tallStoveBelow(world, pos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !tallStoveBelow(world, pos)
            ? net.minecraft.block.Blocks.AIR.getDefaultState() : state;
    }

    /** 准星位置不变，只把"点到的方块"换成下面的灶台。 */
    private static BlockHitResult toStove(BlockPos pos, BlockHitResult hit) {
        return new BlockHitResult(hit.getPos(), hit.getSide(), pos.down(), hit.isInsideBlock());
    }

    /** 1.20.1 移植（Claude 改）：1.20 只有一个右键入口，整个转给下面的灶台。 */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof StoveBlock)) {
            return ActionResult.PASS;
        }
        return below.onUse(world, player, hand, toStove(pos, hit));
    }

    /** 挖的快慢：对着锅按锅算，否则按灶台算。 */
    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof StoveBlock)) {
            return super.calcBlockBreakingDelta(state, player, world, pos);
        }
        HotPotMaterial material = below.get(StoveBlock.POT).material();
        if (material != null && StoveBlock.aimsAtPot(below, pos.down(), player)) {
            return HotPotContent.blockFor(material).getDefaultState().calcBlockBreakingDelta(player, world, pos.down());
        }
        return below.calcBlockBreakingDelta(player, world, pos.down());
    }

    /** 挖上半截 = 挖整个高灶台（工具对才掉灶台本身；锅、汤、燃料照灶台自己的规则掉）。 */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockState below = world.getBlockState(pos.down());
        if (!world.isClient && below.getBlock() instanceof StoveBlock) {
            boolean drop = !player.isCreative() && player.canHarvest(below);
            if (player.isCreative()) {
                below.getBlock().onBreak(world, pos.down(), below, player);
            }
            world.breakBlock(pos.down(), drop, player);
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public ItemStack getPickStack(net.minecraft.world.BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(world.getBlockState(pos.down()).getBlock());
    }
}
