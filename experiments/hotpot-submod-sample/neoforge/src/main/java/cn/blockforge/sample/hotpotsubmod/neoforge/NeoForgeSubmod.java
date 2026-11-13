// 作者：mryt233
package cn.blockforge.sample.hotpotsubmod.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.sample.hotpotsubmod.HotpotSubmod;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge 入口：按注册事件分域注册方块/物品，并在 FMLCommonSetup 登记汤底与蘸料。 */
@Mod(HotpotSubmod.MOD_ID)
public final class NeoForgeSubmod {
    private boolean initializedObjects;

    public NeoForgeSubmod(IEventBus modBus) {
        modBus.addListener((RegisterEvent event) -> {
            if (!initializedObjects) {
                initializedObjects = true;
                HotpotSubmod.prepareGameObjects();
            }
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                event.register(Registries.BLOCK, helper ->
                    helper.register(HotpotSubmod.COASTER_ID, HotpotSubmod.coasterBlock()));
            } else if (event.getRegistryKey().equals(Registries.ITEM)) {
                event.register(Registries.ITEM, helper -> {
                    helper.register(HotpotSubmod.COASTER_ID, HotpotSubmod.coasterItem());
                    helper.register(HotpotSubmod.GINSENG_BOWL_ID, HotpotSubmod.ginsengBowlItem());
                });
            }
        });
        modBus.addListener((FMLCommonSetupEvent event) ->
            event.enqueueWork(HotpotSubmod::registerHotpotEntries));
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(HotPotContent.ITEM_GROUP_KEY)) {
                event.accept(HotpotSubmod.coasterItem());
                event.accept(HotpotSubmod.ginsengBowlItem());
            }
        });
    }
}
