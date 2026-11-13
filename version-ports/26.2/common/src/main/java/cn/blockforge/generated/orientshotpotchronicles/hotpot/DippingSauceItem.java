// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 一碗蘸料（r103，用户定）：拿碗在蘸料台上一种一种打出来，组件 sauce 记着放了哪几种、还能蘸几口（9 口）。
 * 能像盘子一样摆在桌上（筷子夹着菜点一下 = 蘸一口，效果记在菜上）；主手或副手拿着时，用筷子吃会自动蘸一口。
 * 蘸完 9 口变回原版的碗。名字一律叫「蘸料」，提示里列配料（用户）。
 * 作者：mryt233 */
public class DippingSauceItem extends Item implements TableItem {
    public DippingSauceItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static SauceContents contents(ItemStack stack) {
        SauceContents c = stack.get(HotPotContent.SAUCE);
        return c == null ? SauceContents.empty() : c;
    }

    /** 新打的一碗：只有这一种调料、满 9 口。 */
    public static ItemStack create(Seasoning first) {
        ItemStack stack = new ItemStack(HotPotContent.DIPPING_SAUCE);
        stack.set(HotPotContent.SAUCE, new SauceContents(List.of(SauceIds.fullId(first.def())), SauceContents.FULL_BITES));
        return stack;
    }

    /** 按登记 ID 新建，旧枚举入口继续兼容。 */
    public static ItemStack createWithId(String id) {
        ItemStack stack = new ItemStack(HotPotContent.DIPPING_SAUCE);
        stack.set(HotPotContent.SAUCE, new SauceContents(List.of(id), SauceContents.FULL_BITES));
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
        List<String> merged = new ArrayList<>(food.getOrDefault(HotPotContent.DIPPED, List.of()));
        merged.addAll(contents(sauce).ids());
        food.set(HotPotContent.DIPPED, SauceIds.normalize(merged));
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
    @Deprecated public static MutableComponent names(int mask) { return names(SauceIds.fromMask(mask)); }

    public static MutableComponent names(List<String> ids) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (String id : SauceIds.normalize(ids)) {
            SeasoningDef s = SauceIds.def(id);
            if (!first) {
                out.append(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.sep")));
            }
            out.append(s == null ? Component.translatable(HotPotBlockEntity.key("sauce.unknown")).append(" (" + id + ")")
                : Component.translatable(s.nameKey()));
            first = false;
        }
        return out;
    }

    /**
     * mryt233 改（2026-11-04）：没有蘸料（口数 <= 0）的蘸料物品是非法状态，在背包里一 tick 就换成原版空碗。
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (contents(stack).bites() > 0) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        emptyBowlInInventory(player, stack);
    }

    private static void emptyBowlInInventory(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        ItemStack bowl = new ItemStack(Items.BOWL);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                inventory.setItem(i, bowl);
                return;
            }
        }
    }

    /** 手里这碗要是空的（非法状态）就换成空碗。 */
    private static boolean emptyBowlIfInvalid(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (contents(stack).bites() > 0) {
            return false;
        }
        if (!player.level().isClientSide()) {
            player.setItemInHand(hand, new ItemStack(Items.BOWL));
        }
        return true;
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return HotPotContent.BOWL_BLOCK;
    }

    @Override
    public ItemStack prepareForTable(ItemStack stack) {
        return contents(stack).bites() > 0 ? stack : new ItemStack(Items.BOWL);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && emptyBowlIfInvalid(context.getPlayer(), context.getHand())) {
            return Use.item(UseOutcome.SUCCESS, context.getLevel().isClientSide());
        }
        return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        SauceContents c = contents(stack);
        if (c.bites() <= 0) {
            return;
        }
        List<Component> effects = new ArrayList<>();
        Seasoning.appendEffects(c.ids(), effects);
        effects.forEach(tooltip);
        tooltip.accept(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), c.bites()).withStyle(ChatFormatting.GRAY));
    }
}
