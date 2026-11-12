// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.BowlBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopstickRestBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.CupBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceBottleBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceBottleKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableObjects;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 4G/4H 开发探针（不进发布包）：在真实 Fabric / NeoForge 运行时上验证全部内建 {@link TableItem}/{@link TableBlock}
 * 合约、外部自定义扩展接口（{@code -Dhotpot.opt4g=true}）以及 0.2.2-alpha 旧档 {@code plate_holder} 读存一致。
 * 作者：mryt233
 */
public final class Opt4gProbe {
    private static boolean installed;
    private static CustomTrayBlock customTrayBlock;
    private static CustomTrayItem customTrayItem;

    private Opt4gProbe() {
    }

    public static void install(RegistrationApi registration) {
        if (installed) {
            return;
        }
        installed = true;
        customTrayBlock = registration.blocks().register(
            HotPot.id("opt4gtest_custom_tray"), new CustomTrayBlock());
        customTrayItem = registration.items().register(
            HotPot.id("opt4gtest_custom_tray"), new CustomTrayItem(customTrayBlock));
        HotPot.LOGGER.info("[OPT4G] installed external TableBlock/TableItem sample");
    }

    public static String run(ServerPlayer player, String action, BlockPos pos) {
        try {
            return switch (action) {
                case "audit" -> auditAll(player);
                case "external" -> verifyExternal(player, pos);
                case "legacy" -> verifyLegacyRoundTrip(player, pos);
                default -> "unknown action=" + action;
            };
        } catch (Throwable t) {
            HotPot.LOGGER.error("[OPT4G] failed action={}", action, t);
            return "FAIL: " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private static String auditAll(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        int checked = 0;

        // 1. 全部盘子
        for (PlateItem plateItem : HotPotContent.PLATE_ITEMS) {
            ItemStack s = new ItemStack(plateItem);
            req(TableObjects.item(s) == plateItem, "plate item mismatch: " + plateItem);
            req(TableObjects.placedFor(s) == plateItem.plate(), "placedFor mismatch: " + plateItem);
            req(TableObjects.canAutoPlace(s), "plate canAutoPlace false: " + plateItem);
            req(!TableObjects.keepsSelf(s), "plate keepsSelf true: " + plateItem);
            req(!TableObjects.holdsFood(world, s), "plate holdsFood true: " + plateItem);
            checked++;
        }

        // 2. 原版碗 + 专属/通用汤碗 + 蘸料碗
        ItemStack vanillaBowl = new ItemStack(Items.BOWL);
        req(TableObjects.placedFor(vanillaBowl) == HotPotContent.BOWL_BLOCK, "vanilla bowl placedFor");
        req(TableObjects.canAutoPlace(vanillaBowl), "vanilla bowl canAutoPlace");
        req(!TableObjects.keepsSelf(vanillaBowl), "vanilla bowl keepsSelf");

        ItemStack clearBowl = new ItemStack(HotPotContent.SOUP_BOWLS.get(SoupBase.CLEAR.ordinal()));
        req(TableObjects.placedFor(clearBowl) == HotPotContent.BOWL_BLOCK, "clear bowl placedFor");
        req(TableObjects.keepsSelf(clearBowl), "clear bowl keepsSelf");
        req(!TableObjects.holdsFood(world, clearBowl), "clear bowl holdsFood");

        ItemStack genericTomato = new ItemStack(HotPotContent.SOUP_BOWL);
        genericTomato.set(HotPotContent.SOUP, "orients_hotpot_chronicles:tomato");
        req(TableObjects.placedFor(genericTomato) == HotPotContent.BOWL_BLOCK, "generic tomato placedFor");
        req(TableObjects.keepsSelf(genericTomato), "generic tomato keepsSelf");

        ItemStack genericUnknown = new ItemStack(HotPotContent.SOUP_BOWL);
        genericUnknown.set(HotPotContent.SOUP, "example:unknown_broth");

        ItemStack sauceBowl = new ItemStack(HotPotContent.DIPPING_SAUCE);
        req(TableObjects.placedFor(sauceBowl) == HotPotContent.BOWL_BLOCK, "dipping sauce placedFor");
        req(TableObjects.keepsSelf(sauceBowl), "dipping sauce keepsSelf");
        req(!TableObjects.holdsFood(world, sauceBowl), "dipping sauce holdsFood");
        req(TableObjects.prepareForTable(sauceBowl).is(Items.BOWL), "0-bite sauce prepareForTable -> BOWL");

        // 3. 饮料 / 空瓶 / 玻璃杯 / 蘸料瓶 / 竹筷
        for (DrinkKind kind : List.of(DrinkKind.ORANGE_DRINK, DrinkKind.PLANT_PROTEIN_DRINK, DrinkKind.ORIENT_COLA)) {
            ItemStack bottle = new ItemStack(HotPotContent.drinkFor(kind));
            ItemStack empty = new ItemStack(HotPotContent.emptyBottle(kind));
            req(TableObjects.placedFor(bottle) == HotPotContent.DRINK_BLOCK, "drink placedFor");
            req(TableObjects.placedFor(empty) == HotPotContent.DRINK_BLOCK, "empty bottle placedFor");
            req(TableObjects.keepsSelf(bottle) && TableObjects.keepsSelf(empty), "drink/empty keepsSelf");
            checked += 2;
        }

        ItemStack glassCup = new ItemStack(HotPotContent.GLASS_CUP);
        ItemStack waterCup = new ItemStack(HotPotContent.cupFor(DrinkKind.WATER));
        req(TableObjects.placedFor(glassCup) == HotPotContent.CUP_BLOCK, "cup placedFor");
        req(TableObjects.placedFor(waterCup) == HotPotContent.CUP_BLOCK, "water cup placedFor");
        req(TableObjects.keepsSelf(glassCup) && TableObjects.keepsSelf(waterCup), "cup keepsSelf");

        for (Item sauceBottle : List.of(HotPotContent.SESAME_PASTE, HotPotContent.OYSTER_SAUCE)) {
            ItemStack s = new ItemStack(sauceBottle);
            TableItem ti = TableObjects.item(s);
            req(ti != null && ti.tableBlock(s) == HotPotContent.SAUCE_BOTTLE_BLOCK, "sauce bottle tableBlock");
            req(TableObjects.placedFor(s) == null, "sauce bottle placedFor must be null");
            req(!TableObjects.canAutoPlace(s), "sauce bottle canAutoPlace must be false");
            req(TableObjects.keepsSelf(s), "sauce bottle keepsSelf");
            req(!TableObjects.holdsFood(world, s), "sauce bottle holdsFood must be false (has FOOD component)");
            checked++;
        }

        ItemStack chopsticks = new ItemStack(HotPotContent.CHOPSTICKS);
        TableItem chopTi = TableObjects.item(chopsticks);
        req(chopTi != null && chopTi.tableBlock(chopsticks) == HotPotContent.CHOPSTICK_REST_BLOCK, "chopsticks tableBlock");
        req(TableObjects.placedFor(chopsticks) == null, "chopsticks placedFor must be null");
        req(!TableObjects.canAutoPlace(chopsticks), "chopsticks canAutoPlace must be false");
        req(TableObjects.keepsSelf(chopsticks), "chopsticks keepsSelf");
        req(!TableObjects.holdsFood(world, chopsticks), "chopsticks holdsFood must be false");

        // 4. TableBlock 状态 / 高度 / 碎屑 / modelState（含 CupBlock 复用 DrinkBlock.EMPTY 与通用/未知汤碗）
        PlateHolderBlockEntity be = new PlateHolderBlockEntity(BlockPos.ZERO, HotPotContent.PLATE_HOLDER.defaultBlockState());
        req(be.tryAdd(HotPotContent.BOWL_BLOCK, Direction.Axis.X, 4.0, 4.0, clearBowl, true, Direction.NORTH), "add clearBowl");
        req(be.tryAdd(HotPotContent.BOWL_BLOCK, Direction.Axis.X, 12.0, 4.0, genericUnknown, true, Direction.NORTH), "add unknownBowl");
        req(be.tryAdd(HotPotContent.CUP_BLOCK, Direction.Axis.X, 3.5, 12.5, waterCup, true, Direction.EAST), "add waterCup");
        req(be.tryAdd(HotPotContent.SAUCE_BOTTLE_BLOCK, Direction.Axis.X, 12.5, 12.5,
            new ItemStack(HotPotContent.SESAME_PASTE), true, Direction.SOUTH), "add sesame");
        PlateHolderBlockEntity chopBe = new PlateHolderBlockEntity(BlockPos.ZERO, HotPotContent.PLATE_HOLDER.defaultBlockState());
        req(chopBe.tryAdd(HotPotContent.CHOPSTICK_REST_BLOCK, Direction.Axis.X, 8.0, 8.0,
            chopsticks, true, Direction.SOUTH), "add chopsticks");

        List<PlateHolderBlockEntity.Placed> list = be.plates();
        List<PlateHolderBlockEntity.Placed> chopList = chopBe.plates();
        req(PlateHolderBlock.modelState(list.get(0)).getValue(BowlBlock.SOUP) == SoupVisual.CLEAR, "clear modelState");
        req(PlateHolderBlock.modelState(list.get(1)).getValue(BowlBlock.SOUP) == SoupVisual.OTHER, "unknown modelState -> OTHER");
        BlockState cupState = PlateHolderBlock.modelState(list.get(2));
        req(!cupState.getValue(DrinkBlock.EMPTY) && cupState.getValue(CupBlock.KIND) == DrinkKind.WATER, "cup modelState");
        req(!list.get(2).restsChopsticks() && !list.get(3).restsChopsticks() && !chopList.get(0).restsChopsticks(), "non-plate restsChopsticks");
        req(PlateHolderBlockEntity.heightOf(list.get(3)) == 6.0, "sesame height=6");
        req(PlateHolderBlockEntity.heightOf(chopList.get(0)) == 2.0, "chopstick rest height=2");
        req(chopList.get(0).axis == Direction.Axis.Z, "chopstick rest south -> axis Z");
        req(chopList.get(0).plate.breakParticleState(chopList.get(0)).is(Blocks.BAMBOO_PLANKS), "chopstick breakParticle -> BAMBOO_PLANKS");

        return "OK checked=" + checked;
    }

    private static String verifyExternal(ServerPlayer player, BlockPos pos) {
        if (!installed || customTrayBlock == null || customTrayItem == null) {
            return "FAIL: -Dhotpot.opt4g=true not enabled";
        }
        ServerLevel world = player.serverLevel();
        ItemStack customStack = new ItemStack(customTrayItem);
        req(TableObjects.item(customStack) == customTrayItem, "external item lookup");
        req(TableObjects.placedFor(customStack) == customTrayBlock, "external placedFor");
        req(TableObjects.canAutoPlace(customStack), "external canAutoPlace");
        req(TableObjects.keepsSelf(customStack), "external keepsSelf");
        req(!TableObjects.holdsFood(world, customStack), "external holdsFood");

        world.setBlockAndUpdate(pos.below(), HotPotContent.HOTPOT_TABLES.get(0).defaultBlockState());
        world.setBlockAndUpdate(pos, PlateHolderBlock.withSurface(
            HotPotContent.PLATE_HOLDER.defaultBlockState(), world, pos));
        PlateHolderBlockEntity be = (PlateHolderBlockEntity) world.getBlockEntity(pos);
        req(be != null && be.tryAdd(customTrayBlock, Direction.Axis.X, 8.0, 8.0, customStack, true, Direction.WEST),
            "add external tray");
        PlateHolderBlockEntity.Placed placed = be.plates().get(0);
        req(PlateHolderBlockEntity.heightOf(placed) == 7.25, "external tableHeight=7.25");
        req(placed.plate.breakParticleState(placed).is(Blocks.DIAMOND_BLOCK), "external breakParticle");
        req(!placed.restsChopsticks(), "external restsChopsticks");
        req(placed.plate.useOnPlaced(placed, world, pos, be, player, InteractionHand.MAIN_HAND,
            new ItemStack(Items.SUGAR)) == UseOutcome.SUCCESS, "external useOnPlaced sugar");
        req(placed.plate.useOnPlaced(placed, world, pos, be, player, InteractionHand.MAIN_HAND,
            new ItemStack(Items.STICK)) == UseOutcome.PASS, "external useOnPlaced stick -> PASS");
        ItemStack out = placed.toItem();
        ItemContainerContents c = out.get(DataComponents.CONTAINER);
        req(c != null && c.nonEmptyStream().findFirst().orElse(ItemStack.EMPTY).is(Items.SUGAR), "external toTableItem saved food");

        world.removeBlock(pos, false);
        world.removeBlock(pos.below(), false);
        return "OK external hits=" + customTrayBlock.useHits.get();
    }

    private static String verifyLegacyRoundTrip(ServerPlayer player, BlockPos pos) throws Exception {
        ServerLevel world = player.serverLevel();
        var regs = world.registryAccess();
        JsonObject root;
        try (InputStream stream = openFixturesStream()) {
            root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        CompoundTag mixedNbt = TagParser.parseTag(root.getAsJsonObject("entries").getAsJsonObject("table_oak_mixed").get("snbt").getAsString());
        PlateHolderBlockEntity be = new PlateHolderBlockEntity(pos, HotPotContent.PLATE_HOLDER.defaultBlockState());
        be.loadWithComponents(mixedNbt, regs);
        int count = be.plates().size();
        req(count > 0, "table_oak_mixed loaded 0 plates");
        CompoundTag saved = be.saveWithoutMetadata(regs);
        PlateHolderBlockEntity reloaded = new PlateHolderBlockEntity(pos, HotPotContent.PLATE_HOLDER.defaultBlockState());
        reloaded.loadWithComponents(saved, regs);
        req(reloaded.plates().size() == count, "reloaded count mismatch");

        // 含 6 类内建方块的旧存档读回 + 竹筷 Axis 自动对齐
        CompoundTag synthetic = new CompoundTag();
        ListTag plates = new ListTag();
        plates.add(plateEntry("orients_hotpot_chronicles:quartz_small_dish", "x", "north", 3.0, 3.0, null));
        plates.add(plateEntry("orients_hotpot_chronicles:bowl", "x", "east", 13.0, 3.0, "orients_hotpot_chronicles:clear_broth_bowl"));
        plates.add(plateEntry("orients_hotpot_chronicles:drink", "x", "south", 3.0, 13.0, "orients_hotpot_chronicles:orange_drink"));
        plates.add(plateEntry("orients_hotpot_chronicles:cup", "x", "west", 13.0, 13.0, "orients_hotpot_chronicles:glass_cup"));
        plates.add(plateEntry("orients_hotpot_chronicles:sauce_bottle", "x", "north", 8.0, 3.0, "orients_hotpot_chronicles:sesame_paste"));
        plates.add(plateEntry("orients_hotpot_chronicles:chopstick_rest", "x", "south", 8.0, 12.0, "orients_hotpot_chronicles:bamboo_chopsticks"));
        synthetic.put("Plates", plates);
        PlateHolderBlockEntity synBe = new PlateHolderBlockEntity(pos, HotPotContent.PLATE_HOLDER.defaultBlockState());
        synBe.loadWithComponents(synthetic, regs);
        req(synBe.plates().size() == 6, "expected 6 legacy entries, got " + synBe.plates().size());
        req(synBe.plates().get(5).axis == Direction.Axis.Z, "chopstick_rest south -> axis Z on load");
        CompoundTag synSaved = synBe.saveWithoutMetadata(regs);
        req("z".equals(synSaved.getList("Plates", 10).getCompound(5).getString("Axis")), "saved chopstick axis z");
        return "OK legacy table_oak_mixed=" + count + " all6=6";
    }

    private static InputStream openFixturesStream() throws IOException {
        InputStream stream = Opt4gProbe.class.getResourceAsStream("/opt4/legacy-0.2.2-alpha/fixtures.json");
        if (stream != null) {
            return stream;
        }
        String rel = "common/src/test/resources/opt4/legacy-0.2.2-alpha/fixtures.json";
        Path cur = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && cur != null; i++) {
            Path candidate = cur.resolve(rel);
            if (Files.isRegularFile(candidate)) {
                return Files.newInputStream(candidate);
            }
            cur = cur.getParent();
        }
        throw new IllegalStateException("missing fixtures.json from classpath or " + rel);
    }

    private static CompoundTag plateEntry(String plateId, String axis, String facing, double x, double z, String selfId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Plate", plateId);
        tag.putString("Axis", axis);
        tag.putString("Facing", facing);
        tag.put("X", DoubleTag.valueOf(x));
        tag.put("Z", DoubleTag.valueOf(z));
        tag.put("ChopsticksYaw", FloatTag.valueOf(45.0F));
        tag.put("Items", new ListTag());
        if (selfId != null) {
            CompoundTag self = new CompoundTag();
            self.putString("id", selfId);
            self.putInt("count", 1);
            tag.put("Self", self);
        }
        return tag;
    }

    private static void req(boolean cond, String msg) {
        if (!cond) {
            throw new IllegalStateException(msg);
        }
    }

    private static final class CustomTrayBlock extends PlateBlock {
        final AtomicInteger useHits = new AtomicInteger();

        CustomTrayBlock() {
            super("opt4gtest_custom_tray", PlateSize.S1X1, MapColor.COLOR_BLUE, SoundType.ANVIL);
        }

        @Override
        public boolean holdsFood(PlateHolderBlockEntity.Placed placed) {
            return !placed.self.isEmpty() && placed.items.stream().filter(s -> !s.isEmpty()).count() < 2;
        }

        @Override
        public boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
            return false;
        }

        @Override
        public double tableHeight(PlateHolderBlockEntity.Placed placed) {
            return 7.25;
        }

        @Override
        public BlockState breakParticleState(PlateHolderBlockEntity.Placed placed) {
            return Blocks.DIAMOND_BLOCK.defaultBlockState();
        }

        @Override
        public void onLoadedOrAdded(PlateHolderBlockEntity.Placed placed, Direction facing) {
            placed.axis = Direction.Axis.X;
        }

        @Override
        public ItemStack toTableItem(PlateHolderBlockEntity.Placed placed) {
            return TableBlock.containerItem(placed.self.getItem(), placed);
        }

        @Override
        public UseOutcome useOnPlaced(PlateHolderBlockEntity.Placed target, Level world, BlockPos pos,
                                      PlateHolderBlockEntity be, Player player, InteractionHand hand, ItemStack stack) {
            if (stack.is(Items.SUGAR) && target.add(stack)) {
                useHits.incrementAndGet();
                be.changed();
                return UseOutcome.SUCCESS;
            }
            return UseOutcome.PASS;
        }
    }

    private static final class CustomTrayItem extends Item implements TableItem {
        private final PlateBlock block;

        CustomTrayItem(PlateBlock block) {
            super(new Item.Properties().stacksTo(1));
            this.block = block;
        }

        @Override
        public PlateBlock tableBlock(ItemStack stack) {
            return block;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide);
        }
    }
}
