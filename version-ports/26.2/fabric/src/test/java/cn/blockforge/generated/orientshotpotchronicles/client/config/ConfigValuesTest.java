// 作者：mryt233；2026-11-11：实际数值读写、边界及编辑副本切换测试。
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigValuesTest {
    private static final Field REAL = new Field(new String[]{"group", "value"}, false, 0.1, 10, 0.05,
        Unit.PLAIN, null, "", null);
    private static final Field INTEGER = new Field(new String[]{"group", "count"}, true, 1, 100, 10,
        Unit.TICK, null, "", null);
    private final AtomicReference<JsonObject> edited = new AtomicReference<>(new JsonObject());
    private final JsonObject defaults = new JsonObject();
    private final ConfigValues values = new ConfigValues(edited::get, () -> defaults.deepCopy());

    @Test void missingAndNonnumericValuesUseFieldMinimum() {
        assertEquals(0.1, values.value(REAL));
        edited.get().getAsJsonObject("group").addProperty("value", "abc");
        assertEquals(0.1, values.value(REAL));
        edited.get().getAsJsonObject("group").addProperty("value", true);
        assertEquals(0.1, values.value(REAL));
    }
    @Test void writingReplacesMalformedParentsAndClampsBeforeRounding() {
        edited.get().addProperty("group", "old");
        values.setValue(REAL, -3); assertEquals(0.1, values.value(REAL));
        values.setValue(REAL, 20); assertEquals(10, values.value(REAL));
        values.setValue(INTEGER, 4.6); assertEquals(5, values.value(INTEGER));
        assertTrue(edited.get().getAsJsonObject("group").get("count").getAsJsonPrimitive().isNumber());
    }
    @Test void realWritesKeepTwoDecimalsAndIntegerWritesKeepIntegerText() {
        values.setValue(REAL, 1.234); assertEquals(1.23, values.value(REAL));
        values.setValue(INTEGER, 2.49);
        assertEquals("2", edited.get().getAsJsonObject("group").get("count").toString());
    }
    @Test void parserRejectsInvalidFiniteIntegerAndRangeInputs() {
        for (String text : new String[]{"", " ", "abc", "NaN", "Infinity", "-Infinity", "12.5", "0"})
            assertNull(ConfigValues.parse(REAL, text), text);
        assertNull(ConfigValues.parse(INTEGER, "12.5"));
        assertNull(ConfigValues.parse(null, "1")); assertNull(ConfigValues.parse(REAL, null));
        assertEquals(2.5, ConfigValues.parse(REAL, " 2.5 "));
    }
    @Test void parserPreservesExistingToleranceAtBoundsAndIntegerBoundary() {
        assertEquals(0.0999995, ConfigValues.parse(REAL, "0.0999995"));
        assertNull(ConfigValues.parse(REAL, "0.099998"));
        assertEquals(2.0000005, ConfigValues.parse(INTEGER, "2.0000005"));
        assertNull(ConfigValues.parse(INTEGER, "2.000002"));
    }
    @Test void defaultsAndReplacementEditedReferenceRemainIndependent() {
        ConfigValues.node(defaults, REAL.path(), 1).addProperty("value", 2.5);
        values.setValue(REAL, 2.5); assertTrue(values.isDefault(REAL));
        values.setValue(REAL, 3); assertFalse(values.isDefault(REAL));
        JsonObject old = edited.get(); edited.set(new JsonObject());
        values.setValue(REAL, 4); assertEquals(4, values.value(REAL));
        assertEquals(3, old.getAsJsonObject("group").get("value").getAsDouble());
        values.clearDefaults(); assertEquals(2.5, values.defaultValue(REAL));
    }
    @Test void nudgePreservesNormalShiftAndLimitSteps() {
        values.setValue(REAL, 1); values.nudge(REAL, 1, false); assertEquals(1.05, values.value(REAL));
        values.nudge(REAL, -1, true); assertEquals(0.55, values.value(REAL));
        values.setValue(INTEGER, 95); values.nudge(INTEGER, 1, true); assertEquals(100, values.value(INTEGER));
    }
    @Test void displayFormattingKeepsOriginalRoundingAndTrailingZeroRules() {
        assertEquals("0.85", ConfigValues.formatValue(REAL, 0.85));
        assertEquals("5", ConfigValues.formatValue(REAL, 5));
        assertEquals("2.5", ConfigValues.formatValue(REAL, 2.5));
        assertEquals("3", ConfigValues.formatValue(INTEGER, 2.5));
    }
}
