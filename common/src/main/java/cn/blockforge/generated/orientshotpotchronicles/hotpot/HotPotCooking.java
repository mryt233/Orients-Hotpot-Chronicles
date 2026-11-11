package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

    /**
     * mryt233 改（2026-11-05）：本模组各类生食材在火锅里的默认涮煮刻数（20 tick = 1 秒）。
     * 快煮快涮、慢煮慢涮，按食材特性区分；登记进配置 cooking.items.&lt;物品注册路径&gt;，
     * 以后直接改 config/orients_hotpot_chronicles.json，不用去改数据包熔炉配方。
     * 整块冷豆腐不在这里（它走 cooking.tofu_cook_ticks，见 cookTicks）。
     */
    private static final FrozenRegistry<String, Integer> DEFAULT_ITEM_COOK_TICKS = new FrozenRegistry<>(
        "默认涮煮时间表", "默认涮煮时间是启动期数据，加食材时在 registerDefaultItemCookTicks() 里登记。");

    static {
        // 默认值记进配置模板
        Config.rememberCooking(DEFAULT_FALLBACK_TICKS, DEFAULT_FALLBACK_XP, DEFAULT_XP_PER_ITEM);
        registerDefaultItemCookTicks();
        DEFAULT_ITEM_COOK_TICKS.view().forEach(Config::rememberItemCookTicks);
    }

    /** 任务单定的四档默认涮煮时间：极快熟 50、快熟 100、中等 150、慢熟 200。 */
    private static void registerDefaultItemCookTicks() {
        // 极快熟：吊龙、毛肚、金针菇、青菜（2.5 秒；后两样 mryt233 2026-11-06 加，r137）
        putItemCookTicks(50, "beef_loin_slice", "tripe", "enoki_mushroom", "greens");
        // 快熟：肥牛卷、羊肉卷、土豆片、冷豆腐块、火腿肠片、虾仁（5 秒）；
        // r137 追加藕片、鱿鱼卷、腐竹、苕皮
        putItemCookTicks(100, "fatty_beef_roll", "mutton_roll", "potato_slice", "cold_tofu_cube",
            "ham_sausage_slice", "peeled_shrimp",
            "lotus_root_slice", "squid_roll", "tofu_skin", "sweet_potato_skin");
        // 中等：带壳虾、虾滑、生火腿肠（7.5 秒）；r137 追加豆干、宽粉、生酥肉、酥肉
        putItemCookTicks(150, "shrimp", "shrimp_paste", "raw_ham_sausage",
            "dried_tofu", "wide_noodles", "raw_crispy_pork", "crispy_pork");
        // 慢熟：整块牛脊肉 + 8 种丸子（10 秒）
        putItemCookTicks(200, "beef_loin", "beef_meatball", "pork_meatball", "mutton_meatball", "fish_ball",
            "cilantro_beef_meatball", "cilantro_pork_meatball", "cilantro_mutton_meatball", "cilantro_fish_ball",
            // mryt233 改（2026-11-06，r139）：折耳根段 10 秒（腥气煮掉大半）
            "zhe_er_gen_slice");
    }

    private static void putItemCookTicks(int ticks, String... ids) {
        for (String id : ids) {
            DEFAULT_ITEM_COOK_TICKS.put(id, ticks);
        }
    }

    /**
     * 物品的注册路径（命名空间下的 ID），用来查 cooking.items.&lt;路径&gt;；拿不到时返回 null。
     * 用注册表取而不是写死字符串，物品改 ID 时配置键跟着变。
     */
    private static String itemPath(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // mryt233 改（2026-11-10）：短键只属于本模组，第三方同名物品按自身配方煮。
        return key != null && HotPot.MOD_ID.equals(key.getNamespace()) ? key.getPath() : null;
    }

    // mryt233 改（2026-11-10）：配置和数据包重载共用修订号，避免每刻逐槽查配方。
    private static final AtomicLong COOK_REVISION = new AtomicLong();

    public static long cookRevision() {
        return COOK_REVISION.get();
    }

    public static void reviseCookTimes() {
        cachedOutputsManager = null;
        COOK_REVISION.incrementAndGet();
    }

    /** 数据包可追加或替换不许下锅的名单。mryt233 改（2026-11-11）。 */
    public static final TagKey<Item> NOT_POT_INGREDIENT =
        TagKey.create(Registries.ITEM, HotPot.id("not_pot_ingredient"));

    private HotPotCooking() {
    }

    /**
     * 该食材能否下锅：可食用的东西都能涮；汤底原料与筷子除外。
     * mryt233 改（2026-11-05）：辣椒（r136a）只参与红汤辣度调节，不当下锅煮的食材——拿着辣椒右键红汤锅
     * 是加辣，不能被当成生食材塞进锅槽。
     */
    public static boolean acceptable(Level world, ItemStack stack) {
        // mryt233 改（2026-11-11）：数据包标签优先于配方与子模组放行钩子。
        if (stack.isEmpty() || stack.is(NOT_POT_INGREDIENT)) {
            return false;
        }
        if (Soups.fromStack(stack) != null || PlateItem.placedFor(stack) != null) {
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
        return HotPotRecipes.boiling(world, stack) != null
            || stack.has(DataComponents.FOOD)
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
        return world.getRecipeManager().getRecipeFor(type, new SingleRecipeInput(stack.copyWithCount(1)), world);
    }

    /**
     * mryt233 改（2026-11-07）：烹饪配方产物集合缓存。
     * <p>以前 isCookedFood 对没有原版烹饪配方的物品要线性扫烧炼 / 烟熏 / 营火三张表，
     * 而漏斗的 canPlaceItem、面板的 mayPlace 都会调它，等于每刻扫几百条配方。
     * 这里按 RecipeManager 实例缓存成集合：/reload 会换一个新 manager，缓存自动作废。</p>
     */
    private static volatile RecipeManager cachedOutputsManager;
    private static volatile Set<Item> cachedOutputs = Set.of();

    private static Set<Item> cookedOutputs(RecipeManager manager, Level world) {
        if (cachedOutputsManager == manager) {
            return cachedOutputs;
        }
        synchronized (HotPotCooking.class) {
            if (cachedOutputsManager == manager) {
                return cachedOutputs;
            }
            Set<Item> out = new HashSet<>();
            collectOutputs(out, manager, RecipeType.SMELTING, world);
            collectOutputs(out, manager, RecipeType.SMOKING, world);
            collectOutputs(out, manager, RecipeType.CAMPFIRE_COOKING, world);
            cachedOutputs = Set.copyOf(out);
            cachedOutputsManager = manager;
            return cachedOutputs;
        }
    }

    private static <T extends AbstractCookingRecipe> void collectOutputs(
            Set<Item> out, RecipeManager manager, RecipeType<T> type, Level world) {
        for (RecipeHolder<T> entry : manager.getAllRecipesFor(type)) {
            out.add(entry.value().getResultItem(world.registryAccess()).getItem());
        }
    }

    /**
     * 已经熟了的食物：它是某个熔炉/烟熏炉/营火配方的产物，自身却没有烹饪配方。
     * mryt233 改（2026-11-07）：改为查缓存集合，不再每次线性遍历配方表；数据包 reload 后自动失效。
     */
    public static boolean isCookedFood(Level world, ItemStack stack) {
        // mryt233 改（2026-11-06，r137）：酥肉二次下锅。酥肉（crispy_pork）是烧炼配方的产物，按下面那条会被判成
        // 「已经熟了的食物」而不能下锅；但它登记在 POT_RESULTS 里还能继续涮成煮酥肉，所以先放行。
        // 煮酥肉在 POT_RESULTS.values() 里，紧接着的 containsValue 会把它拦下，不会无限往下煮。
        if (HotPotRecipes.boiling(world, stack) != null || POT_RESULTS.containsKey(stack.getItem())) {
            return false;
        }
        if (POT_RESULTS.containsValue(stack.getItem()) || HotPotRecipes.all(world, PotBoilingRecipe.TYPE).stream()
            .anyMatch(e -> e.value().result().is(stack.getItem()))) {
            return true;   // 锅专用产物（熟海带、煮酥肉）也是熟的，不能再下锅
        }
        if (world == null || stack.isEmpty() || smeltingRecipe(world, stack).isPresent()) {
            return false;
        }
        return cookedOutputs(world.getRecipeManager(), world).contains(stack.getItem());
    }

    /**
     * 只在锅里才有的产物（2026-10-25 用户：海带在火锅里煮成「熟海带」，原版熔炉 / 烟熏炉照旧出干海带）：
     * mryt233 改（2026-11-11）：内置条目迁到 pot_boiling 配方；本表仅保留子模组 Java 登记兼容。
     */
    // mryt233 改（2026-11-10，r147）：改成冻结登记表（见 FrozenRegistry / HotPotHooks.freezeRegistries）
    private static final FrozenRegistry<Item, Item> POT_RESULTS = new FrozenRegistry<>(
        "锅专用产物表", "锅专用煮法要在启动期登记（子模组在自己的兼容模块里调 potResult），冻结后不再接受登记。");

    /** mryt233 改（2026-11-11）：保留启动期登记；同原料数据包配方优先，3B 冻结规则不变。 */
    public static void potResult(Item raw, Item cooked) {
        POT_RESULTS.put(raw, cooked);
    }

    /**
     * mryt233 改（2026-11-04）：煮熟之后继续煮会换掉的产物（豆腐改造用）。
     * 生食材 → 熟食的转换在 {@link #finish}；「熟食 → 煮过头产物」在这里登记，锅格到计时后换成 newItem。
     * 做成通用的表，别把豆腐写死在锅里：以后别的食材要「煮过头就碎」也加一行。
     */
    // mryt233 改（2026-11-10，r147）：改成冻结登记表
    private static final FrozenRegistry<Item, Item> OVERCOOK = new FrozenRegistry<>(
        "煮过头产物表", "煮过头的玩法要在启动期登记（子模组在自己的兼容模块里调 overcook），冻结后不再接受登记。");

    /** mryt233 改（2026-11-11）：保留启动期登记；同熟食的数据包煮过头规则优先，冻结后拒绝登记。 */
    public static void overcook(Item cooked, Item overcooked) {
        OVERCOOK.put(cooked, overcooked);
    }

    /** 这样熟食的煮过头产物；没登记过返回 {@code null}（= 不会煮过头，一直留在锅里）。 */
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
        if (cooked == null) return ItemStack.EMPTY;
        PotBoilingRecipe recipe = HotPotRecipes.overcooking(world, new ItemStack(cooked));
        if (recipe != null) return recipe.overcookResult();
        Item legacy = OVERCOOK.get(cooked);
        return legacy == null ? ItemStack.EMPTY : new ItemStack(legacy);
    }

    /** 配置里没有「煮过头」这一项时的兜底总刻数（30 秒）。 */
    public static final int DEFAULT_OVERCOOK_TICKS = 600;

    /**
     * 这样熟食在锅里从下锅到煮过头的总刻数（含煮熟那一段）。
     * mryt233 改（2026-11-04）：豆腐走配置 `cooking.tofu_overcook_ticks`（默认 600 = 30 秒；r134b 从 food.tofu 挪来），每次现读、reload 立即生效；
     * mryt233 改（2026-11-11）：新增久煮规则通过配方提供，只有自家豆腐保留原有专属配置键。
     */
    public static int overcookTicks(Level world, Item happyCooked) {
        // mryt233 改（2026-11-11）：自家豆腐配置优先，第三方与未配项目读取配方。
        if (happyCooked == HotPotContent.HOT_TOFU && Config.has("cooking.tofu_overcook_ticks")) {
            return Config.tofuOvercookTicks(DEFAULT_OVERCOOK_TICKS);
        }
        PotBoilingRecipe recipe = HotPotRecipes.overcooking(world, new ItemStack(happyCooked));
        return recipe == null ? DEFAULT_OVERCOOK_TICKS : recipe.overcookTicks().orElse(DEFAULT_OVERCOOK_TICKS);
    }

    /**
     * 标准煮熟耗时（tick），无配方食材用兜底值。
     * mryt233 改（2026-11-04）：整块冷豆腐在锅里煮 200 刻（10 秒，配置 cooking.tofu_cook_ticks），不跟熔炉走。
     * mryt233 改（2026-11-05）：本模组登记过的生食材先看 cooking.items.&lt;物品注册路径&gt;，
     * 现读配置（reload 立即生效），不再受数据包熔炉配方限制；没有登记的原版 / 第三方食材照旧走
     * 烟熏炉、营火、熔炉配方，最后才用 cooking.fallback_ticks。
     */
    public static int cookTicks(Level world, ItemStack stack) {
        // mryt233 改（2026-11-11）：只给自家命名空间读取配置，缺配置才读配方。
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
            .map(e -> e.value().getCookingTime()).orElse(Config.fallbackTicks(DEFAULT_FALLBACK_TICKS)));
    }

    /** 时间优先级与惰性兜底；单测也使用实际查询的选择函数。mryt233 改（2026-11-11）。 */
    static int cookingTime(Integer configured, PotBoilingRecipe recipe, IntSupplier vanilla) {
        if (configured != null) return configured;
        return recipe != null && recipe.cookTicks().isPresent() ? recipe.cookTicks().get() : vanilla.getAsInt();
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
     *
     * <p>mryt233 改（2026-11-05，r136a）：变了——这里只负责换产物；锅把菜煮好后会在成品上写
     * {@code cooked_in_soup}（哪种汤底）和红汤的 {@code spicy_level}（辣度档位），
     * 见 {@link HotPotCookingRun#complete}。所以同一种菜在两种汤里煮出来不会并成一组，
     * 这是汤底风味加成要的：吃的时候按成品上的组件给效果。</p>
     */
    public static ItemStack finish(Level world, ItemStack raw, SoupBase soup) {
        return finishWithSoup(world, raw, soup == null ? null : soup.def());   // mryt233 改（2026-11-11，4B-2）：旧签名保留给联动兼容和探针
    }

    /** 同上，按登记表里的汤底定义。 */
    public static ItemStack finishWithSoup(Level world, ItemStack raw, SoupDef soup) {
        int portions = Math.max(1, raw.getCount());
        // mryt233 改（2026-11-11）：数据包 > Java 登记 > 烟熏 / 营火 / 食物熔炉 > 原物兜底。
        PotBoilingRecipe recipe = HotPotRecipes.boiling(world, raw);
        if (recipe != null) return recipe.result().copyWithCount(portions);
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
            .map(entry -> entry.value().getResultItem(world.registryAccess()).copy())
            .orElseGet(() -> raw.copyWithCount(portions));
        result.setCount(portions);
        return result;
    }

    /** 煮熟一份食材应给的经验（熔炉同款：优先取配方经验，没有则兜底）。 */
    public static float xpFor(Level world, ItemStack raw) {
        // mryt233 改（2026-11-11）：显式配方经验直接采用，缺省保留原来的经验下限与兜底。
        PotBoilingRecipe recipe = HotPotRecipes.boiling(world, raw);
        if (recipe != null && recipe.experience().isPresent()) return recipe.experience().get();
        return smeltingRecipe(world, raw).map(e -> Math.max(e.value().getExperience(), Config.xpPerItem(DEFAULT_XP_PER_ITEM)))
            .orElse(Config.fallbackXp(DEFAULT_FALLBACK_XP));
    }

    /*
     * mryt233 改（2026-11-10，r147）：登记表冻结入口（由 HotPotHooks.freezeRegistries 在服务端启动时统一调）。
     */

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
