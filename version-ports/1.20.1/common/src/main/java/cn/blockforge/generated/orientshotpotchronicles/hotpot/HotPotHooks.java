package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import cn.blockforge.orient.framework.game.Hooks;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 给其他模组用的扩展点（mryt233 改：1.20.1 适配 framework compat ItemInteractionResult）。
 * 作者：mryt233 */
public final class HotPotHooks {
    @FunctionalInterface
    public interface BlockUse {
        ItemInteractionResult handle(Level world, Player player, InteractionHand hand, ItemStack stack,
                                     BlockPos pos, BlockState state, BlockHitResult hit);
    }

    @FunctionalInterface
    public interface ChopsticksUse {
        InteractionResult handle(Level world, Player player, InteractionHand hand, ItemStack chopsticks,
                                 BlockPos pos, BlockState state, BlockHitResult hit);
    }

    @FunctionalInterface
    public interface OffhandFood {
        FoodProperties foodIn(ItemStack offhand, LivingEntity user);
    }

    public interface EatListener {
        default boolean beforeEating(ItemStack chopsticks, LivingEntity user) {
            return false;
        }

        default void onEating(Level world, LivingEntity user, ItemStack food) {
        }
    }

    public static void freezeRegistries() {
        HotPotCooking.freezeRegistries();
        SoupBase.freezeSources();
        Soups.freeze();
        Seasonings.freeze();
        HotPotFuel.freezeSources();
        if (!HotPotCooking.registriesFrozen()) {
            throw new IllegalStateException("登记表冻结失败");
        }
    }

    public static boolean registriesFrozen() {
        return HotPotCooking.registriesFrozen();
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

    public static Hooks.Registration registerBlockUse(int priority, BlockUse hook) {
        return BLOCK_USES.add(priority, hook);
    }

    public static Hooks.Registration registerChopsticksUse(ChopsticksUse hook) {
        return CHOPSTICKS_USES.add(hook);
    }

    public static Hooks.Registration registerChopsticksUse(int priority, ChopsticksUse hook) {
        return CHOPSTICKS_USES.add(priority, hook);
    }

    public static Hooks.Registration registerCookable(Predicate<ItemStack> canCook) {
        return EXTRA_COOKABLE.add(canCook);
    }

    public static Hooks.Registration registerCookable(int priority, Predicate<ItemStack> canCook) {
        return EXTRA_COOKABLE.add(priority, canCook);
    }

    public static Hooks.Registration registerOffhandFood(OffhandFood source) {
        return OFFHAND_FOODS.add(source);
    }

    public static Hooks.Registration registerOffhandFood(int priority, OffhandFood source) {
        return OFFHAND_FOODS.add(priority, source);
    }

    public static Hooks.Registration registerEatListener(EatListener listener) {
        return EAT_LISTENERS.add(listener);
    }

    public static Hooks.Registration registerEatListener(int priority, EatListener listener) {
        return EAT_LISTENERS.add(priority, listener);
    }

    public static ItemInteractionResult blockUse(Level world, Player player, InteractionHand hand, ItemStack stack,
                                                 BlockPos pos, BlockState state, BlockHitResult hit) {
        for (BlockUse hook : BLOCK_USES.all()) {
            try {
                ItemInteractionResult result = hook.handle(world, player, hand, stack, pos, state, hit);
                if (result != null) {
                    return result;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 方块右键钩子执行失败，已跳过", error);
            }
        }
        return null;
    }

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

    public static int hookCounts() {
        return BLOCK_USES.size() + CHOPSTICKS_USES.size() + EXTRA_COOKABLE.size()
            + OFFHAND_FOODS.size() + EAT_LISTENERS.size();
    }

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

    public static FoodProperties offhandFood(ItemStack chopsticks, LivingEntity user) {
        for (OffhandFood source : OFFHAND_FOODS.all()) {
            try {
                FoodProperties food = source.foodIn(chopsticks, user);
                if (food != null) {
                    return food;
                }
            } catch (Throwable error) {
                HotPot.LOGGER.warn("[hotpot] 副手食物钩子执行失败，已跳过", error);
            }
        }
        return null;
    }

    public static boolean beforeEating(ItemStack chopsticks, LivingEntity user) {
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

    public static void onEating(Level world, LivingEntity user, ItemStack food) {
        EAT_LISTENERS.forEach(listener -> listener.onEating(world, user, food),
            (listener, error) -> HotPot.LOGGER.warn("[hotpot] 进食结算钩子执行失败，已跳过", error));
    }
}
