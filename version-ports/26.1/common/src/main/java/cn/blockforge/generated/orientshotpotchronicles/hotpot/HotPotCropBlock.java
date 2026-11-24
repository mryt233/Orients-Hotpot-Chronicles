// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 9 种火锅配菜共用的 8 阶（0～7）耕地作物基类；直接用同名食材成品下地播种。 */
public class HotPotCropBlock extends CropBlock {
    private static final VoxelShape[] SHAPE_BY_AGE = {
        Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),
        Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0)
    };

    private final Supplier<Item> seedItem;

    public HotPotCropBlock(Supplier<Item> seedItem, BlockBehaviour.Properties properties) {
        super(properties);
        this.seedItem = seedItem;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seedItem.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[getAge(state)];
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        stepScaledGrowth(Config.cropsGrowthRate(), level, pos, this, random,
            (s, r) -> super.randomTick(s, level, pos, r));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return Config.cropsBonemealEnabled() && super.isValidBonemealTarget(level, pos, state);
    }

    static void stepScaledGrowth(float rate, ServerLevel level, BlockPos pos, Block expected,
                                 RandomSource random, BiConsumer<BlockState, RandomSource> tickOnce) {
        if (rate <= 0F) {
            return;
        }
        if (rate < 1F) {
            if (random.nextFloat() < rate) {
                tickOnce.accept(level.getBlockState(pos), random);
            }
            return;
        }
        int rounds = Math.min(8, (int) Math.floor(rate));
        for (int i = 0; i < rounds; i++) {
            BlockState current = level.getBlockState(pos);
            if (!current.is(expected)) {
                return;
            }
            tickOnce.accept(current, random);
        }
        float extra = rate - rounds;
        if (extra > 0F && random.nextFloat() < extra) {
            BlockState current = level.getBlockState(pos);
            if (current.is(expected)) {
                tickOnce.accept(current, random);
            }
        }
    }
}
