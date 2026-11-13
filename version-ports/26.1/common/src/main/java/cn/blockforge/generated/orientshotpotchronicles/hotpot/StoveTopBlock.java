// mryt233 改：26.1 原版 API；继承 TopProxyBlock 合并重复实现。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 高灶台的「上半截」：看不见，占住高灶台上面那一格。
 * 作者：mryt233
 */
public class StoveTopBlock extends TopProxyBlock<StoveTopBlock.Below> {
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

    public StoveTopBlock(BlockBehaviour.Properties properties) {
        super(properties
            .mapColor(MapColor.COLOR_RED)
            .strength(2.0F, 6.0F)
            .sound(SoundType.STONE)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(BELOW, Below.EMPTY));
    }

    /** 给这台灶台配的上半截状态。 */
    public static BlockState mirroring(BlockState stove) {
        return HotPotContent.STOVE_TOP.defaultBlockState().setValue(BELOW, Below.of(stove));
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

    /** 上半截也显示准星所指的锅名或下方高灶台本名。 */
    @Override
    public MutableComponent getName() {
        MutableComponent name = StoveBlock.aimedName(PlateHolderBlock.pickPlayer.get());
        return name == null ? super.getName() : name;
    }

    @Override
    protected EnumProperty<Below> belowProperty() {
        return BELOW;
    }

    @Override
    protected Below belowOf(BlockState state) {
        return Below.of(state);
    }

    @Override
    protected boolean accepts(BlockState state) {
        return state.getBlock() instanceof StoveBlock;
    }

    @Override
    protected VoxelShape[] outlines() {
        return SHAPES;
    }

    @Override
    protected void beforeDestroyBelow(Level world, BlockPos pos, BlockState below, Player player) {
        if (player.isCreative()) {
            below.getBlock().playerWillDestroy(world, pos, below, player);
        }
    }

    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        return super.aimedPart(state, world, pos, player);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return super.getCloneItemStack(world, pos, state, includeData);
    }
}
