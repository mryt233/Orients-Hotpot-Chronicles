// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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

    public BowlBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super("bowl", PlateSize.BOWL, MapColor.COLOR_BROWN, SoundType.WOOD, properties);
        registerDefaultState(defaultBlockState().setValue(SOUP, SoupVisual.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SOUP);
    }

    // mryt233 改：碗的状态、回收物品与蘸料交互收口到 TableBlock 接口。
    @Override
    public BlockState modelState(PlateHolderBlockEntity.Placed placed) {
        BlockState state = super.modelState(placed);
        if (placed.self.getItem() instanceof SoupBowlItem bowl) {
            SoupDef def = bowl.soupDef(placed.self);
            String raw = def == null ? bowl.rawSoupId(placed.self) : null;
            SoupVisual visual = def != null ? SoupVisual.fromDef(def)
                : (raw != null && !raw.isEmpty() ? SoupVisual.OTHER : SoupVisual.NONE);
            state = state.setValue(SOUP, visual);
        }
        return state;
    }

    @Override
    public ItemStack toTableItem(PlateHolderBlockEntity.Placed placed) {
        if (!placed.self.isEmpty()) {
            return placed.self.copy();
        }
        return TableBlock.containerItem(Items.BOWL, placed);
    }

    @Override
    public UseOutcome useOnPlaced(PlateHolderBlockEntity.Placed target, Level world, BlockPos pos,
                                  PlateHolderBlockEntity be, Player player, InteractionHand hand, ItemStack stack) {
        // 筷子夹着菜点桌上的蘸料碗 = 蘸一口，配料记在菜上，吃的时候生效
        if (stack.is(HotPotContent.CHOPSTICKS) && target.self.getItem() instanceof DippingSauceItem) {
            if (!world.isClientSide()) {
                ItemStack held = Chopsticks.held(stack);
                if (held.isEmpty() || !held.has(DataComponents.FOOD)) {
                    player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.dip_need_food")));
                } else {
                    ItemStack dipped = held.copy();
                    ItemStack left = DippingSauceItem.dipInto(target.self, dipped);
                    // 蘸完最后一口变回空碗：桌上这只碗变成能装菜的普通碗
                    target.self = left.getItem() instanceof DippingSauceItem ? left : ItemStack.EMPTY;
                    Chopsticks.hold(stack, dipped);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.HONEY_BLOCK_STEP, SoundSource.BLOCKS, 0.4F, 1.4F);
                }
            }
            return UseOutcome.SUCCESS;
        }
        return UseOutcome.PASS;
    }
}
