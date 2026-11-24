// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature; // mryt233 改：26.3 特征注册表

/** 橙子树苗：树形与结果叶比例由数据包 {@code worldgen/configured_feature/orange_tree.json} 驱动。 */
public class OrangeSaplingBlock extends SaplingBlock {
    public static final ResourceKey<Feature> ORANGE_TREE =
        ResourceKey.create(Registries.FEATURE, HotPot.id("orange_tree"));
    // mryt233 改：26.3 使用加权普通树列表，最矮树仍指向同一橙树。
    private static final TreeGrower GROWER = new TreeGrower(
        HotPot.MOD_ID + ":orange", net.minecraft.util.random.WeightedList.of(ORANGE_TREE),
        net.minecraft.util.random.WeightedList.of(), net.minecraft.util.random.WeightedList.of(), ORANGE_TREE);

    public OrangeSaplingBlock(BlockBehaviour.Properties properties) {
        super(GROWER, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HotPotCropBlock.stepScaledGrowth(Config.cropsGrowthRate(), level, pos, this, random,
            (s, r) -> super.randomTick(s, level, pos, r));
    }

    // mryt233 改：26.3 骨粉接口额外传入来源。
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, net.minecraft.world.level.block.BonemealSource source) {
        return Config.cropsBonemealEnabled() && super.isValidBonemealTarget(level, pos, state, source);
    }
}
