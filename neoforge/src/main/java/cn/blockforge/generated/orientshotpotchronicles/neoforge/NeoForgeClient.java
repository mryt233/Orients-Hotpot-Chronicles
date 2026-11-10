// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/** 只保留客户端加载阶段、原生配置入口和开发 tick。 */
@Mod(value = HotPot.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
    public NeoForgeClient(IEventBus modBus, ModContainer container) {
        // 模组列表配置按钮是 NeoForge 专属扩展点。
        if (container != null) container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
            (mc, parent) -> new HotPotConfigScreen(parent));
        // 原生阶段保证公共客户端初始化在主线程执行。
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            HotPotNeoForge.clientHooks(Runnable::run);
            if (Boolean.getBoolean("hotpot.opt3c")) HotpotContentModule.development("Opt3cNeoForgeProbe", "client", new Class<?>[0]);
        }));
        if (Boolean.getBoolean("hotpot.clientProbe")) {
            // 开发探针 tick 是加载器事件，正式包不携带实现。
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) ->
                HotpotContentModule.development("ClientProbe", "tick", new Class<?>[0]));
        }
    }
}
