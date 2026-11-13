// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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
 * 枚举值自己身上（{@link #cup()} / {@link #bottle()} / {@link #emptyBottle()} / {@link #effect()}）。</p>
 * 作者：mryt233 */
public enum DrinkKind implements StringRepresentable {
    ORANGE_DRINK("orange_drink", true, PlateSize.DRINK_H,
        () -> HotPotContent.ORANGE_DRINK, () -> HotPotContent.EMPTY_ORANGE_DRINK_BOTTLE,
        () -> HotPotContent.ORANGE_DRINK_CUP, MobEffects.SPEED),
    PLANT_PROTEIN_DRINK("plant_protein_drink", true, PlateSize.DRINK_H,
        () -> HotPotContent.PLANT_PROTEIN_DRINK, () -> HotPotContent.EMPTY_PLANT_PROTEIN_DRINK_BOTTLE,
        () -> HotPotContent.PLANT_PROTEIN_DRINK_CUP, null),
    ORIENT_COLA("orient_cola", true, 11.375,
        () -> HotPotContent.ORIENT_COLA, () -> HotPotContent.EMPTY_ORIENT_COLA_BOTTLE,
        () -> HotPotContent.ORIENT_COLA_CUP, MobEffects.SPEED),
    WATER("water", false, PlateSize.DRINK_H, null, null, () -> HotPotContent.WATER_CUP, null),
    MILK("milk", false, PlateSize.DRINK_H, null, null, () -> HotPotContent.MILK_CUP, null);

    /** mryt233 改（2026-11-05）：每口正面效果的默认时长（tick，15 秒），注册时记进配置模板。 */
    private static final int DEFAULT_EFFECT_TICKS = 300;

    private final String id;
    private final boolean hasBottle;
    private final double bottleHeight;
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
        if (this == PLANT_PROTEIN_DRINK) {
            user.removeEffect(MobEffects.NAUSEA);
        }
        if (this == ORIENT_COLA) {
            addEffect(user, MobEffects.HASTE, ticks, amplifier);
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
        for (DrinkKind kind : values()) {
            if (kind.effect != null || kind == ORIENT_COLA) {
                Config.rememberDrinkEffect(kind.id, DEFAULT_EFFECT_TICKS, 0);
            }
        }
    }
}
