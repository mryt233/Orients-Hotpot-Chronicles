//  移植：0.1.4-alpha 冻结底本，保留原作者署名。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;

/**
 * 摆在桌上的玻璃杯（2026-10-25 用户：杯子，碰杯先不做）：和饮料瓶一样借盘子格摆，这个方块只带模型（空杯 / 装着哪种饮料）。
 * 方块状态借用 DrinkBlock 的 empty；kind 用自己的（杯子还能装水、牛奶，2026-10-27）。
 */
public class CupBlock extends PlateBlock {
    public static final net.minecraft.state.property.EnumProperty<DrinkKind> KIND =
        net.minecraft.state.property.EnumProperty.of("kind", DrinkKind.class);

    public CupBlock() {
        super("cup", PlateSize.CUP, MapColor.CLEAR, BlockSoundGroup.GLASS);
        setDefaultState(getDefaultState().with(DrinkBlock.EMPTY, true).with(KIND, DrinkKind.ORANGE_DRINK));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(DrinkBlock.EMPTY, KIND);
    }
}
