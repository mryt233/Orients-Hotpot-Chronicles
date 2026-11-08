// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

/** 26.1 客户端不再接收完整配方，随现有配置通道同步厨具实际使用的配方。 */
public final class RecipeSync {
    private static volatile List<RecipeHolder<?>> clientRecipes = List.of();

    private RecipeSync() { }

    public static List<RecipeHolder<?>> snapshot(MinecraftServer server) {
        return server.overworld().recipeAccess().getRecipes().stream()
            .filter(holder -> holder.value() instanceof AbstractCookingRecipe
                || holder.value() instanceof StonecutterRecipe
                || holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe).toList();
    }

    /** 平台网络接收器在客户端主线程调用。 */
    public static void accept(List<RecipeHolder<?>> recipes) {
        clientRecipes = List.copyOf(recipes);
    }

    public static void clearClient() {
        clientRecipes = List.of();
    }

    /** 当前配方来源的身份对象，给「配方变了就失效」的缓存当键用。作者：mryt233 */
    public static Object source(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess() : clientRecipes;
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
