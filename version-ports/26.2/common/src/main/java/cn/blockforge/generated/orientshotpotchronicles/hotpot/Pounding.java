// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * 用擀面杖在砧板上捣制食材：阶段、产物和中间样子统一读当前世界 {@link PoundingRecipe}。
 */
public final class Pounding {
    /** 捣几下才算捣好（捶碎 -> 肉浆 -> 掉出成品）。 */
    public static final int HITS = 3;
    /** 生肉捣完默认掉几个。 */
    public static final int RESULT_COUNT = 3;

    private Pounding() {
    }

    /** mryt233 改（2026-11-11）：原料、产物、数量和中间样子统一读当前世界配方。 */
    public static PoundingRecipe recipe(Level level, ItemStack stack) {
        return HotPotRecipes.pounding(level, stack);
    }

    /** 旧入口保留；新调用方应显式传世界。mryt233 改（2026-11-11）。 */
    @Deprecated
    public static String meatOf(ItemStack stack) {
        PoundingRecipe recipe = recipe(HotPotRecipes.currentLevel(), stack);
        if (recipe == null) {
            return null;
        }
        CustomModelData cmd = recipe.stage1().getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        int model = cmd.floats().isEmpty() ? 0 : Math.round(cmd.floats().get(0));
        return model >= 1 && model <= 8 ? List.of("beef", "pork", "mutton", "fish").get((model - 1) / 2) : null;
    }

    @Deprecated
    public static boolean isGarlic(ItemStack stack) {
        return stack.is(HotPotContent.GARLIC);
    }

    @Deprecated
    public static boolean isPeeledShrimp(ItemStack stack) {
        return stack.is(HotPotContent.PEELED_SHRIMP);
    }

    @Deprecated
    public static boolean isPeanut(ItemStack stack) {
        return stack.is(HotPotContent.PEANUT);
    }

    @Deprecated
    public static boolean isZheErGen(ItemStack stack) {
        return stack.is(HotPotContent.ZHE_ER_GEN);
    }

    /** 兼容旧无世界参数入口，当前环境配方为空时返回不能捣。 */
    public static boolean poundable(ItemStack stack) {
        return poundable(HotPotRecipes.currentLevel(), stack);
    }

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
