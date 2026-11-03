package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 「能给锅加热的方块」——联动接口。
 *
 * <p>本模组的灶台不走这里（灶台直接看自己的火）。这里决定的是：独立锅摆在别的方块上时算不算热锅。
 * 两种接入方式：</p>
 * <ul>
 *   <li>方块标签 {@code orients_hotpot_chronicles:heat_sources}（数据包可加）。带 {@code lit} 属性的方块只有点着时才算。</li>
 *   <li>代码注册 {@link #register(HeatSource)}：例如以后接农夫乐事（Farmer's Delight）的炉灶，判断它是否在烧。</li>
 * </ul>
 * <p>用到它的地方：冷锅吃了可能反胃；r102 起独立锅摆在热源上也能煮（篝火、灵魂篝火在标签里，火力同灶台烧煤）、
 * 沸腾、给周围的人生命恢复。</p>
 */
public final class HotPotHeat {
    public static final TagKey<net.minecraft.world.level.block.Block> HEAT_SOURCES =
        TagKey.create(Registries.BLOCK, HotPot.id("heat_sources"));

    /** 判断某个位置的方块此刻能不能给上面的锅加热。 */
    @FunctionalInterface
    public interface HeatSource {
        boolean heats(Level world, BlockPos pos, BlockState state);
    }

    private static final List<HeatSource> SOURCES = new ArrayList<>();

    private HotPotHeat() {
    }

    public static void register(HeatSource source) {
        SOURCES.add(source);
    }

    public static boolean isHeatSource(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(HEAT_SOURCES)
            && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT))) {
            return true;
        }
        for (HeatSource source : SOURCES) {
            if (source.heats(world, pos, state)) {
                return true;
            }
        }
        return false;
    }
}
