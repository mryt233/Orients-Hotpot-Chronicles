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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 汤底登记表：ID → {@link SoupDef}，沿用 {@link FrozenRegistry}，支持数据包 {@link SoupBaseRecipe} 叠加视图。
 * 作者：mryt233
 */
public final class Soups {
    private static final Registry TABLE = new Registry();
    private static volatile boolean builtinsReady;
    private static final Map<Object, Registry.View> SNAPSHOTS = new WeakHashMap<>();

    private Soups() {
    }

    /** 清空跨数据包重载的汤底快照缓存。mryt233 改 */
    public static void clearCache() {
        clearRuntimeCache();
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
        Object source = RecipeSync.source(level);
        synchronized (SNAPSHOTS) {
            Registry.View cached = SNAPSHOTS.get(source);
            if (cached != null) {
                return cached;
            }
        }
        List<RecipeHolder<SoupBaseRecipe>> recipes = HotPotRecipes.all(level, SoupBaseRecipe.TYPE);
        Registry.View built = recipes.isEmpty()
            ? TABLE.baseView()
            : TABLE.overlay(recipes, HotPot.LOGGER::warn);
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.put(source, built);
        }
        return built;
    }

    public static boolean isBuiltin(Identifier id) {
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

    static void registerBowl(Identifier soupId, Item bowl) {
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

    public static SoupDef get(Identifier id) {
        return active().get(id);
    }

    public static SoupDef get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String text = id.indexOf(':') >= 0 ? id : HotPot.MOD_ID + ":" + id;
        Identifier location = Identifier.tryParse(text);
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
        record View(SoupDef[] ordered, Map<Identifier, SoupDef> byId, Map<Identifier, Integer> indexById) {
            SoupDef get(Identifier id) { return id == null ? null : byId.get(id); }
            SoupDef byIndex(int index) { return index >= 0 && index < ordered.length ? ordered[index] : null; }
            int indexOf(SoupDef def) {
                if (def == null) return -1;
                Integer idx = indexById.get(def.id());
                return idx == null ? -1 : idx;
            }
            List<SoupDef> all() { return List.of(ordered); }
            int size() { return ordered.length; }
        }

        private final FrozenRegistry<Identifier, SoupDef> registry = new FrozenRegistry<>(
            "汤底登记表", "汤底要在启动期登记（子模组在自己的兼容模块里登记），冻结后不再接受登记。");
        private final List<SoupDef> builtins = new ArrayList<>();
        private final Set<Identifier> builtinIds = new HashSet<>();
        private final List<SoupDef> extras = new ArrayList<>();
        private final Map<Identifier, Item> bowls = new HashMap<>();
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
            Map<Identifier, SoupDef> defs = new HashMap<>(next.length * 2);
            Map<Identifier, Integer> ids = new HashMap<>(next.length * 2);
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

        synchronized View overlay(List<RecipeHolder<SoupBaseRecipe>> recipes, Consumer<String> warn) {
            if (recipes == null || recipes.isEmpty()) {
                return view;
            }
            List<SoupDef> nextBuiltins = new ArrayList<>(builtins);
            Map<Identifier, Integer> builtinSlots = new HashMap<>(builtins.size() * 2);
            for (int i = 0; i < nextBuiltins.size(); i++) {
                builtinSlots.put(nextBuiltins.get(i).id(), i);
            }
            Map<Identifier, SoupDef> nextExtras = new LinkedHashMap<>();
            for (SoupDef extra : extras) {
                nextExtras.put(extra.id(), extra);
            }
            for (RecipeHolder<SoupBaseRecipe> holder : recipes) {
                Identifier target = SoupBaseRecipe.targetId(holder.id());
                if (target == null) {
                    if (warn != null) {
                        warn.accept("数据包汤底配方 " + holder.id() + " 未放在 recipe/soup/ 目录下，已跳过");
                    }
                    continue;
                }
                Integer slot = builtinSlots.get(target);
                if (slot != null) {
                    SoupDef merged = holder.value().resolve(target, nextBuiltins.get(slot), warn);
                    if (merged != null) {
                        nextBuiltins.set(slot, merged);
                    }
                } else {
                    SoupDef merged = holder.value().resolve(target, nextExtras.get(target), warn);
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
        synchronized boolean isBuiltin(Identifier id) { return id != null && builtinIds.contains(id); }
        SoupDef get(Identifier id) { return view.get(id); }
        SoupDef byIndex(int index) { return view.byIndex(index); }
        int indexOf(SoupDef def) { return view.indexOf(def); }
        List<SoupDef> all() { return view.all(); }
        synchronized List<SoupDef> builtins() { return List.copyOf(builtins); }
        int size() { return view.size(); }
        synchronized void registerBowl(Identifier soupId, Item bowl) {
            Objects.requireNonNull(soupId, "soupId");
            Objects.requireNonNull(bowl, "bowl");
            bowls.putIfAbsent(soupId, bowl);
        }
        synchronized Item bowlItem(SoupDef def) { return def == null ? null : bowls.get(def.id()); }
        void freeze() { registry.freeze(); }
        boolean isFrozen() { return registry.isFrozen(); }
    }
}
