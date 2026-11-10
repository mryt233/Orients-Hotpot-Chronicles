// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import cn.blockforge.orient.framework.client.ClientContent;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 真实界面、提示、连接与注册实例取证；不注册假内容。 */
public final class Opt3dClientProbe {
    private static boolean installed;
    private static int disconnects;
    private static String tooltip = "";
    private static final JsonObject renderers = new JsonObject();
    private Opt3dClientProbe() { }
    public static void action(JsonObject request) {
        if (!installed) {
            ClientContent.get().onDisconnect(() -> disconnects++);
            installed = true;
        }
        var args = request.getAsJsonArray("extra");
        String action = args.get(0).getAsString();
        Minecraft mc = Minecraft.getInstance();
        if (action.equals("world")) {
            mc.createWorldOpenFlows().openWorld("opt3d-client", () -> { });
        } else if (action.equals("tooltip")) {
            var stack = new ItemStack(HotPotContent.HAM_SAUSAGE);
            stack.set(HotPotContent.COOKED_IN_SOUP, "spicy");
            stack.set(HotPotContent.SPICY_LEVEL, 2);
            tooltip = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)
                .stream().map(Component::getString).collect(java.util.stream.Collectors.joining("|"));
            if (tooltip.isBlank()) throw new IllegalStateException("empty tooltip");
        } else if (action.equals("renderers")) {
            for (var pos : java.util.List.of(new net.minecraft.core.BlockPos(176, 4, 176),
                    new net.minecraft.core.BlockPos(178, 4, 176), new net.minecraft.core.BlockPos(176, 4, 178))) {
                var block = mc.level.getBlockEntity(pos);
                var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(block);
                if (renderer == null) throw new IllegalStateException("missing renderer at " + pos);
                renderers.addProperty(pos.toShortString(), renderer.getClass().getName());
            }
            var seat = HotPotContent.SEAT.create(mc.level);
            var renderer = mc.getEntityRenderDispatcher().getRenderer(seat);
            if (renderer == null) throw new IllegalStateException("missing seat renderer");
            renderers.addProperty("seat", renderer.getClass().getName());
        } else if (action.equals("command")) {
            var server = mc.getSingleplayerServer();
            if (server == null || mc.player == null) throw new IllegalStateException("isolated world required");
            var uuid = mc.player.getUUID();
            server.execute(() -> server.getCommands().performPrefixedCommand(
                server.getPlayerList().getPlayer(uuid).createCommandSourceStack().withPermission(4), args.get(1).getAsString()));
        } else if (action.equals("save_config")) {
            if (!(mc.screen instanceof cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen screen)) {
                throw new IllegalStateException("config screen required");
            }
            try {
                Class<?> type = screen.getClass();
                var edited = type.getDeclaredField("edited");
                edited.setAccessible(true);
                ((JsonObject) edited.get(screen)).getAsJsonObject("client").addProperty("particle_intensity", 90);
                int px = field(type, screen, "px"), py = field(type, screen, "py");
                int width = field(type, screen, "panelW"), height = field(type, screen, "panelH");
                int bottom = field(type, screen, "BOTTOM_H");
                int button = Math.max(70, Math.min(150, (width - 36) / 3));
                int x = px + (width - (button * 3 + 12)) / 2 + button / 2;
                int y = py + height - bottom - 8 + bottom / 2;
                screen.mouseClicked(x, y, 0);
                screen.mouseReleased(x, y, 0);
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
    }
    private static int field(Class<?> type, Object instance, String name) throws ReflectiveOperationException {
        var value = type.getDeclaredField(name);
        value.setAccessible(true);
        return value.getInt(instance);
    }
    public static JsonObject status() {
        JsonObject out = new JsonObject();
        out.addProperty("disconnects", disconnects);
        out.addProperty("tooltip", tooltip);
        out.add("renderers", renderers.deepCopy());
        return out;
    }
}
