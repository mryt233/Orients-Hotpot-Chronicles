// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * 右键交互的版本适配：把 {@link UseOutcome} 换成 1.21.1 原版的返回值。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。业务代码里的每个结果都交给这里换，
 * 换回来的原版常量和改造前逐条相同（3G 报告里有逐条对照表）。</p>
 *
 * <p>移植时只重写这个类：1.20.1 只有 {@code BlockState.use}，要自己分发；26.x 没有
 * {@code ItemInteractionResult} 与 {@code InteractionResultHolder}，且：
 * <ul>
 *   <li>{@code PASS_TO_EMPTY} 在 26.x 是 {@code InteractionResult.TRY_WITH_EMPTY_HAND}，<b>不是</b>
 *       {@code PASS}——26.x 的 {@code PASS} 不触发空手逻辑，写成 PASS 就会出现「拿着东西右键打不开锅 / 灶台」
 *       「筷子夹着菜吃不了」；</li>
 *   <li>{@code SKIPPED} 才对应 26.x 的 {@code InteractionResult.PASS}；</li>
 *   <li>{@code SUCCESS_SOFT} 就是原版 sidedSuccess，不是 SUCCESS_NO_ITEM_USED；26.x 的挥手来源
 *       对照来自调研，尚未在本轮复核，见 {@code docs/版本适配小包说明.md}。</li>
 * </ul></p>
 *
 * <p>默认值不要从这里取：1.21.1 的 {@code useItemOn} 默认是 {@code PASS_TO_DEFAULT_BLOCK_INTERACTION}，
 * 26.x 是 {@code TRY_WITH_EMPTY_HAND}，1.20.1 的 {@code use} 默认是 {@code PASS}。所以枚举里没有
 * 「默认意图」，每个入口都显式写出来；{@code 3F} 的 {@code SyncedBlockEntity} 也不替谁决定。</p>
 */
public final class Use {
    private Use() {
    }

    /**
     * 方块「拿着东西右键」：{@code ItemInteractionResult}。
     *
     * <p>这一侧没有 {@link UseOutcome#PASS}：1.21.1 的 {@code ItemInteractionResult} 只有
     * {@code PASS_TO_DEFAULT_BLOCK_INTERACTION}，硬塞 PASS 是笔误，直接抛异常暴露出来。</p>
     */
    public static ItemInteractionResult blockItem(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case SUCCESS_SOFT -> ItemInteractionResult.sidedSuccess(client);
            case CONSUME -> ItemInteractionResult.CONSUME;
            case SKIPPED -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            case PASS_TO_EMPTY -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> throw new IllegalArgumentException("方块「拿着东西」侧没有 PASS，要用 PASS_TO_EMPTY：" + outcome);
        };
    }

    /**
     * 方块「空手右键」：{@code InteractionResult}。
     * {@link UseOutcome#SUCCESS_SOFT} 同样按 sidedSuccess 分端；没有「跳过空手」这一档，
     * {@link UseOutcome#SKIPPED} 在这里定义为「吃掉这一下」（当前业务未使用）。
     */
    public static InteractionResult blockEmpty(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> InteractionResult.sidedSuccess(client);
            case CONSUME, SKIPPED -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /**
     * 物品侧（{@code useOn} / {@code interactLivingEntity} / 跨方块转发）：{@code InteractionResult}。
     * 物品侧没有「跳过空手」这一档，{@link UseOutcome#SKIPPED} 就是「放行」。
     */
    public static InteractionResult item(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> InteractionResult.sidedSuccess(client);
            case CONSUME -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS, SKIPPED -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /**
     * 物品「对空气右键」（{@code Item.use}）：{@code InteractionResultHolder<ItemStack>}。
     * 第二个参数是这一下之后手里的物品（没换就是原栈）；换手持物只换这个参数，
     * 26.x 对应 {@code Success.heldItemTransformedTo(stack)}。
     */
    public static InteractionResultHolder<ItemStack> itemUse(UseOutcome outcome, ItemStack held, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResultHolder.success(held);
            case SUCCESS_SOFT -> InteractionResultHolder.sidedSuccess(held, client);
            case CONSUME -> InteractionResultHolder.consume(held);
            case PASS_TO_EMPTY, PASS, SKIPPED -> InteractionResultHolder.pass(held);
            case FAIL -> InteractionResultHolder.fail(held);
        };
    }
}
