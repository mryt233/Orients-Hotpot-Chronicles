// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.compat.CompatRegistry;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.network.HotPotNetwork;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RuntimeHooks;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** 配置、命令、网络、可燃及运行期清理，两个加载器共用。 */
public final class HotPotRuntime {
    private static boolean compatInitialized;
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
        Config.rememberGarnishLoot("scallion", 0.25F);
        Config.rememberGarnishLoot("cilantro", 0.25F);
        Config.rememberGarnishLoot("soybean", 0.15F);
        Config.rememberGarnishLoot("garlic", 0.15F);
        Config.rememberGarnishLoot("tomato", 0.15F);
        Config.rememberGarnishLoot("chili_pepper", 0.15F);
        Config.rememberGarnishLoot("peanut", 0.15F);
        Config.rememberGarnishLoot("greens", 0.15F);
        Config.rememberGarnishLoot("zhe_er_gen", 0.15F);
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
            Config.setServer(server);
            HotPotRecipes.setServer(server);
            HotPotHooks.freezeRegistries();
        });
        events.onPlayerJoin(HotPotNetwork::sendConfig);
        events.onPlayerLeave(player -> {
            ClothApronProtection.removePlayer(player.getUUID());
            CuttingBoardBlock.clearCache();
        });
        events.onServerStopping(server -> {
            ClothApronProtection.clearCache();
            CuttingBoardBlock.clearCache();
        });
        events.onServerStopped(server -> { Config.setServer(null); HotPotRecipes.setServer(null); });
        events.onCommands((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal("orientshotpot").then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2)).executes(command -> {
                    var result = Config.reload();
                    HotPotNetwork.broadcast(command.getSource().getServer());
                    command.getSource().sendSuccess(() -> Component.translatable(
                        HotPotBlockEntity.key("msg.config_reloaded"), result.warnings().size()), true);
                    for (var warning : result.warnings()) {
                        command.getSource().sendSuccess(() -> Config.describe(warning), false);
                    }
                    return 1;
                }))));
        for (int i = 0; i < HotPotContent.TABLE_WOODS.size(); i++) {
            String wood = HotPotContent.TABLE_WOODS.get(i);
            if (wood.equals("crimson") || wood.equals("warped")) continue;
            events.flammable(HotPotContent.HOTPOT_TABLES.get(i), 5, 20);
            events.flammable(HotPotContent.BENCHES.get(i), 5, 20);
            events.flammable(HotPotContent.TALL_BENCHES.get(i), 5, 20);
            events.flammable(HotPotContent.TALL_TABLES.get(i), 5, 20);
            events.flammable(HotPotContent.SAUCE_STATIONS.get(i), 5, 20);
        }
        events.flammable(HotPotContent.CUTTING_BOARD, 5, 20);
        events.beforeBlockBreak(HotPotBreaking::redirect);
    }
}
