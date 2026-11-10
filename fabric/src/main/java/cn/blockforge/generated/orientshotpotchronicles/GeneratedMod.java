package cn.blockforge.generated.orientshotpotchronicles;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orient's Hotpot Chronicles —— Fabric 1.21.1 可交互火锅模组入口。
 *
 * 核心玩法：砌一座灶台，往灶膛里添柴生火，把铜锅/铁锅坐上灶，
 * 倒汤底后用筷子打开 9x9 的九宫格面板下菜，像熔炉一样等它煮熟，
 * 直接取走带经验收益与汤底风味的熟食。mryt233 改（2026-11-10）。
 */
public final class GeneratedMod implements ModInitializer {
    private static final cn.blockforge.orient.framework.loader.ModuleHost MODULES = HotpotContentModule.HOST;
    public static final String MOD_ID = cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.MOD_ID;
    public static final Logger LOGGER = cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.LOGGER;

    public static ResourceLocation id(String path) {
        return cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.id(path);
    }

    public static void clientHooks(cn.blockforge.orient.framework.loader.ClientHooks hooks) {
        MODULES.client(hooks);
    }

    @Override
    public void onInitialize() {
        // mryt233 改（2026-11-10）：正式包不加载开发探针，缺类也只记录一行。
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()
            && Boolean.getBoolean("hotpot.expreg")) {
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.FabricRegProbe")
                    .getMethod("run").invoke(null);
            } catch (ReflectiveOperationException | LinkageError error) {
                LOGGER.warn("registration probe unavailable: {}", error.toString());
            }
        }
        // 修复：先完整登记默认值，再校验配置；物品注册只使用默认组件。
        // 先装平台实现，核心注册时才知道创造栏和界面类型怎么建
        cn.blockforge.generated.orientshotpotchronicles.platform.FabricPlatform.install();
        MODULES.registerAll();
        // mryt233 改（2026-11-10）：独立探针只在显式开发参数下加载。
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("hotpot.opt3c")) {
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.Opt3cFabricProbe")
                    .getMethod("install").invoke(null);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("3C development probe failed", error);
            }
        }

        MODULES.runtime(new cn.blockforge.orient.framework.loader.RuntimeHooks() {
            @Override
            public void onServerStarted(Runnable callback) {
                net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> callback.run());
            }
        });
        cn.blockforge.generated.orientshotpotchronicles.platform.FabricPlatform.afterContentRegistration();
        // 数据/资源重载：每次 reload 都通知内容模块（可重复）
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
            .get(net.minecraft.server.packs.PackType.SERVER_DATA)
            .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.resources.ResourceLocation getFabricId() {
                    return GeneratedMod.id("content_reload");
                }

                @Override
                public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                    MODULES.reload();
                }
            });
        // 燃料和蘸料这两个枚举平时到用时才加载，这里先碰一下，好让它们的默认值也写进配置文件
        cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel.values();
        cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning.values();
        cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.rememberCooking(
            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking.DEFAULT_FALLBACK_TICKS,
            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking.DEFAULT_FALLBACK_XP,
            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking.DEFAULT_XP_PER_ITEM);
        net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
        cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.init(loader.getConfigDir(),
            loader.getEnvironmentType() == net.fabricmc.api.EnvType.SERVER);
        // 注册时把默认数值都记下来了，这里补全成一份可编辑的配置文件
        cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.saveDefaultsIfNeeded();
        cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat.install(loader::isModLoaded);
        cn.blockforge.generated.orientshotpotchronicles.compat.CompatRegistry.init();
        // 服务端 → 客户端的数值同步（对齐 mod2）
        cn.blockforge.generated.orientshotpotchronicles.platform.FabricNetwork.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(
            cn.blockforge.generated.orientshotpotchronicles.hotpot.Config::setServer);
        // mryt233 改（2026-11-10，r147）：登记表在服务端开始启动时冻结（单人和专用服都覆盖）。
        // 这个时机晚于兼容模块（Fabric 在 onInitialize 末尾跑 CompatRegistry.init）。
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(
            server -> cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHooks.freezeRegistries());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
            s -> cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.setServer(null));
        // mryt233 改（2026-11-10）：玩家下线释放围裙热源缓存。
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronProtection.removePlayer(handler.player.getUUID()));
        // 数值热重载命令：食物营养、汤底、燃料、蘸料、烹饪兜底都立即生效（食物走 ConfiguredFoodItem，吃的时候读配置）
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
            dispatcher.register(net.minecraft.commands.Commands.literal("orientshotpot")
                .then(net.minecraft.commands.Commands.literal("reload")
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> {
                        var result = cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.reload();
                        cn.blockforge.generated.orientshotpotchronicles.platform.FabricNetwork
                            .broadcast(ctx.getSource().getServer());
                        // 回执跟随玩家语言（以前写死中文，代码检查 W6）
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable(
                            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.key("msg.config_reloaded"),
                            result.warnings().size()), true);
                        // 每条警告也跟随玩家语言（前置检查 W5，mryt233 改 2026-11-01）
                        for (var warning : result.warnings()) {
                            ctx.getSource().sendSuccess(() ->
                                cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.describe(warning), false);
                        }
                        return 1;
                    }))));
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // 开发环境才有的测试命令 /hpt（devtest 包不进 jar，所以用反射调；新增 2026-10-25）
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.FabricDevTests")
                    .getMethod("register").invoke(null);
            } catch (ClassNotFoundException e) {
                // 生产候选 jar 不带 devtest，在开发启动器里加载时安静跳过即可
                LOGGER.info("dev test commands not packaged in this jar");
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("dev test commands unavailable", e);
            }
        }
    }
}
