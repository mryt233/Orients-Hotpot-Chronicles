package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Formatting;
import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** 客户端入口：注册火锅屏幕，绑定指南、渲染器和提示。 */
// 1.21.1 NeoForge 移植：该类只在物理客户端订阅。
@net.neoforged.fml.common.EventBusSubscriber(modid = GeneratedMod.MOD_ID,
    value = net.neoforged.api.distmarker.Dist.CLIENT,
    bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class HotPotClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(HotPotClient::setupClient);
    }

    private static void setupClient() {
        // ：客户端钩子保持主线时序，网络发送走NeoForge。
        cn.blockforge.generated.orientshotpotchronicles.hotpot.GuideBookItem.clientOpen =
            () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new GuideBookScreen());
        cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction.clientChopsticksAim = (be, cell) -> {
            double ticks = be.getWorld().getTime() + (double) net.minecraft.client.MinecraftClient.getInstance()
                .getRenderTickCounter().getTickDelta(true);
            int slot = HotPotBlockEntityRenderer.shownSlot(be, cell, ticks);
            if (slot >= 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopsticksAimPayload(be.getPos(), slot));
            }
        };
        CreativeSubTabs.register();
        PartOutlineRenderer.register();
        cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlock.openOverview =
            (pos, name) -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new SauceOverviewScreen(pos, name));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) -> {
                appendPotTooltip(event.getItemStack(), event.getToolTip());
                appendPlateTooltip(event.getItemStack(), event.getToolTip());
                Integer dipped = event.getItemStack().get(HotPotContent.DIPPED);
                if (dipped != null && dipped != 0) {
                    event.getToolTip().add(Text.translatable(HotPotBlockEntity.key("tooltip.dipped"),
                        cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem.names(dipped)).formatted(Formatting.GOLD));
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning.appendEffects(dipped, event.getToolTip());
                }
                var id = net.minecraft.registry.Registries.ITEM.getId(event.getItemStack().getItem());
                String key = "tooltip." + GeneratedMod.MOD_ID + ".flavor." + id.getPath();
                if (GeneratedMod.MOD_ID.equals(id.getNamespace()) && net.minecraft.client.resource.language.I18n.hasTranslation(key)) {
                    event.getToolTip().add(Text.translatable(key).formatted(Formatting.GRAY, Formatting.ITALIC));
                }
            });
        // 装着汤的锅物品（掉在地上、物品栏里）显示对应汤面：谓词 = (汤底编号 + 1) / 10，没汤为 0（r94，）
        for (net.minecraft.item.Item pot : new net.minecraft.item.Item[]{
            HotPotContent.IRON_HOT_POT_ITEM, HotPotContent.COPPER_HOT_POT_ITEM}) {
            net.minecraft.client.item.ModelPredicateProviderRegistry.register(pot, GeneratedMod.id("soup"),
                (stack, world, entity, seed) -> {
                    NbtComponent data = stack.get(HotPotContent.POT_CONTENTS);
                    if (data == null) {
                        return 0F;
                    }
                    NbtCompound nbt = data.copyNbt();
                    SoupBase soup = nbt.contains("Base") ? SoupBase.byId(nbt.getInt("Base")) : null;
                    return soup == null ? 0F : (soup.ordinal() + 1) / 10F;
                });
        }
        // 鼠标中键选盘子：复制准星对着的那一个（连菜一起），由客户端告诉方块是谁在选
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.pickPlayer =
            () -> net.minecraft.client.MinecraftClient.getInstance().player;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(HotPotContent.HOT_POT_SCREEN, HotPotScreen::new);
        event.register(HotPotContent.STOVE_SCREEN, StoveScreen::new);
        event.register(HotPotContent.SAUCE_STATION_SCREEN, SauceStationScreen::new);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HotPotContent.HOT_POT_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.STOVE_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.SAUCE_STATION_BE, SauceStationRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.CUTTING_BOARD_BE, CuttingBoardRenderer::new);
        event.registerEntityRenderer(HotPotContent.SEAT, net.minecraft.client.render.entity.EmptyEntityRenderer::new);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerItemExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        // 鸳鸯 / 田字锅的物品：锅身方块模型 + 按物品里存的每边汤底画汤面（任意组合都对，不用为每种组合做模型；）
        for (cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial material
            : cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial.values()) {
            if (!material.layout().multiSoup()) {
                continue;
            }
            net.minecraft.item.BlockItem item = HotPotContent.itemFor(material);
            NeoItemRenderers.register(event, item,
                (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                    net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager()
                        .renderBlockAsEntity(item.getBlock().getDefaultState(), matrices, vertexConsumers, light, overlay);
                    NbtComponent data = stack.get(HotPotContent.POT_CONTENTS);
                    if (data != null) {
                        HotPotBlockEntityRenderer.renderItemSoups(material, data.copyNbt(), matrices, vertexConsumers, light);
                    }
                });
        }
        HeldFoodItemRenderer.register(event);
        SauceRender.register(event);
        for (net.minecraft.item.BlockItem item : HotPotContent.TALL_BENCH_ITEMS) {
            NeoItemRenderers.register(event, item, (stack, mode, matrices, consumers, light, overlay) -> {
                var state = item.getBlock().getDefaultState();
                net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager()
                    .renderBlockAsEntity(state, matrices, consumers, light, overlay);
                BenchLegsRenderer.renderLegs(state, matrices, consumers, light);
            });
        }
    }

    /** 盘子：装了几样 / 能装几样，下面列出每一样；筷子：夹着什么。 */
    private static void appendPlateTooltip(ItemStack stack, java.util.List<Text> lines) {
        if (stack.isOf(HotPotContent.CHOPSTICKS)) {
            ItemStack held = cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks.held(stack);
            if (!held.isEmpty()) {
                lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.chopsticks.holding"), held.getName())
                    .formatted(Formatting.GRAY));
            }
            return;
        }
        // 盘子和装菜的碗（r99）；汤碗、橙子饮料这种整件的不列菜
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock plate =
            cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem.placedFor(stack);
        if (plate == null || cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem.keepsSelf(stack)
            || (stack.isOf(net.minecraft.item.Items.BOWL) && !stack.contains(net.minecraft.component.DataComponentTypes.CONTAINER))) {
            return;
        }
        net.minecraft.component.type.ContainerComponent contents = stack.getOrDefault(
            net.minecraft.component.DataComponentTypes.CONTAINER, net.minecraft.component.type.ContainerComponent.DEFAULT);
        java.util.List<ItemStack> foods = contents.streamNonEmpty().toList();
        lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.plate.count"), foods.size(), plate.size().capacity())
            .formatted(Formatting.GRAY));
        // 像原版收纳袋：同样的菜合成一行，后面写数量（r91，）
        java.util.List<ItemStack> kinds = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        for (ItemStack food : foods) {
            int at = -1;
            for (int k = 0; k < kinds.size(); k++) {
                if (ItemStack.areItemsAndComponentsEqual(kinds.get(k), food)) {
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
            Text line = Text.literal(" ").append(kinds.get(k).getName());
            if (counts.get(k) > 1) {
                line = line.copy().append(Text.literal(" ×" + counts.get(k)));
            }
            lines.add(line.copy().formatted(Formatting.GRAY));
        }
    }

    /** 装着东西的锅物品：提示里写汤底和食材份数。 */
    private static void appendPotTooltip(ItemStack stack, java.util.List<Text> lines) {
        NbtComponent data = stack.get(HotPotContent.POT_CONTENTS);
        if (data == null) {
            return;
        }
        NbtCompound nbt = data.copyNbt();
        // 鸳鸯 / 田字锅：每边的汤底用「/」列出来（同锅的状态栏，）
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout layout =
            stack.getItem() instanceof net.minecraft.item.BlockItem bi
                && bi.getBlock() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock pot
                ? pot.material().layout() : cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout.NINE_GRID;
        String[] keys = {"Base", "BaseB", "BaseC", "BaseD"};
        Text soupName = HotPotBlockEntity.soupText(layout, side -> nbt.contains(keys[side]) ? nbt.getInt(keys[side]) : -1);
        lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.pot.soup"), soupName)
            .formatted(Formatting.GRAY));
        int food = nbt.getList("Items", NbtElement.COMPOUND_TYPE).size();
        if (food > 0) {
            int cooked = 0;
            for (byte b : nbt.getByteArray("Cooked")) {
                cooked += b != 0 ? 1 : 0;
            }
            lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.pot.food"), food, cooked)
                .formatted(Formatting.GRAY));
        }
        lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.pot.hint"))
            .formatted(Formatting.DARK_GRAY));
    }
}
