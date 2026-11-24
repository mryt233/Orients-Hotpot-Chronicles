// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

/**
 * 0.3.1-alpha Phase 2：独立橙子树、9 种独立作物（直接以同名食材播种）、
 * 打草 0.02 稀疏掉率、mode.crops_replace_grass 联动及作物配置回归测试。
 * 作者：mryt233
 */
class Phase2BotanicalTest {
    private static final List<String> CROPS = List.of(
        "scallion", "cilantro", "soybean", "garlic", "tomato",
        "chili_pepper", "peanut", "greens", "zhe_er_gen");

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
    void cropsConfigValidationAndGarnishDefaultsArePointZeroTwo(@TempDir Path dir) throws Exception {
        Config.init(dir, true);
        try {
            Config.reload();
            Config.saveDefaultsIfNeeded();
            assertEquals(1.0F, Config.cropsGrowthRate(), 1.0E-5F);
            assertTrue(Config.cropsBonemealEnabled());
            for (String crop : CROPS) {
                assertEquals(0.02F, Config.lootGarnishChance(crop, GarnishLoot.DEFAULT_CHANCE), 1.0E-6F,
                    "loot.garnish." + crop + ".chance 默认应已下调至 0.02");
            }
            // mode.crops_replace_grass 与未改动默认 0.02 联动
            assertEquals(0.02F, GarnishLoot.effectiveChance("scallion", 0.02F, 0.25F, true), 1.0E-6F);
            assertEquals(0.25F, GarnishLoot.effectiveChance("scallion", 0.02F, 0.25F, false), 1.0E-6F);
            assertEquals(0.15F, GarnishLoot.effectiveChance("tomato", 0.02F, 0.15F, false), 1.0E-6F);
            // 玩家显式改过掉率时即使关了 crops_replace_grass 也保留自定义值
            assertEquals(0.45F, GarnishLoot.effectiveChance("scallion", 0.45F, 0.25F, false), 1.0E-6F);

            // 非法 crops 配置会触发警告并回退默认值
            Path cfg = dir.resolve("orients_hotpot_chronicles.json");
            JsonObject doc = JsonParser.parseString(Files.readString(cfg)).getAsJsonObject();
            JsonObject crops = doc.getAsJsonObject("crops");
            crops.addProperty("growth_rate", 99.0);
            crops.addProperty("bonemeal_enabled", "bad_bool");
            Files.writeString(cfg, doc.toString());

            var reloaded = Config.reload();
            assertEquals(2, reloaded.warnings().size());
            assertEquals(1.0F, Config.cropsGrowthRate(), 1.0E-5F);
            assertTrue(Config.cropsBonemealEnabled());
        } finally {
            Path clean = dir.resolve("clean");
            Files.createDirectories(clean);
            Config.init(clean, false);
            Config.reload();
        }
    }

    @Test
    void botanicalBlockStatesAndPropertiesAreValid() {
        assertEquals(3, OrangeLeavesBlock.MAX_STAGE);
        assertEquals(List.of(0, 1, 2, 3), OrangeLeavesBlock.STAGE.getPossibleValues().stream().sorted().toList());
        assertEquals(HotPot.id("orange_tree"), OrangeSaplingBlock.ORANGE_TREE.location());
        assertEquals(7, CropBlock.MAX_AGE);
        assertNotNull(RotatedPillarBlock.AXIS);
        assertNotNull(WildCropBlock.CODEC);
    }

