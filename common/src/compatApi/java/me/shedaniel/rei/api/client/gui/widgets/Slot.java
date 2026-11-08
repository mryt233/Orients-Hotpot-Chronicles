package me.shedaniel.rei.api.client.gui.widgets;

import me.shedaniel.rei.api.common.entry.EntryStack;

import java.util.Collection;

/**
 * 配方里的一个槽位：填条目、标输入 / 输出、关掉底框。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface Slot extends Widget {
    Slot entries(Collection<? extends EntryStack<?>> stacks);

    Slot entry(EntryStack<?> stack);

    Slot markInput();

    Slot markOutput();

    Slot disableBackground();
}
