package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 汤底登记表：ID → {@link SoupDef}，沿用第三批的 {@link FrozenRegistry}（启动期可写、服务端启动时冻结）。
 *
 * <p>第四批 4B-1（mryt233，2026-11-11）立表：{@link SoupBase} 的六个枚举值在类初始化时按顺序登记成表里的
 * <b>前 6 项</b>（顺序 = 旧存档里的汤底序号 0～5），内置项永远占前 6 项；以后新增的汤底（4F）排在后面。
 * 4B-2：游戏里「按序号取汤底 / 遍历全部汤底 / 按名字查汤底 / 手里这件是不是汤底原料」的调用点全部改成问这张表，
 * 存档、网络、配置仍然按序号 / 短名工作，行为与改动前完全相同。</p>
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
    private static final FrozenRegistry<ResourceLocation, SoupDef> REGISTRY = new FrozenRegistry<>(
        "汤底登记表", "汤底要在启动期登记（子模组在自己的兼容模块里登记），冻结后不再接受登记。");
    /** 内置项（前 6 项）的顺序副本；只在 {@link SoupBase} 静态初始化期间写。 */
    private static final List<SoupDef> BUILTIN = new ArrayList<>();
    /** 按登记顺序排列的快照（每次登记整体替换）。 */
    private static volatile SoupDef[] ordered = new SoupDef[0];
    private static volatile Map<ResourceLocation, Integer> indexById = Map.of();
    /** 内置项登记完了没有（让热路径上的 ensureBuiltins 不必每次都碰枚举）。 */
    private static volatile boolean builtinsReady;

    private Soups() {
    }

    /** 内置项登记：只给 {@link SoupBase} 的静态初始化用，按枚举顺序调用。 */
    static synchronized void registerBuiltin(SoupDef def) {
        if (REGISTRY.containsKey(def.id())) {
            throw new IllegalStateException("内置汤底 ID 重复：" + def.id());
        }
        appendLocked(def);
        BUILTIN.add(def);
    }

    /** {@link SoupBase} 登记完全部内置项后调用一次。 */
    static void builtinsDone() {
        builtinsReady = true;
    }

    /** 内置项还没登记完时也能用（给 {@link SoupVisual} 在 {@link SoupBase} 初始化期间核对用）。 */
    static synchronized List<SoupDef> builtinsSoFar() {
        return List.copyOf(BUILTIN);
    }

    /**
     * 登记一种新汤底（包可见：公开入口留给 4F-1）。ID 不能与已有项重复；冻结后抛 {@link IllegalStateException}。
     */
    static void register(SoupDef def) {
        ensureBuiltins();   // 不能放进下面的同步块：SoupBase 初始化时也要拿这把锁，两边先后顺序不同会死锁
        registerLocked(def);
    }

    private static synchronized void registerLocked(SoupDef def) {
        if (REGISTRY.containsKey(def.id())) {
            throw new IllegalArgumentException("汤底 ID 已被登记：" + def.id());
        }
        appendLocked(def);
    }

    private static void appendLocked(SoupDef def) {
        REGISTRY.put(def.id(), def);   // 冻结后这里抛 IllegalStateException，快照不变
        SoupDef[] next = Arrays.copyOf(ordered, ordered.length + 1);
        next[next.length - 1] = def;
        Map<ResourceLocation, Integer> ids = new HashMap<>(indexById);
        ids.put(def.id(), next.length - 1);
        ordered = next;
        indexById = Map.copyOf(ids);
    }

    /** 按完整 ID 查；没有返回 null。 */
    public static SoupDef get(ResourceLocation id) {
        ensureBuiltins();
        return REGISTRY.get(id);
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
        ensureBuiltins();
        SoupDef[] snapshot = ordered;
        return index >= 0 && index < snapshot.length ? snapshot[index] : null;
    }

    /** 某个汤底的序号；传 null 或没登记过的返回 -1。 */
    public static int indexOf(SoupDef def) {
        if (def == null) {
            return -1;
        }
        ensureBuiltins();
        Integer index = indexById.get(def.id());
        return index == null ? -1 : index;
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
        ensureBuiltins();
        for (SoupDef def : ordered) {
            if (def.matchesSource(stack)) {
                return def;
            }
        }
        SoupBase custom = SoupBase.customSource(stack);
        return custom == null ? null : custom.def();
    }

    /** 全部汤底，按登记顺序（前 6 项是内置项，顺序 = 旧存档序号）。 */
    public static List<SoupDef> all() {
        ensureBuiltins();
        return List.of(ordered);
    }

    /** 内置汤底（旧存档序号 0～5 对应的那 6 项）。 */
    public static List<SoupDef> builtins() {
        ensureBuiltins();
        return builtinsSoFar();
    }

    public static int size() {
        ensureBuiltins();
        return ordered.length;
    }

    /** 冻结（幂等）。服务端开始启动时由 {@link HotPotHooks#freezeRegistries()} 调用。 */
    static void freeze() {
        ensureBuiltins();
        REGISTRY.freeze();
    }

    public static boolean isFrozen() {
        return REGISTRY.isFrozen();
    }

    private static void ensureBuiltins() {
        if (!builtinsReady) {
            SoupBase.values();   // 触发 SoupBase 的静态初始化（同一线程正在初始化时直接返回）
        }
    }
}
