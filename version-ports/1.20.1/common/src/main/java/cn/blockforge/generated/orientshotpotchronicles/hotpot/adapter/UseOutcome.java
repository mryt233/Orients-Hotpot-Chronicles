// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

/**
 * 右键交互的「意图」：业务代码只描述这一下算怎么回事，由 {@link Use} 换成各版本自己的返回值。
 */
public enum UseOutcome {
    /** 成功，按「用到了手里的物品」算（客户端挥手、服务端计统计）。 */
    SUCCESS,
    /** 分端成功：客户端 SUCCESS、服务端 CONSUME；两端均算使用物品，不负责替换手持栈。 */
    SUCCESS_SOFT,
    /** 吃掉这一下：算用到了物品、不挥手、不再走空手、也不再调手里物品。 */
    CONSUME,
    /** 跳过「空手再试」这一步，但仍让手里物品照常处理。 */
    SKIPPED,
    /** 放行给「空手再试」。 */
    PASS_TO_EMPTY,
    /** 失败：不挥手、不改手持物品。 */
    FAIL,
    /** 完全交回原版，什么都不做。 */
    PASS
}
