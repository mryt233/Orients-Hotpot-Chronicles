// 作者：mryt233
package cn.blockforge.orient.framework.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HooksTest {
    @Test
    void higherPriorityListenersComeFirst() {
        var hooks = new Hooks<String>();
        hooks.add(-10, "low");
        hooks.add(0, "normal");
        hooks.add(20, "high");

        assertEquals(List.of("high", "normal", "low"), hooks.all());
    }

    @Test
    void equalPriorityKeepsRegistrationOrder() {
        var hooks = new Hooks<String>();
        hooks.add("first");
        hooks.add("second");
        hooks.add("third");

        assertEquals(List.of("first", "second", "third"), hooks.all());
    }

    @Test
    void registrationUnregistersOnlyItsOwnEntryAndIsIdempotent() {
        var hooks = new Hooks<Object>();
        Object listener = new Object();
        var first = hooks.add(listener);
        var second = hooks.add(listener);

        first.unregister();
        first.unregister();
        assertEquals(1, hooks.size());
        assertSame(listener, hooks.all().getFirst());

        second.unregister();
        assertTrue(hooks.isEmpty());
        assertEquals(0, hooks.size());
    }

    @Test
    void removeUsesListenerIdentityAndRemovesItsRegistrations() {
        var hooks = new Hooks<String>();
        String listener = new String("same value");
        String equalButDifferent = new String("same value");
        hooks.add(listener);
        hooks.add(10, listener);
        hooks.add(equalButDifferent);

        assertTrue(hooks.remove(listener));
        assertEquals(1, hooks.size());
        assertSame(equalButDifferent, hooks.all().getFirst());
        assertFalse(hooks.remove(listener));
    }

    @Test
    void forEachReportsTheFailedListenerAndContinuesInPriorityOrder() {
        var hooks = new Hooks<Runnable>();
        var calls = new ArrayList<String>();
        var failedListeners = new ArrayList<Runnable>();
        var errors = new ArrayList<Throwable>();
        var problem = new IllegalStateException("broken hook");
        Runnable broken = () -> {
            calls.add("broken");
            throw problem;
        };
        hooks.add(-1, () -> calls.add("last"));
        hooks.add(10, () -> calls.add("first"));
        hooks.add(0, broken);

        assertDoesNotThrow(() -> hooks.forEach(Runnable::run, (listener, error) -> {
            failedListeners.add(listener);
            errors.add(error);
        }));

        assertEquals(List.of("first", "broken", "last"), calls);
        assertEquals(List.of(broken), failedListeners);
        assertEquals(List.of(problem), errors);
    }

    @Test
    void snapshotsCannotBeModified() {
        var hooks = new Hooks<String>();
        hooks.add("first");
        List<String> snapshot = hooks.all();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("extra"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.set(0, "changed"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertEquals(List.of("first"), hooks.all());
    }

    @Test
    void unchangedHooksReturnTheSameCachedSnapshot() {
        var hooks = new Hooks<Object>();
        hooks.add(new Object());
        List<Object> snapshot = hooks.all();

        assertSame(snapshot, hooks.all());
        assertFalse(hooks.remove(new Object()));
        assertSame(snapshot, hooks.all());
    }

    @Test
    void additionsAndRemovalsPublishNewSnapshotsWithoutChangingOldOnes() {
        var hooks = new Hooks<String>();
        var first = hooks.add("first");
        List<String> before = hooks.all();

        hooks.add(5, "second");
        List<String> afterAdd = hooks.all();
        first.unregister();
        List<String> afterRemove = hooks.all();

        assertNotSame(before, afterAdd);
        assertNotSame(afterAdd, afterRemove);
        assertEquals(List.of("first"), before);
        assertEquals(List.of("second", "first"), afterAdd);
        assertEquals(List.of("second"), afterRemove);
        assertSame(afterRemove, hooks.all());
    }
}
