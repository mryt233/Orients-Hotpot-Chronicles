// 作者：mryt233
package cn.blockforge.orient.framework.loader;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationApiCompatibilityTest {
    @Test
    void originalPlatformImplementationsKeepDefaultMethods() {
        RegistrationApi original = new RegistrationApi() {
            @Override public ItemRegistrar items() { return null; }
            @Override public BlockRegistrar blocks() { return null; }
            @Override public BlockEntityRegistrar blockEntities() { return null; }
            @Override public MenuRegistrar menus() { return null; }
            @Override public ComponentRegistrar components() { return null; }
            @Override public CreativeTabRegistrar creativeTabs() { return null; }
            @Override public EntityRegistrar entityTypes() { return null; }
        };
        for (Runnable call : List.<Runnable>of(original::recipeSerializers, original::recipeTypes,
                original::criteria, original::sounds, original::particles)) {
            assertTrue(assertThrows(UnsupportedOperationException.class, call::run).getMessage().contains("0.1.5"));
        }
    }

    @Test
    void originalRuntimeAndClientHooksStayFunctionalInterfaces() {
        AtomicInteger calls = new AtomicInteger();
        RuntimeHooks runtime = Runnable::run;
        ClientHooks client = Runnable::run;
        runtime.onServerStarted(calls::incrementAndGet);
        client.onClientSetup(calls::incrementAndGet);
        assertEquals(2, calls.get());
    }
}
