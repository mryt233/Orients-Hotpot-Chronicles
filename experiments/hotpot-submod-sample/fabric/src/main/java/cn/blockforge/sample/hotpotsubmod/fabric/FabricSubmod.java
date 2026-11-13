// 作者：mryt233
package cn.blockforge.sample.hotpotsubmod.fabric;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.sample.hotpotsubmod.HotpotSubmod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fabric 入口：注册方块/物品并接入火锅开放登记。 */
public final class FabricSubmod implements ModInitializer {
    @Override
    public void onInitialize() {
        HotpotSubmod.registerGameObjects(
            (id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block),
            (id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        HotpotSubmod.registerHotpotEntries();
        ItemGroupEvents.modifyEntriesEvent(HotPotContent.ITEM_GROUP_KEY).register(entries -> {
            entries.accept(HotpotSubmod.coasterItem());
            entries.accept(HotpotSubmod.ginsengBowlItem());
        });
    }
}
