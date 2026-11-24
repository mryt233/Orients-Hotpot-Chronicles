package mezz.jei.api.gui.builder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

/** 编译期签名，保留真实泛型返回类型，不进入分发包。作者：mryt233 */
public interface IIngredientAcceptor<THIS extends IIngredientAcceptor<THIS>> {
    THIS addItemStack(ItemStack stack);
    THIS addItemStacks(List<ItemStack> stacks);
    THIS addIngredients(Ingredient ingredient);
}
