// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 蘸料台上的调料；内置 15 种定义转发给 {@link SeasoningDef} 与 {@link Seasonings}。
 *
 * <p>mryt233 改（2026-11-13）：适配 26.1 {@code MobEffects} 命名（{@code SPEED/NAUSEA/HASTE/RESISTANCE/JUMP_BOOST/STRENGTH}）。</p>
 * 作者：mryt233
 */
public enum Seasoning {
    MSG("msg", 0xFFECECF0, null, 0, 0, 2, new Holder[0], () -> Items.WHEAT),
    CHILI_OIL("chili_oil", 0xFFC42816, MobEffects.FIRE_RESISTANCE, 200, 0, 0, new Holder[0],
        () -> Items.BLAZE_POWDER),
    SWEET_SOUR_SAUCE("sweet_sour_sauce", 0xFFC85A3C, MobEffects.SPEED, 200, 0, 0,
        new Holder[0], () -> Items.SWEET_BERRIES),
    HONEY("honey", 0xFFECA826, null, 0, 0, 0,
        new Holder[]{MobEffects.HUNGER, MobEffects.POISON}, () -> Items.HONEY_BOTTLE),
    SESAME_OIL("sesame_oil", 0xFFCC8A28, MobEffects.REGENERATION, 100, 0, 0, new Holder[0],
        () -> Items.SUNFLOWER, () -> Items.PUMPKIN_SEEDS),
    VINEGAR("vinegar", 0xFF462418, null, 0, 0, 0,
        new Holder[]{MobEffects.NAUSEA, MobEffects.HUNGER}, () -> Items.APPLE, () -> Items.BEETROOT),
    SUGAR("sugar", 0xFFF0EEE8, MobEffects.HASTE, 200, 0, 0, new Holder[0], () -> Items.SUGAR),
    CHOPPED_SCALLION("chopped_scallion", 0xFF6CB03A, null, 0, 0, 0, new Holder[0],
        () -> HotPotContent.SCALLION),
    MINCED_CILANTRO("minced_cilantro", 0xFF4E9A2C, null, 0, 0, 0, new Holder[0],
        () -> HotPotContent.CILANTRO),
    GARLIC_PASTE("garlic_paste", 0xFFE8D696, MobEffects.RESISTANCE, 160, 0, 0,
        new Holder[0], () -> HotPotContent.GARLIC_PASTE),
    OYSTER_SAUCE("oyster_sauce", 0xFF5A321E, MobEffects.WATER_BREATHING, 200, 0, 0,
        new Holder[0], () -> HotPotContent.OYSTER_SAUCE),
    SESAME_PASTE("sesame_paste", 0xFFC89E64, MobEffects.ABSORPTION, 200, 0, 0,
        new Holder[0], () -> HotPotContent.SESAME_PASTE),
    CRUSHED_PEANUTS("crushed_peanuts", 0xFFDAB46E, MobEffects.JUMP_BOOST, 200, 0, 0,
        new Holder[0], () -> HotPotContent.CRUSHED_PEANUTS),
    MILLET_CHILI("millet_chili", 0xFFE2281C, MobEffects.STRENGTH, 160, 0, 0,
        new Holder[0], () -> HotPotContent.MILLET_CHILI),
    MINCED_ZHE_ER_GEN("minced_zhe_er_gen", 0xFFD8D2BC, MobEffects.RESISTANCE, 200, 0, 0,
        new Holder[]{MobEffects.POISON}, () -> HotPotContent.MINCED_ZHE_ER_GEN);

    public static final Seasoning[] ALL = values();

    private final SeasoningDef def;
    private final String id;
    private final Supplier<Item>[] raws;
    private final int color;
    private final Holder<MobEffect> effect;
    private final int defaultTicks;
    private final int defaultAmplifier;
    private final int defaultHunger;
    private final Holder<MobEffect>[] removes;

    @SafeVarargs
    Seasoning(String id, int color,
              Holder<MobEffect> effect,
              int defaultTicks, int defaultAmplifier, int defaultHunger,
              Holder<MobEffect>[] removes,
              Supplier<Item>... raws) {
        this.def = new SeasoningDef(this, id, ordinal(), color, effect, defaultTicks, defaultAmplifier, defaultHunger, removes, raws);
        this.id = id;
        this.raws = raws;
        this.color = color;
        this.effect = effect;
        this.defaultTicks = defaultTicks;
        this.defaultAmplifier = defaultAmplifier;
        this.defaultHunger = defaultHunger;
        this.removes = removes;
    }

    public SeasoningDef def() { return def; }
    public String id() { return id; }
    public Item raw() { return raws[0].get(); }
    public List<Item> raws() { return def.raws(); }
    public Item rawShown(long millis) { return raws[(int) (millis / 1000 % raws.length)].get(); }

    public Component rawNames() {
        MutableComponent out = Component.empty();
        for (int i = 0; i < raws.length; i++) {
            if (i > 0) out.append(" / ");
            out.append(new ItemStack(raws[i].get()).getHoverName());
        }
        return out;
    }

    public int color() { return color; }
    public int bit() { return 1 << ordinal(); }
    public String nameKey() { return HotPotBlockEntity.key("seasoning." + id); }
    public String effectKey() { return HotPotBlockEntity.key("seasoning." + id + ".effect"); }

    @Deprecated public static void appendEffects(int mask, List<Component> lines) { appendEffects(SauceIds.fromMask(mask), lines); }

    public static void appendEffects(List<String> ids, List<Component> lines) {
        for (String id : SauceIds.normalize(ids)) {
            SeasoningDef s = SauceIds.def(id);
            if (s == null) {
                lines.add(Component.translatable(HotPotBlockEntity.key("sauce.unknown")).append(" (" + id + ")")
                    .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal(" ").append(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.line"),
                    Component.translatable(s.nameKey()), Component.translatable(s.effectKey())))
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static Seasoning ofRaw(ItemStack stack) {
        SeasoningDef found = Seasonings.ofRaw(stack);
        return found == null ? null : found.legacy();
    }

    public void apply(LivingEntity user) {
        def.apply(user);
    }

    static {
        for (Seasoning seasoning : ALL) {
            Config.rememberSeasoning(seasoning.id, seasoning.defaultTicks, seasoning.defaultAmplifier,
                seasoning.defaultHunger);
        }
    }

    @Deprecated public static void applyAll(int mask, LivingEntity user) { applyAll(SauceIds.fromMask(mask), user); }

    public static void applyAll(List<String> ids, LivingEntity user) {
        for (SeasoningDef s : SauceIds.defs(ids)) {
            s.apply(user);
        }
    }

    @Deprecated public static int mixColor(int mask) { return mixColor(SauceIds.fromMask(mask)); }

    public static int mixColor(List<String> ids) {
        int r = 0, g = 0, b = 0, n = 0;
        for (SeasoningDef s : SauceIds.defs(ids)) {
            r += (s.color() >> 16) & 255;
            g += (s.color() >> 8) & 255;
            b += s.color() & 255;
            n++;
        }
        return n == 0 ? 0 : 0xFF000000 | (r / n) << 16 | (g / n) << 8 | (b / n);
    }
}
