// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

/** 4C / 4F-1 / 4F-2 蘸料登记表；内置 15 种占前 0～14 固定位，扩展与数据包项按完整 ID 排在后面。作者：mryt233 */
public final class Seasonings {
    private static final Registry TABLE = createBuiltins();
    private record CachedView(Object generation, Registry.View view) { }
    private static final Map<RecipeManager, CachedView> SNAPSHOTS = new WeakHashMap<>();

    private Seasonings() { }
    private static Registry createBuiltins() {
        Registry registry = new Registry();
        for (Seasoning value : Seasoning.ALL) registry.registerBuiltin(value.def());
        return registry;
    }

    static void clearRuntimeCache() {
        synchronized (SNAPSHOTS) { SNAPSHOTS.clear(); }
    }

    private static Registry.View active() {
        Level level = HotPotRecipes.currentLevel();
        if (level == null) return TABLE.baseView();
        RecipeManager manager = level.getRecipeManager();
        Object generation = manager.getRecipes();
        synchronized (SNAPSHOTS) {
            CachedView cached = SNAPSHOTS.get(manager);
            if (cached != null && cached.generation() == generation) return cached.view();
        }
        List<RecipeHolder<SeasoningRecipe>> recipes = HotPotRecipes.all(level, SeasoningRecipe.TYPE);
        Registry.View built = recipes.isEmpty()
            ? TABLE.baseView()
            : TABLE.overlay(recipes, HotPot.LOGGER::warn);
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.put(manager, new CachedView(generation, built));
        }
        return built;
    }

    /** 是否是模组内置 15 种蘸料之一（仅内置项读取 config 覆盖；4F-2）。 */
    public static boolean isBuiltin(String id) { return TABLE.isBuiltin(id); }

    /** 4F-1（mryt233）：公开蘸料登记入口（启动期调用，冻结后抛 {@link IllegalStateException}；ID 重复抛 {@link IllegalArgumentException}）。 */
    public static void register(SeasoningDef def) { TABLE.register(Objects.requireNonNull(def, "def")); }
    public static List<SeasoningDef> all() { return active().all(); }
    public static int size() { return active().size(); }
    public static int indexOf(SeasoningDef def) { return active().indexOf(def); }
    public static SeasoningDef byIndex(int index) { return active().byIndex(index); }
    public static SeasoningDef ofRaw(ItemStack stack) { return stack.isEmpty() ? null : active().raw(stack.getItem()); }
    public static SeasoningDef get(String id) { return active().byId(id); }
    static void freeze() { TABLE.freeze(); }
    public static boolean isFrozen() { return TABLE.values.isFrozen(); }

    /** 独立实例供登记规则测试使用，不污染进程里的内置表。 */
    static final class Registry {
        static final class View {
            private final List<SeasoningDef> all;
            private final Map<String, SeasoningDef> byId;
            private final Map<String, Integer> indexById;
            private volatile Map<Item, SeasoningDef> raws;

            View(List<SeasoningDef> all, Map<String, SeasoningDef> byId, Map<String, Integer> indexById) {
                this.all = all;
                this.byId = byId;
                this.indexById = indexById;
            }

            List<SeasoningDef> all() { return all; }
            int size() { return all.size(); }
            SeasoningDef byIndex(int index) { return index >= 0 && index < all.size() ? all.get(index) : null; }
            SeasoningDef byId(String id) { return id == null || id.isEmpty() ? null : byId.get(SauceIds.full(id)); }
            int indexOf(SeasoningDef def) {
                if (def == null) return -1;
                Integer idx = indexById.get(SauceIds.full(def.id()));
                return idx == null ? -1 : idx;
            }
            SeasoningDef raw(Item item) {
                Map<Item, SeasoningDef> cached = raws;
                if (cached == null) {
                    synchronized (this) {
                        if (raws == null) {
                            Map<Item, SeasoningDef> built = new HashMap<>();
                            for (SeasoningDef entry : all) {
                                for (Item raw : entry.raws()) built.putIfAbsent(raw, entry);
                            }
                            raws = built;
                        }
                        cached = raws;
                    }
                }
                return cached.get(item);
            }
        }

        private final FrozenRegistry<String, SeasoningDef> values = new FrozenRegistry<>("蘸料登记表", "蘸料只能在启动期登记");
        private final List<SeasoningDef> builtins = new ArrayList<>();
        private final Set<String> builtinIds = new HashSet<>();
        private final List<SeasoningDef> extras = new ArrayList<>();
        private volatile View view = new View(List.of(), Map.of(), Map.of());

        synchronized void registerBuiltin(SeasoningDef def) {
            String key = SauceIds.full(def.id());
            if (values.containsKey(key)) throw new IllegalArgumentException("蘸料 ID 重复：" + def.id());
            values.put(key, def);
            builtins.add(def);
            builtinIds.add(key);
            view = buildView(builtins, extras);
        }
        synchronized void register(SeasoningDef def) {
            String key = SauceIds.full(def.id());
            if (values.containsKey(key)) throw new IllegalArgumentException("蘸料 ID 重复：" + def.id());
            values.put(key, def);
            extras.add(def);
            extras.sort(Comparator.comparing(d -> SauceIds.full(d.id())));
            view = buildView(builtins, extras);
        }
        private static View buildView(List<SeasoningDef> builtinList, List<SeasoningDef> extraList) {
            List<SeasoningDef> merged = new ArrayList<>(builtinList.size() + extraList.size());
            merged.addAll(builtinList);
            merged.addAll(extraList);
            Map<String, SeasoningDef> byId = new HashMap<>(merged.size() * 2);
            Map<String, Integer> indices = new HashMap<>(merged.size() * 2);
            for (int i = 0; i < merged.size(); i++) {
                SeasoningDef entry = merged.get(i);
                String full = SauceIds.full(entry.id());
                byId.put(full, entry);
                indices.put(full, i);
            }
            return new View(List.copyOf(merged), Map.copyOf(byId), Map.copyOf(indices));
        }

        synchronized View overlay(List<RecipeHolder<SeasoningRecipe>> recipes, Consumer<String> warn) {
            if (recipes == null || recipes.isEmpty()) return view;
            List<SeasoningDef> nextBuiltins = new ArrayList<>(builtins);
            Map<String, Integer> builtinSlots = new HashMap<>(builtins.size() * 2);
            for (int i = 0; i < nextBuiltins.size(); i++) {
                builtinSlots.put(SauceIds.full(nextBuiltins.get(i).id()), i);
            }
            Map<String, SeasoningDef> nextExtras = new LinkedHashMap<>();
            for (SeasoningDef extra : extras) {
                nextExtras.put(SauceIds.full(extra.id()), extra);
            }
            for (RecipeHolder<SeasoningRecipe> holder : recipes) {
                ResourceLocation target = SeasoningRecipe.targetId(holder.id());
                if (target == null) {
                    if (warn != null) {
                        warn.accept("数据包蘸料配方 " + holder.id() + " 未放在 recipe/seasoning/ 目录下，已跳过");
                    }
                    continue;
                }
                String full = SauceIds.full(target.toString());
                Integer slot = builtinSlots.get(full);
                if (slot != null) {
                    SeasoningDef merged = holder.value().resolve(target, nextBuiltins.get(slot), warn);
                    if (merged != null) nextBuiltins.set(slot, merged);
                } else {
                    SeasoningDef merged = holder.value().resolve(target, nextExtras.get(full), warn);
                    if (merged != null) nextExtras.put(full, merged);
                }
            }
            List<SeasoningDef> sortedExtras = new ArrayList<>(nextExtras.values());
            sortedExtras.sort(Comparator.comparing(d -> SauceIds.full(d.id())));
            return buildView(nextBuiltins, sortedExtras);
        }

        View baseView() { return view; }
        synchronized boolean isBuiltin(String id) { return id != null && !id.isEmpty() && builtinIds.contains(SauceIds.full(id)); }
        List<SeasoningDef> all() { return view.all(); }
        SeasoningDef byIndex(int index) { return view.byIndex(index); }
        SeasoningDef byId(String id) { return view.byId(id); }
        SeasoningDef raw(Item item) { return view.raw(item); }
        synchronized void freeze() { values.freeze(); }
    }
}
