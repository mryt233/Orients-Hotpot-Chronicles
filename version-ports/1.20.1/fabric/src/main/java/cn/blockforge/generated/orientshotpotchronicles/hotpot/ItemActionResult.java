// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * 1.20.x 移植版专用：照 1.21 原版 {@code net.minecraft.util.ItemActionResult} 写的一份。1.21 把方块右键拆成
 * 「拿着东西点」（onUseWithItem）和「空手点」（onUse）两步，1.20 只有一个 onUse；方块类里保留 1.21 的两个方法
 * （改名 useWithItem / useWithoutItem），由 {@link #dispatch} 按 1.21 的顺序调用，行为和 1.21 版一致。
 * 作者：Claude
 */
public enum ItemActionResult {
    SUCCESS,
    CONSUME,
    CONSUME_PARTIAL,
    PASS_TO_DEFAULT_BLOCK_INTERACTION,
    SKIP_DEFAULT_BLOCK_INTERACTION,
    FAIL;

    public boolean isAccepted() {
        return this == SUCCESS || this == CONSUME || this == CONSUME_PARTIAL;
    }

    public ActionResult toActionResult() {
        return switch (this) {
            case SUCCESS -> ActionResult.SUCCESS;
            case CONSUME -> ActionResult.CONSUME;
            case CONSUME_PARTIAL -> ActionResult.CONSUME_PARTIAL;
            case FAIL -> ActionResult.FAIL;
            default -> ActionResult.PASS;
        };
    }

    public interface WithItem {
        ItemActionResult use();
    }

    public interface WithoutItem {
        ActionResult use();
    }

    /** 1.21 客户端 / 服务端交互的顺序：先拿着东西点；放行且是主手才空手点；都没接受就交给物品自己的用法。 */
    public static ActionResult dispatch(BlockState state, PlayerEntity player, Hand hand, WithItem withItem, WithoutItem withoutItem) {
        ItemActionResult result = withItem.use();
        if (result.isAccepted()) {
            return result.toActionResult();
        }
        if (result == PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == Hand.MAIN_HAND) {
            return withoutItem.use();
        }
        return ActionResult.PASS;
    }
}
