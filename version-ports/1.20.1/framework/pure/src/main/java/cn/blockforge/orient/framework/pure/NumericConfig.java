package cn.blockforge.orient.framework.pure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 纯数值配置解析器：不访问游戏、加载器或文件。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4/W5）：警告改为 {@link ConfigWarning}（语言键 + 参数）；
 * 结果只在构造时复制一次，{@link Result#data()} 返回的对象视为只读。
 */
public final class NumericConfig {
    public record Rule(BigDecimal min, BigDecimal max, boolean integer) {
        public Rule(double min, double max, boolean integer) {
            this(BigDecimal.valueOf(min), BigDecimal.valueOf(max), integer);
        }
    }

    public interface Rules {
        Rule forPath(String path);
    }

    /** 解析结果；{@code data} 已是独立副本，调用方只读，不要修改。 */
    public record Result(JsonObject data, List<ConfigWarning> warnings, boolean validDocument) {
        public Result {
            data = data.deepCopy();
            warnings = List.copyOf(warnings);
        }
    }

    private NumericConfig() {
    }

    public static Result parse(String text, JsonObject defaults, Rules rules, int format) {
        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (RuntimeException error) {
            return invalid(defaults, ConfigWarning.of("json_invalid",
                "Config is not valid JSON; using defaults, original file kept"));
        }
        if (root == null || !root.isJsonObject()) {
            return invalid(defaults, ConfigWarning.of("root_not_object",
                "Config root must be an object; using defaults, original file kept"));
        }
        JsonObject input = root.getAsJsonObject();
        if (input.has("_format")) {
            try {
                JsonElement value = input.get("_format");
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                    || value.getAsBigDecimal().intValueExact() != format) {
                    return invalid(defaults, ConfigWarning.of("format_unsupported",
                        "Unsupported config format version; using defaults, original file kept"));
                }
            } catch (RuntimeException error) {
                return invalid(defaults, ConfigWarning.of("format_invalid",
                    "Invalid config format version; using defaults, original file kept"));
            }
        }
        var warnings = new ArrayList<ConfigWarning>();
        JsonObject output = defaults.deepCopy();
        overlay(input, output, "", rules, warnings);
        return new Result(output, warnings, true);
    }

    private static Result invalid(JsonObject defaults, ConfigWarning warning) {
        return new Result(defaults, List.of(warning), false);
    }

    private static void overlay(JsonObject input, JsonObject output, String prefix,
                                Rules rules, List<ConfigWarning> warnings) {
        for (var entry : input.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) {
                continue;
            }
            String path = prefix + key;
            JsonElement base = output.get(key);
            JsonElement candidate = entry.getValue();
            if (base == null) {
                warnings.add(ConfigWarning.of("unknown_key", "Unknown config entry %s, ignored", path));
            } else if (base.isJsonObject()) {
                if (!candidate.isJsonObject()) {
                    warnings.add(ConfigWarning.of("not_object", "%s must be an object; default kept", path));
                } else {
                    overlay(candidate.getAsJsonObject(), base.getAsJsonObject(), path + ".", rules, warnings);
                }
            } else {
                Rule rule = rules.forPath(path);
                if (rule == null) {
                    throw new IllegalStateException("配置字段缺少校验规则：" + path);
                }
                ConfigWarning problem = check(path, candidate, rule);
                if (problem == null) {
                    output.add(key, candidate.deepCopy());
                } else {
                    warnings.add(problem);
                }
            }
        }
    }

    /** 合法返回 null，否则返回对应警告（该项回退默认值）。 */
    private static ConfigWarning check(String path, JsonElement candidate, Rule rule) {
        if (!candidate.isJsonPrimitive() || !candidate.getAsJsonPrimitive().isNumber()) {
            return ConfigWarning.of("not_number", "%s must be a number; default used", path);
        }
        BigDecimal number;
        try {
            number = candidate.getAsBigDecimal();
        } catch (RuntimeException error) {
            return ConfigWarning.of("not_number", "%s must be a number; default used", path);
        }
        if (rule.integer() && number.stripTrailingZeros().scale() > 0) {
            return ConfigWarning.of("not_integer", "%s must be a whole number; default used", path);
        }
        if (number.compareTo(rule.min()) < 0 || number.compareTo(rule.max()) > 0) {
            return ConfigWarning.of("out_of_range", "%s must be between %s and %s; default used",
                path, rule.min().stripTrailingZeros().toPlainString(), rule.max().stripTrailingZeros().toPlainString());
        }
        return null;
    }
}
