package mezz.jei.api;

import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 插件的公共接口：注册分类、配方和催化剂。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code mezz/jei} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public interface IModPlugin {
    /** 插件唯一标识，必须全模组唯一。 */
    ResourceLocation getPluginUid();

    /** 注册展示分类。 */
    default void registerCategories(IRecipeCategoryRegistration registration) {
    }

    /** 注册要展示的配方。 */
    default void registerRecipes(IRecipeRegistration registration) {
    }

    /** 注册每种配方的工作台（在 JEI 里作为“可用工作台”显示）。 */
    default void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
    }
}
