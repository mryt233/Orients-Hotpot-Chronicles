// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;

import cn.blockforge.orient.framework.compat.ItemData;
import cn.blockforge.orient.framework.compat.ItemDataKey;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 4F-1：Java 开放登记入口、Builder、冻结/重复/索引规则与专属汤碗单测（1.20.1 适配）。作者：mryt233
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
        SoupDef zeta = SoupDef.builder(new ResourceLocation("modz", "zeta_soup"))
            .source(() -> Items.RABBIT_STEW, Items.BOWL)
            .build();
        SoupDef alpha = SoupDef.builder(new ResourceLocation("moda", "alpha_soup"))
            .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
            .build();

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
        SoupDef late = SoupDef.builder(new ResourceLocation("modx", "late"))
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
        SeasoningDef zesty = SeasoningDef.builder(new ResourceLocation("modz", "zesty"))
            .raw(() -> Items.EMERALD)
            .color(0xFF33AA44)
            .build();
        SeasoningDef anise = SeasoningDef.builder(new ResourceLocation("moda", "anise"))
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
        SoupDef.Builder invalidSoup = SoupDef.builder(new ResourceLocation("example", "empty"));
        assertThrows(IllegalStateException.class, invalidSoup::build);

        SoupDef herb = SoupDef.builder(new ResourceLocation("example", "herb"))
            .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
            .speed(1.5F)
            .bowlFood(6, 0.8F)
            .mainEffect(MobEffects.REGENERATION, 160)
            .clearsNausea(true)
            .build();
        assertEquals("text.example.soup.herb", herb.nameKey());
        assertEquals("example:herb", herb.key());
        assertEquals(SoupVisual.OTHER, herb.visual(), "4F-3 起非内置汤底统一映射为 OTHER");
        assertEquals(SoupVisual.NONE, SoupVisual.fromDef(null));
        assertEquals(1.5F, herb.speedMul());
        assertEquals(6, herb.bowlHunger());
        assertEquals(0.8F, herb.bowlSaturation());

        SeasoningDef.Builder invalidSeasoning = SeasoningDef.builder("example:empty");
        assertThrows(IllegalArgumentException.class, invalidSeasoning::build);

        SeasoningDef wasabi = SeasoningDef.builder(new ResourceLocation("example", "wasabi"))
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
        ResourceLocation soupId = new ResourceLocation("opt4f_unit", "lazy_soup");
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
        HotPotStorage.read(pot, potTag);
        assertEquals(SoupIds.UNKNOWN, pot.baseOfSide(0));
        CompoundTag saved = new CompoundTag();
        HotPotStorage.write(pot, saved);
        assertEquals("other_mod:missing_soup", saved.getString("Soup"));
        assertEquals(SoupIds.UNKNOWN, SoupIds.index("other_mod:missing_soup"));
        assertEquals(SoupVisual.OTHER, HotPotSoup.blockVisual(pot), "未解析汤底在方块状态仍为 NONE 时应兜底用 OTHER");
    }

    @Test
    void genericSoupBowlComponentResolutionNamingAndRouting() throws Exception {
        assertNotNull(SoupBowlItem.class.getConstructor());
        assertEquals(SoupBowlItem.class, HotPotContent.class.getField("SOUP_BOWL").getType());
        assertEquals(ItemDataKey.class, HotPotContent.class.getField("SOUP").getType());

        ItemDataKey<String> soupKey = new ItemDataKey<>("soup", Codec.STRING);
        Soups.Registry registry = new Soups.Registry();
        for (SoupBase base : SoupBase.values()) {
            registry.registerBuiltin(base.def());
        }
        ResourceLocation customId = new ResourceLocation("opt4f_unit", "ginger");
        SoupDef ginger = SoupDef.builder(customId)
            .source(() -> Items.BEETROOT_SOUP, Items.BOWL)
            .bowlFood(6, 0.85F)
            .build();
        registry.register(ginger);

        // NBT 键读写与未知汤底解析
        ItemStack generic = new ItemStack(Items.BOWL);
        ItemData.set(generic, soupKey, customId.toString());
        assertEquals(customId.toString(), SoupBowlItem.rawSoupId(null, generic, soupKey));
        SoupDef resolved = SoupBowlItem.resolveGenericSoup(
            SoupBowlItem.rawSoupId(null, generic, soupKey),
            id -> {
                ResourceLocation loc = ResourceLocation.tryParse(id);
                return loc == null ? null : registry.get(loc);
            });
        assertSame(ginger, resolved);

        ItemData.set(generic, soupKey, "deleted_pack:missing");
        String unknownRaw = SoupBowlItem.rawSoupId(null, generic, soupKey);
        assertEquals("deleted_pack:missing", unknownRaw);
        assertNull(SoupBowlItem.resolveGenericSoup(unknownRaw, id -> {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            return loc == null ? null : registry.get(loc);
        }));
        assertNull(SoupBowlItem.resolveGenericSoup("Bad Id!", id -> null));

        // 名称占位：已解析显示 soup_bowl.named(nameKey)，未解析显示 soup_bowl.unknown(raw)
        Component fallback = Component.literal("fallback");
        Component named = SoupBowlItem.bowlName(ginger, null, fallback);
        TranslatableContents namedContents = assertInstanceOf(TranslatableContents.class, named.getContents());
        assertEquals("item.orients_hotpot_chronicles.soup_bowl.named", namedContents.getKey());
        Component arg = assertInstanceOf(Component.class, namedContents.getArgs()[0]);
        assertEquals(ginger.nameKey(), assertInstanceOf(TranslatableContents.class, arg.getContents()).getKey());

        Component unknown = SoupBowlItem.bowlName(null, "deleted_pack:missing", fallback);
        TranslatableContents unknownContents = assertInstanceOf(TranslatableContents.class, unknown.getContents());
        assertEquals("item.orients_hotpot_chronicles.soup_bowl.unknown", unknownContents.getKey());
        assertEquals("deleted_pack:missing", unknownContents.getArgs()[0]);
        assertSame(fallback, SoupBowlItem.bowlName(null, null, fallback));

        // 未知汤底吃下去回退安全默认营养与饱食度
        FoodProperties unknownFood = SoupBowlItem.eating(null);
        assertEquals(SoupBase.DEFAULT_BOWL_HUNGER, unknownFood.getNutrition());
        assertEquals(0.6F, unknownFood.getSaturationModifier(), 1e-4F);

        // 内置 / 专属碗 vs 通用碗分流，及未注册 SOUP_BOWL 时的安全兜底
        assertTrue(SoupBowlItem.createFor(null).isEmpty());
        assertTrue(Soups.bowlStack(null).isEmpty());
        assertTrue(Soups.bowlStack(ginger).isEmpty(), "测试未初始化 SOUP_BOWL 时应安全返回空栈");

        ItemStack dedicated = SoupBowlItem.createFor(SoupBase.CLEAR.def(), Items.MUSHROOM_STEW, Items.BOWL, soupKey);
        assertTrue(dedicated.is(Items.MUSHROOM_STEW));
        assertNull(ItemData.get(dedicated, soupKey));

        ItemStack customStack = SoupBowlItem.createFor(ginger, null, Items.BOWL, soupKey);
        assertTrue(customStack.is(Items.BOWL));
        assertEquals(customId.toString(), ItemData.get(customStack, soupKey));
    }
}
