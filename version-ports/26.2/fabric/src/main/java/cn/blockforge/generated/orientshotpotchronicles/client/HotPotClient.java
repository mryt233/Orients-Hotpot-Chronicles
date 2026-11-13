// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;

/** Fabric 仅保留客户端加载入口、26.2 物品模型类型登记和开发探针的原生 tick。 */
public class HotPotClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DynamicItemModel.install();
        ItemModels.ID_MAPPER.put(HotPot.id("dynamic"), DynamicItemModel.Unbaked.CODEC);
        RangeSelectItemModelProperties.ID_MAPPER.put(HotPot.id("soup"), SoupItemProperty.CODEC);
        HotpotContentModule.HOST.client(Runnable::run);
        if (Boolean.getBoolean("hotpot.clientProbe")) {
            // 开发探针 tick 是加载器事件，正式包不携带实现。
            ClientTickEvents.END_CLIENT_TICK.register(client ->
                HotpotContentModule.development("ClientProbe", "tick", new Class<?>[0]));
        }
    }
}
