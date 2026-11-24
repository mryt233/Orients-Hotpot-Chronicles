// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

/**
 * 0.3.1-alpha Phase 4：统一桌架动态换材质规则、{@link TableMaterialData} 编解码与预设标志、
 * 方块 ID 安全回退、挖掘手感折算、桌子/备菜架准星分格判定及旧 40 桌映射/迁移防御测试。
 * 作者：mryt233
 */
class Phase4FurnitureTest {
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
    void acceptsWithTagsEnforcesModesBlockPropertiesAndDenyPriority(@TempDir Path dir) throws Exception {
        BlockState oak = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState bricks = Blocks.BRICKS.defaultBlockState();
        BlockState chest = Blocks.CHEST.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        assertEquals(Items.AIR, Blocks.WATER.asItem());

        // off 模式全拒（即使命中白名单或常规实心方块）
        assertFalse(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_OFF, false, false));
        assertFalse(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_OFF, true, false));
        assertFalse(TableMaterials.acceptsWithTags(stone, Config.TABLE_MAT_OFF, true, false));
        assertFalse(TableMaterials.acceptsWithTags(air, Config.TABLE_MAT_OFF, true, false));
        assertFalse(TableMaterials.acceptsWithTags(null, Config.TABLE_MAT_OFF, true, false));

        // tag_only 模式：仅放行 taggedAllow=true、非黑名单、非自带方块、且具备物品形态的方块
        assertFalse(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_TAG_ONLY, false, false));
        assertFalse(TableMaterials.acceptsWithTags(stone, Config.TABLE_MAT_TAG_ONLY, false, false));
        assertFalse(TableMaterials.acceptsWithTags(bricks, Config.TABLE_MAT_TAG_ONLY, false, false));
        assertTrue(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_TAG_ONLY, true, false));
        assertTrue(TableMaterials.acceptsWithTags(glass, Config.TABLE_MAT_TAG_ONLY, true, false));
        assertTrue(TableMaterials.acceptsWithTags(glowstone, Config.TABLE_MAT_TAG_ONLY, true, false));
        assertFalse(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_TAG_ONLY, true, true), "deny 应优先于 allow");
        assertFalse(TableMaterials.acceptsWithTags(water, Config.TABLE_MAT_TAG_ONLY, true, false), "无物品形态方块不得放行");
        assertFalse(TableMaterials.acceptsWithTags(air, Config.TABLE_MAT_TAG_ONLY, true, false));
        assertFalse(TableMaterials.acceptsWithTags(null, Config.TABLE_MAT_TAG_ONLY, true, false));

        // any_solid 模式：放行常规实心不透明方块，拒绝容器/透明/发光/不可破坏/空气/黑名单
        assertTrue(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_ANY_SOLID, false, false));
        assertTrue(TableMaterials.acceptsWithTags(quartz, Config.TABLE_MAT_ANY_SOLID, false, false));
        assertTrue(TableMaterials.acceptsWithTags(stone, Config.TABLE_MAT_ANY_SOLID, false, false));
        assertTrue(TableMaterials.acceptsWithTags(bricks, Config.TABLE_MAT_ANY_SOLID, false, false));

        assertFalse(TableMaterials.acceptsWithTags(chest, Config.TABLE_MAT_ANY_SOLID, false, false), "EntityBlock 应被拒绝");
        assertFalse(TableMaterials.acceptsWithTags(glass, Config.TABLE_MAT_ANY_SOLID, false, false), "非实心不透明方块应被拒绝");
        assertFalse(TableMaterials.acceptsWithTags(glowstone, Config.TABLE_MAT_ANY_SOLID, false, false), "发光方块应被拒绝");
        assertFalse(TableMaterials.acceptsWithTags(bedrock, Config.TABLE_MAT_ANY_SOLID, false, false), "不可破坏方块应被拒绝");
        assertFalse(TableMaterials.acceptsWithTags(air, Config.TABLE_MAT_ANY_SOLID, false, false));
        assertFalse(TableMaterials.acceptsWithTags(null, Config.TABLE_MAT_ANY_SOLID, false, false));
        assertFalse(TableMaterials.acceptsWithTags(water, Config.TABLE_MAT_ANY_SOLID, false, false));
        assertFalse(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_ANY_SOLID, false, true));
        assertFalse(TableMaterials.acceptsWithTags(oak, Config.TABLE_MAT_ANY_SOLID, true, true), "any_solid 下 deny 仍优先于 allow");
        assertTrue(TableMaterials.acceptsWithTags(glass, Config.TABLE_MAT_ANY_SOLID, true, false), "白名单可豁免特殊方块");

        // 本模组自身方块排除判定（兼容纯单测未注册静态字段与完整注册表环境）
        assertFalse(TableMaterials.isExcludedOwnBlock(Blocks.OAK_PLANKS));
        assertFalse(TableMaterials.isExcludedOwnBlock(Blocks.STONE));
        assertFalse(TableMaterials.isExcludedOwnBlock(Blocks.BRICKS));

        for (Block maybeRegistered : new Block[]{
            HotPotContent.HOTPOT_TABLE, HotPotContent.TALL_HOTPOT_TABLE,
            HotPotContent.MULTI_TIER_PREP_RACK, HotPotContent.DESKTOP_PREP_RACK, HotPotContent.HANGING_PREP_RACK
        }) {
            if (maybeRegistered != null) {
                assertTrue(TableMaterials.isExcludedOwnBlock(maybeRegistered));
                assertFalse(TableMaterials.acceptsWithTags(maybeRegistered.defaultBlockState(), Config.TABLE_MAT_ANY_SOLID, true, false));
                assertFalse(TableMaterials.acceptsWithTags(maybeRegistered.defaultBlockState(), Config.TABLE_MAT_TAG_ONLY, true, false));
            }
        }

        // 1.21.1 BuiltInRegistries.BLOCK 在 Bootstrap 后已冻结，绕过构造器分配典型自身方块并验证排除逻辑
        List<Block> ownBlocks = List.of(
            allocate(HotPotTableBlock.class),
            allocate(TallHotPotTableBlock.class),
            allocate(LegacyTableBlock.class),
            allocate(LegacyTallTableBlock.class),
            allocate(PrepRackBlock.class),
            allocate(StoveBlock.class),
            allocate(HotPotBlock.class),
            allocate(BenchBlock.class),
            allocate(StoneMillBlock.class),
            allocate(WoodenVatBlock.class),
            allocate(OrangeLeavesBlock.class),
            allocate(WildCropBlock.class)
        );
        for (Block own : ownBlocks) {
            assertTrue(TableMaterials.isExcludedOwnBlock(own), own.getClass().getSimpleName() + " 应被排除");
            BlockState ownState = stateFor(own);
            assertFalse(TableMaterials.acceptsWithTags(ownState, Config.TABLE_MAT_ANY_SOLID, true, false));
            assertFalse(TableMaterials.acceptsWithTags(ownState, Config.TABLE_MAT_TAG_ONLY, true, false));
        }

        // 真实 Block 标签绑定 + Config.tableMaterialsMode() 联动
        BuiltInRegistries.BLOCK.bindTags(Map.of(
            TableMaterials.ALLOW_TAG, List.of(Blocks.GLASS.builtInRegistryHolder(), Blocks.OAK_PLANKS.builtInRegistryHolder()),
            TableMaterials.DENY_TAG, List.of(Blocks.OAK_PLANKS.builtInRegistryHolder())
        ));
        Config.init(dir, true);
        try {
            Config.saveDefaultsIfNeeded();
            Config.reload();
            assertTrue(TableMaterials.accepts(stone));
            assertTrue(TableMaterials.accepts(glass), "ALLOW_TAG 命中应放行玻璃");
            assertFalse(TableMaterials.accepts(oak), "DENY_TAG 应压过 ALLOW_TAG");

            assertTrue(TableMaterials.accepts(glass, Config.TABLE_MAT_TAG_ONLY));
            assertFalse(TableMaterials.accepts(stone, Config.TABLE_MAT_TAG_ONLY));
            assertFalse(TableMaterials.accepts(oak, Config.TABLE_MAT_TAG_ONLY));
            assertFalse(TableMaterials.accepts(glass, Config.TABLE_MAT_OFF));

            Path cfgFile = dir.resolve("orients_hotpot_chronicles.json");
            JsonObject tagOnlyCfg = Config.defaultsCopy();
            tagOnlyCfg.addProperty("_format", 1);
            tagOnlyCfg.getAsJsonObject("table_materials").addProperty("mode", Config.TABLE_MAT_TAG_ONLY);
            Files.writeString(cfgFile, tagOnlyCfg.toString());
            assertEquals(0, Config.reload().warnings().size());
            assertTrue(TableMaterials.accepts(glass));
            assertFalse(TableMaterials.accepts(stone));

            JsonObject offCfg = Config.defaultsCopy();
            offCfg.addProperty("_format", 1);
            offCfg.getAsJsonObject("table_materials").addProperty("mode", Config.TABLE_MAT_OFF);
            Files.writeString(cfgFile, offCfg.toString());
            assertEquals(0, Config.reload().warnings().size());
            assertFalse(TableMaterials.accepts(glass));
            assertFalse(TableMaterials.accepts(stone));
        } finally {
            BuiltInRegistries.BLOCK.resetTags();
            resetConfig(dir);
        }
    }

    @Test
    void tableMaterialDataLegacyPresetsFlagsAndCodecsRoundTrip() {
        assertFalse(TableMaterialData.EMPTY.hasAny());
        TableMaterialData trimmed = new TableMaterialData(" minecraft:stone ", null, "  ", false, false);
        assertEquals("minecraft:stone", trimmed.top());
        assertEquals("", trimmed.base());
        assertEquals("", trimmed.legacyRing());
        assertTrue(trimmed.hasAny());

        // 木桌预设：矮桌与高桌
        TableMaterials.Preset oakLowPreset = TableMaterials.legacyPresets().get("oak_hotpot_table");
        TableMaterials.Preset oakTallPreset = TableMaterials.legacyPresets().get("oak_tall_hotpot_table");
        assertNotNull(oakLowPreset);
        assertNotNull(oakTallPreset);

        TableMaterialData oakLow = TableMaterialData.fromLegacyPreset(oakLowPreset, false);
        assertEquals("minecraft:oak_planks", oakLow.top());
        assertEquals("minecraft:oak_planks", oakLow.base());
        assertEquals("", oakLow.legacyRing());
        assertTrue(oakLow.topLegacy());
        assertTrue(oakLow.baseLegacy());

        TableMaterialData oakTall = TableMaterialData.fromLegacyPreset(oakTallPreset, true);
        assertEquals("minecraft:oak_planks", oakTall.top());
        assertEquals("minecraft:bricks", oakTall.base());
        assertEquals("minecraft:oak_planks", oakTall.legacyRing());
        assertTrue(oakTall.topLegacy());
        assertTrue(oakTall.baseLegacy());

        // 石桌预设：矮桌与高桌
        TableMaterials.Preset quartzLowPreset = TableMaterials.legacyPresets().get("quartz_hotpot_table");
        TableMaterials.Preset quartzTallPreset = TableMaterials.legacyPresets().get("quartz_tall_hotpot_table");
        assertNotNull(quartzLowPreset);
        assertNotNull(quartzTallPreset);

        TableMaterialData quartzLow = TableMaterialData.fromLegacyPreset(quartzLowPreset, false);
        assertEquals("minecraft:quartz_block", quartzLow.top());
        assertEquals("minecraft:bricks", quartzLow.base());
        assertEquals("", quartzLow.legacyRing());
        assertTrue(quartzLow.topLegacy());
        assertTrue(quartzLow.baseLegacy());

        TableMaterialData quartzTall = TableMaterialData.fromLegacyPreset(quartzTallPreset, true);
        assertEquals("minecraft:quartz_block", quartzTall.top());
        assertEquals("minecraft:bricks", quartzTall.base());
        assertEquals("minecraft:quartz_block", quartzTall.legacyRing());
        assertTrue(quartzTall.topLegacy());
        assertTrue(quartzTall.baseLegacy());

        // withTop / withBase 标志位与 legacyRing 更新校验
        TableMaterialData swappedTop = quartzTall.withTop("minecraft:spruce_planks", false);
        assertEquals("minecraft:spruce_planks", swappedTop.top());
        assertEquals("minecraft:bricks", swappedTop.base());
        assertEquals("minecraft:quartz_block", swappedTop.legacyRing(), "仅换桌面应保留高桌迁移中圈");
        assertFalse(swappedTop.topLegacy());
        assertTrue(swappedTop.baseLegacy());

        TableMaterialData swappedBase = swappedTop.withBase("minecraft:stone_bricks", false);
        assertEquals("minecraft:spruce_planks", swappedBase.top());
        assertEquals("minecraft:stone_bricks", swappedBase.base());
        assertEquals("", swappedBase.legacyRing(), "换底座后应清空 legacyRing 避免重影");
        assertFalse(swappedBase.topLegacy());
        assertFalse(swappedBase.baseLegacy());

        TableMaterialData reLockedTop = oakLow.withTop("minecraft:birch_planks", true);
        assertTrue(reLockedTop.topLegacy());
        assertTrue(reLockedTop.baseLegacy());

        // Codec 与 StreamCodec 往返（含高桌遗留环、第三方未知 ID、空数据与缺省字段）
        List<TableMaterialData> samples = List.of(
            TableMaterialData.EMPTY,
            oakLow,
            quartzTall,
            swappedBase,
            new TableMaterialData("thirdparty_mod:jade_planks", "thirdparty_mod:slate_bricks", "", false, true)
        );
        for (TableMaterialData sample : samples) {
            var encodedJson = TableMaterialData.CODEC.encodeStart(JsonOps.INSTANCE, sample).getOrThrow();
            var decodedJson = TableMaterialData.CODEC.parse(JsonOps.INSTANCE, encodedJson).getOrThrow();
            assertEquals(sample, decodedJson);

            ByteBuf buf = Unpooled.buffer();
            try {
                TableMaterialData.STREAM_CODEC.encode(buf, sample);
                TableMaterialData decodedWire = TableMaterialData.STREAM_CODEC.decode(buf);
                assertEquals(sample, decodedWire);
            } finally {
                buf.release();
            }
        }

        TableMaterialData fromEmptyJson = TableMaterialData.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals(TableMaterialData.EMPTY, fromEmptyJson);
    }

    @Test
    void resolveBlockAndDisplayStateFallbackSafelyOnInvalidIds() {
        assertSame(Blocks.OAK_PLANKS, TableMaterials.resolveBlock("minecraft:oak_planks"));
        assertSame(Blocks.BRICKS, TableMaterials.resolveBlock("  minecraft:bricks  "));
        assertNull(TableMaterials.resolveBlock(null));
        assertNull(TableMaterials.resolveBlock(""));
        assertNull(TableMaterials.resolveBlock("   "));
        assertNull(TableMaterials.resolveBlock("minecraft:air"));
        assertNull(TableMaterials.resolveBlock("nonexistent:fake_block"));
        assertNull(TableMaterials.resolveBlock("Invalid Block Id!"));

        BlockState fallback = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState stoneFallback = Blocks.STONE.defaultBlockState();
        assertEquals(Blocks.QUARTZ_BLOCK.defaultBlockState(),
            TableMaterials.resolveDisplayState("minecraft:quartz_block", fallback));
        assertEquals(fallback, TableMaterials.resolveDisplayState("nonexistent:fake_block", fallback));
        assertEquals(stoneFallback, TableMaterials.resolveDisplayState("Invalid Id!", stoneFallback));
        assertEquals(fallback, TableMaterials.resolveDisplayState("", fallback));
        assertEquals(fallback, TableMaterials.resolveDisplayState(null, fallback));
        assertEquals(fallback, TableMaterials.resolveDisplayState("minecraft:air", fallback));

        assertEquals("", TableMaterials.encodeBlock(null));
        assertEquals("", TableMaterials.encodeBlock(Blocks.AIR));
        assertEquals("minecraft:oak_planks", TableMaterials.encodeBlock(Blocks.OAK_PLANKS));

        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), TableMaterials.primaryState(null));
        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), TableMaterials.primaryState(TableMaterialData.EMPTY));
        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(),
            TableMaterials.primaryState(new TableMaterialData("nonexistent:fake_block", "minecraft:stone", "", false, false)));
        assertEquals(Blocks.STONE.defaultBlockState(),
            TableMaterials.primaryState(new TableMaterialData("", "minecraft:stone", "", false, false)));
        assertEquals(Blocks.BRICKS.defaultBlockState(),
            TableMaterials.primaryState(new TableMaterialData("", "nonexistent:fake_block", "", false, false)));
    }

    @Test
    void destroyProgressHandlesWoodStoneAndObsidianSafely() throws Exception {
        BlockState oak = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState bricks = Blocks.BRICKS.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        float bareProgress = 1.0F / 60.0F;

        BuiltInRegistries.BLOCK.bindTags(Map.of(
            BlockTags.MINEABLE_WITH_AXE, List.of(Blocks.OAK_PLANKS.builtInRegistryHolder()),
            BlockTags.MINEABLE_WITH_PICKAXE, List.of(
                Blocks.STONE.builtInRegistryHolder(),
                Blocks.BRICKS.builtInRegistryHolder(),
                Blocks.OBSIDIAN.builtInRegistryHolder()
            )
        ));
        try {
            Player bareHand = StubPlayer.withTool(ItemStack.EMPTY);
            Player ironAxe = StubPlayer.withTool(new ItemStack(Items.IRON_AXE));
            Player ironPickaxe = StubPlayer.withTool(new ItemStack(Items.IRON_PICKAXE));
            Player diamondPickaxe = StubPlayer.withTool(new ItemStack(Items.DIAMOND_PICKAXE));

            // 空手可拆任何材质（包括黑曜石与空占位），绝不出现 0 或极慢锁死
            assertEquals(bareProgress, TableMaterials.destroyProgress(oak, bareHand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);
            assertEquals(bareProgress, TableMaterials.destroyProgress(stone, bareHand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);
            assertEquals(bareProgress, TableMaterials.destroyProgress(obsidian, bareHand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);
            assertEquals(bareProgress, TableMaterials.destroyProgress(null, bareHand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);
            assertEquals(bareProgress, TableMaterials.destroyProgress(Blocks.AIR.defaultBlockState(), bareHand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);

            // 铁斧对木头材质加速，对石头/黑曜石仍保底 > 0
            float axeOnOak = TableMaterials.destroyProgress(oak, ironAxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            float axeOnStone = TableMaterials.destroyProgress(stone, ironAxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assertEquals(6.0F / 60.0F, axeOnOak, 1e-6F);
            assertEquals(bareProgress, axeOnStone, 1e-6F);
            assertTrue(axeOnOak > axeOnStone);

            // 铁镐/钻石镐对石头、砖块与黑曜石材质加速，而木头维持保底速度
            float pickOnOak = TableMaterials.destroyProgress(oak, ironPickaxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            float pickOnStone = TableMaterials.destroyProgress(stone, ironPickaxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            float pickOnBricks = TableMaterials.destroyProgress(bricks, ironPickaxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            float pickOnObsidian = TableMaterials.destroyProgress(obsidian, ironPickaxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            float diamondOnObsidian = TableMaterials.destroyProgress(obsidian, diamondPickaxe, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

            assertEquals(bareProgress, pickOnOak, 1e-6F);
            assertEquals(6.0F / 60.0F, pickOnStone, 1e-6F);
            assertEquals(6.0F / 60.0F, pickOnBricks, 1e-6F);
            assertEquals(6.0F / 60.0F, pickOnObsidian, 1e-6F);
            assertEquals(8.0F / 60.0F, diamondOnObsidian, 1e-6F);
            assertTrue(diamondOnObsidian > pickOnObsidian && pickOnObsidian > bareProgress);

            // 即使异常情况下工具速度 <= 0，也由 Math.max(1.0F, speed) 钳位保证大于 0
            Player zeroSpeedPlayer = StubPlayer.withFixedSpeed(0.0F);
            Player negativeSpeedPlayer = StubPlayer.withFixedSpeed(-5.0F);
            assertEquals(bareProgress, TableMaterials.destroyProgress(obsidian, zeroSpeedPlayer, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);
            assertEquals(bareProgress, TableMaterials.destroyProgress(oak, negativeSpeedPlayer, EmptyBlockGetter.INSTANCE, BlockPos.ZERO), 1e-6F);
        } finally {
            BuiltInRegistries.BLOCK.resetTags();
        }
    }

    @Test
    void tableAndTallTableSlotAtMatchExactPixelBoundaries() throws Exception {
        HotPotTableBlock lowTable = allocate(HotPotTableBlock.class);
        TallHotPotTableBlock tallTable = allocate(TallHotPotTableBlock.class);

        for (BlockPos pos : new BlockPos[]{BlockPos.ZERO, new BlockPos(-12, 64, 35)}) {
            double baseY = pos.getY();
            // 矮桌 7/16 分界（带 1e-4 容差）
            double lowSplit = 7.0 / 16.0;
            assertEquals(TableMaterials.Slot.BASE, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 0.0, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.BASE, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 0.25, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.BASE, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 6.9 / 16.0, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.BASE, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + lowSplit - 2.0e-4, pos.getZ() + 0.5)));

            assertEquals(TableMaterials.Slot.TOP, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + lowSplit - 0.5e-4, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.TOP, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + lowSplit, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.TOP, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 9.0 / 16.0, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.TOP, lowTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 11.0 / 16.0, pos.getZ() + 0.5)));

            // 高桌 19/16 分界（带 1e-4 容差，1.0 整格顶仍属底座）
            double tallSplit = 19.0 / 16.0;
            assertEquals(TableMaterials.Slot.BASE, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 0.5, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.BASE, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 1.0, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.BASE, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 18.9 / 16.0, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.BASE, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + tallSplit - 2.0e-4, pos.getZ() + 0.5)));

            assertEquals(TableMaterials.Slot.TOP, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + tallSplit - 0.5e-4, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.TOP, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + tallSplit, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.TOP, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 21.0 / 16.0, pos.getZ() + 0.5)));
            assertEquals(TableMaterials.Slot.TOP, tallTable.slotAt(pos, new Vec3(pos.getX() + 0.5, baseY + 23.0 / 16.0, pos.getZ() + 0.5)));
        }
    }

    @Test
    void prepRackSlotAtCoversAllKindsAndAllFourFacings() throws Exception {
        assertEquals(6, PrepRackBlock.Kind.MULTI_TIER.slots());
        assertEquals(4, PrepRackBlock.Kind.DESKTOP.slots());
        assertEquals(4, PrepRackBlock.Kind.HANGING.slots());
        assertEquals(List.of("wall", "ceiling"),
            java.util.Arrays.stream(PrepRackBlock.Mount.values()).map(PrepRackBlock.Mount::getSerializedName).toList());
        assertNotNull(PrepRackBlock.FACING);
        assertNotNull(PrepRackBlock.MOUNT);

        PrepRackBlock multiTier = allocateRack(PrepRackBlock.Kind.MULTI_TIER);
        PrepRackBlock desktop = allocateRack(PrepRackBlock.Kind.DESKTOP);
        PrepRackBlock hanging = allocateRack(PrepRackBlock.Kind.HANGING);
        BlockPos pos = new BlockPos(10, 72, -5);

        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
            BlockState facingState = Blocks.FURNACE.defaultBlockState().setValue(PrepRackBlock.FACING, facing);

            // MULTI_TIER: 3 层 (6.5/16, 12.5/16) × 2 列 (0.5) -> 6 格 (0..5)
            assertEquals(0, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.25, 3.0 / 16.0, 0.5), null));
            assertEquals(1, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.75, 3.0 / 16.0, 0.5), null));
            assertEquals(2, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.25, 6.5 / 16.0, 0.5), null));
            assertEquals(3, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.75, 9.0 / 16.0, 0.5), null));
            assertEquals(4, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.25, 12.5 / 16.0, 0.5), null));
            assertEquals(5, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.75, 14.0 / 16.0, 0.5), null));
            assertEquals(1, multiTier.slotAt(facingState, pos, rackHit(pos, facing, 0.5, 6.49 / 16.0, 0.5), null));

            // DESKTOP: 前下 0/1 (ly < 5.5/16 且 lz >= 0.5)，上层或后排 2/3 -> 4 格 (0..3)
            assertEquals(0, desktop.slotAt(facingState, pos, rackHit(pos, facing, 0.25, 2.0 / 16.0, 0.75), null));
            assertEquals(1, desktop.slotAt(facingState, pos, rackHit(pos, facing, 0.75, 5.49 / 16.0, 0.5), null));
            assertEquals(2, desktop.slotAt(facingState, pos, rackHit(pos, facing, 0.25, 5.5 / 16.0, 0.75), null));
            assertEquals(3, desktop.slotAt(facingState, pos, rackHit(pos, facing, 0.75, 8.0 / 16.0, 0.75), null));
            assertEquals(2, desktop.slotAt(facingState, pos, rackHit(pos, facing, 0.25, 2.0 / 16.0, 0.49), null));
            assertEquals(3, desktop.slotAt(facingState, pos, rackHit(pos, facing, 0.75, 2.0 / 16.0, 0.25), null));

            // HANGING: 横向 4 等分 + 越界夹紧 -> 4 格 (0..3)
            assertEquals(0, hanging.slotAt(facingState, pos, rackHit(pos, facing, 0.125, 0.7, 0.5), null));
            assertEquals(1, hanging.slotAt(facingState, pos, rackHit(pos, facing, 0.375, 0.7, 0.5), null));
            assertEquals(2, hanging.slotAt(facingState, pos, rackHit(pos, facing, 0.625, 0.7, 0.5), null));
            assertEquals(3, hanging.slotAt(facingState, pos, rackHit(pos, facing, 0.875, 0.7, 0.5), null));
            assertEquals(0, hanging.slotAt(facingState, pos, rackHit(pos, facing, -0.25, 0.7, 0.5), null));
            assertEquals(3, hanging.slotAt(facingState, pos, rackHit(pos, facing, 1.25, 0.7, 0.5), null));
        }
    }

    @Test
    void legacyPresetsCoverAllFortyTablesAndTableMigrationIsDefensive() throws Exception {
        Map<String, TableMaterials.Preset> presets = TableMaterials.legacyPresets();
        assertEquals(40, presets.size(), "旧版 11 种木桌×2 + 9 种石/金属桌×2 应共 40 个预设");

        for (String wood : HotPotContent.TABLE_WOODS) {
            String planks = "minecraft:" + wood + "_planks";
            TableMaterials.Preset low = presets.get(wood + "_hotpot_table");
            TableMaterials.Preset tall = presets.get(wood + "_tall_hotpot_table");
            assertNotNull(low, "缺少木矮桌预设: " + wood);
            assertNotNull(tall, "缺少木高桌预设: " + wood);
            assertEquals(planks, low.topId());
            assertEquals(planks, low.lowBaseId());
            assertEquals("", low.tallRingId());
            assertFalse(low.hardSurface());

            assertEquals(planks, tall.topId());
            assertEquals(TableMaterials.DEFAULT_BASE_ID, tall.lowBaseId());
            assertEquals(planks, tall.tallRingId());
            assertEquals(TableMaterials.DEFAULT_BASE_ID, tall.tallWallId());
            assertFalse(tall.hardSurface());
        }

        for (String[] stone : HotPotContent.STONE_TABLES) {
            String blockId = "minecraft:" + stone[1];
            TableMaterials.Preset low = presets.get(stone[0] + "_hotpot_table");
            TableMaterials.Preset tall = presets.get(stone[0] + "_tall_hotpot_table");
            assertNotNull(low, "缺少石矮桌预设: " + stone[0]);
            assertNotNull(tall, "缺少石高桌预设: " + stone[0]);
            assertEquals(blockId, low.topId());
            assertEquals(TableMaterials.DEFAULT_BASE_ID, low.lowBaseId());
            assertEquals("", low.tallRingId());
            assertTrue(low.hardSurface());

            assertEquals(blockId, tall.topId());
            assertEquals(TableMaterials.DEFAULT_BASE_ID, tall.lowBaseId());
            assertEquals(blockId, tall.tallRingId());
            assertEquals(TableMaterials.DEFAULT_BASE_ID, tall.tallWallId());
            assertTrue(tall.hardSurface());
        }

        for (TableMaterials.Preset preset : presets.values()) {
            assertNotNull(TableMaterials.resolveBlock(preset.topId()), "无效 topId: " + preset.key());
            assertNotNull(TableMaterials.resolveBlock(preset.lowBaseId()), "无效 lowBaseId: " + preset.key());
            assertNotNull(TableMaterials.resolveBlock(preset.tallWallId()), "无效 tallWallId: " + preset.key());
            if (!preset.tallRingId().isEmpty()) {
                assertNotNull(TableMaterials.resolveBlock(preset.tallRingId()), "无效 tallRingId: " + preset.key());
            }
            boolean isTall = preset.key().endsWith("_tall_hotpot_table");
            TableMaterialData data = TableMaterialData.fromLegacyPreset(preset, isTall);
            assertTrue(data.topLegacy() && data.baseLegacy());
            assertEquals(isTall ? preset.tallRingId() : "", data.legacyRing());
        }

        assertFalse(TableMaterials.isLegacyTable(null));
        assertFalse(TableMaterials.isLegacyTable(Blocks.OAK_PLANKS.defaultBlockState()));
        assertTrue(TableMaterials.isLegacyTable(stateFor(allocate(LegacyTableBlock.class))));
        assertTrue(TableMaterials.isLegacyTable(stateFor(allocate(LegacyTallTableBlock.class))));
        assertNull(TableMaterials.legacyPreset(null));
        assertNull(TableMaterials.legacyPreset(Blocks.OAK_PLANKS));

        // 迁移工具对空/非旧桌物品与空世界入参均应安全无异常
        assertNull(TableMigration.convertItemStack(null));
        assertSame(ItemStack.EMPTY, TableMigration.convertItemStack(ItemStack.EMPTY));
        ItemStack apple = new ItemStack(Items.APPLE, 3);
        assertSame(apple, TableMigration.convertItemStack(apple));
        ItemStack oakPlanks = new ItemStack(Items.OAK_PLANKS, 5);
        assertSame(oakPlanks, TableMigration.convertItemStack(oakPlanks));

        if (HotPotContent.HOTPOT_TABLE_ITEM != null
            && HotPotContent.TALL_HOTPOT_TABLE_ITEM != null
            && HotPotContent.TABLE_MATERIALS != null
            && !HotPotContent.HOTPOT_TABLE_ITEMS.isEmpty()
            && !HotPotContent.TALL_TABLE_ITEMS.isEmpty()) {
            ItemStack legacyLow = new ItemStack(HotPotContent.HOTPOT_TABLE_ITEMS.get(0), 4);
            ItemStack migratedLow = TableMigration.convertItemStack(legacyLow);
            assertTrue(migratedLow.is(HotPotContent.HOTPOT_TABLE_ITEM));
            assertEquals(4, migratedLow.getCount());
            TableMaterialData lowData = migratedLow.get(HotPotContent.TABLE_MATERIALS);
            assertNotNull(lowData);
            assertTrue(lowData.topLegacy() && lowData.baseLegacy());

            ItemStack legacyTall = new ItemStack(HotPotContent.TALL_TABLE_ITEMS.get(0), 2);
            ItemStack migratedTall = TableMigration.convertItemStack(legacyTall);
            assertTrue(migratedTall.is(HotPotContent.TALL_HOTPOT_TABLE_ITEM));
            assertEquals(2, migratedTall.getCount());
            TableMaterialData tallData = migratedTall.get(HotPotContent.TABLE_MATERIALS);
            assertNotNull(tallData);
            assertTrue(tallData.topLegacy() && tallData.baseLegacy());
            assertFalse(tallData.legacyRing().isEmpty());
        }

        assertEquals(0, TableMigration.migrateChunk(null, null));
        assertEquals(0, TableMigration.migrateInventory(null));
        assertFalse(TableMigration.upgradeBlock(null, BlockPos.ZERO, Blocks.AIR.defaultBlockState()));
    }

    @Test
    void allLowTableConnectionModelsPreserveFourHorizontalSidesForTopAndBase() throws Exception {
        String[] prefixes = {
            "/assets/orients_hotpot_chronicles/models/block/hotpot_table",
            "/assets/orients_hotpot_chronicles/models/block/template/hotpot_table",
            "/assets/orients_hotpot_chronicles/models/block/template/stone_hotpot_table"
        };
        String[] horizontal = {"north", "east", "south", "west"};
        for (int mask = 0; mask < 16; mask++) {
            StringBuilder sfx = new StringBuilder();
            if ((mask & 1) != 0) sfx.append('n');
            if ((mask & 2) != 0) sfx.append('e');
            if ((mask & 4) != 0) sfx.append('s');
            if ((mask & 8) != 0) sfx.append('w');
            String suffix = sfx.isEmpty() ? "" : "_" + sfx;
            for (String prefix : prefixes) {
                String path = prefix + suffix + ".json";
                JsonObject model;
                try (InputStream in = Phase4FurnitureTest.class.getResourceAsStream(path)) {
                    assertNotNull(in, "缺少矮桌模型: " + path);
                    model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                }
                var elements = model.getAsJsonArray("elements");
                assertEquals(2, elements.size(), "矮桌应包含桌面与底座两段: " + path);
                JsonObject topFaces = elements.get(0).getAsJsonObject().getAsJsonObject("faces");
                JsonObject baseFaces = elements.get(1).getAsJsonObject().getAsJsonObject("faces");
                for (String dir : horizontal) {
                    assertTrue(topFaces.has(dir), path + " 桌面板缺失侧面: " + dir);
                    assertTrue(baseFaces.has(dir), path + " 底座缺失侧面（转角处会镂空）: " + dir);
                }
                assertTrue(topFaces.has("up") && topFaces.has("down"), path + " 桌面板缺失上下表面");
                assertFalse(baseFaces.has("up"), path + " 底座顶面紧贴桌面不应重复绘制");
                assertTrue(baseFaces.has("down"), path + " 底座缺失底面");
                assertEquals("down", baseFaces.getAsJsonObject("down").get("cullface").getAsString(), path + " 底座底面缺少 cullface");
            }
        }
    }

    private static Vec3 rackHit(BlockPos pos, Direction facing, double lx, double ly, double lz) {
        double rx = lx - 0.5;
        double rz = lz - 0.5;
        double dx;
        double dz;
        switch (facing) {
            case NORTH -> { dx = -rx; dz = -rz; }
            case EAST -> { dx = rz; dz = -rx; }
            case WEST -> { dx = -rz; dz = rx; }
            default -> { dx = rx; dz = rz; }
        }
        return new Vec3(pos.getX() + 0.5 + dx, pos.getY() + ly, pos.getZ() + 0.5 + dz);
    }

    private static PrepRackBlock allocateRack(PrepRackBlock.Kind kind) throws Exception {
        PrepRackBlock rack = allocate(PrepRackBlock.class);
        Field kindField = PrepRackBlock.class.getDeclaredField("kind");
        kindField.setAccessible(true);
        kindField.set(rack, kind);
        return rack;
    }

    private static BlockState stateFor(Block block) throws Exception {
        BlockState state = allocate(BlockState.class);
        Field ownerField = StateHolder.class.getDeclaredField("owner");
        ownerField.setAccessible(true);
        ownerField.set(state, block);
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        return (T) unsafe.allocateInstance(type);
    }

    private static void resetConfig(Path dir) throws Exception {
        Path clean = dir.resolve("clean");
        Files.createDirectories(clean);
        Config.init(clean, false);
        Config.reload();
    }

    private static class StubPlayer extends Player {
        private Inventory testInventory;
        private boolean useFixedSpeed;
        private float fixedSpeed;

        private StubPlayer() {
            super(null, BlockPos.ZERO, 0.0F, null);
        }

        static StubPlayer withTool(ItemStack tool) throws Exception {
            StubPlayer player = allocate(StubPlayer.class);
            player.testInventory = new Inventory(null);
            player.testInventory.setItem(0, tool);
            return player;
        }

        static StubPlayer withFixedSpeed(float speed) throws Exception {
            StubPlayer player = allocate(StubPlayer.class);
            player.useFixedSpeed = true;
            player.fixedSpeed = speed;
            return player;
        }

        @Override
        public float getDestroySpeed(BlockState state) {
            return useFixedSpeed ? fixedSpeed : testInventory.getDestroySpeed(state);
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }
}
