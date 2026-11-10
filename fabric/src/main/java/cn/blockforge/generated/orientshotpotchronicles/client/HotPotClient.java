// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import net.fabricmc.api.ClientModInitializer;

/** Fabric 仅保留客户端加载入口和开发探针的原生 tick。 */
public final class HotPotClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HotpotContentModule.HOST.client(Runnable::run);
        if (Boolean.getBoolean("hotpot.opt3c")) HotpotContentModule.development("Opt3cFabricProbe", "client", new Class<?>[0]);
        if (Boolean.getBoolean("hotpot.clientProbe")) {
            // 开发探针 tick 是加载器事件，正式包不携带实现。
            ClientTickEvents.END_CLIENT_TICK.register(client ->
                HotpotContentModule.development("ClientProbe", "tick", new Class<?>[0]));
        }
    }
}
