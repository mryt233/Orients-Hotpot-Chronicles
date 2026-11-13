// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.forge;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** 只保留客户端加载阶段、原生配置入口和开发 tick。 */
public final class ForgeClient {
    public ForgeClient(IEventBus modBus) {
        // 模组列表配置按钮是 Forge 专属扩展点。
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new HotPotConfigScreen(parent)));
        // 原生阶段保证公共客户端初始化在主线程执行。
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> HotPotForge.clientHooks(Runnable::run)));
        if (!FMLEnvironment.production && Boolean.getBoolean("hotpot.clientProbe")) {
            // 开发探针 tick 是加载器事件，正式包不携带实现。
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
                if (event.phase == TickEvent.Phase.END) {
                    HotpotContentModule.development("ClientProbe", "tick", new Class<?>[0]);
                }
            });
        }
    }
}
