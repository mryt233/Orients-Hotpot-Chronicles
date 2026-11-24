// 作者：mryt233
package cn.blockforge.orient.framework.loader;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** 创造栏输出契约，由平台包装原版输出，不暴露原版非公开类型。 */
@FunctionalInterface
public interface CreativeTabOutput {
    void accept(ItemStack stack);

    default void accept(ItemLike item) {
        accept(new ItemStack(item));
    }
}
