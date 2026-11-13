package cn.blockforge.orient.framework.platform.forge;

// mryt233 改（2026-11-10）：第三批 3C 平台接线。

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.LootEventBridge;
import cn.blockforge.orient.framework.loader.MenuOpener;
import cn.blockforge.orient.framework.loader.MenuProviderLike;
import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.Platform;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.loader.RuntimeEvents;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Forge 框架 Runtime 的平台实现：注册队列 / 网络窗口 / 菜单 / 战利品 / 配置目录 / 环境查询。
 * 唯一入口 {@link ForgeFrameworkRuntime}；安装一次，内容模块只通过 Framework API 使用。
 * 不含任何内容业务。作者：mryt233
 */
public final class ForgeFrameworkPlatform implements Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger("orient-framework");
    private static ForgeFrameworkPlatform installed;

    private final ForgeRuntimeEvents events = new ForgeRuntimeEvents();
    private final ForgeRegistrationApi registration = new ForgeRegistrationApi();
    private final ForgeNetworkApi network = new ForgeNetworkApi();

    private ForgeFrameworkPlatform() {
    }

    /** 幂等安装；框架 Runtime 与内容侧回退共用。 */
    public static void install(IEventBus modBus) {
        synchronized (ForgeFrameworkPlatform.class) {
            if (installed != null) {
                return;
            }
            ForgeFrameworkPlatform platform = new ForgeFrameworkPlatform();
            RuntimeStats.get().setProcess(FMLEnvironment.dist.isClient() ? "client" : "server");
            RuntimeStats.get().setSink(LOGGER::info);
            if (!Framework.isInstalled()) {
                Framework.install(platform);
            }
            installed = platform;
            ForgeLootEvents.install();
            cn.blockforge.orient.framework.compat.RecipeRegistrations.install(platform.registration::queueRecipeSerializer);
            RuntimeStats.get().runtimeInit();
            modBus.addListener(platform.registration::onRegisterEvent);
            modBus.addListener((FMLCommonSetupEvent event) -> {
                platform.network.finishRegistration();
                platform.registration.closeNonRegistryWindows();
            });

            if (FMLEnvironment.dist.isClient()) {
                ForgeClientEvents.installLifecycle(modBus);
            }
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> RuntimeStats.get().logSummary("server-started"));
            MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> RuntimeStats.get().logSummary("server-stopped"));
            LOGGER.info("[ORIENT-FW] init process={} platform={} count={} installs={}",
                RuntimeStats.get().process(), platform.getClass().getSimpleName(),
                RuntimeStats.get().initCount(), RuntimeStats.get().installCount());
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
        return (player, menu, pos) -> net.minecraftforge.network.NetworkHooks.openScreen(player, new Provider(menu), pos);
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

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist.isDedicatedServer();
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
