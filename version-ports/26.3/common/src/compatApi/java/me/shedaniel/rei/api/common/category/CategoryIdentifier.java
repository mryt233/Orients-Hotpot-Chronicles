package me.shedaniel.rei.api.common.category;

import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.resources.Identifier;

/**
 * 配方类别的标识；和 JEI 的 RecipeType 一一对应。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * mryt233 改（2026-11-22）：对齐 REI 26.2.820 接口形态（interface + static of），避免生成 Methodref 触发 IncompatibleClassChangeError。
 * 作者：mryt233 */

public interface CategoryIdentifier<D extends Display> {
    static <D extends Display> CategoryIdentifier<D> of(Identifier id) {
        return null;
    }

    static <D extends Display> CategoryIdentifier<D> of(String modId, String path) {
        return null;
    }

    Identifier getIdentifier();
}
