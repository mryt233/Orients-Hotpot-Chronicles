package cn.blockforge.generated.orientshotpotchronicles.neoforge;


import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.platform.neoforge.NeoForgeFrameworkPlatform;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * NeoForge 内容侧平台接线：战利品 / 可燃表 / 方块破坏拦截等火锅业务。
 *
 * <p>通用平台能力（注册队列 / 网络窗口 / 菜单 / 战利品 / 配置目录）由框架 Runtime
 * {@link NeoForgeFrameworkPlatform} 提供；这里只在没有 Runtime 的开发环境回退安装，
 * 内容模块本身只通过 Framework API 使用平台能力。作者：mryt233</p>
 */
public final class NeoForgePlatform {
    private NeoForgePlatform() {
    }

    /** 框架 Runtime 已安装时什么都不做；只有单模组开发环境才回退安装通用平台。 */
    public static void install(IEventBus modBus) {
        if (Framework.isInstalled()) {
            return;
        }
        if (net.neoforged.fml.loading.FMLEnvironment.production) {
            // 生产环境必须由 orient_framework Runtime 提供平台；元数据已强制依赖，这里只做快速失败
            throw new IllegalStateException("orient_framework runtime is required in production");
        }
        NeoForgeFrameworkPlatform.install(modBus);
    }

    /** 内容注册完成后的平台侧接线（loot/可燃表/破坏拦截）。作者：mryt233 */
    public static void afterContentRegistration() {
        NeoForgeLootListeners.install();

        // 下界木（绯红、诡异）不可燃，其余和原版木板一样会着火。
        // NeoForge 1.21.1 没有 FlammableBlockRegistry，直接用原版 FireBlock 的烧燃表
        // （Fabric 版用 FlammableBlockRegistry.getDefaultInstance，效果相同）
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (int i = 0; i < HotPotContent.TABLE_WOODS.size(); i++) {
            String wood = HotPotContent.TABLE_WOODS.get(i);
            if (wood.equals("crimson") || wood.equals("warped")) {
                continue;
            }
            fire.setFlammable(HotPotContent.HOTPOT_TABLES.get(i), 5, 20);
            fire.setFlammable(HotPotContent.BENCHES.get(i), 5, 20);
            fire.setFlammable(HotPotContent.TALL_BENCHES.get(i), 5, 20);
            // 高火锅桌以前漏了，不会着火（2026-10-23，用户要求修）
            fire.setFlammable(HotPotContent.TALL_TABLES.get(i), 5, 20);
            // 木头蘸料台（和 TABLE_WOODS 同序，铁的在最后不登记）也会着火（2026-10-23）
            fire.setFlammable(HotPotContent.SAUCE_STATIONS.get(i), 5, 20);
        }
        // 砧板（橡木）也会着火，数值同桌子（2026-10-23，用户要求）
        fire.setFlammable(HotPotContent.CUTTING_BOARD, 5, 20);

        // 灶上有锅时，对着锅挖只挖下锅（连汤带菜打包掉落），灶台留着；对着灶身挖才挖整个灶台。
        // Fabric 用 PlayerBlockBreakEvents.BEFORE（返回 false 取消）；NeoForge 用 BlockEvent.BreakEvent（setCanceled）：
        // 两者都在方块真正被移除之前触发，语义一致
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (event.getLevel() instanceof Level world
                && cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBreaking.redirect(world, event.getPlayer(),
                    event.getPos(), event.getState(), world.getBlockEntity(event.getPos()))) {
                event.setCanceled(true);
            }
        });

        // 旧版「hotpot_table」→「oak_hotpot_table」的 id 迁移：Fabric 版用 Registry.addAlias，
        // 但 Minecraft 1.21.1 的注册表**没有** addAlias（1.21.2 才加），NeoForge 21.1 也没有替代品，
        // 所以第一版先跳过。老存档里的 hotpot_table 会变成缺失方块——这是 1.21.1 的硬限制
    }
}
