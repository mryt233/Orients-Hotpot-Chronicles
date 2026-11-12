package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.BlockPos;

/** 作者：mryt233；mryt233 改（2026-11-10）。注册中心：灶台、锅、筷子、方块实体、屏幕与独立创造标签页。 */
public final class HotPotContent {
    /** 灶台本身就是组合方块的宿主；锅放上去只改变 POT 状态。 */
    public static StoveBlock STOVE;

    /** 高灶台：配 1.5 格高火锅桌，灶口、锅、汤整体抬高 {@link StoveBlock#TALL_LIFT} 像素，灶膛还在最下面。 */
    public static StoveBlock TALL_STOVE;
    /** 高灶台的「上半截」：看不见，让伸进上面一格的锅也点得到（没有物品）。 */
    public static StoveTopBlock STOVE_TOP;

    /**
     * mryt233 改（2026-11-10，r147）：12 种锅的方块 / 物品改成 {@link java.util.EnumMap} + 循环注册。
     * 以前是 12 个字段 + 两个 12 路的 switch（blockFor / itemFor）共 26 处手写；现在只在
     * {@link #POT_ORDER} 里排一次序。注册顺序严格照旧（先铜后铁里也照旧：九宫格、S、O、方心、直隔、田字），
     * ID 与创造栏顺序都由同一份表决定，不会漂移。
     *
     * <p>旧字段名（COPPER_HOT_POT 等）保留成指向同一对象的别名，子模组和旧调用方不受影响。</p>
     */
    public static final List<HotPotMaterial> POT_ORDER = List.of(
        HotPotMaterial.COPPER, HotPotMaterial.IRON,
        HotPotMaterial.COPPER_S, HotPotMaterial.COPPER_O,
        HotPotMaterial.IRON_S, HotPotMaterial.IRON_O,
        HotPotMaterial.COPPER_SQUARE, HotPotMaterial.IRON_SQUARE,
        HotPotMaterial.COPPER_SPLIT, HotPotMaterial.IRON_SPLIT,
        HotPotMaterial.COPPER_FOUR, HotPotMaterial.IRON_FOUR);

    /** 锅方块：材质 → 方块（顺序同 {@link #POT_ORDER}）。 */
    public static final Map<HotPotMaterial, HotPotBlock> POT_BLOCKS =
        new EnumMap<>(HotPotMaterial.class);
    /** 锅物品：材质 → 物品（同一顺序）。 */
    public static final Map<HotPotMaterial, BlockItem> POT_ITEMS =
        new EnumMap<>(HotPotMaterial.class);



