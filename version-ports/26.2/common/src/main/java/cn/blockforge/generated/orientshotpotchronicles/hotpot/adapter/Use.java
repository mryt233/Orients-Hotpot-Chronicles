// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * 右键交互的版本适配：把 {@link UseOutcome} 换成 26.2 原版的 {@link InteractionResult}。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层（26.2 合并使用 {@code InteractionResult}）。</p>
 */
public final class Use {
    private Use() {
    }

    /** 方块 {@code useItemOn}：带手持物品点击方块。 */
    public static InteractionResult blockItem(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            case CONSUME -> InteractionResult.CONSUME;
            case SKIPPED, PASS -> InteractionResult.PASS;
            case PASS_TO_EMPTY -> InteractionResult.TRY_WITH_EMPTY_HAND;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** 方块 {@code useWithoutItem}：空手点击方块。 */
    public static InteractionResult blockEmpty(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            case CONSUME, SKIPPED -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** 物品 {@code useOn} / {@code interactLivingEntity}：对方块或实体使用物品。 */
    public static InteractionResult item(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            case CONSUME -> InteractionResult.CONSUME;
            case SKIPPED, PASS, PASS_TO_EMPTY -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** 物品 {@code use}：对空气使用物品（26.2 直接返回 {@code InteractionResult}）。 */
    public static InteractionResult itemUse(UseOutcome outcome, ItemStack held, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS.heldItemTransformedTo(held);
            case SUCCESS_SOFT -> (client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)
                .heldItemTransformedTo(held);
            case CONSUME -> InteractionResult.CONSUME;
            case SKIPPED, PASS, PASS_TO_EMPTY -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }
}
