package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Orient's Hotpot Chronicles —— NeoForge 1.21.1 入口。
 * <p>
 * 服务端：物品 / 方块 / 方块实体 / 创造栏 / 菜单类型 / 战利品 / 可燃表 / 方块破坏 / 配置与热重载命令；
 * 客户端界面、渲染与按键在 {@link NeoForgeClient}（mryt233 2026-11-01 更新：原注释「客户端还在搬」已过时）。
 *
 * <p><b>为什么内容注册写在 {@link RegisterEvent} 里，而不是构造函数里</b>（核心约定，别顺手改）：
 * <ul>
 *   <li>核心的 {@code HotPotContent} 用原版的 {@code Registry.register(...)} 直接注册（静态块里），
 *       这要求注册表**没被冻结**。</li>
 *   <li>NeoForge 的顺序是：原版 {@code Bootstrap.bootStrap()} 先冻结注册表 → 才构造模组
 *       （见 {@code net.minecraft.server.Main}）→ 然后 {@code GameData.unfreezeData()} 解冻、
 *       逐个注册表发 {@link RegisterEvent}、再 {@code freezeData()} 冻结。</li>
 *   <li>所以在构造函数里摸 {@code HotPotContent}（哪怕只是读一个字段触发类初始化）就会
 *       「Registry is already frozen」直接崩。这里把第一次 RegisterEvent 当开关，
 *       在解冻窗口里才加载并注册核心内容。</li>
 * </ul>
 *
 * 作者：mryt233 */
@Mod("orients_hotpot_chronicles")
public final class HotPotNeoForge {
    private static final cn.blockforge.orient.framework.loader.ModuleHost MODULES =
        cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.HOST;
    /** 核心内容只在第一个 RegisterEvent（NeoForge 保证是 attribute）里注册一次。 */
    private static boolean contentRegistered;

    public HotPotNeoForge(IEventBus modBus, ModContainer container) {
        // 注册表解冻窗口：核心的物品 / 方块 / 方块实体 / 菜单 / 创造栏都在这里落地
        NeoForgePlatform.install(modBus);
        modBus.addListener(this::onRegisterEvent);
        // mryt233 改（2026-11-10）：生产环境跳过；开发探针通过反射接线。
        if (!FMLEnvironment.production && Boolean.getBoolean("hotpot.expreg")) {
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.NeoForgeRegProbe")
                    .getMethod("register", IEventBus.class).invoke(null, modBus);
            } catch (ReflectiveOperationException | LinkageError error) {
                HotPot.LOGGER.warn("registration probe unavailable: {}", error.toString());
            }
        }
        // 通道登记走框架 NetworkApi，Runtime 的 RegisterPayloadHandlersEvent 窗口统一落地
        NeoForgeNetwork.register();
        NeoForge.EVENT_BUS.addListener(NeoForgeNetwork::onLogin);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat.install(
                net.neoforged.fml.ModList.get()::isLoaded);
            cn.blockforge.generated.orientshotpotchronicles.compat.CompatRegistry.init();
        }));

        // 以下都是「游戏事件」，挂在 NeoForge 的游戏总线上，和注册无关
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        // mryt233 改（2026-11-10）：玩家下线释放围裙热源缓存。
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) ->
            cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronProtection.removePlayer(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        // 数据重载：每次 /reload 都通知内容模块（可重复）
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent event) ->
            event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Void>() {
                @Override
                protected Void prepare(net.minecraft.server.packs.resources.ResourceManager manager,
                                       net.minecraft.util.profiling.ProfilerFiller profiler) {
                    return null;
                }

                @Override
                protected void apply(Void unused, net.minecraft.server.packs.resources.ResourceManager manager,
                                     net.minecraft.util.profiling.ProfilerFiller profiler) {
                    MODULES.reload();
                }
            }));
        HotPot.LOGGER.info("[Orient的火锅物语] NeoForge 入口已加载，等注册窗口再注册内容");
    }

    /**
     * 第一个 RegisterEvent 到达时，注册表正好全部处于解冻状态（NeoForge 的
     * {@code GameData.unfreezeData()} → 逐个发 RegisterEvent → {@code freezeData()}）。
     * 这里触发核心的静态注册，和 Fabric 入口在 onInitialize 里的顺序保持一致。
     */
    private void onRegisterEvent(RegisterEvent event) {
        if (contentRegistered) {
            return;
        }
        contentRegistered = true;
        // 先装平台实现，核心注册时才知道创造栏和界面类型怎么建
        MODULES.registerAll();
        MODULES.runtime(new cn.blockforge.orient.framework.loader.RuntimeHooks() {
            @Override
            public void onServerStarted(Runnable callback) {
                NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> callback.run());
            }
        });
        NeoForgePlatform.afterContentRegistration();
        // 燃料和蘸料这两个枚举平时到用时才加载，这里先碰一下，好让它们的默认值也写进配置文件
        HotPotFuel.values();
        Seasoning.values();
        Config.rememberCooking(
            HotPotCooking.DEFAULT_FALLBACK_TICKS,
            HotPotCooking.DEFAULT_FALLBACK_XP,
            HotPotCooking.DEFAULT_XP_PER_ITEM);
        Config.init(FMLPaths.CONFIGDIR.get(), FMLEnvironment.dist.isDedicatedServer());
        // 注册时把默认数值都记下来了，这里补全成一份可编辑的配置文件
        Config.saveDefaultsIfNeeded();
    }

    static void clientHooks(cn.blockforge.orient.framework.loader.ClientHooks hooks) {
        MODULES.client(hooks);
    }

    private void onServerStarting(ServerStartingEvent event) {
        Config.setServer(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        Config.setServer(null);
    }

    /** 数值热重载命令（和 Fabric 版同一条）：/orientshotpot reload，管理员权限。 */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        if (!FMLEnvironment.production) {
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.NeoForgeDevTests")
                    .getMethod("register", RegisterCommandsEvent.class).invoke(null, event);
            } catch (ClassNotFoundException e) {
                // 生产候选 jar 不带 devtest，在开发启动器里加载时安静跳过即可
                HotPot.LOGGER.info("dev test commands not packaged in this jar");
            } catch (ReflectiveOperationException e) {
                HotPot.LOGGER.warn("dev test commands unavailable", e);
            }
        }
        event.getDispatcher().register(
            Commands.literal("orientshotpot")
                .then(Commands.literal("reload")
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> {
                        Config.Loaded result = Config.reload();
                        // 调整：重载后同步所有在线客户端，不必重新进服。
                        NeoForgeNetwork.broadcast(ctx.getSource().getServer());
                        // 回执跟随玩家语言（以前写死中文，代码检查 W6）
                        ctx.getSource().sendSuccess(() -> Component.translatable(
                            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.key("msg.config_reloaded"),
                            result.warnings().size()), true);
                        // 每条警告也跟随玩家语言（前置检查 W5，mryt233 改 2026-11-01）
                        for (var warning : result.warnings()) {
                            ctx.getSource().sendSuccess(() -> Config.describe(warning), false);
                        }
                        return 1;
                    })));
    }
}
