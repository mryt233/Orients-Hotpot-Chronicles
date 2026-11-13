// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.forge;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.loader.ClientHooks;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 保留注册表解冻窗口及原生资源重载事件。 */
@Mod(HotPot.MOD_ID)
public final class HotPotForge {
    private static boolean contentRegistered;

    public HotPotForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 第一次 RegisterEvent 在解冻之后，物品与方块构造需要这个原生窗口；
        // Forge 1.20.1 的模组构造并发执行，延迟到此处再校验/回退安装前置，避免与 orient_framework 构造竞态。
        modBus.addListener(EventPriority.HIGHEST, (RegisterEvent event) -> {
            if (contentRegistered) {
                return;
            }
            contentRegistered = true;
            ForgePlatform.install(modBus);
            HotpotContentModule.HOST.registerAll();
            HotpotContentModule.HOST.runtime(callback ->
                MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> callback.run()));
            ForgePlatform.afterContentRegistration();
        });
        if (FMLEnvironment.dist.isClient()) {
            new ForgeClient(modBus);
        }
        // 开发命令依赖原生事件参数，正式包不携带实现。
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> HotpotContentModule.development(
            "ForgeDevTests", "register", new Class<?>[]{RegisterCommandsEvent.class}, event));
        if (Boolean.getBoolean("hotpot.expreg")) {
            HotpotContentModule.development("ForgeRegProbe", "register", new Class<?>[]{IEventBus.class}, modBus);
        }
        // 两端原生资源重载 API 不同，统一通知公共宿主。
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
            event.addListener(new SimplePreparableReloadListener<Void>() {
                @Override
                protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
                    return null;
                }

                @Override
                protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
                    HotpotContentModule.HOST.reload();
                }
            }));
    }

    static void clientHooks(ClientHooks hooks) {
        HotpotContentModule.HOST.client(hooks);
    }
}
