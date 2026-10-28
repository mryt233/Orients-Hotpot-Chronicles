//  移植：0.1.4-alpha 冻结底本，保留原作者署名。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * 玻璃杯（2026-10-25 用户：杯子；碰杯先不做）。空杯一组 16 个；倒满的杯子装一口饮料（和瓶子的一口一样：回 2 饥饿 + 那种饮料的效果），
 * 喝完变回空杯。倒饮料：物品栏里拿着饮料瓶右键点空杯，或者拿饮料瓶右键桌上摆着的空杯（PlateHolderBlock），瓶子少一口。
 * 杯子能像瓶子一样摆在桌上。
 * 水、牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒（物品栏里点、或右键桌上的空杯），桶不会空；空杯对着水源右键舀一杯水（同原版玻璃瓶）。
 * 一杯水 / 牛奶不回饥饿值（同原版），牛奶清掉所有效果。
 */
public class CupItem extends Item {
    private static final FoodComponent SIP = new FoodComponent.Builder().nutrition(2).saturationModifier(0.3F)
        .alwaysEdible().build();

    @Nullable
    private final DrinkKind kind;

    public CupItem(@Nullable DrinkKind kind) {
        super(new Item.Settings().maxCount(kind == null ? 16 : 1));
        this.kind = kind;
    }

    /** 装的是哪种饮料；空杯是 null。 */
    @Nullable
    public DrinkKind kind() {
        return kind;
    }

    /** 手里的是水桶 / 奶桶：能倒出哪种；别的东西是 null。 */
    @Nullable
    public static DrinkKind bucketKind(ItemStack stack) {
        return stack.isOf(net.minecraft.item.Items.WATER_BUCKET) ? DrinkKind.WATER
            : stack.isOf(net.minecraft.item.Items.MILK_BUCKET) ? DrinkKind.MILK : null;
    }

    public static boolean isEmptyCup(ItemStack stack) {
        return stack.getItem() instanceof CupItem cup && cup.kind == null;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return PlateItem.placeOn(context, HotPotContent.CUP_BLOCK, true);
    }

    /** 物品栏里：空杯放在格子里，鼠标拿着饮料瓶右键点它 = 倒一杯（像原版收纳袋那样的点法）。 */
    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {
        DrinkKind bucket = bucketKind(otherStack);
        if (kind != null || clickType != ClickType.RIGHT || !(otherStack.getItem() instanceof DrinkItem || bucket != null)
            || !slot.canTakePartial(player)) {
            return false;
        }
        ItemStack full;
        if (otherStack.getItem() instanceof DrinkItem drink) {
            full = new ItemStack(HotPotContent.cupFor(drink.kind()));
            cursorStackReference.set(DrinkItem.pourOne(otherStack));
        } else {
            full = new ItemStack(HotPotContent.cupFor(bucket));   // 桶不变：原版的桶无限倒（用户）
        }
        if (stack.getCount() == 1) {
            slot.setStack(full);
        } else {
            stack.decrement(1);
            slot.markDirty();
            player.getInventory().offerOrDrop(full);
        }
        player.playSound(SoundEvents.ITEM_BOTTLE_EMPTY, 0.8F, 1.1F);
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (kind != null) {
            return ItemUsage.consumeHeldItem(world, user, hand);
        }
        // 空杯对着水源：舀一杯水（同原版玻璃瓶）
        net.minecraft.util.hit.BlockHitResult hit = raycast(world, user, net.minecraft.world.RaycastContext.FluidHandling.SOURCE_ONLY);
        if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
            && world.canPlayerModifyAt(user, hit.getBlockPos())
            && world.getFluidState(hit.getBlockPos()).isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
            world.playSound(user, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            world.emitGameEvent(user, net.minecraft.world.event.GameEvent.FLUID_PICKUP, hit.getBlockPos());
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            return TypedActionResult.success(ItemUsage.exchangeStack(stack, user, new ItemStack(HotPotContent.WATER_CUP)), world.isClient());
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return kind == null ? UseAction.NONE : UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return kind == null ? 0 : 32;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (kind == null) {
            return stack;
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_GENERIC_DRINK,
            SoundCategory.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        if (world.isClient) {
            return stack;
        }
        kind.applySip(user);
        if (user instanceof PlayerEntity player) {
            if (kind.hasBottle()) {
                player.getHungerManager().eat(SIP);   // 水、牛奶不回饥饿值（同原版）
            }
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            if (player.getAbilities().creativeMode) {
                return stack;
            }
        }
        return new ItemStack(HotPotContent.GLASS_CUP);
    }
}
