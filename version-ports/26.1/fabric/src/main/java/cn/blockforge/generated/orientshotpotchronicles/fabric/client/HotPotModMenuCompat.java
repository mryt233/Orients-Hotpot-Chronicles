package cn.blockforge.generated.orientshotpotchronicles.fabric.client;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Fabric 模组菜单的配置按钮入口（mryt233 改：26.1 移植补上 JEI / REI / Jade / Mod Menu 入口）。作者：mryt233 */
public final class HotPotModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HotPotConfigScreen::new;
    }
}
