// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.inventory.Slot;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 玻璃杯（2026-10-25 用户：杯子；碰杯先不做）。空杯一组 16 个；倒满的杯子装一口饮料（和瓶子的一口一样：回 2 饥饿 + 那种饮料的效果），
 * 喝完变回空杯。倒饮料：物品栏里拿着饮料瓶右键点空杯，或者拿饮料瓶右键桌上摆着的空杯（PlateHolderBlock），瓶子少一口。
 * 杯子能像瓶子一样摆在桌上。
 * 水、牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒（物品栏里点、或右键桌上的空杯），桶不会空；空杯对着水源右键舀一杯水（同原版玻璃瓶）。
 * 一杯水 / 牛奶不回饥饿值（同原版），牛奶清掉所有效果。作者：mryt233 */
public class CupItem extends Item {
    @Nullable
    private final DrinkKind kind;

    public CupItem(@Nullable DrinkKind kind, Item.Properties properties) {
        super(properties.stacksTo(kind == null ? 16 : 1));
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
        return stack.is(net.minecraft.world.item.Items.WATER_BUCKET) ? DrinkKind.WATER
            : stack.is(net.minecraft.world.item.Items.MILK_BUCKET) ? DrinkKind.MILK : null;
    }

    public static boolean isEmptyCup(ItemStack stack) {
        return stack.getItem() instanceof CupItem cup && cup.kind == null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult poured = DrinkCauldronBlock.pourIntoEmpty(context);
        if (poured != InteractionResult.PASS) {
            return poured;
        }
        return PlateItem.placeOn(context, HotPotContent.CUP_BLOCK, true);
    }

    /** 物品栏里：空杯放在格子里，鼠标拿着饮料瓶右键点它 = 倒一杯（像原版收纳袋那样的点法）。 */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player,
                                            SlotAccess cursorStackReference) {
        DrinkKind bucket = bucketKind(otherStack);
        if (kind != null || clickType != ClickAction.SECONDARY || !(otherStack.getItem() instanceof DrinkItem || bucket != null)
            || !slot.mayPickup(player)) {
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
            slot.set(full);
        } else {
            stack.shrink(1);
            slot.setChanged();
            player.getInventory().placeItemBackInInventory(full);
        }
        player.playSound(SoundEvents.BOTTLE_EMPTY, 0.8F, 1.1F);
        return true;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (kind != null) {
            return ItemUtils.startUsingInstantly(world, user, hand);
        }
        // 空杯对着水源：舀一杯水（同原版玻璃瓶）
        net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(world, user, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
            && world.mayInteract(user, hit.getBlockPos())
            && world.getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
            world.playSound(user, user.getX(), user.getY(), user.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
            world.gameEvent(user, net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, hit.getBlockPos());
            user.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, user, new ItemStack(HotPotContent.WATER_CUP)));
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return kind == null ? ItemUseAnimation.NONE : ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return kind == null ? 0 : 32;
    }

    // mryt233 改：26.x 进食过程的声音和碎渣只认物品自带的 CONSUMABLE
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        // 只有装了饮料的杯子能喝；一口 32 刻，和原版 DEFAULT_DRINK 的 consumeTicks 一致。
        if (kind == null) {
            return;
        }
        Consumable drink = Consumables.DEFAULT_DRINK;
        if (drink.shouldEmitParticlesAndSounds(ticksRemaining)) {
            drink.emitParticlesAndSounds(user.getRandom(), user, stack, 5);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (kind == null) {
            return stack;
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.GENERIC_DRINK,
            SoundSource.PLAYERS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        if (world.isClientSide()) {
            return stack;
        }
        kind.applySip(user);
        if (user instanceof Player player) {
            if (kind.hasBottle()) {
                player.getFoodData().eat(DrinkItem.sipFood());   // 水、牛奶不回饥饿值（同原版）；数值同饮料瓶，读配置（W7）
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player.getAbilities().instabuild) {
                return stack;
            }
        }
        return new ItemStack(HotPotContent.GLASS_CUP);
    }
}
