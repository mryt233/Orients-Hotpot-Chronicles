// 作者：mryt233
package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** 提取阶段冻结几何与模型，提交阶段只消费快照，不再访问世界和方块实体。 */
public final class RenderFrame {
    private interface Command { void submit(PoseStack pose, SubmitNodeCollector collector, int dynamicLight, int dynamicOverlay); }
    private final List<Command> commands = new ArrayList<>();

    public VertexConsumer getBuffer(RenderType type) {
        Geometry geometry = new Geometry(type);
        commands.add(geometry);
        return geometry;
    }

    public void item(ItemStack stack, ItemDisplayContext mode, int light, int overlay,
                     PoseStack pose, Level level, int seed) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, mode, level, null, seed);
        Matrix4f transform = new Matrix4f(pose.last().pose());
        commands.add((root, collector, dynamicLight, dynamicOverlay) -> {
            root.pushPose();
            root.mulPose(transform);
            state.submit(root, collector, dynamicLight < 0 ? light : dynamicLight,
                dynamicOverlay < 0 ? overlay : dynamicOverlay, 0);
            root.popPose();
        });
    }

    public void block(BlockState block, PoseStack pose, int light, int overlay) {
        BlockModelRenderState state = new BlockModelRenderState();
        new net.minecraft.client.renderer.block.BlockModelResolver(Minecraft.getInstance().getModelManager()).update(state, block, BlockDisplayContext.create());
        Matrix4f transform = new Matrix4f(pose.last().pose());
        commands.add((root, collector, dynamicLight, dynamicOverlay) -> {
            root.pushPose();
            root.mulPose(transform);
            state.submit(root, collector, dynamicLight < 0 ? light : dynamicLight,
                dynamicOverlay < 0 ? overlay : dynamicOverlay, 0);
            root.popPose();
        });
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector) {
        submit(pose, collector, -1, -1);
    }

    /** mryt233 改：动态物品提交时补入原版计算的光照，方块实体仍沿用提取值。 */
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        for (Command command : commands) { command.submit(pose, collector, light, overlay); }
    }

    private static final class Vertex {
        float x, y, z, u, v, nx, ny = 1, nz, width = 1;
        int color = -1, overlay, light;
    }

    private static final class Geometry implements VertexConsumer, Command {
        private final RenderType type;
        private final List<Vertex> vertices = new ArrayList<>();
        private Vertex current;
        private Geometry(RenderType type) { this.type = type; }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex();
            current.x = x; current.y = y; current.z = z;
            vertices.add(current);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            current.color = a << 24 | r << 16 | g << 8 | b;
            return this;
        }
        @Override public VertexConsumer setColor(int color) { current.color = color; return this; }
        @Override public VertexConsumer setUv(float u, float v) { current.u = u; current.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { current.overlay = u | v << 16; return this; }
        @Override public VertexConsumer setUv2(int u, int v) { current.light = u | v << 16; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            current.nx = x; current.ny = y; current.nz = z; return this;
        }
        @Override public VertexConsumer setLineWidth(float width) { current.width = width; return this; }

        @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int dynamicLight, int dynamicOverlay) {
            if (vertices.isEmpty()) { return; }
            collector.submitCustomGeometry(pose, type, (transform, out) -> {
                for (Vertex vertex : vertices) {
                    out.addVertex(transform, vertex.x, vertex.y, vertex.z).setColor(vertex.color)
                        .setUv(vertex.u, vertex.v).setOverlay(dynamicOverlay < 0 ? vertex.overlay : dynamicOverlay)
                        .setLight(dynamicLight < 0 ? vertex.light : dynamicLight)
                        .setNormal(transform, vertex.nx, vertex.ny, vertex.nz).setLineWidth(vertex.width);
                }
            });
        }
    }
}
