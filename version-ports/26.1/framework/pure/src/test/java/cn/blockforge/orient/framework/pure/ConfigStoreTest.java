// 作者：mryt233
package cn.blockforge.orient.framework.pure;

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

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    private static final String FILE_NAME = "numeric.json";
    private static final String DEFAULTS = "{\"limit\":4,\"nested\":{\"count\":2,\"weight\":0.5}}";
    private static final String HELP = "{\"limit\":\"上限\",\"nested.count\":\"数量\"}";

    @TempDir
    Path directory;

    private ConfigStore store;
    private List<String> loggedWarnings;

    @BeforeEach
    void setUp() {
        loggedWarnings = new ArrayList<>();
        store = newStore(4096);
        store.init(directory);
    }

    @Test
    void missingFileLoadsDefaultsWithoutWarningsOrCreatingAFile() {
        var loaded = store.reload();

        assertEquals(json(DEFAULTS), loaded.data());
        assertEquals(loaded.data(), store.serverData());
        assertTrue(loaded.details().isEmpty());
        assertTrue(loaded.warnings().isEmpty());
        assertTrue(loggedWarnings.isEmpty());
        assertFalse(Files.exists(configFile()));
    }

    @Test
    void oversizedFileKeepsDefaultsAndItsOriginalBytes() throws IOException {
        var smallStore = newStore(8);
        smallStore.init(directory);
        byte[] original = "{\"limit\":8}".getBytes(StandardCharsets.UTF_8);
        Files.write(configFile(), original);

        var loaded = smallStore.reload();
        smallStore.saveDefaultsIfNeeded();

        assertEquals(json(DEFAULTS), loaded.data());
        assertEquals(List.of("orient_framework.config.too_large"), warningKeys(loaded));
        assertArrayEquals(original, Files.readAllBytes(configFile()));
    }

    @Test
    void saveCompletesMissingValuesAndKeepsUserAndUnknownEntries() throws IOException {
        Files.writeString(configFile(), """
            {"limit":8,"nested":{"count":7,"userField":99},"userObject":{"keep":1},"_custom":"keep"}
            """, StandardCharsets.UTF_8);

        store.saveDefaultsIfNeeded();
        JsonObject saved = json(Files.readString(configFile(), StandardCharsets.UTF_8));

        assertEquals(8, saved.get("limit").getAsInt());
        assertEquals(json("{\"count\":7,\"weight\":0.5,\"userField\":99}"), saved.getAsJsonObject("nested"));
        assertEquals(json("{\"keep\":1}"), saved.getAsJsonObject("userObject"));
        assertEquals("keep", saved.get("_custom").getAsString());
        assertEquals(1, saved.get("_format").getAsInt());
        assertEquals("测试数值配置", saved.get("_说明").getAsString());
        assertEquals(json(HELP), saved.getAsJsonObject("_字段说明"));
        assertTrue(loggedWarnings.isEmpty());
    }

    @Test
    void saveCreatesAFileWithDefaultsAndMetadata() throws IOException {
        store.saveDefaultsIfNeeded();

        JsonObject saved = json(Files.readString(configFile(), StandardCharsets.UTF_8));
        assertEquals(4, saved.get("limit").getAsInt());
        assertEquals(json(DEFAULTS).getAsJsonObject("nested"), saved.getAsJsonObject("nested"));
        assertEquals(1, saved.get("_format").getAsInt());
        assertEquals("测试数值配置", saved.get("_说明").getAsString());
        assertEquals(json(HELP), saved.getAsJsonObject("_字段说明"));
    }

    @Test
    void saveNeverRewritesMalformedJson() throws IOException {
        byte[] original = "{\n  \"说明\": \"保留原文\",\n  \"limit\": ".getBytes(StandardCharsets.UTF_8);
        Files.write(configFile(), original);

        var loaded = store.reload();
        store.saveDefaultsIfNeeded();

        assertEquals(List.of("orient_framework.config.json_invalid"), warningKeys(loaded));
        assertEquals(json(DEFAULTS), loaded.data());
        assertArrayEquals(original, Files.readAllBytes(configFile()));
    }

    @Test
    void loadRejectsAnEmptyDefaultTable() {
        var empty = new ConfigStore(FILE_NAME, 1, 4096, "note", path -> null,
            JsonObject::new, loggedWarnings::add);
        empty.init(directory);

        assertThrows(IllegalStateException.class, empty::load);
    }

    @Test
    void reloadRequiresAnInitializedDirectory() {
        var uninitialized = newStore(4096);

        assertThrows(IllegalStateException.class, uninitialized::reload);
    }

    // mryt233 加：没 init 时 load() 失败，补上 init 后再 load() 必须真的读文件（以前会被「已加载」标记跳过）
    @Test
    void loadRetriesAfterAFailedFirstAttempt() throws IOException {
        var late = newStore(4096);
        assertThrows(IllegalStateException.class, late::load);
        Files.writeString(configFile(), "{\"limit\":8}", StandardCharsets.UTF_8);
        late.init(directory);

        late.load();

        assertEquals(8, late.serverData().get("limit").getAsInt());
    }

    @Test
    void loadStartsClientAtDefaultsWhileServerUsesItsFile() throws IOException {
        Files.writeString(configFile(), "{\"limit\":8}", StandardCharsets.UTF_8);

        store.load();

        assertEquals(8, store.serverData().get("limit").getAsInt());
        assertEquals(store.defaults(), store.clientData());
        assertNotSame(store.defaults(), store.clientData());
    }

    @Test
    void acceptClientValidatesFieldsAndKeepsAnIndependentSnapshot() {
        store.load();
        JsonObject input = json("{\"limit\":100,\"nested\":{\"count\":\"bad\",\"weight\":1.5},\"unknown\":1}");

        store.acceptClient(input);
        input.getAsJsonObject("nested").addProperty("weight", 0);

        assertEquals(json("{\"limit\":4,\"nested\":{\"count\":2,\"weight\":1.5}}"), store.clientData());
        assertEquals(json(DEFAULTS), store.defaults());
        assertEquals(json(DEFAULTS), store.serverData());
    }

    @Test
    void mergeRecursivelyFillsGapsWithoutOverwritingInputValues() {
        JsonObject defaults = json(DEFAULTS);
        JsonObject input = json("{\"limit\":9,\"nested\":{\"count\":7,\"extra\":3},\"unknown\":5}");

        JsonObject merged = ConfigStore.merge(defaults, input);

        assertEquals(json("{\"limit\":9,\"nested\":{\"count\":7,\"weight\":0.5,\"extra\":3},\"unknown\":5}"), merged);
        assertEquals(json("{\"limit\":9,\"nested\":{\"count\":7,\"extra\":3},\"unknown\":5}"), input);
        merged.getAsJsonObject("nested").addProperty("count", 1);
        assertEquals(7, input.getAsJsonObject("nested").get("count").getAsInt());
        assertEquals(json(DEFAULTS), defaults);
    }

    @Test
    void mergePreservesAnExistingValueEvenWhenItsTypeDiffers() {
        JsonObject merged = ConfigStore.merge(json(DEFAULTS), json("{\"nested\":3}"));

        assertEquals(3, merged.get("nested").getAsInt());
        assertEquals(4, merged.get("limit").getAsInt());
    }

    @Test
    void legacyWarningsAreExactlyTheDetailedWarningsEnglishTexts() throws IOException {
        Files.writeString(configFile(), "{\"unknown\":1,\"limit\":100,\"nested\":{\"count\":\"bad\"}}", StandardCharsets.UTF_8);

        ConfigStore.Loaded loaded = store.reload();
        List<String> legacyWarnings = loaded.warnings();

        assertEquals(List.of("orient_framework.config.unknown_key", "orient_framework.config.out_of_range",
            "orient_framework.config.not_number"), warningKeys(loaded));
        assertEquals(loaded.details().stream().map(ConfigWarning::text).toList(), legacyWarnings);
        assertEquals(List.of("Unknown config entry unknown, ignored",
            "limit must be between 0 and 10; default used", "nested.count must be a number; default used"), legacyWarnings);
        assertEquals(legacyWarnings, loggedWarnings);
    }

    private ConfigStore newStore(long maxBytes) {
        var result = new ConfigStore(FILE_NAME, 1, maxBytes, "测试数值配置", path -> switch (path) {
            case "limit", "nested.count" -> new NumericConfig.Rule(0, 10, true);
            case "nested.weight" -> new NumericConfig.Rule(0, 2, false);
            default -> null;
        }, () -> json(HELP), loggedWarnings::add);
        json(DEFAULTS).entrySet().forEach(entry -> result.defaults().add(entry.getKey(), entry.getValue()));
        return result;
    }

    private Path configFile() {
        return directory.resolve(FILE_NAME);
    }

    private static List<String> warningKeys(ConfigStore.Loaded loaded) {
        return loaded.details().stream().map(ConfigWarning::key).toList();
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
