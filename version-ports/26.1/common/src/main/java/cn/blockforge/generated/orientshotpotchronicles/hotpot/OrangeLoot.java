// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 橙子已迁至独立橙子树（{@link OrangeLeavesBlock} / {@link OrangeSaplingBlock}），
 * 橡木与深色橡木树叶不再注入额外橙子掉落。
 * 作者：mryt233
 */
public final class OrangeLoot {
    private OrangeLoot() {
    }

    public static void register() {
        // 保留公开入口作兼容性占位；独立橙子树通过自身方块战利品表与右键采摘提供橙子。
    }
}
