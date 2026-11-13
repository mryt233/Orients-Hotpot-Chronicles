// mryt233 改（2026-11-11）：热源登记改用框架 Hooks，保留现有热源判断。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.game.Hooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 「能给锅加热的方块」——联动接口。
 * 作者：mryt233
 */
public final class HotPotHeat {
    public static final TagKey<Block> HEAT_SOURCES =
        TagKey.create(Registries.BLOCK, HotPot.id("heat_sources"));

    /** 判断某个位置的方块此刻能不能给上面的锅加热。 */
    @FunctionalInterface
    public interface HeatSource {
        boolean heats(Level world, BlockPos pos, BlockState state);
    }

    /** 第三方登记的热源（按优先级、可注销、异常隔离）。 */
    private static final Hooks<HeatSource> SOURCES = new Hooks<>();

    private HotPotHeat() {
    }

    /** 登记一个热源（默认优先级 0）。返回可以注销的句柄。 */
    public static Hooks.Registration register(HeatSource source) {
        return SOURCES.add(source);
    }

    /** 带优先级的热源登记（数字越大越先问），返回可注销的句柄。 */
    public static Hooks.Registration register(int priority, HeatSource source) {
        return SOURCES.add(priority, source);
    }

    public static boolean isHeatSource(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(HEAT_SOURCES)
            && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT))) {
            return true;
        }
        for (HeatSource source : SOURCES.all()) {
            try {
                if (source.heats(world, pos, state)) {
                    return true;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 热源判断回调执行失败，已跳过", error);
            }
        }
        return false;
    }

    /** 诊断：当前代码登记的热源数量（标签里的不算）。 */
    public static int hookCount() {
        return SOURCES.size();
    }
}
