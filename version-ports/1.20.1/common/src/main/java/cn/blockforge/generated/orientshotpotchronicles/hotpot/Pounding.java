package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.compat.CustomModelData;
import cn.blockforge.orient.framework.compat.DataComponents;
import cn.blockforge.orient.framework.compat.ItemData;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 用擀面杖（原叫木棒，2026-10-23 用户改）在砧板上捣丸子（2026-10-23 用户定，方案 A）：生牛肉 / 生猪排 / 生羊肉 / 生鳕鱼 / 生鲑鱼放砧板上，擀面杖捣三下：
 * 第 1 下捶碎、第 2 下成肉浆（砧板上的样子跟着变，用户要求），第 3 下掉出 3 个生丸子。
 * 进度记在 CuttingBoardBlockEntity 的 pounds 里（mryt233 改（2026-11-07）：和「切了几刀」分开记账，
 * 折耳根这类既能切又能捣的食材敲一下之后照样能切）。阶段贴图 textures/block/pounding/&lt;meat&gt;_&lt;minced|paste&gt;。
 * mryt233 改（2026-11-06，r137）：大蒜之后又加了花生，擀面杖敲三下出 2 份花生碎（蘸料原料，不用生肉贴图的中间阶段）。
 * mryt233 改（2026-11-11）：原 SPECS 表迁到数据包 pounding 配方。
 * 作者：mryt233
 */
public final class Pounding {
    public static final int HITS = 3;
    public static final int RESULT_COUNT = 3;

    private Pounding() { }

    /** mryt233 改（2026-11-11）：原料、产物、数量和中间样子统一读当前世界配方。 */
    public static PoundingRecipe recipe(Level level, ItemStack stack) {
        return HotPotRecipes.pounding(level, stack);
    }

    /** 旧入口保留；新调用方应显式传世界。mryt233 改（2026-11-11）。 */
    @Deprecated
    public static String meatOf(ItemStack stack) {
        PoundingRecipe recipe = recipe(HotPotRecipes.currentLevel(), stack);
        if (recipe == null) return null;
        int model = ItemData.getOrDefault(recipe.stage1(), DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0)).value();
        return model >= 1 && model <= 8 ? List.of("beef", "pork", "mutton", "fish").get((model - 1) / 2) : null;
    }

    /** 大蒜也能敲：敲三下出 2 份蒜泥（用户 2026-10-25：大蒜 + 蒜泥，用擀面杖敲）。 */
    // mryt233 改（2026-11-10）：内部已无调用，公开入口保留兼容。
    @Deprecated
    public static boolean isGarlic(ItemStack stack) {
        return stack.is(HotPotContent.GARLIC);
    }

    /** 虾仁拍三下成一颗虾滑（用户 2026-10-26：虾滑用擀面杖拍出来）。 */
    // mryt233 改（2026-11-10）：内部已无调用，公开入口保留兼容。
    @Deprecated
    public static boolean isPeeledShrimp(ItemStack stack) {
        return stack.is(HotPotContent.PEELED_SHRIMP);
    }

    /** mryt233 改（2026-11-06，r137）：花生也能敲，敲三下出 2 份花生碎（蘸料台「花生碎」的原料）。 */
    // mryt233 改（2026-11-10）：内部已无调用，公开入口保留兼容。
    @Deprecated
    public static boolean isPeanut(ItemStack stack) {
        return stack.is(HotPotContent.PEANUT);
    }

    /** mryt233 改（2026-11-06，r139）：折耳根也能敲，敲三下出 2 份折耳根碎（蘸料台第 15 种调料的原料）。 */
    // mryt233 改（2026-11-10）：内部已无调用，公开入口保留兼容。
    @Deprecated
    public static boolean isZheErGen(ItemStack stack) {
        return stack.is(HotPotContent.ZHE_ER_GEN);
    }

    /** 兼容旧无世界参数入口，当前环境配方为空时返回不能捣。 */
    public static boolean poundable(ItemStack stack) { return poundable(HotPotRecipes.currentLevel(), stack); }
    public static boolean poundable(Level level, ItemStack stack) {
        return recipe(level, stack) != null;
    }
    public static int resultCount(ItemStack stack) {
        PoundingRecipe recipe = recipe(HotPotRecipes.currentLevel(), stack);
        return recipe == null ? 0 : recipe.result().getCount();
    }
    public static Item result(ItemStack stack) {
        PoundingRecipe recipe = recipe(HotPotRecipes.currentLevel(), stack);
        return recipe == null ? null : recipe.result().getItem();
    }
    public static ItemStack display(ItemStack stack, int hits) {
        return display(HotPotRecipes.currentLevel(), stack, hits);
    }
    public static ItemStack display(Level level, ItemStack stack, int hits) {
        PoundingRecipe recipe = recipe(level, stack);
        return recipe == null ? stack : recipe.display(stack, hits);
    }
}
