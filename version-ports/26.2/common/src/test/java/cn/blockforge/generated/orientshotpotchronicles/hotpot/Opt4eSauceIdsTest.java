// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 4E：蘸料组件 ID 化的新旧编码、规范顺序与成就基数。 */
class Opt4eSauceIdsTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }
    private static final String P = HotPot.MOD_ID + ":";

    private static JsonObject oldSauce(int mask, int bites) {
        JsonObject o = new JsonObject();
        o.addProperty("mask", mask);
        o.addProperty("bites", bites);
        return o;
    }

    @Test
    void oldMaskTranslatesToIdsInBuiltinOrder() {
        SauceContents c = SauceContents.CODEC.parse(JsonOps.INSTANCE, oldSauce(0b1010, 5)).getOrThrow();
        assertEquals(List.of(P + Seasoning.ALL[1].def().id(), P + Seasoning.ALL[3].def().id()), c.ids());
        assertEquals(5, c.bites());
    }

    @Test
    void newFormatRoundTripsAndIsCanonical() {
        SauceContents c = new SauceContents(List.of(P + "sugar", P + "honey", P + "sugar"), 9);
        assertEquals(List.of(P + "honey", P + "sugar"), c.ids());   // 去重 + 内置旧序号排序
        JsonElement json = SauceContents.CODEC.encodeStart(JsonOps.INSTANCE, c).getOrThrow();
        assertTrue(json.getAsJsonObject().has("seasonings"));
        assertFalse(json.getAsJsonObject().has("mask"));
        assertEquals(c, SauceContents.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        // 蘸的先后不同 → 组件相等
        assertEquals(new SauceContents(List.of(P + "honey", P + "sugar"), 9),
            new SauceContents(List.of(P + "sugar", P + "honey"), 9));
    }

    @Test
    void highBitsAreDroppedAndUnknownIdsKeptAfterBuiltins() {
        SauceContents c = SauceContents.CODEC.parse(JsonOps.INSTANCE, oldSauce(32771, 9)).getOrThrow();
        assertEquals(2, c.ids().size());   // 第 15 位以外的高位丢弃
        SauceContents u = new SauceContents(List.of("example:zzz", P + "sugar", "example:aaa"), 1);
        assertEquals(List.of(P + "sugar", "example:aaa", "example:zzz"), u.ids());
    }

    @Test
    void dippedAcceptsOldIntegerAndNewList() {
        assertEquals(SauceIds.fromMask(8), SauceContents.DIPPED_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(8)).getOrThrow());
        assertTrue(SauceContents.DIPPED_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(0)).getOrThrow().isEmpty());
        JsonArray arr = new JsonArray();
        arr.add(P + "sugar");
        arr.add(P + "honey");
        assertEquals(List.of(P + "honey", P + "sugar"), SauceContents.DIPPED_CODEC.parse(JsonOps.INSTANCE, arr).getOrThrow());
        JsonElement out = SauceContents.DIPPED_CODEC.encodeStart(JsonOps.INSTANCE, List.of(P + "sugar", P + "honey")).getOrThrow();
        assertTrue(out.isJsonArray());
    }

    @Test
    void aliasesAreCanonicalAndUnknownIdsSurvive() {
        List<String> ids = SauceIds.normalize(List.of("honey", P + "honey", "example:later", "sugar"));
        assertEquals(List.of(P + "honey", P + "sugar", "example:later"), ids);
        assertTrue(SauceIds.containsDef(ids, Seasoning.HONEY));
        assertEquals(ids, SauceContents.DIPPED_CODEC.parse(JsonOps.INSTANCE,
            SauceContents.DIPPED_CODEC.encodeStart(JsonOps.INSTANCE, ids).getOrThrow()).getOrThrow());
    }

    @Test
    void networkCodecsCanonicalizeAndPreserveUnknown() {
        ByteBuf bytes = Unpooled.buffer();
        SauceContents original = new SauceContents(List.of("sugar", "honey", P + "honey", "example:future"), 4);
        SauceContents.PACKET_CODEC.encode(bytes, original);
        assertEquals(original, SauceContents.PACKET_CODEC.decode(bytes));
        bytes.clear();
        SauceContents.DIPPED_PACKET_CODEC.encode(bytes, List.of("sugar", "honey", P + "honey", "example:future"));
        assertEquals(original.ids(), SauceContents.DIPPED_PACKET_CODEC.decode(bytes));
        bytes.release();
    }

    @Test
    void sauceFullNeedsAllFifteenBuiltins() {
        List<String> all = SauceIds.fromMask((1 << SauceIds.builtinCount()) - 1);
        assertEquals(15, SauceIds.builtinCount());
        assertTrue(SauceIds.hasAllBuiltins(all));
        assertFalse(SauceIds.hasAllBuiltins(all.subList(1, all.size())));
        assertFalse(SauceIds.hasAllBuiltins(all.subList(0, 14)));
    }
    @Test
    void defsResolveCanonicalAliasesOnlyOnce() {
        int[] calls = {0};
        var definitions = SauceIds.defs(List.of("honey", P + "honey"), id -> {
            calls[0]++;
            return Seasoning.HONEY.def();
        });
        assertEquals(1, calls[0]);
        assertEquals(List.of(Seasoning.HONEY.def()), definitions);
    }
}
