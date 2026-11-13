// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import cn.blockforge.generated.orientshotpotchronicles.client.DynamicItemModel;
import cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen;
import cn.blockforge.generated.orientshotpotchronicles.client.SoupItemProperty;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** 只保留客户端加载阶段、26.2 物品模型类型登记、原生配置入口和开发 tick。 */
@Mod(value = HotPot.MOD_ID, dist = Dist.CLIENT)
public class NeoForgeClient {
    public NeoForgeClient(IEventBus modBus, ModContainer container) {
        // 模组列表配置按钮是 NeoForge 专属扩展点。
        if (container != null) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new HotPotConfigScreen(parent));
        }
        DynamicItemModel.install();
        modBus.addListener((RegisterItemModelsEvent event) ->
            event.register(HotPot.id("dynamic"), DynamicItemModel.Unbaked.CODEC));
        modBus.addListener((RegisterRangeSelectItemModelPropertyEvent event) ->
            event.register(HotPot.id("soup"), SoupItemProperty.CODEC));
        // 原生阶段保证公共客户端初始化在主线程执行。
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
            HotPotNeoForge.clientHooks(Runnable::run)));
        if (!FMLEnvironment.isProduction() && Boolean.getBoolean("hotpot.clientProbe")) {
            // 开发探针 tick 是加载器事件，正式包不携带实现。
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                HotpotContentModule.development("ClientProbe", "tick", new Class<?>[0]));
        }
    }
}
