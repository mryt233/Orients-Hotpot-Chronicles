package mezz.jei.api.gui.drawable;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 可绘制对象：图标、背景这类小图。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code mezz/jei} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface IDrawable {
    int getWidth();

    int getHeight();

    default void draw(GuiGraphics guiGraphics) {
    }

    void draw(GuiGraphics guiGraphics, int xOffset, int yOffset);
}
