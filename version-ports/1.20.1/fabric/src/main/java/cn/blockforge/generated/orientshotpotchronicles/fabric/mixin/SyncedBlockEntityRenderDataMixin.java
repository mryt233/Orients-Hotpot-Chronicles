// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.fabric.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.SyncedBlockEntity;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 在 Fabric 端将 {@link SyncedBlockEntity} 的 {@link MaterialRenderHost} 状态转发给线程安全区块渲染通道，
 * 以兼容 Fabric API 0.83.0+、Sodium + Indium 并避免直接修改 {@code BlockEntity} 造成 Mixin 冲突。作者：mryt233
 */
@Mixin(value = SyncedBlockEntity.class, remap = false)
public abstract class SyncedBlockEntityRenderDataMixin implements RenderAttachmentBlockEntity {
    @Override
    public @Nullable Object getRenderAttachmentData() {
        return this instanceof MaterialRenderHost host ? host.materialRenderData() : null;
    }
}
