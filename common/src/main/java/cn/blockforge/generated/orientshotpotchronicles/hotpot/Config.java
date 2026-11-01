package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.game.ConfigBase;
import cn.blockforge.orient.framework.pure.ConfigStore;
import cn.blockforge.orient.framework.pure.ConfigWarning;
import cn.blockforge.orient.framework.pure.NumericConfig;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.List;

/**
 * 火锅的数值配置门面：键、默认值、范围规则和说明留在内容层；
 * 解析/校验/快照/文件补全由框架 {@link ConfigStore} + {@link ConfigBase} 完成（P1 第四批）。
 * 配置格式、文件名与字段含义保持不变。作者：mryt233 */
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

    private static final ConfigStore STORE = new ConfigStore(
        HotPot.MOD_ID + ".json", 1, 65536, NOTE, Config::rule, Config::fieldHelp,
        message -> HotPot.LOGGER.warn("[hotpot] {}", message));
    private static final ConfigBase BASE = new ConfigBase(STORE);

    private Config() {
    }

    public static void init(Path directory, boolean dedicated) {
        BASE.init(directory, dedicated);
    }

    public static void load() {
        BASE.load();
    }

    public static Loaded reload() {
        ConfigStore.Loaded loaded = BASE.reload();
        return new Loaded(loaded.data(), loaded.details());
    }

    public static void setServer(MinecraftServer value) {
        BASE.setServer(value);
    }

    public static JsonObject serverSnapshot() {
        return BASE.serverSnapshot();
    }

    public static void acceptServer(JsonObject data) {
        BASE.acceptServer(data);
    }

    public static void clearClient() {
        BASE.clearClient();
    }

    public static void saveDefaultsIfNeeded() {
        STORE.saveDefaultsIfNeeded();
    }

    // ------------------------------------------------------------------ 取值

    public static int foodHunger(String id, int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "food", id), "hunger", fallback);
    }

    public static float foodSaturation(String id, float fallback) {
        return ConfigStore.floatAt(ConfigStore.read(BASE.current(), "food", id), "saturation", fallback);
    }

    public static float soupSpeed(String id, float fallback) {
        return ConfigStore.floatAt(ConfigStore.read(BASE.current(), "soup_speed"), id, fallback);
    }

    public static float fuelPower(String id, float fallback) {
        return ConfigStore.floatAt(ConfigStore.read(BASE.current(), "fuel", id), "power", fallback);
    }

    public static int fuelTicks(String id, int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "fuel", id), "burn_ticks", fallback);
    }

    public static int seasoningTicks(String id, int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "seasoning", id), "duration", fallback);
    }

    public static int seasoningAmplifier(String id, int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "seasoning", id), "amplifier", fallback);
    }

    public static int seasoningHunger(String id, int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "seasoning", id), "hunger", fallback);
    }

    public static int drinkHunger(int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "drink", "sip"), "hunger", fallback);
    }

    public static float drinkSaturation(float fallback) {
        return ConfigStore.floatAt(ConfigStore.read(BASE.current(), "drink", "sip"), "saturation", fallback);
    }

    public static int fallbackTicks(int fallback) {
        return ConfigStore.intAt(ConfigStore.read(BASE.current(), "cooking"), "fallback_ticks", fallback);
    }

    public static float fallbackXp(float fallback) {
        return ConfigStore.floatAt(ConfigStore.read(BASE.current(), "cooking"), "fallback_xp", fallback);
    }

    public static float xpPerItem(float fallback) {
        return ConfigStore.floatAt(ConfigStore.read(BASE.current(), "cooking"), "xp_per_item", fallback);
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

    public static void rememberCooking(int ticks, float xp, float perItem) {
        JsonObject node = ConfigStore.make(STORE.defaults(), "cooking");
        node.addProperty("fallback_ticks", ticks);
        node.addProperty("fallback_xp", xp);
        node.addProperty("xp_per_item", perItem);
    }

    // ------------------------------------------------------------------ 火锅规则

    private static NumericConfig.Rule rule(String path) {
        if (path.endsWith(".saturation")) return new NumericConfig.Rule(0, 10, false);
        // 原算法最低速度为 0.1，避免配置 0 却仍烹饪的歧义。
        if (path.startsWith("soup_speed.") || path.endsWith(".power")) return new NumericConfig.Rule(0.1, 10, false);
        if (path.endsWith(".hunger")) return new NumericConfig.Rule(0, 100, true);
        if (path.endsWith(".amplifier")) return new NumericConfig.Rule(0, 9, true);
        if (path.endsWith(".duration") || path.endsWith(".burn_ticks")) return new NumericConfig.Rule(0, 100000, true);
        if (path.endsWith(".fallback_ticks")) return new NumericConfig.Rule(1, 100000, true);
        if (path.endsWith(".fallback_xp") || path.endsWith(".xp_per_item")) return new NumericConfig.Rule(0, 100, false);
        return null;
    }

    private static JsonObject fieldHelp() {
        JsonObject help = new JsonObject();
        help.addProperty("food.<物品>.hunger", "饥饿点数 0～100 整数；2点为一个完整图标");
        help.addProperty("food.<物品>.saturation", "饱和度系数 0～10；不是饱和点数");
        help.addProperty("soup_speed.<汤底>", "加热倍率 0.1～10");
        help.addProperty("fuel.<燃料>.power", "加热倍率 0.1～10");
        help.addProperty("fuel.<燃料>.burn_ticks", "联动燃料的默认时长 0～100000 tick；原版燃料表仍优先");
        help.addProperty("seasoning.<蘸料>.duration", "持续 0～100000 tick，0禁用；20tick=1秒");
        help.addProperty("seasoning.<蘸料>.amplifier", "等级 0～9 整数；0=I级");
        help.addProperty("seasoning.msg.hunger", "味精额外饥饿点数 0～100 整数，其他蘸料不读取此字段");
        help.addProperty("drink.sip.hunger", "饮料瓶 / 一杯饮料每口回的饥饿点数 0～100 整数（水、牛奶不回）");
        help.addProperty("drink.sip.saturation", "饮料每口的饱和度系数 0～10");
        help.addProperty("cooking.fallback_ticks", "兜底时间 1～100000 tick");
        help.addProperty("cooking.fallback_xp", "无配方出锅经验 0～100");
        help.addProperty("cooking.xp_per_item", "有配方出锅经验下限 0～100");
        return help;
    }
}