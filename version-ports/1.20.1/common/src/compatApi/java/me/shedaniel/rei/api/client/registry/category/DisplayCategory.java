package me.shedaniel.rei.api.client.registry.category;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 一个配方类别在 REI 里的展示定义：标题、图标、尺寸与部件布局。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface DisplayCategory<T extends Display> {
    CategoryIdentifier<? extends T> getCategoryIdentifier();

    Component getTitle();

    Renderer getIcon();

    default List<Widget> setupDisplay(T display, Rectangle bounds) {
        return List.of();
    }

    default int getDisplayHeight() {
        return 66;
    }

    default int getDisplayWidth(T display) {
        return 150;
    }
}
