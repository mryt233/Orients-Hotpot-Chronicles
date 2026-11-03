// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.BlockPos;

/** 注册中心：灶台、锅、筷子、方块实体、屏幕与独立创造标签页。 */
public final class HotPotContent {
    // mryt233 改（26.x）：1.21.2 起方块物品默认用 item.<id> 当名字，要 useBlockDescriptionPrefix() 才用 block.<id>；
    // 语言文件只有 block.<id>，漏了这一句 119 个方块物品全显示成翻译键（玩家反馈 i18n 炸了）。
    /** 灶台本身就是组合方块的宿主；锅放上去只改变 POT 状态。 */
    public static final StoveBlock STOVE = reg().blocks().registerFactory(
        HotPot.id("brick_stove"), properties -> new StoveBlock(properties));

    /** 高灶台：配 1.5 格高火锅桌，灶口、锅、汤整体抬高 {@link StoveBlock#TALL_LIFT} 像素，灶膛还在最下面。 */
    public static final StoveBlock TALL_STOVE = reg().blocks().registerFactory(
        HotPot.id("tall_brick_stove"), properties -> new StoveBlock(StoveBlock.TALL_LIFT, properties));
    /** 高灶台的「上半截」：看不见，让伸进上面一格的锅也点得到（没有物品）。 */
    public static final StoveTopBlock STOVE_TOP = reg().blocks().registerFactory(
        HotPot.id("tall_brick_stove_top"), properties -> new StoveTopBlock(properties));

    public static final HotPotBlock COPPER_HOT_POT = reg().blocks().registerFactory(
        HotPot.id(HotPotMaterial.COPPER.idPath()), properties -> new HotPotBlock(HotPotMaterial.COPPER, properties));

    public static final HotPotBlock IRON_HOT_POT = reg().blocks().registerFactory(
        HotPot.id(HotPotMaterial.IRON.idPath()), properties -> new HotPotBlock(HotPotMaterial.IRON, properties));
    /** 鸳鸯锅（2026-10-23，新增）：S 形 / O 形隔板，两边各一种汤；铜、铁各两种。 */
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
    /** 筷子上夹着的那一样食物（组件值包在 HeldFood 里，NeoForge 要求组件值实现 equals/hashCode）。 */
    public static final net.minecraft.core.component.DataComponentType<HeldFood> HELD_FOOD = reg().components().register( HotPot.id("held_food"),
        net.minecraft.core.component.DataComponentType.<HeldFood>builder()
            .persistent(HeldFood.CODEC)
            .networkSynchronized(HeldFood.STREAM_CODEC)
            .build());

    static {
        for (String material : PLATE_MATERIALS) {
            net.minecraft.world.level.material.MapColor color = switch (material) {
                case "quartz" -> net.minecraft.world.level.material.MapColor.QUARTZ;
                case "bamboo" -> net.minecraft.world.level.material.MapColor.SAND;
                case "terracotta" -> net.minecraft.world.level.material.MapColor.COLOR_ORANGE;
                case "copper" -> net.minecraft.world.level.material.MapColor.COLOR_ORANGE;
                default -> net.minecraft.world.level.material.MapColor.METAL;
            };
            net.minecraft.world.level.block.SoundType sounds = switch (material) {
                case "quartz", "terracotta" -> net.minecraft.world.level.block.SoundType.DECORATED_POT;
                case "bamboo" -> net.minecraft.world.level.block.SoundType.BAMBOO_WOOD;
                case "copper" -> net.minecraft.world.level.block.SoundType.COPPER;
                default -> net.minecraft.world.level.block.SoundType.METAL;
            };
            for (PlateSize size : PlateSize.values()) {
                if (size == PlateSize.BOWL || size == PlateSize.DRINK || size == PlateSize.CUP) {
                    continue;   // 碗、橙子饮料不是盘子（只借用盘子格摆放）
                }
                String id = material + "_" + size.itemId();   // ID 和显示名对得上（r102）：quartz_small_dish 等
                PlateBlock plate = reg().blocks().registerFactory( HotPot.id(id), properties -> new PlateBlock(material, size, color, sounds, properties));
                PLATES.add(plate);
                PLATE_ITEMS.add(reg().items().registerFactory( HotPot.id(id), properties -> new PlateItem(plate, properties.stacksTo(16).useBlockDescriptionPrefix())));
            }
        }
    }

