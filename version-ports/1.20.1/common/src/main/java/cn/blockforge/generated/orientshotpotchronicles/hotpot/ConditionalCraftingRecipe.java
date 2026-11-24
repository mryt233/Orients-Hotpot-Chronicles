// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 按模式或细分开关分流工作台配方：物品与配方 ID 保持永久注册，切换模式或热重载配置即刻生效（1.20.1 适配）。
 * 作者：mryt233
 */
public final class ConditionalCraftingRecipe implements CraftingRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    private final @Nullable ResourceLocation id;
    private final String flag;
    private final boolean expected;
    private final CraftingRecipe inner;

    public ConditionalCraftingRecipe(String flag, boolean expected, CraftingRecipe inner) {
        this(inner.getId(), flag, expected, inner);
    }

    public ConditionalCraftingRecipe(@Nullable ResourceLocation id, String flag, boolean expected, CraftingRecipe inner) {
        this.id = id;
        this.flag = flag;
        this.expected = expected;
        this.inner = inner;
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register(HotPot.id("conditional_crafting"), SERIALIZER);
    }

    public String flag() {
        return flag;
    }

    public boolean expected() {
        return expected;
    }

    public CraftingRecipe inner() {
        return inner;
    }

    public boolean isConditionMet() {
        return Config.modeCondition(flag, expected);
    }

    @Override
    public ResourceLocation getId() {
        return id != null ? id : inner.getId();
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return isConditionMet() && inner.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        return isConditionMet() ? inner.assemble(input, registries) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return inner.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return inner.getResultItem(registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        return inner.getRemainingItems(input);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public boolean isIncomplete() {
        return inner.isIncomplete();
    }

    @Override
    public boolean isSpecial() {
        return inner.isSpecial();
    }

    @Override
    public boolean showNotification() {
        return inner.showNotification();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
        return inner.category();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static final class Serializer implements RecipeSerializer<ConditionalCraftingRecipe> {
        @Override
        public ConditionalCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String flag = GsonHelper.getAsString(json, "flag");
            boolean expected = GsonHelper.getAsBoolean(json, "expected", true);
            JsonObject innerJson = GsonHelper.getAsJsonObject(json, "recipe");
            Recipe<?> parsed = RecipeManager.fromJson(id, innerJson);
            if (!(parsed instanceof CraftingRecipe crafting)) {
                throw new JsonSyntaxException("Inner recipe must be a CraftingRecipe: " + parsed.getType());
            }
            return new ConditionalCraftingRecipe(id, flag, expected, crafting);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ConditionalCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String flag = buf.readUtf();
            boolean expected = buf.readBoolean();
            ResourceLocation serializerId = buf.readResourceLocation();
            RecipeSerializer serializer = BuiltInRegistries.RECIPE_SERIALIZER.getOptional(serializerId)
                .orElseThrow(() -> new IllegalStateException("Unknown inner recipe serializer: " + serializerId));
            Recipe<?> parsed = serializer.fromNetwork(id, buf);
            if (!(parsed instanceof CraftingRecipe crafting)) {
                throw new IllegalStateException("Expected CraftingRecipe in conditional_crafting packet: " + parsed);
            }
            return new ConditionalCraftingRecipe(id, flag, expected, crafting);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void toNetwork(FriendlyByteBuf buf, ConditionalCraftingRecipe recipe) {
            buf.writeUtf(recipe.flag());
            buf.writeBoolean(recipe.expected());
            RecipeSerializer serializer = recipe.inner().getSerializer();
            ResourceLocation serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
            if (serializerId == null) {
                throw new IllegalStateException("Unregistered inner recipe serializer: " + serializer);
            }
            buf.writeResourceLocation(serializerId);
            serializer.toNetwork(buf, recipe.inner());
        }
    }
}
