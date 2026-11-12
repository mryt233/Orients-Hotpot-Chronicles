// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
    public int ordinal() { return index; }
    public int bit() { return 1 << index; }
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
    public String nameKey() { return HotPotBlockEntity.key("seasoning." + id); }
    public String effectKey() { return HotPotBlockEntity.key("seasoning." + id + ".effect"); }
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
}
