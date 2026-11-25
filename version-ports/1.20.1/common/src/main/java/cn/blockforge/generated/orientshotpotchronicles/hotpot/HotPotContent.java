package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.compat.CustomData;
import cn.blockforge.orient.framework.compat.CustomModelData;
import cn.blockforge.orient.framework.compat.DataComponents;
import cn.blockforge.orient.framework.compat.ItemData;
import cn.blockforge.orient.framework.compat.ItemDataKey;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;
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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.material.MapColor;

/** 作者：mryt233；mryt233 改（2026-11-10）。注册中心：灶台、锅、筷子、方块实体、屏幕与独立创造标签页（1.20.1 适配）。 */
public final class HotPotContent {
    /** 灶台本身就是组合方块的宿主；锅放上去只改变 POT 状态。 */
    public static StoveBlock STOVE;

    /** 高灶台：配 1.5 格高火锅桌，灶口、锅、汤整体抬高 {@link StoveBlock#TALL_LIFT} 像素，灶膛还在最下面。 */
    public static StoveBlock TALL_STOVE;
    /** 高灶台的「上半截」：看不见，让伸进上面一格的锅也点得到（没有物品）。 */
    public static StoveTopBlock STOVE_TOP;

    /**
     * mryt233 改（2026-11-10，r147）：12 种锅的方块 / 物品改成 {@link EnumMap} + 循环注册。
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
    /** 0.3.1 Phase 4：40→2 统一基础火锅桌与 3 种备菜架（支持换材质、旧档无损迁移）。 */
    public static HotPotTableBlock HOTPOT_TABLE;
    public static BlockItem HOTPOT_TABLE_ITEM;
    public static TallHotPotTableBlock TALL_HOTPOT_TABLE;
    public static BlockItem TALL_HOTPOT_TABLE_ITEM;
    public static ItemDataKey<TableMaterialData> TABLE_MATERIALS;
    public static BlockEntityType<HotPotTableBlockEntity> HOTPOT_TABLE_BE;
    public static PrepRackBlock MULTI_TIER_PREP_RACK;
    public static BlockItem MULTI_TIER_PREP_RACK_ITEM;
    public static PrepRackBlock DESKTOP_PREP_RACK;
    public static BlockItem DESKTOP_PREP_RACK_ITEM;
    public static PrepRackBlock HANGING_PREP_RACK;
    public static BlockItem HANGING_PREP_RACK_ITEM;
    public static BlockEntityType<PrepRackBlockEntity> PREP_RACK_BE;
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
    /** 筷子上夹着的那一样食物（1.20.1 ItemStack NBT 垫片键）。 */
    public static ItemDataKey<HeldFood> HELD_FOOD;

    /** 摆在桌上的碗（只带模型，没有物品；r99）。 */
    public static BowlBlock BOWL_BLOCK;
    /** 摆在桌上的橙子饮料（只带模型，r99）。 */
    public static DrinkBlock DRINK_BLOCK;
    /** 摆在桌上的玻璃杯（只带模型，2026-10-25）。 */
    public static CupBlock CUP_BLOCK;
    /** 摆在桌上的芝麻酱 / 蚝油瓶（只带模型，r146a）。方块状态 kind 决定画哪种瓶子。 */
    public static SauceBottleBlock SAUCE_BOTTLE_BLOCK;
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

    public static Item RAW_TRIPE;
    public static Item COOKED_TRIPE;

    public static Item FATTY_BEEF_ROLL;
    public static Item COOKED_FATTY_BEEF_ROLL;
    public static Item MUTTON_ROLL;
    public static Item COOKED_MUTTON_ROLL;
    public static Item BEEF_LOIN;
    public static Item COOKED_BEEF_LOIN;
    public static Item BEEF_LOIN_SLICE;
    public static Item COOKED_BEEF_LOIN_SLICE;
    public static Item KITCHEN_KNIFE;
    public static CuttingBoardBlock CUTTING_BOARD;
    public static BlockItem CUTTING_BOARD_ITEM;
    public static BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD_BE;

    public static Item POTATO_SLICE;
    public static Item COOKED_POTATO_SLICE;
    public static Item SOYBEAN;
    public static Item COLD_TOFU;
    public static Item HOT_TOFU;
    public static Item COLD_TOFU_CUBE;
    /** 豆腐在锅里煮成热豆腐的刻数（10 秒），以及从下锅算起煮过头的总刻数（30 秒）；都能在配置里改。 */
    public static final int TOFU_COOK_TICKS = 200;
    public static final int TOFU_OVERCOOK_TICKS = 600;
    public static Item ORANGE;
    public static Item GARLIC;
    public static Item GARLIC_PASTE;
    public static Item SHRIMP;
    public static Item COOKED_SHRIMP;
    public static Item PEELED_SHRIMP;
    public static Item COOKED_PEELED_SHRIMP;
    public static Item SHRIMP_PASTE;
    public static Item COOKED_SHRIMP_PASTE;
    public static Item MINCED_PORK;
    public static Item RAW_HAM_SAUSAGE;
    public static Item HAM_SAUSAGE;
    public static Item HAM_SAUSAGE_SLICE;
    public static Item COOKED_HAM_SAUSAGE_SLICE;

