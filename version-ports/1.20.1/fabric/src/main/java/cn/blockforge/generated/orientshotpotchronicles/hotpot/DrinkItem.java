package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/**
 * 一瓶饮料（r99 橙子饮料；r101 改成通用，加维二，种类见 {@link DrinkKind}）。
 * 一瓶喝 4 口（用耐久条显示还剩多少），每口回 2 点饥饿值 + 种类自己的效果；喝完剩一个空瓶。
 * 点在方块顶面是摆上去（和盘子一样），对着空气右键是喝。配方先不做（用户）。
 *
 */
public class DrinkItem extends Item {
    public static final int SIPS = 4;
    private static final FoodComponent SIP = new FoodComponent.Builder().hunger(2).saturationModifier(0.3F)
        .alwaysEdible().build();

    private final DrinkKind kind;

    public DrinkItem(DrinkKind kind) {
        super(new Item.Settings().maxDamage(SIPS));
        this.kind = kind;
    }

    public DrinkKind kind() {
        return kind;
    }

    public static int sipsLeft(ItemStack stack) {
        return SIPS - stack.getDamage();
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return PlateItem.placeOn(context, HotPotContent.DRINK_BLOCK, true);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_GENERIC_DRINK,
            SoundCategory.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        if (world.isClient) {
            return stack;
        }
        if (user instanceof PlayerEntity player) {
            player.getHungerManager().add(SIP.getHunger(), SIP.getSaturationModifier());   // 1.20.1 移植（）
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        kind.applySip(user);
        if (user instanceof PlayerEntity player && player.getAbilities().creativeMode) {
            return stack;
        }
        if (stack.getDamage() + 1 >= SIPS) {
            return new ItemStack(HotPotContent.emptyBottle(kind));
        }
        stack.setDamage(stack.getDamage() + 1);
        return stack;
    }

    /** 倒出一口（倒进杯子，2026-10-25）：返回倒完剩下的瓶子，最后一口倒完是空瓶。创造模式也照样少一口。 */
    public static ItemStack pourOne(ItemStack bottle) {
        if (!(bottle.getItem() instanceof DrinkItem drink)) {
            return bottle;
        }
        if (bottle.getDamage() + 1 >= SIPS) {
            return new ItemStack(HotPotContent.emptyBottle(drink.kind()));
        }
        bottle.setDamage(bottle.getDamage() + 1);
        return bottle;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(HotPotBlockEntity.key("tooltip.drink.sips"), sipsLeft(stack), SIPS)
            .formatted(Formatting.GRAY));
    }
}
