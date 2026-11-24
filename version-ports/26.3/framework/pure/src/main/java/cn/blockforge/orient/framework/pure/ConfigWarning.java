package cn.blockforge.orient.framework.pure;

import java.util.List;

/**
 * 一条配置警告：语言键 + 英文兜底 + 参数（都已是字符串）。
 * 纯内核不碰 Minecraft，交给上层用 {@code Component.translatableWithFallback} 按玩家语言显示；
 * 写日志时用 {@link #text()}（英文兜底按参数填好）。
 * 作者：mryt233（2026-11-01，前置检查 W5：警告不再写死中文）
 */
public record ConfigWarning(String key, String fallback, List<String> args) {
    public static final String PREFIX = "orient_framework.config.";

    public ConfigWarning {
        args = List.copyOf(args);
    }

    static ConfigWarning of(String name, String fallback, Object... args) {
        String[] text = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            text[i] = String.valueOf(args[i]);
        }
        return new ConfigWarning(PREFIX + name, fallback, List.of(text));
    }

    /** 英文兜底文本，参数按 %s 顺序填入（日志用）。 */
    public String text() {
        return String.format(fallback, args.toArray());
    }

    @Override
    public String toString() {
        return text();
    }
}
