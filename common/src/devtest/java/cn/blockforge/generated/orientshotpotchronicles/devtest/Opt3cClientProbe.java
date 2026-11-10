// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.orient.framework.client.ClientContent;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import java.util.concurrent.atomic.AtomicInteger;

/** 客户端证明使用真正的 Screen、渲染分发和物品提示事件。 */
public final class Opt3cClientProbe {
    private static final AtomicInteger screenFrames = new AtomicInteger();
    private static final AtomicInteger blockFrames = new AtomicInteger();
    private static final AtomicInteger entityFrames = new AtomicInteger();
    private static final AtomicInteger armorCalls = new AtomicInteger();
    private static final AtomicInteger tooltipCalls = new AtomicInteger();
    private static final AtomicInteger disconnects = new AtomicInteger();
    private static String tooltip = "";
    private static HumanoidModel<LivingEntity> model;

    public static void install() {
        ClientContent content = ClientContent.get();
        content.menu(Opt3cProbe.menu, ProbeScreen::new);
        content.blockEntityRenderer(Opt3cProbe.blockEntity, context -> new BlockEntityRenderer<>() {
            @Override
            public void render(Opt3cProbe.ProbeBE block, float partial, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay) {
                blockFrames.incrementAndGet();
                pose.pushPose();
                pose.translate(.5, 1.2, .5);
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.DIAMOND),
                    ItemDisplayContext.GROUND, light, overlay, pose, buffers, block.getLevel(), 0);
                pose.popPose();
            }
        });
        content.entityRenderer(Opt3cProbe.entity, context -> new ItemEntityRenderer(context) {
            @Override
            public void render(ItemEntity stand, float yaw, float partial, PoseStack pose,
                               MultiBufferSource buffers, int light) {
                entityFrames.incrementAndGet();
                super.render(stand, yaw, partial, pose, buffers, light);
            }
        });
        content.tooltip((stack, context, flag, lines) -> {
            if (stack.is(Opt3cProbe.item) || stack.is(Opt3cProbe.armor)) {
                tooltipCalls.incrementAndGet();
                lines.add(Component.literal("3C 提示回调生效"));
            }
        });
        content.armorModels((entity, stack, slot, original) -> {
            armorCalls.incrementAndGet();
            if (model == null) {
                model = new HumanoidModel<>(LayerDefinition.create(
                    HumanoidModel.createMesh(new CubeDeformation(.8F), 0), 64, 32).bakeRoot());
            }
            return model;
        }, Opt3cProbe.armor);
        content.onDisconnect(() -> {
            disconnects.incrementAndGet();
            Opt3cProbe.log("client-disconnect=" + disconnects.get());
        });
        Opt3cProbe.log("client registrations installed");
    }

    public static void action(JsonObject request) {
        Minecraft mc = Minecraft.getInstance();
        var args = request.getAsJsonArray("extra");
        String action = args.get(0).getAsString();
        if (action.equals("stats")) return;
        if (action.equals("tooltip")) {
            var lines = new ItemStack(Opt3cProbe.item).getTooltipLines(Item.TooltipContext.of(mc.level),
                mc.player, TooltipFlag.NORMAL);
            tooltip = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("|"));
            if (!tooltip.contains("3C 提示回调生效")) throw new IllegalStateException("tooltip callback missing");
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) throw new IllegalStateException("isolated world required");
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            var source = player.createCommandSourceStack().withPermission(4);
            if (action.equals("command")) {
                server.getCommands().performPrefixedCommand(source, args.get(1).getAsString());
            } else if (action.equals("menu")) {
                server.getCommands().performPrefixedCommand(source, "opt3c menu");
            } else if (action.equals("setup")) {
                server.getCommands().performPrefixedCommand(source, "fill 76 -61 76 86 -61 86 minecraft:stone");
                server.getCommands().performPrefixedCommand(source, "setblock 81 -60 80 opt3c:probe");
                server.getCommands().performPrefixedCommand(source, "summon opt3c:probe 79.5 -59 80.5 {NoGravity:1b,Item:{id:\"minecraft:emerald\",count:1},PickupDelay:32767,Age:-32768}");
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Opt3cProbe.armor));
                player.teleportTo(81.5, -60, 83.5);
            } else {
                throw new IllegalArgumentException("unknown 3C action: " + action);
            }
        });
    }

    public static JsonObject status() {
        JsonObject report = new JsonObject();
        report.addProperty("screen_frames", screenFrames.get());
        report.addProperty("block_frames", blockFrames.get());
        report.addProperty("entity_frames", entityFrames.get());
        report.addProperty("armor_calls", armorCalls.get());
        report.addProperty("tooltip_calls", tooltipCalls.get());
        report.addProperty("tooltip", tooltip);
        report.addProperty("disconnects", disconnects.get());
        report.addProperty("joins", Opt3cProbe.joins.get());
        report.addProperty("leaves", Opt3cProbe.leaves.get());
        return report;
    }

    public static final class ProbeScreen extends AbstractContainerScreen<Opt3cProbe.ProbeMenu> {
        public ProbeScreen(Opt3cProbe.ProbeMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
            screenFrames.incrementAndGet();
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFDDDDDD);
            graphics.drawString(font, "3C 客户端登记生效", leftPos + 12, topPos + 45, 0x203020, false);
        }
    }

    private Opt3cClientProbe() { }
}
