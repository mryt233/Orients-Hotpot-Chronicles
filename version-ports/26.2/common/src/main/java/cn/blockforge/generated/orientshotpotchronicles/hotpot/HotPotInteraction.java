// mryt233 改（2026-11-11）：第三批 3F/3G 合并重复实现，适配 26.2 InteractionResult 与 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 准星与 3x3 九宫格之间的共享空间映射。 */
public final class HotPotInteraction {
    /** 独立摆放的锅：锅腔 1.5..14.5。 */
    public static final double STANDALONE_INNER_MIN = 1.5 / 16.0;
    public static final double STANDALONE_INNER_SIZE = 13.0 / 16.0;
    /** 灶上锅：锅腔 1.837..14.163。 */
    public static final double STOVE_INNER_MIN = 1.837 / 16.0;
    public static final double STOVE_INNER_SIZE = 12.326 / 16.0;

    public static final double STANDALONE_SURFACE = 5.6 / 16.0;
    public static final double STOVE_SURFACE = 14.32 / 16.0;
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

    /** 汤面所在的世界 y。 */
    public static double soupSurfaceY(BlockState state, BlockPos pos, boolean hasSoup) {
        boolean onStove = StoveBlock.isStove(state);
        double h = hasSoup ? (onStove ? STOVE_SURFACE : STANDALONE_SURFACE) : (onStove ? STOVE_FLOOR : STANDALONE_FLOOR);
        return pos.getY() + StoveBlock.liftOf(state) + h;
    }

    /** 玩家视线射线和锅的汤面求交点。 */
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
        double margin = STATE_RANGE_MARGIN;
        if (localX < -margin || localX > 1.0 + margin || localZ < -margin || localZ > 1.0 + margin) {
            return null;
        }
        return point;
    }

    private static final double STATE_RANGE_MARGIN = 0.25;

    private static double[] soupUv(BlockState state, BlockPos pos, Vec3 point, double innerMin, double innerSize) {
        double u = Mth.clamp((point.x - pos.getX() - innerMin) / innerSize, 0, 1);
        double v = Mth.clamp((point.z - pos.getZ() - innerMin) / innerSize, 0, 1);
        return PotLayout.toModel(u, v, rotated(state));
    }

    public static boolean rotated(BlockState state) {
        if (StoveBlock.isStove(state)) {
            return state.getValue(StoveBlock.POT_AXIS) == Direction.Axis.Z;
        }
        return state.getBlock() instanceof HotPotBlock && state.getValue(HotPotBlock.AXIS) == Direction.Axis.Z;
    }

    private static double[] modelUv(BlockState state, BlockPos pos, BlockHitResult hit,
                                    double innerMin, double innerSize) {
        double u = Mth.clamp((hit.getLocation().x - pos.getX() - innerMin) / innerSize, 0, 1);
        double v = Mth.clamp((hit.getLocation().z - pos.getZ() - innerMin) / innerSize, 0, 1);
        return PotLayout.toModel(u, v, rotated(state));
    }

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

    public static BiConsumer<HotPotBlockEntity, Integer> clientChopsticksAim = (be, cell) -> { };

    /** 筷子用于开面板，碗用于直接捞取；空手由方块的 onUse 路径处理。 */
    public static boolean isServingTool(ItemStack stack) {
        return stack.is(HotPotContent.CHOPSTICKS) || stack.is(Items.BOWL);
    }

    /** 已通过入口筛选后的锅操作；钩子只由方块入口调用一次。 */
    public static InteractionResult useItem(HotPotBlockEntity be, ItemStack stack,
            Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, double innerMin, double innerSize) {
        return useItem(be, stack, world, pos, player, hand, hit, innerMin, innerSize, null);
    }

    static InteractionResult useItem(HotPotBlockEntity be, ItemStack stack,
            Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, double innerMin, double innerSize,
            boolean[] checks) {
        int bigCell = HotPotInteraction.cellFromHit(be, player, pos, hit, innerMin, innerSize);
        int cookedSlot = be.firstCookedSlotInCell(bigCell);
        boolean client = world.isClientSide();

        if (checks == null ? HotPotBlock.isPlate(stack) : checks[0]) {
            if (!client) {
                be.pourPlate(player, hand);
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        if (checks == null ? be.canScoopWith(stack) : checks[1]) {
            if (!client) {
                be.scoopSoup(player, hand, HotPotInteraction.sideFromHit(be, player, pos, hit, innerMin, innerSize));
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        if (stack.is(Items.BOWL)) {
            if (client) {
                HotPotInteraction.clientChopsticksAim.accept(be, bigCell);
            }
            if (!client) {
                be.serveWithBowl(player, hand, bigCell);
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        if (checks == null ? isServingTool(stack) : checks[2]) {
            if (stack.is(HotPotContent.CHOPSTICKS)) {
                ItemStack held = Chopsticks.held(stack);
                if (!held.isEmpty() && !HotPotCooking.acceptable(world, held)) {
                    return Use.blockItem(UseOutcome.SKIPPED, client);
                }
                if (client && Chopsticks.held(stack).isEmpty()) {
                    HotPotInteraction.clientChopsticksAim.accept(be, bigCell);
                }
                if (!client) {
                    if (!Chopsticks.held(stack).isEmpty()) {
                        be.insertHeld(player, stack, be.firstEmptySlotInCell(bigCell));
                    } else if (!be.pickShown(player, stack, bigCell)) {
                        player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.no_cooked_here")));
                    }
                }
                return Use.blockItem(UseOutcome.SUCCESS, client);
            }
            if (!client && !be.collectCookedSlot(player, cookedSlot)) {
                player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.no_cooked_here")));
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        if (checks == null ? Soups.fromStack(stack) != null : checks[3]) {
            if (!client && !be.pourSoup(player, hand, HotPotInteraction.sideFromHit(be, player, pos, hit, innerMin, innerSize))) {
                return Use.blockItem(UseOutcome.FAIL, client);
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        if (checks == null ? HotPotBlockEntity.isSpicyModifier(stack) : checks[4]) {
            if (!client) {
                be.adjustSpiciness(player, hand,
                    HotPotInteraction.sideFromHit(be, player, pos, hit, innerMin, innerSize));
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        if (checks == null ? HotPotCooking.acceptable(world, stack) : checks[5]) {
            if (!client && !be.insertIngredient(player, hand, be.firstEmptySlotInCell(bigCell))) {
                return Use.blockItem(UseOutcome.FAIL, client);
            }
            return Use.blockItem(UseOutcome.SUCCESS, client);
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, client);
    }
}
