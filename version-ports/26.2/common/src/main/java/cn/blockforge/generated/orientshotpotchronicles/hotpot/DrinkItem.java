// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 一瓶饮料（r99 橙子饮料；r101 改成通用，加维二，种类见 {@link DrinkKind}）。
 * 一瓶喝 4 口（用耐久条显示还剩多少），每口回 2 点饥饿值 + 种类自己的效果；喝完剩一个空瓶。
 * 点在方块顶面是摆上去（和盘子一样），对着空气右键是喝。配方先不做（用户）。
 * 作者：mryt233 */
public class DrinkItem extends Item {
    public static final int SIPS = 4;
    /** 每口的默认营养；实际值喝的时候读配置文件 drink.sip（代码检查 W7，mryt233 2026-11-01）。 */
    private static final int SIP_HUNGER = 2;
    private static final float SIP_SATURATION = 0.3F;

    static {
        Config.rememberDrink(SIP_HUNGER, SIP_SATURATION);
    }

    /** 喝一口的营养（按当前配置现算，改完 /orientshotpot reload 立即生效）。玻璃杯也用这个。 */
    static FoodProperties sipFood() {
        return new FoodProperties.Builder().nutrition(Config.drinkHunger(SIP_HUNGER))
            .saturationModifier(Config.drinkSaturation(SIP_SATURATION)).alwaysEdible().build();
    }

    private final DrinkKind kind;

    public DrinkItem(DrinkKind kind, Item.Properties properties) {
        super(properties.durability(SIPS));
        this.kind = kind;
    }

    public DrinkKind kind() {
        return kind;
    }

    public static int sipsLeft(ItemStack stack) {
        return SIPS - stack.getDamageValue();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult poured = DrinkCauldronBlock.pourIntoEmpty(context);
        if (poured != InteractionResult.PASS) {
            return poured;
        }
        return PlateItem.placeOn(context, HotPotContent.DRINK_BLOCK, true);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    // mryt233 改：26.x 进食过程的声音和碎渣只认物品自带的 CONSUMABLE
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        // 喝一口是 32 刻，和原版 DEFAULT_DRINK 的 consumeTicks 一致，直接用原版那套。
        Consumable drink = Consumables.DEFAULT_DRINK;
        if (drink.shouldEmitParticlesAndSounds(ticksRemaining)) {
            drink.emitParticlesAndSounds(user.getRandom(), user, stack, 5);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.GENERIC_DRINK,
            SoundSource.PLAYERS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        if (world.isClientSide()) {
            return stack;
        }
        if (user instanceof Player player) {
            player.getFoodData().eat(sipFood());
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        kind.applySip(user);
        if (user instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        if (stack.getDamageValue() + 1 >= SIPS) {
            return new ItemStack(HotPotContent.emptyBottle(kind));
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
        return stack;
    }

    /** 倒出一口（倒进杯子，2026-10-25）：返回倒完剩下的瓶子，最后一口倒完是空瓶。创造模式也照样少一口。 */
    public static ItemStack pourOne(ItemStack bottle) {
        if (!(bottle.getItem() instanceof DrinkItem drink)) {
            return bottle;
        }
        if (bottle.getDamageValue() + 1 >= SIPS) {
            return new ItemStack(HotPotContent.emptyBottle(drink.kind()));
        }
        bottle.setDamageValue(bottle.getDamageValue() + 1);
        return bottle;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.translatable(HotPotBlockEntity.key("tooltip.drink.sips"), sipsLeft(stack), SIPS)
            .withStyle(ChatFormatting.GRAY));
    }
}