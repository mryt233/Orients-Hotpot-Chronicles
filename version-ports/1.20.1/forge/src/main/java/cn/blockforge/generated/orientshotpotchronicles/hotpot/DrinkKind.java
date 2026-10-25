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
 * </ul>
 * 都是一瓶 4 口、每口回 2 点饥饿值、喝完剩空瓶，满瓶空瓶都能摆在桌上。
 * 作者：
 */
public enum DrinkKind implements StringIdentifiable {
    ORANGE_DRINK("orange_drink"),
    PLANT_PROTEIN_DRINK("plant_protein_drink");

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

    /** 喝一口的额外效果（饥饿值另算）。 */
    public void applySip(LivingEntity user) {
        user.removeStatusEffect(StatusEffects.HUNGER);   // 两种都解辣：红汤的饥饿
        switch (this) {
            case ORANGE_DRINK -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0));
            case PLANT_PROTEIN_DRINK -> user.removeStatusEffect(StatusEffects.NAUSEA);   // 解冷锅的反胃
        }
    }
}