    /** 每种原版木板一种火锅桌（顺序同原版木板），围着灶台拼成 3x3 的大桌，顶面和灶台台面齐平。 */
    public static final List<String> TABLE_WOODS = List.of(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped");
    /** 石料 / 金属桌面的桌子：{材质（ID 用）, 取声音和颜色的原版方块}，和 tools/gen_stone_tables.py 的 MATERIALS 同序（2026-10-26）。 */
    static final String[][] STONE_TABLES = {
        {"smooth_stone", "smooth_stone"}, {"polished_andesite", "polished_andesite"}, {"polished_diorite", "polished_diorite"},
        {"polished_granite", "polished_granite"}, {"polished_deepslate", "polished_deepslate"},
        {"polished_blackstone", "polished_blackstone"}, {"quartz", "quartz_block"}, {"cut_copper", "cut_copper"},
        {"iron", "iron_block"}};
    public static final List<HotPotTableBlock> HOTPOT_TABLES = new ArrayList<>();
    public static final List<BlockItem> HOTPOT_TABLE_ITEMS = new ArrayList<>();
    /** 板凳（矮）：同样每种原版木板一种，能坐。 */
    public static final List<BenchBlock> BENCHES = new ArrayList<>();
    public static final List<BlockItem> BENCH_ITEMS = new ArrayList<>();
    /** 1.5 格高的火锅桌和一格高的条凳（配高灶台），同样每种原版木板一种。 */
    public static final List<TallHotPotTableBlock> TALL_TABLES = new ArrayList<>();
    public static final List<BlockItem> TALL_TABLE_ITEMS = new ArrayList<>();
    public static final List<BenchBlock> TALL_BENCHES = new ArrayList<>();
    public static final List<BlockItem> TALL_BENCH_ITEMS = new ArrayList<>();

    /**
     * 盘子材质（顺序：材质在外层；打乱会让注册顺序和创造栏顺序变化）。
     * mryt233 改（2026-11-10，r147）：材质做成一条记录（名字、地图颜色、声音），
     * 以前这里只有名字，颜色和声音在两个按名字写的 switch 里——加一种材质要改三处。
     * 这条记录和 {@code content/materials.json} 的 {@code plates} 段一一对应（启动时核对，
     * 见 {@link #verifyPlateMaterials()}）。
     */
    public record PlateMaterial(String id,
                                MapColor mapColor,
                                SoundType sounds) {
    }

    public static final List<PlateMaterial> PLATE_MATERIALS = List.of(
        new PlateMaterial("quartz", MapColor.QUARTZ,
            SoundType.DECORATED_POT),
        new PlateMaterial("bamboo", MapColor.SAND,
            SoundType.BAMBOO_WOOD),
        new PlateMaterial("terracotta", MapColor.COLOR_ORANGE,
            SoundType.DECORATED_POT),
        new PlateMaterial("copper", MapColor.COLOR_ORANGE,
            SoundType.COPPER),
        new PlateMaterial("iron", MapColor.METAL,
            SoundType.METAL));
    public static final List<PlateBlock> PLATES = new ArrayList<>();
    public static final List<PlateItem> PLATE_ITEMS = new ArrayList<>();
    /** 筷子上夹着的那一样食物（组件值包在 HeldFood 里，NeoForge 要求组件值实现 equals/hashCode）。 */
    public static DataComponentType<HeldFood> HELD_FOOD;



    /** 摆在桌上的碗（只带模型，没有物品；r99）。 */
    public static BowlBlock BOWL_BLOCK;
    /** 摆在桌上的橙子饮料（只带模型，r99）。 */
    public static DrinkBlock DRINK_BLOCK;
    /** 摆在桌上的玻璃杯（只带模型，2026-10-25）。 */
    public static CupBlock CUP_BLOCK;
    // mryt233 改（r146a，2026-11-08）：芝麻酱 / 蚝油瓶也能摆上桌（用户第三项）。和饮料瓶、玻璃杯一样
    // 只带模型、借「盘子格」摆，摆法见 SauceBottleBlock 与 SauceBottleItem。
    // mryt233 改（r146c，2026-11-09）：瓶子只做摆设，桌上加料与每瓶次数已去掉。
    /** 摆在桌上的芝麻酱 / 蚝油瓶（只带模型，r146a）。方块状态 kind 决定画哪种瓶子。 */
    public static SauceBottleBlock SAUCE_BOTTLE_BLOCK;
    // mryt233 改（2026-11-10）：桌面竹筷方块引用——直接摆在桌上的竹筷（带迷你筷架，
    // 借用 PlateHolderBlock 摆放，方块类见 ChopstickRestBlock）。
    /** 直接摆在桌上的竹筷（带迷你筷架，借用 PlateHolderBlock 摆放，见 ChopstickRestBlock）。 */
    public static ChopstickRestBlock CHOPSTICK_REST_BLOCK;

    /** 装着饮料的炼药锅（2026-10-27，见 DrinkCauldronBlock；没有物品，拆了掉原版炼药锅）。 */
    public static DrinkCauldronBlock DRINK_CAULDRON;

    /** 摆盘子的那一格：一格里能摆好几个盘子。 */
    public static PlateHolderBlock PLATE_HOLDER;
    public static BlockEntityType<PlateHolderBlockEntity> PLATE_HOLDER_BE;

    /** 坐条凳时骑的看不见的座位实体。 */
    public static EntityType<SeatEntity> SEAT;



    public static BlockItem STOVE_ITEM;

    public static BlockItem TALL_STOVE_ITEM;

    /**
     * mryt233 改（2026-11-10，r147）：12 种锅物品也改成循环注册，顺序同 {@link #POT_ORDER}。
     * 旧字段名保留成别名。
     */


    // ---- 旧字段名：都指向 EnumMap 里同一个对象，子模组 / 旧调用方不用改（mryt233 2026-11-10） ----
    public static HotPotBlock COPPER_HOT_POT;
    public static HotPotBlock IRON_HOT_POT;
    /** 鸳鸯锅（2026-10-23，新增）：S 形 / O 形隔板，两边各一种汤；铜、铁各两种。 */
    public static HotPotBlock COPPER_S_POT;
    public static HotPotBlock COPPER_O_POT;
    public static HotPotBlock IRON_S_POT;
    public static HotPotBlock IRON_O_POT;
    public static HotPotBlock COPPER_SQUARE_POT;
    public static HotPotBlock IRON_SQUARE_POT;
    public static HotPotBlock COPPER_SPLIT_POT;
    public static HotPotBlock IRON_SPLIT_POT;
    /** 田字四宫格锅：四块各一种汤。 */
    public static HotPotBlock COPPER_FOUR_POT;
    public static HotPotBlock IRON_FOUR_POT;

    public static BlockItem COPPER_HOT_POT_ITEM;
    public static BlockItem IRON_HOT_POT_ITEM;
    public static BlockItem COPPER_S_POT_ITEM;
    public static BlockItem COPPER_O_POT_ITEM;
    public static BlockItem IRON_S_POT_ITEM;
    public static BlockItem IRON_O_POT_ITEM;
    public static BlockItem COPPER_SQUARE_POT_ITEM;
    public static BlockItem IRON_SQUARE_POT_ITEM;
    public static BlockItem COPPER_SPLIT_POT_ITEM;
    public static BlockItem IRON_SPLIT_POT_ITEM;
    public static BlockItem COPPER_FOUR_POT_ITEM;
    public static BlockItem IRON_FOUR_POT_ITEM;

    /**
     * 毛肚（牛掉落，r93）：生的回 2 点饥饿；熟的回 5 点。熔炉 / 烟熏炉 / 营火都能烤，锅里涮得比牛肉快一半（熔炉配方 100 tick）。
     *
     * <p>mryt233 改（2026-11-05）：毛肚以前直接 new Item 写死营养值，配置里的 food.tripe / food.cooked_tripe
     * 改不动它。改成走 {@link #foodItem}，和别的食物一样进配置、吃的时候现读、能热重载。</p>
     */
    public static Item RAW_TRIPE;
    public static Item COOKED_TRIPE;

    /**
     * 切菜（2026-10-23 用户定，新增）：生牛肉 / 生羊肉在砧板上用铁菜刀点一下（或切石机）切成 3 卷；
     * 每卷熟的回 3 饥饿（3 卷 = 9，比整块牛排多一点，算切开涮的奖励），生的回 1；锅里涮得比整块肉快一半（熔炉配方 100 tick）。
     */
    public static Item FATTY_BEEF_ROLL;
    public static Item COOKED_FATTY_BEEF_ROLL;
    public static Item MUTTON_ROLL;
    public static Item COOKED_MUTTON_ROLL;
    /**
     * 吊龙（用户定，新增）：牛 / 哞菇 25% 掉一块牛脊肉（生吃同生牛肉 3 饥饿；整块烤 = 熟牛脊肉，数值同熟牛排），
     * 砧板上一刀（或切石机）切成 3 片吊龙；每片熟的回 3、生的回 1；锅里涮得最快（熔炉 50 tick，肥牛卷的一半）。
     */
    public static Item BEEF_LOIN;
    /** 熟牛脊肉（用户要）：整块烤熟，饥饿和饱和度同原版熟牛排（8 / 0.8）。 */
    public static Item COOKED_BEEF_LOIN;
    public static Item BEEF_LOIN_SLICE;
    public static Item COOKED_BEEF_LOIN_SLICE;
    /** 铁菜刀：耐久同铁剑（250），切一次掉 1 点；也能当武器，伤害比铁剑低（4）。 */
    public static Item KITCHEN_KNIFE;
    // mryt233 改（2026-11-05）：砧板照原版橡木木板（硬度 2.0、抗性 3.0、不强制工具），挖掘工具见
    // data/minecraft/tags/block/mineable/axe.json；板上放着的东西挖的时候单独掉（见 CuttingBoardBlock#onRemove）。
    public static CuttingBoardBlock CUTTING_BOARD;   // 选中框随板上有没有东西变（坑：形状跟着方块实体变就要 dynamicBounds）
    public static BlockItem CUTTING_BOARD_ITEM;
    public static BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD_BE;

    /**
     * 土豆片、豆腐（2026-10-23 用户：先做原版有的食材）：原版土豆在砧板上切三下、每下一份「土豆片」（一份贴图上是三片），
     * 切石机一次出 3 份；熟的每份回 2。黄豆打草掉（GarnishLoot，15%），3 黄豆 + 水桶做一整块豆腐，砧板上一刀切成 3 份豆腐块，熟的每份回 2。
     */
    public static Item POTATO_SLICE;
    public static Item COOKED_POTATO_SLICE;
    public static Item SOYBEAN;
    // mryt233 改（2026-11-04）：豆腐分冷热（用户定）。整块冷豆腐在锅里煮成热豆腐，热豆腐煮过头碎成热豆腐块。
    // 旧 ID（tofu / tofu_cube / cooked_tofu_cube）一并改掉，alpha 期间不做旧存档兼容。
    public static Item COLD_TOFU;
    public static Item HOT_TOFU;
    public static Item COLD_TOFU_CUBE;
    /** 豆腐在锅里煮成热豆腐的刻数（10 秒），以及从下锅算起煮过头的总刻数（30 秒）；都能在配置里改。 */
    public static final int TOFU_COOK_TICKS = 200;
    public static final int TOFU_OVERCOOK_TICKS = 600;
    /**
     * 橙子（2026-10-25 用户：橙子饮料要有原料）：橡木 / 深色橡木树叶像掉苹果一样掉（OrangeLoot），能吃，数值同苹果。
     * 大蒜：打草掉（GarnishLoot，同小葱 / 香菜 / 黄豆；用户：作物先保留）；在砧板上用擀面杖敲三下出 2 份蒜泥（Pounding），
     * 蒜泥是蘸料台第 10 种调料（Seasoning.GARLIC_PASTE）。新增
     */
    public static Item ORANGE;
    public static Item GARLIC;
    public static Item GARLIC_PASTE;
    /**
     * 虾（2026-10-26 用户：加虾和虾滑）：钓鱼钓到带壳的虾（ShrimpLoot）；砧板上菜刀切一刀剥成虾仁（去壳去头，也能用切石机）；
     * 虾仁放砧板上擀面杖拍三下成一颗虾滑（Pounding）。三样都能下锅涮 / 烤熟，熟的回 3 / 3 / 4。新增
     */
    public static Item SHRIMP;
    public static Item COOKED_SHRIMP;
    public static Item PEELED_SHRIMP;
    public static Item COOKED_PEELED_SHRIMP;
    public static Item SHRIMP_PASTE;
    public static Item COOKED_SHRIMP_PASTE;
    /** 火腿肠（2026-10-29 用户：不用单独淀粉，原版小麦 + 猪肉加工）：猪排切成猪肉末，再和小麦合成；原有猪肉捣丸子不变。 */
    public static Item MINCED_PORK;
    public static Item RAW_HAM_SAUSAGE;
    /** 火腿肠 = 生火腿肠煮熟（2026-10-29 用户改名：熟火腿肠改叫「火腿肠」，ID 同步成 ham_sausage）。 */
    public static Item HAM_SAUSAGE;
    /** 火腿肠片（2026-10-29 用户：火腿肠放菜板上切，一刀出 3 片；还能再加热）。 */
    public static Item HAM_SAUSAGE_SLICE;
    /** 熟火腿肠片：火腿肠片再加热一次（2026-10-29 用户）。 */
    public static Item COOKED_HAM_SAUSAGE_SLICE;
    // ---- mryt233 改（2026-11-06）：r137 新增 4 种蘸料原料与 10 组第二梯队火锅食材 ----
    /**
     * 蚝油、芝麻酱、花生、花生碎、小米辣（辣椒在砧板上切 1 刀出 2 份小米辣；花生用擀面杖敲 3 下出 2 份花生碎）。
     * <p>mryt233 改（r146a，2026-11-08）：蚝油、芝麻酱改成 {@link SauceBottleItem}——ID、贴图、配方、
     * 饥饿值都不变，多出「摆上桌」的玩法（用户第三项）。</p>
     * <p>mryt233 改（r146c，2026-11-09）：摆上桌只是摆设，不能给菜加料（加这一味仍去蘸料台），也没有次数；
     * 物品恢复成可正常堆叠。</p>
     */
    public static SauceBottleItem OYSTER_SAUCE;
    public static SauceBottleItem SESAME_PASTE;
    public static Item PEANUT;
    public static Item CRUSHED_PEANUTS;
    public static Item MILLET_CHILI;

    /** 酥肉链：猪肉末 + 小麦 → 3 份生酥肉（raw_crispy_pork）→ 烧炼/下锅成酥肉（crispy_pork，回 4）→ 再下锅涮成煮酥肉（boiled_crispy_pork，回 6）。 */
    public static Item RAW_CRISPY_PORK;
    public static Item CRISPY_PORK;
    public static Item BOILED_CRISPY_PORK;

    /** 豆干与腐竹：冷豆腐合成/加工 → 豆干（dried_tofu）与腐竹（tofu_skin），下锅/烧炼成熟豆干、熟腐竹。 */
    public static Item DRIED_TOFU;
    public static Item COOKED_DRIED_TOFU;
    public static Item TOFU_SKIN;
    public static Item COOKED_TOFU_SKIN;

    /** 宽粉与苕皮：土豆片加工 → 宽粉（wide_noodles）与苕皮（sweet_potato_skin），下锅/烧炼成熟宽粉、熟苕皮。 */
    public static Item WIDE_NOODLES;
    public static Item COOKED_WIDE_NOODLES;
    public static Item SWEET_POTATO_SKIN;
    public static Item COOKED_SWEET_POTATO_SKIN;

    /** 金针菇、青菜、莲藕与藕片、生鱿鱼与鱿鱼卷、蛋炒饭。 */
    public static Item ENOKI_MUSHROOM;
    public static Item COOKED_ENOKI_MUSHROOM;
    public static Item GREENS;
    public static Item COOKED_GREENS;
    public static Item LOTUS_ROOT;
    public static Item LOTUS_ROOT_SLICE;
    public static Item COOKED_LOTUS_ROOT_SLICE;
    public static Item RAW_SQUID;
    public static Item SQUID_ROLL;
    public static Item COOKED_SQUID_ROLL;
    public static Item EGG_FRIED_RICE;

    // ---- mryt233 改（2026-11-06）：r139 新增折耳根（鱼腥草）与布围裙 ----
    /**
     * 折耳根（鱼腥草，2026-11-06 用户定，西南火锅灵魂食材）：打草掉（GarnishLoot 15%，和青菜、花生一样）。
     * 生折耳根能直接吃（白嫩根节，脆、带腥香），也能放砧板上切一刀出 3 段；段下锅涮 10 秒变熟段。
     * 折耳根碎是蘸料台第 15 种调料（Seasoning.MINCED_ZHE_ER_GEN）的原料：整根在砧板上用擀面杖敲 3 下出 2 份碎末。
     */
    public static Item ZHE_ER_GEN;
    /** 折耳根段：砧板一刀切成 3 段（切石机同样是 3 段），下火锅煮。生吃回 1。 */
    public static Item ZHE_ER_GEN_SLICE;
    /** 熟折耳根段：锅里涮 ~10 秒（200 刻）的产物，回 3。腥气煮掉大半，只剩一点回甘。 */
    public static Item COOKED_ZHE_ER_GEN_SLICE;
    /** 折耳根碎：擀面杖敲 3 下出 2 份（Pounding），蘸料台第 15 种调料「折耳根碎」的原料。 */
    public static Item MINCED_ZHE_ER_GEN;
    /**
     * 布围裙（2026-11-06 用户定，塑料围裙方案已取消，只做布的）：胸甲部位，属性照原版皮革防具
     * （防御 3、耐久 80、附魔性 15、皮革装备音），修复材料用白色羊毛或线（可修复物品见
     * {@link ClothApronItem#isValidRepairItem}，配方见 recipes/cloth_apron.json）。
     *
     * <p>防护特性：穿上它时（{@link ClothApronItem}）① 变态辣菜品吃下去不再往嘴上点火苗；
     * ② 站在烧着的火锅 / 灶台旁不再被热油沸水溅到（热的灶台边不受伤、不点着）。</p>
     */
    public static Item CLOTH_APRON;
    // mryt233 改（2026-11-09，r146b）：另外两款布围裙（蓝印花 / 棕边带兜），数值与米白款完全一样，
    // 只有护甲层贴图和名字不同；三款之间用工作台加染料互相换款（配方见 recipe/ 下的 apron_*_recolor.json）。
    /** 蓝印花布围裙：靛蓝底 + 白色十字小花。 */
    public static Item BLUE_PRINT_CLOTH_APRON;
    /** 棕边布围裙：米白底 + 深棕滚边 + 方框口袋。 */
    public static Item BROWN_TRIM_CLOTH_APRON;

    /** 三款布围裙（创造栏 / 换款配方 / 成就共用）。 */
    public static List<Item> CLOTH_APRONS;

    // ==== tools/new_item.py：新物品的注册插在这行上面（mryt233 2026-11-01，别删这行）====
    /** 番茄（2026-10-25）：打草掉（GarnishLoot，同大蒜 15%），能吃；做番茄锅底料。 */
    public static Item TOMATO;
    // mryt233 改（2026-11-05）：辣椒（r136a）。打草掉（GarnishLoot 15%），能直接吃；拿在手里右键红汤锅提升辣度档位。
    // 辣椒不参与下锅涮煮，只做红汤的辣度调味（HotPotCooking.acceptable 里明确拒收）。
    /** 辣椒（2026-11-05 用户定）：打草掉（GarnishLoot，15%），能直接吃；拿着右键红汤锅提升辣度档位。 */
    public static Item CHILI_PEPPER;
    /** 游戏内教程书「火锅指南」（2026-10-25 用户）：菜刀 + 书无序合成，右键打开（GuideBookItem / client.GuideBookScreen）。 */
    public static Item HOTPOT_GUIDE;
    /** 锅底料（2026-10-25）：拿着右键锅 = 倒进新汤底（SoupBase.TOMATO / THREE_FRESH），一块一锅。 */
    public static Item TOMATO_HOTPOT_BASE;
    public static Item THREE_FRESH_HOTPOT_BASE;
    /**
     * 汤底桶（2026-10-26 用户）：只能用空桶从番茄锅 / 三鲜锅里舀出来（没有配方），倒回锅里就是那种汤（SoupBase 的 source）；
     * 不是液体，不能倒在地上。图标照原版水桶 / 岩浆桶的样子（tools/gen_garden.py）。
     */
    public static Item TOMATO_SOUP_BUCKET;
    public static Item THREE_FRESH_SOUP_BUCKET;
    /** 熟海带（2026-10-25 用户）：原版海带在锅里煮出来（HotPotCooking.potResult），熔炉 / 烟熏炉照旧是干海带；回 2。 */
    public static Item COOKED_KELP;
    /** 热豆腐块（2026-11-04 改名）：原熟豆腐块，数值不变。 */
    public static Item HOT_TOFU_CUBE;

    /**
     * 丸子（2026-10-23 用户定）：木棒在砧板上捣三下出 3 个生丸子（Pounding）；熟的每个回 3，香菜版（2 生丸子 + 1 香菜 → 2 个）每个回 4。
     * 鱼丸用生鳕鱼 / 生鲑鱼。擀面杖（用户：木棒改成擀面杖的样子，名字和 ID 一起改）耐久 120，捣一下掉 1 点。
     */
    public static Item ROLLING_PIN;
    public static Item BEEF_MEATBALL;
    public static Item PORK_MEATBALL;
    public static Item MUTTON_MEATBALL;
    public static Item FISH_BALL;
    public static Item CILANTRO_BEEF_MEATBALL;
    public static Item CILANTRO_PORK_MEATBALL;
    public static Item CILANTRO_MUTTON_MEATBALL;
    public static Item CILANTRO_FISH_BALL;
    public static Item COOKED_BEEF_MEATBALL;
    public static Item COOKED_PORK_MEATBALL;
    public static Item COOKED_MUTTON_MEATBALL;
    public static Item COOKED_FISH_BALL;
    public static Item COOKED_CILANTRO_BEEF_MEATBALL;
    public static Item COOKED_CILANTRO_PORK_MEATBALL;
    public static Item COOKED_CILANTRO_MUTTON_MEATBALL;
    public static Item COOKED_CILANTRO_FISH_BALL;

    /** 砧板上捣到一半的肉（捶碎 / 肉浆）：只给砧板渲染器画立体用，不进创造栏；样子按自定义模型数据换（Pounding.display）。 */
    public static Item POUNDED_MEAT;

    /**
     * 食物数值先问配置文件有没有覆盖，没有就用这里写的默认值（见 Config）。
     * 注册进组件的数值只是默认值；真正吃的时候 {@link ConfiguredFoodItem} 会再按配置算一次，
     * 这样改配置以后 /orientshotpot reload 就能生效，不用重启游戏。
     * 本模组的食物都是「饱了也能吃」：饥饿条满了不再涨，饱和度照常加到 20（用户 2026-11-01：
     * 原版吃法就能一直吃；另装 Always Eat 也不冲突，它只是提前开始吃，结算仍走这里。mryt233 改）。
     */
    private static Item foodItem(RegistrationApi registration, String id, int nutrition, float saturation) {
        Config.rememberFood(id, nutrition, saturation);
        return registration.items().register( HotPot.id(id),
            new ConfiguredFoodItem(id, new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build())));
    }

