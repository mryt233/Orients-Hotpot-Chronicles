// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * 一种盘子（材质 + 大小）。它本身不会作为方块放进世界：盘子都摆在 {@link PlateHolderBlock} 里，
 * 这个方块只用来带模型（物品图标、盘子格渲染时画哪个模型）、声音和名字。
 * AXIS 只是给渲染选模型方向（长边沿 x 或 z）。
 */
public class PlateBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    private final String material;
    private final PlateSize size;

    public PlateBlock(String material, PlateSize size, MapColor color, BlockSoundGroup sounds) {
        super(AbstractBlock.Settings.create()
            .mapColor(color)
            .strength(0.3F)
            .sounds(sounds)
            .nonOpaque());
        this.material = material;
        this.size = size;
        setDefaultState(getDefaultState().with(AXIS, Direction.Axis.X));
    }

    public String material() {
        return material;
    }

    public PlateSize size() {
        return size;
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
