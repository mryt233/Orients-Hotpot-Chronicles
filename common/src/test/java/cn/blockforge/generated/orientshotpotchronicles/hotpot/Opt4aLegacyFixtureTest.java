// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 4A：旧档基线夹具的自检。作者：mryt233
 *
 * <p>夹具来自已发布的 0.2.2-alpha 包真跑游戏后用 {@code /data get} 导出的原始 SNBT
 * （{@code tools/legacy_baseline.py}，说明见 {@code tools/legacy_baseline说明.md}）。
 * 这里只证明两件事：夹具在单元测试环境里能被原版 {@link TagParser} 读回；
 * 里面的关键数字和建档时摆的一致（后面 4D-1 / 4E-1 的单测直接用这份文件当旧格式输入）。
 * 不涉及任何产品代码行为，所以只用到 {@link SauceContents} 的编解码器，不需要注册平台。</p>
 */
class Opt4aLegacyFixtureTest {
    private static final String DIR = "/opt4/legacy-0.2.2-alpha/";
    private static JsonObject entries;

    @BeforeAll
    static void load() throws Exception {
        entries = readJson("fixtures.json").getAsJsonObject("entries");
    }

    private static JsonObject readJson(String name) throws Exception {
        try (InputStream in = Opt4aLegacyFixtureTest.class.getResourceAsStream(DIR + name)) {
            assertNotNull(in, "缺少夹具文件 " + name);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Tag parse(String snbt) throws Exception {
        return new TagParser(new StringReader(snbt)).readValue();
    }

    private static CompoundTag tag(String name) throws Exception {
        JsonObject entry = entries.getAsJsonObject(name);
        assertNotNull(entry, "夹具里没有 " + name);
        assertTrue(entry.has("snbt"), name + " 不是正常的 data get 回执");
        return (CompoundTag) parse(entry.get("snbt").getAsString());
    }

    @Test
    void everyEntryParsesWithVanillaTagParser() throws Exception {
        assertEquals(102, entries.size());
        for (String name : entries.keySet()) {
            JsonObject entry = entries.getAsJsonObject(name);
            if (entry.has("snbt")) {
                assertNotNull(parse(entry.get("snbt").getAsString()), name);
            } else {
                assertTrue(entry.has("raw"), name);
            }
        }
    }

    @Test
    void potsKeepOrdinalAndSpiceKeys() throws Exception {
        String[] names = {"pot_clear", "pot_rich_stock", "pot_mushroom", "pot_spicy_l0", "pot_spicy_l1",
            "pot_spicy_l2", "pot_spicy_l3", "pot_tomato", "pot_three_fresh"};
        int[] bases = {0, 1, 2, 3, 3, 3, 3, 4, 5};
        int[] spicy = {0, 0, 0, 0, 1, 2, 3, 0, 0};
        for (int i = 0; i < names.length; i++) {
            CompoundTag pot = tag(names[i]);
            assertEquals(bases[i], pot.getInt("Base"), names[i]);
            assertEquals(spicy[i], pot.getInt("Spicy"), names[i]);
            assertEquals(-1, pot.getInt("BaseB"), names[i]);
        }
        CompoundTag four = tag("pot_four_all");
        assertArrayEquals(new int[] {0, 3, 4, 5},
            new int[] {four.getInt("Base"), four.getInt("BaseB"), four.getInt("BaseC"), four.getInt("BaseD")});
        assertEquals(1, four.getInt("SpicyB"));
        assertEquals(-1, tag("pot_empty").getInt("Base"));
        assertEquals(81, tag("pot_with_food").getIntArray("Doneness").length);
    }

    @Test
    void packedPotCarriesTheSameKeysAsTheBlock() throws Exception {
        CompoundTag item = tag("chest_packed_pots.a6");   // 第 7 口：红汤特辣档（辣度 3）
        CompoundTag contents = item.getCompound("components").getCompound("orients_hotpot_chronicles:pot_contents");
        assertEquals(3, contents.getInt("Base"));
        assertEquals(3, contents.getInt("Spicy"));
        assertFalse(contents.contains("x"), "打包锅里不应带方块坐标");
    }

    @Test
    void sauceComponentsDecodeWithTheCurrentCodec() throws Exception {
        int[][] expected = {{1, 9}, {16384, 9}, {32767, 9}, {32767, 4}, {16383, 9}, {32766, 9}, {10, 1}, {0, 9},
            {32771, 9}, {255, 0}};
        for (int slot = 0; slot < expected.length; slot++) {
            CompoundTag item = tag("chest_sauce_bowls.slot" + slot);
            Tag raw = item.getCompound("components").get("orients_hotpot_chronicles:sauce");
            SauceContents contents = SauceContents.CODEC.parse(NbtOps.INSTANCE, raw).getOrThrow();
            assertEquals(expected[slot][0], contents.mask(), "slot" + slot);
            assertEquals(expected[slot][1], contents.bites(), "slot" + slot);
        }
        assertFalse(tag("chest_sauce_bowls.slot10").contains("components"), "无组件的蘸料碗");
        // 14 种满的碗和 15 种满的碗只差最高位
        assertEquals(SauceContents.CODEC.parse(NbtOps.INSTANCE,
                tag("chest_sauce_bowls.slot2").getCompound("components").get("orients_hotpot_chronicles:sauce"))
            .getOrThrow().mask() & ~16384,
            SauceContents.CODEC.parse(NbtOps.INSTANCE,
                tag("chest_sauce_bowls.slot4").getCompound("components").get("orients_hotpot_chronicles:sauce"))
            .getOrThrow().mask());
    }

    @Test
    void dippedAndCookedInSoupAreRawNumbersAndNames() throws Exception {
        CompoundTag dish = tag("chest_dipped_dishes.slot9").getCompound("components");
        assertEquals(8, dish.getInt("orients_hotpot_chronicles:dipped"));
        assertEquals(3, dish.getInt("orients_hotpot_chronicles:spicy_level"));
        assertEquals("spicy", dish.getString("orients_hotpot_chronicles:cooked_in_soup"));
        assertEquals(32768, tag("chest_dipped_dishes.slot12").getCompound("components")
            .getInt("orients_hotpot_chronicles:dipped"));
        assertEquals("minecraft:unknown_future", tag("chest_dipped_dishes.slot15").getCompound("components")
            .getString("orients_hotpot_chronicles:cooked_in_soup"));
    }

    @Test
    void sauceStationKindsIncludeLegacyFill() throws Exception {
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}, tag("station_kinds_0_8").getIntArray("Kinds"));
        assertArrayEquals(new int[] {9, 10, 11, 12, 13, 14, -1, -1, -1}, tag("station_kinds_9_14").getIntArray("Kinds"));
        // 没有 Kinds 的老存档被旧版读成「第 i 格 = 第 i 种」，再存出来就带 Kinds 了
        assertArrayEquals(new int[] {0, -1, 2, -1, -1, -1, -1, -1, 8}, tag("station_legacy_no_kinds").getIntArray("Kinds"));
    }

