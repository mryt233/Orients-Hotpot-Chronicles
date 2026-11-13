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
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三批 3G：版本适配小包<b>语义层</b>的独立验收（26.1 版，与 {@link Opt3gAdapterTest} 互补，不重复它的用例）。
 *
 * <p>mryt233 加（2026-11-11）；mryt233 改（2026-11-13，适配 26.1 {@link InteractionResult} sealed interface
 * 与 {@code TRY_WITH_EMPTY_HAND} 默认值）。</p>
 */
class Opt3gUseMappingSemanticsTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
    }

    // ------------------------------------------------------------ 语义签名

    private record Semantics(
        Class<? extends InteractionResult> kind,
        boolean consumesAction,
        InteractionResult.SwingSource swingSource,
        boolean wasItemInteraction
    ) {
    }

    private static Semantics of(InteractionResult result) {
        if (result instanceof InteractionResult.Success s) {
            return new Semantics(s.getClass(), s.consumesAction(), s.swingSource(), s.wasItemInteraction());
        }
        return new Semantics(result.getClass(), result.consumesAction(), InteractionResult.SwingSource.NONE, false);
    }

    // ------------------------------------------------------------ 1. 七个意图的语义

    @Test
    void blockItemIntentSignaturesFollowVanillaSemantics() {
        for (boolean client : new boolean[]{true, false}) {
            assertSemantics(InteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS, client));
            assertSemantics(InteractionResult.CONSUME, Use.blockItem(UseOutcome.CONSUME, client));
            assertSemantics(InteractionResult.PASS, Use.blockItem(UseOutcome.SKIPPED, client));
            assertSemantics(InteractionResult.TRY_WITH_EMPTY_HAND, Use.blockItem(UseOutcome.PASS_TO_EMPTY, client));
            assertSemantics(InteractionResult.FAIL, Use.blockItem(UseOutcome.FAIL, client));
        }

        assertTrue(of(InteractionResult.SUCCESS).consumesAction());
        assertTrue(of(InteractionResult.CONSUME).consumesAction());
        assertFalse(of(InteractionResult.PASS).consumesAction());
        assertFalse(of(InteractionResult.TRY_WITH_EMPTY_HAND).consumesAction());
        assertFalse(of(InteractionResult.FAIL).consumesAction());
    }

    @Test
    void successSoftBehavesLikeSidedSuccessOnBothSides() {
        for (boolean client : new boolean[]{true, false}) {
            InteractionResult expected = client ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            assertSame(expected, Use.blockItem(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(expected), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, client)));

            assertEquals(of(expected), of(Use.item(UseOutcome.SUCCESS_SOFT, client)));

            InteractionResult.Success held = assertInstanceOf(InteractionResult.Success.class,
                Use.itemUse(UseOutcome.SUCCESS_SOFT, new ItemStack(Items.BOWL), client));
            assertEquals(of(expected).swingSource(), held.swingSource());
            assertTrue(held.consumesAction());

            assertSame(expected, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(expected), of(Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client)));
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
            InteractionResult sided = client ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS, client));
            assertSame(sided, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
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
            assertEquals(InteractionResult.SwingSource.NONE, of(Use.blockEmpty(UseOutcome.CONSUME, client)).swingSource());
            assertEquals(of(InteractionResult.PASS), of(Use.item(UseOutcome.SKIPPED, client)));
            assertEquals(of(InteractionResult.FAIL), of(Use.item(UseOutcome.FAIL, client)));
        }

        ItemStack held = new ItemStack(Items.BOWL, 3);
        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SUCCESS, UseOutcome.CONSUME}) {
            InteractionResult.Success res = assertInstanceOf(InteractionResult.Success.class,
                Use.itemUse(outcome, held, false));
            assertSame(held, res.heldItemTransformedTo(), outcome.name());
            assertEquals(of(Use.item(outcome, false)), of(res), outcome.name());
        }
        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SKIPPED, UseOutcome.PASS_TO_EMPTY, UseOutcome.PASS,
            UseOutcome.FAIL}) {
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

        assertSame(InteractionResult.CONSUME, consume);
        assertSame(InteractionResult.PASS, skip);
        assertEquals(InteractionResult.SwingSource.NONE, of(consume).swingSource());
        assertTrue(of(consume).wasItemInteraction());
        assertFalse(of(skip).wasItemInteraction());
    }

    // ------------------------------------------------------------ 3. 26.1 的三个默认值事实

    @Test
    void blockUseDefaultsOn261AreTryWithEmptyHandAndPass() throws Exception {
        Method useItemOn = BlockBehaviour.class.getDeclaredMethod("useItemOn", ItemStack.class, BlockState.class,
            Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
        Method useWithoutItem = BlockBehaviour.class.getDeclaredMethod("useWithoutItem", BlockState.class,
            Level.class, BlockPos.class, Player.class, BlockHitResult.class);
        useItemOn.setAccessible(true);
        useWithoutItem.setAccessible(true);

        BlockState stoneState = Blocks.STONE.defaultBlockState();
        InteractionResult defaultItemOn = (InteractionResult) useItemOn.invoke(
            Blocks.STONE, ItemStack.EMPTY, stoneState, null, BlockPos.ZERO, null, InteractionHand.MAIN_HAND, null);
        InteractionResult defaultEmpty = (InteractionResult) useWithoutItem.invoke(
            Blocks.STONE, stoneState, null, BlockPos.ZERO, null, null);

        assertSame(InteractionResult.TRY_WITH_EMPTY_HAND, defaultItemOn);
        assertSame(InteractionResult.PASS, defaultEmpty);
        assertFalse(defaultItemOn.consumesAction());
        assertFalse(defaultEmpty.consumesAction());

        assertSame(defaultItemOn, Use.blockItem(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS, false));
    }

    // ------------------------------------------------------------ 4. 原语往返

    /** 数值 / 字符串 / 数组的极端值往返：负数、MIN / MAX、NaN、Infinity、空串、空数组、非 ASCII。 */
    @Test
    void numericPrimitiveRoundTripKeepsExtremesAndEmptyArrays() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
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

        CompoundReader reader = new CompoundReader(tag, registries());
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

        // 空串是「有键」而不是「缺键」：has 必须为真，兜底值不能被拿出来。
        assertTrue(reader.has("EmptyString"));
        assertTrue(reader.has("EmptyInts"));
        assertTrue(reader.has("EmptyBytes"));
    }

    /**
     * 兜底值只在<b>缺键</b>时生效，和原版 {@code CompoundTag.contains(key) ? get : fallback} 的写法一致；
     * 键在但类型不对时给的是 {@code CompoundTag} 自己的默认（数值 0 / 空串），不是调用方给的 fallback。
     */
    @Test
    void fallbacksApplyOnlyWhenTheKeyIsAbsent() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Text", "abc");
        tag.putDouble("Decimal", 1.5D);
        tag.putFloat("Single", 2.5F);
        tag.putBoolean("Flag", true);
        CompoundReader reader = new CompoundReader(tag, registries());

        // 缺键：逐个原语回调用方给的兜底。
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

        // 键在但类型不对：has 为真，26.1 CompoundTag.getIntOr/getFloatOr 返回兜底值。
        assertTrue(reader.has("Text"));
        assertEquals(-7, reader.getInt("Text", -7));
        assertEquals(0.5F, reader.getFloat("Text", 0.5F));
        assertFalse(reader.hasNumber("Text"));
        assertFalse(reader.hasCompound("Text"));
        assertTrue(reader.getItem("Text").isEmpty());
        // 数值型键之间可以互读（原版 getInt / getFloat / getDouble 都按 Number 取）。
        assertEquals(1, reader.getInt("Decimal", -7));
        assertEquals(1.5F, reader.getFloat("Decimal", -7F));
        assertEquals(2.5D, reader.getDouble("Single", -7D));
        assertTrue(reader.hasNumber("Decimal"));
        assertTrue(reader.hasNumber("Single"));
        // 原版把布尔也当数字（TAG_ANY_NUMERIC 覆盖 byte）。
        assertTrue(reader.hasNumber("Flag"));
        assertEquals(1, reader.getInt("Flag", -7));

        // null 标签当空档处理：所有键都缺，兜底值统一。
        CompoundReader empty = new CompoundReader(null, registries());
        assertEquals(-7, empty.getInt("Missing", -7));
        assertFalse(empty.has("Missing"));
        assertEquals(0, empty.items().size());
        assertEquals(0, empty.children("Missing").size());
        assertEquals(-7, empty.child("Missing").getInt("Anything", -7));
    }

    /** 写出空栈要跳过（原版 {@code ContainerHelper} / {@code ItemStack.save} 的写法就是非空才写）。 */
    @Test
    void putItemSkipsEmptyAndKeepsComponents() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        writer.putItem("Empty", ItemStack.EMPTY);
        assertFalse(tag.contains("Empty"));

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.DAMAGE, 5);
        writer.putItem("Sword", sword);
        assertTrue(tag.contains("Sword"));
        assertEquals(Tag.TAG_COMPOUND, tag.get("Sword").getId());

        ItemStack readBack = new CompoundReader(tag, registries()).getItem("Sword");
        assertEquals(Items.IRON_SWORD, readBack.getItem());
        assertEquals(1, readBack.getCount());
        assertEquals(Integer.valueOf(5), readBack.get(DataComponents.DAMAGE));

        // 计数也要原样回来。
        writer.putItem("Bowl", new ItemStack(Items.BOWL, 3));
        assertEquals(3, new CompoundReader(tag, registries()).getItem("Bowl").getCount());
    }

    /**
     * {@code putItems} 写出来的必须是原版 {@code ContainerHelper.saveAllItems} 那份：
     * 键名 {@code Items}、每条带 {@code Slot}、空槽不写、条目顺序按输入顺序。
     */
    @Test
    void putItemsIsIdenticalToContainerHelperSaveAllItems() {
        List<ItemStack> items = new ArrayList<>(Arrays.asList(new ItemStack(Items.BOWL, 3), ItemStack.EMPTY,
            new ItemStack(Items.STICK, 64), ItemStack.EMPTY, new ItemStack(Items.CARROT, 1)));
        CompoundTag ours = new CompoundTag();
        new CompoundWriter(ours, registries()).putItems(items);

        NonNullList<ItemStack> vanillaList = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            vanillaList.set(i, items.get(i));
        }
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries());
        ContainerHelper.saveAllItems(out, vanillaList, true);
        CompoundTag vanilla = out.buildResult();

        assertEquals(vanilla, ours);
        assertEquals(vanilla.toString(), ours.toString());
        assertEquals(Tag.TAG_LIST, ours.get("Items").getId());
        assertEquals(3, ours.getListOrEmpty("Items").size());

        // 全空表也一样要逐字相同（原版三参数重载的空表结果就是 {Items:[]}）。
        CompoundTag oursEmpty = new CompoundTag();
        new CompoundWriter(oursEmpty, registries()).putItems(Arrays.asList(ItemStack.EMPTY, ItemStack.EMPTY));
        TagValueOutput outEmpty = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries());
        ContainerHelper.saveAllItems(outEmpty, NonNullList.withSize(2, ItemStack.EMPTY), true);
        CompoundTag vanillaEmpty = outEmpty.buildResult();
        assertEquals(vanillaEmpty, oursEmpty);
        assertEquals(vanillaEmpty.toString(), oursEmpty.toString());
    }

    /** 空表也写 {@code Items} 键，类型是 ListTag（原版 {@code {Items:[]}}），并且读回来是空表。 */
    @Test
    void emptyItemListWritesListTagAndLoadsNothing() {
        CompoundTag tag = new CompoundTag();
        new CompoundWriter(tag, registries()).putItems(Collections.emptyList());

        assertTrue(tag.contains("Items"));
        assertEquals(Tag.TAG_LIST, tag.get("Items").getId());
        assertEquals(0, tag.getListOrEmpty("Items").size());
        assertEquals("{Items:[]}", tag.toString());

        CompoundReader reader = new CompoundReader(tag, registries());
        assertEquals(0, reader.items().size());
        List<ItemStack> out = new ArrayList<>(Arrays.asList(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        reader.loadItems(out);
        for (ItemStack stack : out) {
            assertTrue(stack.isEmpty());
        }

        // 空 childList 也一样：键在、是 ListTag，只是没有条目。
        CompoundTag childTag = new CompoundTag();
        new CompoundWriter(childTag, registries()).childList("Plates");
        assertTrue(childTag.contains("Plates"));
        assertEquals(Tag.TAG_LIST, childTag.get("Plates").getId());
        assertEquals(0, childTag.getListOrEmpty("Plates").size());
        assertEquals(0, new CompoundReader(childTag, registries()).children("Plates").size());
    }

    /** 子节点 / 子节点列表的类型、嵌套、条目顺序，以及缺键 / 类型不符时的空 reader。 */
    @Test
    void childAndChildListKeepTypesNestingAndOrder() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        var fuel = writer.child("Fuel");
        fuel.child("Deep").putInt("Level", 3);
        fuel.putItems(List.of(new ItemStack(Items.COAL, 2)));
        writer.child("Empty");
        var plates = writer.childList("Plates");
        plates.addChild().putInt("Order", 1);
        plates.addChild().putInt("Order", 2);
        plates.addChild().putString("Name", "minecraft:oak_planks");

        // 类型：child 是 CompoundTag，childList 是「放 CompoundTag 的 ListTag」。
        assertEquals(Tag.TAG_COMPOUND, tag.get("Fuel").getId());
        assertEquals(Tag.TAG_COMPOUND, tag.get("Empty").getId());
        assertEquals(Tag.TAG_LIST, tag.get("Plates").getId());
        assertEquals(3, tag.getListOrEmpty("Plates").size());

        CompoundReader reader = new CompoundReader(tag, registries());
        // 嵌套：Fuel -> Deep 两层都读得到，Fuel 自己也带着物品列表。
        assertTrue(reader.hasCompound("Fuel"));
        assertTrue(reader.child("Fuel").hasCompound("Deep"));
        assertEquals(3, reader.child("Fuel").child("Deep").getInt("Level", -1));
        assertEquals(1, reader.child("Fuel").items().size());
        assertEquals(Items.COAL, reader.child("Fuel").items().get(0).stack().getItem());

        // 条目顺序：children() 按下标顺序交回，不是无序集合。
        List<Object> orders = new ArrayList<>();
        for (var entry : reader.children("Plates")) {
            orders.add(entry.getInt("Order", -1));
        }
        assertEquals(Arrays.asList(1, 2, -1), orders);
        assertEquals("minecraft:oak_planks", reader.children("Plates").get(2).getString("Name", ""));

        // child 是空 CompoundTag 时能读到、但里面什么都没有。
        assertTrue(reader.has("Empty"));
        assertFalse(reader.child("Empty").has("Anything"));
        assertEquals(42, reader.child("Empty").getInt("Anything", 42));

        // 缺键 / 类型不符：child 回空 reader、children 回空列表，不抛异常。
        assertEquals(42, reader.child("Missing").getInt("Anything", 42));
        assertEquals(0, reader.children("Missing").size());
        assertEquals(0, reader.children("Fuel").size()); // 是 compound 不是 list
        assertFalse(reader.child("Plates").has("Order")); // 是 list 不是 compound
        assertEquals(0, reader.children("Empty").size());
    }

    /**
     * {@code StoredItem} 要如实交回「这条有没有 {@code Slot}」：旧存档（{@code SimpleInventory.toNbtList}
     * 生成、没有 Slot）必须能被区分出来，坏条目也要占着位置、物品是空栈。
     */
    @Test
    void storedItemsReportLegacyEntriesWithoutSlot() {
        RegistryOps<Tag> ops = registries().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag legacy = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.BOWL, 2)).getOrThrow();
        list.add(legacy);
        CompoundTag slotted = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.CARROT, 5)).getOrThrow();
        slotted.putByte("Slot", (byte) 130); // 原版 Slot 存的是 byte，读出时要按无符号处理
        list.add(slotted);
        CompoundTag broken = new CompoundTag();
        broken.putString("id", "minecraft:definitely_not_an_item");
        broken.putInt("count", 1);
        list.add(broken);
        tag.put("Items", list);

        List<StoredItem> stored = new CompoundReader(tag, registries()).items();
        assertEquals(3, stored.size());

        // 旧档：没有 Slot，slot 用 -1 占位，hasSlot 为假。
        assertFalse(stored.get(0).hasSlot());
        assertEquals(-1, stored.get(0).slot());
        assertEquals(Items.BOWL, stored.get(0).stack().getItem());
        assertEquals(2, stored.get(0).stack().getCount());

        // 有 Slot：130 按无符号读出来还是 130。
        assertTrue(stored.get(1).hasSlot());
        assertEquals(130, stored.get(1).slot());
        assertEquals(Items.CARROT, stored.get(1).stack().getItem());

        // 坏条目：位置还在（不能静默丢条目），物品是空栈。
        assertFalse(stored.get(2).hasSlot());
        assertEquals(-1, stored.get(2).slot());
        assertTrue(stored.get(2).stack().isEmpty());

        // Items 键类型不对时按「没有物品」处理。
        CompoundTag wrongType = new CompoundTag();
        wrongType.putString("Items", "not_a_list");
        assertEquals(0, new CompoundReader(wrongType, registries()).items().size());
    }

    /**
     * 读档放回容器：有 Slot 的按槽位放、没有 Slot 的旧档按写入顺序放，
     * 必须和原版 {@code ContainerHelper.loadAllItems} 逐槽相同（这里直接拿原版结果对比，不猜顺序）。
     */
    @Test
    void itemListLoadsIntoContainerBySlotWithLegacyFallback() {
        RegistryOps<Tag> ops = registries().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag first = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.BOWL, 2)).getOrThrow();
        first.putByte("Slot", (byte) 0);
        list.add(first);
        CompoundTag fifth = (CompoundTag) ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.CARROT, 1)).getOrThrow();
        fifth.putByte("Slot", (byte) 5);
        list.add(fifth);
        // 旧档夹在中间，没有 Slot。
        list.add(ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.STICK, 3)).getOrThrow());
        tag.put("Items", list);

        CompoundReader reader = new CompoundReader(tag, registries());
        List<ItemStack> viaOurs = new ArrayList<>(Collections.nCopies(8, ItemStack.EMPTY));
        reader.loadItems(viaOurs);

        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries(), tag), viaVanilla);

        for (int i = 0; i < viaOurs.size(); i++) {
            assertEquals(viaVanilla.get(i).getItem(), viaOurs.get(i).getItem(), "槽 " + i);
            assertEquals(viaVanilla.get(i).getCount(), viaOurs.get(i).getCount(), "槽 " + i);
        }
        // 单独核实一遍最容易出错的那条：旧档（无 Slot）按原版规则进 0 号槽。
        assertEquals(Items.STICK, viaOurs.get(0).getItem());
        assertEquals(Items.CARROT, viaOurs.get(5).getItem());

        // 容器比存档短时不能越界（原版也忽略超出部分的条目），越界的 Slot 5 不能被写出去抛异常。
        List<ItemStack> shortTarget = new ArrayList<>(Collections.nCopies(1, ItemStack.EMPTY));
        reader.loadItems(shortTarget);
        NonNullList<ItemStack> shortVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, registries(), tag), shortVanilla);
        assertEquals(shortVanilla.get(0).getItem(), shortTarget.get(0).getItem());
    }

    /**
     * 缺 {@code Items} 键时 {@link CompoundReader#loadItems} 和原版 {@code ContainerHelper.loadAllItems}
     * 一样：什么都不做，容器保持原样（不是清空）。
     */
    @Test
    void loadItemsWithoutItemsKeyPreservesTheTargetLikeVanilla() {
        List<ItemStack> viaOurs = new ArrayList<>(List.of(new ItemStack(Items.BOWL, 1)));
        new CompoundReader(new CompoundTag(), registries()).loadItems(viaOurs);
        assertEquals(Items.BOWL, viaOurs.get(0).getItem());

        // 原版对同一份缺键的标签：容器同样原封不动。
        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        viaVanilla.set(0, new ItemStack(Items.BOWL, 1));
        ContainerHelper.loadAllItems(
            TagValueInput.create(ProblemReporter.DISCARDING, registries(), new CompoundTag()), viaVanilla);
        assertEquals(Items.BOWL, viaVanilla.get(0).getItem());

        assertEquals(viaVanilla.get(0).getItem(), viaOurs.get(0).getItem());
    }

    /**
     * <b>使用陷阱（如实钉住）</b>：{@link StoreWriter#child(String)} 每调一次都新建一个空节点并覆盖同名键，
     * 所以对一个子节点写多个键时必须复用第一次拿到的 writer——
     * {@code writer.child("Fuel").putA(); writer.child("Fuel").putB();} 会把 A 冲掉。
     * 业务代码目前都只取一次（如 {@code StoveBlockEntity} 的 {@code w.child("Fuel").putItems(list)}），
     * 这里把这个约定写下来，防止以后拆成多句。
     */
    @Test
    void childOpensAFreshNodeSoTheWriterMustBeReused() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        writer.child("Fuel").putInt("A", 1);
        writer.child("Fuel").putInt("B", 2);
        // 第二次 child("Fuel") 换了新节点，A 已经不在了。
        CompoundReader reader = new CompoundReader(tag, registries());
        assertEquals(2, reader.child("Fuel").getInt("B", -1));
        assertEquals(-1, reader.child("Fuel").getInt("A", -1));

        // 复用 writer 才对：同一个子节点上两个键都在。
        CompoundTag reused = new CompoundTag();
        CompoundWriter writer2 = new CompoundWriter(reused, registries());
        var fuel = writer2.child("Fuel");
        fuel.putInt("A", 1);
        fuel.putInt("B", 2);
        CompoundReader reader2 = new CompoundReader(reused, registries());
        assertEquals(1, reader2.child("Fuel").getInt("A", -1));
        assertEquals(2, reader2.child("Fuel").getInt("B", -1));
    }

    // ------------------------------------------------------------ 小工具

    /** 断言「实际换回来的常量」和 {@code expected} 语义一致。 */
    private static void assertSemantics(InteractionResult expected, InteractionResult actual) {
        assertEquals(of(expected), of(actual), String.valueOf(actual));
        assertSame(expected, actual);
    }
}
