package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 熔炉式烹饪工具：食材在锅里加热，熟了就变成对应的熟食
 * （能进熔炉的走熔炉配方，煮不了的原样端出）。汤底只影响加热速度，
 * 产物与原版熟食保持一致，因此不同类型汤底煮出的同种食物也能正常堆叠。
 */
public final class HotPotCooking {
    /** 熟度千分比上限（结构性常量，不进配置文件）。 */
    public static final int FULL_DONE = 1000;
    /** 没有熔炉配方的食材（焯熟蔬菜等）的兜底耗时与经验：这里是默认值，配置文件里的 cooking.* 可以覆盖。 */
    public static final int DEFAULT_FALLBACK_TICKS = 200;
    public static final float DEFAULT_FALLBACK_XP = 0.15F;
    /** 无配方食材的兜底经验（按份计）。 */
    public static final float DEFAULT_XP_PER_ITEM = 0.7F;

    static {
        // 默认值记进配置模板
        Config.rememberCooking(DEFAULT_FALLBACK_TICKS, DEFAULT_FALLBACK_XP, DEFAULT_XP_PER_ITEM);
    }

    private HotPotCooking() {
    }

    /** 该食材能否下锅：可食用的东西都能涮；汤底原料与筷子除外。 */
    public static boolean acceptable(Level world, ItemStack stack) {
        if (stack.isEmpty() || stack.is(HotPotContent.CHOPSTICKS)) {
            return false;
        }
        if (SoupBase.fromStack(stack) != null || PlateItem.placedFor(stack) != null) {
            return false;   // 汤底原料；碗、汤碗、橙子饮料这些（r99）
        }
        // 熟牛肉、烤土豆这类已经熟了的东西没有自己的熔炉配方，以前会掉进“无配方兜底”
        // 被当成焯菜再煮一遍（重新读条、还能重复刷经验），这里直接拒收。
        if (isCookedFood(world, stack)) {
            return false;
        }
        // 原版与第三方食物优先读取 FOOD 组件；肉/鱼标签作为没有组件时的兜底，
        // 这样常见火锅食材不会因为模组配方注册方式不同而完全放不进去。
        // 0.1.2-alpha 后用户报：对着锅右键什么都能下锅（木头煮成木炭、铁器煮成铁粒）——以前「熔炉能烧」也算。
        // 改成照烟熏炉：只收能吃的东西，或者有烟熏炉 / 营火配方的东西。
        return stack.has(DataComponents.FOOD)
            || stack.is(ItemTags.MEAT)
            || stack.is(ItemTags.FISHES)
            || foodRecipe(world, stack, RecipeType.SMOKING).isPresent()
            || foodRecipe(world, stack, RecipeType.CAMPFIRE_COOKING).isPresent()
            || HotPotHooks.extraCookable(stack);   // 其他模组声明「这个也能涮」
    }

    /**
     * 锅里用的烹饪配方，用来取熟食产出、耗时与经验：先烟熏炉、再营火（这两种只有食物）；
     * 熔炉配方只给食物用（熔炉还会烧木头、矿石，锅不能当熔炉，0.1.2-alpha 后 调整）。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeHolder<? extends AbstractCookingRecipe>> smeltingRecipe(Level world, ItemStack stack) {
        if (world == null || stack.isEmpty()) {
            return Optional.empty();
        }
        for (RecipeType type : new RecipeType[]{RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING}) {
            Optional entry = foodRecipe(world, stack, type);
            if (entry.isPresent()) {
                return entry;
            }
        }
        if (stack.has(DataComponents.FOOD) || stack.is(ItemTags.MEAT) || stack.is(ItemTags.FISHES)) {
            return (Optional) foodRecipe(world, stack, RecipeType.SMELTING);
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<RecipeHolder<? extends AbstractCookingRecipe>> foodRecipe(Level world, ItemStack stack, RecipeType type) {
        if (world == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return RecipeSync.find(world, type, stack);
    }

    /**
     * 已经熟了的食物：它是某个熔炉/烟熏炉/营火配方的产物，自身却没有烹饪配方。
     * 每次现查配方表（只在放入食材时调用，量很小），数据包重载后也不会过期。
     */
    public static boolean isCookedFood(Level world, ItemStack stack) {
        if (POT_RESULTS.containsValue(stack.getItem())) {
            return true;   // 锅专用产物（熟海带）也是熟的，不能再下锅
        }
        if (world == null || stack.isEmpty() || smeltingRecipe(world, stack).isPresent()) {
            return false;
        }
        Item item = stack.getItem();
        return isOutputOf(RecipeType.SMELTING, world, item)
            || isOutputOf(RecipeType.SMOKING, world, item)
            || isOutputOf(RecipeType.CAMPFIRE_COOKING, world, item);
    }

