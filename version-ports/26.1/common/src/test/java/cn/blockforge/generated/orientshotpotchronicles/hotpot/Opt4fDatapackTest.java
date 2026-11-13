// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 4F-2：数据包汤底 / 蘸料配方编解码、目录前缀约定、热重载叠加表及 Config > Datapack > Default 优先级单测。作者：mryt233
 */
class Opt4fDatapackTest {
    private static RegistryAccess.Frozen access;
    private static RegistryOps<com.google.gson.JsonElement> jsonOps;

    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
        access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        jsonOps = RegistryOps.create(JsonOps.INSTANCE, access);
    }

    @Test
    void soupBaseAndSeasoningCodecAndStreamRoundTrip() {
        String soupJson = """
            {
              "source": "minecraft:carrot",
              "remainder": "minecraft:bowl",
              "speed_multiplier": 1.8,
              "clears_nausea": true,
              "bowl_food": { "hunger": 7, "saturation": 0.9 },
              "main_effect": { "id": "minecraft:regeneration", "duration": 220 },
              "screen_colors": { "base": "#FFD9C7A3", "hi": "#FFF1E5C9", "lo": "#FFB8A37D", "bits": ["#FF8C7A5B"] },
              "bubble_colors": { "body": "#FFF3EAD6", "hi": "#FFFFFFFF" }
            }
            """;
        SoupBaseRecipe parsedSoup = SoupBaseRecipe.SERIALIZER.codec().codec()
            .parse(jsonOps, JsonParser.parseString(soupJson)).getOrThrow();
        RegistryFriendlyByteBuf soupBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        SoupBaseRecipe.SERIALIZER.streamCodec().encode(soupBuf, parsedSoup);
        SoupBaseRecipe decodedSoup = SoupBaseRecipe.SERIALIZER.streamCodec().decode(soupBuf);
        assertEquals(parsedSoup, decodedSoup);
        assertEquals(Items.CARROT, decodedSoup.source().orElseThrow());
        assertEquals(0xFFD9C7A3, decodedSoup.screenColors().orElseThrow().base());

        // raws 同时兼容单字符串和数组
        String singleRawJson = """
            {
              "raws": "minecraft:brown_mushroom",
              "color": "#7CB342",
              "effect": { "id": "minecraft:haste", "duration": 240, "amplifier": 1 },
              "hunger": 3,
              "removes": ["minecraft:hunger"],
              "garnish": true
            }
            """;
        SeasoningRecipe parsedSauce = SeasoningRecipe.SERIALIZER.codec().codec()
            .parse(jsonOps, JsonParser.parseString(singleRawJson)).getOrThrow();
        RegistryFriendlyByteBuf sauceBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        SeasoningRecipe.SERIALIZER.streamCodec().encode(sauceBuf, parsedSauce);
        SeasoningRecipe decodedSauce = SeasoningRecipe.SERIALIZER.streamCodec().decode(sauceBuf);
        assertEquals(parsedSauce, decodedSauce);
        assertEquals(List.of(Items.BROWN_MUSHROOM), decodedSauce.raws().orElseThrow());
        assertEquals(0xFF7CB342, decodedSauce.color().orElseThrow());
        assertEquals(1, decodedSauce.effect().orElseThrow().amplifier());

        // 非法颜色、非法物品 ID、负时长或超范围应解析失败
        String badJson = """
            { "raws": "minecraft:sugar", "color": "#123" }
            """;
        assertTrue(SeasoningRecipe.SERIALIZER.codec().codec().parse(jsonOps, JsonParser.parseString(badJson)).error().isPresent());
        String badItemJson = """
            { "raws": "no_such:missing_item", "color": "#FFFFFF" }
            """;
        assertTrue(SeasoningRecipe.SERIALIZER.codec().codec().parse(jsonOps, JsonParser.parseString(badItemJson)).error().isPresent());
        String badSoupJson = """
            {
              "source": "minecraft:carrot",
              "main_effect": { "id": "minecraft:speed", "duration": -10 }
            }
            """;
        assertTrue(SoupBaseRecipe.SERIALIZER.codec().codec().parse(jsonOps, JsonParser.parseString(badSoupJson)).error().isPresent());
        String badSoupSourceJson = """
            { "source": "no_such:missing_item" }
            """;
        assertTrue(SoupBaseRecipe.SERIALIZER.codec().codec().parse(jsonOps, JsonParser.parseString(badSoupSourceJson)).error().isPresent());
    }

    @Test
    void pathPrefixStrippingAndOverlayOrderWork() {
        Soups.Registry soupReg = new Soups.Registry();
        for (SoupBase base : SoupBase.values()) {
            soupReg.registerBuiltin(base.def());
        }
        Identifier badSoupRecipe = Identifier.fromNamespaceAndPath("pack", "ginger");
        Identifier goodSoupRecipe = Identifier.fromNamespaceAndPath("pack", "soup/ginger");
        Identifier clearOverrideRecipe = HotPot.id("soup/clear");

        SoupBaseRecipe customSoup = new SoupBaseRecipe(
            java.util.Optional.of(Items.CARROT), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.of(1.8F), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty());
        SoupBaseRecipe clearOverride = new SoupBaseRecipe(
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.of(2.4F), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty());

        List<String> warns = new ArrayList<>();
        Soups.Registry.View overlaid = soupReg.overlay(List.of(
            new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, badSoupRecipe), customSoup),
            new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, goodSoupRecipe), customSoup),
            new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, clearOverrideRecipe), clearOverride)
        ), warns::add);

        assertEquals(1, warns.size(), "未放在 recipe/soup/ 下的配方应记警告并跳过");
        assertEquals(7, overlaid.size());
        assertEquals(HotPot.id("clear"), overlaid.byIndex(0).id(), "内置项位置不变");
        assertEquals(2.4F, overlaid.byIndex(0).defaultSpeedMul());
        assertEquals(Items.WATER_BUCKET, overlaid.byIndex(0).source(), "部分覆盖保留原 source");
        assertEquals(Identifier.fromNamespaceAndPath("pack", "ginger"), overlaid.byIndex(6).id());

        // /reload 移除数据包后回退基线
        Soups.Registry.View reloadedEmpty = soupReg.overlay(List.of(), warns::add);
        assertEquals(6, reloadedEmpty.size());
        assertEquals(1.0F, reloadedEmpty.byIndex(0).defaultSpeedMul());

        // 蘸料叠加、原料反查更新与错路径告警（选用原版物品内置项以在无模组物品注册的单测中构建索引）
        Seasonings.Registry sauceReg = new Seasonings.Registry();
        Seasoning[] vanillaBuiltins = {
            Seasoning.MSG, Seasoning.CHILI_OIL, Seasoning.SWEET_SOUR_SAUCE,
            Seasoning.HONEY, Seasoning.SESAME_OIL, Seasoning.VINEGAR, Seasoning.SUGAR
        };
        for (Seasoning s : vanillaBuiltins) {
            sauceReg.registerBuiltin(s.def());
        }
        SeasoningRecipe truffle = new SeasoningRecipe(
            java.util.Optional.of(List.of(Items.BROWN_MUSHROOM)),
            java.util.Optional.of(0xFF5C4033),
            java.util.Optional.of(new SeasoningRecipe.EffectSpec(MobEffects.HASTE, 200, 1)),
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.Optional.of(true),
            java.util.Optional.empty(),
            java.util.Optional.empty());
        List<String> sauceWarns = new ArrayList<>();
        Seasonings.Registry.View sauceView = sauceReg.overlay(List.of(
            new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("pack", "bad_sauce")), truffle),
            new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("pack", "seasoning/truffle")), truffle)
        ), sauceWarns::add);
        assertEquals(1, sauceWarns.size());
        assertEquals(vanillaBuiltins.length + 1, sauceView.size());
        SeasoningDef truffleDef = sauceView.byId("pack:truffle");
        assertNotNull(truffleDef);
        assertTrue(truffleDef.garnish(), "数据包 garnish=true 应在 SeasoningDef.garnish() 生效");
        assertSame(truffleDef, sauceView.raw(Items.BROWN_MUSHROOM));
        assertNull(sauceReg.baseView().raw(Items.BROWN_MUSHROOM), "基线视图不污染");
    }

    @Test
    void configOverridesDatapackWhichOverridesDefaultWithoutCrossNamespaceLeak(@TempDir Path dir) throws Exception {
        SoupBase.values();
        Seasoning.values();
        Config.init(dir, true);
        Config.saveDefaultsIfNeeded();
        try {
            // 1. 配置文件仅自动填入了模板默认 1.0：数据包 clear=2.4 生效；异命名空间 pack:clear 不读本模组 config
            SoupDef datapackClear = SoupBase.CLEAR.def().toBuilder().speed(2.4F).build();
            SoupDef foreignClear = SoupDef.builder(Identifier.fromNamespaceAndPath("pack", "clear"))
                .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
                .speed(3.1F)
                .langKey("item.minecraft.beetroot_soup")
                .build();
            assertEquals(2.4F, datapackClear.speedMul(), 1e-4F);
            assertEquals(3.1F, foreignClear.speedMul(), 1e-4F);
            assertEquals("item.minecraft.beetroot_soup", foreignClear.nameKey(), "带点号自定义语言键应原样保留");

            // 蘸料亦遵守 Config > Datapack > Default，且异命名空间 pack:msg 不读内置 msg 配置
            SeasoningDef datapackMsg = Seasoning.MSG.def().toBuilder().hunger(5).build();
            SeasoningDef foreignMsg = SeasoningDef.builder(Identifier.fromNamespaceAndPath("pack", "msg"))
                .raw(() -> Items.RED_MUSHROOM)
                .color(0xFFB71C1C)
                .hunger(6)
                .build();
            assertEquals(5, Config.overriddenSeasoningHunger(datapackMsg.id(), 5));
            assertFalse(Seasonings.isBuiltin(foreignMsg.id()));

            // 2. 玩家显式把 config 改到 1.7 / msg.hunger=8：Config > Datapack > Code Default
            Path cfgFile = dir.resolve("orients_hotpot_chronicles.json");
            JsonObject root = JsonParser.parseString(Files.readString(cfgFile, StandardCharsets.UTF_8)).getAsJsonObject();
            root.getAsJsonObject("soup_speed").addProperty("clear", 1.7);
            root.getAsJsonObject("seasoning").getAsJsonObject("msg").addProperty("hunger", 8);
            Files.writeString(cfgFile, root.toString(), StandardCharsets.UTF_8);
            Config.reload();
            assertEquals(1.7F, datapackClear.speedMul(), 1e-4F, "config 显式修改应覆盖数据包");
            assertEquals(3.1F, foreignClear.speedMul(), 1e-4F, "异命名空间条目不得串用内置 config");
            assertEquals(8, Config.overriddenSeasoningHunger(datapackMsg.id(), 5));

            // 3. 恢复回模板默认 1.0：数据包重新胜出
            root.getAsJsonObject("soup_speed").addProperty("clear", 1.0);
            Files.writeString(cfgFile, root.toString(), StandardCharsets.UTF_8);
            Config.reload();
            assertEquals(2.4F, datapackClear.speedMul(), 1e-4F);
        } finally {
            Path clean = dir.resolve("clean");
            Files.createDirectories(clean);
            Config.init(clean, false);
            Config.reload();
        }
    }
}
