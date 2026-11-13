// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

/** 26.2 客户端不再接收完整配方，随现有配置通道同步厨具与数据包定义实际使用的配方。 */
public final class RecipeSync {
    private static volatile List<RecipeHolder<?>> clientRecipes = List.of();

    private RecipeSync() {
    }

    public static List<RecipeHolder<?>> snapshot(MinecraftServer server) {
        if (server == null) {
            return List.of();
        }
        return server.overworld().recipeAccess().getRecipes().stream()
            .filter(holder -> {
                Recipe<?> recipe = holder.value();
                return recipe instanceof AbstractCookingRecipe
                    || recipe instanceof StonecutterRecipe
                    || recipe instanceof CraftingRecipe
                    || recipe instanceof PotBoilingRecipe
                    || recipe instanceof PoundingRecipe
                    || recipe instanceof SoupBaseRecipe
                    || recipe instanceof SeasoningRecipe;
            })
            .toList();
    }

    /** 平台网络接收器在客户端主线程调用。 */
    public static void accept(List<RecipeHolder<?>> recipes) {
        clientRecipes = List.copyOf(recipes);
        HotPotRecipes.clearRuntimeCache();
        Soups.clearRuntimeCache();
        Seasonings.clearRuntimeCache();
    }

    public static void clearClient() {
        clientRecipes = List.of();
        HotPotRecipes.clearRuntimeCache();
        Soups.clearRuntimeCache();
        Seasonings.clearRuntimeCache();
    }

    /** 当前配方来源的身份对象（26.2 服务端每次 /reload 返回新的 ImmutableMap.values 实例），给缓存当键用。作者：mryt233 */
    public static Object source(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().getRecipes() : clientRecipes;
    }

    public static Iterable<RecipeHolder<?>> recipes(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().getRecipes() : clientRecipes;
    }

    @SuppressWarnings("unchecked")
    static <T extends Recipe<SingleRecipeInput>> Optional<RecipeHolder<T>> find(
            Level level, RecipeType<T> type, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        for (RecipeHolder<?> holder : recipes(level)) {
            if (holder.value().getType() == type) {
                RecipeHolder<T> typed = (RecipeHolder<T>) holder;
                if (typed.value().matches(input, level)) {
                    return Optional.of(typed);
                }
            }
        }
        return Optional.empty();
    }
}
