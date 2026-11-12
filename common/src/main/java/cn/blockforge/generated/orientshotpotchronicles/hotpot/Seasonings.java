// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 4C / 4F-1 蘸料登记表；内置 15 种占前 0～14 固定位，扩展项按完整 ID 排在后面。作者：mryt233 */
public final class Seasonings {
    private static final Registry TABLE = createBuiltins();
    private Seasonings() { }
    private static Registry createBuiltins() {
        Registry registry = new Registry();
        for (Seasoning value : Seasoning.ALL) registry.registerBuiltin(value.def());
        return registry;
    }
    /** 4F-1（mryt233）：公开蘸料登记入口（启动期调用，冻结后抛 {@link IllegalStateException}；ID 重复抛 {@link IllegalArgumentException}）。 */
    public static void register(SeasoningDef def) { TABLE.register(Objects.requireNonNull(def, "def")); }
    public static List<SeasoningDef> all() { return TABLE.all(); }
    public static int size() { return TABLE.all().size(); }
    public static int indexOf(SeasoningDef def) { return def == null ? -1 : TABLE.all().indexOf(def); }
    public static SeasoningDef byIndex(int index) { return TABLE.byIndex(index); }
    public static SeasoningDef ofRaw(ItemStack stack) { return stack.isEmpty() ? null : TABLE.raw(stack.getItem()); }
    public static SeasoningDef get(String id) { return TABLE.byId(id); }
    static void freeze() { TABLE.freeze(); }
    public static boolean isFrozen() { return TABLE.values.isFrozen(); }

    /** 独立实例供登记规则测试使用，不污染进程里的内置表。 */
    static final class Registry {
        private final FrozenRegistry<String, SeasoningDef> values = new FrozenRegistry<>("蘸料登记表", "蘸料只能在启动期登记");
        private final List<SeasoningDef> builtins = new ArrayList<>();
        private final List<SeasoningDef> extras = new ArrayList<>();
        private final Map<Item, SeasoningDef> raws = new HashMap<>();
        private boolean rawsBuilt;
        private volatile List<SeasoningDef> snapshot = List.of();
        synchronized void registerBuiltin(SeasoningDef def) {
            String key = SauceIds.full(def.id());
            if (values.containsKey(key)) throw new IllegalArgumentException("蘸料 ID 重复：" + def.id());
            values.put(key, def);
            builtins.add(def);
            rebuildSnapshot();
        }
        synchronized void register(SeasoningDef def) {
            String key = SauceIds.full(def.id());
            if (values.containsKey(key)) throw new IllegalArgumentException("蘸料 ID 重复：" + def.id());
            values.put(key, def);
            extras.add(def);
            extras.sort(Comparator.comparing(d -> SauceIds.full(d.id())));
            rebuildSnapshot();
        }
        private void rebuildSnapshot() {
            List<SeasoningDef> merged = new ArrayList<>(builtins.size() + extras.size());
            merged.addAll(builtins);
            merged.addAll(extras);
            snapshot = List.copyOf(merged);
            rawsBuilt = false;
            raws.clear();
        }
        List<SeasoningDef> all() { return snapshot; }
        SeasoningDef byIndex(int index) { List<SeasoningDef> list = snapshot; return index >= 0 && index < list.size() ? list.get(index) : null; }
        synchronized SeasoningDef byId(String id) { return id == null || id.isEmpty() ? null : values.get(SauceIds.full(id)); }
        synchronized SeasoningDef raw(Item item) {
            if (!rawsBuilt) {
                raws.clear();
                for (SeasoningDef entry : snapshot) {
                    for (Item raw : entry.raws()) raws.putIfAbsent(raw, entry);
                }
                rawsBuilt = true;
            }
            return raws.get(item);
        }
        synchronized void freeze() { values.freeze(); }
    }
}
