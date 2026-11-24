// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 橙子已迁至独立橙子树（{@link OrangeLeavesBlock} / {@link OrangeSaplingBlock}），
 * 自 0.3.1 起不再向原版橡木 / 深色橡木树叶注入橙子掉落，原版树叶仅保留原本苹果与木棍/树苗掉落。
 */
public final class OrangeLoot {
    private OrangeLoot() {
    }

    public static void register() {
        // 保留公开入口作兼容性占位；独立橙子树通过自身方块战利品表与右键采摘提供橙子。
    }
}
