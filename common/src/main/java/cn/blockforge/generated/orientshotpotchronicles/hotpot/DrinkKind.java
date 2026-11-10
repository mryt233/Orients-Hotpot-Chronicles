package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

/**
 * 饮料的种类（r101 起不止橙子饮料）。外观照现实的瓶子做、名字避开真品牌（用户要求）；配方都先不做。
 * <ul>
 *   <li>橙子饮料：每口 15 秒速度 I，解红汤的饥饿。</li>
 *   <li>维二（植物蛋白饮料）：解红汤的饥饿、解冷锅的反胃（用户定的）。</li>
 *   <li>之晗可乐（原名 之晗可乐，2026-10-26 用户改名）（2026-10-25）：每口速度 I + 急迫 I 各 15 秒，解红汤的饥饿。</li>
 * </ul>
 * 都是一瓶 4 口、每口回 2 点饥饿值、喝完剩空瓶，满瓶空瓶都能摆在桌上。
 * 水、牛奶（2026-10-27 用户：杯子能装水和牛奶）只有杯装（{@link #hasBottle} 为 false）：拿原版水桶 / 奶桶往空杯里倒，桶不会空；
 * 照原版：水什么都不加，牛奶清掉身上所有效果，两种都不回饥饿值。
 *
 * <p>mryt233 改（2026-11-10，r147）：瓶子、杯子、空瓶物品，瓶高、有没有瓶装、每口效果全部挂到
 * 枚举值自己身上（{@link #cup()} / {@link #bottle()} / {@link #emptyBottle()} / {@link #effect()}）。
 * {@code HotPotContent} 里原来那三个把枚举翻成物品的 switch（cupFor / drinkFor / emptyBottle）删掉了，
 * 加一种饮料只要在这里补一个枚举值。</p>
 *
 * <p><b>类初始化顺序</b>（mryt233 改 2026-11-10，r147）：枚举里不能直接写 {@code HotPotContent.XXX_CUP}
 * ——枚举可能比 HotPotContent 先初始化，那时物品字段还是 null。所以物品用 {@link Supplier} 延迟取：
 * lambda 体只在真正被调用时才读 {@code HotPotContent} 的字段。这样既不看类加载顺序，
 * 也不按 ID 查注册表——NeoForge 的注册是排队到 RegisterEvent 才真正落库的，
 * 在 {@code HotPotContent.register()} 里按 ID 查会拿到 air。</p>
 * 作者：mryt233 */
public enum DrinkKind implements StringRepresentable {
    /*
     * mryt233 改（2026-11-10）：构造参数一览：
     *   id           饮料 ID（语言键、配置键、方块状态序列化都用它）
     *   hasBottle    有没有瓶装（水和牛奶只有杯装）
     *   bottleHeight 摆在桌上的瓶子多高（像素）
     *   bottleId     满瓶物品的 ID（没有瓶装就是 null）
     *   emptyBottleId 空瓶物品的 ID（没有瓶装就用原版玻璃瓶）
     *   cupId        满杯物品的 ID
     *   effect       每口的正面效果（null = 没有额外正面效果）
     */

    ORANGE_DRINK("orange_drink", true, PlateSize.DRINK_H,
        () -> HotPotContent.ORANGE_DRINK, () -> HotPotContent.EMPTY_ORANGE_DRINK_BOTTLE,
        () -> HotPotContent.ORANGE_DRINK_CUP, MobEffects.MOVEMENT_SPEED),
    PLANT_PROTEIN_DRINK("plant_protein_drink", true, PlateSize.DRINK_H,
        () -> HotPotContent.PLANT_PROTEIN_DRINK, () -> HotPotContent.EMPTY_PLANT_PROTEIN_DRINK_BOTTLE,
        () -> HotPotContent.PLANT_PROTEIN_DRINK_CUP, null),
    ORIENT_COLA("orient_cola", true, 11.375,
        // 之晗可乐是玻璃汽水瓶，比另外两种大瓶矮（tools/gen_orient_cola.py 的 HEIGHT；2026-10-26 瓶盖变薄，原 11.75）
        () -> HotPotContent.ORIENT_COLA, () -> HotPotContent.EMPTY_ORIENT_COLA_BOTTLE,
        () -> HotPotContent.ORIENT_COLA_CUP, MobEffects.MOVEMENT_SPEED),
    WATER("water", false, PlateSize.DRINK_H, null, null, () -> HotPotContent.WATER_CUP, null),
    MILK("milk", false, PlateSize.DRINK_H, null, null, () -> HotPotContent.MILK_CUP, null);

    /** mryt233 改（2026-11-05）：每口正面效果的默认时长（tick，15 秒），注册时记进配置模板。 */
    private static final int DEFAULT_EFFECT_TICKS = 300;

