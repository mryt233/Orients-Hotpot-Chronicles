package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
 * <p>目前只有"冷锅吃了可能反胃"用到它；锅的烹饪仍只认本模组灶台。</p>
 */
public final class HotPotHeat {
    public static final TagKey<net.minecraft.block.Block> HEAT_SOURCES =
        TagKey.of(RegistryKeys.BLOCK, GeneratedMod.id("heat_sources"));

    /** 判断某个位置的方块此刻能不能给上面的锅加热。 */
    @FunctionalInterface
    public interface HeatSource {
        boolean heats(World world, BlockPos pos, BlockState state);
    }

    private static final List<HeatSource> SOURCES = new ArrayList<>();

    private HotPotHeat() {
    }

    public static void register(HeatSource source) {
        SOURCES.add(source);
    }

    public static boolean isHeatSource(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isIn(HEAT_SOURCES)
            && (!state.contains(Properties.LIT) || state.get(Properties.LIT))) {
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
