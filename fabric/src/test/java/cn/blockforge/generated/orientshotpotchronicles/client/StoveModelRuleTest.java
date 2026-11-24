// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.DataVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 灶台平滑 AO 规则单元测试；仅验证规则判定与默认值兼容，不替代游戏内区块渲染验证。 */
class StoveModelRuleTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        try {
            SharedConstants.getCurrentVersion();
            Bootstrap.bootStrap();
            return;
        } catch (IllegalStateException missingVersion) { }
        SharedConstants.setVersion(new WorldVersion() {
            private final DataVersion dataVersion = new DataVersion(3955);
            @Override public DataVersion getDataVersion() { return dataVersion; }
            @Override public String getId() { return "1.21.1"; }
            @Override public String getName() { return "1.21.1"; }
            @Override public int getProtocolVersion() { return 767; }
            @Override public int getPackVersion(PackType type) { return type == PackType.CLIENT_RESOURCES ? 34 : 48; }
            @Override public Date getBuildTime() { return new Date(0L); }
            @Override public boolean isStable() { return true; }
        });
        Bootstrap.bootStrap();
    }

    @Test void matchesOnlyLowAndTallStoveModels() {
        StoveModelRule rule = new StoveModelRule();
        assertTrue(rule.matches(HotPot.id("brick_stove")));
        assertTrue(rule.matches(HotPot.id("tall_brick_stove")));
        assertFalse(rule.matches(HotPot.id("hotpot_table")));
        assertFalse(rule.matches(HotPot.id("tall_brick_stove_top")));
        assertFalse(rule.matches(ResourceLocation.withDefaultNamespace("bricks")));
        assertFalse(rule.matches(null));
        assertNull(rule.resolveSwaps(null, null, null, id -> null));
    }

    @Test void forcesAoOnlyWhenBlockStateEmitsLight() {
        StoveModelRule rule = new StoveModelRule();

        assertFalse(rule.forceAmbientOcclusion(null));
        assertFalse(rule.forceAmbientOcclusion(Blocks.BRICKS.defaultBlockState()));
        assertFalse(rule.forceAmbientOcclusion(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, false)));
        assertFalse(rule.forceAmbientOcclusion(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false)));

        assertTrue(rule.forceAmbientOcclusion(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true)));
        assertTrue(rule.forceAmbientOcclusion(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true)));
    }

    @Test void keepsOnlyLitSocketEmbersUnoccluded() {
        StoveModelRule rule = new StoveModelRule();
        assertTrue(rule.keepUnoccludedSprite(HotPot.id("block/stove_socket_lit")));
        assertFalse(rule.keepUnoccludedSprite(HotPot.id("block/stove_socket")));
        assertFalse(rule.keepUnoccludedSprite(HotPot.id("block/stove_front_lit")));
        assertFalse(rule.keepUnoccludedSprite(ResourceLocation.withDefaultNamespace("block/bricks")));
        assertFalse(rule.keepUnoccludedSprite(null));
    }

    @Test void defaultSwappableRuleMethodsPreserveLegacyBehavior() {
        SwappableModelRule legacy = new SwappableModelRule() {
            @Override public boolean matches(ResourceLocation modelId) { return false; }
            @Override public Map<TextureAtlasSprite, TextureAtlasSprite> resolveSwaps(
                Object renderData, ItemStack stack, BlockState state, Function<ResourceLocation, TextureAtlasSprite> atlas) {
                return null;
            }
        };
        assertFalse(legacy.forceAmbientOcclusion(Blocks.FURNACE.defaultBlockState()));
        assertFalse(legacy.keepUnoccludedSprite(HotPot.id("block/stove_socket_lit")));
    }
}
