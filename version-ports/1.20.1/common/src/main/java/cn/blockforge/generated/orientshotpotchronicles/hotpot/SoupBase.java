package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 汤底体系（mryt233 改：1.20.1 适配直接使用 MobEffect）。
 * 作者：mryt233 */
public enum SoupBase {
    CLEAR("soup.clear", "clear_broth_bowl", 1.00F, () -> Items.WATER_BUCKET, Items.BUCKET, null,
        4, 0.6F,
        MobEffects.MOVEMENT_SPEED, 300, MobEffects.SATURATION, 1,
        null, 0, null, 0,
        true, false,
        0xFFB4BCBC, 0xFFF4F4EC, 0xFFC6CCCA, new int[]{0xFF6FAF3A, 0xFF8FCF52, 0xFFD04A2A},
        0xFFC6CCCA, 0xFFF4F4EC),
    RICH_STOCK("soup.rich_stock", "rich_stock_bowl", 0.85F, () -> Items.MILK_BUCKET, Items.BUCKET, null,
        4, 0.9F,
        MobEffects.DAMAGE_RESISTANCE, 300, MobEffects.ABSORPTION, 600,
        null, 0, null, 0,
        false, false,
        0xFFE3D7BF, 0xFFFFFFFF, 0xFFF2E7D2, new int[]{0xFFFFF8EA, 0xFF6FAF3A, 0xFFD9C9A8},
        0xFFF2E7D2, 0xFFFFFFFF),
    MUSHROOM("soup.mushroom", "mushroom_broth_bowl", 1.15F, () -> Items.MUSHROOM_STEW, Items.BOWL, null,
        4, 0.6F,
        MobEffects.REGENERATION, 200, null, 0,
        null, 0, null, 0,
        false, false,
        0xFF7C5836, 0xFFE8C88A, 0xFF9A7048, new int[]{0xFF4A3220, 0xFFC9A16A, 0xFF5E4026},
        0xFF9A7048, 0xFFE8C88A),
    SPICY("soup.spicy", "spicy_red_broth_bowl", 1.30F, () -> Items.LAVA_BUCKET, Items.BUCKET, null,
        4, 0.6F,
        MobEffects.DAMAGE_BOOST, 120, MobEffects.MOVEMENT_SPEED, 120,
        MobEffects.FIRE_RESISTANCE, 300, MobEffects.HUNGER, 120,
        false, true,
        0xFFB32A1E, 0xFFFFB070, 0xFFD94A2E, new int[]{0xFFE8862A, 0xFF7A1408, 0xFF3A1208},
        0xFFBE3325, 0xFFD8553A),
    TOMATO("soup.tomato", "tomato_soup_bowl", 1.10F, () -> HotPotContent.TOMATO_SOUP_BUCKET, Items.BUCKET,
        () -> HotPotContent.TOMATO_HOTPOT_BASE,
        4, 0.6F,
        MobEffects.DIG_SPEED, 300, null, 0,
        null, 0, null, 0,
        true, false,
        0xFFD8482A, 0xFFFF9A6A, 0xFFE86040, new int[]{0xFFF2C14A, 0xFFB82818, 0xFF6FAF3A},
        0xFFE2563A, 0xFFFF9A6A),
    THREE_FRESH("soup.three_fresh", "three_fresh_soup_bowl", 1.00F, () -> HotPotContent.THREE_FRESH_SOUP_BUCKET,
        Items.BUCKET, () -> HotPotContent.THREE_FRESH_HOTPOT_BASE,
        4, 0.9F,
        MobEffects.WATER_BREATHING, 400, MobEffects.LUCK, 600,
        null, 0, null, 0,
        false, false,
        0xFFD8C79A, 0xFFFFF6DC, 0xFFE6D8B0, new int[]{0xFF8A5A34, 0xFFF08A30, 0xFF3E7A3A},
        0xFFE6D8B0, 0xFFFFF6DC);

    private static final SoupBase[] BY_ID = values();

    private static final FrozenRegistry<Item, SoupBase> CUSTOM_SOURCES = new FrozenRegistry<>(
        "第三方汤底来源表", "汤底来源要在启动期登记（子模组在自己的兼容模块里调 registerSource），冻结后不再接受登记。");

    public static void registerSource(Item item, SoupBase base) {
        CUSTOM_SOURCES.put(item, base);
    }

