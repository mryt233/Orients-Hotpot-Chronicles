package me.shedaniel.rei.api.common.display;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import java.util.List;

/**
 * 一条配方在 REI 里的展示数据：输入、输出与所属类别。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface Display {
    List<EntryIngredient> getInputEntries();

    List<EntryIngredient> getOutputEntries();

    CategoryIdentifier<?> getCategoryIdentifier();
}
