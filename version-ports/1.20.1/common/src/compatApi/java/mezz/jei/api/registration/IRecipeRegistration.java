package mezz.jei.api.registration;

import mezz.jei.api.recipe.RecipeType;

import java.util.List;

/**
 * 注册配方内容用的入口。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code mezz/jei} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface IRecipeRegistration {
    <T> void addRecipes(RecipeType<T> recipeType, List<T> recipes);
}
