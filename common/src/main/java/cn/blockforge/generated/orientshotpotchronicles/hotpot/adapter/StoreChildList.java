// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

/**
 * 子节点列表的写入口（{@link StoreWriter#childList(String)} 返回它）。
 * 26.x 对应 {@code ValueOutput.childrenList(key)} 的 {@code addChild()}。
 */
public interface StoreChildList {
    /** 加一条，返回写这一条用的 writer。 */
    StoreWriter addChild();
}
