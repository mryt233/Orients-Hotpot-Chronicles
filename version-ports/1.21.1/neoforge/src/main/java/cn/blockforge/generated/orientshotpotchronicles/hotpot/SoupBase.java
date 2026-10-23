package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 汤底体系：全部由原版物品参与（水/牛奶/蘑菇煲/岩浆桶）。
 * 汤底决定加热速度，并预留与熟食效果联动的正/负状态效果接口；当前版本尚未在食用事件中应用。
 * 后续联动模组可通过 {@link cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat}
 * 注册新的汤底来源。
 */
public enum SoupBase {
    /** 清汤锅底：水桶，均衡速度；预留饱和效果。 */
    CLEAR("soup.clear", 1.00F, Items.WATER_BUCKET, Items.BUCKET),
    /** 浓汤锅底：牛奶桶，速度较慢；预留吸收与缓慢效果。 */
    MILKY("soup.milky", 0.85F, Items.MILK_BUCKET, Items.BUCKET),
    /** 菌菇高汤：原版蘑菇煲，速度较快；预留再生效果。 */
    MUSHROOM("soup.mushroom", 1.15F, Items.MUSHROOM_STEW, Items.BOWL),
    /** 红汤锅底：岩浆桶，速度最快；预留力量与饥饿效果。 */
    SPICY("soup.spicy", 1.30F, Items.LAVA_BUCKET, Items.BUCKET);

    private static final SoupBase[] BY_ID = values();

    private final String nameKey;
    private final float speedMul;
    private final Item sourceItem;
    private final Item remainderItem;

    SoupBase(String nameKey, float speedMul, Item sourceItem, Item remainderItem) {
        this.nameKey = nameKey;
        this.speedMul = speedMul;
        this.sourceItem = sourceItem;
        this.remainderItem = remainderItem;
    }

    public String nameKey() {
        return "text." + cn.blockforge.generated.orientshotpotchronicles.GeneratedMod.MOD_ID + "." + nameKey;
    }

    public float speedMul() {
        return speedMul;
    }

    /** 倒进锅里的原料（水桶、牛奶桶、蘑菇煲、岩浆桶），舀出来时原样还回去。 */
    public Item source() {
        return sourceItem;
    }

    public Item remainder() {
        return remainderItem;
    }

    /** 若手持物品可以当汤底倒入锅中，返回对应汤底，否则 null。 */
    public static SoupBase fromStack(ItemStack stack) {
        for (SoupBase base : BY_ID) {
            if (stack.isOf(base.sourceItem)) {
                return base;
            }
        }
        return cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat.customBase(stack);
    }

    /** 一碗汤的物品 ID，和显示名对得上（r102）：清汤 clear_broth_bowl、浓汤 creamy_broth_bowl …… */
    public String bowlId() {
        return switch (this) {
            case CLEAR -> "clear_broth_bowl";
            case MILKY -> "creamy_broth_bowl";
            case MUSHROOM -> "mushroom_broth_bowl";
            case SPICY -> "spicy_red_broth_bowl";
        };
    }

    public static SoupBase byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    /** 煮熟食物被吃下时附加的效果（正负皆有，风味所在）。 */
    public List<StatusEffectInstance> dishBonusEffects() {
        List<StatusEffectInstance> list = new ArrayList<>();
        switch (this) {
            case MUSHROOM -> list.add(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 0, false, true));
            case SPICY -> {
                list.add(new StatusEffectInstance(StatusEffects.STRENGTH, 180, 0, false, true));
                list.add(new StatusEffectInstance(StatusEffects.HUNGER, 60, 0, false, true));
            }
            case MILKY -> {
                list.add(new StatusEffectInstance(StatusEffects.ABSORPTION, 240, 0, false, true));
                list.add(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0, false, true));
            }
            case CLEAR -> list.add(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, false, true));
        }
        return list;
    }
}
