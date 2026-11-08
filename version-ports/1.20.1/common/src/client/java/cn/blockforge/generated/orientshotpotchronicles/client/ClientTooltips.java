package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * 两加载器共用提示内容，顺序与原 Fabric 客户端一致。作者：mryt233
 * <p>mryt233 改（2026-11-05，清单 #105）：{@code appendDipped} 抽成公共方法，
 * 筷子夹着的菜和盘子里的菜也能显示「蘸了哪些料、各什么效果」。
 * <p>mryt233 改（2026-11-05，清单 #117/#124）：{@code appendCookedSoup} 显示「在哪种汤里涮的」，
 * 并把汤底效果逐条列成一栏缩进小字（等级罗马数字 + m:ss 时长）。
 * <p>mryt233 改（0.2.2-alpha，1.20.1 适配）：物品数据走 ItemData（NBT）；
 * 1.20.1 的 {@code MobEffectInstance#getEffect()} 直接返回 {@code MobEffect}，判负面直接
 * {@code getCategory()}；效果名用 {@code getDescriptionId()}，与 1.21.1 一致。
 */
public final class ClientTooltips {
    private ClientTooltips() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        SharedClientSetup.appendPotTooltip(stack, lines);
        appendDipped(stack, lines);
        appendCookedSoup(stack, lines);   // mryt233 改（2026-11-05，清单 #117）：菜是在哪种汤底里煮的
        appendFlavor(stack, lines);
        SharedClientSetup.appendPlateTooltip(stack, lines);
    }

    /**
     * 这道菜是在哪种汤底里煮出来的、红汤是几档辣；
     * 红汤碗再单独列一行辣度。别的汤底用金色、红汤用红色，一眼看得出「这道菜有味道」。
     * 汤底名字下面再逐条列出吃下 / 喝下实际得到的状态效果和时长（「  力量 II (0:09)」这种缩进小字），
     * 汤碗（{@link SoupBowlItem}）喝下去的效果同样列出来（清单 #124）。
     */
    public static void appendCookedSoup(ItemStack food, List<Component> lines) {
        String soupKey = cn.blockforge.orient.framework.compat.ItemData.get(food, HotPotContent.COOKED_IN_SOUP);
        SoupBase soup = SoupBase.byTextureKey(soupKey);
        // 红汤碗和红汤里夹出来的菜都带 SPICY_LEVEL，先取出来备用
        Integer storedSpicy = cn.blockforge.orient.framework.compat.ItemData.get(food, HotPotContent.SPICY_LEVEL);
        int spicy = Math.max(0, Math.min(3, storedSpicy == null ? 0 : storedSpicy));
        if (soup != null) {
            // 红汤后面接「· 特辣」（状态栏那边是「红汤锅底（特辣）」）
            Component soupName = Component.translatable(soup.nameKey());
            if (soup == SoupBase.SPICY) {
                soupName = Component.empty().append(soupName).append(Component.literal(" · "))
                    .append(HotPotBlockEntity.spicyLevelText(spicy));
            }
            lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.cooked_in_soup"), soupName)
                .withStyle(soup == SoupBase.SPICY ? ChatFormatting.RED : ChatFormatting.GOLD));
            appendSoupEffects(soup, spicy, food, lines);
        }
        if (food.getItem() instanceof SoupBowlItem bowl) {
            if (bowl.soup() == SoupBase.SPICY) {
                lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.spicy_level"),
                    HotPotBlockEntity.spicyLevelText(spicy))
                    .withStyle(ChatFormatting.RED));
            }
            // 汤碗喝下去的效果和锅里涮出来的菜一样，也逐条列出来
            appendSoupEffects(bowl.soup(), spicy, food, lines);
        }
    }

    /**
     * 把汤底（含红汤辣度）附带的 {@link MobEffectInstance} 逐条写进提示，
     * 一行一个缩进两格的效果：「  力量 II (0:09)」；增益蓝色、负面（例如红汤的饥饿）红色，
     * 和原版状态效果提示的颜色一致。蘸了蜂蜜或醋的菜吃下去不会真的加负面效果，这里也不列，
     * 免得提示和实际吃下去的效果对不上（结算在 LivingEntityEatMixin）。
     */
    private static void appendSoupEffects(SoupBase soup, int spicy, ItemStack food, List<Component> lines) {
        List<MobEffectInstance> effects = soup.dishBonusEffects(spicy);
        if (effects.isEmpty()) {
            return;
        }
        // 蘸过解辣的东西（蜂蜜 / 醋）：红汤的饥饿不再叠上去，提示里也别写
        Integer dipped = cn.blockforge.orient.framework.compat.ItemData.get(food, HotPotContent.DIPPED);
        boolean calmSpicy = dipped != null
            && ((dipped & Seasoning.HONEY.bit()) != 0 || (dipped & Seasoning.VINEGAR.bit()) != 0);
        for (MobEffectInstance effect : effects) {
            // mryt233 改（1.20.1 适配）：1.20.1 的 getEffect() 直接返回 MobEffect
            if (calmSpicy && effect.getEffect() == MobEffects.HUNGER) {
                continue;
            }
            boolean harmful = effect.getEffect().getCategory() == MobEffectCategory.HARMFUL;
            lines.add(Component.literal("  ").append(effectLine(effect))
                .withStyle(harmful ? ChatFormatting.RED : ChatFormatting.BLUE));
        }
    }

    /** 一条效果：「力量 II (0:09)」——等级 0 不写罗马数字，时长按 m:ss 写。 */
    private static Component effectLine(MobEffectInstance effect) {
        Component text = Component.translatable(effect.getDescriptionId());
        int amplifier = effect.getAmplifier();
        if (amplifier > 0) {
            text = text.copy().append(Component.literal(" " + roman(amplifier + 1)));
        }
        return text.copy().append(Component.literal(" (" + clock(effect.getDuration()) + ")"));
    }

    /** 1 → I、2 → II …… 10 → X；超过 10 档（配置理论上能调到 9 级 = X）直接写数字，不出怪字符。 */
    private static String roman(int level) {
        String[] table = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level < table.length ? table[level] : Integer.toString(level);
    }

    /**
     * tick 换算成 m:ss：20 tick = 1 秒，不到 1 秒的向上取整成 0:01（不显示 0:00 让人以为没效果）；
     * 原版的无限时长（-1）写成 ∞。
     */
    private static String clock(int ticks) {
        if (ticks < 0) {
            return "∞";
        }
        int seconds = (ticks + 19) / 20;
        return (seconds / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
    }

    /**
     * 这样菜蘸过什么、各有什么效果（清单 #105：筷子和盘子里的菜也复用）。
     * 一行「蘸了：味精、辣油」+ 每种调料一行灰字效果；没蘸过的什么都不加。
     */
    public static void appendDipped(ItemStack food, List<Component> lines) {
        Integer dipped = cn.blockforge.orient.framework.compat.ItemData.get(food, HotPotContent.DIPPED);
        if (dipped == null || dipped == 0) {
            return;
        }
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.dipped"),
            DippingSauceItem.names(dipped)).withStyle(ChatFormatting.GOLD));
        Seasoning.appendEffects(dipped, lines);
    }

    /** 物品趣味小字。 */
    private static void appendFlavor(ItemStack stack, List<Component> lines) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (HotPot.MOD_ID.equals(id.getNamespace())) {
            String key = "tooltip." + HotPot.MOD_ID + ".flavor." + id.getPath();
            if (net.minecraft.client.resources.language.I18n.exists(key)) {
                lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }
}
