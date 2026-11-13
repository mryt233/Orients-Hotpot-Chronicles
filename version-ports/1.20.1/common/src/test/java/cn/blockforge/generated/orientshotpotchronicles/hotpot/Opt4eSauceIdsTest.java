// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.Codec;
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

/** 4E：蘸料组件 ID 化的新旧编码、规范顺序与成就基数（1.20.1 适配）。 */
class Opt4eSauceIdsTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }
    private static final String P = HotPot.MOD_ID + ":";

    private static JsonObject oldSauce(int mask, int bites) {
        JsonObject o = new JsonObject();
        o.addProperty("mask", mask);
        o.addProperty("bites", bites);
        return o;
    }

    private static <T> T decode(Codec<T> codec, JsonElement el) {
        return codec.parse(JsonOps.INSTANCE, el)
            .getOrThrow(false, msg -> { throw new IllegalStateException(msg); });
    }

    private static <T> JsonElement encode(Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value)
            .getOrThrow(false, msg -> { throw new IllegalStateException(msg); });
    }

    @Test
    void oldMaskTranslatesToIdsInBuiltinOrder() {
        SauceContents c = decode(SauceContents.CODEC, oldSauce(0b1010, 5));
        assertEquals(List.of(P + Seasoning.ALL[1].def().id(), P + Seasoning.ALL[3].def().id()), c.ids());
        assertEquals(5, c.bites());
    }

    @Test
    void newFormatRoundTripsAndIsCanonical() {
        SauceContents c = new SauceContents(List.of(P + "sugar", P + "honey", P + "sugar"), 9);
        assertEquals(List.of(P + "honey", P + "sugar"), c.ids());
        JsonElement json = encode(SauceContents.CODEC, c);
        assertTrue(json.getAsJsonObject().has("seasonings"));
        assertFalse(json.getAsJsonObject().has("mask"));
        assertEquals(c, decode(SauceContents.CODEC, json));
        assertEquals(new SauceContents(List.of(P + "honey", P + "sugar"), 9),
            new SauceContents(List.of(P + "sugar", P + "honey"), 9));
    }

    @Test
    void highBitsAreDroppedAndUnknownIdsKeptAfterBuiltins() {
        SauceContents c = decode(SauceContents.CODEC, oldSauce(32771, 9));
        assertEquals(2, c.ids().size());
        SauceContents u = new SauceContents(List.of("example:zzz", P + "sugar", "example:aaa"), 1);
        assertEquals(List.of(P + "sugar", "example:aaa", "example:zzz"), u.ids());
    }

    @Test
    void dippedAcceptsOldIntegerAndNewList() {
        assertEquals(SauceIds.fromMask(8), decode(SauceContents.DIPPED_CODEC, new JsonPrimitive(8)));
        assertTrue(decode(SauceContents.DIPPED_CODEC, new JsonPrimitive(0)).isEmpty());
        JsonArray arr = new JsonArray();
        arr.add(P + "sugar");
        arr.add(P + "honey");
        assertEquals(List.of(P + "honey", P + "sugar"), decode(SauceContents.DIPPED_CODEC, arr));
        JsonElement out = encode(SauceContents.DIPPED_CODEC, List.of(P + "sugar", P + "honey"));
        assertTrue(out.isJsonArray());
    }

    @Test
    void aliasesAreCanonicalAndUnknownIdsSurvive() {
        List<String> ids = SauceIds.normalize(List.of("honey", P + "honey", "example:later", "sugar"));
        assertEquals(List.of(P + "honey", P + "sugar", "example:later"), ids);
        assertTrue(SauceIds.containsDef(ids, Seasoning.HONEY));
        assertEquals(ids, decode(SauceContents.DIPPED_CODEC, encode(SauceContents.DIPPED_CODEC, ids)));
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