    @Test
    void tableAndEntityItemsAreKept() throws Exception {
        ListTag plates = tag("table_andesite_bowls_all_soups").getList("Plates", Tag.TAG_COMPOUND);
        assertEquals(6, plates.size());
        assertEquals("orients_hotpot_chronicles:three_fresh_soup_bowl", plates.getCompound(5).getCompound("Self").getString("id"));
        assertEquals("orients_hotpot_chronicles:bamboo_chopsticks",
            ((CompoundTag) ((ListTag) parse(entries.getAsJsonObject("entity_stand_hands").get("snbt").getAsString())).get(0))
                .getString("id"));
    }

    @Test
    void configFixturesAreOldFormatWithPlayerEdits() throws Exception {
        JsonObject generated = readJson("config-generated.json");
        JsonObject edited = readJson("config-edited.json");
        assertEquals(1, generated.get("_format").getAsInt());
        assertEquals(120, generated.getAsJsonObject("soup_bowl").getAsJsonObject("spicy").get("duration").getAsInt());
        assertEquals(240, edited.getAsJsonObject("soup_bowl").getAsJsonObject("spicy").get("duration").getAsInt());
        assertEquals(100, edited.getAsJsonObject("seasoning").getAsJsonObject("honey").get("duration").getAsInt());
        assertTrue(generated.has("seasoning") && generated.has("soup_bowl"));
    }
}
