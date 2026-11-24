// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.neoforge.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.SyncedBlockEntity;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import cn.blockforge.orient.framework.platform.neoforge.NeoForgeModelData;
import net.neoforged.neoforge.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 在 NeoForge 客户端将 {@link SyncedBlockEntity} 的 {@link MaterialRenderHost} 状态暴露给 {@code ModelData}，
 * 以供后台区块构建线程安全读取。作者：mryt233
 */
@Mixin(value = SyncedBlockEntity.class, remap = false)
public abstract class SyncedBlockEntityModelDataMixin {
    public ModelData getModelData() {
        if (this instanceof MaterialRenderHost host) {
            Object snap = host.materialRenderData();
            if (snap != null) {
                return ModelData.of(NeoForgeModelData.SNAPSHOT, snap);
            }
        }
        return ModelData.EMPTY;
    }
}
