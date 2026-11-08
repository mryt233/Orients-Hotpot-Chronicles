// 作者：mryt233（2026-11-04，r134b）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.pure.ConfigStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** 豆腐改 ID 后配置旧键迁移：旧三组键改名、新旧同时存在以新为准、没旧键不动文件、坏文件不动。 */
class ConfigMigrationTest {
    private static final long MAX_BYTES = 65536;

    @TempDir
    Path directory;

    private Path file;
    private List<String> warnings;
    /** 和游戏里一样用框架的文档有效性判断（格式版本 1）。 */
    private Predicate<String> valid;

    @BeforeEach
    void setUp() {
        file = directory.resolve("orients_hotpot_chronicles.json");
        warnings = new ArrayList<>();
        ConfigStore store = new ConfigStore("orients_hotpot_chronicles.json", 1, MAX_BYTES, "",
            path -> null, JsonObject::new, warnings::add);
        valid = store::isValidDocument;
    }

    @Test
    void renamesAllThreeLegacyGroups() throws IOException {
        write("""
            {
              "_format": 1,
              "food": {
                "beef_roll": {"hunger": 3, "saturation": 0.4},
                "tofu": {"hunger": 5, "saturation": 0.6, "cook_ticks": 240, "overcook_ticks": 700},
                "tofu_cube": {"hunger": 1, "saturation": 0.2},
                "cooked_tofu_cube": {"hunger": 3, "saturation": 0.5}
              },
              "cooking": {"fallback_ticks": 200},
              "custom_note": "keep me"
            }
            """);

        List<String> moved = migrate();

        JsonObject root = read();
        JsonObject food = root.getAsJsonObject("food");
        assertFalse(food.has("tofu"));
        assertFalse(food.has("tofu_cube"));
        assertFalse(food.has("cooked_tofu_cube"));
        assertEquals(json("{\"hunger\":5,\"saturation\":0.6}"), food.get("cold_tofu"));
        assertEquals(json("{\"hunger\":1,\"saturation\":0.2}"), food.get("cold_tofu_cube"));
        assertEquals(json("{\"hunger\":3,\"saturation\":0.5}"), food.get("hot_tofu_cube"));
        assertEquals(json("{\"fallback_ticks\":200,\"tofu_cook_ticks\":240,\"tofu_overcook_ticks\":700}"),
            root.get("cooking"));
        // 其他内容原样保留，键的先后顺序不乱
        assertEquals(json("{\"hunger\":3,\"saturation\":0.4}"), food.get("beef_roll"));
        assertEquals("keep me", root.get("custom_note").getAsString());
        assertEquals(1, root.get("_format").getAsInt());
        assertEquals(List.of("beef_roll", "cold_tofu", "cold_tofu_cube", "hot_tofu_cube"),
            List.copyOf(food.keySet()));

        assertTrue(moved.contains("food.tofu.hunger -> food.cold_tofu.hunger"));
        assertTrue(moved.contains("food.tofu.cook_ticks -> cooking.tofu_cook_ticks"));
        assertTrue(moved.contains("food.tofu_cube -> food.cold_tofu_cube"));
        assertTrue(moved.contains("food.cooked_tofu_cube -> food.hot_tofu_cube"));
        assertTrue(warnings.isEmpty());
        assertNoTempFiles();
    }

    @Test
    void createsCookingGroupWhenMissing() throws IOException {
        write("{\"food\":{\"tofu\":{\"cook_ticks\":100}}}");

        migrate();

        JsonObject root = read();
        assertFalse(root.getAsJsonObject("food").has("tofu"));
        assertFalse(root.getAsJsonObject("food").has("cold_tofu"));
        assertEquals(json("{\"tofu_cook_ticks\":100}"), root.get("cooking"));
    }

    @Test
    void newNamesWinWhenBothExist() throws IOException {
        write("""
            {
              "food": {
                "tofu": {"hunger": 9, "saturation": 0.9, "cook_ticks": 999},
                "cold_tofu": {"hunger": 4},
                "tofu_cube": {"hunger": 9},
                "cold_tofu_cube": {"hunger": 2},
                "cooked_tofu_cube": {"hunger": 9},
                "hot_tofu_cube": {"hunger": 6}
              },
              "cooking": {"tofu_cook_ticks": 300}
            }
            """);

        List<String> moved = migrate();

        JsonObject root = read();
        JsonObject food = root.getAsJsonObject("food");
        // hunger 以新名字为准；新名字里没有的 saturation 从旧键搬过来
        assertEquals(json("{\"hunger\":4,\"saturation\":0.9}"), food.get("cold_tofu"));
        assertEquals(json("{\"hunger\":2}"), food.get("cold_tofu_cube"));
        assertEquals(json("{\"hunger\":6}"), food.get("hot_tofu_cube"));
        assertEquals(json("{\"tofu_cook_ticks\":300}"), root.get("cooking"));
        assertFalse(food.has("tofu"));
        assertFalse(food.has("tofu_cube"));
        assertFalse(food.has("cooked_tofu_cube"));
        assertTrue(moved.contains("food.tofu.hunger -> food.cold_tofu.hunger (kept existing)"));
        assertTrue(moved.contains("food.tofu_cube -> food.cold_tofu_cube (kept existing)"));
    }