    /** 摆在桌上的碗（只带模型，没有物品；r99）。 */
    public static final BowlBlock BOWL_BLOCK = reg().blocks().registerFactory(
        HotPot.id("bowl"), properties -> new BowlBlock(properties));
    /** 摆在桌上的橙子饮料（只带模型，r99）。 */
    public static final DrinkBlock DRINK_BLOCK = reg().blocks().registerFactory(
        HotPot.id("drink"), properties -> new DrinkBlock(properties));
    /** 摆在桌上的玻璃杯（只带模型，2026-10-25）。 */
    public static final CupBlock CUP_BLOCK = reg().blocks().registerFactory(
        HotPot.id("cup"), properties -> new CupBlock(properties));

    /** 装着饮料的炼药锅（2026-10-27，见 DrinkCauldronBlock；没有物品，拆了掉原版炼药锅）。 */
    public static final DrinkCauldronBlock DRINK_CAULDRON = reg().blocks().registerFactory(
        HotPot.id("drink_cauldron"), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAULDRON), DrinkCauldronBlock::new);

    /** 摆盘子的那一格：一格里能摆好几个盘子。 */
    public static final PlateHolderBlock PLATE_HOLDER = reg().blocks().registerFactory(
        HotPot.id("plates"), properties -> new PlateHolderBlock(properties));
    public static BlockEntityType<PlateHolderBlockEntity> PLATE_HOLDER_BE;

    /** 坐条凳时骑的看不见的座位实体。 */
    public static final net.minecraft.world.entity.EntityType<SeatEntity> SEAT = reg().entityTypes().register( HotPot.id("seat"),
        net.minecraft.world.entity.EntityType.Builder.<SeatEntity>of(SeatEntity::new, net.minecraft.world.entity.MobCategory.MISC)
            .sized(0.25F, 0.01F)
            // mryt233 改（26.x，同步 26.1 修复清单第 5 条）：26.x 原版不让骑乘 noSave 类型的实体，带 noSave 凳子坐不上去；
            // 座位实体自己的 shouldBeSaved() 仍返回 false，所以照样不存档。
            .noSummon()
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, HotPot.id("seat"))));

    static {
        for (String wood : TABLE_WOODS) {
            net.minecraft.world.level.block.Block planks = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(wood + "_planks"));
            HotPotTableBlock table = reg().blocks().registerFactory(
                HotPot.id(wood + "_hotpot_table"), properties -> new HotPotTableBlock(planks, properties));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(reg().items().registerFactory(
                HotPot.id(wood + "_hotpot_table"), properties -> new BlockItem(table, properties.useBlockDescriptionPrefix())));
        }
        for (String wood : TABLE_WOODS) {
            net.minecraft.world.level.block.Block planks = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(wood + "_planks"));
            BenchBlock bench = reg().blocks().registerFactory(
                HotPot.id(wood + "_stool"), properties -> new BenchBlock(planks, properties));   // 板凳 = Stool（r102 改 ID）
            BENCHES.add(bench);
            BENCH_ITEMS.add(reg().items().registerFactory(
                HotPot.id(wood + "_stool"), properties -> new BlockItem(bench, properties.useBlockDescriptionPrefix())));
        }
        for (String wood : TABLE_WOODS) {
            net.minecraft.world.level.block.Block planks = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(wood + "_planks"));
            TallHotPotTableBlock table = reg().blocks().registerFactory(
                HotPot.id(wood + "_tall_hotpot_table"), properties -> new TallHotPotTableBlock(planks, properties));
            TALL_TABLES.add(table);
            TALL_TABLE_ITEMS.add(reg().items().registerFactory(
                HotPot.id(wood + "_tall_hotpot_table"), properties -> new BlockItem(table, properties.useBlockDescriptionPrefix())));
            BenchBlock bench = reg().blocks().registerFactory(
                HotPot.id(wood + "_bench"), properties -> new BenchBlock(planks, true, properties));   // 条凳 = Bench
            TALL_BENCHES.add(bench);
            TALL_BENCH_ITEMS.add(reg().items().registerFactory(
                HotPot.id(wood + "_bench"), properties -> new BlockItem(bench, properties.useBlockDescriptionPrefix())));
        }
        // 石料 / 金属桌面的火锅桌（2026-10-26 用户：平滑石头、磨制安山岩……铁块；桌面是这种材质，下面红砖）：
        // 矮桌、高桌各一张，模型等由 tools/gen_stone_tables.py 生成，ID = <材质>_hotpot_table / <材质>_tall_hotpot_table。
        // 声音、地图颜色取那种原版方块；不会着火。新增
        for (String[] m : STONE_TABLES) {
            net.minecraft.world.level.block.Block material = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(m[1]));
            HotPotTableBlock table = reg().blocks().registerFactory(
                HotPot.id(m[0] + "_hotpot_table"), properties -> new HotPotTableBlock(material, properties));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(reg().items().registerFactory(
                HotPot.id(m[0] + "_hotpot_table"), properties -> new BlockItem(table, properties.useBlockDescriptionPrefix())));
            TallHotPotTableBlock tall = reg().blocks().registerFactory(
                HotPot.id(m[0] + "_tall_hotpot_table"), properties -> new TallHotPotTableBlock(material, properties));
            TALL_TABLES.add(tall);
            TALL_TABLE_ITEMS.add(reg().items().registerFactory(
                HotPot.id(m[0] + "_tall_hotpot_table"), properties -> new BlockItem(tall, properties.useBlockDescriptionPrefix())));
        }
    }

    public static final BlockItem STOVE_ITEM = reg().items().registerFactory(
        HotPot.id("brick_stove"), properties -> new BlockItem(STOVE, properties.useBlockDescriptionPrefix()));

    public static final BlockItem TALL_STOVE_ITEM = reg().items().registerFactory(
        HotPot.id("tall_brick_stove"), properties -> new BlockItem(TALL_STOVE, properties.useBlockDescriptionPrefix()));

    public static final BlockItem COPPER_HOT_POT_ITEM = reg().items().registerFactory(
        HotPot.id(HotPotMaterial.COPPER.idPath()), properties -> new BlockItem(COPPER_HOT_POT, properties.useBlockDescriptionPrefix()));

    public static final BlockItem IRON_HOT_POT_ITEM = reg().items().registerFactory(
        HotPot.id(HotPotMaterial.IRON.idPath()), properties -> new BlockItem(IRON_HOT_POT, properties.useBlockDescriptionPrefix()));
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
    public static final Item RAW_TRIPE = reg().items().registerFactory( HotPot.id("tripe"), properties -> new Item(properties.food(new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(2).saturationModifier(0.3F).alwaysEdible().build())));
    public static final Item COOKED_TRIPE = reg().items().registerFactory( HotPot.id("cooked_tripe"), properties -> new Item(properties.food(new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(5).saturationModifier(0.6F).alwaysEdible().build())));

    /**
     * 切菜（2026-10-23 用户定，新增）：生牛肉 / 生羊肉在砧板上用铁菜刀点一下（或切石机）切成 3 卷；
     * 每卷熟的回 3 饥饿（3 卷 = 9，比整块牛排多一点，算切开涮的奖励），生的回 1；锅里涮得比整块肉快一半（熔炉配方 100 tick）。
     */
    public static final Item FATTY_BEEF_ROLL = foodItem("fatty_beef_roll", 1, 0.3F);
    public static final Item COOKED_FATTY_BEEF_ROLL = foodItem("cooked_fatty_beef_roll", 3, 0.6F);
    public static final Item MUTTON_ROLL = foodItem("mutton_roll", 1, 0.3F);
    public static final Item COOKED_MUTTON_ROLL = foodItem("cooked_mutton_roll", 3, 0.6F);
    /**
     * 吊龙（用户定，新增）：牛 / 哞菇 25% 掉一块牛脊肉（生吃同生牛肉 3 饥饿；整块烤 = 熟牛脊肉，数值同熟牛排），
     * 砧板上一刀（或切石机）切成 3 片吊龙；每片熟的回 3、生的回 1；锅里涮得最快（熔炉 50 tick，肥牛卷的一半）。
     */
    public static final Item BEEF_LOIN = foodItem("beef_loin", 3, 0.3F);
    /** 熟牛脊肉（用户要）：整块烤熟，饥饿和饱和度同原版熟牛排（8 / 0.8）。 */
    public static final Item COOKED_BEEF_LOIN = foodItem("cooked_beef_loin", 8, 0.8F);
    public static final Item BEEF_LOIN_SLICE = foodItem("beef_loin_slice", 1, 0.3F);
    public static final Item COOKED_BEEF_LOIN_SLICE = foodItem("cooked_beef_loin_slice", 3, 0.6F);
    /** 铁菜刀：耐久同铁剑（250），切一次掉 1 点；也能当武器，伤害比铁剑低（4）。 */
    public static final Item KITCHEN_KNIFE = reg().items().registerFactory( HotPot.id("iron_kitchen_knife"), properties -> new Item(properties.durability(250).attributes(
            net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 3, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.2F, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build())));
    public static final CuttingBoardBlock CUTTING_BOARD = reg().blocks().registerFactory( HotPot.id("cutting_board"), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).noOcclusion().dynamicShape(), CuttingBoardBlock::new);   // 选中框随板上有没有东西变（坑：形状跟着方块实体变就要 dynamicBounds）
    public static final BlockItem CUTTING_BOARD_ITEM = reg().items().registerFactory( HotPot.id("cutting_board"), properties -> new BlockItem(CUTTING_BOARD, properties.useBlockDescriptionPrefix()));
    public static BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD_BE;

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
     * 蒜泥是蘸料台第 10 种调料（Seasoning.GARLIC_PASTE）。新增
     */
    public static final Item ORANGE = foodItem("orange", 4, 0.3F);
    public static final Item GARLIC = foodItem("garlic", 1, 0.1F);
    public static final Item GARLIC_PASTE = foodItem("garlic_paste", 1, 0.1F);
    /**
     * 虾（2026-10-26 用户：加虾和虾滑）：钓鱼钓到带壳的虾（ShrimpLoot）；砧板上菜刀切一刀剥成虾仁（去壳去头，也能用切石机）；
     * 虾仁放砧板上擀面杖拍三下成一颗虾滑（Pounding）。三样都能下锅涮 / 烤熟，熟的回 3 / 3 / 4。新增
     */
    public static final Item SHRIMP = foodItem("shrimp", 1, 0.2F);
    public static final Item COOKED_SHRIMP = foodItem("cooked_shrimp", 3, 0.5F);
    public static final Item PEELED_SHRIMP = foodItem("peeled_shrimp", 1, 0.2F);
    public static final Item COOKED_PEELED_SHRIMP = foodItem("cooked_peeled_shrimp", 3, 0.6F);
    public static final Item SHRIMP_PASTE = foodItem("shrimp_paste", 1, 0.2F);
    public static final Item COOKED_SHRIMP_PASTE = foodItem("cooked_shrimp_paste", 4, 0.6F);
    /** 火腿肠（2026-10-29 用户：不用单独淀粉，原版小麦 + 猪肉加工）：猪排切成猪肉末，再和小麦合成；原有猪肉捣丸子不变。 */
    public static final Item MINCED_PORK = reg().items().registerFactory( HotPot.id("minced_pork"), properties -> new Item(properties));
    public static final Item RAW_HAM_SAUSAGE = foodItem("raw_ham_sausage", 1, 0.3F);
    /** 火腿肠 = 生火腿肠煮熟（2026-10-29 用户改名：熟火腿肠改叫「火腿肠」，ID 同步成 ham_sausage）。 */
    public static final Item HAM_SAUSAGE = foodItem("ham_sausage", 3, 0.6F);
    /** 火腿肠片（2026-10-29 用户：火腿肠放菜板上切，一刀出 3 片；还能再加热）。 */
    public static final Item HAM_SAUSAGE_SLICE = foodItem("ham_sausage_slice", 1, 0.3F);
    /** 熟火腿肠片：火腿肠片再加热一次（2026-10-29 用户）。 */
    public static final Item COOKED_HAM_SAUSAGE_SLICE = foodItem("cooked_ham_sausage_slice", 2, 0.5F);
    // ==== tools/new_item.py：新物品的注册插在这行上面（mryt233 2026-11-01，别删这行）====
    /** 番茄（2026-10-25）：打草掉（GarnishLoot，同大蒜 15%），能吃；做番茄锅底料。 */
    public static final Item TOMATO = foodItem("tomato", 2, 0.3F);
    /** 游戏内教程书「火锅指南」（2026-10-25 用户）：菜刀 + 书无序合成，右键打开（GuideBookItem / client.GuideBookScreen）。 */
    public static final Item HOTPOT_GUIDE = reg().items().registerFactory( HotPot.id("hotpot_guide"), properties -> new GuideBookItem(properties));
    /** 锅底料（2026-10-25）：拿着右键锅 = 倒进新汤底（SoupBase.TOMATO / THREE_FRESH），一块一锅。 */
    public static final Item TOMATO_HOTPOT_BASE = reg().items().registerFactory( HotPot.id("tomato_hotpot_base"), properties -> new Item(properties));
    public static final Item THREE_FRESH_HOTPOT_BASE = reg().items().registerFactory( HotPot.id("three_fresh_hotpot_base"), properties -> new Item(properties));
    /**
     * 汤底桶（2026-10-26 用户）：只能用空桶从番茄锅 / 三鲜锅里舀出来（没有配方），倒回锅里就是那种汤（SoupBase 的 source）；
     * 不是液体，不能倒在地上。图标照原版水桶 / 岩浆桶的样子（tools/gen_garden.py）。
     */
    public static final Item TOMATO_SOUP_BUCKET = reg().items().registerFactory( HotPot.id("tomato_soup_bucket"), properties -> new Item(properties.stacksTo(1)));
    public static final Item THREE_FRESH_SOUP_BUCKET = reg().items().registerFactory( HotPot.id("three_fresh_soup_bucket"), properties -> new Item(properties.stacksTo(1)));
    /** 熟海带（2026-10-25 用户）：原版海带在锅里煮出来（HotPotCooking.potResult），熔炉 / 烟熏炉照旧是干海带；回 2。 */
    public static final Item COOKED_KELP = foodItem("cooked_kelp", 2, 0.3F);
    public static final Item COOKED_TOFU_CUBE = foodItem("cooked_tofu_cube", 2, 0.6F);

    /**
     * 丸子（2026-10-23 用户定）：木棒在砧板上捣三下出 3 个生丸子（Pounding）；熟的每个回 3，香菜版（2 生丸子 + 1 香菜 → 2 个）每个回 4。
     * 鱼丸用生鳕鱼 / 生鲑鱼。擀面杖（用户：木棒改成擀面杖的样子，名字和 ID 一起改）耐久 120，捣一下掉 1 点。
     */
    public static final Item ROLLING_PIN = reg().items().registerFactory( HotPot.id("rolling_pin"), properties -> new Item(properties.durability(120)));
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
    public static final Item POUNDED_MEAT = reg().items().registerFactory( HotPot.id("pounded_meat"), properties -> new Item(properties));

    /**
     * 食物数值先问配置文件有没有覆盖，没有就用这里写的默认值（见 Config）。
     * 注册进组件的数值只是默认值；真正吃的时候 {@link ConfiguredFoodItem} 会再按配置算一次，
     * 这样改配置以后 /orientshotpot reload 就能生效，不用重启游戏。
     * 本模组的食物都是「饱了也能吃」：饥饿条满了不再涨，饱和度照常加到 20（用户 2026-11-01：
     * 原版吃法就能一直吃；另装 Always Eat 也不冲突，它只是提前开始吃，结算仍走这里。mryt233 改）。
     */
    private static Item foodItem(String id, int nutrition, float saturation) {
        Config.rememberFood(id, nutrition, saturation);
        return reg().items().registerFactory( HotPot.id(id), properties -> new ConfiguredFoodItem(id, properties.food(new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build())));
    }

    /** 一碗汤（r99）：顺序同 SoupBase（清汤、高汤、菌汤、红汤）。 */
    public static final java.util.List<SoupBowlItem> SOUP_BOWLS = java.util.Arrays.stream(SoupBase.values())
        .map(soup -> reg().items().registerFactory( HotPot.id(soup.bowlId()), properties -> new SoupBowlItem(soup, properties)))
        .toList();

    /** 饮料（r99 橙子饮料，r101 维二）和喝完的空瓶。 */
    public static final DrinkItem ORANGE_DRINK = reg().items().registerFactory(
        HotPot.id("orange_drink"), properties -> new DrinkItem(DrinkKind.ORANGE_DRINK, properties));
    public static final EmptyBottleItem EMPTY_ORANGE_DRINK_BOTTLE = reg().items().registerFactory(
        HotPot.id("empty_orange_drink_bottle"), properties -> new EmptyBottleItem(DrinkKind.ORANGE_DRINK, properties));
    public static final DrinkItem PLANT_PROTEIN_DRINK = reg().items().registerFactory(
        HotPot.id("plant_protein_drink"), properties -> new DrinkItem(DrinkKind.PLANT_PROTEIN_DRINK, properties));
    public static final EmptyBottleItem EMPTY_PLANT_PROTEIN_DRINK_BOTTLE = reg().items().registerFactory(
        HotPot.id("empty_plant_protein_drink_bottle"), properties -> new EmptyBottleItem(DrinkKind.PLANT_PROTEIN_DRINK, properties));
    /** 之晗可乐（原名 之晗可乐，2026-10-26 用户改名）（2026-10-25 用户定名）：一瓶 4 口，喝完剩空瓶，满瓶空瓶都能摆桌上（和另外两种饮料一样）。 */
    public static final DrinkItem ORIENT_COLA = reg().items().registerFactory(
        HotPot.id("orient_cola"), properties -> new DrinkItem(DrinkKind.ORIENT_COLA, properties));
    public static final EmptyBottleItem EMPTY_ORIENT_COLA_BOTTLE = reg().items().registerFactory(
        HotPot.id("empty_orient_cola_bottle"), properties -> new EmptyBottleItem(DrinkKind.ORIENT_COLA, properties));
    /** 玻璃杯（2026-10-25 用户）：空杯 + 三种饮料各一种装满的杯子（一口），见 CupItem。 */
    public static final CupItem GLASS_CUP = reg().items().registerFactory( HotPot.id("glass_cup"), properties -> new CupItem(null, properties));
    public static final CupItem ORANGE_DRINK_CUP = reg().items().registerFactory( HotPot.id("orange_drink_cup"), properties -> new CupItem(DrinkKind.ORANGE_DRINK, properties));
    public static final CupItem PLANT_PROTEIN_DRINK_CUP = reg().items().registerFactory(
        HotPot.id("plant_protein_drink_cup"), properties -> new CupItem(DrinkKind.PLANT_PROTEIN_DRINK, properties));
    public static final CupItem ORIENT_COLA_CUP = reg().items().registerFactory( HotPot.id("orient_cola_cup"), properties -> new CupItem(DrinkKind.ORIENT_COLA, properties));
    /** 一杯水、一杯牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒，桶不会空；空杯对着水源右键也能舀一杯水。 */
    public static final CupItem WATER_CUP = reg().items().registerFactory( HotPot.id("water_cup"), properties -> new CupItem(DrinkKind.WATER, properties));
    public static final CupItem MILK_CUP = reg().items().registerFactory( HotPot.id("milk_cup"), properties -> new CupItem(DrinkKind.MILK, properties));

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
            case WATER, MILK -> net.minecraft.world.item.Items.GLASS_BOTTLE;   // 用不到：水、牛奶没有瓶装
        };
    }

    public static final Item CHOPSTICKS = reg().items().registerFactory(
        HotPot.id("bamboo_chopsticks"), properties -> new ChopsticksItem(properties));

    // ------------------------------------------------------------------ 蘸料（r103，用户定的玩法见 Seasoning / SauceStationBlock）
    /** 小葱、香菜：打草掉落（各 25%），能吃回 1 点饥饿值；放进蘸料台变成葱花、香菜碎末。 */
    public static final Item SCALLION = reg().items().registerFactory( HotPot.id("scallion"), properties -> new Item(properties.food(new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.1F).alwaysEdible().build())));
    public static final Item CILANTRO = reg().items().registerFactory( HotPot.id("cilantro"), properties -> new Item(properties.food(new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.1F).alwaysEdible().build())));
    /** 一碗蘸料里放了哪几种、还能蘸几口。 */
    public static final DataComponentType<SauceContents> SAUCE = reg().components().register( HotPot.id("sauce"),
        DataComponentType.<SauceContents>builder().persistent(SauceContents.CODEC).networkSynchronized(SauceContents.PACKET_CODEC).build());
    /** 菜蘸过哪几种调料（Seasoning.bit 的组合）；吃下去时生效（LivingEntityEatMixin）。 */
    public static final DataComponentType<Integer> DIPPED = reg().components().register( HotPot.id("dipped"),
        DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
    public static final DippingSauceItem DIPPING_SAUCE = reg().items().registerFactory(
        HotPot.id("dipping_sauce"), properties -> new DippingSauceItem(properties));
    /** 蘸料台：11 种原版木头各一个 + 铁的（用户定）；ID = <木头>_sauce_station / iron_sauce_station。 */
    public static final java.util.List<SauceStationBlock> SAUCE_STATIONS = new java.util.ArrayList<>();
    public static final java.util.List<BlockItem> SAUCE_STATION_ITEMS = new java.util.ArrayList<>();

    static {
        java.util.List<String> bodies = new java.util.ArrayList<>(TABLE_WOODS);
        bodies.add("iron");
        for (String body : bodies) {
            boolean iron = body.equals("iron");
            net.minecraft.world.level.block.Block like = iron ? net.minecraft.world.level.block.Blocks.IRON_BLOCK
                : BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(body + "_planks"));
            SauceStationBlock station = reg().blocks().registerFactory( HotPot.id(body + "_sauce_station"), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(like).noOcclusion(), SauceStationBlock::new);
            SAUCE_STATIONS.add(station);
            SAUCE_STATION_ITEMS.add(reg().items().registerFactory( HotPot.id(body + "_sauce_station"), properties -> new BlockItem(station, properties.useBlockDescriptionPrefix())));
        }
    }

    /** 蘸料台的「上半截」：看不见，占住头顶那一格（没有物品；2026-10-23）。 */
    public static final SauceStationTopBlock SAUCE_STATION_TOP = reg().blocks().registerFactory(
        HotPot.id("sauce_station_top"), properties -> new SauceStationTopBlock(properties));

    public static BlockEntityType<SauceStationBlockEntity> SAUCE_STATION_BE;
    /** 由平台在 {@link #register} 里注册并赋值。 */
    public static net.minecraft.world.inventory.MenuType<SauceStationScreenHandler> SAUCE_STATION_SCREEN;

    /** 装着东西的锅物品：汤底、食材、熟度原样打包（格式同锅方块实体存档）。 */
    public static final DataComponentType<CustomData> POT_CONTENTS = reg().components().register( HotPot.id("pot_contents"),
        DataComponentType.<CustomData>builder()
            .persistent(CustomData.CODEC)
            .networkSynchronized(CustomData.STREAM_CODEC)
            .build());

    /** 独立锅方块仍保留，兼容未放在灶台上的旧世界与自由摆放。 */
    public static BlockEntityType<HotPotBlockEntity> HOT_POT_BE;

    /** 组合灶台方块实体：燃料、锅库存和烹饪数据全部在同一个位置。 */
    public static BlockEntityType<StoveBlockEntity> STOVE_BE;

    /** 条凳的方块实体（只用来画斜腿）。 */
    public static BlockEntityType<BenchBlockEntity> BENCH_BE;

    public static net.minecraft.world.inventory.MenuType<HotPotScreenHandler> HOT_POT_SCREEN;

    /** 灶台底座的灶膛燃料界面。 */
    public static net.minecraft.world.inventory.MenuType<StoveScreenHandler> STOVE_SCREEN;

    /** 本模组专属分类：使用 Fabric 的追加式标签页，自动排在原版标签页之后并进入后续页面。 */
    public static final ResourceKey<CreativeModeTab> ITEM_GROUP_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, HotPot.id("hotpot"));

    /** 由平台在 {@link #register} 里创建并赋值。 */
    public static CreativeModeTab ITEM_GROUP;

    /** 创造栏里放什么：纯数据，平台只负责套上加载器的标签页外壳。 */
    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters context, cn.blockforge.orient.framework.loader.CreativeTabOutput entries) {
                entries.accept(STOVE_ITEM);
                entries.accept(TALL_STOVE_ITEM);
                // 锅：先铁锅全部、再铜锅全部，每种按 九宫格 / S / O / 直隔 / 田字（用户 2026-10-24 定）
                entries.accept(IRON_HOT_POT_ITEM);
                entries.accept(IRON_S_POT_ITEM);
                entries.accept(IRON_O_POT_ITEM);
                entries.accept(IRON_SQUARE_POT_ITEM);
                entries.accept(IRON_SPLIT_POT_ITEM);
                entries.accept(IRON_FOUR_POT_ITEM);
                entries.accept(COPPER_HOT_POT_ITEM);
                entries.accept(COPPER_S_POT_ITEM);
                entries.accept(COPPER_O_POT_ITEM);
                entries.accept(COPPER_SQUARE_POT_ITEM);
                entries.accept(COPPER_SPLIT_POT_ITEM);
                entries.accept(COPPER_FOUR_POT_ITEM);
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
                entries.accept(TOFU);
                entries.accept(TOFU_CUBE);
                entries.accept(COOKED_TOFU_CUBE);
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
                entries.accept(TOMATO_HOTPOT_BASE);
                entries.accept(THREE_FRESH_HOTPOT_BASE);
                entries.accept(TOMATO_SOUP_BUCKET);
                entries.accept(THREE_FRESH_SOUP_BUCKET);
                entries.accept(DIPPING_SAUCE);
                HOTPOT_TABLE_ITEMS.forEach(entries::accept);
                BENCH_ITEMS.forEach(entries::accept);
                TALL_TABLE_ITEMS.forEach(entries::accept);
                TALL_BENCH_ITEMS.forEach(entries::accept);
                PLATE_ITEMS.forEach(entries::accept);
                SAUCE_STATION_ITEMS.forEach(entries::accept);
    }

    private static RegistrationApi reg() {
        return Framework.platform().registration();
    }

    private HotPotContent() {
    }

    /** 在模组初始化阶段调用，确保注册顺序完成后输出一条可追踪日志。 */
    public static void register() {
        // 创造栏 / 菜单 / 方块实体类型统一走分域 Registrar（P1 第三批）
        RegistrationApi reg = reg();
        ITEM_GROUP = reg.creativeTabs().register(ITEM_GROUP_KEY.identifier(),
            net.minecraft.network.chat.Component.translatable("itemGroup." + HotPot.MOD_ID),
            HotPotContent::tabIcon, HotPotContent::fillCreativeTab);
        HOT_POT_SCREEN = reg.menus().register(HotPot.id("hot_pot"),
            (syncId, inventory, pos) -> new HotPotScreenHandler(syncId, inventory, pos));
        STOVE_SCREEN = reg.menus().register(HotPot.id("stove_fuel"),
            (syncId, inventory, pos) -> new StoveScreenHandler(syncId, inventory, pos));
        SAUCE_STATION_SCREEN = reg.menus().register(HotPot.id("sauce_station"),
            (syncId, inventory, pos) -> new SauceStationScreenHandler(syncId, inventory, pos));
        PLATE_HOLDER_BE = reg.blockEntities().register(HotPot.id("plates"), PlateHolderBlockEntity::new, PLATE_HOLDER);
        CUTTING_BOARD_BE = reg.blockEntities().register(HotPot.id("cutting_board"), CuttingBoardBlockEntity::new, CUTTING_BOARD);
        SAUCE_STATION_BE = reg.blockEntities().register(HotPot.id("sauce_station"), SauceStationBlockEntity::new,
            SAUCE_STATIONS.toArray(new net.minecraft.world.level.block.Block[0]));
        HOT_POT_BE = reg.blockEntities().register(HotPot.id("hot_pot"), HotPotBlockEntity::new,
            COPPER_HOT_POT, IRON_HOT_POT, COPPER_S_POT, COPPER_O_POT, IRON_S_POT, IRON_O_POT,
            COPPER_SQUARE_POT, IRON_SQUARE_POT, COPPER_SPLIT_POT, IRON_SPLIT_POT, COPPER_FOUR_POT, IRON_FOUR_POT);
        STOVE_BE = reg.blockEntities().register(HotPot.id("stove"), StoveBlockEntity::new, STOVE, TALL_STOVE);
        BENCH_BE = reg.blockEntities().register(HotPot.id("bench"), BenchBlockEntity::new,
            TALL_BENCHES.toArray(new net.minecraft.world.level.block.Block[0]));
        // 触碰静态字段，明确让物品组在客户端和服务端都完成初始化。
        ITEM_GROUP.getDisplayName();
        TripeLoot.register();
        GarnishLoot.register();
        OrangeLoot.register();
        DrinkCauldronBlock.registerEmptyCauldron();
        HotPotCooking.potResult(net.minecraft.world.item.Items.KELP, COOKED_KELP);
        HotPotCriterion.register();
        SoupBucketRecipe.register();
        HotPot.LOGGER.info("[Orient的火锅物语] 内容已注册（组合灶台 + 铜锅 + 铁锅 + 独立分类）");
    }

    /**
     * 分类页图标：铁锅 + 沸腾的红汤 + 漂着几样原版食物。用自定义模型数据 1 切到
     * models/item/iron_hot_pot_tab_icon.json，普通铁锅物品不受影响。
     */
    public static ItemStack tabIcon() {
        ItemStack icon = new ItemStack(IRON_HOT_POT_ITEM);
        icon.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
            new net.minecraft.world.item.component.CustomModelData(java.util.List.of((float) (1)), java.util.List.of(), java.util.List.of(), java.util.List.of()));
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
        return reg().blocks().registerFactory( HotPot.id(material.idPath()), properties -> new HotPotBlock(material, properties));
    }

    private static BlockItem potItem(HotPotBlock block) {
        return reg().items().registerFactory( HotPot.id(block.material().idPath()), properties -> new BlockItem(block, properties.useBlockDescriptionPrefix()));
    }

    public static Identifier id(String path) {
        return HotPot.id(path);
    }
}
