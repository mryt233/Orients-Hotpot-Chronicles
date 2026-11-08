package me.shedaniel.rei.api.common.entry;

import me.shedaniel.rei.api.client.gui.Renderer;

/**
 * 一个展示用的条目（物品、标签、自定义类型都包装成它）。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface EntryStack<T> extends Renderer {
    /** 条目本身的值（物品条目就是 ItemStack）。 */
    T getValue();

    EntryStack<T> copy();
}
