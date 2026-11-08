package cn.blockforge.generated.orientshotpotchronicles.compat;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Pounding;
import net.minecraft.resources.Identifier;
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
 * 之后，在 JEI / REI 里重新打开的配方显示的就是新数值。
 * 作者：mryt233 */
public final class HotPotRecipeViewerData {
    /** 拍打配方的默认拍打次数（和砧板上的实际判定一致，见 {@link Pounding#HITS}）。 */
    private static final int POUNDING_HITS = Pounding.HITS;
    /** 火锅涮煮的默认经验（配置没有该项时使用）。 */
    private static final float DEFAULT_XP = 0.35F;

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
        Identifier id,
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
        Identifier id,
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
        Identifier id,
        ItemStack input,
        ItemStack tool,
        ItemStack stage1Display,
        ItemStack stage2Display,
        ItemStack output,
        int hits
    ) {
    }

    // ------------------------------------------------------------------ 火锅涮煮

    /**
     * 一条涮煮配方的「骨架」：只放不随配置变的 ItemStack，刻数 / 经验留到每次枚举时现读配置。
     *
     * <p>mryt233 改（2026-11-07）：以前每次调用都重新 new 三十个条目的全部 ItemStack（约 160 个），
     * 而 JEI / REI 每次打开配方页、切分类、搜索都会重新调一次。现在骨架只建一次，
     * 刻数与经验仍然现读 {@link Config}，reload 后配方查看器照样立刻显示新数值。</p>
     *
     * <p>注意：{@code input} / {@code output} 这些栈在输出时<b>必须 {@code copy()}</b>——
     * {@link ItemStack} 的 count 可变，直接返回共享栈会被配方查看器或别的调用方改坏。</p>
     *
     * @param configKey 配置里 {@code cooking.items.<configKey>} 的键；整块豆腐这类走专属键的为 null
     */
    private record CookingSkeleton(Identifier id, String configKey, ItemStack input, ItemStack output,
                                   ItemStack overcookOutput, int overcookTotalTicks, int defaultTicks) {
    }

    /** 30 条涮煮配方的骨架（与 {@code HotPotCooking#registerDefaultItemCookTicks} 的四档默认一致：50 / 100 / 150 / 200）。 */
    private static final List<CookingSkeleton> COOKING_SKELETONS = buildCookingSkeletons();

    private static List<CookingSkeleton> buildCookingSkeletons() {
        List<CookingSkeleton> list = new ArrayList<>(30);
        list.add(cooking("fatty_beef_roll", HotPotContent.FATTY_BEEF_ROLL, HotPotContent.COOKED_FATTY_BEEF_ROLL, 100));
        list.add(cooking("mutton_roll", HotPotContent.MUTTON_ROLL, HotPotContent.COOKED_MUTTON_ROLL, 100));
        list.add(cooking("beef_loin", HotPotContent.BEEF_LOIN, HotPotContent.COOKED_BEEF_LOIN, 200));
        list.add(cooking("beef_loin_slice", HotPotContent.BEEF_LOIN_SLICE, HotPotContent.COOKED_BEEF_LOIN_SLICE, 50));
        list.add(cooking("tripe", HotPotContent.RAW_TRIPE, HotPotContent.COOKED_TRIPE, 50));
        list.add(cooking("shrimp", HotPotContent.SHRIMP, HotPotContent.COOKED_SHRIMP, 150));
        list.add(cooking("peeled_shrimp", HotPotContent.PEELED_SHRIMP, HotPotContent.COOKED_PEELED_SHRIMP, 100));
        list.add(cooking("shrimp_paste", HotPotContent.SHRIMP_PASTE, HotPotContent.COOKED_SHRIMP_PASTE, 150));
        list.add(cooking("potato_slice", HotPotContent.POTATO_SLICE, HotPotContent.COOKED_POTATO_SLICE, 100));
        // 整块冷豆腐：刻数走 cooking.tofu_cook_ticks（默认 10 秒），经验用兜底值；
        // 煮熟以后继续久煮会碎成 2 块热豆腐块（cooking.tofu_overcook_ticks，默认从下锅算 30 秒）。
        list.add(new CookingSkeleton(
            HotPot.id("hotpot_cooking/cold_tofu"),
            null,
            new ItemStack(HotPotContent.COLD_TOFU),
            new ItemStack(HotPotContent.HOT_TOFU),
            new ItemStack(HotPotContent.HOT_TOFU_CUBE, 2),
            HotPotCooking.DEFAULT_OVERCOOK_TICKS,
            0));
        list.add(cooking("cold_tofu_cube", HotPotContent.COLD_TOFU_CUBE, HotPotContent.HOT_TOFU_CUBE, 100));
        list.add(cooking("beef_meatball", HotPotContent.BEEF_MEATBALL, HotPotContent.COOKED_BEEF_MEATBALL, 200));
        list.add(cooking("cilantro_beef_meatball", HotPotContent.CILANTRO_BEEF_MEATBALL,
            HotPotContent.COOKED_CILANTRO_BEEF_MEATBALL, 200));
        list.add(cooking("pork_meatball", HotPotContent.PORK_MEATBALL, HotPotContent.COOKED_PORK_MEATBALL, 200));
        list.add(cooking("cilantro_pork_meatball", HotPotContent.CILANTRO_PORK_MEATBALL,
            HotPotContent.COOKED_CILANTRO_PORK_MEATBALL, 200));
        list.add(cooking("mutton_meatball", HotPotContent.MUTTON_MEATBALL, HotPotContent.COOKED_MUTTON_MEATBALL, 200));
        list.add(cooking("cilantro_mutton_meatball", HotPotContent.CILANTRO_MUTTON_MEATBALL,
            HotPotContent.COOKED_CILANTRO_MUTTON_MEATBALL, 200));
        list.add(cooking("fish_ball", HotPotContent.FISH_BALL, HotPotContent.COOKED_FISH_BALL, 200));
        list.add(cooking("cilantro_fish_ball", HotPotContent.CILANTRO_FISH_BALL,
            HotPotContent.COOKED_CILANTRO_FISH_BALL, 200));
        // r137 追加的 10 样新食材：酥肉两段（生酥肉 → 酥肉 → 煮酥肉，第二段下锅走锅专用产物表）。
        list.add(cooking("raw_crispy_pork", HotPotContent.RAW_CRISPY_PORK, HotPotContent.CRISPY_PORK, 150));
        list.add(cooking("crispy_pork", HotPotContent.CRISPY_PORK, HotPotContent.BOILED_CRISPY_PORK, 150));
        list.add(cooking("dried_tofu", HotPotContent.DRIED_TOFU, HotPotContent.COOKED_DRIED_TOFU, 150));
        list.add(cooking("tofu_skin", HotPotContent.TOFU_SKIN, HotPotContent.COOKED_TOFU_SKIN, 100));
        list.add(cooking("wide_noodles", HotPotContent.WIDE_NOODLES, HotPotContent.COOKED_WIDE_NOODLES, 150));
        list.add(cooking("sweet_potato_skin", HotPotContent.SWEET_POTATO_SKIN,
            HotPotContent.COOKED_SWEET_POTATO_SKIN, 100));
        list.add(cooking("enoki_mushroom", HotPotContent.ENOKI_MUSHROOM, HotPotContent.COOKED_ENOKI_MUSHROOM, 50));
        list.add(cooking("greens", HotPotContent.GREENS, HotPotContent.COOKED_GREENS, 50));
        list.add(cooking("lotus_root_slice", HotPotContent.LOTUS_ROOT_SLICE, HotPotContent.COOKED_LOTUS_ROOT_SLICE, 100));
        list.add(cooking("squid_roll", HotPotContent.SQUID_ROLL, HotPotContent.COOKED_SQUID_ROLL, 100));
        list.add(cooking("zhe_er_gen_slice", HotPotContent.ZHE_ER_GEN_SLICE, HotPotContent.COOKED_ZHE_ER_GEN_SLICE, 200));
        return List.copyOf(list);
    }

    /** 造一条普通涮煮配方的骨架：没有煮老产物，刻数走 {@code cooking.items.<rawPath>}。 */
    private static CookingSkeleton cooking(String rawPath, Item raw, Item cooked, int defaultTicks) {
        return new CookingSkeleton(
            HotPot.id("hotpot_cooking/" + rawPath),
            rawPath,
            new ItemStack(raw),
            new ItemStack(cooked),
            ItemStack.EMPTY,
            0,
            defaultTicks);
    }

    /**
     * 全部 30 条火锅专属涮煮配方（r137 前 19 条 + 2026-11-06 追加的 10 条 + r139 折耳根段 1 条）；
     * 刻数、经验每次调用现读配置，reload 后立即反映到配方查看器。
     *
     * <p>每条骨架后面的数字只是「配置里缺这一项时」的兜底值，和
     * {@code HotPotCooking#registerDefaultItemCookTicks} 登记进配置模板的四档默认一致
     * （极快熟 50、快熟 100、中等 150、慢熟 200）。</p>
     */
    public static List<CookingEntry> cookingRecipes() {
        // 普通食材的经验走 cooking.xp_per_item；整块豆腐一直走 cooking.fallback_xp（原有行为，保持不变）
        float itemXp = Config.xpPerItem(DEFAULT_XP);
        float tofuXp = Config.fallbackXp(DEFAULT_XP);
        List<CookingEntry> recipes = new ArrayList<>(COOKING_SKELETONS.size());
        for (CookingSkeleton skeleton : COOKING_SKELETONS) {
            boolean tofu = skeleton.configKey() == null;
            // 只有整块豆腐走 cooking.tofu_*，其余走 cooking.items.<食材>
            int cookTicks = tofu
                ? Config.tofuCookTicks(HotPotContent.TOFU_COOK_TICKS)
                : Config.itemCookTicks(skeleton.configKey(), skeleton.defaultTicks());
            int overcookTotal = tofu
                ? Config.tofuOvercookTicks(HotPotCooking.DEFAULT_OVERCOOK_TICKS)
                : skeleton.overcookTotalTicks();
            // 每个条目都 copy()：ItemStack 的 count 可变，共享同一个栈会被插件改坏
            recipes.add(new CookingEntry(skeleton.id(), skeleton.input().copy(), skeleton.output().copy(), cookTicks,
                tofu ? tofuXp : itemXp, skeleton.overcookOutput().copy(), overcookTotal));
        }
        return List.copyOf(recipes);
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

    /** 全部 9 条擀面杖拍打配方（r137 前 7 条 + mryt233 改 2026-11-06 追加的花生、r139 折耳根）：拍 3 下出丸子 / 蒜泥 / 虾滑 / 花生碎，中间两下砧板上的样子也一并展示。 */
    public static List<PoundingEntry> poundingRecipes() {
        List<PoundingEntry> recipes = new ArrayList<>(9);
        recipes.add(pounding("beef", Items.BEEF, HotPotContent.BEEF_MEATBALL, 3));
        recipes.add(pounding("porkchop", Items.PORKCHOP, HotPotContent.PORK_MEATBALL, 3));
        recipes.add(pounding("mutton", Items.MUTTON, HotPotContent.MUTTON_MEATBALL, 3));
        recipes.add(pounding("cod", Items.COD, HotPotContent.FISH_BALL, 3));
        recipes.add(pounding("salmon", Items.SALMON, HotPotContent.FISH_BALL, 3));
        recipes.add(pounding("garlic", HotPotContent.GARLIC, HotPotContent.GARLIC_PASTE, 2));
        recipes.add(pounding("peeled_shrimp", HotPotContent.PEELED_SHRIMP, HotPotContent.SHRIMP_PASTE, 1));
        // r137：花生敲 3 下出 2 份花生碎。
        recipes.add(pounding("peanut", HotPotContent.PEANUT, HotPotContent.CRUSHED_PEANUTS, 2));
        // r139：折耳根敲 3 下出 2 份折耳根碎（蘸料台第 15 种调料的原料）。
        recipes.add(pounding("zhe_er_gen", HotPotContent.ZHE_ER_GEN, HotPotContent.MINCED_ZHE_ER_GEN, 2));
        return List.copyOf(recipes);
    }

    private static PoundingEntry pounding(String path, Item raw, Item output, int count) {
        // mryt233 改（2026-11-07）：两个中间阶段的展示栈也统一从这里建，不再各写一次 new ItemStack(raw)
        ItemStack stage = new ItemStack(raw);
        return new PoundingEntry(
            HotPot.id("pounding/" + path),
            stage.copy(),
            new ItemStack(HotPotContent.ROLLING_PIN),
            Pounding.display(stage.copy(), 1),
            Pounding.display(stage.copy(), 2),
            new ItemStack(output, count),
            POUNDING_HITS);
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
}
