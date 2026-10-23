package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** 条凳的方块实体：不存任何数据，只是让 {@code BenchLegsRenderer} 能按任意角度画斜腿（方块模型只能转 22.5° 的倍数）。 */
public class BenchBlockEntity extends BlockEntity {
    public BenchBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.BENCH_BE, pos, state);
    }
}
