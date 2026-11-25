// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** 供数据包与子模组共用的备菜架规则，不接受仅能在一端执行的回调。 */
public record RackRuleDef(Identifier id, Target target, String match, int priority,
                          Patch base, Map<PrepRackBlock.Kind, Patch> overrides) {
    public static final int STACK_LIMIT = 64;
    public static final int VISUAL_LIMIT = 16;
    public enum Target { ITEM, TAG, CATEGORY }
    public enum Layout { ROW_DEPTH, FLAT_PILE, UPRIGHT, SINGLE, HANGING }
    public enum Category { FOOD, BLOCK, OTHER }

    public RackRuleDef {
        Objects.requireNonNull(id);
        Objects.requireNonNull(target);
        Objects.requireNonNull(base);
        Objects.requireNonNull(match);
        if (id.toString().length() > 128 || match.length() > 128 || priority < -1000 || priority > 1000) {
            throw new IllegalArgumentException("rule id, selector or priority out of bounds");
        }
        match = switch (target) {
            case ITEM, TAG -> Identifier.parse(match.startsWith("#") && target == Target.TAG
                ? match.substring(1) : match).toString();
            case CATEGORY -> Category.valueOf(match.toUpperCase(Locale.ROOT)).name();
        };
        overrides = Map.copyOf(overrides);
    }

    /** null 表示未覆写；数值在入口就验证，运行期只对实际栈与几何做夹取。 */
    public record Patch(Integer maxStack, Layout layout, Integer maxVisual, Float scale, Float spacing, Float tilt) {
        static final Patch EMPTY = new Patch(null, null, null, null, null, null);
        public Patch {
            check(maxStack, 1, STACK_LIMIT, "max_stack");
            check(maxVisual, 1, VISUAL_LIMIT, "max_visual");
            check(scale, .05F, .5F, "scale");
            check(spacing, 0, .25F, "spacing");
            check(tilt, -45, 45, "tilt");
        }

        private static void check(Number number, float min, float max, String key) {
            if (number != null && (!Float.isFinite(number.floatValue()) || number.floatValue() < min || number.floatValue() > max)) {
                throw new IllegalArgumentException(key + " out of bounds");
            }
        }

        private static <T> T pick(T base, T over) { return over == null ? base : over; }
        Patch overlay(Patch p) {
            if (p == null || p.equals(EMPTY)) return this;
            return new Patch(pick(maxStack,p.maxStack), pick(layout,p.layout), pick(maxVisual,p.maxVisual),
                pick(scale,p.scale), pick(spacing,p.spacing), pick(tilt,p.tilt));
        }

        JsonObject toJson() {
            JsonObject j = new JsonObject();
            if (maxStack != null) j.addProperty("max_stack", maxStack);
            if (layout != null) j.addProperty("layout", layout.name().toLowerCase(Locale.ROOT));
            if (maxVisual != null) j.addProperty("max_visual", maxVisual);
            if (scale != null) j.addProperty("scale", scale);
            if (spacing != null) j.addProperty("spacing", spacing);
            if (tilt != null) j.addProperty("tilt", tilt);
            return j;
        }
    }

    private static final Set<String> PATCH = Set.of("max_stack","layout","max_visual","scale","spacing","tilt");
    private static final Set<String> META = Set.of("item","tag","category","priority","overrides");

    static RackRuleDef parse(Identifier id, JsonObject json) {
        Target target = null;
        String match = null;
        for (Target t : Target.values()) {
            String key = t.name().toLowerCase(Locale.ROOT);
            if (json.has(key)) {
                if (target != null) throw new IllegalArgumentException("exactly one selector required");
                target = t;
                match = string(json.get(key));
            }
        }
        if (target == null) throw new IllegalArgumentException("no selector");
        var overrides = new EnumMap<PrepRackBlock.Kind, Patch>(PrepRackBlock.Kind.class);
        if (json.has("overrides")) for (var e : json.getAsJsonObject("overrides").entrySet()) {
            var kind = PrepRackBlock.Kind.valueOf(e.getKey().toUpperCase(Locale.ROOT));
            if (overrides.put(kind,parsePatch(e.getValue().getAsJsonObject(), false)) != null) {
                throw new IllegalArgumentException("duplicate rack override");
            }
        }
        return new RackRuleDef(id, target, match, json.has("priority") ? integer(json.get("priority")) : 0,
            parsePatch(json, true), overrides);
    }

    static Patch parsePatch(JsonObject j, boolean base) {
        for (String key : j.keySet()) if (!PATCH.contains(key) && !(base && META.contains(key))) {
            throw new IllegalArgumentException("unknown field: " + key);
        }
        return new Patch(j.has("max_stack") ? integer(j.get("max_stack")) : null,
            j.has("layout") ? Layout.valueOf(string(j.get("layout")).toUpperCase(Locale.ROOT)) : null,
            j.has("max_visual") ? integer(j.get("max_visual")) : null,
            decimal(j,"scale"), decimal(j,"spacing"), decimal(j,"tilt"));
    }

    static int integer(JsonElement v) {
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("integer required");
        return v.getAsBigDecimal().intValueExact();
    }

    private static String string(JsonElement v) {
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("string required");
        return v.getAsString();
    }

    private static Float decimal(JsonObject j, String key) {
        if (!j.has(key)) return null;
        var v = j.get(key);
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(key + " must be a number");
        return v.getAsFloat();
    }
}
