// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.client.BakedQuadRemap;
import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import cn.blockforge.orient.framework.client.mixin.ItemStackRenderStateAccessor;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import cn.blockforge.orient.framework.pure.DeferredRegistrations;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** NeoForge 通用客户端能力实现：26.2 物品模型由 DynamicItemModel 自行接管渲染器表；通过 DeferredRegistrations 支持 client(hooks) 晚于事件回调的顺序。作者：mryt233 */
final class NeoForgeClientContent implements ClientContent {
    private final DeferredRegistrations<RegisterMenuScreensEvent> menus = new DeferredRegistrations<>();
    private final DeferredRegistrations<EntityRenderersEvent.RegisterRenderers> renderers = new DeferredRegistrations<>();
    private final DeferredRegistrations<RegisterClientExtensionsEvent> extensions = new DeferredRegistrations<>();
    private final DeferredRegistrations<RegisterKeyMappingsEvent> keyMappings = new DeferredRegistrations<>();
    private final List<SwappableModelRule> modelRules = new CopyOnWriteArrayList<>();

    static void install(IEventBus bus) {
        NeoForgeClientContent content = new NeoForgeClientContent();
        bus.addListener((RegisterMenuScreensEvent e) -> content.menus.bind(e));
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> content.renderers.bind(e));
        bus.addListener((RegisterClientExtensionsEvent e) -> content.extensions.bind(e));
        bus.addListener((RegisterKeyMappingsEvent e) -> content.keyMappings.bind(e));
        bus.addListener((ModelEvent.ModifyBakingResult e) -> content.onModifyBakingResult(e));
        ClientContent.install(content);
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(
            MenuType<M> type, ScreenFactory<M, S> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        menus.add(e -> e.register(type, factory::create));
    }