    @Test
    void fileWithoutLegacyKeysIsNotTouched() throws IOException {
        String text = "{\n  \"food\": {\"cold_tofu\": {\"hunger\": 4}},\r\n  \"cooking\": {\"tofu_cook_ticks\": 200}\n}";
        write(text);
        byte[] before = Files.readAllBytes(file);
        var modified = Files.getLastModifiedTime(file);

        List<String> moved = migrate();

        assertTrue(moved.isEmpty());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(modified, Files.getLastModifiedTime(file));
        assertNoTempFiles();
    }

    @Test
    void brokenJsonIsNotTouched() throws IOException {
        String text = "{\"food\": {\"tofu\": {\"hunger\": 5,}  // 少个括号";
        write(text);
        byte[] before = Files.readAllBytes(file);

        List<String> moved = migrate();

        assertTrue(moved.isEmpty());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertTrue(warnings.isEmpty());
        assertNoTempFiles();
    }

    @Test
    void unsupportedFormatIsNotTouched() throws IOException {
        String text = "{\"_format\": 2, \"food\": {\"tofu\": {\"hunger\": 5}}}";
        write(text);
        byte[] before = Files.readAllBytes(file);

        assertTrue(migrate().isEmpty());
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void missingFileDoesNothing() {
        assertTrue(migrate().isEmpty());
        assertFalse(Files.exists(file));
    }

    @Test
    void secondRunIsANoOp() throws IOException {
        write("{\"food\":{\"tofu_cube\":{\"hunger\":1}}}");
        assertFalse(migrate().isEmpty());
        byte[] after = Files.readAllBytes(file);

        assertTrue(migrate().isEmpty());
        assertArrayEquals(after, Files.readAllBytes(file));
    }

    // ---------------------------------------------------------------- r139：汤碗 slow_* 旧键

    @Test
    void movesSoupBowlSlowDurationIntoSecondDuration() throws IOException {
        write("""
            {
              "_format": 1,
              "soup_bowl": {
                "rich_stock": {"hunger": 4, "saturation": 0.9, "duration": 300, "amplifier": 0,
                    "hunger_duration": 0, "hunger_amplifier": 0, "slow_duration": 60, "slow_amplifier": 0},
                "clear": {"duration": 300}
              },
              "custom_note": "keep me"
            }
            """);

        List<String> moved = migrate();

        JsonObject soup = read().getAsJsonObject("soup_bowl");
        JsonObject rich = soup.getAsJsonObject("rich_stock");
        assertFalse(rich.has("slow_duration"));
        assertFalse(rich.has("slow_amplifier"));
        assertEquals(60, rich.get("second_duration").getAsInt());
        assertEquals(300, rich.get("duration").getAsInt());
        // 没有 slow_* 的汤底原样不动
        assertEquals(json("{\"duration\":300}"), soup.get("clear"));
        assertEquals("keep me", read().get("custom_note").getAsString());

        assertTrue(moved.contains("soup_bowl.rich_stock.slow_duration -> soup_bowl.rich_stock.second_duration"));
        assertTrue(moved.contains("soup_bowl.rich_stock.slow_amplifier (dropped)"));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void existingSecondDurationWinsOverSlowDuration() throws IOException {
        write("""
            {"soup_bowl": {"rich_stock": {"second_duration": 600, "slow_duration": 60, "slow_amplifier": 2}}}
            """);

        List<String> moved = migrate();

        JsonObject rich = read().getAsJsonObject("soup_bowl").getAsJsonObject("rich_stock");
        assertEquals(600, rich.get("second_duration").getAsInt());
        assertFalse(rich.has("slow_duration"));
        assertFalse(rich.has("slow_amplifier"));
        assertTrue(moved.contains("soup_bowl.rich_stock.slow_duration (dropped)"));
    }

    @Test
    void soupBowlWithoutSlowKeysIsNotTouched() throws IOException {
        String text = "{\"soup_bowl\":{\"clear\":{\"duration\":300,\"second_duration\":1}}}";
        write(text);
        byte[] before = Files.readAllBytes(file);

        assertTrue(migrate().isEmpty());
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    private List<String> migrate() {
        return ConfigMigration.migrate(file, MAX_BYTES, valid, warnings::add);
    }

    private void write(String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private JsonObject read() throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private void assertNoTempFiles() throws IOException {
        try (var files = Files.list(directory)) {
            assertEquals(List.of(file.getFileName().toString()),
                files.map(p -> p.getFileName().toString()).toList());
        }
    }
}
