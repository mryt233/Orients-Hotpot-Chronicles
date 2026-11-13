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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 汤底登记表：ID → {@link SoupDef}（mryt233 改：1.20.1 适配无 RecipeHolder）。
 * 作者：mryt233 */
public final class Soups {
    private static final Registry TABLE = new Registry();
    private static volatile boolean builtinsReady;
    private record CachedView(List<SoupBaseRecipe> recipes, Registry.View view) { }
    private static final Map<RecipeManager, CachedView> SNAPSHOTS = new WeakHashMap<>();

    private Soups() {
    }

    static void clearRuntimeCache() {
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.clear();
        }
    }

    private static Registry.View active() {
        ensureBuiltins();
        Level level = HotPotRecipes.currentLevel();
        if (level == null) {
            return TABLE.baseView();
        }
        RecipeManager manager = level.getRecipeManager();
        List<SoupBaseRecipe> recipes = HotPotRecipes.all(level, SoupBaseRecipe.TYPE);
        synchronized (SNAPSHOTS) {
            CachedView cached = SNAPSHOTS.get(manager);
            if (cached != null && cached.recipes() == recipes) {
                return cached.view();
            }
        }
        Registry.View built = recipes.isEmpty()
            ? TABLE.baseView()
            : TABLE.overlay(recipes, HotPot.LOGGER::warn);
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.put(manager, new CachedView(recipes, built));
        }
        return built;
    }

    public static boolean isBuiltin(ResourceLocation id) {
        ensureBuiltins();
        return TABLE.isBuiltin(id);
    }

    static void registerBuiltin(SoupDef def) {
        TABLE.registerBuiltin(def);
    }

    static void builtinsDone() {
        builtinsReady = true;
    }

    static List<SoupDef> builtinsSoFar() {
        return TABLE.builtins();
    }

    public static void register(SoupDef def) {
        Objects.requireNonNull(def, "def");
        ensureBuiltins();
        TABLE.register(def);
    }

    static void registerBowl(ResourceLocation soupId, Item bowl) {
        TABLE.registerBowl(soupId, bowl);
    }

    public static Item bowlItem(SoupDef def) {
        if (def == null) {
            return null;
        }
        int idx = indexOf(def);
        if (HotPotContent.SOUP_BOWLS != null && idx >= 0 && idx < HotPotContent.SOUP_BOWLS.size()) {
            return HotPotContent.SOUP_BOWLS.get(idx);
        }
        return TABLE.bowlItem(def);
    }

    public static ItemStack bowlStack(@Nullable SoupDef def) {
        return SoupBowlItem.createFor(def);
    }

    public static SoupDef get(ResourceLocation id) {
        return active().get(id);
    }

    public static SoupDef get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String text = id.indexOf(':') >= 0 ? id : HotPot.MOD_ID + ":" + id;
        ResourceLocation location = ResourceLocation.tryParse(text);
        return location == null ? null : get(location);
    }

    public static SoupDef byIndex(int index) {
        return active().byIndex(index);
    }

    public static int indexOf(SoupDef def) {
        if (def == null) {
            return -1;
        }
        return active().indexOf(def);
    }

    public static SoupDef clear() {
        return byIndex(0);
    }

    public static SoupDef fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (SoupDef def : active().ordered()) {
            if (def.matchesSource(stack)) {
                return def;
            }
        }
        SoupBase custom = SoupBase.customSource(stack);
        return custom == null ? null : get(custom.def().id());
    }

    public static List<SoupDef> all() {
        return active().all();
    }

    public static List<SoupDef> builtins() {
        ensureBuiltins();
        return TABLE.builtins();
    }

    public static int size() {
        return active().size();
    }

    static void freeze() {
        ensureBuiltins();
        TABLE.freeze();
    }

    public static boolean isFrozen() {
        return TABLE.isFrozen();
    }

    private static void ensureBuiltins() {
        if (!builtinsReady) {
            SoupBase.values();
        }
    }

    static final class Registry {
        record View(SoupDef[] ordered, Map<ResourceLocation, SoupDef> byId, Map<ResourceLocation, Integer> indexById) {
            SoupDef get(ResourceLocation id) { return id == null ? null : byId.get(id); }
            SoupDef byIndex(int index) { return index >= 0 && index < ordered.length ? ordered[index] : null; }
            int indexOf(SoupDef def) {
                if (def == null) return -1;
                Integer idx = indexById.get(def.id());
                return idx == null ? -1 : idx;
            }
            List<SoupDef> all() { return List.of(ordered); }
            int size() { return ordered.length; }
        }

        private final FrozenRegistry<ResourceLocation, SoupDef> registry = new FrozenRegistry<>(
            "汤底登记表", "汤底要在启动期登记（子模组在自己的兼容模块里登记），冻结后不再接受登记。");
        private final List<SoupDef> builtins = new ArrayList<>();
        private final Set<ResourceLocation> builtinIds = new HashSet<>();
        private final List<SoupDef> extras = new ArrayList<>();
        private final Map<ResourceLocation, Item> bowls = new HashMap<>();
        private volatile View view = new View(new SoupDef[0], Map.of(), Map.of());

        synchronized void registerBuiltin(SoupDef def) {
            Objects.requireNonNull(def, "def");
            if (registry.containsKey(def.id())) {
                throw new IllegalStateException("内置汤底 ID 重复：" + def.id());
            }
            registry.put(def.id(), def);
            builtins.add(def);
            builtinIds.add(def.id());
            view = buildView(builtins, extras);
        }

        synchronized void register(SoupDef def) {
            Objects.requireNonNull(def, "def");
            if (registry.containsKey(def.id())) {
                throw new IllegalArgumentException("汤底 ID 已被登记：" + def.id());
            }
            registry.put(def.id(), def);
            extras.add(def);
            extras.sort(Comparator.comparing(d -> d.id().toString()));
            view = buildView(builtins, extras);
        }

        private static View buildView(List<SoupDef> builtinList, List<SoupDef> extraList) {
            SoupDef[] next = new SoupDef[builtinList.size() + extraList.size()];
            Map<ResourceLocation, SoupDef> defs = new HashMap<>(next.length * 2);
            Map<ResourceLocation, Integer> ids = new HashMap<>(next.length * 2);
            int i = 0;
            for (SoupDef entry : builtinList) {
                next[i] = entry;
                defs.put(entry.id(), entry);
                ids.put(entry.id(), i++);
            }
            for (SoupDef entry : extraList) {
                next[i] = entry;
                defs.put(entry.id(), entry);
                ids.put(entry.id(), i++);
            }
            return new View(next, Map.copyOf(defs), Map.copyOf(ids));
        }

        synchronized View overlay(List<SoupBaseRecipe> recipes, Consumer<String> warn) {
            if (recipes == null || recipes.isEmpty()) {
                return view;
            }
            List<SoupDef> nextBuiltins = new ArrayList<>(builtins);
            Map<ResourceLocation, Integer> builtinSlots = new HashMap<>(builtins.size() * 2);
            for (int i = 0; i < nextBuiltins.size(); i++) {
                builtinSlots.put(nextBuiltins.get(i).id(), i);
            }
            Map<ResourceLocation, SoupDef> nextExtras = new LinkedHashMap<>();
            for (SoupDef extra : extras) {
                nextExtras.put(extra.id(), extra);
            }
            for (SoupBaseRecipe recipe : recipes) {
                ResourceLocation target = SoupBaseRecipe.targetId(recipe.getId());
                if (target == null) {
                    if (warn != null) {
                        warn.accept("数据包汤底配方 " + recipe.getId() + " 未放在 recipes/soup/ 目录下，已跳过");
                    }
                    continue;
                }
                Integer slot = builtinSlots.get(target);
                if (slot != null) {
                    SoupDef merged = recipe.resolve(target, nextBuiltins.get(slot), warn);
                    if (merged != null) {
                        nextBuiltins.set(slot, merged);
                    }
                } else {
                    SoupDef merged = recipe.resolve(target, nextExtras.get(target), warn);
                    if (merged != null) {
                        nextExtras.put(target, merged);
                    }
                }
            }
            List<SoupDef> sortedExtras = new ArrayList<>(nextExtras.values());
            sortedExtras.sort(Comparator.comparing(d -> d.id().toString()));
            return buildView(nextBuiltins, sortedExtras);
        }

        View baseView() { return view; }
        synchronized boolean isBuiltin(ResourceLocation id) { return id != null && builtinIds.contains(id); }
        SoupDef get(ResourceLocation id) { return view.get(id); }
        SoupDef byIndex(int index) { return view.byIndex(index); }
        int indexOf(SoupDef def) { return view.indexOf(def); }
        List<SoupDef> all() { return view.all(); }
        synchronized List<SoupDef> builtins() { return List.copyOf(builtins); }
        int size() { return view.size(); }
        synchronized void registerBowl(ResourceLocation soupId, Item bowl) {
            Objects.requireNonNull(soupId, "soupId");
            Objects.requireNonNull(bowl, "bowl");
            bowls.putIfAbsent(soupId, bowl);
        }
        synchronized Item bowlItem(SoupDef def) { return def == null ? null : bowls.get(def.id()); }
        void freeze() { registry.freeze(); }
        boolean isFrozen() { return registry.isFrozen(); }
    }
}
