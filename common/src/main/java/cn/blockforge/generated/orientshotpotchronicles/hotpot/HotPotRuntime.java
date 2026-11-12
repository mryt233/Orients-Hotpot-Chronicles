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
        // mryt233 改（2026-11-11，第四批 4B-1）：强制初始化 SoupBase，让六个内置汤底定义在读配置（Config.init）之前
        // 登记进汤底登记表（Soups）并把默认值记进配置模板。以前靠别的类碰巧先加载它。
        SoupBase.values();
        Config.rememberCooking(HotPotCooking.DEFAULT_FALLBACK_TICKS,
            HotPotCooking.DEFAULT_FALLBACK_XP, HotPotCooking.DEFAULT_XP_PER_ITEM);
        var platform = Framework.platform();
        Config.init(platform.configDir(), platform.isDedicatedServer());
        Config.saveDefaultsIfNeeded();
        HotPotNetwork.register();
        var events = hooks.events();
        // 兼容模块按 ID 查询第三方物品：统一等到全部注册完成、冻结之前，只初始化一次。
        events.onServerStarting(server -> {
            if (!compatInitialized) {
                HotPotCompat.install(platform::isModLoaded);
                CompatRegistry.init();
                compatInitialized = true;
            }
            Config.setServer(server);
            HotPotRecipes.setServer(server); // mryt233 改（2026-11-11）
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