    @Test
    void datapackWorldgenLootTablesAndModifiersExistAndParse() throws Exception {
        Path res = Path.of("src", "main", "resources").toAbsolutePath().normalize();
        if (!Files.isDirectory(res)) {
            res = Path.of("common", "src", "main", "resources").toAbsolutePath().normalize();
        }
        Path data = res.resolve("data/orients_hotpot_chronicles");

        // 橙子树世界生成 & 橙叶战利品表
        assertTrue(Files.isRegularFile(data.resolve("worldgen/configured_feature/orange_tree.json")));
        assertTrue(Files.isRegularFile(data.resolve("worldgen/placed_feature/orange_tree_checked.json")));
        assertTrue(Files.isRegularFile(data.resolve("tags/worldgen/biome/has_orange_tree.json")));

        JsonObject leavesLoot = JsonParser.parseString(
            Files.readString(data.resolve("loot_table/blocks/orange_leaves.json"))).getAsJsonObject();
        assertEquals(4, leavesLoot.getAsJsonArray("pools").size());

        // 9 种独立作物战利品（1 基础 + mature extra uniform 1..3 = 2..4）& 野生植株特征
        for (String crop : CROPS) {
            JsonObject cropLoot = JsonParser.parseString(
                Files.readString(data.resolve("loot_table/blocks/" + crop + "_crop.json"))).getAsJsonObject();
            JsonArray pools = cropLoot.getAsJsonArray("pools");
            assertEquals(2, pools.size(), crop + " 作物战利品应包含基础 1 颗与成熟追加 1～3 颗两池");

            assertTrue(Files.isRegularFile(data.resolve("loot_table/blocks/wild_" + crop + ".json")));
            assertTrue(Files.isRegularFile(data.resolve("worldgen/configured_feature/wild_" + crop + ".json")));
            assertTrue(Files.isRegularFile(data.resolve("worldgen/placed_feature/wild_" + crop + ".json")));
        }

        // NeoForge biome modifiers 同级工程核对
        Path modRoot = res.getParent().getParent().getParent().getParent();
        Path neoRes = modRoot.resolve("neoforge/src/main/resources/data/orients_hotpot_chronicles/neoforge/biome_modifier");
        assertTrue(Files.isDirectory(neoRes), "NeoForge biome_modifier 目录应存在: " + neoRes);
        assertTrue(Files.isRegularFile(neoRes.resolve("add_orange_tree.json")));
        assertTrue(Files.isRegularFile(neoRes.resolve("add_wild_temperate_crops.json")));
        assertTrue(Files.isRegularFile(neoRes.resolve("add_wild_warm_crops.json")));
        assertTrue(Files.isRegularFile(neoRes.resolve("add_wild_humid_crops.json")));
    }

    @Test
    void orangeLeavesArePassableWithOneFifthCobwebSlowdown() throws Exception {
        assertEquals(0.85D, OrangeLeavesBlock.SLOWDOWN_MULTIPLIER.x, 1.0E-6D);
        assertEquals(0.81D, OrangeLeavesBlock.SLOWDOWN_MULTIPLIER.y, 1.0E-6D);
        assertEquals(0.85D, OrangeLeavesBlock.SLOWDOWN_MULTIPLIER.z, 1.0E-6D);

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        OrangeLeavesBlock leaves = (OrangeLeavesBlock) unsafe.allocateInstance(OrangeLeavesBlock.class);
        BlockState state = (BlockState) unsafe.allocateInstance(BlockState.class);
        Field ownerField = StateHolder.class.getDeclaredField("owner");
        ownerField.setAccessible(true);
        ownerField.set(state, leaves);

        assertEquals(Shapes.block().bounds(), leaves.getShape(state, null, BlockPos.ZERO, CollisionContext.empty()).bounds());
        assertTrue(leaves.getCollisionShape(state, null, BlockPos.ZERO, CollisionContext.empty()).isEmpty());
        assertTrue(leaves.getVisualShape(state, null, BlockPos.ZERO, CollisionContext.empty()).isEmpty());
    }

