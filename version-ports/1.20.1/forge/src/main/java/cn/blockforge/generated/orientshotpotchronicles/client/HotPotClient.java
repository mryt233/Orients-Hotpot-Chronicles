package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
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
// 1.20.1 Forge 移植：仅物理客户端订阅。
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID,
    value = net.minecraftforge.api.distmarker.Dist.CLIENT,
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class HotPotClient {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(HotPotClient::setupClient);
    }

    private static void setupClient() {
        HandledScreens.register(HotPotContent.HOT_POT_SCREEN, HotPotScreen::new);
        HandledScreens.register(HotPotContent.STOVE_SCREEN, StoveScreen::new);
        CreativeSubTabs.register();
        cn.blockforge.generated.orientshotpotchronicles.hotpot.GuideBookItem.clientOpen =
            () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new GuideBookScreen());
        PartOutlineRenderer.register();
        HandledScreens.register(HotPotContent.SAUCE_STATION_SCREEN, SauceStationScreen::new);
        cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlock.openOverview =
            (pos, name) -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new SauceOverviewScreen(pos, name));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
            (net.minecraftforge.event.entity.player.ItemTooltipEvent event) -> {
                appendPotTooltip(event.getItemStack(), event.getToolTip());
                appendPlateTooltip(event.getItemStack(), event.getToolTip());
                int dipped = cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.dipped(event.getItemStack());
                if (dipped != 0) {
                    event.getToolTip().add(Text.translatable(HotPotBlockEntity.key("tooltip.dipped"),
                        cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem.names(dipped)).formatted(Formatting.GOLD));
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning.appendEffects(dipped, event.getToolTip());
                }
                // 趣味小字（用户 2026-10-25）：语言文件里有 tooltip.<模组>.flavor.<物品 ID> 的物品，提示最后多一行灰色斜体。
                net.minecraft.util.Identifier flavorId = net.minecraft.registry.Registries.ITEM.getId(event.getItemStack().getItem());
                if (GeneratedMod.MOD_ID.equals(flavorId.getNamespace())) {
                    String flavorKey = "tooltip." + GeneratedMod.MOD_ID + ".flavor." + flavorId.getPath();
                    if (net.minecraft.client.resource.language.I18n.hasTranslation(flavorKey)) {
                        event.getToolTip().add(Text.translatable(flavorKey).formatted(Formatting.GRAY, Formatting.ITALIC));
                    }
                }
            });
        // 装着汤的锅物品（掉在地上、物品栏里）显示对应汤面：谓词 = (汤底编号 + 1) / 10，没汤为 0（r94，）
        for (net.minecraft.item.Item pot : new net.minecraft.item.Item[]{
            HotPotContent.IRON_HOT_POT_ITEM, HotPotContent.COPPER_HOT_POT_ITEM}) {
            net.minecraft.client.item.ModelPredicateProviderRegistry.register(pot, GeneratedMod.id("soup"),
                (stack, world, entity, seed) -> {
                    NbtCompound nbt = cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.potData(stack);   // 1.20.1 移植（）
                    if (nbt == null) {
                        return 0F;
                    }
                    SoupBase soup = nbt.contains("Base") ? SoupBase.byId(nbt.getInt("Base")) : null;
                    return soup == null ? 0F : (soup.ordinal() + 1) / 10F;
                });
        }
        // 鼠标中键选盘子：复制准星对着的那一个（连菜一起），由客户端告诉方块是谁在选
        cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.pickPlayer =
            () -> net.minecraft.client.MinecraftClient.getInstance().player;
        // 空筷子直接右键锅：把这一刻浮在那格上的是哪样报给服务端，夹的就是看到的那样（用户 2026-10-25，）
        cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction.clientChopsticksAim = (be, cell) -> {
            double ticks = be.getWorld().getTime() + (double) net.minecraft.client.MinecraftClient.getInstance().getTickDelta();
            int slot = HotPotBlockEntityRenderer.shownSlot(be, cell, ticks);
            if (slot >= 0) {
                cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopsticksAimPayload.send(be.getPos(), slot);
            }
        };
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HotPotContent.HOT_POT_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.STOVE_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.SAUCE_STATION_BE, SauceStationRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.CUTTING_BOARD_BE, CuttingBoardRenderer::new);
        event.registerEntityRenderer(HotPotContent.SEAT, net.minecraft.client.render.entity.EmptyEntityRenderer::new);
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
            || (stack.isOf(net.minecraft.item.Items.BOWL) && !cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.hasContents(stack))) {
            return;
        }
        java.util.List<ItemStack> foods = cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.contents(stack).stream().filter(s -> !s.isEmpty()).toList();
        lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.plate.count"), foods.size(), plate.size().capacity())
            .formatted(Formatting.GRAY));
        // 像原版收纳袋：同样的菜合成一行，后面写数量（r91，）
        java.util.List<ItemStack> kinds = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        for (ItemStack food : foods) {
            int at = -1;
            for (int k = 0; k < kinds.size(); k++) {
                if (ItemStack.canCombine(kinds.get(k), food)) {
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
        NbtCompound nbt = cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.potData(stack);
        if (nbt == null) {
            return;
        }
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
