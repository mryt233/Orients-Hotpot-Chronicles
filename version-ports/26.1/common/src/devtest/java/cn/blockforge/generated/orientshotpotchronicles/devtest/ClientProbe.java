package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.client.*;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.network.OpenSauceRestockPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.nio.file.Files;

/** 可选开发客户端验收探针；只读本实例请求，不捕获/操纵用户桌面。不进 jar。作者：mryt233 */
public final class ClientProbe {
    private static int ticks;
    private static String lastId = "";
    private static String pending;
    private static int readyAt;

    private ClientProbe() {
    }

    public static void tick() {
        if (!Boolean.getBoolean("hotpot.clientProbe")) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (pending != null && ticks >= readyAt) {
            snapshot(mc, pending);
            pending = null;
        }
        if (ticks % 10 != 0) {
            return;
        }
        var file = mc.gameDirectory.toPath().resolve("hotpot-probe-request.json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            var request = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String id = request.get("id").getAsString();
            if (id.equals(lastId) || !id.matches("[a-zA-Z0-9_-]{1,64}")) {
                return;
            }
            lastId = id;
            String action = request.get("action").getAsString();
            switch (action) {
                case "snapshot" -> { }
                case "guide" -> mc.setScreen(new GuideBookScreen());
                case "overview" -> mc.setScreen(new SauceOverviewScreen(new BlockPos(82, 4, 80), Component.literal("Dev sauce")));
                case "restock" -> {
                    if (!(mc.screen instanceof SauceOverviewScreen)) {
                        throw new IllegalStateException("Restock requires overview screen");
                    }
                    var button = mc.screen.children().stream()
                        .filter(widget -> widget instanceof net.minecraft.client.gui.components.Button)
                        .map(widget -> (net.minecraft.client.gui.components.Button) widget).findFirst().orElseThrow();
                    button.onPress();
                }
                case "inventory" -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                case "aim" -> {
                    var pos = new BlockPos(request.get("x").getAsInt(), request.get("y").getAsInt(), request.get("z").getAsInt());
                    int cell = request.get("cell").getAsInt();
                    if (mc.level.getBlockEntity(pos) instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity be) {
                        cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction.clientChopsticksAim.accept(be, cell);
                        HotPot.LOGGER.info("[hotpot-probe] aim reported pos={} cell={}", pos, cell);
                    }
                }
                case "creative" -> {
                    var screen = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                        mc.player, mc.player.connection.enabledFeatures(), true);
                    mc.setScreen(screen);
                    ((cn.blockforge.generated.orientshotpotchronicles.mixin.CreativeInventoryScreenAccessor) screen)
                        .invokeSetSelectedTab(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.ITEM_GROUP);
                }
                case "close" -> {
                    if (mc.player != null) {
                        mc.player.closeContainer();
                    }
                    mc.setScreen(null);
                }
                case "disconnect" -> {
                    if (mc.level != null) {
                        mc.level.disconnect();
                        mc.disconnect();
                    }
                }
                case "stop" -> {
                    HotPot.LOGGER.info("[hotpot-probe] 开发客户端正常退出");
                    mc.stop();
                    return;
                }
                default -> throw new IllegalArgumentException("Unsupported probe action");
            }
            pending = id;
            readyAt = ticks + 40;
        } catch (Exception e) {
            HotPot.LOGGER.error("[hotpot-probe] 请求处理失败", e);
        }
    }

    private static void snapshot(Minecraft mc, String id) {
        var report = new com.google.gson.JsonObject();
        report.addProperty("id", id);
        report.addProperty("screen", mc.screen == null ? "none" : mc.screen.getClass().getName());
        report.addProperty("world", mc.level != null);
        report.addProperty("player", mc.player == null ? "none" : mc.player.getGameProfile().getName());
        report.addProperty("menu", mc.player == null ? "none" : mc.player.containerMenu.getClass().getName());
        if (mc.player != null) {
            report.addProperty("x", mc.player.getX());
            report.addProperty("y", mc.player.getY());
            report.addProperty("z", mc.player.getZ());
        }
        report.addProperty("ham_sausage_hunger", Config.foodHunger("ham_sausage", -1));
        report.addProperty("module_declaration_calls", cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.declarationCalls());
        report.addProperty("module_client_calls", cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.clientCalls());
        report.addProperty("module_runtime_calls", cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.runtimeCalls());
        report.addProperty("module_order", String.join(",", cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.HOST.order()));
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("hotpot-probe-" + id + ".json"), report.toString());
            Screenshot.grab(mc.gameDirectory, "hotpot-probe-" + id + ".png", mc.getMainRenderTarget(),
                message -> HotPot.LOGGER.info("[hotpot-probe] {} 截图完成", id));
            HotPot.LOGGER.info("[hotpot-probe] {} screen={} menu={}", id, report.get("screen"), report.get("menu"));
        } catch (Exception e) {
            HotPot.LOGGER.error("[hotpot-probe] 取证失败", e);
        }
    }
}
