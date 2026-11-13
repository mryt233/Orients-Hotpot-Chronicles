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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用数值配置内核（从火锅 Config 抽出；与 Minecraft / 加载器无关）。
 * 只负责：默认值表、解析校验、服务端/客户端快照、补全文件；线程归属由 {@link #serverData()}/{@link #clientData()} 使用方决定。
 * 每个内容模块一个实例，状态互不共享。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4/W5）：警告带语言键；快照只复制一次，取出的对象只读；
 * 客户端快照加载后即为默认值（以前先是空对象、断线后才是默认值）。
 * mryt233 改（2026-11-07）：每份快照额外带一张「配置路径 → 数值」的扁平只读表（{@link Snapshot#numbers()}），
 * 取值从「按 . 拆路径逐层遍历 JsonObject」变成一次哈希查找；快照与扁平表永远成对换引用，
 * 所以单人游戏里集成服线程与渲染线程并发读取也不会读到「新树配旧表」的中间态。
 */
public final class ConfigStore {
    /** mryt233 改（2026-11-10）：明确区分持久化、仅内存生效与未应用。 */
    public enum SaveStatus { WRITTEN, MEMORY_ONLY, FAILED }

    public record Saved(Loaded loaded, SaveStatus status, String reason, Path backup) {
    }

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

    /**
     * 一份配置快照：对外只读的树 + 由它摊平出来的数值表。
     *
     * <p>mryt233 改（2026-11-07）：两者一起构建、一起用 volatile 引用换掉，读侧要么看到旧的一对、
     * 要么看到新的一对，不会出现「JsonObject 已换、扁平表还是旧的」这种撕裂。
     * 树依旧是 {@code JsonObject}（设置界面、同步包要用），{@code numbers} 是 {@link Map#copyOf} 出来的不可变表。</p>
     */
    private record Snapshot(JsonObject data, Map<String, Double> numbers) {
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
    private volatile Snapshot server = new Snapshot(new JsonObject(), Map.of());
    private volatile Snapshot client = new Snapshot(new JsonObject(), Map.of());
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
            client = snapshotOf(defaults.deepCopy());
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
        // mryt233 改（2026-11-07）：树与扁平表一起换掉，读侧不会再看到半新半旧的一对
        server = snapshotOf(result.data());
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
        return server.data();
    }

    /** 当前客户端快照（只读）。 */
    public JsonObject clientData() {
        return client.data();
    }

    /**
     * mryt233 改（2026-11-07）：取一条扁平化配置的数值，一次哈希查找。
     *
     * @param snapshot 必须是 {@link #serverData()} / {@link #clientData()} 返回的那一份（按引用识别，O(1)）；
     *                 认不出来时退回逐层遍历树，保证任何副本传进来也不会算错（只是慢一点）
     * @param path     扁平路径，例如 {@code food.cold_tofu.hunger}、{@code cooking.items.beef}
     * @param fallback 没有这一项（或这一项不是数字）时用默认值兜底
     */
    public double real(JsonObject snapshot, String path, double fallback) {
        Double value = numbersOf(snapshot).get(path);
        if (value == null) {
            value = slowLookup(snapshot, path);
        }
        return value == null ? fallback : value;
    }

    /**
     * 同 {@link #real}，按整数取。
     *
     * <p>截断方向与旧的 {@code JsonPrimitive#getAsInt()}（{@code Number#intValue()}）一致：
     * 整数值原样返回，万一有人把整数项写成 7.6 也仍是 7 而不是四舍五入成 8——
     * 整数项在解析期已经校验过「必须是整数」，这里的写法只是保证与旧行为逐位相同。</p>
     */
    public int integer(JsonObject snapshot, String path, int fallback) {
        Double value = numbersOf(snapshot).get(path);
        if (value == null) {
            value = slowLookup(snapshot, path);
        }
        return value == null ? fallback : (int) (double) value;
    }

    /**
     * 这份快照里有没有这一项（不看类型；用来区分「没配」和「配成默认值」）。
     * 语义与原来的 {@code ConfigStore.read(root, path...).has(key)} 一致：这一层里存在这个键就算有，
     * 哪怕是文字 / 对象而不是数字（那种情况 {@link #real} / {@link #integer} 会回退默认值，但这里仍然算「有」）。
     */
    public boolean contains(JsonObject snapshot, String path) {
        if (numbersOf(snapshot).containsKey(path)) {
            return true;
        }
        JsonElement leaf = walk(snapshot, path);
        return leaf != null;
    }

    /** 取快照对应的扁平表；只对当前这两份快照生效，其它对象返回空表（由调用方走慢路径）。 */
    private Map<String, Double> numbersOf(JsonObject snapshot) {
        Snapshot serverNow = server;
        if (serverNow.data() == snapshot) {
            return serverNow.numbers();
        }
        Snapshot clientNow = client;
        if (clientNow.data() == snapshot) {
            return clientNow.numbers();
        }
        return Map.of();
    }

    public void resetServerToDefaults() {
        server = snapshotOf(defaults.deepCopy());
    }

    public void replaceServer(JsonObject data) {
        server = snapshotOf(data.deepCopy());
    }

    /**
     * mryt233 改（2026-11-07）：与 {@link #clearClient()} 进同一把锁——断线那一瞬间「清回默认值」与
     * 「刚收到上一个服的同步包」可以同时在跑，以前两个写者没有互斥，最后谁赢不确定。
     */
    public synchronized void acceptClient(JsonObject data) {
        client = snapshotOf(parse(data.toString()).data());
    }

    public synchronized void clearClient() {
        client = snapshotOf(defaults.deepCopy());
    }

    /** mryt233 改（2026-11-10）：设置保存复用原子写入；坏文档必须先完整备份。 */
    public synchronized Saved saveEdited(JsonObject edited) {
        Path backup = null;
        JsonObject existing = new JsonObject();
        try {
            if (path != null && Files.exists(path)) {
                if (!Files.isRegularFile(path)) {
                    throw new IOException("Config path is not a regular file: " + path);
                }
                if (Files.size(path) > maxBytes) {
                    backup = backupBrokenDocument();
                } else {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    if (isValidDocument(text)) {
                        existing = JsonParser.parseString(text).getAsJsonObject();
                    } else {
                        backup = backupBrokenDocument();
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            warn.accept("Could not preserve original config: " + error.getMessage());
            return new Saved(new Loaded(serverData().deepCopy(), List.of()), SaveStatus.FAILED,
                error.toString(), backup);
        }
        JsonObject out = merge(merge(defaults, existing), edited == null ? new JsonObject() : edited);
        out.addProperty("_format", format);
        out.addProperty("_说明", note);
        out.add("_字段说明", fieldHelp.get());
        Loaded loaded = parse(out.toString());
        SaveStatus status = SaveStatus.MEMORY_ONLY;
        String reason = "Config directory is not initialized";
        if (path != null) {
            try {
                writeAtomically(path, gson.toJson(out) + "\n");
                status = SaveStatus.WRITTEN;
                reason = "";
            } catch (IOException | RuntimeException error) {
                reason = error.toString();
                warn.accept("Could not write config (" + reason + "); changes applied in memory only");
            }
        }
        server = snapshotOf(loaded.data());
        loaded.details().forEach(warning -> warn.accept(warning.text()));
        return new Saved(loaded, status, reason, backup);
    }

    private Path backupBrokenDocument() throws IOException {
        String stamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        Path backup = path.resolveSibling(path.getFileName() + ".broken-" + stamp);
        for (int suffix = 1; Files.exists(backup); suffix++) {
            backup = path.resolveSibling(path.getFileName() + ".broken-" + stamp + "-" + suffix);
        }
        Files.copy(path, backup);
        warn.accept("Invalid config backed up to " + backup);
        return backup;
    }

    /** mryt233 改（2026-11-10）：模板补全和主动保存共用一套安全替换规则。 */
    private static void writeAtomically(Path file, String text) throws IOException {
        Path temp = null;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            temp = Files.createTempFile(file.toAbsolutePath().getParent(), "config-", ".tmp");
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }

    /** 补全有效文件，保留用户值/未知键；无效文档不重写。写入失败不破坏旧文件。 */
    public synchronized void saveDefaultsIfNeeded() {
        Path file = path;
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
            writeAtomically(file, text);
        } catch (IOException error) {
            warn.accept(ConfigWarning.of("write_failed", "Could not write config (%s); original file kept",
                error.getMessage()).text());
        }
    }

    // ------------------------------------------------------------------ 快照与扁平表

    /** mryt233 改（2026-11-07）：把树摊平成「配置路径 → 数值」的不可变表，快照建立时算一次。 */
    private static Snapshot snapshotOf(JsonObject data) {
        return new Snapshot(data, flatten(data));
    }

    private static Map<String, Double> flatten(JsonObject root) {
        Map<String, Double> numbers = new HashMap<>(256);
        collect(root, "", numbers);
        return Map.copyOf(numbers);
    }

    /**
     * 递归收集数字项。键里出现 {@code .} 时路径会有歧义（食材 ID 由玩家 / 数据包决定），
     * 这与原有规则表按前缀 / 后缀匹配的口径一致，不是这次改动引入的。
     */
    private static void collect(JsonObject node, String prefix, Map<String, Double> out) {
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null) {
                continue;
            }
            String path = prefix + entry.getKey();
            if (value.isJsonObject()) {
                collect(value.getAsJsonObject(), path + ".", out);
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                try {
                    out.put(path, value.getAsDouble());
                } catch (RuntimeException ignored) {
                    // 解析期已经校验过范围与整数性，这里理论上不会失败；真失败就当作没这一项（走默认值）
                }
            }
        }
    }

    /** 兜底慢路径的取数：认不出来的副本（深拷贝、默认值副本等）按 {@code .} 拆开逐层找。 */
    private static Double slowLookup(JsonObject snapshot, String path) {
        JsonElement leaf = walk(snapshot, path);
        if (leaf == null || !leaf.isJsonPrimitive() || !leaf.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            return leaf.getAsDouble();
        } catch (RuntimeException error) {
            return null;
        }
    }

    /** 按 {@code .} 拆开逐层走；任一层不是对象或没有这个键时返回 null。 */
    private static JsonElement walk(JsonObject snapshot, String path) {
        JsonElement current = snapshot;
        for (String part : path.split("\\.")) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(part);
        }
        return current;
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
