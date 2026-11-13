package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.game.ConfigBase;
import cn.blockforge.orient.framework.pure.ConfigStore;
import cn.blockforge.orient.framework.pure.ConfigWarning;
import cn.blockforge.orient.framework.pure.NumericConfig;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

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
    // mryt233 改（2026-11-10）：保留原有构造入口，同时携带保存回执。
    public record Loaded(JsonObject data, List<ConfigWarning> warnings,
                         ConfigStore.SaveStatus saveStatus, String reason, Path backup) {
        public Loaded(JsonObject data, List<ConfigWarning> warnings) {
            this(data, warnings, null, "", null);
        }

        public Loaded {
            warnings = List.copyOf(warnings);
        }
    }

    /** 一条配置警告按玩家语言显示；没有翻译时显示英文。 */
    public static Component describe(ConfigWarning warning) {
        return Component.translatableWithFallback(
            warning.key(), warning.fallback(), warning.args().toArray());
    }

    private static final String FILE_NAME = HotPot.MOD_ID + ".json";
    private static final long MAX_BYTES = 65536;

    private static final ConfigStore STORE = new ConfigStore(
        FILE_NAME, 1, MAX_BYTES, NOTE, Config::rule, Config::fieldHelp,
        message -> HotPot.LOGGER.warn("[hotpot] {}", message));
    private static final ConfigBase BASE = new ConfigBase(STORE);

    static {
        // mryt233 改（2026-11-05，r135e）：客户端项必须在 load() 之前登记进模板，
        // 否则 client.particle_intensity 会被判成 unknown_key。这里放在静态初始化块最稳妥：
        // SoupBase / Seasoning 的静态块虽然也会调 rememberXxx，但都晚于 Config 类自身的初始化。
        rememberClientDefaults();
        // mryt233 改（第四批 4I / 4J / 4K）：围锅回血、冷锅反胃、干杯、辣度递增与战利品默认值一并进入模板，
        // 保证单测直接查 defaultsCopy() 或 load() 时不漏键；HotPotRuntime 启动期也会在 init 前再登记一次。
        rememberGameplayDefaults();
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

    /** 模板默认数值扁平缓存（4F-2 区分「配置显式修改」与「自动补全的模板默认值」）。 */
    private static volatile java.util.Map<String, Double> defaultNumbers;

    private Config() {
    }

    public static void init(Path directory, boolean dedicated) {
        Config.directory = directory;
        defaultNumbers = null;
        migrateLegacyKeys();
        BASE.init(directory, dedicated);
        // mryt233 改（2026-11-05，r135e）：读完本地文件后把客户端粒子强度取回来
        readLocalParticleIntensity();
    }

    public static void load() {
        defaultNumbers = null;
        migrateLegacyKeys();
        BASE.load();
        // mryt233 改（2026-11-05，r135e）：同上，load 之后本地文件里的值才是准的
        readLocalParticleIntensity();
    }

    public static Loaded reload() {
        defaultNumbers = null;
        migrateLegacyKeys();
        ConfigStore.Loaded loaded = BASE.reload();
        // mryt233 改（2026-11-05，r135e）：热重载也要刷新本地粒子强度
        readLocalParticleIntensity();
        HotPotCooking.reviseCookTimes(); // mryt233 改（2026-11-10）：已下锅食材保留熟度并刷新时长。
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
        if (value == null) {
            // mryt233 改（2026-11-10）：关服后不再持有玩家或配方管理器。
            CuttingBoardBlock.clearCache();
            ClothApronProtection.clearCache();
        }
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
        // mryt233 改（2026-11-10）：退服释放客户端缓存。
        CuttingBoardBlock.clearCache();
        ClothApronProtection.clearCache();
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

    /** mryt233 改（2026-11-10）：保存结果如实回传；文件备份和安全写入统一交给 ConfigStore。 */
    public static synchronized Loaded saveAndReload(JsonObject edited) {
        defaultNumbers = null;
        migrateLegacyKeys();
        ConfigStore.Saved saved = STORE.saveEdited(edited);
        if (saved.status() != ConfigStore.SaveStatus.FAILED) {
            readLocalParticleIntensity();
            HotPotCooking.reviseCookTimes();
            if (activeServer != null && !activeServer.isDedicatedServer()) {
                BASE.acceptServer(saved.loaded().data());
            }
        }
        return new Loaded(saved.loaded().data(), saved.loaded().details(),
            saved.status(), saved.reason(), saved.backup());
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

    /**
     * 仅当配置文件中显式存在且数值与模板默认值不同时才覆盖 {@code fallback}，
     * 用于实现「配置文件显式修改 &gt; 数据包 &gt; 代码默认值」优先级（4F-2 拍板 12）。
     */
    public static float overriddenReal(String path, float fallback) {
        if (!has(path)) {
            return fallback;
        }
        double current = real(path, fallback);
        Double def = defaultsNumbers().get(path);
        return (def == null || Math.abs(current - def) > 1e-6) ? (float) current : fallback;
    }

    /** {@link #overriddenReal} 的整数版本。 */
    public static int overriddenInt(String path, int fallback) {
        if (!has(path)) {
            return fallback;
        }
        int current = integer(path, fallback);
        Double def = defaultsNumbers().get(path);
        return (def == null || current != (int) (double) def) ? current : fallback;
    }

    private static java.util.Map<String, Double> defaultsNumbers() {
        java.util.Map<String, Double> cached = defaultNumbers;
        if (cached != null) {
            return cached;
        }
        synchronized (Config.class) {
            if (defaultNumbers != null) {
                return defaultNumbers;
            }
            java.util.Map<String, Double> built = new java.util.HashMap<>();
            flattenNumbers("", STORE.defaults(), built);
            defaultNumbers = java.util.Map.copyOf(built);
            return defaultNumbers;
        }
    }

    private static void flattenNumbers(String prefix, JsonObject obj, java.util.Map<String, Double> out) {
        for (var entry : obj.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            var value = entry.getValue();
            if (value.isJsonObject()) {
                flattenNumbers(key, value.getAsJsonObject(), out);
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                out.put(key, value.getAsDouble());
            }
        }
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

    /** 4F-2：配置显式修改 > 数据包 > 代码默认值（仅对内置汤底使用）。 */
    public static float overriddenSoupSpeed(String id, float fallback) {
        return overriddenReal("soup_speed." + id, fallback);
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

    /** 4F-2：配置显式修改 > 数据包 > 代码默认值。 */
    public static int overriddenSoupBowlInt(String id, String key, int fallback) {
        return overriddenInt("soup_bowl." + id + "." + key, fallback);
    }

    /** mryt233 改（2026-11-05）：汤碗的小数项（目前只有 saturation），路径 soup_bowl.&lt;汤底&gt;.&lt;键&gt;。 */
    public static float soupBowlFloat(String id, String key, float fallback) {
        return (float) real("soup_bowl." + id + "." + key, fallback);
    }

    /** 4F-2：配置显式修改 > 数据包 > 代码默认值。 */
    public static float overriddenSoupBowlFloat(String id, String key, float fallback) {
        return overriddenReal("soup_bowl." + id + "." + key, fallback);
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

    /** 4F-2：配置显式修改 > 数据包 > 代码默认值。 */
    public static int overriddenSeasoningTicks(String id, int fallback) {
        return overriddenInt("seasoning." + id + ".duration", fallback);
    }

    public static int seasoningAmplifier(String id, int fallback) {
        return integer("seasoning." + id + ".amplifier", fallback);
    }

    /** 4F-2：配置显式修改 > 数据包 > 代码默认值。 */
    public static int overriddenSeasoningAmplifier(String id, int fallback) {
        return overriddenInt("seasoning." + id + ".amplifier", fallback);
    }

    public static int seasoningHunger(String id, int fallback) {
        return integer("seasoning." + id + ".hunger", fallback);
    }

    /** 4F-2：配置显式修改 > 数据包 > 代码默认值。 */
    public static int overriddenSeasoningHunger(String id, int fallback) {
        return overriddenInt("seasoning." + id + ".hunger", fallback);
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

    // ------------------------------------------------------------------ 4I / 4J / 4K 新增默认值常量与访问器（mryt233 改）

    static final int DEFAULT_WARMTH_INTERVAL_TICKS = 80;
    static final int DEFAULT_WARMTH_RANGE = 6;
    static final int DEFAULT_WARMTH_DURATION = 200;
    static final int DEFAULT_WARMTH_AMPLIFIER = 0;

    static final float DEFAULT_COLD_POT_NAUSEA_CHANCE = 0.3F;
    static final int DEFAULT_COLD_POT_NAUSEA_DURATION = 300;

    static final int DEFAULT_CHEERS_COOLDOWN_TICKS = 20;
    static final int DEFAULT_CHEERS_LUCK_DURATION = 600;
    static final int DEFAULT_CHEERS_LUCK_AMPLIFIER = 0;
    static final int DEFAULT_CHEERS_REGEN_DURATION = 100;
    static final int DEFAULT_CHEERS_REGEN_AMPLIFIER = 0;
    static final int DEFAULT_CHEERS_BONUS_HUNGER = 1;
    static final float DEFAULT_CHEERS_BONUS_SATURATION = 0.4F;

    static final int DEFAULT_SPICY_LEVEL_DURATION_STEP = 60;
    static final int DEFAULT_SPICY_LEVEL_AMPLIFIER_FROM = 1;
    static final int DEFAULT_SPICY_SECOND_BONUS_L1 = 60;
    static final int DEFAULT_SPICY_SECOND_BONUS_L2 = 80;
    static final int DEFAULT_SPICY_SECOND_BONUS_L3 = 120;
    static final int DEFAULT_SPICY_FIRE_FROM_LEVEL = 2;
    static final int DEFAULT_SPICY_FIRE_TOP_MULTIPLIER = 2;
    static final int DEFAULT_SPICY_HUNGER_TOP_BONUS = 40;
    static final int DEFAULT_SPICY_MOUTH_FIRE_FROM_LEVEL = 3;
    static final int DEFAULT_SPICY_MOUTH_FIRE_TICKS = 40;

    static final int DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT = 20;
    static final float DEFAULT_LOOT_LILY_PAD_LOTUS_ROOT_CHANCE = 0.5F;
    static final float DEFAULT_LOOT_MUSHROOM_ENOKI_CHANCE = 0.5F;
    static final float DEFAULT_LOOT_COW_BEEF_LOIN_CHANCE = 0.25F;
    static final float DEFAULT_LOOT_COW_BEEF_LOIN_LOOTING_BONUS = 0.1F;

    public static int warmthIntervalTicks() {
        return integer("warmth.interval_ticks", DEFAULT_WARMTH_INTERVAL_TICKS);
    }

    public static int warmthRange() {
        return integer("warmth.range", DEFAULT_WARMTH_RANGE);
    }

    public static int warmthDuration() {
        return integer("warmth.duration", DEFAULT_WARMTH_DURATION);
    }

    public static int warmthAmplifier() {
        return integer("warmth.amplifier", DEFAULT_WARMTH_AMPLIFIER);
    }

    public static float coldPotNauseaChance() {
        return (float) real("cold_pot.nausea_chance", DEFAULT_COLD_POT_NAUSEA_CHANCE);
    }

    public static int coldPotNauseaDuration() {
        return integer("cold_pot.nausea_duration", DEFAULT_COLD_POT_NAUSEA_DURATION);
    }

    public static int cheersCooldownTicks() {
        return integer("cheers.cooldown_ticks", DEFAULT_CHEERS_COOLDOWN_TICKS);
    }

    public static int cheersLuckDuration() {
        return integer("cheers.luck_duration", DEFAULT_CHEERS_LUCK_DURATION);
    }

    public static int cheersLuckAmplifier() {
        return integer("cheers.luck_amplifier", DEFAULT_CHEERS_LUCK_AMPLIFIER);
    }

    public static int cheersRegenDuration() {
        return integer("cheers.regen_duration", DEFAULT_CHEERS_REGEN_DURATION);
    }

    public static int cheersRegenAmplifier() {
        return integer("cheers.regen_amplifier", DEFAULT_CHEERS_REGEN_AMPLIFIER);
    }

    public static int cheersBonusHunger() {
        return integer("cheers.bonus.hunger", DEFAULT_CHEERS_BONUS_HUNGER);
    }

    public static float cheersBonusSaturation() {
        return (float) real("cheers.bonus.saturation", DEFAULT_CHEERS_BONUS_SATURATION);
    }

    public static int spicyLevelDurationStep() {
        return integer("spicy.level_duration_step", DEFAULT_SPICY_LEVEL_DURATION_STEP);
    }

    public static int spicyLevelAmplifierFrom() {
        return integer("spicy.level_amplifier_from", DEFAULT_SPICY_LEVEL_AMPLIFIER_FROM);
    }

    public static int spicySecondBonusL1() {
        return integer("spicy.second_bonus_l1", DEFAULT_SPICY_SECOND_BONUS_L1);
    }

    public static int spicySecondBonusL2() {
        return integer("spicy.second_bonus_l2", DEFAULT_SPICY_SECOND_BONUS_L2);
    }

    public static int spicySecondBonusL3() {
        return integer("spicy.second_bonus_l3", DEFAULT_SPICY_SECOND_BONUS_L3);
    }

    public static int spicyFireFromLevel() {
        return integer("spicy.fire_from_level", DEFAULT_SPICY_FIRE_FROM_LEVEL);
    }

    public static int spicyFireTopMultiplier() {
        return integer("spicy.fire_top_multiplier", DEFAULT_SPICY_FIRE_TOP_MULTIPLIER);
    }

    public static int spicyHungerTopBonus() {
        return integer("spicy.hunger_top_bonus", DEFAULT_SPICY_HUNGER_TOP_BONUS);
    }

    public static int spicyMouthFireFromLevel() {
        return integer("spicy.mouth_fire_from_level", DEFAULT_SPICY_MOUTH_FIRE_FROM_LEVEL);
    }

    public static int spicyMouthFireTicks() {
        return integer("spicy.mouth_fire_ticks", DEFAULT_SPICY_MOUTH_FIRE_TICKS);
    }

    public static int lootShrimpFishingWeight() {
        return integer("loot.shrimp_fishing.weight", DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT);
    }

    public static float lootGarnishChance(String id, float fallback) {
        return (float) real("loot.garnish." + id + ".chance", fallback);
    }

    public static float lootLilyPadLotusRootChance() {
        return (float) real("loot.lily_pad.lotus_root.chance", DEFAULT_LOOT_LILY_PAD_LOTUS_ROOT_CHANCE);
    }

    public static float lootMushroomEnokiChance() {
        return (float) real("loot.mushroom.enoki.chance", DEFAULT_LOOT_MUSHROOM_ENOKI_CHANCE);
    }

    public static float lootCowBeefLoinChance() {
        return (float) real("loot.cow.beef_loin.chance", DEFAULT_LOOT_COW_BEEF_LOIN_CHANCE);
    }

    public static float lootCowBeefLoinLootingBonus() {
        return (float) real("loot.cow.beef_loin.looting_bonus", DEFAULT_LOOT_COW_BEEF_LOIN_LOOTING_BONUS);
    }

    // ------------------------------------------------------------------ 默认值登记

    public static void rememberFood(String id, int hunger, float saturation) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "food", id);
        node.addProperty("hunger", hunger);
        node.addProperty("saturation", saturation);
    }

    public static void rememberSoupSpeed(String id, float speed) {
        defaultNumbers = null;
        ConfigStore.make(STORE.defaults(), "soup_speed").addProperty(id, speed);
    }

    public static void rememberFuel(String id, float power, int ticks) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "fuel", id);
        node.addProperty("power", power);
        node.addProperty("burn_ticks", ticks);
    }

    public static void rememberSeasoning(String id, int duration, int amplifier, int hunger) {
        defaultNumbers = null;
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
        defaultNumbers = null;
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

    /** mryt233 改（第四批 4I）：围锅回血默认参数，路径 warmth.*。 */
    public static void rememberWarmth(int intervalTicks, int range, int duration, int amplifier) {
        defaultNumbers = null;
        JsonObject node = ConfigStore.make(STORE.defaults(), "warmth");
        node.addProperty("interval_ticks", intervalTicks);
        node.addProperty("range", range);
        node.addProperty("duration", duration);
        node.addProperty("amplifier", amplifier);
    }

    /** mryt233 改（第四批 4I）：冷锅吃菜反胃默认参数，路径 cold_pot.*。 */
    public static void rememberColdPot(float nauseaChance, int nauseaDuration) {
        defaultNumbers = null;
        JsonObject node = ConfigStore.make(STORE.defaults(), "cold_pot");
        node.addProperty("nausea_chance", nauseaChance);
        node.addProperty("nausea_duration", nauseaDuration);
    }

    /** mryt233 改（第四批 4I）：干杯默认参数，路径 cheers.*。 */
    public static void rememberCheers(int cooldownTicks, int luckDuration, int luckAmplifier,
                                      int regenDuration, int regenAmplifier,
                                      int bonusHunger, float bonusSaturation) {
        defaultNumbers = null;
        JsonObject node = ConfigStore.make(STORE.defaults(), "cheers");
        node.addProperty("cooldown_ticks", cooldownTicks);
        node.addProperty("luck_duration", luckDuration);
        node.addProperty("luck_amplifier", luckAmplifier);
        node.addProperty("regen_duration", regenDuration);
        node.addProperty("regen_amplifier", regenAmplifier);
        JsonObject bonus = ConfigStore.make(node, "bonus");
        bonus.addProperty("hunger", bonusHunger);
        bonus.addProperty("saturation", bonusSaturation);
    }

    /** mryt233 改（第四批 4J）：红汤辣度递增与嘴上冒火默认参数，路径 spicy.*。 */
    public static void rememberSpicy(int levelDurationStep, int levelAmplifierFrom,
                                     int secondBonusL1, int secondBonusL2, int secondBonusL3,
                                     int fireFromLevel, int fireTopMultiplier, int hungerTopBonus,
                                     int mouthFireFromLevel, int mouthFireTicks) {
        defaultNumbers = null;
        JsonObject node = ConfigStore.make(STORE.defaults(), "spicy");
        node.addProperty("level_duration_step", levelDurationStep);
        node.addProperty("level_amplifier_from", levelAmplifierFrom);
        node.addProperty("second_bonus_l1", secondBonusL1);
        node.addProperty("second_bonus_l2", secondBonusL2);
        node.addProperty("second_bonus_l3", secondBonusL3);
        node.addProperty("fire_from_level", fireFromLevel);
        node.addProperty("fire_top_multiplier", fireTopMultiplier);
        node.addProperty("hunger_top_bonus", hungerTopBonus);
        node.addProperty("mouth_fire_from_level", mouthFireFromLevel);
        node.addProperty("mouth_fire_ticks", mouthFireTicks);
    }

    /** mryt233 改（第四批 4K）：钓鱼出虾、睡莲莲藕、蘑菇金针菇与牛脊肉掉落默认值。 */
    public static void rememberLoot(int shrimpFishingWeight, float lilyPadLotusRootChance,
                                    float mushroomEnokiChance, float cowBeefLoinChance,
                                    float cowBeefLoinLootingBonus) {
        defaultNumbers = null;
        ConfigStore.make(STORE.defaults(), "loot", "shrimp_fishing")
            .addProperty("weight", shrimpFishingWeight);
        ConfigStore.make(STORE.defaults(), "loot", "lily_pad", "lotus_root")
            .addProperty("chance", lilyPadLotusRootChance);
        ConfigStore.make(STORE.defaults(), "loot", "mushroom", "enoki")
            .addProperty("chance", mushroomEnokiChance);
        JsonObject cow = ConfigStore.make(STORE.defaults(), "loot", "cow", "beef_loin");
        cow.addProperty("chance", cowBeefLoinChance);
        cow.addProperty("looting_bonus", cowBeefLoinLootingBonus);
    }

    /** mryt233 改（第四批 4K）：打草掉落配菜的默认概率，路径 loot.garnish.&lt;id&gt;.chance。 */
    public static void rememberGarnishLoot(String id, float chance) {
        defaultNumbers = null;
        ConfigStore.make(STORE.defaults(), "loot", "garnish", id).addProperty("chance", chance);
    }

    private static void rememberGameplayDefaults() {
        rememberWarmth(DEFAULT_WARMTH_INTERVAL_TICKS, DEFAULT_WARMTH_RANGE,
            DEFAULT_WARMTH_DURATION, DEFAULT_WARMTH_AMPLIFIER);
        rememberColdPot(DEFAULT_COLD_POT_NAUSEA_CHANCE, DEFAULT_COLD_POT_NAUSEA_DURATION);
        rememberCheers(DEFAULT_CHEERS_COOLDOWN_TICKS,
            DEFAULT_CHEERS_LUCK_DURATION, DEFAULT_CHEERS_LUCK_AMPLIFIER,
            DEFAULT_CHEERS_REGEN_DURATION, DEFAULT_CHEERS_REGEN_AMPLIFIER,
            DEFAULT_CHEERS_BONUS_HUNGER, DEFAULT_CHEERS_BONUS_SATURATION);
        rememberSpicy(DEFAULT_SPICY_LEVEL_DURATION_STEP, DEFAULT_SPICY_LEVEL_AMPLIFIER_FROM,
            DEFAULT_SPICY_SECOND_BONUS_L1, DEFAULT_SPICY_SECOND_BONUS_L2, DEFAULT_SPICY_SECOND_BONUS_L3,
            DEFAULT_SPICY_FIRE_FROM_LEVEL, DEFAULT_SPICY_FIRE_TOP_MULTIPLIER, DEFAULT_SPICY_HUNGER_TOP_BONUS,
            DEFAULT_SPICY_MOUTH_FIRE_FROM_LEVEL, DEFAULT_SPICY_MOUTH_FIRE_TICKS);
        rememberLoot(DEFAULT_LOOT_SHRIMP_FISHING_WEIGHT, DEFAULT_LOOT_LILY_PAD_LOTUS_ROOT_CHANCE,
            DEFAULT_LOOT_MUSHROOM_ENOKI_CHANCE, DEFAULT_LOOT_COW_BEEF_LOIN_CHANCE,
            DEFAULT_LOOT_COW_BEEF_LOIN_LOOTING_BONUS);
        rememberGarnishLoot("scallion", 0.25F);
        rememberGarnishLoot("cilantro", 0.25F);
        rememberGarnishLoot("soybean", 0.15F);
        rememberGarnishLoot("garlic", 0.15F);
        rememberGarnishLoot("tomato", 0.15F);
        rememberGarnishLoot("chili_pepper", 0.15F);
        rememberGarnishLoot("peanut", 0.15F);
        rememberGarnishLoot("greens", 0.15F);
        rememberGarnishLoot("zhe_er_gen", 0.15F);
    }

    // ------------------------------------------------------------------ 火锅规则

    static NumericConfig.Rule rule(String path) {
        // mryt233 改（2026-11-05，r135e）：客户端粒子强度百分比 0～100 整数，0=关闭粒子
        if (path.equals("client.particle_intensity")) return new NumericConfig.Rule(0, 100, true);
        // mryt233 改（2026-11-05）：生食材涮煮刻数最先接住，路径 cooking.items.<物品注册路径>，值就是刻数本身。
        // 放在最前面是因为食材 ID 由玩家 / 数据包决定，万一有人登记了以 _duration 结尾的 ID 也不会被下面的后缀规则抢走。
        if (path.startsWith("cooking.items.")) return new NumericConfig.Rule(1, 100000, true);
        // mryt233 改（第四批 4K）：loot.* 规则放在后缀规则（.hunger / .saturation 等）之前。
        if (path.startsWith("loot.")) {
            if (path.endsWith(".weight")) return new NumericConfig.Rule(0, 1000, true);
            if (path.endsWith(".chance") || path.endsWith(".looting_bonus")) return new NumericConfig.Rule(0, 1, false);
            return null;
        }
        // mryt233 改（第四批 4I / 4J）：围锅回血、冷锅反胃、干杯、辣度递增的专属校验规则
        if (path.equals("warmth.interval_ticks") || path.equals("cheers.cooldown_ticks")) {
            return new NumericConfig.Rule(1, 1200, true);
        }
        if (path.equals("warmth.range")) return new NumericConfig.Rule(1, 32, true);
        if (path.equals("cold_pot.nausea_chance")) return new NumericConfig.Rule(0, 1, false);
        if (path.equals("spicy.level_duration_step")
            || path.equals("spicy.second_bonus_l1")
            || path.equals("spicy.second_bonus_l2")
            || path.equals("spicy.second_bonus_l3")
            || path.equals("spicy.hunger_top_bonus")) {
            return new NumericConfig.Rule(0, 10000, true);
        }
        if (path.equals("spicy.level_amplifier_from")) return new NumericConfig.Rule(0, 4, true);
        if (path.equals("spicy.fire_from_level") || path.equals("spicy.mouth_fire_from_level")) {
            return new NumericConfig.Rule(1, 4, true);
        }
        if (path.equals("spicy.fire_top_multiplier")) return new NumericConfig.Rule(1, 10, true);
        if (path.equals("spicy.mouth_fire_ticks")) return new NumericConfig.Rule(0, 1200, true);

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
        help.addProperty("food.<物品>.hunger", "吃下去回多少饥饿点数；2 点 = 一个鸡腿图标。范围 0～100");
        help.addProperty("food.<物品>.saturation", "回多少饱和度系数（不是点数），越大越顶饿。范围 0～10");
        help.addProperty("soup_bowl.<汤底>.hunger", "喝完这碗汤回多少饥饿点数；2 点 = 一个鸡腿图标。范围 0～100");
        help.addProperty("soup_bowl.<汤底>.saturation", "这碗汤的饱和度系数（不是点数），越大越顶饿。范围 0～10");
        help.addProperty("soup_bowl.<汤底>.duration", "喝完这碗汤的主效果持续多久，0 = 不加，20 tick = 1 秒（高汤抗性、红汤力量、清汤 / 番茄速度与急迫、三鲜水下呼吸、菌汤生命恢复）。范围 0～100000");
        help.addProperty("soup_bowl.<汤底>.amplifier", "主效果等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("soup_bowl.<汤底>.second_duration", "副效果持续多久，0 = 不加，20 tick = 1 秒（高汤伤害吸收、红汤速度、三鲜幸运、清汤饱和）。范围 0～100000");
        help.addProperty("soup_bowl.<汤底>.second_amplifier", "副效果等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("soup_bowl.<汤底>.fire_duration", "红汤「抗火」持续多久，0 = 不加；只有红汤用，特辣起生效，变态辣自动翻倍。20 tick = 1 秒，范围 0～100000");
        help.addProperty("soup_bowl.<汤底>.fire_amplifier", "红汤「抗火」的等级：0 = I 级，1 = II 级，最多 9（只有红汤用）。范围 0～9");
        help.addProperty("soup_bowl.<汤底>.hunger_duration", "红汤负面「饥饿」持续多久，0 = 不加；只有红汤用，特辣起生效。20 tick = 1 秒，范围 0～100000");
        help.addProperty("soup_bowl.<汤底>.hunger_amplifier", "红汤负面「饥饿」的等级：0 = I 级，最多 9（只有红汤用）。范围 0～9");
        help.addProperty("soup_speed.<汤底>", "这种汤底把菜涮熟的速度：1 = 标准，1.3 = 快三成，0.5 = 慢一半。范围 0.1～10");
        help.addProperty("fuel.<燃料>.power", "这种燃料的灶火有多旺（影响涮熟快慢）：1 = 标准，越大越快。范围 0.1～10");
        help.addProperty("fuel.<燃料>.burn_ticks", "联动模组登记的燃料能烧多久（20 tick = 1 秒）；原版燃料照原版自己的时长，不受这项影响。范围 0～100000");
        help.addProperty("seasoning.<蘸料>.duration", "这个蘸料的效果持续多久，0 = 不加效果，20 tick = 1 秒。范围 0～100000");
        help.addProperty("seasoning.<蘸料>.amplifier", "这个蘸料的效果等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("seasoning.msg.hunger", "蘸了味精的菜多吃回几点饥饿；2 点 = 一个鸡腿图标，其他蘸料不读这一项。范围 0～100");
        help.addProperty("drink.sip.hunger", "饮料每口回几点饥饿；2 点 = 一个鸡腿图标，水和牛奶不回。范围 0～100");
        help.addProperty("drink.sip.saturation", "饮料每口的饱和度系数（不是点数），越大越顶饿。范围 0～10");
        help.addProperty("drink.<饮料>.duration", "这种饮料每口的效果持续多久，0 = 不加，20 tick = 1 秒。范围 0～100000");
        help.addProperty("drink.<饮料>.amplifier", "这种饮料每口的效果等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("cooking.fallback_ticks", "没在配置里单独写时间的食材（原版 / 第三方）按这个时长涮熟，20 tick = 1 秒。范围 1～100000");
        help.addProperty("cooking.fallback_xp", "没有熔炉配方的食材出锅时给的经验值。范围 0～100");
        help.addProperty("cooking.xp_per_item", "有熔炉配方的食材每份至少给的经验值。范围 0～100");
        help.addProperty("cooking.tofu_cook_ticks", "整块冷豆腐放进锅里煮多久才变成热豆腐，20 tick = 1 秒。范围 1～100000");
        help.addProperty("cooking.tofu_overcook_ticks", "豆腐从下锅到煮碎的总时长，要比 tofu_cook_ticks 大，20 tick = 1 秒。范围 1～100000");
        help.addProperty("cooking.items.<食材>", "这种食材要涮多久才熟，20 tick = 1 秒；改这里不用去动数据包里的熔炉配方。范围 1～100000");
        help.addProperty("warmth.interval_ticks", "围锅回血每隔多少刻给锅边玩家续一次生命恢复，20 tick = 1 秒。范围 1～1200");
        help.addProperty("warmth.range", "围锅回血的作用半径（格）。范围 1～32");
        help.addProperty("warmth.duration", "围锅回血每次赋予生命恢复的持续时长，0 = 关闭围锅回血，20 tick = 1 秒。范围 0～100000");
        help.addProperty("warmth.amplifier", "围锅回血生命恢复的等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("cold_pot.nausea_chance", "用筷子从没加热的冷锅里夹菜吃时触发反胃的概率（0 = 不触发，1 = 必触发）。范围 0～1");
        help.addProperty("cold_pot.nausea_duration", "冷锅吃菜触发反胃的持续时长，0 = 关闭冷锅反胃，20 tick = 1 秒。范围 0～100000");
        help.addProperty("cheers.cooldown_ticks", "两次干杯之间的冷却时间，20 tick = 1 秒。范围 1～1200");
        help.addProperty("cheers.luck_duration", "干杯赋予幸运效果的持续时长，0 = 不加幸运，20 tick = 1 秒。范围 0～100000");
        help.addProperty("cheers.luck_amplifier", "干杯幸运效果的等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("cheers.regen_duration", "干杯赋予生命恢复的持续时长，0 = 不加生命恢复，20 tick = 1 秒。范围 0～100000");
        help.addProperty("cheers.regen_amplifier", "干杯生命恢复的等级：0 = I 级，1 = II 级，最多 9。范围 0～9");
        help.addProperty("cheers.bonus.hunger", "干过杯的饮料喝下去额外多回多少饥饿点数；2 点 = 一个鸡腿图标。范围 0～100");
        help.addProperty("cheers.bonus.saturation", "干过杯的饮料喝下去额外增加多少饱和度系数（不是点数）。范围 0～10");
        help.addProperty("spicy.level_duration_step", "红汤辣度每升一档，主效果（力量）额外延长的时长，20 tick = 1 秒。范围 0～10000");
        help.addProperty("spicy.level_amplifier_from", "红汤从第几档辣度起（0 微辣、1 中辣、2 特辣、3 变态辣；4 = 不升级）主效果等级 +1。范围 0～4");
        help.addProperty("spicy.second_bonus_l1", "红汤 1 档（中辣）副效果（速度）额外延长的时长，20 tick = 1 秒。范围 0～10000");
        help.addProperty("spicy.second_bonus_l2", "红汤 2 档（特辣）副效果（速度）额外延长的时长，20 tick = 1 秒。范围 0～10000");
        help.addProperty("spicy.second_bonus_l3", "红汤 3 档（变态辣）副效果（速度）额外延长的时长，20 tick = 1 秒。范围 0～10000");
        help.addProperty("spicy.fire_from_level", "红汤从第几档辣度起附加抗火与负面饥饿（4 = 不附加）。范围 1～4");
        help.addProperty("spicy.fire_top_multiplier", "红汤最高档（3 档变态辣）时抗火时长的倍率。范围 1～10");
        help.addProperty("spicy.hunger_top_bonus", "红汤最高档（3 档变态辣）时负面饥饿额外延长的时长（仅 hunger_duration > 0 时生效），20 tick = 1 秒。范围 0～10000");
        help.addProperty("spicy.mouth_fire_from_level", "红汤从第几档辣度起吃下会在嘴上冒无伤火苗（4 = 不冒火）。范围 1～4");
        help.addProperty("spicy.mouth_fire_ticks", "红汤嘴上冒火苗的持续时长，0 = 关闭，20 tick = 1 秒。范围 0～1200");
        help.addProperty("loot.shrimp_fishing.weight", "原版钓鱼鱼池中钓到虾的权重，0 = 钓不到（改后需 /reload 生效）。范围 0～1000");
        help.addProperty("loot.garnish.<配菜>.chance", "打草掉落该配菜的概率（0 = 不掉，1 = 必掉；改后需 /reload 生效）。范围 0～1");
        help.addProperty("loot.lily_pad.lotus_root.chance", "破坏睡莲（非剪刀）掉落莲藕的概率（改后需 /reload 生效）。范围 0～1");
        help.addProperty("loot.mushroom.enoki.chance", "破坏棕色 / 红色蘑菇掉落金针菇的概率（改后需 /reload 生效）。范围 0～1");
        help.addProperty("loot.cow.beef_loin.chance", "击杀牛 / 哞菇掉落牛脊肉的基础概率（改后需 /reload 生效）。范围 0～1");
        help.addProperty("loot.cow.beef_loin.looting_bonus", "抢夺附魔每级为牛脊肉追加的掉落概率（改后需 /reload 生效）。范围 0～1");
        help.addProperty("client.particle_intensity", "灶口火苗、火星与汤面热气的浓淡（0 = 关掉粒子，100 = 标准）；只对本机生效。范围 0～100");
        return help;
    }
}
