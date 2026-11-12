// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 内置蘸料的不可变定义；旧枚举与位掩码保留兼容。作者：mryt233 */
public final class SeasoningDef {
    private static final float[][][] DOTS = {
        {{6.2F, 6.6F}, {9.4F, 9.0F}}, {{7.4F, 6.0F}, {8.8F, 7.6F}}, {{6.0F, 8.6F}, {9.6F, 6.4F}},
        {{7.8F, 9.4F}, {6.6F, 7.6F}}, {{8.4F, 6.8F}, {7.0F, 9.6F}}, {{9.2F, 8.2F}, {6.4F, 9.0F}},
        {{7.6F, 7.0F}, {8.6F, 9.6F}}, {{6.8F, 6.2F}, {9.0F, 7.0F}, {7.4F, 8.8F}}, {{8.2F, 8.4F}, {6.0F, 7.2F}, {9.6F, 9.4F}},
        {{7.0F, 8.0F}, {9.0F, 6.0F}},                       // 蒜泥（2026-10-25）
        {{6.4F, 6.8F}, {8.2F, 9.2F}},                       // 蚝油（r137）
        {{8.8F, 6.2F}, {7.2F, 8.0F}},                       // 芝麻酱（r137）
        {{6.0F, 9.0F}, {9.4F, 7.4F}, {7.6F, 6.4F}},         // 花生碎（r137）
        {{8.6F, 8.8F}, {6.6F, 6.2F}},                       // 小米辣（r137）
        {{7.2F, 9.4F}, {9.2F, 6.6F}, {6.2F, 7.8F}}};        // 折耳根碎（r139）
    private final Seasoning legacy;
    private final String id;
    private final int index, color, ticks, amplifier, hunger;
    private final Holder<MobEffect> effect;
    private final Holder<MobEffect>[] removes;
    private final Supplier<Item>[] raws;
    private final float[][] dots;
    private final float dotSize;
    private final String nameKey;
    private final String effectKey;
    private volatile List<Item> rawList;

