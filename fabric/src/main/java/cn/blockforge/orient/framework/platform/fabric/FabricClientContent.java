// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.client.BakedQuadRemap;
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.blockforge.orient.framework.client.ClientContent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Objects;

/** 只在客户端环境加载；菜单、渲染器立即登记。 */
final class FabricClientContent implements ClientContent {
    static void install() {
        ClientContent.install(new FabricClientContent());
        // mryt233 改（2026-11-10）：动态物品平台适配由框架安装，内容只登记绘制回调。
        ClientItemRenderers.install((item, renderer) ->
            BuiltinItemRendererRegistry.INSTANCE.register(item, renderer::render));
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(MenuType<M> type, ScreenFactory<M, S> factory) {
        MenuScreens.register(Objects.requireNonNull(type), Objects.requireNonNull(factory)::create);
    }

    @Override
    public <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<T> factory) {
        BlockEntityRendererRegistry.register(Objects.requireNonNull(type), Objects.requireNonNull(factory));
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory) {
        EntityRendererRegistry.register(Objects.requireNonNull(type), Objects.requireNonNull(factory));
    }

    @Override
    public void tooltip(Tooltip callback) {
        Objects.requireNonNull(callback);
        ItemTooltipCallback.EVENT.register(callback::append);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        if (items.length == 0) throw new IllegalArgumentException("no armor items");
        for (Item item : items) {
            if (!(item instanceof ArmorItem)) throw new IllegalArgumentException("armor item required: " + item);
        }
        ArmorRenderer.register((pose, buffers, stack, entity, slot, light, parent) -> {
            HumanoidModel model = Objects.requireNonNull(factory.model(entity, stack, slot, parent));
            parent.copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> { model.head.visible = true; model.hat.visible = true; }
                case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
                case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
                case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
                default -> { return; }
            }
            ArmorItem item = (ArmorItem) stack.getItem();
            for (var layer : item.getMaterial().value().layers()) {
                ArmorRenderer.renderPart(pose, buffers, light, stack, model, layer.texture(slot == EquipmentSlot.LEGS));
            }
        }, items);
    }

    @Override
    public void onDisconnect(Runnable callback) {
        Objects.requireNonNull(callback);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
    }
    // mryt233 改（2026-11-10）：只把客户端原生事件转换为公共回调。
    @Override
    public void cutout(Block... blocks) {
        BlockRenderLayerMap.INSTANCE.putBlocks(
            RenderType.cutout(), blocks);
    }

    @Override
    public void screens(ScreenInit initializer) {
        Objects.requireNonNull(initializer);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenHooks hooks = initializer.init(screen, widget ->
                Screens.getButtons(screen).add(widget));
            if (hooks == null) return;
            ScreenEvents.beforeRender(screen).register((s, graphics, x, y, delta) -> hooks.beforeRender().run());
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> hooks.scroll().allow(x, y, vertical));
        });
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        Objects.requireNonNull(callback);
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) ->
            !callback.render(outline.blockState(), outline.blockPos(), outline.cameraX(), outline.cameraY(), outline.cameraZ(), context.matrixStack(), outline.vertexConsumer()));
    }

    @Override
    public void wrappedModels(SwappableModelRule rule) {
        Objects.requireNonNull(rule);
        ModelLoadingPlugin.register(pluginCtx -> {
            cn.blockforge.orient.framework.client.BakedQuadRemap.clearSpriteCache(); // mryt233 改：清图集代缓存。
            pluginCtx.modifyModelAfterBake().register((model, context) -> {
                ModelResourceLocation id = context.topLevelId();
                if (model == null || id == null || !rule.matches(id.id())) {
                    return model;
                }
                return new FabricSwappableModel(model, rule, atlasLookup(context));
            });
        });
    }

    @Override
    public void refreshBlockModel(Level level, BlockPos pos) {
        if (level != null && level.isClientSide && pos != null) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
            Minecraft.getInstance().levelRenderer.setBlocksDirty(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
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
        HudRenderCallback.EVENT.register((graphics, delta) ->
            callback.render(graphics, delta.getGameTimeDeltaPartialTick(true)));
    }

    @Override
    public void keyMappings(net.minecraft.client.KeyMapping... mappings) {
        Objects.requireNonNull(mappings);
        for (net.minecraft.client.KeyMapping mapping : mappings) {
            if (mapping != null) {
                KeyBindingHelper.registerKeyBinding(mapping);
            }
        }
    }

    // mryt233 改（2026-11-19）：回调上下文会被复用，只保存本次烘焙的查找函数。
    static Function<ResourceLocation, TextureAtlasSprite> atlasLookup(ModelModifier.AfterBake.Context context) {
        Function<Material, TextureAtlasSprite> textureGetter = Objects.requireNonNull(context.textureGetter());
        return tex -> textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
    }

    /** 输出时向实际渲染器取材质，避免过早初始化或把空值交给渲染后端。 */
    static RenderMaterial standardMaterial(Renderer renderer) {
        if (renderer == null) {
            throw new IllegalStateException("模型正在输出，但 Fabric 渲染器尚未就绪");
        }
        RenderMaterial material = renderer.materialById(RenderMaterial.MATERIAL_STANDARD);
        if (material == null) {
            material = renderer.materialFinder().find();
        }
        if (material == null) {
            throw new IllegalStateException("Fabric 渲染器没有提供标准模型材质");
        }
        return material;
    }

    /** 输出时向实际渲染器取强制开启平滑 AO 的材质；仅在 {@code SwappableModelRule#forceAmbientOcclusion} 为 true 时使用。 */
    static RenderMaterial ambientOcclusionMaterial(Renderer renderer) {
        if (renderer == null) {
            throw new IllegalStateException("模型正在输出，但 Fabric 渲染器尚未就绪");
        }
        RenderMaterial material = renderer.materialFinder().ambientOcclusion(TriState.TRUE).find();
        if (material == null) {
            throw new IllegalStateException("Fabric 渲染器没有提供平滑环境光遮蔽模型材质");
        }
        return material;
    }

    /** 被包装模型异常时同样释放变换，保留原异常传播。 */
    static void emitTransformed(RenderContext context, RenderContext.QuadTransform transform, Runnable emit) {
        context.pushTransform(transform);
        try {
            emit.run();
        } finally {
            context.popTransform();
        }
    }

    private static void remapQuadUv(MutableQuadView quad, TextureAtlasSprite from, TextureAtlasSprite next) {
        for (int v = 0; v < 4; v++) {
            quad.uv(v, BakedQuadRemap.remapU(from, next, quad.u(v)),
                BakedQuadRemap.remapV(from, next, quad.v(v)));
        }
        quad.colorIndex(-1);
    }

    private static boolean shouldForceQuadAo(SwappableModelRule rule, TextureAtlasSprite sprite) {
        ResourceLocation spriteId = sprite == null ? null : sprite.contents().name();
        return !rule.keepUnoccludedSprite(spriteId);
    }

    private static final class FabricSwappableModel extends ForwardingBakedModel {
        private final SwappableModelRule rule;
        private final Function<ResourceLocation, TextureAtlasSprite> blockAtlas;

        private FabricSwappableModel(BakedModel wrapped, SwappableModelRule rule,
                                     Function<ResourceLocation, TextureAtlasSprite> blockAtlas) {
            this.wrapped = wrapped;
            this.rule = rule;
            this.blockAtlas = blockAtlas;
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(null, null, state, blockAtlas);
            return BakedQuadRemap.remapQuads(wrapped.getQuads(state, side, rand), swaps);
        }

        @Override
        public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                                   Supplier<RandomSource> randomSupplier, RenderContext context) {
            Object data = blockView.getBlockEntityRenderData(pos);
            if (data == null && blockView.getBlockEntity(pos) instanceof MaterialRenderHost host) {
                data = host.materialRenderData();
            }
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(data, null, state, blockAtlas);
            boolean hasSwaps = swaps != null && !swaps.isEmpty();
            boolean forceAo = rule.forceAmbientOcclusion(state);
            if (!hasSwaps && !forceAo) {
                super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
                return;
            }
            if (hasSwaps && wrapped.isVanillaAdapter()) {
                emitRemappedVanilla(state, randomSupplier.get(), context, swaps, forceAo);
                return;
            }
            RenderMaterial aoMaterial = forceAo ? ambientOcclusionMaterial(RendererAccess.INSTANCE.getRenderer()) : null;
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            SpriteFinder finder = SpriteFinder.get(atlas);
            emitTransformed(context, quad -> {
                TextureAtlasSprite found = finder.find(quad);
                TextureAtlasSprite target = found;
                if (hasSwaps) {
                    TextureAtlasSprite next = swaps.get(found);
                    if (next != null && next != found) {
                        remapQuadUv(quad, found, next);
                        target = next;
                    }
                }
                if (aoMaterial != null && quad.material().ambientOcclusion() == TriState.DEFAULT
                    && shouldForceQuadAo(rule, target)) {
                    quad.material(aoMaterial);
                }
                return true;
            }, () -> wrapped.emitBlockQuads(blockView, state, pos, randomSupplier, context));
        }

        @Override
        public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
            Map<TextureAtlasSprite, TextureAtlasSprite> swaps = rule.resolveSwaps(null, stack, null, blockAtlas);
            if (swaps == null || swaps.isEmpty()) {
                super.emitItemQuads(stack, randomSupplier, context);
                return;
            }
            if (wrapped.isVanillaAdapter()) {
                emitRemappedVanilla(null, randomSupplier.get(), context, swaps, false);
                return;
            }
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            SpriteFinder finder = SpriteFinder.get(atlas);
            emitTransformed(context, quad -> {
                TextureAtlasSprite found = finder.find(quad);
                TextureAtlasSprite next = swaps.get(found);
                if (next != null && next != found) {
                    remapQuadUv(quad, found, next);
                }
                return true;
            }, () -> wrapped.emitItemQuads(stack, randomSupplier, context));
        }

        private void emitRemappedVanilla(BlockState state, RandomSource rand, RenderContext context,
                                         Map<TextureAtlasSprite, TextureAtlasSprite> swaps, boolean forceAo) {
            QuadEmitter emitter = context.getEmitter();
            RenderMaterial material = standardMaterial(RendererAccess.INSTANCE.getRenderer());
            RenderMaterial aoMaterial = (forceAo && wrapped.useAmbientOcclusion())
                ? ambientOcclusionMaterial(RendererAccess.INSTANCE.getRenderer()) : null;
            for (int i = -1; i < 6; i++) {
                Direction dir = i < 0 ? null : Direction.from3DDataValue(i);
                if (dir != null && state != null && context.isFaceCulled(dir)) {
                    continue;
                }
                for (BakedQuad q : wrapped.getQuads(state, dir, rand)) {
                    TextureAtlasSprite from = q.getSprite();
                    TextureAtlasSprite next = swaps.get(from);
                    TextureAtlasSprite target = (next != null && next != from) ? next : from;
                    emitter.fromVanilla(q, material, dir);
                    if (aoMaterial != null && shouldForceQuadAo(rule, target)) {
                        emitter.material(aoMaterial);
                    }
                    if (target != from) {
                        remapQuadUv(emitter, from, target);
                    }
                    emitter.emit();
                }
            }
        }
    }
}
