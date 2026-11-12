// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 4C 内置蘸料登记表；序号及掩码兼容旧存档。作者：mryt233 */
public final class Seasonings {
    private static final Registry TABLE = createBuiltins();
    private Seasonings() { }
    private static Registry createBuiltins() {
        Registry registry = new Registry();
        for (Seasoning value : Seasoning.ALL) registry.register(value.def());
        return registry;
    }
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
        private final List<SeasoningDef> ordered = new ArrayList<>();
        private final Map<Item, SeasoningDef> raws = new HashMap<>();
        private volatile List<SeasoningDef> snapshot = List.of();
        synchronized void register(SeasoningDef def) {
            if (values.containsKey(def.id())) throw new IllegalArgumentException("蘸料 ID 重复：" + def.id());
            List<Item> sources = def.raws();
            values.put(def.id(), def);
            ordered.add(def);
            for (Item raw : sources) raws.putIfAbsent(raw, def);
            snapshot = List.copyOf(ordered);
        }
        List<SeasoningDef> all() { return snapshot; }
        SeasoningDef byIndex(int index) { List<SeasoningDef> list = snapshot; return index >= 0 && index < list.size() ? list.get(index) : null; }
        synchronized SeasoningDef byId(String id) { return id == null ? null : values.get(id); }
        synchronized SeasoningDef raw(Item item) { return raws.get(item); }
        synchronized void freeze() { values.freeze(); }
    }
}
