package cn.blockforge.orient.framework.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块宿主：拓扑排序、缺依赖 / 环 / 重复 id 失败即停、跨宿主（跨模组）依赖。
 * 作者：mryt233（2026-11-01，前置检查 W6 / W8）
 */
class ModuleHostTest {
    private static ContentModule module(String id, String... dependsOn) {
        ModuleDeclaration declaration = new ModuleDeclaration(id, id, Set.of(dependsOn), List.of());
        return new ContentModule() {
            @Override
            public ModuleDeclaration declaration() {
                return declaration;
            }

            @Override
            public void register(RegistrationApi registration) {
            }
        };
    }

    @BeforeEach
    void reset() {
        ModuleHost.resetForTests();
    }

    @Test
    void dependenciesComeFirst() {
        ModuleHost host = new ModuleHost(List.of(module("c", "b"), module("a"), module("b", "a")));
        assertEquals(List.of("a", "b", "c"), host.order());
    }

    @Test
    void missingDependencyFails() {
        assertThrows(IllegalStateException.class, () -> new ModuleHost(List.of(module("a", "nope"))));
    }

    @Test
    void cycleFails() {
        assertThrows(IllegalStateException.class, () -> new ModuleHost(List.of(module("a", "b"), module("b", "a"))));
    }

    @Test
    void duplicateInOneHostFails() {
        assertThrows(IllegalStateException.class, () -> new ModuleHost(List.of(module("a"), module("a"))));
    }

    @Test
    void dependsOnModuleOfEarlierHost() {
        new ModuleHost(List.of(module("hotpot")));
        ModuleHost noodles = new ModuleHost(List.of(module("noodles", "hotpot")));
        assertEquals(List.of("noodles"), noodles.order());
        assertTrue(ModuleHost.isDeclared("hotpot"));
        assertTrue(ModuleHost.isDeclared("noodles"));
        assertFalse(ModuleHost.isDeclared("snacks"));
    }

    @Test
    void dependsOnLaterHostFails() {
        // 面条先建宿主、火锅后建：说明加载器顺序没配好，必须报错
        assertThrows(IllegalStateException.class, () -> new ModuleHost(List.of(module("noodles", "hotpot"))));
    }

    @Test
    void duplicateAcrossHostsFails() {
        new ModuleHost(List.of(module("hotpot")));
        assertThrows(IllegalStateException.class, () -> new ModuleHost(List.of(module("hotpot"))));
    }

    @Test
    void failedHostDoesNotClaimIds() {
        assertThrows(IllegalStateException.class, () -> new ModuleHost(List.of(module("a", "missing"))));
        new ModuleHost(List.of(module("a")));   // 上一次失败不能把 a 占住
        assertTrue(ModuleHost.isDeclared("a"));
    }
}
