// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 配方查询统一使用调用方世界；26.1 通过 {@link RecipeSync} 读取服务端与已同步客户端配方。
 *
 * <p>mryt233 改（2026-11-13）：适配 26.1 无 {@code level.getRecipeManager()}，缓存以
 * {@link Level} 为弱键、以 {@link RecipeSync#generation(Level)} 为代次凭证。</p>
 */
public final class HotPotRecipes {
    private static volatile MinecraftServer server;
    private static volatile Supplier<Level> clientLevel = () -> null;
    private record Snapshot(Object generation, Map<RecipeType<?>, List<?>> types) { }
    private static final Map<Level, Snapshot> SNAPSHOTS = new WeakHashMap<>();

    private HotPotRecipes() { }

    public static void register(RegistrationApi registration) {
        registration.recipeTypes().register(HotPot.id("pot_boiling"), PotBoilingRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("pot_boiling"), PotBoilingRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("pounding"), PoundingRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("pounding"), PoundingRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("soup_base"), SoupBaseRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("soup_base"), SoupBaseRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("seasoning"), SeasoningRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("seasoning"), SeasoningRecipe.SERIALIZER);
    }

    static void clearRuntimeCache() {
        synchronized (SNAPSHOTS) { SNAPSHOTS.clear(); }
    }

    public static void setServer(MinecraftServer value) {
        server = value;
        if (value == null) {
            clearRuntimeCache();
            Soups.clearRuntimeCache();
            Seasonings.clearRuntimeCache();
        }
    }

    public static void clientLevel(Supplier<Level> supplier) { clientLevel = supplier; }

    public static Level currentLevel() {
        MinecraftServer active = server;
        if (active != null && active.isSameThread()) return active.overworld();
        Level client = clientLevel.get();
        return client != null ? client : active == null ? null : active.overworld();
    }

    /** 同原料多条配方按完整 ID 排序，避免加载器或重载顺序改变选择结果。 */
    @SuppressWarnings("unchecked")
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> all(Level level, RecipeType<T> type) {
        if (level == null) return List.of();
        synchronized (SNAPSHOTS) {
            Snapshot snapshot = SNAPSHOTS.get(level);
            Object generation = RecipeSync.generation(level);
            if (snapshot == null || snapshot.generation != generation) {
                snapshot = new Snapshot(generation, new HashMap<>());
                SNAPSHOTS.put(level, snapshot);
            }
            return (List<RecipeHolder<T>>) snapshot.types.computeIfAbsent(type, ignored -> {
                List<RecipeHolder<T>> found = new ArrayList<>();
                for (RecipeHolder<?> holder : RecipeSync.recipes(level)) {
                    if (holder.value().getType() == type) {
                        found.add((RecipeHolder<T>) holder);
                    }
                }
                found.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));
                return List.copyOf(found);
            });
        }
    }

    public static PotBoilingRecipe boiling(Level level, ItemStack input) {
        if (input.isEmpty()) return null;
        return all(level, PotBoilingRecipe.TYPE).stream().map(RecipeHolder::value)
            .filter(recipe -> recipe.ingredient().test(input)).findFirst().orElse(null);
    }

    public static PotBoilingRecipe overcooking(Level level, ItemStack cooked) {
        if (cooked.isEmpty()) return null;
        return all(level, PotBoilingRecipe.TYPE).stream().map(RecipeHolder::value)
            .filter(recipe -> !recipe.overcookResult().isEmpty()
                && recipe.result().is(cooked.getItem()))
            .findFirst().orElse(null);
    }

    public static PoundingRecipe pounding(Level level, ItemStack input) {
        if (input.isEmpty()) return null;
        return all(level, PoundingRecipe.TYPE).stream().map(RecipeHolder::value)
            .filter(recipe -> recipe.ingredient().test(input)).findFirst().orElse(null);
    }
}
