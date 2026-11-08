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

/** 两加载器共用提示内容，顺序与原 Fabric 客户端一致。作者：mryt233 */
public final class ClientTooltips {
    private ClientTooltips() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        SharedClientSetup.appendPotTooltip(stack, lines);
        appendDipped(stack, lines);
        appendCookedSoup(stack, lines);   // mryt233 改（2026-11-05）：菜是在哪种汤底里煮的（r136a）
        appendFlavor(stack, lines);
        SharedClientSetup.appendPlateTooltip(stack, lines);
    }

    /**
     * mryt233 改（2026-11-05，r136a）：这道菜是在哪种汤底里煮出来的、红汤是几档辣；
     * 红汤碗再单独列一行辣度。别的汤底用金色、红汤用红色，一眼看得出「这道菜有味道」。
     * mryt233 改（2026-11-06，r139）：汤底名字下面再逐条列出吃下 / 喝下实际得到的状态效果和时长
     * （「  力量 II (0:09)」这种缩进小字），汤碗（{@link SoupBowlItem}）喝下去的效果同样列出来。
     */
    public static void appendCookedSoup(ItemStack food, List<Component> lines) {
        String soupKey = food.get(HotPotContent.COOKED_IN_SOUP);
        SoupBase soup = SoupBase.byTextureKey(soupKey);
        // 红汤碗和红汤里夹出来的菜都带 SPICY_LEVEL 组件，先取出来备用
        int spicy = Math.max(0, Math.min(3, food.getOrDefault(HotPotContent.SPICY_LEVEL, 0)));
        if (soup != null) {
            // 任务单的写法：红汤后面接「· 特辣」（状态栏那边是「红汤锅底（特辣）」）
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
                    HotPotBlockEntity.spicyLevelText(food.getOrDefault(HotPotContent.SPICY_LEVEL, 0)))
                    .withStyle(ChatFormatting.RED));
            }
            // mryt233 改（2026-11-06，r139）：汤碗喝下去的效果和锅里涮出来的菜一样，也逐条列出来
            appendSoupEffects(bowl.soup(), spicy, food, lines);
        }
    }

    /**
     * mryt233 改（2026-11-06，r139）：把汤底（含红汤辣度）附带的 {@link MobEffectInstance} 逐条写进提示，
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
        Integer dipped = food.get(HotPotContent.DIPPED);
        boolean calmSpicy = dipped != null
            && ((dipped & Seasoning.HONEY.bit()) != 0 || (dipped & Seasoning.VINEGAR.bit()) != 0);
        for (MobEffectInstance effect : effects) {
            if (calmSpicy && effect.getEffect().is(MobEffects.HUNGER)) {
                continue;
            }
            boolean harmful = effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
            lines.add(Component.literal("  ").append(effectLine(effect))
                .withStyle(harmful ? ChatFormatting.RED : ChatFormatting.BLUE));
        }
    }

    /** 一条效果：「力量 II (0:09)」——等级 0 不写罗马数字，时长按 m:ss 写（mryt233 改 2026-11-06）。 */
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
     * 这样菜蘸过什么、各有什么效果（mryt233 改（2026-11-04）：抽成公共方法，筷子和盘子里的菜也复用）。
     * 一行「蘸了：味精、辣油」+ 每种调料一行灰字效果；没蘸过的什么都不加。
     */
    public static void appendDipped(ItemStack food, List<Component> lines) {
        Integer dipped = food.get(HotPotContent.DIPPED);
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
