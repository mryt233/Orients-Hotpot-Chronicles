// mryt233 改：26.2 原版 API。
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
 * 蘸料台上的调料（r103，用户定）。顺序 = 蘸料台小碗的顺序（阶梯式：从最上层到最下层、每层从左到右）= 补货界面的格子顺序。
 * 每种由一种原料补货（1 个原料 = 4 份）；蘸过的菜吃下去时，碗里有几种就叠几种效果（用户：全部叠加）。
 * 葱花、香菜碎末只提味，没有效果（用户）。
 * 2026-10-23 用户改：麻酱换成味精（原料小麦，提鲜：多回 2 点饥饿值）；醋的原料改成苹果或甜菜根（一种调料可以有几种原料）。
 * mryt233 改（2026-11-06，r137）：在蒜泥之后加蚝油、芝麻酱、花生碎、小米辣，现在共 14 种；原料表已按 r146f 改为单列分页（mryt233 改）。
 *
 * <p>mryt233 改（2026-11-10，r147）：把「效果种类 + 默认时长 + 默认等级 + 多回饥饿」全部收进
 * 枚举构造参数。以前这些数字分两处写：{@code apply} 里的 switch 写效果种类与默认时长，
 * 静态块里的 {@link Config#rememberSeasoning} 又抄一遍同样的数字，改一处忘另一处不会报错。
 * 现在只有这一份数据，{@link #apply} 和配置默认值都从它读。</p>
 * 作者：mryt233 */
public enum Seasoning {
    MSG("msg", 0xFFECECF0, null, 0, 0, 2, new Holder[0], () -> Items.WHEAT),
    CHILI_OIL("chili_oil", 0xFFC42816, MobEffects.FIRE_RESISTANCE, 200, 0, 0, new Holder[0],
        () -> Items.BLAZE_POWDER),
    SWEET_SOUR_SAUCE("sweet_sour_sauce", 0xFFC85A3C, MobEffects.SPEED, 200, 0, 0,
        new Holder[0], () -> Items.SWEET_BERRIES),
    HONEY("honey", 0xFFECA826, null, 0, 0, 0,
        new Holder[]{MobEffects.HUNGER, MobEffects.POISON}, () -> Items.HONEY_BOTTLE),
    SESAME_OIL("sesame_oil", 0xFFCC8A28, MobEffects.REGENERATION, 100, 0, 0, new Holder[0],
        () -> Items.SUNFLOWER, () -> Items.PUMPKIN_SEEDS),   // 用户 2026-10-23：向日葵或南瓜种子
    VINEGAR("vinegar", 0xFF462418, null, 0, 0, 0,
        new Holder[]{MobEffects.NAUSEA, MobEffects.HUNGER}, () -> Items.APPLE, () -> Items.BEETROOT),
    SUGAR("sugar", 0xFFF0EEE8, MobEffects.HASTE, 200, 0, 0, new Holder[0], () -> Items.SUGAR),
    CHOPPED_SCALLION("chopped_scallion", 0xFF6CB03A, null, 0, 0, 0, new Holder[0],
        () -> HotPotContent.SCALLION),
    MINCED_CILANTRO("minced_cilantro", 0xFF4E9A2C, null, 0, 0, 0, new Holder[0],
        () -> HotPotContent.CILANTRO),
    GARLIC_PASTE("garlic_paste", 0xFFE8D696, MobEffects.RESISTANCE, 160, 0, 0,
        new Holder[0], () -> HotPotContent.GARLIC_PASTE),   // 蒜泥（2026-10-25 用户加，第 10 种）
    // mryt233 改（2026-11-06）：r137 新增 4 种蘸料，共 14 种。蚝油（水下呼吸）、芝麻酱（伤害吸收）、
    // 花生碎（跳跃提升）、小米辣（力量）；原料都是 r137 新加的蘸料原料，1 个原料照旧 4 份。
    OYSTER_SAUCE("oyster_sauce", 0xFF5A321E, MobEffects.WATER_BREATHING, 200, 0, 0,
        new Holder[0], () -> HotPotContent.OYSTER_SAUCE),
    SESAME_PASTE("sesame_paste", 0xFFC89E64, MobEffects.ABSORPTION, 200, 0, 0,
        new Holder[0], () -> HotPotContent.SESAME_PASTE),
    CRUSHED_PEANUTS("crushed_peanuts", 0xFFDAB46E, MobEffects.JUMP_BOOST, 200, 0, 0,
        new Holder[0], () -> HotPotContent.CRUSHED_PEANUTS),
    MILLET_CHILI("millet_chili", 0xFFE2281C, MobEffects.STRENGTH, 160, 0, 0,
        new Holder[0], () -> HotPotContent.MILLET_CHILI),
    // mryt233 改（2026-11-06，r139）：第 15 种调料——折耳根碎（西南吃法：折耳根剁碎拌进蘸水）。
    // 象征折耳根清热解毒：给 10 秒抗性提升 I（和蒜泥同一档，但时间更长），原料是折耳根碾成的碎末；
    // 顺手解掉中毒（毒也是「热毒」）。
    MINCED_ZHE_ER_GEN("minced_zhe_er_gen", 0xFFD8D2BC, MobEffects.RESISTANCE, 200, 0, 0,
        new Holder[]{MobEffects.POISON}, () -> HotPotContent.MINCED_ZHE_ER_GEN);

    public static final Seasoning[] ALL = values();

    private final SeasoningDef def;
    private final String id;
    private final Supplier<Item>[] raws;
    private final int color;
    /** 主效果（null = 这种调料没有正面效果）。 */
    private final Holder<MobEffect> effect;
    /** 主效果的默认时长 / 等级（配成 0 就不加）。 */
    private final int defaultTicks;
    private final int defaultAmplifier;
    /** 多回的饥饿值（只有味精用）。 */
    private final int defaultHunger;
    /** 吃下时解除的状态效果。 */
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

    public String id() {
        return id;
    }

    /** 第一种原料（只要一种时用）。 */
    public Item raw() {
        return raws[0].get();
    }

    /** 所有能补这种调料的原料（类加载时算好的常量表，直接返回，不再每次新建）。mryt233 改 */
    public List<Item> raws() {
        return def.raws();
    }

    /** 界面上空格子轮流显示的原料（有几种就每秒换一种）。 */
    public Item rawShown(long millis) {
        return raws[(int) (millis / 1000 % raws.length)].get();
    }

    /** 原料名字，几种用「 / 」连起来。 */
    public Component rawNames() {
        MutableComponent out = Component.empty();
        for (int i = 0; i < raws.length; i++) {
            if (i > 0) {
                out.append(" / ");
            }
            out.append(new ItemStack(raws[i].get()).getHoverName());
        }
        return out;
    }

    /** 碗里这种调料的颜色（ARGB），几种混在一起时取平均。 */
    public int color() {
        return color;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String nameKey() {
        return HotPotBlockEntity.key("seasoning." + id);
    }

    /** 效果说明（物品提示里用，和 apply 对应；新增 2026-10-25）。 */
    public String effectKey() {
        return HotPotBlockEntity.key("seasoning." + id + ".effect");
    }

    /** 「 辣油：抗火 10 秒」这样一行灰字：蘸料碗、蘸过的菜的提示共用。 */
    @Deprecated public static void appendEffects(int mask, List<Component> lines) { appendEffects(SauceIds.fromMask(mask), lines); }

    public static void appendEffects(List<String> ids, List<Component> lines) {
        // mryt233 改（4E-1）：按调料 ID 列表，不再用位掩码。
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

    /** 这件东西是哪种调料的原料；不是返回 null（mryt233 改：一次 Map 查表，不再遍历 15 种调料）。 */
    public static Seasoning ofRaw(ItemStack stack) {
        SeasoningDef found = Seasonings.ofRaw(stack);
        return found == null ? null : found.legacy();
    }

    /**
     * 吃下蘸过这种调料的菜时的效果。
     * 时长、等级、味精多回的饥饿值都能在配置文件里改（见 Config）。
     */
    public void apply(LivingEntity user) {
        def.apply(user);   // mryt233 改：旧入口保持，效果顺序由定义执行。
    }

    static {
        // 默认值记进配置模板：duration / amplifier / hunger。
        // mryt233 改（2026-11-10，r147）：数字不再手抄一遍，直接从枚举自己的字段登记。
        for (Seasoning seasoning : ALL) {
            Config.rememberSeasoning(seasoning.id, seasoning.defaultTicks, seasoning.defaultAmplifier,
                seasoning.defaultHunger);
        }
    }

    /** ID 列表里的每种调料都生效一次（未知 ID 跳过）。 */
    @Deprecated public static void applyAll(int mask, LivingEntity user) { applyAll(SauceIds.fromMask(mask), user); }

    public static void applyAll(List<String> ids, LivingEntity user) {
        for (SeasoningDef s : SauceIds.defs(ids)) {
            s.apply(user);
        }
    }

    /** ID 列表里几种调料的平均颜色（ARGB）；空的返回 0。 */
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
