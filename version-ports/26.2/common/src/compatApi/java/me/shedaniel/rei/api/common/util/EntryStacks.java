package me.shedaniel.rei.api.common.util;

import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 把原版物品包装成 REI 条目。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public final class EntryStacks {
    private EntryStacks() {
    }

    public static EntryStack<ItemStack> of(ItemStack stack) {
        return null;
    }

    public static EntryStack<ItemStack> of(ItemLike item) {
        return null;
    }
}
