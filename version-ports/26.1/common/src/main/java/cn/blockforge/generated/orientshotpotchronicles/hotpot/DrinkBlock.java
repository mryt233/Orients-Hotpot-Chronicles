// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 摆在桌上的饮料（r99 橙子饮料，r101 加种类 KIND）：和盘子一样摆在盘子格里，这个方块只带模型（满瓶 / 空瓶）。
 * 摆着的瓶子记着原来那瓶（还剩几口），拿起来原样还给玩家。
 * 作者：mryt233 */
public class DrinkBlock extends PlateBlock {
    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DrinkKind> KIND =
        net.minecraft.world.level.block.state.properties.EnumProperty.create("kind", DrinkKind.class, DrinkKind::hasBottle);   // 水、牛奶没有瓶装（2026-10-27）

    public DrinkBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super("drink", PlateSize.DRINK, MapColor.COLOR_ORANGE, SoundType.DECORATED_POT, properties);
        registerDefaultState(defaultBlockState().setValue(EMPTY, false).setValue(KIND, DrinkKind.ORANGE_DRINK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EMPTY, KIND);
    }
}
