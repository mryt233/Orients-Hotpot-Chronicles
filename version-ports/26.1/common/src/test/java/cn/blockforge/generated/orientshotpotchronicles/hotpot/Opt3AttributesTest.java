// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 3A：属性收拢后的默认值回归。作者：mryt233
 *
 * <p>这里只测<b>不依赖注册平台</b>的属性（汤底 / 饮料 / 锅材质的枚举本身）。
 * {@link HotPotContent} 的真实登记需要平台；提前类初始化已由生命周期回归覆盖；
 * 蘸料 {@link Seasoning} 的静态块要取 {@link HotPotContent} 的物品字段，同样跑不起来。
 * 所以「注册 ID 列表、创造栏顺序、方块 / 物品切实注册出来的属性、调料属性」用开发命令
 * {@code /hpt dumpattrs} 在改动前后各导出一份 JSON 逐项比对（tools/out/opt3_compare.py，结果在报告里）。</p>
 */
class Opt3AttributesTest {

    private static boolean componentsBound;

    /**
     * 26.1 原版 jar 内自带 {@code version.json}，用 {@link net.minecraft.SharedConstants#tryDetectVersion()}
     * 检测后再执行 {@link net.minecraft.server.Bootstrap#bootStrap()}，并绑定内置物品数据组件初始化器。
     */
    @BeforeAll
    static void bootstrapMinecraft() {
        // mryt233 改（2026-11-13）：26.1 适配 tryDetectVersion + DATA_COMPONENT_INITIALIZERS。
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        if (!componentsBound) {
            var registries = net.minecraft.data.registries.VanillaRegistries.createLookup();
            net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(registries)
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
            componentsBound = true;
        }
    }

    // ------------------------------------------------------------ 枚举顺序（存档存 ordinal，一个都不能变）

