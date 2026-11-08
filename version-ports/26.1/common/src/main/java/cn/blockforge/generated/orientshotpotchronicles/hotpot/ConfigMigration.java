package cn.blockforge.generated.orientshotpotchronicles.hotpot;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 火锅配置文件的旧键迁移（r134b，豆腐改 ID 后）。只依赖 Gson 和文件读写，不碰游戏类，方便单元测试。
 * 作者：mryt233（2026-11-04）
 *
 * <ul>
 *   <li>{@code food.tofu} → {@code food.cold_tofu}（只搬 hunger / saturation，搬完删掉 food.tofu）</li>
 *   <li>{@code food.tofu_cube} → {@code food.cold_tofu_cube}；{@code food.cooked_tofu_cube} → {@code food.hot_tofu_cube}</li>
 *   <li>{@code food.tofu.cook_ticks} / {@code overcook_ticks}（r134 开发中短暂用过）→
 *       {@code cooking.tofu_cook_ticks} / {@code cooking.tofu_overcook_ticks}</li>
 *   <li>{@code soup_bowl.<汤底>.slow_duration} / {@code slow_amplifier}（r139 起高汤不再缓慢）→ 直接删掉，
 *       把高汤老的 slow_duration 值搬进新的 {@code second_duration}（老配置 60 tick 变成伤害吸收 3 秒，
 *       玩家自己再调；没有老值就用默认值），免得升级后每次开游戏都报「不支持的配置项」</li>
 * </ul>
 * 新名字下已有值时以新名字为准，旧键直接删。没有旧键时不写文件；文件无效（不是合法 JSON、格式版本不对、太大）时不动，
 * 交给原来的读取流程报警告。写回用「临时文件 + 替换」，失败不破坏原文件。
 */
