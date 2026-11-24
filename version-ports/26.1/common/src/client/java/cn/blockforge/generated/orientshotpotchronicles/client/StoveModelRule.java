// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 矮灶台与高灶台点燃态外砖平滑 AO 保留规则：发光时仍走原版平滑光照/AO，避免外砖泛白；
 * 锅槽内余烬保持原本亮度，汤面模型自身的 ambientocclusion=false 也会被尊重。作者：mryt233
 */
public final class StoveModelRule implements SwappableModelRule {
    private static final Set<Identifier> TARGETS = Set.of(
        HotPot.id("brick_stove"),
        HotPot.id("tall_brick_stove")
    );
    private static final Identifier SOCKET_LIT = HotPot.id("block/stove_socket_lit");

    @Override
    public boolean matches(Identifier modelId) {
        return modelId != null && TARGETS.contains(modelId);
    }

    @Override
    public @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> resolveSwaps(
        @Nullable Object renderData, @Nullable ItemStack stack,
        @Nullable BlockState state, Function<Identifier, TextureAtlasSprite> blockAtlas) {
        return null;
    }

    @Override
    public boolean forceAmbientOcclusion(@Nullable BlockState state) {
        return state != null && state.getLightEmission() > 0;
    }

    @Override
    public boolean keepUnoccludedSprite(@Nullable Identifier spriteId) {
        return SOCKET_LIT.equals(spriteId);
    }
}