    @Test
    void soupOrderIsUnchanged() {
        // 汤底：清汤、高汤、菌汤、红汤、番茄、三鲜（存档里 Base 存的就是这个序号）
        assertArrayEquals(new String[]{"CLEAR", "RICH_STOCK", "MUSHROOM", "SPICY", "TOMATO", "THREE_FRESH"},
            java.util.Arrays.stream(SoupBase.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void drinkAndVisualOrderAreUnchanged() {
        assertArrayEquals(new String[]{"ORANGE_DRINK", "PLANT_PROTEIN_DRINK", "ORIENT_COLA", "WATER", "MILK"},
            java.util.Arrays.stream(DrinkKind.values()).map(Enum::name).toArray(String[]::new));
        assertArrayEquals(new String[]{"NONE", "CLEAR", "RICH_STOCK", "MUSHROOM", "SPICY", "TOMATO", "THREE_FRESH", "OTHER"},
            java.util.Arrays.stream(SoupVisual.values()).map(Enum::name).toArray(String[]::new));
    }

    // ------------------------------------------------------------ 汤底属性

    @Test
    void soupBowlIdsMatchBeforeRefactor() {
        assertAll(
            () -> assertEquals("clear_broth_bowl", SoupBase.CLEAR.bowlId()),
            () -> assertEquals("rich_stock_bowl", SoupBase.RICH_STOCK.bowlId()),
            () -> assertEquals("mushroom_broth_bowl", SoupBase.MUSHROOM.bowlId()),
            () -> assertEquals("spicy_red_broth_bowl", SoupBase.SPICY.bowlId()),
            () -> assertEquals("tomato_soup_bowl", SoupBase.TOMATO.bowlId()),
            () -> assertEquals("three_fresh_soup_bowl", SoupBase.THREE_FRESH.bowlId()));
    }

    @Test
    void soupDefaultDurationsMatchBeforeRefactor() {
        // 主效果 / 副效果 / 抗火 / 饥饿的默认时长（tick）——改动前写在一串 switch 里
        int[][] expected = {
            {300, 1, 0, 0},       // CLEAR
            {300, 600, 0, 0},     // RICH_STOCK
            {200, 0, 0, 0},       // MUSHROOM
            {120, 120, 300, 120}, // SPICY
            {300, 0, 0, 0},       // TOMATO
            {400, 600, 0, 0},     // THREE_FRESH
        };
        SoupBase[] all = SoupBase.values();
        for (int i = 0; i < all.length; i++) {
            SoupBase soup = all[i];
            assertEquals(expected[i][0], soup.defaultDuration(), soup + " duration");
            assertEquals(expected[i][1], soup.defaultSecondDuration(), soup + " second");
            assertEquals(expected[i][2], soup.defaultFireDuration(), soup + " fire");
            assertEquals(expected[i][3], soup.defaultHungerDuration(), soup + " hunger");
        }
    }

    @Test
    void soupBooleansMatchBeforeRefactor() {
        // 改动前是「soup == RICH_STOCK || soup == THREE_FRESH ? 0.9 : 0.6」和「soup == CLEAR || soup == TOMATO」
        assertAll(
            () -> assertEquals(0.9F, SoupBase.RICH_STOCK.defaultBowlSaturation()),
            () -> assertEquals(0.9F, SoupBase.THREE_FRESH.defaultBowlSaturation()),
            () -> assertEquals(0.6F, SoupBase.CLEAR.defaultBowlSaturation()),
            () -> assertEquals(0.6F, SoupBase.MUSHROOM.defaultBowlSaturation()),
            () -> assertEquals(0.6F, SoupBase.SPICY.defaultBowlSaturation()),
            () -> assertEquals(0.6F, SoupBase.TOMATO.defaultBowlSaturation()));

        assertTrue(SoupBase.CLEAR.clearsNausea());
        assertTrue(SoupBase.TOMATO.clearsNausea());
        assertFalse(SoupBase.RICH_STOCK.clearsNausea());
        assertFalse(SoupBase.MUSHROOM.clearsNausea());
        assertFalse(SoupBase.SPICY.clearsNausea());
        assertFalse(SoupBase.THREE_FRESH.clearsNausea());

        for (SoupBase soup : SoupBase.values()) {
            assertEquals(soup == SoupBase.SPICY, soup.isSpicy(), soup + " isSpicy");
        }
    }

    @Test
    void soupColorsMatchBeforeRefactor() {
        // 界面配色 / 气泡配色（改动前是 HotPotScreen、BoilingEffects 里两个 switch）
        assertEquals(0xFFB4BCBC, SoupBase.CLEAR.screenBase());
        assertEquals(0xFFE3D7BF, SoupBase.RICH_STOCK.screenBase());
        assertEquals(0xFF7C5836, SoupBase.MUSHROOM.screenBase());
        assertEquals(0xFFB32A1E, SoupBase.SPICY.screenBase());
        assertEquals(0xFFD8482A, SoupBase.TOMATO.screenBase());
        assertEquals(0xFFD8C79A, SoupBase.THREE_FRESH.screenBase());

        assertEquals(0xFFF4F4EC, SoupBase.CLEAR.screenHi());
        assertEquals(0xFFFFFFFF, SoupBase.RICH_STOCK.screenHi());
        assertEquals(0xFFE8C88A, SoupBase.MUSHROOM.screenHi());
        assertEquals(0xFFFFB070, SoupBase.SPICY.screenHi());
        assertEquals(0xFFFF9A6A, SoupBase.TOMATO.screenHi());
        assertEquals(0xFFFFF6DC, SoupBase.THREE_FRESH.screenHi());

        assertEquals(0xFFC6CCCA, SoupBase.CLEAR.screenLo());
        assertEquals(0xFFD94A2E, SoupBase.SPICY.screenLo());
        assertEquals(0xFFE6D8B0, SoupBase.THREE_FRESH.screenLo());

        assertEquals(0xFFC6CCCA, SoupBase.CLEAR.bubbleBody());
        assertEquals(0xFFF2E7D2, SoupBase.RICH_STOCK.bubbleBody());
        assertEquals(0xFF9A7048, SoupBase.MUSHROOM.bubbleBody());
        assertEquals(0xFFBE3325, SoupBase.SPICY.bubbleBody());
        assertEquals(0xFFE2563A, SoupBase.TOMATO.bubbleBody());
        assertEquals(0xFFE6D8B0, SoupBase.THREE_FRESH.bubbleBody());

        assertEquals(0xFFF4F4EC, SoupBase.CLEAR.bubbleHi());
        assertEquals(0xFFD8553A, SoupBase.SPICY.bubbleHi());

        // 食材小点颜色：3 个一轮
        assertArrayEquals(new int[]{0xFF6FAF3A, 0xFF8FCF52, 0xFFD04A2A}, SoupBase.CLEAR.screenBits());
        assertArrayEquals(new int[]{0xFFF2C14A, 0xFFB82818, 0xFF6FAF3A}, SoupBase.TOMATO.screenBits());
    }

    @Test
    void everySoupHasAMatchingSoupVisual() {
        // 启动时的一致性检查（SoupVisual.verifyAgainstSoupBases）反向验证一遍
        for (SoupBase soup : SoupBase.values()) {
            SoupVisual visual = soup.visual();
            assertNotEquals(SoupVisual.NONE, visual, soup + " 没有汤面值");
            assertEquals(soup.textureKey(), visual.getSerializedName(), soup + " 汤面名对不上");
            assertSame(visual, SoupVisual.fromBase(soup));
        }
        assertSame(SoupVisual.NONE, SoupVisual.fromBase(null));
        assertDoesNotThrow(SoupVisual::verifyAgainstSoupBases);
    }

    // ------------------------------------------------------------ 饮料

    @Test
    void drinkBottleHeightsAndBottleFlagsMatchBeforeRefactor() {
        assertEquals(15.0, DrinkKind.ORANGE_DRINK.bottleHeight());
        assertEquals(15.0, DrinkKind.PLANT_PROTEIN_DRINK.bottleHeight());
        assertEquals(11.375, DrinkKind.ORIENT_COLA.bottleHeight());
        assertEquals(15.0, DrinkKind.WATER.bottleHeight());
        assertEquals(15.0, DrinkKind.MILK.bottleHeight());

        assertTrue(DrinkKind.ORANGE_DRINK.hasBottle());
        assertTrue(DrinkKind.PLANT_PROTEIN_DRINK.hasBottle());
        assertTrue(DrinkKind.ORIENT_COLA.hasBottle());
        assertFalse(DrinkKind.WATER.hasBottle());
        assertFalse(DrinkKind.MILK.hasBottle());

        // 方块状态里序列化用的名字不能变（DrinkBlock / CupBlock 的 EnumProperty）
        assertEquals("orange_drink", DrinkKind.ORANGE_DRINK.getSerializedName());
        assertEquals("plant_protein_drink", DrinkKind.PLANT_PROTEIN_DRINK.getSerializedName());
        assertEquals("orient_cola", DrinkKind.ORIENT_COLA.getSerializedName());
        assertEquals("water", DrinkKind.WATER.getSerializedName());
        assertEquals("milk", DrinkKind.MILK.getSerializedName());
    }

    // ------------------------------------------------------------ 锅材质 / 盘子材质

    @Test
    void potMaterialOrderAndPropertiesAreUnchanged() {
        // HotPotMaterial 的枚举顺序（注册 / 兼容代码按它遍历）
        assertArrayEquals(new String[]{
            "COPPER", "IRON", "COPPER_S", "COPPER_O", "IRON_S", "IRON_O",
            "COPPER_SPLIT", "IRON_SPLIT", "COPPER_FOUR", "IRON_FOUR",
            "COPPER_SQUARE", "IRON_SQUARE"},
            java.util.Arrays.stream(HotPotMaterial.values()).map(Enum::name).toArray(String[]::new));

        // ID 路径、硬度、声音、音符盒音色都不能变
        assertEquals("copper_nine_grid_hotpot", HotPotMaterial.COPPER.idPath());
        assertEquals("iron_nine_grid_hotpot", HotPotMaterial.IRON.idPath());
        assertEquals("copper_taiji_yuanyang_hotpot", HotPotMaterial.COPPER_S.idPath());
        assertEquals("copper_round_yuanyang_hotpot", HotPotMaterial.COPPER_O.idPath());
        assertEquals("copper_split_yuanyang_hotpot", HotPotMaterial.COPPER_SPLIT.idPath());
        assertEquals("copper_four_grid_hotpot", HotPotMaterial.COPPER_FOUR.idPath());
        assertEquals("copper_square_yuanyang_hotpot", HotPotMaterial.COPPER_SQUARE.idPath());
        assertEquals(3.0F, HotPotMaterial.COPPER.hardness());
        assertEquals(5.0F, HotPotMaterial.IRON.hardness());
        assertSame(net.minecraft.world.level.block.SoundType.COPPER, HotPotMaterial.COPPER.sounds());
        assertSame(net.minecraft.world.level.block.SoundType.METAL, HotPotMaterial.IRON.sounds());
        assertEquals(PotLayout.NINE_GRID, HotPotMaterial.COPPER.layout());
        assertEquals(PotLayout.YUANYANG_S, HotPotMaterial.COPPER_S.layout());
        assertEquals(PotLayout.FOUR_GRID, HotPotMaterial.IRON_FOUR.layout());
        // 铜系 / 铁系归类和贴图键
        assertEquals(HotPotMaterial.COPPER, HotPotMaterial.COPPER_FOUR.metal());
        assertEquals(HotPotMaterial.IRON, HotPotMaterial.IRON_SQUARE.metal());
        assertEquals("copper", HotPotMaterial.COPPER_S.textureKey());
        assertEquals("iron", HotPotMaterial.IRON_O.textureKey());
    }
    // mryt233 改（2026-11-10）：提前加载只产生元数据，不依赖平台也不提前登记。
    @Test
    void touchingContentBeforeRegistrationDoesNotUseAPlatform() {
        assertFalse(cn.blockforge.orient.framework.loader.Framework.isInstalled());
        assertEquals(12, HotPotContent.POT_ORDER.size());
        assertNull(HotPotContent.STOVE);
        assertNull(HotPotContent.CLOTH_APRON);
        assertNull(HotPotContent.POT_CONTENTS);
        assertTrue(HotPotContent.POT_BLOCKS.isEmpty());
        assertTrue(HotPotContent.PLATES.isEmpty());
        assertFalse(cn.blockforge.orient.framework.loader.Framework.isInstalled());
    }

}
