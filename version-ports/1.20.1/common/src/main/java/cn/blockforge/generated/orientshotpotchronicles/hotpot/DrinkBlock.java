package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.world.level.block.state.properties.EnumProperty;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 摆在桌上的饮料（r99 橙子饮料，r101 加种类 KIND）：和盘子一样摆在盘子格里，这个方块只带模型（满瓶 / 空瓶）。
 * 摆着的瓶子记着原来那瓶（还剩几口），拿起来原样还给玩家。
 * 作者：mryt233 */
public class DrinkBlock extends PlateBlock {
    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");
    public static final EnumProperty<DrinkKind> KIND =
        EnumProperty.create("kind", DrinkKind.class, DrinkKind::hasBottle);   // 水、牛奶没有瓶装（2026-10-27）

    public DrinkBlock() {
        super("drink", PlateSize.DRINK, MapColor.COLOR_ORANGE, SoundType.DECORATED_POT);
        registerDefaultState(defaultBlockState().setValue(EMPTY, false).setValue(KIND, DrinkKind.ORANGE_DRINK));
    }

    /** mryt233 改（r146d）：两条上桌路径共用；只给有剩余饮料的瓶子叠加水晃声，音高固定。 */
    public static void playPlaceSound(Level world, BlockPos pos, ItemStack stack) {
        boolean empty = stack.getItem() instanceof EmptyBottleItem;
        DrinkKind kind;
        if (stack.getItem() instanceof EmptyBottleItem bottle) {
            kind = bottle.kind();
        } else if (stack.getItem() instanceof DrinkItem drink) {
            kind = drink.kind();
        } else {
            return;
        }
        boolean cola = kind == DrinkKind.ORIENT_COLA;
        world.playSound(null, pos, cola ? SoundEvents.MEDIUM_AMETHYST_BUD_PLACE : SoundEvents.ITEM_FRAME_ADD_ITEM,
            SoundSource.BLOCKS, 1.0F, cola ? 1.3F : 1.2F);
        if (!empty) {
            world.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.3F);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EMPTY, KIND);
    }

    // mryt233 改：饮料瓶桌面高度、状态与不可架筷收口到 TableBlock 接口。
    @Override
    public boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
        return false;
    }

    @Override
    public double tableHeight(PlateHolderBlockEntity.Placed placed) {
        DrinkKind kind = placed.self.getItem() instanceof DrinkItem d ? d.kind()
            : placed.self.getItem() instanceof EmptyBottleItem e ? e.kind() : null;
        return kind == null ? size().height() : kind.bottleHeight();
    }

    @Override
    public BlockState modelState(PlateHolderBlockEntity.Placed placed) {
        boolean empty = placed.self.getItem() instanceof EmptyBottleItem;
        DrinkKind kind = placed.self.getItem() instanceof EmptyBottleItem e ? e.kind()
            : placed.self.getItem() instanceof DrinkItem d ? d.kind() : DrinkKind.ORANGE_DRINK;
        return super.modelState(placed).setValue(EMPTY, empty).setValue(KIND, kind);
    }
}
