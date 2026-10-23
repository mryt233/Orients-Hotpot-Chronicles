package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** 准星与 3x3 九宫格之间的共享空间映射。 */
public final class HotPotInteraction {
    /** 独立摆放的锅：锅腔 1.5..14.5（对应 iron/copper_hot_pot_shell 模型）。 */
    public static final double STANDALONE_INNER_MIN = 1.5 / 16.0;
    public static final double STANDALONE_INNER_SIZE = 13.0 / 16.0;
    /** 灶上锅：锅腔 1.837..14.163（stove_*_hot_pot_shell 模型，接近地上单独锅的大小）。 */
    public static final double STOVE_INNER_MIN = 1.837 / 16.0;
    public static final double STOVE_INNER_SIZE = 12.326 / 16.0;

    private HotPotInteraction() {
    }

    /** 独立摆放的锅。 */
    public static int bigCellFromHit(BlockPos pos, BlockHitResult hit) {
        return bigCellFromHit(pos, hit, STANDALONE_INNER_MIN, STANDALONE_INNER_SIZE);
    }

    /** 将命中点投影到锅内 3x3 大格，边沿命中收敛到最近格。 */
    public static int bigCellFromHit(BlockPos pos, BlockHitResult hit, double innerMin, double innerSize) {
        double localX = MathHelper.clamp(hit.getPos().x - pos.getX(), innerMin, innerMin + innerSize);
        double localZ = MathHelper.clamp(hit.getPos().z - pos.getZ(), innerMin, innerMin + innerSize);
        int col = MathHelper.clamp((int) Math.floor((localX - innerMin) / innerSize * 3.0), 0, 2);
        int row = MathHelper.clamp((int) Math.floor((localZ - innerMin) / innerSize * 3.0), 0, 2);
        return row * 3 + col;
    }

    /** 筷子用于开面板，碗用于直接捞取；空手由方块的 onUse 路径处理。 */
    public static boolean isServingTool(ItemStack stack) {
        return stack.isOf(HotPotContent.CHOPSTICKS) || stack.isOf(Items.BOWL);
    }
}
