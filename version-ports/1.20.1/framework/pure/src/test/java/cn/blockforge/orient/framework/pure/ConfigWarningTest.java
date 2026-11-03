// 作者：mryt233
package cn.blockforge.orient.framework.pure;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigWarningTest {
    @Test
    void textFormatsArgumentsInTheirOriginalOrder() {
        var warning = ConfigWarning.of("out_of_range", "%s must be between %s and %s", "nested.count", 0, 10);

        assertEquals("nested.count must be between 0 and 10", warning.text());
        assertEquals(List.of("nested.count", "0", "10"), warning.args());
    }

    @Test
    void warningKeyHasTheFrameworkPrefix() {
        var warning = ConfigWarning.of("not_number", "%s must be a number", "limit");

        assertEquals("orient_framework.config.not_number", warning.key());
    }

    @Test
    void argumentListIsAnImmutableSnapshot() {
        var input = new ArrayList<>(List.of("limit"));
        var warning = new ConfigWarning("orient_framework.config.not_number", "%s must be a number", input);
        input.set(0, "changed");

        assertEquals("limit must be a number", warning.text());
        assertThrows(UnsupportedOperationException.class, () -> warning.args().add("extra"));
    }
}
