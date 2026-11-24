// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/**
 * 橙子树紧凑树冠生成器（1.20.1 适配，mryt233 改）：
 * 底层露出 1 格主干，中上层形成 3×3×2（长宽高）紧凑树冠（上层四角可按配置轻度削角），
 * 并按配置概率在正顶中央额外放置 1 格顶叶。全部几何比例由世界生成 JSON 驱动。
 * 作者：mryt233
 */
public class OrangeFoliagePlacer extends FoliagePlacer {
    public static final Codec<OrangeFoliagePlacer> CODEC = RecordCodecBuilder.create(instance ->
        foliagePlacerParts(instance).and(instance.group(
            Codec.intRange(1, 8).fieldOf("height").forGetter(p -> p.height),
            Codec.floatRange(0F, 1F).optionalFieldOf("top_leaf_chance", 0.65F).forGetter(p -> p.topLeafChance),
            Codec.floatRange(0F, 1F).optionalFieldOf("upper_corner_skip_chance", 0.45F)
                .forGetter(p -> p.upperCornerSkipChance)
        )).apply(instance, OrangeFoliagePlacer::new));

    private final int height;
    private final float topLeafChance;
    private final float upperCornerSkipChance;

    public OrangeFoliagePlacer(net.minecraft.util.valueproviders.IntProvider radius,
                               net.minecraft.util.valueproviders.IntProvider offset,
                               int height, float topLeafChance, float upperCornerSkipChance) {
        super(radius, offset);
        this.height = height;
        this.topLeafChance = topLeafChance;
        this.upperCornerSkipChance = upperCornerSkipChance;
    }

    public int heightValue() {
        return height;
    }

    public float topLeafChance() {
        return topLeafChance;
    }

    public float upperCornerSkipChance() {
        return upperCornerSkipChance;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return HotPotContent.ORANGE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random,
                                 TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment,
                                 int foliageHeight, int foliageRadius, int offset) {
        int topY = offset;
        int bottomY = offset - foliageHeight + 1;
        int r = Math.max(0, foliageRadius + attachment.radiusOffset());
        for (int y = topY; y >= bottomY; y--) {
            placeLeavesRow(level, setter, random, config, attachment.pos(), r, y, attachment.doubleTrunk());
        }
        if (topLeafChance > 0F && random.nextFloat() < topLeafChance) {
            placeLeavesRow(level, setter, random, config, attachment.pos(), 0, topY + 1, attachment.doubleTrunk());
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int treeHeight, TreeConfiguration config) {
        return height;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ,
                                         int range, boolean large) {
        if (range <= 0) {
            return false;
        }
        if (localY >= 0 && localX == range && localZ == range) {
            return upperCornerSkipChance > 0F && random.nextFloat() < upperCornerSkipChance;
        }
        return false;
    }
}
