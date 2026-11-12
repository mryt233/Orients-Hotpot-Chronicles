// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 4A 真实蘸料台夹具、新旧优先级及未知 ID 保存。作者：mryt233 */
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
        BeIo.read(tag, null, r -> SauceStationBlockEntity.readKindIds(r, amounts, ids));
        return ids;
    }
    @Test void readsRealOldAndEarlierStationFixtures() throws Exception {
        int[] amounts = new int[9];
        CompoundTag old = fixture("station_kinds_0_8");
        String[] ids = read(old, amounts);
        for (int i = 0; i < 9; i++) assertEquals(SauceIds.legacyId(i), ids[i]);
        CompoundTag ancient = old.copy(); ancient.remove("Kinds");
        int[] portions = new int[9];Arrays.fill(portions, 1); ancient.putIntArray("Portions", portions);
        ids = read(ancient, amounts);
        for (int i = 0; i < 9; i++) assertEquals(SauceIds.legacyId(i), ids[i]);
    }
    @Test void shortEmptyAndWrongTypeOldKindsKeepLegacyFallback() throws Exception {
        CompoundTag tag = fixture("station_kinds_0_8");int[] portions = new int[9];Arrays.fill(portions, 1);
        tag.putIntArray("Portions", portions);tag.putIntArray("Kinds", new int[]{3});
        String[] ids = read(tag, new int[9]);assertEquals(SauceIds.legacyId(3), ids[0]);assertEquals(SauceIds.legacyId(1), ids[1]);
        tag.putIntArray("Kinds", new int[0]);ids=read(tag,new int[9]);assertEquals(SauceIds.legacyId(0),ids[0]);
        tag.putString("Kinds", "wrong");ids=read(tag,new int[9]);assertEquals(SauceIds.legacyId(8),ids[8]);
    }
    @Test void presentNewKeyNeverFallsBackAndUnknownRoundTrips() throws Exception {
        CompoundTag tag=fixture("station_kinds_0_8");
        BeIo.write(tag,null,w -> w.putStringList("KindIds",List.of("example:unknown", "")));
        String[] ids=read(tag,new int[9]);assertEquals("example:unknown",ids[0]);assertEquals("",ids[1]);assertEquals("",ids[2]);
        String[] retained=ids; CompoundTag saved=new CompoundTag();BeIo.write(saved,null,w -> w.putStringList("KindIds",Arrays.asList(retained)));
        assertArrayEquals(ids,read(saved,new int[9]));
        tag.putString("KindIds","wrong");ids=read(tag,new int[9]);assertEquals("",ids[0]);
        BeIo.write(tag,null,w -> w.putStringList("KindIds",List.of()));ids=read(tag,new int[9]);assertEquals("",ids[0]);
    }
    @Test void unknownWithRawButZeroPortionsSurvivesUnrelatedChangesAndReload() {
        String unknown = "example:unknown";
        assertEquals(-2, SauceStationBlockEntity.slotIndex(unknown, 4, -1, true));
        assertEquals(-1, SauceStationBlockEntity.slotIndex(unknown, 0, -1, true));
        String kept = SauceStationBlockEntity.updatedKindId(unknown, 0, true, true, false, "minecraft:other");
        assertEquals(unknown, kept);
        CompoundTag saved = new CompoundTag();
        BeIo.write(saved, null, w -> w.putStringList("KindIds", List.of(kept)));
        assertEquals(unknown, read(saved, new int[9])[0]);
        assertEquals(unknown, SauceStationBlockEntity.updatedKindId(unknown, 0, false, false, false, ""));
        assertEquals("minecraft:other", SauceStationBlockEntity.updatedKindId(unknown, 0, false, true, false, "minecraft:other"));
        assertEquals("minecraft:other", SauceStationBlockEntity.updatedKindId("minecraft:known", 3, true, true, true, "minecraft:other"));
        assertEquals("minecraft:known", SauceStationBlockEntity.updatedKindId("minecraft:known", 3, true, false, true, ""));
        assertEquals(-1, SauceStationBlockEntity.slotIndex("minecraft:known", 4, -1, false));
    }
}
