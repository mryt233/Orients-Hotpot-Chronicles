// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.properties.EnumProperty;
import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * 蘸料台（r103，用户定：11 种原版木头各一个 + 铁的）：台面上 3×3 九个白瓷小碗，一个碗一种调料。
 * 拿碗（或一碗没打完的蘸料）右键某个小碗 = 往碗里加一份那种调料；别的情况（空手、拿原料等）右键打开补货界面。
 * 2026-10-23 用户改成阶梯式（照自助调料台照片，调整）：1.5 格高，三层台阶前低后高，每层一横排 3 个碗；
 * 有朝向 FACING（正面朝放的人），row 0 = 最后最高那层，col 0 = 站在正面看最左边。和 tools/gen_sauce_station.py 一致。
 * 2026-10-23 用户又改：9 个碗不固定装哪种，空碗放什么原料就是什么（存货见 SauceStationBlockEntity）；
 * 方块状态只剩朝向，碗里的调料面由 client/SauceStationRenderer 按方块实体画（原来的 FILLED 属性去掉了）。
 * 作者：mryt233 */
public class SauceStationBlock extends Block implements PartOutline, EntityBlock {
    public static final EnumProperty<net.minecraft.core.Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 三层台面高度（像素，前 → 后），同 gen_sauce_station.py 的 TIER_TOPS。 */
    public static final double[] TIER_TOPS = {11, 15, 19};   // 0.1.2-alpha 后用户：整体降 3 像素（一个活板门高），调整
    public static final double BOWL = 5, BOWL_H = 2.5, WALL = 0.75;   // 小碗（a 款直壁碗缩小版），同 gen_sauce_station.py；SauceStationRenderer 也用
    /** 按 FACING.get2DDataValue() 排：选中框（台阶 + 小碗）、碰撞（只有台阶）。 */
    private static final VoxelShape[] OUTLINE = new VoxelShape[4];
    private static final VoxelShape[] COLLISION = new VoxelShape[4];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape steps = Shapes.empty();
            VoxelShape bowls = Shapes.empty();
            for (int tier = 0; tier < 3; tier++) {
                steps = Shapes.or(steps, rotated(facing, 0, 0, 16 * tier / 3.0, 16, TIER_TOPS[tier], 16 * (tier + 1) / 3.0));
            }
            for (int i = 0; i < 9; i++) {
                int tier = 2 - i / 3, col = i % 3;
                double cx = 16 - 16 / 3.0 * (col + 0.5), cz = 16 / 3.0 * (tier + 0.5);
                bowls = Shapes.or(bowls, rotated(facing, cx - BOWL / 2, TIER_TOPS[tier], cz - BOWL / 2,
                    cx + BOWL / 2, TIER_TOPS[tier] + BOWL_H, cz + BOWL / 2));
            }
            COLLISION[facing.get2DDataValue()] = steps.optimize();
            OUTLINE[facing.get2DDataValue()] = Shapes.or(steps, bowls).optimize();
        }
    }

    /** 按朝向的选中框 / 碰撞形状（上半截按自己记的朝向取用，不读世界；mryt233 2026-11-01）。 */
    static VoxelShape outline(Direction facing) {
        return OUTLINE[facing.get2DDataValue()];
    }

    static VoxelShape collision(Direction facing) {
        return COLLISION[facing.get2DDataValue()];
    }

    /** 模型坐标（正面朝北）的长方块转到 facing 朝向。 */
    private static VoxelShape rotated(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        double[] a = toWorld(facing, x0, z0), b = toWorld(facing, x1, z1);
        return Block.box(Math.min(a[0], b[0]), y0, Math.min(a[1], b[1]), Math.max(a[0], b[0]), y1, Math.max(a[1], b[1]));
    }

    private static double[] toWorld(Direction facing, double x, double z) {
        return switch (facing) {
            case EAST -> new double[]{16 - z, x};
            case SOUTH -> new double[]{16 - x, 16 - z};
            case WEST -> new double[]{z, 16 - x};
            default -> new double[]{x, z};
        };
    }

    /**
     * 蘸料台的方块属性（mryt233 改，2026-11-05）：以前整份复制原版方块，铁蘸料台照铁块就成了
     * 硬度 5.0 且要求正确工具，空手挖极慢；挖掘工具见 data/minecraft/tags/block/mineable/。
     *
     * <p>现在木头照木板（2.0 / 3.0，木头声音），铁的照石头类（2.0 / 6.0，金属声音），都保留那种材质的
     * 地图颜色和踩上去的声音，也都不强制工具——蘸料台里有玩家存的原料，空手挖也要正常掉出来
     * （掉落见 {@link #onRemove}）。</p>
     */
    public static BlockBehaviour.Properties settingsFor(Block like, boolean iron) {
        return BlockBehaviour.Properties.of()
            .mapColor(like.defaultMapColor())
            .instrument(iron ? net.minecraft.world.level.block.state.properties.NoteBlockInstrument.IRON_XYLOPHONE
                : net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS)
            .strength(2.0F, iron ? 6.0F : 3.0F)
            .sound(like.defaultBlockState().getSoundType())
            .noOcclusion();
    }

    public SauceStationBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SauceStationBlockEntity(pos, state);
    }

    /** 1.5 格高占两格（用户）：头顶那格要能放下「上半截」SauceStationTopBlock，否则放不下。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (!ctx.getLevel().getBlockState(ctx.getClickedPos().above()).canBeReplaced(ctx)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
                         ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        placeTop(world, pos);
    }

    /** 上面是空的就补上「上半截」；已有的上半截记的朝向不对就纠正（旧存档没有这个状态，mryt233 2026-11-01）。 */
    static void placeTop(Level world, BlockPos pos) {
        if (world.isClientSide()) {
            return;
        }
        BlockState above = world.getBlockState(pos.above());
        BlockState want = SauceStationTopBlock.mirroring(world.getBlockState(pos));
        if (above.is(HotPotContent.SAUCE_STATION_TOP)) {
            if (above != want) {
                world.setBlock(pos.above(), want, Block.UPDATE_CLIENTS);
            }
        } else if (above.canBeReplaced()) {
            world.setBlock(pos.above(), want, Block.UPDATE_ALL);
        }
    }

    /** 服务端每秒检查一次上半截（同高灶台的补顶），旧存档里的上半截靠这里补上朝向。 */
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level world, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (world.isClientSide() || type != HotPotContent.SAUCE_STATION_BE) {
            return null;
        }
        return (w, p, s, be) -> {
            if (w.getGameTime() % 20L == 0L) {
                placeTop(w, p);
            }
        };
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
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINE[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return COLLISION[state.getValue(FACING).get2DDataValue()];
    }

    /** 第 i 个小碗的选中框（按朝向转好）。 */
    private static VoxelShape bowlShape(Direction facing, int i) {
        int tier = 2 - i / 3, col = i % 3;
        double cx = 16 - 16 / 3.0 * (col + 0.5), cz = 16 / 3.0 * (tier + 0.5);
        return rotated(facing, cx - BOWL / 2, TIER_TOPS[tier], cz - BOWL / 2, cx + BOWL / 2, TIER_TOPS[tier] + BOWL_H, cz + BOWL / 2);
    }

    /**
     * 选中框只框准星对着的那个小碗；对着台阶就框台阶（0.1.2-alpha 后用户：判定显示成了一整个，要指哪个碗框哪个碗）。
     * 头顶那格的 SauceStationTopBlock 也走这里。
     */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        net.minecraft.world.phys.HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult b) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return null;
        }
        int bowl = aimed(state, pos, b);
        Direction facing = state.getValue(FACING);
        return bowl < 0 ? COLLISION[facing.get2DDataValue()] : bowlShape(facing, bowl);
    }

    /** 点到的是第几个小碗（0..8，从最上层到最下层、每层从左到右）：先把点往方块里推一点（点在台阶立面上算后面那层），转回模型坐标，按进深分层、按左右分列；
     * 点在某层台面以下（台身立面）返回 -1。 */
    public static int aimed(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
        Vec3 p = hit.getLocation().subtract(n.scale(0.001)).subtract(Vec3.atLowerCornerOf(pos));
        double wx = p.x, wz = p.z, lx, lz;
        switch (state.getValue(FACING)) {
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

    /** mryt233 改：名称与图标只显示所瞄小碗中仍有存货的调料。 */
    private static Seasoning aimedSeasoning(BlockState state, LevelReader world, BlockPos pos, BlockHitResult hit) {
        int bowl = aimed(state, pos, hit);
        if (bowl >= 0 && world.getBlockEntity(pos) instanceof SauceStationBlockEntity be
            && be.available(bowl) > 0) {
            return be.kind(bowl);
        }
        return null;
    }

    static net.minecraft.network.chat.MutableComponent aimedName(Player player) {
        if (player != null && player.level() != null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = blockHit.getBlockPos();
                BlockPos stationPos = player.level().getBlockState(hitPos).is(HotPotContent.SAUCE_STATION_TOP)
                    ? hitPos.below() : hitPos;
                BlockState stationState = player.level().getBlockState(stationPos);
                if (stationState.getBlock() instanceof SauceStationBlock) {
                    net.minecraft.network.chat.MutableComponent name =
                        Component.translatable(stationState.getBlock().getDescriptionId());
                    Seasoning seasoning = aimedSeasoning(stationState, player.level(), stationPos, blockHit);
                    return seasoning == null ? name
                        : name.append(" · ").append(Component.translatable(seasoning.nameKey()));
                }
            }
        }
        return null;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        net.minecraft.network.chat.MutableComponent name = aimedName(PlateHolderBlock.pickPlayer.get());
        return name == null ? super.getName() : name;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        Player player = PlateHolderBlock.pickPlayer.get();
        if (player != null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = blockHit.getBlockPos();
                if (hitPos.equals(pos)
                    || (hitPos.equals(pos.above()) && world.getBlockState(hitPos).is(HotPotContent.SAUCE_STATION_TOP))) {
                    Seasoning seasoning = aimedSeasoning(state, world, pos, blockHit);
                    if (seasoning != null && !seasoning.raws().isEmpty()) {
                        return new ItemStack(seasoning.raw());
                    }
                }
            }
        }
        return new ItemStack(state.getBlock());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.is(Items.BOWL) || stack.getItem() instanceof DippingSauceItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int bowl = aimed(state, pos, hit);
        if (bowl < 0 || !(world.getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!world.isClientSide()) {
            be.serve(player, hand, bowl);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // 2026-10-23 用户：右键先看总览（client/SauceOverviewScreen，3×3 和台上的碗一一对应），点「补充蘸料台」再进补货界面
        if (world.isClientSide() && world.getBlockEntity(pos) instanceof SauceStationBlockEntity) {
            // mryt233 改（2026-11-07，#129）：总览标题固定用台名（描述键），不跟着准星上的调料名变
            openOverview.accept(pos, Component.translatable(this.getDescriptionId()));
        }
        return InteractionResult.SUCCESS;
    }

    /** 客户端打开总览界面（HotPotClient 填上；服务端什么也不做）。 */
    public static java.util.function.BiConsumer<BlockPos, net.minecraft.network.chat.Component> openOverview = (pos, name) -> {
    };

    /** 挖掉时格子里的原料掉出来（已经化开的份数就没了）。 */

}
