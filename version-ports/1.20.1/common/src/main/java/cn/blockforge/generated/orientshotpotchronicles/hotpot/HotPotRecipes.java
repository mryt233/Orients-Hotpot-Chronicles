// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.compat.CustomModelData;
import cn.blockforge.orient.framework.compat.DataComponents;
import cn.blockforge.orient.framework.compat.ItemData;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** 配方查询统一使用调用方世界；旧无世界参数入口保留当前环境的回退（mryt233 改：1.20.1 适配）。 */
public final class HotPotRecipes {
    private static volatile MinecraftServer server;
    private static volatile Supplier<Level> clientLevel = () -> null;

    private record CachedType<T>(List<T> raw, List<T> sorted) {
        boolean matches(List<T> current) {
            if (current.size() != raw.size()) return false;
            for (int i = 0; i < raw.size(); i++) {
                if (current.get(i) != raw.get(i)) return false;
            }
            return true;
        }
    }

    private static final Map<RecipeManager, Map<RecipeType<?>, CachedType<?>>> SNAPSHOTS = new WeakHashMap<>();

    private HotPotRecipes() { }

    public static void register(RegistrationApi registration) {
        registration.recipeTypes().register(HotPot.id("pot_boiling"), PotBoilingRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("pot_boiling"), PotBoilingRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("pounding"), PoundingRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("pounding"), PoundingRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("soup_base"), SoupBaseRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("soup_base"), SoupBaseRecipe.SERIALIZER);
        registration.recipeTypes().register(HotPot.id("seasoning"), SeasoningRecipe.TYPE);
        registration.recipeSerializers().register(HotPot.id("seasoning"), SeasoningRecipe.SERIALIZER);
    }

    static void clearCache() {
        synchronized (SNAPSHOTS) { SNAPSHOTS.clear(); }
        Soups.clearRuntimeCache();
        Seasonings.clearRuntimeCache();
    }

    public static void setServer(MinecraftServer value) {
        server = value;
        if (value == null) {
            clearCache();
        }
    }

    public static void clientLevel(Supplier<Level> supplier) { clientLevel = supplier; }

    public static Level currentLevel() {
        MinecraftServer active = server;
        if (active != null && active.isSameThread()) return active.overworld();
        Level client = clientLevel.get();
        return client != null ? client : active == null ? null : active.overworld();
    }

    /** 同原料多条配方按完整 ID 排序，避免加载器或重载顺序改变选择结果。 */
    @SuppressWarnings("unchecked")
    public static <C extends Container, T extends Recipe<C>> List<T> all(Level level, RecipeType<T> type) {
        if (level == null) return List.of();
        RecipeManager manager = level.getRecipeManager();
        List<T> current = manager.getAllRecipesFor(type);
        synchronized (SNAPSHOTS) {
            Map<RecipeType<?>, CachedType<?>> byType = SNAPSHOTS.computeIfAbsent(manager, ignored -> new HashMap<>());
            CachedType<T> cached = (CachedType<T>) byType.get(type);
            if (cached != null && cached.matches(current)) {
                return cached.sorted();
            }
            List<T> rawSnapshot = List.copyOf(current);
            List<T> sorted = rawSnapshot.stream()
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .toList();
            byType.put(type, new CachedType<>(rawSnapshot, sorted));
            return sorted;
        }
    }

    public static PotBoilingRecipe boiling(Level level, ItemStack input) {
        if (input.isEmpty()) return null;
        return all(level, PotBoilingRecipe.TYPE).stream()
            .filter(recipe -> recipe.ingredient().test(input)).findFirst().orElse(null);
    }

    public static PotBoilingRecipe overcooking(Level level, ItemStack cooked) {
        if (cooked.isEmpty()) return null;
        return all(level, PotBoilingRecipe.TYPE).stream()
            .filter(recipe -> !recipe.overcookResult().isEmpty()
                && recipe.result().is(cooked.getItem()))
            .findFirst().orElse(null);
    }

    public static PoundingRecipe pounding(Level level, ItemStack input) {
        if (input.isEmpty()) return null;
        return all(level, PoundingRecipe.TYPE).stream()
            .filter(recipe -> recipe.ingredient().test(input)).findFirst().orElse(null);
    }

    /** 解析配方 JSON 里的物品栈：同时兼容 1.20.1 {"item","count","nbt"} 与 1.21 {"id","count","components"}。 */
    static ItemStack itemStackFromJson(JsonElement element, boolean strictSingle) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonSyntaxException("配方物品必须为 JSON 对象");
        }
        JsonObject obj = element.getAsJsonObject();
        String itemId = obj.has("item") ? GsonHelper.getAsString(obj, "item")
            : obj.has("id") ? GsonHelper.getAsString(obj, "id") : null;
        if (itemId == null) {
            throw new JsonSyntaxException("配方物品缺少 item / id 字段");
        }
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            throw new JsonSyntaxException("未知物品：" + itemId);
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == Items.AIR) {
            throw new JsonSyntaxException("配方物品不能为空气：" + itemId);
        }
        int count = obj.has("count") ? GsonHelper.getAsInt(obj, "count")
            : obj.has("Count") ? GsonHelper.getAsInt(obj, "Count") : 1;
        if (count < 1 || (strictSingle && count != 1)) {
            throw new JsonSyntaxException("非法物品数量：" + count);
        }
        ItemStack stack = new ItemStack(item, count);
        JsonElement tagEl = obj.has("nbt") ? obj.get("nbt") : obj.get("tag");
        if (tagEl != null && tagEl.isJsonObject()) {
            CompoundTag nbt = CompoundTag.CODEC.parse(JsonOps.INSTANCE, tagEl)
                .getOrThrow(false, msg -> { throw new JsonSyntaxException(msg); });
            stack.setTag(nbt);
        }
        if (obj.has("components") && obj.get("components").isJsonObject()) {
            JsonObject components = obj.getAsJsonObject("components");
            if (components.has("minecraft:custom_model_data")) {
                int cmd = GsonHelper.getAsInt(components, "minecraft:custom_model_data");
                ItemData.set(stack, DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cmd));
            }
        }
        return stack;
    }

    static int intRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new JsonSyntaxException(field + " 超出范围 [" + min + ", " + max + "]：" + value);
        }
        return value;
    }

    static float floatRange(float value, float min, float max, String field) {
        if (value < min || value > max || Float.isNaN(value)) {
            throw new JsonSyntaxException(field + " 超出范围 [" + min + ", " + max + "]：" + value);
        }
        return value;
    }
}
