// 作者：mryt233
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
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
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.WritableLevelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第四批 4G/4H：{@link TableItem} / {@link TableBlock} / {@link TableObjects} 单元测试（纯无平台部分）。
 * <p>说明：1.21.1 {@link BuiltInRegistries} 在测试引导期即冻结，无法再实例化未注册的 {@link Block} 或 {@link Item}；
 * 全部模组已注册盘/碗/杯/瓶/酱瓶/竹筷实例及外部自定义 {@code TableBlock} 的世界交互回归由服务端探针 {@code Opt4gProbe}
 * 与双端对照脚本 {@code run_4g4h_baseline.py} 覆盖。</p>
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
                return HotPotContent.BOWL_BLOCK; // 纯单元测试未注册内容时为 null，真实运行时为对应方块
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
            // 所有登记为 TableItem 的物品一律不能被当成盘中食材塞进其他盘子
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

            // 再测 autoPlace=false + 默认 playAddSound 委托 playPlaceSound
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

        // 卸载后恢复为非桌面食物
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
            CompoundTag nbt = TagParser.parseCompoundFully(entry.get("snbt").getAsString());
            assertTrue(nbt.contains("Plates"));
            assertFalse(nbt.getListOrEmpty("Plates").isEmpty());
        }

        // 在未注册模组方块的纯单测 JVM 中加载旧版 Plates 列表应安全跳过未知方块而不崩溃
        CompoundTag legacyTag = new CompoundTag();
        ListTag list = new ListTag();
        list.add(legacyEntry("orients_hotpot_chronicles:quartz_small_dish", "x", "north", 3.0, 3.0));
        list.add(legacyEntry("orients_hotpot_chronicles:chopstick_rest", "x", "south", 8.0, 12.0));
        legacyTag.put("Plates", list);

        PlateHolderBlockEntity loaded = new PlateHolderBlockEntity(BlockPos.ZERO, Blocks.AIR.defaultBlockState()) {
            @Override
            public boolean isValidBlockState(BlockState state) {
                return true;
            }
        };
        var regs = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
        loaded.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, regs, legacyTag));
        assertTrue(loaded.plates().isEmpty());
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, regs);
        loaded.saveAdditional(out);
        CompoundTag saved = out.buildResult();
        assertTrue(saved.contains("Plates"));
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

    private record SoundRecord(Identifier event, SoundSource source, float volume, float pitch) {
    }

    private static void assertSound(SoundRecord actual, SoundEvent expected, SoundSource source, float volume, float pitch) {
        assertEquals(expected.location(), actual.event());
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
                VanillaRegistries.createLookup().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                false, false, 0L, 0);
        }

        private static <T> Registry<T> copyRegistry(net.minecraft.core.HolderLookup.Provider lookup, ResourceKey<? extends Registry<T>> key) {
            MappedRegistry<T> reg = new MappedRegistry<>(key, Lifecycle.stable());
            lookup.lookupOrThrow(key).listElements().forEach(ref ->
                reg.register(ref.key(), ref.value(), RegistrationInfo.BUILT_IN));
            reg.freeze();
            return reg;
        }

        private static RegistryAccess.Frozen levelRegistries() {
            RegistryAccess.Frozen base = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
            net.minecraft.core.HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
            Registry<net.minecraft.world.damagesource.DamageType> damageTypes = copyRegistry(vanilla, Registries.DAMAGE_TYPE);
            Registry<net.minecraft.world.level.biome.Biome> biomes = copyRegistry(vanilla, Registries.BIOME);
            return new RegistryAccess.Frozen() {
                @Override
                @SuppressWarnings("unchecked")
                public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> key) {
                    if (Registries.DAMAGE_TYPE.equals(key)) return Optional.of((Registry<E>) damageTypes);
                    if (Registries.BIOME.equals(key)) return Optional.of((Registry<E>) biomes);
                    return base.lookup(key);
                }

                @Override
                public Stream<RegistryEntry<?>> registries() {
                    return Stream.concat(
                        base.registries(),
                        Stream.of(
                            new RegistryEntry<>(Registries.DAMAGE_TYPE, damageTypes),
                            new RegistryEntry<>(Registries.BIOME, biomes)));
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

        @Override public int getSeaLevel() { return 63; }
        @Override public net.minecraft.world.level.border.WorldBorder getWorldBorder() { return new net.minecraft.world.level.border.WorldBorder(); }

        @Override
        public void playSeededSound(Entity entity, double x, double y, double z,
                                    Holder<SoundEvent> sound, SoundSource source,
                                    float volume, float pitch, long seed) {
            sounds.add(new SoundRecord(sound.value().location(), source, volume, pitch));
        }

        @Override
        public void playSeededSound(Entity entity, Entity target, Holder<SoundEvent> sound,
                                    SoundSource source, float volume, float pitch, long seed) {
            sounds.add(new SoundRecord(sound.value().location(), source, volume, pitch));
        }

        @Override public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}
        @Override public void playSound(Entity entity, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
            sounds.add(new SoundRecord(sound.location(), category, volume, pitch));
        }
        @Override public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {}
        @Override public void explode(Entity source, net.minecraft.world.damagesource.DamageSource damageSource,
                                      net.minecraft.world.level.ExplosionDamageCalculator damageCalculator,
                                      double x, double y, double z, float radius, boolean fire,
                                      ExplosionInteraction interaction,
                                      net.minecraft.core.particles.ParticleOptions smallExplosionParticles,
                                      net.minecraft.core.particles.ParticleOptions largeExplosionParticles,
                                      net.minecraft.util.random.WeightedList<net.minecraft.core.particles.ExplosionParticleInfo> blockParticles,
                                      Holder<SoundEvent> explosionSound) {}
        @Override public String gatherChunkSourceStats() { return ""; }
        @Override public void setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData data) {}
        @Override public net.minecraft.world.level.storage.LevelData.RespawnData getRespawnData() { return null; }
        @Override public Entity getEntity(int id) { return null; }
        @Override public Collection<net.minecraft.world.entity.boss.enderdragon.EnderDragonPart> dragonParts() { return List.of(); }
        @Override public net.minecraft.world.level.saveddata.maps.MapItemSavedData getMapData(net.minecraft.world.level.saveddata.maps.MapId id) { return null; }
        @Override public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}
        @Override public net.minecraft.world.scores.Scoreboard getScoreboard() { return new net.minecraft.world.scores.Scoreboard(); }
        @Override public net.minecraft.world.item.crafting.RecipeAccess recipeAccess() { return null; }
        @Override protected net.minecraft.world.level.entity.LevelEntityGetter<Entity> getEntities() { return null; }
        @Override public net.minecraft.world.ticks.LevelTickAccess<Block> getBlockTicks() { return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList(); }
        @Override public net.minecraft.world.ticks.LevelTickAccess<net.minecraft.world.level.material.Fluid> getFluidTicks() { return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList(); }
        @Override public net.minecraft.world.level.chunk.ChunkSource getChunkSource() { return null; }
        @Override public void levelEvent(Entity entity, int type, BlockPos pos, int data) {}
        @Override public void gameEvent(Holder<net.minecraft.world.level.gameevent.GameEvent> event, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.level.gameevent.GameEvent.Context context) {}
        @Override public List<? extends Player> players() { return List.of(); }
        @Override public Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x, int y, int z) { return null; }
        @Override public net.minecraft.world.TickRateManager tickRateManager() { return null; }
        @Override public net.minecraft.world.clock.ClockManager clockManager() { return null; }
        @Override public net.minecraft.world.attribute.EnvironmentAttributeSystem environmentAttributes() { return null; }
        @Override public net.minecraft.world.item.alchemy.PotionBrewing potionBrewing() { return null; }
        @Override public net.minecraft.world.level.block.entity.FuelValues fuelValues() { return null; }
        @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { return FeatureFlags.DEFAULT_FLAGS; }
    }
}