    @Test
    void orangeTreeFeatureFoliagePlacerAndLeavesGraphicsContract() throws Exception {
        Path res = Path.of("src", "main", "resources").toAbsolutePath().normalize();
        if (!Files.isDirectory(res)) {
            res = Path.of("common", "src", "main", "resources").toAbsolutePath().normalize();
        }
        Path modRoot = res.getParent().getParent().getParent().getParent();

        // 1. 世界生成配置：底部露出 1 格树干 + 3×3×2(+1) OrangeFoliagePlacer
        JsonObject tree = JsonParser.parseString(
            Files.readString(res.resolve("data/orients_hotpot_chronicles/worldgen/configured_feature/orange_tree.json")))
            .getAsJsonObject();
        JsonObject config = tree.getAsJsonObject("config");
        JsonObject trunk = config.getAsJsonObject("trunk_placer");
        assertEquals("minecraft:straight_trunk_placer", trunk.get("type").getAsString());
        assertEquals(2, trunk.get("base_height").getAsInt());
        assertEquals(0, trunk.get("height_rand_a").getAsInt());
        assertEquals(0, trunk.get("height_rand_b").getAsInt());

        JsonObject foliage = config.getAsJsonObject("foliage_placer");
        assertEquals("orients_hotpot_chronicles:orange_foliage_placer", foliage.get("type").getAsString());
        assertEquals(1, foliage.get("radius").getAsInt());
        assertEquals(0, foliage.get("offset").getAsInt());
        assertEquals(2, foliage.get("height").getAsInt());

        // 2. OrangeFoliagePlacer CODEC 往返与层间裁剪契约
        OrangeFoliagePlacer decoded = OrangeFoliagePlacer.CODEC.decoder().parse(JsonOps.INSTANCE, foliage).getOrThrow();
        assertEquals(2, decoded.heightValue());
        assertEquals(0.65F, decoded.topLeafChance(), 1.0E-5F);
        assertEquals(0.45F, decoded.upperCornerSkipChance(), 1.0E-5F);

        RandomSource rand = RandomSource.create(12345L);
        OrangeFoliagePlacer keepCorners = new OrangeFoliagePlacer(ConstantInt.of(1), ConstantInt.of(0), 2, 1.0F, 0.0F);
        OrangeFoliagePlacer alwaysTrimUpperCorners = new OrangeFoliagePlacer(ConstantInt.of(1), ConstantInt.of(0), 2, 1.0F, 1.0F);
        // 底层 (-1) 与顶层单叶 (range=0) 永不跳过
        assertFalse(alwaysTrimUpperCorners.shouldSkipLocation(rand, 1, -1, 1, 1, false));
        assertFalse(alwaysTrimUpperCorners.shouldSkipLocation(rand, 0, 1, 0, 0, false));
        // 上层 (localY=0) 仅对四角按概率裁剪，边心不跳过
        assertTrue(alwaysTrimUpperCorners.shouldSkipLocation(rand, 1, 0, 1, 1, false));
        assertFalse(alwaysTrimUpperCorners.shouldSkipLocation(rand, 1, 0, 0, 1, false));
        assertFalse(keepCorners.shouldSkipLocation(rand, 1, 0, 1, 1, false));
        // type 注册常量可读（即使尚未经平台加载器绑定也确保符号存在）
        FoliagePlacerType<OrangeFoliagePlacer> unusedRef = HotPotContent.ORANGE_FOLIAGE_PLACER;
        assertNull(unusedRef);

        // 3. 树叶原版标签与性能开关（不能再固定加进 CUTOUT_BLOCKS，需交由 LeavesBlock 自动切换 Fancy/Fast）
        JsonObject mcLeavesTag = JsonParser.parseString(
            Files.readString(res.resolve("data/minecraft/tags/block/leaves.json"))).getAsJsonObject();
        assertTrue(mcLeavesTag.getAsJsonArray("values").toString().contains("orients_hotpot_chronicles:orange_leaves"));
        String mcCropsTag = Files.readString(res.resolve("data/minecraft/tags/block/crops.json"));
        assertTrue(mcCropsTag.contains("orients_hotpot_chronicles:scallion_crop"));
        assertTrue(mcCropsTag.contains("orients_hotpot_chronicles:zhe_er_gen_crop"));
        String mcSaplingsTag = Files.readString(res.resolve("data/minecraft/tags/block/saplings.json"));
        assertTrue(mcSaplingsTag.contains("orients_hotpot_chronicles:orange_sapling"));
        String mcFlowersTag = Files.readString(res.resolve("data/minecraft/tags/block/small_flowers.json"));
        assertTrue(mcFlowersTag.contains("orients_hotpot_chronicles:wild_scallion"));
        assertTrue(mcFlowersTag.contains("orients_hotpot_chronicles:wild_zhe_er_gen"));
        String contentJava = Files.readString(modRoot.resolve(
            "common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot/HotPotContent.java"));
        assertFalse(contentJava.contains("CUTOUT_BLOCKS.add(ORANGE_LEAVES)"),
            "ORANGE_LEAVES 不得写死 cutout 层，否则将无视原版流畅/高品质画质性能开关");

        // 4. mryt233 改（2026-11-22）：光影树苗模板优先借用 AZALEA（10700）以随风摆动，且 REI 插件导入真实 DisplayCategory 包。
        String irisCompatJava = Files.readString(modRoot.resolve(
            "common/src/client/java/cn/blockforge/generated/orientshotpotchronicles/client/IrisShaderCompat.java"));
        int saplingIdx = irisCompatJava.indexOf("HotPotContent.ORANGE_SAPLING");
        assertTrue(saplingIdx >= 0);
        int azaleaIdx = irisCompatJava.indexOf("Blocks.AZALEA", saplingIdx);
        int oakIdx = irisCompatJava.indexOf("Blocks.OAK_SAPLING", saplingIdx);
        assertTrue(azaleaIdx > saplingIdx && oakIdx > azaleaIdx,
            "ORANGE_SAPLING 应优先映射 Blocks.AZALEA（其次 Blocks.OAK_SAPLING），保证光影下与杜鹃一样随风轻晃");

        String reiPluginJava = Files.readString(modRoot.resolve(
            "common/src/client/java/cn/blockforge/generated/orientshotpotchronicles/client/compat/rei/HotPotReiClientPlugin.java"));
        assertTrue(reiPluginJava.contains("import me.shedaniel.rei.api.client.registry.display.DisplayCategory;"),
            "HotPotReiClientPlugin 必须导入真实 REI 16 的 registry.display.DisplayCategory 包");
        assertFalse(reiPluginJava.contains("registry.category.DisplayCategory"),
            "HotPotReiClientPlugin 不得残留错误的 registry.category.DisplayCategory 导入");
    }
}
