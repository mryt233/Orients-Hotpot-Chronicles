// 0.1.2 的 1.20.1 Fabric 接口迁移：。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * 竹筷（r103 起有自己的类）：夹着能吃的菜时，对着空气按住右键就把菜吃掉（用户同意的吃法），
 * 吃法、饱食度、声音全走原版 eatFood；菜蘸过蘸料的话效果在 LivingEntityEatMixin 里生效。
 * 夹菜 / 放菜 / 开锅界面这些还是在各个方块里处理。
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
        // 副手拿着蘸料：吃之前自动蘸一口（2026-10-23 用户：蘸料只能副手拿着，桌上蘸那条去掉了）
        if (!world.isClient && user.getOffHandStack().getItem() instanceof DippingSauceItem
            && DippingSauceItem.contents(user.getOffHandStack()).bites() > 0) {
            ItemData.setDipped(eaten, ItemData.dipped(eaten)
                | DippingSauceItem.contents(user.getOffHandStack()).mask());
            user.setStackInHand(net.minecraft.util.Hand.OFF_HAND, DippingSauceItem.bite(user.getOffHandStack()));
        }
        user.eatFood(world, eaten);
        if (!world.isClient) {
            Chopsticks.hold(stack, ItemStack.EMPTY);
            if (user instanceof PlayerEntity player && !player.isCreative()) {
                // 沿用 1.20.1 的碗装食物及合成剩余物判断。
                ItemStack rest = eaten.getItem() instanceof net.minecraft.item.StewItem
                    || eaten.getItem() instanceof net.minecraft.item.SuspiciousStewItem
                    ? new ItemStack(net.minecraft.item.Items.BOWL) : eaten.getItem().getRecipeRemainder(eaten);
                if (!rest.isEmpty()) player.getInventory().offerOrDrop(rest);
            }
        }
        return stack;
    }
}
