// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.Field;

/** 作者：mryt233；2026-11-11：设置数值读写、默认值与输入校验。 */
public final class ConfigValues {
    private final java.util.function.Supplier<JsonObject> edited;
    private final java.util.function.Supplier<JsonObject> defaults;
    private JsonObject defaultsCache;
    private final Map<String, Double> defaultValueCache = new HashMap<>();
    public ConfigValues(java.util.function.Supplier<JsonObject> edited, java.util.function.Supplier<JsonObject> defaults) {
        this.edited = edited; this.defaults = defaults;
    }
    public void clearDefaults() { defaultValueCache.clear(); }
    public void nudge(Field f, int dir, boolean shift) {
        double step = f.step() * (shift ? 10 : 1) * dir;
        setValue(f, value(f) + step);
    }
    public double value(Field f) {
        JsonObject parentNode = node(edited.get(), f.path(), f.path().length - 1);
        JsonElement el = parentNode.get(f.path()[f.path().length - 1]);
        return el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()
            ? f.min() : el.getAsDouble();
    }

    public void setValue(Field f, double raw) {
        double v = Math.max(f.min(), Math.min(f.max(), raw));
        JsonObject parentNode = node(edited.get(), f.path(), f.path().length - 1);
        String key = f.path()[f.path().length - 1];
        if (f.integer()) {
            parentNode.addProperty(key, (int) Math.round(v));
        } else {
            parentNode.addProperty(key, Math.round(v * 100.0) / 100.0);
        }
    }

    /** 默认值从配置模板取（不在这里另写一份，改默认值只要改 rememberXxx 那一处）。 */
    public double defaultValue(Field f) {
        Double cached = defaultValueCache.get(f.key());
        if (cached != null) {
            return cached;
        }
        if (defaultsCache == null) {
            defaultsCache = defaults.get();
        }
        JsonObject parentNode = node(defaultsCache, f.path(), f.path().length - 1);
        JsonElement el = parentNode.get(f.path()[f.path().length - 1]);
        double v = el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()
            ? el.getAsDouble() : f.min();
        defaultValueCache.put(f.key(), v);
        return v;
    }

    public boolean isDefault(Field f) {
        return Math.abs(value(f) - defaultValue(f)) < 0.0005;
    }

    /** 取出（必要时创建）配置路径的前 upto 段对象。 */
    public static JsonObject node(JsonObject root, String[] path, int upto) {
        JsonObject cur = root;
        for (int i = 0; i < upto; i++) {
            JsonElement el = cur.get(path[i]);
            if (el == null || !el.isJsonObject()) {
                JsonObject next = new JsonObject();
                cur.add(path[i], next);
                cur = next;
            } else {
                cur = el.getAsJsonObject();
            }
        }
        return cur;
    }

    public static String formatValue(Field f, double v) {
        return f.integer() ? String.valueOf((int) Math.round(v)) : trim(v);
    }

    /** 小数去掉多余的尾零（0.85 保留，5.00 → 5）。 */
    public static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    /** 不是数字 / 不是整数 / 超出范围都算不合法。 */
    public static Double parse(Field f, String text) {
        if (f == null || text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(trimmed);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            if (f.integer() && Math.abs(v - Math.rint(v)) > 1e-6) {
                return null;
            }
            if (v < f.min() - 1e-6 || v > f.max() + 1e-6) {
                return null;
            }
            return v;
        } catch (NumberFormatException error) {
            return null;
        }
    }

}
