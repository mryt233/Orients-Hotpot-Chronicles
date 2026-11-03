// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import cn.blockforge.orient.framework.game.Hooks;
import java.util.function.Predicate;

/**
 * 给其他模组用的扩展点。
 * <p>
 * 系列里的其他模组（面条物语、以后的小吃 / 酒馆等）接火锅时，<b>通过这里注册，不要去改火锅的源码</b>。
 * 这样火锅每次更新，子模组里的那份副本可以整包覆盖同步，不用人工逐行判断「这处改动要不要搬过去」。
 * <p>
 * 现有钩子（都是从面条物语实际需要的改动反推出来的）：
 * <ul>
 *   <li>{@link #registerBlockUse} —— 方块右键：外来物品先处理（煮面锅要放到灶台上、空碗要先盛汤）。</li>
 *   <li>{@link #registerChopsticksUse} —— 筷子 Shift+右键：外来的方块先处理（面条锅打开自己的界面）。</li>
 *   <li>{@link #registerCookable} —— 额外可下锅的物品（火锅默认只收有 FOOD 组件或有烹饪配方的）。</li>
 * </ul>
 * 全部是本地列表，不依赖任何外部模组；没有注册者时行为和不加钩子完全一样。
 * 作者：mryt233 */
public final class HotPotHooks {
    /** 方块右键钩子：返回非 null 表示这件事已经被接手，火锅不再继续处理。 */
    @FunctionalInterface
    public interface BlockUse {
        InteractionResult handle(Level world, Player player, InteractionHand hand, ItemStack stack,
                                BlockPos pos, BlockState state, BlockHitResult hit);
    }

    /** 筷子 Shift+右键钩子：返回非 null 表示已经接手。 */
    @FunctionalInterface
    public interface ChopsticksUse {
        InteractionResult handle(Level world, Player player, InteractionHand hand, ItemStack chopsticks,
                            BlockPos pos, BlockState state, BlockHitResult hit);
    }

    /**
     * 副手容器里的食物：比如面条碗端在副手时，筷子也能直接吃。
     * 火锅自己只认「筷子里夹着的菜」，这里补上「另一只手里装着的东西」。返回 null 表示没有。
     */
    @FunctionalInterface
    public interface OffhandFood {
        net.minecraft.world.food.FoodProperties foodIn(ItemStack offhand, net.minecraft.world.entity.LivingEntity user);
    }

    /** 筷子吃东西前后给外部的通知（面条碗要在结算前认出口、吃完刷红汤）。 */
    public interface EatListener {
        /** 即将吃掉这一份，此时物品栈还没变；返回 true 表示这次由它接管，火锅不再继续。 */
        default boolean beforeEating(ItemStack chopsticks, net.minecraft.world.entity.LivingEntity user) {
            return false;
        }

        /** 已经决定吃掉这一份食物，**在真正结算之前**调用。 */
        default void onEating(Level world, net.minecraft.world.entity.LivingEntity user, ItemStack food) {
        }
    }

    private static final Hooks<BlockUse> BLOCK_USES = new Hooks<>();
    private static final Hooks<ChopsticksUse> CHOPSTICKS_USES = new Hooks<>();
    private static final Hooks<Predicate<ItemStack>> EXTRA_COOKABLE = new Hooks<>();
    private static final Hooks<OffhandFood> OFFHAND_FOODS = new Hooks<>();
    private static final Hooks<EatListener> EAT_LISTENERS = new Hooks<>();

    private HotPotHooks() {
    }

    public static Hooks.Registration registerBlockUse(BlockUse hook) {
        return BLOCK_USES.add(hook);
    }

    public static Hooks.Registration registerChopsticksUse(ChopsticksUse hook) {
        return CHOPSTICKS_USES.add(hook);
    }

    /** 声明「这种物品也能下锅」，在火锅原有的判断之外追加。 */
    public static Hooks.Registration registerCookable(Predicate<ItemStack> canCook) {
        return EXTRA_COOKABLE.add(canCook);
    }

    public static Hooks.Registration registerOffhandFood(OffhandFood source) {
        return OFFHAND_FOODS.add(source);
    }

    public static Hooks.Registration registerEatListener(EatListener listener) {
        return EAT_LISTENERS.add(listener);
    }

    // ---------------------------------------------------------------- 内部调用

    /** 灶台 / 锅的右键入口调用；没人接手时返回 null。 */
    public static InteractionResult blockUse(Level world, Player player, InteractionHand hand, ItemStack stack,
                                            BlockPos pos, BlockState state, BlockHitResult hit) {
        for (BlockUse hook : BLOCK_USES.all()) {
            try {
                InteractionResult result = hook.handle(world, player, hand, stack, pos, state, hit);
                if (result != null) {
                    return result;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 方块右键钩子执行失败，已跳过", error);
            }
        }
        return null;
    }

    /** 筷子的 Shift+右键入口调用；没人接手时返回 null。 */
    public static InteractionResult chopsticksUse(Level world, Player player, InteractionHand hand, ItemStack chopsticks,
                                             BlockPos pos, BlockState state, BlockHitResult hit) {
        for (ChopsticksUse hook : CHOPSTICKS_USES.all()) {
            try {
                InteractionResult result = hook.handle(world, player, hand, chopsticks, pos, state, hit);
                if (result != null) {
                    return result;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 筷子钩子执行失败，已跳过", error);
            }
        }
        return null;
    }

    /** 诊断：当前钩子注册数量。 */
    public static int hookCounts() {
        return BLOCK_USES.size() + CHOPSTICKS_USES.size() + EXTRA_COOKABLE.size()
            + OFFHAND_FOODS.size() + EAT_LISTENERS.size();
    }

    /** 火锅自己的判断都不成立时，再问一遍外部注册的「能不能下锅」。 */
    public static boolean extraCookable(ItemStack stack) {
        for (Predicate<ItemStack> predicate : EXTRA_COOKABLE.all()) {
            try {
                if (predicate.test(stack)) {
                    return true;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 可下锅钩子执行失败，已跳过", error);
            }
        }
        return false;
    }

    /** 筷子里没夹菜时，问一问副手有没有能吃的东西；没有返回 null。 */
    public static net.minecraft.world.food.FoodProperties offhandFood(ItemStack chopsticks,
                                                                         net.minecraft.world.entity.LivingEntity user) {
        for (OffhandFood source : OFFHAND_FOODS.all()) {
            try {
                net.minecraft.world.food.FoodProperties food = source.foodIn(chopsticks, user);
                if (food != null) {
                    return food;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 副手食物钩子执行失败，已跳过", error);
            }
        }
        return null;
    }

    /** 吃东西之前；返回 true 表示这次由外部接管，火锅不再继续。 */
    public static boolean beforeEating(ItemStack chopsticks, net.minecraft.world.entity.LivingEntity user) {
        for (EatListener listener : EAT_LISTENERS.all()) {
            try {
                if (listener.beforeEating(chopsticks, user)) {
                    return true;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 进食前钩子执行失败，已跳过", error);
            }
        }
        return false;
    }

    /** 即将吃掉这份食物（真正结算之前，物品栈还没变）。 */
    public static void onEating(Level world, net.minecraft.world.entity.LivingEntity user, ItemStack food) {
        EAT_LISTENERS.forEach(listener -> listener.onEating(world, user, food),
            (listener, error) -> HotPot.LOGGER.warn("[hotpot] 进食结算钩子执行失败，已跳过", error));
    }
}
