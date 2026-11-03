// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;

/**
 * 一种盘子（材质 + 大小）。它本身不会作为方块放进世界：盘子都摆在 {@link PlateHolderBlock} 里，
 * 这个方块只用来带模型（物品图标、盘子格渲染时画哪个模型）、声音和名字。
 * AXIS 只是给渲染选模型方向（长边沿 x 或 z）。
 * 2026-10-30 Mojang 映射迁移。
 */
public class PlateBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private final String material;
    private final PlateSize size;

    public PlateBlock(String material, PlateSize size, MapColor color, SoundType sounds, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties
            .mapColor(color)
            .strength(0.3F)
            .sound(sounds)
            .noOcclusion());
        this.material = material;
        this.size = size;
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }

    public String material() {
        return material;
    }

    public PlateSize size() {
        return size;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
