package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 摆在桌上的玻璃杯（2026-10-25 用户：杯子，碰杯先不做）：和饮料瓶一样借盘子格摆，这个方块只带模型（空杯 / 装着哪种饮料）。
 * 方块状态借用 DrinkBlock 的 empty；kind 用自己的（杯子还能装水、牛奶，2026-10-27）。作者：mryt233 * 2026-10-30 Mojang 映射迁移。
 */
public class CupBlock extends PlateBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DrinkKind> KIND =
        net.minecraft.world.level.block.state.properties.EnumProperty.create("kind", DrinkKind.class);

    public CupBlock() {
        super("cup", PlateSize.CUP, MapColor.NONE, SoundType.GLASS);
        registerDefaultState(defaultBlockState().setValue(DrinkBlock.EMPTY, true).setValue(KIND, DrinkKind.ORANGE_DRINK));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DrinkBlock.EMPTY, KIND);
    }
}
