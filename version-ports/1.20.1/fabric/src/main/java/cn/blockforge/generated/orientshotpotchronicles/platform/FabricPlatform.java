package cn.blockforge.generated.orientshotpotchronicles.platform;


import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.platform.fabric.FabricFrameworkPlatform;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Fabric 内容侧平台接线：可燃表 / 别名 / 方块破坏拦截等火锅业务。
 *
 * <p>通用平台能力（注册 / 网络 / 菜单 / 战利品 / 配置目录）由框架 Runtime
 * {@link FabricFrameworkPlatform} 提供；这里只在没有 Runtime 的开发环境回退安装，
 * 内容模块本身只通过 Framework API 使用平台能力。作者：mryt233</p>
 */
public final class FabricPlatform {
    private FabricPlatform() {
    }

    /** 框架 Runtime 已安装时什么都不做；只有单模组开发环境才回退安装通用平台。 */
    public static void install() {
        if (Framework.isInstalled()) {
            return;
        }
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // 生产环境必须由 orient_framework Runtime 提供平台；元数据已强制依赖，这里只做快速失败
            throw new IllegalStateException("orient_framework runtime is required in production");
        }
        FabricFrameworkPlatform.install();
    }

    /** 内容注册完成后的平台侧接线（loot/别名/可燃表/破坏拦截）。作者：mryt233 */
    public static void afterContentRegistration() {
        // 平台侧的收尾接线（从核心移过来）
        LootListeners.install();
        ShrimpLoot.register();   // 钓鱼出虾用 Fabric 注入的 modifyPools，放在平台
        // mryt233 改（2026-11-02）：1.20.1 不做 hotpot_table → oak_hotpot_table 旧别名。
        // 这个旧 ID 只在 1.21.1 很早的开发版存档里有，1.20.1 移植版从 0.1.1 起就没有它（旧移植版也已去掉）；
        // 而 FabricRegistry.addAlias 在 Fabric API 0.92.8 才有，留着会把最低要求抬到 API 0.92.8 + Loader 0.16.10。
        // 调整：三个包统一由入口的 FabricNetwork 注册。
        // 下界木（绯红、诡异）不可燃，其余和原版木板一样会着火
        for (int i = 0; i < HotPotContent.TABLE_WOODS.size(); i++) {
            String wood = HotPotContent.TABLE_WOODS.get(i);
            if (wood.equals("crimson") || wood.equals("warped")) {
                continue;
            }
            FlammableBlockRegistry.getDefaultInstance().add(HotPotContent.HOTPOT_TABLES.get(i), 5, 20);
            FlammableBlockRegistry.getDefaultInstance().add(HotPotContent.BENCHES.get(i), 5, 20);
            FlammableBlockRegistry.getDefaultInstance().add(HotPotContent.TALL_BENCHES.get(i), 5, 20);
            // 高火锅桌以前漏了，不会着火（2026-10-23，用户要求修）
            FlammableBlockRegistry.getDefaultInstance().add(HotPotContent.TALL_TABLES.get(i), 5, 20);
            // 木头蘸料台（和 TABLE_WOODS 同序，铁的在最后不登记）也会着火（2026-10-23）
            FlammableBlockRegistry.getDefaultInstance().add(HotPotContent.SAUCE_STATIONS.get(i), 5, 20);
        }
        // 砧板（橡木）也会着火，数值同桌子（2026-10-23，用户要求）
        FlammableBlockRegistry.getDefaultInstance().add(HotPotContent.CUTTING_BOARD, 5, 20);

        // 灶上有锅时，对着锅挖只挖下锅（连汤带菜打包掉落），灶台留着；对着灶身挖才挖整个灶台
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
            !cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBreaking.redirect(world, player, pos, state, blockEntity));
    }
}
