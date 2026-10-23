package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * 旧存档兼容（2026-10-23，）：r102 把物品 / 方块 ID 改成和英文名对得上（对照表 tools/rename_ids.py），
 * 但 0.1.0-alpha.1 已经公开发布、用的是旧 ID，旧存档进新版本时灶台、盘子、凳子等会直接消失（用户实测）。
 * 这里在方块 / 物品注册表查 ID 时把旧 ID 换成新 ID（mixin/LegacyIdRegistryMixin），读档时就认得了。
 * 例外：r101 以前的 {@code <木头>_bench} 是板凳，但现在 {@code <木头>_bench} 是条凳（新 ID 已占用，没法换），
 * 所以旧存档里的板凳会变成条凳；旧的条凳 {@code <木头>_tall_bench} 正常变成新条凳。
 *
 */
public final class LegacyIds {
    private static final Map<String, String> OLD_TO_NEW = Map.ofEntries(
        Map.entry("stove", "brick_stove"),
        Map.entry("tall_stove", "tall_brick_stove"),
        Map.entry("tall_stove_top", "tall_brick_stove_top"),
        Map.entry("copper_hot_pot", "copper_nine_grid_hotpot"),
        Map.entry("iron_hot_pot", "iron_nine_grid_hotpot"),
        Map.entry("chopsticks", "bamboo_chopsticks"),
        Map.entry("clear_soup_bowl", "clear_broth_bowl"),
        Map.entry("milky_soup_bowl", "creamy_broth_bowl"),
        Map.entry("mushroom_soup_bowl", "mushroom_broth_bowl"),
        Map.entry("spicy_soup_bowl", "spicy_red_broth_bowl"),
        Map.entry("guochengduo", "orange_drink"),
        Map.entry("empty_guochengduo", "empty_orange_drink_bottle"),
        Map.entry("weier", "plant_protein_drink"),
        Map.entry("empty_weier", "empty_plant_protein_drink_bottle"),
        Map.entry("oak_tall_bench", "oak_bench"),
        Map.entry("spruce_tall_bench", "spruce_bench"),
        Map.entry("birch_tall_bench", "birch_bench"),
        Map.entry("jungle_tall_bench", "jungle_bench"),
        Map.entry("acacia_tall_bench", "acacia_bench"),
        Map.entry("dark_oak_tall_bench", "dark_oak_bench"),
        Map.entry("mangrove_tall_bench", "mangrove_bench"),
        Map.entry("cherry_tall_bench", "cherry_bench"),
        Map.entry("bamboo_tall_bench", "bamboo_bench"),
        Map.entry("crimson_tall_bench", "crimson_bench"),
        Map.entry("warped_tall_bench", "warped_bench"),
        Map.entry("quartz_plate_1x1", "quartz_small_dish"),
        Map.entry("quartz_plate_1x2", "quartz_two_slot_plate"),
        Map.entry("quartz_plate_1x3", "quartz_three_slot_long_plate"),
        Map.entry("quartz_plate_2x2", "quartz_four_slot_square_plate"),
        Map.entry("quartz_plate_1x5", "quartz_five_slot_long_plate"),
        Map.entry("quartz_plate_2x4", "quartz_eight_slot_platter"),
        Map.entry("bamboo_plate_1x1", "bamboo_small_dish"),
        Map.entry("bamboo_plate_1x2", "bamboo_two_slot_plate"),
        Map.entry("bamboo_plate_1x3", "bamboo_three_slot_long_plate"),
        Map.entry("bamboo_plate_2x2", "bamboo_four_slot_square_plate"),
        Map.entry("bamboo_plate_1x5", "bamboo_five_slot_long_plate"),
        Map.entry("bamboo_plate_2x4", "bamboo_eight_slot_platter"),
        Map.entry("terracotta_plate_1x1", "terracotta_small_dish"),
        Map.entry("terracotta_plate_1x2", "terracotta_two_slot_plate"),
        Map.entry("terracotta_plate_1x3", "terracotta_three_slot_long_plate"),
        Map.entry("terracotta_plate_2x2", "terracotta_four_slot_square_plate"),
        Map.entry("terracotta_plate_1x5", "terracotta_five_slot_long_plate"),
        Map.entry("terracotta_plate_2x4", "terracotta_eight_slot_platter"),
        Map.entry("copper_plate_1x1", "copper_small_dish"),
        Map.entry("copper_plate_1x2", "copper_two_slot_plate"),
        Map.entry("copper_plate_1x3", "copper_three_slot_long_plate"),
        Map.entry("copper_plate_2x2", "copper_four_slot_square_plate"),
        Map.entry("copper_plate_1x5", "copper_five_slot_long_plate"),
        Map.entry("copper_plate_2x4", "copper_eight_slot_platter"),
        Map.entry("iron_plate_1x1", "iron_small_dish"),
        Map.entry("iron_plate_1x2", "iron_two_slot_plate"),
        Map.entry("iron_plate_1x3", "iron_three_slot_long_plate"),
        Map.entry("iron_plate_2x2", "iron_four_slot_square_plate"),
        Map.entry("iron_plate_1x5", "iron_five_slot_long_plate"),
        Map.entry("iron_plate_2x4", "iron_eight_slot_platter"));

    private LegacyIds() {
    }

    private static boolean applies(Registry<?> registry) {
        RegistryKey<?> key = registry.getKey();
        return key == RegistryKeys.BLOCK || key == RegistryKeys.ITEM;
    }

    /** 旧 ID 换成新 ID；不是本模组的旧 ID 原样返回。 */
    public static Identifier remap(Registry<?> registry, Identifier id) {
        if (id == null || !GeneratedMod.MOD_ID.equals(id.getNamespace()) || !applies(registry)) {
            return id;
        }
        String now = OLD_TO_NEW.get(id.getPath());
        return now == null ? id : GeneratedMod.id(now);
    }

    public static <T> RegistryKey<T> remap(Registry<?> registry, RegistryKey<T> key) {
        if (key == null) {
            return null;
        }
        Identifier now = remap(registry, key.getValue());
        return now == key.getValue() ? key : RegistryKey.of(key.getRegistryRef(), now);
    }
}
