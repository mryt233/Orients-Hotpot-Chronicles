// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class Opt3cFabricProbe {
    public static void install() {
        Opt3cProbe.install(callback -> ServerLifecycleEvents.SERVER_STARTED.register(server -> callback.run()));
    }
    public static void client() {
        Opt3cProbe.clientModules(Runnable::run);
    }
    private Opt3cFabricProbe() { }
}
