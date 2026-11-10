package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

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
        // 装着汤的锅物品（掉在地上、物品栏里）显示对应汤面：谓词 = (汤底编号 + 1) / 10，没汤为 0（r94）
        for (Item pot : new Item[]{
            HotPotContent.IRON_HOT_POT_ITEM, HotPotContent.COPPER_HOT_POT_ITEM}) {
            ItemProperties.register(pot, HotPot.id("soup"),
                (stack, world, entity, seed) -> {
                    CustomData data = stack.get(HotPotContent.POT_CONTENTS);
                    if (data == null) {
                        return 0F;
                    }
                    CompoundTag nbt = data.copyTag();
                    SoupBase soup = nbt.contains("Base") ? SoupBase.byId(nbt.getInt("Base")) : null;
                    return soup == null ? 0F : (soup.ordinal() + 1) / 10F;
                });
        }
        // 鸳鸯 / 田字锅的物品：锅身方块模型 + 按物品里存的每边汤底画汤面（任意组合都对，不用为每种组合做模型；新增）
        for (HotPotMaterial material
            : HotPotMaterial.values()) {
            if (!material.layout().multiSoup()) {
                continue;
            }
            BlockItem item = HotPotContent.itemFor(material);
            ClientItemRenderers.register(item,
                (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                    Minecraft.getInstance().getBlockRenderer()
                        .renderSingleBlock(item.getBlock().defaultBlockState(), matrices, vertexConsumers, light, overlay);
                    CustomData data = stack.get(HotPotContent.POT_CONTENTS);
                    if (data != null) {
                        HotPotBlockEntityRenderer.renderItemSoups(material, data.copyTag(), matrices, vertexConsumers, light);
                    }
                });
        }
        // 鼠标中键选盘子：复制准星对着的那一个（连菜一起），由客户端告诉方块是谁在选
        PlateHolderBlock.pickPlayer =
            () -> Minecraft.getInstance().player;
        // 条凳的物品图标：凳面模型 + 斜腿（斜腿不在方块模型里，物品模型用 builtin/entity 交给这里画）
        for (BlockItem item : HotPotContent.TALL_BENCH_ITEMS) {
            ClientItemRenderers.register(item,
                (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                    BlockState state = item.getBlock().defaultBlockState();
                    Minecraft.getInstance().getBlockRenderer()
                        .renderSingleBlock(state, matrices, vertexConsumers, light, overlay);
                    BenchLegsRenderer.renderLegs(state, matrices, vertexConsumers, light);
                });
        }
    }

    /** 盘子：装了几样 / 能装几样，下面列出每一样；筷子：夹着什么。 */
    public static void appendPlateTooltip(ItemStack stack, List<Component> lines) {
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            ItemStack held = Chopsticks.held(stack);
            if (!held.isEmpty()) {
                lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.chopsticks.holding"), held.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
                // mryt233 改（2026-11-04）：夹着的菜蘸过就接着列蘸料和效果，不用放下筷子才看得到。
                ClientTooltips.appendDipped(held, lines);
                // mryt233 改（2026-11-05，r136a）：夹着的菜是在哪种汤底里煮出来的也列出来
                ClientTooltips.appendCookedSoup(held, lines);
            }
            return;
        }
        // 盘子和装菜的碗（r99）；汤碗、橙子饮料这种整件的不列菜
        PlateBlock plate =
            PlateItem.placedFor(stack);
        if (plate == null || PlateItem.keepsSelf(stack)
            || (stack.is(Items.BOWL) && !stack.has(DataComponents.CONTAINER))) {
            return;
        }
        ItemContainerContents contents = stack.getOrDefault(
            DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> foods = contents.nonEmptyStream().toList();
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.plate.count"), foods.size(), plate.size().capacity())
            .withStyle(ChatFormatting.GRAY));
        // 像原版收纳袋：同样的菜合成一行，后面写数量（r91）
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
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
            // mryt233 改（2026-11-04）：这样菜蘸过的话，在它这一行下面接着列蘸料和效果。
            // 合并计数不受影响：不同蘸料的同种菜组件不同，本来就不会并成一行。
            ClientTooltips.appendDipped(kinds.get(k), lines);
            // mryt233 改（2026-11-05，r136a）：再补一行「汤底：红汤锅底 · 特辣」这种说明
            ClientTooltips.appendCookedSoup(kinds.get(k), lines);
        }
    }

    /** 装着东西的锅物品：提示里写汤底和食材份数。 */
    public static void appendPotTooltip(ItemStack stack, List<Component> lines) {
        CustomData data = stack.get(HotPotContent.POT_CONTENTS);
        if (data == null) {
            return;
        }
        CompoundTag nbt = data.copyTag();
        // 鸳鸯 / 田字锅：每边的汤底用「/」列出来（同锅的状态栏，调整）
        PotLayout layout =
            stack.getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof HotPotBlock pot
                ? pot.material().layout() : PotLayout.NINE_GRID;
        String[] keys = {"Base", "BaseB", "BaseC", "BaseD"};
        // mryt233 改（2026-11-05，r136a）：红汤边在提示里也带辣度档位，比如「红汤锅底（特辣）」
        String[] spicyKeys = {"Spicy", "SpicyB", "SpicyC", "SpicyD"};
        Component soupName = HotPotBlockEntity.soupText(layout,
            side -> nbt.contains(keys[side]) ? nbt.getInt(keys[side]) : -1,
            side -> nbt.contains(spicyKeys[side]) ? nbt.getInt(spicyKeys[side]) : 0);
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.pot.soup"), soupName)
            .withStyle(ChatFormatting.GRAY));
        int food = nbt.getList("Items", Tag.TAG_COMPOUND).size();
        if (food > 0) {
            int cooked = 0;
            for (byte b : nbt.getByteArray("Cooked")) {
                cooked += b != 0 ? 1 : 0;
            }
            lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.pot.food"), food, cooked)
                .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.pot.hint"))
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
