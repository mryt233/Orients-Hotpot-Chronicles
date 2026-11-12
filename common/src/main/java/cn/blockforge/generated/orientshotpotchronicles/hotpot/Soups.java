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
import org.jetbrains.annotations.Nullable;

/**
 * 汤底登记表：ID → {@link SoupDef}，沿用第三批的 {@link FrozenRegistry}（启动期可写、服务端启动时冻结）。
 *
 * <p>第四批 4B-1（mryt233，2026-11-11）立表：{@link SoupBase} 的六个枚举值在类初始化时按顺序登记成表里的
 * <b>前 6 项</b>（顺序 = 旧存档里的汤底序号 0～5），内置项永远占前 6 项；以后新增的汤底（4F）排在后面。
 * 4B-2：游戏里「按序号取汤底 / 遍历全部汤底 / 按名字查汤底 / 手里这件是不是汤底原料」的调用点全部改成问这张表，
 * 4D 起锅存档持久化 ID，菜单传会话索引，内置配置仍沿用短名。</p>
 *
 * <p><b>序号（索引）</b>：{@link #byIndex(int)} / {@link #indexOf(SoupDef)} 用的是登记顺序。内置项的索引就是旧存档里的
 * 汤底序号，不会变；其余项的索引只在本次运行里有意义（4D 起存档改存 ID，菜单属性传会话内索引）。</p>
 *
 * <p>ID 规则：内置项的 ID 是火锅命名空间下的短名（{@code clear}、{@code spicy} ……，同 {@code SoupBase.textureKey()}）；
 * 查询时不带命名空间的名字一律按火锅命名空间解释。</p>
 *
 * <p>线程与时机：内置项由 {@link SoupBase} 的静态初始化登记，{@code HotPotRuntime.install} 在读配置之前强制初始化它；
 * 服务端开始启动时由 {@link HotPotHooks#freezeRegistries()} 冻结，之后再登记抛 {@link IllegalStateException}。
 * 查询类方法会先确保内置项已登记，所以任何时候第一个调用的是谁都不影响结果；
 * 索引数组每次登记整体替换（读取不加锁，拿到的总是某一时刻完整的快照）。</p>
 * 作者：mryt233 */
