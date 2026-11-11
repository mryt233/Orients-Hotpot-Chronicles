package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 汤底体系：全部由原版物品参与（水/牛奶/蘑菇煲/岩浆桶）。
 * 汤底决定加热速度，并决定在它里面涮熟的菜、盛出来的汤喝下去时带的状态效果
 * （结算见 {@link #dishBonusEffects(int)}，吃的那一刻现读配置，改完 /orientshotpot reload 立即生效）。
 *
 * <p>mryt233 改（2026-11-10，r147）：这一批把「一种汤底的全部属性」都收进各枚举值自己的字段 /
 * 方法里：碗的物品 ID、界面与气泡配色、默认营养、主 / 副效果种类与默认时长、抗火与饥饿、
 * 「喝下解反胃」「特辣会点着嘴上火苗」这类开关。加一种新汤底只需要在这里补一个枚举值
 * （顺序必须加在最后，存档里存的是 ordinal），配两条汤面贴图、一条语言键、一个碗模型即可；
 * 客户端配色也从这里读（{@link #screenColors()} / {@link #bubbleColors()} 只是 int），
 * 漏写不会有默认兜底——枚举构造必须写满，漏了编译就过不去。</p>
 *
 * <p>后续联动模组可通过 {@link #registerSource}（由兼容层转发）注册新的汤底来源。</p>
 *
 * <p>mryt233 改（2026-11-11，第四批 4B-1）：每个枚举值的全部定义抽成不可变记录 {@link SoupDef}，
 * 类初始化时按枚举顺序登记进 {@link Soups}（登记表的前 6 项，顺序 = 旧存档里的汤底序号）。
 * 枚举仍是内置定义的写法和对外入口，下面所有访问器转给定义；值、行为、存档、配置键都没变。</p>
 * 作者：mryt233 */
public enum SoupBase {
    /*
     * mryt233 改（2026-11-10，r147）：构造参数一览（按顺序）：
     *   nameKey          语言键后缀（text.<modid>.soup.xxx）
     *   bowlId           一碗汤的物品 ID（和显示名对得上，r102）
     *   speedMul         加热速度倍率（配置可覆盖）
     *   source / remainder  倒进锅里的原料 / 舀出来时还回去的容器
     *   additive         锅底料（加进清汤把它变成这种汤；没有就是 null）
     *   bowlHunger       一碗汤回多少饥饿（配置可覆盖）
     *   bowlSaturation   饱和度系数（配置可覆盖）
     *   mainEffect       主效果；副效果；抗火；饥饿（null = 没有这条）
     *   duration / secondDuration / fireDuration / hungerDuration  这几条的默认时长（配置可覆盖）
     *   clearsNausea     喝下 / 吃下时解掉「反胃」（清汤、番茄汤）
     *   spicy            true = 红汤，吃到特辣以上会在嘴上点 2 秒火苗
     *   screen*          锅里界面的汤面色块配色
     *   bubble*          沸腾气泡配色
     */

    /** 清汤锅底：水桶，均衡速度。原汁原味、轻盈健步——速度 I + 饱和。 */
    CLEAR("soup.clear", "clear_broth_bowl", 1.00F, () -> Items.WATER_BUCKET, Items.BUCKET, null,
        4, 0.6F,
        MobEffects.MOVEMENT_SPEED, 300, MobEffects.SATURATION, 1,
        null, 0, null, 0,
        true, false,
        0xFFB4BCBC, 0xFFF4F4EC, 0xFFC6CCCA, new int[]{0xFF6FAF3A, 0xFF8FCF52, 0xFFD04A2A},
        0xFFC6CCCA, 0xFFF4F4EC),
    /** 高汤锅底：牛奶桶，速度较慢。牛骨老鸡浓汤、坚韧滋补——抗性提升 I + 伤害吸收 I（不再有缓慢）。 */
    RICH_STOCK("soup.rich_stock", "rich_stock_bowl", 0.85F, () -> Items.MILK_BUCKET, Items.BUCKET, null,
        4, 0.9F,
        MobEffects.DAMAGE_RESISTANCE, 300, MobEffects.ABSORPTION, 600,
        null, 0, null, 0,
        false, false,
        0xFFE3D7BF, 0xFFFFFFFF, 0xFFF2E7D2, new int[]{0xFFFFF8EA, 0xFF6FAF3A, 0xFFD9C9A8},
        0xFFF2E7D2, 0xFFFFFFFF),
    /** 菌菇高汤：原版蘑菇煲，速度较快。山珍菌菇、活血养生——生命恢复 I。 */
    MUSHROOM("soup.mushroom", "mushroom_broth_bowl", 1.15F, () -> Items.MUSHROOM_STEW, Items.BOWL, null,
        4, 0.6F,
        MobEffects.REGENERATION, 200, null, 0,
        null, 0, null, 0,
        false, false,
        0xFF7C5836, 0xFFE8C88A, 0xFF9A7048, new int[]{0xFF4A3220, 0xFFC9A16A, 0xFF5E4026},
        0xFF9A7048, 0xFFE8C88A),
    /** 红汤锅底：岩浆桶，速度最快。麻辣浓烈、发汗驱寒——力量 + 速度，特辣以上再加抗火与适度饥饿。 */
    SPICY("soup.spicy", "spicy_red_broth_bowl", 1.30F, () -> Items.LAVA_BUCKET, Items.BUCKET, null,
        4, 0.6F,
        MobEffects.DAMAGE_BOOST, 120, MobEffects.MOVEMENT_SPEED, 120,
        MobEffects.FIRE_RESISTANCE, 300, MobEffects.HUNGER, 120,
        false, true,
        0xFFB32A1E, 0xFFFFB070, 0xFFD94A2E, new int[]{0xFFE8862A, 0xFF7A1408, 0xFF3A1208},
        0xFFBE3325, 0xFFD8553A),
    /**
     * 番茄锅底（2026-10-25 加；2026-10-26 用户定玩法）：锅里先倒清汤，再加番茄锅底料（3 番茄 + 糖）变成番茄锅；
     * 空桶能舀出「番茄汤底桶」（只能从锅里舀，倒回锅里就是番茄锅，不能倒在地上）。速度 1.10。
     */
    TOMATO("soup.tomato", "tomato_soup_bowl", 1.10F, () -> HotPotContent.TOMATO_SOUP_BUCKET, Items.BUCKET,
        () -> HotPotContent.TOMATO_HOTPOT_BASE,
        4, 0.6F,
        MobEffects.DIG_SPEED, 300, null, 0,
        null, 0, null, 0,
        true, false,
        0xFFD8482A, 0xFFFF9A6A, 0xFFE86040, new int[]{0xFFF2C14A, 0xFFB82818, 0xFF6FAF3A},
        0xFFE2563A, 0xFFFF9A6A),
    /** 三鲜锅底：同上，清汤 + 三鲜锅底料（鳕鱼 + 棕色蘑菇 + 胡萝卜 + 海带），舀出「三鲜汤底桶」。速度 1.00。 */
    THREE_FRESH("soup.three_fresh", "three_fresh_soup_bowl", 1.00F, () -> HotPotContent.THREE_FRESH_SOUP_BUCKET,
        Items.BUCKET, () -> HotPotContent.THREE_FRESH_HOTPOT_BASE,
        4, 0.9F,
        MobEffects.WATER_BREATHING, 400, MobEffects.LUCK, 600,
        null, 0, null, 0,
        false, false,
        0xFFD8C79A, 0xFFFFF6DC, 0xFFE6D8B0, new int[]{0xFF8A5A34, 0xFFF08A30, 0xFF3E7A3A},
        0xFFE6D8B0, 0xFFFFF6DC);

    private static final SoupBase[] BY_ID = values();

    /**
     * 第三方物品能倒成哪种汤（兼容层注册进来，核心自己查）。
     * mryt233 改（2026-11-10，r147）：改成冻结登记表——启动期登记，服务端启动后不再接受新的登记。
     */
    private static final FrozenRegistry<Item, SoupBase> CUSTOM_SOURCES = new FrozenRegistry<>(
        "第三方汤底来源表", "汤底来源要在启动期登记（子模组在自己的兼容模块里调 registerSource），冻结后不再接受登记。");

    /** 注册「某个物品倒出来是哪种汤」，由兼容模块调用。 */
    public static void registerSource(Item item, SoupBase base) {
        CUSTOM_SOURCES.put(item, base);
    }

    /** 这件物品能不能倒成汤；不能返回 null。 */
    public static SoupBase customSource(ItemStack stack) {
        return stack.isEmpty() ? null : CUSTOM_SOURCES.get(stack.getItem());
    }

    /** mryt233 改（2026-11-10）：冻结第三方汤底来源表（服务端启动时由 HotPotHooks 统一调，幂等）。 */
    static void freezeSources() {
        CUSTOM_SOURCES.freeze();
    }

    /** 诊断：第三方汤底来源表冻了没有。 */
    public static boolean sourcesFrozen() {
        return CUSTOM_SOURCES.isFrozen();
    }

    /** 这个内置汤底的全部定义（mryt233 改 2026-11-11，4B-1：原来的 24 个字段收进这里）。 */
    private final SoupDef def;

    // 原料用 Supplier：新汤底的原料是本模组物品，枚举初始化时 HotPotContent 可能还没注册好（2026-10-25）
    SoupBase(String nameKey, String bowlId, float speedMul,
             Supplier<Item> sourceItem, Item remainderItem,
             Supplier<Item> additive,
             int bowlHunger, float bowlSaturation,
             Holder<MobEffect> mainEffect, int duration,
             Holder<MobEffect> secondEffect, int secondDuration,
             Holder<MobEffect> fireEffect, int fireDuration,
             Holder<MobEffect> hungerEffect, int hungerDuration,
             boolean clearsNausea, boolean spicy,
             int screenBase, int screenHi, int screenLo, int[] screenBits,
             int bubbleBody, int bubbleHi) {
        this.def = new SoupDef(HotPot.id(name().toLowerCase(Locale.ROOT)), nameKey, bowlId, speedMul,
            sourceItem, remainderItem, additive, bowlHunger, bowlSaturation,
            mainEffect, duration, secondEffect, secondDuration, fireEffect, fireDuration,
            hungerEffect, hungerDuration, clearsNausea, spicy,
            screenBase, screenHi, screenLo, screenBits, bubbleBody, bubbleHi);
        // 默认速度记进配置模板，配置文件里同名项可以覆盖
        Config.rememberSoupSpeed(textureKey(), speedMul);   // 构造参数 speedMul 即定义里的默认速度
    }

    /** 这个汤底的定义（登记表里的同一份）。 */
    public SoupDef def() {
        return def;
    }

    // mryt233 改（2026-11-11，第四批 4B-2）：下面所有访问器和玩法方法都转给 SoupDef（实现搬过去了），
    // 签名与数值不变；游戏里的调用点已改成问登记表 Soups 拿定义，这个枚举留给单测、探针和联动兼容。

    public String nameKey() {
        return def.nameKey();
    }

    /**
     * 汤面贴图名：textures/block/hot_pot_top_&lt;它&gt。png，沸腾时 hot_pot_top_&lt;它&gt;_boiling.png。
     * 鸳鸯 / 田字锅的汤面由渲染器按这个名字取贴图画，所以以后加新汤底只要加枚举值 + 这两张贴图，任何一边都能用。
     */
    public String textureKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * 方块状态里的汤面值（{@link SoupVisual}）。内置项按名字推，找不到由
     * {@link SoupVisual#verifyAgainstSoupBases()} 在启动时报错。
     */
    public SoupVisual visual() {
        return def.visual();
    }

    /**
     * 手里这件是不是「加进清汤里」的锅底料（不是直接倒的汤）：番茄 / 三鲜锅底料，以及联动登记成番茄锅底的东西。
     * 汤底桶（source）是直接倒的。2026-10-26 用户定。
     */
    public boolean isAdditive(ItemStack stack) {
        return def.isAdditive(stack);
    }

    public float speedMul() {
        return def.speedMul();
    }

    /** 倒进锅里的原料（水桶、牛奶桶、蘑菇煲、岩浆桶），舀出来时原样还回去。 */
    public Item source() {
        return def.source();
    }

    public Item remainder() {
        return def.remainder();
    }

    /**
     * 若手持物品可以当汤底倒入锅中，返回对应汤底，否则 null。
     * mryt233 改（2026-11-11，4B-2）：游戏里的调用点已改用 {@link Soups#fromStack}；这里保留枚举版本。
     */
    public static SoupBase fromStack(ItemStack stack) {
        for (SoupBase base : BY_ID) {
            if (base.def.matchesSource(stack)) {
                return base;
            }
        }
        return customSource(stack);
    }

    /** 一碗汤的物品 ID，和显示名对得上（r102）：清汤 clear_broth_bowl、高汤 rich_stock_bowl …… */
    public String bowlId() {
        return def.bowlId();
    }

    public static SoupBase byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    /**
     * mryt233 改（2026-11-05，r136a）：从汤底的贴图名（{@link #textureKey()}，如 "spicy"）查回枚举。
     * 菜上记着 COOKED_IN_SOUP 组件；查不到（旧存档 / 别的模组乱写）返回 null。
     * mryt233 改（2026-11-11，4B-2）：游戏里的调用点已改用 {@link Soups#get(String)}。
     */
    public static SoupBase byTextureKey(String key) {
        if (key == null) {
            return null;
        }
        for (SoupBase base : BY_ID) {
            if (base.textureKey().equals(key)) {
                return base;
            }
        }
        return null;
    }

    /**
     * mryt233 改（2026-11-05）：一碗汤的默认饥饿值。
     * 默认值留在这里，注册时记进配置模板，吃的时候现读配置（见 {@link SoupBowlItem}）。
     */
    public static final int DEFAULT_BOWL_HUNGER = 4;

    /** 一碗汤的默认饱和度系数：高汤、三鲜汤更「浓」一些（用户定的）。 */
    public float defaultBowlSaturation() {
        return def.defaultBowlSaturation();
    }

    /** 主效果的默认时长（tick）。清汤速度 15 秒、高汤抗性 15 秒、菌汤生命恢复 10 秒、红汤力量 6 秒、番茄急迫 15 秒、三鲜水下呼吸 20 秒。 */
    public int defaultDuration() {
        return def.defaultDuration();
    }

    /** 副效果的默认时长（tick）；没有副效果就是 0。清汤饱和 1 tick、高汤伤害吸收 30 秒、红汤速度 6 秒、三鲜幸运 30 秒。 */
    public int defaultSecondDuration() {
        return def.defaultSecondDuration();
    }

    /** 负面「饥饿」效果的默认时长：只有红汤有（特辣 6 秒，3 级特辣顺延至 8 秒，象征吃辣开胃易饿）。 */
    public int defaultHungerDuration() {
        return def.defaultHungerDuration();
    }

    /** 「抗火」效果的默认时长：只有红汤有（15 秒，特辣以上才加；变态辣翻倍到 30 秒）。 */
    public int defaultFireDuration() {
        return def.defaultFireDuration();
    }

    /** 这种汤底喝下 / 吃下时会不会解掉「反胃」（清汤、番茄汤）。 */
    public boolean clearsNausea() {
        return def.clearsNausea();
    }

    /** 这种汤底是不是红汤（吃特辣以上会点嘴上火苗、别处按「红汤」特判的地方看这一条）。 */
    public boolean isSpicy() {
        return def.spicy();
    }

    /** 一碗汤回多少饥饿点数；配置 soup_bowl.&lt;汤底&gt;.hunger 优先。 */
    public int bowlHunger() {
        return def.bowlHunger();
    }

    /** 一碗汤的饱和度系数；配置 soup_bowl.&lt;汤底&gt;.saturation 优先。 */
    public float bowlSaturation() {
        return def.bowlSaturation();
    }

    // ---------------------------------------------------------------- 配色

    /** 锅里界面汤面色块：底色。 */
    public int screenBase() {
        return def.screenBase();
    }

    /** 锅里界面汤面色块：高光。 */
    public int screenHi() {
        return def.screenHi();
    }

    /** 锅里界面汤面色块：暗部。 */
    public int screenLo() {
        return def.screenLo();
    }

    /** 锅里界面上浮着的食材小点颜色（3 个，轮流用）。 */
    public int[] screenBits() {
        return def.screenBits();   // 记录里读出来就是拷贝
    }

    /** 沸腾气泡的体色。 */
    public int bubbleBody() {
        return def.bubbleBody();
    }

    /** 沸腾气泡的高光色。 */
    public int bubbleHi() {
        return def.bubbleHi();
    }

    /** 这种汤底自己那一套效果（不含红汤按辣度加强的部分）；null 表示这条效果不存在。 */
    Holder<MobEffect> mainEffect() {
        return def.mainEffect();
    }

    Holder<MobEffect> secondEffect() {
        return def.secondEffect();
    }

    Holder<MobEffect> fireEffect() {
        return def.fireEffect();
    }

    Holder<MobEffect> hungerEffect() {
        return def.hungerEffect();
    }

    /**
     * 煮熟食物被吃下 / 一碗汤喝下时附加的效果（正负皆有，风味所在）。时长和等级现读配置 soup_bowl.&lt;汤底&gt;，
     * 改完 /orientshotpot reload 立即生效。实现和各汤底的风味说明在 {@link SoupDef#dishBonusEffects(int)}。
     */
    public List<MobEffectInstance> dishBonusEffects() {
        return def.dishBonusEffects();
    }

    /** 带辣度档位的风味加成（红汤按辣度加强，其他汤底忽略辣度）。 */
    public List<MobEffectInstance> dishBonusEffects(int spicyLevel) {
        return def.dishBonusEffects(spicyLevel);
    }

    static {
        // mryt233 改（2026-11-11，第四批 4B-1）：先把六个内置定义按枚举顺序登记成登记表的前 6 项，
        // 再做下面的配置模板记录和汤面核对。HotPotRuntime.install 在读配置之前强制初始化本类，
        // 保证内置项在 Config.init 之前登记完。
        for (SoupBase base : BY_ID) {
            Soups.registerBuiltin(base.def);
        }
        Soups.builtinsDone();
        // mryt233 改（2026-11-06，r139）：六种汤碗的默认营养和效果记进配置模板。
        // 每种汤底都写满一组键（没有的效果登记 0），玩家打开配置文件就能看到全部可调项：
        // duration / amplifier 主效果，second_duration / second_amplifier 副效果，
        // fire_* 抗火、hunger_* 饥饿（这两组只有红汤用）。原来的 slow_* 已随「高汤不再缓慢」一起删掉。
        for (SoupBase base : BY_ID) {
            Config.rememberSoupBowl(base.textureKey(), DEFAULT_BOWL_HUNGER, base.defaultBowlSaturation(),
                base.defaultDuration(), 0, base.defaultSecondDuration(), 0,
                base.defaultFireDuration(), 0, base.defaultHungerDuration(), 0);
        }
        // 启动时核对「方块状态里的汤面」和「汤底枚举」一一对得上（不一致就直接报错，别等到进游戏才发现）
        SoupVisual.verifyAgainstSoupBases();
    }
}