    private static <T extends AbstractCookingRecipe> boolean isOutputOf(RecipeType<T> type,
                                                                        Level world, Item item) {
        for (RecipeHolder<?> entry : RecipeSync.recipes(world)) {
            if (entry.value().getType() == type) {
                for (var display : entry.value().display()) {
                    if (display.result().resolveForStacks(net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(world))
                            .stream().anyMatch(result -> result.is(item))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 只在锅里才有的产物（2026-10-25 用户：海带在火锅里煮成「熟海带」，原版熔炉 / 烟熏炉照旧出干海带）：
     * 耗时和经验还照原版配方，只换产物。以后锅（或模组别的厨具）专用的煮法都加在这里。新增
     */
    private static final java.util.Map<Item, Item> POT_RESULTS = new java.util.HashMap<>();

    public static void potResult(Item raw, Item cooked) {
        POT_RESULTS.put(raw, cooked);
    }

    /** 标准煮熟耗时（tick），无配方食材用兜底值。 */
    public static int cookTicks(Level world, ItemStack stack) {
        return smeltingRecipe(world, stack).map(e -> e.value().cookingTime()).orElse(Config.fallbackTicks(DEFAULT_FALLBACK_TICKS));
    }

    /** 每 tick 熟度增量：燃料功率 × 汤底倍率折算到千分比。 */
    public static int progressPerTick(int cookTicks, float power, float soupMul) {
        return Math.max(1, Math.round(FULL_DONE * Math.max(0.1F, power) * Math.max(0.1F, soupMul) / cookTicks));
    }

    /**
     * 煮熟瞬间的产物转换：有熔炉配方则变成对应熟食，否则原样保留。
     *
     * <p>产物保持与原版熟食完全相同的 NBT/Data Component，这样锅煮出来的食物可以
     * 和熔炉、烟熏炉等其它来源的食物正常堆叠（同类型的多份会合并成一组），不会因为
     * 挂在成品身上的汤底效果使组件不一致而“同类的不能堆叠”。</p>
     */
    public static ItemStack finish(Level world, ItemStack raw, SoupBase soup) {
        int portions = Math.max(1, raw.getCount());
        Item potOnly = POT_RESULTS.get(raw.getItem());
        if (potOnly != null) {
            return new ItemStack(potOnly, portions);
        }
        // 原版 AbstractCookingRecipe#getResult() 返回的是配方自身缓存的 result 实例本身，
        // 不是拷贝（SimpleInventory#setStack 也只存引用）。若把它直接塞进锅格或在上面
        // setCount，所有同配方的熟食就会共用同一个 ItemStack 对象：之后任意一格被
        // copyAndEmpty() 清空，其余同款熟食会跟着变成 0 个——表现为“一键收集只剩一个”。
        // 因此这里必须取一份自有拷贝，绝不持有或改动配方缓存的那个对象。
        ItemStack result = smeltingRecipe(world, raw)
            .map(entry -> entry.value().assemble(new SingleRecipeInput(raw.copyWithCount(1))).copy())
            .orElseGet(() -> raw.copyWithCount(portions));
        result.setCount(portions);
        return result;
    }

    /** 煮熟一份食材应给的经验（熔炉同款：优先取配方经验，没有则兜底）。 */
    public static float xpFor(Level world, ItemStack raw) {
        return smeltingRecipe(world, raw).map(e -> Math.max(e.value().experience(), Config.xpPerItem(DEFAULT_XP_PER_ITEM)))
            .orElse(Config.fallbackXp(DEFAULT_FALLBACK_XP));
    }

    /** 熟度千分比 [0..1]。 */
    public static float ratio(int doneness) {
        return Math.max(0F, Math.min(1F, doneness / (float) FULL_DONE));
    }
}
