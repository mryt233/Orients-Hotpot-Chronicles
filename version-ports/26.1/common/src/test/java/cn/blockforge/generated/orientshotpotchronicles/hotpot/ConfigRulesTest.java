// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import cn.blockforge.orient.framework.pure.ConfigStore;
import cn.blockforge.orient.framework.pure.ConfigWarning;
import cn.blockforge.orient.framework.pure.NumericConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 第四批 4I + 4J + 4K + 4K-2：配置校验规则、0.2.2-alpha 旧配置迁入、loot.* 前缀优先级、
 * 0 时长 / 0 掉率边界及 4×6 红汤与各汤底风味加成一致性测试。
 * 作者：mryt233
 */
class ConfigRulesTest {
    private static final String CONFIG_FILE = "orients_hotpot_chronicles.json";
    private static final String LEGACY_DIR = "/opt4/legacy-0.2.2-alpha/";
    private static final long MAX_BYTES = 65536;
    private static final String[] GARNISH_IDS = {
        "scallion", "cilantro", "soybean", "garlic", "tomato",
        "chili_pepper", "peanut", "greens", "zhe_er_gen"
    };

    @BeforeAll
    static void bootstrapDefaults() {
        Opt3AttributesTest.bootstrapMinecraft();
        HotPotFuel.values();
        Seasonings.all();
        SoupBase.values();
        DrinkKind.values();
        HotPotCooking.cookRevision();
    }

    // ------------------------------------------------------------------ 1. 全量默认键命中规则与 0 告警载入

    @Test
    void everyDefaultKeyMatchesNonNullRuleAndLoadsWithZeroWarnings(@TempDir Path dir) throws Exception {
        JsonObject defaults = Config.defaultsCopy();
        Map<String, JsonElement> leaves = collectLeaves(defaults);
        assertFalse(leaves.isEmpty(), "defaultsCopy 不应为空");

        // 核心新增分组（4I/4J/4K）与已有分组键必须齐全
        assertTrue(leaves.containsKey("client.particle_intensity"));
        assertTrue(leaves.containsKey("warmth.interval_ticks"));
        assertTrue(leaves.containsKey("warmth.range"));
        assertTrue(leaves.containsKey("warmth.duration"));
        assertTrue(leaves.containsKey("warmth.amplifier"));
        assertTrue(leaves.containsKey("cold_pot.nausea_chance"));
        assertTrue(leaves.containsKey("cold_pot.nausea_duration"));
        assertTrue(leaves.containsKey("cheers.cooldown_ticks"));
        assertTrue(leaves.containsKey("cheers.luck_duration"));
        assertTrue(leaves.containsKey("cheers.luck_amplifier"));
        assertTrue(leaves.containsKey("cheers.regen_duration"));
        assertTrue(leaves.containsKey("cheers.regen_amplifier"));
        assertTrue(leaves.containsKey("cheers.bonus.hunger"));
        assertTrue(leaves.containsKey("cheers.bonus.saturation"));
        assertTrue(leaves.containsKey("spicy.level_duration_step"));
        assertTrue(leaves.containsKey("spicy.level_amplifier_from"));
        assertTrue(leaves.containsKey("spicy.second_bonus_l1"));
        assertTrue(leaves.containsKey("spicy.second_bonus_l2"));
        assertTrue(leaves.containsKey("spicy.second_bonus_l3"));
        assertTrue(leaves.containsKey("spicy.fire_from_level"));
        assertTrue(leaves.containsKey("spicy.fire_top_multiplier"));
        assertTrue(leaves.containsKey("spicy.hunger_top_bonus"));
        assertTrue(leaves.containsKey("spicy.mouth_fire_from_level"));
        assertTrue(leaves.containsKey("spicy.mouth_fire_ticks"));
        assertTrue(leaves.containsKey("loot.shrimp_fishing.weight"));
        assertTrue(leaves.containsKey("loot.lily_pad.lotus_root.chance"));
        assertTrue(leaves.containsKey("loot.mushroom.enoki.chance"));
        assertTrue(leaves.containsKey("loot.cow.beef_loin.chance"));
        assertTrue(leaves.containsKey("loot.cow.beef_loin.looting_bonus"));
        for (String garnishId : GARNISH_IDS) {
            assertTrue(leaves.containsKey("loot.garnish." + garnishId + ".chance"),
                "缺少配菜掉率默认键: " + garnishId);
        }

        for (Map.Entry<String, JsonElement> entry : leaves.entrySet()) {
            String path = entry.getKey();
            NumericConfig.Rule rule = Config.rule(path);
            assertNotNull(rule, "默认键缺少校验规则: " + path);
            BigDecimal number = entry.getValue().getAsBigDecimal();
            if (rule.integer()) {
                assertTrue(number.stripTrailingZeros().scale() <= 0,
                    "整数规则默认键却为小数: " + path + "=" + number);
            }
            double value = number.doubleValue();
            assertTrue(value >= rule.min().doubleValue() && value <= rule.max().doubleValue(),
                "默认键值超出规则范围: " + path + "=" + value + " not in [" + rule.min() + ", " + rule.max() + "]");
        }

        NumericConfig.Result direct = NumericConfig.parse(defaults.toString(), defaults, Config::rule, 1);
        assertTrue(direct.validDocument());
        assertEquals(0, direct.warnings().size(), "全量默认键直解析应 0 告警: " + direct.warnings());

        Path activeDir = dir.resolve("active");
        Files.createDirectories(activeDir);
        try {
            Config.init(activeDir, true);
            Config.saveDefaultsIfNeeded();
            Config.Loaded initial = Config.reload();
            assertEquals(0, initial.warnings().size(), "首次生成并重载默认配置应 0 告警: " + initial.warnings());

            JsonObject serializedDefaults = Config.defaultsCopy();
            serializedDefaults.addProperty("_format", 1);
            Files.writeString(activeDir.resolve(CONFIG_FILE), serializedDefaults.toString(), StandardCharsets.UTF_8);
            Config.Loaded reloaded = Config.reload();
            assertEquals(0, reloaded.warnings().size(), "写入全部默认值再重载应 0 告警: " + reloaded.warnings());
        } finally {
            resetConfig(dir);
        }
    }

