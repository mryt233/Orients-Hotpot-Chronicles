package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** 两加载器共用提示内容，顺序与原 Fabric 客户端一致。作者：mryt233 */
public final class ClientTooltips {
    private ClientTooltips() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        SharedClientSetup.appendPotTooltip(stack, lines);
        Integer dipped = stack.get(HotPotContent.DIPPED);
        if (dipped != null && dipped != 0) {
            lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.dipped"),
                DippingSauceItem.names(dipped)).withStyle(ChatFormatting.GOLD));
            Seasoning.appendEffects(dipped, lines);
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (HotPot.MOD_ID.equals(id.getNamespace())) {
            String key = "tooltip." + HotPot.MOD_ID + ".flavor." + id.getPath();
            if (net.minecraft.locale.Language.getInstance().has(key)) {
                lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
        SharedClientSetup.appendPlateTooltip(stack, lines);
    }
}
