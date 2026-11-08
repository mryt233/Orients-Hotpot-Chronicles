// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 汤底体系：全部由原版物品参与（水/牛奶/蘑菇煲/岩浆桶）。
 * 汤底决定加热速度，并决定在它里面涮熟的菜、盛出来的汤喝下去时带的状态效果
 * （结算见 {@link #dishBonusEffects(int)}，吃的那一刻现读配置，改完 /orientshotpot reload 立即生效）。
 * 后续联动模组可通过 {@link #registerSource}（由兼容层转发）
 * 注册新的汤底来源。
 */
public enum SoupBase {
    /** 清汤锅底：水桶，均衡速度。原汁原味、轻盈健步——速度 I + 饱和。 */
    CLEAR("soup.clear", 1.00F, () -> Items.WATER_BUCKET, Items.BUCKET),
    /** 高汤锅底：牛奶桶，速度较慢。牛骨老鸡浓汤、坚韧滋补——抗性提升 I + 伤害吸收 I（不再有缓慢）。 */
    RICH_STOCK("soup.rich_stock", 0.85F, () -> Items.MILK_BUCKET, Items.BUCKET),
    /** 菌菇高汤：原版蘑菇煲，速度较快。山珍菌菇、活血养生——生命恢复 I。 */
    MUSHROOM("soup.mushroom", 1.15F, () -> Items.MUSHROOM_STEW, Items.BOWL),
    /** 红汤锅底：岩浆桶，速度最快。麻辣浓烈、发汗驱寒——力量 + 速度，特辣以上再加抗火与适度饥饿。 */
    SPICY("soup.spicy", 1.30F, () -> Items.LAVA_BUCKET, Items.BUCKET),
    /**
     * 番茄锅底（2026-10-25 加；2026-10-26 用户定玩法）：锅里先倒清汤，再加番茄锅底料（3 番茄 + 糖）变成番茄锅；
     * 空桶能舀出「番茄汤底桶」（只能从锅里舀，倒回锅里就是番茄锅，不能倒在地上）。速度 1.10。
     */
    TOMATO("soup.tomato", 1.10F, () -> HotPotContent.TOMATO_SOUP_BUCKET, Items.BUCKET, () -> HotPotContent.TOMATO_HOTPOT_BASE),
    /** 三鲜锅底：同上，清汤 + 三鲜锅底料（鳕鱼 + 棕色蘑菇 + 胡萝卜 + 海带），舀出「三鲜汤底桶」。速度 1.00。 */
    THREE_FRESH("soup.three_fresh", 1.00F, () -> HotPotContent.THREE_FRESH_SOUP_BUCKET, Items.BUCKET,
        () -> HotPotContent.THREE_FRESH_HOTPOT_BASE);

    private static final SoupBase[] BY_ID = values();

    /** 第三方物品能倒成哪种汤（兼容层注册进来，核心自己查）。 */
    private static final java.util.Map<Item, SoupBase> CUSTOM_SOURCES = new java.util.HashMap<>();

    /** 注册「某个物品倒出来是哪种汤」，由兼容模块调用。 */
    public static void registerSource(Item item, SoupBase base) {
        CUSTOM_SOURCES.put(item, base);
    }

    /** 这件物品能不能倒成汤；不能返回 null。 */
    public static SoupBase customSource(ItemStack stack) {
        return stack.isEmpty() ? null : CUSTOM_SOURCES.get(stack.getItem());
    }

    private final String nameKey;
    private final float speedMul;
    private final java.util.function.Supplier<Item> sourceItem;
    private final Item remainderItem;
    /** 锅底料：加进清汤里把它变成这种汤（只有番茄 / 三鲜有）；没有就是 null。 */
    private final java.util.function.Supplier<Item> additive;

    // 原料用 Supplier：新汤底的原料是本模组物品，枚举初始化时 HotPotContent 可能还没注册好（2026-10-25）
    SoupBase(String nameKey, float speedMul, java.util.function.Supplier<Item> sourceItem, Item remainderItem) {
        this(nameKey, speedMul, sourceItem, remainderItem, null);
    }

    SoupBase(String nameKey, float speedMul, java.util.function.Supplier<Item> sourceItem, Item remainderItem,
             java.util.function.Supplier<Item> additive) {
        this.additive = additive;
        this.nameKey = nameKey;
        this.speedMul = speedMul;
        this.sourceItem = sourceItem;
        this.remainderItem = remainderItem;
        // 默认速度记进配置模板，配置文件里同名项可以覆盖
        Config.rememberSoupSpeed(textureKey(), speedMul);
    }

    public String nameKey() {
        return "text." + cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.MOD_ID + "." + nameKey;
    }

    /**
     * 汤面贴图名：textures/block/hot_pot_top_&lt;它&gt。png，沸腾时 hot_pot_top_&lt;它&gt;_boiling.png。
     * 鸳鸯 / 田字锅的汤面由渲染器按这个名字取贴图画，所以以后加新汤底只要加枚举值 + 这两张贴图，任何一边都能用。
     */
    public String textureKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 手里这件是不是「加进清汤里」的锅底料（不是直接倒的汤）：番茄 / 三鲜锅底料，以及联动登记成番茄锅底的东西（农夫乐事的番茄）。
     * 汤底桶（source）是直接倒的。2026-10-26 用户定。
     */
    public boolean isAdditive(ItemStack stack) {
        return additive != null && !stack.is(sourceItem.get());
    }

    public float speedMul() {
        // 配置文件里的 soup_speed.<名字> 优先，没写就用枚举里的默认值
        return Config.soupSpeed(textureKey(), speedMul);
    }

    /** 倒进锅里的原料（水桶、牛奶桶、蘑菇煲、岩浆桶），舀出来时原样还回去。 */
    public Item source() {
        return sourceItem.get();
    }

    public Item remainder() {
        return remainderItem;
    }

    /** 若手持物品可以当汤底倒入锅中，返回对应汤底，否则 null。 */
    public static SoupBase fromStack(ItemStack stack) {
        for (SoupBase base : BY_ID) {
            if (stack.is(base.sourceItem.get()) || (base.additive != null && stack.is(base.additive.get()))) {
                return base;
            }
        }
        return customSource(stack);
    }

    /** 一碗汤的物品 ID，和显示名对得上（r102）：清汤 clear_broth_bowl、高汤 rich_stock_bowl …… */
    public String bowlId() {
        return switch (this) {
            case CLEAR -> "clear_broth_bowl";
            case RICH_STOCK -> "rich_stock_bowl";
            case MUSHROOM -> "mushroom_broth_bowl";
            case SPICY -> "spicy_red_broth_bowl";
            case TOMATO -> "tomato_soup_bowl";
            case THREE_FRESH -> "three_fresh_soup_bowl";
        };
    }

    public static SoupBase byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    /**
     * mryt233 改（2026-11-05，r136a）：从汤底的贴图名（{@link #textureKey()}，如 "spicy"）查回枚举。
     * 菜上记着 COOKED_IN_SOUP 组件，吃的时候用这个查回汤底给风味加成；查不到（旧存档 / 别的模组乱写）返回 null。
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
    private float defaultBowlSaturation() {
        return this == RICH_STOCK || this == THREE_FRESH ? 0.9F : 0.6F;
    }

    /**
     * mryt233 改（2026-11-06，r139 / r140）：主效果的默认时长（tick），风味与数值按用户任务单定。
     * 清汤速度 15 秒、高汤抗性 15 秒、菌汤生命恢复 10 秒、红汤力量 6 秒、番茄急迫 15 秒、三鲜水下呼吸 20 秒。
     */
    private int defaultDuration() {
        return switch (this) {
            case CLEAR -> 300;
            case RICH_STOCK -> 300;
            case MUSHROOM -> 200;
            case SPICY -> 120;
            case TOMATO -> 300;
            case THREE_FRESH -> 400;
        };
    }

    /**
     * mryt233 改（2026-11-06，r139 / r140）：副效果的默认时长（tick）。每锅汤底最多两条正面效果，
     * 第二条风味放在这里；没有副效果就是 0（不加）。
     * 清汤饱和 1 tick（清爽解腻，一闪而过）、高汤伤害吸收 30 秒、红汤速度 6 秒、三鲜幸运 30 秒。
     */
    private int defaultSecondDuration() {
        return switch (this) {
            case CLEAR -> 1;
            case RICH_STOCK -> 600;
            case SPICY -> 120;
            case THREE_FRESH -> 600;
            case MUSHROOM, TOMATO -> 0;
        };
    }

    /** 负面「饥饿」效果的默认时长：只有红汤有（特辣 6 秒，3 级特辣顺延至 8 秒，象征吃辣开胃易饿）。 */
    private int defaultHungerDuration() {
        return this == SPICY ? 120 : 0;
    }

    /** 「抗火」效果的默认时长：只有红汤有（15 秒，特辣以上才加；变态辣翻倍到 30 秒）。 */
    private int defaultFireDuration() {
        return this == SPICY ? 300 : 0;
    }

    /** 一碗汤回多少饥饿点数；配置 soup_bowl.&lt;汤底&gt;.hunger 优先。 */
    public int bowlHunger() {
        return Config.soupBowlInt(textureKey(), "hunger", DEFAULT_BOWL_HUNGER);
    }

    /** 一碗汤的饱和度系数；配置 soup_bowl.&lt;汤底&gt;.saturation 优先。 */
    public float bowlSaturation() {
        return Config.soupBowlFloat(textureKey(), "saturation", defaultBowlSaturation());
    }

    /**
     * 煮熟食物被吃下 / 一碗汤喝下时附加的效果（正负皆有，风味所在）。
     * mryt233 改（2026-11-05）：时长和等级改成每次现读配置 soup_bowl.&lt;汤底&gt;，
     * 改完 /orientshotpot reload 立即生效（以前写死在这里）。时长配成 0 或更小就不加这个效果。
     */
    public List<MobEffectInstance> dishBonusEffects() {
        return dishBonusEffects(0);
    }

    /**
     * 汤底给菜品 / 汤碗的风味加成。
     * <p>mryt233 改（2026-11-06，r139）：按各汤底真实风味重排效果（用户任务单「汤底风味 Buff 映射合理化」）：
     * <ul>
     *   <li><b>红汤</b>（麻辣浓烈、发汗驱寒）：力量 + 移动速度；特辣 / 变态辣再追加抗火与适度饥饿（吃辣开胃易饿）。</li>
     *   <li><b>清汤</b>（原汁原味、轻盈健步）：移动速度 I 15 秒 + 饱和度 1 tick（清爽解腻）。</li>
     *   <li><b>高汤</b>（牛骨老鸡浓汤、坚韧滋补）：抗性提升 I 15 秒 + 伤害吸收 I 30 秒；<b>彻底移除缓慢</b>。</li>
     *   <li><b>菌汤</b>（山珍菌菇、活血养生）：生命恢复 I 10 秒。</li>
     *   <li><b>番茄汤</b>（酸甜浓郁、生津开胃）：急迫 I 15 秒（精力充沛）；喝下 / 吃下时另解反胃（见 {@link SoupBowlItem}）。</li>
     *   <li><b>三鲜汤</b>（干贝海米、鲜掉眉毛）：水下呼吸 20 秒 + 幸运 I 30 秒（时来运转）。</li>
     * </ul>
     * 食材在哪种汤里煮出来就带哪种效果（HotPotCookingRun 写 COOKED_IN_SOUP 组件，吃的时候由
     * LivingEntityEatMixin / {@link SoupBowlItem} 结算）。辣度超出 0..3 夹回来，传 0 就是微辣。
     * 时长与等级全部现读配置：duration / amplifier 是主效果，second_duration / second_amplifier 是副效果，
     * fire_* 与 hunger_* 只红汤用；任何一项配成 0 或不配（默认 0）就不加。
     */
    public List<MobEffectInstance> dishBonusEffects(int spicyLevel) {
        List<MobEffectInstance> list = new ArrayList<>();
        String id = textureKey();
        int ticks = Config.soupBowlInt(id, "duration", defaultDuration());
        int amplifier = Config.soupBowlInt(id, "amplifier", 0);
        int secondTicks = Config.soupBowlInt(id, "second_duration", defaultSecondDuration());
        int secondAmp = Config.soupBowlInt(id, "second_amplifier", 0);
        if (this == SPICY) {
            int spicy = Math.max(0, Math.min(3, spicyLevel));
            // 越辣越上头：力量与速度的时长随档位递加，中辣起力量再升一级。
            // 配成 0 的那一项保持关闭（不加辣度带来的时长），和别的汤底一样「0 = 不加」。
            if (ticks > 0) {
                ticks += spicy * 60;
                amplifier += spicy >= 1 ? 1 : 0;
            }
            addEffect(list, MobEffects.STRENGTH, ticks, amplifier);
            int speedAdd = spicy == 1 ? 60 : (spicy == 2 ? 80 : (spicy >= 3 ? 120 : 0));
            addEffect(list, MobEffects.SPEED, secondTicks > 0 ? secondTicks + speedAdd : 0, secondAmp);
            if (spicy >= 2) {
                // 特辣 / 变态辣：发汗驱寒（抗火，变态辣加倍）+ 开胃易饿（饥饿，越辣越饿）
                int fireTicks = Config.soupBowlInt(id, "fire_duration", defaultFireDuration());
                addEffect(list, MobEffects.FIRE_RESISTANCE, spicy >= 3 ? fireTicks * 2 : fireTicks,
                    Config.soupBowlInt(id, "fire_amplifier", 0));
                int hungerTicks = Config.soupBowlInt(id, "hunger_duration", defaultHungerDuration());
                // 配成 0 表示「红汤不加饥饿」：这时不再按辣度往上加，玩家的配置说了算
                if (hungerTicks > 0 && spicy >= 3) {
                    hungerTicks += 40;
                }
                addEffect(list, MobEffects.HUNGER, hungerTicks,
                    Config.soupBowlInt(id, "hunger_amplifier", 0));
            }
            return list;
        }
        switch (this) {
            // 清汤：轻盈健步 + 清爽解腻（饱和只闪 1 tick，不压饥饿）
            case CLEAR -> {
                addEffect(list, MobEffects.SPEED, ticks, amplifier);
                addEffect(list, MobEffects.SATURATION, secondTicks, secondAmp);
            }
            // 高汤：坚韧滋补（抗性提升）+ 伤害吸收；mryt233 改（2026-11-06）：缓慢彻底移除
            case RICH_STOCK -> {
                addEffect(list, MobEffects.RESISTANCE, ticks, amplifier);
                addEffect(list, MobEffects.ABSORPTION, secondTicks, secondAmp);
            }
            // 菌汤：活血养生
            case MUSHROOM -> addEffect(list, MobEffects.REGENERATION, ticks, amplifier);
            // 番茄汤：生津开胃的急迫；喝汤 / 吃菜时另外解反胃（见 SoupBowlItem 与 LivingEntityEatMixin）
            case TOMATO -> addEffect(list, MobEffects.HASTE, ticks, amplifier);
            // 三鲜汤：海鲜味的憋气 + 时来运转的幸运
            case THREE_FRESH -> {
                addEffect(list, MobEffects.WATER_BREATHING, ticks, amplifier);
                addEffect(list, MobEffects.LUCK, secondTicks, secondAmp);
            }
        }
        return list;
    }

    /** mryt233 改（2026-11-05）：按配置的时长加效果；时长 &lt;= 0 表示这条效果关掉了。 */
    private static void addEffect(List<MobEffectInstance> list, Holder<MobEffect> effect, int ticks, int amplifier) {
        if (ticks <= 0) {
            return;
        }
        list.add(new MobEffectInstance(effect, ticks, amplifier, false, true));
    }

    static {
        // mryt233 改（2026-11-06，r139）：六种汤碗的默认营养和效果记进配置模板。
        // 每种汤底都写满一组键（没有的效果登记 0），玩家打开配置文件就能看到全部可调项：
        // duration / amplifier 主效果，second_duration / second_amplifier 副效果，
        // fire_* 抗火、hunger_* 饥饿（这两组只有红汤用）。原来的 slow_* 已随「高汤不再缓慢」一起删掉。
        for (SoupBase base : BY_ID) {
            Config.rememberSoupBowl(base.textureKey(), DEFAULT_BOWL_HUNGER, base.defaultBowlSaturation(),
                base.defaultDuration(), 0, base.defaultSecondDuration(), 0,
                base.defaultFireDuration(), 0, base.defaultHungerDuration(), 0);
        }
    }
}