    /**
     * mryt233 改（r146a，2026-11-08）：能摆上桌的蘸料瓶（芝麻酱 / 蚝油）。
     * 营养值和别的食物一样记进 {@code food.<id>.hunger / .saturation}，吃的时候现读配置。
     * mryt233 改（r146c，2026-11-09）：只做摆设，没有次数配置项（见 {@link SauceBottleKind}）。
     */
    private static SauceBottleItem sauceBottle(RegistrationApi registration, SauceBottleKind kind, int nutrition, float saturation) {
        return registration.items().register( HotPot.id(kind.id()),
            new SauceBottleItem(kind, kind.id(), nutrition, saturation));
    }

    /** 一碗汤（r99）：顺序同 SoupBase（清汤、高汤、菌汤、红汤）。 */
    public static List<SoupBowlItem> SOUP_BOWLS;
    /** 通用汤碗（mryt233，4F-3）：未登记专属碗的自定义 / 数据包汤底共用，通过 {@link #SOUP} 组件区分汤底。 */
    public static SoupBowlItem SOUP_BOWL;

    /** 饮料（r99 橙子饮料，r101 维二）和喝完的空瓶。 */
    public static DrinkItem ORANGE_DRINK;
    public static EmptyBottleItem EMPTY_ORANGE_DRINK_BOTTLE;
    public static DrinkItem PLANT_PROTEIN_DRINK;
    public static EmptyBottleItem EMPTY_PLANT_PROTEIN_DRINK_BOTTLE;
    /** 之晗可乐（原名 之晗可乐，2026-10-26 用户改名）（2026-10-25 用户定名）：一瓶 4 口，喝完剩空瓶，满瓶空瓶都能摆桌上（和另外两种饮料一样）。 */
    public static DrinkItem ORIENT_COLA;
    public static EmptyBottleItem EMPTY_ORIENT_COLA_BOTTLE;
    /** 玻璃杯（2026-10-25 用户）：空杯 + 三种饮料各一种装满的杯子（一口），见 CupItem。 */
    public static CupItem GLASS_CUP;
    public static CupItem ORANGE_DRINK_CUP;
    public static CupItem PLANT_PROTEIN_DRINK_CUP;
    public static CupItem ORIENT_COLA_CUP;
    /** 一杯水、一杯牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒，桶不会空；空杯对着水源右键也能舀一杯水。 */
    public static CupItem WATER_CUP;
    public static CupItem MILK_CUP;

