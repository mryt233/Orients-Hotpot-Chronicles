package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 熔炉式烹饪工具：食材在锅里加热，熟了就变成对应的熟食（mryt233 改：1.20.1 适配无 RecipeHolder / SingleRecipeInput / DataComponents.FOOD）。
 * 作者：mryt233 */
public final class HotPotCooking {
    public static final int FULL_DONE = 1000;
    public static final int DEFAULT_FALLBACK_TICKS = 200;
    public static final float DEFAULT_FALLBACK_XP = 0.15F;
    public static final float DEFAULT_XP_PER_ITEM = 0.7F;

    private static final FrozenRegistry<String, Integer> DEFAULT_ITEM_COOK_TICKS = new FrozenRegistry<>(
        "默认涮煮时间表", "默认涮煮时间是启动期数据，加食材时在 registerDefaultItemCookTicks() 里登记。");

    static {
        Config.rememberCooking(DEFAULT_FALLBACK_TICKS, DEFAULT_FALLBACK_XP, DEFAULT_XP_PER_ITEM);
        registerDefaultItemCookTicks();
        DEFAULT_ITEM_COOK_TICKS.view().forEach(Config::rememberItemCookTicks);
    }

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

    private static String itemPath(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && HotPot.MOD_ID.equals(key.getNamespace()) ? key.getPath() : null;
    }

    private static final AtomicLong COOK_REVISION = new AtomicLong();

    public static long cookRevision() {
        return COOK_REVISION.get();
    }

    public static void reviseCookTimes() {
        cachedOutputsManager = null;
        HotPotRecipes.clearCache();
        COOK_REVISION.incrementAndGet();
    }

    public static final TagKey<Item> NOT_POT_INGREDIENT =
        TagKey.create(Registries.ITEM, HotPot.id("not_pot_ingredient"));