    // ------------------------------------------------------------------ 2. 0.2.2-alpha 旧配置夹具升级迁入

    @Test
    void legacy022AlphaFixturesMatchRulesAndUpgradeCleanly(@TempDir Path dir) throws Exception {
        JsonObject generated = readLegacyJson("config-generated.json");
        JsonObject edited = readLegacyJson("config-edited.json");

        assertAllLeavesMatchRules(generated, "config-generated.json");
        assertAllLeavesMatchRules(edited, "config-edited.json");

        // 以 0.2.2-alpha 完整食品 / 饮品 / 豆腐模板合并当前 defaultsCopy，验证整份旧档无 1 条校验告警
        JsonObject fullDefaults = mergeMissingLeaves(Config.defaultsCopy(), generated);
        NumericConfig.Result generatedParsed = NumericConfig.parse(generated.toString(), fullDefaults, Config::rule, 1);
        assertTrue(generatedParsed.validDocument());
        assertEquals(0, generatedParsed.warnings().size(), "config-generated.json 应 0 告警: " + generatedParsed.warnings());

        NumericConfig.Result editedParsed = NumericConfig.parse(edited.toString(), fullDefaults, Config::rule, 1);
        assertTrue(editedParsed.validDocument());
        assertEquals(0, editedParsed.warnings().size(), "config-edited.json 应 0 告警: " + editedParsed.warnings());

        // 通过真实 Config.init / ConfigMigration / Config.reload / saveDefaultsIfNeeded 迁入 edited 夹具
        Path upgradeDir = dir.resolve("upgrade-edited");
        Files.createDirectories(upgradeDir);
        Path cfgFile = upgradeDir.resolve(CONFIG_FILE);
        Files.writeString(cfgFile, edited.toString(), StandardCharsets.UTF_8);

        List<String> migrationWarnings = new ArrayList<>();
        Predicate<String> valid = new ConfigStore(CONFIG_FILE, 1, MAX_BYTES, "", p -> null,
            JsonObject::new, migrationWarnings::add)::isValidDocument;
        List<String> moved = ConfigMigration.migrate(cfgFile, MAX_BYTES, valid, migrationWarnings::add);
        assertTrue(moved.isEmpty(), "0.2.2-alpha 夹具已是 134b/139 后键名，不应误迁移: " + moved);
        assertTrue(migrationWarnings.isEmpty(), "旧配置迁移不应产生告警: " + migrationWarnings);

        try {
            Config.init(upgradeDir, true);
            Config.Loaded loaded = Config.reload();
            for (ConfigWarning warning : loaded.warnings()) {
                assertEquals(ConfigWarning.PREFIX + "unknown_key", warning.key(),
                    "旧档迁入只允许未在单测中注册实体的 food/drink.sip/tofu 键触发 unknown_key，实际: " + warning);
                String unknownPath = warning.args().isEmpty() ? "" : warning.args().get(0);
                assertTrue(unknownPath.equals("food")
                        || unknownPath.startsWith("food.")
                        || unknownPath.equals("drink.sip")
                        || unknownPath.startsWith("drink.sip.")
                        || unknownPath.startsWith("cooking.tofu_"),
                    "不应出现预期之外的未知键: " + unknownPath);
            }

            // 玩家在 0.2.2-alpha 里调过的项原样生效
            assertEquals(240, Config.soupBowlInt("spicy", "duration", 0));
            assertEquals(100, Config.seasoningTicks("honey", 0));
            assertEquals(0.7F, Config.fuelPower("wood", 0F), 1e-4F);
            assertEquals(200, Config.fallbackTicks(0));
            assertEquals(50, Config.itemCookTicks("beef_loin_slice", 0));

            // 0.2.2-alpha 缺少的 4I / 4J / 4K 新增项全部补齐为合理默认值
            assertEquals(Config.DEFAULT_WARMTH_INTERVAL_TICKS, Config.warmthIntervalTicks());
            assertEquals(Config.DEFAULT_COLD_POT_NAUSEA_CHANCE, Config.coldPotNauseaChance(), 1e-6F);
            assertEquals(Config.DEFAULT_CHEERS_COOLDOWN_TICKS, Config.cheersCooldownTicks());
            assertEquals(Config.DEFAULT_SPICY_LEVEL_DURATION_STEP, Config.spicyLevelDurationStep());
            assertEquals(Config.DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT, Config.lootShrimpFishingWeight());
            assertEquals(0.15F, Config.lootGarnishChance("tomato", 0F), 1e-6F);

            Config.saveDefaultsIfNeeded();
            JsonObject savedRoot = JsonParser.parseString(Files.readString(cfgFile, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(240, savedRoot.getAsJsonObject("soup_bowl").getAsJsonObject("spicy").get("duration").getAsInt());
            assertTrue(savedRoot.has("warmth"));
            assertTrue(savedRoot.has("cold_pot"));
            assertTrue(savedRoot.has("cheers"));
            assertTrue(savedRoot.has("spicy"));
            assertTrue(savedRoot.has("loot"));
        } finally {
            resetConfig(dir);
        }
    }

    // ------------------------------------------------------------------ 3. loot.* 前缀规则优先级与值域

    @Test
    void lootPrefixRulesEnforceBoundsAndBeatSuffixRules(@TempDir Path dir) throws Exception {
        NumericConfig.Rule weightRule = Config.rule("loot.shrimp_fishing.weight");
        assertNotNull(weightRule);
        assertTrue(weightRule.integer());
        assertEquals(0.0, weightRule.min().doubleValue(), 1e-9);
        assertEquals(1000.0, weightRule.max().doubleValue(), 1e-9);

        for (String garnishId : GARNISH_IDS) {
            NumericConfig.Rule rule = Config.rule("loot.garnish." + garnishId + ".chance");
            assertNotNull(rule, "配菜规则缺失: " + garnishId);
            assertFalse(rule.integer());
            assertEquals(0.0, rule.min().doubleValue(), 1e-9);
            assertEquals(1.0, rule.max().doubleValue(), 1e-9);
        }
        assertChanceRule("loot.lily_pad.lotus_root.chance");
        assertChanceRule("loot.mushroom.enoki.chance");
        assertChanceRule("loot.cow.beef_loin.chance");
        assertChanceRule("loot.cow.beef_loin.looting_bonus");

        // 前缀优先级：loot.* 不会误命中 .hunger / .saturation / .duration / .amplifier / .power 等后缀规则
        assertNull(Config.rule("loot.garnish.tomato.hunger"));
        assertNull(Config.rule("loot.garnish.tomato.saturation"));
        assertNull(Config.rule("loot.cow.beef_loin.duration"));
        assertNull(Config.rule("loot.cow.beef_loin.amplifier"));
        assertNull(Config.rule("loot.shrimp_fishing.power"));
        assertNull(Config.rule("loot.lily_pad.lotus_root.burn_ticks"));
        assertNull(Config.rule("loot.mushroom.enoki.fallback_ticks"));
        assertNull(Config.rule("loot.mushroom.enoki.xp_per_item"));

        Path testDir = dir.resolve("loot");
        Files.createDirectories(testDir);
        try {
            // 下界 0 允许，且 0 警告
            JsonObject zeroRoot = Config.defaultsCopy();
            zeroRoot.addProperty("_format", 1);
            JsonObject zeroLoot = zeroRoot.getAsJsonObject("loot");
            zeroLoot.getAsJsonObject("shrimp_fishing").addProperty("weight", 0);
            for (String garnishId : GARNISH_IDS) {
                zeroLoot.getAsJsonObject("garnish").getAsJsonObject(garnishId).addProperty("chance", 0.0);
            }
            zeroLoot.getAsJsonObject("lily_pad").getAsJsonObject("lotus_root").addProperty("chance", 0.0);
            zeroLoot.getAsJsonObject("mushroom").getAsJsonObject("enoki").addProperty("chance", 0.0);
            JsonObject zeroCow = zeroLoot.getAsJsonObject("cow").getAsJsonObject("beef_loin");
            zeroCow.addProperty("chance", 0.0);
            zeroCow.addProperty("looting_bonus", 0.0);

            Files.writeString(testDir.resolve(CONFIG_FILE), zeroRoot.toString(), StandardCharsets.UTF_8);
            Config.init(testDir, true);
            Config.Loaded zeroLoaded = Config.reload();
            assertEquals(0, zeroLoaded.warnings().size(), "loot 下界 0 应 0 告警: " + zeroLoaded.warnings());
            assertEquals(0, Config.lootShrimpFishingWeight());
            for (String garnishId : GARNISH_IDS) {
                assertEquals(0F, Config.lootGarnishChance(garnishId, 0.25F), 1e-6F);
            }
            assertEquals(0F, Config.lootLilyPadLotusRootChance(), 1e-6F);
            assertEquals(0F, Config.lootMushroomEnokiChance(), 1e-6F);
            assertEquals(0F, Config.lootCowBeefLoinChance(), 1e-6F);
            assertEquals(0F, Config.lootCowBeefLoinLootingBonus(), 1e-6F);

            // 上界 (1000 / 1.0) 允许
            JsonObject maxRoot = Config.defaultsCopy();
            maxRoot.addProperty("_format", 1);
            JsonObject maxLoot = maxRoot.getAsJsonObject("loot");
            maxLoot.getAsJsonObject("shrimp_fishing").addProperty("weight", 1000);
            maxLoot.getAsJsonObject("garnish").getAsJsonObject("tomato").addProperty("chance", 1.0);
            maxLoot.getAsJsonObject("lily_pad").getAsJsonObject("lotus_root").addProperty("chance", 1.0);
            maxLoot.getAsJsonObject("mushroom").getAsJsonObject("enoki").addProperty("chance", 1.0);
            JsonObject maxCow = maxLoot.getAsJsonObject("cow").getAsJsonObject("beef_loin");
            maxCow.addProperty("chance", 1.0);
            maxCow.addProperty("looting_bonus", 1.0);

            Files.writeString(testDir.resolve(CONFIG_FILE), maxRoot.toString(), StandardCharsets.UTF_8);
            Config.Loaded maxLoaded = Config.reload();
            assertEquals(0, maxLoaded.warnings().size(), "loot 上界应 0 告警: " + maxLoaded.warnings());
            assertEquals(1000, Config.lootShrimpFishingWeight());
            assertEquals(1F, Config.lootGarnishChance("tomato", 0F), 1e-6F);
            assertEquals(1F, Config.lootCowBeefLoinChance(), 1e-6F);
            assertEquals(1F, Config.lootCowBeefLoinLootingBonus(), 1e-6F);

            // 越界 / 小数权重 / 负值触发告警并归整回默认值
            JsonObject badRoot = Config.defaultsCopy();
            badRoot.addProperty("_format", 1);
            JsonObject badLoot = badRoot.getAsJsonObject("loot");
            badLoot.getAsJsonObject("shrimp_fishing").addProperty("weight", 1001);
            badLoot.getAsJsonObject("garnish").getAsJsonObject("tomato").addProperty("chance", 1.01);
            badLoot.getAsJsonObject("garnish").getAsJsonObject("scallion").addProperty("chance", -0.01);
            badLoot.getAsJsonObject("lily_pad").getAsJsonObject("lotus_root").addProperty("chance", 1.5);
            badLoot.getAsJsonObject("mushroom").getAsJsonObject("enoki").addProperty("chance", -0.2);
            badLoot.getAsJsonObject("cow").getAsJsonObject("beef_loin").addProperty("chance", 2.0);
            badLoot.getAsJsonObject("cow").getAsJsonObject("beef_loin").addProperty("looting_bonus", -1.0);

            Files.writeString(testDir.resolve(CONFIG_FILE), badRoot.toString(), StandardCharsets.UTF_8);
            Config.Loaded badLoaded = Config.reload();
            assertEquals(7, badLoaded.warnings().size(), "7 个越界项应各产出 1 条告警: " + badLoaded.warnings());
            assertEquals(Config.DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT, Config.lootShrimpFishingWeight());
            assertEquals(0.15F, Config.lootGarnishChance("tomato", 0.15F), 1e-6F);
            assertEquals(0.25F, Config.lootGarnishChance("scallion", 0.25F), 1e-6F);
            assertEquals(Config.DEFAULT_LOOT_LILY_PAD_LOTUS_ROOT_CHANCE, Config.lootLilyPadLotusRootChance(), 1e-6F);
            assertEquals(Config.DEFAULT_LOOT_MUSHROOM_ENOKI_CHANCE, Config.lootMushroomEnokiChance(), 1e-6F);
            assertEquals(Config.DEFAULT_LOOT_COW_BEEF_LOIN_CHANCE, Config.lootCowBeefLoinChance(), 1e-6F);
            assertEquals(Config.DEFAULT_LOOT_COW_BEEF_LOIN_LOOTING_BONUS, Config.lootCowBeefLoinLootingBonus(), 1e-6F);

            // 权重为小数或负数时触发 not_integer / out_of_range 并回退默认值
            badLoot.getAsJsonObject("shrimp_fishing").addProperty("weight", 12.5);
            Files.writeString(testDir.resolve(CONFIG_FILE), badRoot.toString(), StandardCharsets.UTF_8);
            Config.Loaded decimalWeight = Config.reload();
            assertTrue(decimalWeight.warnings().stream()
                .anyMatch(w -> w.key().equals(ConfigWarning.PREFIX + "not_integer")
                    && w.args().contains("loot.shrimp_fishing.weight")));
            assertEquals(Config.DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT, Config.lootShrimpFishingWeight());

            badLoot.getAsJsonObject("shrimp_fishing").addProperty("weight", -1);
            Files.writeString(testDir.resolve(CONFIG_FILE), badRoot.toString(), StandardCharsets.UTF_8);
            Config.Loaded negativeWeight = Config.reload();
            assertTrue(negativeWeight.warnings().stream()
                .anyMatch(w -> w.key().equals(ConfigWarning.PREFIX + "out_of_range")
                    && w.args().contains("loot.shrimp_fishing.weight")));
            assertEquals(Config.DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT, Config.lootShrimpFishingWeight());
        } finally {
            resetConfig(dir);
        }
    }

    // ------------------------------------------------------------------ 4. warmth / cold_pot / cheers / spicy 边界

    @Test
    void gameplayRulesEnforceBoundsAndAllowZeroDurations(@TempDir Path dir) throws Exception {
        // 验证核心规则区间
        assertRule("warmth.interval_ticks", 1, 1200, true);
        assertRule("warmth.range", 1, 32, true);
        assertRule("warmth.duration", 0, 100000, true);
        assertRule("warmth.amplifier", 0, 9, true);

        assertRule("cold_pot.nausea_chance", 0, 1, false);
        assertRule("cold_pot.nausea_duration", 0, 100000, true);

        assertRule("cheers.cooldown_ticks", 1, 1200, true);
        assertRule("cheers.luck_duration", 0, 100000, true);
        assertRule("cheers.luck_amplifier", 0, 9, true);
        assertRule("cheers.regen_duration", 0, 100000, true);
        assertRule("cheers.regen_amplifier", 0, 9, true);
        assertRule("cheers.bonus.hunger", 0, 100, true);
        assertRule("cheers.bonus.saturation", 0, 10, false);

        assertRule("spicy.level_duration_step", 0, 10000, true);
        assertRule("spicy.level_amplifier_from", 0, 4, true);
        assertRule("spicy.second_bonus_l1", 0, 10000, true);
        assertRule("spicy.second_bonus_l2", 0, 10000, true);
        assertRule("spicy.second_bonus_l3", 0, 10000, true);
        assertRule("spicy.fire_from_level", 1, 4, true);
        assertRule("spicy.fire_top_multiplier", 1, 10, true);
        assertRule("spicy.hunger_top_bonus", 0, 10000, true);
        assertRule("spicy.mouth_fire_from_level", 1, 4, true);
        assertRule("spicy.mouth_fire_ticks", 0, 1200, true);

        Path testDir = dir.resolve("bounds");
        Files.createDirectories(testDir);
        try {
            // 合法下界：时长类与概率允许为 0；间隔/冷却/半径/起燃档位/倍率最小为 1
            JsonObject minRoot = Config.defaultsCopy();
            minRoot.addProperty("_format", 1);
            JsonObject warmth = minRoot.getAsJsonObject("warmth");
            warmth.addProperty("interval_ticks", 1);
            warmth.addProperty("range", 1);
            warmth.addProperty("duration", 0);
            warmth.addProperty("amplifier", 0);

            JsonObject coldPot = minRoot.getAsJsonObject("cold_pot");
            coldPot.addProperty("nausea_chance", 0.0);
            coldPot.addProperty("nausea_duration", 0);

            JsonObject cheers = minRoot.getAsJsonObject("cheers");
            cheers.addProperty("cooldown_ticks", 1);
            cheers.addProperty("luck_duration", 0);
            cheers.addProperty("luck_amplifier", 0);
            cheers.addProperty("regen_duration", 0);
            cheers.addProperty("regen_amplifier", 0);
            cheers.getAsJsonObject("bonus").addProperty("hunger", 0);
            cheers.getAsJsonObject("bonus").addProperty("saturation", 0.0);

            JsonObject spicy = minRoot.getAsJsonObject("spicy");
            spicy.addProperty("level_duration_step", 0);
            spicy.addProperty("level_amplifier_from", 0);
            spicy.addProperty("second_bonus_l1", 0);
            spicy.addProperty("second_bonus_l2", 0);
            spicy.addProperty("second_bonus_l3", 0);
            spicy.addProperty("fire_from_level", 1);
            spicy.addProperty("fire_top_multiplier", 1);
            spicy.addProperty("hunger_top_bonus", 0);
            spicy.addProperty("mouth_fire_from_level", 1);
            spicy.addProperty("mouth_fire_ticks", 0);

            Files.writeString(testDir.resolve(CONFIG_FILE), minRoot.toString(), StandardCharsets.UTF_8);
            Config.init(testDir, true);
            Config.Loaded minLoaded = Config.reload();
            assertEquals(0, minLoaded.warnings().size(), "合法下界应 0 告警: " + minLoaded.warnings());
            assertEquals(1, Config.warmthIntervalTicks());
            assertEquals(1, Config.warmthRange());
            assertEquals(0, Config.warmthDuration());
            assertEquals(0, Config.coldPotNauseaDuration());
            assertEquals(1, Config.cheersCooldownTicks());
            assertEquals(0, Config.cheersLuckDuration());
            assertEquals(0, Config.cheersRegenDuration());
            assertEquals(1, Config.spicyFireFromLevel());
            assertEquals(1, Config.spicyFireTopMultiplier());
            assertEquals(1, Config.spicyMouthFireFromLevel());
            assertEquals(0, Config.spicyMouthFireTicks());

            // 最小为 1 的键填 0 以及负值 / 超上界：全部产出 out_of_range 告警并回退默认值
            JsonObject badRoot = Config.defaultsCopy();
            badRoot.addProperty("_format", 1);
            badRoot.getAsJsonObject("warmth").addProperty("interval_ticks", 0);
            badRoot.getAsJsonObject("warmth").addProperty("range", 0);
            badRoot.getAsJsonObject("warmth").addProperty("duration", -1);
            badRoot.getAsJsonObject("cold_pot").addProperty("nausea_chance", 1.01);
            badRoot.getAsJsonObject("cold_pot").addProperty("nausea_duration", -1);
            badRoot.getAsJsonObject("cheers").addProperty("cooldown_ticks", 0);
            badRoot.getAsJsonObject("cheers").addProperty("luck_duration", -1);
            badRoot.getAsJsonObject("cheers").getAsJsonObject("bonus").addProperty("hunger", 101);
            badRoot.getAsJsonObject("cheers").getAsJsonObject("bonus").addProperty("saturation", 10.1);
            JsonObject badSpicy = badRoot.getAsJsonObject("spicy");
            badSpicy.addProperty("level_duration_step", 10001);
            badSpicy.addProperty("level_amplifier_from", 5);
            badSpicy.addProperty("fire_from_level", 0);
            badSpicy.addProperty("fire_top_multiplier", 11);
            badSpicy.addProperty("mouth_fire_from_level", 0);
            badSpicy.addProperty("mouth_fire_ticks", 1201);

            Files.writeString(testDir.resolve(CONFIG_FILE), badRoot.toString(), StandardCharsets.UTF_8);
            Config.Loaded badLoaded = Config.reload();
            assertEquals(15, badLoaded.warnings().size(), "15 项越界应全部告警: " + badLoaded.warnings());
            for (ConfigWarning warning : badLoaded.warnings()) {
                assertEquals(ConfigWarning.PREFIX + "out_of_range", warning.key(), "应为 out_of_range: " + warning);
            }
            assertEquals(Config.DEFAULT_WARMTH_INTERVAL_TICKS, Config.warmthIntervalTicks());
            assertEquals(Config.DEFAULT_WARMTH_RANGE, Config.warmthRange());
            assertEquals(Config.DEFAULT_WARMTH_DURATION, Config.warmthDuration());
            assertEquals(Config.DEFAULT_COLD_POT_NAUSEA_CHANCE, Config.coldPotNauseaChance(), 1e-6F);
            assertEquals(Config.DEFAULT_COLD_POT_NAUSEA_DURATION, Config.coldPotNauseaDuration());
            assertEquals(Config.DEFAULT_CHEERS_COOLDOWN_TICKS, Config.cheersCooldownTicks());
            assertEquals(Config.DEFAULT_CHEERS_LUCK_DURATION, Config.cheersLuckDuration());
            assertEquals(Config.DEFAULT_CHEERS_BONUS_HUNGER, Config.cheersBonusHunger());
            assertEquals(Config.DEFAULT_CHEERS_BONUS_SATURATION, Config.cheersBonusSaturation(), 1e-6F);
            assertEquals(Config.DEFAULT_SPICY_LEVEL_DURATION_STEP, Config.spicyLevelDurationStep());
            assertEquals(Config.DEFAULT_SPICY_LEVEL_AMPLIFIER_FROM, Config.spicyLevelAmplifierFrom());
            assertEquals(Config.DEFAULT_SPICY_FIRE_FROM_LEVEL, Config.spicyFireFromLevel());
            assertEquals(Config.DEFAULT_SPICY_FIRE_TOP_MULTIPLIER, Config.spicyFireTopMultiplier());
            assertEquals(Config.DEFAULT_SPICY_MOUTH_FIRE_FROM_LEVEL, Config.spicyMouthFireFromLevel());
            assertEquals(Config.DEFAULT_SPICY_MOUTH_FIRE_TICKS, Config.spicyMouthFireTicks());
        } finally {
            resetConfig(dir);
        }
    }

    // ------------------------------------------------------------------ 5. 4×6 = 24 组合及 0 时长关闭行为

    @Test
    void defaultConfigAll24SoupAndSpicyCombinationsMatchLegacyFormula(@TempDir Path dir) throws Exception {
        Path testDir = dir.resolve("soup-matrix");
        Files.createDirectories(testDir);
        try {
            Config.init(testDir, true);
            Config.saveDefaultsIfNeeded();
            Config.Loaded loaded = Config.reload();
            assertEquals(0, loaded.warnings().size());

            SoupBase[] bases = SoupBase.values();
            assertEquals(6, bases.length);
            int combinations = 0;
            for (SoupBase base : bases) {
                for (int spicyLevel = 0; spicyLevel <= 3; spicyLevel++) {
                    combinations++;
                    List<MobEffectInstance> expected = expectedLegacyBonusEffects(base, spicyLevel);
                    List<MobEffectInstance> fromDef = base.def().dishBonusEffects(spicyLevel);
                    List<MobEffectInstance> fromEnum = base.dishBonusEffects(spicyLevel);
                    assertEffectsMatch(base.name() + "@spicy=" + spicyLevel, expected, fromDef);
                    assertEffectsMatch(base.name() + "@enum@spicy=" + spicyLevel, expected, fromEnum);
                }
            }
            assertEquals(24, combinations);

            // 边界为 0：mouth_fire_ticks / warmth.duration / cold_pot.nausea_duration / cheers.luck_duration / hunger_duration
            JsonObject zeroEffects = Config.defaultsCopy();
            zeroEffects.addProperty("_format", 1);
            zeroEffects.getAsJsonObject("warmth").addProperty("duration", 0);
            zeroEffects.getAsJsonObject("cold_pot").addProperty("nausea_duration", 0);
            zeroEffects.getAsJsonObject("cheers").addProperty("luck_duration", 0);
            zeroEffects.getAsJsonObject("spicy").addProperty("mouth_fire_ticks", 0);
            zeroEffects.getAsJsonObject("soup_bowl").getAsJsonObject("spicy").addProperty("hunger_duration", 0);

            Files.writeString(testDir.resolve(CONFIG_FILE), zeroEffects.toString(), StandardCharsets.UTF_8);
            Config.Loaded zeroLoaded = Config.reload();
            assertEquals(0, zeroLoaded.warnings().size(), "0 时长边界应被允许且 0 告警: " + zeroLoaded.warnings());
            assertEquals(0, Config.warmthDuration());
            assertEquals(0, Config.coldPotNauseaDuration());
            assertEquals(0, Config.cheersLuckDuration());
            assertEquals(0, Config.spicyMouthFireTicks());

            // 红汤 3 档当 hunger_duration = 0 时不触发饥饿效果（即使 hunger_top_bonus = 40）
            List<MobEffectInstance> topSpicyNoHunger = SoupBase.SPICY.def().dishBonusEffects(3);
            assertEquals(3, topSpicyNoHunger.size());
            assertTrue(topSpicyNoHunger.stream().noneMatch(e -> e.getEffect().equals(MobEffects.HUNGER)));
        } finally {
            resetConfig(dir);
        }
    }

    // ------------------------------------------------------------------ 辅助方法

    private static List<MobEffectInstance> expectedLegacyBonusEffects(SoupBase base, int spicyLevel) {
        return switch (base) {
            case CLEAR -> List.of(
                effect(MobEffects.SPEED, 300, 0),
                effect(MobEffects.SATURATION, 1, 0));
            case RICH_STOCK -> List.of(
                effect(MobEffects.RESISTANCE, 300, 0),
                effect(MobEffects.ABSORPTION, 600, 0));
            case MUSHROOM -> List.of(
                effect(MobEffects.REGENERATION, 200, 0));
            case TOMATO -> List.of(
                effect(MobEffects.HASTE, 300, 0));
            case THREE_FRESH -> List.of(
                effect(MobEffects.WATER_BREATHING, 400, 0),
                effect(MobEffects.LUCK, 600, 0));
            case SPICY -> switch (spicyLevel) {
                case 0 -> List.of(
                    effect(MobEffects.STRENGTH, 120, 0),
                    effect(MobEffects.SPEED, 120, 0));
                case 1 -> List.of(
                    effect(MobEffects.STRENGTH, 180, 1),
                    effect(MobEffects.SPEED, 180, 0));
                case 2 -> List.of(
                    effect(MobEffects.STRENGTH, 240, 1),
                    effect(MobEffects.SPEED, 200, 0),
                    effect(MobEffects.FIRE_RESISTANCE, 300, 0),
                    effect(MobEffects.HUNGER, 120, 0));
                case 3 -> List.of(
                    effect(MobEffects.STRENGTH, 300, 1),
                    effect(MobEffects.SPEED, 240, 0),
                    effect(MobEffects.FIRE_RESISTANCE, 600, 0),
                    effect(MobEffects.HUNGER, 160, 0));
                default -> throw new IllegalArgumentException("Unexpected spicyLevel: " + spicyLevel);
            };
        };
    }

    private static MobEffectInstance effect(Holder<MobEffect> holder, int ticks, int amplifier) {
        return new MobEffectInstance(holder, ticks, amplifier, false, true);
    }

    private static void assertEffectsMatch(String label, List<MobEffectInstance> expected, List<MobEffectInstance> actual) {
        assertEquals(expected.size(), actual.size(), label + " 效果数量不一致");
        for (int i = 0; i < expected.size(); i++) {
            MobEffectInstance exp = expected.get(i);
            MobEffectInstance act = actual.get(i);
            assertEquals(exp.getEffect(), act.getEffect(), label + "[" + i + "] effect");
            assertEquals(exp.getDuration(), act.getDuration(), label + "[" + i + "] duration");
            assertEquals(exp.getAmplifier(), act.getAmplifier(), label + "[" + i + "] amplifier");
            assertEquals(exp.isAmbient(), act.isAmbient(), label + "[" + i + "] ambient");
            assertEquals(exp.isVisible(), act.isVisible(), label + "[" + i + "] visible");
        }
    }

    private static void assertChanceRule(String path) {
        assertRule(path, 0, 1, false);
    }

    private static void assertRule(String path, double min, double max, boolean integer) {
        NumericConfig.Rule rule = Config.rule(path);
        assertNotNull(rule, "规则缺失: " + path);
        assertEquals(integer, rule.integer(), path + " integer");
        assertEquals(min, rule.min().doubleValue(), 1e-9, path + " min");
        assertEquals(max, rule.max().doubleValue(), 1e-9, path + " max");
    }

    private static void assertAllLeavesMatchRules(JsonObject root, String label) {
        Map<String, JsonElement> leaves = collectLeaves(root);
        assertFalse(leaves.isEmpty(), label + " 不应为空");
        for (Map.Entry<String, JsonElement> entry : leaves.entrySet()) {
            String path = entry.getKey();
            NumericConfig.Rule rule = Config.rule(path);
            assertNotNull(rule, label + " 存在无规则键: " + path);
            BigDecimal number = entry.getValue().getAsBigDecimal();
            if (rule.integer()) {
                assertTrue(number.stripTrailingZeros().scale() <= 0, label + " 应为整数: " + path + "=" + number);
            }
            double val = number.doubleValue();
            assertTrue(val >= rule.min().doubleValue() && val <= rule.max().doubleValue(),
                label + " 数值越界: " + path + "=" + val + " not in [" + rule.min() + ", " + rule.max() + "]");
        }
    }

    private static Map<String, JsonElement> collectLeaves(JsonObject root) {
        Map<String, JsonElement> out = new LinkedHashMap<>();
        collectLeaves(root, "", out);
        return out;
    }

    private static void collectLeaves(JsonObject node, String prefix, Map<String, JsonElement> out) {
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                collectLeaves(value.getAsJsonObject(), path, out);
            } else {
                out.put(path, value);
            }
        }
    }

    private static JsonObject mergeMissingLeaves(JsonObject base, JsonObject fallback) {
        JsonObject merged = base.deepCopy();
        mergeMissingInto(merged, fallback);
        return merged;
    }

    private static void mergeMissingInto(JsonObject target, JsonObject source) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) {
                continue;
            }
            JsonElement srcVal = entry.getValue();
            if (!target.has(key)) {
                target.add(key, srcVal.deepCopy());
            } else if (target.get(key).isJsonObject() && srcVal.isJsonObject()) {
                mergeMissingInto(target.getAsJsonObject(key), srcVal.getAsJsonObject());
            }
        }
    }

    private static JsonObject readLegacyJson(String name) throws Exception {
        try (InputStream in = ConfigRulesTest.class.getResourceAsStream(LEGACY_DIR + name)) {
            assertNotNull(in, "缺少夹具文件: " + LEGACY_DIR + name);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void resetConfig(Path dir) throws Exception {
        Path clean = dir.resolve("clean");
        Files.createDirectories(clean);
        Config.init(clean, false);
        Config.reload();
    }
}
