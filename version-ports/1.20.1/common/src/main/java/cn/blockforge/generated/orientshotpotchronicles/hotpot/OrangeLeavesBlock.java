// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.compat.ItemInteractionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 橙子树树叶：0=绿叶、1=开花、2=青果、3=成熟挂果（1.20.1 适配）。
 * <p>保留原版树叶凋零判断，支持自然结实、骨粉催熟与空手/普通右键直接采摘橙子；
 * 可让玩家与生物穿行穿过并施加原版蜘蛛网 1/5 减速幅度。</p>
 * 作者：mryt233
 */
public class OrangeLeavesBlock extends LeavesBlock implements BonemealableBlock, ItemInteractionBlock {
    public static final int MAX_STAGE = 3;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, MAX_STAGE);
    public static final double COBWEB_XZ_FACTOR = 0.25D;
    public static final double COBWEB_Y_FACTOR = 0.05D;
    /** 蜘蛛网减速幅度 (1 - 0.25 = 0.75, 1 - 0.05 = 0.95) 的 1/5：水平保留 0.85，垂直保留 0.81。 */
    // mryt233 改（2026-11-22）：由 1/3 下调为 1/5，穿叶阻滞感更轻快。
    public static final Vec3 SLOWDOWN_MULTIPLIER = new Vec3(
        1.0D - (1.0D - COBWEB_XZ_FACTOR) / 5.0D,
        1.0D - (1.0D - COBWEB_Y_FACTOR) / 5.0D,
        1.0D - (1.0D - COBWEB_XZ_FACTOR) / 5.0D
    );

    public OrangeLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
            .setValue(DISTANCE, DECAY_DISTANCE)
            .setValue(PERSISTENT, false)
            .setValue(WATERLOGGED, false)
            .setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ItemEntity)) {
            entity.makeStuckInBlock(state, SLOWDOWN_MULTIPLIER);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state) || state.getValue(STAGE) < MAX_STAGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.decaying(state)) {
            super.randomTick(state, level, pos, random);
            return;
        }
        float rate = Config.cropsGrowthRate();
        int stage = state.getValue(STAGE);
        if (rate > 0F && stage < MAX_STAGE && level.getRawBrightness(pos.above(), 0) >= 9
            && random.nextFloat() < Math.min(1F, 0.16F * rate)) {
            BlockState next = state.setValue(STAGE, stage + 1);
            level.setBlock(pos, next, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(next));
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return Config.cropsBonemealEnabled() && state.getValue(STAGE) < MAX_STAGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int nextStage = Math.min(MAX_STAGE, state.getValue(STAGE) + 1);
        level.setBlock(pos, state.setValue(STAGE, nextStage), 2);
    }

    @Override
    public InteractionResult compatUseWithoutItem(BlockState state, Level level, BlockPos pos,
                                                    Player player, BlockHitResult hit) {
        if (state.getValue(STAGE) < MAX_STAGE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int count = 1 + (level.random.nextFloat() < 0.25F ? 1 : 0);
            Block.popResourceFromFace(level, pos, hit.getDirection(), new ItemStack(HotPotContent.ORANGE, count));
            BlockState next = state.setValue(STAGE, 0);
            level.setBlock(pos, next, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** mryt233 改：1.20.1 的单回调交互入口。 */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        return dispatchUse(state, world, pos, player, hand, hit);
    }
}
