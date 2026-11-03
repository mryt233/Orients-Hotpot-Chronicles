package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** EXP-REG 的 Fabric 侧：顺序注册、时机读取、重复注册错误。作者：mryt233 */
public final class FabricRegProbe {
    private FabricRegProbe() {
    }

    public static void run() {
        RegProbe.log("fabric phase=init");
        RegProbe.log("fabric read-before: item present=" + BuiltInRegistries.ITEM.containsKey(RegProbe.ITEM_ID)
            + ", block present=" + BuiltInRegistries.BLOCK.containsKey(RegProbe.BLOCK_ID));
        RegProbe.log("fabric supplier-before: early=" + RegProbe.describeItem(RegProbe.earlySupplier.get())
            + ", late=" + RegProbe.describeItem(RegProbe.lateSupplier.get()));

        RegProbe.block = Registry.register(BuiltInRegistries.BLOCK, RegProbe.BLOCK_ID, new RegProbe.ProbeBlock());
        RegProbe.log("fabric block registered, lookup=" + RegProbe.describeBlock());
        RegProbe.beType = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RegProbe.BE_ID,
            FabricBlockEntityTypeBuilder.create(RegProbe.ProbeBE::new, RegProbe.block).build());
        RegProbe.log("fabric be type registered=" + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(RegProbe.beType));
        RegProbe.item = Registry.register(BuiltInRegistries.ITEM, RegProbe.ITEM_ID, RegProbe.newProbeItem(RegProbe.block));
        RegProbe.log("fabric supplier-after: early=" + RegProbe.describeItem(RegProbe.earlySupplier.get())
            + ", late=" + RegProbe.describeItem(RegProbe.lateSupplier.get()));

        try {
            Registry.register(BuiltInRegistries.ITEM, RegProbe.ITEM_ID, new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
            RegProbe.log("fabric duplicate -> NO ERROR (unexpected)");
        } catch (Throwable t) {
            RegProbe.logError("fabric duplicate", t);
        }
        RegProbe.log("fabric phase=done");
    }

    public static void describe() {
        RegProbe.log("fabric tick: item=" + RegProbe.describeItem(RegProbe.item)
            + ", late=" + RegProbe.describeItem(RegProbe.lateSupplier.get()));
    }
}