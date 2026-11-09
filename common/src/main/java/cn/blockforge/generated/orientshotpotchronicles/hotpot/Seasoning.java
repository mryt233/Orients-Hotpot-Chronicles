package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * 蘸料台上的调料（r103，用户定）。顺序 = 蘸料台小碗的顺序（阶梯式：从最上层到最下层、每层从左到右）= 补货界面的格子顺序。
 * 每种由一种原料补货（1 个原料 = 4 份）；蘸过的菜吃下去时，碗里有几种就叠几种效果（用户：全部叠加）。
 * 葱花、香菜碎末只提味，没有效果（用户）。
 * 2026-10-23 用户改：麻酱换成味精（原料小麦，提鲜：多回 2 点饥饿值）；醋的原料改成苹果或甜菜根（一种调料可以有几种原料）。
 * mryt233 改（2026-11-06，r137）：在蒜泥之后加蚝油、芝麻酱、花生碎、小米辣，现在共 14 种；原料表已按 r146f 改为单列分页（mryt233 改）。
 * 作者：mryt233 */
public enum Seasoning {
    MSG("msg", 0xFFECECF0, () -> Items.WHEAT),
    CHILI_OIL("chili_oil", 0xFFC42816, () -> Items.BLAZE_POWDER),
    SWEET_SOUR_SAUCE("sweet_sour_sauce", 0xFFC85A3C, () -> Items.SWEET_BERRIES),
    HONEY("honey", 0xFFECA826, () -> Items.HONEY_BOTTLE),
    SESAME_OIL("sesame_oil", 0xFFCC8A28, () -> Items.SUNFLOWER, () -> Items.PUMPKIN_SEEDS),   // 用户 2026-10-23：向日葵或南瓜种子
    VINEGAR("vinegar", 0xFF462418, () -> Items.APPLE, () -> Items.BEETROOT),
    SUGAR("sugar", 0xFFF0EEE8, () -> Items.SUGAR),
    CHOPPED_SCALLION("chopped_scallion", 0xFF6CB03A, () -> HotPotContent.SCALLION),
    MINCED_CILANTRO("minced_cilantro", 0xFF4E9A2C, () -> HotPotContent.CILANTRO),
    GARLIC_PASTE("garlic_paste", 0xFFE8D696, () -> HotPotContent.GARLIC_PASTE),   // 蒜泥（2026-10-25 用户加，第 10 种）
    // mryt233 改（2026-11-06）：r137 新增 4 种蘸料，共 14 种。蚝油（水下呼吸）、芝麻酱（伤害吸收）、
    // 花生碎（跳跃提升）、小米辣（力量）；原料都是 r137 新加的蘸料原料，1 个原料照旧 4 份。
    OYSTER_SAUCE("oyster_sauce", 0xFF5A321E, () -> HotPotContent.OYSTER_SAUCE),
    SESAME_PASTE("sesame_paste", 0xFFC89E64, () -> HotPotContent.SESAME_PASTE),
    CRUSHED_PEANUTS("crushed_peanuts", 0xFFDAB46E, () -> HotPotContent.CRUSHED_PEANUTS),
    MILLET_CHILI("millet_chili", 0xFFE2281C, () -> HotPotContent.MILLET_CHILI),
    // mryt233 改（2026-11-06，r139）：第 15 种调料——折耳根碎（西南吃法：折耳根剁碎拌进蘸水）。
    // 象征折耳根清热解毒：给 10 秒抗性提升 I（和蒜泥同一档，但时间更长），原料是折耳根碾成的碎末。
    MINCED_ZHE_ER_GEN("minced_zhe_er_gen", 0xFFD8D2BC, () -> HotPotContent.MINCED_ZHE_ER_GEN);

    public static final Seasoning[] ALL = values();