    public static SoupBase customSource(ItemStack stack) {
        return stack.isEmpty() ? null : CUSTOM_SOURCES.get(stack.getItem());
    }

    static void freezeSources() {
        CUSTOM_SOURCES.freeze();
    }

    public static boolean sourcesFrozen() {
        return CUSTOM_SOURCES.isFrozen();
    }

    private final SoupDef def;

    SoupBase(String nameKey, String bowlId, float speedMul,
             Supplier<Item> sourceItem, Item remainderItem,
             Supplier<Item> additive,
             int bowlHunger, float bowlSaturation,
             MobEffect mainEffect, int duration,
             MobEffect secondEffect, int secondDuration,
             MobEffect fireEffect, int fireDuration,
             MobEffect hungerEffect, int hungerDuration,
             boolean clearsNausea, boolean spicy,
             int screenBase, int screenHi, int screenLo, int[] screenBits,
             int bubbleBody, int bubbleHi) {
        this.def = new SoupDef(HotPot.id(name().toLowerCase(Locale.ROOT)), nameKey, bowlId, speedMul,
            sourceItem, remainderItem, additive, bowlHunger, bowlSaturation,
            mainEffect, duration, secondEffect, secondDuration, fireEffect, fireDuration,
            hungerEffect, hungerDuration, clearsNausea, spicy,
            screenBase, screenHi, screenLo, screenBits, bubbleBody, bubbleHi);
        Config.rememberSoupSpeed(textureKey(), speedMul);
    }

    public SoupDef def() {
        return def;
    }

    public String nameKey() { return def.nameKey(); }
    public String textureKey() { return name().toLowerCase(Locale.ROOT); }
    public SoupVisual visual() { return def.visual(); }
    public boolean isAdditive(ItemStack stack) { return def.isAdditive(stack); }
    public float speedMul() { return def.speedMul(); }
    public Item source() { return def.source(); }
    public Item remainder() { return def.remainder(); }

    public static SoupBase fromStack(ItemStack stack) {
        for (SoupBase base : BY_ID) {
            if (base.def.matchesSource(stack)) {
                return base;
            }
        }
        return customSource(stack);
    }

    public String bowlId() { return def.bowlId(); }

    public static SoupBase byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

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

    public static final int DEFAULT_BOWL_HUNGER = 4;

    public float defaultBowlSaturation() { return def.defaultBowlSaturation(); }
    public int defaultDuration() { return def.defaultDuration(); }
    public int defaultSecondDuration() { return def.defaultSecondDuration(); }
    public int defaultHungerDuration() { return def.defaultHungerDuration(); }
    public int defaultFireDuration() { return def.defaultFireDuration(); }
    public boolean clearsNausea() { return def.clearsNausea(); }
    public boolean isSpicy() { return def.spicy(); }
    public int bowlHunger() { return def.bowlHunger(); }
    public float bowlSaturation() { return def.bowlSaturation(); }
    public int screenBase() { return def.screenBase(); }
    public int screenHi() { return def.screenHi(); }
    public int screenLo() { return def.screenLo(); }
    public int[] screenBits() { return def.screenBits(); }
    public int bubbleBody() { return def.bubbleBody(); }
    public int bubbleHi() { return def.bubbleHi(); }
    MobEffect mainEffect() { return def.mainEffect(); }
    MobEffect secondEffect() { return def.secondEffect(); }
    MobEffect fireEffect() { return def.fireEffect(); }
    MobEffect hungerEffect() { return def.hungerEffect(); }
    public List<MobEffectInstance> dishBonusEffects() { return def.dishBonusEffects(); }
    public List<MobEffectInstance> dishBonusEffects(int spicyLevel) { return def.dishBonusEffects(spicyLevel); }

    static {
        for (SoupBase base : BY_ID) {
            Soups.registerBuiltin(base.def);
        }
        Soups.builtinsDone();
        for (SoupBase base : BY_ID) {
            Config.rememberSoupBowl(base.textureKey(), DEFAULT_BOWL_HUNGER, base.defaultBowlSaturation(),
                base.defaultDuration(), 0, base.defaultSecondDuration(), 0,
                base.defaultFireDuration(), 0, base.defaultHungerDuration(), 0);
        }
        SoupVisual.verifyAgainstSoupBases();
    }
}
