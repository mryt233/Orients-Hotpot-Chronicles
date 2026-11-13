package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * 用擀面杖在砧板上捣丸子 / 蒜泥 / 花生碎 / 折耳根碎 / 虾滑，原料与产物统一读数据包 {@link PoundingRecipe}。
 *
 * <p>mryt233 改（2026-11-13）：26.1 {@link CustomModelData} 改用 {@code EMPTY} 与 {@code getFloat(0)}。</p>
 * 作者：mryt233
 */
public final class Pounding {
    /** 捣几下才算捣好。 */
    public static final int HITS = 3;
    /** 生肉捣完掉几个。 */
    public static final int RESULT_COUNT = 3;

    private Pounding() { }

    public static PoundingRecipe recipe(Level level, ItemStack stack) {
        return HotPotRecipes.pounding(level, stack);
    }

    @Deprecated
    public static String meatOf(ItemStack stack) {
        PoundingRecipe recipe = recipe(HotPotRecipes.currentLevel(), stack);
        if (recipe == null) return null;
        Float raw = recipe.stage1().getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY).getFloat(0);
        int model = raw == null ? 0 : Math.round(raw);
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

    public static boolean poundable(ItemStack stack) { return poundable(HotPotRecipes.currentLevel(), stack); }
    public static boolean poundable(Level level, ItemStack stack) { return recipe(level, stack) != null; }

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
