package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 一种汤底的全部「定义」：它是谁、倒进去的是什么、喝下去有什么效果、界面和气泡长什么色；
 * 以及由这些定义算出来的玩法（mryt233 改：1.20.1 适配直接使用 MobEffect）。
 * 作者：mryt233 */
public record SoupDef(
    ResourceLocation id, String langKey, String bowlId, float defaultSpeedMul,
    Supplier<Item> sourceItem, Item remainderItem, Supplier<Item> additive,
    int defaultBowlHunger, float defaultBowlSaturation,
    MobEffect mainEffect, int defaultDuration,
    MobEffect secondEffect, int defaultSecondDuration,
    MobEffect fireEffect, int defaultFireDuration,
    MobEffect hungerEffect, int defaultHungerDuration,
    boolean clearsNausea, boolean spicy,
    int screenBase, int screenHi, int screenLo, int[] screenBits,
    int bubbleBody, int bubbleHi) {

    public SoupDef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(langKey, "langKey");
        Objects.requireNonNull(bowlId, "bowlId");
        Objects.requireNonNull(sourceItem, "sourceItem");
        Objects.requireNonNull(remainderItem, "remainderItem");
        Objects.requireNonNull(screenBits, "screenBits");
        screenBits = screenBits.clone();
    }

    /** 食材小点颜色（返回拷贝）。 */
    @Override
    public int[] screenBits() {
        return screenBits.clone();
    }

    public String key() {
        return HotPot.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    public String nameKey() {
        return (langKey.startsWith("text.") || (langKey.contains(".") && !langKey.startsWith("soup.")))
            ? langKey : "text." + HotPot.MOD_ID + "." + langKey;
    }

    public SoupVisual visual() {
        for (SoupVisual candidate : SoupVisual.values()) {
            if (candidate != SoupVisual.NONE && candidate != SoupVisual.OTHER
                && candidate.getSerializedName().equals(key())) {
                return candidate;
            }
        }
        return SoupVisual.OTHER;
    }

    public Item source() {
        return sourceItem.get();
    }

    public Item remainder() {
        return remainderItem;
    }

    public boolean matchesSource(ItemStack stack) {
        return stack.is(sourceItem.get()) || (additive != null && stack.is(additive.get()));
    }

    public boolean isAdditive(ItemStack stack) {
        return additive != null && !stack.is(sourceItem.get());
    }

    public float speedMul() {
        return Soups.isBuiltin(id) ? Config.overriddenSoupSpeed(key(), defaultSpeedMul) : defaultSpeedMul;
    }

    public int bowlHunger() {
        return Soups.isBuiltin(id) ? Config.overriddenSoupBowlInt(key(), "hunger", defaultBowlHunger) : defaultBowlHunger;
    }

    public float bowlSaturation() {
        return Soups.isBuiltin(id)
            ? Config.overriddenSoupBowlFloat(key(), "saturation", defaultBowlSaturation) : defaultBowlSaturation;
    }

    public List<MobEffectInstance> dishBonusEffects() {
        return dishBonusEffects(0);
    }

    public List<MobEffectInstance> dishBonusEffects(int spicyLevel) {
        if (spicy) {
            return spicyBonusEffects(spicyLevel);
        }
        List<MobEffectInstance> list = new ArrayList<>();
        boolean builtin = Soups.isBuiltin(id);
        String name = key();
        int amplifier = builtin ? Config.overriddenSoupBowlInt(name, "amplifier", 0) : 0;
        addEffect(list, mainEffect,
            builtin ? Config.overriddenSoupBowlInt(name, "duration", defaultDuration) : defaultDuration, amplifier);
        addEffect(list, secondEffect,
            builtin ? Config.overriddenSoupBowlInt(name, "second_duration", defaultSecondDuration) : defaultSecondDuration,
            builtin ? Config.overriddenSoupBowlInt(name, "second_amplifier", 0) : 0);
        return list;
    }

    List<MobEffectInstance> spicyBonusEffects(int spicyLevel) {
        List<MobEffectInstance> list = new ArrayList<>();
        boolean builtin = Soups.isBuiltin(id);
        String name = key();
        int ticks = builtin ? Config.overriddenSoupBowlInt(name, "duration", defaultDuration) : defaultDuration;
        int amplifier = builtin ? Config.overriddenSoupBowlInt(name, "amplifier", 0) : 0;
        int secondTicks = builtin
            ? Config.overriddenSoupBowlInt(name, "second_duration", defaultSecondDuration) : defaultSecondDuration;
        int secondAmp = builtin ? Config.overriddenSoupBowlInt(name, "second_amplifier", 0) : 0;
        int level = Math.max(0, Math.min(HotPotBlockEntity.SPICY_MAX, spicyLevel));
        if (ticks > 0) {
            ticks += level * Config.spicyLevelDurationStep();
            amplifier += level >= Config.spicyLevelAmplifierFrom() ? 1 : 0;
        }
        addEffect(list, mainEffect, ticks, amplifier);
        int speedAdd = level == 1 ? Config.spicySecondBonusL1()
            : (level == 2 ? Config.spicySecondBonusL2()
            : (level >= HotPotBlockEntity.SPICY_MAX ? Config.spicySecondBonusL3() : 0));
        addEffect(list, secondEffect, secondTicks > 0 ? secondTicks + speedAdd : 0, secondAmp);
        if (level >= Config.spicyFireFromLevel()) {
            int fireTicks = builtin
                ? Config.overriddenSoupBowlInt(name, "fire_duration", defaultFireDuration) : defaultFireDuration;
            addEffect(list, fireEffect,
                level >= HotPotBlockEntity.SPICY_MAX ? fireTicks * Config.spicyFireTopMultiplier() : fireTicks,
                builtin ? Config.overriddenSoupBowlInt(name, "fire_amplifier", 0) : 0);
            int hungerTicks = builtin
                ? Config.overriddenSoupBowlInt(name, "hunger_duration", defaultHungerDuration) : defaultHungerDuration;
            if (hungerTicks > 0 && level >= HotPotBlockEntity.SPICY_MAX) {
                hungerTicks += Config.spicyHungerTopBonus();
            }
            addEffect(list, hungerEffect, hungerTicks,
                builtin ? Config.overriddenSoupBowlInt(name, "hunger_amplifier", 0) : 0);
        }
        return list;
    }

    private static void addEffect(List<MobEffectInstance> list, MobEffect effect, int ticks, int amplifier) {
        if (effect == null || ticks <= 0) {
            return;
        }
        list.add(new MobEffectInstance(effect, ticks, amplifier, false, true));
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private String langKey;
        private String bowlId;
        private float defaultSpeedMul = 1.0F;
        private Supplier<Item> sourceItem;
        private Item remainderItem = Items.AIR;
        private Supplier<Item> additive;
        private int defaultBowlHunger = SoupBase.DEFAULT_BOWL_HUNGER;
        private float defaultBowlSaturation = 0.6F;
        private MobEffect mainEffect;
        private int defaultDuration;
        private MobEffect secondEffect;
        private int defaultSecondDuration;
        private MobEffect fireEffect;
        private int defaultFireDuration;
        private MobEffect hungerEffect;
        private int defaultHungerDuration;
        private boolean clearsNausea;
        private boolean spicy;
        private int screenBase = 0xFFD9C7A3;
        private int screenHi = 0xFFF1E5C9;
        private int screenLo = 0xFFB8A37D;
        private int[] screenBits = {0xFF8C7A5B, 0xFFE6D6B8};
        private int bubbleBody = 0xFFF3EAD6;
        private int bubbleHi = 0xFFFFFFFF;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.langKey = "text." + id.getNamespace() + ".soup." + id.getPath();
            this.bowlId = id.getPath() + "_bowl";
        }

        private Builder(SoupDef base) {
            this.id = base.id;
            this.langKey = base.langKey;
            this.bowlId = base.bowlId;
            this.defaultSpeedMul = base.defaultSpeedMul;
            this.sourceItem = base.sourceItem;
            this.remainderItem = base.remainderItem;
            this.additive = base.additive;
            this.defaultBowlHunger = base.defaultBowlHunger;
            this.defaultBowlSaturation = base.defaultBowlSaturation;
            this.mainEffect = base.mainEffect;
            this.defaultDuration = base.defaultDuration;
            this.secondEffect = base.secondEffect;
            this.defaultSecondDuration = base.defaultSecondDuration;
            this.fireEffect = base.fireEffect;
            this.defaultFireDuration = base.defaultFireDuration;
            this.hungerEffect = base.hungerEffect;
            this.defaultHungerDuration = base.defaultHungerDuration;
            this.clearsNausea = base.clearsNausea;
            this.spicy = base.spicy;
            this.screenBase = base.screenBase;
            this.screenHi = base.screenHi;
            this.screenLo = base.screenLo;
            this.screenBits = base.screenBits.clone();
            this.bubbleBody = base.bubbleBody;
            this.bubbleHi = base.bubbleHi;
        }

        public Builder langKey(String langKey) { this.langKey = Objects.requireNonNull(langKey, "langKey"); return this; }
        public Builder bowlId(String bowlId) { this.bowlId = Objects.requireNonNull(bowlId, "bowlId"); return this; }
        public Builder speed(float speedMul) { this.defaultSpeedMul = speedMul; return this; }
        public Builder source(Supplier<Item> sourceItem) { return source(sourceItem, Items.AIR); }
        public Builder source(Supplier<Item> sourceItem, Item remainderItem) {
            this.sourceItem = Objects.requireNonNull(sourceItem, "sourceItem");
            this.remainderItem = Objects.requireNonNull(remainderItem, "remainderItem");
            return this;
        }
        public Builder additive(Supplier<Item> additive) { this.additive = Objects.requireNonNull(additive, "additive"); return this; }
        public Builder bowlFood(int hunger, float saturation) {
            this.defaultBowlHunger = hunger;
            this.defaultBowlSaturation = saturation;
            return this;
        }
        public Builder mainEffect(MobEffect effect, int durationTicks) {
            this.mainEffect = effect; this.defaultDuration = durationTicks; return this;
        }
        public Builder secondEffect(MobEffect effect, int durationTicks) {
            this.secondEffect = effect; this.defaultSecondDuration = durationTicks; return this;
        }
        public Builder fireEffect(MobEffect effect, int durationTicks) {
            this.fireEffect = effect; this.defaultFireDuration = durationTicks; return this;
        }
        public Builder hungerEffect(MobEffect effect, int durationTicks) {
            this.hungerEffect = effect; this.defaultHungerDuration = durationTicks; return this;
        }
        public Builder clearsNausea(boolean clearsNausea) { this.clearsNausea = clearsNausea; return this; }
        public Builder spicy(boolean spicy) { this.spicy = spicy; return this; }
        public Builder screenColors(int base, int hi, int lo, int... bits) {
            this.screenBase = base; this.screenHi = hi; this.screenLo = lo;
            if (bits != null && bits.length > 0) this.screenBits = bits.clone();
            return this;
        }
        public Builder bubbleColors(int body, int hi) {
            this.bubbleBody = body; this.bubbleHi = hi; return this;
        }

        public SoupDef build() {
            if (sourceItem == null && additive == null) {
                throw new IllegalStateException("汤底 " + id + " 必须设置倒入原料 source 或锅底料 additive");
            }
            Supplier<Item> resolvedSource = sourceItem != null ? sourceItem : () -> Items.AIR;
            return new SoupDef(id, langKey, bowlId, defaultSpeedMul, resolvedSource, remainderItem, additive,
                defaultBowlHunger, defaultBowlSaturation, mainEffect, defaultDuration, secondEffect, defaultSecondDuration,
                fireEffect, defaultFireDuration, hungerEffect, defaultHungerDuration, clearsNausea, spicy,
                screenBase, screenHi, screenLo, screenBits, bubbleBody, bubbleHi);
        }
    }
}
