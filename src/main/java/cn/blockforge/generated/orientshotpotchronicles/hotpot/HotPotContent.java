package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;

/** 注册中心：灶台、锅、筷子、方块实体、屏幕与独立创造标签页。 */
public final class HotPotContent {
    /** 灶台本身就是组合方块的宿主；锅放上去只改变 POT 状态。 */
    public static final StoveBlock STOVE = Registry.register(Registries.BLOCK,
        GeneratedMod.id("brick_stove"), new StoveBlock());

    /** 高灶台：配 1.5 格高火锅桌，灶口、锅、汤整体抬高 {@link StoveBlock#TALL_LIFT} 像素，灶膛还在最下面。 */
    public static final StoveBlock TALL_STOVE = Registry.register(Registries.BLOCK,
        GeneratedMod.id("tall_brick_stove"), new StoveBlock(StoveBlock.TALL_LIFT));
    /** 高灶台的「上半截」：看不见，让伸进上面一格的锅也点得到（没有物品）。 */
    public static final StoveTopBlock STOVE_TOP = Registry.register(Registries.BLOCK,
        GeneratedMod.id("tall_brick_stove_top"), new StoveTopBlock());

    public static final HotPotBlock COPPER_HOT_POT = Registry.register(Registries.BLOCK,
        GeneratedMod.id(HotPotMaterial.COPPER.idPath()), new HotPotBlock(HotPotMaterial.COPPER));

    public static final HotPotBlock IRON_HOT_POT = Registry.register(Registries.BLOCK,
        GeneratedMod.id(HotPotMaterial.IRON.idPath()), new HotPotBlock(HotPotMaterial.IRON));
    /** 鸳鸯锅（2026-10-23，）：S 形 / O 形隔板，两边各一种汤；铜、铁各两种。 */
    public static final HotPotBlock COPPER_S_POT = potBlock(HotPotMaterial.COPPER_S);
    public static final HotPotBlock COPPER_O_POT = potBlock(HotPotMaterial.COPPER_O);
    public static final HotPotBlock IRON_S_POT = potBlock(HotPotMaterial.IRON_S);
    public static final HotPotBlock IRON_O_POT = potBlock(HotPotMaterial.IRON_O);
    public static final HotPotBlock COPPER_SQUARE_POT = potBlock(HotPotMaterial.COPPER_SQUARE);
    public static final HotPotBlock IRON_SQUARE_POT = potBlock(HotPotMaterial.IRON_SQUARE);
    public static final HotPotBlock COPPER_SPLIT_POT = potBlock(HotPotMaterial.COPPER_SPLIT);
    public static final HotPotBlock IRON_SPLIT_POT = potBlock(HotPotMaterial.IRON_SPLIT);
    /** 田字四宫格锅：四块各一种汤。 */
    public static final HotPotBlock COPPER_FOUR_POT = potBlock(HotPotMaterial.COPPER_FOUR);
    public static final HotPotBlock IRON_FOUR_POT = potBlock(HotPotMaterial.IRON_FOUR);

    /** 每种原版木板一种火锅桌（顺序同原版木板），围着灶台拼成 3x3 的大桌，顶面和灶台台面齐平。 */
    public static final java.util.List<String> TABLE_WOODS = java.util.List.of(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped");
    /** 石料 / 金属桌面的桌子：{材质（ID 用）, 取声音和颜色的原版方块}，和 tools/gen_stone_tables.py 的 MATERIALS 同序（2026-10-26）。 */
    static final String[][] STONE_TABLES = {
        {"smooth_stone", "smooth_stone"}, {"polished_andesite", "polished_andesite"}, {"polished_diorite", "polished_diorite"},
        {"polished_granite", "polished_granite"}, {"polished_deepslate", "polished_deepslate"},
        {"polished_blackstone", "polished_blackstone"}, {"quartz", "quartz_block"}, {"cut_copper", "cut_copper"},
        {"iron", "iron_block"}};
    public static final java.util.List<HotPotTableBlock> HOTPOT_TABLES = new java.util.ArrayList<>();
    public static final java.util.List<BlockItem> HOTPOT_TABLE_ITEMS = new java.util.ArrayList<>();
    /** 板凳（矮）：同样每种原版木板一种，能坐。 */
    public static final java.util.List<BenchBlock> BENCHES = new java.util.ArrayList<>();
    public static final java.util.List<BlockItem> BENCH_ITEMS = new java.util.ArrayList<>();
    /** 1.5 格高的火锅桌和一格高的条凳（配高灶台），同样每种原版木板一种。 */
    public static final java.util.List<TallHotPotTableBlock> TALL_TABLES = new java.util.ArrayList<>();
    public static final java.util.List<BlockItem> TALL_TABLE_ITEMS = new java.util.ArrayList<>();
    public static final java.util.List<BenchBlock> TALL_BENCHES = new java.util.ArrayList<>();
    public static final java.util.List<BlockItem> TALL_BENCH_ITEMS = new java.util.ArrayList<>();

    /** 盘子：5 种材质 × 6 种大小（顺序：材质在外层）。 */
    public static final java.util.List<String> PLATE_MATERIALS = java.util.List.of(
        "quartz", "bamboo", "terracotta", "copper", "iron");
    public static final java.util.List<PlateBlock> PLATES = new java.util.ArrayList<>();
    public static final java.util.List<PlateItem> PLATE_ITEMS = new java.util.ArrayList<>();
    /** 筷子上夹着的那一样食物。 */
    public static final net.minecraft.component.ComponentType<ItemStack> HELD_FOOD = Registry.register(
        Registries.DATA_COMPONENT_TYPE, GeneratedMod.id("held_food"),
        net.minecraft.component.ComponentType.<ItemStack>builder()
            .codec(ItemStack.CODEC)
            .packetCodec(ItemStack.PACKET_CODEC)
            .build());

    static {
        for (String material : PLATE_MATERIALS) {
            net.minecraft.block.MapColor color = switch (material) {
                case "quartz" -> net.minecraft.block.MapColor.OFF_WHITE;
                case "bamboo" -> net.minecraft.block.MapColor.PALE_YELLOW;
                case "terracotta" -> net.minecraft.block.MapColor.ORANGE;
                case "copper" -> net.minecraft.block.MapColor.ORANGE;
                default -> net.minecraft.block.MapColor.IRON_GRAY;
            };
            net.minecraft.sound.BlockSoundGroup sounds = switch (material) {
                case "quartz", "terracotta" -> net.minecraft.sound.BlockSoundGroup.DECORATED_POT;
                case "bamboo" -> net.minecraft.sound.BlockSoundGroup.BAMBOO_WOOD;
                case "copper" -> net.minecraft.sound.BlockSoundGroup.COPPER;
                default -> net.minecraft.sound.BlockSoundGroup.METAL;
            };
            for (PlateSize size : PlateSize.values()) {
                if (size == PlateSize.BOWL || size == PlateSize.DRINK || size == PlateSize.CUP) {
                    continue;   // 碗、橙子饮料不是盘子（只借用盘子格摆放）
                }
                String id = material + "_" + size.itemId();   // ID 和显示名对得上（r102）：quartz_small_dish 等
                PlateBlock plate = Registry.register(Registries.BLOCK, GeneratedMod.id(id),
                    new PlateBlock(material, size, color, sounds));
                PLATES.add(plate);
                PLATE_ITEMS.add(Registry.register(Registries.ITEM, GeneratedMod.id(id),
                    new PlateItem(plate, new Item.Settings().maxCount(16))));
            }
        }
    }

    /** 摆在桌上的碗（只带模型，没有物品；r99）。 */
    public static final BowlBlock BOWL_BLOCK = Registry.register(Registries.BLOCK,
        GeneratedMod.id("bowl"), new BowlBlock());
    /** 摆在桌上的橙子饮料（只带模型，r99）。 */
    public static final DrinkBlock DRINK_BLOCK = Registry.register(Registries.BLOCK,
        GeneratedMod.id("drink"), new DrinkBlock());
    /** 摆在桌上的玻璃杯（只带模型，2026-10-25）。 */
    public static final CupBlock CUP_BLOCK = Registry.register(Registries.BLOCK,
        GeneratedMod.id("cup"), new CupBlock());

    /** 装着饮料的炼药锅（2026-10-27，见 DrinkCauldronBlock；没有物品，拆了掉原版炼药锅）。 */
    public static final DrinkCauldronBlock DRINK_CAULDRON = Registry.register(Registries.BLOCK,
        GeneratedMod.id("drink_cauldron"), new DrinkCauldronBlock());

    /** 摆盘子的那一格：一格里能摆好几个盘子。 */
    public static final PlateHolderBlock PLATE_HOLDER = Registry.register(Registries.BLOCK,
        GeneratedMod.id("plates"), new PlateHolderBlock());
    public static final BlockEntityType<PlateHolderBlockEntity> PLATE_HOLDER_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("plates"),
        FabricBlockEntityTypeBuilder.create(PlateHolderBlockEntity::new, PLATE_HOLDER).build());