    /**
     * 原料 → 调料、每种调料的原料表：类加载时算一次（mryt233 改）。
     * 以前 ofRaw 每次新建 15 个 List + 15 条 Stream，raws() 也被补货界面的槽位判定和原料表渲染
     * （SauceStationScreen 每帧 drawGuide、SauceStationBlockEntity 的 changed / take）反复调用，
     * 纯查表的事不该在每帧、每次点击的路径上分配。
     */
    private static final Map<Item, Seasoning> RAW_INDEX = new HashMap<>();
    @SuppressWarnings("unchecked")
    private static final List<Item>[] RAW_LISTS = new List[ALL.length];

    static {
        for (Seasoning s : ALL) {
            List<Item> items = new ArrayList<>(s.raws.length);
            for (Supplier<Item> raw : s.raws) {
                items.add(raw.get());
            }
            RAW_LISTS[s.ordinal()] = List.copyOf(items);
            for (Item raw : items) {
                // 同一原料属于两种调料时取序号靠前的那个（和以前按 values() 顺序遍历取第一个一致）
                RAW_INDEX.putIfAbsent(raw, s);
            }
        }
    }

    private final String id;
    private final Supplier<Item>[] raws;
    private final int color;

    @SafeVarargs
    Seasoning(String id, int color, Supplier<Item>... raws) {
        this.id = id;
        this.raws = raws;
        this.color = color;
    }

    public String id() {
        return id;
    }

    /** 第一种原料（只要一种时用）。 */
    public Item raw() {
        return raws[0].get();
    }

