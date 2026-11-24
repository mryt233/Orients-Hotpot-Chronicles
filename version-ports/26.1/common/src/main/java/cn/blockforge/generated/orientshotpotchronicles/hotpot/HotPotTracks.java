// 作者：mryt233；mryt233 改（2026-11-16）：适配 26.1 Identifier。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** 数据包驱动的火锅 4 轨音游谱面（data/<ns>/hotpot_tracks/*.json）；服务端重载并 S2C 同步。作者：mryt233 */
public final class HotPotTracks {
    public static final String FOLDER = "hotpot_tracks";
    public static final Identifier DEFAULT_TRACK_ID = HotPot.id("midnight_boil");
    private static final Gson GSON = new GsonBuilder().create();
    private static final List<String> BUILTIN_NAMES = List.of("midnight_boil", "bamboo_chopsticks", "red_soup_rush");

    private static volatile Map<Identifier, HotPotTrack> serverTracks = loadBuiltinFallback();
    private static volatile Map<Identifier, HotPotTrack> clientTracks = null;
    private static volatile long revision = 1L;

    private HotPotTracks() { }

    public record TrackNote(int timeMs, int lane, String sound, float pitch) { }

    public record HotPotTrack(
        Identifier id,
        String title,
        String titleKey,
        String author,
        int bpm,
        int leadInMs,
        int durationMs,
        String difficulty,
        List<TrackNote> notes
    ) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", id.toString());
            if (!titleKey.isEmpty()) {
                JsonObject t = new JsonObject();
                t.addProperty("translate", titleKey);
                obj.add("title", t);
            } else {
                obj.addProperty("title", title);
            }
            obj.addProperty("author", author);
            obj.addProperty("bpm", bpm);
            obj.addProperty("lead_in_ms", leadInMs);
            obj.addProperty("duration_ms", durationMs);
            obj.addProperty("difficulty", difficulty);
            JsonArray arr = new JsonArray();
            for (TrackNote n : notes) {
                JsonObject no = new JsonObject();
                no.addProperty("time_ms", n.timeMs());
                no.addProperty("lane", n.lane());
                no.addProperty("sound", n.sound());
                no.addProperty("pitch", n.pitch());
                arr.add(no);
            }
            obj.add("notes", arr);
            return obj;
        }
    }

    public static void reload(ResourceManager manager) {
        Map<Identifier, HotPotTrack> next = new LinkedHashMap<>();
        if (manager != null) {
            Map<Identifier, Resource> found = manager.listResources(FOLDER,
                loc -> loc.getPath().endsWith(".json"));
            List<Identifier> keys = new ArrayList<>(found.keySet());
            keys.sort(Comparator.comparing(Identifier::toString));
            for (Identifier loc : keys) {
                Identifier id = trackIdFromFile(loc);
                if (id == null) {
                    continue;
                }
                try (var reader = found.get(loc).openAsReader()) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed.isJsonObject()) {
                        HotPotTrack track = parseTrack(id, parsed.getAsJsonObject());
                        if (track != null && !track.notes().isEmpty()) {
                            next.put(id, track);
                        }
                    }
                } catch (Exception error) {
                    HotPot.LOGGER.warn("[hotpot] 加载音游谱面 {} 失败，已跳过", loc, error);
                }
            }
        }
        if (next.isEmpty()) {
            next = loadBuiltinFallback();
        }
        serverTracks = Collections.unmodifiableMap(next);
        revision++;
        HotPot.LOGGER.info("[hotpot] 已加载 {} 首火锅音游谱面", serverTracks.size());
    }

    public static Identifier trackIdFromFile(Identifier fileLocation) {
        if (fileLocation == null) {
            return null;
        }
        String path = fileLocation.getPath();
        String prefix = FOLDER + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        String name = path.substring(prefix.length(), path.length() - 5);
        if (name.isEmpty()) {
            return null;
        }
        return Identifier.tryBuild(fileLocation.getNamespace(), name);
    }

    public static HotPotTrack parseTrack(Identifier id, JsonObject root) {
        if (id == null || root == null) {
            return null;
        }
        String title = "";
        String titleKey = "";
        JsonElement titleEl = root.get("title");
        if (titleEl != null) {
            if (titleEl.isJsonObject() && titleEl.getAsJsonObject().has("translate")) {
                titleKey = titleEl.getAsJsonObject().get("translate").getAsString();
            } else if (titleEl.isJsonPrimitive()) {
                title = titleEl.getAsString();
            }
        }
        if (title.isEmpty() && titleKey.isEmpty()) {
            title = id.getPath();
        }
        String author = stringOr(root, "author", "mryt233");
        int bpm = clampInt(intOr(root, "bpm", 120), 40, 300);
        int leadInMs = clampInt(intOr(root, "lead_in_ms", 1400), 0, 10000);
        String difficulty = stringOr(root, "difficulty", "normal").toLowerCase(Locale.ROOT);

        List<TrackNote> notes = new ArrayList<>();
        int maxNoteTime = leadInMs;
        if (root.has("notes") && root.get("notes").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("notes")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject no = el.getAsJsonObject();
                int timeMs;
                if (no.has("time_ms")) {
                    timeMs = Math.max(0, no.get("time_ms").getAsInt());
                } else if (no.has("beat")) {
                    double beat = Math.max(0.0, no.get("beat").getAsDouble());
                    timeMs = (int) Math.round(leadInMs + beat * (60000.0 / bpm));
                } else {
                    continue;
                }
                int lane = clampInt(intOr(no, "lane", 0), 0, 3);
                String sound = stringOr(no, "sound", "note_harp");
                float pitch = clampFloat(floatOr(no, "pitch", 1.0F), 0.5F, 2.0F);
                notes.add(new TrackNote(timeMs, lane, sound, pitch));
                if (timeMs > maxNoteTime) {
                    maxNoteTime = timeMs;
                }
            }
        }
        notes.sort(Comparator.comparingInt(TrackNote::timeMs).thenComparingInt(TrackNote::lane));
        int explicitDuration = intOr(root, "duration_ms", 0);
        int durationMs = Math.max(Math.max(explicitDuration, maxNoteTime + 1500), 4000);
        return new HotPotTrack(id, title, titleKey, author, bpm, leadInMs, durationMs, difficulty, List.copyOf(notes));
    }

    public static List<HotPotTrack> all(boolean client) {
        Map<Identifier, HotPotTrack> map = activeMap(client);
        return List.copyOf(map.values());
    }

    public static HotPotTrack get(Identifier id, boolean client) {
        Map<Identifier, HotPotTrack> map = activeMap(client);
        if (id != null && map.containsKey(id)) {
            return map.get(id);
        }
        if (map.containsKey(DEFAULT_TRACK_ID)) {
            return map.get(DEFAULT_TRACK_ID);
        }
        return map.values().stream().findFirst().orElse(null);
    }

    public static long revision() {
        return revision;
    }

    public static String toNetworkJson() {
        JsonObject envelope = new JsonObject();
        JsonArray arr = new JsonArray();
        for (HotPotTrack track : serverTracks.values()) {
            arr.add(track.toJson());
        }
        envelope.add("tracks", arr);
        return GSON.toJson(envelope);
    }

    public static void acceptClientJson(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) {
                return;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("tracks") || !obj.get("tracks").isJsonArray()) {
                return;
            }
            Map<Identifier, HotPotTrack> parsed = new LinkedHashMap<>();
            for (JsonElement item : obj.getAsJsonArray("tracks")) {
                if (!item.isJsonObject()) {
                    continue;
                }
                JsonObject to = item.getAsJsonObject();
                Identifier id = Identifier.tryParse(stringOr(to, "id", ""));
                if (id == null) {
                    continue;
                }
                HotPotTrack track = parseTrack(id, to);
                if (track != null && !track.notes().isEmpty()) {
                    parsed.put(id, track);
                }
            }
            if (!parsed.isEmpty()) {
                clientTracks = Collections.unmodifiableMap(parsed);
                revision++;
            }
        } catch (RuntimeException error) {
            HotPot.LOGGER.warn("[hotpot] 客户端解析音游谱面同步包失败", error);
        }
    }

    public static void clearClient() {
        clientTracks = null;
        revision++;
    }

    private static Map<Identifier, HotPotTrack> activeMap(boolean client) {
        if (client && clientTracks != null && !clientTracks.isEmpty()) {
            return clientTracks;
        }
        return serverTracks;
    }

    private static Map<Identifier, HotPotTrack> loadBuiltinFallback() {
        Map<Identifier, HotPotTrack> fallback = new LinkedHashMap<>();
        ClassLoader cl = HotPotTracks.class.getClassLoader();
        for (String name : BUILTIN_NAMES) {
            String path = "data/" + HotPot.MOD_ID + "/" + FOLDER + "/" + name + ".json";
            try (InputStream stream = cl.getResourceAsStream(path)) {
                if (stream == null) {
                    continue;
                }
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonElement el = JsonParser.parseReader(reader);
                    if (el.isJsonObject()) {
                        HotPotTrack track = parseTrack(HotPot.id(name), el.getAsJsonObject());
                        if (track != null && !track.notes().isEmpty()) {
                            fallback.put(track.id(), track);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return Collections.unmodifiableMap(fallback);
    }

    private static String stringOr(JsonObject obj, String key, String def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : def;
    }

    private static int intOr(JsonObject obj, String key, int def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : def;
    }

    private static float floatOr(JsonObject obj, String key, float def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : def;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
