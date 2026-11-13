// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundWriter;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoredItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三批 3G：版本适配小包的两组验收（26.2 适配）。
 *
 * <p>第一组把「意图 → 26.2 原版常量」的映射钉死：改前每个调用点是什么，改后换回来必须还是什么，
 * 任何一次映射漂移都会在这里报出来。第二组走读写原语的往返：写出来的标签要和原版写法逐字相同、
 * 读回来要和真实存档一致。</p>
 *
 * <p>作者：mryt233</p>
 */
class Opt3gAdapterTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    /**
     * 单测环境没有真实世界：从 Bootstrap 好的注册表里拼一个只读 Provider。
     * 物品的序列化只查物品 / 方块 / 药水等基础注册表，这里把它们一起放进去。
     */
    private static HolderLookup.Provider registries() {
        return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
            net.minecraft.core.registries.BuiltInRegistries.REGISTRY).freeze();
    }

    private static TagValueOutput newOut() {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries());
    }

    private static CompoundReader newReader(CompoundTag tag) {
        return new CompoundReader(TagValueInput.create(ProblemReporter.DISCARDING, registries(), tag));
    }

    // ------------------------------------------------------------ 右键意图映射

    @Test
    void blockItemMappingMatchesVanillaConstants() {
        assertSame(InteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS, false));
        assertSame(InteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS_SOFT, true));
        assertSame(InteractionResult.SUCCESS_SERVER, Use.blockItem(UseOutcome.SUCCESS_SOFT, false));
        assertSame(InteractionResult.CONSUME, Use.blockItem(UseOutcome.CONSUME, false));
        assertSame(InteractionResult.PASS, Use.blockItem(UseOutcome.SKIPPED, false));
        assertSame(InteractionResult.TRY_WITH_EMPTY_HAND,
            Use.blockItem(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(InteractionResult.FAIL, Use.blockItem(UseOutcome.FAIL, false));
        assertSame(InteractionResult.PASS, Use.blockItem(UseOutcome.PASS, false));
    }

    @Test
    void blockEmptyMappingMatchesVanillaConstants() {
        assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS, false));
        assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, true));
        assertSame(InteractionResult.SUCCESS_SERVER, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, false));
        assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.CONSUME, false));
        assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.SKIPPED, false));
        assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS, false));
        assertSame(InteractionResult.FAIL, Use.blockEmpty(UseOutcome.FAIL, false));
    }

    @Test
    void itemMappingMatchesVanillaConstants() {
        assertSame(InteractionResult.SUCCESS, Use.item(UseOutcome.SUCCESS, false));
        assertSame(InteractionResult.SUCCESS, Use.item(UseOutcome.SUCCESS_SOFT, true));
        assertSame(InteractionResult.SUCCESS_SERVER, Use.item(UseOutcome.SUCCESS_SOFT, false));
        assertSame(InteractionResult.CONSUME, Use.item(UseOutcome.CONSUME, false));
        assertSame(InteractionResult.PASS, Use.item(UseOutcome.PASS, false));
        assertSame(InteractionResult.FAIL, Use.item(UseOutcome.FAIL, false));
    }

    /** 物品 {@code use}：{@code InteractionResult} 的结果与换掉的手持物都要对。 */
    @Test
    void itemUseKeepsOutcomeAndHeldStack() {
        ItemStack held = new ItemStack(Items.BOWL);
        ItemStack swapped = new ItemStack(Items.WATER_BUCKET);
        InteractionResult.Success success = assertInstanceOf(InteractionResult.Success.class,
            Use.itemUse(UseOutcome.SUCCESS, held, false));
        assertSame(InteractionResult.SwingSource.CLIENT, success.swingSource());
        assertSame(held, success.heldItemTransformedTo());

        InteractionResult.Success softClient = assertInstanceOf(InteractionResult.Success.class,
            Use.itemUse(UseOutcome.SUCCESS_SOFT, held, true));
        assertSame(InteractionResult.SwingSource.CLIENT, softClient.swingSource());

        InteractionResult.Success softServer = assertInstanceOf(InteractionResult.Success.class,
            Use.itemUse(UseOutcome.SUCCESS_SOFT, swapped, false));
        assertSame(InteractionResult.SwingSource.SERVER, softServer.swingSource());
        assertSame(swapped, softServer.heldItemTransformedTo());

        assertSame(InteractionResult.CONSUME, Use.itemUse(UseOutcome.CONSUME, held, false));
        assertSame(InteractionResult.PASS, Use.itemUse(UseOutcome.PASS, held, false));
        assertSame(InteractionResult.FAIL, Use.itemUse(UseOutcome.FAIL, held, false));
    }

    /** 意图的种类必须覆盖主工程用到的全部原版常量，少一个就会有调用点没法表达。 */
    @Test
    void intentsCoverEveryConstantUsedInTheProject() {
        List<InteractionResult> mapped = new ArrayList<>();
        for (UseOutcome outcome : UseOutcome.values()) {
            mapped.add(Use.blockItem(outcome, false));
        }
        for (InteractionResult expected : List.of(
                InteractionResult.SUCCESS,
                InteractionResult.SUCCESS_SERVER,
                InteractionResult.CONSUME,
                InteractionResult.PASS,
                InteractionResult.TRY_WITH_EMPTY_HAND,
                InteractionResult.FAIL)) {
            assertTrue(mapped.contains(expected), String.valueOf(expected));
        }
        assertSame(InteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS_SOFT, true));
        assertSame(InteractionResult.SUCCESS_SERVER, Use.blockItem(UseOutcome.SUCCESS_SOFT, false));
        assertTrue(Arrays.stream(UseOutcome.values()).anyMatch(o -> o == UseOutcome.SUCCESS_SOFT));
    }

    /**
     * 语义核实（写进单测防止有人「顺手合并」）：
     * 26.2 的 CONSUME 会 {@code consumesAction()}，PASS 不会——两者不能共用一个意图。
     */
    @Test
    void consumeAndSkipAreNotTheSameOn262() {
        assertTrue(Use.blockItem(UseOutcome.CONSUME, false).consumesAction());
        assertFalse(Use.blockItem(UseOutcome.SKIPPED, false).consumesAction());
        assertNotSame(Use.blockItem(UseOutcome.CONSUME, false), Use.blockItem(UseOutcome.SKIPPED, false));
    }

    // ------------------------------------------------------------ 存读档原语往返

    @Test
    void writerAndReaderRoundTripEveryPrimitive() {
        TagValueOutput out = newOut();
        CompoundWriter writer = new CompoundWriter(out);
        writer.putInt("Int", 42);
        writer.putFloat("Float", 1.5F);
        writer.putDouble("Double", 2.25D);
        writer.putBoolean("Bool", true);
        writer.putString("String", "abc");
        writer.putIntArray("Ints", new int[]{1, 2, 3});
        writer.putByteArray("Bytes", new byte[]{1, 0, 1});
        writer.putItem("Item", new ItemStack(Items.BOWL, 1));
        writer.putItems(List.of(new ItemStack(Items.BOWL, 1), ItemStack.EMPTY, new ItemStack(Items.STICK, 1)));
        writer.child("Child").putInt("Deep", 7);
        var list = writer.childList("Plates");
        list.addChild().putString("Plate", "minecraft:oak_planks");
        list.addChild().putDouble("X", 8.0);
        CompoundTag tag = out.buildResult();

        CompoundReader reader = newReader(tag);
        assertEquals(42, reader.getInt("Int", -1));
        assertEquals(1.5F, reader.getFloat("Float", 0F));
        assertEquals(2.25D, reader.getDouble("Double", 0D));
        assertTrue(reader.getBoolean("Bool", false));
        assertEquals("abc", reader.getString("String", ""));
        assertArrayEquals(new int[]{1, 2, 3}, reader.getIntArray("Ints"));
        assertArrayEquals(new byte[]{1, 0, 1}, reader.getByteArray("Bytes"));
        assertTrue(reader.has("Item"));
        assertTrue(reader.hasCompound("Child"));
        assertFalse(reader.hasCompound("Int"));
        assertEquals(Items.BOWL, reader.getItem("Item").getItem());
        // 缺键、坏档的兜底。
        assertTrue(reader.getItem("Missing").isEmpty());
        assertEquals(-1, reader.getInt("Missing", -1));
        assertEquals(0F, reader.getFloat("Missing", 0F));
        assertEquals(0, reader.getIntArray("Missing").length);
        assertEquals(0, reader.getByteArray("Missing").length);
        assertEquals(7, reader.child("Child").getInt("Deep", 0));
        assertEquals(0, reader.child("Missing").getInt("Deep", 0));
        assertEquals(2, reader.children("Plates").size());
        assertEquals("minecraft:oak_planks", reader.children("Plates").get(0).getString("Plate", ""));
        assertEquals(0, reader.children("Missing").size());
    }

    /** 物品列表必须还是原版 {@code Items} + 每条 {@code Slot}，逐字相同。 */
    @Test
    void itemListIsWrittenExactlyLikeVanilla() {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(Items.BOWL, 1), ItemStack.EMPTY,
            new ItemStack(Items.STICK, 1)));
        TagValueOutput oursOut = newOut();
        new CompoundWriter(oursOut).putItems(items);
        CompoundTag ours = oursOut.buildResult();

        TagValueOutput vanillaOut = newOut();
        var list = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
        list.set(0, items.get(0));
        list.set(2, items.get(2));
        ContainerHelper.saveAllItems(vanillaOut, list);
        CompoundTag vanilla = vanillaOut.buildResult();
        assertEquals(vanilla.getListOrEmpty("Items").toString(), ours.getListOrEmpty("Items").toString());
        assertEquals(vanilla.toString(), ours.toString());

        // 旧存档没有 Slot：我们的 items() 也把「没有 Slot」如实交出来。
        CompoundTag legacy = new CompoundTag();
        ListTag raw = new ListTag();
        raw.add(ItemStack.CODEC.encodeStart(registries().createSerializationContext(NbtOps.INSTANCE),
            new ItemStack(Items.BOWL)).getOrThrow());
        legacy.put("Items", raw);
        List<StoredItem> stored = newReader(legacy).items();
        assertEquals(1, stored.size());
        assertFalse(stored.get(0).hasSlot());
        assertEquals(Items.BOWL, stored.get(0).stack().getItem());
    }

    /** 空表仍然写 {@code Items} 键（{@code ContainerHelper.saveAllItems} 两参重载就是这样）。 */
    @Test
    void emptyItemListStillWritesTheKey() {
        TagValueOutput out = newOut();
        new CompoundWriter(out).putItems(List.of(ItemStack.EMPTY, ItemStack.EMPTY));
        CompoundTag tag = out.buildResult();
        assertTrue(tag.contains("Items"));
        assertEquals(0, tag.getListOrEmpty("Items").size());
    }

    /** 子节点 / 子节点列表的类型和原版一样：CompoundTag 与 ListTag<CompoundTag>。 */
    @Test
    void childNodesHaveVanillaTypes() {
        TagValueOutput out = newOut();
        CompoundWriter writer = new CompoundWriter(out);
        writer.child("Fuel").putItems(List.of(new ItemStack(Items.COAL)));
        writer.childList("Plates").addChild().putString("Plate", "minecraft:oak_planks");
        CompoundTag tag = out.buildResult();
        assertInstanceOf(CompoundTag.class, tag.get("Fuel"));
        assertInstanceOf(ListTag.class, tag.get("Plates"));
        assertEquals("minecraft:oak_planks",
            tag.getListOrEmpty("Plates").getCompoundOrEmpty(0).getStringOr("Plate", ""));
    }

    /** BeIo 只是薄壳：写出来 / 读回来和直接 new 一个 writer / reader 完全一样。 */
    @Test
    void beIoIsTransparent() {
        ItemStack stack = new ItemStack(Items.CARROT, 1);
        CompoundTag viaBeIo = new CompoundTag();
        BeIo.write(viaBeIo, registries(), w -> {
            w.putInt("A", 1);
            w.putItem("Item", stack);
        });
        TagValueOutput directOut = newOut();
        CompoundWriter writer = new CompoundWriter(directOut);
        writer.putInt("A", 1);
        writer.putItem("Item", stack);
        CompoundTag direct = directOut.buildResult();
        assertEquals(direct.toString(), viaBeIo.toString());
        int[] read = {-1};
        BeIo.read(viaBeIo, registries(), r -> read[0] = r.getInt("A", -1));
        assertEquals(1, read[0]);
    }
}
