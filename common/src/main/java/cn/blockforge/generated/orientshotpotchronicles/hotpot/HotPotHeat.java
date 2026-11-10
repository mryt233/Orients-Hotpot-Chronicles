package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.world.level.block.Block;

import cn.blockforge.orient.framework.game.Hooks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 「能给锅加热的方块」——联动接口。
 *
 * <p>本模组的灶台不走这里（灶台直接看自己的火）。这里决定的是：独立锅摆在别的方块上时算不算热锅。
 * 三种接入方式：</p>
 * <ul>
 *   <li>方块标签 {@code orients_hotpot_chronicles:heat_sources}（数据包可加）。带 {@code lit} 属性的方块只有点着时才算。</li>
 *   <li>代码注册 {@link #register(HeatSource)}：例如以后接农夫乐事（Farmer's Delight）的炉灶，判断它是否在烧。</li>
 *   <li>mryt233 改（2026-11-10，r147）：带优先级的 {@link #register(int, HeatSource)}，返回可注销的句柄。</li>
 * </ul>
 * <p>用到它的地方：冷锅吃了可能反胃；r102 起独立锅摆在热源上也能煮（篝火、灵魂篝火在标签里，火力同灶台烧煤）、
 * 沸腾、给周围的人生命恢复。</p>
 *
 * <p>mryt233 改（2026-11-10，r147）：热源登记表改成框架的 {@link Hooks} 容器——
 * 按优先级排序、能注销、单个回调抛异常只记录不连累别人。以前这里是 {@code ArrayList}，
 * 一个第三方热源判断抛异常整口锅的加热判断都会炸掉。</p>
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

    /**
     * mryt233 改（2026-11-10，r147）：带优先级的热源登记（数字越大越先问），返回可注销的句柄。
     * 登记时机：启动期（模组初始化 / 兼容模块 setup）。标签 {@link #HEAT_SOURCES} 仍是最省事的接入方式。
     */
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
                // mryt233 改（2026-11-10，r147）：单个热源判断出错不再连累别的热源（以前直接抛给调用方）
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
