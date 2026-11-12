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
    /** 截图时要强制保持的玩家朝向（度）；null = 不干预。mryt233 改（2026-11-09，r146b）。 */
    private static Float holdYaw;
    /** 身体朝向单独保持（看侧面时身体转 90°、镜头不转）；null = 跟 holdYaw 一样。mryt233 改（r146g）。 */
    private static Float holdBody;
    // mryt233 改：r146e 实景取证保持俯仰，不操纵桌面输入。
    private static Float holdPitch;
    private static String lastId = "";
    private static String pending;
    private static int readyAt;
    // mryt233 改（r146 合并实测）：真实前进输入及第三人称取景，只用于开发取证。
    private static int walkUntil;

    private ClientProbe() {
    }

    public static void tick() {
        if (!Boolean.getBoolean("hotpot.clientProbe")) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        // 截图用：每 tick 把朝向按回去，否则玩家转身会被位置同步覆盖
        if (holdYaw != null && mc.player != null) {
            mc.player.setYRot(holdYaw);
            mc.player.setYHeadRot(holdYaw);
            mc.player.setYBodyRot(holdBody != null ? holdBody : holdYaw);
            mc.player.yBodyRotO = holdBody != null ? holdBody : holdYaw;
            if (holdPitch != null) mc.player.setXRot(holdPitch);
            if (ticks >= walkUntil) mc.player.setDeltaMovement(0, 0, 0);
        }
        if (walkUntil > 0 && ticks >= walkUntil) {
            mc.options.keyUp.setDown(false);
            walkUntil = 0;
        }
        if (pending != null && ticks >= readyAt) {
            snapshot(mc, pending);
            pending = null;
        }
        // mryt233 改（r146f）：等资源加载遮罩退出后再读档，避免启动阶段嵌套加载。
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
            String action = request.get("action").getAsString();
            // mryt233 改（2026-11-10）：加载中也可截图和关本实例；进世界仍等遮罩退出。
            if (mc.getOverlay() != null && !action.equals("snapshot") && !action.equals("stop")) return;
            lastId = id;
            switch (action) {
                // mryt233 改（2026-11-10）：3C 客户端能力取证。
                case "opt3c" -> Opt3cClientProbe.action(request);
                // mryt233 改（2026-11-10）：3D 只取证真实火锅内容，不登记假物品。
                case "opt3d" -> Opt3dClientProbe.action(request);
                case "snapshot" -> { }
                case "observe" -> {
                    if (mc.player == null) throw new IllegalStateException("Observer requires player");
                    var extra = request.getAsJsonArray("extra");
                    // 使用原版第三人称保持本地玩家输入生效，侧面沿用身体与镜头分离。
                    boolean side = Math.abs(extra.get(0).getAsDouble()) > .5;
                    mc.setCameraEntity(mc.player);
                    mc.options.setCameraType(!side && extra.get(2).getAsDouble() > 0
                        ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                        : net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                    mc.options.fov().set(55); mc.options.hideGui = true; mc.setScreen(null);
                    holdYaw = 0F; holdBody = side ? Float.valueOf(90F) : null; holdPitch = 0F;
                }
                case "walk" -> {
                    if (mc.player == null) throw new IllegalStateException("Walk requires player");
                    var extra = request.getAsJsonArray("extra");
                    int duration = Math.max(2, Math.min(100, extra.get(0).getAsInt()));
                    int captureAt = Math.max(1, Math.min(duration - 1, extra.get(1).getAsInt()));
                    mc.options.keyUp.setDown(true); walkUntil = ticks + duration;
                    readyAt = ticks + captureAt;
                }
                case "pose" -> {
                    boolean crouch = request.getAsJsonArray("extra").get(0).getAsString().equals("crouch");
                    mc.options.keyShift.setDown(crouch);
                }
                case "input" -> {
                    var extra = request.getAsJsonArray("extra");
                    if (mc.screen == null) throw new IllegalStateException("Input requires screen");
                    double x = extra.get(0).getAsDouble(), y = extra.get(1).getAsDouble();
                    mc.screen.mouseClicked(x, y, 0); mc.screen.mouseReleased(x, y, 0);
                }
                case "useblock" -> {
                    var extra = request.getAsJsonArray("extra");
                    var pos = new BlockPos(extra.get(0).getAsInt(), extra.get(1).getAsInt(), extra.get(2).getAsInt());
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(
                            new net.minecraft.world.phys.Vec3(pos.getX()+.5, pos.getY()+.4, pos.getZ()+.5),
                            net.minecraft.core.Direction.UP, pos, false));
                }
                // mryt233 改（r146f）：独立超平坦世界和原料表输入取证，不操纵桌面。
                case "world" -> mc.createWorldOpenFlows().openWorld("r146f-flat", () -> { });
                case "language" -> {
                    String language = request.getAsJsonArray("extra").get(0).getAsString();
                    mc.getLanguageManager().setSelected(language);
                    mc.options.languageCode = language;
                    mc.reloadResourcePacks();
                }
                case "resize" -> {
                    var extra = request.getAsJsonArray("extra");
                    mc.options.guiScale().set(Integer.parseInt(extra.get(2).getAsString()));
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),
                        Integer.parseInt(extra.get(0).getAsString()), Integer.parseInt(extra.get(1).getAsString()));
                    mc.resizeDisplay();
                }
                case "sauce" -> {
                    if (mc.player == null) throw new IllegalStateException("Sauce requires a player");
                    var server = mc.getSingleplayerServer();
                    if (server == null) throw new IllegalStateException("Sauce fixture requires an isolated singleplayer world");
                    var uuid = mc.player.getUUID();
                    server.execute(() -> {
                        var player = server.getPlayerList().getPlayer(uuid);
                        if (player == null) return;
                        var pos = new BlockPos(82, -60, 80);
                        player.serverLevel().setBlockAndUpdate(pos,
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(HotPot.id("bamboo_sauce_station")).defaultBlockState());
                        player.teleportTo(82.5, -60, 82.5);
                        new OpenSauceRestockPayload(pos).handle(player);
                    });
                }
                case "sauce_restock" -> {
                    if (!(mc.screen instanceof SauceOverviewScreen ov)) throw new IllegalStateException("SauceOverviewScreen required");
                    ov.children().stream()
                        .filter(w -> w instanceof net.minecraft.client.gui.components.Button)
                        .map(w -> (net.minecraft.client.gui.components.Button) w)
                        .findFirst().orElseThrow().onPress();
                }
                case "sauceinput" -> {
                    if (!(mc.screen instanceof SauceStationScreen screen)) throw new IllegalStateException("Sauce screen required");
                    for (var value : request.getAsJsonArray("extra")) {
                        String input = value.getAsString();
                        var toggle = screen.children().stream()
                            .filter(widget -> widget instanceof net.minecraft.client.gui.components.Button)
                            .map(widget -> (net.minecraft.client.gui.components.Button) widget)
                            .filter(button -> button.getMessage().getString().equals("?")).findFirst().orElseThrow();
                        if (input.equals("toggle")) toggle.onPress();
                        else if (input.equals("next") || input.equals("previous")) {
                            String label = input.equals("next") ? ">" : "<";
                            var button = screen.children().stream()
                                .filter(widget -> widget instanceof net.minecraft.client.gui.components.Button)
                                .map(widget -> (net.minecraft.client.gui.components.Button) widget)
                                .filter(widget -> widget.getMessage().getString().equals(label)).findFirst().orElseThrow();
                            screen.mouseClicked(button.getX() + 3, button.getY() + 3, 0);
                        } else {
                            var area = screen.guideAreas().getFirst();
                            screen.mouseScrolled(area.getX() + 10, area.getY() + 30, 0,
                                input.equals("wheelup") ? 1 : -1);
                        }
                    }
                }

                // mryt233 改（2026-11-11）：只读排版导出，不进入成品包。
                case "opt3h2" -> ConfigAt.run(mc, request.getAsJsonArray("extra"));
                case "guideat" -> GuideAt.run(mc, request.getAsJsonArray("extra"));
                case "guide" -> mc.setScreen(new GuideBookScreen());
                // mryt233 改（2026-11-09，r146a）：截图用——打开游戏内设置界面
                case "config" -> mc.setScreen(new HotPotConfigScreen(mc.screen));
                // 在设置界面里切页签 / 展开分组：extra = 参数（"advanced" / "expand:0" / "shift"）
                case "configset" -> {
                    if (!(mc.screen instanceof HotPotConfigScreen screen)) {
                        throw new IllegalStateException("Configset requires the config screen");
                    }
                    for (int i = 0; i < request.getAsJsonArray("extra").size(); i++) {
                        screen.probeAction(request.getAsJsonArray("extra").get(i).getAsString());
                    }
                }
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
                // mryt233 改（2026-11-09，r146b）：截图用——把自己身上换成指定那一款围裙
                case "wear" -> {
                    if (mc.player == null) {
                        throw new IllegalStateException("Wear requires a player");
                    }
                    String wornId = request.getAsJsonArray("extra").get(0).getAsString();
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation.parse(wornId));
                    mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                        new net.minecraft.world.item.ItemStack(item));
                    // 手上清空、别挡着围裙；顺便把视角摆正（默认第三人称背面）
                    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        net.minecraft.world.item.ItemStack.EMPTY);
                    mc.setScreen(null);
                }
                // 第三人称 + 朝向（back / front）：截图时看背面 / 正面
                case "camera" -> {
                    mc.setCameraEntity(mc.player); holdBody = null;
                    var args = request.getAsJsonArray("extra");
                    holdYaw = args.get(0).getAsFloat();
                    holdPitch = args.get(1).getAsFloat();
                    mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                    mc.options.fov().set(Math.max(30, Math.min(110, args.size() > 2 ? args.get(2).getAsInt() : 30)));
                    mc.options.hideGui = true;
                    mc.setScreen(null);
                }
                case "view" -> {
                    mc.setCameraEntity(mc.player); holdPitch = 0F;
                    mc.options.fov().set(55);
                    // 第三人称：back = 镜头在身后（看玩家背面）；front = 前置镜头（正对玩家正面）。
                    // 注意不能用「把玩家转 180°」来看正面——后置镜头会跟着一起转，看到的还是背面。
                    String mode = request.getAsJsonArray("extra").get(0).getAsString();
                    boolean front = "front".equals(mode);
                    mc.options.setCameraType("hand".equals(mode) ? net.minecraft.client.CameraType.FIRST_PERSON : front
                        ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                        : net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                    // mryt233 改（2026-11-09，r146g）：side / side2 = 镜头在身后、人转 90°（看左 / 右侧面）
                    float yaw = 0F;
                    holdBody = "side".equals(mode) ? Float.valueOf(90F) : "side2".equals(mode) ? Float.valueOf(-90F) : null;
                    holdYaw = yaw;
                    if (mc.player != null) {
                        mc.player.setYRot(yaw);
                        mc.player.setYHeadRot(yaw);
                        mc.player.setYBodyRot(holdBody != null ? holdBody : yaw);
                        mc.player.setDeltaMovement(0, 0, 0);
                    }
                    mc.options.hideGui = !"hand".equals(mode);
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
                case "connect" -> {
                    String addr = request.getAsJsonArray("extra").get(0).getAsString();
                    var srv = new net.minecraft.client.multiplayer.ServerData(
                        "dev", addr, net.minecraft.client.multiplayer.ServerData.Type.OTHER);
                    net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                        new net.minecraft.client.gui.screens.TitleScreen(), mc,
                        net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(addr),
                        srv, false, null);
                }
                case "stop" -> {
                    HotPot.LOGGER.info("[hotpot-probe] 开发客户端正常退出");
                    mc.stop();
                    return;
                }
                default -> throw new IllegalArgumentException("Unsupported probe action");
            }
            pending = id;
            if (!action.equals("walk")) readyAt = ticks + (Boolean.getBoolean("hotpot.opt3c") ? 6 : 40);
        } catch (Exception e) {
            HotPot.LOGGER.error("[hotpot-probe] 请求处理失败", e);
        }
    }

    private static void snapshot(Minecraft mc, String id) {
        var report = new com.google.gson.JsonObject();
        report.addProperty("id", id);
        report.addProperty("overlay", mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getName());
        report.addProperty("window_focused", org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),
            org.lwjgl.glfw.GLFW.GLFW_FOCUSED) != 0);
        report.addProperty("pause_on_lost_focus", mc.options.pauseOnLostFocus);
        if (Boolean.getBoolean("hotpot.opt3c")) report.add("opt3c", Opt3cClientProbe.status());
        report.addProperty("screen", mc.screen == null ? "none" : mc.screen.getClass().getName());
        report.add("opt3d", Opt3dClientProbe.status());
        report.add("guideat", GuideAt.result());
        report.add("opt3h2", ConfigAt.result());
        report.addProperty("window_width", mc.getWindow().getWidth());
        report.addProperty("window_height", mc.getWindow().getHeight());
        report.addProperty("gui_width", mc.getWindow().getGuiScaledWidth());
        report.addProperty("gui_height", mc.getWindow().getGuiScaledHeight());
        report.addProperty("gui_scale", mc.getWindow().getGuiScale());
        report.addProperty("jei_loaded", cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat.isModLoaded("jei"));
        report.addProperty("rei_loaded", cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat.isModLoaded("roughlyenoughitems"));
        if (mc.level != null) {
            var opt4f = new com.google.gson.JsonObject();
            var ginger = cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups.get("opt4fpack:ginger_soup");
            if (ginger != null) {
                opt4f.addProperty("ginger_soup", true);
                opt4f.addProperty("ginger_speed", ginger.defaultSpeedMul());
                opt4f.addProperty("ginger_name_key", ginger.nameKey());
                opt4f.addProperty("ginger_visual", ginger.visual().getSerializedName());
                var bowlStack = cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups.bowlStack(ginger);
                opt4f.addProperty("ginger_bowl_item",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(bowlStack.getItem()).toString());
                opt4f.addProperty("ginger_bowl_comp",
                    bowlStack.get(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.SOUP));
                opt4f.addProperty("ginger_bowl_name", bowlStack.getHoverName().getString());
                var bowlTips = new java.util.ArrayList<Component>();
                ClientTooltips.append(bowlStack, bowlTips);
                opt4f.add("ginger_bowl_tooltip", new com.google.gson.Gson().toJsonTree(bowlTips.stream().map(Component::getString).toList()));
            }
            var clearBowl = cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups.bowlStack(
                cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups.clear());
            opt4f.addProperty("clear_bowl_item",
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(clearBowl.getItem()).toString());
            var unknownBowl = new net.minecraft.world.item.ItemStack(
                cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.SOUP_BOWL);
            unknownBowl.set(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.SOUP, "missing:nope");
            opt4f.addProperty("unknown_bowl_name", unknownBowl.getHoverName().getString());
            var unknownTips = new java.util.ArrayList<Component>();
            ClientTooltips.append(unknownBowl, unknownTips);
            opt4f.add("unknown_bowl_tooltip", new com.google.gson.Gson().toJsonTree(unknownTips.stream().map(Component::getString).toList()));
            var truffle = cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings.get("opt4fpack:truffle");
            if (truffle != null) {
                opt4f.addProperty("truffle", true);
                opt4f.addProperty("truffle_garnish", truffle.garnish());
                opt4f.addProperty("truffle_effect_key", truffle.effectKey());
                var testSlice = new net.minecraft.world.item.ItemStack(
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.COOKED_BEEF_LOIN_SLICE);
                testSlice.set(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.COOKED_IN_SOUP, "opt4fpack:ginger_soup");
                var sliceTips = new java.util.ArrayList<Component>();
                ClientTooltips.append(testSlice, sliceTips);
                var sauceStack = cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem.createWithId("opt4fpack:truffle");
                var sauceTips = new java.util.ArrayList<Component>();
                ClientTooltips.append(sauceStack, sauceTips);
                opt4f.add("slice_tooltip", new com.google.gson.Gson().toJsonTree(sliceTips.stream().map(Component::getString).toList()));
                opt4f.add("sauce_tooltip", new com.google.gson.Gson().toJsonTree(sauceTips.stream().map(Component::getString).toList()));
            }
            opt4f.addProperty("soups_count", cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups.size());
            opt4f.addProperty("seasonings_count", cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings.size());
            for (int sx : new int[]{2040, 2042}) {
                if (mc.level.getBlockEntity(new BlockPos(sx, 4, 2040)) instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity be) {
                    var sdef = be.kindDef(0);
                    opt4f.addProperty("station_" + sx + "_def0", sdef != null ? sdef.id() : "null");
                    opt4f.addProperty("station_" + sx + "_portions0", be.portions(0));
                    opt4f.addProperty("station_" + sx + "_avail0", be.available(0));
                }
            }
            report.add("opt4f", opt4f);
        }
        if (mc.screen instanceof HotPotConfigScreen config) {
            try {
                for (String name : new String[]{"px", "py", "panelW", "panelH", "listX", "listW", "listTop", "listBottom", "tab"}) {
                    var field = HotPotConfigScreen.class.getDeclaredField(name); field.setAccessible(true);
                    report.addProperty("config_"+name, field.getInt(config));
                }
                var field = HotPotConfigScreen.class.getDeclaredField("edited"); field.setAccessible(true);
                report.add("config_edited", ((com.google.gson.JsonObject)field.get(config)).deepCopy());
                report.add("config_current", Config.editableCopy());
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        if (mc.screen instanceof SauceStationScreen sauce) {
            try {
                var menu = sauce.getMenu();
                var mdef = menu.kindDef(0);
                report.addProperty("sauce_menu_def0", mdef != null ? mdef.id() : "null");
                report.addProperty("sauce_menu_avail0", menu.available(0));
                report.addProperty("sauce_menu_unknown0", menu.unknownKind(0));
                report.addProperty("sauce_menu_mayPlace_lapis", menu.getSlot(0).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI)));
                report.addProperty("sauce_menu_mayPlace_brick", menu.getSlot(0).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRICK)));
                report.addProperty("sauce_menu_mayPlace_wheat", menu.getSlot(0).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT)));
                for (String name : new String[]{"guidePage", "guideOpen"}) {
                    var field = SauceStationScreen.class.getDeclaredField(name);
                    field.setAccessible(true);
                    report.addProperty(name, String.valueOf(field.get(sauce)));
                }
                var field = SauceStationScreen.class.getDeclaredField("guidePages");
                field.setAccessible(true);
                var pages = (java.util.List<?>) field.get(sauce);
                int total = 0, wrapped = 0, bottom = 0;
                for (var page : pages) {
                    int y = 22;
                    for (var row : (java.util.List<?>) page) {
                        var height = row.getClass().getDeclaredMethod("height"); height.setAccessible(true);
                        var seasoning = row.getClass().getDeclaredMethod("seasoning"); seasoning.setAccessible(true);
                        var inline = row.getClass().getDeclaredMethod("inline"); inline.setAccessible(true);
                        var s = (cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef) seasoning.invoke(row);
                        if (total < cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning.ALL.length
                            && !cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds.fullId(s)
                                .equals(cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds.fullId(
                                    cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning.ALL[total].def())))
                            throw new IllegalStateException("Guide seasoning order changed");
                        total++;
                        if (!(boolean) inline.invoke(row)) wrapped++;
                        for (String name : new String[]{"names", "effects"}) {
                            var lines = row.getClass().getDeclaredMethod(name); lines.setAccessible(true);
                            for (var line : (java.util.List<?>) lines.invoke(row)) {
                                if (mc.font.width((net.minecraft.util.FormattedCharSequence) line) > 124)
                                    throw new IllegalStateException("Guide text outside panel");
                            }
                        }
                        y += (int) height.invoke(row);
                    }
                    if (y > 154) throw new IllegalStateException("Guide row outside content area");
                    bottom = Math.max(bottom, y);
                }
                if (total < cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning.ALL.length)
                    throw new IllegalStateException("Guide seasoning missing");
                report.addProperty("guide_content_verified", true);
                report.addProperty("guide_wrapped_rows", wrapped);
                report.addProperty("guide_content_bottom", bottom);
                report.addProperty("guide_page_count", pages.size());
                report.addProperty("guide_rows", pages.stream().map(page -> String.valueOf(((java.util.List<?>) page).size()))
                    .collect(java.util.stream.Collectors.joining(",")));
                report.add("guide_areas", new com.google.gson.Gson().toJsonTree(sauce.guideAreas()));
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }
        report.addProperty("world", mc.level != null);
        report.addProperty("player", mc.player == null ? "none" : mc.player.getGameProfile().getName());
        report.addProperty("menu", mc.player == null ? "none" : mc.player.containerMenu.getClass().getName());
        if (mc.player != null) {
            report.addProperty("x", mc.player.getX());
            report.addProperty("y", mc.player.getY());
            report.addProperty("z", mc.player.getZ());
            // mryt233 改：记录实际行走、骑乘及已渲染模型姿势，不能用预设动画代替实测。
            report.addProperty("crouching", mc.player.isCrouching());
            report.addProperty("passenger", mc.player.isPassenger());
            report.addProperty("walk_speed", mc.player.walkAnimation.speed());
            report.addProperty("walk_position", mc.player.walkAnimation.position());
            report.addProperty("chest_foil", mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).hasFoil());
            var apron = ApronModel.get();
            report.addProperty("apron_right_leg", apron.rightLeg.xRot);
            report.addProperty("apron_left_leg", apron.leftLeg.xRot);
            report.addProperty("apron_body_visible", apron.body.visible);
            try {
                var field = ApronModel.class.getDeclaredField("skirt"); field.setAccessible(true);
                var skirt = (net.minecraft.client.model.geom.ModelPart)field.get(apron);
                report.addProperty("apron_skirt", skirt.xRot);
                report.addProperty("apron_skirt_visible", skirt.visible);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
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
