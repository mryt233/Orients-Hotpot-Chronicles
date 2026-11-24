package cn.blockforge.orient.framework.pure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 纯数值与基础类型配置解析器：不访问游戏、加载器或文件。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4/W5）：警告改为 {@link ConfigWarning}（语言键 + 参数）；
 * 结果只在构造时复制一次，{@link Result#data()} 返回的对象视为只读。
 * mryt233 改（2026-11-15）：新增 {@link TypedRules}/{@link ValueValidator}、布尔与枚举字符串校验，
 * 保留既有 {@link Rules} 与四参 {@link #parse(String, JsonObject, Rules, int)} 二进制兼容。
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

    /** 通用字段校验器：合法返回 {@code null}，否则返回对应警告（该项回退默认值）。 */
    @FunctionalInterface
    public interface ValueValidator {
        ConfigWarning validate(JsonElement candidate);
    }

    /** 按配置路径提供非数值（或自定义）校验规则；返回 {@code null} 时走 {@link Rules}。 */
    @FunctionalInterface
    public interface TypedRules {
        ValueValidator validatorFor(String path);
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
        return parse(text, defaults, rules, null, format);
    }

    public static Result parse(String text, JsonObject defaults, Rules rules, TypedRules typedRules, int format) {
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
        overlay(input, output, "", rules, typedRules, warnings);
        return new Result(output, warnings, true);
    }

    private static Result invalid(JsonObject defaults, ConfigWarning warning) {
        return new Result(defaults, List.of(warning), false);
    }

    private static void overlay(JsonObject input, JsonObject output, String prefix,
                                Rules rules, TypedRules typedRules, List<ConfigWarning> warnings) {
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
                if (candidate == null || !candidate.isJsonObject()) {
                    warnings.add(ConfigWarning.of("not_object", "%s must be an object; default kept", path));
                } else {
                    overlay(candidate.getAsJsonObject(), base.getAsJsonObject(), path + ".", rules, typedRules, warnings);
                }
            } else {
                ValueValidator validator = typedRules == null ? null : typedRules.validatorFor(path);
                ConfigWarning problem;
                if (validator != null) {
                    problem = validator.validate(candidate);
                } else {
                    Rule rule = rules == null ? null : rules.forPath(path);
                    if (rule == null) {
                        throw new IllegalStateException("配置字段缺少校验规则：" + path);
                    }
                    problem = check(path, candidate, rule);
                }
                if (problem == null) {
                    output.add(key, candidate.deepCopy());
                } else {
                    warnings.add(problem);
                }
            }
        }
    }

    /** 校验布尔配置项；合法返回 {@code null}。 */
    public static ConfigWarning checkBoolean(String path, JsonElement candidate) {
        if (candidate == null || !candidate.isJsonPrimitive() || !candidate.getAsJsonPrimitive().isBoolean()) {
            return ConfigWarning.of("not_boolean", "%s must be a boolean; default used", path);
        }
        return null;
    }

    /** 校验枚举字符串配置项；合法返回 {@code null}。 */
    public static ConfigWarning checkChoice(String path, JsonElement candidate, String... allowed) {
        if (candidate == null || !candidate.isJsonPrimitive() || !candidate.getAsJsonPrimitive().isString()) {
            return ConfigWarning.of("not_string", "%s must be a string; default used", path);
        }
        String value = candidate.getAsString();
        for (String option : allowed) {
            if (option.equals(value)) {
                return null;
            }
        }
        return ConfigWarning.of("invalid_choice", "%s must be one of [%s]; default used",
            path, String.join(", ", allowed));
    }

    /** 合法返回 null，否则返回对应警告（该项回退默认值）。 */
    private static ConfigWarning check(String path, JsonElement candidate, Rule rule) {
        if (candidate == null || !candidate.isJsonPrimitive() || !candidate.getAsJsonPrimitive().isNumber()) {
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
