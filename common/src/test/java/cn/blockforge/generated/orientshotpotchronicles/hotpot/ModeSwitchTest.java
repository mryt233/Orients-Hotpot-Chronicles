// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import cn.blockforge.orient.framework.pure.ConfigStore;
import cn.blockforge.orient.framework.pure.NumericConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.NonNullList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 0.3.1-alpha Phase 1：精简/完整模式预设、7 个细分开关、TypedRules 校验、
 * ConditionalCraftingRecipe 条件分流、惩罚开关以及鸡爪剔骨/涮煮/掉落配置回归测试。
 * 作者：mryt233
 */
class ModeSwitchTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
        HotPotFuel.values();
        Seasonings.all();
        SoupBase.values();
        DrinkKind.values();
        HotPotCooking.cookRevision();
    }

    @Test
    void presetAndCustomToggleOverridesWorkWithTypedValidation(@TempDir Path dir) throws Exception {
        Config.init(dir, true);
        try {
            Config.reload();
            assertEquals(Config.MODE_FULL, Config.modePreset());
            assertFalse(Config.isSimpleMode());
            assertTrue(Config.detailedTofuProcessing());
            assertTrue(Config.penaltiesEnabled());
            assertTrue(Config.cropsReplaceGrass());
            assertTrue(Config.minigamesEnabled());
            assertTrue(Config.rackSwapMaterials());
            assertFalse(Config.simpleUiCollapse());

            // 切换到精简模式预设
            var simpleLoaded = Config.switchMode(Config.MODE_SIMPLE);
            assertNotEquals(ConfigStore.SaveStatus.FAILED, simpleLoaded.saveStatus());
            assertEquals(Config.MODE_SIMPLE, Config.modePreset());
            assertTrue(Config.isSimpleMode());
            assertFalse(Config.detailedTofuProcessing());
            assertFalse(Config.penaltiesEnabled());
            assertFalse(Config.cropsReplaceGrass());
            assertFalse(Config.minigamesEnabled());
            assertFalse(Config.rackSwapMaterials());
            assertTrue(Config.simpleUiCollapse());

            // 混合覆盖：在精简模式下单独开启惩罚机制，同时传非法类型测试 TypedRules 单项回退
            Path cfg = dir.resolve("orients_hotpot_chronicles.json");
            JsonObject doc = JsonParser.parseString(Files.readString(cfg)).getAsJsonObject();
            JsonObject mode = doc.getAsJsonObject("mode");
            mode.addProperty("penalties_enabled", true);
            mode.addProperty("detailed_tofu_processing", "invalid_not_bool");
            mode.addProperty("preset", "invalid_preset");
            Files.writeString(cfg, doc.toString());

            var mixed = Config.reload();
            assertEquals(2, mixed.warnings().size(), "非法 preset 与 boolean 应各自产生警告并回退默认值");
            assertEquals(Config.MODE_FULL, Config.modePreset());
            assertTrue(Config.detailedTofuProcessing());
            assertTrue(Config.penaltiesEnabled());
            assertTrue(Config.simpleUiCollapse(), "合法的 simple_ui_collapse 用户覆盖应保留");
            assertFalse(Config.minigamesEnabled(), "合法的 minigames_enabled 用户覆盖应保留");
        } finally {
            Path clean = dir.resolve("clean");
            Files.createDirectories(clean);
            Config.init(clean, false);
            Config.reload();
        }
    }

    @Test
    void conditionalCraftingAndPenaltyToggleRespondToConfig(@TempDir Path dir) throws Exception {
        Config.init(dir, true);
        try {
            Config.switchMode(Config.MODE_FULL);
            // 完整模式下：仅期待 detailed_tofu_processing=false 的快捷配方不匹配
            ShapelessRecipe inner = new ShapelessRecipe(
                "", CraftingBookCategory.MISC, new ItemStack(Items.APPLE, 2),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.STICK), Ingredient.of(Items.WHEAT)));
            ConditionalCraftingRecipe simpleOnly = new ConditionalCraftingRecipe(
                "mode.detailed_tofu_processing", false, inner);

            CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.STICK), new ItemStack(Items.WHEAT)));
            var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            assertFalse(simpleOnly.matches(input, null));
            assertTrue(simpleOnly.assemble(input, registries).isEmpty());

            // 红汤在惩罚开启时：变态辣应保留负面 HUNGER 效果
            assertTrue(SoupBase.SPICY.dishBonusEffects(3).stream()
                .anyMatch(eff -> eff.getEffect().is(MobEffects.HUNGER)));

            // 切到精简模式：快捷配方激活，同时红汤保留正面抗火但跳过负面 HUNGER
            Config.switchMode(Config.MODE_SIMPLE);
            assertTrue(simpleOnly.isConditionMet());
            assertTrue(simpleOnly.matches(input, null));
            assertEquals(2, simpleOnly.assemble(input, registries).getCount());
            assertTrue(SoupBase.SPICY.dishBonusEffects(3).stream()
                .noneMatch(eff -> eff.getEffect().is(MobEffects.HUNGER)));

            // 网络 StreamCodec 往返应保持 flag / expected / inner 一致
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
            try {
                ConditionalCraftingRecipe.SERIALIZER.streamCodec().encode(buf, simpleOnly);
                ConditionalCraftingRecipe decoded = ConditionalCraftingRecipe.SERIALIZER.streamCodec().decode(buf);
                assertEquals("mode.detailed_tofu_processing", decoded.flag());
                assertFalse(decoded.expected());
                assertEquals(2, decoded.getResultItem(registries).getCount());
            } finally {
                buf.release();
            }
        } finally {
            Path clean = dir.resolve("clean");
            Files.createDirectories(clean);
            Config.init(clean, false);
            Config.reload();
        }
    }

    @Test
    void chickenFeetDefaultsAndDatapackRecipesPresent(@TempDir Path dir) throws Exception {
        Config.init(dir, true);
        try {
            Config.reload();
            assertEquals(0.5F, Config.lootChickenFeetChance(), 1e-6F);
            assertEquals(0.1F, Config.lootChickenFeetLootingBonus(), 1e-6F);
            assertEquals(150, Config.itemCookTicks("chicken_feet", -1));
            assertEquals(100, Config.itemCookTicks("boneless_chicken_feet", -1));

            try (var in = ModeSwitchTest.class.getResourceAsStream(
                "/data/orients_hotpot_chronicles/tags/item/cuttable.json")) {
                assertNotNull(in, "cuttable.json missing");
                String txt = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(txt.contains("orients_hotpot_chronicles:chicken_feet"));
            }
            try (var in = ModeSwitchTest.class.getResourceAsStream(
                "/data/minecraft/tags/item/meat.json")) {
                assertNotNull(in, "meat.json missing");
                String txt = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(txt.contains("orients_hotpot_chronicles:chicken_feet"));
                assertTrue(txt.contains("orients_hotpot_chronicles:boneless_chicken_feet"));
            }
            try (var in = ModeSwitchTest.class.getResourceAsStream(
                "/data/orients_hotpot_chronicles/recipe/boneless_chicken_feet_from_stonecutting.json")) {
                assertNotNull(in, "stonecutting recipe missing");
                JsonObject obj = JsonParser.parseString(new String(in.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("orients_hotpot_chronicles:boneless_chicken_feet",
                    obj.getAsJsonObject("result").get("id").getAsString());
            }
        } finally {
            Path clean = dir.resolve("clean");
            Files.createDirectories(clean);
            Config.init(clean, false);
            Config.reload();
        }
    }

    @Test
    void numericAndTypedValidatorsCatchBadTypesCleanly() {
        JsonObject defaults = new JsonObject();
        defaults.addProperty("flag", true);
        defaults.addProperty("choice", "full");
        defaults.addProperty("count", 5);
        NumericConfig.Rules numRules = p -> "count".equals(p) ? new NumericConfig.Rule(1, 10, true) : null;
        NumericConfig.TypedRules typedRules = p -> switch (p) {
            case "flag" -> candidate -> NumericConfig.checkBoolean(p, candidate);
            case "choice" -> candidate -> NumericConfig.checkChoice(p, candidate, "full", "simple");
            default -> null;
        };
        var ok = NumericConfig.parse("{\"flag\":false,\"choice\":\"simple\",\"count\":8}",
            defaults, numRules, typedRules, 1);
        assertTrue(ok.validDocument());
        assertEquals(0, ok.warnings().size());
        assertFalse(ok.data().get("flag").getAsBoolean());
        assertEquals("simple", ok.data().get("choice").getAsString());

        var bad = NumericConfig.parse("{\"flag\":1,\"choice\":\"other\",\"count\":8}",
            defaults, numRules, typedRules, 1);
        assertTrue(bad.validDocument());
        assertEquals(2, bad.warnings().size());
        assertTrue(bad.data().get("flag").getAsBoolean());
        assertEquals("full", bad.data().get("choice").getAsString());
        assertEquals(8, bad.data().get("count").getAsInt());
    }
}
