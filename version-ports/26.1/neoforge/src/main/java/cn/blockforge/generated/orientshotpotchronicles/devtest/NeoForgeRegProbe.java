// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** EXP-REG 的 NeoForge 侧：构造期注册、按注册表事件、错窗口注册、重复 id。作者：mryt233 */
public final class NeoForgeRegProbe {
    private static boolean done;

    private NeoForgeRegProbe() {
    }

    /** mryt233 改（2026-11-10）：入口只反射调用这一处，生产代码不直接引用探针类型。 */
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        constructorProbe();
        bus.addListener(NeoForgeRegProbe::onRegisterEvent);
    }

    /** 构造期（注册表冻结窗口之前）尝试注册：预期抛异常。 */
    public static void constructorProbe() {
        RegProbe.log("neoforge phase=constructor");
        try {
            Registry.register(BuiltInRegistries.ITEM, RegProbe.ITEM_ID,
                new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
            RegProbe.log("neoforge constructor-register -> NO ERROR (unexpected)");
        } catch (Throwable t) {
            RegProbe.logError("neoforge constructor-register", t);
        }
        RegProbe.log("neoforge constructor-read: item present=" + BuiltInRegistries.ITEM.containsKey(RegProbe.ITEM_ID));
    }

    public static void onRegisterEvent(RegisterEvent event) {
        var key = event.getRegistryKey().identifier();
        RegProbe.log("neoforge event=" + key);
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            RegProbe.block = Registry.register(BuiltInRegistries.BLOCK, RegProbe.BLOCK_ID, new RegProbe.ProbeBlock());
            RegProbe.log("neoforge block registered in BLOCK window; lookup=" + RegProbe.describeBlock());
            // 错窗口：在 BLOCK 事件里注册另一个物品 id（ITEM 事件还没到）
            try {
                Registry.register(BuiltInRegistries.ITEM, RegProbe.ITEM_W_ID,
                    new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
                RegProbe.log("neoforge wrong-window-item -> NO ERROR");
            } catch (Throwable t) {
                RegProbe.logError("neoforge wrong-window-item", t);
            }
        }
        if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            RegProbe.beType = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RegProbe.BE_ID,
                new net.minecraft.world.level.block.entity.BlockEntityType<>(RegProbe.ProbeBE::new, RegProbe.block));
            RegProbe.log("neoforge be registered in BLOCK_ENTITY_TYPE window");
        }
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            RegProbe.log("neoforge supplier-before-item: early=" + RegProbe.describeItem(RegProbe.earlySupplier.get())
                + ", late=" + RegProbe.describeItem(RegProbe.lateSupplier.get())
                + ", block=" + RegProbe.describeBlock());
            if (RegProbe.item == null) {
                RegProbe.item = Registry.register(BuiltInRegistries.ITEM, RegProbe.ITEM_ID,
                    RegProbe.newProbeItem(RegProbe.block));
            }
            RegProbe.log("neoforge item registered; early=" + RegProbe.describeItem(RegProbe.earlySupplier.get())
                + ", late=" + RegProbe.describeItem(RegProbe.lateSupplier.get()));
            if (Boolean.getBoolean("hotpot.expreg.duplicate")) {
                try {
                    Registry.register(BuiltInRegistries.ITEM, RegProbe.ITEM_ID,
                        new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
                    RegProbe.log("neoforge duplicate -> NO ERROR (unexpected)");
                } catch (Throwable t) {
                    RegProbe.logError("neoforge duplicate", t);
                }
            } else {
                RegProbe.log("neoforge duplicate test skipped (hotpot.expreg.duplicate=false)");
            }
            RegProbe.log("neoforge phase=item-done");
        }
        if (!done) {
            done = true;
        }
    }
}