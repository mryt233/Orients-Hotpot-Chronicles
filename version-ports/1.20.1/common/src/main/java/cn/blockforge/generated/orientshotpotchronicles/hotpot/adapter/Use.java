// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * 右键交互的版本适配：把 {@link UseOutcome} 换成 1.20.1 垫片与原版的返回值。
 *
 * <p>mryt233 改（1.20.1 适配）：方块「拿着东西右键」返回 {@link ItemInteractionResult}（垫片），
 * 由 {@code ItemInteractionBlock#dispatchUse} 负责分派到空手侧与物品侧。</p>
 */
public final class Use {
    private Use() {
    }

    /** 方块「拿着东西右键」：{@code cn.blockforge.orient.framework.compat.ItemInteractionResult}。 */
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

    /** 方块「空手右键」：{@code InteractionResult}。 */
    public static InteractionResult blockEmpty(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> InteractionResult.sidedSuccess(client);
            case CONSUME, SKIPPED -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** 物品侧（{@code useOn} / {@code interactLivingEntity} / 跨方块转发）：{@code InteractionResult}。 */
    public static InteractionResult item(UseOutcome outcome, boolean client) {
        return switch (outcome) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_SOFT -> InteractionResult.sidedSuccess(client);
            case CONSUME -> InteractionResult.CONSUME;
            case PASS_TO_EMPTY, PASS, SKIPPED -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /** 物品「对空气右键」（{@code Item.use}）：{@code InteractionResultHolder<ItemStack>}。 */
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
