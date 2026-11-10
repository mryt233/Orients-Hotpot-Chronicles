// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import cn.blockforge.orient.framework.loader.ClientHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge 保留注册表解冻窗口及原生资源重载事件。 */
@Mod("orients_hotpot_chronicles")
public final class HotPotNeoForge {
    private static boolean contentRegistered;
    public HotPotNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgePlatform.install(modBus);
        // 第一次 RegisterEvent 在解冻之后，物品与方块构造需要这个原生窗口。
        modBus.addListener((RegisterEvent event) -> {
            if (contentRegistered) return;
            contentRegistered = true;
            HotpotContentModule.HOST.registerAll();
            HotpotContentModule.HOST.runtime(callback -> NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStartedEvent e) -> callback.run()));
            NeoForgePlatform.afterContentRegistration();
        });
        // 开发命令依赖原生事件参数，正式包不携带实现。
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> HotpotContentModule.development(
            "NeoForgeDevTests", "register", new Class<?>[]{RegisterCommandsEvent.class}, event));
        if (Boolean.getBoolean("hotpot.expreg")) HotpotContentModule.development("NeoForgeRegProbe", "register", new Class<?>[]{IEventBus.class}, modBus);
        if (Boolean.getBoolean("hotpot.opt3c")) HotpotContentModule.development("Opt3cNeoForgeProbe", "install", new Class<?>[]{IEventBus.class}, modBus);
        // 两端原生资源重载 API 不同，统一通知公共宿主。
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent event) ->
            event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Void>() {
                @Override protected Void prepare(net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) { return null; }
                @Override protected void apply(Void unused, net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) { HotpotContentModule.HOST.reload(); }
            }));
    }
    static void clientHooks(ClientHooks hooks) { HotpotContentModule.HOST.client(hooks); }
}
