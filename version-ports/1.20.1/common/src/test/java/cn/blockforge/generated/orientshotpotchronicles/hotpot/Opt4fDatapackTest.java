// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 4F-2：数据包汤底 / 蘸料配方编解码、目录前缀约定、热重载叠加表及 Config > Datapack > Default 优先级单测（1.20.1 适配）。作者：mryt233
 */
class Opt4fDatapackTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
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
        ResourceLocation soupId = new ResourceLocation("pack", "soup/custom");
        SoupBaseRecipe parsedSoup = SoupBaseRecipe.SERIALIZER.fromJson(soupId,
            JsonParser.parseString(soupJson).getAsJsonObject());
        FriendlyByteBuf soupBuf = new FriendlyByteBuf(Unpooled.buffer());
        SoupBaseRecipe decodedSoup;
        try {
            SoupBaseRecipe.SERIALIZER.toNetwork(soupBuf, parsedSoup);
            decodedSoup = SoupBaseRecipe.SERIALIZER.fromNetwork(soupId, soupBuf);
        } finally {
            soupBuf.release();
        }
        assertEquals(parsedSoup.getId(), decodedSoup.getId());
        assertEquals(parsedSoup.source(), decodedSoup.source());
        assertEquals(parsedSoup.remainder(), decodedSoup.remainder());
        assertEquals(parsedSoup.additive(), decodedSoup.additive());
        assertEquals(parsedSoup.speedMultiplier(), decodedSoup.speedMultiplier());
        assertEquals(parsedSoup.bowlFood(), decodedSoup.bowlFood());
        assertEquals(parsedSoup.mainEffect(), decodedSoup.mainEffect());
        assertEquals(parsedSoup.secondEffect(), decodedSoup.secondEffect());
        assertEquals(parsedSoup.fireEffect(), decodedSoup.fireEffect());
        assertEquals(parsedSoup.hungerEffect(), decodedSoup.hungerEffect());
        assertEquals(parsedSoup.clearsNausea(), decodedSoup.clearsNausea());
        assertEquals(parsedSoup.spicy(), decodedSoup.spicy());
        assertEquals(parsedSoup.langKey(), decodedSoup.langKey());
        assertEquals(parsedSoup.bowlId(), decodedSoup.bowlId());
        assertEquals(parsedSoup.screenColors(), decodedSoup.screenColors());
        assertEquals(parsedSoup.bubbleColors(), decodedSoup.bubbleColors());
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
        ResourceLocation sauceId = new ResourceLocation("pack", "seasoning/herb");
        SeasoningRecipe parsedSauce = SeasoningRecipe.SERIALIZER.fromJson(sauceId,
            JsonParser.parseString(singleRawJson).getAsJsonObject());
        FriendlyByteBuf sauceBuf = new FriendlyByteBuf(Unpooled.buffer());
        SeasoningRecipe decodedSauce;
        try {
            SeasoningRecipe.SERIALIZER.toNetwork(sauceBuf, parsedSauce);
            decodedSauce = SeasoningRecipe.SERIALIZER.fromNetwork(sauceId, sauceBuf);
        } finally {
            sauceBuf.release();
        }
        assertEquals(parsedSauce.getId(), decodedSauce.getId());
        assertEquals(parsedSauce.raws(), decodedSauce.raws());
        assertEquals(parsedSauce.color(), decodedSauce.color());
        assertEquals(parsedSauce.effect(), decodedSauce.effect());
        assertEquals(parsedSauce.hunger(), decodedSauce.hunger());
        assertEquals(parsedSauce.removes(), decodedSauce.removes());
        assertEquals(parsedSauce.garnish(), decodedSauce.garnish());
        assertEquals(parsedSauce.nameKey(), decodedSauce.nameKey());
        assertEquals(parsedSauce.effectKey(), decodedSauce.effectKey());
        assertEquals(List.of(Items.BROWN_MUSHROOM), decodedSauce.raws().orElseThrow());
        assertEquals(0xFF7CB342, decodedSauce.color().orElseThrow());
        assertEquals(1, decodedSauce.effect().orElseThrow().amplifier());

        // 非法颜色、非法物品 ID、负时长或超范围应解析失败
        String badJson = """
            { "raws": "minecraft:sugar", "color": "#123" }
            """;
        assertThrows(RuntimeException.class, () -> SeasoningRecipe.SERIALIZER.fromJson(
            sauceId, JsonParser.parseString(badJson).getAsJsonObject()));
        String badItemJson = """
            { "raws": "no_such:missing_item", "color": "#FFFFFF" }
            """;
        assertThrows(RuntimeException.class, () -> SeasoningRecipe.SERIALIZER.fromJson(
            sauceId, JsonParser.parseString(badItemJson).getAsJsonObject()));
        String badSoupJson = """
            {
              "source": "minecraft:carrot",
              "main_effect": { "id": "minecraft:speed", "duration": -10 }
            }
            """;
        assertThrows(RuntimeException.class, () -> SoupBaseRecipe.SERIALIZER.fromJson(
            soupId, JsonParser.parseString(badSoupJson).getAsJsonObject()));
        String badSoupSourceJson = """
            { "source": "no_such:missing_item" }
            """;
        assertThrows(RuntimeException.class, () -> SoupBaseRecipe.SERIALIZER.fromJson(
            soupId, JsonParser.parseString(badSoupSourceJson).getAsJsonObject()));
    }

    @Test
    void pathPrefixStrippingAndOverlayOrderWork() {
        Soups.Registry soupReg = new Soups.Registry();
        for (SoupBase base : SoupBase.values()) {
            soupReg.registerBuiltin(base.def());
        }
        ResourceLocation badSoupRecipe = new ResourceLocation("pack", "ginger");
        ResourceLocation goodSoupRecipe = new ResourceLocation("pack", "soup/ginger");
        ResourceLocation clearOverrideRecipe = HotPot.id("soup/clear");

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
            customSoup.withId(badSoupRecipe),
            customSoup.withId(goodSoupRecipe),
            clearOverride.withId(clearOverrideRecipe)
        ), warns::add);

        assertEquals(1, warns.size(), "未放在 recipe/soup/ 下的配方应记警告并跳过");
        assertEquals(7, overlaid.size());
        assertEquals(HotPot.id("clear"), overlaid.byIndex(0).id(), "内置项位置不变");
        assertEquals(2.4F, overlaid.byIndex(0).defaultSpeedMul());
        assertEquals(Items.WATER_BUCKET, overlaid.byIndex(0).source(), "部分覆盖保留原 source");
        assertEquals(new ResourceLocation("pack", "ginger"), overlaid.byIndex(6).id());

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
            java.util.Optional.of(new SeasoningRecipe.EffectSpec(MobEffects.DIG_SPEED, 200, 1)),
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.Optional.of(true),
            java.util.Optional.empty(),
            java.util.Optional.empty());
        List<String> sauceWarns = new ArrayList<>();
        Seasonings.Registry.View sauceView = sauceReg.overlay(List.of(
            truffle.withId(new ResourceLocation("pack", "bad_sauce")),
            truffle.withId(new ResourceLocation("pack", "seasoning/truffle"))
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
            SoupDef datapackClear = SoupBase.CLEAR.def().toBuilder().speed(2.4F).build();
            SoupDef foreignClear = SoupDef.builder(new ResourceLocation("pack", "clear"))
                .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
                .speed(3.1F)
                .langKey("item.minecraft.beetroot_soup")
                .build();
            assertEquals(2.4F, datapackClear.speedMul(), 1e-4F);
            assertEquals(3.1F, foreignClear.speedMul(), 1e-4F);
            assertEquals("item.minecraft.beetroot_soup", foreignClear.nameKey(), "带点号自定义语言键应原样保留");

            SeasoningDef datapackMsg = Seasoning.MSG.def().toBuilder().hunger(5).build();
            SeasoningDef foreignMsg = SeasoningDef.builder(new ResourceLocation("pack", "msg"))
                .raw(() -> Items.RED_MUSHROOM)
                .color(0xFFB71C1C)
                .hunger(6)
                .build();
            assertEquals(5, Config.overriddenSeasoningHunger(datapackMsg.id(), 5));
            assertFalse(Seasonings.isBuiltin(foreignMsg.id()));

            Path cfgFile = dir.resolve("orients_hotpot_chronicles.json");
            JsonObject root = JsonParser.parseString(Files.readString(cfgFile, StandardCharsets.UTF_8)).getAsJsonObject();
            root.getAsJsonObject("soup_speed").addProperty("clear", 1.7);
            root.getAsJsonObject("seasoning").getAsJsonObject("msg").addProperty("hunger", 8);
            Files.writeString(cfgFile, root.toString(), StandardCharsets.UTF_8);
            Config.reload();
            assertEquals(1.7F, datapackClear.speedMul(), 1e-4F, "config 显式修改应覆盖数据包");
            assertEquals(3.1F, foreignClear.speedMul(), 1e-4F, "异命名空间条目不得串用内置 config");
            assertEquals(8, Config.overriddenSeasoningHunger(datapackMsg.id(), 5));

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
