package me.shedaniel.math;

/**
 * 界面矩形区域：REI 用它给出展示区的绝对坐标。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public class Rectangle {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMinX() {
        return x;
    }

    public int getMinY() {
        return y;
    }

    public int getMaxX() {
        return x + width;
    }

    public int getMaxY() {
        return y + height;
    }

    public int getCenterX() {
        return x + width / 2;
    }

    public int getCenterY() {
        return y + height / 2;
    }
}
