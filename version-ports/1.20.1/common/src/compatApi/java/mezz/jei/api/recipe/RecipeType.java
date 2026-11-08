package mezz.jei.api.recipe;

import net.minecraft.resources.ResourceLocation;

/**
 * 配方的种类标识：一个 RecipeType 对应 JEI 里的一个配方类别。
 * <p>
 * 本文件是本工程「编译期签名」的一部分：只在编译时提供与官方二进制一致的包名 / 类名 / 方法描述符，
 * 让插件源码可以零联网编译；成品 jar 会排除整个 {@code mezz/jei} 目录，运行时用的是玩家真正安装的那份。
 * 作者：mryt233 */

public class RecipeType<T> {
    private final ResourceLocation uid;
    private final Class<? extends T> recipeClass;

    private RecipeType(ResourceLocation uid, Class<? extends T> recipeClass) {
        this.uid = uid;
        this.recipeClass = recipeClass;
    }

    /** 用命名空间与路径创建配方种类。 */
    public static <T> RecipeType<T> create(String nameSpace, String path, Class<? extends T> recipeClass) {
        return new RecipeType<>(new ResourceLocation(nameSpace, path), recipeClass);
    }

    public ResourceLocation getUid() {
        return uid;
    }

    public Class<? extends T> getRecipeClass() {
        return recipeClass;
    }
}
