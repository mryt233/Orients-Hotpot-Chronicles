// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.client.BakedQuadRemap;
import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import cn.blockforge.orient.framework.client.mixin.ItemStackRenderStateAccessor;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricTextureAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/** Fabric 通用客户端能力实现：26.1 物品模型由 DynamicItemModel 自行接管渲染器表。作者：mryt233 */
final class FabricClientContent implements ClientContent {
    static void install() {
        ClientContent.install(new FabricClientContent());
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(
            MenuType<M> type, ScreenFactory<M, S> factory) {
        MenuScreens.register(type, factory::create);
    }

    @Override
    public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<T> type, BlockEntityRendererProvider<T, S> factory) {
        BlockEntityRendererRegistry.register(type, factory);
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory) {
        EntityRendererRegistry.register(type, factory);
    }

    @Override
    public void tooltip(Tooltip callback) {
        ItemTooltipCallback.EVENT.register(callback::append);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void armorModels(ArmorModel factory, Item... items) {
        ArmorRenderer.register((matrices, submitNodeCollector, stack, state, slot, light, contextModel) -> {
            HumanoidModel model = factory.model(stack, slot, contextModel);
            if (model == null) return;
            Identifier texture = armorTexture(stack, slot);
            if (texture == null) return;
            model.setupAnim(state);
            submitNodeCollector.submitModel(model, state, matrices,
                RenderTypes.armorCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        }, items);
    }

    private static Identifier armorTexture(ItemStack stack, EquipmentSlot slot) {
        Equippable eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq == null) return null;
        Identifier asset = eq.assetId().map(ResourceKey::identifier).orElse(null);
        if (asset == null) return null;
        String layer = slot == EquipmentSlot.LEGS ? "humanoid_leggings/" : "humanoid/";
        return Identifier.fromNamespaceAndPath(asset.getNamespace(),
            "textures/entity/equipment/" + layer + asset.getPath() + ".png");
    }

    @Override
    public void onDisconnect(Runnable callback) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
    }

    // mryt233 改：26.1 按每面贴图 alpha 自动确定 cutout，无需全局登记。
    @Override
    public void cutout(Block... blocks) {
        for (Block block : blocks) {
            Objects.requireNonNull(block, "block");
        }
    }

