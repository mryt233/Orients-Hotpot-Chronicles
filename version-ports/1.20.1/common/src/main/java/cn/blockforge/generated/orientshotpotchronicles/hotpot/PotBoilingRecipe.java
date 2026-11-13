// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 锅专用配方；未写的时间和经验继续使用原版配方与配置兜底（mryt233 改：1.20.1 适配）。 */
public final class PotBoilingRecipe implements Recipe<SimpleContainer> {
    public static final RecipeType<PotBoilingRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":pot_boiling"; }
    };
    public static final Serializer SERIALIZER = new Serializer();

    private final @Nullable ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final Optional<Integer> cookTicks;
    private final Optional<Float> experience;
    private final ItemStack overcookResult;
    private final Optional<Integer> overcookTicks;

    public PotBoilingRecipe(Ingredient ingredient, ItemStack result, Optional<Integer> cookTicks,
                            Optional<Float> experience, ItemStack overcookResult,
                            Optional<Integer> overcookTicks) {
        this(null, ingredient, result, cookTicks, experience, overcookResult, overcookTicks);
    }

    public PotBoilingRecipe(@Nullable ResourceLocation id, Ingredient ingredient, ItemStack result,
                            Optional<Integer> cookTicks, Optional<Float> experience,
                            ItemStack overcookResult, Optional<Integer> overcookTicks) {
        if (ingredient == null || ingredient.isEmpty() || result == null || result.isEmpty() || result.getCount() != 1
            || (overcookResult != null && !overcookResult.isEmpty() && overcookResult.getCount() != 1)) {
            throw new IllegalArgumentException("锅配方原料不能为空，产物每份必须为一个");
        }
        this.id = id;
        this.ingredient = ingredient;
        this.result = result.copy();
        this.cookTicks = Objects.requireNonNull(cookTicks);
        this.experience = Objects.requireNonNull(experience);
        this.overcookResult = overcookResult == null ? ItemStack.EMPTY : overcookResult.copy();
        this.overcookTicks = Objects.requireNonNull(overcookTicks);
    }

    public PotBoilingRecipe withId(ResourceLocation id) {
        return new PotBoilingRecipe(id, ingredient, result, cookTicks, experience, overcookResult, overcookTicks);
    }

    public Ingredient ingredient() { return ingredient; }
    public ItemStack result() { return result.copy(); }
    public Optional<Integer> cookTicks() { return cookTicks; }
    public Optional<Float> experience() { return experience; }
    public ItemStack overcookResult() { return overcookResult.copy(); }
    public Optional<Integer> overcookTicks() { return overcookTicks; }

    @Override public ResourceLocation getId() { return id != null ? id : HotPot.id("unbound"); }
    @Override public boolean matches(SimpleContainer input, Level level) {
        return input.getContainerSize() > 0 && ingredient.test(input.getItem(0));
    }
    @Override public ItemStack assemble(SimpleContainer input, RegistryAccess registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return result(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }

    public static final class Serializer implements RecipeSerializer<PotBoilingRecipe> {
        @Override
        public PotBoilingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            ItemStack result = HotPotRecipes.itemStackFromJson(json.get("result"), true);
            Optional<Integer> ticks = json.has("cook_ticks")
                ? Optional.of(HotPotRecipes.intRange(GsonHelper.getAsInt(json, "cook_ticks"), 1, 100000, "cook_ticks"))
                : Optional.empty();
            Optional<Float> xp = json.has("experience")
                ? Optional.of(HotPotRecipes.floatRange(GsonHelper.getAsFloat(json, "experience"), 0F, 100F, "experience"))
                : Optional.empty();
            ItemStack overcook = json.has("overcook_result")
                ? HotPotRecipes.itemStackFromJson(json.get("overcook_result"), true)
                : ItemStack.EMPTY;
            Optional<Integer> total = json.has("overcook_ticks")
                ? Optional.of(HotPotRecipes.intRange(GsonHelper.getAsInt(json, "overcook_ticks"), 1, 100000, "overcook_ticks"))
                : Optional.empty();
            return new PotBoilingRecipe(id, ingredient, result, ticks, xp, overcook, total);
        }

        @Override
        public PotBoilingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient in = Ingredient.fromNetwork(buf);
            ItemStack out = buf.readItem();
            int ticks = buf.readVarInt();
            float xp = buf.readFloat();
            ItemStack over = buf.readItem();
            int total = buf.readVarInt();
            return new PotBoilingRecipe(id, in, out, ticks == 0 ? Optional.empty() : Optional.of(ticks),
                xp < 0 ? Optional.empty() : Optional.of(xp), over,
                total == 0 ? Optional.empty() : Optional.of(total));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, PotBoilingRecipe r) {
            r.ingredient.toNetwork(buf);
            buf.writeItem(r.result);
            buf.writeVarInt(r.cookTicks.orElse(0));
            buf.writeFloat(r.experience.orElse(-1F));
            buf.writeItem(r.overcookResult);
            buf.writeVarInt(r.overcookTicks.orElse(0));
        }
    }
}
