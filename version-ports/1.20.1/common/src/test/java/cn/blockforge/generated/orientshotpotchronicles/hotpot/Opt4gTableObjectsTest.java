// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Lifecycle;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第四批 4G/4H：{@link TableItem} / {@link TableBlock} / {@link TableObjects} 单元测试（1.20.1 适配）。
 * 作者：mryt233
 */
class Opt4gTableObjectsTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    @Test
    void vanillaAndEmptyItemsMatchTableObjectsContracts() {
        ItemStack bowl = new ItemStack(Items.BOWL);
        TableItem bowlTableItem = TableObjects.item(bowl);
        assertNotNull(bowlTableItem);
        assertFalse(bowlTableItem.keepsSelf(bowl));
        assertTrue(TableObjects.canAutoPlace(bowl));
        assertFalse(TableObjects.keepsSelf(bowl));
        assertFalse(TableObjects.holdsFood(null, bowl));
        assertSame(HotPotContent.BOWL_BLOCK, TableObjects.placedFor(bowl));

        ItemStack apple = new ItemStack(Items.APPLE);
        assertNull(TableObjects.item(apple));
        assertNull(TableObjects.placedFor(apple));
        assertFalse(TableObjects.canAutoPlace(apple));
        assertFalse(TableObjects.keepsSelf(apple));
        assertTrue(TableObjects.holdsFood(null, apple));

        ItemStack stick = new ItemStack(Items.STICK);
        assertNull(TableObjects.item(stick));
        assertFalse(TableObjects.canAutoPlace(stick));
        assertFalse(TableObjects.holdsFood(null, stick));

        assertNull(TableObjects.item(ItemStack.EMPTY));
        assertNull(TableObjects.placedFor(ItemStack.EMPTY));
        assertFalse(TableObjects.canAutoPlace(ItemStack.EMPTY));
        assertFalse(TableObjects.keepsSelf(ItemStack.EMPTY));
        assertFalse(TableObjects.holdsFood(null, ItemStack.EMPTY));
        assertTrue(TableObjects.prepareForTable(ItemStack.EMPTY).isEmpty());
    }

    @Test
    void externalRegisteredTableItemAndDualSoundPathsWorkWithoutCoreEdits() {
        Item vanillaHost = Items.PAPER;
        AtomicInteger placeCalls = new AtomicInteger();
        AtomicInteger addCalls = new AtomicInteger();
        AtomicReference<ItemStack> preparedFrom = new AtomicReference<>();

        TableItem externalAutoItem = new TableItem() {
            @Override
            public PlateBlock tableBlock(ItemStack stack) {
                return HotPotContent.BOWL_BLOCK;
            }

            @Override
            public boolean autoPlace(ItemStack stack) {
                return true;
            }

            @Override
            public boolean keepsSelf(ItemStack stack) {
                return true;
            }

            @Override
            public ItemStack prepareForTable(ItemStack stack) {
                preparedFrom.set(stack.copy());
                return new ItemStack(Items.SUGAR);
            }

            @Override
            public void playPlaceSound(Level world, BlockPos pos, ItemStack stack) {
                placeCalls.incrementAndGet();
                world.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            @Override
            public void playAddSound(Level world, BlockPos pos, ItemStack stack) {
                addCalls.incrementAndGet();
                world.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
        };

        try {
            TableObjects.registerItem(vanillaHost, externalAutoItem);
            ItemStack paper = new ItemStack(vanillaHost, 2);
            assertSame(externalAutoItem, TableObjects.item(paper));
            assertTrue(TableObjects.canAutoPlace(paper));
            assertTrue(TableObjects.keepsSelf(paper));
            assertFalse(TableObjects.holdsFood(null, paper));

            ItemStack prepared = TableObjects.prepareForTable(paper);
            assertTrue(prepared.is(Items.SUGAR));
            assertTrue(preparedFrom.get().is(vanillaHost));

            RecordingLevel level = new RecordingLevel();
            TableObjects.playPlaceSound(level, BlockPos.ZERO, null, paper);
            assertEquals(1, placeCalls.get());
            assertSound(level.sounds.get(0), SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

            level.sounds.clear();
            externalAutoItem.playAddSound(level, BlockPos.ZERO, paper);
            assertEquals(1, addCalls.get());
            assertSound(level.sounds.get(0), SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);

            TableItem sneakOnlyItem = new TableItem() {
                @Override
                public PlateBlock tableBlock(ItemStack stack) {
                    return null;
                }

                @Override
                public boolean autoPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public void playPlaceSound(Level world, BlockPos pos, ItemStack stack) {
                    world.playSound(null, pos, SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            };
            TableObjects.registerItem(vanillaHost, sneakOnlyItem);
            assertFalse(TableObjects.canAutoPlace(paper));
            assertNull(TableObjects.placedFor(paper));
            assertTrue(TableObjects.keepsSelf(paper));
            assertFalse(TableObjects.holdsFood(null, paper));
            level.sounds.clear();
            sneakOnlyItem.playAddSound(level, BlockPos.ZERO, paper);
            assertSound(level.sounds.get(0), SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        } finally {
            TableObjects.unregisterItem(vanillaHost);
        }

        assertNull(TableObjects.item(new ItemStack(vanillaHost)));
        assertFalse(TableObjects.canAutoPlace(new ItemStack(vanillaHost)));
    }

    @Test
    void tableBlockPropertiesHeightsAndDefaultsMatchContract() {
        assertTrue(PlateSize.values().length >= 9);
        assertEquals(3.0, PlateSize.BOWL.height());
        assertEquals(15.0, PlateSize.DRINK.height());
        assertEquals(6.0, PlateSize.CUP.height());
        assertEquals(6.0, SauceBottleBlock.heightOf(SauceBottleKind.SESAME_PASTE));
        assertEquals(8.0, SauceBottleBlock.heightOf(SauceBottleKind.OYSTER_SAUCE));
        assertEquals(8.0, SauceBottleBlock.heightOf(null));
        assertEquals(2.0, PlateHolderBlockEntity.TABLE_CHOPSTICKS_H);

        assertEquals(List.of(SoupVisual.NONE, SoupVisual.CLEAR, SoupVisual.RICH_STOCK,
            SoupVisual.MUSHROOM, SoupVisual.SPICY, SoupVisual.TOMATO, SoupVisual.THREE_FRESH, SoupVisual.OTHER),
            List.of(BowlBlock.SOUP.getPossibleValues().toArray(SoupVisual[]::new)));
        assertEquals("empty", DrinkBlock.EMPTY.getName());
        assertTrue(CupBlock.KIND.getPossibleValues().contains(DrinkKind.WATER));
        assertTrue(CupBlock.KIND.getPossibleValues().contains(DrinkKind.MILK));
        assertFalse(DrinkBlock.KIND.getPossibleValues().contains(DrinkKind.WATER));
        assertFalse(DrinkBlock.KIND.getPossibleValues().contains(DrinkKind.MILK));

        TableBlock defaultImpl = new TableBlock() {};
        assertEquals(UseOutcome.PASS,
            defaultImpl.useOnPlaced(null, null, BlockPos.ZERO, null, null, InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        TableItem nullBlockItem = stack -> null;
        assertTrue(nullBlockItem.autoPlace(ItemStack.EMPTY));
        assertTrue(nullBlockItem.keepsSelf(ItemStack.EMPTY));
        ItemStack stick = new ItemStack(Items.STICK);
        assertSame(stick, nullBlockItem.prepareForTable(stick));
        RecordingLevel level = new RecordingLevel();
        nullBlockItem.playPlaceSound(level, BlockPos.ZERO, stick);
        assertTrue(level.sounds.isEmpty());
    }

    @Test
    void legacyPlateHolderSnbtFixturesParseAndUnknownPlateIdsSkipSafely() throws Exception {
        JsonObject root = readLegacyFixtures();
        JsonObject entries = root.getAsJsonObject("entries");
        assertNotNull(entries);
        for (String key : List.of("table_oak_mixed", "table_iron_bowl_food", "table_andesite_bowls_all_soups", "table_tall_quartz")) {
            JsonObject entry = entries.getAsJsonObject(key);
            assertNotNull(entry, () -> "缺少旧档夹具: " + key);
            assertTrue(entry.has("snbt"), () -> "旧档夹具缺少 snbt 字段: " + key);
            CompoundTag nbt = TagParser.parseTag(entry.get("snbt").getAsString());
            assertTrue(nbt.contains("Plates", Tag.TAG_LIST));
            assertFalse(nbt.getList("Plates", Tag.TAG_COMPOUND).isEmpty());
        }

        CompoundTag legacyTag = new CompoundTag();
        ListTag list = new ListTag();
        list.add(legacyEntry("orients_hotpot_chronicles:quartz_small_dish", "x", "north", 3.0, 3.0));
        list.add(legacyEntry("orients_hotpot_chronicles:chopstick_rest", "x", "south", 8.0, 12.0));
        legacyTag.put("Plates", list);

        PlateHolderBlockEntity loaded = new PlateHolderBlockEntity(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        loaded.load(legacyTag);
        assertTrue(loaded.plates().isEmpty());
        CompoundTag saved = new CompoundTag();
        loaded.saveAdditional(saved);
        assertTrue(saved.contains("Plates", Tag.TAG_LIST));
    }

    private static JsonObject readLegacyFixtures() throws Exception {
        try (InputStream stream = Opt4gTableObjectsTest.class.getResourceAsStream("/opt4/legacy-0.2.2-alpha/fixtures.json")) {
            assertNotNull(stream, "找不到测试资源 /opt4/legacy-0.2.2-alpha/fixtures.json");
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static CompoundTag legacyEntry(String plateId, String axis, String facing, double x, double z) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Plate", plateId);
        tag.putString("Axis", axis);
        tag.putString("Facing", facing);
        tag.put("X", DoubleTag.valueOf(x));
        tag.put("Z", DoubleTag.valueOf(z));
        tag.put("ChopsticksYaw", FloatTag.valueOf(45.0F));
        tag.put("Items", new ListTag());
        return tag;
    }

    private record SoundRecord(ResourceLocation event, SoundSource source, float volume, float pitch) {
    }

    private static void assertSound(SoundRecord actual, SoundEvent expected, SoundSource source, float volume, float pitch) {
        assertEquals(expected.getLocation(), actual.event());
        assertEquals(source, actual.source());
        assertEquals(volume, actual.volume(), 1e-4F);
        assertEquals(pitch, actual.pitch(), 1e-4F);
    }

    private static final class RecordingLevel extends Level {
        final List<SoundRecord> sounds = new ArrayList<>();

        RecordingLevel() {
            super(
                (WritableLevelData) Proxy.newProxyInstance(
                    RecordingLevel.class.getClassLoader(),
                    new Class<?>[]{WritableLevelData.class},
                    (proxy, method, args) -> defaultReturn(method.getReturnType())),
                Level.OVERWORLD,
                levelRegistries(),
                levelRegistries().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                () -> net.minecraft.util.profiling.InactiveProfiler.INSTANCE,
                false, false, 0L, 0);
        }

        private static RegistryAccess.Frozen levelRegistries() {
            RegistryAccess.Frozen base = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
            var damageLookup = VanillaRegistries.createLookup().lookupOrThrow(Registries.DAMAGE_TYPE);
            MappedRegistry<net.minecraft.world.damagesource.DamageType> damageTypes =
                new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
            damageLookup.listElements().forEach(ref ->
                damageTypes.register(ref.key(), ref.value(), Lifecycle.stable()));
            damageTypes.freeze();
            MappedRegistry<DimensionType> dimensionTypes =
                new MappedRegistry<>(Registries.DIMENSION_TYPE, Lifecycle.stable());
            dimensionTypes.register(BuiltinDimensionTypes.OVERWORLD, new DimensionType(
                java.util.OptionalLong.empty(), true, false, false, false, 1.0, false, false, 0, 256, 256,
                net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F,
                new DimensionType.MonsterSettings(false, false,
                    net.minecraft.util.valueproviders.UniformInt.of(0, 7), 0)), Lifecycle.stable());
            dimensionTypes.freeze();
            return new RegistryAccess.Frozen() {
                @Override
                @SuppressWarnings("unchecked")
                public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> key) {
                    if (Registries.DAMAGE_TYPE.equals(key)) {
                        return Optional.of((Registry<E>) damageTypes);
                    }
                    if (Registries.DIMENSION_TYPE.equals(key)) {
                        return Optional.of((Registry<E>) dimensionTypes);
                    }
                    return base.registry(key);
                }

                @Override
                public Stream<RegistryEntry<?>> registries() {
                    return Stream.concat(
                        base.registries(),
                        Stream.of(
                            new RegistryEntry<>(Registries.DAMAGE_TYPE, damageTypes),
                            new RegistryEntry<>(Registries.DIMENSION_TYPE, dimensionTypes)));
                }
            };
        }

        private static Object defaultReturn(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0F;
            if (type == double.class) return 0.0;
            return null;
        }

        @Override
        public void playSeededSound(Player player, double x, double y, double z,
                                    Holder<SoundEvent> sound, SoundSource source,
                                    float volume, float pitch, long seed) {
            sounds.add(new SoundRecord(sound.value().getLocation(), source, volume, pitch));
        }

        @Override
        public void playSeededSound(Player player, Entity entity, Holder<SoundEvent> sound,
                                    SoundSource source, float volume, float pitch, long seed) {
            sounds.add(new SoundRecord(sound.value().getLocation(), source, volume, pitch));
        }

        @Override public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}
        @Override public void playSound(Player player, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
            sounds.add(new SoundRecord(sound.getLocation(), category, volume, pitch));
        }
        @Override public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {}
        @Override public String gatherChunkSourceStats() { return ""; }
        @Override public Entity getEntity(int id) { return null; }
        @Override public net.minecraft.world.level.saveddata.maps.MapItemSavedData getMapData(String id) { return null; }
        @Override public void setMapData(String id, net.minecraft.world.level.saveddata.maps.MapItemSavedData data) {}
        @Override public int getFreeMapId() { return 0; }
        @Override public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}
        @Override public net.minecraft.world.scores.Scoreboard getScoreboard() { return new net.minecraft.world.scores.Scoreboard(); }
        @Override public net.minecraft.world.item.crafting.RecipeManager getRecipeManager() { return null; }
        @Override protected net.minecraft.world.level.entity.LevelEntityGetter<Entity> getEntities() { return null; }
        @Override public net.minecraft.world.ticks.LevelTickAccess<Block> getBlockTicks() { return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList(); }
        @Override public net.minecraft.world.ticks.LevelTickAccess<net.minecraft.world.level.material.Fluid> getFluidTicks() { return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList(); }
        @Override public net.minecraft.world.level.chunk.ChunkSource getChunkSource() { return null; }
        @Override public void levelEvent(Player player, int type, BlockPos pos, int data) {}
        @Override public void gameEvent(net.minecraft.world.level.gameevent.GameEvent event, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.level.gameevent.GameEvent.Context context) {}
        @Override public List<? extends Player> players() { return List.of(); }
        @Override public Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x, int y, int z) { return null; }
        @Override public float getShade(Direction direction, boolean shade) { return 1.0F; }
        @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { return FeatureFlags.DEFAULT_FLAGS; }
        @Override public BlockPos getSharedSpawnPos() { return BlockPos.ZERO; }
        @Override public float getSharedSpawnAngle() { return 0.0F; }
    }
}
