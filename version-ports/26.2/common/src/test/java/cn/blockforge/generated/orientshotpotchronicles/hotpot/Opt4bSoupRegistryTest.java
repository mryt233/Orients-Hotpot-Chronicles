// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 4B-1：汤底定义 {@link SoupDef} 与登记表 {@link Soups} 的骨架回归。作者：mryt233
 *
 * <p>只证明三件事：六个内置汤底按枚举顺序占登记表前 6 项（顺序 = 旧存档序号）；枚举访问器与定义里的值一一对应；
 * 登记表的查询与重复登记规则。调用点（{@code byId}、{@code values()}）本步一个没动，由 3A 的
 * {@link Opt3AttributesTest} 和 4A 的旧档对比继续守着。</p>
 */
class Opt4bSoupRegistryTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    @Test
    void builtinsOccupyTheFirstSixSlotsInEnumOrder() {
        List<SoupDef> all = Soups.all();
        assertTrue(all.size() >= 6);
        SoupBase[] bases = SoupBase.values();
        assertEquals(6, bases.length);
        assertEquals(6, Soups.builtins().size());
        for (int i = 0; i < bases.length; i++) {
            assertSame(bases[i].def(), all.get(i), "登记表第 " + i + " 项必须是 " + bases[i]);
            assertSame(bases[i].def(), Soups.builtins().get(i));
            assertEquals(bases[i].textureKey(), bases[i].def().id().getPath());
            assertEquals(HotPot.MOD_ID, bases[i].def().id().getNamespace());
        }
    }

    @Test
    void lookupByIdTextAndLocation() {
        assertSame(SoupBase.SPICY.def(), Soups.get("spicy"));
        assertSame(SoupBase.SPICY.def(), Soups.get(HotPot.MOD_ID + ":spicy"));
        assertSame(SoupBase.THREE_FRESH.def(), Soups.get(Identifier.fromNamespaceAndPath(HotPot.MOD_ID, "three_fresh")));
        assertNull(Soups.get("minecraft:spicy"), "别的命名空间不能借火锅的短名");
        assertNull(Soups.get("no_such_soup"));
        assertNull(Soups.get(""));
        assertNull(Soups.get((String) null));
        assertNull(Soups.get("Bad Id!"), "非法写法返回 null，不抛异常");
    }

    @Test
    void definitionMatchesEnumAccessors() {
        for (SoupBase base : SoupBase.values()) {
            SoupDef def = base.def();
            assertEquals(base.bowlId(), def.bowlId());
            assertEquals(base.defaultBowlSaturation(), def.defaultBowlSaturation());
            assertEquals(base.defaultDuration(), def.defaultDuration());
            assertEquals(base.defaultSecondDuration(), def.defaultSecondDuration());
            assertEquals(base.defaultFireDuration(), def.defaultFireDuration());
            assertEquals(base.defaultHungerDuration(), def.defaultHungerDuration());
            assertEquals(base.clearsNausea(), def.clearsNausea());
            assertEquals(base.isSpicy(), def.spicy());
            assertEquals(base.speedMul(), def.speedMul());
            assertEquals(base.bowlHunger(), def.bowlHunger());
            assertEquals(base.bowlSaturation(), def.bowlSaturation());
            assertEquals(base.textureKey(), def.key());
            assertEquals(base.visual(), def.visual());
            assertEquals(base.dishBonusEffects(3).size(), def.dishBonusEffects(3).size());
            assertEquals(base.screenBase(), def.screenBase());
            assertArrayEquals(base.screenBits(), def.screenBits());
            assertEquals(base.bubbleBody(), def.bubbleBody());
            assertEquals(def.nameKey(), base.nameKey());
        }
        assertTrue(SoupBase.TOMATO.def().additive() != null && SoupBase.THREE_FRESH.def().additive() != null);
        assertNull(SoupBase.CLEAR.def().additive());
    }

    @Test
    void definitionIsImmutableAndRejectsNulls() {
        SoupDef def = SoupBase.CLEAR.def();
        int[] bits = def.screenBits();
        int first = bits[0];
        bits[0] = first + 1;
        assertEquals(first, def.screenBits()[0], "读出来的数组是拷贝");
        int[] source = {1, 2, 3};
        SoupDef copy = sample("opt4b_copy_check", source);
        source[0] = 99;
        assertEquals(1, copy.screenBits()[0], "构造时也拷贝");
        assertThrows(NullPointerException.class, () -> new SoupDef(null, "n", "b", 1F, () -> Items.BUCKET,
            Items.BUCKET, null, 4, 0.6F, null, 0, null, 0, null, 0, null, 0, false, false, 0, 0, 0, new int[1], 0, 0));
    }

    @Test
    void registerRejectsDuplicatesAndKeepsBuiltinsFirst() {
        int before = Soups.size();
        SoupDef extra = sample("opt4b_test_soup", new int[]{1});
        Soups.register(extra);
        assertEquals(before + 1, Soups.size());
        assertSame(extra, Soups.get("opt4b_test_soup"));
        assertSame(SoupBase.CLEAR.def(), Soups.all().get(0), "新登记的排在后面，内置项仍占前 6 项");
        assertThrows(IllegalArgumentException.class, () -> Soups.register(extra));
        assertThrows(IllegalArgumentException.class, () -> Soups.register(SoupBase.SPICY.def()));
    }

    @Test
    void visualCheckOnlyCoversBuiltins() {
        // 类初始化时已经跑过一次；再跑一次也必须通过，且新登记的非内置项（上面那个测试汤底）不参与核对
        SoupVisual.verifyAgainstSoupBases();
        for (SoupDef def : Soups.builtins()) {
            assertNotNull(SoupBase.byTextureKey(def.id().getPath()));
        }
    }

    @Test
    void indexLookupKeepsLegacyOrdinals() {
        SoupBase[] bases = SoupBase.values();
        for (int i = 0; i < bases.length; i++) {
            assertSame(bases[i].def(), Soups.byIndex(i), "序号 " + i);
            assertEquals(i, Soups.indexOf(bases[i].def()));
        }
        assertNull(Soups.byIndex(-1), "-1 = 没汤");
        assertNull(Soups.byIndex(9999));
        assertEquals(-1, Soups.indexOf(null));
        assertEquals(-1, Soups.indexOf(sample("opt4b_never_registered", new int[]{1})));
        assertSame(SoupBase.CLEAR.def(), Soups.clear());
        assertEquals(0, Soups.indexOf(Soups.clear()));
    }

    @Test
    void newlyRegisteredSoupGetsALaterIndex() {
        SoupDef later = sample("opt4b_index_soup", new int[]{1});
        Soups.register(later);
        int index = Soups.indexOf(later);
        assertTrue(index >= 6);
        assertSame(later, Soups.byIndex(index));
        assertEquals(SoupVisual.OTHER, later.visual(), "非内置汤底统一映射为 OTHER");
        assertEquals("opt4b_index_soup", later.key(), "火锅命名空间的汤底用短名");
        for (int i = 0; i < 6; i++) {
            assertEquals(SoupBase.values()[i].def(), Soups.byIndex(i), "内置序号不被新登记挤动");
        }
    }

    @Test
    void sourceStackLookupMatchesTheEnum() {
        net.minecraft.world.item.ItemStack water = new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET);
        net.minecraft.world.item.ItemStack milk = new net.minecraft.world.item.ItemStack(Items.MILK_BUCKET);
        net.minecraft.world.item.ItemStack stew = new net.minecraft.world.item.ItemStack(Items.MUSHROOM_STEW);
        net.minecraft.world.item.ItemStack lava = new net.minecraft.world.item.ItemStack(Items.LAVA_BUCKET);
        net.minecraft.world.item.ItemStack stick = new net.minecraft.world.item.ItemStack(Items.STICK);
        assertSame(SoupBase.CLEAR.def(), Soups.fromStack(water));
        assertSame(SoupBase.RICH_STOCK.def(), Soups.fromStack(milk));
        assertSame(SoupBase.MUSHROOM.def(), Soups.fromStack(stew));
        assertSame(SoupBase.SPICY.def(), Soups.fromStack(lava));
        assertNull(Soups.fromStack(stick));
        assertNull(Soups.fromStack(net.minecraft.world.item.ItemStack.EMPTY));
        assertSame(SoupBase.CLEAR, SoupBase.fromStack(water));
    }

    @Test
    void legacyCookingAndSlotSignaturesRemainCallable() throws Exception {
        assertEquals(SoupBase.class, HotPotBlockEntity.class.getMethod("soupForSlot", int.class).getReturnType());
        assertEquals(SoupDef.class, HotPotBlockEntity.class.getMethod("soupDefForSlot", int.class).getReturnType());
        assertNotNull(HotPotCooking.class.getMethod("finish", net.minecraft.world.level.Level.class,
            net.minecraft.world.item.ItemStack.class, SoupBase.class));
        java.util.function.BiFunction<net.minecraft.world.level.Level, net.minecraft.world.item.ItemStack,
            net.minecraft.world.item.ItemStack> nullSoup = (world, raw) -> HotPotCooking.finish(world, raw, null);
        assertNotNull(nullSoup);
    }

    private static SoupDef sample(String path, int[] bits) {
        return new SoupDef(Identifier.fromNamespaceAndPath(HotPot.MOD_ID, path), "soup." + path, path + "_bowl",
            1F, () -> Items.WATER_BUCKET, Items.BUCKET, null, 4, 0.6F, MobEffects.SPEED, 100, null, 0,
            null, 0, null, 0, false, false, 1, 2, 3, bits, 4, 5);
    }
}