    /** 坐条凳时骑的看不见的座位实体。 */
    public static final net.minecraft.entity.EntityType<SeatEntity> SEAT = Registry.register(
        Registries.ENTITY_TYPE, GeneratedMod.id("seat"),
        net.minecraft.entity.EntityType.Builder.<SeatEntity>create(SeatEntity::new, net.minecraft.entity.SpawnGroup.MISC)
            .dimensions(0.25F, 0.01F)
            .disableSaving()
            .disableSummon()
            .maxTrackingRange(10)
            .build());

    static {
        for (String wood : TABLE_WOODS) {
            net.minecraft.block.Block planks = Registries.BLOCK.get(Identifier.ofVanilla(wood + "_planks"));
            HotPotTableBlock table = Registry.register(Registries.BLOCK,
                GeneratedMod.id(wood + "_hotpot_table"), new HotPotTableBlock(planks));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(wood + "_hotpot_table"), new BlockItem(table, new Item.Settings())));
        }
        for (String wood : TABLE_WOODS) {
            net.minecraft.block.Block planks = Registries.BLOCK.get(Identifier.ofVanilla(wood + "_planks"));
            BenchBlock bench = Registry.register(Registries.BLOCK,
                GeneratedMod.id(wood + "_stool"), new BenchBlock(planks));   // 板凳 = Stool（r102 改 ID）
            BENCHES.add(bench);
            BENCH_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(wood + "_stool"), new BlockItem(bench, new Item.Settings())));
        }
        for (String wood : TABLE_WOODS) {
            net.minecraft.block.Block planks = Registries.BLOCK.get(Identifier.ofVanilla(wood + "_planks"));
            TallHotPotTableBlock table = Registry.register(Registries.BLOCK,
                GeneratedMod.id(wood + "_tall_hotpot_table"), new TallHotPotTableBlock(planks));
            TALL_TABLES.add(table);
            TALL_TABLE_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(wood + "_tall_hotpot_table"), new BlockItem(table, new Item.Settings())));
            BenchBlock bench = Registry.register(Registries.BLOCK,
                GeneratedMod.id(wood + "_bench"), new BenchBlock(planks, true));   // 条凳 = Bench
            TALL_BENCHES.add(bench);
            TALL_BENCH_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(wood + "_bench"), new BlockItem(bench, new Item.Settings())));
        }
        // 石料 / 金属桌面的火锅桌（2026-10-26 用户：平滑石头、磨制安山岩……铁块；桌面是这种材质，下面红砖）：
        // 矮桌、高桌各一张，模型等由 tools/gen_stone_tables.py 生成，ID = <材质>_hotpot_table / <材质>_tall_hotpot_table。
        // 声音、地图颜色取那种原版方块；不会着火。
        for (String[] m : STONE_TABLES) {
            net.minecraft.block.Block material = Registries.BLOCK.get(Identifier.ofVanilla(m[1]));
            HotPotTableBlock table = Registry.register(Registries.BLOCK,
                GeneratedMod.id(m[0] + "_hotpot_table"), new HotPotTableBlock(material));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(m[0] + "_hotpot_table"), new BlockItem(table, new Item.Settings())));
            TallHotPotTableBlock tall = Registry.register(Registries.BLOCK,
                GeneratedMod.id(m[0] + "_tall_hotpot_table"), new TallHotPotTableBlock(material));
            TALL_TABLES.add(tall);
            TALL_TABLE_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(m[0] + "_tall_hotpot_table"), new BlockItem(tall, new Item.Settings())));
        }
        // 旧版只有一种火锅桌（hotpot_table），老存档里的桌子和物品变成橡木火锅桌
        Registries.BLOCK.addAlias(GeneratedMod.id("hotpot_table"), GeneratedMod.id("oak_hotpot_table"));
        Registries.ITEM.addAlias(GeneratedMod.id("hotpot_table"), GeneratedMod.id("oak_hotpot_table"));
    }

    public static final BlockItem STOVE_ITEM = Registry.register(Registries.ITEM,
        GeneratedMod.id("brick_stove"), new BlockItem(STOVE, new Item.Settings()));

    public static final BlockItem TALL_STOVE_ITEM = Registry.register(Registries.ITEM,
        GeneratedMod.id("tall_brick_stove"), new BlockItem(TALL_STOVE, new Item.Settings()));

    public static final BlockItem COPPER_HOT_POT_ITEM = Registry.register(Registries.ITEM,
        GeneratedMod.id(HotPotMaterial.COPPER.idPath()),
        new BlockItem(COPPER_HOT_POT, new Item.Settings()));

    public static final BlockItem IRON_HOT_POT_ITEM = Registry.register(Registries.ITEM,
        GeneratedMod.id(HotPotMaterial.IRON.idPath()),
        new BlockItem(IRON_HOT_POT, new Item.Settings()));
    public static final BlockItem COPPER_S_POT_ITEM = potItem(COPPER_S_POT);
    public static final BlockItem COPPER_O_POT_ITEM = potItem(COPPER_O_POT);
    public static final BlockItem IRON_S_POT_ITEM = potItem(IRON_S_POT);
    public static final BlockItem IRON_O_POT_ITEM = potItem(IRON_O_POT);
    public static final BlockItem COPPER_SQUARE_POT_ITEM = potItem(COPPER_SQUARE_POT);
    public static final BlockItem IRON_SQUARE_POT_ITEM = potItem(IRON_SQUARE_POT);
    public static final BlockItem COPPER_SPLIT_POT_ITEM = potItem(COPPER_SPLIT_POT);
    public static final BlockItem IRON_SPLIT_POT_ITEM = potItem(IRON_SPLIT_POT);
    public static final BlockItem COPPER_FOUR_POT_ITEM = potItem(COPPER_FOUR_POT);
    public static final BlockItem IRON_FOUR_POT_ITEM = potItem(IRON_FOUR_POT);

    /** 毛肚（牛掉落，r93）：生的回 2 点饥饿；熟的回 5 点。熔炉 / 烟熏炉 / 营火都能烤，锅里涮得比牛肉快一半（熔炉配方 100 tick）。 */
    public static final Item RAW_TRIPE = Registry.register(Registries.ITEM, GeneratedMod.id("tripe"),
        new Item(new Item.Settings().food(new net.minecraft.component.type.FoodComponent.Builder()
            .nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_TRIPE = Registry.register(Registries.ITEM, GeneratedMod.id("cooked_tripe"),
        new Item(new Item.Settings().food(new net.minecraft.component.type.FoodComponent.Builder()
            .nutrition(5).saturationModifier(0.6F).build())));

    /**
     * 切菜（2026-10-23 用户定，）：生牛肉 / 生羊肉在砧板上用铁菜刀点一下（或切石机）切成 3 卷；
     * 每卷熟的回 3 饥饿（3 卷 = 9，比整块牛排多一点，算切开涮的奖励），生的回 1；锅里涮得比整块肉快一半（熔炉配方 100 tick）。
     */
    public static final Item FATTY_BEEF_ROLL = foodItem("fatty_beef_roll", 1, 0.3F);
    public static final Item COOKED_FATTY_BEEF_ROLL = foodItem("cooked_fatty_beef_roll", 3, 0.6F);
    public static final Item MUTTON_ROLL = foodItem("mutton_roll", 1, 0.3F);
    public static final Item COOKED_MUTTON_ROLL = foodItem("cooked_mutton_roll", 3, 0.6F);
    /**
     * 吊龙（用户定，）：牛 / 哞菇 25% 掉一块牛脊肉（生吃同生牛肉 3 饥饿；整块烤 = 熟牛脊肉，数值同熟牛排），
     * 砧板上一刀（或切石机）切成 3 片吊龙；每片熟的回 3、生的回 1；锅里涮得最快（熔炉 50 tick，肥牛卷的一半）。
     */
    public static final Item BEEF_LOIN = foodItem("beef_loin", 3, 0.3F);
    /** 熟牛脊肉（用户要）：整块烤熟，饥饿和饱和度同原版熟牛排（8 / 0.8）。 */
    public static final Item COOKED_BEEF_LOIN = foodItem("cooked_beef_loin", 8, 0.8F);
    public static final Item BEEF_LOIN_SLICE = foodItem("beef_loin_slice", 1, 0.3F);
    public static final Item COOKED_BEEF_LOIN_SLICE = foodItem("cooked_beef_loin_slice", 3, 0.6F);
    /** 铁菜刀：耐久同铁剑（250），切一次掉 1 点；也能当武器，伤害比铁剑低（4）。 */
    public static final Item KITCHEN_KNIFE = Registry.register(Registries.ITEM, GeneratedMod.id("iron_kitchen_knife"),
        new Item(new Item.Settings().maxDamage(250).attributeModifiers(
            net.minecraft.item.SwordItem.createAttributeModifiers(net.minecraft.item.ToolMaterials.IRON, 1, -2.2F))));
    public static final CuttingBoardBlock CUTTING_BOARD = Registry.register(Registries.BLOCK, GeneratedMod.id("cutting_board"),
        new CuttingBoardBlock(net.minecraft.block.AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_PLANKS).nonOpaque()
            .dynamicBounds()));   // 选中框随板上有没有东西变（坑：形状跟着方块实体变就要 dynamicBounds）
    public static final BlockItem CUTTING_BOARD_ITEM = Registry.register(Registries.ITEM, GeneratedMod.id("cutting_board"),
        new BlockItem(CUTTING_BOARD, new Item.Settings()));
    public static final BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("cutting_board"),
        FabricBlockEntityTypeBuilder.create(CuttingBoardBlockEntity::new, CUTTING_BOARD).build());

    /**
     * 土豆片、豆腐（2026-10-23 用户：先做原版有的食材）：原版土豆在砧板上切三下、每下一份「土豆片」（一份贴图上是三片），
     * 切石机一次出 3 份；熟的每份回 2。黄豆打草掉（GarnishLoot，15%），3 黄豆 + 水桶做一整块豆腐，砧板上一刀切成 3 份豆腐块，熟的每份回 2。
     */
    public static final Item POTATO_SLICE = foodItem("potato_slice", 1, 0.3F);
    public static final Item COOKED_POTATO_SLICE = foodItem("cooked_potato_slice", 2, 0.6F);
    public static final Item SOYBEAN = foodItem("soybean", 1, 0.1F);
    public static final Item TOFU = foodItem("tofu", 2, 0.4F);
    public static final Item TOFU_CUBE = foodItem("tofu_cube", 1, 0.3F);
    /**
     * 橙子（2026-10-25 用户：橙子饮料要有原料）：橡木 / 深色橡木树叶像掉苹果一样掉（OrangeLoot），能吃，数值同苹果。
     * 大蒜：打草掉（GarnishLoot，同小葱 / 香菜 / 黄豆；用户：作物先保留）；在砧板上用擀面杖敲三下出 2 份蒜泥（Pounding），
     * 蒜泥是蘸料台第 10 种调料（Seasoning.GARLIC_PASTE）。
     */
    public static final Item ORANGE = foodItem("orange", 4, 0.3F);
    public static final Item GARLIC = foodItem("garlic", 1, 0.1F);
    public static final Item GARLIC_PASTE = foodItem("garlic_paste", 1, 0.1F);
    /**
     * 虾（2026-10-26 用户：加虾和虾滑）：钓鱼钓到带壳的虾（ShrimpLoot）；砧板上菜刀切一刀剥成虾仁（去壳去头，也能用切石机）；
     * 虾仁放砧板上擀面杖拍三下成一颗虾滑（Pounding）。三样都能下锅涮 / 烤熟，熟的回 3 / 3 / 4。
     */
    public static final Item SHRIMP = foodItem("shrimp", 1, 0.2F);
    public static final Item COOKED_SHRIMP = foodItem("cooked_shrimp", 3, 0.5F);
    public static final Item PEELED_SHRIMP = foodItem("peeled_shrimp", 1, 0.2F);
    public static final Item COOKED_PEELED_SHRIMP = foodItem("cooked_peeled_shrimp", 3, 0.6F);
    public static final Item SHRIMP_PASTE = foodItem("shrimp_paste", 1, 0.2F);
    public static final Item COOKED_SHRIMP_PASTE = foodItem("cooked_shrimp_paste", 4, 0.6F);
    /** 番茄（2026-10-25）：打草掉（GarnishLoot，同大蒜 15%），能吃；做番茄锅底料。 */
    public static final Item TOMATO = foodItem("tomato", 2, 0.3F);
    /** 游戏内教程书「火锅指南」（2026-10-25 用户）：菜刀 + 书无序合成，右键打开（GuideBookItem / client.GuideBookScreen）。 */
    public static final Item HOTPOT_GUIDE = Registry.register(Registries.ITEM, GeneratedMod.id("hotpot_guide"), new GuideBookItem());
    /** 锅底料（2026-10-25）：拿着右键锅 = 倒进新汤底（SoupBase.TOMATO / THREE_FRESH），一块一锅。 */
    public static final Item TOMATO_HOTPOT_BASE = Registry.register(Registries.ITEM, GeneratedMod.id("tomato_hotpot_base"),
        new Item(new Item.Settings()));
    public static final Item THREE_FRESH_HOTPOT_BASE = Registry.register(Registries.ITEM, GeneratedMod.id("three_fresh_hotpot_base"),
        new Item(new Item.Settings()));
    /**
     * 汤底桶（2026-10-26 用户）：只能用空桶从番茄锅 / 三鲜锅里舀出来（没有配方），倒回锅里就是那种汤（SoupBase 的 source）；
     * 不是液体，不能倒在地上。图标照原版水桶 / 岩浆桶的样子（tools/gen_garden.py）。
     */
    public static final Item TOMATO_SOUP_BUCKET = Registry.register(Registries.ITEM, GeneratedMod.id("tomato_soup_bucket"),
        new Item(new Item.Settings().maxCount(1)));
    public static final Item THREE_FRESH_SOUP_BUCKET = Registry.register(Registries.ITEM, GeneratedMod.id("three_fresh_soup_bucket"),
        new Item(new Item.Settings().maxCount(1)));
    /** 熟海带（2026-10-25 用户）：原版海带在锅里煮出来（HotPotCooking.potResult），熔炉 / 烟熏炉照旧是干海带；回 2。 */
    public static final Item COOKED_KELP = foodItem("cooked_kelp", 2, 0.3F);
    public static final Item COOKED_TOFU_CUBE = foodItem("cooked_tofu_cube", 2, 0.6F);

    /**
     * 丸子（2026-10-23 用户定）：木棒在砧板上捣三下出 3 个生丸子（Pounding）；熟的每个回 3，香菜版（2 生丸子 + 1 香菜 → 2 个）每个回 4。
     * 鱼丸用生鳕鱼 / 生鲑鱼。擀面杖（用户：木棒改成擀面杖的样子，名字和 ID 一起改）耐久 120，捣一下掉 1 点。
     */
    public static final Item ROLLING_PIN = Registry.register(Registries.ITEM, GeneratedMod.id("rolling_pin"),
        new Item(new Item.Settings().maxDamage(120)));
    public static final Item BEEF_MEATBALL = foodItem("beef_meatball", 1, 0.3F);
    public static final Item PORK_MEATBALL = foodItem("pork_meatball", 1, 0.3F);
    public static final Item MUTTON_MEATBALL = foodItem("mutton_meatball", 1, 0.3F);
    public static final Item FISH_BALL = foodItem("fish_ball", 1, 0.3F);
    public static final Item CILANTRO_BEEF_MEATBALL = foodItem("cilantro_beef_meatball", 1, 0.3F);
    public static final Item CILANTRO_PORK_MEATBALL = foodItem("cilantro_pork_meatball", 1, 0.3F);
    public static final Item CILANTRO_MUTTON_MEATBALL = foodItem("cilantro_mutton_meatball", 1, 0.3F);
    public static final Item CILANTRO_FISH_BALL = foodItem("cilantro_fish_ball", 1, 0.3F);
    public static final Item COOKED_BEEF_MEATBALL = foodItem("cooked_beef_meatball", 3, 0.6F);
    public static final Item COOKED_PORK_MEATBALL = foodItem("cooked_pork_meatball", 3, 0.6F);
    public static final Item COOKED_MUTTON_MEATBALL = foodItem("cooked_mutton_meatball", 3, 0.6F);
    public static final Item COOKED_FISH_BALL = foodItem("cooked_fish_ball", 3, 0.6F);
    public static final Item COOKED_CILANTRO_BEEF_MEATBALL = foodItem("cooked_cilantro_beef_meatball", 4, 0.6F);
    public static final Item COOKED_CILANTRO_PORK_MEATBALL = foodItem("cooked_cilantro_pork_meatball", 4, 0.6F);
    public static final Item COOKED_CILANTRO_MUTTON_MEATBALL = foodItem("cooked_cilantro_mutton_meatball", 4, 0.6F);
    public static final Item COOKED_CILANTRO_FISH_BALL = foodItem("cooked_cilantro_fish_ball", 4, 0.6F);

    /** 砧板上捣到一半的肉（捶碎 / 肉浆）：只给砧板渲染器画立体用，不进创造栏；样子按自定义模型数据换（Pounding.display）。 */
    public static final Item POUNDED_MEAT = Registry.register(Registries.ITEM, GeneratedMod.id("pounded_meat"),
        new Item(new Item.Settings()));

    private static Item foodItem(String id, int nutrition, float saturation) {
        return Registry.register(Registries.ITEM, GeneratedMod.id(id),
            new Item(new Item.Settings().food(new net.minecraft.component.type.FoodComponent.Builder()
                .nutrition(nutrition).saturationModifier(saturation).build())));
    }

    /** 一碗汤（r99）：顺序同 SoupBase（清汤、高汤、菌汤、红汤）。 */
    public static final java.util.List<SoupBowlItem> SOUP_BOWLS = java.util.Arrays.stream(SoupBase.values())
        .map(soup -> Registry.register(Registries.ITEM, GeneratedMod.id(soup.bowlId()),
            new SoupBowlItem(soup)))
        .toList();

    /** 饮料（r99 橙子饮料，r101 维二）和喝完的空瓶。 */
    public static final DrinkItem ORANGE_DRINK = Registry.register(Registries.ITEM,
        GeneratedMod.id("orange_drink"), new DrinkItem(DrinkKind.ORANGE_DRINK));
    public static final EmptyBottleItem EMPTY_ORANGE_DRINK_BOTTLE = Registry.register(Registries.ITEM,
        GeneratedMod.id("empty_orange_drink_bottle"), new EmptyBottleItem(DrinkKind.ORANGE_DRINK));
    public static final DrinkItem PLANT_PROTEIN_DRINK = Registry.register(Registries.ITEM,
        GeneratedMod.id("plant_protein_drink"), new DrinkItem(DrinkKind.PLANT_PROTEIN_DRINK));
    public static final EmptyBottleItem EMPTY_PLANT_PROTEIN_DRINK_BOTTLE = Registry.register(Registries.ITEM,
        GeneratedMod.id("empty_plant_protein_drink_bottle"), new EmptyBottleItem(DrinkKind.PLANT_PROTEIN_DRINK));
    /** 之晗可乐（原名 之晗可乐，2026-10-26 用户改名）（2026-10-25 用户定名）：一瓶 4 口，喝完剩空瓶，满瓶空瓶都能摆桌上（和另外两种饮料一样）。 */
    public static final DrinkItem ORIENT_COLA = Registry.register(Registries.ITEM,
        GeneratedMod.id("orient_cola"), new DrinkItem(DrinkKind.ORIENT_COLA));
    public static final EmptyBottleItem EMPTY_ORIENT_COLA_BOTTLE = Registry.register(Registries.ITEM,
        GeneratedMod.id("empty_orient_cola_bottle"), new EmptyBottleItem(DrinkKind.ORIENT_COLA));
    /** 玻璃杯（2026-10-25 用户）：空杯 + 三种饮料各一种装满的杯子（一口），见 CupItem。 */
    public static final CupItem GLASS_CUP = Registry.register(Registries.ITEM, GeneratedMod.id("glass_cup"), new CupItem(null));
    public static final CupItem ORANGE_DRINK_CUP = Registry.register(Registries.ITEM, GeneratedMod.id("orange_drink_cup"),
        new CupItem(DrinkKind.ORANGE_DRINK));
    public static final CupItem PLANT_PROTEIN_DRINK_CUP = Registry.register(Registries.ITEM,
        GeneratedMod.id("plant_protein_drink_cup"), new CupItem(DrinkKind.PLANT_PROTEIN_DRINK));
    public static final CupItem ORIENT_COLA_CUP = Registry.register(Registries.ITEM, GeneratedMod.id("orient_cola_cup"),
        new CupItem(DrinkKind.ORIENT_COLA));
    /** 一杯水、一杯牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒，桶不会空；空杯对着水源右键也能舀一杯水。 */
    public static final CupItem WATER_CUP = Registry.register(Registries.ITEM, GeneratedMod.id("water_cup"),
        new CupItem(DrinkKind.WATER));
    public static final CupItem MILK_CUP = Registry.register(Registries.ITEM, GeneratedMod.id("milk_cup"),
        new CupItem(DrinkKind.MILK));

    public static CupItem cupFor(DrinkKind kind) {
        return switch (kind) {
            case ORANGE_DRINK -> ORANGE_DRINK_CUP;
            case PLANT_PROTEIN_DRINK -> PLANT_PROTEIN_DRINK_CUP;
            case ORIENT_COLA -> ORIENT_COLA_CUP;
            case WATER -> WATER_CUP;
            case MILK -> MILK_CUP;
        };
    }

    public static DrinkItem drinkFor(DrinkKind kind) {
        return switch (kind) {
            case ORANGE_DRINK -> ORANGE_DRINK;
            case PLANT_PROTEIN_DRINK -> PLANT_PROTEIN_DRINK;
            case ORIENT_COLA -> ORIENT_COLA;
            case WATER, MILK -> throw new IllegalArgumentException("水、牛奶没有瓶装");
        };
    }

    public static Item emptyBottle(DrinkKind kind) {
        return switch (kind) {
            case ORANGE_DRINK -> EMPTY_ORANGE_DRINK_BOTTLE;
            case PLANT_PROTEIN_DRINK -> EMPTY_PLANT_PROTEIN_DRINK_BOTTLE;
            case ORIENT_COLA -> EMPTY_ORIENT_COLA_BOTTLE;
            case WATER, MILK -> net.minecraft.item.Items.GLASS_BOTTLE;   // 用不到：水、牛奶没有瓶装
        };
    }

    public static final Item CHOPSTICKS = Registry.register(Registries.ITEM,
        GeneratedMod.id("bamboo_chopsticks"), new ChopsticksItem());

    // ------------------------------------------------------------------ 蘸料（r103，用户定的玩法见 Seasoning / SauceStationBlock）
    /** 小葱、香菜：打草掉落（各 25%），能吃回 1 点饥饿值；放进蘸料台变成葱花、香菜碎末。 */
    public static final Item SCALLION = Registry.register(Registries.ITEM, GeneratedMod.id("scallion"),
        new Item(new Item.Settings().food(new net.minecraft.component.type.FoodComponent.Builder()
            .nutrition(1).saturationModifier(0.1F).build())));
    public static final Item CILANTRO = Registry.register(Registries.ITEM, GeneratedMod.id("cilantro"),
        new Item(new Item.Settings().food(new net.minecraft.component.type.FoodComponent.Builder()
            .nutrition(1).saturationModifier(0.1F).build())));
    /** 一碗蘸料里放了哪几种、还能蘸几口。 */
    public static final ComponentType<SauceContents> SAUCE = Registry.register(
        Registries.DATA_COMPONENT_TYPE, GeneratedMod.id("sauce"),
        ComponentType.<SauceContents>builder().codec(SauceContents.CODEC).packetCodec(SauceContents.PACKET_CODEC).build());
    /** 菜蘸过哪几种调料（Seasoning.bit 的组合）；吃下去时生效（LivingEntityEatMixin）。 */
    public static final ComponentType<Integer> DIPPED = Registry.register(
        Registries.DATA_COMPONENT_TYPE, GeneratedMod.id("dipped"),
        ComponentType.<Integer>builder().codec(com.mojang.serialization.Codec.INT)
            .packetCodec(net.minecraft.network.codec.PacketCodecs.VAR_INT).build());
    public static final DippingSauceItem DIPPING_SAUCE = Registry.register(Registries.ITEM,
        GeneratedMod.id("dipping_sauce"), new DippingSauceItem());
    /** 蘸料台：11 种原版木头各一个 + 铁的（用户定）；ID = <木头>_sauce_station / iron_sauce_station。 */
    public static final java.util.List<SauceStationBlock> SAUCE_STATIONS = new java.util.ArrayList<>();
    public static final java.util.List<BlockItem> SAUCE_STATION_ITEMS = new java.util.ArrayList<>();

    static {
        java.util.List<String> bodies = new java.util.ArrayList<>(TABLE_WOODS);
        bodies.add("iron");
        for (String body : bodies) {
            boolean iron = body.equals("iron");
            net.minecraft.block.Block like = iron ? net.minecraft.block.Blocks.IRON_BLOCK
                : Registries.BLOCK.get(net.minecraft.util.Identifier.ofVanilla(body + "_planks"));
            SauceStationBlock station = Registry.register(Registries.BLOCK, GeneratedMod.id(body + "_sauce_station"),
                new SauceStationBlock(net.minecraft.block.AbstractBlock.Settings.copy(like).nonOpaque()));
            SAUCE_STATIONS.add(station);
            SAUCE_STATION_ITEMS.add(Registry.register(Registries.ITEM, GeneratedMod.id(body + "_sauce_station"),
                new BlockItem(station, new Item.Settings())));
        }
    }

    /** 蘸料台的「上半截」：看不见，占住头顶那一格（没有物品；2026-10-23，）。 */
    public static final SauceStationTopBlock SAUCE_STATION_TOP = Registry.register(Registries.BLOCK,
        GeneratedMod.id("sauce_station_top"), new SauceStationTopBlock());

    public static final BlockEntityType<SauceStationBlockEntity> SAUCE_STATION_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("sauce_station"),
        FabricBlockEntityTypeBuilder.create(SauceStationBlockEntity::new,
            SAUCE_STATIONS.toArray(new net.minecraft.block.Block[0])).build());
    public static final ExtendedScreenHandlerType<SauceStationScreenHandler, BlockPos> SAUCE_STATION_SCREEN =
        Registry.register(Registries.SCREEN_HANDLER, GeneratedMod.id("sauce_station"),
            new ExtendedScreenHandlerType<>(SauceStationScreenHandler::new, BlockPos.PACKET_CODEC));

    /** 装着东西的锅物品：汤底、食材、熟度原样打包（格式同锅方块实体存档）。 */
    public static final ComponentType<NbtComponent> POT_CONTENTS = Registry.register(
        Registries.DATA_COMPONENT_TYPE, GeneratedMod.id("pot_contents"),
        ComponentType.<NbtComponent>builder()
            .codec(NbtComponent.CODEC)
            .packetCodec(NbtComponent.PACKET_CODEC)
            .build());

    /** 独立锅方块仍保留，兼容未放在灶台上的旧世界与自由摆放。 */
    public static final BlockEntityType<HotPotBlockEntity> HOT_POT_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("hot_pot"),
        FabricBlockEntityTypeBuilder.create(HotPotBlockEntity::new,
            COPPER_HOT_POT, IRON_HOT_POT, COPPER_S_POT, COPPER_O_POT, IRON_S_POT, IRON_O_POT, COPPER_SQUARE_POT, IRON_SQUARE_POT,
            COPPER_SPLIT_POT, IRON_SPLIT_POT, COPPER_FOUR_POT, IRON_FOUR_POT).build());

    /** 组合灶台方块实体：燃料、锅库存和烹饪数据全部在同一个位置。 */
    public static final BlockEntityType<StoveBlockEntity> STOVE_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("stove"),
        FabricBlockEntityTypeBuilder.create(StoveBlockEntity::new, STOVE, TALL_STOVE).build());

    /** 条凳的方块实体（只用来画斜腿）。 */
    public static final BlockEntityType<BenchBlockEntity> BENCH_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("bench"),
        FabricBlockEntityTypeBuilder.create(BenchBlockEntity::new,
            TALL_BENCHES.toArray(new net.minecraft.block.Block[0])).build());

    public static final ExtendedScreenHandlerType<HotPotScreenHandler, BlockPos> HOT_POT_SCREEN =
        Registry.register(Registries.SCREEN_HANDLER, GeneratedMod.id("hot_pot"),
            new ExtendedScreenHandlerType<>(HotPotScreenHandler::new, BlockPos.PACKET_CODEC));

    /** 灶台底座的灶膛燃料界面。 */
    public static final ExtendedScreenHandlerType<StoveScreenHandler, BlockPos> STOVE_SCREEN =
        Registry.register(Registries.SCREEN_HANDLER, GeneratedMod.id("stove_fuel"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, pos) -> new StoveScreenHandler(syncId, playerInventory, pos),
                BlockPos.PACKET_CODEC));

    /** 本模组专属分类：使用 Fabric 的追加式标签页，自动排在原版标签页之后并进入后续页面。 */
    public static final RegistryKey<ItemGroup> ITEM_GROUP_KEY = RegistryKey.of(
        RegistryKeys.ITEM_GROUP, GeneratedMod.id("hotpot"));

    public static final ItemGroup ITEM_GROUP = Registry.register(Registries.ITEM_GROUP,
        ITEM_GROUP_KEY,
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup." + GeneratedMod.MOD_ID))
            .icon(HotPotContent::tabIcon)
            .entries((context, entries) -> {
                entries.add(STOVE_ITEM);
                entries.add(TALL_STOVE_ITEM);
                // 锅：先铁锅全部、再铜锅全部，每种按 九宫格 / S / O / 直隔 / 田字（用户 2026-10-24 定）
                entries.add(IRON_HOT_POT_ITEM);
                entries.add(IRON_S_POT_ITEM);
                entries.add(IRON_O_POT_ITEM);
                entries.add(IRON_SQUARE_POT_ITEM);
                entries.add(IRON_SPLIT_POT_ITEM);
                entries.add(IRON_FOUR_POT_ITEM);
                entries.add(COPPER_HOT_POT_ITEM);
                entries.add(COPPER_S_POT_ITEM);
                entries.add(COPPER_O_POT_ITEM);
                entries.add(COPPER_SQUARE_POT_ITEM);
                entries.add(COPPER_SPLIT_POT_ITEM);
                entries.add(COPPER_FOUR_POT_ITEM);
                entries.add(CHOPSTICKS);
                entries.add(RAW_TRIPE);
                entries.add(COOKED_TRIPE);
                entries.add(FATTY_BEEF_ROLL);
                entries.add(COOKED_FATTY_BEEF_ROLL);
                entries.add(MUTTON_ROLL);
                entries.add(COOKED_MUTTON_ROLL);
                entries.add(BEEF_LOIN);
                entries.add(COOKED_BEEF_LOIN);
                entries.add(BEEF_LOIN_SLICE);
                entries.add(COOKED_BEEF_LOIN_SLICE);
                entries.add(POTATO_SLICE);
                entries.add(COOKED_POTATO_SLICE);
                entries.add(SOYBEAN);
                entries.add(TOFU);
                entries.add(TOFU_CUBE);
                entries.add(COOKED_TOFU_CUBE);
                for (Item ball : new Item[]{BEEF_MEATBALL, COOKED_BEEF_MEATBALL, CILANTRO_BEEF_MEATBALL, COOKED_CILANTRO_BEEF_MEATBALL, PORK_MEATBALL, COOKED_PORK_MEATBALL, CILANTRO_PORK_MEATBALL, COOKED_CILANTRO_PORK_MEATBALL, MUTTON_MEATBALL, COOKED_MUTTON_MEATBALL, CILANTRO_MUTTON_MEATBALL, COOKED_CILANTRO_MUTTON_MEATBALL, FISH_BALL, COOKED_FISH_BALL, CILANTRO_FISH_BALL, COOKED_CILANTRO_FISH_BALL}) {
                    entries.add(ball);
                }
                for (Item shrimp : new Item[]{SHRIMP, COOKED_SHRIMP, PEELED_SHRIMP, COOKED_PEELED_SHRIMP, SHRIMP_PASTE, COOKED_SHRIMP_PASTE}) {
                    entries.add(shrimp);
                }
                entries.add(HOTPOT_GUIDE);
                entries.add(KITCHEN_KNIFE);
                entries.add(ROLLING_PIN);
                entries.add(CUTTING_BOARD_ITEM);
                SOUP_BOWLS.forEach(entries::add);
                entries.add(ORANGE_DRINK);
                entries.add(EMPTY_ORANGE_DRINK_BOTTLE);
                entries.add(PLANT_PROTEIN_DRINK);
                entries.add(EMPTY_PLANT_PROTEIN_DRINK_BOTTLE);
                entries.add(ORIENT_COLA);
                entries.add(EMPTY_ORIENT_COLA_BOTTLE);
                entries.add(GLASS_CUP);
                entries.add(ORANGE_DRINK_CUP);
                entries.add(PLANT_PROTEIN_DRINK_CUP);
                entries.add(ORIENT_COLA_CUP);
                entries.add(WATER_CUP);
                entries.add(MILK_CUP);
                entries.add(SCALLION);
                entries.add(CILANTRO);
                entries.add(ORANGE);
                entries.add(GARLIC);
                entries.add(GARLIC_PASTE);
                entries.add(COOKED_KELP);
                entries.add(TOMATO);
                entries.add(TOMATO_HOTPOT_BASE);
                entries.add(THREE_FRESH_HOTPOT_BASE);
                entries.add(TOMATO_SOUP_BUCKET);
                entries.add(THREE_FRESH_SOUP_BUCKET);
                entries.add(DIPPING_SAUCE);
                HOTPOT_TABLE_ITEMS.forEach(entries::add);
                BENCH_ITEMS.forEach(entries::add);
                TALL_TABLE_ITEMS.forEach(entries::add);
                TALL_BENCH_ITEMS.forEach(entries::add);
                PLATE_ITEMS.forEach(entries::add);
                SAUCE_STATION_ITEMS.forEach(entries::add);
            })
            .build());

    private HotPotContent() {
    }

    /** 在模组初始化阶段调用，确保注册顺序完成后输出一条可追踪日志。 */
    public static void register() {
        // 触碰静态字段，明确让物品组在客户端和服务端都完成初始化。
        ITEM_GROUP.getDisplayName();
        TripeLoot.register();
        GarnishLoot.register();
        OrangeLoot.register();
        ShrimpLoot.register();
        DrinkCauldronBlock.registerEmptyCauldron();
        HotPotCooking.potResult(net.minecraft.item.Items.KELP, COOKED_KELP);
        OpenSauceRestockPayload.register();
        ChopsticksAimPayload.register();
        HotPotCriterion.register();
        SoupBucketRecipe.register();
        // 下界木（绯红、诡异）不可燃，其余和原版木板一样会着火
        for (int i = 0; i < TABLE_WOODS.size(); i++) {
            String wood = TABLE_WOODS.get(i);
            if (!wood.equals("crimson") && !wood.equals("warped")) {
                net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance()
                    .add(HOTPOT_TABLES.get(i), 5, 20);
                net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance()
                    .add(BENCHES.get(i), 5, 20);
                net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance()
                    .add(TALL_BENCHES.get(i), 5, 20);
                // 高火锅桌以前漏了，不会着火（2026-10-23，用户要求修；Forge 系移植版已经会着火）
                net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance()
                    .add(TALL_TABLES.get(i), 5, 20);
                // 木头蘸料台（和 TABLE_WOODS 同序，铁的在最后不登记）也会着火（2026-10-23，用户要求）
                net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance()
                    .add(SAUCE_STATIONS.get(i), 5, 20);
            }
        }
        // 砧板（橡木）也会着火，数值同桌子（2026-10-23，用户要求）
        net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance()
            .add(CUTTING_BOARD, 5, 20);
        // 灶上有锅时，对着锅挖只挖下锅（连汤带菜打包掉落），灶台留着；对着灶身挖才挖整个灶台
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register(
            (world, player, pos, state, blockEntity) -> {
                // 砧板：对着板上的东西挖只挖东西，砧板留着（0.1.2-alpha 后用户报）
                if (blockEntity instanceof CuttingBoardBlockEntity board && !board.item().isEmpty()
                    && CuttingBoardBlock.aimsAtItem(pos, player)) {
                    CuttingBoardBlock.breakItem(world, pos, board, player);
                    return false;
                }
                if (blockEntity instanceof StoveBlockEntity stove && StoveBlock.aimsAtPot(state, pos, player)) {
                    stove.breakPot(player);
                    return false;
                }
                // 盘子格：只挖准星对着的那一个盘子（连菜掉出来），别的盘子留着（r87，）
                if (blockEntity instanceof PlateHolderBlockEntity holder) {
                    PlateHolderBlock.breakOne(world, pos, holder, player);
                    return false;
                }
                // 挖高灶台的「上半截」时对着锅：同样只挖锅
                if (state.isOf(STOVE_TOP) && world.getBlockEntity(pos.down()) instanceof StoveBlockEntity stove
                    && StoveBlock.aimsAtPot(world.getBlockState(pos.down()), pos.down(), player)) {
                    stove.breakPot(player);
                    return false;
                }
                return true;
            });
        GeneratedMod.LOGGER.info("[Orient的火锅物语] 内容已注册（组合灶台 + 铜锅 + 铁锅 + 独立分类）");
    }

    /**
     * 分类页图标：铁锅 + 沸腾的红汤 + 漂着几样原版食物。用自定义模型数据 1 切到
     * models/item/iron_hot_pot_tab_icon.json，普通铁锅物品不受影响。
     */
    private static ItemStack tabIcon() {
        ItemStack icon = new ItemStack(IRON_HOT_POT_ITEM);
        icon.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
            new net.minecraft.component.type.CustomModelDataComponent(1));
        return icon;
    }

    public static HotPotBlock blockFor(HotPotMaterial material) {
        return switch (material) {
            case COPPER -> COPPER_HOT_POT;
            case IRON -> IRON_HOT_POT;
            case COPPER_S -> COPPER_S_POT;
            case COPPER_O -> COPPER_O_POT;
            case IRON_S -> IRON_S_POT;
            case IRON_O -> IRON_O_POT;
            case COPPER_SQUARE -> COPPER_SQUARE_POT;
            case IRON_SQUARE -> IRON_SQUARE_POT;
            case COPPER_SPLIT -> COPPER_SPLIT_POT;
            case IRON_SPLIT -> IRON_SPLIT_POT;
            case COPPER_FOUR -> COPPER_FOUR_POT;
            case IRON_FOUR -> IRON_FOUR_POT;
        };
    }

    public static BlockItem itemFor(HotPotMaterial material) {
        return switch (material) {
            case COPPER -> COPPER_HOT_POT_ITEM;
            case IRON -> IRON_HOT_POT_ITEM;
            case COPPER_S -> COPPER_S_POT_ITEM;
            case COPPER_O -> COPPER_O_POT_ITEM;
            case IRON_S -> IRON_S_POT_ITEM;
            case IRON_O -> IRON_O_POT_ITEM;
            case COPPER_SQUARE -> COPPER_SQUARE_POT_ITEM;
            case IRON_SQUARE -> IRON_SQUARE_POT_ITEM;
            case COPPER_SPLIT -> COPPER_SPLIT_POT_ITEM;
            case IRON_SPLIT -> IRON_SPLIT_POT_ITEM;
            case COPPER_FOUR -> COPPER_FOUR_POT_ITEM;
            case IRON_FOUR -> IRON_FOUR_POT_ITEM;
        };
    }

    private static HotPotBlock potBlock(HotPotMaterial material) {
        return Registry.register(Registries.BLOCK, GeneratedMod.id(material.idPath()), new HotPotBlock(material));
    }

    private static BlockItem potItem(HotPotBlock block) {
        return Registry.register(Registries.ITEM, GeneratedMod.id(block.material().idPath()),
            new BlockItem(block, new Item.Settings()));
    }

    public static Identifier id(String path) {
        return GeneratedMod.id(path);
    }
}
