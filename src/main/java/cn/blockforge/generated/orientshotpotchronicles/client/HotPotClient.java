package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Formatting;
import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** 客户端入口：注册火锅屏幕，并默认启用模组内置材质包。 */
public final class HotPotClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerBuiltinPack();
        HandledScreens.register(HotPotContent.HOT_POT_SCREEN,
            (HotPotScreenHandler handler, PlayerInventory inventory, Text title) ->
                new HotPotScreen(handler, inventory, title));
        HandledScreens.register(HotPotContent.STOVE_SCREEN,
            (StoveScreenHandler handler, PlayerInventory inventory, Text title) ->
                new StoveScreen(handler, inventory, title));
        BlockEntityRendererRegistry.register(HotPotContent.HOT_POT_BE,
            HotPotBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.STOVE_BE,
            HotPotBlockEntityRenderer::new);
        // 座位实体看不见，只是让玩家坐着
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(HotPotContent.SEAT,
            net.minecraft.client.render.entity.EmptyEntityRenderer::new);
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> appendPotTooltip(stack, lines));
        CreativeSubTabs.register();
        HeldFoodItemRenderer.register();
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> appendPlateTooltip(stack, lines));
        BlockEntityRendererRegistry.register(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        // 装着汤的锅物品（掉在地上、物品栏里）显示对应汤面：谓词 = (汤底编号 + 1) / 10，没汤为 0（r94，Claude）
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
        // 条凳的物品图标：凳面模型 + 斜腿（斜腿不在方块模型里，物品模型用 builtin/entity 交给这里画）
        for (net.minecraft.item.BlockItem item : HotPotContent.TALL_BENCH_ITEMS) {
            net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(item,
                (stack, mode, matrices, vertexConsumers, light, overlay) -> {
                    net.minecraft.block.BlockState state = item.getBlock().getDefaultState();
                    net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager()
                        .renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);
                    BenchLegsRenderer.renderLegs(state, matrices, vertexConsumers, light);
                });
        }
    }

    /** 内置资源包与模组一起加载，确保 16×16 材质和模型路径不会丢失。 */
    private static void registerBuiltinPack() {
        FabricLoader.getInstance().getModContainer(GeneratedMod.MOD_ID).ifPresent(container -> {
            boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(
                GeneratedMod.id("hotpot_assets"), container,
                Text.translatable("resourcePack." + GeneratedMod.MOD_ID + ".hotpot_assets"),
                ResourcePackActivationType.ALWAYS_ENABLED);
            if (!registered) {
                GeneratedMod.LOGGER.warn("[Orient的火锅物语] 内置资源包注册失败，继续使用模组默认资源");
            }
        });
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
        if (!(stack.getItem() instanceof net.minecraft.item.BlockItem item)
            || !(item.getBlock() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock plate)) {
            return;
        }
        net.minecraft.component.type.ContainerComponent contents = stack.getOrDefault(
            net.minecraft.component.DataComponentTypes.CONTAINER, net.minecraft.component.type.ContainerComponent.DEFAULT);
        java.util.List<ItemStack> foods = contents.streamNonEmpty().toList();
        lines.add(Text.translatable(HotPotBlockEntity.key("tooltip.plate.count"), foods.size(), plate.size().capacity())
            .formatted(Formatting.GRAY));
        // 像原版收纳袋：同样的菜合成一行，后面写数量（r91，Claude）
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
        SoupBase soup = nbt.contains("Base") ? SoupBase.byId(nbt.getInt("Base")) : null;
        Text soupName = Text.translatable(soup == null
            ? HotPotBlockEntity.key("soup.none") : soup.nameKey());
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
