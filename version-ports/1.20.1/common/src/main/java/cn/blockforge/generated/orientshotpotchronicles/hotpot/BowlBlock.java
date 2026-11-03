package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 摆在桌上的原版碗（r99，用户要求）：和盘子一样不单独占方块，摆在盘子格里；这个方块只用来带模型（空碗 / 盛着哪种汤）。
 * 没有自己的物品：拿起来是原版的碗（装菜时带 container 组件）或本模组的汤碗。
 * 作者：mryt233 */
public class BowlBlock extends PlateBlock {
    public static final EnumProperty<SoupVisual> SOUP = EnumProperty.create("soup", SoupVisual.class);

    public BowlBlock() {
        super("bowl", PlateSize.BOWL, MapColor.COLOR_BROWN, SoundType.WOOD);
        registerDefaultState(defaultBlockState().setValue(SOUP, SoupVisual.NONE));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SOUP);
    }
}
