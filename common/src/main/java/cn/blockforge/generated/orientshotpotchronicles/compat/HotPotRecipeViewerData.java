package cn.blockforge.generated.orientshotpotchronicles.compat;

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
 * 以及三类玩法各自的工作台（催化剂）列表。
 * <p>
 * 这里只是一张「可枚举的配方表」，不 import 任何 {@code net.minecraft.client.*}、JEI 或 REI 的类型，
 * 也不依赖加载器：两个平台的插件各自把这里的条目翻译成自己的展示格式，核心层不感知配方查看器是否存在。
 * <p>
 * 火锅涮煮的刻数与经验<b>每次调用都现读</b> {@link Config}，所以改完配置执行 {@code /orientshotpot reload}
 * 之后，再次枚举本数据源即可取得新数值；界面重建时机由查看器负责。
 * 作者：mryt233 */
public final class HotPotRecipeViewerData {
    private HotPotRecipeViewerData() {
    }

    // ------------------------------------------------------------------ 数据结构

    /**
     * 一条火锅涮煮配方。
     *
     * @param id                配方标识（{@code orients_hotpot_chronicles:hotpot_cooking/&lt;食材&gt;}）
     * @param input             下锅的生食材
     * @param output            煮熟后的产物
     * @param cookTicks         煮熟需要的刻数（20 tick = 1 秒），每次枚举时现读配置
     * @param xp                煮熟一份给出的经验
     * @param overcookOutput    煮熟后继续久煮会碎成的产物；不会煮老时为 {@link ItemStack#EMPTY}
     * @param overcookTotalTicks 从下锅算起、煮到碎掉的总刻数（含煮熟那一段）；不会煮老时为 0
     */
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

    /**
     * 一条砧板切配配方。
     *
     * @param id            配方标识（{@code orients_hotpot_chronicles:cutting_board/&lt;食材&gt;}）
     * @param input         放上砧板的食材
     * @param tool          切配用的工具（铁菜刀）
     * @param output        切出来的产物（数量就是一刀 / 切完的总份数）
     * @param pieceByPiece  是否分刀切：true 表示每刀掉 1 份、切满 {@code output.getCount()} 刀才切完（土豆切 3 下）
     */
    public record CuttingEntry(
        ResourceLocation id,
        ItemStack input,
        ItemStack tool,
        ItemStack output,
        boolean pieceByPiece
    ) {
    }

