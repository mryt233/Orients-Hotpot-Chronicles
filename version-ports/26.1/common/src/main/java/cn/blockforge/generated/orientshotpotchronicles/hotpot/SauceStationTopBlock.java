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
 * 蘸料台的「上半截」：看不见，占住蘸料台头顶那一格。
 * 作者：mryt233
 */
public class SauceStationTopBlock extends TopProxyBlock<SauceStationTopBlock.Below> {
    public SauceStationTopBlock(BlockBehaviour.Properties properties) {
        super(properties
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 6.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(BELOW, Below.NORTH));
    }

    public enum Below implements StringRepresentable {
        NONE("none", null), NORTH("north", Direction.NORTH), EAST("east", Direction.EAST),
        SOUTH("south", Direction.SOUTH), WEST("west", Direction.WEST);

        private final String name;
        private final Direction facing;

        Below(String name, Direction facing) {
            this.name = name;
            this.facing = facing;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static Below of(BlockState station) {
            if (!(station.getBlock() instanceof SauceStationBlock)) {
                return NONE;
            }
            return switch (station.getValue(SauceStationBlock.FACING)) {
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                default -> NORTH;
            };
        }
    }

    public static final EnumProperty<Below> BELOW = EnumProperty.create("below", Below.class);

    public static BlockState mirroring(BlockState station) {
        return HotPotContent.SAUCE_STATION_TOP.defaultBlockState().setValue(BELOW, Below.of(station));
    }

    private static final VoxelShape[] OUTLINES = new VoxelShape[Below.values().length];
    private static final VoxelShape[] COLLISIONS = new VoxelShape[Below.values().length];

    static {
        for (Below b : Below.values()) {
            OUTLINES[b.ordinal()] = b.facing == null ? Shapes.empty() : SauceStationBlock.outline(b.facing).move(0, -1, 0);
            COLLISIONS[b.ordinal()] = b.facing == null ? Shapes.empty() : SauceStationBlock.collision(b.facing).move(0, -1, 0);
        }
    }

    /** 沿用下方材质名称与同一小碗的调料判定。 */
    @Override
    public MutableComponent getName() {
        MutableComponent name = SauceStationBlock.aimedName(PlateHolderBlock.pickPlayer.get());
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
        return state.getBlock() instanceof SauceStationBlock;
    }

    @Override
    protected VoxelShape[] outlines() {
        return OUTLINES;
    }

    @Override
    protected VoxelShape[] collisions() {
        return COLLISIONS;
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