    @Override
    public void screens(ScreenInit initializer) {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            var widgets = Screens.getWidgets(screen);
            ScreenHooks hooks = initializer.init(screen, widgets::add);
            if (hooks == null) return;
            if (hooks.beforeRender() != null) {
                ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) ->
                    hooks.beforeRender().run());
            }
            if (hooks.scroll() != null) {
                ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, h, v) ->
                    hooks.scroll().allow(x, y, v));
            }
        });
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        LevelRenderEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hitResult) -> {
            if (!(hitResult instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return;
            ClientLevel level = context.level();
            Player player = Minecraft.getInstance().player;
            if (level == null || player == null) return;
            BlockPos pos = bhr.getBlockPos();
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = callback.extract(state, pos, level, player);
            if (shape == null) return;
            BlockOutlineRenderState prev = context.levelState().blockOutlineRenderState;
            boolean translucent = prev != null && prev.isTranslucent();
            boolean highContrast = prev != null && prev.highContrast();
            context.levelState().blockOutlineRenderState = new BlockOutlineRenderState(
                pos, translucent, highContrast, shape);
        });
    }

    private static final AtomicInteger HUD_INDEX = new AtomicInteger();
    private static final ModelDebugName MODEL_DEBUG_NAME = () -> "orient_framework:swappable_model";

    @Override
    public void wrappedModels(SwappableModelRule rule) {
        Objects.requireNonNull(rule);
        ModelLoadingPlugin.register(pluginCtx -> {
            BakedQuadRemap.clearSpriteCache();
            pluginCtx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                if (model == null) {
                    return null;
                }
                Identifier id = BuiltInRegistries.BLOCK.getKey(context.state().getBlock());
                if (!rule.matches(id)) {
                    return model;
                }
                // mryt233 改：只保留本次烘焙的材质查询器，不把可复用的回调上下文带入区块线程。
                var materials = context.baker().materials();
                Function<Identifier, TextureAtlasSprite> blockAtlas = tex ->
                    safeSprite(materials.get(new Material(tex), MODEL_DEBUG_NAME).sprite());
                return new FabricSwappableBlockModel(model, rule, blockAtlas);
            });
            pluginCtx.modifyItemModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
                if (model == null) {
                    return null;
                }
                Identifier id = context.itemId();
                if (id == null || !rule.matches(id)) {
                    return model;
                }
                var materials = context.bakingContext().blockModelBaker().materials();
                Function<Identifier, TextureAtlasSprite> blockAtlas = tex ->
                    safeSprite(materials.get(new Material(tex), MODEL_DEBUG_NAME).sprite());
                return new FabricSwappableItemModel(model, rule, blockAtlas);
            });
        });
    }

    @Override
    public void refreshBlockModel(Level level, BlockPos pos) {
        if (level != null && level.isClientSide() && pos != null) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
            if (level instanceof ClientLevel clientLevel) {
                clientLevel.setSectionDirtyWithNeighbors(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
            }
        }
    }

    @Override
    public void clientTick(Runnable callback) {
        Objects.requireNonNull(callback);
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    @Override
    public void hudRender(HudRender callback) {
        Objects.requireNonNull(callback);
        Identifier hudId = Identifier.fromNamespaceAndPath("orient_framework", "hud_" + HUD_INDEX.incrementAndGet());
        HudElementRegistry.addLast(hudId, (graphics, delta) ->
            callback.render(graphics, delta.getGameTimeDeltaPartialTick(true)));
    }

    @Override
    public void keyMappings(KeyMapping... mappings) {
        Objects.requireNonNull(mappings);
        for (KeyMapping mapping : mappings) {
            if (mapping != null) {
                KeyMappingHelper.registerKeyMapping(mapping);
            }
        }
    }

    private static @Nullable TextureAtlasSprite safeSprite(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return null;
        }
        return sprite;
    }

    // mryt233 改：只提升默认面，保留资源或其他模组明确指定的 AO 状态。
    static TriState quadAo(SwappableModelRule rule, @Nullable Identifier spriteId, TriState current, boolean forceAo) {
        return forceAo && current == TriState.DEFAULT && !rule.keepUnoccludedSprite(spriteId)
            ? TriState.TRUE : current;
    }

    static void emitTransformed(QuadEmitter emitter, net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform transform,
                                Runnable emit) {
        emitter.pushTransform(transform);
        try {
            emit.run();
        } finally {
            emitter.popTransform();
        }
    }

    private static @Nullable Object extractRenderData(BlockAndTintGetter level, BlockPos pos) {
        Object data = level instanceof FabricBlockGetter getter ? getter.getBlockEntityRenderData(pos) : null;
        if (data == null && level.getBlockEntity(pos) instanceof MaterialRenderHost host) {
            data = host.materialRenderData();
        }
        return data;
    }

    private record SwappedGeometryKey(Object base, @Nullable Object data) {
    }

    private static final class FabricSwappableBlockModel extends WrapperBlockStateModel {
        private final SwappableModelRule rule;
        private final Function<Identifier, TextureAtlasSprite> blockAtlas;

        private FabricSwappableBlockModel(BlockStateModel wrapped, SwappableModelRule rule,
                                          Function<Identifier, TextureAtlasSprite> blockAtlas) {
            super(wrapped);
            this.rule = rule;
            this.blockAtlas = blockAtlas;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(null, null, null, blockAtlas);
            if (swaps == null || swaps.isEmpty()) {
                super.collectParts(random, output);
                return;
            }
            List<BlockStateModelPart> raw = new ArrayList<>();
            super.collectParts(random, raw);
            TextureAtlasSprite particle = rule.particleSprite(null, null, blockAtlas);
            Material.Baked particleMat = particle != null ? new Material.Baked(particle, false) : null;
            for (BlockStateModelPart part : raw) {
                output.add(BakedQuadRemap.remapPart(part, swaps, particleMat));
            }
        }

        @Override
        public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
                              RandomSource random, Predicate<Direction> cullTest) {
            Object data = extractRenderData(level, pos);
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(data, null, state, blockAtlas);
            boolean hasSwaps = swaps != null && !swaps.isEmpty();
            boolean forceAo = rule.forceAmbientOcclusion(state);
            if (!hasSwaps && !forceAo) {
                super.emitQuads(emitter, level, pos, state, random, cullTest);
                return;
            }
            TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            SpriteFinder finder = ((FabricTextureAtlas) atlas).spriteFinder();
            emitTransformed(emitter, quad -> {
                TextureAtlasSprite found = finder.find(quad);
                TextureAtlasSprite target = found;
                if (hasSwaps) {
                    TextureAtlasSprite next = swaps.get(found);
                    if (next != null && next != found) {
                        for (int v = 0; v < 4; v++) {
                            quad.uv(v, BakedQuadRemap.remapU(found, next, quad.u(v)),
                                BakedQuadRemap.remapV(found, next, quad.v(v)));
                        }
                        quad.tintIndex(-1);
                        quad.chunkLayer(ChunkSectionLayer.byTransparency(next.transparency()));
                        quad.animated(next.contents().isAnimated());
                        target = next;
                    }
                }
                TriState originalAo = quad.ambientOcclusion();
                TriState ao = quadAo(rule, target == null ? null : target.contents().name(), originalAo, forceAo);
                if (ao != originalAo) quad.ambientOcclusion(ao);
                return true;
            }, () -> super.emitQuads(emitter, level, pos, state, random, cullTest));
        }

        @Override
        public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos,
                                                  BlockState state, RandomSource random) {
            Object base = super.createGeometryKey(level, pos, state, random);
            if (base == null) {
                return null;
            }
            return new SwappedGeometryKey(base, extractRenderData(level, pos));
        }

        @Override
        public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            Object data = extractRenderData(level, pos);
            TextureAtlasSprite sprite = rule.particleSprite(data, state, blockAtlas);
            return sprite != null ? new Material.Baked(sprite, false) : super.particleMaterial(level, pos, state);
        }

        @Override
        public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            // mryt233 改：26.1 / 26.1.1 的包装基类缺少此转发，三版均可调用底层扩展接口。
            int base = ((FabricBlockStateModel) wrapped).materialFlags(level, pos, state, random);
            Object data = extractRenderData(level, pos);
            return BakedQuadRemap.combineMaterialFlags(base, rule.resolveSwaps(data, null, state, blockAtlas));
        }

        @Override
        public boolean hasMaterialFlag(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                       RandomSource random, int flag) {
            return (materialFlags(level, pos, state, random) & flag) != 0;
        }
    }

    private static final class FabricSwappableItemModel extends WrapperBakedItemModel {
        private final SwappableModelRule rule;
        private final Function<Identifier, TextureAtlasSprite> blockAtlas;
        private final Map<BakedQuadRemap.QuadSwapKey, BakedQuad> quadCache = new ConcurrentHashMap<>();

        private FabricSwappableItemModel(ItemModel wrapped, SwappableModelRule rule,
                                         Function<Identifier, TextureAtlasSprite> blockAtlas) {
            super(wrapped);
            this.rule = rule;
            this.blockAtlas = blockAtlas;
        }

        @Override
        public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                           ItemDisplayContext displayContext, @Nullable ClientLevel level,
                           @Nullable ItemOwner owner, int seed) {
            int startLayer = output instanceof ItemStackRenderStateAccessor accessor ? accessor.orient$activeLayerCount() : 0;
            super.update(output, item, resolver, displayContext, level, owner, seed);
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(null, item, null, blockAtlas);
            BakedQuadRemap.remapItemRenderState(output, startLayer, swaps, quadCache);
        }
    }
}
