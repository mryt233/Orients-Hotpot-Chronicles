// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

/** Fabric 只负责加载阶段与原生资源重载事件。 */
public final class GeneratedMod implements ModInitializer {
    public static final String MOD_ID = HotPot.MOD_ID;
    public static final org.slf4j.Logger LOGGER = HotPot.LOGGER;
    public static ResourceLocation id(String path) { return HotPot.id(path); }
    public static void clientHooks(cn.blockforge.orient.framework.loader.ClientHooks hooks) { HotpotContentModule.HOST.client(hooks); }
    @Override
    public void onInitialize() {
        FabricPlatform.install();
        if (Boolean.getBoolean("hotpot.expreg")) HotpotContentModule.development("FabricRegProbe", "run", new Class<?>[0]);
        HotpotContentModule.HOST.registerAll();
        if (Boolean.getBoolean("hotpot.opt3c")) HotpotContentModule.development("Opt3cFabricProbe", "install", new Class<?>[0]);
        HotpotContentModule.HOST.runtime(callback -> ServerLifecycleEvents.SERVER_STARTED.register(server -> callback.run()));
        FabricPlatform.afterContentRegistration();
        // 资源重载监听器的原生类型属于 Fabric，公共生命周期只接收一次 reload 通知。
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return id("content_reload"); }
            @Override public void onResourceManagerReload(ResourceManager manager) { HotpotContentModule.HOST.reload(); }
        });
        // /hpt 注册入口使用 Fabric 命令事件，只在开发源码存在。
        HotpotContentModule.development("FabricDevTests", "register", new Class<?>[0]);
    }
}
