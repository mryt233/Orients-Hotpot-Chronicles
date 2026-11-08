// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PotLayout;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.blockforge.orient.framework.client.RenderFrame;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import cn.blockforge.generated.orientshotpotchronicles.client.RenderItems;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import com.mojang.math.Axis;

/** 将后台 9x9 槽按九个大格映射到锅面，并按格子相位做水面浮动。 */
public final class HotPotBlockEntityRenderer<T extends HotPotBlockEntity>
    extends cn.blockforge.orient.framework.client.FrameBlockEntityRenderer<T> {
    private static final float OMEGA = HotPotBlockEntity.BOB_OMEGA;
    private static final float AMPLITUDE = 0.022F;
    // 冷锅（用户报：没开锅时食材在汤面上闪、一沉一浮）：以前冷锅食材压在汤面下 0.007，
    // 和汤面几乎同高在打架；轮换食材还照沸腾的快节奏。现在冷锅时浮在汤面上方一点，很慢地上下漂（约 6 秒一个来回），
    // 随火力在冷 / 沸两套之间平滑过渡。
    private static final float COLD_OMEGA = HotPotBlockEntity.BOB_COLD_OMEGA;
    private static final float COLD_AMPLITUDE = 0.004F;
    private static final float COLD_FLOAT = 0.009F;
    // 水面/内底高度与锅腔范围都跟着模型走：独立锅腔 1.5..14.5、汤面 y=5.6、内底 y=1.25；
    // 灶上锅坐进 7.6/16 深的锅槽，锅腔 1.837..14.163，汤面 y=14.32、内底 y=9.1。
    // mryt233 改（2026-11-04）：汤面高度改用 HotPotInteraction 里那份（服务端算准星格子也用同一个值，别各写一个数）。
    private static final float STANDALONE_SURFACE = (float) cn.blockforge.generated.orientshotpotchronicles
        .hotpot.HotPotInteraction.STANDALONE_SURFACE;
    private static final float STANDALONE_FLOOR = (float) cn.blockforge.generated.orientshotpotchronicles
        .hotpot.HotPotInteraction.STANDALONE_FLOOR;
    private static final float STOVE_SURFACE = (float) cn.blockforge.generated.orientshotpotchronicles
        .hotpot.HotPotInteraction.STOVE_SURFACE;
    private static final float STOVE_FLOOR = (float) cn.blockforge.generated.orientshotpotchronicles
        .hotpot.HotPotInteraction.STOVE_FLOOR;

    /** 漂在汤面上的食材大小（物品模型的倍率）。 */
    private static final float FOOD_SCALE = 0.24F;

    private final RenderItems itemRenderer;
    /**
     * 每帧复用的格子缓冲（渲染与右键瞄准都只在客户端主线程跑）：[大格][这一格里的第几个小格]。
     * mryt233 改：以前每帧对 9 个大格各扫一遍 81 格（多汤锅 9 × 81 = 729 次 getItem），现在一次扫完。
     */
    private final int[][] cellSlots = new int[9][HotPotBlockEntity.SLOTS];
    private final int[] cellSlotCount = new int[9];
    /** 汤面贴图的 ID 按汤底预先拼好，不再每帧拼字符串（代码检查 W5）。 */
    private static final Identifier[] STILL = new Identifier[SoupBase.values().length];
    private static final Identifier[] BOILING = new Identifier[SoupBase.values().length];

    /** 九宫格锅整面沸腾叠层（按方块状态里的汤面种类）。 */
    private static final Identifier[] OVERLAY = new Identifier[SoupVisual.values().length];

    static {
        for (SoupVisual visual : SoupVisual.values()) {
            OVERLAY[visual.ordinal()] = HotPot.id("block/hot_pot_top_" + visual.getSerializedName() + "_boiling");
        }
        for (SoupBase soup : SoupBase.values()) {
            STILL[soup.ordinal()] = HotPot.id("block/hot_pot_top_" + soup.textureKey());
            BOILING[soup.ordinal()] = HotPot.id("block/hot_pot_top_" + soup.textureKey() + "_boiling");
        }
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

        boolean onStove = cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock.isStove(be.getBlockState());
        // 高灶台：锅、汤面整体抬高
        float lift = cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock.liftOf(be.getBlockState());
        float innerMin = (float) (onStove
            ? HotPotInteraction.STOVE_INNER_MIN : HotPotInteraction.STANDALONE_INNER_MIN);
        float innerSize = (float) (onStove
            ? HotPotInteraction.STOVE_INNER_SIZE : HotPotInteraction.STANDALONE_INNER_SIZE);
        float soupY = lift + (onStove ? STOVE_SURFACE : STANDALONE_SURFACE);
        float floorY = lift + (onStove ? STOVE_FLOOR : STANDALONE_FLOOR);
        float surfaceY = be.hasSoup() ? soupY : floorY;
        float heat = be.clientHeat();
        // 用 double：世界时间从存档创建起一直累加，存成 float 时老存档里（几百万刻以上）
        // 一刻内的 tickDelta 会被精度吃掉，食材浮动 / 气泡只能每刻跳一下，看起来像锁在 20 帧
        double ticks = be.getLevel().getGameTime() + (double) tickDelta;
        PotLayout layout = be.layout();
        boolean rotated = HotPotInteraction.rotated(be.getBlockState());
        if (layout.multiSoup()) {
            renderSideSoups(be, layout, rotated, soupY, innerMin, innerSize, heat, tickDelta, matrices, vertexConsumers, light);
        } else if (be.hasSoup() && heat > 0.01F) {
            renderBoilingOverlay(be.soupVisual(), surfaceY + 0.001F, innerMin, innerSize,
                heat, matrices, vertexConsumers, light);
            // 立体气泡与滚包（大滚效果）
            BoilingEffects.render(be.soupVisual(), surfaceY + 0.002F, innerMin, innerSize, heat,
                be.bubbleSeconds(tickDelta), be.getBlockPos().hashCode() & 0x3FF, matrices, vertexConsumers, light);
        }

        collectSlots(be, layout);
        for (int bigCell = 0; bigCell < 9; bigCell++) {
            int n = cellSlotCount[bigCell];
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
            double[] cc = layout.cellCenter(bigCell);   // 以前这一句里调了两次，每次都 new 一个 double[2]
            double[] center = PotLayout.toWorld(cc[0], cc[1], rotated && layout.multiSoup());
            // 相位只跟大格有关：九个大格各自错开浮动，换食材时节奏不会跳
            float phase = bigCell * 1.37F;
            double angle = OMEGA * ticks + phase;
            double coldAngle = COLD_OMEGA * ticks + phase;
            float cold = cellSoup ? 1F - heat : 0F;

            // 一格里有几样食材就轮流显示：每个浮动周期在最低点（sin = -1）换下一样；冷锅按慢节奏换
            double cycle = displayCycle(be, side, bigCell, tickDelta);
            int slot = cellSlots[bigCell][Math.floorMod((int) Math.floor(cycle), n)];
            ItemStack stack = be.getItem(slot);
            // 换的前后一小段：当前这样沉进汤里，下一样再浮上来（大小不变）
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
            // 大小始终不变（用户 2026-11-01：浮动时放大缩小不合理）；换下一样只靠上面的下沉：当前这样沉进汤面以下，下一样再浮上来
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

    /**
     * 鸳鸯 / 田字锅的汤面：方块模型里只有锅身和隔板，各边的汤面在这里按 {@link PotLayout#regions}
     * 一块块画，贴图按汤底名取（hot_pot_top_&lt;汤底&gt;，沸腾时叠 _boiling），所以任何汤底、以后新加的汤底都能放任何一边。
     * 沸腾的立体气泡也只冒在各自那边。
     */
    private static void renderSideSoups(HotPotBlockEntity be, PotLayout layout, boolean rotated, float y,
                                        float innerMin, float innerSize, float heat, float tickDelta,
                                        PoseStack matrices, RenderFrame consumers, int light) {
        for (int side = 0; side < layout.sides(); side++) {
            SoupBase soup = SoupBase.byId(be.baseOfSide(side));
            if (soup == null) {
                continue;
            }
            List<float[]> regions = layout.regions(side);
            TextureAtlasSprite still = sprite(STILL[soup.ordinal()]);
            drawRegions(regions, rotated, y, innerMin, innerSize, still, 255,
                consumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
            if (heat > 0.01F) {
                TextureAtlasSprite boiling = sprite(BOILING[soup.ordinal()]);
                drawRegions(regions, rotated, y + 0.001F, innerMin, innerSize, boiling,
                    Math.max(0, Math.min(255, Math.round(heat * 210F))),
                    consumers.getBuffer(RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
                final int s = side;
                BoilingEffects.render(SoupVisual.fromBase(soup), y + 0.002F, innerMin, innerSize, heat,
                    be.bubbleSeconds(tickDelta), (be.getBlockPos().hashCode() + side * 97) & 0x3FF, matrices, consumers, light,
                    (bx, bz) -> {
                        double[] m = PotLayout.toModel((bx - innerMin) / innerSize, (bz - innerMin) / innerSize, rotated);
                        return layout.sideAt(m[0], m[1]) == s;
                    });
            }
        }
    }

    /**
     * 鸳鸯 / 田字锅的物品图标（收起来的锅、掉在地上的锅）：锅身由方块模型画，这里按物品里存的每边汤底画汤面，
     * 和摆在地上时同一套画法，所以任意汤底组合、以后新加的汤底都对（用户 2026-10-24 报：收起来的新锅图标没汤）。
     * 坐标是方块坐标 0..1（独立锅：汤面 5.6 像素高、锅腔 1.5..14.5）。
     */
    public static void renderItemSoups(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial material,
                                       net.minecraft.nbt.CompoundTag data, PoseStack matrices,
                                       RenderFrame consumers, int light) {
        PotLayout layout = material.layout();
        String[] keys = {"Base", "BaseB", "BaseC", "BaseD"};
        for (int side = 0; side < layout.sides(); side++) {
            SoupBase soup = data.contains(keys[side]) ? SoupBase.byId(data.getIntOr(keys[side], -1)) : null;
            if (soup == null) {
                continue;
            }
            drawRegions(layout.regions(side), false, STANDALONE_SURFACE,
                (float) HotPotInteraction.STANDALONE_INNER_MIN, (float) HotPotInteraction.STANDALONE_INNER_SIZE,
                sprite(STILL[soup.ordinal()]), 255,
                consumers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS)), matrices, light);
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().get(new net.minecraft.client.resources.model.sprite.SpriteId(TextureAtlas.LOCATION_BLOCKS, id));
    }

    /** 一边汤面的每一块：模型坐标的长方形换到世界（锅转了 90° 也跟着转），贴图按模型坐标取，和整张汤面贴图对得上。 */
    private static void drawRegions(List<float[]> regions, boolean rotated, float y, float innerMin, float innerSize,
                                    TextureAtlasSprite sprite, int alpha, VertexConsumer vertices, PoseStack matrices, int light) {
        PoseStack.Pose entry = matrices.last();
        float du = sprite.getU1() - sprite.getU0();
        float dv = sprite.getV1() - sprite.getV0();
        for (float[] r : regions) {
            // 逆时针（从上往下看）：(u0,v1) (u1,v1) (u1,v0) (u0,v0)，同九宫格汤面的顶点顺序
            float[][] corners = {{r[0], r[3]}, {r[2], r[3]}, {r[2], r[1]}, {r[0], r[1]}};
            for (float[] c : corners) {
                double[] w = PotLayout.toWorld(c[0], c[1], rotated);
                addSoupVertex(vertices, entry, innerMin + innerSize * (float) w[0], y, innerMin + innerSize * (float) w[1],
                    sprite.getU0() + du * c[0], sprite.getV0() + dv * c[1], alpha, light);
            }
        }
    }

    /**
     * 轮换显示的进度：整数部分 = 轮到第几样，每个浮动周期在最低点换下一样。
     * 相位用锅自己按边累计的 {@link HotPotBlockEntity#cycleAngle}，冷 / 沸换档时连续不跳（代码检查 B4）。
     */
    private static double displayCycle(HotPotBlockEntity be, int side, int bigCell, float partial) {
        double angle = be.cycleAngle(side, partial) + bigCell * 1.37F;
        return (angle - 1.5 * Math.PI) / (2 * Math.PI);
    }

    /**
     * 这一刻这个大格浮在汤面上显示的是哪一小格（和 render 用同一套算法）；空格返回 -1。
     * 筷子直接右键锅时，客户端用它告诉服务端「我看到的是这一样」（ChopsticksAimPayload；用户 2026-10-25：
     * 看到的是牛肉，夹起来却是同一格别的东西。新增）。
     */
    public static int shownSlot(HotPotBlockEntity be, int bigCell, float partial) {
        PotLayout layout = be.layout();
        if (bigCell < 0 || bigCell > 8 || !layout.usesCell(bigCell)) {
            return -1;
        }
        int side = layout.sideOfCell(bigCell);
        if (side == PotLayout.NONE) {
            return -1;
        }
        // 和渲染共用一份「大格 → 小格」结果，不再每次右键都 new 一个 int[81]
        int[][] slots = SLOT_SCRATCH.get();
        int[] counts = COUNT_SCRATCH.get();
        collectSlots(be, layout, slots, counts);
        int n = counts[bigCell];
        if (n == 0) {
            return -1;
        }
        return slots[bigCell][Math.floorMod((int) Math.floor(displayCycle(be, side, bigCell, partial)), n)];
    }

    /** 右键瞄准用的临时缓冲（只在客户端主线程用；单独一份，不去踩渲染器实例上的状态）。 */
    private static final ThreadLocal<int[][]> SLOT_SCRATCH = ThreadLocal.withInitial(
        () -> new int[9][HotPotBlockEntity.SLOTS]);
    private static final ThreadLocal<int[]> COUNT_SCRATCH = ThreadLocal.withInitial(() -> new int[9]);

    /**
     * 一次扫完 81 格，把有东西的小格按大格分好，写进 slots / counts（渲染每帧、右键瞄准都走这一份）。
     * mryt233 改：以前按大格各扫一遍（多汤锅 9 × 81 次 getItem），现在只扫一遍。
     */
    private static void collectSlots(HotPotBlockEntity be, PotLayout layout, int[][] slots, int[] counts) {
        java.util.Arrays.fill(counts, 0);
        boolean multi = layout.multiSoup();
        for (int slot = 0; slot < HotPotBlockEntity.SLOTS; slot++) {
            if (be.getItem(slot).isEmpty()) {
                continue;
            }
            // 九宫格：81 格本来就是 9 大格各 9 格；鸳鸯 / 田字：按 spotOfSlot 归位
            int cell = multi ? layout.spotOfSlot(slot) : PotLayout.cellOfSlot(slot);
            if (cell < 0 || cell > 8) {
                continue;
            }
            slots[cell][counts[cell]++] = slot;
        }
    }

    /** 渲染用的那一份（渲染器实例上的缓冲，不分配）。 */
    private void collectSlots(HotPotBlockEntity be, PotLayout layout) {
        collectSlots(be, layout, cellSlots, cellSlotCount);
    }

    /**
     * 燃烧时叠加动画精灵；clientHeat 下降时透明度随之衰减，视觉上平滑回到静态汤面。
     */
    private static void renderBoilingOverlay(SoupVisual soup, float y, float innerMin, float innerSize,
                                              float heat, PoseStack matrices,
                                              RenderFrame vertexConsumers, int light) {
        if (!soup.hasSoup()) {
            return;
        }
        TextureAtlasSprite sprite = sprite(OVERLAY[soup.ordinal()]);
        VertexConsumer vertices = vertexConsumers.getBuffer(
            RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose entry = matrices.last();
        int alpha = Math.max(0, Math.min(255, Math.round(heat * 210F)));
        float min = innerMin;
        float max = innerMin + innerSize;
        addSoupVertex(vertices, entry, min, y, max, sprite.getU0(), sprite.getV1(), alpha, light);
        addSoupVertex(vertices, entry, max, y, max, sprite.getU1(), sprite.getV1(), alpha, light);
        addSoupVertex(vertices, entry, max, y, min, sprite.getU1(), sprite.getV0(), alpha, light);
        addSoupVertex(vertices, entry, min, y, min, sprite.getU0(), sprite.getV0(), alpha, light);
    }

    private static void addSoupVertex(VertexConsumer vertices, PoseStack.Pose entry,
                                      float x, float y, float z, float u, float v,
                                      int alpha, int light) {
        vertices.addVertex(entry, x, y, z)
            .setColor(255, 255, 255, alpha)
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
