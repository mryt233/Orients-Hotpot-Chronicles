// 作者：mryt233
package cn.blockforge.orient.framework.loader;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class HolderRefTest {
    @Test
    void missingDoesNotReadFallbackValue() {
        AtomicInteger reads = new AtomicInteger();
        HolderRef<String> ref = HolderRef.of("probe:missing", () -> false, () -> {
            reads.incrementAndGet();
            return "air";
        });
        assertEquals("probe:missing", ref.id());
        assertFalse(ref.isPresent());
        assertTrue(assertThrows(IllegalStateException.class, ref::get).getMessage().contains("probe:missing"));
        assertEquals(0, reads.get());
    }

    @Test
    void resolvesAfterRegistrationAndTracksChanges() {
        AtomicBoolean present = new AtomicBoolean();
        AtomicReference<String> value = new AtomicReference<>("air");
        HolderRef<String> ref = HolderRef.of("probe:late", present::get, value::get);
        assertFalse(ref.isPresent());
        value.set("block");
        present.set(true);
        assertTrue(ref.isPresent());
        assertEquals("block", ref.get());
        value.set("replacement");
        assertEquals("replacement", ref.get());
        present.set(false);
        assertThrows(IllegalStateException.class, ref::get);
    }

    @Test
    void presentNullReportsId() {
        HolderRef<Object> ref = HolderRef.of("probe:null", () -> true, () -> null);
        assertTrue(assertThrows(IllegalStateException.class, ref::get).getMessage().contains("probe:null"));
    }

    @Test
    void protocolAndRuntimeStayOnDeclaredVersions() {
        assertEquals(1, NetworkApi.PROTOCOL_VERSION);
        assertEquals("0.1.5", ApiVersion.RUNTIME);
    }
}