    private HotPotCooking() {
    }

    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("minecraft", "meat"));

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
            || stack.getItem().isEdible()
            || stack.is(MEAT_TAG)
            || stack.is(ItemTags.FISHES)
            || foodRecipe(world, stack, RecipeType.SMOKING).isPresent()
            || foodRecipe(world, stack, RecipeType.CAMPFIRE_COOKING).isPresent()
            || HotPotHooks.extraCookable(stack);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<? extends AbstractCookingRecipe> smeltingRecipe(Level world, ItemStack stack) {
        if (world == null || stack.isEmpty()) {
            return Optional.empty();
        }
        for (RecipeType type : new RecipeType[]{RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING}) {
            Optional entry = foodRecipe(world, stack, type);
            if (entry.isPresent()) {
                return entry;
            }
        }
        if (stack.getItem().isEdible() || stack.is(MEAT_TAG) || stack.is(ItemTags.FISHES)) {
            return (Optional) foodRecipe(world, stack, RecipeType.SMELTING);
        }
        return Optional.empty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<? extends AbstractCookingRecipe> foodRecipe(Level world, ItemStack stack, RecipeType type) {
        if (world == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return world.getRecipeManager().getRecipeFor(type, new SimpleContainer(stack.copyWithCount(1)), world);
    }

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
        for (T entry : manager.getAllRecipesFor(type)) {
            out.add(entry.getResultItem(world.registryAccess()).getItem());
        }
    }

    public static boolean isCookedFood(Level world, ItemStack stack) {
        if (HotPotRecipes.boiling(world, stack) != null || POT_RESULTS.containsKey(stack.getItem())) {
            return false;
        }
        if (POT_RESULTS.containsValue(stack.getItem()) || HotPotRecipes.all(world, PotBoilingRecipe.TYPE).stream()
            .anyMatch(r -> r.result().is(stack.getItem()))) {
            return true;
        }
        if (world == null || stack.isEmpty() || smeltingRecipe(world, stack).isPresent()) {
            return false;
        }
        return cookedOutputs(world.getRecipeManager(), world).contains(stack.getItem());
    }

    private static final FrozenRegistry<Item, Item> POT_RESULTS = new FrozenRegistry<>(
        "锅专用产物表", "锅专用煮法要在启动期登记（子模组在自己的兼容模块里调 potResult），冻结后不再接受登记。");

    public static void potResult(Item raw, Item cooked) {
        POT_RESULTS.put(raw, cooked);
    }

    private static final FrozenRegistry<Item, Item> OVERCOOK = new FrozenRegistry<>(
        "煮过头产物表", "煮过头的玩法要在启动期登记（子模组在自己的兼容模块里调 overcook），冻结后不再接受登记。");

    public static void overcook(Item cooked, Item overcooked) {
        OVERCOOK.put(cooked, overcooked);
    }

    public static Item overcookProduct(Item cooked) {
        return overcookProduct(HotPotRecipes.currentLevel(), cooked);
    }

    public static Item overcookProduct(Level world, Item cooked) {
        ItemStack result = overcookResult(world, cooked);
        return result.isEmpty() ? null : result.getItem();
    }

    public static ItemStack overcookResult(Level world, Item cooked) {
        if (cooked == null) return ItemStack.EMPTY;
        PotBoilingRecipe recipe = HotPotRecipes.overcooking(world, new ItemStack(cooked));
        if (recipe != null) return recipe.overcookResult();
        Item legacy = OVERCOOK.get(cooked);
        return legacy == null ? ItemStack.EMPTY : new ItemStack(legacy);
    }

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
            .map(AbstractCookingRecipe::getCookingTime).orElse(Config.fallbackTicks(DEFAULT_FALLBACK_TICKS)));
    }

    static int cookingTime(Integer configured, PotBoilingRecipe recipe, IntSupplier vanilla) {
        if (configured != null) return configured;
        return recipe != null && recipe.cookTicks().isPresent() ? recipe.cookTicks().get() : vanilla.getAsInt();
    }

    public static int progressPerTick(int cookTicks, float power, float soupMul) {
        return Math.max(1, Math.round(FULL_DONE * Math.max(0.1F, power) * Math.max(0.1F, soupMul) / cookTicks));
    }

    public static ItemStack finish(Level world, ItemStack raw, SoupBase soup) {
        return finishWithSoup(world, raw, soup == null ? null : soup.def());
    }

    public static ItemStack finishWithSoup(Level world, ItemStack raw, SoupDef soup) {
        int portions = Math.max(1, raw.getCount());
        PotBoilingRecipe recipe = HotPotRecipes.boiling(world, raw);
        if (recipe != null) return recipe.result().copyWithCount(portions);
        Item potOnly = POT_RESULTS.get(raw.getItem());
        if (potOnly != null) {
            return new ItemStack(potOnly, portions);
        }
        ItemStack result = smeltingRecipe(world, raw)
            .map(entry -> entry.getResultItem(world.registryAccess()).copy())
            .orElseGet(() -> raw.copyWithCount(portions));
        result.setCount(portions);
        return result;
    }

    public static float xpFor(Level world, ItemStack raw) {
        PotBoilingRecipe recipe = HotPotRecipes.boiling(world, raw);
        if (recipe != null && recipe.experience().isPresent()) return recipe.experience().get();
        return smeltingRecipe(world, raw).map(e -> Math.max(e.getExperience(), Config.xpPerItem(DEFAULT_XP_PER_ITEM)))
            .orElse(Config.fallbackXp(DEFAULT_FALLBACK_XP));
    }

    public static void freezeRegistries() {
        POT_RESULTS.freeze();
        OVERCOOK.freeze();
        DEFAULT_ITEM_COOK_TICKS.freeze();
    }

    public static boolean registriesFrozen() {
        return POT_RESULTS.isFrozen() && OVERCOOK.isFrozen() && DEFAULT_ITEM_COOK_TICKS.isFrozen();
    }

    public static String registrySummary() {
        return "pot_results=" + POT_RESULTS.size() + "/" + POT_RESULTS.isFrozen()
            + " overcook=" + OVERCOOK.size() + "/" + OVERCOOK.isFrozen()
            + " cook_ticks=" + DEFAULT_ITEM_COOK_TICKS.size() + "/" + DEFAULT_ITEM_COOK_TICKS.isFrozen();
    }

    public static float ratio(int doneness) {
        return Math.max(0F, Math.min(1F, doneness / (float) FULL_DONE));
    }
}
