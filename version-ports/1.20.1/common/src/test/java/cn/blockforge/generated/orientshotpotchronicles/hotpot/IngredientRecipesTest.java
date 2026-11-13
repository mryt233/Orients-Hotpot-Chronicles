// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.compat.DataComponents;
import cn.blockforge.orient.framework.compat.ItemData;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 锅煮制与捣制配方单元测试（1.20.1 适配）。 */
class IngredientRecipesTest {
    @BeforeAll static void bootstrap() {
        try { net.minecraft.SharedConstants.getCurrentVersion(); }
        catch (IllegalStateException missing) { Opt3AttributesTest.bootstrapMinecraft(); }
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static PotBoilingRecipe pot(String fields) {
        return PotBoilingRecipe.SERIALIZER.fromJson(
            HotPot.id("test"),
            JsonParser.parseString(
                "{\"ingredient\":{\"item\":\"minecraft:apple\"},\"result\":{\"id\":\"minecraft:baked_potato\"}" + fields + "}"
            ).getAsJsonObject());
    }

    @Test void absentFieldsKeepVanillaFallbacks() {
        var recipe = pot("");
        assertTrue(recipe.cookTicks().isEmpty()); assertTrue(recipe.experience().isEmpty());
        assertTrue(recipe.overcookResult().isEmpty()); assertTrue(recipe.overcookTicks().isEmpty());
        assertTrue(recipe.ingredient().test(new ItemStack(Items.APPLE)));
        assertFalse(recipe.ingredient().test(new ItemStack(Items.CARROT)));
    }

    @Test void explicitFieldsAndZeroExperienceParse() {
        var r = pot(",\"cook_ticks\":45,\"experience\":0,\"overcook_result\":{\"id\":\"minecraft:potato\"},\"overcook_ticks\":90");
        assertEquals(45, r.cookTicks().orElseThrow()); assertEquals(0F, r.experience().orElseThrow());
        assertTrue(r.overcookResult().is(Items.POTATO)); assertEquals(90, r.overcookTicks().orElseThrow());
    }

    @Test void invalidTimesAndExperienceRejected() {
        for (String value : new String[]{",\"cook_ticks\":0", ",\"overcook_ticks\":-1", ",\"experience\":-1", ",\"experience\":101"}) {
            assertThrows(RuntimeException.class, () -> pot(value));
        }
    }

    @Test void returnedStacksNeverAliasRecipeState() {
        var r = pot(""); r.result().setCount(17);
        assertEquals(1, r.result().getCount());
    }

    @Test void poundingDefaultsAndStageComponentsRoundTrip() {
        String json = "{\"ingredient\":{\"item\":\"minecraft:beef\"},\"result\":{\"id\":\"minecraft:slime_ball\",\"count\":3},"
            + "\"stage1\":{\"id\":\"minecraft:beef\",\"components\":{\"minecraft:custom_model_data\":7}}}";
        var r = PoundingRecipe.SERIALIZER.fromJson(HotPot.id("test_pounding"), JsonParser.parseString(json).getAsJsonObject());
        assertEquals(Pounding.HITS, r.hits()); assertEquals(3, r.result().getCount());
        assertEquals(7, ItemData.get(r.display(new ItemStack(Items.BEEF), 1), DataComponents.CUSTOM_MODEL_DATA).value());
        assertTrue(r.display(new ItemStack(Items.BEEF), 2).is(Items.BEEF));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PoundingRecipe.SERIALIZER.toNetwork(buf, r);
            var decoded = PoundingRecipe.SERIALIZER.fromNetwork(HotPot.id("test_pounding"), buf);
            assertEquals(3, decoded.result().getCount());
            assertEquals(7, ItemData.get(decoded.display(new ItemStack(Items.BEEF), 1), DataComponents.CUSTOM_MODEL_DATA).value());
        } finally {
            buf.release();
        }
    }

