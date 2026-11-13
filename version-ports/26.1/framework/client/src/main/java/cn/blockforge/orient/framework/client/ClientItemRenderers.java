package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * 动态物品绘制的加载器无关接线，Fabric/NeoForge 用 {@link #install} 装自己的注册入口。作者：mryt233
 * mryt233 改（2026-11-11）：Runtime 0.2.0 收紧旧字段，只通过 install() / register() 接线。
 */
public final class ClientItemRenderers {
    @FunctionalInterface
    public interface Renderer {
        void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                    RenderFrame buffers, int light, int overlay);
    }

    /** mryt233 改（2026-11-11）：平台注册入口仅供内部持有。 */
    private static volatile BiConsumer<Item, Renderer> registrar = (item, renderer) -> {
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
