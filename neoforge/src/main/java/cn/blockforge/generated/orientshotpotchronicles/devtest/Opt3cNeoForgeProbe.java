// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class Opt3cNeoForgeProbe {
    private static boolean registered;
    public static void install(IEventBus bus) {
        bus.addListener((RegisterEvent event) -> {
            if (!registered) {
                registered = true;
                Opt3cProbe.install(callback -> NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> callback.run()));
            }
        });
    }
    public static void client() {
        Opt3cProbe.clientModules(Runnable::run);
    }
    private Opt3cNeoForgeProbe() { }
}
