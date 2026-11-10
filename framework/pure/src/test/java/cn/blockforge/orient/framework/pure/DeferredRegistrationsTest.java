// 作者：mryt233
package cn.blockforge.orient.framework.pure;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeferredRegistrationsTest {
    @Test
    void handlesRegistrationsBeforeAndAfterLoaderEvent() {
        DeferredRegistrations<String> entries = new DeferredRegistrations<>();
        List<String> calls = new ArrayList<>();
        entries.add(event -> calls.add("first:" + event));
        assertTrue(calls.isEmpty());
        entries.bind("setup");
        entries.add(event -> calls.add("second:" + event));
        assertEquals(List.of("first:setup", "second:setup"), calls);
        entries.bind("reload");
        assertEquals(List.of("first:setup", "second:setup", "first:reload", "second:reload"), calls);
    }

    @Test
    void initializationFailuresAreNotSwallowed() {
        DeferredRegistrations<String> entries = new DeferredRegistrations<>();
        entries.add(event -> { throw new IllegalStateException("probe"); });
        assertThrows(IllegalStateException.class, () -> entries.bind("setup"));
    }

    @Test
    void rejectsNulls() {
        DeferredRegistrations<String> entries = new DeferredRegistrations<>();
        assertThrows(NullPointerException.class, () -> entries.add(null));
        assertThrows(NullPointerException.class, () -> entries.bind(null));
    }
}
