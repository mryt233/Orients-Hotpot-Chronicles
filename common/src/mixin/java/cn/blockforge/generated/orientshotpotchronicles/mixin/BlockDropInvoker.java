// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import java.util.function.Supplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 只自定生成坐标；掉落规则、拾取延迟、平台捕获与回滚检查仍走原版入口。 */
@Mixin(Block.class)
public interface BlockDropInvoker {
    @Invoker("popResource")
    static void hotpot$popResource(Level level, Supplier<ItemEntity> factory, ItemStack stack) {
        throw new AssertionError("方块掉落调用桥未应用");
    }
}