    @Override
    public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<T> type, BlockEntityRendererProvider<T, S> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        renderers.add(e -> e.registerBlockEntityRenderer(type, factory));
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        renderers.add(e -> e.registerEntityRenderer(type, factory));
    }

    @Override
    public void tooltip(Tooltip callback) {
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
            callback.append(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip()));
    }

    @Override
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        extensions.add(event -> event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack,
                                               EquipmentClientInfo.LayerType layerType,
                                               Model original) {
                EquipmentSlot slot = layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                    ? EquipmentSlot.LEGS
                    : EquipmentSlot.CHEST;
                HumanoidModel<?> base = original instanceof HumanoidModel<?> humanoid ? humanoid : null;
                HumanoidModel<?> custom = factory.model(stack, slot, base);
                return custom != null ? custom : original;
            }
        }, items));
    }

    @Override
    public void onDisconnect(Runnable callback) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> callback.run());
    }

    // mryt233 改：26.2 按每面贴图 alpha 自动确定 cutout，无需全局登记。
    @Override
    public void cutout(Block... blocks) {
        for (Block block : blocks) {
            Objects.requireNonNull(block, "block");
        }
    }

    @Override
    public void screens(ScreenInit initializer) {
        Map<Screen, ScreenHooks> active = new IdentityHashMap<>();
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            ScreenHooks hooks = initializer.init(event.getScreen(), event::addListener);
            if (hooks != null) {
                active.put(event.getScreen(), hooks);
            } else {
                active.remove(event.getScreen());
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event) -> {
            ScreenHooks hooks = active.get(event.getScreen());
            if (hooks != null && hooks.beforeRender() != null) {
                hooks.beforeRender().run();
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre event) -> {
            ScreenHooks hooks = active.get(event.getScreen());
            if (hooks != null && hooks.scroll() != null
                && !hooks.scroll().allow(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing event) -> active.remove(event.getScreen()));
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        NeoForge.EVENT_BUS.addListener((ExtractBlockOutlineRenderStateEvent event) -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            VoxelShape shape = callback.extract(event.getBlockState(), event.getBlockPos(), event.getLevel(), player);
            if (shape == null) return;
            event.setCanceled(true);
            event.getLevelRenderState().blockOutlineRenderState = new BlockOutlineRenderState(
                event.getBlockPos(), event.isInTranslucentPass(), event.isHighContrast(), shape, event.getCustomRenderers());
        });
    }

    @Override
    public void wrappedModels(SwappableModelRule rule) {
        modelRules.add(Objects.requireNonNull(rule));
    }

    @Override
    public void refreshBlockModel(Level level, BlockPos pos) {
        if (level != null && level.isClientSide() && pos != null) {
            var be = level.getBlockEntity(pos);
            if (be != null) {
                be.requestModelDataUpdate();
            }
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
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> callback.run());
    }

    @Override
    public void hudRender(HudRender callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) ->
            callback.render(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(true)));
    }

    @Override
    public void keyMappings(KeyMapping... mappings) {
        Objects.requireNonNull(mappings);
        KeyMapping[] copy = mappings.clone();
        keyMappings.add(e -> {
            for (KeyMapping mapping : copy) {
                if (mapping != null) {
                    e.register(mapping);
                }
            }
        });
    }

    private void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (modelRules.isEmpty()) {
            return;
        }
        BakedQuadRemap.clearSpriteCache();
        Function<Identifier, TextureAtlasSprite> blockAtlas = event.getTextureGetter();
        for (Map.Entry<BlockState, BlockStateModel> entry : event.getBakingResult().blockStateModels().entrySet()) {
            BlockStateModel current = entry.getValue();
            if (current == null) {
                continue;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(entry.getKey().getBlock());
            for (SwappableModelRule rule : modelRules) {
                if (rule.matches(id)) {
                    current = new NeoForgeSwappableBlockModel(current, rule, blockAtlas);
                    entry.setValue(current);
                }
            }
        }
        for (Map.Entry<Identifier, ItemModel> entry : event.getBakingResult().itemStackModels().entrySet()) {
            ItemModel current = entry.getValue();
            if (current == null) {
                continue;
            }
            Identifier id = entry.getKey();
            for (SwappableModelRule rule : modelRules) {
                if (rule.matches(id)) {
                    current = new NeoForgeSwappableItemModel(current, rule, blockAtlas);
                    entry.setValue(current);
                }
            }
        }
    }

    // mryt233 改（2026-11-23）：平台侧复制 NeoForge 扩展属性 ambientOcclusion。
    static BakedQuad copyRemappedQuad(BakedQuad original, long uv0, long uv1, long uv2, long uv3,
                                      TextureAtlasSprite target,
                                      net.minecraft.client.renderer.chunk.ChunkSectionLayer layer,
                                      net.minecraft.client.renderer.rendertype.RenderType itemRenderType) {
        BakedQuad.MaterialInfo oldInfo = original.materialInfo();
        BakedQuad.MaterialInfo nextInfo = new BakedQuad.MaterialInfo(
            target, layer, itemRenderType, -1, oldInfo.shade(), oldInfo.lightEmission(), oldInfo.ambientOcclusion());
        return new BakedQuad(
            original.position0(), original.position1(), original.position2(), original.position3(),
            uv0, uv1, uv2, uv3, original.direction(), nextInfo);
    }

    /** 仅调整 NeoForge 四边形的 AO 开关，保留顶点、颜色索引与其余属性；一致时复用原实例。 */
    static BakedQuad copyWithAo(BakedQuad original, boolean ao) {
        BakedQuad.MaterialInfo oldInfo = original.materialInfo();
        if (oldInfo.ambientOcclusion() == ao) {
            return original;
        }
        BakedQuad.MaterialInfo nextInfo = new BakedQuad.MaterialInfo(
            oldInfo.sprite(), oldInfo.layer(), oldInfo.itemRenderType(),
            oldInfo.tintIndex(), oldInfo.shade(), oldInfo.lightEmission(), ao);
        return new BakedQuad(
            original.position0(), original.position1(), original.position2(), original.position3(),
            original.packedUV0(), original.packedUV1(), original.packedUV2(), original.packedUV3(),
            original.direction(), nextInfo);
    }

    private static final BakedQuadRemap.QuadFactory QUAD_FACTORY = NeoForgeClientContent::copyRemappedQuad;

    private static @Nullable Object extractRenderData(BlockAndTintGetter level, BlockPos pos) {
        Object data = level.getModelData(pos).get(NeoForgeModelData.SNAPSHOT);
        if (data == null && level.getBlockEntity(pos) instanceof MaterialRenderHost host) {
            data = host.materialRenderData();
        }
        return data;
    }

    private record SwappedGeometryKey(Object base, @Nullable Object data) {
    }

    // mryt233 改：提升允许 AO 的部件，汤面等明确关闭 AO 的部件保持原样。
    static BlockStateModelPart withForcedAo(BlockStateModelPart part, SwappableModelRule rule) {
        return part.ambientOcclusion() == TriState.FALSE ? part : new ForceAoPart(part, rule);
    }

    static BakedQuad preserveQuadAo(BakedQuad quad, @Nullable Identifier spriteId, SwappableModelRule rule) {
        boolean ao = quad.materialInfo().ambientOcclusion()
            && (spriteId == null || !rule.keepUnoccludedSprite(spriteId));
        return copyWithAo(quad, ao);
    }

    private static final class ForceAoPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final SwappableModelRule rule;
        private volatile @Nullable List<BakedQuad> nullQuads;
        private final java.util.EnumMap<net.minecraft.core.Direction, List<BakedQuad>> dirQuads =
            new java.util.EnumMap<>(net.minecraft.core.Direction.class);

        private ForceAoPart(BlockStateModelPart delegate, SwappableModelRule rule) {
            this.delegate = delegate;
            this.rule = rule;
        }

        private List<BakedQuad> filter(List<BakedQuad> quads) {
            if (quads.isEmpty()) {
                return quads;
            }
            List<BakedQuad> out = null;
            for (int i = 0; i < quads.size(); i++) {
                BakedQuad q = quads.get(i);
                TextureAtlasSprite sprite = q.materialInfo().sprite();
                BakedQuad adjusted = preserveQuadAo(q, sprite == null ? null : sprite.contents().name(), rule);
                if (adjusted != q) {
                    if (out == null) {
                        out = new ArrayList<>(quads.size());
                        for (int j = 0; j < i; j++) {
                            out.add(quads.get(j));
                        }
                    }
                    out.add(adjusted);
                } else if (out != null) {
                    out.add(q);
                }
            }
            return out != null ? List.copyOf(out) : quads;
        }

        @Override
        public List<BakedQuad> getQuads(net.minecraft.core.@Nullable Direction direction) {
            if (direction == null) {
                List<BakedQuad> cached = nullQuads;
                if (cached == null) {
                    cached = filter(delegate.getQuads(null));
                    nullQuads = cached;
                }
                return cached;
            }
            synchronized (dirQuads) {
                return dirQuads.computeIfAbsent(direction, d -> filter(delegate.getQuads(d)));
            }
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public TriState ambientOcclusion() {
            return TriState.TRUE;
        }

        @Override
        public Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
        }
    }

    private static final class NeoForgeSwappableBlockModel extends DelegateBlockStateModel {
        private final SwappableModelRule rule;
        private final Function<Identifier, TextureAtlasSprite> blockAtlas;

        private NeoForgeSwappableBlockModel(BlockStateModel delegate, SwappableModelRule rule,
                                            Function<Identifier, TextureAtlasSprite> blockAtlas) {
            super(delegate);
            this.rule = rule;
            this.blockAtlas = blockAtlas;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            List<BlockStateModelPart> raw = new ArrayList<>();
            super.collectParts(random, raw);
            wrapParts(null, null, raw, parts);
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                 RandomSource random, List<BlockStateModelPart> parts) {
            Object data = extractRenderData(level, pos);
            List<BlockStateModelPart> raw = new ArrayList<>();
            super.collectParts(level, pos, state, random, raw);
            wrapParts(data, state, raw, parts);
        }

        private void wrapParts(@Nullable Object data, @Nullable BlockState state,
                               List<BlockStateModelPart> raw, List<BlockStateModelPart> out) {
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(data, null, state, blockAtlas);
            boolean hasSwaps = swaps != null && !swaps.isEmpty();
            boolean forceAo = rule.forceAmbientOcclusion(state);
            if (!hasSwaps && !forceAo) {
                out.addAll(raw);
                return;
            }
            Material.Baked particleMat = null;
            if (hasSwaps) {
                TextureAtlasSprite particle = rule.particleSprite(data, state, blockAtlas);
                particleMat = particle != null ? new Material.Baked(particle, false) : null;
            }
            for (BlockStateModelPart part : raw) {
                BlockStateModelPart next = hasSwaps
                    ? BakedQuadRemap.remapPart(part, swaps, particleMat, QUAD_FACTORY)
                    : part;
                out.add(forceAo ? withForcedAo(next, rule) : next);
            }
        }

        @Override
        public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            Object data = extractRenderData(level, pos);
            TextureAtlasSprite sprite = rule.particleSprite(data, state, blockAtlas);
            return sprite != null ? new Material.Baked(sprite, false) : super.particleMaterial(level, pos, state);
        }

        @Override
        public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            int base = super.materialFlags(level, pos, state);
            Object data = extractRenderData(level, pos);
            return BakedQuadRemap.combineMaterialFlags(base, rule.resolveSwaps(data, null, state, blockAtlas));
        }

        @Override
        public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos,
                                                  BlockState state, RandomSource random) {
            Object base = delegate.createGeometryKey(level, pos, state, random);
            if (base == null) {
                return null;
            }
            return new SwappedGeometryKey(base, extractRenderData(level, pos));
        }
    }

    private static final class NeoForgeSwappableItemModel implements ItemModel {
        private final ItemModel delegate;
        private final SwappableModelRule rule;
        private final Function<Identifier, TextureAtlasSprite> blockAtlas;
        private final Map<BakedQuadRemap.QuadSwapKey, BakedQuad> quadCache = new ConcurrentHashMap<>();

        private NeoForgeSwappableItemModel(ItemModel delegate, SwappableModelRule rule,
                                           Function<Identifier, TextureAtlasSprite> blockAtlas) {
            this.delegate = delegate;
            this.rule = rule;
            this.blockAtlas = blockAtlas;
        }

        @Override
        public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                           ItemDisplayContext displayContext, @Nullable ClientLevel level,
                           @Nullable ItemOwner owner, int seed) {
            int startLayer = output instanceof ItemStackRenderStateAccessor accessor ? accessor.orient$activeLayerCount() : 0;
            delegate.update(output, item, resolver, displayContext, level, owner, seed);
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(null, item, null, blockAtlas);
            BakedQuadRemap.remapItemRenderState(output, startLayer, swaps, quadCache, QUAD_FACTORY);
        }
    }
}
