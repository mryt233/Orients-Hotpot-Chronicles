package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.game.ConfigBase;
import cn.blockforge.orient.framework.pure.ConfigStore;
import cn.blockforge.orient.framework.pure.ConfigWarning;
import cn.blockforge.orient.framework.pure.NumericConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 火锅的数值配置门面：键、默认值、范围规则和说明留在内容层；
 * 解析/校验/快照/文件补全由框架 {@link ConfigStore} + {@link ConfigBase} 完成（P1 第四批）。
 * 配置格式、文件名与字段含义保持不变。
 * mryt233 改（2026-11-07）：取值改成查 {@link ConfigStore} 的扁平数值快照（一条路径一次哈希查找），
 * 不再每次按 `.` 拆路径逐层走 JsonObject；快照与扁平表在 load / reload / 应用同步包 / 恢复默认时成对重建，
 * 单人游戏里集成服线程与渲染线程并发读取也安全。作者：mryt233 */
public final class Config {
    private static final String NOTE = """
        之晗的火锅物语 · 数值配置
        修改后执行 /orientshotpot reload，食物营养、汤底、燃料、蘸料和烹饪参数立即生效。
        配方数值在数据包 recipe 里，使用原版 /reload。
        缺项使用默认值；写错一项仅回退该项并警告；坏文件原样保留。联机只改服务端，数值自动同步。""";

    /** 一次重载的结果；警告带语言键（前置检查 W5，mryt233 改 2026-11-01），用 {@link #describe} 显示。 */
    public record Loaded(JsonObject data, List<ConfigWarning> warnings) {
        public Loaded {
            warnings = List.copyOf(warnings);
        }
    }

    /** 一条配置警告按玩家语言显示；没有翻译时显示英文。 */
    public static net.minecraft.network.chat.Component describe(ConfigWarning warning) {
        return net.minecraft.network.chat.Component.translatableWithFallback(
            warning.key(), warning.fallback(), warning.args().toArray());
    }

    private static final String FILE_NAME = HotPot.MOD_ID + ".json";
    private static final long MAX_BYTES = 65536;

    private static final ConfigStore STORE = new ConfigStore(
        FILE_NAME, 1, MAX_BYTES, NOTE, Config::rule, Config::fieldHelp,
        message -> HotPot.LOGGER.warn("[hotpot] {}", message));
    private static final ConfigBase BASE = new ConfigBase(STORE);

    /** mryt233 改（2026-11-05，r135e）：写回配置文件的 JSON 格式，与 {@link ConfigStore} 里那份一致。 */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    static {
        // mryt233 改（2026-11-05，r135e）：客户端项必须在 load() 之前登记进模板，
        // 否则 client.particle_intensity 会被判成 unknown_key。这里放在静态初始化块最稳妥：
        // SoupBase / Seasoning 的静态块虽然也会调 rememberXxx，但都晚于 Config 类自身的初始化。
        rememberClientDefaults();
    }

    /** mryt233 改（2026-11-05，r134b）：记住配置目录，读文件前先做旧键迁移（见 {@link ConfigMigration}）。 */
    private static volatile Path directory;

    /**
     * mryt233 改（2026-11-05，r135e）：粒子强度永远以本机为准，不受服务端快照覆盖，
     * 所以单独存一份本地值（0～100 的整数百分比）。
     */
    private static volatile int localParticleIntensity = 100;

    /** 粒子强度在扁平快照里的路径（mryt233 改 2026-11-07：取值走扁平表，路径只写这一处）。 */
    private static final String PARTICLE_INTENSITY = "client.particle_intensity";

    /** mryt233 改（2026-11-05，r135e）：当前运行中的服务端（单人集成服用来就地重载）。 */
    private static volatile MinecraftServer activeServer;

    private Config() {
    }

    public static void init(Path directory, boolean dedicated) {
        Config.directory = directory;
        migrateLegacyKeys();
        BASE.init(directory, dedicated);
        // mryt233 改（2026-11-05，r135e）：读完本地文件后把客户端粒子强度取回来
        readLocalParticleIntensity();
    }

    public static void load() {
        migrateLegacyKeys();
        BASE.load();
        // mryt233 改（2026-11-05，r135e）：同上，load 之后本地文件里的值才是准的
        readLocalParticleIntensity();
    }