    /**
     * mryt233 改（2026-11-10，r147）：这三个把饮料种类翻成物品的 switch 删掉了——
     * 瓶子 / 空瓶 / 杯子都挂在 {@link DrinkKind} 自己身上（延迟读取本类物品字段，不在注册排队期间查注册表）。
     * 保留这三个公开方法是为了子模组和旧调用方还能按原名字拿到东西。
     */
    public static CupItem cupFor(DrinkKind kind) {
        return kind.cup();
    }

    public static DrinkItem drinkFor(DrinkKind kind) {
        return kind.bottle();
    }

    public static Item emptyBottle(DrinkKind kind) {
        return kind.emptyBottle();
    }

    public static Item CHOPSTICKS;

    // ------------------------------------------------------------------ 蘸料（r103，用户定的玩法见 Seasoning / SauceStationBlock）
    /**
     * 小葱、香菜：打草掉落（各 25%），能吃回 1 点饥饿值；放进蘸料台变成葱花、香菜碎末。
     *
     * <p>mryt233 改（2026-11-05）：同毛肚，改成走 {@link #foodItem}，营养值进配置、能热重载。</p>
     */
    public static Item SCALLION;
    public static Item CILANTRO;
    /** 一碗蘸料里放了哪几种、还能蘸几口。 */
    public static DataComponentType<SauceContents> SAUCE;
    /** 菜蘸过哪几种调料（Seasoning.bit 的组合）；吃下去时生效（LivingEntityEatMixin）。 */
    public static DataComponentType<java.util.List<String>> DIPPED;
    // mryt233 改（2026-11-05）：汤底风味加成（r136a）。菜记着自己是在哪种汤底里煮出来的，
    // 吃下去时按汤底（红汤再按辣度档位）补一份状态效果，见 SoupBase.dishBonusEffects(int) 与 LivingEntityEatMixin。
    /** 这道菜是在哪种汤底里煮出来的（SoupBase.textureKey()，如 "spicy"、"mushroom"）；吃下时给汤底风味加成。 */
    public static DataComponentType<String> COOKED_IN_SOUP;
    /** 通用汤碗所盛汤底的完整 ID（mryt233，4F-3）。 */
    public static DataComponentType<String> SOUP;
    /** 红汤辣度档位（0=微辣，1=中辣，2=特辣，3=变态辣）：记在红汤煮出的菜或盛出的红汤碗上。 */
    public static DataComponentType<Integer> SPICY_LEVEL;
    // mryt233 改（2026-11-10）：干杯与桌面筷架使用的组件和方块引用（干杯标记、桌面迷你筷架）。
    /** 玻璃杯是否已经碰过杯（干杯标记，喝下时多回 1 饥饿值）。 */
    public static DataComponentType<Boolean> CHEERED;
    public static DippingSauceItem DIPPING_SAUCE;
    /** 蘸料台：11 种原版木头各一个 + 铁的（用户定）；ID = <木头>_sauce_station / iron_sauce_station。 */
    public static final List<SauceStationBlock> SAUCE_STATIONS = new ArrayList<>();
    public static final List<BlockItem> SAUCE_STATION_ITEMS = new ArrayList<>();



    /** 蘸料台的「上半截」：看不见，占住头顶那一格（没有物品；2026-10-23）。 */
    public static SauceStationTopBlock SAUCE_STATION_TOP;

    public static BlockEntityType<SauceStationBlockEntity> SAUCE_STATION_BE;
    /** 由平台在 {@link #register} 里注册并赋值。 */
    public static MenuType<SauceStationScreenHandler> SAUCE_STATION_SCREEN;

    /** 装着东西的锅物品：汤底、食材、熟度原样打包（格式同锅方块实体存档）。 */
    public static DataComponentType<CustomData> POT_CONTENTS;

    /** 独立锅方块仍保留，兼容未放在灶台上的旧世界与自由摆放。 */
    public static BlockEntityType<HotPotBlockEntity> HOT_POT_BE;

    /** 组合灶台方块实体：燃料、锅库存和烹饪数据全部在同一个位置。 */
    public static BlockEntityType<StoveBlockEntity> STOVE_BE;

    /** 条凳的方块实体（只用来画斜腿）。 */
    public static BlockEntityType<BenchBlockEntity> BENCH_BE;

    public static MenuType<HotPotScreenHandler> HOT_POT_SCREEN;

    /** 灶台底座的灶膛燃料界面。 */
    public static MenuType<StoveScreenHandler> STOVE_SCREEN;

