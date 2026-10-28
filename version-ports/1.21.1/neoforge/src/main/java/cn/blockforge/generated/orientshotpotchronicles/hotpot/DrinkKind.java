package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.StringIdentifiable;

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
 */
public enum DrinkKind implements StringIdentifiable {
    ORANGE_DRINK("orange_drink"),
    PLANT_PROTEIN_DRINK("plant_protein_drink"),
    ORIENT_COLA("orient_cola"),   // 2026-10-25 用户定名；
    WATER("water"),               // 2026-10-27 只有杯装；
    MILK("milk");

    private final String id;

    DrinkKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String asString() {
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

    /** 喝一口的额外效果（饥饿值另算）。 */
    public void applySip(LivingEntity user) {
        if (this == WATER) {
            return;
        }
        if (this == MILK) {
            user.clearStatusEffects();   // 同原版牛奶桶
            return;
        }
        user.removeStatusEffect(StatusEffects.HUNGER);   // 几种饮料都解辣：红汤的饥饿
        switch (this) {
            case ORANGE_DRINK -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0));
            case PLANT_PROTEIN_DRINK -> user.removeStatusEffect(StatusEffects.NAUSEA);   // 解冷锅的反胃
            case ORIENT_COLA -> {   // 提神：速度 I + 急迫 I 各 15 秒（计划里的建议）
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 0));
            }
        }
    }
}
