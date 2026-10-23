package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.entity.BlockEntityType;
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

    /** 每种原版木板一种火锅桌（顺序同原版木板），围着灶台拼成 3x3 的大桌，顶面和灶台台面齐平。 */
    public static final java.util.List<String> TABLE_WOODS = java.util.List.of(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped");
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
    // 1.20.1 移植（Claude 改）：筷子夹着的食物、装着东西的锅原来是注册的物品组件，1.20 没有组件，改存 NBT（见 ItemData）。

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
                if (size == PlateSize.BOWL || size == PlateSize.DRINK) {
                    continue;   // 碗、果橙多不是盘子（只借用盘子格摆放）
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
    /** 摆在桌上的果橙多（只带模型，r99）。 */
    public static final DrinkBlock DRINK_BLOCK = Registry.register(Registries.BLOCK,
        GeneratedMod.id("drink"), new DrinkBlock());

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
            .setDimensions(0.25F, 0.01F)
            .disableSaving()
            .disableSummon()
            .maxTrackingRange(10)
            .build("seat"));

    static {
        for (String wood : TABLE_WOODS) {
            net.minecraft.block.Block planks = Registries.BLOCK.get(new Identifier(wood + "_planks"));
            HotPotTableBlock table = Registry.register(Registries.BLOCK,
                GeneratedMod.id(wood + "_hotpot_table"), new HotPotTableBlock(planks));
            HOTPOT_TABLES.add(table);
            HOTPOT_TABLE_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(wood + "_hotpot_table"), new BlockItem(table, new Item.Settings())));
        }
        for (String wood : TABLE_WOODS) {
            net.minecraft.block.Block planks = Registries.BLOCK.get(new Identifier(wood + "_planks"));
            BenchBlock bench = Registry.register(Registries.BLOCK,
                GeneratedMod.id(wood + "_stool"), new BenchBlock(planks));   // 板凳 = Stool（r102 改 ID）
            BENCHES.add(bench);
            BENCH_ITEMS.add(Registry.register(Registries.ITEM,
                GeneratedMod.id(wood + "_stool"), new BlockItem(bench, new Item.Settings())));
        }
        for (String wood : TABLE_WOODS) {
            net.minecraft.block.Block planks = Registries.BLOCK.get(new Identifier(wood + "_planks"));
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

    /** 毛肚（牛掉落，r93）：生的回 2 点饥饿；熟的回 5 点。熔炉 / 烟熏炉 / 营火都能烤，锅里涮得比牛肉快一半（熔炉配方 100 tick）。 */
    public static final Item RAW_TRIPE = Registry.register(Registries.ITEM, GeneratedMod.id("tripe"),
        new Item(new Item.Settings().food(new net.minecraft.item.FoodComponent.Builder()
            .hunger(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_TRIPE = Registry.register(Registries.ITEM, GeneratedMod.id("cooked_tripe"),
        new Item(new Item.Settings().food(new net.minecraft.item.FoodComponent.Builder()
            .hunger(5).saturationModifier(0.6F).build())));

    /** 一碗汤（r99）：顺序同 SoupBase（清汤、浓汤、菌汤、红汤）。 */
    public static final java.util.List<SoupBowlItem> SOUP_BOWLS = java.util.Arrays.stream(SoupBase.values())
        .map(soup -> Registry.register(Registries.ITEM, GeneratedMod.id(soup.bowlId()),
            new SoupBowlItem(soup)))
        .toList();

    /** 饮料（r99 果橙多，r101 维二）和喝完的空瓶。 */
    public static final DrinkItem GUOCHENGDUO = Registry.register(Registries.ITEM,
        GeneratedMod.id("orange_drink"), new DrinkItem(DrinkKind.GUOCHENGDUO));
    public static final EmptyBottleItem EMPTY_GUOCHENGDUO = Registry.register(Registries.ITEM,
        GeneratedMod.id("empty_orange_drink_bottle"), new EmptyBottleItem(DrinkKind.GUOCHENGDUO));
    public static final DrinkItem WEIER = Registry.register(Registries.ITEM,
        GeneratedMod.id("plant_protein_drink"), new DrinkItem(DrinkKind.WEIER));
    public static final EmptyBottleItem EMPTY_WEIER = Registry.register(Registries.ITEM,
        GeneratedMod.id("empty_plant_protein_drink_bottle"), new EmptyBottleItem(DrinkKind.WEIER));

    public static Item emptyBottle(DrinkKind kind) {
        return kind == DrinkKind.WEIER ? EMPTY_WEIER : EMPTY_GUOCHENGDUO;
    }

    public static final Item CHOPSTICKS = Registry.register(Registries.ITEM,
        GeneratedMod.id("bamboo_chopsticks"),
        new Item(new Item.Settings().maxCount(1).rarity(Rarity.COMMON)));

    /** 独立锅方块仍保留，兼容未放在灶台上的旧世界与自由摆放。 */
    public static final BlockEntityType<HotPotBlockEntity> HOT_POT_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("hot_pot"),
        FabricBlockEntityTypeBuilder.create(HotPotBlockEntity::new,
            COPPER_HOT_POT, IRON_HOT_POT).build());

    /** 组合灶台方块实体：燃料、锅库存和烹饪数据全部在同一个位置。 */
    public static final BlockEntityType<StoveBlockEntity> STOVE_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("stove"),
        FabricBlockEntityTypeBuilder.create(StoveBlockEntity::new, STOVE, TALL_STOVE).build());

    /** 条凳的方块实体（只用来画斜腿）。 */
    public static final BlockEntityType<BenchBlockEntity> BENCH_BE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, GeneratedMod.id("bench"),
        FabricBlockEntityTypeBuilder.create(BenchBlockEntity::new,
            TALL_BENCHES.toArray(new net.minecraft.block.Block[0])).build());

    public static final ExtendedScreenHandlerType<HotPotScreenHandler> HOT_POT_SCREEN =
        Registry.register(Registries.SCREEN_HANDLER, GeneratedMod.id("hot_pot"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, buf) -> new HotPotScreenHandler(syncId, playerInventory, buf.readBlockPos())));

    /** 灶台底座的灶膛燃料界面。 */
    public static final ExtendedScreenHandlerType<StoveScreenHandler> STOVE_SCREEN =
        Registry.register(Registries.SCREEN_HANDLER, GeneratedMod.id("stove_fuel"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, buf) -> new StoveScreenHandler(syncId, playerInventory, buf.readBlockPos())));

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
                entries.add(IRON_HOT_POT_ITEM);
                entries.add(COPPER_HOT_POT_ITEM);
                entries.add(CHOPSTICKS);
                entries.add(RAW_TRIPE);
                entries.add(COOKED_TRIPE);
                SOUP_BOWLS.forEach(entries::add);
                entries.add(GUOCHENGDUO);
                entries.add(EMPTY_GUOCHENGDUO);
                entries.add(WEIER);
                entries.add(EMPTY_WEIER);
                HOTPOT_TABLE_ITEMS.forEach(entries::add);
                BENCH_ITEMS.forEach(entries::add);
                TALL_TABLE_ITEMS.forEach(entries::add);
                TALL_BENCH_ITEMS.forEach(entries::add);
                PLATE_ITEMS.forEach(entries::add);
            })
            .build());

    private HotPotContent() {
    }

    /** 在模组初始化阶段调用，确保注册顺序完成后输出一条可追踪日志。 */
    public static void register() {
        // 触碰静态字段，明确让物品组在客户端和服务端都完成初始化。
        ITEM_GROUP.getDisplayName();
        TripeLoot.register();
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
            }
        }
        // 灶上有锅时，对着锅挖只挖下锅（连汤带菜打包掉落），灶台留着；对着灶身挖才挖整个灶台
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register(
            (world, player, pos, state, blockEntity) -> {
                if (blockEntity instanceof StoveBlockEntity stove && StoveBlock.aimsAtPot(state, pos, player)) {
                    stove.breakPot(player);
                    return false;
                }
                // 盘子格：只挖准星对着的那一个盘子（连菜掉出来），别的盘子留着（r87，Claude）
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
        ItemData.setModel(icon, 1);
        return icon;
    }

    public static HotPotBlock blockFor(HotPotMaterial material) {
        return material == HotPotMaterial.COPPER ? COPPER_HOT_POT : IRON_HOT_POT;
    }

    public static BlockItem itemFor(HotPotMaterial material) {
        return material == HotPotMaterial.COPPER ? COPPER_HOT_POT_ITEM : IRON_HOT_POT_ITEM;
    }

    public static Identifier id(String path) {
        return GeneratedMod.id(path);
    }
}
