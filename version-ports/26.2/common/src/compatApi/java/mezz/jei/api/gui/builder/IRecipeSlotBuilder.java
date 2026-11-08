package mezz.jei.api.gui.builder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * 槽位构造器：往一个槽里填要展示的物品。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code mezz/jei} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface IRecipeSlotBuilder {
    IRecipeSlotBuilder addItemStack(ItemStack stack);

    IRecipeSlotBuilder addItemStacks(List<ItemStack> stacks);

    IRecipeSlotBuilder addIngredients(Ingredient ingredient);
}
