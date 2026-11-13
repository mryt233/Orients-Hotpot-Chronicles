package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 锅的客户端表现：热气、火星子、咕嘟声，以及热度缓动（从
 * {@link HotPotBlockEntity#clientTick} 抽出来）。
 * <p>
 * 这里只做「看起来怎么样」，不碰任何库存和烹饪数据；所以可以独立成类，
 * 改特效时不用翻 1400 行的主类。
 * <p>
 * 网页预览 viewer/animations.js 与本类一一对应，改这里要同步改那边。
 * 作者：mryt233 */
public final class HotPotEffects {
    /** 客户端安装白烟外观，公共代码不加载客户端类型。mryt233 改。 */
    @FunctionalInterface
    public interface SmokeEmitter {
        void emit(double x, double y, double z, double vx, double vy, double vz);
    }
    public static SmokeEmitter whiteSmoke = (x, y, z, vx, vy, vz) -> { };

    private HotPotEffects() {
    }

    /**
     * 每客户端 tick 调用一次。
     *
     * @param heat 上一 tick 的缓动热度
     * @return 本 tick 的新热度，调用方存回自己的字段
     */
    public static float tick(HotPotBlockEntity be, Level world, BlockPos pos, BlockState state, float heat) {
        boolean stoveShape = StoveBlock.isStove(state);
        double lift = StoveBlock.liftOf(state);
        boolean lit = be.hasFire();
        boolean soupVisible = be.hasSoup() && be.hasPot();
        heat += ((lit && soupVisible ? 1F : 0F) - heat) * 0.08F;

        // mryt233 改（2026-11-05，r135e）：本机粒子强度 0.0F～1.0F，0 就是整段粒子全关
        float pScale = Config.particleScale();
        // mryt233 改（2026-11-05，r135e）：灶膛口火苗按强度缩放（原 50% 概率）
        if (stoveShape && lit && pScale > 0F && world.random.nextFloat() < 0.5F * pScale) {
            Direction facing = state.getValue(StoveBlock.FACING);
            double side = (world.random.nextDouble() - 0.5) * 0.36;
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.43 + facing.getStepZ() * side;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.43 - facing.getStepX() * side;
            world.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.27, z, 0.0, 0.035, 0.0);
        }
        // 气泡由汤面的沸腾贴图表现（原版 BUBBLE_POP 是蓝色水泡，放在红汤上不对）；
        // 这里只从汤面（与模型同高：灶上 14.32/16、独立 5.6/16）缓缓冒出稀疏的热气。
        // 火星子：从锅和灶口砖墙之间那圈缝（四个面随机）往上冒；没装锅时从整个灶口里冒
        // mryt233 改（2026-11-05，r135e）：锅边火星子按强度缩放（原 35% 概率）
        if (stoveShape && lit && pScale > 0F && world.random.nextFloat() < 0.35F * pScale) {
            double x;
            double z;
            double y;
            if (be.hasPot()) {
                double gap = 1.125 / 16.0;               // 灶口内沿 1/16 与锅身 1.25/16 之间
                double along = (1 + world.random.nextDouble() * 14) / 16.0;
                switch (world.random.nextInt(4)) {
                    case 0 -> { x = along; z = gap; }
                    case 1 -> { x = along; z = 1 - gap; }
                    case 2 -> { x = gap; z = along; }
                    default -> { x = 1 - gap; z = along; }
                }
                y = 11.0 / 16.0 + lift + 0.01;           // 灶台顶面（高灶台整体抬高）
            } else {
                x = (2 + world.random.nextDouble() * 12) / 16.0;
                z = (2 + world.random.nextDouble() * 12) / 16.0;
                y = 8.0 / 16.0 + lift;                   // 锅槽底上方
            }
            world.addParticle(ParticleTypes.SMALL_FLAME,
                pos.getX() + x, pos.getY() + y, pos.getZ() + z,
                (x - 0.5) * 0.01, 0.02 + world.random.nextDouble() * 0.02, (z - 0.5) * 0.01);
        }
        // 大滚时热气又密又急：火越旺，冒得越多、升得越快
        double surface = pos.getY() + lift + (stoveShape ? 14.32 : 5.6) / 16.0;
        if (soupVisible && heat > 0.08F && pScale > 0F) {
            // mryt233 改（2026-11-05，r135e）：汤面热气的两段概率都乘上粒子强度
            int puffs = world.random.nextFloat() < heat * 0.6F * pScale ? 1 + (world.random.nextFloat() < heat * 0.5F * pScale ? 1 : 0) : 0;
            for (int i = 0; i < puffs; i++) {
                double ox = (world.random.nextDouble() - 0.5) * 0.55;
                double oz = (world.random.nextDouble() - 0.5) * 0.55;
                whiteSmoke.emit(
                    pos.getX() + 0.5 + ox, surface + 0.03, pos.getZ() + 0.5 + oz,
                    (world.random.nextDouble() - 0.5) * 0.01, 0.03 + heat * 0.045, (world.random.nextDouble() - 0.5) * 0.01);
            }
        }
        // 咕嘟咕嘟：火越旺越密。客户端必须用不带玩家参数的重载，传 null 玩家在客户端不会出声
        if (soupVisible && heat > 0.3F && world.random.nextInt(Math.max(3, Math.round(16 - 12 * heat))) == 0) {
            world.playLocalSound(pos.getX() + 0.5, surface, pos.getZ() + 0.5,
                SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS,
                0.18F + 0.17F * heat, 0.55F + world.random.nextFloat() * 0.35F, false);
        }
        if (soupVisible && lit && world.random.nextInt(60) == 0) {
            world.playLocalSound(pos.getX() + 0.5, surface, pos.getZ() + 0.5,
                SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                0.25F, 0.8F + world.random.nextFloat() * 0.4F, false);
        }
        return heat;
    }

    /** 独立摆在地上的锅：只有它自己有 BOILING 方块状态，灶上锅的沸腾由灶台表达。 */
    public static void updateStandaloneBoiling(HotPotBlockEntity be, boolean boiling) {
        Level world = be.getLevel();
        if (world == null) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof HotPotBlock && state.getValue(HotPotBlock.BOILING) != boiling) {
            world.setBlock(pos, state.setValue(HotPotBlock.BOILING, boiling), Block.UPDATE_CLIENTS);
        }
    }
}