final class ConfigMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ConfigMigration() {
    }

    /**
     * 检查并迁移一个配置文件。
     *
     * @param file      配置文件路径（不存在就什么都不做）
     * @param maxBytes  超过这个大小不处理（和读取流程同一上限）
     * @param valid     文档是否有效（传框架的 {@code ConfigStore::isValidDocument}，保持同一判断）
     * @param warn      写回失败时的警告（只一条）
     * @return 搬了哪些键的说明；文件没改动时为空
     */
    static List<String> migrate(Path file, long maxBytes, Predicate<String> valid, Consumer<String> warn) {
        String text;
        try {
            if (file == null || !Files.isRegularFile(file) || Files.size(file) > maxBytes) {
                return List.of();
            }
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException error) {
            return List.of();
        }
        JsonObject root;
        try {
            if (!valid.test(text)) {
                return List.of();
            }
            JsonElement parsed = JsonParser.parseString(text);
            if (parsed == null || !parsed.isJsonObject()) {
                return List.of();
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException error) {
            return List.of();
        }

        List<String> moved = migrate(root);
        if (moved.isEmpty()) {
            return moved;
        }
        if (!write(file, GSON.toJson(root) + "\n", warn)) {
            return List.of();
        }
        return moved;
    }

    /** 在内存里迁移（就地修改 root），返回搬了哪些键。 */
    static List<String> migrate(JsonObject root) {
        List<String> moved = new ArrayList<>();
        migrateSoupBowlSlowKeys(root, moved);
        JsonObject food = child(root, "food");
        if (food == null) {
            return moved;
        }

        JsonObject tofu = child(food, "tofu");
        if (tofu != null) {
            // 开发中短暂用过的两个时长 → cooking 组
            if (tofu.has("cook_ticks") || tofu.has("overcook_ticks")) {
                JsonObject cooking = childOrCreate(root, "cooking");
                if (cooking != null) {
                    moveField(tofu, "cook_ticks", cooking, "tofu_cook_ticks",
                        "food.tofu.cook_ticks", "cooking.tofu_cook_ticks", moved);
                    moveField(tofu, "overcook_ticks", cooking, "tofu_overcook_ticks",
                        "food.tofu.overcook_ticks", "cooking.tofu_overcook_ticks", moved);
                }
            }
            // 营养值 → cold_tofu（只搬 hunger / saturation）
            if (tofu.has("hunger") || tofu.has("saturation")) {
                JsonObject cold = childOrCreateInPlace(food, "cold_tofu", "tofu");
                if (cold != null) {
                    moveField(tofu, "hunger", cold, "hunger", "food.tofu.hunger", "food.cold_tofu.hunger", moved);
                    moveField(tofu, "saturation", cold, "saturation",
                        "food.tofu.saturation", "food.cold_tofu.saturation", moved);
                }
                // cold_tofu 已有但不是对象：按「新名字已有值」处理，旧值随 food.tofu 一起删
            }
            // 搬完删掉整个 food.tofu；剩下搬不过去的键（未知键等）也一起删，并在日志里列出来
            for (String key : List.copyOf(tofu.keySet())) {
                moved.add("food.tofu." + key + " (dropped)");
            }
            food.remove("tofu");
            if (moved.isEmpty()) {
                moved.add("food.tofu (dropped, empty)");
            }
        }

        renameKey(food, "tofu_cube", "cold_tofu_cube", "food.", moved);
        renameKey(food, "cooked_tofu_cube", "hot_tofu_cube", "food.", moved);
        return moved;
    }

    /**
     * mryt233 改（2026-11-06，r139）：高汤不再给「缓慢」，配置文件里的 {@code soup_bowl.<汤底>.slow_duration} /
     * {@code slow_amplifier} 已经作废。老值顺手搬到新的 {@code second_duration}（副效果：高汤的伤害吸收），
     * 然后删掉两个旧键；整个 soup_bowl 组不规范（不是对象）时跳过，交给读取流程照常报警告。
     */
    private static void migrateSoupBowlSlowKeys(JsonObject root, List<String> moved) {
        JsonObject soupBowl = child(root, "soup_bowl");
        if (soupBowl == null) {
            return;
        }
        for (String soupId : List.copyOf(soupBowl.keySet())) {
            JsonObject node = child(soupBowl, soupId);
            if (node == null) {
                continue;
            }
            if (node.has("slow_duration")) {
                JsonElement value = node.remove("slow_duration");
                if (!node.has("second_duration")) {
                    node.add("second_duration", value);
                    moved.add("soup_bowl." + soupId + ".slow_duration -> soup_bowl." + soupId + ".second_duration");
                } else {
                    moved.add("soup_bowl." + soupId + ".slow_duration (dropped)");
                }
            }
            if (node.has("slow_amplifier")) {
                node.remove("slow_amplifier");
                moved.add("soup_bowl." + soupId + ".slow_amplifier (dropped)");
            }
        }
    }

    /** 把 from[fromKey] 搬到 to[toKey]；目标已有值时以目标为准。搬或丢都从 from 删掉。 */
    private static void moveField(JsonObject from, String fromKey, JsonObject to, String toKey,
                                  String fromPath, String toPath, List<String> moved) {
        if (!from.has(fromKey)) {
            return;
        }
        JsonElement value = from.remove(fromKey);
        if (to.has(toKey)) {
            moved.add(fromPath + " -> " + toPath + " (kept existing)");
        } else {
            to.add(toKey, value);
            moved.add(fromPath + " -> " + toPath);
        }
    }

    /**
     * 同一层里改键名，保持原来的位置；新键已有值时以新键为准，旧键删掉。
     *
     * <p>mryt233 改（2026-11-07）：以前自己写「先把这一层的键全删、再按顺序重新 add」，
     * {@link #childOrCreateInPlace} 里又抄了一遍同样两段循环。现在两处都改用 {@link #rewriteLayer}，
     * 只保留一段「取原顺序 → 按映射重建」的代码，插入新键也走同一条路。</p>
     */
    private static void renameKey(JsonObject parent, String oldKey, String newKey, String prefix, List<String> moved) {
        if (!parent.has(oldKey)) {
            return;
        }
        if (parent.has(newKey)) {
            parent.remove(oldKey);
            moved.add(prefix + oldKey + " -> " + prefix + newKey + " (kept existing)");
            return;
        }
        rewriteLayer(parent, oldKey, newKey, null);
        moved.add(prefix + oldKey + " -> " + prefix + newKey);
    }

    /**
     * mryt233 改（2026-11-07）：把这一层里的 {@code from} 换成 {@code to} 之后按原顺序重建，其它键位置不变。
     *
     * <p>Gson 的 {@code JsonObject} 底层是 {@code LinkedTreeMap}，没有「改键名的同时保住位置」的 API，
     * 所以只能取一份原顺序快照、清空、再按新名字重新 add。这段代码只写在这里一处：
     * 以前 {@link #renameKey} 与 {@link #childOrCreateInPlace} 各抄一遍，插新键的那份末尾还要靠
     * {@code if (!parent.has(key))} 兜底，外人很难看出它到底在兜什么。</p>
     *
     * @param inserted 非空时用这个值占据 {@code to}（插入新键）；为空时搬 {@code from} 原来的值
     */
    private static void rewriteLayer(JsonObject parent, String from, String to, JsonElement inserted) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(parent.entrySet());
        for (Map.Entry<String, JsonElement> entry : entries) {
            parent.remove(entry.getKey());
        }
        boolean placed = false;
        for (Map.Entry<String, JsonElement> entry : entries) {
            boolean anchor = entry.getKey().equals(from);
            if (anchor) {
                placed = true;
            }
            if (inserted == null) {
                // 改名：把 from 换成 to，占它原来的位置；to 已经有值（不该发生，renameKey 先拦过）就丢掉旧值
                if (!anchor) {
                    parent.add(entry.getKey(), entry.getValue());
                } else if (!parent.has(to)) {
                    parent.add(to, entry.getValue());
                }
                continue;
            }
            // 插新键：新键占在 from 的前面，from 自己照旧保留（调用方随后会搬空 / 删掉它）
            if (anchor) {
                parent.add(to, inserted);
            }
            parent.add(entry.getKey(), entry.getValue());
        }
        if (inserted != null && !placed) {
            // 要插的键没有可依附的旧键（例如 food 里没有 tofu 这一项），补在末尾
            parent.add(to, inserted);
        }
    }

    private static JsonObject child(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    /** 取子对象，没有就在末尾建一个；已有但不是对象时返回 null（不覆盖玩家写的东西）。 */
    private static JsonObject childOrCreate(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null) {
            JsonObject created = new JsonObject();
            parent.add(key, created);
            return created;
        }
        return value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    /** 同 {@link #childOrCreate}，但新建时插在 {@code besideKey} 原来的位置（那个旧键随后会被删掉）。 */
    private static JsonObject childOrCreateInPlace(JsonObject parent, String key, String besideKey) {
        JsonElement value = parent.get(key);
        if (value != null) {
            return value.isJsonObject() ? value.getAsJsonObject() : null;
        }
        JsonObject created = new JsonObject();
        // mryt233 改（2026-11-07）：插入位置交给 rewriteLayer，不再手写两遍循环，也不再需要末尾的 has() 兜底
        rewriteLayer(parent, besideKey, key, created);
        return created;
    }

    /** 和框架补全文件同一写法：同目录临时文件 → 原子替换（不支持时普通替换）；失败只警告，原文件不动。 */
    private static boolean write(Path file, String text, Consumer<String> warn) {
        Path temp = null;
        try {
            temp = Files.createTempFile(file.toAbsolutePath().getParent(), "config-", ".tmp");
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException error) {
            warn.accept("Could not rename legacy config keys (" + error.getMessage() + "); original file kept");
            return false;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 临时文件删不掉不影响配置本身
                }
            }
        }
    }
}