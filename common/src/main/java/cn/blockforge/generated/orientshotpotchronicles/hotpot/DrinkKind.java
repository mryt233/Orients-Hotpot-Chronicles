package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.StringRepresentable;

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
 * 作者：mryt233 */
public enum DrinkKind implements StringRepresentable {
    ORANGE_DRINK("orange_drink"),
    PLANT_PROTEIN_DRINK("plant_protein_drink"),
    ORIENT_COLA("orient_cola"),   // 2026-10-25 用户定名；新增
    WATER("water"),               // 2026-10-27 只有杯装；新增
    MILK("milk");

    /** mryt233 改（2026-11-05）：每口正面效果的默认时长（tick，15 秒），注册时记进配置模板。 */
    private static final int DEFAULT_EFFECT_TICKS = 300;

    private final String id;

    DrinkKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** 瓶子多高（像素，摆在桌上的选中框用）：两种大瓶 15，之晗可乐是玻璃汽水瓶 11.375（tools/gen_orient_cola.py 的 HEIGHT；2026-10-26 瓶盖变薄，原 11.75）。 */
    public double bottleHeight() {
        return this == ORIENT_COLA ? 11.375 : PlateSize.DRINK_H;
    }

    /** 有没有瓶装（饮料瓶、炼药锅、桌上摆的瓶子只用这几种）；水和牛奶只有杯装。 */
    public boolean hasBottle() {
        return this != WATER && this != MILK;
    }

    /**
     * 喝一口的额外效果（饥饿值另算）。
     * mryt233 改（2026-11-05）：效果的时长和等级改成每次现读配置 drink.&lt;饮料&gt;.duration / .amplifier，
     * 改完 /orientshotpot reload 立即生效（以前写死 300 刻）；时长配成 0 或更小就不加这个效果。
     * 之晗可乐的速度和急迫共用同一组时长、等级。
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
        switch (this) {
            case ORANGE_DRINK -> addEffect(user, MobEffects.MOVEMENT_SPEED, ticks, amplifier);
            case PLANT_PROTEIN_DRINK -> user.removeEffect(MobEffects.CONFUSION);   // 解冷锅的反胃
            case ORIENT_COLA -> {   // 提神：速度 + 急迫，各用同一组时长与等级（计划里的建议）
                addEffect(user, MobEffects.MOVEMENT_SPEED, ticks, amplifier);
                addEffect(user, MobEffects.DIG_SPEED, ticks, amplifier);
            }
        }
    }

    /** 这种饮料每口正面效果的时长；配置 drink.&lt;饮料&gt;.duration 优先。 */
    private int effectTicks() {
        return Config.drinkInt(id, "duration", DEFAULT_EFFECT_TICKS);
    }

    /** mryt233 改（2026-11-05）：时长 &lt;= 0 表示这条效果在配置里关掉了。 */
    private static void addEffect(LivingEntity user, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                  int ticks, int amplifier) {
        if (ticks <= 0) {
            return;
        }
        user.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    static {
        // mryt233 改（2026-11-05）：有正面效果的饮料把默认时长和等级记进配置模板，改的时候不用碰代码。
        Config.rememberDrinkEffect("orange_drink", DEFAULT_EFFECT_TICKS, 0);
        Config.rememberDrinkEffect("orient_cola", DEFAULT_EFFECT_TICKS, 0);
    }
}