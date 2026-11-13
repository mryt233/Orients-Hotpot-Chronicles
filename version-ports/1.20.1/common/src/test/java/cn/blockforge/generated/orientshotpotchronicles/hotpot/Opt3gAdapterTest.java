// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundWriter;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoredItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三批 3G：版本适配小包的两组验收（1.20.1 适配）。
 *
 * <p>第一组把「意图 → 兼容交互常量」的映射钉死：任何一次映射漂移都会在这里报出来。
 * 第二组走读写原语的往返：写出来的标签要和原版写法逐字相同、读回来要和真实存档一致。</p>
 *
 * <p>作者：mryt233</p>
 */
class Opt3gAdapterTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    // ------------------------------------------------------------ 右键意图映射

    @Test
    void blockItemMappingMatchesVanillaConstants() {
        assertSame(ItemInteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS, false));
        assertSame(ItemInteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS_SOFT, true));
        assertSame(ItemInteractionResult.CONSUME, Use.blockItem(UseOutcome.SUCCESS_SOFT, false));
        assertSame(ItemInteractionResult.CONSUME, Use.blockItem(UseOutcome.CONSUME, false));
        assertSame(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, Use.blockItem(UseOutcome.SKIPPED, false));
        assertSame(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
            Use.blockItem(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(ItemInteractionResult.FAIL, Use.blockItem(UseOutcome.FAIL, false));
        assertThrows(IllegalArgumentException.class, () -> Use.blockItem(UseOutcome.PASS, false));
    }

    @Test
    void blockEmptyMappingMatchesVanillaConstants() {
        assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS, false));
        assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, true));
        assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.SUCCESS_SOFT, false));
        assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.CONSUME, false));
        assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS, false));
        assertSame(InteractionResult.FAIL, Use.blockEmpty(UseOutcome.FAIL, false));
    }

    @Test
    void itemMappingMatchesVanillaConstants() {
        assertSame(InteractionResult.SUCCESS, Use.item(UseOutcome.SUCCESS, false));
        assertSame(InteractionResult.SUCCESS, Use.item(UseOutcome.SUCCESS_SOFT, true));
        assertSame(InteractionResult.CONSUME, Use.item(UseOutcome.SUCCESS_SOFT, false));
        assertSame(InteractionResult.CONSUME, Use.item(UseOutcome.CONSUME, false));
        assertSame(InteractionResult.PASS, Use.item(UseOutcome.PASS, false));
        assertSame(InteractionResult.FAIL, Use.item(UseOutcome.FAIL, false));
    }

    /** 物品 {@code use}：{@code InteractionResultHolder} 的三个字段（结果 / 换掉的手持物）都要对。 */
    @Test
    void itemUseKeepsOutcomeAndHeldStack() {
        ItemStack held = new ItemStack(Items.BOWL);
        ItemStack swapped = new ItemStack(Items.WATER_BUCKET);
        InteractionResultHolder<ItemStack> success = Use.itemUse(UseOutcome.SUCCESS, held, false);
        assertSame(InteractionResult.SUCCESS, success.getResult());
        assertSame(held, success.getObject());
        InteractionResultHolder<ItemStack> softClient = Use.itemUse(UseOutcome.SUCCESS_SOFT, held, true);
        assertSame(InteractionResult.SUCCESS, softClient.getResult());
        InteractionResultHolder<ItemStack> softServer = Use.itemUse(UseOutcome.SUCCESS_SOFT, swapped, false);
        assertSame(InteractionResult.CONSUME, softServer.getResult());
        assertSame(swapped, softServer.getObject());
        assertSame(InteractionResult.CONSUME, Use.itemUse(UseOutcome.CONSUME, held, false).getResult());
        assertSame(InteractionResult.PASS, Use.itemUse(UseOutcome.PASS, held, false).getResult());
        assertSame(InteractionResult.FAIL, Use.itemUse(UseOutcome.FAIL, held, false).getResult());
    }

    /** 意图的种类必须覆盖主工程用到的全部常量，少一个就会有调用点没法表达。 */
    @Test
    void intentsCoverEveryConstantUsedInTheProject() {
        List<String> mapped = new ArrayList<>();
        for (UseOutcome outcome : UseOutcome.values()) {
            try {
                mapped.add(Use.blockItem(outcome, false).name());
            } catch (IllegalArgumentException expected) {
                // 方块「拿着东西」侧没有 PASS，属于设计如此。
            }
        }
        for (String name : List.of("SUCCESS", "CONSUME", "SKIP_DEFAULT_BLOCK_INTERACTION",
            "PASS_TO_DEFAULT_BLOCK_INTERACTION", "FAIL")) {
            assertNotNull(ItemInteractionResult.valueOf(name));
            assertTrue(mapped.contains(name), name);
        }
        assertSame(ItemInteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS_SOFT, true));
        assertSame(ItemInteractionResult.CONSUME, Use.blockItem(UseOutcome.SUCCESS_SOFT, false));
        assertTrue(Arrays.stream(UseOutcome.values()).anyMatch(o -> o == UseOutcome.SUCCESS_SOFT));
    }

    /**
     * 语义核实（写进单测防止有人「顺手合并」）：
     * {@code CONSUME} 落到 {@code InteractionResult.CONSUME}（{@code consumesAction()} 为真），
     * {@code SKIP_DEFAULT_BLOCK_INTERACTION} 落到 {@code InteractionResult.PASS}（为假）。
     */
    @Test
    void consumeAndSkipAreNotTheSame() {
        assertTrue(ItemInteractionResult.CONSUME.result().consumesAction());
        assertFalse(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION.result().consumesAction());
        assertSame(InteractionResult.CONSUME, ItemInteractionResult.CONSUME.result());
        assertSame(InteractionResult.PASS, ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION.result());
    }

    // ------------------------------------------------------------ 存读档原语往返

    @Test
    void writerAndReaderRoundTripEveryPrimitive() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag);
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

        CompoundReader reader = new CompoundReader(tag);
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
        CompoundTag ours = new CompoundTag();
        new CompoundWriter(ours).putItems(items);
        CompoundTag vanilla = new CompoundTag();
        var list = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
        list.set(0, items.get(0));
        list.set(2, items.get(2));
        ContainerHelper.saveAllItems(vanilla, list);
        assertEquals(vanilla.getList("Items", 10).toString(), ours.getList("Items", 10).toString());
        assertEquals(vanilla.toString(), ours.toString());

        // 旧存档没有 Slot：原版 loadAllItems 会把它写到槽 0，我们的 items() 也把「没有 Slot」如实交出来。
        CompoundTag legacy = new CompoundTag();
        ListTag raw = new ListTag();
        raw.add(new ItemStack(Items.BOWL).save(new CompoundTag()));
        legacy.put("Items", raw);
        List<StoredItem> stored = new CompoundReader(legacy).items();
        assertEquals(1, stored.size());
        assertFalse(stored.get(0).hasSlot());
        assertEquals(Items.BOWL, stored.get(0).stack().getItem());
    }

    /** 空表仍然写 {@code Items} 键。 */
    @Test
    void emptyItemListStillWritesTheKey() {
        CompoundTag tag = new CompoundTag();
        new CompoundWriter(tag).putItems(List.of(ItemStack.EMPTY, ItemStack.EMPTY));
        assertTrue(tag.contains("Items"));
        assertEquals(0, tag.getList("Items", 10).size());
    }

    /** 子节点 / 子节点列表的类型和原版一样：CompoundTag 与 ListTag<TAG_COMPOUND>。 */
    @Test
    void childNodesHaveVanillaTypes() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag);
        writer.child("Fuel").putItems(List.of(new ItemStack(Items.COAL)));
        writer.childList("Plates").addChild().putString("Plate", "minecraft:oak_planks");
        assertEquals(10, tag.getTagType("Fuel"));
        assertEquals(9, tag.getTagType("Plates"));
        assertEquals(10, tag.getList("Plates", 10).getElementType());
        assertEquals("minecraft:oak_planks",
            tag.getList("Plates", 10).getCompound(0).getString("Plate"));
    }

    /** BeIo 只是薄壳：写出来 / 读回来和直接 new 一个 writer / reader 完全一样。 */
    @Test
    void beIoIsTransparent() {
        ItemStack stack = new ItemStack(Items.CARROT, 1);
        CompoundTag viaBeIo = new CompoundTag();
        BeIo.write(viaBeIo, w -> {
            w.putInt("A", 1);
            w.putItem("Item", stack);
        });
        CompoundTag direct = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(direct);
        writer.putInt("A", 1);
        writer.putItem("Item", stack);
        assertEquals(direct.toString(), viaBeIo.toString());
        int[] read = {-1};
        BeIo.read(viaBeIo, r -> read[0] = r.getInt("A", -1));
        assertEquals(1, read[0]);
    }
}
