package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 一碗蘸料（r103，用户定）：拿碗在蘸料台上一种一种打出来，组件 sauce 记着放了哪几种、还能蘸几口（9 口）。
 * 能像盘子一样摆在桌上（筷子夹着菜点一下 = 蘸一口，效果记在菜上）；主手或副手拿着时，用筷子吃会自动蘸一口。
 * 蘸完 9 口变回原版的碗。名字一律叫「蘸料」，提示里列配料（用户）。
 *
 */
public class DippingSauceItem extends Item {
    public DippingSauceItem() {
        super(new Item.Settings().maxCount(1));
    }

    public static SauceContents contents(ItemStack stack) {
        SauceContents c = ItemData.sauce(stack);
        return c == null ? new SauceContents(0, 0) : c;
    }

    /** 新打的一碗：只有这一种调料、满 9 口。 */
    public static ItemStack create(Seasoning first) {
        ItemStack stack = new ItemStack(HotPotContent.DIPPING_SAUCE);
        ItemData.setSauce(stack, new SauceContents(first.bit(), SauceContents.FULL_BITES));
        return stack;
    }

    /** 蘸掉一口；没口了返回一只空碗（原版），否则返回这碗本身。 */
    public static ItemStack bite(ItemStack stack) {
        SauceContents left = contents(stack).bitten();
        if (left.bites() <= 0) {
            return new ItemStack(net.minecraft.item.Items.BOWL);
        }
        ItemData.setSauce(stack, left);
        return stack;
    }

    /**
     * 往这样菜上蘸一口：配料记进菜的 dipped 组件（吃的时候生效），碗里少一口。
     * 返回蘸完剩下的那碗（没口了是空碗）；碗里没有蘸料就原样返回、不蘸。（2026-10-25）。
     */
    public static ItemStack dipInto(ItemStack sauce, ItemStack food) {
        if (!(sauce.getItem() instanceof DippingSauceItem) || contents(sauce).bites() <= 0) {
            return sauce;
        }
        // ：1.20.1 用既有物品 NBT 保存蘸料。
        ItemData.setDipped(food, ItemData.dipped(food) | contents(sauce).mask());
        return bite(sauce);
    }

    /** 吃之前：主手或副手拿着蘸料就自动蘸一口（先看副手；用户 2026-10-25：另一只手主副都行）。 */
    public static void dipFromHands(net.minecraft.entity.LivingEntity user, ItemStack food) {
        for (net.minecraft.util.Hand hand : new net.minecraft.util.Hand[]{net.minecraft.util.Hand.OFF_HAND, net.minecraft.util.Hand.MAIN_HAND}) {
            ItemStack sauce = user.getStackInHand(hand);
            if (sauce.getItem() instanceof DippingSauceItem && contents(sauce).bites() > 0) {
                user.setStackInHand(hand, dipInto(sauce, food));
                return;
            }
        }
    }

    /** 「麻酱、辣油、葱花」这样的配料名单。 */
    public static MutableText names(int mask) {
        MutableText out = Text.empty();
        boolean first = true;
        for (Seasoning s : Seasoning.ALL) {
            if ((mask & s.bit()) != 0) {
                if (!first) {
                    out.append(Text.translatable(HotPotBlockEntity.key("tooltip.sauce.sep")));
                }
                out.append(Text.translatable(s.nameKey()));
                first = false;
            }
        }
        return out;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return PlateItem.placeOn(context, HotPotContent.BOWL_BLOCK, true);
    }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.world.World world, List<Text> tooltip, TooltipContext context) {
        SauceContents c = contents(stack);
        // 每种配料一行，带效果（用户 2026-10-25：蘸料的属性要显示对应调料和 buff）
        Seasoning.appendEffects(c.mask(), tooltip);
        tooltip.add(Text.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), c.bites()).formatted(Formatting.GRAY));
    }

    // ：沿用本工程 Forge 物品客户端扩展。
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(cn.blockforge.generated.orientshotpotchronicles.client.ForgeItemRenderers.extension());
    }
}
