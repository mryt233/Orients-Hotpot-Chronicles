// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.orient.framework.client.RenderFrame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.resources.Identifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** 动态物品保留显示上下文，在模型提取阶段构造独立的绘制快照。 */
public final class DynamicItemModel implements ItemModel {
    private static final Map<Item, ClientItemRenderers.Renderer> RENDERERS = new IdentityHashMap<>();
    private static final Vector3fc[] EXTENTS = {new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)};
    private static final FrameRenderer FRAME_RENDERER = new FrameRenderer();
    private final Matrix4fc transform;
    /** 基础模型里的显示变换（物品栏 / 手持 / 地上的角度和大小）；没给 base 时为 null。 */
    private final ModelRenderProperties properties;

    private DynamicItemModel(Matrix4fc transform, ModelRenderProperties properties) {
        this.transform = new Matrix4f(transform);
        this.properties = properties;
    }

    public static void install() { ClientItemRenderers.install(RENDERERS::put); }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext context, ClientLevel level, ItemOwner owner, int seed) {
        var renderer = RENDERERS.get(item.getItem());
        if (renderer == null) { return; }
        RenderFrame frame = new RenderFrame();
        renderer.render(item.copy(), context, new PoseStack(), frame, 0, 0);
        output.setAnimated();
        output.appendModelIdentityElement(this);
        var layer = output.newLayer();
        layer.setLocalTransform(transform);
        layer.setUsesBlockLight(false);
        layer.setExtents(() -> EXTENTS);
        layer.setupSpecialModel(FRAME_RENDERER, frame);
        // mryt233 改：和原版 SpecialModelWrapper 一样，按显示场景套用基础模型的 display 变换。
        // 以前漏了这一步，鸳鸯锅、装菜的盘子等在物品栏里只剩一条扁平的方块（1.21.1 的 BEWLR 是按模型 display 摆的）。
        if (properties != null) { properties.applyToLayer(layer, context); }
    }

    /** base：带 display 变换的原物品模型（1.21.1 时父模型是 builtin/entity 的那个），由 tools/gen_item_definitions.py 写入。 */
    public record Unbaked(Optional<Identifier> base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.optionalFieldOf("base").forGetter(Unbaked::base)).apply(i, Unbaked::new));
        @Override public MapCodec<Unbaked> type() { return CODEC; }
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            ModelRenderProperties properties = base.map(id -> {
                var baker = context.blockModelBaker();
                var model = baker.getModel(id);
                return ModelRenderProperties.fromResolvedModel(baker, model, model.getTopTextureSlots());
            }).orElse(null);
            return new DynamicItemModel(transform, properties);
        }
        @Override public void resolveDependencies(ResolvableModel.Resolver resolver) { base.ifPresent(resolver::markDependency); }
    }

    private static final class FrameRenderer implements SpecialModelRenderer<RenderFrame> {
        @Override public RenderFrame extractArgument(ItemStack item) { return null; }
        @Override public void getExtents(Consumer<Vector3fc> output) {
            for (Vector3fc extent : EXTENTS) { output.accept(extent); }
        }
        @Override public void submit(RenderFrame frame, PoseStack pose, SubmitNodeCollector collector,
                                      int light, int overlay, boolean foil, int outline) {
            // mryt233 改（同步 26.1 修复清单第 4 条）：模型更新阶段没有光照，用原版提交阶段的亮度与叠加层，否则夹着菜的筷子等动态物品全黑。
            if (frame != null) { frame.submit(pose, collector, light, overlay); }
        }
    }
}
