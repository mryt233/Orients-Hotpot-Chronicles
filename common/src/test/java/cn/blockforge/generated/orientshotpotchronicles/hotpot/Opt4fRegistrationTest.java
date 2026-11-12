// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 4F-1：Java 开放登记入口、Builder、冻结/重复/索引规则与专属汤碗单测。作者：mryt233
 */
class Opt4fRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    @Test
    void soupRegistryEnforcesFreezeDuplicateAndIdSortedExtras() {
        Soups.Registry registry = new Soups.Registry();
        for (SoupBase base : SoupBase.values()) {
            registry.registerBuiltin(base.def());
        }
        SoupDef zeta = SoupDef.builder(ResourceLocation.fromNamespaceAndPath("modz", "zeta_soup"))
            .source(() -> Items.RABBIT_STEW, Items.BOWL)
            .build();
        SoupDef alpha = SoupDef.builder(ResourceLocation.fromNamespaceAndPath("moda", "alpha_soup"))
            .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
            .build();

        // 后登记的 alpha（按完整 ID 字典序更小）应排在 zeta 前面，且内置前 6 项固定不动
        registry.register(zeta);
        registry.register(alpha);
        assertEquals(8, registry.size());
        for (int i = 0; i < 6; i++) {
            assertSame(SoupBase.values()[i].def(), registry.byIndex(i));
        }
        assertSame(alpha, registry.byIndex(6));
        assertSame(zeta, registry.byIndex(7));
        assertEquals(6, registry.indexOf(alpha));
        assertEquals(7, registry.indexOf(zeta));

        assertThrows(IllegalArgumentException.class, () -> registry.register(alpha));
        assertThrows(IllegalArgumentException.class, () -> registry.register(SoupBase.CLEAR.def()));

        registry.freeze();
        assertTrue(registry.isFrozen());
        SoupDef late = SoupDef.builder(ResourceLocation.fromNamespaceAndPath("modx", "late"))
            .source(() -> Items.SUSPICIOUS_STEW, Items.BOWL)
            .build();
        assertThrows(IllegalStateException.class, () -> registry.register(late));
    }

    @Test
    void seasoningRegistryEnforcesFreezeDuplicateLazyRawsAndSortedExtras() {
        Seasonings.Registry registry = new Seasonings.Registry();
        for (int i = 0; i < Seasoning.ALL.length; i++) {
            registry.registerBuiltin(SeasoningDef.builder(Seasoning.ALL[i].id())
                .raw(() -> Items.SUGAR)
                .build());
        }

        AtomicBoolean evaluatedDuringRegister = new AtomicBoolean(false);
        Supplier<Item> lazyRaw = () -> {
            evaluatedDuringRegister.set(true);
            return Items.AMETHYST_SHARD;
        };
        SeasoningDef zesty = SeasoningDef.builder(ResourceLocation.fromNamespaceAndPath("modz", "zesty"))
            .raw(() -> Items.EMERALD)
            .color(0xFF33AA44)
            .build();
        SeasoningDef anise = SeasoningDef.builder(ResourceLocation.fromNamespaceAndPath("moda", "anise"))
            .raw(lazyRaw)
            .effect(MobEffects.DIG_SPEED, 200, 1)
            .hunger(2)
            .build();

        registry.register(zesty);
        registry.register(anise);
        assertFalse(evaluatedDuringRegister.get(), "登记阶段不得提前解析原料 Supplier");

        List<SeasoningDef> all = registry.all();
        assertEquals(Seasoning.ALL.length + 2, all.size());
        for (int i = 0; i < Seasoning.ALL.length; i++) {
            assertEquals(SauceIds.full(Seasoning.ALL[i].id()), SauceIds.full(all.get(i).id()));
        }
        assertSame(anise, all.get(Seasoning.ALL.length));
        assertSame(zesty, all.get(Seasoning.ALL.length + 1));
        assertSame(anise, registry.byId("moda:anise"));
        assertNotNull(registry.byId("honey"));
        assertSame(registry.byId("honey"), registry.byId(HotPot.MOD_ID + ":honey"));

        assertSame(anise, registry.raw(Items.AMETHYST_SHARD));
        assertTrue(evaluatedDuringRegister.get());

        assertThrows(IllegalArgumentException.class, () -> registry.register(anise));
        assertThrows(IllegalArgumentException.class, () -> registry.register(
            SeasoningDef.builder("honey").raw(() -> Items.DIAMOND).build()));

        registry.freeze();
        assertThrows(IllegalStateException.class, () -> registry.register(
            SeasoningDef.builder("modx:late").raw(() -> Items.STICK).build()));
    }

    @Test
    void buildersValidateAndPreserveTemporaryVisualNone() {
        SoupDef.Builder invalidSoup = SoupDef.builder(ResourceLocation.fromNamespaceAndPath("example", "empty"));
        assertThrows(IllegalStateException.class, invalidSoup::build);

        SoupDef herb = SoupDef.builder(ResourceLocation.fromNamespaceAndPath("example", "herb"))
            .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
            .speed(1.5F)
            .bowlFood(6, 0.8F)
            .mainEffect(MobEffects.REGENERATION, 160)
            .clearsNausea(true)
            .build();
        assertEquals("text.example.soup.herb", herb.nameKey());
        assertEquals("example:herb", herb.key());
        assertEquals(SoupVisual.NONE, herb.visual(), "4F-1 期间非内置汤底暂映射为 NONE");
        assertEquals(1.5F, herb.speedMul());
        assertEquals(6, herb.bowlHunger());
        assertEquals(0.8F, herb.bowlSaturation());

        SeasoningDef.Builder invalidSeasoning = SeasoningDef.builder("example:empty");
        assertThrows(IllegalArgumentException.class, invalidSeasoning::build);

        SeasoningDef wasabi = SeasoningDef.builder(ResourceLocation.fromNamespaceAndPath("example", "wasabi"))
            .raw(() -> Items.AMETHYST_SHARD)
            .garnish(true)
            .color(0xFF7CB342)
            .build();
        assertEquals("text.example.seasoning.wasabi", wasabi.nameKey());
        assertEquals("text.example.seasoning.wasabi.effect", wasabi.effectKey());
        assertEquals(0.7F, wasabi.dotSize());
        assertEquals(2, wasabi.dotCount());
    }

    @Test
    void exclusiveSoupBowlResolvesRegisteredSoupLazily() throws Exception {
        assertNotNull(SoupBowlItem.class.getConstructor(ResourceLocation.class));
        Soups.Registry registry = new Soups.Registry();
        ResourceLocation soupId = ResourceLocation.fromNamespaceAndPath("opt4f_unit", "lazy_soup");
        registry.registerBowl(soupId, Items.MUSHROOM_STEW);
        assertThrows(IllegalStateException.class, () -> SoupBowlItem.requireSoupDef(soupId, registry::get));

        SoupDef lazySoup = SoupDef.builder(soupId)
            .source(() -> Items.SUSPICIOUS_STEW, Items.BOWL)
            .bowlFood(5, 0.7F)
            .build();
        registry.register(lazySoup);

        assertSame(lazySoup, SoupBowlItem.requireSoupDef(soupId, registry::get));
        assertSame(Items.MUSHROOM_STEW, registry.bowlItem(lazySoup));
    }

    @Test
    void unknownIdsRemainPreservedInStorageAndSauce() {
        List<String> normalized = SauceIds.normalize(List.of("other_mod:missing_spice", "honey"));
        assertEquals(List.of(HotPot.MOD_ID + ":honey", "other_mod:missing_spice"), normalized);

        CompoundTag potTag = new CompoundTag();
        potTag.putString("Soup", "other_mod:missing_soup");
        HotPotBlockEntity pot = new HotPotBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        HotPotStorage.read(pot, potTag, null);
        assertEquals(SoupIds.UNKNOWN, pot.baseOfSide(0));
        CompoundTag saved = new CompoundTag();
        HotPotStorage.write(pot, saved, null);
        assertEquals("other_mod:missing_soup", saved.getString("Soup"));
        assertEquals(SoupIds.UNKNOWN, SoupIds.index("other_mod:missing_soup"));
    }
}
