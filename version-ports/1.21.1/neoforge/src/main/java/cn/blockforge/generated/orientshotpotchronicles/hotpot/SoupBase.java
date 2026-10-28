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
    CLEAR("soup.clear", 1.00F, () -> Items.WATER_BUCKET, Items.BUCKET),
    /** 高汤锅底：牛奶桶，速度较慢；预留吸收与缓慢效果。 */
    RICH_STOCK("soup.rich_stock", 0.85F, () -> Items.MILK_BUCKET, Items.BUCKET),
    /** 菌菇高汤：原版蘑菇煲，速度较快；预留再生效果。 */
    MUSHROOM("soup.mushroom", 1.15F, () -> Items.MUSHROOM_STEW, Items.BOWL),
    /** 红汤锅底：岩浆桶，速度最快；预留力量与饥饿效果。 */
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

    private final String nameKey;
    private final float speedMul;
    private final java.util.function.Supplier<Item> sourceItem;
    private final Item remainderItem;
    /** 锅底料：加进清汤里把它变成这种汤（只有番茄 / 三鲜有）；没有就是 null。 */
    private final java.util.function.Supplier<Item> additive;

    // 原料用 Supplier：新汤底的原料是本模组物品，枚举初始化时 HotPotContent 可能还没注册好（ 2026-10-25）
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
    }

    public String nameKey() {
        return "text." + cn.blockforge.generated.orientshotpotchronicles.GeneratedMod.MOD_ID + "." + nameKey;
    }

    /**
     * 汤面贴图名：textures/block/hot_pot_top_&lt;它&gt;.png，沸腾时 hot_pot_top_&lt;它&gt;_boiling.png（）。
     * 鸳鸯 / 田字锅的汤面由渲染器按这个名字取贴图画，所以以后加新汤底只要加枚举值 + 这两张贴图，任何一边都能用。
     */
    public String textureKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 手里这件是不是「加进清汤里」的锅底料（不是直接倒的汤）：番茄 / 三鲜锅底料，以及联动登记成番茄锅底的东西（农夫乐事的番茄）。
     * 汤底桶（source）是直接倒的。2026-10-26 用户定（）。
     */
    public boolean isAdditive(ItemStack stack) {
        return additive != null && !stack.isOf(sourceItem.get());
    }

    public float speedMul() {
        return speedMul;
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
            if (stack.isOf(base.sourceItem.get()) || (base.additive != null && stack.isOf(base.additive.get()))) {
                return base;
            }
        }
        return cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat.customBase(stack);
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

    /** 煮熟食物被吃下时附加的效果（正负皆有，风味所在）。 */
    public List<StatusEffectInstance> dishBonusEffects() {
        List<StatusEffectInstance> list = new ArrayList<>();
        switch (this) {
            case MUSHROOM -> list.add(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 0, false, true));
            case SPICY -> {
                list.add(new StatusEffectInstance(StatusEffects.STRENGTH, 180, 0, false, true));
                list.add(new StatusEffectInstance(StatusEffects.HUNGER, 60, 0, false, true));
            }
            case RICH_STOCK -> {
                list.add(new StatusEffectInstance(StatusEffects.ABSORPTION, 240, 0, false, true));
                list.add(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0, false, true));
            }
            case CLEAR -> list.add(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, false, true));
            // 番茄汤：生命恢复 I 6 秒（喝汤时另外解反胃，见 SoupBowlItem）；三鲜汤：水下呼吸 15 秒（「鲜」= 海鲜味）
            case TOMATO -> list.add(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 0, false, true));
            case THREE_FRESH -> list.add(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 300, 0, false, true));
        }
        return list;
    }
}
