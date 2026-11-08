package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * 高灶台的「上半截」：看不见，占住高灶台上面那一格。
 *
 * <p>高灶台的锅有一大半伸进了上面一格，而 MC 判断准星点到什么是一格一格查的：站着往锅里看，
 * 视线常常只穿过上面那一格（原来是空气）就离开了，根本查不到灶台，倒汤、放菜都点不到（r83 的 bug）。
 * 这一格放上这个方块后，它的形状就是灶台伸上来的那部分，点它的所有操作都原样转给下面的灶台（准星位置不变），
 * 挖它就是挖灶台（对着锅挖则只挖锅，见 HotPotContent 的破坏事件）。下面不是高灶台了它自己消失。</p>
 *
 * 作者：mryt233 */
public class StoveTopBlock extends Block implements PartOutline {
    /**
     * 下面那台高灶台的形状类别，记在自己的方块状态里（mryt233 2026-11-01）。
     * <p>以前形状是现读下面方块算的，还开了 dynamicShape：光照线程算挡光时会调 getShape，
     * 用 ServerLevel 读下面方块要等区块到 FULL，而区块正在等光照算完——服务端整服卡死（服主 mkperf + spark 证实）。
     * 现在形状只看自己的状态、由原版按状态缓存，光照线程碰不到世界。状态在放置、下面方块变化（updateShape 的
     * neighborState）和灶台每秒补顶时更新，都在主线程。</p>
     */
    public enum Below implements StringRepresentable {
        NONE("none"), EMPTY("empty"), POT_X("pot_x"), POT_Z("pot_z");

        private final String name;

        Below(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static Below of(BlockState stove) {
            if (!(stove.getBlock() instanceof StoveBlock s) || s.lift() <= 0) {
                return NONE;
            }
            if (!stove.getValue(StoveBlock.POT).hasPot()) {
                return EMPTY;
            }
            return stove.getValue(StoveBlock.POT_AXIS) == Direction.Axis.Z ? POT_Z : POT_X;
        }
    }

    public static final EnumProperty<Below> BELOW = EnumProperty.create("below", Below.class);
    /** 各类别的形状：下面灶台的形状往下挪一格（只有伸进这一格的部分有效）。 */
    private static final VoxelShape[] SHAPES = new VoxelShape[Below.values().length];

    static {
        SHAPES[Below.NONE.ordinal()] = Shapes.empty();
        SHAPES[Below.EMPTY.ordinal()] = StoveBlock.shapeFor(StoveBlock.TALL_LIFT, false, Direction.Axis.X).move(0, -1, 0);
        SHAPES[Below.POT_X.ordinal()] = StoveBlock.shapeFor(StoveBlock.TALL_LIFT, true, Direction.Axis.X).move(0, -1, 0);
        SHAPES[Below.POT_Z.ordinal()] = StoveBlock.shapeFor(StoveBlock.TALL_LIFT, true, Direction.Axis.Z).move(0, -1, 0);
    }

    /**
     * mryt233 改（2026-11-05）：硬度 2.0、抗性 6.0，与下面的高灶台一致；不强制工具（照 {@link StoveBlock}）。
     * 挖掘工具见 data/minecraft/tags/block/mineable/pickaxe.json，上半截自己的条目就是为它加的：
     * 玩家对着上半截举镐时，挖快了还是慢由这个方块自己所在的标签决定。
     */
    public StoveTopBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .strength(2.0F, 6.0F)
            .sound(SoundType.STONE)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(BELOW, Below.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BELOW);
    }

    /** 给这台灶台配的上半截状态。 */
    public static BlockState mirroring(BlockState stove) {
        return HotPotContent.STOVE_TOP.defaultBlockState().setValue(BELOW, Below.of(stove));
    }

    private static boolean tallStoveBelow(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.below()).getBlock() instanceof StoveBlock stove && stove.lift() > 0;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 形状只看自己的状态，绝不读世界（见 {@link Below}）。 */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BELOW).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BELOW).ordinal()];
    }

    /** 选中框：照下面灶台的规则（锅 / 灶身分开框），挪到这一格的坐标。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        BlockState below = world.getBlockState(pos.below());
        if (below.getBlock() instanceof StoveBlock stove) {
            VoxelShape part = stove.aimedPart(below, world, pos.below(), player);
            return part == null ? null : part.move(0, -1, 0);
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return tallStoveBelow(world, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction != Direction.DOWN) {
            return state;
        }
        // 下面变了：不是高灶台就消失，否则照新的锅状态更新形状（用传进来的 neighborState，不再读世界）
        Below below = Below.of(neighborState);
        return below == Below.NONE ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state.setValue(BELOW, below);
    }

    /** 准星位置不变，只把"点到的方块"换成下面的灶台。 */
    private static BlockHitResult toStove(BlockPos pos, BlockHitResult hit) {
        return new BlockHitResult(hit.getLocation(), hit.getDirection(), pos.below(), hit.isInside());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.below());
        if (!(below.getBlock() instanceof StoveBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return below.useItemOn(stack, world, player, hand, toStove(pos, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.below());
        if (!(below.getBlock() instanceof StoveBlock)) {
            return InteractionResult.PASS;
        }
        return below.useWithoutItem(world, player, toStove(pos, hit));
    }

    /** 挖的快慢：对着锅按锅算，否则按灶台算。 */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        if (!(below.getBlock() instanceof StoveBlock)) {
            return super.getDestroyProgress(state, player, world, pos);
        }
        HotPotMaterial material = below.getValue(StoveBlock.POT).material();
        if (material != null && StoveBlock.aimsAtPot(below, pos.below(), player)) {
            return HotPotContent.blockFor(material).defaultBlockState().getDestroyProgress(player, world, pos.below());
        }
        return below.getDestroyProgress(player, world, pos.below());
    }

    /** 挖上半截 = 挖整个高灶台（工具对才掉灶台本身；锅、汤、燃料照灶台自己的规则掉）。 */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        BlockState below = world.getBlockState(pos.below());
        if (!world.isClientSide && below.getBlock() instanceof StoveBlock) {
            boolean drop = !player.isCreative() && player.hasCorrectToolForDrops(below);
            if (player.isCreative()) {
                below.getBlock().playerWillDestroy(world, pos.below(), below, player);
            }
            world.destroyBlock(pos.below(), drop, player);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    /** mryt233 改：上半截也显示准星所指的锅名或下方高灶台本名。 */
    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        net.minecraft.network.chat.MutableComponent name = StoveBlock.aimedName(PlateHolderBlock.pickPlayer.get());
        return name == null ? super.getName() : name;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        BlockState below = world.getBlockState(pos.below());
        return below.getBlock().getCloneItemStack(world, pos.below(), below);   // 对着锅复制锅（同 StoveBlock）
    }
}
