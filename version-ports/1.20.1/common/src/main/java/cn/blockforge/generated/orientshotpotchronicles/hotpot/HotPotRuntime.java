// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.compat.CompatRegistry;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.network.HotPotNetwork;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RuntimeHooks;
import cn.blockforge.orient.framework.pure.ConfigStore;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** 配置、命令、网络、可燃及运行期清理，两个加载器共用。 */
public final class HotPotRuntime {
    private static boolean compatInitialized;
    private static volatile MinecraftServer activeServer;
    private HotPotRuntime() { }

    public static void install(RuntimeHooks hooks) {
        HotPotFuel.values();
        Seasonings.all();
        SoupBase.values();
        HotPotCooking.cookRevision();
        Config.rememberCooking(HotPotCooking.DEFAULT_FALLBACK_TICKS,
            HotPotCooking.DEFAULT_FALLBACK_XP, HotPotCooking.DEFAULT_XP_PER_ITEM);
        Config.rememberWarmth(Config.DEFAULT_WARMTH_INTERVAL_TICKS, Config.DEFAULT_WARMTH_RANGE,
            Config.DEFAULT_WARMTH_DURATION, Config.DEFAULT_WARMTH_AMPLIFIER);
        Config.rememberColdPot(Config.DEFAULT_COLD_POT_NAUSEA_CHANCE, Config.DEFAULT_COLD_POT_NAUSEA_DURATION);
        Config.rememberCheers(Config.DEFAULT_CHEERS_COOLDOWN_TICKS,
            Config.DEFAULT_CHEERS_LUCK_DURATION, Config.DEFAULT_CHEERS_LUCK_AMPLIFIER,
            Config.DEFAULT_CHEERS_REGEN_DURATION, Config.DEFAULT_CHEERS_REGEN_AMPLIFIER,
            Config.DEFAULT_CHEERS_BONUS_HUNGER, Config.DEFAULT_CHEERS_BONUS_SATURATION);
        Config.rememberSpicy(Config.DEFAULT_SPICY_LEVEL_DURATION_STEP, Config.DEFAULT_SPICY_LEVEL_AMPLIFIER_FROM,
            Config.DEFAULT_SPICY_SECOND_BONUS_L1, Config.DEFAULT_SPICY_SECOND_BONUS_L2,
            Config.DEFAULT_SPICY_SECOND_BONUS_L3, Config.DEFAULT_SPICY_FIRE_FROM_LEVEL,
            Config.DEFAULT_SPICY_FIRE_TOP_MULTIPLIER, Config.DEFAULT_SPICY_HUNGER_TOP_BONUS,
            Config.DEFAULT_SPICY_MOUTH_FIRE_FROM_LEVEL, Config.DEFAULT_SPICY_MOUTH_FIRE_TICKS);
        Config.rememberLoot(Config.DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT, Config.DEFAULT_LOOT_LILY_PAD_LOTUS_ROOT_CHANCE,
            Config.DEFAULT_LOOT_MUSHROOM_ENOKI_CHANCE, Config.DEFAULT_LOOT_COW_BEEF_LOIN_CHANCE,
            Config.DEFAULT_LOOT_COW_BEEF_LOIN_LOOTING_BONUS);
        Config.rememberChickenLoot(Config.DEFAULT_LOOT_CHICKEN_FEET_CHANCE,
            Config.DEFAULT_LOOT_CHICKEN_FEET_LOOTING_BONUS);
        Config.rememberModeDefaults();
        Config.rememberCropsDefaults();
        Config.rememberProcessingDefaults();
        Config.rememberTableMaterialsDefaults();
        Config.rememberMinigameDefaults();
        Config.rememberGarnishLoot("scallion", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("cilantro", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("soybean", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("garlic", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("tomato", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("chili_pepper", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("peanut", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("greens", GarnishLoot.DEFAULT_CHANCE);
        Config.rememberGarnishLoot("zhe_er_gen", GarnishLoot.DEFAULT_CHANCE);
        var platform = Framework.platform();
        HotPotCompat.install(platform::isModLoaded);
        Config.init(platform.configDir(), platform.isDedicatedServer());
        Config.saveDefaultsIfNeeded();
        HotPotNetwork.register();
        var events = hooks.events();
        events.onServerStarting(server -> {
            if (!compatInitialized) {
                CompatRegistry.init();
                compatInitialized = true;
            }
            activeServer = server;
            Config.setServer(server);
            HotPotRecipes.setServer(server);
            HotPotHooks.freezeRegistries();
        });
        events.onPlayerJoin(player -> {
            TableMigration.migrateInventory(player);
            HotPotNetwork.sendConfig(player);
            HotPotNetwork.sendTracks(player);
        });
        events.onChunkLoad(TableMigration::migrateChunk);
        events.onPlayerLeave(player -> {
            ClothApronProtection.removePlayer(player.getUUID());
            HotPotMinigames.onPlayerLeave(player);
            CuttingBoardBlock.clearCache();
        });
        events.onServerTick(HotPotMinigames::tick);
        events.onServerStopping(server -> {
            ClothApronProtection.clearCache();
            HotPotMinigames.clearAll();
            CuttingBoardBlock.clearCache();
        });
        events.onServerStopped(server -> {
            activeServer = null;
            Config.setServer(null);
            HotPotRecipes.setServer(null);
            HotPotMinigames.clearAll();
        });
        events.onCommands((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal("orientshotpot")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2)).executes(command -> {
                        var result = Config.reload();
                        HotPotNetwork.broadcast(command.getSource().getServer());
                        HotPotNetwork.broadcastTracks(command.getSource().getServer());
                        command.getSource().sendSuccess(() -> Component.translatable(
                            HotPotBlockEntity.key("msg.config_reloaded"), result.warnings().size()), true);
                        for (var warning : result.warnings()) {
                            command.getSource().sendSuccess(() -> Config.describe(warning), false);
                        }
                        return 1;
                    }))
                .then(Commands.literal("mode")
                    .requires(source -> source.hasPermission(2))
                    .executes(command -> showMode(command.getSource()))
                    .then(Commands.literal("full").executes(command -> applyMode(command.getSource(), Config.MODE_FULL)))
                    .then(Commands.literal("simple").executes(command -> applyMode(command.getSource(), Config.MODE_SIMPLE))))
                .then(Commands.literal("minigame")
                    .executes(command -> showMinigameStatus(command.getSource()))
                    .then(Commands.literal("tracks").executes(command -> listTracks(command.getSource())))
                    .then(Commands.literal("accept").executes(command -> respondInvite(command.getSource(), true)))
                    .then(Commands.literal("decline").executes(command -> respondInvite(command.getSource(), false)))
                    .then(Commands.literal("solo")
                        .executes(command -> runMinigame(command.getSource(), "rush", "real", "", false))
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .executes(command -> runMinigame(command.getSource(),
                                StringArgumentType.getString(command, "mode"), "real", "", false))
                            .then(Commands.argument("food", StringArgumentType.word())
                                .executes(command -> runMinigame(command.getSource(),
                                    StringArgumentType.getString(command, "mode"),
                                    StringArgumentType.getString(command, "food"), "", false))
                                .then(Commands.argument("track", StringArgumentType.greedyString())
                                    .executes(command -> runMinigame(command.getSource(),
                                        StringArgumentType.getString(command, "mode"),
                                        StringArgumentType.getString(command, "food"),
                                        StringArgumentType.getString(command, "track"), false))))))
                    .then(Commands.literal("invite")
                        .executes(command -> runMinigame(command.getSource(), "rush", "real", "", true))
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .executes(command -> runMinigame(command.getSource(),
                                StringArgumentType.getString(command, "mode"), "real", "", true))
                            .then(Commands.argument("food", StringArgumentType.word())
                                .executes(command -> runMinigame(command.getSource(),
                                    StringArgumentType.getString(command, "mode"),
                                    StringArgumentType.getString(command, "food"), "", true))
                                .then(Commands.argument("track", StringArgumentType.greedyString())
                                    .executes(command -> runMinigame(command.getSource(),
                                        StringArgumentType.getString(command, "mode"),
                                        StringArgumentType.getString(command, "food"),
                                        StringArgumentType.getString(command, "track"), true)))))))));
        for (int i = 0; i < HotPotContent.TABLE_WOODS.size(); i++) {
            String wood = HotPotContent.TABLE_WOODS.get(i);
            if (wood.equals("crimson") || wood.equals("warped")) continue;
            // mryt233 改（2026-11-16）：全系列火锅桌统一不可燃，不再向原版火焰表登记木桌。
            events.flammable(HotPotContent.BENCHES.get(i), 5, 20);
            events.flammable(HotPotContent.TALL_BENCHES.get(i), 5, 20);
            events.flammable(HotPotContent.SAUCE_STATIONS.get(i), 5, 20);
        }
        events.flammable(HotPotContent.CUTTING_BOARD, 5, 20);
        events.flammable(HotPotContent.ORANGE_LOG, 5, 5);
        events.flammable(HotPotContent.ORANGE_WOOD, 5, 5);
        events.flammable(HotPotContent.STRIPPED_ORANGE_LOG, 5, 5);
        events.flammable(HotPotContent.STRIPPED_ORANGE_WOOD, 5, 5);
        events.flammable(HotPotContent.ORANGE_LEAVES, 30, 60);
        for (WildCropBlock wild : HotPotContent.WILD_CROPS) {
            events.flammable(wild, 60, 100);
        }
        events.beforeBlockBreak(HotPotBreaking::redirect);
    }

    private static int showMode(CommandSourceStack source) {
        Component modeName = Component.translatable("text." + HotPot.MOD_ID + ".config.mode." + Config.modePreset());
        source.sendSuccess(() -> Component.translatable(
            HotPotBlockEntity.key("msg.mode_current"),
            modeName,
            onOff(Config.detailedTofuProcessing()),
            onOff(Config.penaltiesEnabled()),
            onOff(Config.cropsReplaceGrass()),
            onOff(Config.minigamesEnabled()),
            onOff(Config.rackSwapMaterials()),
            onOff(Config.simpleUiCollapse())), false);
        return 1;
    }

    private static int applyMode(CommandSourceStack source, String preset) {
        Config.Loaded loaded = Config.switchMode(preset);
        if (loaded.saveStatus() == ConfigStore.SaveStatus.FAILED) {
            source.sendFailure(Component.translatable("text." + HotPot.MOD_ID + ".config.msg.save_failed", loaded.reason()));
            return 0;
        }
        HotPotNetwork.broadcast(source.getServer());
        Component modeName = Component.translatable("text." + HotPot.MOD_ID + ".config.mode." + Config.modePreset());
        source.sendSuccess(() -> Component.translatable(
            HotPotBlockEntity.key("msg.mode_switched"), modeName, loaded.warnings().size()), true);
        for (var warning : loaded.warnings()) {
            source.sendSuccess(() -> Config.describe(warning), false);
        }
        return 1;
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    public static void onDataReloaded() {
        HotPotCooking.reviseCookTimes();
        if (activeServer != null) {
            HotPotNetwork.broadcastTracks(activeServer);
        }
    }

    private static int showMinigameStatus(CommandSourceStack source) {
        var player = source.getPlayer();
        source.sendSuccess(() -> Component.literal("[minigame] " + HotPotMinigames.statusSummary(player)), false);
        return 1;
    }

    private static int listTracks(CommandSourceStack source) {
        var list = HotPotTracks.all(false);
        String summary = list.stream().map(t -> t.id() + "(" + t.bpm() + "bpm," + t.notes().size() + "n)")
            .collect(java.util.stream.Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("[hotpot_tracks] count=" + list.size() + " | " + summary), false);
        return list.size();
    }

    private static int respondInvite(CommandSourceStack source, boolean accept) {
        var player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        boolean ok = HotPotMinigames.respondPendingInvite(player, accept);
        source.sendSuccess(() -> Component.literal("[minigame] " + (ok ? (accept ? "accepted" : "declined") : "no_invite")), false);
        return ok ? 1 : 0;
    }

    private static int runMinigame(CommandSourceStack source, String modeStr, String foodStr, String trackStr, boolean invite) {
        var player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        HotPotMinigameLogic.Mode mode = HotPotMinigameLogic.Mode.fromId(modeStr);
        boolean virtual = "virtual".equalsIgnoreCase(foodStr) || "v".equalsIgnoreCase(foodStr) || "true".equalsIgnoreCase(foodStr);
        ResourceLocation trackId = trackStr == null || trackStr.isBlank()
            ? HotPotTracks.DEFAULT_TRACK_ID
            : ResourceLocation.tryParse(trackStr.contains(":") ? trackStr : HotPot.MOD_ID + ":" + trackStr);
        return HotPotMinigames.startCommandChallenge(player, null, mode, virtual, trackId, invite) ? 1 : 0;
    }
}
