package cn.blockforge.generated.orientshotpotchronicles.forge;


import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.platform.forge.ForgeFrameworkPlatform;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Forge 内容侧平台接线：战利品 / 可燃表 / 方块破坏拦截等火锅业务。
 *
 * <p>通用平台能力（注册队列 / 网络窗口 / 菜单 / 战利品 / 配置目录）由框架 Runtime
 * {@link ForgeFrameworkPlatform} 提供；这里只在没有 Runtime 的开发环境回退安装，
 * 内容模块本身只通过 Framework API 使用平台能力。作者：mryt233</p>
 */
public final class ForgePlatform {
    private ForgePlatform() {
    }

    /** 框架 Runtime 已安装时什么都不做；只有单模组开发环境才回退安装通用平台。 */
    public static void install(IEventBus modBus) {
        if (Framework.isInstalled()) {
            return;
        }
        if (net.minecraftforge.fml.loading.FMLEnvironment.production) {
            // 生产环境必须由 orient_framework Runtime 提供平台；元数据已强制依赖，这里只做快速失败
            throw new IllegalStateException("orient_framework runtime is required in production");
        }
        ForgeFrameworkPlatform.install(modBus);
    }

    /** 内容注册完成后的平台侧接线（loot/可燃表/破坏拦截）。作者：mryt233 */
    public static void afterContentRegistration() {
        ForgeLootListeners.install();

        // 灶上有锅时，对着锅挖只挖下锅（连汤带菜打包掉落），灶台留着；对着灶身挖才挖整个灶台。
        // Fabric 用 PlayerBlockBreakEvents.BEFORE（返回 false 取消）；Forge 用 BlockEvent.BreakEvent（setCanceled）：
        // 两者都在方块真正被移除之前触发，语义一致
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (event.getLevel() instanceof Level world
                && cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBreaking.redirect(world, event.getPlayer(),
                    event.getPos(), event.getState(), world.getBlockEntity(event.getPos()))) {
                event.setCanceled(true);
            }
        });

        // 旧版「hotpot_table」→「oak_hotpot_table」的 id 迁移：Fabric 版用 Registry.addAlias，
        // 但 Minecraft 1.21.1 的注册表**没有** addAlias（1.21.2 才加），Forge 21.1 也没有替代品，
        // 所以第一版先跳过。老存档里的 hotpot_table 会变成缺失方块——这是 1.21.1 的硬限制
    }
}
