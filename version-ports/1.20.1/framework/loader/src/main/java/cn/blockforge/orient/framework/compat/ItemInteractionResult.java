package cn.blockforge.orient.framework.compat;

import net.minecraft.world.InteractionResult;

/** 保留新版物品交互结果和旧版 use 的分派顺序。作者：mryt233 */
public enum ItemInteractionResult {
    SUCCESS(InteractionResult.SUCCESS), CONSUME(InteractionResult.CONSUME), FAIL(InteractionResult.FAIL),
    PASS_TO_DEFAULT_BLOCK_INTERACTION(InteractionResult.PASS), SKIP_DEFAULT_BLOCK_INTERACTION(InteractionResult.PASS);
    private final InteractionResult result;
    ItemInteractionResult(InteractionResult result) { this.result = result; }
    public InteractionResult result() { return result; }
    public static ItemInteractionResult sidedSuccess(boolean client) { return client ? SUCCESS : CONSUME; }
}
