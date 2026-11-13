// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
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

    /** 高级页的全部分组：涮煮时间 / 食材 / 汤底 / 燃料 / 汤碗 / 蘸料 / 饮料 / 食物营养 / 围锅回血 / 冷锅反胃 / 干杯 / 辣度 / 战利品。 */
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
        // 4F-2：与内置汤底一致，设置界面只给内置蘸料列配置项，不把数据包或外部扩展蘸料混入配置界面
        for (Seasoning legacy : Seasoning.ALL) {
            SeasoningDef s = legacy.def();
            Component name = seasoningName(legacy);
            if (legacy == Seasoning.MSG) {
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

        // mryt233 改（4K-2）：新增围锅回血（warmth.*）、冷锅反胃（cold_pot.*）、干杯（cheers.*）、辣度（spicy.*）与战利品（loot.*）高级页分组（共 37 项）。
        List<Field> warmth = new ArrayList<>();
        warmth.add(field("warmth.interval_ticks", true, 1, 1200, 10, Unit.TICK,
            prop("warmth_interval_ticks"), "warmth_interval_ticks"));
        warmth.add(field("warmth.range", true, 1, 32, 1, Unit.BLOCK,
            prop("warmth_range"), "warmth_range"));
        warmth.add(field("warmth.duration", true, 0, 100000, 20, Unit.TICK,
            prop("warmth_duration"), "warmth_duration"));
        warmth.add(field("warmth.amplifier", true, 0, 9, 1, Unit.LEVEL,
            prop("warmth_amplifier"), "amplifier"));
        out.add(new Section(k("group.warmth"), warmth));

        List<Field> coldPot = new ArrayList<>();
        coldPot.add(field("cold_pot.nausea_chance", false, 0, 1, 0.05, Unit.PLAIN,
            prop("cold_pot_nausea_chance"), "cold_pot_nausea_chance"));
        coldPot.add(field("cold_pot.nausea_duration", true, 0, 100000, 20, Unit.TICK,
            prop("cold_pot_nausea_duration"), "cold_pot_nausea_duration"));
        out.add(new Section(k("group.cold_pot"), coldPot));

        List<Field> cheers = new ArrayList<>();
        cheers.add(field("cheers.cooldown_ticks", true, 1, 1200, 5, Unit.TICK,
            prop("cheers_cooldown_ticks"), "cheers_cooldown_ticks"));
        cheers.add(field("cheers.luck_duration", true, 0, 100000, 20, Unit.TICK,
            prop("cheers_luck_duration"), "cheers_luck_duration"));
        cheers.add(field("cheers.luck_amplifier", true, 0, 9, 1, Unit.LEVEL,
            prop("cheers_luck_amplifier"), "amplifier"));
        cheers.add(field("cheers.regen_duration", true, 0, 100000, 20, Unit.TICK,
            prop("cheers_regen_duration"), "cheers_regen_duration"));
        cheers.add(field("cheers.regen_amplifier", true, 0, 9, 1, Unit.LEVEL,
            prop("cheers_regen_amplifier"), "amplifier"));
        cheers.add(field("cheers.bonus.hunger", true, 0, 100, 1, Unit.POINTS,
            prop("cheers_bonus_hunger"), "cheers_bonus_hunger"));
        cheers.add(field("cheers.bonus.saturation", false, 0, 10, 0.05, Unit.PLAIN,
            prop("cheers_bonus_saturation"), "cheers_bonus_saturation"));
        out.add(new Section(k("group.cheers"), cheers));

        List<Field> spicy = new ArrayList<>();
        spicy.add(field("spicy.level_duration_step", true, 0, 10000, 20, Unit.TICK,
            prop("spicy_level_duration_step"), "spicy_level_duration_step"));
        spicy.add(field("spicy.level_amplifier_from", true, 0, 4, 1, Unit.LEVEL,
            prop("spicy_level_amplifier_from"), "spicy_level_amplifier_from"));
        spicy.add(field("spicy.second_bonus_l1", true, 0, 10000, 20, Unit.TICK,
            prop("spicy_second_bonus_l1"), "spicy_second_bonus"));
        spicy.add(field("spicy.second_bonus_l2", true, 0, 10000, 20, Unit.TICK,
            prop("spicy_second_bonus_l2"), "spicy_second_bonus"));
        spicy.add(field("spicy.second_bonus_l3", true, 0, 10000, 20, Unit.TICK,
            prop("spicy_second_bonus_l3"), "spicy_second_bonus"));
        spicy.add(field("spicy.fire_from_level", true, 1, 4, 1, Unit.LEVEL,
            prop("spicy_fire_from_level"), "spicy_fire_from_level"));
        spicy.add(field("spicy.fire_top_multiplier", true, 1, 10, 1, Unit.MULTIPLIER,
            prop("spicy_fire_top_multiplier"), "spicy_fire_top_multiplier"));
        spicy.add(field("spicy.hunger_top_bonus", true, 0, 10000, 20, Unit.TICK,
            prop("spicy_hunger_top_bonus"), "spicy_hunger_top_bonus"));
        spicy.add(field("spicy.mouth_fire_ticks", true, 0, 1200, 10, Unit.TICK,
            prop("spicy_mouth_fire_ticks"), "spicy_mouth_fire_ticks"));
        spicy.add(field("spicy.mouth_fire_from_level", true, 1, 4, 1, Unit.LEVEL,
            prop("spicy_mouth_fire_from_level"), "spicy_mouth_fire_from_level"));
        out.add(new Section(k("group.spicy"), spicy));

        List<Field> loot = new ArrayList<>();
        for (String garnishId : new String[]{
            "scallion", "cilantro", "soybean", "garlic", "tomato",
            "chili_pepper", "peanut", "greens", "zhe_er_gen"
        }) {
            loot.add(lootField("loot.garnish." + garnishId + ".chance", garnishId,
                false, 1, 0.05, "loot_garnish_chance", "loot_garnish_chance"));
        }
        loot.add(lootField("loot.lily_pad.lotus_root.chance", "lotus_root",
            false, 1, 0.05, "loot_lily_pad_chance", "loot_chance"));
        loot.add(lootField("loot.mushroom.enoki.chance", "enoki_mushroom",
            false, 1, 0.05, "loot_mushroom_chance", "loot_chance"));
        loot.add(lootField("loot.cow.beef_loin.chance", "beef_loin",
            false, 1, 0.05, "loot_cow_chance", "loot_cow_chance"));
        loot.add(lootField("loot.cow.beef_loin.looting_bonus", "beef_loin",
            false, 1, 0.05, "loot_looting_bonus", "loot_looting_bonus"));
        loot.add(lootField("loot.shrimp_fishing.weight", "shrimp",
            true, 1000, 5, "loot_shrimp_fishing_weight", "loot_shrimp_fishing_weight"));
        out.add(new Section(k("group.loot"), loot));

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

    /** 战利品掉率/权重一项：带对应物品图标与提示说明（路径以 loot. 开头）。 */
    private Field lootField(String path, String itemId, boolean integer, double max, double step,
                            String propKey, String helpProp) {
        ItemStack icon = this.items.apply(itemId);
        Component name = icon.isEmpty() ? Component.literal(itemId) : icon.getHoverName();
        return field(path, integer, 0, max, step, Unit.PLAIN,
            name.copy().append(DOT).append(prop(propKey)), helpProp, icon);
    }

}
