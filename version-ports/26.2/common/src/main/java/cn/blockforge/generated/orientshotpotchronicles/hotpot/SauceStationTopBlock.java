// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
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
 * 蘸料台的「上半截」（2026-10-23 用户：1.5 格高肯定占 2 格）：看不见，占住蘸料台头顶那一格，
 * 别的方块放不进来、不会和台子穿插。做法同高灶台的 StoveTopBlock：形状 = 下面蘸料台的形状下移一格，
 * 点它（打蘸料、开补货界面）原样转给下面的蘸料台，挖它 = 挖蘸料台；下面不是蘸料台了它自己消失。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class SauceStationTopBlock extends TopProxyBlock<SauceStationTopBlock.Below> {
    /**
     * mryt233 改（2026-11-05）：硬度 2.0 与下面蘸料台一致（木 3.0 / 铁 6.0 的爆炸抗性那一半由
     * {@link #getDestroyProgress} 转发给下面的方块算，这里只保底）；不强制工具。
     * 挖它会连下面整个蘸料台一起挖掉（见 {@link #playerWillDestroy}），掉落由下面那块决定。
     * 26.x：注册走框架给的带注册键 Properties。
     */
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

    /**
     * 下面蘸料台的朝向，记在自己的方块状态里（mryt233 2026-11-01）。以前形状现读下面方块、还开了 dynamicShape，
     * 光照线程调 getShape 读世界会和主线程互等，整服卡死（同 {@link StoveTopBlock.Below}）。
     */
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

    public static final EnumProperty<Below> BELOW =
        EnumProperty.create("below", Below.class);

    /** 给这个蘸料台配的上半截状态。 */
    public static BlockState mirroring(BlockState station) {
        return HotPotContent.SAUCE_STATION_TOP.defaultBlockState().setValue(BELOW, Below.of(station));
    }

    /** 各朝向预先算好的形状：下面蘸料台的形状往下挪一格。 */
    private static final VoxelShape[] OUTLINES = new VoxelShape[Below.values().length];
    private static final VoxelShape[] COLLISIONS = new VoxelShape[Below.values().length];

    static {
        for (Below b : Below.values()) {
            OUTLINES[b.ordinal()] = b.facing == null ? Shapes.empty() : SauceStationBlock.outline(b.facing).move(0, -1, 0);
            COLLISIONS[b.ordinal()] = b.facing == null ? Shapes.empty() : SauceStationBlock.collision(b.facing).move(0, -1, 0);
        }
    }

    /** mryt233 改：沿用下方材质名称与同一小碗的调料判定。 */
    @Override
    public MutableComponent getName() {
        MutableComponent name = SauceStationBlock.aimedName(PlateHolderBlock.pickPlayer.get());
        return name == null ? super.getName() : name;
    }

    @Override
    protected EnumProperty<Below> belowProperty() { return BELOW; }

    @Override
    protected Below belowOf(BlockState state) { return Below.of(state); }

    @Override
    protected boolean accepts(BlockState state) { return state.getBlock() instanceof SauceStationBlock; }

    @Override
    protected VoxelShape[] outlines() { return OUTLINES; }

    @Override
    protected VoxelShape[] collisions() { return COLLISIONS; }

    /** 保留原有公开入口，供外部调用。 */
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
