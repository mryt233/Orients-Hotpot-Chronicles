package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;

/**
 * 摆在桌上的原版碗（r99，用户要求）：和盘子一样不单独占方块，摆在盘子格里；这个方块只用来带模型（空碗 / 盛着哪种汤）。
 * 没有自己的物品：拿起来是原版的碗（装菜时带 container 组件）或本模组的汤碗。
 *
 */
public class BowlBlock extends PlateBlock {
    public static final EnumProperty<SoupVisual> SOUP = EnumProperty.of("soup", SoupVisual.class);

    public BowlBlock() {
        super("bowl", PlateSize.BOWL, MapColor.BROWN, BlockSoundGroup.WOOD);
        setDefaultState(getDefaultState().with(SOUP, SoupVisual.NONE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(SOUP);
    }
}
