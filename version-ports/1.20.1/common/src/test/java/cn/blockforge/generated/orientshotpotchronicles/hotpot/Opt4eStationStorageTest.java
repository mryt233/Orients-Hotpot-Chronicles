// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 4A 真实蘸料台夹具、新旧优先级及未知 ID 保存（1.20.1 适配）。作者：mryt233 */
class Opt4eStationStorageTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }

    private static CompoundTag fixture(String key) throws Exception {
        try (var in = Opt4eStationStorageTest.class.getResourceAsStream("/opt4/legacy-0.2.2-alpha/fixtures.json")) {
            var all = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return (CompoundTag) new TagParser(new StringReader(all.getAsJsonObject("entries")
                .getAsJsonObject(key).get("snbt").getAsString())).readValue();
        }
    }

    private static String[] read(CompoundTag tag, int[] amounts) {
        String[] ids = new String[9];
        BeIo.read(tag, r -> SauceStationBlockEntity.readKindIds(r, amounts, ids));
        return ids;
    }

    @Test void readsRealOldAndEarlierStationFixtures() throws Exception {
        int[] amounts = new int[9];
        CompoundTag old = fixture("station_kinds_0_8");
        String[] ids = read(old, amounts);
        for (int i = 0; i < 9; i++) assertEquals(SauceIds.legacyId(i), ids[i]);
        CompoundTag ancient = old.copy(); ancient.remove("Kinds");
        int[] portions = new int[9]; Arrays.fill(portions, 1); ancient.putIntArray("Portions", portions);
        ids = read(ancient, amounts);
        for (int i = 0; i < 9; i++) assertEquals(SauceIds.legacyId(i), ids[i]);
    }

    @Test void shortEmptyAndWrongTypeOldKindsKeepLegacyFallback() throws Exception {
        CompoundTag tag = fixture("station_kinds_0_8"); int[] portions = new int[9]; Arrays.fill(portions, 1);
        tag.putIntArray("Portions", portions); tag.putIntArray("Kinds", new int[]{3});
        String[] ids = read(tag, new int[9]); assertEquals(SauceIds.legacyId(3), ids[0]); assertEquals(SauceIds.legacyId(1), ids[1]);
        tag.putIntArray("Kinds", new int[0]); ids = read(tag, new int[9]); assertEquals(SauceIds.legacyId(0), ids[0]);
        tag.putString("Kinds", "wrong"); ids = read(tag, new int[9]); assertEquals(SauceIds.legacyId(8), ids[8]);
    }

    @Test void presentNewKeyNeverFallsBackAndUnknownRoundTrips() throws Exception {
        CompoundTag tag = fixture("station_kinds_0_8");
        BeIo.write(tag, w -> w.putStringList("KindIds", List.of("example:unknown", "")));
        String[] ids = read(tag, new int[9]); assertEquals("example:unknown", ids[0]); assertEquals("", ids[1]); assertEquals("", ids[2]);
        String[] retained = ids; CompoundTag saved = new CompoundTag(); BeIo.write(saved, w -> w.putStringList("KindIds", Arrays.asList(retained)));
        assertArrayEquals(ids, read(saved, new int[9]));
        tag.putString("KindIds", "wrong"); ids = read(tag, new int[9]); assertEquals("", ids[0]);
        BeIo.write(tag, w -> w.putStringList("KindIds", List.of())); ids = read(tag, new int[9]); assertEquals("", ids[0]);
    }

    @Test void unknownWithRawButZeroPortionsSurvivesUnrelatedChangesAndReload() {
        String unknown = "example:unknown";
        assertEquals(-2, SauceStationBlockEntity.slotIndex(unknown, 4, -1, true));
        assertEquals(-1, SauceStationBlockEntity.slotIndex(unknown, 0, -1, true));
        String kept = SauceStationBlockEntity.updatedKindId(unknown, 0, true, true, false, "minecraft:other");
        assertEquals(unknown, kept);
        CompoundTag saved = new CompoundTag();
        BeIo.write(saved, w -> w.putStringList("KindIds", List.of(kept)));
        assertEquals(unknown, read(saved, new int[9])[0]);
        assertEquals(unknown, SauceStationBlockEntity.updatedKindId(unknown, 0, false, false, false, ""));
        assertEquals("minecraft:other", SauceStationBlockEntity.updatedKindId(unknown, 0, false, true, false, "minecraft:other"));
        assertEquals("minecraft:other", SauceStationBlockEntity.updatedKindId("minecraft:known", 3, true, true, true, "minecraft:other"));
        assertEquals("minecraft:known", SauceStationBlockEntity.updatedKindId("minecraft:known", 3, true, false, true, ""));
        assertEquals(-1, SauceStationBlockEntity.slotIndex("minecraft:known", 4, -1, false));
    }

    @Test void dissolvedKnownPortionsSurviveReloadMappingLossAndRemapAcrossUnrelatedChanges() {
        String aId = SauceIds.fullId(Seasoning.MSG.def());
        String bId = SauceIds.fullId(Seasoning.VINEGAR.def());
        assertEquals(aId, SauceStationBlockEntity.updatedKindId(aId, 3, true, true, true, "", true));
        assertEquals(aId, SauceStationBlockEntity.updatedKindId(aId, 3, true, true, true, "", false));
        assertEquals(aId, SauceStationBlockEntity.updatedKindId(aId, 3, true, true, true, bId, true));
        assertEquals(bId, SauceStationBlockEntity.updatedKindId(aId, 0, true, true, true, bId, true));
        assertEquals("", SauceStationBlockEntity.updatedKindId(aId, 0, true, true, true, "", true));

        NonNullList<ItemStack> lostItems = NonNullList.withSize(9, ItemStack.EMPTY);
        lostItems.set(0, new ItemStack(Items.STICK, 2));
        CompoundTag saved = new CompoundTag();
        BeIo.write(saved, w -> {
            w.putItems(lostItems);
            w.putIntArray("Portions", new int[] { 3, 0, 0, 0, 0, 0, 0, 0, 0 });
            w.putStringList("KindIds", List.of(aId, "", "", "", "", "", "", "", ""));
        });
        SauceStationBlockEntity reloaded =
            new SauceStationBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        reloaded.load(saved);
        assertEquals(Seasoning.MSG.def().id(), reloaded.kindDef(0).id());
        assertEquals(Seasonings.indexOf(Seasoning.MSG.def()), reloaded.kindIndex(0));
        reloaded.rawInventory().setItem(1, new ItemStack(Items.APPLE, 1));
        assertEquals(Seasoning.MSG.def().id(), reloaded.kindDef(0).id());
        CompoundTag afterOtherChange = new CompoundTag();
        reloaded.saveAdditional(afterOtherChange);
        String[] savedIds = read(afterOtherChange, new int[9]);
        assertEquals(aId, savedIds[0]);
        assertEquals(bId, savedIds[1]);

        NonNullList<ItemStack> remappedItems = NonNullList.withSize(9, ItemStack.EMPTY);
        remappedItems.set(0, new ItemStack(Items.APPLE, 2));
        CompoundTag remappedRaw = new CompoundTag();
        BeIo.write(remappedRaw, w -> {
            w.putItems(remappedItems);
            w.putIntArray("Portions", new int[] { 3, 0, 0, 0, 0, 0, 0, 0, 0 });
            w.putStringList("KindIds", List.of(aId, "", "", "", "", "", "", "", ""));
        });
        SauceStationBlockEntity remapBe =
            new SauceStationBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        remapBe.load(remappedRaw);
        assertEquals(Seasoning.MSG.def().id(), remapBe.kindDef(0).id());
        remapBe.rawInventory().setItem(2, new ItemStack(Items.SUGAR, 1));
        assertEquals(Seasoning.MSG.def().id(), remapBe.kindDef(0).id());
        CompoundTag savedRemap = new CompoundTag();
        remapBe.saveAdditional(savedRemap);
        assertEquals(aId, read(savedRemap, new int[9])[0]);
    }

    @Test void screenHandlerClientAndServerMatchDissolvedPortionsAcrossReload() {
        String msgId = SauceIds.fullId(Seasoning.MSG.def());
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        items.set(0, new ItemStack(Items.APPLE, 2));
        CompoundTag tag = new CompoundTag();
        BeIo.write(tag, w -> {
            w.putItems(items);
            w.putIntArray("Portions", new int[] { 3, 0, 0, 0, 0, 0, 0, 0, 0 });
            w.putStringList("KindIds", List.of(msgId, "", "", "", "", "", "", "", ""));
        });
        SauceStationBlockEntity be =
            new SauceStationBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        be.load(tag);

        var serverMenu = new SauceStationScreenHandler(1, new net.minecraft.world.entity.player.Inventory(null), be);
        assertEquals(Seasoning.MSG.def().id(), serverMenu.kindDef(0).id());
        assertTrue(serverMenu.getSlot(0).mayPlace(new ItemStack(Items.WHEAT)));
        assertFalse(serverMenu.getSlot(0).mayPlace(new ItemStack(Items.APPLE)));

        var clientMenu = new SauceStationScreenHandler(2, new net.minecraft.world.entity.player.Inventory(null), BlockPos.ZERO);
        clientMenu.setItem(0, 0, new ItemStack(Items.APPLE, 2));
        clientMenu.setData(0, 3);
        clientMenu.setData(SauceStationBlockEntity.BOWLS, Seasonings.indexOf(Seasoning.MSG.def()) + 1);
        assertEquals(Seasoning.MSG.def().id(), clientMenu.kindDef(0).id());
        assertTrue(clientMenu.getSlot(0).mayPlace(new ItemStack(Items.WHEAT)));
        assertFalse(clientMenu.getSlot(0).mayPlace(new ItemStack(Items.APPLE)));

        clientMenu.setData(0, 0);
        assertEquals(Seasoning.VINEGAR.def().id(), clientMenu.kindDef(0).id());
    }
}
