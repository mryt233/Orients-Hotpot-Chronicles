// mryt233 改（2026-11-11）：适配 26.2 配方体系与 Identifier。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

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

    /**
     * mryt233 改（2026-11-05）：本模组各类生食材在火锅里的默认涮煮刻数（20 tick = 1 秒）。
     */
    private static final FrozenRegistry<String, Integer> DEFAULT_ITEM_COOK_TICKS = new FrozenRegistry<>(
        "默认涮煮时间表", "默认涮煮时间是启动期数据，加食材时在 registerDefaultItemCookTicks() 里登记。");

    static {
        Config.rememberCooking(DEFAULT_FALLBACK_TICKS, DEFAULT_FALLBACK_XP, DEFAULT_XP_PER_ITEM);
        registerDefaultItemCookTicks();
        DEFAULT_ITEM_COOK_TICKS.view().forEach(Config::rememberItemCookTicks);
    }

    /** 任务单定的四档默认涮煮时间：极快熟 50、快熟 100、中等 150、慢熟 200。 */
    private static void registerDefaultItemCookTicks() {
        putItemCookTicks(50, "beef_loin_slice", "tripe", "enoki_mushroom", "greens");
        putItemCookTicks(100, "fatty_beef_roll", "mutton_roll", "potato_slice", "cold_tofu_cube",
            "ham_sausage_slice", "peeled_shrimp",
            "lotus_root_slice", "squid_roll", "tofu_skin", "sweet_potato_skin");
        putItemCookTicks(150, "shrimp", "shrimp_paste", "raw_ham_sausage",
            "dried_tofu", "wide_noodles", "raw_crispy_pork", "crispy_pork");
        putItemCookTicks(200, "beef_loin", "beef_meatball", "pork_meatball", "mutton_meatball", "fish_ball",
            "cilantro_beef_meatball", "cilantro_pork_meatball", "cilantro_mutton_meatball", "cilantro_fish_ball",
            "zhe_er_gen_slice");
    }

    private static void putItemCookTicks(int ticks, String... ids) {
        for (String id : ids) {
            DEFAULT_ITEM_COOK_TICKS.put(id, ticks);
        }
    }

    /**
     * 物品的注册路径（命名空间下的 ID），用来查 cooking.items.&lt;路径&gt;；拿不到时返回 null。
     */
    private static String itemPath(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // mryt233 改（2026-11-10）：短键只属于本模组，第三方同名物品按自身配方煮。
        return key != null && HotPot.MOD_ID.equals(key.getNamespace()) ? key.getPath() : null;
    }

    // mryt233 改（2026-11-10）：配置和数据包重载共用修订号，避免每刻逐槽查配方。
    private static final AtomicLong COOK_REVISION = new AtomicLong();

    public static long cookRevision() {
        return COOK_REVISION.get();
    }

    public static void reviseCookTimes() {
        cachedOutputsSource = null;
        COOK_REVISION.incrementAndGet();
    }

    /** 数据包可追加或替换不许下锅的名单。mryt233 改（2026-11-11）。 */
    public static final TagKey<Item> NOT_POT_INGREDIENT =
        TagKey.create(Registries.ITEM, HotPot.id("not_pot_ingredient"));

    private HotPotCooking() {
    }

    /** 该食材能否下锅：可食用的东西都能涮；汤底原料与桌面物件等除外。 */
    public static boolean acceptable(Level world, ItemStack stack) {
        if (stack.isEmpty() || stack.is(NOT_POT_INGREDIENT)) {
            return false;
        }
        if (Soups.fromStack(stack) != null || TableObjects.item(stack) != null) {
            return false;
        }
        if (isCookedFood(world, stack)) {
            return false;
        }
        return HotPotRecipes.boiling(world, stack) != null
            || stack.has(DataComponents.FOOD)
            || stack.is(ItemTags.MEAT)
            || stack.is(ItemTags.FISHES)
            || foodRecipe(world, stack, RecipeType.SMOKING).isPresent()
            || foodRecipe(world, stack, RecipeType.CAMPFIRE_COOKING).isPresent()
            || HotPotHooks.extraCookable(stack);
    }

    /** 锅里用的烹饪配方：先烟熏炉、再营火；熔炉配方只给食物用。 */
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

    private static volatile Object cachedOutputsSource;
    private static volatile Set<Item> cachedOutputs = Set.of();

    // mryt233 改（2026-11-08，26.x）：缓存键使用 RecipeSync.source(world)，产物从配方 display 解析。
    private static Set<Item> cookedOutputsCached(Level world) {
        Object source = RecipeSync.source(world);
        if (cachedOutputsSource == source) {
            return cachedOutputs;
        }
        synchronized (HotPotCooking.class) {
            if (cachedOutputsSource == source) {
                return cachedOutputs;
            }
            cachedOutputs = Set.copyOf(cookedOutputs(world));
            cachedOutputsSource = source;
            return cachedOutputs;
        }
    }

    /** 已经熟了的食物：它是某个熔炉/烟熏炉/营火配方的产物，自身却没有烹饪配方。 */
    public static boolean isCookedFood(Level world, ItemStack stack) {
        if (HotPotRecipes.boiling(world, stack) != null || POT_RESULTS.containsKey(stack.getItem())) {
            return false;
        }
        if (POT_RESULTS.containsValue(stack.getItem()) || HotPotRecipes.all(world, PotBoilingRecipe.TYPE).stream()
            .anyMatch(e -> e.value().result().is(stack.getItem()))) {
            return true;
        }
        if (world == null || stack.isEmpty() || smeltingRecipe(world, stack).isPresent()) {
            return false;
        }
        return cookedOutputsCached(world).contains(stack.getItem());
    }

    /** 熔炉 / 烟熏炉 / 营火三种烧炼配方的全部产物（26.x 从 {@link RecipeSync} 取）。 */
    private static Set<Item> cookedOutputs(Level world) {
        Set<Item> out = new HashSet<>();
        for (RecipeHolder<?> entry : RecipeSync.recipes(world)) {
            if (!(entry.value() instanceof AbstractCookingRecipe)) {
                continue;
            }
            for (var display : entry.value().display()) {
                display.result()
                    .resolveForStacks(SlotDisplayContext.fromLevel(world))
                    .forEach(result -> out.add(result.getItem()));
            }
        }
        return out;
    }

    private static final FrozenRegistry<Item, Item> POT_RESULTS = new FrozenRegistry<>(
        "锅专用产物表", "锅专用煮法要在启动期登记（子模组在自己的兼容模块里调 potResult），冻结后不再接受登记。");

    /** mryt233 改（2026-11-11）：保留启动期登记；同原料数据包配方优先，3B 冻结规则不变。 */
    public static void potResult(Item raw, Item cooked) {
        POT_RESULTS.put(raw, cooked);
    }

    private static final FrozenRegistry<Item, Item> OVERCOOK = new FrozenRegistry<>(
        "煮过头产物表", "煮过头的玩法要在启动期登记（子模组在自己的兼容模块里调 overcook），冻结后不再接受登记。");

    /** mryt233 改（2026-11-11）：保留启动期登记；同熟食的数据包煮过头规则优先，冻结后拒绝登记。 */
    public static void overcook(Item cooked, Item overcooked) {
        OVERCOOK.put(cooked, overcooked);
    }

    /** 这样熟食的煮过头产物；没登记过返回 {@code null}。 */
    public static Item overcookProduct(Item cooked) {
        return overcookProduct(HotPotRecipes.currentLevel(), cooked);
    }

    /** 带世界的查询避免客户端与集成服共用配方管理器。mryt233 改（2026-11-11）。 */
    public static Item overcookProduct(Level world, Item cooked) {
        ItemStack result = overcookResult(world, cooked);
        return result.isEmpty() ? null : result.getItem();
    }

    /** mryt233 改（2026-11-11）：运行期保留配方产物组件，旧 Item 返回入口继续兼容。 */
    public static ItemStack overcookResult(Level world, Item cooked) {
        if (cooked == null) {
            return ItemStack.EMPTY;
        }
        PotBoilingRecipe recipe = HotPotRecipes.overcooking(world, new ItemStack(cooked));
        if (recipe != null) {
            return recipe.overcookResult();
        }
        Item legacy = OVERCOOK.get(cooked);
        return legacy == null ? ItemStack.EMPTY : new ItemStack(legacy);
    }

    /** 配置里没有「煮过头」这一项时的兜底总刻数（30 秒）。 */
    public static final int DEFAULT_OVERCOOK_TICKS = 600;

    public static int overcookTicks(Level world, Item happyCooked) {
        if (happyCooked == HotPotContent.HOT_TOFU && Config.has("cooking.tofu_overcook_ticks")) {
            return Config.tofuOvercookTicks(DEFAULT_OVERCOOK_TICKS);
        }
        PotBoilingRecipe recipe = HotPotRecipes.overcooking(world, new ItemStack(happyCooked));
        return recipe == null ? DEFAULT_OVERCOOK_TICKS : recipe.overcookTicks().orElse(DEFAULT_OVERCOOK_TICKS);
    }

    public static int cookTicks(Level world, ItemStack stack) {
        Integer configured = null;
        if (stack.is(HotPotContent.COLD_TOFU) && Config.has("cooking.tofu_cook_ticks")) {
            configured = Config.tofuCookTicks(HotPotContent.TOFU_COOK_TICKS);
        } else {
            String path = itemPath(stack);
            if (path != null && Config.hasItemCookTicks(path)) {
                Integer ticks = DEFAULT_ITEM_COOK_TICKS.get(path);
                configured = Config.itemCookTicks(path, ticks == null ? DEFAULT_FALLBACK_TICKS : ticks);
            }
        }
        return cookingTime(configured, HotPotRecipes.boiling(world, stack), () -> smeltingRecipe(world, stack)
            .map(e -> e.value().cookingTime()).orElse(Config.fallbackTicks(DEFAULT_FALLBACK_TICKS)));
    }

    /** 时间优先级与惰性兜底；单测也使用实际查询的选择函数。mryt233 改（2026-11-11）。 */
    static int cookingTime(Integer configured, PotBoilingRecipe recipe, IntSupplier vanilla) {
        if (configured != null) {
            return configured;
        }
        return recipe != null && recipe.cookTicks().isPresent() ? recipe.cookTicks().get() : vanilla.getAsInt();
    }

    /** 每 tick 熟度增量：燃料功率 × 汤底倍率折算到千分比。 */
    public static int progressPerTick(int cookTicks, float power, float soupMul) {
        return Math.max(1, Math.round(FULL_DONE * Math.max(0.1F, power) * Math.max(0.1F, soupMul) / cookTicks));
    }

    public static ItemStack finish(Level world, ItemStack raw, SoupBase soup) {
        return finishWithSoup(world, raw, soup == null ? null : soup.def());
    }

    /** 同上，按登记表里的汤底定义。 */
    public static ItemStack finishWithSoup(Level world, ItemStack raw, SoupDef soup) {
        int portions = Math.max(1, raw.getCount());
        PotBoilingRecipe recipe = HotPotRecipes.boiling(world, raw);
        if (recipe != null) {
            return recipe.result().copyWithCount(portions);
        }
        Item potOnly = POT_RESULTS.get(raw.getItem());
        if (potOnly != null) {
            return new ItemStack(potOnly, portions);
        }
        ItemStack result = smeltingRecipe(world, raw)
            .map(entry -> entry.value().assemble(new SingleRecipeInput(raw.copyWithCount(1))).copy())
            .orElseGet(() -> raw.copyWithCount(portions));
        result.setCount(portions);
        return result;
    }

    /** 煮熟一份食材应给的经验（熔炉同款：优先取配方经验，没有则兜底）。 */
    public static float xpFor(Level world, ItemStack raw) {
        PotBoilingRecipe recipe = HotPotRecipes.boiling(world, raw);
        if (recipe != null && recipe.experience().isPresent()) {
            return recipe.experience().get();
        }
        return smeltingRecipe(world, raw).map(e -> Math.max(e.value().experience(), Config.xpPerItem(DEFAULT_XP_PER_ITEM)))
            .orElse(Config.fallbackXp(DEFAULT_FALLBACK_XP));
    }

    /** 冻结锅专用产物表、煮过头表、默认涮煮时间表（服务端启动时调用，幂等）。 */
    public static void freezeRegistries() {
        POT_RESULTS.freeze();
        OVERCOOK.freeze();
        DEFAULT_ITEM_COOK_TICKS.freeze();
    }

    /** 诊断：三张表是否都已经冻结。 */
    public static boolean registriesFrozen() {
        return POT_RESULTS.isFrozen() && OVERCOOK.isFrozen() && DEFAULT_ITEM_COOK_TICKS.isFrozen();
    }

    /** 诊断：某张表的名字 + 条数（测试与 /hpt 用）。 */
    public static String registrySummary() {
        return "pot_results=" + POT_RESULTS.size() + "/" + POT_RESULTS.isFrozen()
            + " overcook=" + OVERCOOK.size() + "/" + OVERCOOK.isFrozen()
            + " cook_ticks=" + DEFAULT_ITEM_COOK_TICKS.size() + "/" + DEFAULT_ITEM_COOK_TICKS.isFrozen();
    }

    /** 熟度千分比 [0..1]。 */
    public static float ratio(int doneness) {
        return Math.max(0F, Math.min(1F, doneness / (float) FULL_DONE));
    }
}
