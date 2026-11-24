// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.fabric.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.SyncedBlockEntity;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 在 Fabric 端将 {@link SyncedBlockEntity} 的 {@link MaterialRenderHost} 状态转发给线程安全区块渲染通道。作者：mryt233
 */
@Mixin(value = SyncedBlockEntity.class, remap = false)
public abstract class SyncedBlockEntityRenderDataMixin {
    public @Nullable Object getRenderData() {
        return this instanceof MaterialRenderHost host ? host.materialRenderData() : null;
    }
}
