// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundWriter;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoredItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.compat.ItemInteractionBlock;
import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三批 3G：版本适配小包<b>语义层</b>的独立验收（1.20.1 适配）。
 *
 * <p>用原版 {@link InteractionResult} 的 {@code consumesAction()} / {@code shouldSwing()} /
 * {@code shouldAwardStats()} 判断意图映射是否忠实于原版语义，并对 {@link CompoundWriter} /
 * {@link CompoundReader} 做极端原语及容器同步往返验收。</p>
 */
class Opt3gUseMappingSemanticsTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    // ------------------------------------------------------------ 语义签名

    private record Semantics(boolean consumesAction, boolean swings, boolean shouldAwardStats) {
    }

    private static Semantics of(ItemInteractionResult result) {
        return of(result.result());
    }

    private static Semantics of(InteractionResult result) {
        return new Semantics(result.consumesAction(), result.shouldSwing(), result.shouldAwardStats());
    }

    // ------------------------------------------------------------ 1. 七个意图的语义

    @Test
    void blockItemIntentSignaturesFollowVanillaSemantics() {
        for (boolean client : new boolean[]{true, false}) {
            assertSemantics(ItemInteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS, client));
            assertSemantics(ItemInteractionResult.CONSUME, Use.blockItem(UseOutcome.CONSUME, client));
            assertSemantics(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                Use.blockItem(UseOutcome.SKIPPED, client));
            assertSemantics(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                Use.blockItem(UseOutcome.PASS_TO_EMPTY, client));
            assertSemantics(ItemInteractionResult.FAIL, Use.blockItem(UseOutcome.FAIL, client));
        }

        assertEquals(of(InteractionResult.SUCCESS), of(ItemInteractionResult.SUCCESS));
        assertEquals(of(InteractionResult.CONSUME), of(ItemInteractionResult.CONSUME));
        assertEquals(of(InteractionResult.PASS), of(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION));
        assertEquals(of(InteractionResult.PASS), of(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION));
        assertEquals(of(InteractionResult.FAIL), of(ItemInteractionResult.FAIL));

        assertTrue(of(ItemInteractionResult.SUCCESS).consumesAction());
        assertTrue(of(ItemInteractionResult.CONSUME).consumesAction());
        assertFalse(of(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION).consumesAction());
        assertFalse(of(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION).consumesAction());
        assertFalse(of(ItemInteractionResult.FAIL).consumesAction());
    }

    @Test
    void successSoftBehavesLikeSidedSuccessOnBothSides() {
        for (boolean client : new boolean[]{true, false}) {
            ItemInteractionResult expected = ItemInteractionResult.sidedSuccess(client);
            assertSame(expected, Use.blockItem(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(expected), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, client)));

            assertEquals(of(InteractionResult.sidedSuccess(client)), of(Use.item(UseOutcome.SUCCESS_SOFT, client)));

            InteractionResultHolder<ItemStack> held = Use.itemUse(UseOutcome.SUCCESS_SOFT,
                new ItemStack(Items.BOWL), client);
            assertEquals(of(InteractionResult.sidedSuccess(client)), of(held.getResult()));

            assertSame(InteractionResult.sidedSuccess(client), Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(InteractionResult.sidedSuccess(client)), of(Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client)));
        }
        assertEquals(of(InteractionResult.SUCCESS), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, true)));
        assertEquals(of(InteractionResult.CONSUME), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, false)));
        assertNotEquals(of(Use.blockItem(UseOutcome.SUCCESS_SOFT, true)),
            of(Use.blockItem(UseOutcome.SUCCESS_SOFT, false)));

        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SUCCESS, UseOutcome.CONSUME, UseOutcome.SKIPPED,
            UseOutcome.PASS_TO_EMPTY, UseOutcome.FAIL}) {
            assertSame(Use.blockItem(outcome, true), Use.blockItem(outcome, false), outcome.name());
        }
    }

    @Test
    void blockEmptyAndItemSideFollowVanillaSignatures() {
        for (boolean client : new boolean[]{true, false}) {
            assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS, client));
            assertSame(InteractionResult.sidedSuccess(client), Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
            assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.CONSUME, client));
            assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.SKIPPED, client));
            assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, client));
            assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS, client));
            assertSame(InteractionResult.FAIL, Use.blockEmpty(UseOutcome.FAIL, client));

            assertSame(InteractionResult.SUCCESS, Use.item(UseOutcome.SUCCESS, client));
            assertSame(InteractionResult.CONSUME, Use.item(UseOutcome.CONSUME, client));
            assertSame(InteractionResult.PASS, Use.item(UseOutcome.SKIPPED, client));
            assertSame(InteractionResult.PASS, Use.item(UseOutcome.PASS_TO_EMPTY, client));
            assertSame(InteractionResult.PASS, Use.item(UseOutcome.PASS, client));
            assertSame(InteractionResult.FAIL, Use.item(UseOutcome.FAIL, client));

            assertTrue(of(Use.blockEmpty(UseOutcome.CONSUME, client)).consumesAction());
            assertFalse(of(Use.blockEmpty(UseOutcome.CONSUME, client)).swings());
            assertEquals(of(InteractionResult.PASS), of(Use.item(UseOutcome.SKIPPED, client)));
            assertEquals(of(InteractionResult.FAIL), of(Use.item(UseOutcome.FAIL, client)));
        }

        ItemStack held = new ItemStack(Items.BOWL, 3);
        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SUCCESS, UseOutcome.CONSUME, UseOutcome.SKIPPED,
            UseOutcome.PASS_TO_EMPTY, UseOutcome.PASS, UseOutcome.FAIL}) {
            InteractionResultHolder<ItemStack> holder = Use.itemUse(outcome, held, false);
            assertSame(held, holder.getObject(), outcome.name());
            assertSame(Use.item(outcome, false), holder.getResult(), outcome.name());
        }
        assertSame(InteractionResultHolder.pass(held).getResult(),
            Use.itemUse(UseOutcome.SKIPPED, held, false).getResult());
    }

    // ------------------------------------------------------------ 2. CONSUME 与 SKIPPED 语义不同

    @Test
    void consumeAndSkipHaveDifferentActionAndResultSemantics() {
        ItemInteractionResult consume = Use.blockItem(UseOutcome.CONSUME, false);
        ItemInteractionResult skip = Use.blockItem(UseOutcome.SKIPPED, false);

        assertNotSame(consume, skip);
        assertNotEquals(consume.name(), skip.name());

        assertTrue(consume.result().consumesAction());
        assertFalse(skip.result().consumesAction());
        assertNotEquals(consume.result().consumesAction(), skip.result().consumesAction());

        assertSame(InteractionResult.CONSUME, consume.result());
        assertSame(InteractionResult.PASS, skip.result());
        assertNotSame(consume.result(), skip.result());

        assertEquals(of(InteractionResult.CONSUME), of(consume));
        assertEquals(of(InteractionResult.PASS), of(skip));
        assertFalse(of(consume).swings());
        assertTrue(of(consume).shouldAwardStats());
        assertFalse(of(skip).swings());
        assertFalse(of(skip).shouldAwardStats());
    }

    // ------------------------------------------------------------ 3. 1.20.1 方块 use 与 ItemInteractionBlock 默认值事实

    @Test
    void blockUseDefaultsOn1201ArePassAndCompatDefaultsAlign() throws Exception {
        Method vanillaUse = BlockBehaviour.class.getDeclaredMethod("use", BlockState.class,
            Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
        vanillaUse.setAccessible(true);
        BlockState stoneState = Blocks.STONE.defaultBlockState();
        Object defaultVanilla = vanillaUse.invoke(Blocks.STONE, stoneState, null, BlockPos.ZERO,
            null, InteractionHand.MAIN_HAND, null);
        assertSame(InteractionResult.PASS, defaultVanilla);

        ItemInteractionBlock blankCompatBlock = new ItemInteractionBlock() { };
        ItemInteractionResult defaultItemOn = blankCompatBlock.useItemOn(ItemStack.EMPTY, stoneState, null,
            BlockPos.ZERO, null, InteractionHand.MAIN_HAND, null);
        InteractionResult defaultEmpty = blankCompatBlock.useWithoutItem(stoneState, null, BlockPos.ZERO, null, null);

        assertSame(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, defaultItemOn);
        assertSame(InteractionResult.PASS, defaultEmpty);
        assertSame(defaultItemOn.result(), defaultEmpty);
        assertEquals(of(InteractionResult.PASS), of(defaultItemOn));
        assertEquals(of(InteractionResult.PASS), of(defaultEmpty));

        assertSame(defaultItemOn, Use.blockItem(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS, false));
    }

    // ------------------------------------------------------------ 4. 原语往返

    @Test
    void numericPrimitiveRoundTripKeepsExtremesAndEmptyArrays() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag);
        writer.putInt("IntMin", Integer.MIN_VALUE);
        writer.putInt("IntMax", Integer.MAX_VALUE);
        writer.putInt("IntNeg", -42);
        writer.putFloat("FloatMin", Float.MIN_VALUE);
        writer.putFloat("FloatNegMax", -Float.MAX_VALUE);
        writer.putFloat("FloatNaN", Float.NaN);
        writer.putFloat("FloatInf", Float.POSITIVE_INFINITY);
        writer.putDouble("DoubleMin", Double.MIN_VALUE);
        writer.putDouble("DoubleNegMax", -Double.MAX_VALUE);
        writer.putDouble("DoubleNaN", Double.NaN);
        writer.putDouble("DoubleNegInf", Double.NEGATIVE_INFINITY);
        writer.putBoolean("True", true);
        writer.putBoolean("False", false);
        writer.putString("EmptyString", "");
        writer.putString("Unicode", "\u706b\u9505\ud83c\udf72");
        writer.putIntArray("EmptyInts", new int[0]);
        writer.putByteArray("EmptyBytes", new byte[0]);
        writer.putIntArray("NegativeInts", new int[]{-1, Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        writer.putByteArray("SignedBytes", new byte[]{-128, -1, 0, 127});

        CompoundReader reader = new CompoundReader(tag);
        assertEquals(Integer.MIN_VALUE, reader.getInt("IntMin", 0));
        assertEquals(Integer.MAX_VALUE, reader.getInt("IntMax", 0));
        assertEquals(-42, reader.getInt("IntNeg", 0));
        assertEquals(Float.MIN_VALUE, reader.getFloat("FloatMin", 0F));
        assertEquals(-Float.MAX_VALUE, reader.getFloat("FloatNegMax", 0F));
        assertEquals(Float.floatToIntBits(Float.NaN), Float.floatToIntBits(reader.getFloat("FloatNaN", 0F)));
        assertEquals(Float.POSITIVE_INFINITY, reader.getFloat("FloatInf", 0F));
        assertEquals(Double.MIN_VALUE, reader.getDouble("DoubleMin", 0D));
        assertEquals(-Double.MAX_VALUE, reader.getDouble("DoubleNegMax", 0D));
        assertEquals(Double.doubleToLongBits(Double.NaN), Double.doubleToLongBits(reader.getDouble("DoubleNaN", 0D)));
        assertEquals(Double.NEGATIVE_INFINITY, reader.getDouble("DoubleNegInf", 0D));
        assertTrue(reader.getBoolean("True", false));
        assertFalse(reader.getBoolean("False", true));
        assertEquals("", reader.getString("EmptyString", "fallback"));
        assertEquals("\u706b\u9505\ud83c\udf72", reader.getString("Unicode", ""));
        assertArrayEquals(new int[0], reader.getIntArray("EmptyInts"));
        assertArrayEquals(new byte[0], reader.getByteArray("EmptyBytes"));
        assertArrayEquals(new int[]{-1, Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, reader.getIntArray("NegativeInts"));
        assertArrayEquals(new byte[]{-128, -1, 0, 127}, reader.getByteArray("SignedBytes"));

        assertTrue(reader.has("EmptyString"));
        assertTrue(reader.has("EmptyInts"));
        assertTrue(reader.has("EmptyBytes"));
    }

    @Test
    void fallbacksApplyOnlyWhenTheKeyIsAbsent() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Text", "abc");
        tag.putDouble("Decimal", 1.5D);
        tag.putFloat("Single", 2.5F);
        tag.putBoolean("Flag", true);
        CompoundReader reader = new CompoundReader(tag);

        assertEquals(-7, reader.getInt("Missing", -7));
        assertEquals(0.5F, reader.getFloat("Missing", 0.5F));
        assertEquals(1.25D, reader.getDouble("Missing", 1.25D));
        assertTrue(reader.getBoolean("Missing", true));
        assertEquals("fallback", reader.getString("Missing", "fallback"));
        assertEquals(0, reader.getIntArray("Missing").length);
        assertEquals(0, reader.getByteArray("Missing").length);
        assertFalse(reader.has("Missing"));
        assertFalse(reader.hasCompound("Missing"));
        assertFalse(reader.hasNumber("Missing"));
        assertTrue(reader.getItem("Missing").isEmpty());

        assertTrue(reader.has("Text"));
        assertEquals(0, reader.getInt("Text", -7));
        assertEquals(0F, reader.getFloat("Text", 0.5F));
        assertFalse(reader.hasNumber("Text"));
        assertFalse(reader.hasCompound("Text"));
        assertTrue(reader.getItem("Text").isEmpty());
        assertEquals(1, reader.getInt("Decimal", -7));
        assertEquals(1.5F, reader.getFloat("Decimal", -7F));
        assertEquals(2.5D, reader.getDouble("Single", -7D));
        assertTrue(reader.hasNumber("Decimal"));
        assertTrue(reader.hasNumber("Single"));
        assertTrue(reader.hasNumber("Flag"));
        assertEquals(1, reader.getInt("Flag", -7));

        CompoundReader empty = new CompoundReader(null);
        assertEquals(-7, empty.getInt("Missing", -7));
        assertFalse(empty.has("Missing"));
        assertEquals(0, empty.items().size());
        assertEquals(0, empty.children("Missing").size());
        assertEquals(-7, empty.child("Missing").getInt("Anything", -7));
    }

    /** 写出空栈要跳过，非空栈保留 1.20.1 NBT（含耐久）。 */
    @Test
    void putItemSkipsEmptyAndKeepsDamageNbt() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag);
        writer.putItem("Empty", ItemStack.EMPTY);
        assertFalse(tag.contains("Empty"));

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(5);
        writer.putItem("Sword", sword);
        assertTrue(tag.contains("Sword"));
        assertEquals(Tag.TAG_COMPOUND, tag.getTagType("Sword"));

        ItemStack readBack = new CompoundReader(tag).getItem("Sword");
        assertEquals(Items.IRON_SWORD, readBack.getItem());
        assertEquals(1, readBack.getCount());
        assertEquals(5, readBack.getDamageValue());

        writer.putItem("Bowl", new ItemStack(Items.BOWL, 3));
        assertEquals(3, new CompoundReader(tag).getItem("Bowl").getCount());
    }

    @Test
    void putItemsIsIdenticalToContainerHelperSaveAllItems() {
        List<ItemStack> items = new ArrayList<>(Arrays.asList(new ItemStack(Items.BOWL, 3), ItemStack.EMPTY,
            new ItemStack(Items.STICK, 64), ItemStack.EMPTY, new ItemStack(Items.CARROT, 1)));
        CompoundTag ours = new CompoundTag();
        new CompoundWriter(ours).putItems(items);

        CompoundTag vanilla = new CompoundTag();
        NonNullList<ItemStack> vanillaList = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            vanillaList.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(vanilla, vanillaList);

        assertEquals(vanilla, ours);
        assertEquals(vanilla.toString(), ours.toString());
        assertEquals(Tag.TAG_LIST, ours.getTagType("Items"));
        assertEquals(Tag.TAG_COMPOUND, ours.getList("Items", Tag.TAG_COMPOUND).getElementType());
        assertEquals(3, ours.getList("Items", Tag.TAG_COMPOUND).size());

        CompoundTag oursEmpty = new CompoundTag();
        new CompoundWriter(oursEmpty).putItems(Arrays.asList(ItemStack.EMPTY, ItemStack.EMPTY));
        CompoundTag vanillaEmpty = new CompoundTag();
        ContainerHelper.saveAllItems(vanillaEmpty, NonNullList.withSize(2, ItemStack.EMPTY));
        assertEquals(vanillaEmpty, oursEmpty);
        assertEquals(vanillaEmpty.toString(), oursEmpty.toString());
    }

    @Test
    void emptyItemListWritesListTagAndLoadsNothing() {
        CompoundTag tag = new CompoundTag();
        new CompoundWriter(tag).putItems(Collections.emptyList());

        assertTrue(tag.contains("Items"));
        assertEquals(Tag.TAG_LIST, tag.getTagType("Items"));
        assertEquals(0, tag.getList("Items", Tag.TAG_COMPOUND).size());
        assertEquals("{Items:[]}", tag.toString());

        CompoundReader reader = new CompoundReader(tag);
        assertEquals(0, reader.items().size());
        List<ItemStack> out = new ArrayList<>(Arrays.asList(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        reader.loadItems(out);
        for (ItemStack stack : out) {
            assertTrue(stack.isEmpty());
        }

        CompoundTag childTag = new CompoundTag();
        new CompoundWriter(childTag).childList("Plates");
        assertTrue(childTag.contains("Plates"));
        assertEquals(Tag.TAG_LIST, childTag.getTagType("Plates"));
        assertEquals(0, childTag.getList("Plates", Tag.TAG_COMPOUND).size());
        assertEquals(0, new CompoundReader(childTag).children("Plates").size());
    }

    @Test
    void childAndChildListKeepTypesNestingAndOrder() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag);
        var fuel = writer.child("Fuel");
        fuel.child("Deep").putInt("Level", 3);
        fuel.putItems(List.of(new ItemStack(Items.COAL, 2)));
        writer.child("Empty");
        var plates = writer.childList("Plates");
        plates.addChild().putInt("Order", 1);
        plates.addChild().putInt("Order", 2);
        plates.addChild().putString("Name", "minecraft:oak_planks");

        assertEquals(Tag.TAG_COMPOUND, tag.getTagType("Fuel"));
        assertEquals(Tag.TAG_COMPOUND, tag.getTagType("Empty"));
        assertEquals(Tag.TAG_LIST, tag.getTagType("Plates"));
        assertEquals(Tag.TAG_COMPOUND, tag.getList("Plates", Tag.TAG_COMPOUND).getElementType());
        assertEquals(3, tag.getList("Plates", Tag.TAG_COMPOUND).size());

        CompoundReader reader = new CompoundReader(tag);
        assertTrue(reader.hasCompound("Fuel"));
        assertTrue(reader.child("Fuel").hasCompound("Deep"));
        assertEquals(3, reader.child("Fuel").child("Deep").getInt("Level", -1));
        assertEquals(1, reader.child("Fuel").items().size());
        assertEquals(Items.COAL, reader.child("Fuel").items().get(0).stack().getItem());

        List<Object> orders = new ArrayList<>();
        for (var entry : reader.children("Plates")) {
            orders.add(entry.getInt("Order", -1));
        }
        assertEquals(Arrays.asList(1, 2, -1), orders);
        assertEquals("minecraft:oak_planks", reader.children("Plates").get(2).getString("Name", ""));

        assertTrue(reader.has("Empty"));
        assertFalse(reader.child("Empty").has("Anything"));
        assertEquals(42, reader.child("Empty").getInt("Anything", 42));

        assertEquals(42, reader.child("Missing").getInt("Anything", 42));
        assertEquals(0, reader.children("Missing").size());
        assertEquals(0, reader.children("Fuel").size());
        assertFalse(reader.child("Plates").has("Order"));
        assertEquals(0, reader.children("Empty").size());
    }

    @Test
    void storedItemsReportLegacyEntriesWithoutSlot() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag legacy = new ItemStack(Items.BOWL, 2).save(new CompoundTag());
        list.add(legacy);
        CompoundTag slotted = new ItemStack(Items.CARROT, 5).save(new CompoundTag());
        slotted.putByte("Slot", (byte) 130);
        list.add(slotted);
        CompoundTag broken = new CompoundTag();
        broken.putString("id", "minecraft:definitely_not_an_item");
        broken.putByte("Count", (byte) 1);
        list.add(broken);
        tag.put("Items", list);

        List<StoredItem> stored = new CompoundReader(tag).items();
        assertEquals(3, stored.size());

        assertFalse(stored.get(0).hasSlot());
        assertEquals(-1, stored.get(0).slot());
        assertEquals(Items.BOWL, stored.get(0).stack().getItem());
        assertEquals(2, stored.get(0).stack().getCount());

        assertTrue(stored.get(1).hasSlot());
        assertEquals(130, stored.get(1).slot());
        assertEquals(Items.CARROT, stored.get(1).stack().getItem());

        assertFalse(stored.get(2).hasSlot());
        assertEquals(-1, stored.get(2).slot());
        assertTrue(stored.get(2).stack().isEmpty());

        CompoundTag wrongType = new CompoundTag();
        wrongType.putString("Items", "not_a_list");
        assertEquals(0, new CompoundReader(wrongType).items().size());
    }

    @Test
    void itemListLoadsIntoContainerBySlotWithLegacyFallback() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag first = new ItemStack(Items.BOWL, 2).save(new CompoundTag());
        first.putByte("Slot", (byte) 0);
        list.add(first);
        CompoundTag fifth = new ItemStack(Items.CARROT, 1).save(new CompoundTag());
        fifth.putByte("Slot", (byte) 5);
        list.add(fifth);
        list.add(new ItemStack(Items.STICK, 3).save(new CompoundTag()));
        tag.put("Items", list);

        CompoundReader reader = new CompoundReader(tag);
        List<ItemStack> viaOurs = new ArrayList<>(Collections.nCopies(8, ItemStack.EMPTY));
        reader.loadItems(viaOurs);

        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, viaVanilla);

        for (int i = 0; i < viaOurs.size(); i++) {
            assertEquals(viaVanilla.get(i).getItem(), viaOurs.get(i).getItem(), "槽 " + i);
            assertEquals(viaVanilla.get(i).getCount(), viaOurs.get(i).getCount(), "槽 " + i);
        }
        assertEquals(Items.STICK, viaOurs.get(0).getItem());
        assertEquals(Items.CARROT, viaOurs.get(5).getItem());

        List<ItemStack> shortTarget = new ArrayList<>(Collections.nCopies(1, ItemStack.EMPTY));
        reader.loadItems(shortTarget);
        NonNullList<ItemStack> shortVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, shortVanilla);
        assertEquals(shortVanilla.get(0).getItem(), shortTarget.get(0).getItem());
    }

    @Test
    void loadItemsWithoutItemsKeyPreservesTheTargetLikeVanilla() {
        List<ItemStack> viaOurs = new ArrayList<>(List.of(new ItemStack(Items.BOWL, 1)));
        new CompoundReader(new CompoundTag()).loadItems(viaOurs);
        assertEquals(Items.BOWL, viaOurs.get(0).getItem());

        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        viaVanilla.set(0, new ItemStack(Items.BOWL, 1));
        ContainerHelper.loadAllItems(new CompoundTag(), viaVanilla);
        assertEquals(Items.BOWL, viaVanilla.get(0).getItem());

        assertEquals(viaVanilla.get(0).getItem(), viaOurs.get(0).getItem());
    }

    @Test
    void childOpensAFreshNodeSoTheWriterMustBeReused() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag);
        writer.child("Fuel").putInt("A", 1);
        writer.child("Fuel").putInt("B", 2);
        CompoundReader reader = new CompoundReader(tag);
        assertEquals(2, reader.child("Fuel").getInt("B", -1));
        assertEquals(-1, reader.child("Fuel").getInt("A", -1));

        CompoundTag reused = new CompoundTag();
        CompoundWriter writer2 = new CompoundWriter(reused);
        var fuel = writer2.child("Fuel");
        fuel.putInt("A", 1);
        fuel.putInt("B", 2);
        CompoundReader reader2 = new CompoundReader(reused);
        assertEquals(1, reader2.child("Fuel").getInt("A", -1));
        assertEquals(2, reader2.child("Fuel").getInt("B", -1));
    }

    private static void assertSemantics(ItemInteractionResult expected, ItemInteractionResult actual) {
        assertEquals(of(expected), of(actual), actual.name());
        assertSame(expected, actual);
    }
}
