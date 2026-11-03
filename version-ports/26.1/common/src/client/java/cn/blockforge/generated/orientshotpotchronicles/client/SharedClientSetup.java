package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;

/** 两个平台共用的物品绘制/提示，不包含加载器 API。作者：mryt233 */
public final class SharedClientSetup {
    private SharedClientSetup() {}

    public static void registerItems() {
        SauceRender.register();
        HeldFoodItemRenderer.register();
        // mryt233 改：汤底谓词由平台注册 SoupItemProperty，物品定义引用。
        // 鸳鸯 / 田字锅的物品：锅身方块模型 + 按物品里存的每边汤底画汤面（任意组合都对，不用为每种组合做模型；新增）
        for (cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial material
            : cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial.values()) {
            if (!material.layout().multiSoup()) {
                continue;
            }
            BlockItem item = HotPotContent.itemFor(material);
            ClientItemRenderers.register(item,
                (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                    RenderItems.INSTANCE
                        .renderSingleBlock(item.getBlock().defaultBlockState(), matrices, vertexConsumers, light, overlay);
                    CustomData data = stack.get(HotPotContent.POT_CONTENTS);
                    if (data != null) {
                        HotPotBlockEntityRenderer.renderItemSoups(material, data.copyTag(), matrices, vertexConsumers, light);
                    }
                });
        }
        // 鼠标中键选盘子：复制准星对着的那一个（连菜一起），由客户端告诉方块是谁在选
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.pickPlayer =
            () -> net.minecraft.client.Minecraft.getInstance().player;
        // mryt233 改：26.1斜腿已在方块模型中，物品沿用原版模型绘制。
    }

    /** 盘子：装了几样 / 能装几样，下面列出每一样；筷子：夹着什么。 */
    public static void appendPlateTooltip(ItemStack stack, java.util.List<Component> lines) {
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            ItemStack held = cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks.held(stack);
            if (!held.isEmpty()) {
                lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.chopsticks.holding"), held.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        // 盘子和装菜的碗（r99）；汤碗、橙子饮料这种整件的不列菜
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock plate =
            cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem.placedFor(stack);
        if (plate == null || cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem.keepsSelf(stack)
            || (stack.is(net.minecraft.world.item.Items.BOWL) && !stack.has(net.minecraft.core.component.DataComponents.CONTAINER))) {
            return;
        }
        ItemContainerContents contents = stack.getOrDefault(
            net.minecraft.core.component.DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        java.util.List<ItemStack> foods = contents.nonEmptyItemCopyStream().toList();
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.plate.count"), foods.size(), plate.size().capacity())
            .withStyle(ChatFormatting.GRAY));
        // 像原版收纳袋：同样的菜合成一行，后面写数量（r91）
        java.util.List<ItemStack> kinds = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        for (ItemStack food : foods) {
            int at = -1;
            for (int k = 0; k < kinds.size(); k++) {
                if (ItemStack.isSameItemSameComponents(kinds.get(k), food)) {
                    at = k;
                    break;
                }
            }
            if (at < 0) {
                kinds.add(food);
                counts.add(1);
            } else {
                counts.set(at, counts.get(at) + 1);
            }
        }
        for (int k = 0; k < kinds.size(); k++) {
            Component line = Component.literal(" ").append(kinds.get(k).getHoverName());
            if (counts.get(k) > 1) {
                line = line.copy().append(Component.literal(" ×" + counts.get(k)));
            }
            lines.add(line.copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /** 装着东西的锅物品：提示里写汤底和食材份数。 */
    public static void appendPotTooltip(ItemStack stack, java.util.List<Component> lines) {
        CustomData data = stack.get(HotPotContent.POT_CONTENTS);
        if (data == null) {
            return;
        }
        CompoundTag nbt = data.copyTag();
        // 鸳鸯 / 田字锅：每边的汤底用「/」列出来（同锅的状态栏，调整）
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout layout =
            stack.getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock pot
                ? pot.material().layout() : cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout.NINE_GRID;
        String[] keys = {"Base", "BaseB", "BaseC", "BaseD"};
        Component soupName = HotPotBlockEntity.soupText(layout, side -> nbt.getIntOr(keys[side], -1));
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.pot.soup"), soupName)
            .withStyle(ChatFormatting.GRAY));
        int food = nbt.getListOrEmpty("Items").size();
        if (food > 0) {
            int cooked = 0;
            for (byte b : nbt.getByteArray("Cooked").orElseGet(() -> new byte[0])) {
                cooked += b != 0 ? 1 : 0;
            }
            lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.pot.food"), food, cooked)
                .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.pot.hint"))
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
