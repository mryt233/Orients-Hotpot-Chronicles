package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

/**
 * 摆在桌上的玻璃杯（2026-10-25 用户：杯子，碰杯先不做）：和饮料瓶一样借盘子格摆，这个方块只带模型（空杯 / 装着哪种饮料）。
 * 方块状态借用 DrinkBlock 的 empty；kind 用自己的（杯子还能装水、牛奶，2026-10-27）。作者：mryt233 * 2026-10-30 Mojang 映射迁移。
 */
public class CupBlock extends PlateBlock {
    public static final EnumProperty<DrinkKind> KIND =
        EnumProperty.create("kind", DrinkKind.class);

    public CupBlock() {
        super("cup", PlateSize.CUP, MapColor.NONE, SoundType.GLASS);
        registerDefaultState(defaultBlockState().setValue(DrinkBlock.EMPTY, true).setValue(KIND, DrinkKind.ORANGE_DRINK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DrinkBlock.EMPTY, KIND);
    }

    // mryt233 改：玻璃杯桌面状态、倒饮品与碰杯交互收口到 TableBlock 接口。
    @Override
    public boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
        return false;
    }

    @Override
    public BlockState modelState(PlateHolderBlockEntity.Placed placed) {
        DrinkKind kind = placed.self.getItem() instanceof CupItem cup ? cup.kind() : null;
        return super.modelState(placed)
            .setValue(DrinkBlock.EMPTY, kind == null)
            .setValue(KIND, kind == null ? DrinkKind.ORANGE_DRINK : kind);
    }

    @Override
    public UseOutcome useOnPlaced(PlateHolderBlockEntity.Placed target, Level world, BlockPos pos,
                                  PlateHolderBlockEntity be, Player player, InteractionHand hand, ItemStack stack) {
        // 拿饮料瓶右键桌上的空杯 = 倒一杯
        if (stack.getItem() instanceof DrinkItem drink && CupItem.isEmptyCup(target.self)) {
            if (!world.isClientSide) {
                target.self = new ItemStack(HotPotContent.cupFor(drink.kind()));
                player.setItemInHand(hand, DrinkItem.pourOne(stack));
                be.changed();
                world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.1F);
            }
            return UseOutcome.SUCCESS;
        }
        // 拿水桶 / 奶桶右键桌上的空杯 = 倒一杯，桶不会空
        DrinkKind fromBucket = CupItem.bucketKind(stack);
        if (fromBucket != null && CupItem.isEmptyCup(target.self)) {
            if (!world.isClientSide) {
                target.self = new ItemStack(HotPotContent.cupFor(fromBucket));
                be.changed();
                world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            return UseOutcome.SUCCESS;
        }
        // 拿着装满饮品的玻璃杯右键桌上摆着的满杯：跟桌上这杯碰一下
        if (stack.getItem() instanceof CupItem handCup && handCup.kind() != null
            && target.self.getItem() instanceof CupItem tableCup && tableCup.kind() != null) {
            if (!world.isClientSide) {
                Vec3 at3 = Vec3.atCenterOf(pos)
                    .add(0, (PlateHolderBlock.lift(be.getBlockState()) + PlateSize.CUP_H + 1.0) / 16.0, 0);
                CupItem.cheers(world, player, stack, null, target.self, at3);
                be.changed();   // 桌上的杯子也带上 CHEERED，客户端拿到的物品栈才是新的
            }
            return UseOutcome.SUCCESS;
        }
        return UseOutcome.PASS;
    }
}
