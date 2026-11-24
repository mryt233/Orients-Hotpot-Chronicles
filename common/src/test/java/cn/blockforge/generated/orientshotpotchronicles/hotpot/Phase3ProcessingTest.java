// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 0.3.1-alpha Phase 3：小石磨（stone_mill）、开口大木桶（wooden_vat）自定义配方编码、
 * 加工配置热重载与干货泡发/豆腐细化工序数据文件校验。
 * 作者：mryt233
 */
class Phase3ProcessingTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
        HotPotFuel.values();
        Seasonings.all();
        SoupBase.values();
        DrinkKind.values();
        HotPotCooking.cookRevision();
    }

    @Test
    void stoneMillAndWoodenVatCodecsAndMultipliersWork(@TempDir Path dir) throws Exception {
        Config.init(dir, true);
        try {
            Config.reload();
            Config.saveDefaultsIfNeeded();
            assertEquals(1.0F, Config.millTurnsMultiplier(), 1.0E-5F);
            assertEquals(1.0F, Config.vatTimeMultiplier(), 1.0E-5F);
            assertEquals(3, Config.millWaterUses());

            StoneMillRecipe mill = new StoneMillRecipe(
                Ingredient.of(Items.WHEAT), 2, true, Optional.of(Items.BUCKET),
                new ItemStack(Items.MILK_BUCKET), 4, 0xFFF3ECD8);
            WoodenVatRecipe vat = new WoodenVatRecipe(
                WoodenVatRecipe.MODE_COAGULATE, WoodenVatRecipe.FLUID_SOYMILK,
                Optional.of(Ingredient.of(Items.SUGAR)), 1, true, 80,
                new ItemStack(Items.BREAD, 2), 0xFFFAF3DF);

            assertEquals(4, mill.effectiveTurns());
            assertEquals(80, vat.effectiveTicks());

            // 序列化与 StreamCodec 往返
            var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var millJson = StoneMillRecipe.SERIALIZER.codec().encoder().encodeStart(JsonOps.INSTANCE, mill)
                .getOrThrow();
            var millDecodedJson = StoneMillRecipe.SERIALIZER.codec().decoder().parse(JsonOps.INSTANCE, millJson)
                .getOrThrow();
            assertEquals(2, millDecodedJson.ingredientCount());
            assertTrue(millDecodedJson.requiresWater());
            assertEquals(Optional.of(Items.BUCKET), millDecodedJson.container());

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
            try {
                StoneMillRecipe.SERIALIZER.streamCodec().encode(buf, mill);
                StoneMillRecipe millStream = StoneMillRecipe.SERIALIZER.streamCodec().decode(buf);
                assertEquals(4, millStream.turns());
                assertEquals(0xFFF3ECD8, millStream.color());

                WoodenVatRecipe.SERIALIZER.streamCodec().encode(buf, vat);
                WoodenVatRecipe vatStream = WoodenVatRecipe.SERIALIZER.streamCodec().decode(buf);
                assertEquals(WoodenVatRecipe.MODE_COAGULATE, vatStream.mode());
                assertTrue(vatStream.needsLid());
                assertEquals(2, vatStream.result().getCount());
            } finally {
                buf.release();
            }

            // 改倍率并测试越界警告
            Path cfg = dir.resolve("orients_hotpot_chronicles.json");
            JsonObject doc = JsonParser.parseString(Files.readString(cfg)).getAsJsonObject();
            JsonObject proc = doc.getAsJsonObject("processing");
            proc.addProperty("mill_turns_multiplier", 2.0);
            proc.addProperty("vat_time_multiplier", 0.5);
            Files.writeString(cfg, doc.toString());
            assertEquals(0, Config.reload().warnings().size());
            assertEquals(8, mill.effectiveTurns());
            assertEquals(40, vat.effectiveTicks());

            proc.addProperty("mill_water_uses", 99);
            Files.writeString(cfg, doc.toString());
            assertEquals(1, Config.reload().warnings().size());
            assertEquals(3, Config.millWaterUses());
        } finally {
            Path clean = dir.resolve("clean");
            Files.createDirectories(clean);
            Config.init(clean, false);
            Config.reload();
        }
    }

    @Test
    void phase3RecipesTagsAndBlockPropertiesMatchContract() throws Exception {
        assertEquals(List.of(false, true), WoodenVatBlock.HAS_LID.getPossibleValues().stream().sorted().toList());

        List<String> files = List.of(
            "/data/orients_hotpot_chronicles/recipe/stone_mill.json",
            "/data/orients_hotpot_chronicles/recipe/wooden_vat.json",
            "/data/orients_hotpot_chronicles/recipe/wooden_press_lid.json",
            "/data/orients_hotpot_chronicles/recipe/brine_bottle.json",
            "/data/orients_hotpot_chronicles/recipe/dried_tofu_from_stonecutting.json",
            "/data/orients_hotpot_chronicles/recipe/cold_tofu.json",
            "/data/orients_hotpot_chronicles/recipe/stone_mill/soymilk.json",
            "/data/orients_hotpot_chronicles/recipe/stone_mill/crushed_peanuts.json",
            "/data/orients_hotpot_chronicles/recipe/stone_mill/millet_chili.json",
            "/data/orients_hotpot_chronicles/recipe/stone_mill/garlic_paste.json",
            "/data/orients_hotpot_chronicles/recipe/wooden_vat/cold_tofu.json",
            "/data/orients_hotpot_chronicles/tags/item/tofu_coagulants.json"
        );
        for (String path : files) {
            try (var in = Phase3ProcessingTest.class.getResourceAsStream(path)) {
                assertNotNull(in, "缺少 Phase 3 数据文件: " + path);
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(JsonParser.parseString(text).isJsonObject(), "JSON 格式无效: " + path);
            }
        }

        // cold_tofu.json 在完整模式下须条件关闭，关闭 detailed_tofu_processing 时才激活工作台直合
        try (var in = Phase3ProcessingTest.class.getResourceAsStream(
            "/data/orients_hotpot_chronicles/recipe/cold_tofu.json")) {
            JsonObject obj = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject();
            assertEquals("orients_hotpot_chronicles:conditional_crafting", obj.get("type").getAsString());
            assertEquals("mode.detailed_tofu_processing", obj.get("flag").getAsString());
            assertFalse(obj.get("expected").getAsBoolean());
        }
    }

    @Test
    void stoneMillItemParticleRemainsEncodableAfterSourceStackShrinksToEmpty() {
        assertNull(StoneMillBlockEntity.itemParticle(null));
        assertNull(StoneMillBlockEntity.itemParticle(ItemStack.EMPTY));

        ItemStack source = new ItemStack(Items.WHEAT, 2);
        ItemParticleOption option = StoneMillBlockEntity.itemParticle(source);
        assertNotNull(option);

        // 模拟石磨最后一下研磨把原输入栈原地扣空后，Netty 线程才编码粒子网络包
        source.shrink(2);
        assertTrue(source.isEmpty());
        assertFalse(option.getItem().isEmpty());
        assertEquals(1, option.getItem().getCount());

        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
            option, false, 0.5, 0.72, 0.5, 0.16F, 0.06F, 0.16F, 0.04F, 6);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
        try {
            ClientboundLevelParticlesPacket.STREAM_CODEC.encode(buf, packet);
            ClientboundLevelParticlesPacket decoded = ClientboundLevelParticlesPacket.STREAM_CODEC.decode(buf);
            assertInstanceOf(ItemParticleOption.class, decoded.getParticle());
            ItemStack decodedItem = ((ItemParticleOption) decoded.getParticle()).getItem();
            assertEquals(Items.WHEAT, decodedItem.getItem());
            assertEquals(1, decodedItem.getCount());
            assertEquals(ParticleTypes.ITEM, decoded.getParticle().getType());
        } finally {
            buf.release();
        }
    }

    @Test
    void woodenVatPressAnimationMaintainsClearanceAndInterpolatesSmoothly() throws Exception {
        for (int i = 0; i <= 10; i++) {
            float press = i / 10.0F;
            float lidY = WoodenVatBlockEntity.lidBottomY(false, press);
            float liqY = WoodenVatBlockEntity.effectiveLiquidY(0.9F, true, false, press);
            assertTrue(liqY > 3.0F, "press=" + press + " 液面应保持在桶底之上");
            assertEquals(WoodenVatBlockEntity.LID_CLEARANCE, lidY - liqY, 1.0E-4F,
                "press=" + press + " 木压板底面与液面应恒定保留 0.15px 防穿模间距");
        }
        float oldCoplanarPress = (11.2F - 11.63F) / -1.4F;
        float lidCoplanar = WoodenVatBlockEntity.lidBottomY(false, oldCoplanarPress);
        float liqCoplanar = WoodenVatBlockEntity.effectiveLiquidY(0.9F, true, false, oldCoplanarPress);
        assertEquals(WoodenVatBlockEntity.LID_CLEARANCE, lidCoplanar - liqCoplanar, 1.0E-4F);

        float doneLidY = WoodenVatBlockEntity.lidBottomY(true, 1.0F);
        float doneLiqY = WoodenVatBlockEntity.effectiveLiquidY(0.45F, true, true, 1.0F);
        assertTrue(doneLiqY < doneLidY, "压制完成态成品清液不应穿透木压板");

        // 无盖时保持原始充液高度
        assertEquals(11.63F, WoodenVatBlockEntity.effectiveLiquidY(0.9F, false, false, 0.0F), 1.0E-4F);

        // 验证 progressRatio(tickDelta) 帧内线性单调插值
        WoodenVatBlockEntity vat = allocateVat();
        setVatField(vat, "hasLid", true);
        setVatField(vat, "progress", 10);
        setVatField(vat, "totalTicks", 100);
        assertEquals(0.10F, vat.progressRatio(0.0F), 1.0E-6F);
        assertEquals(0.105F, vat.progressRatio(0.5F), 1.0E-6F);
        assertEquals(0.11F, vat.progressRatio(1.0F), 1.0E-6F);
    }

    @Test
    void stoneMillTallShapeAndWoodenVatAutoReceiveContract() throws Exception {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
        StoneMillBlock mill = (StoneMillBlock) unsafe.allocateInstance(StoneMillBlock.class);
        double maxY = mill.getShape(null, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())
            .max(Direction.Axis.Y);
        double colMaxY = mill.getCollisionShape(null, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())
            .max(Direction.Axis.Y);
        assertEquals(22.0 / 16.0, maxY, 1.0E-6, "小石磨 1.5 格高抬架轮廓顶点应达 22 px");
        assertEquals(22.0 / 16.0, colMaxY, 1.0E-6, "小石磨 1.5 格高碰撞箱顶点应达 22 px");

        WoodenVatBlockEntity vat = allocateVat();
        assertTrue(vat.canReceiveSoymilk());
        assertTrue(vat.receiveSoymilkFromMill(0x00F3ECD8));
        assertEquals(WoodenVatRecipe.FLUID_SOYMILK, vat.fluid());
        assertEquals(1, vat.fluidUses());
        assertEquals(0xFFF3ECD8, vat.color());
        assertFalse(vat.canReceiveSoymilk());
        assertFalse(vat.receiveSoymilkFromMill(0x00F3ECD8));

        WoodenVatBlockEntity withLid = allocateVat();
        setVatField(withLid, "hasLid", true);
        assertFalse(withLid.canReceiveSoymilk());

        WoodenVatBlockEntity withOutput = allocateVat();
        setVatField(withOutput, "output", new ItemStack(Items.BREAD));
        assertFalse(withOutput.canReceiveSoymilk());
    }

    private static WoodenVatBlockEntity allocateVat() throws Exception {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
        WoodenVatBlockEntity vat = (WoodenVatBlockEntity) unsafe.allocateInstance(WoodenVatBlockEntity.class);
        setVatField(vat, "fluid", "");
        setVatField(vat, "input", ItemStack.EMPTY);
        setVatField(vat, "output", ItemStack.EMPTY);
        return vat;
    }

    private static void setVatField(Object target, String name, Object val) throws Exception {
        var field = WoodenVatBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, val);
    }
}
