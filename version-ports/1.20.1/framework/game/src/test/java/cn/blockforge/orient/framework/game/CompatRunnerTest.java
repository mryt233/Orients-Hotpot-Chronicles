// 作者：mryt233
package cn.blockforge.orient.framework.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompatRunnerTest {
    @Test
    void absentModsAreSkippedWithoutRegistrationOrWarning() {
        var calls = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        var absent = module("absent", List.of("missing"), () -> calls.add("registered"));

        int loaded = CompatRunner.run(List.of(absent), id -> false, (name, error) -> warnings.add(name));

        assertEquals(0, loaded);
        assertTrue(calls.isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void presentModulesExecuteOnceAndOnlySuccessfulModulesAreCounted() {
        var calls = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        var first = module("first", List.of("alias", "first"), () -> calls.add("first"));
        var absent = module("absent", List.of("missing"), () -> calls.add("absent"));
        var last = module("last", List.of("last"), () -> calls.add("last"));
        Set<String> installed = Set.of("alias", "first", "last");

        int loaded = CompatRunner.run(List.of(first, absent, last), installed::contains,
            (name, error) -> warnings.add(name));

        assertEquals(2, loaded);
        assertEquals(List.of("first", "last"), calls);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void laterModAliasCanEnableAModule() {
        var calls = new ArrayList<String>();
        var candidate = module("compat", List.of("not_installed", "installed"), () -> calls.add("registered"));

        int loaded = CompatRunner.run(List.of(candidate), "installed"::equals,
            (name, error) -> fail("Unexpected compatibility warning: " + name, error));

        assertEquals(1, loaded);
        assertEquals(List.of("registered"), calls);
    }

    @Test
    void failedModuleWarnsAndDoesNotPreventTheNextModuleFromLoading() {
        var calls = new ArrayList<String>();
        var warningNames = new ArrayList<String>();
        var errors = new ArrayList<Throwable>();
        var problem = new IllegalStateException("broken compatibility module");
        var first = module("first", List.of("first"), () -> calls.add("first"));
        var broken = module("broken module", List.of("missing", "broken_mod"), () -> {
            calls.add("broken");
            throw problem;
        });
        var last = module("last", List.of("last"), () -> calls.add("last"));
        Set<String> installed = Set.of("first", "broken_mod", "last");

        int loaded = assertDoesNotThrow(() -> CompatRunner.run(List.of(first, broken, last), installed::contains,
            (name, error) -> {
                warningNames.add(name);
                errors.add(error);
            }));

        assertEquals(2, loaded);
        assertEquals(List.of("first", "broken", "last"), calls);
        assertEquals(1, warningNames.size());
        assertTrue(warningNames.get(0).contains("broken module"));
        assertTrue(warningNames.get(0).contains("broken_mod"));
        assertEquals(List.of(problem), errors);
    }

    private static CompatModule module(String name, List<String> ids, Runnable registration) {
        return new CompatModule() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public List<String> modIds() {
                return ids;
            }

            @Override
            public void register() {
                registration.run();
            }
        };
    }
}
