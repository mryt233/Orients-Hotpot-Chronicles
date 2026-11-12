// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 4A 真实 SNBT 翻译、新键优先及未知 ID 回归。作者：mryt233 */
class Opt4dSoupIdsTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }
    private static String read(CompoundTag tag, int side) {
        String[] result = {null};
        BeIo.read(tag, null, r -> result[0] = SoupIds.read(r, side));
        return result[0];
    }
    private static CompoundTag fixture(String key) throws Exception {
        try (var in = Opt4dSoupIdsTest.class.getResourceAsStream("/opt4/legacy-0.2.2-alpha/fixtures.json")) {
            JsonObject all = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return (CompoundTag) new TagParser(new StringReader(all.getAsJsonObject("entries")
                .getAsJsonObject(key).get("snbt").getAsString())).readValue();
        }
    }
    @Test void fixedLegacyTableAndRealPotFixtures() throws Exception {
        String[] names = {"clear", "rich_stock", "mushroom", "spicy", "tomato", "three_fresh"};
        String[] entries = {"pot_clear", "pot_rich_stock", "pot_mushroom", "pot_spicy_l3", "pot_tomato", "pot_three_fresh"};
        for (int i = 0; i < names.length; i++) {
            String expected = "orients_hotpot_chronicles:" + names[i];
            assertEquals(expected, SoupIds.legacy(i));
            assertEquals(expected, read(fixture(entries[i]), 0));
            assertEquals(expected, SoupIds.fromPotTag(fixture(entries[i]), 0));
        }
        assertEquals("", SoupIds.legacy(-1)); assertEquals("", SoupIds.legacy(6));
        CompoundTag pot = fixture("chest_packed_pots.a6").getCompound("components")
            .getCompound("orients_hotpot_chronicles:pot_contents");
        assertEquals(SoupIds.legacy(3), read(pot, 0)); assertEquals(3, pot.getInt("Spicy"));
    }
    @Test void newKeyWinsEvenWhenMalformedOrEmpty() {
        CompoundTag tag = new CompoundTag(); tag.putInt("Base", 3);
        assertEquals(SoupIds.legacy(3), read(tag, 0));
        tag.putInt("Soup", 123);
        assertEquals("", read(tag, 0)); assertEquals("", SoupIds.fromPotTag(tag, 0));
        tag.putString("Soup", ""); assertEquals("", read(tag, 0));
        tag.putString("Soup", "example:future");
        assertEquals("example:future", read(tag, 0)); assertEquals("example:future", SoupIds.fromPotTag(tag, 0));
    }
    @Test void storageReadsOldWritesNewAndClampsSpice() throws Exception {
        HotPotBlockEntity pot = new HotPotBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        CompoundTag old = fixture("pot_four_all"); old.putInt("SpicyB", 99);
        HotPotStorage.read(pot, old, null);
        assertEquals(SoupIds.legacy(0), pot.bases[0]); assertEquals(SoupIds.legacy(3), pot.bases[1]);
        assertEquals(3, pot.spicyLevels[1]);
        pot.bases[2] = "example:future";
        CompoundTag saved = old.copy(); HotPotStorage.write(pot, saved, null);
        for (int i = 0; i < 4; i++) {
            assertFalse(saved.contains(SoupIds.oldKey(i)));
            assertEquals(pot.bases[i], saved.getString(SoupIds.key(i)));
        }
        HotPotStorage.read(pot, saved, null);
        assertEquals("example:future", pot.bases[2]);
        assertEquals(SoupIds.UNKNOWN, pot.propertyAt(HotPotBlockEntity.PROP_EXTRA_BASES + 1));
    }
    @Test void unknownCannotBeReplacedByAnIndex() {
        HotPotBlockEntity pot = new HotPotBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        pot.bases[0] = "example:missing";
        assertTrue(pot.hasSoup());
        assertEquals(SoupIds.UNKNOWN, pot.propertyAt(0));
        assertThrows(IllegalArgumentException.class, () -> pot.setBaseOfSide(0, SoupIds.UNKNOWN));
        assertEquals("example:missing", pot.bases[0]);
    }
    @Test void componentDedupRemovesNewAndOldKeys() {
        HotPotBlockEntity pot = new HotPotBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < 4; i++) { tag.putString(SoupIds.key(i), "example:unknown"); tag.putInt(SoupIds.oldKey(i), i); }
        tag.putInt("Spicy", 2); tag.putString("custom", "kept");
        pot.removeComponentsFromTag(tag);
        for (int i = 0; i < 4; i++) { assertFalse(tag.contains(SoupIds.key(i))); assertFalse(tag.contains(SoupIds.oldKey(i))); }
        assertFalse(tag.contains("Spicy")); assertEquals("kept", tag.getString("custom"));
    }
    @Test void cookedComponentUsesShortNamesOnlyForBuiltins() {
        for (SoupBase base : SoupBase.values()) assertEquals(base.textureKey(), SoupIds.cookedKey(base.def().id()));
        assertEquals("orients_hotpot_chronicles:future", SoupIds.cookedKey(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(HotPot.MOD_ID, "future")));
        assertEquals("another_mod:future", SoupIds.cookedKey(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("another_mod", "future")));
    }
    @Test void writesStringsAndPreservesUnknownAndSentinels() {
        CompoundTag tag = new CompoundTag();
        BeIo.write(tag, null, w -> SoupIds.write(w, 0, "example:future"));
        assertEquals("example:future", read(tag, 0));
        assertEquals(SoupIds.EMPTY, SoupIds.index(""));
        assertEquals(SoupIds.UNKNOWN, SoupIds.index("example:future"));
        assertNotEquals(SoupIds.EMPTY, SoupIds.UNKNOWN);
        assertEquals("", read(tag, 1));
    }
}