    /**
     * 一条擀面杖拍打配方。
     *
     * @param id            配方标识（{@code orients_hotpot_chronicles:pounding/&lt;食材&gt;}）
     * @param input         放上砧板的食材
     * @param tool          拍打用的工具（擀面杖）
     * @param stage1Display 拍第 1 下以后砧板上的样子
     * @param stage2Display 拍第 2 下以后砧板上的样子（已经是肉浆 / 蒜泥 / 虾滑）
     * @param output        拍完掉的产物
     * @param hits          拍几下才出产物
     */
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
    public static List<CookingEntry> cookingRecipes(net.minecraft.world.level.Level level) {
        if (level == null) return List.of();
        List<CookingEntry> entries = new ArrayList<>();
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack input = new ItemStack(item);
            if (!HotPotCooking.acceptable(level, input)) continue;
            boolean custom = HotPotRecipes.boiling(level, input) != null;
            boolean vanilla = HotPotCooking.smeltingRecipe(level, input).isPresent();
            if (!custom && !vanilla) continue;
            ItemStack output = HotPotCooking.finish(level, input, cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.CLEAR);
            ItemStack over = HotPotCooking.overcookResult(level, output.getItem());
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation id = HotPot.id("hotpot_cooking/" + (key.getNamespace().equals(HotPot.MOD_ID)
                || key.getNamespace().equals("minecraft") ? key.getPath() : key.getNamespace() + "/" + key.getPath()));
            entries.add(new CookingEntry(id, input, output, HotPotCooking.cookTicks(level, input),
                HotPotCooking.xpFor(level, input), over.copy(),
                over.isEmpty() ? 0 : HotPotCooking.overcookTicks(level, output.getItem())));
        }
        entries.sort(java.util.Comparator.comparing(e -> e.id().toString()));
        return List.copyOf(entries);
    }

    // ------------------------------------------------------------------ 砧板切配

    /** 全部 13 条砧板切配配方（r137 前 9 条 + mryt233 改 2026-11-06 追加的 3 条 + r139 折耳根 1 条）；工具统一是铁菜刀。 */
    public static List<CuttingEntry> cuttingRecipes() {
        List<CuttingEntry> recipes = new ArrayList<>(13);
        recipes.add(cutting("beef", Items.BEEF, HotPotContent.FATTY_BEEF_ROLL, 3, false));
        recipes.add(cutting("mutton", Items.MUTTON, HotPotContent.MUTTON_ROLL, 3, false));
        // 土豆一刀掉 1 份，切三刀才切完（物品标签 cut_piece_by_piece）
        recipes.add(cutting("potato", Items.POTATO, HotPotContent.POTATO_SLICE, 3, true));
        recipes.add(cutting("porkchop", Items.PORKCHOP, HotPotContent.MINCED_PORK, 1, false));
        recipes.add(cutting("beef_loin", HotPotContent.BEEF_LOIN, HotPotContent.BEEF_LOIN_SLICE, 3, false));
        recipes.add(cutting("cold_tofu", HotPotContent.COLD_TOFU, HotPotContent.COLD_TOFU_CUBE, 3, false));
        recipes.add(cutting("hot_tofu", HotPotContent.HOT_TOFU, HotPotContent.HOT_TOFU_CUBE, 3, false));
        recipes.add(cutting("shrimp", HotPotContent.SHRIMP, HotPotContent.PEELED_SHRIMP, 1, false));
        recipes.add(cutting("ham_sausage", HotPotContent.HAM_SAUSAGE, HotPotContent.HAM_SAUSAGE_SLICE, 3, false));
        // r137：辣椒一刀出 2 份小米辣；莲藕和土豆一样切 3 刀、每刀 1 片藕片；生鱿鱼一刀切成 3 份鱿鱼卷。
        recipes.add(cutting("chili_pepper", HotPotContent.CHILI_PEPPER, HotPotContent.MILLET_CHILI, 2, false));
        recipes.add(cutting("lotus_root", HotPotContent.LOTUS_ROOT, HotPotContent.LOTUS_ROOT_SLICE, 3, true));
        recipes.add(cutting("raw_squid", HotPotContent.RAW_SQUID, HotPotContent.SQUID_ROLL, 3, false));
        // r139：折耳根整根一刀切成 3 段。
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
    public static List<PoundingEntry> poundingRecipes(net.minecraft.world.level.Level level) {
        List<PoundingEntry> entries = new ArrayList<>();
        for (var holder : HotPotRecipes.all(level, PoundingRecipe.TYPE)) {
            var recipe = holder.value();
            for (ItemStack input : recipe.ingredient().getItems()) {
                if (HotPotRecipes.pounding(level, input) != recipe) continue;
                var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem());
                ResourceLocation id = recipe.ingredient().getItems().length == 1 ? holder.id()
                    : ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(), holder.id().getPath()
                        + "/" + key.getNamespace() + "/" + key.getPath());
                entries.add(new PoundingEntry(id, input.copy(), new ItemStack(HotPotContent.ROLLING_PIN),
                    recipe.display(input, 1), recipe.display(input, 2), recipe.result(), recipe.hits()));
            }
        }
        return List.copyOf(entries);
    }

    // ------------------------------------------------------------------ 工作台（催化剂）

    /** 火锅涮煮的工作台：全部材质的火锅（按 HotPotMaterial 的顺序：九宫格 / 鸳鸯 / 田字…）、矮灶台、高灶台、筷子。 */
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

    /** 砧板切配的工作台：砧板 + 铁菜刀。 */
    public static List<ItemStack> cuttingCatalysts() {
        return List.of(new ItemStack(HotPotContent.CUTTING_BOARD_ITEM), new ItemStack(HotPotContent.KITCHEN_KNIFE));
    }

    /** 擀面杖拍打的工作台：砧板 + 擀面杖。 */
    public static List<ItemStack> poundingCatalysts() {
        return List.of(new ItemStack(HotPotContent.CUTTING_BOARD_ITEM), new ItemStack(HotPotContent.ROLLING_PIN));
    }

    // mryt233 改（2026-11-11）：两种查看器只共享文案与配色，布局仍各自负责。
    public static final int TEXT = 0xFF404040;
    public static final int TEXT_DARK = 0xFF303030;
    public static final int HINT = 0xFF8A5524;
    public static final int HINT_DARK = 0xFF6E421B;
    public static final int WARN = 0xFFA63D2A;
    public static final int WARN_DARK = 0xFF7C2A1C;

    public static String seconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20F);
    }

    public static net.minecraft.network.chat.MutableComponent recipeText(String suffix, Object... args) {
        return net.minecraft.network.chat.Component.translatable("text.orients_hotpot_chronicles.recipe." + suffix, args);
    }

    public static net.minecraft.network.chat.Component cookingTime(CookingEntry recipe) {
        return recipeText("cooking.time", seconds(recipe.cookTicks()), String.valueOf(recipe.cookTicks()));
    }

    public static net.minecraft.network.chat.Component overcookHint(CookingEntry recipe) {
        int extra = Math.max(0, recipe.overcookTotalTicks() - recipe.cookTicks());
        return recipeText("cooking.overcook", seconds(extra), recipe.overcookOutput().getHoverName());
    }

    public static net.minecraft.network.chat.Component cuttingHint(CuttingEntry recipe) {
        return recipe.pieceByPiece() ? recipeText("cutting.multi", String.valueOf(recipe.output().getCount()))
            : recipeText("cutting.single");
    }

    public static net.minecraft.network.chat.Component poundingHint(PoundingEntry recipe) {
        return recipeText("pounding.hits", String.valueOf(recipe.hits()));
    }
}
