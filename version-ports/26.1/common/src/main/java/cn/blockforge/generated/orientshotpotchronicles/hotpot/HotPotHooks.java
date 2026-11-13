// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;

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
 *
 * <h2>登记时机（mryt233 改 2026-11-10，r147）</h2>
 * <p>钩子和下面这些登记表都<b>只能在启动期登记</b>：</p>
 * <ul>
 *   <li>钩子（{@code register*}）：模组初始化 / 兼容模块 setup 时登记，之后一直有效，随时可以注销。</li>
 *   <li>锅专用产物 {@link HotPotCooking#potResult}、煮过头 {@link HotPotCooking#overcook}、
 *       第三方汤底来源 {@link SoupBase#registerSource}、汤底登记表 {@link Soups}、第三方燃料 {@link HotPotFuel#registerCustomSource}：
 *       这些表在<b>服务端开始启动那一刻</b>冻结（{@link #freezeRegistries()}，单人和专用服都会走到）。
 *       冻结之后再登记会抛 {@link IllegalStateException}，异常里写明是哪张表。
 *       冻结点在 {@link HotPotHooks#freezeRegistries()}：Fabric 在 {@code ServerLifecycleEvents.SERVER_STARTING}，
 *       NeoForge 在 {@code ServerStartingEvent}（{@code HotPotRuntime} 的 {@code onServerStarting} 回调，
 *       不是 {@code ServerAboutToStartEvent}；mryt233 改 2026-11-11 更正此前的说法）——两边都晚于兼容模块
 *       （Fabric 初始化末尾 / NeoForge {@code FMLCommonSetupEvent}）。</li>
 * </ul>
 * <p>为什么冻结一次就一直冻：单人游戏退出世界再进另一个世界时不会重新登记，也不需要「解冻 / 再冻结」；
 * 若允许每次开服重新解冻，就会出现「刚进新世界那一小段窗口期里，晚到的第三方登记能够悄悄改掉已生效的表」
 * 这种不确定状态。需要新登记的东西应该放进它自己的兼容模块，在启动期跑。</p>
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
        FoodProperties foodIn(ItemStack offhand, LivingEntity user);
    }

    /**
     * mryt233 改（2026-11-10）：手持筷子物品进食、锅界面用筷子点熟食两条路径都会触发。
     * 每口各通知一次；beforeEating 接管后不扣食物，也不触发 onEating。
     */
    public interface EatListener {
        /** 即将吃掉这一份，此时物品栈还没变；返回 true 表示这次由它接管，火锅不再继续。 */
        default boolean beforeEating(ItemStack chopsticks, LivingEntity user) {
            return false;
        }

        /** 已经决定吃掉这一份食物，**在真正结算之前**调用。 */
        default void onEating(Level world, LivingEntity user, ItemStack food) {
        }
    }

    /**
     * 冻结所有「登记期可写、之后只读」的表。
     * <p>mryt233 改（2026-11-10，r147）：服务端开始启动时调用（单人和专用服都覆盖），
     * 确保两个加载器上的兼容模块都已经登记完。重复调用是幂等的。</p>
     * <p>冻结点：Fabric {@code ServerLifecycleEvents.SERVER_STARTING}／NeoForge {@code ServerStartingEvent}
     * （都是 {@code HotPotRuntime} 的 {@code onServerStarting} 回调；mryt233 改 2026-11-11 更正旧注释里的
     * {@code ServerAboutToStartEvent}）。</p>
     */
    public static void freezeRegistries() {
        HotPotCooking.freezeRegistries();
        SoupBase.freezeSources();
        Soups.freeze();   // mryt233 改（2026-11-11，4B-1）：汤底登记表
        Seasonings.freeze();   // mryt233 改（4C）：蘸料登记表
        HotPotFuel.freezeSources();
        if (!HotPotCooking.registriesFrozen()) {
            throw new IllegalStateException("登记表冻结失败");
        }
    }

    /** 诊断：登记表现在冻结了没有。 */
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

    /*
     * mryt233 改（2026-11-10，r147）：每个 register* 都多一个带 int priority 的重载，转给框架 Hooks.add(priority, listener)。
     * 优先级大者先执行，同级按登记顺序（见 {@link Hooks}）。原来不带参数的重载保持不变，走默认优先级 0，
     * 已有调用一行都不用改。要「我的钩子先看一眼」的子模组用带优先级那一版。
     *
     * 登记时机：这些钩子在启动期登记（模组初始化 / FMLCommonSetupEvent），之后一直有效；
     * 返回的 {@link Hooks.Registration} 可以注销。
     */

    public static Hooks.Registration registerBlockUse(BlockUse hook) {
        return BLOCK_USES.add(hook);
    }

    /** 带优先级的方块右键钩子（数字越大越先调用）。 */
    public static Hooks.Registration registerBlockUse(int priority, BlockUse hook) {
        return BLOCK_USES.add(priority, hook);
    }

    public static Hooks.Registration registerChopsticksUse(ChopsticksUse hook) {
        return CHOPSTICKS_USES.add(hook);
    }

    /** 带优先级的筷子 Shift+右键钩子（数字越大越先调用）。 */
    public static Hooks.Registration registerChopsticksUse(int priority, ChopsticksUse hook) {
        return CHOPSTICKS_USES.add(priority, hook);
    }

    /** 声明「这种物品也能下锅」，在火锅原有的判断之外追加。 */
    public static Hooks.Registration registerCookable(Predicate<ItemStack> canCook) {
        return EXTRA_COOKABLE.add(canCook);
    }

    /** 带优先级的「也能下锅」钩子（数字越大越先问）。 */
    public static Hooks.Registration registerCookable(int priority, Predicate<ItemStack> canCook) {
        return EXTRA_COOKABLE.add(priority, canCook);
    }

    public static Hooks.Registration registerOffhandFood(OffhandFood source) {
        return OFFHAND_FOODS.add(source);
    }

    /** 带优先级的副手食物钩子（数字越大越先问）。 */
    public static Hooks.Registration registerOffhandFood(int priority, OffhandFood source) {
        return OFFHAND_FOODS.add(priority, source);
    }

    public static Hooks.Registration registerEatListener(EatListener listener) {
        return EAT_LISTENERS.add(listener);
    }

    /** 带优先级的进食钩子（数字越大越先调用）。 */
    public static Hooks.Registration registerEatListener(int priority, EatListener listener) {
        return EAT_LISTENERS.add(priority, listener);
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
    public static FoodProperties offhandFood(ItemStack chopsticks,
                                                                         LivingEntity user) {
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

    /** 吃东西之前；返回 true 表示这次由外部接管，火锅不再继续。 */
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

    /** 即将吃掉这份食物（真正结算之前，物品栈还没变）。 */
    public static void onEating(Level world, LivingEntity user, ItemStack food) {
        EAT_LISTENERS.forEach(listener -> listener.onEating(world, user, food),
            (listener, error) -> HotPot.LOGGER.warn("[hotpot] 进食结算钩子执行失败，已跳过", error));
    }
}