    public static SauceBottleItem OYSTER_SAUCE;
    public static SauceBottleItem SESAME_PASTE;
    public static Item PEANUT;
    public static Item CRUSHED_PEANUTS;
    public static Item MILLET_CHILI;

    public static Item RAW_CRISPY_PORK;
    public static Item CRISPY_PORK;
    public static Item BOILED_CRISPY_PORK;

    public static Item DRIED_TOFU;
    public static Item COOKED_DRIED_TOFU;
    public static Item TOFU_SKIN;
    public static Item COOKED_TOFU_SKIN;

    public static Item WIDE_NOODLES;
    public static Item COOKED_WIDE_NOODLES;
    public static Item SWEET_POTATO_SKIN;
    public static Item COOKED_SWEET_POTATO_SKIN;

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

    public static Item ZHE_ER_GEN;
    public static Item ZHE_ER_GEN_SLICE;
    public static Item COOKED_ZHE_ER_GEN_SLICE;
    public static Item MINCED_ZHE_ER_GEN;
    public static Item CLOTH_APRON;
    public static Item BLUE_PRINT_CLOTH_APRON;
    public static Item BROWN_TRIM_CLOTH_APRON;
    /** 三款布围裙（创造栏 / 换款配方 / 成就共用）。 */
    public static List<Item> CLOTH_APRONS;

    /** 鸡爪（成年鸡掉落）、熟鸡爪、无骨鸡爪（砧板/切石机剔骨）、熟无骨鸡爪。 */
    public static Item CHICKEN_FEET;
    public static Item COOKED_CHICKEN_FEET;
    public static Item BONELESS_CHICKEN_FEET;
    public static Item COOKED_BONELESS_CHICKEN_FEET;

    /** 独立橙子树方块族（0.3.1 Phase 2.1）：原版橡木不再掉落橙子。 */
    public static OrangeLogBlock ORANGE_LOG;
    public static OrangeLogBlock ORANGE_WOOD;
    public static RotatedPillarBlock STRIPPED_ORANGE_LOG;
    public static RotatedPillarBlock STRIPPED_ORANGE_WOOD;
    public static OrangeLeavesBlock ORANGE_LEAVES;
    public static OrangeSaplingBlock ORANGE_SAPLING;
    public static FoliagePlacerType<OrangeFoliagePlacer> ORANGE_FOLIAGE_PLACER;
    public static BlockItem ORANGE_LOG_ITEM;
    public static BlockItem ORANGE_WOOD_ITEM;
    public static BlockItem STRIPPED_ORANGE_LOG_ITEM;
    public static BlockItem STRIPPED_ORANGE_WOOD_ITEM;
    public static BlockItem ORANGE_LEAVES_ITEM;
    public static BlockItem ORANGE_SAPLING_ITEM;

    /** 小石磨与开口大木桶：豆制品 2～3 步细化工序与干货泡发（0.3.1 Phase 3）。 */
    public static StoneMillBlock STONE_MILL;
    public static BlockItem STONE_MILL_ITEM;
    public static BlockEntityType<StoneMillBlockEntity> STONE_MILL_BE;
    public static WoodenVatBlock WOODEN_VAT;
    public static BlockItem WOODEN_VAT_ITEM;
    public static BlockEntityType<WoodenVatBlockEntity> WOODEN_VAT_BE;
    public static SoymilkBucketItem SOYMILK_BUCKET;
    public static Item BRINE_BOTTLE;
    public static Item WOODEN_PRESS_LID;

    /** 9 种独立耕地作物（直接以同名食材成品播种，不新增多余种子物品）与野外自然生成植株簇（0.3.1 Phase 2.2）。 */
    public static HotPotCropBlock SCALLION_CROP;
    public static HotPotCropBlock CILANTRO_CROP;
    public static HotPotCropBlock SOYBEAN_CROP;
    public static HotPotCropBlock GARLIC_CROP;
    public static HotPotCropBlock TOMATO_CROP;
    public static HotPotCropBlock CHILI_PEPPER_CROP;
    public static HotPotCropBlock PEANUT_CROP;
    public static HotPotCropBlock GREENS_CROP;
    public static HotPotCropBlock ZHE_ER_GEN_CROP;

