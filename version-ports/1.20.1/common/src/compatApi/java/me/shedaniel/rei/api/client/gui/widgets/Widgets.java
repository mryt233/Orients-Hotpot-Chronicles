package me.shedaniel.rei.api.client.gui.widgets;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.DrawableConsumer;
import net.minecraft.network.chat.Component;

/**
 * 界面部件工厂：底框、槽位、文字、箭头、自由绘制。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public final class Widgets {
    private Widgets() {
    }

    public static Widget createRecipeBase(Rectangle bounds) {
        return null;
    }

    public static Slot createSlot(Point point) {
        return null;
    }

    public static Label createLabel(Point point, Component text) {
        return null;
    }

    public static Arrow createArrow(Point point) {
        return null;
    }

    public static Widget createDrawableWidget(DrawableConsumer drawable) {
        return null;
    }
}
