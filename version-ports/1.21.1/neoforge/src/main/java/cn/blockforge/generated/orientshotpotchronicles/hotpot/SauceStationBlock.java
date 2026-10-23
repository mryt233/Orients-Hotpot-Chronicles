// 0.1.2 的 NeoForge 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * 蘸料台（r103，用户定：11 种原版木头各一个 + 铁的）：台面上 3×3 九个白瓷小碗，一个碗一种调料。
 * 拿碗（或一碗没打完的蘸料）右键某个小碗 = 往碗里加一份那种调料；别的情况（空手、拿原料等）右键打开补货界面。
 * 2026-10-23 用户改成阶梯式（照自助调料台照片，Claude 改）：1.5 格高，三层台阶前低后高，每层一横排 3 个碗；
 * 有朝向 FACING（正面朝放的人），row 0 = 最后最高那层，col 0 = 站在正面看最左边。和 tools/gen_sauce_station.py 一致。
 * 2026-10-23 用户又改：9 个碗不固定装哪种，空碗放什么原料就是什么（存货见 SauceStationBlockEntity）；
 * 方块状态只剩朝向，碗里的调料面由 client/SauceStationRenderer 按方块实体画（原来的 FILLED 属性去掉了）。
 * 作者：Claude
 */
public class SauceStationBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /** 三层台面高度（像素，前 → 后），同 gen_sauce_station.py 的 TIER_TOPS。 */
    public static final double[] TIER_TOPS = {14, 18, 22};
    public static final double BOWL = 5, BOWL_H = 2.5, WALL = 0.75;   // 小碗（a 款直壁碗缩小版），同 gen_sauce_station.py；SauceStationRenderer 也用
    /** 按 FACING.getHorizontal() 排：选中框（台阶 + 小碗）、碰撞（只有台阶）。 */
    private static final VoxelShape[] OUTLINE = new VoxelShape[4];
    private static final VoxelShape[] COLLISION = new VoxelShape[4];

    static {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            VoxelShape steps = VoxelShapes.empty();
            VoxelShape bowls = VoxelShapes.empty();
            for (int tier = 0; tier < 3; tier++) {
                steps = VoxelShapes.union(steps, rotated(facing, 0, 0, 16 * tier / 3.0, 16, TIER_TOPS[tier], 16 * (tier + 1) / 3.0));
            }
            for (int i = 0; i < 9; i++) {
                int tier = 2 - i / 3, col = i % 3;
                double cx = 16 - 16 / 3.0 * (col + 0.5), cz = 16 / 3.0 * (tier + 0.5);
                bowls = VoxelShapes.union(bowls, rotated(facing, cx - BOWL / 2, TIER_TOPS[tier], cz - BOWL / 2,
                    cx + BOWL / 2, TIER_TOPS[tier] + BOWL_H, cz + BOWL / 2));
            }
            COLLISION[facing.getHorizontal()] = steps.simplify();
            OUTLINE[facing.getHorizontal()] = VoxelShapes.union(steps, bowls).simplify();
        }
    }

    /** 模型坐标（正面朝北）的长方块转到 facing 朝向。 */
    private static VoxelShape rotated(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        double[] a = toWorld(facing, x0, z0), b = toWorld(facing, x1, z1);
        return Block.createCuboidShape(Math.min(a[0], b[0]), y0, Math.min(a[1], b[1]), Math.max(a[0], b[0]), y1, Math.max(a[1], b[1]));
    }

    private static double[] toWorld(Direction facing, double x, double z) {
        return switch (facing) {
            case EAST -> new double[]{16 - z, x};
            case SOUTH -> new double[]{16 - x, 16 - z};
            case WEST -> new double[]{z, 16 - x};
            default -> new double[]{x, z};
        };
    }

    private final Block planks;

    public SauceStationBlock(AbstractBlock.Settings settings, Block planks) {
        super(settings);
        this.planks = planks;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SauceStationBlockEntity(pos, state);
    }

    /** 1.5 格高占两格（用户）：头顶那格要能放下「上半截」SauceStationTopBlock，否则放不下。 */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (!ctx.getWorld().getBlockState(ctx.getBlockPos().up()).canReplace(ctx)) {
            return null;
        }
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.entity.LivingEntity placer,
                         ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        BlockState above = world.getBlockState(pos.up());
        if (!world.isClient && !above.isOf(HotPotContent.SAUCE_STATION_TOP) && above.isReplaceable()) {
            world.setBlockState(pos.up(), HotPotContent.SAUCE_STATION_TOP.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE[state.get(FACING).getHorizontal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION[state.get(FACING).getHorizontal()];
    }

    /** 点到的是第几个小碗（0..8，从最上层到最下层、每层从左到右）：先把点往方块里推一点（点在台阶立面上算后面那层），转回模型坐标，按进深分层、按左右分列；
     * 点在某层台面以下（台身立面）返回 -1。 */
    public static int aimed(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3d n = Vec3d.of(hit.getSide().getVector());
        Vec3d p = hit.getPos().subtract(n.multiply(0.001)).subtract(Vec3d.of(pos));
        double wx = p.x, wz = p.z, lx, lz;
        switch (state.get(FACING)) {
            case EAST -> { lx = wz; lz = 1 - wx; }
            case SOUTH -> { lx = 1 - wx; lz = 1 - wz; }
            case WEST -> { lx = 1 - wz; lz = wx; }
            default -> { lx = wx; lz = wz; }
        }
        int tier = Math.min(2, Math.max(0, (int) (lz * 3)));
        if (p.y * 16 < TIER_TOPS[tier] - 0.01) {
            return -1;
        }
        int col = Math.min(2, Math.max(0, (int) ((1 - lx) * 3)));
        return (2 - tier) * 3 + col;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(stack.isOf(Items.BOWL) || stack.getItem() instanceof DippingSauceItem)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int bowl = aimed(state, pos, hit);
        if (bowl < 0 || !(world.getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClient) {
            be.serve(player, hand, bowl);
        }
        return ItemActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // 2026-10-23 用户：右键先看总览（client/SauceOverviewScreen，3×3 和台上的碗一一对应），点「补充蘸料台」再进补货界面
        if (world.isClient && world.getBlockEntity(pos) instanceof SauceStationBlockEntity) {
            openOverview.accept(pos, getName());
        }
        return ActionResult.SUCCESS;
    }

    /** 客户端打开总览界面（HotPotClient 填上；服务端什么也不做）。 */
    public static java.util.function.BiConsumer<BlockPos, net.minecraft.text.Text> openOverview = (pos, name) -> {
    };

    /** 挖掉时格子里的原料掉出来（已经化开的份数就没了）。 */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof SauceStationBlockEntity be) {
            ItemScatterer.spawn(world, pos, be.rawInventory());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    // Codex 改：NeoForge 可燃接口；铁、绯红、诡异不燃烧，其余木材原数值5/20。
    @Override
    public boolean isFlammable(BlockState state, BlockView world, BlockPos pos, Direction face) {
        return planks != net.minecraft.block.Blocks.IRON_BLOCK
            && planks != net.minecraft.block.Blocks.CRIMSON_PLANKS && planks != net.minecraft.block.Blocks.WARPED_PLANKS;
    }

    @Override
    public int getFlammability(BlockState state, BlockView world, BlockPos pos, Direction face) {
        return isFlammable(state, world, pos, face) ? 5 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockView world, BlockPos pos, Direction face) {
        return isFlammable(state, world, pos, face) ? 20 : 0;
    }

}
