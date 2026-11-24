package me.shedaniel.rei.api.client.registry.display;

import me.shedaniel.rei.api.common.display.Display;

/**
 * 配方注册表：把配方展示数据交给 REI。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface DisplayRegistry {
    void add(Display display);
}
