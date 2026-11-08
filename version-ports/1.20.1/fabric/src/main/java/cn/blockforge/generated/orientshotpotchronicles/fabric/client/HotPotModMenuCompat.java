package cn.blockforge.generated.orientshotpotchronicles.fabric.client;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Fabric 模组菜单（Mod Menu）的配置按钮入口。作者：mryt233
 * <p>mryt233 改（0.2.2-alpha，清单 #128）：设置入口从火锅指南的书脊齿轮移到模组列表的「配置」按钮；
 * Mod Menu 只是编译期签名桩，没装 Mod Menu 时这个类不会被加载，模组照常运行。
 */
public final class HotPotModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HotPotConfigScreen::new;
    }
}
