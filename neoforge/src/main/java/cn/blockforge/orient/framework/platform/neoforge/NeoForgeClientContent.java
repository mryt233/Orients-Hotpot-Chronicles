// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.client.BakedQuadRemap;
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.pure.DeferredRegistrations;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import java.util.Objects;

/** 各登记器通过原生事件落地；也支持 client(hooks) 晚于事件的初始化顺序。 */
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
        // mryt233 改（2026-11-10）：复用扩展事件队列，公共登记不依赖原生事件先后。
        ClientItemRenderers.install((item, renderer) ->
            content.extensions.add(e -> e.registerItem(new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer custom;
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (custom == null) {
                        var mc = Minecraft.getInstance();
                        custom = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                            @Override
                            public void renderByItem(ItemStack stack, ItemDisplayContext context,
                                    PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                                renderer.render(stack, context, pose, buffers, light, overlay);
                            }
                        };
                    }
                    return custom;
                }
            }, item)));
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(MenuType<M> type, ScreenFactory<M, S> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        menus.add(e -> e.register(type, factory::create));
    }

    @Override
    public <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<T> factory) {
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
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) ->
            callback.append(e.getItemStack(), e.getContext(), e.getFlags(), e.getToolTip()));
    }

    @Override
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        if (items.length == 0) throw new IllegalArgumentException("no armor items");
        Item[] copy = items.clone();
        for (Item item : copy) {
            if (!(item instanceof ArmorItem)) throw new IllegalArgumentException("armor item required: " + item);
        }
        extensions.add(e -> e.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                           EquipmentSlot slot, HumanoidModel<?> original) {
                return Objects.requireNonNull(factory.model(entity, stack, slot, original));
            }
        }, copy));
    }

    @Override
    public void onDisconnect(Runnable callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            // 原生事件在进入世界前也会用空连接触发，只转发真正的玩家断开。
            if (e.getPlayer() != null && e.getConnection() != null) callback.run();
        });
    }
    // mryt233 改（2026-11-10）：子标签和选中框的原生差异统一在平台转换。
    @Override
    public void cutout(Block... blocks) {
        for (var block : blocks) ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
    }

    @Override
    public void screens(ScreenInit initializer) {
        Objects.requireNonNull(initializer);
        Map<Screen, ScreenHooks> hooks = new IdentityHashMap<>();
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            ScreenHooks value = initializer.init(e.getScreen(), e::addListener);
            if (value == null) hooks.remove(e.getScreen()); else hooks.put(e.getScreen(), value);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre e) -> {
            ScreenHooks value = hooks.get(e.getScreen());
            if (value != null) value.beforeRender().run();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> {
            ScreenHooks value = hooks.get(e.getScreen());
            if (value != null && !value.scroll().allow(e.getMouseX(), e.getMouseY(), e.getScrollDeltaY())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing e) -> hooks.remove(e.getScreen()));
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((RenderHighlightEvent.Block e) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var pos = e.getTarget().getBlockPos();
            var camera = e.getCamera().getPosition();
            if (callback.render(mc.level.getBlockState(pos), pos, camera.x, camera.y, camera.z, e.getPoseStack(),
                    e.getMultiBufferSource().getBuffer(RenderType.lines()))) e.setCanceled(true);
        });
    }

    @Override
    public void wrappedModels(SwappableModelRule rule) {
        modelRules.add(Objects.requireNonNull(rule));
    }

    @Override
    public void refreshBlockModel(Level level, BlockPos pos) {
        if (level != null && level.isClientSide && pos != null) {
            var be = level.getBlockEntity(pos);
            if (be != null) {
                be.requestModelDataUpdate();
            }
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
            Minecraft.getInstance().levelRenderer.setBlocksDirty(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
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
    public void keyMappings(net.minecraft.client.KeyMapping... mappings) {
        Objects.requireNonNull(mappings);
        net.minecraft.client.KeyMapping[] copy = mappings.clone();
        keyMappings.add(e -> {
            for (net.minecraft.client.KeyMapping mapping : copy) {
                if (mapping != null) {
                    e.register(mapping);
                }
            }
        });
    }

    private void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedQuadRemap.clearSpriteCache(); // mryt233 改：新图集代次不复用旧精灵。
        if (modelRules.isEmpty()) {
            return;
        }
        Function<ResourceLocation, TextureAtlasSprite> blockAtlas = tex ->
            event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ResourceLocation id = entry.getKey().id();
            BakedModel current = entry.getValue();
            if (current == null) {
                continue;
            }
            for (SwappableModelRule rule : modelRules) {
                if (rule.matches(id)) {
                    current = new NeoForgeSwappableModel(current, rule, blockAtlas);
                    entry.setValue(current);
                }
            }
        }
    }

    // mryt233 改（2026-11-19）：扩展属性只在平台侧复制，共享框架仍只依赖原版类型。
    static BakedQuad copyRemappedQuad(BakedQuad original, int[] vertices, TextureAtlasSprite target) {
        return new BakedQuad(vertices, -1, original.getDirection(), target, original.isShade(),
            original.hasAmbientOcclusion());
    }

    /** 仅调整 NeoForge 四边形的 AO 开关，保留顶点、颜色索引与其余属性；一致时复用原实例。 */
    static BakedQuad copyWithAo(BakedQuad original, boolean ao) {
        if (original.hasAmbientOcclusion() == ao) {
            return original;
        }
        return new BakedQuad(original.getVertices(), original.getTintIndex(), original.getDirection(),
            original.getSprite(), original.isShade(), ao);
    }

    // mryt233 改：只提升默认 AO，保留底层模型对当前状态和渲染层的明确决定。
    static TriState resolveModelAo(TriState original, boolean force) {
        return force && original == TriState.DEFAULT ? TriState.TRUE : original;
    }

    private static final BakedQuadRemap.QuadFactory QUAD_FACTORY = NeoForgeClientContent::copyRemappedQuad;
    private static final ModelProperty<Object> SNAPSHOT_PROP = new ModelProperty<>();

    private static final class NeoForgeSwappableModel extends BakedModelWrapper<BakedModel> {
        private final SwappableModelRule rule;
        private final Function<ResourceLocation, TextureAtlasSprite> blockAtlas;
        private final BakedQuadRemap.ItemCache itemCache = new BakedQuadRemap.ItemCache();
        private final ItemOverrides itemOverrides;

        private NeoForgeSwappableModel(BakedModel originalModel, SwappableModelRule rule,
                                       Function<ResourceLocation, TextureAtlasSprite> blockAtlas) {
            super(originalModel);
            this.rule = rule;
            this.blockAtlas = blockAtlas;
            this.itemOverrides = new ItemOverrides() {
                @Override
                public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel world,
                                          LivingEntity entity, int seed) {
                    Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(null, stack, null, blockAtlas);
                    if (swaps == null || swaps.isEmpty()) {
                        return originalModel;
                    }
                    return itemCache.getOrCreate(swaps, key -> new NeoForgeSwappableItemModel(originalModel, key));
                }
            };
        }

        @Override
        public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
            return resolveModelAo(super.useAmbientOcclusion(state, data, renderType), rule.forceAmbientOcclusion(state));
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData tileData) {
            // mryt233 改：保留被包装模型派生的属性，再叠加本模组材质快照。
            ModelData input = tileData == null ? ModelData.EMPTY : tileData;
            ModelData base = originalModel.getModelData(level, pos, state, input);
            if (base == null) base = input;
            if (level.getBlockEntity(pos) instanceof MaterialRenderHost host) {
                Object snap = host.materialRenderData();
                if (snap != null) {
                    return base.derive().with(SNAPSHOT_PROP, snap).build();
                }
            }
            return base;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
            List<BakedQuad> remapped = BakedQuadRemap.remapQuads(originalModel.getQuads(state, side, rand),
                rule.resolveSwaps(null, null, state, blockAtlas), QUAD_FACTORY);
            return applyKeepUnoccluded(remapped, state);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                                        ModelData data, RenderType renderType) {
            if (data == null) data = ModelData.EMPTY;
            Object snap = data.get(SNAPSHOT_PROP);
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(snap, null, state, blockAtlas);
            List<BakedQuad> remapped = BakedQuadRemap.remapQuads(
                originalModel.getQuads(state, side, rand, data, renderType), swaps, QUAD_FACTORY);
            return applyKeepUnoccluded(remapped, state);
        }

        private List<BakedQuad> applyKeepUnoccluded(List<BakedQuad> quads, BlockState state) {
            if (quads.isEmpty() || !rule.forceAmbientOcclusion(state)) {
                return quads;
            }
            List<BakedQuad> out = null;
            for (int i = 0; i < quads.size(); i++) {
                BakedQuad q = quads.get(i);
                if (q.hasAmbientOcclusion() && rule.keepUnoccludedSprite(q.getSprite().contents().name())) {
                    if (out == null) {
                        out = new ArrayList<>(quads.size());
                        for (int j = 0; j < i; j++) {
                            out.add(quads.get(j));
                        }
                    }
                    out.add(copyWithAo(q, false));
                } else if (out != null) {
                    out.add(q);
                }
            }
            return out != null ? out : quads;
        }

        @Override
        public TextureAtlasSprite getParticleIcon(ModelData data) {
            if (data == null) data = ModelData.EMPTY;
            TextureAtlasSprite s = rule.particleSprite(data.get(SNAPSHOT_PROP), null, blockAtlas);
            return s != null ? s : super.getParticleIcon(data);
        }

        @Override
        public ItemOverrides getOverrides() {
            return itemOverrides;
        }
    }

    private static final class NeoForgeSwappableItemModel extends BakedModelWrapper<BakedModel> {
        private final Map<TextureAtlasSprite, TextureAtlasSprite> swaps;

        private NeoForgeSwappableItemModel(BakedModel originalModel,
                                           Map<TextureAtlasSprite, TextureAtlasSprite> swaps) {
            super(originalModel);
            this.swaps = swaps;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
            return BakedQuadRemap.remapQuads(originalModel.getQuads(state, side, rand), swaps, QUAD_FACTORY);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                                        ModelData data, RenderType renderType) {
            return BakedQuadRemap.remapQuads(originalModel.getQuads(state, side, rand, data, renderType), swaps, QUAD_FACTORY);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
            originalModel.applyTransform(context, poseStack, applyLeftHandTransform);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }
    }
}