    @Test void customHitCountAndDefensiveCopies() {
        var source = new ItemStack(Items.CARROT, 2);
        var r = new PoundingRecipe(Ingredient.of(Items.APPLE), source, 5, ItemStack.EMPTY, source);
        source.setCount(9); r.result().setCount(1); r.stage2().setCount(1);
        assertEquals(5, r.hits()); assertEquals(2, r.result().getCount()); assertEquals(2, r.stage2().getCount());
        assertThrows(IllegalArgumentException.class, () -> new PoundingRecipe(Ingredient.of(Items.APPLE), source, 0, source, source));
    }

    @Test void forbiddenTagPrecedesFoodAcceptance() {
        // 旧 Java 产物登记不单独放行非食物，继续由额外可煮钩子声明。
        HotPotCooking.potResult(Items.STICK, Items.APPLE);
        assertFalse(HotPotCooking.acceptable(null, new ItemStack(Items.STICK)));

        var tag = HotPotCooking.NOT_POT_INGREDIENT;
        BuiltInRegistries.ITEM.bindTags(java.util.Map.of(tag, java.util.List.of(Items.APPLE.builtInRegistryHolder())));
        try {
            assertFalse(HotPotCooking.acceptable(null, new ItemStack(Items.APPLE)));
            assertTrue(HotPotCooking.acceptable(null, new ItemStack(Items.CARROT)));
        } finally { BuiltInRegistries.ITEM.resetTags(); }
    }

    @Test void foreignItemsDoNotUseOwnShortConfigKeys() {
        assertEquals(HotPotCooking.DEFAULT_FALLBACK_TICKS, HotPotCooking.cookTicks(null, new ItemStack(Items.BEEF)));
    }

    @Test void configuredTimePrecedesRecipeAndRecipePrecedesVanilla() {
        var recipe = pot(",\"cook_ticks\":45");
        assertEquals(70, HotPotCooking.cookingTime(70, recipe, () -> { fail("不应读原版配方"); return 0; }));
        assertEquals(45, HotPotCooking.cookingTime(null, recipe, () -> { fail("不应读原版配方"); return 0; }));
        assertEquals(100, HotPotCooking.cookingTime(null, pot(""), () -> 100));
        assertEquals(200, HotPotCooking.cookingTime(null, null, () -> 200));
    }

    @Test void bothRecipeTypesRoundTripThroughClientWireCodec() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var boiling = pot(",\"cook_ticks\":45,\"experience\":0,\"overcook_result\":{\"id\":\"minecraft:potato\"},\"overcook_ticks\":90");
            PotBoilingRecipe.SERIALIZER.toNetwork(buffer, boiling);
            var decoded = PotBoilingRecipe.SERIALIZER.fromNetwork(HotPot.id("test_boiling"), buffer);
            assertEquals(boiling.cookTicks(), decoded.cookTicks());
            assertEquals(boiling.experience(), decoded.experience());
            assertEquals(boiling.overcookTicks(), decoded.overcookTicks());
            assertTrue(ItemStack.isSameItemSameTags(boiling.result(), decoded.result()));
            assertTrue(ItemStack.isSameItemSameTags(boiling.overcookResult(), decoded.overcookResult()));
            assertTrue(decoded.ingredient().test(new ItemStack(Items.APPLE)));
            var pounding = new PoundingRecipe(Ingredient.of(Items.BEEF), new ItemStack(Items.CARROT, 3), 5,
                new ItemStack(Items.POTATO), ItemStack.EMPTY);
            PoundingRecipe.SERIALIZER.toNetwork(buffer, pounding);
            var p = PoundingRecipe.SERIALIZER.fromNetwork(HotPot.id("test_pounding"), buffer);
            assertEquals(5, p.hits()); assertEquals(3, p.result().getCount());
            assertTrue(p.stage1().is(Items.POTATO)); assertTrue(p.stage2().isEmpty());
            assertTrue(p.ingredient().test(new ItemStack(Items.BEEF)));
        } finally { buffer.release(); }
    }
}
