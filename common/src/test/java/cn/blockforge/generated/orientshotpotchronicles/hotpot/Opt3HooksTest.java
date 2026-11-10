// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 3B：子模组钩子优先级、异常隔离、登记表冻结。作者：mryt233
 *
 * <p>只测不依赖 Minecraft 运行时的部分：{@link FrozenRegistry} 的登记 / 冻结语义，
 * 以及框架 {@code Hooks} 的优先级 / 注销 / 异常隔离（这部分火锅侧只是转手，框架侧已有测试，
 * 这里再按火锅的用法核一遍，防止哪天转手时把优先级丢了）。</p>
 */
class Opt3HooksTest {

    // ------------------------------------------------------------ 登记表冻结

    @Test
    void registryAcceptsWritesBeforeFreeze() {
        FrozenRegistry<String, String> registry = new FrozenRegistry<>("测试表", "启动期登记");
        registry.put("a", "1");
        registry.put("b", "2");

        assertEquals("1", registry.get("a"));
        assertEquals("2", registry.get("b"));
        assertTrue(registry.containsKey("b"));
        assertTrue(registry.containsValue("2"));
        assertEquals(2, registry.size());
        assertFalse(registry.isFrozen());
    }

    @Test
    void registryRejectsWritesAfterFreezeWithExplainedMessage() {
        FrozenRegistry<String, String> registry = new FrozenRegistry<>("测试表", "要在启动期登记");
        registry.put("a", "1");
        registry.freeze();

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> registry.put("b", "2"));
        // 报错要说清楚是哪张表、怎么办
        assertTrue(error.getMessage().contains("测试表"), error.getMessage());
        assertTrue(error.getMessage().contains("要在启动期登记"), error.getMessage());
        // 冻结后表里还是那一项，没被悄悄改掉
        assertEquals(1, registry.size());
        assertFalse(registry.containsKey("b"));
    }

    @Test
    void registryFreezeIsIdempotent() {
        FrozenRegistry<String, String> registry = new FrozenRegistry<>("测试表", "启动期登记");
        registry.put("a", "1");
        registry.freeze();
        assertDoesNotThrow(registry::freeze);
        assertTrue(registry.isFrozen());
        assertEquals(1, registry.size());
    }

    @Test
    void frozenViewIsUnmodifiableAndKeepsLookupsWorking() {
        FrozenRegistry<String, String> registry = new FrozenRegistry<>("测试表", "启动期登记");
        registry.put("a", "1");
        registry.freeze();

        assertThrows(UnsupportedOperationException.class, () -> registry.view().put("b", "2"));
        assertEquals("1", registry.get("a"));
        assertEquals(1, registry.view().size());
    }

    @Test
    void lookupsWorkBothBeforeAndAfterFreeze() {
        FrozenRegistry<String, String> registry = new FrozenRegistry<>("测试表", "启动期登记");
        registry.put("a", "1");

        assertEquals("1", registry.get("a"));
        registry.freeze();
        assertEquals("1", registry.get("a"));
        assertNull(registry.get("missing"));
    }

    // ------------------------------------------------------------ 火锅登记表本体

    @Test
    void cookingTablesExposeFreezeState() {
        // 火锅的三张表都能被冻（真实的冻结时机在服务端启动时，这里只测接口）
        String summary = HotPotCooking.registrySummary();
        assertTrue(summary.contains("pot_results="), summary);
        assertTrue(summary.contains("overcook="), summary);
        assertTrue(summary.contains("cook_ticks="), summary);
    }

    // 注：SoupBase / HotPotFuel 的冻结状态不能在纯单元测试里读——它们的静态初始化要 Minecraft 的
    // Bootstrap（SoundEvents / Items），单元测试环境没有。改到起服后的 /hpt 实测核对（见报告）。

    // ------------------------------------------------------------ 钩子优先级 / 异常隔离

    @Test
    void frameworkHooksOrderByPriorityThenRegistration() {
        cn.blockforge.orient.framework.game.Hooks<String> hooks =
            new cn.blockforge.orient.framework.game.Hooks<>();
        hooks.add(0, "default");
        hooks.add(10, "high");
        hooks.add(-5, "low");
        hooks.add(0, "second-default");

        assertEquals(List.of("high", "default", "second-default", "low"), hooks.all());
    }

    @Test
    void frameworkHooksIsolateOneBrokenListener() {
        cn.blockforge.orient.framework.game.Hooks<Runnable> hooks =
            new cn.blockforge.orient.framework.game.Hooks<>();
        StringBuilder seen = new StringBuilder();
        hooks.add(5, () -> seen.append("first;"));
        hooks.add(1, () -> {
            throw new IllegalStateException("boom");
        });
        hooks.add(-1, () -> seen.append("last;"));

        assertDoesNotThrow(() -> hooks.forEach(Runnable::run, (listener, error) -> seen.append("isolated;")));
        assertTrue(seen.toString().contains("first;"), seen.toString());
        assertTrue(seen.toString().contains("isolated;"), seen.toString());
        assertTrue(seen.toString().contains("last;"), seen.toString());
        assertFalse(seen.toString().contains("boom"), seen.toString());
    }

    @Test
    void frameworkHooksRegistrationCanBeCancelled() {
        cn.blockforge.orient.framework.game.Hooks<String> hooks =
            new cn.blockforge.orient.framework.game.Hooks<>();
        var registration = hooks.add(3, "x");
        assertEquals(1, hooks.size());
        registration.unregister();
        assertEquals(0, hooks.size());
    }
}
