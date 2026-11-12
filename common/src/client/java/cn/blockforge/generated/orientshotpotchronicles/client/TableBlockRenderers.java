package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * 桌面物件渲染回调登记表：让特殊桌面方块或整件物品在 {@link PlateHolderRenderer} 里自定义绘制，
 * 不必在渲染器主循环里硬编码具体类。
 * 作者：mryt233
 */
public final class TableBlockRenderers {
    @FunctionalInterface
    public interface BlockRenderer {
        void render(PlateHolderBlockEntity be, PlateHolderBlockEntity.Placed placed,
                    PoseStack matrices, MultiBufferSource buffers,
                    int light, int overlay, ItemRenderer itemRenderer, int seed);
    }

    @FunctionalInterface
    public interface SelfOverlayRenderer {
        void render(PlateHolderBlockEntity.Placed placed, PoseStack matrices,
                    MultiBufferSource buffers, int light);
    }

    private static final Map<PlateBlock, BlockRenderer> BLOCK_RENDERERS = new ConcurrentHashMap<>();
    private static final Map<Item, SelfOverlayRenderer> OVERLAY_RENDERERS = new ConcurrentHashMap<>();

    private TableBlockRenderers() {
    }

    /** 登记整件代画回调（后注册覆盖先注册，重复加载安全）。 */
    public static void registerBlock(PlateBlock plate, BlockRenderer renderer) {
        BLOCK_RENDERERS.put(Objects.requireNonNull(plate, "plate"),
            Objects.requireNonNull(renderer, "renderer"));
    }

    /** 登记在默认方块模型之上叠加绘制的回调（例如蘸料碗酱面）。 */
    public static void registerOverlay(Item selfItem, SelfOverlayRenderer renderer) {
        OVERLAY_RENDERERS.put(Objects.requireNonNull(selfItem, "selfItem"),
            Objects.requireNonNull(renderer, "renderer"));
    }

    public static @Nullable BlockRenderer blockRenderer(PlateBlock plate) {
        return plate == null ? null : BLOCK_RENDERERS.get(plate);
    }

    public static @Nullable SelfOverlayRenderer overlayRenderer(Item selfItem) {
        return selfItem == null ? null : OVERLAY_RENDERERS.get(selfItem);
    }
}
