package mezz.jei.api.recipe.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 一个配方类别的展示定义：标题、图标、尺寸、槽位排布与额外绘制。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code mezz/jei} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface IRecipeCategory<T> {
    RecipeType<T> getRecipeType();

    Component getTitle();

    default int getWidth() {
        return 0;
    }

    default int getHeight() {
        return 0;
    }

    IDrawable getIcon();

    void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    default void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
    }
}
