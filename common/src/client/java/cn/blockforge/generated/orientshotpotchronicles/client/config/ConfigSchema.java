// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.*;

/** 作者：mryt233；2026-11-11：设置字段与分组清单，物品解析由构造参数注入。 */
public final class ConfigSchema {
    private final java.util.function.Function<String, ItemStack> items;
    private List<Section> sectionsCache;
    private JsonObject sectionsDoc;
    public ConfigSchema(java.util.function.Function<String, ItemStack> items) { this.items = items; }
    public void invalidate() { sectionsCache = null; }
    /** 常用页：除粒子强度（上面的滑块）以外的几项最常改的数值。 */
    public List<Field> commonFields(JsonObject edited) {
        List<Field> out = new ArrayList<>();
        // 煤炭档的灶火功率：原版煤炭 / 木炭都算这一档，是「整体涮煮快慢」的总闸
        out.add(field("fuel.coal.power", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            fuelName("coal").copy().append(DOT).append(prop("power")), "power"));
        // 没单独登记时间的食材（原版 / 第三方）按这个兜底时间涮熟
        out.add(field("cooking.fallback_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("fallback_ticks"), "fallback_ticks"));
        // 三种主力汤底的加热倍率（汤底本身决定涮煮快慢）
        out.add(field("soup_speed.spicy", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            soupName(Soups.get("spicy")).copy().append(DOT).append(prop("speed")), "speed"));
        out.add(field("soup_speed.clear", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            soupName(Soups.get("clear")).copy().append(DOT).append(prop("speed")), "speed"));
        out.add(field("soup_speed.rich_stock", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            soupName(Soups.get("rich_stock")).copy().append(DOT).append(prop("speed")), "speed"));
        return out;
    }

    /** 高级页的全部分组，按原来的分类：涮煮时间 / 食材 / 汤底 / 燃料 / 汤碗 / 蘸料 / 饮料 / 食物营养。 */
    public List<Section> sections(JsonObject edited) {
        if (sectionsCache != null && sectionsDoc == edited) {
            return sectionsCache;
        }
        List<Section> out = new ArrayList<>();

        List<Field> cooking = new ArrayList<>();
        cooking.add(field("cooking.fallback_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("fallback_ticks"), "fallback_ticks"));
        cooking.add(field("cooking.fallback_xp", false, 0, 100, 0.05, Unit.POINTS,
            prop("fallback_xp"), "fallback_xp"));
        cooking.add(field("cooking.xp_per_item", false, 0, 100, 0.05, Unit.POINTS,
            prop("xp_per_item"), "xp_per_item"));
        cooking.add(field("cooking.tofu_cook_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("tofu_cook_ticks"), "tofu_cook_ticks"));
        cooking.add(field("cooking.tofu_overcook_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("tofu_overcook_ticks"), "tofu_overcook_ticks"));
        out.add(new Section(k("group.cooking"), cooking));

        // 本模组登记过涮煮时间的生食材（cooking.items.<物品注册路径>）
        List<Field> items = new ArrayList<>();
        JsonObject itemsNode = ConfigValues.node(edited, new String[]{"cooking", "items"}, 2);
        for (String id : List.copyOf(itemsNode.keySet())) {
            ItemStack icon = this.items.apply(id);
            Component name = icon.isEmpty() ? Component.literal(id) : icon.getHoverName();
            items.add(field("cooking.items." + id, true, 1, 100000, 20, Unit.TICK,
                name.copy().append(DOT).append(prop("cook_ticks")), "cook_ticks", icon));
        }
        out.add(new Section(k("group.items"), items));

        List<Field> soup = new ArrayList<>();
        // mryt233 改（2026-11-11，4B-2）：设置界面只给内置汤底列配置项，从登记表取（前 6 项）
        for (SoupDef base : Soups.builtins()) {
            soup.add(field("soup_speed." + base.key(), false, 0.1, 10, 0.05, Unit.MULTIPLIER,
                soupName(base).copy().append(DOT).append(prop("speed")), "speed"));
        }
        out.add(new Section(k("group.soup"), soup));

        List<Field> fuel = new ArrayList<>();
        for (HotPotFuel grade : HotPotFuel.values()) {
            Component name = fuelName(grade.configKey());
            fuel.add(field("fuel." + grade.configKey() + ".power", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
                name.copy().append(DOT).append(prop("power")), "power"));
            fuel.add(field("fuel." + grade.configKey() + ".burn_ticks", true, 0, 100000, 200, Unit.TICK,
                name.copy().append(DOT).append(prop("burn_ticks")), "burn_ticks"));
        }
        out.add(new Section(k("group.fuel"), fuel));

        List<Field> bowl = new ArrayList<>();
        for (SoupDef base : Soups.builtins()) {
            for (String key : new String[]{"hunger", "saturation", "duration", "amplifier",
                "second_duration", "second_amplifier", "fire_duration", "fire_amplifier",
                "hunger_duration", "hunger_amplifier"}) {
                bowl.add(bowlField(base, key));
            }
        }
        out.add(new Section(k("group.bowl"), bowl));

        List<Field> seasoning = new ArrayList<>();
        for (Seasoning s : Seasoning.ALL) {
            Component name = seasoningName(s);
            if (s == Seasoning.MSG) {
                // 味精只读 hunger（额外饥饿点数），另外两个键对它没有意义
                seasoning.add(field("seasoning.msg.hunger", true, 0, 100, 1, Unit.POINTS,
                    name.copy().append(DOT).append(prop("hunger")), "hunger"));
            } else {
                seasoning.add(field("seasoning." + s.id() + ".duration", true, 0, 100000, 20, Unit.TICK,
                    name.copy().append(DOT).append(prop("duration")), "duration"));
                seasoning.add(field("seasoning." + s.id() + ".amplifier", true, 0, 9, 1, Unit.LEVEL,
                    name.copy().append(DOT).append(prop("amplifier")), "amplifier"));
            }
        }
        out.add(new Section(k("group.seasoning"), seasoning));

        List<Field> drink = new ArrayList<>();
        drink.add(field("drink.sip.hunger", true, 0, 100, 1, Unit.POINTS,
            prop("sip_hunger"), "sip_hunger"));
        drink.add(field("drink.sip.saturation", false, 0, 10, 0.05, Unit.PLAIN,
            prop("sip_saturation"), "sip_saturation"));
        for (DrinkKind kind : DrinkKind.values()) {
            if (kind == DrinkKind.WATER || kind == DrinkKind.MILK) {
                continue;
            }
            Component name = drinkName(kind);
            drink.add(field("drink." + kind.id() + ".duration", true, 0, 100000, 20, Unit.TICK,
                name.copy().append(DOT).append(prop("duration")), "duration"));
            drink.add(field("drink." + kind.id() + ".amplifier", true, 0, 9, 1, Unit.LEVEL,
                name.copy().append(DOT).append(prop("amplifier")), "amplifier"));
        }
        out.add(new Section(k("group.drink"), drink));

        List<Field> food = new ArrayList<>();
        JsonObject foodNode = ConfigValues.node(edited, new String[]{"food"}, 1);
        for (String id : List.copyOf(foodNode.keySet())) {
            ItemStack icon = this.items.apply(id);
            Component name = icon.isEmpty() ? Component.literal(id) : icon.getHoverName();
            food.add(field("food." + id + ".hunger", true, 0, 100, 1, Unit.POINTS,
                name.copy().append(DOT).append(prop("hunger")), "hunger", icon));
            food.add(field("food." + id + ".saturation", false, 0, 10, 0.05, Unit.PLAIN,
                name.copy().append(DOT).append(prop("saturation")), "saturation", icon));
        }
        out.add(new Section(k("group.food"), food));

        sectionsCache = out;
        sectionsDoc = edited;
        return out;
    }

    /** 汤碗一项：路径 soup_bowl.&lt;汤底&gt;.&lt;键&gt;，范围和单位按后缀分。 */
    private Field bowlField(SoupDef base, String key) {
        Component label = soupName(base).copy().append(DOT).append(prop(key));
        String[] path = {"soup_bowl", base.key(), key};
        return switch (key) {
            case "hunger" -> new Field(path, true, 0, 100, 1, Unit.POINTS, label, k("help.hunger"), ItemStack.EMPTY);
            case "saturation" -> new Field(path, false, 0, 10, 0.05, Unit.PLAIN, label,
                k("help.saturation"), ItemStack.EMPTY);
            case "amplifier", "second_amplifier", "fire_amplifier", "hunger_amplifier" ->
                new Field(path, true, 0, 9, 1, Unit.LEVEL, label, k("help.amplifier"), ItemStack.EMPTY);
            default -> new Field(path, true, 0, 100000, 20, Unit.TICK, label, k("help.duration"), ItemStack.EMPTY);
        };
    }

}
