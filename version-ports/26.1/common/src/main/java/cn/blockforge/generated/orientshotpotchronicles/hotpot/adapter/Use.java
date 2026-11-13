// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * 右键交互的 26.1 版本适配：把 {@link UseOutcome} 换成 26.1 原版的 {@link InteractionResult}。
 *
 * <p>mryt233 改（2026-11-13）：26.1 无 {@code ItemInteractionResult} 与
 * {@code InteractionResultHolder}，手持物品右键方块侧 {@code PASS_TO_EMPTY} 对应
 * {@link InteractionResult#TRY_WITH_EMPTY_HAND}，{@code SKIPPED} 对应
 * {@link InteractionResult#PASS}。</p>
 */
public final class Use {
    private Use() {
    }

    /**
     * 方块「拿着东西右键」：{@code InteractionResult}。
     * 这一侧没有 {@link UseOutcome#PASS}，要放行给空手必须传 {@link UseOutcome#PASS_TO_EMPTY}。
     */
    public static InteractionResult blockItem(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> client ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            case CONSUME -> InteractionResult.CONSUME;
            case SKIPPED -> InteractionResult.PASS;
            case PASS_TO_EMPTY -> InteractionResult.TRY_WITH_EMPTY_HAND;
            case FAIL -> InteractionResult.FAIL;
            case PASS -> throw new IllegalArgumentException("方块「拿着东西」侧没有 PASS，要用 PASS_TO_EMPTY：" + outcome);
        };
    }

    /** 方块「空手右键」：{@code InteractionResult}。 */
    public static InteractionResult blockEmpty(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> client ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            case CONSUME, SKIPPED -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** 物品侧（{@code useOn} / {@code interactLivingEntity} / 跨方块转发）：{@code InteractionResult}。 */
    public static InteractionResult item(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> client ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            case CONSUME -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS, SKIPPED -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /**
     * 物品「对空气右键」（{@code Item.use}）：{@code InteractionResult}。
     * 成功或消耗时通过 {@code heldItemTransformedTo(held)} 带回手上的物品栈。
     */
    public static InteractionResult itemUse(UseOutcome outcome, ItemStack held, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS.heldItemTransformedTo(held);
            case SUCCESS_SOFT -> (client ? InteractionResult.SUCCESS : InteractionResult.CONSUME).heldItemTransformedTo(held);
            case CONSUME -> InteractionResult.CONSUME.heldItemTransformedTo(held);
            case PASS_TO_EMPTY, PASS, SKIPPED -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }
}
