// 作者：mryt233
package cn.blockforge.orient.framework.pure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationWindowTest {
    @Test
    void registryCloseReportsRegistryIdModuleAndPhase() {
        RegistrationWindow window = new RegistrationWindow();
        window.close("minecraft:item");
        String message = assertThrows(IllegalStateException.class,
            () -> window.claim("minecraft:item", "other:late", "probe", false)).getMessage();
        for (String field : new String[]{"registry=minecraft:item", "id=other:late", "owner=probe", "phase=closed"}) {
            assertTrue(message.contains(field), message);
        }
        window.claim("minecraft:block", "other:late", "probe", false);
    }

    @Test
    void fabricCloseIsGlobalAndIdempotent() {
        RegistrationWindow window = new RegistrationWindow();
        window.closeAll();
        window.closeAll();
        assertThrows(IllegalStateException.class, () -> window.claim("minecraft:sound_event", "p:x", "p", false));
        assertThrows(IllegalStateException.class, () -> window.claim("minecraft:recipe_type", "p:y", "p", false));
    }

    @Test
    void detectsDuplicateBeforeQueueFlushAndReportsBothModules() {
        RegistrationWindow window = new RegistrationWindow();
        window.claim("minecraft:item", "shared:x", "first", false);
        var error = assertThrows(IllegalStateException.class,
            () -> window.claim("minecraft:item", "shared:x", "second", false));
        assertTrue(error.getMessage().contains("owner=second"));
        assertTrue(error.getCause().getMessage().contains("first"));
        window.claim("minecraft:block", "shared:x", "second", false);
    }

    @Test
    void detectsExistingRegistryEntry() {
        RegistrationWindow window = new RegistrationWindow();
        assertThrows(IllegalStateException.class, () -> window.claim("item", "minecraft:stone", "probe", true));
    }

    @Test
    void moduleScopeRestoresOnExceptionAndNesting() {
        assertEquals("fallback", RegistrationGuard.owner("fallback"));
        RegistrationGuard.withOwner("outer", () -> {
            assertEquals("outer", RegistrationGuard.owner("fallback"));
            assertThrows(IllegalArgumentException.class, () -> RegistrationGuard.withOwner("inner", () -> {
                assertEquals("inner", RegistrationGuard.owner("fallback"));
                throw new IllegalArgumentException("probe");
            }));
            assertEquals("outer", RegistrationGuard.owner("fallback"));
        });
        assertEquals("fallback", RegistrationGuard.owner("fallback"));
    }

    @Test
    void failureKeepsOriginalCause() {
        RuntimeException cause = new IllegalArgumentException("probe");
        assertSame(cause, RegistrationGuard.failure("r", "i", "m", "register", cause).getCause());
    }
}