public final class Soups {
    private static final Registry TABLE = new Registry();
    /** 内置项登记完了没有（让热路径上的 ensureBuiltins 不必每次都碰枚举）。 */
    private static volatile boolean builtinsReady;
    private record CachedView(Object generation, Registry.View view) { }
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
        Object generation = manager.getRecipes();
        synchronized (SNAPSHOTS) {
            CachedView cached = SNAPSHOTS.get(manager);
            if (cached != null && cached.generation() == generation) {
                return cached.view();
            }
        }
        List<RecipeHolder<SoupBaseRecipe>> recipes = HotPotRecipes.all(level, SoupBaseRecipe.TYPE);
        Registry.View built = recipes.isEmpty()
            ? TABLE.baseView()
            : TABLE.overlay(recipes, HotPot.LOGGER::warn);
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.put(manager, new CachedView(generation, built));
        }
        return built;
    }

    /** 是否是模组内置 6 种汤底之一（仅内置项读取 config 覆盖；4F-2）。 */
    public static boolean isBuiltin(ResourceLocation id) {
        ensureBuiltins();
        return TABLE.isBuiltin(id);
    }

    /** 内置项登记：只给 {@link SoupBase} 的静态初始化用，按枚举顺序调用。 */
    static void registerBuiltin(SoupDef def) {
        TABLE.registerBuiltin(def);
    }

    /** {@link SoupBase} 登记完全部内置项后调用一次。 */
    static void builtinsDone() {
        builtinsReady = true;
    }

    /** 内置项还没登记完时也能用（给 {@link SoupVisual} 在 {@link SoupBase} 初始化期间核对用）。 */
    static List<SoupDef> builtinsSoFar() {
        return TABLE.builtins();
    }

    /**
     * 4F-1（mryt233）：公开汤底登记入口（启动期调用，冻结后抛 {@link IllegalStateException}；ID 重复抛 {@link IllegalArgumentException}）。
     */
    public static void register(SoupDef def) {
        Objects.requireNonNull(def, "def");
        ensureBuiltins();   // 不能放进下面的同步块：SoupBase 初始化时也要拿这把锁，两边先后顺序不同会死锁
        TABLE.register(def);
    }

    /** 4F-1（mryt233）：登记专属汤碗（由 {@link SoupBowlItem#SoupBowlItem(ResourceLocation)} 调用）。 */
    static void registerBowl(ResourceLocation soupId, Item bowl) {
        TABLE.registerBowl(soupId, bowl);
    }

    /** 4F-1（mryt233）：盛这种汤用的碗（内置前 6 项走 {@code SOUP_BOWLS}，否则查专属碗，没有返回 null）。 */
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

    /** 4F-3（mryt233）：按汤底生成可盛出的汤碗物品栈（优先专属碗，否则走通用汤碗；未初始化通用汤碗时返回空栈）。 */
    public static ItemStack bowlStack(@Nullable SoupDef def) {
        return SoupBowlItem.createFor(def);
    }

    /** 按完整 ID 查；没有返回 null。 */
    public static SoupDef get(ResourceLocation id) {
        return active().get(id);
    }

    /** 按 ID 文本查；不带命名空间（没有冒号）的一律按火锅命名空间解释。查不到或写法非法返回 null。 */
    public static SoupDef get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String text = id.indexOf(':') >= 0 ? id : HotPot.MOD_ID + ":" + id;
        ResourceLocation location = ResourceLocation.tryParse(text);
        return location == null ? null : get(location);
    }

    /**
     * 按序号 / 索引取汤底；负数（-1 = 没汤）或超出范围返回 null。
     * 内置项的序号就是旧存档里的汤底序号（清汤 0、高汤 1、菌汤 2、红汤 3、番茄 4、三鲜 5）。
     */
    public static SoupDef byIndex(int index) {
        return active().byIndex(index);
    }

    /** 某个汤底的序号；传 null 或没登记过的返回 -1。 */
    public static int indexOf(SoupDef def) {
        if (def == null) {
            return -1;
        }
        return active().indexOf(def);
    }

    /** 清汤（序号 0）。锅底料只能加进清汤这条旧规则要拿它比较。 */
    public static SoupDef clear() {
        return byIndex(0);
    }

    /**
     * 手里这件能不能当汤底倒入锅中（含第三方登记的来源），能返回对应汤底，否则 null。
     * 先按登记顺序逐种比对原料，再查第三方来源表（{@link SoupBase#registerSource}）。
     */
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

    /** 全部汤底，按登记顺序（前 6 项是内置项，顺序 = 旧存档序号）。 */
    public static List<SoupDef> all() {
        return active().all();
    }

    /** 内置汤底（旧存档序号 0～5 对应的那 6 项）。 */
    public static List<SoupDef> builtins() {
        ensureBuiltins();
        return TABLE.builtins();
    }

    public static int size() {
        return active().size();
    }

    /** 冻结（幂等）。服务端开始启动时由 {@link HotPotHooks#freezeRegistries()} 调用。 */
    static void freeze() {
        ensureBuiltins();
        TABLE.freeze();
    }

    public static boolean isFrozen() {
        return TABLE.isFrozen();
    }

    private static void ensureBuiltins() {
        if (!builtinsReady) {
            SoupBase.values();   // 触发 SoupBase 的静态初始化（同一线程正在初始化时直接返回）
        }
    }

    /** 4F-1 / 4F-2（mryt233）：独立登记表实例；主表与单测隔离实例共用，支持数据包配方叠加视图。 */
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

        synchronized View overlay(List<RecipeHolder<SoupBaseRecipe>> recipes, Consumer<String> warn) {
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
            for (RecipeHolder<SoupBaseRecipe> holder : recipes) {
                ResourceLocation target = SoupBaseRecipe.targetId(holder.id());
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
