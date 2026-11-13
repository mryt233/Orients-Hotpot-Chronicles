package cn.blockforge.generated.orientshotpotchronicles.compat;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import java.util.Comparator;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotRecipes;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PoundingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方查看器（JEI / REI）共用的纯数据源：火锅涮煮、砧板切配、擀面杖拍打三类玩法配方，
 * 以及三类玩法各自的工作台（催化剂）列表（mryt233 改：1.20.1 适配）。
 * 作者：mryt233 */
public final class HotPotRecipeViewerData {
    private HotPotRecipeViewerData() {
    }

    public record CookingEntry(
        ResourceLocation id,
        ItemStack input,
        ItemStack output,
        int cookTicks,
        float xp,
        ItemStack overcookOutput,
        int overcookTotalTicks
    ) {
    }

    public record CuttingEntry(
        ResourceLocation id,
        ItemStack input,
        ItemStack tool,
        ItemStack output,
        boolean pieceByPiece
    ) {
    }

    public record PoundingEntry(
        ResourceLocation id,
        ItemStack input,
        ItemStack tool,
        ItemStack stage1Display,
        ItemStack stage2Display,
        ItemStack output,
        int hits
    ) {
    }

    // ------------------------------------------------------------------ 火锅涮煮

    /** 旧无参数入口保留；客户端插件与开发导出显式传当前世界。 */
    public static List<CookingEntry> cookingRecipes() { return cookingRecipes(HotPotRecipes.currentLevel()); }

