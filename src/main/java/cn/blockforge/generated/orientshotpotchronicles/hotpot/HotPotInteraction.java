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

    /** 锅模型有没有绕 y 转 90°（耳朵朝南北摆：独立锅 axis=z、灶上锅 pot_axis=z）。 */
    public static boolean rotated(net.minecraft.block.BlockState state) {
        if (StoveBlock.isStove(state)) {
            return state.get(StoveBlock.POT_AXIS) == net.minecraft.util.math.Direction.Axis.Z;
        }
        return state.getBlock() instanceof HotPotBlock && state.get(HotPotBlock.AXIS) == net.minecraft.util.math.Direction.Axis.Z;
    }

    /** 命中点在锅腔里的模型坐标比例（0..1）。 */
    private static double[] modelUv(net.minecraft.block.BlockState state, BlockPos pos, BlockHitResult hit,
                                    double innerMin, double innerSize) {
        double u = MathHelper.clamp((hit.getPos().x - pos.getX() - innerMin) / innerSize, 0, 1);
        double v = MathHelper.clamp((hit.getPos().z - pos.getZ() - innerMin) / innerSize, 0, 1);
        return PotLayout.toModel(u, v, rotated(state));
    }

    /** 准星对着锅里哪个大格：九宫格按 3x3，鸳鸯锅按 S / O 形的格子（{@link PotLayout#cellAt}）。。 */
    public static int cellFromHit(HotPotBlockEntity be, BlockPos pos, BlockHitResult hit, double innerMin, double innerSize) {
        PotLayout layout = be.layout();
        if (!layout.multiSoup()) {
            return bigCellFromHit(pos, hit, innerMin, innerSize);
        }
        double[] uv = modelUv(be.getCachedState(), pos, hit, innerMin, innerSize);
        return layout.cellAt(uv[0], uv[1]);
    }

    /** 准星对着鸳鸯锅的哪一边（倒汤、舀汤用）；九宫格锅永远是 A。 */
    public static int sideFromHit(HotPotBlockEntity be, BlockPos pos, BlockHitResult hit, double innerMin, double innerSize) {
        PotLayout layout = be.layout();
        if (!layout.multiSoup()) {
            return PotLayout.SIDE_A;
        }
        double[] uv = modelUv(be.getCachedState(), pos, hit, innerMin, innerSize);
        return layout.sideNear(uv[0], uv[1]);
    }

    /**
     * 客户端钩子：空筷子 / 空手 / 碗右键锅时，报告这一刻浮在准星那一格上的是哪一小格（HotPotClient 里设成发
     * ChopsticksAimPayload）。服务端什么都不做。（2026-10-25）。
     */
    public static java.util.function.BiConsumer<HotPotBlockEntity, Integer> clientChopsticksAim = (be, cell) -> { };

    /** 筷子用于开面板，碗用于直接捞取；空手由方块的 onUse 路径处理。 */
    public static boolean isServingTool(ItemStack stack) {
        return stack.isOf(HotPotContent.CHOPSTICKS) || stack.isOf(Items.BOWL);
    }
}