    /** 所有能补这种调料的原料（类加载时算好的常量表，直接返回，不再每次新建）。mryt233 改 */
    public List<Item> raws() {
        return RAW_LISTS[ordinal()];
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
    public static void appendEffects(int mask, java.util.List<Component> lines) {
        for (Seasoning s : ALL) {
            if ((mask & s.bit()) != 0) {
                lines.add(Component.literal(" ").append(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.line"),
                    Component.translatable(s.nameKey()), Component.translatable(s.effectKey())))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }

    /** 这件东西是哪种调料的原料；不是返回 null（mryt233 改：一次 Map 查表，不再遍历 15 种调料）。 */
    public static Seasoning ofRaw(ItemStack stack) {
        return stack.isEmpty() ? null : RAW_INDEX.get(stack.getItem());
    }

    /**
     * 吃下蘸过这种调料的菜时的效果。
     * 时长、等级、味精多回的饥饿值都能在配置文件里改（见 Config）。
     */
    public void apply(LivingEntity user) {
        switch (this) {
            // 味精：提鲜，多回 2 点饥饿值（用户 2026-10-23 选的）
            case MSG -> {
                if (user instanceof Player player) {
                    int hunger = Config.seasoningHunger(id, 2);
                    if (hunger > 0) {
                        player.getFoodData().eat(hunger, 0);
                    }
                }
            }
            case CHILI_OIL -> addEffect(user, MobEffects.FIRE_RESISTANCE, 200, 0);
            case SWEET_SOUR_SAUCE -> addEffect(user, MobEffects.MOVEMENT_SPEED, 200, 0);
            case HONEY -> {   // 解辣、解毒
                user.removeEffect(MobEffects.HUNGER);
                user.removeEffect(MobEffects.POISON);
            }
            case SESAME_OIL -> addEffect(user, MobEffects.REGENERATION, 100, 0);
            case VINEGAR -> {   // 解反胃；2026-10-23 用户加：也解辣（去掉红汤的饥饿）
                user.removeEffect(MobEffects.CONFUSION);
                user.removeEffect(MobEffects.HUNGER);
            }
            case SUGAR -> addEffect(user, MobEffects.DIG_SPEED, 200, 0);
            // 蒜泥：抗性提升 I 8 秒（「蒜能辟邪」，计划里的建议；新增）
            case GARLIC_PASTE -> addEffect(user, MobEffects.DAMAGE_RESISTANCE, 160, 0);
            // mryt233 改（2026-11-06）：r137 的 4 种新蘸料。
            case OYSTER_SAUCE -> addEffect(user, MobEffects.WATER_BREATHING, 200, 0);   // 蚝油：水下呼吸 10 秒
            case SESAME_PASTE -> addEffect(user, MobEffects.ABSORPTION, 200, 0);        // 芝麻酱：伤害吸收 I 10 秒
            case CRUSHED_PEANUTS -> addEffect(user, MobEffects.JUMP, 200, 0);           // 花生碎：跳跃提升 I 10 秒
            case MILLET_CHILI -> addEffect(user, MobEffects.DAMAGE_BOOST, 160, 0);      // 小米辣：力量 I 8 秒
            // mryt233 改（2026-11-06，r139）：折耳根碎——清热解毒，抗性提升 I 10 秒；顺手解掉中毒（毒也是「热毒」）。
            case MINCED_ZHE_ER_GEN -> {
                addEffect(user, MobEffects.DAMAGE_RESISTANCE, 200, 0);
                user.removeEffect(MobEffects.POISON);
            }
            case CHOPPED_SCALLION, MINCED_CILANTRO -> {
            }
        }
    }

    /** 按配置给状态效果；时长配成 0 就不加。 */
    private void addEffect(LivingEntity user,
                           net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                           int defaultTicks, int defaultAmplifier) {
        int ticks = Config.seasoningTicks(id, defaultTicks);
        if (ticks <= 0) {
            return;
        }
        user.addEffect(new MobEffectInstance(effect, ticks,
            Config.seasoningAmplifier(id, defaultAmplifier)));
    }

    static {
        // 默认值记进配置模板：duration / amplifier / hunger
        Config.rememberSeasoning("msg", 0, 0, 2);
        Config.rememberSeasoning("chili_oil", 200, 0, 0);
        Config.rememberSeasoning("sweet_sour_sauce", 200, 0, 0);
        Config.rememberSeasoning("honey", 0, 0, 0);
        Config.rememberSeasoning("sesame_oil", 100, 0, 0);
        Config.rememberSeasoning("vinegar", 0, 0, 0);
        Config.rememberSeasoning("sugar", 200, 0, 0);
        Config.rememberSeasoning("chopped_scallion", 0, 0, 0);
        Config.rememberSeasoning("minced_cilantro", 0, 0, 0);
        Config.rememberSeasoning("garlic_paste", 160, 0, 0);
        // mryt233 改（2026-11-06）：r137 的 4 种新蘸料的默认时长 / 等级 / 多回饥饿值。
        Config.rememberSeasoning("oyster_sauce", 200, 0, 0);
        Config.rememberSeasoning("sesame_paste", 200, 0, 0);
        Config.rememberSeasoning("crushed_peanuts", 200, 0, 0);
        Config.rememberSeasoning("millet_chili", 160, 0, 0);
        // mryt233 改（2026-11-06，r139）：折耳根碎（第 15 种）的默认时长 / 等级 / 多回饥饿值。
        Config.rememberSeasoning("minced_zhe_er_gen", 200, 0, 0);
    }

    /** mask 里的每种调料都生效一次。 */
    public static void applyAll(int mask, LivingEntity user) {
        for (Seasoning s : ALL) {
            if ((mask & s.bit()) != 0) {
                s.apply(user);
            }
        }
    }

    /** mask 里几种调料的平均颜色（ARGB）；空的返回 0。 */
    public static int mixColor(int mask) {
        int r = 0, g = 0, b = 0, n = 0;
        for (Seasoning s : ALL) {
            if ((mask & s.bit()) != 0) {
                r += (s.color >> 16) & 255;
                g += (s.color >> 8) & 255;
                b += s.color & 255;
                n++;
            }
        }
        return n == 0 ? 0 : 0xFF000000 | (r / n) << 16 | (g / n) << 8 | (b / n);
    }
}