    /** mryt233 改（2026-11-11）：展示与实际下锅查询共用配置、产物与经验规则，不再手抄食材表。 */
    public static List<CookingEntry> cookingRecipes(Level level) {
        if (level == null) return List.of();
        List<CookingEntry> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack input = new ItemStack(item);
            if (!HotPotCooking.acceptable(level, input)) continue;
            boolean custom = HotPotRecipes.boiling(level, input) != null;
            boolean vanilla = HotPotCooking.smeltingRecipe(level, input).isPresent();
            if (!custom && !vanilla) continue;
            ItemStack output = HotPotCooking.finishWithSoup(level, input, Soups.clear());
            ItemStack over = HotPotCooking.overcookResult(level, output.getItem());
            var key = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation id = HotPot.id("hotpot_cooking/" + (key.getNamespace().equals(HotPot.MOD_ID)
                || key.getNamespace().equals("minecraft") ? key.getPath() : key.getNamespace() + "/" + key.getPath()));
            entries.add(new CookingEntry(id, input, output, HotPotCooking.cookTicks(level, input),
                HotPotCooking.xpFor(level, input), over.copy(),
                over.isEmpty() ? 0 : HotPotCooking.overcookTicks(level, output.getItem())));
        }
        entries.sort(Comparator.comparing(e -> e.id().toString()));
        return List.copyOf(entries);
    }

    // ------------------------------------------------------------------ 砧板切配

    /** 全部 13 条砧板切配配方；工具统一是铁菜刀。 */
    public static List<CuttingEntry> cuttingRecipes() {
        List<CuttingEntry> recipes = new ArrayList<>(13);
        recipes.add(cutting("beef", Items.BEEF, HotPotContent.FATTY_BEEF_ROLL, 3, false));
        recipes.add(cutting("mutton", Items.MUTTON, HotPotContent.MUTTON_ROLL, 3, false));
        recipes.add(cutting("potato", Items.POTATO, HotPotContent.POTATO_SLICE, 3, true));
        recipes.add(cutting("porkchop", Items.PORKCHOP, HotPotContent.MINCED_PORK, 1, false));
        recipes.add(cutting("beef_loin", HotPotContent.BEEF_LOIN, HotPotContent.BEEF_LOIN_SLICE, 3, false));
        recipes.add(cutting("cold_tofu", HotPotContent.COLD_TOFU, HotPotContent.COLD_TOFU_CUBE, 3, false));
        recipes.add(cutting("hot_tofu", HotPotContent.HOT_TOFU, HotPotContent.HOT_TOFU_CUBE, 3, false));
        recipes.add(cutting("shrimp", HotPotContent.SHRIMP, HotPotContent.PEELED_SHRIMP, 1, false));
        recipes.add(cutting("ham_sausage", HotPotContent.HAM_SAUSAGE, HotPotContent.HAM_SAUSAGE_SLICE, 3, false));
        recipes.add(cutting("chili_pepper", HotPotContent.CHILI_PEPPER, HotPotContent.MILLET_CHILI, 2, false));
        recipes.add(cutting("lotus_root", HotPotContent.LOTUS_ROOT, HotPotContent.LOTUS_ROOT_SLICE, 3, true));
        recipes.add(cutting("raw_squid", HotPotContent.RAW_SQUID, HotPotContent.SQUID_ROLL, 3, false));
        recipes.add(cutting("zhe_er_gen", HotPotContent.ZHE_ER_GEN, HotPotContent.ZHE_ER_GEN_SLICE, 3, false));
        return List.copyOf(recipes);
    }

    private static CuttingEntry cutting(String path, Item raw, Item output, int count, boolean pieceByPiece) {
        return new CuttingEntry(
            HotPot.id("cutting_board/" + path),
            new ItemStack(raw),
            new ItemStack(HotPotContent.KITCHEN_KNIFE),
            new ItemStack(output, count),
            pieceByPiece);
    }

    // ------------------------------------------------------------------ 擀面杖拍打

    public static List<PoundingEntry> poundingRecipes() { return poundingRecipes(HotPotRecipes.currentLevel()); }

    /** mryt233 改（2026-11-11）：按同步到客户端的捣制配方枚举，标签原料逐个展开。 */
    public static List<PoundingEntry> poundingRecipes(Level level) {
        List<PoundingEntry> entries = new ArrayList<>();
        for (PoundingRecipe recipe : HotPotRecipes.all(level, PoundingRecipe.TYPE)) {
            ResourceLocation recipeId = recipe.getId();
            for (ItemStack input : recipe.ingredient().getItems()) {
                if (HotPotRecipes.pounding(level, input) != recipe) continue;
                var key = BuiltInRegistries.ITEM.getKey(input.getItem());
                ResourceLocation id = recipe.ingredient().getItems().length == 1 ? recipeId
                    : new ResourceLocation(recipeId.getNamespace(), recipeId.getPath()
                        + "/" + key.getNamespace() + "/" + key.getPath());
                entries.add(new PoundingEntry(id, input.copy(), new ItemStack(HotPotContent.ROLLING_PIN),
                    recipe.display(input, 1), recipe.display(input, 2), recipe.result(), recipe.hits()));
            }
        }
        return List.copyOf(entries);
    }

    // ------------------------------------------------------------------ 工作台（催化剂）

    public static List<ItemStack> cookingCatalysts() {
        List<ItemStack> catalysts = new ArrayList<>();
        for (HotPotMaterial material : HotPotMaterial.values()) {
            catalysts.add(new ItemStack(HotPotContent.itemFor(material)));
        }
        catalysts.add(new ItemStack(HotPotContent.STOVE_ITEM));
        catalysts.add(new ItemStack(HotPotContent.TALL_STOVE_ITEM));
        catalysts.add(new ItemStack(HotPotContent.CHOPSTICKS));
        return List.copyOf(catalysts);
    }

    public static List<ItemStack> cuttingCatalysts() {
        return List.of(new ItemStack(HotPotContent.CUTTING_BOARD_ITEM), new ItemStack(HotPotContent.KITCHEN_KNIFE));
    }

    public static List<ItemStack> poundingCatalysts() {
        return List.of(new ItemStack(HotPotContent.CUTTING_BOARD_ITEM), new ItemStack(HotPotContent.ROLLING_PIN));
    }

    public static final int TEXT = 0xFF404040;
    public static final int TEXT_DARK = 0xFF303030;
    public static final int HINT = 0xFF8A5524;
    public static final int HINT_DARK = 0xFF6E421B;
    public static final int WARN = 0xFFA63D2A;
    public static final int WARN_DARK = 0xFF7C2A1C;

    public static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20F);
    }

    public static MutableComponent recipeText(String suffix, Object... args) {
        return Component.translatable("text.orients_hotpot_chronicles.recipe." + suffix, args);
    }

    public static Component cookingTime(CookingEntry recipe) {
        return recipeText("cooking.time", seconds(recipe.cookTicks()), String.valueOf(recipe.cookTicks()));
    }

    public static Component overcookHint(CookingEntry recipe) {
        int extra = Math.max(0, recipe.overcookTotalTicks() - recipe.cookTicks());
        return recipeText("cooking.overcook", seconds(extra), recipe.overcookOutput().getHoverName());
    }

    public static Component cuttingHint(CuttingEntry recipe) {
        return recipe.pieceByPiece() ? recipeText("cutting.multi", String.valueOf(recipe.output().getCount()))
            : recipeText("cutting.single");
    }

    public static Component poundingHint(PoundingEntry recipe) {
        return recipeText("pounding.hits", String.valueOf(recipe.hits()));
    }
}
