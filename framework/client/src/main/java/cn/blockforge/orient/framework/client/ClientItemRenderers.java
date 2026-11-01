package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * 动态物品绘制的加载器无关接线，Fabric/NeoForge 用 {@link #install} 装自己的注册入口。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4）：统一走 install()；旧字段只为兼容保留。
 */
public final class ClientItemRenderers {
    @FunctionalInterface
    public interface Renderer {
        void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                    MultiBufferSource buffers, int light, int overlay);
    }

    /** 已弃用：旧内容包（Runtime 0.1.1）直接给这个字段赋值，保留二进制兼容，0.2.0 改为私有；新代码用 install。 */
    @Deprecated(since = "0.1.2", forRemoval = true)
    public static volatile BiConsumer<Item, Renderer> registrar = (item, renderer) -> {
        throw new IllegalStateException("Client item renderer platform not installed");
    };

    private ClientItemRenderers() {
    }

    /** 平台客户端入口调用：之后的 {@link #register} 都交给它。 */
    public static void install(BiConsumer<Item, Renderer> platformRegistrar) {
        registrar = platformRegistrar;
    }

    public static void register(Item item, Renderer renderer) {
        registrar.accept(item, renderer);
    }
}
