package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.LootEventBridge;
import cn.blockforge.orient.framework.loader.MenuOpener;
import cn.blockforge.orient.framework.loader.MenuProviderLike;
import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.Platform;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * NeoForge 框架 Runtime 的平台实现：注册队列 / 网络窗口 / 菜单 / 战利品 / 配置目录 / 环境查询。
 * 唯一入口 {@link NeoForgeFrameworkRuntime}；安装一次，内容模块只通过 Framework API 使用。
 * 不含任何内容业务。作者：mryt233
 */
public final class NeoForgeFrameworkPlatform implements Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger("orient-framework");
    private static NeoForgeFrameworkPlatform installed;

    private final NeoForgeRegistrationApi registration = new NeoForgeRegistrationApi();
    private final NeoForgeNetworkApi network = new NeoForgeNetworkApi();

    private NeoForgeFrameworkPlatform() {
    }

    /** 幂等安装；框架 Runtime 与内容侧回退共用。 */
    public static void install(IEventBus modBus) {
        synchronized (NeoForgeFrameworkPlatform.class) {
            if (installed != null) {
                return;
            }
            NeoForgeFrameworkPlatform platform = new NeoForgeFrameworkPlatform();
            RuntimeStats.get().setProcess(FMLEnvironment.dist.isClient() ? "client" : "server");
            RuntimeStats.get().setSink(LOGGER::info);
            if (!Framework.isInstalled()) {
                Framework.install(platform);
            }
            installed = platform;
            RuntimeStats.get().runtimeInit();
            modBus.addListener(platform.registration::onRegisterEvent);
            modBus.addListener(platform.network::onRegisterPayloads);
            if (FMLEnvironment.dist.isClient()) {
                NeoForgeClientEvents.installLifecycle();
            }
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> RuntimeStats.get().logSummary("server-started"));
            NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> RuntimeStats.get().logSummary("server-stopped"));
            LOGGER.info("[ORIENT-FW] init process={} platform={} count={} installs={}",
                RuntimeStats.get().process(), platform.getClass().getSimpleName(),
                RuntimeStats.get().initCount(), RuntimeStats.get().installCount());
        }
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
        return (player, menu, pos) -> player.openMenu(new Provider(menu), pos);
    }

    @Override
    public LootEventBridge lootEvents() {
        return Framework.lootEvents();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    private record Provider(MenuProviderLike menu) implements MenuProvider {
        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
            return menu.createMenu(syncId, inventory, player);
        }

        @Override
        public Component getDisplayName() {
            return menu.title();
        }
    }
}