    public static WildCropBlock WILD_SCALLION;
    public static WildCropBlock WILD_CILANTRO;
    public static WildCropBlock WILD_SOYBEAN;
    public static WildCropBlock WILD_GARLIC;
    public static WildCropBlock WILD_TOMATO;
    public static WildCropBlock WILD_CHILI_PEPPER;
    public static WildCropBlock WILD_PEANUT;
    public static WildCropBlock WILD_GREENS;
    public static WildCropBlock WILD_ZHE_ER_GEN;
    public static final List<WildCropBlock> WILD_CROPS = new ArrayList<>();
    public static final List<BlockItem> WILD_CROP_ITEMS = new ArrayList<>();
    public static final List<Block> CUTOUT_BLOCKS = new ArrayList<>();
    private static final Map<String, HotPotCropBlock> CROPS_BY_FOOD = new LinkedHashMap<>();

    // ==== tools/new_item.py：新物品的注册插在这行上面（mryt233 2026-11-01，别删这行）====
    public static Item TOMATO;
    public static Item CHILI_PEPPER;
    public static Item HOTPOT_GUIDE;
    public static Item TOMATO_HOTPOT_BASE;
    public static Item THREE_FRESH_HOTPOT_BASE;
    public static Item TOMATO_SOUP_BUCKET;
    public static Item THREE_FRESH_SOUP_BUCKET;
    public static Item COOKED_KELP;
    public static Item HOT_TOFU_CUBE;

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

    private static Item foodItem(RegistrationApi registration, String id, int nutrition, float saturation) {
        Config.rememberFood(id, nutrition, saturation);
        FoodProperties food = new FoodProperties.Builder()
            .nutrition(nutrition).saturationMod(saturation).alwaysEat().build();
        HotPotCropBlock crop = CROPS_BY_FOOD.get(id);
        if (crop != null) {
            ConfiguredCropItem cropItem = new ConfiguredCropItem(crop, id, new Item.Properties().food(food));
            return registration.items().register(HotPot.id(id), cropItem);
        }
        return registration.items().register(HotPot.id(id),
            new ConfiguredFoodItem(id, new Item.Properties().food(food)));
    }

    private static BlockItem registerBlockItem(RegistrationApi registration, String id, Block block) {
        return registration.items().register(HotPot.id(id), new BlockItem(block, new Item.Properties()));
    }

    private static HotPotCropBlock registerCropBlock(RegistrationApi registration, String foodId, String blockId,
                                                     Supplier<Item> seed) {
        HotPotCropBlock crop = registration.blocks().register(HotPot.id(blockId),
            new HotPotCropBlock(seed, BlockBehaviour.Properties.copy(Blocks.WHEAT)));
        CROPS_BY_FOOD.put(foodId, crop);
        CUTOUT_BLOCKS.add(crop);
        return crop;
    }

    private static WildCropBlock registerWildCrop(RegistrationApi registration, String blockId) {
        WildCropBlock block = registration.blocks().register(HotPot.id(blockId),
            new WildCropBlock(BlockBehaviour.Properties.copy(Blocks.DANDELION)));
        WILD_CROPS.add(block);
        WILD_CROP_ITEMS.add(registerBlockItem(registration, blockId, block));
        CUTOUT_BLOCKS.add(block);
        return block;
    }

    private static SauceBottleItem sauceBottle(RegistrationApi registration, SauceBottleKind kind, int nutrition, float saturation) {
        return registration.items().register(HotPot.id(kind.id()),
            new SauceBottleItem(kind, kind.id(), nutrition, saturation));
    }

    /** 一碗汤（r99）：顺序同 SoupBase（清汤、高汤、菌汤、红汤）。 */
    public static List<SoupBowlItem> SOUP_BOWLS;
    /** 通用汤碗（mryt233，4F-3）：未登记专属碗的自定义 / 数据包汤底共用，通过 {@link #SOUP} 键区分汤底。 */
    public static SoupBowlItem SOUP_BOWL;

    /** 饮料（r99 橙子饮料，r101 维二）和喝完的空瓶。 */
    public static DrinkItem ORANGE_DRINK;
    public static EmptyBottleItem EMPTY_ORANGE_DRINK_BOTTLE;
    public static DrinkItem PLANT_PROTEIN_DRINK;
    public static EmptyBottleItem EMPTY_PLANT_PROTEIN_DRINK_BOTTLE;
    public static DrinkItem ORIENT_COLA;
    public static EmptyBottleItem EMPTY_ORIENT_COLA_BOTTLE;
    public static CupItem GLASS_CUP;
    public static CupItem ORANGE_DRINK_CUP;
    public static CupItem PLANT_PROTEIN_DRINK_CUP;
    public static CupItem ORIENT_COLA_CUP;
    public static CupItem WATER_CUP;
    public static CupItem MILK_CUP;

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

