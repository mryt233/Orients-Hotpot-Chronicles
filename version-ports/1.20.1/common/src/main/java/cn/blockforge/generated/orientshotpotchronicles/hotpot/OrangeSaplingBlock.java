// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** 橙子树苗：树形与结果叶比例由数据包 {@code worldgen/configured_feature/orange_tree.json} 驱动（1.20.1 适配）。作者：mryt233 */
public class OrangeSaplingBlock extends SaplingBlock {
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORANGE_TREE =
        ResourceKey.create(Registries.CONFIGURED_FEATURE, HotPot.id("orange_tree"));
    private static final AbstractTreeGrower GROWER = new AbstractTreeGrower() {
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return ORANGE_TREE;
        }
    };

    public OrangeSaplingBlock(BlockBehaviour.Properties properties) {
        super(GROWER, properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HotPotCropBlock.stepScaledGrowth(Config.cropsGrowthRate(), level, pos, this, random,
            (s, r) -> super.randomTick(s, level, pos, r));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return Config.cropsBonemealEnabled() && super.isValidBonemealTarget(level, pos, state, isClient);
    }
}
