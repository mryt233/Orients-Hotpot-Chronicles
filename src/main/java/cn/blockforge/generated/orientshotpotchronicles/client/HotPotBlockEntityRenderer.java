package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
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
        float surfaceY = lift + (onStove
            ? (be.hasSoup() ? STOVE_SURFACE : STOVE_FLOOR)
            : (be.hasSoup() ? STANDALONE_SURFACE : STANDALONE_FLOOR));
        float heat = be.clientHeat();
        // 用 double：世界时间从存档创建起一直累加，存成 float 时老存档里（几百万刻以上）
        // 一刻内的 tickDelta 会被精度吃掉，食材浮动 / 气泡只能每刻跳一下，看起来像锁在 20 帧
        double ticks = be.getWorld().getTime() + (double) tickDelta;
        if (be.hasSoup() && heat > 0.01F) {
            renderBoilingOverlay(be.soupVisual(), surfaceY + 0.001F, innerMin, innerSize,
                heat, matrices, vertexConsumers, light);
            // 立体气泡与滚包（大滚效果）
            BoilingEffects.render(be.soupVisual(), surfaceY + 0.002F, innerMin, innerSize, heat,
                ticks / 20.0, be.getPos().hashCode() & 0x3FF, matrices, vertexConsumers, light);
        }

        int[] slots = new int[9];
        for (int bigCell = 0; bigCell < 9; bigCell++) {
            int n = visibleSlots(be, bigCell, slots);
            if (n == 0) {
                continue;
            }
            int cellCol = bigCell % 3;
            int cellRow = bigCell / 3;
            // 相位只跟大格有关：九个大格各自错开浮动，换食材时节奏不会跳
            float phase = bigCell * 1.37F;
            double angle = OMEGA * ticks + phase;

            // 一格里有几样食材就轮流显示：每个浮动周期在最低点（sin = -1）换下一样
            double cycle = (angle - 1.5 * Math.PI) / (2 * Math.PI);
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
            float yOffset = AMPLITUDE * heat * (float) Math.sin(angle);
            float drift = 0.006F * heat * (float) Math.cos(OMEGA * 0.7F * ticks + phase);
            float tilt = 3.5F * heat * (float) Math.sin(OMEGA * 0.55F * ticks + phase);
            float x = innerMin + innerSize * (cellCol + 0.5F) / 3.0F + drift;
            float z = innerMin + innerSize * (cellRow + 0.5F) / 3.0F - drift;
            float submerged = (be.hasSoup() ? 0.007F : 0F) + 0.03F * dip;
            float scale = FOOD_SCALE * (1F - 0.65F * dip);

            matrices.push();
            matrices.translate(x, surfaceY - submerged + yOffset, z);
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

    /** 大格里有东西的小格，按行优先写进 out，返回个数。 */
    private static int visibleSlots(HotPotBlockEntity be, int bigCell, int[] out) {
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
