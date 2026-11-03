package cn.blockforge.orient.framework.pure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用数值配置内核（从火锅 Config 抽出；与 Minecraft / 加载器无关）。
 * 只负责：默认值表、解析校验、服务端/客户端快照、补全文件；线程归属由 {@link #serverData()}/{@link #clientData()} 使用方决定。
 * 每个内容模块一个实例，状态互不共享。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4/W5）：警告带语言键；快照只复制一次，取出的对象只读；
 * 客户端快照加载后即为默认值（以前先是空对象、断线后才是默认值）。
 */
public final class ConfigStore {
    /**
     * 一次读取的结果；{@code data} 是独立副本，只读。{@code details} 是带语言键的警告（新代码用它按玩家语言显示）；
     * {@link #warnings()} 是英文文本，保留给 Runtime 0.1.1 时编译的旧内容包（二进制兼容，0.2.0 再收紧）。
     */
    public record Loaded(JsonObject data, List<ConfigWarning> details) {
        public Loaded {
            details = List.copyOf(details);
        }

        public List<String> warnings() {
            return details.stream().map(ConfigWarning::text).toList();
        }
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final String fileName;
    private final int format;
    private final long maxBytes;
    private final String note;
    private final NumericConfig.Rules rules;
    private final Supplier<JsonObject> fieldHelp;
    private final Consumer<String> warn;

    private final JsonObject defaults = new JsonObject();
    private volatile JsonObject serverConfig = new JsonObject();
    private volatile JsonObject clientConfig = new JsonObject();
    private Path path;
    private boolean ready;

    public ConfigStore(String fileName, int format, long maxBytes, String note,
                       NumericConfig.Rules rules, Supplier<JsonObject> fieldHelp, Consumer<String> warn) {
        this.fileName = fileName;
        this.format = format;
        this.maxBytes = maxBytes;
        this.note = note;
        this.rules = rules;
        this.fieldHelp = fieldHelp;
        this.warn = warn;
    }

    public synchronized void init(Path directory) {
        this.path = directory.resolve(fileName);
    }

    public JsonObject defaults() {
        return defaults;
    }

    /** 注册完成后首次读取；默认值表为空时禁止调用。 */
    public synchronized void load() {
        if (!ready) {
            if (defaults.size() == 0) {
                throw new IllegalStateException("必须先注册数值默认值，再加载配置");
            }
            clientConfig = defaults.deepCopy();
            reload();
            // 读取成功才算加载过；失败（如没 init）时下次 load() 还会重试（单元测试发现，mryt233 改 2026-11-01）
            ready = true;
        }
    }

    public synchronized Loaded reload() {
        if (path == null) {
            throw new IllegalStateException("配置目录未初始化：先调用 init(directory)");
        }
        Loaded result = read(path);
        serverConfig = result.data();
        for (ConfigWarning message : result.details()) {
            warn.accept(message.text());
        }
        return result;
    }

    public Loaded read(Path file) {
        if (!Files.exists(file)) {
            return new Loaded(defaults.deepCopy(), List.of());
        }
        try {
            if (Files.size(file) > maxBytes) {
                return new Loaded(defaults.deepCopy(), List.of(ConfigWarning.of("too_large",
                    "Config file is too large; using defaults, original file kept")));
            }
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException error) {
            return new Loaded(defaults.deepCopy(), List.of(ConfigWarning.of("read_failed",
                "Could not read config (%s); using defaults, original file kept", error.getMessage())));
        }
    }

    public Loaded parse(String text) {
        NumericConfig.Result parsed = NumericConfig.parse(text, defaults, rules, format);
        return new Loaded(parsed.data(), parsed.warnings());
    }

    public boolean isValidDocument(String text) {
        return NumericConfig.parse(text, defaults, rules, format).validDocument();
    }

    /** 当前服务端快照（只读，不要修改；要改请用 {@link #replaceServer}）。 */
    public JsonObject serverData() {
        return serverConfig;
    }

    /** 当前客户端快照（只读）。 */
    public JsonObject clientData() {
        return clientConfig;
    }

    public void resetServerToDefaults() {
        serverConfig = defaults.deepCopy();
    }

    public void replaceServer(JsonObject data) {
        serverConfig = data.deepCopy();
    }

    public void acceptClient(JsonObject data) {
        clientConfig = parse(data.toString()).data();
    }

    public void clearClient() {
        clientConfig = defaults.deepCopy();
    }

    /** 补全有效文件，保留用户值/未知键；无效文档不重写。写入失败不破坏旧文件。 */
    public synchronized void saveDefaultsIfNeeded() {
        Path file = path;
        Path temp = null;
        try {
            JsonObject existing = new JsonObject();
            if (Files.exists(file)) {
                if (Files.size(file) > maxBytes) return;
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (!isValidDocument(text)) return;
                existing = JsonParser.parseString(text).getAsJsonObject();
            }
            JsonObject out = merge(defaults, existing);
            out.addProperty("_format", format);
            out.addProperty("_说明", note);
            out.add("_字段说明", fieldHelp.get());
            String text = gson.toJson(out) + "\n";
            if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(text)) return;
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "config-", ".tmp");
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            warn.accept(ConfigWarning.of("write_failed", "Could not write config (%s); original file kept",
                error.getMessage()).text());
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }

    public static JsonObject merge(JsonObject defaults, JsonObject input) {
        JsonObject out = input.deepCopy();
        for (var entry : defaults.entrySet()) {
            JsonElement old = out.get(entry.getKey());
            if (old == null) {
                out.add(entry.getKey(), entry.getValue().deepCopy());
            } else if (old.isJsonObject() && entry.getValue().isJsonObject()) {
                out.add(entry.getKey(), merge(entry.getValue().getAsJsonObject(), old.getAsJsonObject()));
            }
        }
        return out;
    }

    public static JsonObject read(JsonObject root, String... path) {
        JsonObject cur = root;
        for (String key : path) {
            JsonElement next = cur == null ? null : cur.get(key);
            cur = next != null && next.isJsonObject() ? next.getAsJsonObject() : null;
        }
        return cur;
    }

    public static JsonObject make(JsonObject root, String... path) {
        JsonObject cur = root;
        for (String key : path) {
            if (!cur.has(key)) cur.add(key, new JsonObject());
            cur = cur.getAsJsonObject(key);
        }
        return cur;
    }

    public static int intAt(JsonObject node, String key, int fallback) {
        return node == null || !node.has(key) ? fallback : node.get(key).getAsInt();
    }

    public static float floatAt(JsonObject node, String key, float fallback) {
        return node == null || !node.has(key) ? fallback : node.get(key).getAsFloat();
    }
}