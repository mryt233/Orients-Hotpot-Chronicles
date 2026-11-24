// 作者：mryt233
package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** 共用提取/提交生命周期；子类只在提取时读实体并保留原绘制计算。 */
public abstract class FrameBlockEntityRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T, FrameBlockEntityRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private RenderFrame frame;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(T entity, State state, float partialTicks, Vec3 camera,
                                             ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, camera, overlay);
        state.frame = new RenderFrame();
        extractFrame(entity, partialTicks, new PoseStack(), state.frame, state.lightCoords,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    protected abstract void extractFrame(T entity, float partialTicks, PoseStack pose, RenderFrame frame,
                                         int light, int overlay);

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.frame != null) { state.frame.submit(pose, collector); }
    }
}
