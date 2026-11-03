// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * 一碗蘸料（r103，用户定）：拿碗在蘸料台上一种一种打出来，组件 sauce 记着放了哪几种、还能蘸几口（9 口）。
 * 能像盘子一样摆在桌上（筷子夹着菜点一下 = 蘸一口，效果记在菜上）；主手或副手拿着时，用筷子吃会自动蘸一口。
 * 蘸完 9 口变回原版的碗。名字一律叫「蘸料」，提示里列配料（用户）。
 * 作者：mryt233 */
public class DippingSauceItem extends Item {
    public DippingSauceItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static SauceContents contents(ItemStack stack) {
        SauceContents c = stack.get(HotPotContent.SAUCE);
        return c == null ? new SauceContents(0, 0) : c;
    }

    /** 新打的一碗：只有这一种调料、满 9 口。 */
    public static ItemStack create(Seasoning first) {
        ItemStack stack = new ItemStack(HotPotContent.DIPPING_SAUCE);
        stack.set(HotPotContent.SAUCE, new SauceContents(first.bit(), SauceContents.FULL_BITES));
        return stack;
    }

    /** 蘸掉一口；没口了返回一只空碗（原版），否则返回这碗本身。 */
    public static ItemStack bite(ItemStack stack) {
        SauceContents left = contents(stack).bitten();
        if (left.bites() <= 0) {
            return new ItemStack(net.minecraft.world.item.Items.BOWL);
        }
        stack.set(HotPotContent.SAUCE, left);
        return stack;
    }

    /**
     * 往这样菜上蘸一口：配料记进菜的 dipped 组件（吃的时候生效），碗里少一口。
     * 返回蘸完剩下的那碗（没口了是空碗）；碗里没有蘸料就原样返回、不蘸。新增（2026-10-25）。
     */
    public static ItemStack dipInto(ItemStack sauce, ItemStack food) {
        if (!(sauce.getItem() instanceof DippingSauceItem) || contents(sauce).bites() <= 0) {
            return sauce;
        }
        food.set(HotPotContent.DIPPED, food.getOrDefault(HotPotContent.DIPPED, 0) | contents(sauce).mask());
        return bite(sauce);
    }

    /** 吃之前：主手或副手拿着蘸料就自动蘸一口（先看副手；用户 2026-10-25：另一只手主副都行）。 */
    public static void dipFromHands(net.minecraft.world.entity.LivingEntity user, ItemStack food) {
        for (net.minecraft.world.InteractionHand hand : new net.minecraft.world.InteractionHand[]{net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.InteractionHand.MAIN_HAND}) {
            ItemStack sauce = user.getItemInHand(hand);
            if (sauce.getItem() instanceof DippingSauceItem && contents(sauce).bites() > 0) {
                user.setItemInHand(hand, dipInto(sauce, food));
                return;
            }
        }
    }

    /** 「麻酱、辣油、葱花」这样的配料名单。 */
    public static MutableComponent names(int mask) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Seasoning s : Seasoning.ALL) {
            if ((mask & s.bit()) != 0) {
                if (!first) {
                    out.append(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.sep")));
                }
                out.append(Component.translatable(s.nameKey()));
                first = false;
            }
        }
        return out;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return PlateItem.placeOn(context, HotPotContent.BOWL_BLOCK, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag type) {
        SauceContents c = contents(stack);
        // 每种配料一行，带效果（用户 2026-10-25：蘸料的属性要显示对应调料和 buff）
        java.util.List<Component> effects = new java.util.ArrayList<>();
        Seasoning.appendEffects(c.mask(), effects);
        effects.forEach(tooltip);
        tooltip.accept(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), c.bites()).withStyle(ChatFormatting.GRAY));
    }
}