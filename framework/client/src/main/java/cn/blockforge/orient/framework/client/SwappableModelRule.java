// 作者：mryt233
package cn.blockforge.orient.framework.client;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原版区块烘焙模型贴图槽位动态替换规则，完全与加载器解耦，由 Fabric / NeoForge 各自接入其模型系统。
 * 作者：mryt233
 */
public interface SwappableModelRule {
    /** 判断该顶层模型 ID（BlockState 或 inventory 物品）是否启用本规则包装。 */
    boolean matches(ResourceLocation modelId);

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
        @Nullable BlockState state, Function<ResourceLocation, TextureAtlasSprite> blockAtlas);

    /** 返回与当前快照对应的破坏 / 奔跑粒子精灵；返回 null 时回退原模型粒子。 */
    default @Nullable TextureAtlasSprite particleSprite(
        @Nullable Object renderData, @Nullable BlockState state,
        Function<ResourceLocation, TextureAtlasSprite> blockAtlas) {
        return null;
    }

    /** 为 true 时即使方块自身发光也强制启用模型平滑 AO（仍遵循玩家全局 AO 开关）。 */
    default boolean forceAmbientOcclusion(@Nullable BlockState state) {
        return false;
    }

    /** 配合 {@link #forceAmbientOcclusion} 使用；返回 true 的贴图精灵不强制开启平滑 AO。 */
    default boolean keepUnoccludedSprite(@Nullable ResourceLocation spriteId) {
        return false;
    }
}
