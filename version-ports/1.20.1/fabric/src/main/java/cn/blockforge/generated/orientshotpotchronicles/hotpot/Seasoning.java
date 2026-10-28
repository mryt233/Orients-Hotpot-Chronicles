package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.function.Supplier;

/**
 * 蘸料台上的 9 种调料（r103，用户定）。顺序 = 蘸料台小碗的顺序（阶梯式：从最上层到最下层、每层从左到右）= 补货界面的格子顺序。
 * 每种由一种原料补货（1 个原料 = 4 份）；蘸过的菜吃下去时，碗里有几种就叠几种效果（用户：全部叠加）。
 * 葱花、香菜碎末只提味，没有效果（用户）。
 * 2026-10-23 用户改（）：麻酱换成味精（原料小麦，提鲜：多回 2 点饥饿值）；醋的原料改成苹果或甜菜根（一种调料可以有几种原料）。
 *
 */
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
    GARLIC_PASTE("garlic_paste", 0xFFE8D696, () -> HotPotContent.GARLIC_PASTE);   // 蒜泥（2026-10-25 用户加，第 10 种）

    public static final Seasoning[] ALL = values();

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

    /** 所有能补这种调料的原料。 */
    public List<Item> raws() {
        return Arrays.stream(raws).map(Supplier::get).toList();
    }

    /** 界面上空格子轮流显示的原料（有几种就每秒换一种）。 */
    public Item rawShown(long millis) {
        return raws[(int) (millis / 1000 % raws.length)].get();
    }

    /** 原料名字，几种用「 / 」连起来。 */
    public Text rawNames() {
        MutableText out = Text.empty();
        for (int i = 0; i < raws.length; i++) {
            if (i > 0) {
                out.append(" / ");
            }
            out.append(new ItemStack(raws[i].get()).getName());
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

    /** 效果说明（物品提示里用，和 apply 对应； 2026-10-25）。 */
    public String effectKey() {
        return HotPotBlockEntity.key("seasoning." + id + ".effect");
    }

    /** 「 辣油：抗火 10 秒」这样一行灰字：蘸料碗、蘸过的菜的提示共用。 */
    public static void appendEffects(int mask, java.util.List<Text> lines) {
        for (Seasoning s : ALL) {
            if ((mask & s.bit()) != 0) {
                lines.add(Text.literal(" ").append(Text.translatable(HotPotBlockEntity.key("tooltip.sauce.line"),
                    Text.translatable(s.nameKey()), Text.translatable(s.effectKey())))
                    .formatted(net.minecraft.util.Formatting.GRAY));
            }
        }
    }

    /** 这件东西是哪种调料的原料；不是返回 null。 */
    public static Seasoning ofRaw(ItemStack stack) {
        for (Seasoning s : ALL) {
            if (s.raws().stream().anyMatch(stack::isOf)) {
                return s;
            }
        }
        return null;
    }

    /** 吃下蘸过这种调料的菜时的效果。 */
    public void apply(LivingEntity user) {
        switch (this) {
            // 味精：提鲜，多回 2 点饥饿值（用户 2026-10-23 选的）
            case MSG -> {
                if (user instanceof PlayerEntity player) {
                    player.getHungerManager().add(2, 0);
                }
            }
            case CHILI_OIL -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0));
            case SWEET_SOUR_SAUCE -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 0));
            case HONEY -> {   // 解辣、解毒
                user.removeStatusEffect(StatusEffects.HUNGER);
                user.removeStatusEffect(StatusEffects.POISON);
            }
            case SESAME_OIL -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
            case VINEGAR -> {   // 解反胃；2026-10-23 用户加：也解辣（去掉红汤的饥饿）
                user.removeStatusEffect(StatusEffects.NAUSEA);
                user.removeStatusEffect(StatusEffects.HUNGER);
            }
            case SUGAR -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 200, 0));
            // 蒜泥：抗性提升 I 8 秒（「蒜能辟邪」，计划里的建议；）
            case GARLIC_PASTE -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 0));
            case CHOPPED_SCALLION, MINCED_CILANTRO -> {
            }
        }
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
