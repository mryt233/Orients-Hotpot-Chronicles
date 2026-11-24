// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.orient.framework.client.RenderFrame;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** 提取阶段的物品/方块绘制入口，保留原模型中的显示变换。 */
public final class RenderItems {
    public static final RenderItems INSTANCE = new RenderItems();
    private static final Map<Identifier, Transforms> TRANSFORMS = new HashMap<>();
    private RenderItems() { }

    public void renderStatic(ItemStack stack, ItemDisplayContext context, int light, int overlay,
                             PoseStack pose, RenderFrame frame, Level level, int seed) {
        frame.item(stack, context, light, overlay, pose, level, seed);
    }

    public void renderSingleBlock(BlockState state, PoseStack pose, RenderFrame frame, int light, int overlay) {
        frame.block(state, pose, light, overlay);
    }

    public Transforms getModel(ItemStack stack, Level level, Object owner, int seed) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return TRANSFORMS.computeIfAbsent(id, key -> readModel(key.withPath("item/" + key.getPath()), 0));
    }

    public static void clearCache() { TRANSFORMS.clear(); }

    private static Transforms readModel(Identifier model, int depth) {
        Transforms out = new Transforms();
        if (depth > 32 || model.getPath().startsWith("builtin/")) { return out; }
        var resource = Minecraft.getInstance().getResourceManager().getResource(model.withPath("models/" + model.getPath() + ".json"));
        if (resource.isEmpty()) { return out; }
        try (var reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("parent")) {
                out.values.putAll(readModel(Identifier.parse(json.get("parent").getAsString()), depth + 1).values);
            }
            if (json.has("display")) {
                JsonObject display = json.getAsJsonObject("display");
                for (ItemDisplayContext mode : ItemDisplayContext.values()) {
                    if (display.has(mode.getSerializedName())) {
                        JsonObject value = display.getAsJsonObject(mode.getSerializedName());
                        Vector3f translation = vector(value, "translation", 0).mul(1F / 16F);
                        translation.set(clamp(translation.x, 5), clamp(translation.y, 5), clamp(translation.z, 5));
                        Vector3f scale = vector(value, "scale", 1);
                        scale.set(clamp(scale.x, 4), clamp(scale.y, 4), clamp(scale.z, 4));
                        out.values.put(mode, new Transform(new ItemTransform(vector(value, "rotation", 0), translation, scale)));
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.LOGGER.warn("无法读取物品显示变换 {}", model, e);
        }
        return out;
    }

    private static float clamp(float value, float limit) { return Math.max(-limit, Math.min(limit, value)); }
    private static Vector3f vector(JsonObject json, String name, float fallback) {
        if (!json.has(name)) { return new Vector3f(fallback); }
        var values = json.getAsJsonArray(name);
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    public static final class Transforms {
        private final Map<ItemDisplayContext, Transform> values = new EnumMap<>(ItemDisplayContext.class);
        public Transforms getTransforms() { return this; }
        public Transform getTransform(ItemDisplayContext context) {
            Transform value = values.get(context);
            if (value == null && context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                value = values.get(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            } else if (value == null && context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                value = values.get(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
            }
            return value == null ? new Transform(ItemTransform.NO_TRANSFORM) : value;
        }
    }

    public record Transform(ItemTransform value) {
        public void apply(boolean leftHand, PoseStack pose) {
            value.apply(leftHand, pose.last());
            // mryt233 改：旧入口在调用处单独居中，抵消26.1新增的末尾居中位移。
            pose.translate(0.5F, 0.5F, 0.5F);
        }
    }
}
