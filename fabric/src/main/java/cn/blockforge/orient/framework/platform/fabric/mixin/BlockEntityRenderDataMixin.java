// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric.mixin;

import cn.blockforge.orient.framework.game.MaterialRenderHost;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 将实现 {@link MaterialRenderHost} 的方块实体转发给 Fabric 的线程安全区块渲染数据通道，
 * 以兼容 Sodium + Indium 等多线程区块构网引擎。作者：mryt233
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityRenderDataMixin implements RenderDataBlockEntity {
    @Override
    public @Nullable Object getRenderData() {
        return this instanceof MaterialRenderHost host ? host.materialRenderData() : null;
    }
}
