// 作者：mryt233
package cn.blockforge.orient.framework.pure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumericConfigTest {
    private JsonObject defaults;

    private static final NumericConfig.Rules RULES = path -> switch (path) {
        case "limit", "nested.count" -> new NumericConfig.Rule(0, 10, true);
        case "ratio", "nested.weight" -> new NumericConfig.Rule(0, 2, false);
        default -> null;
    };

    @BeforeEach
    void setUp() {
        defaults = json("""
            {"limit":4,"ratio":1.25,"nested":{"count":2,"weight":0.5}}
            """);
    }

    @Test
    void validValuesOverrideDefaultsAndKeepMissingFields() {
        var result = parse("{\"limit\":8,\"ratio\":1.75}");

        assertTrue(result.validDocument());
        assertTrue(result.warnings().isEmpty());
        assertEquals(json("{\"limit\":8,\"ratio\":1.75,\"nested\":{\"count\":2,\"weight\":0.5}}"), result.data());
    }

    @Test
    void nestedValuesAreValidatedByTheirFullPath() {
        var result = parse("{\"nested\":{\"count\":7}}");

        assertTrue(result.warnings().isEmpty());
        assertEquals(7, result.data().getAsJsonObject("nested").get("count").getAsInt());
        assertEquals(0.5, result.data().getAsJsonObject("nested").get("weight").getAsDouble());
        assertEquals(4, result.data().get("limit").getAsInt());
    }

    @Test
    void metadataKeysAreIgnoredAtEveryLevel() {
        var result = parse("""
            {"_format":1,"_说明":{"free":"text"},"_unknown":false,
             "nested":{"_help":["anything"],"count":6}}
            """);

        assertTrue(result.validDocument());
        assertTrue(result.warnings().isEmpty());
        assertEquals(json("{\"limit\":4,\"ratio\":1.25,\"nested\":{\"count\":6,\"weight\":0.5}}"), result.data());
    }

    @Test
    void unknownKeysWarnWithTheirPathsAndAreNotAdded() {
        var result = parse("{\"extra\":1,\"nested\":{\"extra\":2}}");

        assertTrue(result.validDocument());
        assertEquals(defaults, result.data());
        assertEquals(2, result.warnings().size());
        assertEquals(List.of("extra", "nested.extra"), result.warnings().stream()
            .map(w -> w.args().getFirst()).toList());
        assertTrue(result.warnings().stream().allMatch(w -> w.key().equals(key("unknown_key"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"3\"", "true", "null", "[]", "{}"})
    void nonNumericValuesKeepDefaultsAndWarn(String value) {
        var result = parse("{\"limit\":" + value + ",\"ratio\":1.5}");

        assertTrue(result.validDocument());
        assertEquals(4, result.data().get("limit").getAsInt());
        assertEquals(1.5, result.data().get("ratio").getAsDouble());
        assertWarning(result, "not_number", List.of("limit"));
    }

    @Test
    void fractionIsRejectedForAnIntegerRule() {
        var result = parse("{\"limit\":3.5}");

        assertEquals(4, result.data().get("limit").getAsInt());
        assertWarning(result, "not_integer", List.of("limit"));
    }

    @Test
    void decimalRepresentationOfAWholeNumberIsAccepted() {
        var result = parse("{\"limit\":3.0}");

        assertTrue(result.warnings().isEmpty());
        assertEquals(3, result.data().get("limit").getAsInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void outOfRangeWarnsWithPathAndBounds(int value) {
        var result = parse("{\"nested\":{\"count\":" + value + "}}");

        assertEquals(2, result.data().getAsJsonObject("nested").get("count").getAsInt());
        assertWarning(result, "out_of_range", List.of("nested.count", "0", "10"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void rangeEndpointsAreInclusive(int value) {
        var result = parse("{\"limit\":" + value + "}");

        assertTrue(result.warnings().isEmpty());
        assertEquals(value, result.data().get("limit").getAsInt());
    }

    @Test
    void numberInPlaceOfAnObjectKeepsTheWholeDefaultObject() {
        var result = parse("{\"nested\":3}");

        assertEquals(defaults, result.data());
        assertWarning(result, "not_object", List.of("nested"));
    }

    @Test
    void malformedJsonIsAnInvalidDocument() {
        var result = parse("{\"limit\":");

        assertFalse(result.validDocument());
        assertEquals(defaults, result.data());
        assertWarning(result, "json_invalid", List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "3", "\"text\"", "null"})
    void nonObjectRootIsAnInvalidDocument(String text) {
        var result = parse(text);

        assertFalse(result.validDocument());
        assertEquals(defaults, result.data());
        assertWarning(result, "root_not_object", List.of());
    }

    @Test
    void unsupportedFormatDoesNotApplyAnyUserValues() {
        var result = parse("{\"_format\":2,\"limit\":9}");

        assertFalse(result.validDocument());
        assertEquals(defaults, result.data());
        assertWarning(result, "format_unsupported", List.of());
    }

    @Test
    void missingValidationRuleFailsWithTheFieldPath() {
        var error = assertThrows(IllegalStateException.class,
            () -> NumericConfig.parse("{\"nested\":{\"count\":3}}", defaults, path -> null, 1));

        assertTrue(error.getMessage().contains("nested.count"));
    }

    @Test
    void resultsAndDefaultsHaveIndependentNestedObjects() {
        var result = parse("{\"limit\":8}");
        JsonObject original = defaults.deepCopy();

        // 仅为验证副本隔离而修改结果；业务代码仍按只读快照使用。
        result.data().getAsJsonObject("nested").addProperty("count", 9);
        assertEquals(original, defaults);
        defaults.addProperty("limit", 1);
        assertEquals(8, result.data().get("limit").getAsInt());
    }

    private NumericConfig.Result parse(String text) {
        return NumericConfig.parse(text, defaults, RULES, 1);
    }

    private static void assertWarning(NumericConfig.Result result, String name, List<String> args) {
        assertEquals(1, result.warnings().size());
        assertEquals(key(name), result.warnings().getFirst().key());
        assertEquals(args, result.warnings().getFirst().args());
    }

    private static String key(String name) {
        return "orient_framework.config." + name;
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