    public static Loaded reload() {
        migrateLegacyKeys();
        ConfigStore.Loaded loaded = BASE.reload();
        // mryt233 改（2026-11-05，r135e）：热重载也要刷新本地粒子强度
        readLocalParticleIntensity();
        return new Loaded(loaded.data(), loaded.details());
    }

    public static void setServer(MinecraftServer value) {
        // 进服时框架会重读一次文件，同样先迁移（mryt233 改，r134b）
        if (value != null) {
            migrateLegacyKeys();
        }
        // mryt233 改（2026-11-05，r135e）：记住服务端实例，单人集成服存盘后要就地重载
        activeServer = value;
        BASE.setServer(value);
        if (value != null) {
            // 断线（value == null）时快照会退回默认值，此时不要去覆盖玩家本机的粒子强度
            readLocalParticleIntensity();
        }
    }

    /**
     * mryt233 改（2026-11-04，r134b）：豆腐改 ID 后玩家旧配置里的键自动改名（food.tofu → food.cold_tofu 等），
     * 免得每次 reload 都报「不支持的配置项」。有改动才写回；文件无效时不动，交给原来的读取流程。
     */
    private static synchronized void migrateLegacyKeys() {
        Path dir = directory;
        if (dir == null) {
            return;
        }
        List<String> moved = ConfigMigration.migrate(dir.resolve(FILE_NAME), MAX_BYTES, STORE::isValidDocument,
            message -> HotPot.LOGGER.warn("[hotpot] {}", message));
        if (!moved.isEmpty()) {
            HotPot.LOGGER.info("[hotpot] Renamed legacy config keys: {}", String.join(", ", moved));
        }
    }

    public static JsonObject serverSnapshot() {
        return BASE.serverSnapshot();
    }

    public static void acceptServer(JsonObject data) {
        BASE.acceptServer(data);
    }

    public static void clearClient() {
        BASE.clearClient();
        RecipeSync.clearClient();
    }

    public static void saveDefaultsIfNeeded() {
        STORE.saveDefaultsIfNeeded();
    }

    // ------------------------------------------------------------------ 客户端粒子强度（r135e）

    /**
     * mryt233 改（2026-11-05，r135e）：客户端粒子强度的默认值，必须在 {@link ConfigStore#load()} 之前登记进模板，
     * 否则读文件时 client.particle_intensity 会被判成 unknown_key。由静态初始化块调用。
     */
    private static void rememberClientDefaults() {
        ConfigStore.make(STORE.defaults(), "client").addProperty("particle_intensity", 100);
    }

    /**
     * mryt233 改（2026-11-05，r135e）：从本地文件解析结果里取回粒子强度，缺项 / 写错都保持 100。
     * mryt233 改（2026-11-07）：改成扁平查表，不再逐层取 client 子对象再判类型。
     */
    private static void readLocalParticleIntensity() {
        JsonObject server = STORE.serverData();
        if (!STORE.contains(server, PARTICLE_INTENSITY)) {
            // 没有这一项、或者写成了非数字：继续用上一份本地值（配置解析那边已经报过警告）
            return;
        }
        localParticleIntensity = Math.max(0, Math.min(100,
            STORE.integer(server, PARTICLE_INTENSITY, localParticleIntensity)));
    }

    /** mryt233 改（2026-11-05，r135e）：本机粒子强度百分比 0～100（0=关闭粒子）。 */
    public static int particleIntensity() {
        return localParticleIntensity;
    }

    /** mryt233 改（2026-11-05，r135e）：粒子强度折算成 0.0F～1.0F 的缩放系数。 */
    public static float particleScale() {
        return Math.max(0F, Math.min(1F, localParticleIntensity / 100F));
    }

    /** mryt233 改（2026-11-05，r135e）：设置本机粒子强度，超出 0～100 自动夹紧；只改内存，存盘走 {@link #saveAndReload}。 */
    public static void setParticleIntensity(int percent) {
        localParticleIntensity = Math.max(0, Math.min(100, percent));
    }

