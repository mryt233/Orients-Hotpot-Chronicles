package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;

/**
 * 摆在桌上的饮料（r99 果橙多，r101 加种类 KIND）：和盘子一样摆在盘子格里，这个方块只带模型（满瓶 / 空瓶）。
 * 摆着的瓶子记着原来那瓶（还剩几口），拿起来原样还给玩家。
 *
 */
public class DrinkBlock extends PlateBlock {
    public static final BooleanProperty EMPTY = BooleanProperty.of("empty");
    public static final net.minecraft.state.property.EnumProperty<DrinkKind> KIND =
        net.minecraft.state.property.EnumProperty.of("kind", DrinkKind.class);

    public DrinkBlock() {
        super("drink", PlateSize.DRINK, MapColor.ORANGE, BlockSoundGroup.DECORATED_POT);
        setDefaultState(getDefaultState().with(EMPTY, false).with(KIND, DrinkKind.GUOCHENGDUO));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(EMPTY, KIND);
    }
}
