// 作者：mryt233
package cn.blockforge.orient.framework.client;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 原版区块烘焙模型贴图槽位动态替换规则，完全与加载器解耦，由 Fabric / NeoForge 各自接入其模型系统。
 * 作者：mryt233
 */
public interface SwappableModelRule {
    /** 判断该顶层模型 ID（BlockState 或 inventory 物品）是否启用本规则包装。 */
    boolean matches(Identifier modelId);

    /**
     * 解析当前方块快照或物品栈需要把哪些占位精灵替换成目标材质精灵。
     *
     * @param renderData 方块实体提供的不可变渲染快照（物品渲染时为 null）
     * @param stack      正在渲染的物品栈（世界方块渲染时为 null）
     * @param state      当前方块状态（物品渲染时可能为 null）
     * @param blockAtlas 方块图集查找入口
     * @return 「占位精灵 -> 目标精灵」引用映射；返回 null 或空表时不改原面
     */
    @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> resolveSwaps(
        @Nullable Object renderData, @Nullable ItemStack stack,
        @Nullable BlockState state, Function<Identifier, TextureAtlasSprite> blockAtlas);

    /** 返回与当前快照对应的破坏 / 奔跑粒子精灵；返回 null 时回退原模型粒子。 */
    default @Nullable TextureAtlasSprite particleSprite(
        @Nullable Object renderData, @Nullable BlockState state,
        Function<Identifier, TextureAtlasSprite> blockAtlas) {
        return null;
    }

    /** mryt233 改（2026-11-23）：发光方块若仍希望外表面保留平滑 AO，可覆写此方法返回 true。 */
    default boolean forceAmbientOcclusion(@Nullable BlockState state) {
        return false;
    }

    /** mryt233 改（2026-11-23）：配合 {@link #forceAmbientOcclusion}，指明应保持无遮挡（不强开 AO）的自发光精灵。 */
    default boolean keepUnoccludedSprite(@Nullable Identifier spriteId) {
        return false;
    }
}
