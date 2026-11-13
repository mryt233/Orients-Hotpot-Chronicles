// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Collection;
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

/**
 * 26.1 客户端不再接收完整配方，随现有配置通道同步厨具与数据包汤底 / 蘸料实际使用的配方。
 *
 * <p>mryt233 改（2026-11-13）：同步列表补齐 {@link PotBoilingRecipe}、{@link PoundingRecipe}、
 * {@link SoupBaseRecipe} 与 {@link SeasoningRecipe}。</p>
 */
public final class RecipeSync {
    private static volatile List<RecipeHolder<?>> clientRecipes = List.of();
    private static volatile int clientGeneration = 0;

    private RecipeSync() { }

    public static List<RecipeHolder<?>> snapshot(MinecraftServer server) {
        return server.overworld().recipeAccess().getRecipes().stream()
            .filter(holder -> holder.value() instanceof AbstractCookingRecipe
                || holder.value() instanceof StonecutterRecipe
                || holder.value() instanceof CraftingRecipe
                || holder.value() instanceof PotBoilingRecipe
                || holder.value() instanceof PoundingRecipe
                || holder.value() instanceof SoupBaseRecipe
                || holder.value() instanceof SeasoningRecipe)
            .toList();
    }

    /** 平台网络接收器在客户端主线程调用。 */
    public static void accept(List<RecipeHolder<?>> recipes) {
        clientRecipes = List.copyOf(recipes);
        clientGeneration++;
        HotPotRecipes.clearRuntimeCache();
        Soups.clearRuntimeCache();
        Seasonings.clearRuntimeCache();
    }

    public static void clearClient() {
        clientRecipes = List.of();
        clientGeneration++;
        HotPotRecipes.clearRuntimeCache();
        Soups.clearRuntimeCache();
        Seasonings.clearRuntimeCache();
    }

    public static Collection<RecipeHolder<?>> recipes(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().getRecipes() : clientRecipes;
    }

    /** 用于缓存失效判断的配方代次凭证：服务端取 recipes 集合引用，客户端取递增代次号。 */
    public static Object source(Level level) {
        return generation(level);
    }

    static Object generation(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().getRecipes() : Integer.valueOf(clientGeneration);
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
