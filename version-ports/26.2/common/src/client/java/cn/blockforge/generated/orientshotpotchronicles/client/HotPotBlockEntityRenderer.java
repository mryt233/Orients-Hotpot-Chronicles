// mryt233 改：26.2 RenderFrame / RenderItems / Identifier API。
package cn.blockforge.generated.orientshotpotchronicles.client;

import static cn.blockforge.generated.orientshotpotchronicles.client.RenderSprites.sprite;

import cn.blockforge.orient.framework.client.FrameBlockEntityRenderer;
import cn.blockforge.orient.framework.client.RenderFrame;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 画火锅锅里的东西（独立火锅 / 灶上火锅共用）。
 * 作者：mryt233
 */
public final class HotPotBlockEntityRenderer<T extends HotPotBlockEntity>
    extends FrameBlockEntityRenderer<T> {
    private static final float OMEGA = HotPotBlockEntity.BOB_OMEGA;
    private static final float AMPLITUDE = 0.022F;
    private static final float COLD_OMEGA = HotPotBlockEntity.BOB_COLD_OMEGA;
    private static final float COLD_AMPLITUDE = 0.004F;
    private static final float COLD_FLOAT = 0.009F;
    private static final float STANDALONE_SURFACE = (float) HotPotInteraction.STANDALONE_SURFACE;
    private static final float STANDALONE_FLOOR = (float) HotPotInteraction.STANDALONE_FLOOR;
    private static final float STOVE_SURFACE = (float) HotPotInteraction.STOVE_SURFACE;
    private static final float STOVE_FLOOR = (float) HotPotInteraction.STOVE_FLOOR;

    /** 漂在汤面上的食材大小（物品模型的倍率）。 */
    private static final float FOOD_SCALE = 0.24F;

    private final RenderItems itemRenderer;
    private final int[][] cellSlots = new int[9][HotPotBlockEntity.SLOTS];
    private final int[] cellSlotCount = new int[9];

    private static final ConcurrentHashMap<Identifier, Identifier> STILL_TEXTURES =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Identifier, Identifier> BOILING_TEXTURES =
        new ConcurrentHashMap<>();

    /** 九宫格锅整面沸腾叠层（按方块状态里的汤面种类；NONE 与 OTHER 不走静态叠层数组）。 */
    private static final Identifier[] OVERLAY = new Identifier[SoupVisual.values().length];
    private static final Identifier CLEAR_STILL = HotPot.id("block/hot_pot_top_clear");
    private static final Identifier CLEAR_BOILING = HotPot.id("block/hot_pot_top_clear_boiling");
    private static final int UNKNOWN_SOUP_COLOR = 0xFF8A8094;
    private static final int UNKNOWN_BOIL_COLOR = 0xFFB8AEC8;

    private record SoupSurface(TextureAtlasSprite sprite, int argb) { }

    static {
        for (SoupVisual visual : SoupVisual.values()) {
            if (visual != SoupVisual.NONE && visual != SoupVisual.OTHER) {
                OVERLAY[visual.ordinal()] = HotPot.id("block/hot_pot_top_" + visual.getSerializedName() + "_boiling");
            }
        }
    }

    /** 汤面静态贴图（textures/block/hot_pot_top_&lt;名字&gt;.png）。 */
    private static Identifier stillTexture(SoupDef soup) {
        return STILL_TEXTURES.computeIfAbsent(soup.id(),
            id -> Identifier.fromNamespaceAndPath(id.getNamespace(), "block/hot_pot_top_" + id.getPath()));
    }

    /** 汤面沸腾贴图（…_boiling.png）。 */
    private static Identifier boilingTexture(SoupDef soup) {
        return BOILING_TEXTURES.computeIfAbsent(soup.id(),
            id -> Identifier.fromNamespaceAndPath(id.getNamespace(), "block/hot_pot_top_" + id.getPath() + "_boiling"));
    }

    /** 若自定义汤面贴图已进方块图集则直接原色使用，否则按清汤基底+定义底色染色。 */
    private static SoupSurface stillSurface(SoupDef soup) {
        if (soup != null) {
            TextureAtlasSprite custom = sprite(stillTexture(soup));
            if (!custom.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                return new SoupSurface(custom, 0xFFFFFFFF);
            }
            return new SoupSurface(sprite(CLEAR_STILL), soup.screenBase());
        }
        return new SoupSurface(sprite(CLEAR_STILL), UNKNOWN_SOUP_COLOR);
    }

    private static SoupSurface boilingSurface(SoupDef soup) {
        if (soup != null) {
            TextureAtlasSprite custom = sprite(boilingTexture(soup));
            if (!custom.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                return new SoupSurface(custom, 0xFFFFFFFF);
            }
            return new SoupSurface(sprite(CLEAR_BOILING), soup.bubbleBody());
        }
        return new SoupSurface(sprite(CLEAR_BOILING), UNKNOWN_BOIL_COLOR);
    }

    public HotPotBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = RenderItems.INSTANCE;
    }

    @Override
    protected void extractFrame(T be, float tickDelta, PoseStack matrices,
                                RenderFrame vertexConsumers, int light, int overlay) {
        if (!be.hasPot() || be.getLevel() == null) {
            return;
        }

        boolean onStove = StoveBlock.isStove(be.getBlockState());
        float lift = StoveBlock.liftOf(be.getBlockState());
        float innerMin = (float) (onStove
            ? HotPotInteraction.STOVE_INNER_MIN : HotPotInteraction.STANDALONE_INNER_MIN);
        float innerSize = (float) (onStove
            ? HotPotInteraction.STOVE_INNER_SIZE : HotPotInteraction.STANDALONE_INNER_SIZE);
        float soupY = lift + (onStove ? STOVE_SURFACE : STANDALONE_SURFACE);
        float floorY = lift + (onStove ? STOVE_FLOOR : STANDALONE_FLOOR);
        float surfaceY = be.hasSoup() ? soupY : floorY;
        float heat = be.clientHeat();
        double ticks = be.getLevel().getGameTime() + (double) tickDelta;

        PotLayout layout = be.layout();
        boolean rotated = HotPotInteraction.rotated(be.getBlockState());
        if (layout.multiSoup()) {
            renderSideSoups(be, layout, rotated, soupY, innerMin, innerSize, heat, tickDelta, matrices, vertexConsumers, light);
        } else if (be.hasSoup()) {
            SoupDef singleSoup = Soups.byIndex(be.base());
            SoupVisual visual = be.soupVisual();
            BlockState state = be.getBlockState();
            boolean blockIsOther = (state.hasProperty(HotPotBlock.SOUP) && state.getValue(HotPotBlock.SOUP) == SoupVisual.OTHER)
                || (state.hasProperty(StoveBlock.SOUP) && state.getValue(StoveBlock.SOUP) == SoupVisual.OTHER);
            if (visual == SoupVisual.OTHER || blockIsOther) {
                SoupSurface still = stillSurface(singleSoup);
                renderFullSurface(still.sprite(), still.argb(), 255, surfaceY, innerMin, innerSize,
                    vertexConsumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
            }
            if (heat > 0.01F) {
                if (visual == SoupVisual.OTHER || blockIsOther) {
                    SoupSurface boiling = boilingSurface(singleSoup);
                    renderFullSurface(boiling.sprite(), boiling.argb(), Math.max(0, Math.min(255, Math.round(heat * 210F))),
                        surfaceY + 0.001F, innerMin, innerSize,
                        vertexConsumers.getBuffer(RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
                } else {
                    renderBoilingOverlay(visual, surfaceY + 0.001F, innerMin, innerSize,
                        heat, matrices, vertexConsumers, light);
                }
                BoilingEffects.render(singleSoup, surfaceY + 0.002F, innerMin, innerSize, heat,
                    be.bubbleSeconds(tickDelta), be.getBlockPos().hashCode() & 0x3FF, matrices, vertexConsumers, light);
            }
        }

        collectSlots(be, layout);
        for (int bigCell = 0; bigCell < 9; bigCell++) {
            int n = cellSlotCount[bigCell];
            if (n == 0) {
                continue;
            }
            int side = layout.sideOfCell(bigCell);
            if (side == PotLayout.NONE) {
                continue;
            }
            boolean cellSoup = be.baseOfSide(side) != SoupIds.EMPTY;
            float cellY = cellSoup ? soupY : floorY;
            double[] cc = layout.cellCenter(bigCell);
            double[] center = PotLayout.toWorld(cc[0], cc[1], rotated && layout.multiSoup());
            float phase = bigCell * 1.37F;
            double angle = OMEGA * ticks + phase;
            double coldAngle = COLD_OMEGA * ticks + phase;
            float cold = cellSoup ? 1F - heat : 0F;

            double cycle = displayCycle(be, side, bigCell, tickDelta);
            int slot = cellSlots[bigCell][Math.floorMod((int) Math.floor(cycle), n)];
            ItemStack stack = be.getItem(slot);
            float dip = 0F;
            if (n > 1) {
                double f = cycle - Math.floor(cycle);
                double edge = Math.min(f, 1 - f);
                if (edge < 0.15) {
                    dip = (float) Math.pow(1 - edge / 0.15, 2);
                }
            }

            float yOffset = AMPLITUDE * heat * (float) Math.sin(angle)
                + COLD_AMPLITUDE * cold * (float) Math.sin(coldAngle);
            float drift = 0.006F * heat * (float) Math.cos(OMEGA * 0.7F * ticks + phase);
            float tilt = 3.5F * heat * (float) Math.sin(OMEGA * 0.55F * ticks + phase)
                + 1.0F * cold * (float) Math.sin(COLD_OMEGA * 0.6F * ticks + phase);
            float x = innerMin + innerSize * (float) center[0] + drift;
            float z = innerMin + innerSize * (float) center[1] - drift;
            float submerged = (cellSoup ? 0.007F * (1F - cold) - COLD_FLOAT * cold : -0.003F) + 0.03F * dip;
            float scale = FOOD_SCALE;

            matrices.pushPose();
            matrices.translate(x, cellY - submerged + yOffset, z);
            matrices.mulPose(Axis.YP.rotationDegrees((slot * 47) % 360));
            matrices.mulPose(Axis.XP.rotationDegrees(90.0F + tilt));
            matrices.mulPose(Axis.ZP.rotationDegrees(tilt * 0.55F));
            matrices.scale(scale, scale, scale);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, matrices, vertexConsumers,
                be.getLevel(), be.getBlockPos().hashCode() + slot);
            matrices.popPose();
        }
    }

    private static void renderSideSoups(HotPotBlockEntity be, PotLayout layout, boolean rotated, float y,
                                        float innerMin, float innerSize, float heat, float tickDelta,
                                        PoseStack matrices, RenderFrame consumers, int light) {
        for (int side = 0; side < layout.sides(); side++) {
            int baseId = be.baseOfSide(side);
            if (baseId == SoupIds.EMPTY) {
                continue;
            }
            SoupDef soup = Soups.byIndex(baseId);
            List<float[]> regions = layout.regions(side);
            SoupSurface still = stillSurface(soup);
            drawRegions(regions, rotated, y, innerMin, innerSize, still.sprite(), still.argb(), 255,
                consumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
            if (heat > 0.01F) {
                SoupSurface boiling = boilingSurface(soup);
                drawRegions(regions, rotated, y + 0.001F, innerMin, innerSize, boiling.sprite(), boiling.argb(),
                    Math.max(0, Math.min(255, Math.round(heat * 210F))),
                    consumers.getBuffer(RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
                final int s = side;
                BoilingEffects.render(soup, y + 0.002F, innerMin, innerSize, heat,
                    be.bubbleSeconds(tickDelta), (be.getBlockPos().hashCode() + side * 97) & 0x3FF, matrices, consumers, light,
                    (bx, bz) -> {
                        double[] m = PotLayout.toModel((bx - innerMin) / innerSize, (bz - innerMin) / innerSize, rotated);
                        return layout.sideAt(m[0], m[1]) == s;
                    });
            }
        }
    }

    /** 4F-3（mryt233）：通用汤碗（桌上或手持）的汤面平面，对齐 bowl_b_soup_*.json 的 [5.5..10.5] 与 y=2.51。 */
    public static void renderBowlSurface(SoupDef soup, PoseStack matrices, RenderFrame consumers, int light) {
        SoupSurface surface = stillSurface(soup);
        TextureAtlasSprite sprite = surface.sprite();
        float min = 5.5F / 16F;
        float max = 10.5F / 16F;
        float y = 2.51F / 16F;
        float u0 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * min;
        float u1 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * max;
        float v0 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * min;
        float v1 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * max;
        VertexConsumer vertices = consumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose entry = matrices.last();
        addSoupVertex(vertices, entry, min, y, max, u0, v1, surface.argb(), 255, light);
        addSoupVertex(vertices, entry, max, y, max, u1, v1, surface.argb(), 255, light);
        addSoupVertex(vertices, entry, max, y, min, u1, v0, surface.argb(), 255, light);
        addSoupVertex(vertices, entry, min, y, min, u0, v0, surface.argb(), 255, light);
    }

    /** 鸳鸯 / 田字锅的物品图标按物品里存的每边汤底画汤面。 */
    public static void renderItemSoups(HotPotMaterial material,
                                       CompoundTag data, PoseStack matrices,
                                       RenderFrame consumers, int light) {
        PotLayout layout = material.layout();
        for (int side = 0; side < layout.sides(); side++) {
            String rawId = SoupIds.fromPotTag(data, side);
            if (rawId.isEmpty()) {
                continue;
            }
            SoupSurface still = stillSurface(Soups.get(rawId));
            drawRegions(layout.regions(side), false, STANDALONE_SURFACE,
                (float) HotPotInteraction.STANDALONE_INNER_MIN, (float) HotPotInteraction.STANDALONE_INNER_SIZE,
                still.sprite(), still.argb(), 255,
                consumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
        }
    }

    private static void drawRegions(List<float[]> regions, boolean rotated, float y, float innerMin, float innerSize,
                                    TextureAtlasSprite sprite, int color, int alpha,
                                    VertexConsumer vertices, PoseStack matrices, int light) {
        PoseStack.Pose entry = matrices.last();
        float du = sprite.getU1() - sprite.getU0();
        float dv = sprite.getV1() - sprite.getV0();
        for (float[] r : regions) {
            float[][] corners = {{r[0], r[3]}, {r[2], r[3]}, {r[2], r[1]}, {r[0], r[1]}};
            for (float[] c : corners) {
                double[] w = PotLayout.toWorld(c[0], c[1], rotated);
                addSoupVertex(vertices, entry, innerMin + innerSize * (float) w[0], y, innerMin + innerSize * (float) w[1],
                    sprite.getU0() + du * c[0], sprite.getV0() + dv * c[1], color, alpha, light);
            }
        }
    }

    private static double displayCycle(HotPotBlockEntity be, int side, int bigCell, float partial) {
        double angle = be.cycleAngle(side, partial) + bigCell * 1.37F;
        return (angle - 1.5 * Math.PI) / (2 * Math.PI);
    }

    public static int shownSlot(HotPotBlockEntity be, int bigCell, float partial) {
        PotLayout layout = be.layout();
        if (bigCell < 0 || bigCell > 8 || !layout.usesCell(bigCell)) {
            return -1;
        }
        int side = layout.sideOfCell(bigCell);
        if (side == PotLayout.NONE) {
            return -1;
        }
        int[][] slots = SLOT_SCRATCH.get();
        int[] counts = COUNT_SCRATCH.get();
        collectSlots(be, layout, slots, counts);
        int n = counts[bigCell];
        if (n == 0) {
            return -1;
        }
        return slots[bigCell][Math.floorMod((int) Math.floor(displayCycle(be, side, bigCell, partial)), n)];
    }

    private static final ThreadLocal<int[][]> SLOT_SCRATCH = ThreadLocal.withInitial(
        () -> new int[9][HotPotBlockEntity.SLOTS]);
    private static final ThreadLocal<int[]> COUNT_SCRATCH = ThreadLocal.withInitial(() -> new int[9]);

    private static void collectSlots(HotPotBlockEntity be, PotLayout layout, int[][] slots, int[] counts) {
        Arrays.fill(counts, 0);
        boolean multi = layout.multiSoup();
        for (int slot = 0; slot < HotPotBlockEntity.SLOTS; slot++) {
            if (be.getItem(slot).isEmpty()) {
                continue;
            }
            int cell = multi ? layout.spotOfSlot(slot) : PotLayout.cellOfSlot(slot);
            if (cell < 0 || cell > 8) {
                continue;
            }
            slots[cell][counts[cell]++] = slot;
        }
    }

    private void collectSlots(HotPotBlockEntity be, PotLayout layout) {
        collectSlots(be, layout, cellSlots, cellSlotCount);
    }

    private static void renderBoilingOverlay(SoupVisual soup, float y, float innerMin, float innerSize,
                                              float heat, PoseStack matrices,
                                              RenderFrame vertexConsumers, int light) {
        if (!soup.hasSoup() || OVERLAY[soup.ordinal()] == null) {
            return;
        }
        TextureAtlasSprite sprite = sprite(OVERLAY[soup.ordinal()]);
        int alpha = Math.max(0, Math.min(255, Math.round(heat * 210F)));
        renderFullSurface(sprite, 0xFFFFFFFF, alpha, y, innerMin, innerSize,
            vertexConsumers.getBuffer(RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
    }

    private static void renderFullSurface(TextureAtlasSprite sprite, int color, int alpha, float y,
                                          float innerMin, float innerSize, VertexConsumer vertices,
                                          PoseStack matrices, int light) {
        PoseStack.Pose entry = matrices.last();
        float min = innerMin;
        float max = innerMin + innerSize;
        addSoupVertex(vertices, entry, min, y, max, sprite.getU0(), sprite.getV1(), color, alpha, light);
        addSoupVertex(vertices, entry, max, y, max, sprite.getU1(), sprite.getV1(), color, alpha, light);
        addSoupVertex(vertices, entry, max, y, min, sprite.getU1(), sprite.getV0(), color, alpha, light);
        addSoupVertex(vertices, entry, min, y, min, sprite.getU0(), sprite.getV0(), color, alpha, light);
    }

    private static void addSoupVertex(VertexConsumer vertices, PoseStack.Pose entry,
                                      float x, float y, float z, float u, float v,
                                      int color, int alpha, int light) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        vertices.addVertex(entry, x, y, z)
            .setColor(r, g, b, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(entry, 0F, 1F, 0F);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
