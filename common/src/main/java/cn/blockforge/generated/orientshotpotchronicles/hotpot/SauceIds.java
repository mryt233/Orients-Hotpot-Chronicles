// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 4E：蘸料组件里的调料 ID 列表。规则：去重；内置 15 种按旧序号、其余按完整 ID 字典序排在后面；
 * 旧位掩码按「位号 → 内置 ID」翻译，15 位以外的高位丢弃并记日志。未知 ID 原样保留，不参与效果。
 */
public final class SauceIds {
    private static final List<String> BUILTIN = new ArrayList<>();
    private static final Map<String, Integer> ORDER = new HashMap<>();
    static {
        for (Seasoning s : Seasoning.ALL) {
            String id = full(s.def().id());
            ORDER.put(id, BUILTIN.size());
            BUILTIN.add(id);
        }
    }
    private SauceIds() { }

    /** 内置调料的完整 ID：{@code orients_hotpot_chronicles:honey}；已带命名空间的原样返回。 */
    public static String full(String id) {
        return id.indexOf(':') >= 0 ? id : HotPot.MOD_ID + ":" + id;
    }

    public static String fullId(SeasoningDef def) { return full(def.id()); }

    public static int builtinCount() { return BUILTIN.size(); }
    public static String legacyId(int index) { return index >= 0 && index < BUILTIN.size() ? BUILTIN.get(index) : ""; }

    /** 规范化：去空、去重，按固定序排序。 */
    public static List<String> normalize(Collection<String> ids) {
        TreeSet<String> set = new TreeSet<>(Comparator
            .comparingInt((String s) -> ORDER.getOrDefault(s, Integer.MAX_VALUE))
            .thenComparing(Comparator.naturalOrder()));
        for (String id : ids) {
            if (id != null && !id.isEmpty()) set.add(full(id));
        }
        return List.copyOf(set);
    }

    /** 旧存档位掩码 → ID 列表；超出内置范围的高位丢弃并记日志。 */
    public static List<String> fromMask(int mask) {
        int known = (1 << BUILTIN.size()) - 1;
        if ((mask & ~known) != 0) {
            HotPot.LOGGER.warn("蘸料旧位掩码 {} 含内置范围以外的高位，已丢弃 {}", mask, mask & ~known);
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < BUILTIN.size(); i++) {
            if ((mask & (1 << i)) != 0) out.add(BUILTIN.get(i));
        }
        return List.copyOf(out);
    }

    /** 查调料定义；未知返回 null。完整 ID 与内置短名都认。 */
    public static SeasoningDef def(String id) {
        SeasoningDef def = Seasonings.get(id);
        String prefix = HotPot.MOD_ID + ":";
        if (def == null && id != null && id.startsWith(prefix)) def = Seasonings.get(id.substring(prefix.length()));
        return def;
    }

    /** ID 列表里能认出的定义，保持列表顺序。 */
    public static List<SeasoningDef> defs(Collection<String> ids) {
        return defs(ids, SauceIds::def);
    }

    static List<SeasoningDef> defs(Collection<String> ids, Function<String, SeasoningDef> resolver) {
        List<SeasoningDef> out = new ArrayList<>();
        for (String id : normalize(ids)) {
            SeasoningDef def = resolver.apply(id);
            if (def != null) out.add(def);
        }
        return out;
    }

    public static boolean containsDef(Collection<String> ids, Seasoning s) {
        return normalize(ids).contains(fullId(s.def()));
    }

    /** 已含全部内置调料。 */
    public static boolean hasAllBuiltins(Collection<String> ids) {
        return normalize(ids).containsAll(BUILTIN);
    }
}
