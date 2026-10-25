package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** 将后台 9x9 槽按九个大格映射到锅面，并按格子相位做水面浮动。 */
public final class HotPotBlockEntityRenderer<T extends HotPotBlockEntity>
    implements BlockEntityRenderer<T> {
    private static final float OMEGA = 0.22F;
    private static final float AMPLITUDE = 0.022F;
    // 冷锅（调整，用户报：没开锅时食材在汤面上闪、一沉一浮）：以前冷锅食材压在汤面下 0.007，
    // 和汤面几乎同高在打架；轮换食材还照沸腾的快节奏。现在冷锅时浮在汤面上方一点，很慢地上下漂（约 6 秒一个来回），
    // 随火力在冷 / 沸两套之间平滑过渡。
    private static final float COLD_OMEGA = 0.05F;
    private static final float COLD_AMPLITUDE = 0.004F;
    private static final float COLD_FLOAT = 0.009F;
    // 水面/内底高度与锅腔范围都跟着模型走：独立锅腔 1.5..14.5、汤面 y=5.6、内底 y=1.25；
    // 灶上锅坐进 7.6/16 深的锅槽，锅腔 1.837..14.163，汤面 y=14.32、内底 y=9.1。
    private static final float STANDALONE_SURFACE = 5.6F / 16.0F;
    private static final float STANDALONE_FLOOR = 1.25F / 16.0F;
    private static final float STOVE_SURFACE = 14.32F / 16.0F;
    private static final float STOVE_FLOOR = 9.1F / 16.0F;

    /** 漂在汤面上的食材大小（物品模型的倍率）。 */
    private static final float FOOD_SCALE = 0.24F;

    private final ItemRenderer itemRenderer;

    public HotPotBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!be.hasPot() || be.getWorld() == null) {
            return;
        }

        boolean onStove = cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock.isStove(be.getCachedState());
        // 高灶台：锅、汤面整体抬高
        float lift = cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock.liftOf(be.getCachedState());
        float innerMin = (float) (onStove
            ? HotPotInteraction.STOVE_INNER_MIN : HotPotInteraction.STANDALONE_INNER_MIN);
        float innerSize = (float) (onStove
            ? HotPotInteraction.STOVE_INNER_SIZE : HotPotInteraction.STANDALONE_INNER_SIZE);
        float soupY = lift + (onStove ? STOVE_SURFACE : STANDALONE_SURFACE);
        float floorY = lift + (onStove ? STOVE_FLOOR : STANDALONE_FLOOR);
        float surfaceY = be.hasSoup() ? soupY : floorY;
        float heat = be.clientHeat();
        // 用 double（调整）：世界时间从存档创建起一直累加，存成 float 时老存档里（几百万刻以上）
        // 一刻内的 tickDelta 会被精度吃掉，食材浮动 / 气泡只能每刻跳一下，看起来像锁在 20 帧
        double ticks = be.getWorld().getTime() + (double) tickDelta;
        PotLayout layout = be.layout();
        boolean rotated = HotPotInteraction.rotated(be.getCachedState());
        if (layout.multiSoup()) {
            renderSideSoups(be, layout, rotated, soupY, innerMin, innerSize, heat, ticks, matrices, vertexConsumers, light);
        } else if (be.hasSoup() && heat > 0.01F) {
            renderBoilingOverlay(be.soupVisual(), surfaceY + 0.001F, innerMin, innerSize,
                heat, matrices, vertexConsumers, light);
            // 立体气泡与滚包（大滚效果）
            BoilingEffects.render(be.soupVisual(), surfaceY + 0.002F, innerMin, innerSize, heat,
                ticks / 20.0, be.getPos().hashCode() & 0x3FF, matrices, vertexConsumers, light);
        }

        int[] slots = new int[HotPotBlockEntity.SLOTS];
        for (int bigCell = 0; bigCell < 9; bigCell++) {
            int n = visibleSlots(be, bigCell, slots);
            if (n == 0) {
                continue;
            }
            // 鸳鸯 / 田字锅：这一格那边有汤才浮在汤面上，没汤的那边沉在锅底
            int side = layout.sideOfCell(bigCell);
            if (side == PotLayout.NONE) {
                continue;
            }
            boolean cellSoup = be.baseOfSide(side) >= 0;
            float cellY = cellSoup ? soupY : floorY;
            double[] center = PotLayout.toWorld(layout.cellCenter(bigCell)[0], layout.cellCenter(bigCell)[1], rotated && layout.multiSoup());
            // 相位只跟大格有关：九个大格各自错开浮动，换食材时节奏不会跳
            float phase = bigCell * 1.37F;
            double angle = OMEGA * ticks + phase;
            double coldAngle = COLD_OMEGA * ticks + phase;
            float cold = cellSoup ? 1F - heat : 0F;

            // 一格里有几样食材就轮流显示：每个浮动周期在最低点（sin = -1）换下一样；冷锅按慢节奏换
            double cycle = ((cold > 0.5F ? coldAngle : angle) - 1.5 * Math.PI) / (2 * Math.PI);
            int slot = slots[Math.floorMod((int) Math.floor(cycle), n)];
            ItemStack stack = be.getStack(slot);
            // 换的前后一小段：当前这样沉下去、变小，下一样再冒上来
            float dip = 0F;
            if (n > 1) {
                double f = cycle - Math.floor(cycle);
                double edge = Math.min(f, 1 - f);
                if (edge < 0.15) {
                    dip = (float) Math.pow(1 - edge / 0.15, 2);
                }
            }

            // yOffset = A * sin(omega * tick + phi_grid)，九个大格相位各不相同。
            float yOffset = AMPLITUDE * heat * (float) Math.sin(angle)
                + COLD_AMPLITUDE * cold * (float) Math.sin(coldAngle);
            float drift = 0.006F * heat * (float) Math.cos(OMEGA * 0.7F * ticks + phase);
            float tilt = 3.5F * heat * (float) Math.sin(OMEGA * 0.55F * ticks + phase)
                + 1.0F * cold * (float) Math.sin(COLD_OMEGA * 0.6F * ticks + phase);
            float x = innerMin + innerSize * (float) center[0] + drift;
            float z = innerMin + innerSize * (float) center[1] - drift;
            // 有汤：沸腾时压进汤面 0.007，冷锅浮在上面 COLD_FLOAT；没汤：抬离锅底一点，免得和锅底打架
            float submerged = (cellSoup ? 0.007F * (1F - cold) - COLD_FLOAT * cold : -0.003F) + 0.03F * dip;
            float scale = FOOD_SCALE * (1F - 0.65F * dip);

            matrices.push();
            matrices.translate(x, cellY - submerged + yOffset, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((slot * 47) % 360));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F + tilt));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt * 0.55F));
            matrices.scale(scale, scale, scale);
            itemRenderer.renderItem(stack, ModelTransformationMode.FIXED,
                light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers,
                be.getWorld(), be.getPos().hashCode() + slot);
            matrices.pop();
        }
    }

    /**
     * 鸳鸯 / 田字锅的汤面（新增）：方块模型里只有锅身和隔板，各边的汤面在这里按 {@link PotLayout#regions}
     * 一块块画，贴图按汤底名取（hot_pot_top_&lt;汤底&gt;，沸腾时叠 _boiling），所以任何汤底、以后新加的汤底都能放任何一边。
     * 沸腾的立体气泡也只冒在各自那边。
     */
    private static void renderSideSoups(HotPotBlockEntity be, PotLayout layout, boolean rotated, float y,
                                        float innerMin, float innerSize, float heat, double ticks,
                                        MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        for (int side = 0; side < layout.sides(); side++) {
            SoupBase soup = SoupBase.byId(be.baseOfSide(side));
            if (soup == null) {
                continue;
            }
            List<float[]> regions = layout.regions(side);
            Sprite still = sprite("block/hot_pot_top_" + soup.textureKey());
            drawRegions(regions, rotated, y, innerMin, innerSize, still, 255,
                consumers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)), matrices, light);
            if (heat > 0.01F) {
                Sprite boiling = sprite("block/hot_pot_top_" + soup.textureKey() + "_boiling");
                drawRegions(regions, rotated, y + 0.001F, innerMin, innerSize, boiling,
                    Math.max(0, Math.min(255, Math.round(heat * 210F))),
                    consumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)), matrices, light);
                final int s = side;
                BoilingEffects.render(SoupVisual.fromBase(soup), y + 0.002F, innerMin, innerSize, heat,
                    ticks / 20.0, (be.getPos().hashCode() + side * 97) & 0x3FF, matrices, consumers, light,
                    (bx, bz) -> {
                        double[] m = PotLayout.toModel((bx - innerMin) / innerSize, (bz - innerMin) / innerSize, rotated);
                        return layout.sideAt(m[0], m[1]) == s;
                    });
            }
        }
    }

    /**
     * 鸳鸯 / 田字锅的物品图标（收起来的锅、掉在地上的锅）：锅身由方块模型画，这里按物品里存的每边汤底画汤面，
     * 和摆在地上时同一套画法，所以任意汤底组合、以后新加的汤底都对（新增，用户 2026-10-24 报：收起来的新锅图标没汤）。
     * 坐标是方块坐标 0..1（独立锅：汤面 5.6 像素高、锅腔 1.5..14.5）。
     */
    public static void renderItemSoups(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial material,
                                       net.minecraft.nbt.NbtCompound data, MatrixStack matrices,
                                       VertexConsumerProvider consumers, int light) {
        PotLayout layout = material.layout();
        String[] keys = {"Base", "BaseB", "BaseC", "BaseD"};
        for (int side = 0; side < layout.sides(); side++) {
            SoupBase soup = data.contains(keys[side]) ? SoupBase.byId(data.getInt(keys[side])) : null;
            if (soup == null) {
                continue;
            }
            drawRegions(layout.regions(side), false, STANDALONE_SURFACE,
                (float) HotPotInteraction.STANDALONE_INNER_MIN, (float) HotPotInteraction.STANDALONE_INNER_SIZE,
                sprite("block/hot_pot_top_" + soup.textureKey()), 255,
                consumers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)), matrices, light);
        }
    }

    private static Sprite sprite(String path) {
        return MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(GeneratedMod.id(path));
    }

    /** 一边汤面的每一块：模型坐标的长方形换到世界（锅转了 90° 也跟着转），贴图按模型坐标取，和整张汤面贴图对得上。 */
    private static void drawRegions(List<float[]> regions, boolean rotated, float y, float innerMin, float innerSize,
                                    Sprite sprite, int alpha, VertexConsumer vertices, MatrixStack matrices, int light) {
        MatrixStack.Entry entry = matrices.peek();
        float du = sprite.getMaxU() - sprite.getMinU();
        float dv = sprite.getMaxV() - sprite.getMinV();
        for (float[] r : regions) {
            // 逆时针（从上往下看）：(u0,v1) (u1,v1) (u1,v0) (u0,v0)，同九宫格汤面的顶点顺序
            float[][] corners = {{r[0], r[3]}, {r[2], r[3]}, {r[2], r[1]}, {r[0], r[1]}};
            for (float[] c : corners) {
                double[] w = PotLayout.toWorld(c[0], c[1], rotated);
                addSoupVertex(vertices, entry, innerMin + innerSize * (float) w[0], y, innerMin + innerSize * (float) w[1],
                    sprite.getMinU() + du * c[0], sprite.getMinV() + dv * c[1], alpha, light);
            }
        }
    }

    /** 大格里有东西的小格，按行优先写进 out，返回个数。 */
    private static int visibleSlots(HotPotBlockEntity be, int bigCell, int[] out) {
        PotLayout layout = be.layout();
        if (layout.multiSoup()) {
            // 鸳鸯 / 田字锅：归到这个位置的小格（PotLayout.spotOfSlot），按格号顺序轮流显示
            int n = 0;
            for (int slot = 0; slot < HotPotBlockEntity.SLOTS; slot++) {
                if (layout.spotOfSlot(slot) == bigCell && !be.getStack(slot).isEmpty()) {
                    out[n++] = slot;
                }
            }
            return n;
        }
        int baseRow = (bigCell / 3) * 3;
        int baseCol = (bigCell % 3) * 3;
        int n = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = (baseRow + row) * 9 + baseCol + col;
                if (!be.getStack(slot).isEmpty()) {
                    out[n++] = slot;
                }
            }
        }
        return n;
    }

    /**
     * 燃烧时叠加动画精灵；clientHeat 下降时透明度随之衰减，视觉上平滑回到静态汤面。
     */
    private static void renderBoilingOverlay(SoupVisual soup, float y, float innerMin, float innerSize,
                                              float heat, MatrixStack matrices,
                                              VertexConsumerProvider vertexConsumers, int light) {
        if (!soup.hasSoup()) {
            return;
        }
        Identifier texture = GeneratedMod.id("block/hot_pot_top_" + soup.asString() + "_boiling");
        Sprite sprite = MinecraftClient.getInstance()
            .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(texture);
        VertexConsumer vertices = vertexConsumers.getBuffer(
            RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        int alpha = Math.max(0, Math.min(255, Math.round(heat * 210F)));
        float min = innerMin;
        float max = innerMin + innerSize;
        addSoupVertex(vertices, entry, min, y, max, sprite.getMinU(), sprite.getMaxV(), alpha, light);
        addSoupVertex(vertices, entry, max, y, max, sprite.getMaxU(), sprite.getMaxV(), alpha, light);
        addSoupVertex(vertices, entry, max, y, min, sprite.getMaxU(), sprite.getMinV(), alpha, light);
        addSoupVertex(vertices, entry, min, y, min, sprite.getMinU(), sprite.getMinV(), alpha, light);
    }

    private static void addSoupVertex(VertexConsumer vertices, MatrixStack.Entry entry,
                                      float x, float y, float z, float u, float v,
                                      int alpha, int light) {
        vertices.vertex(entry, x, y, z)
            .color(255, 255, 255, alpha)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, 0F, 1F, 0F);
    }

    @Override
    public int getRenderDistance() {
        return 48;
    }
}