    private final String id;
    private final boolean hasBottle;
    private final double bottleHeight;
    /** 满瓶 / 空瓶 / 满杯的物品，用 Supplier 延迟到真正要用的时候再读 HotPotContent 的字段。 */
    private final Supplier<DrinkItem> bottle;
    private final Supplier<Item> emptyBottle;
    private final Supplier<CupItem> cup;
    private final Holder<MobEffect> effect;

    DrinkKind(String id, boolean hasBottle, double bottleHeight,
              Supplier<DrinkItem> bottle, Supplier<Item> emptyBottle,
              Supplier<CupItem> cup,
              Holder<MobEffect> effect) {
        this.id = id;
        this.hasBottle = hasBottle;
        this.bottleHeight = bottleHeight;
        this.bottle = bottle;
        this.emptyBottle = emptyBottle;
        this.cup = cup;
        this.effect = effect;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /**
     * 瓶子多高（像素，摆在桌上的选中框用）。
     * 两种大瓶 15，之晗可乐是玻璃汽水瓶 11.375（tools/gen_orient_cola.py 的 HEIGHT）。
     */
    public double bottleHeight() {
        return bottleHeight;
    }

    /** 有没有瓶装（饮料瓶、炼药锅、桌上摆的瓶子只用这几种）；水和牛奶只有杯装。 */
    public boolean hasBottle() {
        return hasBottle;
    }

    /** 这种饮料的满瓶物品；水和牛奶没有瓶装，直接抛异常（调用方必须先看 {@link #hasBottle()}）。 */
    public DrinkItem bottle() {
        if (!hasBottle) {
            throw new IllegalStateException("饮料 " + id + " 没有瓶装");
        }
        return bottle.get();
    }

    /** 喝完剩的空瓶物品；水和牛奶没有瓶装，返回原版玻璃瓶（用不到，只是给调用方一个安全的兜底）。 */
    public Item emptyBottle() {
        return hasBottle ? emptyBottle.get() : Items.GLASS_BOTTLE;
    }

    /** 满杯物品（每种饮料都有杯子，包括水和牛奶）。 */
    public CupItem cup() {
        return cup.get();
    }

    /**
     * 喝一口的额外效果（饥饿值另算）。
     * mryt233 改（2026-11-05）：效果的时长和等级改成每次现读配置 drink.&lt;饮料&gt;.duration / .amplifier，
     * 改完 /orientshotpot reload 立即生效（以前写死 300 刻）；时长配成 0 或更小就不加这个效果。
     * mryt233 改（2026-11-10，r147）：效果种类改读枚举构造参数，「解反胃」「清空效果」这些特例
     * 仍按具体饮料判断——那是各自独特的玩法，不属于可共享的属性。
     */
    public void applySip(LivingEntity user) {
        if (this == WATER) {
            return;
        }
        if (this == MILK) {
            user.removeAllEffects();   // 同原版牛奶桶
            return;
        }
        user.removeEffect(MobEffects.HUNGER);   // 几种饮料都解辣：红汤的饥饿
        int ticks = effectTicks();
        int amplifier = Config.drinkInt(id, "amplifier", 0);
        // 植物蛋白饮料：解冷锅的反胃（用户定的）；之晗可乐除了速度再多一条急迫
        if (this == PLANT_PROTEIN_DRINK) {
            user.removeEffect(MobEffects.CONFUSION);
        }
        if (this == ORIENT_COLA) {
            // 提神：速度 + 急迫，各用同一组时长与等级（计划里的建议）
            addEffect(user, MobEffects.DIG_SPEED, ticks, amplifier);
        }
        addEffect(user, effect, ticks, amplifier);
    }

    /** 这种饮料每口正面效果的时长；配置 drink.&lt;饮料&gt;.duration 优先。 */
    private int effectTicks() {
        return Config.drinkInt(id, "duration", DEFAULT_EFFECT_TICKS);
    }

    /** mryt233 改（2026-11-05）：时长 &lt;= 0 表示这条效果在配置里关掉了。 */
    private static void addEffect(LivingEntity user, Holder<MobEffect> effect,
                                  int ticks, int amplifier) {
        if (effect == null || ticks <= 0) {
            return;
        }
        user.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    static {
        // mryt233 改（2026-11-05）：有正面效果的饮料把默认时长和等级记进配置模板，改的时候不用碰代码。
        // mryt233 改（2026-11-10）：登记 ON 的饮料（有正面效果）自动来，不再手写 ID 清单。
        for (DrinkKind kind : values()) {
            if (kind.effect != null || kind == ORIENT_COLA) {
                Config.rememberDrinkEffect(kind.id, DEFAULT_EFFECT_TICKS, 0);
            }
        }
    }
}