    public static Item SCALLION;
    public static Item CILANTRO;
    /** 一碗蘸料里放了哪几种、还能蘸几口。 */
    public static ItemDataKey<SauceContents> SAUCE;
    /** 菜蘸过哪几种调料（Seasoning.id 列表）；吃下去时生效（LivingEntityEatMixin）。 */
    public static ItemDataKey<List<String>> DIPPED;
    /** 这道菜是在哪种汤底里煮出来的（SoupBase.textureKey()，如 "spicy"、"mushroom"）；吃下时给汤底风味加成。 */
    public static ItemDataKey<String> COOKED_IN_SOUP;
    /** 通用汤碗所盛汤底的完整 ID（mryt233，4F-3）。 */
    public static ItemDataKey<String> SOUP;
    /** 红汤辣度档位（0=微辣，1=中辣，2=特辣，3=变态辣）：记在红汤煮出的菜或盛出的红汤碗上。 */
    public static ItemDataKey<Integer> SPICY_LEVEL;
    /** 玻璃杯是否已经碰过杯（干杯标记，喝下时多回 1 饥饿值）。 */
    public static ItemDataKey<Boolean> CHEERED;
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
    public static ItemDataKey<CustomData> POT_CONTENTS;

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
        for (Item feet : new Item[]{CHICKEN_FEET, COOKED_CHICKEN_FEET, BONELESS_CHICKEN_FEET, COOKED_BONELESS_CHICKEN_FEET}) {
            entries.accept(feet);
        }
        // ==== tools/new_item.py：新物品进创造栏插在这行上面（别删这行）====
        entries.accept(HOTPOT_GUIDE);
        entries.accept(KITCHEN_KNIFE);
        entries.accept(ROLLING_PIN);
        entries.accept(CUTTING_BOARD_ITEM);
        entries.accept(STONE_MILL_ITEM);
        entries.accept(WOODEN_VAT_ITEM);
        entries.accept(WOODEN_PRESS_LID);
        entries.accept(BRINE_BOTTLE);
        entries.accept(SOYMILK_BUCKET);
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
        entries.accept(HOTPOT_TABLE_ITEM);
        entries.accept(TALL_HOTPOT_TABLE_ITEM);
        entries.accept(MULTI_TIER_PREP_RACK_ITEM);
        entries.accept(DESKTOP_PREP_RACK_ITEM);
        entries.accept(HANGING_PREP_RACK_ITEM);
        BENCH_ITEMS.forEach(entries::accept);
        TALL_BENCH_ITEMS.forEach(entries::accept);
        PLATE_ITEMS.forEach(entries::accept);
        SAUCE_STATION_ITEMS.forEach(entries::accept);
        entries.accept(ORANGE_LOG_ITEM);
        entries.accept(ORANGE_WOOD_ITEM);
        entries.accept(STRIPPED_ORANGE_LOG_ITEM);
        entries.accept(STRIPPED_ORANGE_WOOD_ITEM);
        entries.accept(ORANGE_LEAVES_ITEM);
        entries.accept(ORANGE_SAPLING_ITEM);
        WILD_CROP_ITEMS.forEach(entries::accept);
    }

    private HotPotContent() {
    }

    public static void verifyPlateMaterials() {
        Path json = Path.of("content", "materials.json");
        if (!Files.isRegularFile(json)) {
            return;
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

    private static String soundName(SoundType sounds) {
        for (Field field : SoundType.class.getFields()) {
            try {
                if (field.getType() == SoundType.class && field.get(null) == sounds) {
                    return field.getName();
                }
            } catch (IllegalAccessException ignored) {
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
        HELD_FOOD = registration.components().register(HotPot.id("held_food"),
            new ItemDataKey<>("orients_hotpot_chronicles:held_food", HeldFood.CODEC));
        TABLE_MATERIALS = registration.components().register(HotPot.id("table_materials"),
            new ItemDataKey<>("orients_hotpot_chronicles:table_materials", TableMaterialData.CODEC));

        for (PlateMaterial material : PLATE_MATERIALS) {
            for (PlateSize size : PlateSize.values()) {
                if (size == PlateSize.BOWL || size == PlateSize.DRINK || size == PlateSize.CUP) {
                    continue;
                }
                String id = material.id() + "_" + size.itemId();
                PlateBlock plate = registration.blocks().register(HotPot.id(id),
                    new PlateBlock(material.id(), size, material.mapColor(), material.sounds()));
                PLATES.add(plate);
                PLATE_ITEMS.add(registration.items().register(HotPot.id(id),
                    new PlateItem(plate, new Item.Properties().stacksTo(16))));
            }
        }
        BOWL_BLOCK = registration.blocks().register(
            HotPot.id("bowl"), new BowlBlock());
        TableObjects.registerItem(Items.BOWL, TableObjects.bowlTableItem());
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
        CUTOUT_BLOCKS.clear();
        WILD_CROPS.clear();
        WILD_CROP_ITEMS.clear();
        CROPS_BY_FOOD.clear();
        STRIPPED_ORANGE_LOG = registration.blocks().register(HotPot.id("stripped_orange_log"),
            new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG)));
        ORANGE_LOG = registration.blocks().register(HotPot.id("orange_log"),
            new OrangeLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG), () -> STRIPPED_ORANGE_LOG));
        STRIPPED_ORANGE_WOOD = registration.blocks().register(HotPot.id("stripped_orange_wood"),
            new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_WOOD)));
        ORANGE_WOOD = registration.blocks().register(HotPot.id("orange_wood"),
            new OrangeLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD), () -> STRIPPED_ORANGE_WOOD));
        ORANGE_LEAVES = registration.blocks().register(HotPot.id("orange_leaves"),
            new OrangeLeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).noCollission()));
        ORANGE_SAPLING = registration.blocks().register(HotPot.id("orange_sapling"),
            new OrangeSaplingBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
        ORANGE_LOG_ITEM = registerBlockItem(registration, "orange_log", ORANGE_LOG);
        ORANGE_WOOD_ITEM = registerBlockItem(registration, "orange_wood", ORANGE_WOOD);
        STRIPPED_ORANGE_LOG_ITEM = registerBlockItem(registration, "stripped_orange_log", STRIPPED_ORANGE_LOG);
        STRIPPED_ORANGE_WOOD_ITEM = registerBlockItem(registration, "stripped_orange_wood", STRIPPED_ORANGE_WOOD);
        ORANGE_LEAVES_ITEM = registerBlockItem(registration, "orange_leaves", ORANGE_LEAVES);
        ORANGE_SAPLING_ITEM = registerBlockItem(registration, "orange_sapling", ORANGE_SAPLING);
        // mryt233 改：ORANGE_LEAVES 继承 LeavesBlock，不再固定加入 CUTOUT_BLOCKS，以随原版画质开关自动切换流畅(solid)/高品质(cutout_mipped)。
        CUTOUT_BLOCKS.add(ORANGE_SAPLING);
        ORANGE_FOLIAGE_PLACER = registration.foliagePlacers().register(
            HotPot.id("orange_foliage_placer"), OrangeFoliagePlacer.CODEC);

        SCALLION_CROP = registerCropBlock(registration, "scallion", "scallion_crop", () -> SCALLION);
        CILANTRO_CROP = registerCropBlock(registration, "cilantro", "cilantro_crop", () -> CILANTRO);
        SOYBEAN_CROP = registerCropBlock(registration, "soybean", "soybean_crop", () -> SOYBEAN);
        GARLIC_CROP = registerCropBlock(registration, "garlic", "garlic_crop", () -> GARLIC);
        TOMATO_CROP = registerCropBlock(registration, "tomato", "tomato_crop", () -> TOMATO);
        CHILI_PEPPER_CROP = registerCropBlock(registration, "chili_pepper", "chili_pepper_crop", () -> CHILI_PEPPER);
        PEANUT_CROP = registerCropBlock(registration, "peanut", "peanut_crop", () -> PEANUT);
        GREENS_CROP = registerCropBlock(registration, "greens", "greens_crop", () -> GREENS);
        ZHE_ER_GEN_CROP = registerCropBlock(registration, "zhe_er_gen", "zhe_er_gen_crop", () -> ZHE_ER_GEN);

        WILD_SCALLION = registerWildCrop(registration, "wild_scallion");
        WILD_CILANTRO = registerWildCrop(registration, "wild_cilantro");
        WILD_SOYBEAN = registerWildCrop(registration, "wild_soybean");
        WILD_GARLIC = registerWildCrop(registration, "wild_garlic");
        WILD_TOMATO = registerWildCrop(registration, "wild_tomato");
        WILD_CHILI_PEPPER = registerWildCrop(registration, "wild_chili_pepper");
        WILD_PEANUT = registerWildCrop(registration, "wild_peanut");
        WILD_GREENS = registerWildCrop(registration, "wild_greens");
        WILD_ZHE_ER_GEN = registerWildCrop(registration, "wild_zhe_er_gen");

        SEAT = registration.entityTypes().register(HotPot.id("seat"),
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.01F)
                .noSave()
                .noSummon()
                .clientTrackingRange(10)
                .build(HotPot.id("seat").toString()));

        HOTPOT_TABLE = registration.blocks().register(HotPot.id("hotpot_table"),
            new HotPotTableBlock(Blocks.OAK_PLANKS, false, true));
        HOTPOT_TABLE_ITEM = registerBlockItem(registration, "hotpot_table", HOTPOT_TABLE);
        TALL_HOTPOT_TABLE = registration.blocks().register(HotPot.id("tall_hotpot_table"),
            new TallHotPotTableBlock(Blocks.OAK_PLANKS, false, true));
        TALL_HOTPOT_TABLE_ITEM = registerBlockItem(registration, "tall_hotpot_table", TALL_HOTPOT_TABLE);
        MULTI_TIER_PREP_RACK = registration.blocks().register(HotPot.id("multi_tier_prep_rack"),
            new PrepRackBlock(PrepRackBlock.Kind.MULTI_TIER,
                BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0F, 3.0F).noOcclusion().dynamicShape()));
        MULTI_TIER_PREP_RACK_ITEM = registerBlockItem(registration, "multi_tier_prep_rack", MULTI_TIER_PREP_RACK);
        DESKTOP_PREP_RACK = registration.blocks().register(HotPot.id("desktop_prep_rack"),
            new PrepRackBlock(PrepRackBlock.Kind.DESKTOP,
                BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0F, 3.0F).noOcclusion().dynamicShape()));
        DESKTOP_PREP_RACK_ITEM = registerBlockItem(registration, "desktop_prep_rack", DESKTOP_PREP_RACK);
        HANGING_PREP_RACK = registration.blocks().register(HotPot.id("hanging_prep_rack"),
            new PrepRackBlock(PrepRackBlock.Kind.HANGING,
                BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0F, 3.0F).noOcclusion().dynamicShape()));
        HANGING_PREP_RACK_ITEM = registerBlockItem(registration, "hanging_prep_rack", HANGING_PREP_RACK);
        CUTOUT_BLOCKS.add(MULTI_TIER_PREP_RACK);
        CUTOUT_BLOCKS.add(DESKTOP_PREP_RACK);
        CUTOUT_BLOCKS.add(HANGING_PREP_RACK);

        for (String wood : TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(new ResourceLocation(wood + "_planks"));
            HotPotTableBlock table = registration.blocks().register(
                HotPot.id(wood + "_hotpot_table"), new LegacyTableBlock(planks, false));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_hotpot_table"), new BlockItem(table, new Item.Properties())));
        }
        for (String wood : TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(new ResourceLocation(wood + "_planks"));
            BenchBlock bench = registration.blocks().register(
                HotPot.id(wood + "_stool"), new BenchBlock(planks));   // 板凳 = Stool（r102 改 ID）
            BENCHES.add(bench);
            BENCH_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_stool"), new BlockItem(bench, new Item.Properties())));
        }
        for (String wood : TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(new ResourceLocation(wood + "_planks"));
            TallHotPotTableBlock table = registration.blocks().register(
                HotPot.id(wood + "_tall_hotpot_table"), new LegacyTallTableBlock(planks, false));
            TALL_TABLES.add(table);
            TALL_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_tall_hotpot_table"), new BlockItem(table, new Item.Properties())));
            BenchBlock bench = registration.blocks().register(
                HotPot.id(wood + "_bench"), new BenchBlock(planks, true));   // 条凳 = Bench
            TALL_BENCHES.add(bench);
            TALL_BENCH_ITEMS.add(registration.items().register(
                HotPot.id(wood + "_bench"), new BlockItem(bench, new Item.Properties())));
        }
        // 石料 / 金属桌面的火锅桌旧预设占位保留（兼容旧存档迁移升级）。
        for (String[] m : STONE_TABLES) {
            Block material = BuiltInRegistries.BLOCK.get(new ResourceLocation(m[1]));
            HotPotTableBlock table = registration.blocks().register(
                HotPot.id(m[0] + "_hotpot_table"), new LegacyTableBlock(material, true));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(registration.items().register(
                HotPot.id(m[0] + "_hotpot_table"), new BlockItem(table, new Item.Properties())));
            TallHotPotTableBlock tall = registration.blocks().register(
                HotPot.id(m[0] + "_tall_hotpot_table"), new LegacyTallTableBlock(material, true));
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
        KITCHEN_KNIFE = registration.items().register(HotPot.id("iron_kitchen_knife"),
            new KitchenKnifeItem());
        CUTTING_BOARD = registration.blocks().register(HotPot.id("cutting_board"),
            new CuttingBoardBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()
                .dynamicShape()));
        CUTTING_BOARD_ITEM = registration.items().register(HotPot.id("cutting_board"),
            new BlockItem(CUTTING_BOARD, new Item.Properties()));
        STONE_MILL = registration.blocks().register(HotPot.id("stone_mill"),
            new StoneMillBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .strength(1.5F, 6.0F)
                .sound(SoundType.STONE)
                .dynamicShape()
                .noOcclusion()));
        STONE_MILL_ITEM = registerBlockItem(registration, "stone_mill", STONE_MILL);
        WOODEN_VAT = registration.blocks().register(HotPot.id("wooden_vat"),
            new WoodenVatBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0F, 3.0F).noOcclusion()));
        WOODEN_VAT_ITEM = registerBlockItem(registration, "wooden_vat", WOODEN_VAT);
        SOYMILK_BUCKET = registration.items().register(HotPot.id("soymilk_bucket"),
            new SoymilkBucketItem());
        BRINE_BOTTLE = registration.items().register(HotPot.id("brine_bottle"),
            new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
        WOODEN_PRESS_LID = registration.items().register(HotPot.id("wooden_press_lid"),
            new Item(new Item.Properties().stacksTo(16)));
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
        MINCED_PORK = registration.items().register(HotPot.id("minced_pork"),
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
        CLOTH_APRON = registration.items().register(HotPot.id("cloth_apron"),
            new ClothApronItem(registration, ClothApronItem.Style.LINEN));
        BLUE_PRINT_CLOTH_APRON = registration.items().register(
            HotPot.id("blue_print_cloth_apron"), new ClothApronItem(registration, ClothApronItem.Style.BLUE_PRINT));
        BROWN_TRIM_CLOTH_APRON = registration.items().register(
            HotPot.id("brown_trim_cloth_apron"), new ClothApronItem(registration, ClothApronItem.Style.BROWN_TRIM));
        CLOTH_APRONS = List.of(
            CLOTH_APRON, BLUE_PRINT_CLOTH_APRON, BROWN_TRIM_CLOTH_APRON);
        CHICKEN_FEET = foodItem(registration, "chicken_feet", 1, 0.2F);
        COOKED_CHICKEN_FEET = foodItem(registration, "cooked_chicken_feet", 3, 0.6F);
        BONELESS_CHICKEN_FEET = foodItem(registration, "boneless_chicken_feet", 1, 0.2F);
        COOKED_BONELESS_CHICKEN_FEET = foodItem(registration, "cooked_boneless_chicken_feet", 4, 0.7F);
        // ==== tools/new_item.py：新物品的登记赋值插在这行上面（mryt233 2026-11-10）====
        TOMATO = foodItem(registration, "tomato", 2, 0.3F);
        CHILI_PEPPER = foodItem(registration, "chili_pepper", 1, 0.1F);
        HOTPOT_GUIDE = registration.items().register(HotPot.id("hotpot_guide"), new GuideBookItem());
        TOMATO_HOTPOT_BASE = registration.items().register(HotPot.id("tomato_hotpot_base"),
            new Item(new Item.Properties()));
        THREE_FRESH_HOTPOT_BASE = registration.items().register(HotPot.id("three_fresh_hotpot_base"),
            new Item(new Item.Properties()));
        TOMATO_SOUP_BUCKET = registration.items().register(HotPot.id("tomato_soup_bucket"),
            new Item(new Item.Properties().stacksTo(1)));
        THREE_FRESH_SOUP_BUCKET = registration.items().register(HotPot.id("three_fresh_soup_bucket"),
            new Item(new Item.Properties().stacksTo(1)));
        COOKED_KELP = foodItem(registration, "cooked_kelp", 2, 0.3F);
        HOT_TOFU_CUBE = foodItem(registration, "hot_tofu_cube", 2, 0.6F);
        ROLLING_PIN = registration.items().register(HotPot.id("rolling_pin"),
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
        POUNDED_MEAT = registration.items().register(HotPot.id("pounded_meat"),
            new Item(new Item.Properties()));
        SOUP_BOWLS = Soups.builtins().stream()
            .map(soup -> registration.items().register(HotPot.id(soup.bowlId()),
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
        GLASS_CUP = registration.items().register(HotPot.id("glass_cup"), new CupItem(null));
        ORANGE_DRINK_CUP = registration.items().register(HotPot.id("orange_drink_cup"),
            new CupItem(DrinkKind.ORANGE_DRINK));
        PLANT_PROTEIN_DRINK_CUP = registration.items().register(
            HotPot.id("plant_protein_drink_cup"), new CupItem(DrinkKind.PLANT_PROTEIN_DRINK));
        ORIENT_COLA_CUP = registration.items().register(HotPot.id("orient_cola_cup"),
            new CupItem(DrinkKind.ORIENT_COLA));
        WATER_CUP = registration.items().register(HotPot.id("water_cup"),
            new CupItem(DrinkKind.WATER));
        MILK_CUP = registration.items().register(HotPot.id("milk_cup"),
            new CupItem(DrinkKind.MILK));
        CHOPSTICKS = registration.items().register(
            HotPot.id("bamboo_chopsticks"), new ChopsticksItem());
        SCALLION = foodItem(registration, "scallion", 1, 0.1F);
        CILANTRO = foodItem(registration, "cilantro", 1, 0.1F);
        // 1.20.1 ItemStack NBT 垫片键（与 1.21+ DataComponent 键名对齐，底层走 ItemData NBT）
        SAUCE = registration.components().register(HotPot.id("sauce"),
            new ItemDataKey<>("orients_hotpot_chronicles:sauce", SauceContents.CODEC));
        DIPPED = registration.components().register(HotPot.id("dipped"),
            new ItemDataKey<>("orients_hotpot_chronicles:dipped", SauceContents.DIPPED_CODEC));
        COOKED_IN_SOUP = registration.components().register(HotPot.id("cooked_in_soup"),
            new ItemDataKey<>("orients_hotpot_chronicles:cooked_in_soup", Codec.STRING));
        SOUP = registration.components().register(HotPot.id("soup"),
            new ItemDataKey<>("orients_hotpot_chronicles:soup", Codec.STRING));
        SPICY_LEVEL = registration.components().register(HotPot.id("spicy_level"),
            new ItemDataKey<>("orients_hotpot_chronicles:spicy_level", Codec.INT));
        CHEERED = registration.components().register(HotPot.id("cheered"),
            new ItemDataKey<>("orients_hotpot_chronicles:cheered", Codec.BOOL));
        DIPPING_SAUCE = registration.items().register(
            HotPot.id("dipping_sauce"), new DippingSauceItem());

        List<String> bodies = new ArrayList<>(TABLE_WOODS);
        bodies.add("iron");
        for (String body : bodies) {
            boolean iron = body.equals("iron");
            Block like = iron ? Blocks.IRON_BLOCK
                : BuiltInRegistries.BLOCK.get(new ResourceLocation(body + "_planks"));
            SauceStationBlock station = registration.blocks().register(HotPot.id(body + "_sauce_station"),
                new SauceStationBlock(SauceStationBlock.settingsFor(like, iron)));
            SAUCE_STATIONS.add(station);
            SAUCE_STATION_ITEMS.add(registration.items().register(HotPot.id(body + "_sauce_station"),
                new BlockItem(station, new Item.Properties())));
        }
        SAUCE_STATION_TOP = registration.blocks().register(
            HotPot.id("sauce_station_top"), new SauceStationTopBlock());
        POT_CONTENTS = registration.components().register(HotPot.id("pot_contents"),
            new ItemDataKey<>("orients_hotpot_chronicles:pot_contents", CustomData.CODEC));

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
        STONE_MILL_BE = registration.blockEntities().register(HotPot.id("stone_mill"), StoneMillBlockEntity::new, STONE_MILL);
        WOODEN_VAT_BE = registration.blockEntities().register(HotPot.id("wooden_vat"), WoodenVatBlockEntity::new, WOODEN_VAT);
        SAUCE_STATION_BE = registration.blockEntities().register(HotPot.id("sauce_station"), SauceStationBlockEntity::new,
            SAUCE_STATIONS.toArray(new Block[0]));
        HOT_POT_BE = registration.blockEntities().register(HotPot.id("hot_pot"), HotPotBlockEntity::new,
            POT_ORDER.stream().map(m -> POT_BLOCKS.get(m)).toArray(Block[]::new));
        STOVE_BE = registration.blockEntities().register(HotPot.id("stove"), StoveBlockEntity::new, STOVE, TALL_STOVE);
        BENCH_BE = registration.blockEntities().register(HotPot.id("bench"), BenchBlockEntity::new,
            TALL_BENCHES.toArray(new Block[0]));
        HOTPOT_TABLE_BE = registration.blockEntities().register(HotPot.id("hotpot_table"),
            HotPotTableBlockEntity::new, HOTPOT_TABLE, TALL_HOTPOT_TABLE);
        PREP_RACK_BE = registration.blockEntities().register(HotPot.id("prep_rack"),
            PrepRackBlockEntity::new, MULTI_TIER_PREP_RACK, DESKTOP_PREP_RACK, HANGING_PREP_RACK);
        // 触碰静态字段，明确让物品组在客户端和服务端都完成初始化。
        ITEM_GROUP.getDisplayName();
        verifyPlateMaterials();
        TripeLoot.register();
        GarnishLoot.register();
        OrangeLoot.register();
        DrinkCauldronBlock.registerEmptyCauldron();
        Config.rememberTofuCooking(TOFU_COOK_TICKS, TOFU_OVERCOOK_TICKS);
        HotPotRecipes.register(registration);
        HotPotCriterion.register(registration);
        SoupBucketRecipe.register(registration);
        ApronRecolorRecipe.register(registration);
        HotPot.LOGGER.info("[Orient的火锅物语] 内容已注册（组合灶台 + 铜锅 + 铁锅 + 独立分类）");
    }

    public static ItemStack tabIcon() {
        ItemStack icon = new ItemStack(IRON_HOT_POT_ITEM);
        ItemData.set(icon, DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return icon;
    }

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

    public static Collection<HotPotCropBlock> cropBlocks() {
        return Collections.unmodifiableCollection(CROPS_BY_FOOD.values());
    }

    public static ResourceLocation id(String path) {
        return HotPot.id(path);
    }
}
