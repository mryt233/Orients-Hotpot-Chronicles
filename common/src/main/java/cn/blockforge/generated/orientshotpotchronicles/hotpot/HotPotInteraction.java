package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** 准星与 3x3 九宫格之间的共享空间映射。 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class HotPotInteraction {
    /** 独立摆放的锅：锅腔 1.5..14.5（对应 iron/copper_hot_pot_shell 模型）。 */
    public static final double STANDALONE_INNER_MIN = 1.5 / 16.0;
    public static final double STANDALONE_INNER_SIZE = 13.0 / 16.0;
    /** 灶上锅：锅腔 1.837..14.163（stove_*_hot_pot_shell 模型，接近地上单独锅的大小）。 */
    public static final double STOVE_INNER_MIN = 1.837 / 16.0;
    public static final double STOVE_INNER_SIZE = 12.326 / 16.0;

    /**
     * 汤面高度（方块单位，不含灶台抬高）。mryt233 改（2026-11-04）：从渲染器搬到这里，
     * 服务端算「准星落在哪格」时要用同一个值，不能另写一个数。
     * 与 HotPotBlockEntityRenderer 的 STANDALONE_SURFACE / STOVE_SURFACE 一致。
     */
    public static final double STANDALONE_SURFACE = 5.6 / 16.0;
    public static final double STOVE_SURFACE = 14.32 / 16.0;
    /** 锅里没汤时食材沉在锅底的高度（同样从渲染器搬来，渲染器引用这里；mryt233 改）。 */
    public static final double STANDALONE_FLOOR = 1.25 / 16.0;
    public static final double STOVE_FLOOR = 9.1 / 16.0;

    private HotPotInteraction() {
    }

    /** 独立摆放的锅。 */
    public static int bigCellFromHit(BlockPos pos, BlockHitResult hit) {
        return bigCellFromHit(pos, hit, STANDALONE_INNER_MIN, STANDALONE_INNER_SIZE);
    }

    /** 将命中点投影到锅内 3x3 大格，边沿命中收敛到最近格。 */
    public static int bigCellFromHit(BlockPos pos, BlockHitResult hit, double innerMin, double innerSize) {
        double localX = Mth.clamp(hit.getLocation().x - pos.getX(), innerMin, innerMin + innerSize);
        double localZ = Mth.clamp(hit.getLocation().z - pos.getZ(), innerMin, innerMin + innerSize);
        int col = Mth.clamp((int) Math.floor((localX - innerMin) / innerSize * 3.0), 0, 2);
        int row = Mth.clamp((int) Math.floor((localZ - innerMin) / innerSize * 3.0), 0, 2);
        return row * 3 + col;
    }

    /**
     * mryt233 改（2026-11-04）：汤面所在的世界 y。灶上的锅、高灶台、篝火上的锅各自抬高多少，用同一个 liftOf。
     * `onStove` 决定用灶上锅还是独立锅的汤面高度。
     */
    public static double soupSurfaceY(BlockState state, BlockPos pos, boolean hasSoup) {
        // 和渲染器同一规则：有汤时食材浮在汤面，没汤时沉在锅底（mryt233 改）
        boolean onStove = StoveBlock.isStove(state);
        double h = hasSoup ? (onStove ? STOVE_SURFACE : STANDALONE_SURFACE) : (onStove ? STOVE_FLOOR : STANDALONE_FLOOR);
        return pos.getY() + StoveBlock.liftOf(state) + h;
    }

    /**
     * mryt233 改（2026-11-04）：玩家视线射线和锅的汤面（水平面）求交点，返回交点相对方块原点的 (x, z)；
     * 射线平行于汤面、交点在身后、或者交点不在这口锅的水平范围里就返回 null。
     *
     * <p>以前直接用 {@link BlockHitResult#getLocation()} 算格子，那个点是射线打在锅的碰撞 / 选择外形上的点：
     * 坐在桌边低角度看锅，射线先打到锅沿或外形顶面靠自己这一侧，换算出来永远是最靠前的一排，
     * 于是「准星指着远处的格子，夹起来的却是最近那格」（用户报）。改用汤面交点就没有这个问题。</p>
     */
    public static Vec3 lookAtSoupSurface(LivingEntity user, BlockState state, BlockPos pos, boolean hasSoup) {
        Vec3 eye = user.getEyePosition(1.0F);
        Vec3 dir = user.getViewVector(1.0F);
        double planeY = soupSurfaceY(state, pos, hasSoup);
        if (Math.abs(dir.y) < 1.0E-6) {
            return null;
        }
        double along = (planeY - eye.y) / dir.y;
        if (along <= 0.0) {
            return null;
        }
        Vec3 point = eye.add(dir.scale(along));
        double localX = point.x - pos.getX();
        double localZ = point.z - pos.getZ();
        // 交点要落在这口锅的水平范围里（锅腔 + 一点边，锅腔外一点点还是算边沿格；远处的锅不算）
        double margin = STATE_RANGE_MARGIN;
        if (localX < -margin || localX > 1.0 + margin || localZ < -margin || localZ > 1.0 + margin) {
            return null;
        }
        return point;
    }

    /** 汤面交点落在锅外多少还认（方块单位）。 */
    private static final double STATE_RANGE_MARGIN = 0.25;

    /** 把汤面交点换算成锅腔比例坐标（u 向东、v 向南，已按锅的朝向转回模型坐标）。 */
    private static double[] soupUv(BlockState state, BlockPos pos, Vec3 point, double innerMin, double innerSize) {
        double u = Mth.clamp((point.x - pos.getX() - innerMin) / innerSize, 0, 1);
        double v = Mth.clamp((point.z - pos.getZ() - innerMin) / innerSize, 0, 1);
        return PotLayout.toModel(u, v, rotated(state));
    }

    /** 锅模型有没有绕 y 转 90°（耳朵朝南北摆：独立锅 axis=z、灶上锅 pot_axis=z）。 */
    public static boolean rotated(net.minecraft.world.level.block.state.BlockState state) {
        if (StoveBlock.isStove(state)) {
            return state.getValue(StoveBlock.POT_AXIS) == net.minecraft.core.Direction.Axis.Z;
        }
        return state.getBlock() instanceof HotPotBlock && state.getValue(HotPotBlock.AXIS) == net.minecraft.core.Direction.Axis.Z;
    }

    /** 命中点在锅腔里的模型坐标比例（0..1）。 */
    private static double[] modelUv(net.minecraft.world.level.block.state.BlockState state, BlockPos pos, BlockHitResult hit,
                                    double innerMin, double innerSize) {
        double u = Mth.clamp((hit.getLocation().x - pos.getX() - innerMin) / innerSize, 0, 1);
        double v = Mth.clamp((hit.getLocation().z - pos.getZ() - innerMin) / innerSize, 0, 1);
        return PotLayout.toModel(u, v, rotated(state));
    }

    /**
     * 准星对着锅里哪个大格：九宫格按 3x3，鸳鸯锅按 S / O 形的格子（{@link PotLayout#cellAt}）。
     *
     * <p>mryt233 改（2026-11-04）：先用玩家视线和汤面的交点算，交点不可用时才退回命中点（{@link BlockHitResult}），
     * 见 {@link #lookAtSoupSurface}。{@code user} 传 null 时用命中点。</p>
     */
    public static int cellFromHit(HotPotBlockEntity be, LivingEntity user, BlockPos pos, BlockHitResult hit,
                                  double innerMin, double innerSize) {
        PotLayout layout = be.layout();
        Vec3 point = user == null ? null : lookAtSoupSurface(user, be.getBlockState(), pos, be.hasSoup());
        if (!layout.multiSoup()) {
            return bigCellFromInteraction(be, point, pos, hit, innerMin, innerSize);
        }
        double[] uv = point != null
            ? soupUv(be.getBlockState(), pos, point, innerMin, innerSize)
            : modelUv(be.getBlockState(), pos, hit, innerMin, innerSize);
        return layout.cellAt(uv[0], uv[1]);
    }

    /** 九宫格：汤面交点优先，没有就用命中点。 */
    private static int bigCellFromInteraction(HotPotBlockEntity be, Vec3 point, BlockPos pos, BlockHitResult hit,
                                              double innerMin, double innerSize) {
        if (point != null) {
            double localX = Mth.clamp(point.x - pos.getX(), innerMin, innerMin + innerSize);
            double localZ = Mth.clamp(point.z - pos.getZ(), innerMin, innerMin + innerSize);
            int col = Mth.clamp((int) Math.floor((localX - innerMin) / innerSize * 3.0), 0, 2);
            int row = Mth.clamp((int) Math.floor((localZ - innerMin) / innerSize * 3.0), 0, 2);
            return row * 3 + col;
        }
        return bigCellFromHit(pos, hit, innerMin, innerSize);
    }

    /** 准星对着鸳鸯锅的哪一边（倒汤、舀汤用）；九宫格锅永远是 A。汤面交点优先，同 {@link #cellFromHit}。 */
    public static int sideFromHit(HotPotBlockEntity be, LivingEntity user, BlockPos pos, BlockHitResult hit,
                                  double innerMin, double innerSize) {
        PotLayout layout = be.layout();
        if (!layout.multiSoup()) {
            return PotLayout.SIDE_A;
        }
        Vec3 point = user == null ? null : lookAtSoupSurface(user, be.getBlockState(), pos, be.hasSoup());
        double[] uv = point != null
            ? soupUv(be.getBlockState(), pos, point, innerMin, innerSize)
            : modelUv(be.getBlockState(), pos, hit, innerMin, innerSize);
        return layout.sideNear(uv[0], uv[1]);
    }

    /**
     * 客户端钩子：空筷子 / 空手 / 碗右键锅时，报告这一刻浮在准星那一格上的是哪一小格（HotPotClient 里设成发
     * ChopsticksAimPayload）。服务端什么都不做。新增（2026-10-25）。
     */
    public static java.util.function.BiConsumer<HotPotBlockEntity, Integer> clientChopsticksAim = (be, cell) -> { };

    /** 筷子用于开面板，碗用于直接捞取；空手由方块的 onUse 路径处理。 */
    public static boolean isServingTool(ItemStack stack) {
        return stack.is(HotPotContent.CHOPSTICKS) || stack.is(Items.BOWL);
    }

    /** 已通过入口筛选后的锅操作；钩子只由方块入口调用一次。 */
    public static ItemInteractionResult useItem(HotPotBlockEntity be, ItemStack stack,
            Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, double innerMin, double innerSize) {
        return useItem(be, stack, world, pos, player, hand, hit, innerMin, innerSize, null);
    }

    static ItemInteractionResult useItem(HotPotBlockEntity be, ItemStack stack,
            Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, double innerMin, double innerSize,
            boolean[] checks) {
        int bigCell = HotPotInteraction.cellFromHit(be, player, pos, hit, innerMin, innerSize);
        int cookedSlot = be.firstCookedSlotInCell(bigCell);

        if (checks == null ? HotPotBlock.isPlate(stack) : checks[0]) {
            if (!world.isClientSide) {
                be.pourPlate(player, hand);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (checks == null ? be.canScoopWith(stack) : checks[1]) {
            if (!world.isClientSide) {
                be.scoopSoup(player, hand, HotPotInteraction.sideFromHit(be, player, pos, hit, innerMin, innerSize));
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(Items.BOWL)) {
            if (world.isClientSide) {
                HotPotInteraction.clientChopsticksAim.accept(be, bigCell);   // 碗先盛看到的那样（2026-10-26）
            }
            if (!world.isClientSide) {
                be.serveWithBowl(player, hand, bigCell);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (checks == null ? isServingTool(stack) : checks[2]) {
            if (stack.is(HotPotContent.CHOPSTICKS)) {
                // 夹着的是熟菜（下不了锅）：不管锅，交给筷子吃（按住右键吃）。以前会提示「煮不了」，
                // 刚从锅里夹起来、右键还按着时就吃不上（用户报：筷子夹起来的菜吃不了。调整）
                ItemStack held = Chopsticks.held(stack);
                if (!held.isEmpty() && !HotPotCooking.acceptable(world, held)) {
                    return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
                }
                // 筷子上夹着菜：放进准星对着的那一格；空筷子：夹起这一格的一样熟食
                // （2026-10-25 用户定的新交互；开锅界面改成拿筷子 Shift+右键，见 ChopsticksItem.useOnBlock。调整）
                if (world.isClientSide && Chopsticks.held(stack).isEmpty()) {
                    // 告诉服务端这一刻浮在这格上的是哪样，夹的就是看到的那样（用户 2026-10-25）
                    HotPotInteraction.clientChopsticksAim.accept(be, bigCell);
                }
                if (!world.isClientSide) {
                    if (!Chopsticks.held(stack).isEmpty()) {
                        be.insertHeld(player, stack, be.firstEmptySlotInCell(bigCell));
                    } else if (!be.pickShown(player, stack, bigCell)) {
                        player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.no_cooked_here")), true);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
            if (!world.isClientSide && !be.collectCookedSlot(player, cookedSlot)) {
                player.displayClientMessage(Component.translatable(
                    HotPotBlockEntity.key("msg.no_cooked_here")), true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (checks == null ? SoupBase.fromStack(stack) != null : checks[3]) {
            if (!world.isClientSide && !be.pourSoup(player, hand, HotPotInteraction.sideFromHit(be, player, pos, hit, innerMin, innerSize))) {
                return ItemInteractionResult.FAIL;
            }
            return ItemInteractionResult.SUCCESS;
        }
        // mryt233 改（2026-11-05，r136a）：辣椒加辣、冰块降辣——排在倒汤底之后、下锅食材之前，
        // 免得辣椒被当成生食材塞进锅槽里（HotPotCooking.acceptable 里也明确拒收辣椒）。
        if (checks == null ? HotPotBlockEntity.isSpicyModifier(stack) : checks[4]) {
            if (!world.isClientSide) {
                be.adjustSpiciness(player, hand,
                    HotPotInteraction.sideFromHit(be, player, pos, hit, innerMin, innerSize));
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (checks == null ? HotPotCooking.acceptable(world, stack) : checks[5]) {
            if (!world.isClientSide && !be.insertIngredient(player, hand, be.firstEmptySlotInCell(bigCell))) {
                return ItemInteractionResult.FAIL;
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
