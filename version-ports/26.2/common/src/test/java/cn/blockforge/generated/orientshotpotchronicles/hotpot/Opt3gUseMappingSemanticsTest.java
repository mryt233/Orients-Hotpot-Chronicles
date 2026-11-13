// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundWriter;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoredItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三批 3G：版本适配小包<b>语义层</b>的独立验收（26.2 适配）。
 *
 * <p>{@code Opt3gAdapterTest} 钉的是「某个意图换回来的是哪个常量」，
 * 这里钉的是「换回来的常量在 26.2 原版里到底怎么表现」——用原版 {@link InteractionResult}
 * 的 {@code consumesAction()} / {@code swingSource()} / {@code wasItemInteraction()} 来判断。</p>
 */
class Opt3gUseMappingSemanticsTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
    }

    private static TagValueOutput newOut() {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries());
    }

    private static CompoundReader newReader(CompoundTag tag) {
        return new CompoundReader(TagValueInput.create(ProblemReporter.DISCARDING, registries(), tag));
    }

    // ------------------------------------------------------------ 语义签名

    private record Semantics(boolean consumesAction, InteractionResult.SwingSource swingSource, boolean wasItemInteraction) {
    }

    private static Semantics of(InteractionResult result) {
        if (result instanceof InteractionResult.Success success) {
            return new Semantics(success.consumesAction(), success.swingSource(), success.wasItemInteraction());
        }
        return new Semantics(result.consumesAction(), InteractionResult.SwingSource.NONE, false);
    }

    // ------------------------------------------------------------ 1. 七个意图的语义

    @Test
    void blockItemIntentSignaturesFollowVanillaSemantics() {
        for (boolean client : new boolean[]{true, false}) {
            assertSemantics(InteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS, client));
            assertSemantics(InteractionResult.CONSUME, Use.blockItem(UseOutcome.CONSUME, client));
            assertSemantics(InteractionResult.PASS, Use.blockItem(UseOutcome.SKIPPED, client));
            assertSemantics(InteractionResult.PASS, Use.blockItem(UseOutcome.PASS, client));
            assertSemantics(InteractionResult.TRY_WITH_EMPTY_HAND, Use.blockItem(UseOutcome.PASS_TO_EMPTY, client));
            assertSemantics(InteractionResult.FAIL, Use.blockItem(UseOutcome.FAIL, client));
        }

        assertTrue(of(InteractionResult.SUCCESS).consumesAction());
        assertTrue(of(InteractionResult.SUCCESS_SERVER).consumesAction());
        assertTrue(of(InteractionResult.CONSUME).consumesAction());
        assertFalse(of(InteractionResult.PASS).consumesAction());
        assertFalse(of(InteractionResult.TRY_WITH_EMPTY_HAND).consumesAction());
        assertFalse(of(InteractionResult.FAIL).consumesAction());
    }

    @Test
    void successSoftBehavesLikeSidedSuccessOnBothSides() {
        for (boolean client : new boolean[]{true, false}) {
            InteractionResult expected = client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            assertSame(expected, Use.blockItem(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(expected), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, client)));
            assertEquals(of(expected), of(Use.item(UseOutcome.SUCCESS_SOFT, client)));

            InteractionResult held = Use.itemUse(UseOutcome.SUCCESS_SOFT, new ItemStack(Items.BOWL), client);
            InteractionResult.Success heldSuccess = assertInstanceOf(InteractionResult.Success.class, held);
            assertSame(expected.consumesAction(), heldSuccess.consumesAction());
            assertSame(((InteractionResult.Success) expected).swingSource(), heldSuccess.swingSource());

            assertSame(expected, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(expected), of(Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client)));
        }
        assertEquals(of(InteractionResult.SUCCESS), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, true)));
        assertEquals(of(InteractionResult.SUCCESS_SERVER), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, false)));
        assertNotEquals(of(Use.blockItem(UseOutcome.SUCCESS_SOFT, true)),
            of(Use.blockItem(UseOutcome.SUCCESS_SOFT, false)));

        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SUCCESS, UseOutcome.CONSUME, UseOutcome.SKIPPED,
            UseOutcome.PASS_TO_EMPTY, UseOutcome.PASS, UseOutcome.FAIL}) {
            assertSame(Use.blockItem(outcome, true), Use.blockItem(outcome, false), outcome.name());
        }
    }

    @Test
    void blockEmptyAndItemSideFollowVanillaSignatures() {
        for (boolean client : new boolean[]{true, false}) {
            assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS, client));
            assertSame(client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER,
                Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
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
            assertSame(InteractionResult.SwingSource.NONE, of(Use.blockEmpty(UseOutcome.CONSUME, client)).swingSource());
            assertEquals(of(InteractionResult.PASS), of(Use.item(UseOutcome.SKIPPED, client)));
            assertEquals(of(InteractionResult.FAIL), of(Use.item(UseOutcome.FAIL, client)));
        }

        ItemStack held = new ItemStack(Items.BOWL, 3);
        InteractionResult.Success successUse = assertInstanceOf(InteractionResult.Success.class,
            Use.itemUse(UseOutcome.SUCCESS, held, false));
        assertSame(held, successUse.heldItemTransformedTo());
        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.CONSUME, UseOutcome.SKIPPED,
            UseOutcome.PASS_TO_EMPTY, UseOutcome.PASS, UseOutcome.FAIL}) {
            assertSame(Use.item(outcome, false), Use.itemUse(outcome, held, false), outcome.name());
        }
    }

    // ------------------------------------------------------------ 2. CONSUME 与 SKIPPED 语义不同

    @Test
    void consumeAndSkipHaveDifferentActionAndResultSemantics() {
        InteractionResult consume = Use.blockItem(UseOutcome.CONSUME, false);
        InteractionResult skip = Use.blockItem(UseOutcome.SKIPPED, false);

        assertNotSame(consume, skip);
        assertTrue(consume.consumesAction());
        assertFalse(skip.consumesAction());
        assertNotEquals(consume.consumesAction(), skip.consumesAction());
        assertEquals(of(InteractionResult.CONSUME), of(consume));
        assertEquals(of(InteractionResult.PASS), of(skip));
        assertSame(InteractionResult.SwingSource.NONE, of(consume).swingSource());
        assertTrue(of(consume).wasItemInteraction());
        assertFalse(of(skip).wasItemInteraction());
    }

    // ------------------------------------------------------------ 3. 26.2 的默认值事实

    @Test
    void blockUseDefaultsOn262AreTryWithEmptyHandAndPass() throws Exception {
        Method useItemOn = BlockBehaviour.class.getDeclaredMethod("useItemOn", ItemStack.class, BlockState.class,
            Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
        Method useWithoutItem = BlockBehaviour.class.getDeclaredMethod("useWithoutItem", BlockState.class,
            Level.class, BlockPos.class, Player.class, BlockHitResult.class);
        useItemOn.setAccessible(true);
        useWithoutItem.setAccessible(true);

        BlockState stoneState = Blocks.STONE.defaultBlockState();
        InteractionResult defaultItemOn = (InteractionResult) useItemOn.invoke(Blocks.STONE, ItemStack.EMPTY, stoneState,
            null, BlockPos.ZERO, null, InteractionHand.MAIN_HAND, null);
        InteractionResult defaultEmpty = (InteractionResult) useWithoutItem.invoke(Blocks.STONE, stoneState, null,
            BlockPos.ZERO, null, null);

        assertSame(InteractionResult.TRY_WITH_EMPTY_HAND, defaultItemOn);
        assertSame(InteractionResult.PASS, defaultEmpty);
        assertEquals(of(InteractionResult.PASS), of(defaultItemOn));
        assertEquals(of(InteractionResult.PASS), of(defaultEmpty));

        assertSame(defaultItemOn, Use.blockItem(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS, false));
    }

    // ------------------------------------------------------------ 4. 原语往返

    @Test
    void numericPrimitiveRoundTripKeepsExtremesAndEmptyArrays() {
        TagValueOutput out = newOut();
        CompoundWriter writer = new CompoundWriter(out);
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

        CompoundReader reader = newReader(out.buildResult());
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
        CompoundReader reader = newReader(tag);

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
        assertEquals(-7, reader.getInt("Text", -7));
        assertEquals(0.5F, reader.getFloat("Text", 0.5F));
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

    @Test
    void putItemSkipsEmptyAndKeepsComponents() {
        TagValueOutput out = newOut();
        CompoundWriter writer = new CompoundWriter(out);
        writer.putItem("Empty", ItemStack.EMPTY);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.DAMAGE, 5);
        writer.putItem("Sword", sword);
        writer.putItem("Bowl", new ItemStack(Items.BOWL, 3));
        CompoundTag tag = out.buildResult();

        assertFalse(tag.contains("Empty"));
        assertTrue(tag.contains("Sword"));
        assertInstanceOf(CompoundTag.class, tag.get("Sword"));

        ItemStack readBack = newReader(tag).getItem("Sword");
        assertEquals(Items.IRON_SWORD, readBack.getItem());
        assertEquals(1, readBack.getCount());
        assertEquals(Integer.valueOf(5), readBack.get(DataComponents.DAMAGE));
        assertEquals(3, newReader(tag).getItem("Bowl").getCount());
    }

    @Test
    void putItemsIsIdenticalToContainerHelperSaveAllItems() {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(Items.BOWL, 3), ItemStack.EMPTY,
            new ItemStack(Items.STICK, 64), ItemStack.EMPTY, new ItemStack(Items.CARROT, 1)));
        TagValueOutput oursOut = newOut();
        new CompoundWriter(oursOut).putItems(items);
        CompoundTag ours = oursOut.buildResult();

        TagValueOutput vanillaOut = newOut();
        NonNullList<ItemStack> vanillaList = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            vanillaList.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(vanillaOut, vanillaList);
        CompoundTag vanilla = vanillaOut.buildResult();

        assertEquals(vanilla, ours);
        assertEquals(vanilla.toString(), ours.toString());
        assertInstanceOf(ListTag.class, ours.get("Items"));
        assertEquals(3, ours.getListOrEmpty("Items").size());

        TagValueOutput oursEmptyOut = newOut();
        new CompoundWriter(oursEmptyOut).putItems(List.of(ItemStack.EMPTY, ItemStack.EMPTY));
        TagValueOutput vanillaEmptyOut = newOut();
        ContainerHelper.saveAllItems(vanillaEmptyOut, NonNullList.withSize(2, ItemStack.EMPTY));
        assertEquals(vanillaEmptyOut.buildResult(), oursEmptyOut.buildResult());
    }

    @Test
    void emptyItemListWritesListTagAndLoadsNothing() {
        TagValueOutput out = newOut();
        new CompoundWriter(out).putItems(Collections.emptyList());
        CompoundTag tag = out.buildResult();

        assertTrue(tag.contains("Items"));
        assertInstanceOf(ListTag.class, tag.get("Items"));
        assertEquals(0, tag.getListOrEmpty("Items").size());
        assertEquals("{Items:[]}", tag.toString());

        CompoundReader reader = newReader(tag);
        assertEquals(0, reader.items().size());
        List<ItemStack> outList = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        reader.loadItems(outList);
        for (ItemStack stack : outList) {
            assertTrue(stack.isEmpty());
        }

        TagValueOutput childOut = newOut();
        new CompoundWriter(childOut).childList("Plates");
        CompoundTag childTag = childOut.buildResult();
        assertTrue(childTag.contains("Plates"));
        assertInstanceOf(ListTag.class, childTag.get("Plates"));
        assertEquals(0, childTag.getListOrEmpty("Plates").size());
        assertEquals(0, newReader(childTag).children("Plates").size());
    }

    @Test
    void childAndChildListKeepTypesNestingAndOrder() {
        TagValueOutput out = newOut();
        CompoundWriter writer = new CompoundWriter(out);
        var fuel = writer.child("Fuel");
        fuel.child("Deep").putInt("Level", 3);
        fuel.putItems(List.of(new ItemStack(Items.COAL, 2)));
        writer.child("Empty");
        var plates = writer.childList("Plates");
        plates.addChild().putInt("Order", 1);
        plates.addChild().putInt("Order", 2);
        plates.addChild().putString("Name", "minecraft:oak_planks");
        CompoundTag tag = out.buildResult();

        assertInstanceOf(CompoundTag.class, tag.get("Fuel"));
        assertInstanceOf(CompoundTag.class, tag.get("Empty"));
        assertInstanceOf(ListTag.class, tag.get("Plates"));
        assertEquals(3, tag.getListOrEmpty("Plates").size());

        CompoundReader reader = newReader(tag);
        assertTrue(reader.hasCompound("Fuel"));
        assertTrue(reader.child("Fuel").hasCompound("Deep"));
        assertEquals(3, reader.child("Fuel").child("Deep").getInt("Level", -1));
        assertEquals(1, reader.child("Fuel").items().size());
        assertEquals(Items.COAL, reader.child("Fuel").items().get(0).stack().getItem());

        List<Object> orders = new ArrayList<>();
        for (var entry : reader.children("Plates")) {
            orders.add(entry.getInt("Order", -1));
        }
        assertEquals(List.of(1, 2, -1), orders);
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
        var ops = registries().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag legacy = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.BOWL, 2)).getOrThrow();
        list.add(legacy);
        CompoundTag slotted = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.CARROT, 5)).getOrThrow();
        slotted.putByte("Slot", (byte) 130);
        list.add(slotted);
        CompoundTag broken = new CompoundTag();
        broken.putString("id", "minecraft:definitely_not_an_item");
        broken.putInt("count", 1);
        list.add(broken);
        tag.put("Items", list);

        List<StoredItem> stored = newReader(tag).items();
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
        assertEquals(0, newReader(wrongType).items().size());
    }

    @Test
    void itemListLoadsIntoContainerBySlotWithLegacyFallback() {
        var ops = registries().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag first = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.BOWL, 2)).getOrThrow();
        first.putByte("Slot", (byte) 0);
        list.add(first);
        CompoundTag fifth = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.CARROT, 1)).getOrThrow();
        fifth.putByte("Slot", (byte) 5);
        list.add(fifth);
        tag.put("Items", list);

        CompoundReader reader = newReader(tag);
        List<ItemStack> viaOurs = new ArrayList<>(Collections.nCopies(8, ItemStack.EMPTY));
        reader.loadItems(viaOurs);

        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries(), tag), viaVanilla);

        for (int i = 0; i < viaOurs.size(); i++) {
            assertEquals(viaVanilla.get(i).getItem(), viaOurs.get(i).getItem(), "槽 " + i);
            assertEquals(viaVanilla.get(i).getCount(), viaOurs.get(i).getCount(), "槽 " + i);
        }
        assertEquals(Items.BOWL, viaOurs.get(0).getItem());
        assertEquals(Items.CARROT, viaOurs.get(5).getItem());

        List<ItemStack> shortTarget = new ArrayList<>(Collections.nCopies(1, ItemStack.EMPTY));
        reader.loadItems(shortTarget);
        NonNullList<ItemStack> shortVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries(), tag), shortVanilla);
        assertEquals(shortVanilla.get(0).getItem(), shortTarget.get(0).getItem());
    }

    @Test
    void loadItemsWithoutItemsKeyPreservesTheTargetLikeVanilla() {
        List<ItemStack> viaOurs = new ArrayList<>(List.of(new ItemStack(Items.BOWL, 1)));
        newReader(new CompoundTag()).loadItems(viaOurs);
        assertEquals(Items.BOWL, viaOurs.get(0).getItem());

        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        viaVanilla.set(0, new ItemStack(Items.BOWL, 1));
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries(), new CompoundTag()), viaVanilla);
        assertEquals(Items.BOWL, viaVanilla.get(0).getItem());
        assertEquals(viaVanilla.get(0).getItem(), viaOurs.get(0).getItem());
    }

    @Test
    void childOpensAFreshNodeSoTheWriterMustBeReused() {
        TagValueOutput out = newOut();
        CompoundWriter writer = new CompoundWriter(out);
        writer.child("Fuel").putInt("A", 1);
        writer.child("Fuel").putInt("B", 2);
        CompoundReader reader = newReader(out.buildResult());
        assertEquals(2, reader.child("Fuel").getInt("B", -1));
        assertEquals(-1, reader.child("Fuel").getInt("A", -1));

        TagValueOutput reusedOut = newOut();
        CompoundWriter writer2 = new CompoundWriter(reusedOut);
        var fuel = writer2.child("Fuel");
        fuel.putInt("A", 1);
        fuel.putInt("B", 2);
        CompoundReader reader2 = newReader(reusedOut.buildResult());
        assertEquals(1, reader2.child("Fuel").getInt("A", -1));
        assertEquals(2, reader2.child("Fuel").getInt("B", -1));
    }

    // ------------------------------------------------------------ 小工具

    private static void assertSemantics(InteractionResult expected, InteractionResult actual) {
        assertEquals(of(expected), of(actual), String.valueOf(actual));
        assertSame(expected, actual);
    }
}
