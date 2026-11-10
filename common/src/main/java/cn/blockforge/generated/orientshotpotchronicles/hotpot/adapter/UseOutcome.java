// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

/**
 * 右键交互的「意图」：业务代码只描述这一下算怎么回事，由 {@link Use} 换成各版本自己的返回值。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。1.21.1 的映射见 {@link Use}；以后移植到
 * 1.20.1 / 26.x 时只重写 {@link Use}，业务文件原样同步。</p>
 *
 * <p>为什么不是调研草稿里的 6 种：1.21.1 的 {@code ItemInteractionResult.CONSUME} 和
 * {@code SKIP_DEFAULT_BLOCK_INTERACTION} 行为并不相同（已读原版字节码核实）。
 * {@code CONSUME.consumesAction()} 为真，原版 {@code ServerPlayerGameMode.useItemOn} 直接返回，
 * 不挥手，但仍算「用到了物品」；{@code SKIP_DEFAULT_BLOCK_INTERACTION.consumesAction()} 为假，
 * 原版只跳过「空手再试」这一步，接着仍会调手里物品的 {@code useOn}。两者必须分开，所以这里是 7 种。</p>
 */
public enum UseOutcome {
    /** 成功，按「用到了手里的物品」算（客户端挥手、服务端计统计）。 */
    SUCCESS,
    /** 分端成功：客户端 SUCCESS、服务端 CONSUME；两端均算使用物品，不负责替换手持栈。 */
    SUCCESS_SOFT,
    /** 吃掉这一下：算用到了物品、不挥手、不再走空手、也不再调手里物品。 */
    CONSUME,
    /** 跳过「空手再试」这一步，但仍让手里物品照常处理（原版 SKIP_DEFAULT_BLOCK_INTERACTION）。 */
    SKIPPED,
    /** 放行给「空手再试」：这不是默认值，1.21.1 的默认就是它，26.x 的默认是 TRY_WITH_EMPTY_HAND。 */
    PASS_TO_EMPTY,
    /** 失败：不挥手、不改手持物品。 */
    FAIL,
    /** 完全交回原版，什么都不做。注意 26.x 的 PASS 语义等于这里的 SKIPPED，不是 PASS_TO_EMPTY。 */
    PASS
}
