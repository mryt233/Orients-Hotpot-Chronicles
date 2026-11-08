package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.function.Predicate;

/**
 * 用擀面杖（原叫木棒，2026-10-23 用户改）在砧板上捣丸子（2026-10-23 用户定，方案 A）：生牛肉 / 生猪排 / 生羊肉 / 生鳕鱼 / 生鲑鱼放砧板上，擀面杖捣三下：
 * 第 1 下捶碎、第 2 下成肉浆（砧板上的样子跟着变，用户要求），第 3 下掉出 3 个生丸子。
 * 进度记在 CuttingBoardBlockEntity 的 pounds 里（mryt233 改（2026-11-07）：和「切了几刀」分开记账，
 * 折耳根这类既能切又能捣的食材敲一下之后照样能切）。阶段贴图 textures/block/pounding/&lt;meat&gt;_&lt;minced|paste&gt;。
 * mryt233 改（2026-11-06，r137）：大蒜之后又加了花生，擀面杖敲三下出 2 份花生碎（蘸料原料，不用生肉贴图的中间阶段）。
 * mryt233 改（2026-11-07）：能捣的食材收成一张 {@link #SPECS} 表，加一样新食材只改表里一行。
 * 作者：mryt233 */
public final class Pounding {
    /** 捣几下才算捣好（捶碎 → 肉浆 → 掉出成品）。 */
    public static final int HITS = 3;
    /** 生肉捣完掉几个（丸子 3 个；蒜泥 / 花生碎 / 折耳根碎 / 虾滑各自写在表里）。 */
    public static final int RESULT_COUNT = 3;