    /**
     * mryt233 改（2026-11-05，r135e）：给游戏内设置界面用的可编辑副本（进界面时读它）。
     * 本地文件存在就用它解析出的快照，否则用默认值；client.particle_intensity 同步成本机当前值。
     */
    public static JsonObject editableCopy() {
        Path file = directory == null ? null : directory.resolve(FILE_NAME);
        JsonObject copy = file != null && Files.exists(file)
            ? STORE.serverData().deepCopy()
            : STORE.defaults().deepCopy();
        ConfigStore.make(copy, "client").addProperty("particle_intensity", localParticleIntensity);
        return copy;
    }

    /** mryt233 改（2026-11-05，r135e）：默认值副本，供设置界面的「恢复默认」按钮使用。 */
    public static JsonObject defaultsCopy() {
        return STORE.defaults().deepCopy();
    }

    /**
     * mryt233 改（2026-11-05，r135e）：设置界面保存。
     * <p>流程：读回 particle_intensity → 与默认值合并（保留 _format / _说明 / _字段说明）→ 临时文件原子替换写入
     * 配置目录 → {@link #reload()} 重新加载本地配置 → 单人集成服在服务端线程再重载一次并补全文件，
     * 让食物营养、涮煮时间、汤底速度等不必敲命令就立即在世界里生效。
     * <p>配置目录还没初始化时（单元测试 / 极早期）只更新内存快照，不碰文件。
     */
    public static synchronized Loaded saveAndReload(JsonObject edited) {
        // mryt233 改（2026-11-06，r139b）：先清一遍旧键，免得把已经作废的 slow_* 又从旧文件合并回新文件
        migrateLegacyKeys();
        JsonObject source = edited == null ? new JsonObject() : edited;
        JsonObject client = ConfigStore.read(source, "client");
        if (client != null && client.has("particle_intensity")) {
            JsonElement value = client.get("particle_intensity");
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                try {
                    setParticleIntensity(value.getAsInt());
                } catch (RuntimeException ignored) {
                    // 界面写坏的数字忽略，继续用当前本地值
                }
            }
        }

        // 以「默认值 + 现有文件」为底，再盖上界面改过的值：这样文件里玩家手写的未知键不会被界面保存冲掉
        // （ConfigStore 的解析快照只保留认识的键，所以得从文件再读一遍原始内容）。
        JsonObject base = ConfigStore.merge(STORE.defaults(), existingDocument());
        JsonObject out = ConfigStore.merge(base, source);
        ConfigStore.make(out, "client").addProperty("particle_intensity", localParticleIntensity);
        out.addProperty("_format", 1);
        out.addProperty("_说明", NOTE);
        out.add("_字段说明", fieldHelp());

        Path file = directory == null ? null : directory.resolve(FILE_NAME);
        if (file == null) {
            STORE.replaceServer(out);
            return new Loaded(out.deepCopy(), List.of());
        }