    /** 本模组专属分类：使用 Fabric 的追加式标签页，自动排在原版标签页之后并进入后续页面。 */
    public static final ResourceKey<CreativeModeTab> ITEM_GROUP_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, HotPot.id("hotpot"));

    /** 由平台在 {@link #register} 里创建并赋值。 */
    public static CreativeModeTab ITEM_GROUP;

    /**
     * 创造栏里锅的顺序（用户 2026-10-24 定）：先铁锅全部、再铜锅全部，每种按
     * 九宫格 / S / O / 方心 / 直隔 / 田字。和注册用的 {@link #POT_ORDER} 刻意不同，所以单独排一份，
     * 别把注册顺序也一起改了（mryt233 改 2026-11-10）。
     */
    static final List<HotPotMaterial> CREATIVE_POT_ORDER = List.of(
        HotPotMaterial.IRON, HotPotMaterial.IRON_S, HotPotMaterial.IRON_O,
        HotPotMaterial.IRON_SQUARE, HotPotMaterial.IRON_SPLIT, HotPotMaterial.IRON_FOUR,
        HotPotMaterial.COPPER, HotPotMaterial.COPPER_S, HotPotMaterial.COPPER_O,
        HotPotMaterial.COPPER_SQUARE, HotPotMaterial.COPPER_SPLIT, HotPotMaterial.COPPER_FOUR);

    /** 创造栏里放什么：纯数据，平台只负责套上加载器的标签页外壳。 */
    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters context, CreativeModeTab.Output entries) {
                entries.accept(STOVE_ITEM);
                entries.accept(TALL_STOVE_ITEM);
                // 锅：先铁锅全部、再铜锅全部，每种按 九宫格 / S / O / 方心 / 直隔 / 田字（用户 2026-10-24 定）
                // mryt233 改（2026-11-10，r147）：12 行手写改成按创造栏专用顺序循环（逐项和以前一致）
                for (HotPotMaterial material : CREATIVE_POT_ORDER) {
                    entries.accept(POT_ITEMS.get(material));
                }
                entries.accept(CHOPSTICKS);
                entries.accept(RAW_TRIPE);
                entries.accept(COOKED_TRIPE);
                entries.accept(FATTY_BEEF_ROLL);
                entries.accept(COOKED_FATTY_BEEF_ROLL);
                entries.accept(MUTTON_ROLL);
                entries.accept(COOKED_MUTTON_ROLL);
                entries.accept(BEEF_LOIN);
                entries.accept(COOKED_BEEF_LOIN);
                entries.accept(BEEF_LOIN_SLICE);
                entries.accept(COOKED_BEEF_LOIN_SLICE);
                entries.accept(POTATO_SLICE);
                entries.accept(COOKED_POTATO_SLICE);
                entries.accept(SOYBEAN);
                entries.accept(COLD_TOFU);
                entries.accept(HOT_TOFU);
                entries.accept(COLD_TOFU_CUBE);
                entries.accept(HOT_TOFU_CUBE);
                for (Item ball : new Item[]{BEEF_MEATBALL, COOKED_BEEF_MEATBALL, CILANTRO_BEEF_MEATBALL, COOKED_CILANTRO_BEEF_MEATBALL, PORK_MEATBALL, COOKED_PORK_MEATBALL, CILANTRO_PORK_MEATBALL, COOKED_CILANTRO_PORK_MEATBALL, MUTTON_MEATBALL, COOKED_MUTTON_MEATBALL, CILANTRO_MUTTON_MEATBALL, COOKED_CILANTRO_MUTTON_MEATBALL, FISH_BALL, COOKED_FISH_BALL, CILANTRO_FISH_BALL, COOKED_CILANTRO_FISH_BALL}) {
                    entries.accept(ball);
                }
                for (Item shrimp : new Item[]{SHRIMP, COOKED_SHRIMP, PEELED_SHRIMP, COOKED_PEELED_SHRIMP, SHRIMP_PASTE, COOKED_SHRIMP_PASTE}) {
                    entries.accept(shrimp);
                }
                entries.accept(MINCED_PORK);
                entries.accept(RAW_HAM_SAUSAGE);
                entries.accept(HAM_SAUSAGE);
                entries.accept(HAM_SAUSAGE_SLICE);
                entries.accept(COOKED_HAM_SAUSAGE_SLICE);
                // mryt233 改（2026-11-06）：r137 的 22 样新食材，接在火腿肠片后面，按生 → 熟成对排列。
                for (Item food : new Item[]{RAW_CRISPY_PORK, CRISPY_PORK, BOILED_CRISPY_PORK,
                    DRIED_TOFU, COOKED_DRIED_TOFU, TOFU_SKIN, COOKED_TOFU_SKIN,
                    WIDE_NOODLES, COOKED_WIDE_NOODLES, SWEET_POTATO_SKIN, COOKED_SWEET_POTATO_SKIN,
                    ENOKI_MUSHROOM, COOKED_ENOKI_MUSHROOM, GREENS, COOKED_GREENS,
                    LOTUS_ROOT, LOTUS_ROOT_SLICE, COOKED_LOTUS_ROOT_SLICE,
                    RAW_SQUID, SQUID_ROLL, COOKED_SQUID_ROLL, EGG_FRIED_RICE}) {
                    entries.accept(food);
                }
                // mryt233 改（2026-11-06）：r139 折耳根四样（生 / 段 / 熟段 / 碎）接在 r137 食材后面，按生 → 熟的顺序。
                for (Item zheErGen : new Item[]{ZHE_ER_GEN, ZHE_ER_GEN_SLICE, COOKED_ZHE_ER_GEN_SLICE, MINCED_ZHE_ER_GEN}) {
                    entries.accept(zheErGen);
                }
                // 布围裙：防溅油装备，排在厨具（菜刀 / 擀面杖 / 砧板）后面
                // mryt233 改（2026-11-09，r146b）：三款布围裙都进创造栏
                for (Item apron : CLOTH_APRONS) {
                    entries.accept(apron);
                }
                // ==== tools/new_item.py：新物品进创造栏插在这行上面（别删这行）====
                entries.accept(HOTPOT_GUIDE);
                entries.accept(KITCHEN_KNIFE);
                entries.accept(ROLLING_PIN);
                entries.accept(CUTTING_BOARD_ITEM);
                SOUP_BOWLS.forEach(entries::accept);
                entries.accept(ORANGE_DRINK);
                entries.accept(EMPTY_ORANGE_DRINK_BOTTLE);
                entries.accept(PLANT_PROTEIN_DRINK);
                entries.accept(EMPTY_PLANT_PROTEIN_DRINK_BOTTLE);
                entries.accept(ORIENT_COLA);
                entries.accept(EMPTY_ORIENT_COLA_BOTTLE);
                entries.accept(GLASS_CUP);
                entries.accept(ORANGE_DRINK_CUP);
                entries.accept(PLANT_PROTEIN_DRINK_CUP);
                entries.accept(ORIENT_COLA_CUP);
                entries.accept(WATER_CUP);
                entries.accept(MILK_CUP);
                entries.accept(SCALLION);
                entries.accept(CILANTRO);
                entries.accept(ORANGE);
                entries.accept(GARLIC);
                entries.accept(GARLIC_PASTE);
                entries.accept(COOKED_KELP);
                entries.accept(TOMATO);
                entries.accept(CHILI_PEPPER);   // mryt233 改（2026-11-05）：辣椒（r136a）排在番茄后面
                // mryt233 改（2026-11-06）：r137 的 5 种蘸料原料，接在辣椒后面（都是能在蘸料台上用的原料）。
                for (Item raw : new Item[]{OYSTER_SAUCE, SESAME_PASTE, PEANUT, CRUSHED_PEANUTS, MILLET_CHILI}) {
                    entries.accept(raw);
                }
                entries.accept(TOMATO_HOTPOT_BASE);
                entries.accept(THREE_FRESH_HOTPOT_BASE);
                entries.accept(TOMATO_SOUP_BUCKET);
                entries.accept(THREE_FRESH_SOUP_BUCKET);
                // mryt233 改（2026-11-04）：不再放空的蘸料。默认物品没有 sauce 组件，显示「蘸料 · 还能蘸 0 口」，
                // 玩家截图报过；蘸料本来就只能在蘸料台上打，创造栏不放。
                // 万一还有地方拿到空蘸料，DippingSauceItem 会把它换成空碗（见那边的兜底）。
                HOTPOT_TABLE_ITEMS.forEach(entries::accept);
                BENCH_ITEMS.forEach(entries::accept);
                TALL_TABLE_ITEMS.forEach(entries::accept);
                TALL_BENCH_ITEMS.forEach(entries::accept);
                PLATE_ITEMS.forEach(entries::accept);
                SAUCE_STATION_ITEMS.forEach(entries::accept);
    }


    private HotPotContent() {
    }

    /**
     * mryt233 改（2026-11-10，r147）：盘子材质和 {@code content/materials.json} 的一致性核对。
     * Java 侧的颜色 / 声音现在是材质记录上的字段，脚本侧的同一份数据在 materials.json 的 {@code plates} 段；
     * 两边写岔了不会报错，只会让「生成脚本产出的贴图 / 模型」和「代码注册的属性」对不上。
     * 启动时按 ID 核对一遍：材质名单顺序、地图颜色、声音名，不一致就抛异常（而不是安静地用着错的）。
     *
     * <p>materials.json 不在运行时资源里（它是生成脚本的源数据，不进 jar），所以用不到的时候跳过；
     * 开发环境能读到就核对，正式包里读不到就跳过。</p>
     */
    public static void verifyPlateMaterials() {
        Path json = Path.of("content", "materials.json");
        if (!Files.isRegularFile(json)) {
            return;   // 正式包 / 工作目录不同：没有源数据可核，跳过
        }
        try {
            String text = Files.readString(json, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            JsonArray plates = root.getAsJsonArray("plates");
            if (plates == null) {
                throw new IllegalStateException("content/materials.json 缺少 plates 段");
            }
            if (plates.size() != PLATE_MATERIALS.size()) {
                throw new IllegalStateException("盘子材质数量不一致：Java " + PLATE_MATERIALS.size()
                    + " 种，materials.json " + plates.size() + " 种");
            }
            for (int i = 0; i < plates.size(); i++) {
                JsonObject entry = plates.get(i).getAsJsonObject();
                PlateMaterial material = PLATE_MATERIALS.get(i);
                String jsonId = entry.get("id").getAsString();
                if (!jsonId.equals(material.id())) {
                    throw new IllegalStateException("第 " + i + " 种盘子材质顺序不一致：Java " + material.id()
                        + "，materials.json " + jsonId);
                }
                String jsonSound = entry.has("sound") ? entry.get("sound").getAsString() : null;
                if (jsonSound != null && !jsonSound.equals(soundName(material.sounds()))) {
                    throw new IllegalStateException("盘子材质 " + material.id() + " 的声音不一致：Java "
                        + soundName(material.sounds()) + "，materials.json " + jsonSound);
                }
            }
        } catch (IOException error) {
            HotPot.LOGGER.warn("[hotpot] 读取 content/materials.json 失败，跳过盘子材质一致性核对", error);
        }
    }

    /** 声音类型的名字（和 materials.json 里的写法一致，见 tools/pixkit/materials.py）。 */
    private static String soundName(SoundType sounds) {
        for (Field field : SoundType.class.getFields()) {
            try {
                if (field.getType() == SoundType.class && field.get(null) == sounds) {
                    return field.getName();
                }
            } catch (IllegalAccessException ignored) {
                // 公开静态字段，取不到就继续
            }
        }
        return "unknown";
    }

    /** 旧调用入口保留；类初始化不再注册任何内容。 */
    @Deprecated
    public static void register() {
        register(Framework.platform().registration());
    }

    private static boolean registered;

    // mryt233 改（2026-11-10）：按原字段和静态块的顺序登记，静态字段保留原类型供外部直接读取。
    public static synchronized void register(RegistrationApi registration) {
        Objects.requireNonNull(registration);
        if (registered) throw new IllegalStateException("hotpot content already registered");
        registered = true;
        STOVE = registration.blocks().register(
            HotPot.id("brick_stove"), new StoveBlock());
        TALL_STOVE = registration.blocks().register(
            HotPot.id("tall_brick_stove"), new StoveBlock(StoveBlock.TALL_LIFT));
        STOVE_TOP = registration.blocks().register(
            HotPot.id("tall_brick_stove_top"), new StoveTopBlock());

        for (HotPotMaterial material : POT_ORDER) {
            POT_BLOCKS.put(material, registration.blocks().register(
                HotPot.id(material.idPath()), new HotPotBlock(material)));
        }
        HELD_FOOD = registration.components().register( HotPot.id("held_food"),
            DataComponentType.<HeldFood>builder()
                .persistent(HeldFood.CODEC)
                .networkSynchronized(HeldFood.STREAM_CODEC)
                .build());

        // mryt233 改（2026-11-10）：颜色和声音直接从材质记录上取，不再按名字写两个 switch
        for (PlateMaterial material : PLATE_MATERIALS) {
            for (PlateSize size : PlateSize.values()) {
                if (size == PlateSize.BOWL || size == PlateSize.DRINK || size == PlateSize.CUP) {
                    continue;   // 碗、橙子饮料不是盘子（只借用盘子格摆放）
                }
                String id = material.id() + "_" + size.itemId();   // ID 和显示名对得上（r102）：quartz_small_dish 等
                PlateBlock plate = registration.blocks().register( HotPot.id(id),
                    new PlateBlock(material.id(), size, material.mapColor(), material.sounds()));
                PLATES.add(plate);
                PLATE_ITEMS.add(registration.items().register( HotPot.id(id),
                    new PlateItem(plate, new Item.Properties().stacksTo(16))));
            }
        }
        BOWL_BLOCK = registration.blocks().register(
            HotPot.id("bowl"), new BowlBlock());
        DRINK_BLOCK = registration.blocks().register(
            HotPot.id("drink"), new DrinkBlock());
        CUP_BLOCK = registration.blocks().register(
            HotPot.id("cup"), new CupBlock());
        SAUCE_BOTTLE_BLOCK = registration.blocks().register(
            HotPot.id("sauce_bottle"), new SauceBottleBlock());
        CHOPSTICK_REST_BLOCK = registration.blocks().register(
            HotPot.id("chopstick_rest"), new ChopstickRestBlock());
        DRINK_CAULDRON = registration.blocks().register(
            HotPot.id("drink_cauldron"), new DrinkCauldronBlock());
        PLATE_HOLDER = registration.blocks().register(
            HotPot.id("plates"), new PlateHolderBlock());
        SEAT = registration.entityTypes().register( HotPot.id("seat"),
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.01F)
                .noSave()
                .noSummon()
                .clientTrackingRange(10)
                .build(HotPot.id("seat").toString()));

        for (String wood : TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(wood + "_planks"));
            HotPotTableBlock table = registration.blocks().register(
                HotPot.id(wood + "_hotpot_table"), new HotPotTableBlock(planks));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_hotpot_table"), new BlockItem(table, new Item.Properties())));
        }
        for (String wood : TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(wood + "_planks"));
            BenchBlock bench = registration.blocks().register(
                HotPot.id(wood + "_stool"), new BenchBlock(planks));   // 板凳 = Stool（r102 改 ID）
            BENCHES.add(bench);
            BENCH_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_stool"), new BlockItem(bench, new Item.Properties())));
        }
        for (String wood : TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(wood + "_planks"));
            TallHotPotTableBlock table = registration.blocks().register(
                HotPot.id(wood + "_tall_hotpot_table"), new TallHotPotTableBlock(planks));
            TALL_TABLES.add(table);
            TALL_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_tall_hotpot_table"), new BlockItem(table, new Item.Properties())));
            BenchBlock bench = registration.blocks().register(
                HotPot.id(wood + "_bench"), new BenchBlock(planks, true));   // 条凳 = Bench
            TALL_BENCHES.add(bench);
            TALL_BENCH_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_bench"), new BlockItem(bench, new Item.Properties())));
        }
        // 石料 / 金属桌面的火锅桌（2026-10-26 用户：平滑石头、磨制安山岩……铁块；桌面是这种材质，下面红砖）：
        // 矮桌、高桌各一张，模型等由 tools/gen_stone_tables.py 生成，ID = <材质>_hotpot_table / <材质>_tall_hotpot_table。
        // 声音、地图颜色取那种原版方块；不会着火。新增
        // mryt233 改（2026-11-05）：石料 / 金属桌面传 true，硬度 2.0、抗性 6.0，空手也能挖（以前照铁块 5.0 太慢）。
        for (String[] m : STONE_TABLES) {
            Block material = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(m[1]));
            HotPotTableBlock table = registration.blocks().register(
                HotPot.id(m[0] + "_hotpot_table"), new HotPotTableBlock(material, true));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(m[0] + "_hotpot_table"), new BlockItem(table, new Item.Properties())));
            TallHotPotTableBlock tall = registration.blocks().register(
                HotPot.id(m[0] + "_tall_hotpot_table"), new TallHotPotTableBlock(material, true));
            TALL_TABLES.add(tall);
            TALL_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(m[0] + "_tall_hotpot_table"), new BlockItem(tall, new Item.Properties())));
        }
        STOVE_ITEM = registration.items().register(
            HotPot.id("brick_stove"), new BlockItem(STOVE, new Item.Properties()));
        TALL_STOVE_ITEM = registration.items().register(
            HotPot.id("tall_brick_stove"), new BlockItem(TALL_STOVE, new Item.Properties()));

        for (HotPotMaterial material : POT_ORDER) {
            POT_ITEMS.put(material, registration.items().register(
                HotPot.id(material.idPath()),
                new BlockItem(POT_BLOCKS.get(material), new Item.Properties())));
        }
        COPPER_HOT_POT = POT_BLOCKS.get(HotPotMaterial.COPPER);
        IRON_HOT_POT = POT_BLOCKS.get(HotPotMaterial.IRON);
        COPPER_S_POT = POT_BLOCKS.get(HotPotMaterial.COPPER_S);
        COPPER_O_POT = POT_BLOCKS.get(HotPotMaterial.COPPER_O);
        IRON_S_POT = POT_BLOCKS.get(HotPotMaterial.IRON_S);
        IRON_O_POT = POT_BLOCKS.get(HotPotMaterial.IRON_O);
        COPPER_SQUARE_POT = POT_BLOCKS.get(HotPotMaterial.COPPER_SQUARE);
        IRON_SQUARE_POT = POT_BLOCKS.get(HotPotMaterial.IRON_SQUARE);
        COPPER_SPLIT_POT = POT_BLOCKS.get(HotPotMaterial.COPPER_SPLIT);
        IRON_SPLIT_POT = POT_BLOCKS.get(HotPotMaterial.IRON_SPLIT);
        COPPER_FOUR_POT = POT_BLOCKS.get(HotPotMaterial.COPPER_FOUR);
        IRON_FOUR_POT = POT_BLOCKS.get(HotPotMaterial.IRON_FOUR);
        COPPER_HOT_POT_ITEM = POT_ITEMS.get(HotPotMaterial.COPPER);
        IRON_HOT_POT_ITEM = POT_ITEMS.get(HotPotMaterial.IRON);
        COPPER_S_POT_ITEM = POT_ITEMS.get(HotPotMaterial.COPPER_S);
        COPPER_O_POT_ITEM = POT_ITEMS.get(HotPotMaterial.COPPER_O);
        IRON_S_POT_ITEM = POT_ITEMS.get(HotPotMaterial.IRON_S);
        IRON_O_POT_ITEM = POT_ITEMS.get(HotPotMaterial.IRON_O);
        COPPER_SQUARE_POT_ITEM = POT_ITEMS.get(HotPotMaterial.COPPER_SQUARE);
        IRON_SQUARE_POT_ITEM = POT_ITEMS.get(HotPotMaterial.IRON_SQUARE);
        COPPER_SPLIT_POT_ITEM = POT_ITEMS.get(HotPotMaterial.COPPER_SPLIT);
        IRON_SPLIT_POT_ITEM = POT_ITEMS.get(HotPotMaterial.IRON_SPLIT);
        COPPER_FOUR_POT_ITEM = POT_ITEMS.get(HotPotMaterial.COPPER_FOUR);
        IRON_FOUR_POT_ITEM = POT_ITEMS.get(HotPotMaterial.IRON_FOUR);
        RAW_TRIPE = foodItem(registration, "tripe", 2, 0.3F);
        COOKED_TRIPE = foodItem(registration, "cooked_tripe", 5, 0.6F);
        FATTY_BEEF_ROLL = foodItem(registration, "fatty_beef_roll", 1, 0.3F);
        COOKED_FATTY_BEEF_ROLL = foodItem(registration, "cooked_fatty_beef_roll", 3, 0.6F);
        MUTTON_ROLL = foodItem(registration, "mutton_roll", 1, 0.3F);
        COOKED_MUTTON_ROLL = foodItem(registration, "cooked_mutton_roll", 3, 0.6F);
        BEEF_LOIN = foodItem(registration, "beef_loin", 3, 0.3F);
        COOKED_BEEF_LOIN = foodItem(registration, "cooked_beef_loin", 8, 0.8F);
        BEEF_LOIN_SLICE = foodItem(registration, "beef_loin_slice", 1, 0.3F);
        COOKED_BEEF_LOIN_SLICE = foodItem(registration, "cooked_beef_loin_slice", 3, 0.6F);
        KITCHEN_KNIFE = registration.items().register( HotPot.id("iron_kitchen_knife"),
            new Item(new Item.Properties().durability(250).attributes(
                SwordItem.createAttributes(Tiers.IRON, 1, -2.2F))));
        CUTTING_BOARD = registration.blocks().register( HotPot.id("cutting_board"),
            new CuttingBoardBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()
                .dynamicShape()));
        CUTTING_BOARD_ITEM = registration.items().register( HotPot.id("cutting_board"),
            new BlockItem(CUTTING_BOARD, new Item.Properties()));
        POTATO_SLICE = foodItem(registration, "potato_slice", 1, 0.3F);
        COOKED_POTATO_SLICE = foodItem(registration, "cooked_potato_slice", 2, 0.6F);
        SOYBEAN = foodItem(registration, "soybean", 1, 0.1F);
        COLD_TOFU = foodItem(registration, "cold_tofu", 2, 0.4F);
        HOT_TOFU = foodItem(registration, "hot_tofu", 2, 0.4F);
        COLD_TOFU_CUBE = foodItem(registration, "cold_tofu_cube", 1, 0.3F);
        ORANGE = foodItem(registration, "orange", 4, 0.3F);
        GARLIC = foodItem(registration, "garlic", 1, 0.1F);
        GARLIC_PASTE = foodItem(registration, "garlic_paste", 1, 0.1F);
        SHRIMP = foodItem(registration, "shrimp", 1, 0.2F);
        COOKED_SHRIMP = foodItem(registration, "cooked_shrimp", 3, 0.5F);
        PEELED_SHRIMP = foodItem(registration, "peeled_shrimp", 1, 0.2F);
        COOKED_PEELED_SHRIMP = foodItem(registration, "cooked_peeled_shrimp", 3, 0.6F);
        SHRIMP_PASTE = foodItem(registration, "shrimp_paste", 1, 0.2F);
        COOKED_SHRIMP_PASTE = foodItem(registration, "cooked_shrimp_paste", 4, 0.6F);
        MINCED_PORK = registration.items().register( HotPot.id("minced_pork"),
            new Item(new Item.Properties()));
        RAW_HAM_SAUSAGE = foodItem(registration, "raw_ham_sausage", 1, 0.3F);
        HAM_SAUSAGE = foodItem(registration, "ham_sausage", 3, 0.6F);
        HAM_SAUSAGE_SLICE = foodItem(registration, "ham_sausage_slice", 1, 0.3F);
        COOKED_HAM_SAUSAGE_SLICE = foodItem(registration, "cooked_ham_sausage_slice", 2, 0.5F);
        OYSTER_SAUCE = sauceBottle(registration, SauceBottleKind.OYSTER_SAUCE, 1, 0.2F);
        SESAME_PASTE = sauceBottle(registration, SauceBottleKind.SESAME_PASTE, 2, 0.3F);
        PEANUT = foodItem(registration, "peanut", 1, 0.2F);
        CRUSHED_PEANUTS = foodItem(registration, "crushed_peanuts", 1, 0.2F);
        MILLET_CHILI = foodItem(registration, "millet_chili", 1, 0.1F);
        RAW_CRISPY_PORK = foodItem(registration, "raw_crispy_pork", 1, 0.3F);
        CRISPY_PORK = foodItem(registration, "crispy_pork", 4, 0.6F);
        BOILED_CRISPY_PORK = foodItem(registration, "boiled_crispy_pork", 6, 0.8F);
        DRIED_TOFU = foodItem(registration, "dried_tofu", 2, 0.3F);
        COOKED_DRIED_TOFU = foodItem(registration, "cooked_dried_tofu", 4, 0.6F);
        TOFU_SKIN = foodItem(registration, "tofu_skin", 2, 0.3F);
        COOKED_TOFU_SKIN = foodItem(registration, "cooked_tofu_skin", 4, 0.6F);
        WIDE_NOODLES = foodItem(registration, "wide_noodles", 1, 0.2F);
        COOKED_WIDE_NOODLES = foodItem(registration, "cooked_wide_noodles", 4, 0.6F);
        SWEET_POTATO_SKIN = foodItem(registration, "sweet_potato_skin", 1, 0.2F);
        COOKED_SWEET_POTATO_SKIN = foodItem(registration, "cooked_sweet_potato_skin", 4, 0.6F);
        ENOKI_MUSHROOM = foodItem(registration, "enoki_mushroom", 1, 0.2F);
        COOKED_ENOKI_MUSHROOM = foodItem(registration, "cooked_enoki_mushroom", 3, 0.5F);
        GREENS = foodItem(registration, "greens", 1, 0.2F);
        COOKED_GREENS = foodItem(registration, "cooked_greens", 3, 0.5F);
        LOTUS_ROOT = foodItem(registration, "lotus_root", 2, 0.3F);
        LOTUS_ROOT_SLICE = foodItem(registration, "lotus_root_slice", 1, 0.2F);
        COOKED_LOTUS_ROOT_SLICE = foodItem(registration, "cooked_lotus_root_slice", 3, 0.6F);
        RAW_SQUID = foodItem(registration, "raw_squid", 2, 0.2F);
        SQUID_ROLL = foodItem(registration, "squid_roll", 1, 0.2F);
        COOKED_SQUID_ROLL = foodItem(registration, "cooked_squid_roll", 4, 0.6F);
        EGG_FRIED_RICE = foodItem(registration, "egg_fried_rice", 7, 0.8F);
        ZHE_ER_GEN = foodItem(registration, "zhe_er_gen", 1, 0.15F);
        ZHE_ER_GEN_SLICE = foodItem(registration, "zhe_er_gen_slice", 1, 0.15F);
        COOKED_ZHE_ER_GEN_SLICE = foodItem(registration, "cooked_zhe_er_gen_slice", 3, 0.5F);
        MINCED_ZHE_ER_GEN = foodItem(registration, "minced_zhe_er_gen", 1, 0.1F);
        CLOTH_APRON = registration.items().register( HotPot.id("cloth_apron"),
            new ClothApronItem(registration, ClothApronItem.Style.LINEN));
        BLUE_PRINT_CLOTH_APRON = registration.items().register(
            HotPot.id("blue_print_cloth_apron"), new ClothApronItem(registration, ClothApronItem.Style.BLUE_PRINT));
        BROWN_TRIM_CLOTH_APRON = registration.items().register(
            HotPot.id("brown_trim_cloth_apron"), new ClothApronItem(registration, ClothApronItem.Style.BROWN_TRIM));
        CLOTH_APRONS = List.of(
            CLOTH_APRON, BLUE_PRINT_CLOTH_APRON, BROWN_TRIM_CLOTH_APRON);
        // ==== tools/new_item.py：新物品的登记赋值插在这行上面（mryt233 2026-11-10）====
        TOMATO = foodItem(registration, "tomato", 2, 0.3F);
        CHILI_PEPPER = foodItem(registration, "chili_pepper", 1, 0.1F);
        HOTPOT_GUIDE = registration.items().register( HotPot.id("hotpot_guide"), new GuideBookItem());
        TOMATO_HOTPOT_BASE = registration.items().register( HotPot.id("tomato_hotpot_base"),
            new Item(new Item.Properties()));
        THREE_FRESH_HOTPOT_BASE = registration.items().register( HotPot.id("three_fresh_hotpot_base"),
            new Item(new Item.Properties()));
        TOMATO_SOUP_BUCKET = registration.items().register( HotPot.id("tomato_soup_bucket"),
            new Item(new Item.Properties().stacksTo(1)));
        THREE_FRESH_SOUP_BUCKET = registration.items().register( HotPot.id("three_fresh_soup_bucket"),
            new Item(new Item.Properties().stacksTo(1)));
        COOKED_KELP = foodItem(registration, "cooked_kelp", 2, 0.3F);
        HOT_TOFU_CUBE = foodItem(registration, "hot_tofu_cube", 2, 0.6F);
        ROLLING_PIN = registration.items().register( HotPot.id("rolling_pin"),
            new Item(new Item.Properties().durability(120)));
        BEEF_MEATBALL = foodItem(registration, "beef_meatball", 1, 0.3F);
        PORK_MEATBALL = foodItem(registration, "pork_meatball", 1, 0.3F);
        MUTTON_MEATBALL = foodItem(registration, "mutton_meatball", 1, 0.3F);
        FISH_BALL = foodItem(registration, "fish_ball", 1, 0.3F);
        CILANTRO_BEEF_MEATBALL = foodItem(registration, "cilantro_beef_meatball", 1, 0.3F);
        CILANTRO_PORK_MEATBALL = foodItem(registration, "cilantro_pork_meatball", 1, 0.3F);
        CILANTRO_MUTTON_MEATBALL = foodItem(registration, "cilantro_mutton_meatball", 1, 0.3F);
        CILANTRO_FISH_BALL = foodItem(registration, "cilantro_fish_ball", 1, 0.3F);
        COOKED_BEEF_MEATBALL = foodItem(registration, "cooked_beef_meatball", 3, 0.6F);
        COOKED_PORK_MEATBALL = foodItem(registration, "cooked_pork_meatball", 3, 0.6F);
        COOKED_MUTTON_MEATBALL = foodItem(registration, "cooked_mutton_meatball", 3, 0.6F);
        COOKED_FISH_BALL = foodItem(registration, "cooked_fish_ball", 3, 0.6F);
        COOKED_CILANTRO_BEEF_MEATBALL = foodItem(registration, "cooked_cilantro_beef_meatball", 4, 0.6F);
        COOKED_CILANTRO_PORK_MEATBALL = foodItem(registration, "cooked_cilantro_pork_meatball", 4, 0.6F);
        COOKED_CILANTRO_MUTTON_MEATBALL = foodItem(registration, "cooked_cilantro_mutton_meatball", 4, 0.6F);
        COOKED_CILANTRO_FISH_BALL = foodItem(registration, "cooked_cilantro_fish_ball", 4, 0.6F);
        POUNDED_MEAT = registration.items().register( HotPot.id("pounded_meat"),
            new Item(new Item.Properties()));
        // mryt233 改（2026-11-11，4B-2）：内置汤底从登记表取（前 6 项，顺序 = 旧序号），汤碗物品持有定义。
        SOUP_BOWLS = Soups.builtins().stream()
            .map(soup -> registration.items().register( HotPot.id(soup.bowlId()),
                new SoupBowlItem(soup)))
            .toList();
        SOUP_BOWL = registration.items().register(HotPot.id("soup_bowl"), new SoupBowlItem());
        ORANGE_DRINK = registration.items().register(
            HotPot.id("orange_drink"), new DrinkItem(DrinkKind.ORANGE_DRINK));
        EMPTY_ORANGE_DRINK_BOTTLE = registration.items().register(
            HotPot.id("empty_orange_drink_bottle"), new EmptyBottleItem(DrinkKind.ORANGE_DRINK));
        PLANT_PROTEIN_DRINK = registration.items().register(
            HotPot.id("plant_protein_drink"), new DrinkItem(DrinkKind.PLANT_PROTEIN_DRINK));
        EMPTY_PLANT_PROTEIN_DRINK_BOTTLE = registration.items().register(
            HotPot.id("empty_plant_protein_drink_bottle"), new EmptyBottleItem(DrinkKind.PLANT_PROTEIN_DRINK));
        ORIENT_COLA = registration.items().register(
            HotPot.id("orient_cola"), new DrinkItem(DrinkKind.ORIENT_COLA));
        EMPTY_ORIENT_COLA_BOTTLE = registration.items().register(
            HotPot.id("empty_orient_cola_bottle"), new EmptyBottleItem(DrinkKind.ORIENT_COLA));
        GLASS_CUP = registration.items().register( HotPot.id("glass_cup"), new CupItem(null));
        ORANGE_DRINK_CUP = registration.items().register( HotPot.id("orange_drink_cup"),
            new CupItem(DrinkKind.ORANGE_DRINK));
        PLANT_PROTEIN_DRINK_CUP = registration.items().register(
            HotPot.id("plant_protein_drink_cup"), new CupItem(DrinkKind.PLANT_PROTEIN_DRINK));
        ORIENT_COLA_CUP = registration.items().register( HotPot.id("orient_cola_cup"),
            new CupItem(DrinkKind.ORIENT_COLA));
        WATER_CUP = registration.items().register( HotPot.id("water_cup"),
            new CupItem(DrinkKind.WATER));
        MILK_CUP = registration.items().register( HotPot.id("milk_cup"),
            new CupItem(DrinkKind.MILK));
        CHOPSTICKS = registration.items().register(
            HotPot.id("bamboo_chopsticks"), new ChopsticksItem());
        SCALLION = foodItem(registration, "scallion", 1, 0.1F);
        CILANTRO = foodItem(registration, "cilantro", 1, 0.1F);
        SAUCE = registration.components().register( HotPot.id("sauce"),
            DataComponentType.<SauceContents>builder().persistent(SauceContents.CODEC).networkSynchronized(SauceContents.PACKET_CODEC).build());
        DIPPED = registration.components().register( HotPot.id("dipped"),
            DataComponentType.<java.util.List<String>>builder().persistent(SauceContents.DIPPED_CODEC)
                .networkSynchronized(SauceContents.DIPPED_PACKET_CODEC).build());   // mryt233 改（4E-1）
        COOKED_IN_SOUP = registration.components().register(HotPot.id("cooked_in_soup"),
            DataComponentType.<String>builder().persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        SOUP = registration.components().register(HotPot.id("soup"),
            DataComponentType.<String>builder().persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        SPICY_LEVEL = registration.components().register(HotPot.id("spicy_level"),
            DataComponentType.<Integer>builder().persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT).build());
        CHEERED = registration.components().register(HotPot.id("cheered"),
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL).build());
        DIPPING_SAUCE = registration.items().register(
            HotPot.id("dipping_sauce"), new DippingSauceItem());

        List<String> bodies = new ArrayList<>(TABLE_WOODS);
        bodies.add("iron");
        for (String body : bodies) {
            boolean iron = body.equals("iron");
            Block like = iron ? Blocks.IRON_BLOCK
                : BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(body + "_planks"));
            // mryt233 改（2026-11-05）：属性统一由 SauceStationBlock.settingsFor 给，木头 2.0/3.0、铁 2.0/6.0，都不强制工具。
            SauceStationBlock station = registration.blocks().register( HotPot.id(body + "_sauce_station"),
                new SauceStationBlock(SauceStationBlock.settingsFor(like, iron)));
            SAUCE_STATIONS.add(station);
            SAUCE_STATION_ITEMS.add(registration.items().register( HotPot.id(body + "_sauce_station"),
                new BlockItem(station, new Item.Properties())));
        }
        SAUCE_STATION_TOP = registration.blocks().register(
            HotPot.id("sauce_station_top"), new SauceStationTopBlock());
        POT_CONTENTS = registration.components().register( HotPot.id("pot_contents"),
            DataComponentType.<CustomData>builder()
                .persistent(CustomData.CODEC)
                .networkSynchronized(CustomData.STREAM_CODEC)
                .build());
        // 创造栏 / 菜单 / 方块实体类型统一走分域 Registrar（P1 第三批）
        ITEM_GROUP = registration.creativeTabs().register(ITEM_GROUP_KEY.location(),
            Component.translatable("itemGroup." + HotPot.MOD_ID),
            HotPotContent::tabIcon, HotPotContent::fillCreativeTab);
        HOT_POT_SCREEN = registration.menus().register(HotPot.id("hot_pot"),
            (syncId, inventory, pos) -> new HotPotScreenHandler(syncId, inventory, pos));
        STOVE_SCREEN = registration.menus().register(HotPot.id("stove_fuel"),
            (syncId, inventory, pos) -> new StoveScreenHandler(syncId, inventory, pos));
        SAUCE_STATION_SCREEN = registration.menus().register(HotPot.id("sauce_station"),
            (syncId, inventory, pos) -> new SauceStationScreenHandler(syncId, inventory, pos));
        PLATE_HOLDER_BE = registration.blockEntities().register(HotPot.id("plates"), PlateHolderBlockEntity::new, PLATE_HOLDER);
        CUTTING_BOARD_BE = registration.blockEntities().register(HotPot.id("cutting_board"), CuttingBoardBlockEntity::new, CUTTING_BOARD);
        SAUCE_STATION_BE = registration.blockEntities().register(HotPot.id("sauce_station"), SauceStationBlockEntity::new,
            SAUCE_STATIONS.toArray(new Block[0]));
        // mryt233 改（2026-11-10，r147）：方块实体类型挂哪些锅也从 POT_ORDER 来（顺序照旧）
        HOT_POT_BE = registration.blockEntities().register(HotPot.id("hot_pot"), HotPotBlockEntity::new,
            POT_ORDER.stream().map(m -> POT_BLOCKS.get(m)).toArray(Block[]::new));
        STOVE_BE = registration.blockEntities().register(HotPot.id("stove"), StoveBlockEntity::new, STOVE, TALL_STOVE);
        BENCH_BE = registration.blockEntities().register(HotPot.id("bench"), BenchBlockEntity::new,
            TALL_BENCHES.toArray(new Block[0]));
        // 触碰静态字段，明确让物品组在客户端和服务端都完成初始化。
        ITEM_GROUP.getDisplayName();
        // mryt233 改（2026-11-10，r147）：核对盘子材质和生成脚本源数据一致（漏写 / 写岔直接报错）
        verifyPlateMaterials();
        TripeLoot.register();
        GarnishLoot.register();
        OrangeLoot.register();
        DrinkCauldronBlock.registerEmptyCauldron();
        // mryt233 改（2026-11-11）：四条锅专用煮法和豆腐久煮迁至数据配方，配置默认值保留。
        Config.rememberTofuCooking(TOFU_COOK_TICKS, TOFU_OVERCOOK_TICKS);
        HotPotRecipes.register(registration);
        HotPotCriterion.register(registration);
        SoupBucketRecipe.register(registration);
        // mryt233 改（2026-11-09，r146b）：布围裙换款配方（任意一款 + 染料 → 指定款，保留耐久 / 附魔 / 名字）
        ApronRecolorRecipe.register(registration);
        HotPot.LOGGER.info("[Orient的火锅物语] 内容已注册（组合灶台 + 铜锅 + 铁锅 + 独立分类）");
    }

    /**
     * 分类页图标：铁锅 + 沸腾的红汤 + 漂着几样原版食物。用自定义模型数据 1 切到
     * models/item/iron_hot_pot_tab_icon.json，普通铁锅物品不受影响。
     */
    public static ItemStack tabIcon() {
        ItemStack icon = new ItemStack(IRON_HOT_POT_ITEM);
        icon.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(1));
        return icon;
    }

    /**
     * mryt233 改（2026-11-10，r147）：12 路 switch 改成查 {@link #POT_BLOCKS}。
     * 加一种锅型只要在 {@link #POT_ORDER} 和 {@link HotPotMaterial} 里各加一个值。
     */
    public static HotPotBlock blockFor(HotPotMaterial material) {
        HotPotBlock block = POT_BLOCKS.get(material);
        if (block == null) {
            throw new IllegalStateException("锅材质 " + material + " 没有注册方块；加锅型时要在 POT_ORDER 里补上。");
        }
        return block;
    }

    public static BlockItem itemFor(HotPotMaterial material) {
        BlockItem item = POT_ITEMS.get(material);
        if (item == null) {
            throw new IllegalStateException("锅材质 " + material + " 没有注册物品；加锅型时要在 POT_ORDER 里补上。");
        }
        return item;
    }

    public static ResourceLocation id(String path) {
        return HotPot.id(path);
    }
}
