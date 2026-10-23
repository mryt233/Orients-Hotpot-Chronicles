// 0.1.2 的 1.20.1 Forge 接口迁移：Codex 改。
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
 * 能像盘子一样摆在桌上（筷子夹着菜点一下 = 蘸一口，效果记在菜上）；副手拿着时在锅界面里吃会自动蘸一口。
 * 蘸完 9 口变回原版的碗。名字一律叫「蘸料」，提示里列配料（用户）。
 * 作者：Claude
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
        for (Seasoning s : Seasoning.ALL) {
            if (c.has(s)) {
                tooltip.add(Text.literal(" ").append(Text.translatable(s.nameKey())).formatted(Formatting.GRAY));
            }
        }
        tooltip.add(Text.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), c.bites()).formatted(Formatting.GRAY));
    }
    // Codex 改：沿用本工程Forge物品客户端扩展。
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(cn.blockforge.generated.orientshotpotchronicles.client.ForgeItemRenderers.extension());
    }
}
