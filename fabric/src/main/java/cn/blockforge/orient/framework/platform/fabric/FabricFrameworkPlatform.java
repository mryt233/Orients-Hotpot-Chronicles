package cn.blockforge.orient.framework.platform.fabric;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.loader.RuntimeEvents;

// mryt233 改（2026-11-10）：第三批 3C 平台接线。

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.LootEventBridge;
import cn.blockforge.orient.framework.loader.MenuOpener;
import cn.blockforge.orient.framework.loader.MenuProviderLike;
import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.Platform;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Fabric 框架 Runtime 的平台实现：注册 / 网络 / 菜单 / 战利品 / 配置目录 / 环境查询。
 * 唯一入口 {@link FabricFrameworkRuntime}；安装一次，内容模块只通过 Framework API 使用。
 * 不含任何内容业务。作者：mryt233
 */
public final class FabricFrameworkPlatform implements Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger("orient-framework");
    private static FabricFrameworkPlatform installed;

    private final FabricRuntimeEvents events = new FabricRuntimeEvents();

    private final FabricRegistrationApi registration = new FabricRegistrationApi();
    private final FabricNetworkApi network = new FabricNetworkApi();

    private FabricFrameworkPlatform() {
    }

    /** 幂等安装；框架 Runtime 与内容侧回退共用。 */
    public static void install() {
        synchronized (FabricFrameworkPlatform.class) {
            if (installed != null) {
                return;
            }
            FabricFrameworkPlatform platform = new FabricFrameworkPlatform();
            boolean isClient = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
            RuntimeStats.get().setProcess(isClient ? "client" : "server");
            RuntimeStats.get().setSink(LOGGER::info);
            if (!Framework.isInstalled()) {
                Framework.install(platform);
            }
            installed = platform;
            FabricLootEvents.install();
            ServerLifecycleEvents.SERVER_STARTING.register(server -> platform.registration.closeRegistration());
            RuntimeStats.get().runtimeInit();
            if (isClient) {
                FabricClientNetworkHandlers.installLifecycle();
            }
            LOGGER.info("[ORIENT-FW] init process={} platform={} count={} installs={}",
                RuntimeStats.get().process(), platform.getClass().getSimpleName(),
                RuntimeStats.get().initCount(), RuntimeStats.get().installCount());
            ServerLifecycleEvents.SERVER_STARTED.register(server -> RuntimeStats.get().logSummary("server-started"));
            ServerLifecycleEvents.SERVER_STOPPED.register(server -> RuntimeStats.get().logSummary("server-stopped"));
        }
    }

    @Override
    public RuntimeEvents events() {
        return events;
    }

    @Override
    public RegistrationApi registration() {
        return registration;
    }

    @Override
    public NetworkApi network() {
        return network;
    }

    @Override
    public MenuOpener menus() {
        return (player, menu, pos) -> player.openMenu(new Factory(menu, pos));
    }

    @Override
    public LootEventBridge lootEvents() {
        return Framework.lootEvents();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    private record Factory(MenuProviderLike menu, BlockPos data) implements ExtendedScreenHandlerFactory<BlockPos> {
        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
            return menu.createMenu(syncId, inventory, player);
        }

        @Override
        public BlockPos getScreenOpeningData(ServerPlayer player) {
            return data;
        }

        @Override
        public Component getDisplayName() {
            return menu.title();
        }
    }
    // mryt233 改（2026-11-10）：配置初始化需要的环境信息。
    @Override
    public boolean isDedicatedServer() { return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER; }

}
