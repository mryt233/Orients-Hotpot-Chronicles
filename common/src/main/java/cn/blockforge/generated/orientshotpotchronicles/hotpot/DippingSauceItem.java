package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3G 右键结果改走适配小包（意图 → 原版常量），逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public DippingSauceItem() {
        super(new Item.Properties().stacksTo(1));
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
            return new ItemStack(Items.BOWL);
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
    public static void dipFromHands(LivingEntity user, ItemStack food) {
        for (InteractionHand hand : new InteractionHand[]{InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND}) {
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
        for (SeasoningDef s : Seasonings.all()) {
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

    /**
     * mryt233 改（2026-11-04）：没有蘸料（口数 <= 0）的蘸料物品是非法状态——创造栏不再发它，
     * 但 /give 一个不带组件的、存档里留下的、别的模组给的，都可能混进来。在背包里一 tick 就换成原版空碗。
     * mryt233 改（2026-11-07）：改成在当前槽位原地换成空碗。以前是先 setCount(0) 再补一只碗，
     * 两步之间客户端还显示原物；而且躺在非玩家容器里（箱子、方块实体、别的模组的容器）时
     * 只清数量、不补碗，等于把碗一起吞掉。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level world,
                              Entity entity, int slot, boolean selected) {
        // 只在服务端换（客户端也跑 inventoryTick，两边各塞一只碗会多出一只假碗；mryt233 改）
        if (world.isClientSide() || contents(stack).bites() > 0) {
            return;
        }
        if (!(entity instanceof Player player)) {
            // mryt233 改（2026-11-07）：非玩家容器不处理，避免吞物品
            return;
        }
        // 玩家背包：Inventory.tick() 是按「物品栏 → 盔甲 → 副手」的顺序连续编号传 slot 的，
        // 正好等于 Inventory 容器下标（getItem / setItem 用的是同一套编号），所以能直接原地换。
        // 当前槽位原地换成空碗，一次到位、不经过「数量清 0」的中间状态。
        player.getInventory().setItem(slot, new ItemStack(Items.BOWL));
    }

    /** mryt233 改（2026-11-04）：手里这碗要是空的（非法状态）就换成空碗。inventoryTick 只在东西进背包时才跑，
     *  拿在手里、一开始就是空的那种靠这里兜住。 */
    private static boolean emptyBowlIfInvalid(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (contents(stack).bites() > 0) {
            return false;
        }
        if (!player.level().isClientSide()) {   // 换碗只在服务端做，客户端等同步（mryt233 改）
            player.setItemInHand(hand, new ItemStack(Items.BOWL));
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && emptyBowlIfInvalid(context.getPlayer(), context.getHand())) {
            return Use.item(UseOutcome.SUCCESS, context.getLevel().isClientSide);
        }
        return PlateItem.placeOn(context, HotPotContent.BOWL_BLOCK, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        SauceContents c = contents(stack);
        // mryt233 改（2026-11-04）：口数为 0 的是非法状态（进背包就会被换成空碗），说明里不显示「还能蘸 0 口」。
        if (c.bites() <= 0) {
            return;
        }
        // 每种配料一行，带效果（用户 2026-10-25：蘸料的属性要显示对应调料和 buff）
        Seasoning.appendEffects(c.mask(), tooltip);
        tooltip.add(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), c.bites()).withStyle(ChatFormatting.GRAY));
    }
}