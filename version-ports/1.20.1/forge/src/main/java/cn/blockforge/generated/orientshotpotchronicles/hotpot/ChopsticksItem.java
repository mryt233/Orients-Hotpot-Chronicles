package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.BlockState;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 竹筷（r103 起有自己的类）：夹着能吃的菜时，对着空气按住右键就把菜吃掉（用户同意的吃法），
 * 吃法、饱食度、声音全走原版 eatFood；菜蘸过蘸料的话效果在 LivingEntityEatMixin 里生效。
 * Shift+右键锅开锅界面在这里（useOnBlock）；夹菜 / 放菜 / 蘸桌上的蘸料在各个方块里处理。
 *
 */
public class ChopsticksItem extends Item {
    public ChopsticksItem() {
        super(new Item.Settings().maxCount(1).rarity(Rarity.COMMON));
    }

    private static FoodComponent heldFood(ItemStack stack) {
        ItemStack held = Chopsticks.held(stack);
        return held.isEmpty() ? null : held.getItem().getFoodComponent();
    }

    /**
     * 拿筷子 Shift+右键锅 = 开锅界面（2026-10-25 用户定，代替原来的直接右键开锅；直接右键改成夹菜，见 HotPotBlock / StoveBlock）。
     * 潜行又拿着东西时原版不调方块的右键，只调物品的 useOnBlock，所以放在这里。。
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockHitResult hit = new BlockHitResult(context.getHitPos(), context.getSide(), pos, context.hitsInsideBlock());
        if (world.getBlockState(pos).isOf(HotPotContent.STOVE_TOP)) {
            // 高灶台的上半截：当成点在下面的灶台上
            pos = pos.down();
            hit = new BlockHitResult(hit.getPos(), hit.getSide(), pos, hit.isInsideBlock());
        }
        BlockState state = world.getBlockState(pos);
        // Shift+右键桌上的碗 / 盘子：筷子（连夹着的菜）架在碗沿上（2026-10-27 用户：只架碗和盘子，不架锅）
        if (state.isOf(HotPotContent.PLATE_HOLDER) && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity holder) {
            PlateHolderBlockEntity.Placed plate = PlateHolderBlock.aimed(player, pos, holder);
            if (plate == null || !plate.restsChopsticks() || !plate.chopsticks.isEmpty()) {
                return ActionResult.PASS;
            }
            if (!world.isClient) {
                plate.chopsticks = context.getStack().copy();
                player.setStackInHand(context.getHand(), ItemStack.EMPTY);
                holder.changed();
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_BAMBOO_PLACE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.6F, 1.4F);
            }
            return ActionResult.success(world.isClient);
        }
        boolean pot = state.getBlock() instanceof HotPotBlock
            || (StoveBlock.isStove(state) && StoveBlock.isPotHit(state, pos, hit));
        if (!pot || !(world.getBlockEntity(pos) instanceof HotPotBlockEntity be) || !be.hasPot()) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            be.useWithChopsticks(player);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        FoodComponent food = heldFood(stack);
        if (food == null || !user.canConsume(food.isAlwaysEdible())) {
            return TypedActionResult.pass(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return heldFood(stack) != null ? UseAction.EAT : UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        FoodComponent food = heldFood(stack);
        return food == null ? 0 : (food.isSnack() ? 16 : 32);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack held = Chopsticks.held(stack);
        FoodComponent food = heldFood(stack);
        if (food == null) {
            return stack;
        }
        ItemStack eaten = held.copy();
        // 另一只手（主手或副手）拿着蘸料：吃之前自动蘸一口（2026-10-25 用户：主手副手都行；）
        if (!world.isClient) {
            DippingSauceItem.dipFromHands(user, eaten);
        }
        user.eatFood(world, eaten);
        if (!world.isClient) {
            Chopsticks.hold(stack, ItemStack.EMPTY);
            if (user instanceof PlayerEntity player && !player.isCreative()) {
                // ：沿用 1.20.1 的碗装食物及合成剩余物判断。
                ItemStack rest = eaten.getItem() instanceof net.minecraft.item.StewItem
                    || eaten.getItem() instanceof net.minecraft.item.SuspiciousStewItem
                    ? new ItemStack(net.minecraft.item.Items.BOWL) : eaten.getCraftingRemainingItem();
                if (!rest.isEmpty()) player.getInventory().offerOrDrop(rest);
            }
        }
        return stack;
    }

    // ：沿用本工程 Forge 物品客户端扩展。
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(cn.blockforge.generated.orientshotpotchronicles.client.ForgeItemRenderers.extension());
    }
}
