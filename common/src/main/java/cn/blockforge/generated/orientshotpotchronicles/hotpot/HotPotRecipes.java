// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** 配方查询统一使用调用方世界；旧无世界参数入口保留当前环境的回退。 */
public final class HotPotRecipes {
    private static volatile MinecraftServer server;
    private static volatile Supplier<Level> clientLevel = () -> null;
    // 只缓存当前配方代次的排序结果；弱键不阻止旧世界与旧 manager 回收。
    private record Snapshot(Object generation, java.util.Map<RecipeType<?>, List<?>> types) { }
    private static final java.util.Map<RecipeManager, Snapshot> SNAPSHOTS = new java.util.WeakHashMap<>();
    private HotPotRecipes() { }
    public static void register(RegistrationApi registration) {
        registration.recipeTypes().register(HotPot.id("pot_boiling"), PotBoilingRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("pot_boiling"), PotBoilingRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("pounding"), PoundingRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("pounding"), PoundingRecipe.SERIALIZER);
    }
    public static void setServer(MinecraftServer value) {
        server = value;
        if (value == null) synchronized (SNAPSHOTS) { SNAPSHOTS.clear(); }
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
        RecipeManager manager = level.getRecipeManager();
        synchronized (SNAPSHOTS) {
            Snapshot snapshot = SNAPSHOTS.get(manager);
            // 客户端 replaceRecipes 会原地换表，不能只检查 manager 身份。
            Object generation = manager.getRecipes();
            if (snapshot == null || snapshot.generation != generation) {
                snapshot = new Snapshot(generation, new java.util.HashMap<>());
                SNAPSHOTS.put(manager, snapshot);
            }
            return (List<RecipeHolder<T>>) snapshot.types.computeIfAbsent(type, ignored ->
                manager.getAllRecipesFor(type).stream()
                    .sorted(Comparator.comparing(holder -> holder.id().toString())).toList());
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