    /**
     * 一样能捣的东西：怎么认、敲完出什么、敲出几个、敲到一半板上的样子。
     * mryt233 改（2026-11-07）：以前这四件事在 poundable / resultCount / display / result 里各写一遍 if 链，
     * 加一样新食材要改五个地方还容易漏，现在只认 {@link #SPECS} 这一张表。
     * @param matches  认它的条件
     * @param out      敲完掉出来的东西
     * @param count    敲完掉几个
     * @param midStage 敲到第二下起板上的样子（null = 没有单独中间样子，按原样或生肉贴图画）
     * @param meatName 生肉贴图的名字（beef / pork / mutton / fish；不是生肉就是 null）
     */
    private record Spec(Predicate<ItemStack> matches, Item out, int count, Item midStage, String meatName) {
        /** 砧板上捣了 hits 下的样子：生肉第 1 下捶碎、第 2 下起肉浆，其他食材第 2 下起换成中间样子。 */
        ItemStack display(ItemStack in, int hits) {
            if (midStage != null && hits >= 2) {
                return new ItemStack(midStage);
            }
            if (meatName == null || hits <= 0) {
                return in;
            }
            ItemStack pounded = new ItemStack(HotPotContent.POUNDED_MEAT);
            pounded.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(java.util.List.of((float) (MEAT_ORDER.indexOf(meatName) * 2 + (hits == 1 ? 1 : 2))), java.util.List.of(), java.util.List.of(), java.util.List.of()));
            return pounded;
        }
    }

    /** 生肉贴图用自定义模型数据的顺序（beef / pork / mutton / fish）。 */
    private static final List<String> MEAT_ORDER = List.of("beef", "pork", "mutton", "fish");

    /** 全部能捣的东西：加新食材只往这张表里加一行，别再动下面几个方法。 */
    private static final List<Spec> SPECS = List.of(
        new Spec(s -> s.is(Items.BEEF), HotPotContent.BEEF_MEATBALL, RESULT_COUNT, null, "beef"),
        new Spec(s -> s.is(Items.PORKCHOP), HotPotContent.PORK_MEATBALL, RESULT_COUNT, null, "pork"),
        new Spec(s -> s.is(Items.MUTTON), HotPotContent.MUTTON_MEATBALL, RESULT_COUNT, null, "mutton"),
        new Spec(s -> s.is(Items.COD) || s.is(Items.SALMON), HotPotContent.FISH_BALL, RESULT_COUNT, null, "fish"),
        // 大蒜（用户 2026-10-25）：敲三下出 2 份蒜泥，敲一下还是蒜头，第二下起是一摊蒜泥
        new Spec(s -> s.is(HotPotContent.GARLIC), HotPotContent.GARLIC_PASTE, 2, HotPotContent.GARLIC_PASTE, null),
        // 虾仁（用户 2026-10-26）：拍三下成一颗虾滑，拍一下还是虾仁，第二下起是一团虾滑
        new Spec(s -> s.is(HotPotContent.PEELED_SHRIMP), HotPotContent.SHRIMP_PASTE, 1, HotPotContent.SHRIMP_PASTE, null),
        // 花生（mryt233 改（2026-11-06，r137））：敲三下出 2 份花生碎，第二下起是一小堆花生碎
        new Spec(s -> s.is(HotPotContent.PEANUT), HotPotContent.CRUSHED_PEANUTS, 2, HotPotContent.CRUSHED_PEANUTS, null),
        // 折耳根（mryt233 改（2026-11-06，r139））：敲三下出 2 份折耳根碎，第二下起是一小撮碎末
        new Spec(s -> s.is(HotPotContent.ZHE_ER_GEN), HotPotContent.MINCED_ZHE_ER_GEN, 2,
            HotPotContent.MINCED_ZHE_ER_GEN, null)
        // ==== tools/new_item.py：新的可捣食材插在这行上面（别删这行）====
    );

    private Pounding() {
    }

    /** 这件东西捣起来是哪一条规则；不能捣返回 null。 */
    private static Spec specOf(ItemStack stack) {
        for (Spec spec : SPECS) {
            if (spec.matches().test(stack)) {
                return spec;
            }
        }
        return null;
    }

    /** 生肉 → 阶段贴图用的名字（beef / pork / mutton / fish）；不是生肉返回 null。 */
    public static String meatOf(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec == null ? null : spec.meatName();
    }

    /** 大蒜也能敲：敲三下出 2 份蒜泥（用户 2026-10-25：大蒜 + 蒜泥，用擀面杖敲）。 */
    public static boolean isGarlic(ItemStack stack) {
        return stack.is(HotPotContent.GARLIC);
    }

    /** 虾仁拍三下成一颗虾滑（用户 2026-10-26：虾滑用擀面杖拍出来）。 */
    public static boolean isPeeledShrimp(ItemStack stack) {
        return stack.is(HotPotContent.PEELED_SHRIMP);
    }

    /** mryt233 改（2026-11-06，r137）：花生也能敲，敲三下出 2 份花生碎（蘸料台「花生碎」的原料）。 */
    public static boolean isPeanut(ItemStack stack) {
        return stack.is(HotPotContent.PEANUT);
    }

    /** mryt233 改（2026-11-06，r139）：折耳根也能敲，敲三下出 2 份折耳根碎（蘸料台第 15 种调料的原料）。 */
    public static boolean isZheErGen(ItemStack stack) {
        return stack.is(HotPotContent.ZHE_ER_GEN);
    }

    /** 这件东西能不能用擀面杖捣。 */
    public static boolean poundable(ItemStack stack) {
        return specOf(stack) != null;
    }

    /** 敲完掉几个：丸子 3 个，蒜泥 2 份，花生碎 2 份，折耳根碎 2 份，虾滑 1 颗；不能捣返回 0。 */
    public static int resultCount(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec == null ? 0 : spec.count();
    }

    /**
     * 砧板上捣了 hits 下的样子：隐藏物品 pounded_meat + 自定义模型数据（1..8 = 牛 / 猪 / 羊 / 鱼 × 捶碎 / 肉浆，
     * 同 tools/gen_meatballs.py），按普通物品画就有厚度（2026-10-23 用户：要和原版食材放上去一样是立体的）。
     */
    public static ItemStack display(ItemStack meat, int hits) {
        Spec spec = specOf(meat);
        return spec == null ? meat : spec.display(meat, hits);
    }

    /** 这样东西捣出来的物品；不能捣返回 null（调用方已经在判空）。 */
    public static Item result(ItemStack stack) {
        Spec spec = specOf(stack);
        return spec == null ? null : spec.out();
    }
}