        Path temp = null;
        boolean written = false;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "config-", ".tmp");
            Files.writeString(temp, GSON.toJson(out) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            written = true;
        } catch (IOException error) {
            // mryt233 改（2026-11-05，r135e）：写盘失败不动旧文件，界面改动只留在内存快照里
            HotPot.LOGGER.warn("[hotpot] Could not write config ({}); original file kept", error.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 临时文件删不掉不影响结果
                }
            }
        }
        if (!written) {
            // 写盘失败时至少让内存快照跟上，界面上的改动不白填
            STORE.replaceServer(out);
            return new Loaded(out.deepCopy(), List.of());
        }

        Loaded result = reload();
        MinecraftServer server = activeServer;
        if (server != null && !server.isDedicatedServer()) {
            // 单人集成服：客户端和服务端在同一个进程里，服务端线程重载一次并补全文件即可，
            // 不用额外发网络包（客户端粒子强度读的是本机那份配置）。
            server.execute(Config::reload);
            server.execute(STORE::saveDefaultsIfNeeded);
        }
        return result;
    }

    /**
     * mryt233 改（2026-11-05，r135e）：读现有配置文件的原始内容，给 {@link #saveAndReload} 当合并底稿。
     * 文件不存在、太大或写坏时返回空对象——写坏的文件交给调用方照常覆盖，反正原文件已经解析不出来。
     */
    private static JsonObject existingDocument() {
        Path dir = directory;
        if (dir == null) {
            return new JsonObject();
        }
        Path file = dir.resolve(FILE_NAME);
        try {
            if (!Files.exists(file) || Files.size(file) > MAX_BYTES) {
                return new JsonObject();
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (!STORE.isValidDocument(text)) {
                return new JsonObject();
            }
            JsonElement parsed = JsonParser.parseString(text);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (IOException | RuntimeException error) {
            return new JsonObject();
        }
    }

    // ------------------------------------------------------------------ 取值（mryt233 改 2026-11-07：全部走扁平快照）

    /**
     * mryt233 改（2026-11-07）：所有取值都是一次哈希查找，不再「拆路径 + 逐层遍历 JsonObject」。
     * 路径拼在调用处（`food.` + id + `.hunger`），和原来 ConfigStore.read(...) 的层级完全对应，行为不变。
     * 每次现读快照，改完配置 /orientshotpot reload 立即生效；单人游戏里集成服线程与渲染线程并发读也安全。
     */
    private static JsonObject snapshot() {
        return BASE.current();
    }

    /** mryt233 改（2026-11-07）：读取一条数值配置（小数），一次哈希查找；没有这一项就用 fallback。 */
    public static double real(String path, double fallback) {
        return STORE.real(snapshot(), path, fallback);
    }

    /** mryt233 改（2026-11-07）：读取一条数值配置（整数），一次哈希查找；没有这一项就用 fallback。 */
    public static int integer(String path, int fallback) {
        return STORE.integer(snapshot(), path, fallback);
    }

    /** mryt233 改（2026-11-07）：这一条配置在不在（用来区分「没配」与「配成默认值」）。 */
    public static boolean has(String path) {
        return STORE.contains(snapshot(), path);
    }

    public static int foodHunger(String id, int fallback) {
        return integer("food." + id + ".hunger", fallback);
    }

    public static float foodSaturation(String id, float fallback) {
        return (float) real("food." + id + ".saturation", fallback);
    }

    public static float soupSpeed(String id, float fallback) {
        return (float) real("soup_speed." + id, fallback);
    }

    /**
     * mryt233 改（2026-11-05）：汤碗分组里的整数项，路径 soup_bowl.&lt;汤底&gt;.&lt;键&gt;
     * （hunger / duration / amplifier / second_duration / second_amplifier / fire_duration / fire_amplifier /
     * hunger_duration / hunger_amplifier；r139 起用 second_* 取代原来的 slow_*）。
     * 每次现读快照，改完配置 /orientshotpot reload 立即生效。
     */
    public static int soupBowlInt(String id, String key, int fallback) {
        return integer("soup_bowl." + id + "." + key, fallback);
    }

    /** mryt233 改（2026-11-05）：汤碗的小数项（目前只有 saturation），路径 soup_bowl.&lt;汤底&gt;.&lt;键&gt;。 */
    public static float soupBowlFloat(String id, String key, float fallback) {
        return (float) real("soup_bowl." + id + "." + key, fallback);
    }

    public static float fuelPower(String id, float fallback) {
        return (float) real("fuel." + id + ".power", fallback);
    }

    public static int fuelTicks(String id, int fallback) {
        return integer("fuel." + id + ".burn_ticks", fallback);
    }

    public static int seasoningTicks(String id, int fallback) {
        return integer("seasoning." + id + ".duration", fallback);
    }

    public static int seasoningAmplifier(String id, int fallback) {
        return integer("seasoning." + id + ".amplifier", fallback);
    }

    public static int seasoningHunger(String id, int fallback) {
        return integer("seasoning." + id + ".hunger", fallback);
    }

    public static int drinkHunger(int fallback) {
        return integer("drink.sip.hunger", fallback);
    }

    public static float drinkSaturation(float fallback) {
        return (float) real("drink.sip.saturation", fallback);
    }

    /**
     * mryt233 改（2026-11-05）：某种饮料每口正面效果的整数项，路径 drink.&lt;饮料&gt;.&lt;键&gt;
     * （duration / amplifier）。与 drink.sip.&lt;键&gt;（每口回多少饥饿值）是两组不同的键。
     */
    public static int drinkInt(String id, String key, int fallback) {
        return integer("drink." + id + "." + key, fallback);
    }

    public static int fallbackTicks(int fallback) {
        return integer("cooking.fallback_ticks", fallback);
    }

    public static float fallbackXp(float fallback) {
        return (float) real("cooking.fallback_xp", fallback);
    }

    /**
     * mryt233 改（2026-11-04）：整块冷豆腐在锅里煮成热豆腐的刻数。
     * 路径 `cooking.tofu_cook_ticks`（r134b 从 `food.tofu.cook_ticks` 挪到 cooking 组，旧键自动迁移），
     * 锅那边现读，reload 立即生效。
     */
    public static int tofuCookTicks(int fallback) {
        return integer("cooking.tofu_cook_ticks", fallback);
    }

    /** 豆腐从下锅到煮过头的总刻数（含煮熟那段），路径 `cooking.tofu_overcook_ticks`。 */
    public static int tofuOvercookTicks(int fallback) {
        return integer("cooking.tofu_overcook_ticks", fallback);
    }

    public static float xpPerItem(float fallback) {
        return (float) real("cooking.xp_per_item", fallback);
    }

    /**
     * mryt233 改（2026-11-05）：某种生食材在火锅里的涮煮刻数，路径 cooking.items.&lt;物品注册路径&gt;。
     * 值就是刻数本身（不是对象）。登记过的食材不再问数据包熔炉配方，直接在配置里改、reload 立即生效。
     * 没登记过返回 fallback，让锅那边照旧走熔炉配方 / 兜底。
     */
    public static int itemCookTicks(String id, int fallback) {
        return integer("cooking.items." + id, fallback);
    }

    /**
     * mryt233 改（2026-11-05）：这个物品有没有单独的涮煮时间（决定锅要不要绕过熔炉配方）。
     * mryt233 改（2026-11-07）：与 {@link #itemCookTicks} 共用同一张扁平表，调用方「先问有没有、再问多少」
     * 不会再各走一遍 JsonObject 树。
     */
    public static boolean hasItemCookTicks(String id) {
        return has("cooking.items." + id);
    }

    // ------------------------------------------------------------------ 默认值登记

    public static void rememberFood(String id, int hunger, float saturation) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "food", id);
        node.addProperty("hunger", hunger);
        node.addProperty("saturation", saturation);
    }

    public static void rememberSoupSpeed(String id, float speed) {
        ConfigStore.make(STORE.defaults(), "soup_speed").addProperty(id, speed);
    }

    public static void rememberFuel(String id, float power, int ticks) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "fuel", id);
        node.addProperty("power", power);
        node.addProperty("burn_ticks", ticks);
    }

    public static void rememberSeasoning(String id, int duration, int amplifier, int hunger) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "seasoning", id);
        node.addProperty("duration", duration);
        node.addProperty("amplifier", amplifier);
        node.addProperty("hunger", hunger);
    }

    /** 饮料 / 一杯饮料每喝一口回的饥饿值和饱和度（mryt233 2026-11-01，代码检查 W7：以前写死在代码里）。 */
    public static void rememberDrink(int hunger, float saturation) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "drink", "sip");
        node.addProperty("hunger", hunger);
        node.addProperty("saturation", saturation);
    }

    /**
     * mryt233 改（2026-11-05，r139 扩充）：一碗汤的营养与效果默认值，注册 {@link SoupBowlItem} 时记进模板。
     * 路径 soup_bowl.&lt;汤底&gt;：hunger / saturation 是喝完回的营养；
     * duration / amplifier 是主效果（第一条风味），second_duration / second_amplifier 是副效果（第二条风味）；
     * fire_* 是红汤特辣以上的抗火，hunger_* 是红汤的负面饥饿。r139 起不再有 slow_*（高汤不再缓慢）。
     * 每种汤底都写满 10 个键（没有的效果写 0），玩家一眼能看到全部可调项。
     */
    public static void rememberSoupBowl(String id, int hunger, float saturation, int duration, int amplifier,
                                        int secondDuration, int secondAmplifier,
                                        int fireDuration, int fireAmplifier,
                                        int hungerDuration, int hungerAmplifier) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "soup_bowl", id);
        node.addProperty("hunger", hunger);
        node.addProperty("saturation", saturation);
        node.addProperty("duration", duration);
        node.addProperty("amplifier", amplifier);
        node.addProperty("second_duration", secondDuration);
        node.addProperty("second_amplifier", secondAmplifier);
        node.addProperty("fire_duration", fireDuration);
        node.addProperty("fire_amplifier", fireAmplifier);
        node.addProperty("hunger_duration", hungerDuration);
        node.addProperty("hunger_amplifier", hungerAmplifier);
    }

    /** mryt233 改（2026-11-05）：某种饮料每口正面效果的默认时长与等级，路径 drink.&lt;饮料&gt;。 */
    public static void rememberDrinkEffect(String id, int duration, int amplifier) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "drink", id);
        node.addProperty("duration", duration);
        node.addProperty("amplifier", amplifier);
    }

    /**
     * mryt233 改（2026-11-05）：生食材的默认涮煮刻数，路径 cooking.items.&lt;物品注册路径&gt;。
     * 每种食材单独一行，快煮慢煮按食材特性区分（见 {@link HotPotCooking} 的登记表）。
     */
    public static void rememberItemCookTicks(String itemId, int ticks) {
        ConfigStore.make(STORE.defaults(), "cooking", "items").addProperty(itemId, ticks);
    }

    /**
     * mryt233 改（2026-11-04）：豆腐的锅里煮制时间。煮熟 200 刻（10 秒），从下锅到煮过头 600 刻（30 秒）。
     * 跟其他食物数值一样，默认值留在这里、注册时记进模板、用时现读。
     * r134b：和 `cooking.fallback_ticks` 放在同一组（`cooking.tofu_cook_ticks` / `cooking.tofu_overcook_ticks`）。
     */
    public static void rememberTofuCooking(int cookTicks, int overcookTicks) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "cooking");
        node.addProperty("tofu_cook_ticks", cookTicks);
        node.addProperty("tofu_overcook_ticks", overcookTicks);
    }

    public static void rememberCooking(int ticks, float xp, float perItem) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "cooking");
        node.addProperty("fallback_ticks", ticks);
        node.addProperty("fallback_xp", xp);
        node.addProperty("xp_per_item", perItem);
    }

    // ------------------------------------------------------------------ 火锅规则

    private static NumericConfig.Rule rule(String path) {
        // mryt233 改（2026-11-05，r135e）：客户端粒子强度百分比 0～100 整数，0=关闭粒子
        if (path.equals("client.particle_intensity")) return new NumericConfig.Rule(0, 100, true);
        // mryt233 改（2026-11-05）：生食材涮煮刻数最先接住，路径 cooking.items.<物品注册路径>，值就是刻数本身。
        // 放在最前面是因为食材 ID 由玩家 / 数据包决定，万一有人登记了以 _duration 结尾的 ID 也不会被下面的后缀规则抢走。
        if (path.startsWith("cooking.items.")) return new NumericConfig.Rule(1, 100000, true);
        if (path.endsWith(".saturation")) return new NumericConfig.Rule(0, 10, false);
        // 原算法最低速度为 0.1，避免配置 0 却仍烹饪的歧义。
        if (path.startsWith("soup_speed.") || path.endsWith(".power")) return new NumericConfig.Rule(0.1, 10, false);
        if (path.endsWith(".hunger")) return new NumericConfig.Rule(0, 100, true);
        if (path.endsWith(".amplifier")) return new NumericConfig.Rule(0, 9, true);
        if (path.endsWith(".duration") || path.endsWith(".burn_ticks")) return new NumericConfig.Rule(0, 100000, true);
        // mryt233 改（2026-11-05）：汤碗的副效果与红汤专属键是 second_duration / second_amplifier /
        // fire_duration / fire_amplifier / hunger_duration / hunger_amplifier，后缀前面是下划线不是点，
        // 上面的 .hunger / .duration / .amplifier 规则都吃不到，这里单独接住。
        if (path.endsWith("_duration")) return new NumericConfig.Rule(0, 100000, true);
        if (path.endsWith("_amplifier")) return new NumericConfig.Rule(0, 9, true);
        if (path.endsWith(".fallback_ticks")) return new NumericConfig.Rule(1, 100000, true);
        if (path.equals("cooking.tofu_cook_ticks") || path.equals("cooking.tofu_overcook_ticks")) return new NumericConfig.Rule(1, 100000, true);
        if (path.endsWith(".fallback_xp") || path.endsWith(".xp_per_item")) return new NumericConfig.Rule(0, 100, false);
        return null;
    }

    private static JsonObject fieldHelp() {
        JsonObject help = new JsonObject();
        help.addProperty("food.<物品>.hunger", "饥饿点数 0～100 整数；2点为一个完整图标");
        help.addProperty("food.<物品>.saturation", "饱和度系数 0～10；不是饱和点数");
        help.addProperty("soup_bowl.<汤底>.hunger", "一碗汤回多少饥饿点数 0～100 整数");
        help.addProperty("soup_bowl.<汤底>.saturation", "一碗汤的饱和度系数 0～10，不是饱和点数");
        help.addProperty("soup_bowl.<汤底>.duration", "主效果持续 0～100000 tick，0=不加；20tick=1秒（高汤抗性、红汤力量、清汤/番茄速度与急迫、三鲜水下呼吸、菌汤生命恢复）");
        help.addProperty("soup_bowl.<汤底>.amplifier", "主效果等级 0～9 整数，0=I级");
        help.addProperty("soup_bowl.<汤底>.second_duration", "副效果持续 0～100000 tick，0=不加；20tick=1秒（高汤伤害吸收、红汤速度、三鲜幸运、清汤饱和）");
        help.addProperty("soup_bowl.<汤底>.second_amplifier", "副效果等级 0～9 整数，0=I级");
        help.addProperty("soup_bowl.<汤底>.fire_duration", "红汤「抗火」持续 0～100000 tick，0=不加（只有红汤用，特辣起生效；变态辣自动翻倍）；20tick=1秒");
        help.addProperty("soup_bowl.<汤底>.fire_amplifier", "红汤「抗火」等级 0～9 整数（只有红汤用）");
        help.addProperty("soup_bowl.<汤底>.hunger_duration", "红汤负面「饥饿」持续 0～100000 tick，0=不加（只有红汤用，特辣起生效）；20tick=1秒");
        help.addProperty("soup_bowl.<汤底>.hunger_amplifier", "红汤负面「饥饿」等级 0～9 整数（只有红汤用）");
        help.addProperty("soup_speed.<汤底>", "加热倍率 0.1～10");
        help.addProperty("fuel.<燃料>.power", "加热倍率 0.1～10");
        help.addProperty("fuel.<燃料>.burn_ticks", "联动燃料的默认时长 0～100000 tick；原版燃料表仍优先");
        help.addProperty("seasoning.<蘸料>.duration", "持续 0～100000 tick，0禁用；20tick=1秒");
        help.addProperty("seasoning.<蘸料>.amplifier", "等级 0～9 整数；0=I级");
        help.addProperty("seasoning.msg.hunger", "味精额外饥饿点数 0～100 整数，其他蘸料不读取此字段");
        help.addProperty("drink.sip.hunger", "饮料瓶 / 一杯饮料每口回的饥饿点数 0～100 整数（水、牛奶不回）");
        help.addProperty("drink.sip.saturation", "饮料每口的饱和度系数 0～10");
        help.addProperty("drink.<饮料>.duration", "这种饮料每口的正面效果持续 0～100000 tick，0=不加；20tick=1秒");
        help.addProperty("drink.<饮料>.amplifier", "这种饮料每口正面效果等级 0～9 整数，0=I级");
        help.addProperty("cooking.fallback_ticks", "兜底时间 1～100000 tick");
        help.addProperty("cooking.fallback_xp", "无配方出锅经验 0～100");
        help.addProperty("cooking.xp_per_item", "有配方出锅经验下限 0～100");
        help.addProperty("cooking.tofu_cook_ticks", "整块冷豆腐在锅里煮成热豆腐的刻数 1～100000；20tick=1秒");
        help.addProperty("cooking.tofu_overcook_ticks", "豆腐从下锅到煮过头的总刻数 1～100000（含煮熟那段），要大于 tofu_cook_ticks；20tick=1秒");
        help.addProperty("cooking.items.<食材>", "这种生食材在锅里煮熟的刻数 1～100000，20tick=1秒；写进这里就不用去改数据包熔炉配方");
        help.addProperty("client.particle_intensity", "火锅热气与火星粒子强度百分比 0～100 整数；0=关闭粒子，100=标准强度（仅客户端本地生效）");
        return help;
    }
}
