// 作者：mryt233
package me.shedaniel.rei.api.common.entry;

import java.util.List;

/**
 * 一组等价条目（同一槽位里任选其一）。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code me/shedaniel} 目录，运行时用的是玩家真正安装的那份。
 * mryt233 改（2026-11-22）：对齐 REI 26.1.819 接口形态（interface extends List）。
 * 作者：mryt233 */

public interface EntryIngredient extends List<EntryStack<?>> {
}
