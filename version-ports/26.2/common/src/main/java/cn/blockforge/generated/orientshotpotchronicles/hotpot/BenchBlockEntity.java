package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

/** 条凳的方块实体：不存任何数据，以前给斜腿渲染器用；26.x 腿已写进方块模型，这里只为不改注册表 / 存档而保留（mryt233 改）。 */
public class BenchBlockEntity extends BlockEntity {
    public BenchBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.BENCH_BE, pos, state);
    }
}