    SeasoningDef(Seasoning legacy, String id, int index, int color, Holder<MobEffect> effect,
                 int ticks, int amplifier, int hunger, Holder<MobEffect>[] removes, Supplier<Item>[] raws) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(removes, "removes");
        Objects.requireNonNull(raws, "raws");
        if (index < 0) throw new IllegalArgumentException("蘸料序号不能为负");
        if (raws.length == 0) throw new IllegalArgumentException("蘸料至少需要一种原料");
        for (Supplier<Item> raw : raws) Objects.requireNonNull(raw, "raw");
        for (Holder<MobEffect> removed : removes) Objects.requireNonNull(removed, "removed");
        this.legacy = legacy; this.id = id; this.index = index; this.color = color; this.effect = effect;
        this.ticks = ticks; this.amplifier = amplifier; this.hunger = hunger;
        this.removes = removes.clone(); this.raws = raws.clone();
        this.dots = index < DOTS.length ? copyDots(DOTS[index]) : generatedDots(id);
        this.dotSize = index == 7 || index == 8 ? 0.7F : 0.5F;
        this.nameKey = defaultKey(id, false);
        this.effectKey = defaultKey(id, true);
    }

    private SeasoningDef(Builder builder) {
        this.legacy = null;
        this.id = builder.id;
        this.index = -1;
        this.color = builder.color;
        this.effect = builder.effect;
        this.ticks = builder.ticks;
        this.amplifier = builder.amplifier;
        this.hunger = builder.hunger;
        @SuppressWarnings("unchecked")
        Holder<MobEffect>[] removed = builder.removes.toArray((Holder<MobEffect>[]) new Holder<?>[0]);
        @SuppressWarnings("unchecked")
        Supplier<Item>[] sources = builder.raws.toArray((Supplier<Item>[]) new Supplier<?>[0]);
        this.removes = removed;
        this.raws = sources;
        this.dots = builder.dots != null ? copyDots(builder.dots) : generatedDots(this.id);
        this.dotSize = builder.dotSize;
        this.nameKey = builder.nameKey != null ? builder.nameKey : defaultKey(this.id, false);
        this.effectKey = builder.effectKey != null ? builder.effectKey : defaultKey(this.id, true);
    }

    private static String defaultKey(String id, boolean effectSuffix) {
        int colon = id.indexOf(':');
        String base = colon >= 0
            ? "text." + id.substring(0, colon) + ".seasoning." + id.substring(colon + 1)
            : HotPotBlockEntity.key("seasoning." + id);
        return effectSuffix ? base + ".effect" : base;
    }
    private static float[][] generatedDots(String id) {
        int hash = id.hashCode();
        return new float[][]{{6F + Math.floorMod(hash, 40) / 10F, 6F + Math.floorMod(hash >>> 8, 40) / 10F},
            {6F + Math.floorMod(hash >>> 16, 40) / 10F, 6F + Math.floorMod(hash >>> 24, 40) / 10F}};
    }
    private static float[][] copyDots(float[][] source) {
        float[][] copy = new float[source.length][];
        for (int i = 0; i < source.length; i++) copy[i] = source[i].clone();
        return copy;
    }
    public Seasoning legacy() { return legacy; }
    public String id() { return id; }
    public int ordinal() { return index >= 0 ? index : Seasonings.indexOf(this); }
    public int bit() { return index >= 0 && index < 31 ? 1 << index : 0; }
    public int color() { return color; }
    public float[][] dots() { return copyDots(dots); }
    public int dotCount() { return dots.length; }
    public float dotX(int i) { return dots[i][0]; }
    public float dotZ(int i) { return dots[i][1]; }
    public float dotSize() { return dotSize; }
    public boolean garnish() { return index == 7 || index == 8; }
    public Item raw() { return raws[0].get(); }
    public List<Item> raws() {
        List<Item> items = rawList;
        if (items == null) {
            List<Item> found = new ArrayList<>(raws.length);
            for (Supplier<Item> raw : raws) found.add(raw.get());
            items = List.copyOf(found);
            rawList = items;
        }
        return items;
    }
    public Item rawShown(long millis) { return raws[(int)(millis / 1000 % raws.length)].get(); }
    public Component rawNames() {
        MutableComponent out = Component.empty();
        for (int i = 0; i < raws.length; i++) {
            if (i > 0) out.append(" / ");
            out.append(new ItemStack(raws[i].get()).getHoverName());
        }
        return out;
    }
    public String nameKey() { return nameKey; }
    public String effectKey() { return effectKey; }
    public void apply(LivingEntity user) {
        for (Holder<MobEffect> removed : removes) user.removeEffect(removed);
        if (hunger > 0 && user instanceof Player player) {
            int amount = Config.seasoningHunger(id, hunger);
            if (amount > 0) player.getFoodData().eat(amount, 0);
        }
        if (effect != null) {
            int duration = Config.seasoningTicks(id, ticks);
            if (duration > 0) user.addEffect(new MobEffectInstance(effect, duration,
                Config.seasoningAmplifier(id, amplifier)));
        }
    }

    /** 4F-1（mryt233）：构造自定义蘸料定义。 */
    public static Builder builder(ResourceLocation id) {
        return new Builder(Objects.requireNonNull(id, "id").toString());
    }

    /** 4F-1（mryt233）：构造自定义蘸料定义（不带命名空间按火锅命名空间解释）。 */
    public static Builder builder(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isEmpty()) throw new IllegalArgumentException("蘸料 ID 不能为空");
        return new Builder(SauceIds.full(id));
    }

    /** 4F-1（mryt233）：蘸料定义构造器（支持延迟原料引用与自定义语言键）。 */
    public static final class Builder {
        private final String id;
        private int color = 0xFFD4A054;
        private Holder<MobEffect> effect;
        private int ticks;
        private int amplifier;
        private int hunger;
        private final List<Holder<MobEffect>> removes = new ArrayList<>();
        private final List<Supplier<Item>> raws = new ArrayList<>();
        private float[][] dots;
        private float dotSize = 0.5F;
        private String nameKey;
        private String effectKey;

        private Builder(String id) {
            this.id = id;
        }

        public Builder color(int color) { this.color = color; return this; }
        public Builder effect(Holder<MobEffect> effect, int ticks) { return effect(effect, ticks, 0); }
        public Builder effect(Holder<MobEffect> effect, int ticks, int amplifier) {
            this.effect = effect; this.ticks = ticks; this.amplifier = amplifier; return this;
        }
        public Builder hunger(int hunger) { this.hunger = hunger; return this; }
        @SafeVarargs
        public final Builder removes(Holder<MobEffect>... effects) {
            if (effects != null) {
                for (Holder<MobEffect> removed : effects) removes.add(Objects.requireNonNull(removed, "removed"));
            }
            return this;
        }
        public Builder raw(Supplier<Item> raw) {
            raws.add(Objects.requireNonNull(raw, "raw")); return this;
        }
        @SafeVarargs
        public final Builder raws(Supplier<Item>... sources) {
            if (sources != null) {
                for (Supplier<Item> raw : sources) raws.add(Objects.requireNonNull(raw, "raw"));
            }
            return this;
        }
        public Builder raws(List<Supplier<Item>> sources) {
            if (sources != null) {
                for (Supplier<Item> raw : sources) raws.add(Objects.requireNonNull(raw, "raw"));
            }
            return this;
        }
        public Builder garnish(boolean garnish) { this.dotSize = garnish ? 0.7F : 0.5F; return this; }
        public Builder dots(float[][] dots, float dotSize) {
            this.dots = dots == null ? null : copyDots(dots);
            this.dotSize = dotSize;
            return this;
        }
        public Builder nameKey(String nameKey) { this.nameKey = Objects.requireNonNull(nameKey, "nameKey"); return this; }
        public Builder effectKey(String effectKey) { this.effectKey = Objects.requireNonNull(effectKey, "effectKey"); return this; }

        public SeasoningDef build() {
            if (raws.isEmpty()) throw new IllegalArgumentException("蘸料 " + id + " 至少需要一种原料");
            return new SeasoningDef(this);
        }
    }
}
