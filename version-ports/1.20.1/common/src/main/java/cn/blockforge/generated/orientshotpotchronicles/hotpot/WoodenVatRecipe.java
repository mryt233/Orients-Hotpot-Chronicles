// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
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

/**
 * 开口大木桶数据包配方（{@code data/<命名空间>/recipes/wooden_vat/<名>.json}；1.20.1 适配）。
 * 统一驱动两类工序：豆腐点卤压制（{@code coagulate}，需木压板）与干货清水泡发（{@code soak}）。
 * 作者：mryt233
 */
public record WoodenVatRecipe(
    @Nullable ResourceLocation id,
    String mode,
    String fluid,
    Optional<Ingredient> ingredient,
    int ingredientCount,
    boolean needsLid,
    int ticks,
    ItemStack result,
    int color
) implements Recipe<SimpleContainer> {
    public static final String MODE_COAGULATE = "coagulate";
    public static final String FLUID_SOYMILK = "soymilk";
    public static final int DEFAULT_TICKS = 80;
    public static final int DEFAULT_COLOR = 0xFFF2ECE0;

    public static final RecipeType<WoodenVatRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return HotPot.MOD_ID + ":wooden_vat";
        }
    };
    public static final Serializer SERIALIZER = new Serializer();

    public WoodenVatRecipe(String mode, String fluid, Optional<Ingredient> ingredient,
                           int ingredientCount, boolean needsLid, int ticks,
                           ItemStack result, int color) {
        this(null, mode, fluid, ingredient, ingredientCount, needsLid, ticks, result, color);
    }

    public WoodenVatRecipe {
        if (mode == null || mode.isBlank() || fluid == null || fluid.isBlank()
            || result.isEmpty() || ingredientCount < 1 || ingredientCount > 64 || ticks < 1 || ticks > 72000) {
            throw new IllegalArgumentException("开口大木桶配方模式、液体、产物与耗时必须合法");
        }
        result = result.copy();
    }

    public WoodenVatRecipe withId(ResourceLocation id) {
        return new WoodenVatRecipe(id, mode, fluid, ingredient, ingredientCount, needsLid, ticks, result, color);
    }

    @Override
    public ResourceLocation getId() {
        return id != null ? id : HotPot.id("unbound");
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    public int effectiveTicks() {
        return Math.max(1, Math.round(ticks * (float) Config.vatTimeMultiplier()));
    }

    public boolean matchesState(String vatFluid, ItemStack input) {
        if (!fluid.equals(vatFluid)) {
            return false;
        }
        if (ingredient.isPresent()) {
            return !input.isEmpty() && ingredient.get().test(input);
        }
        return true;
    }

    @Override
    public boolean matches(SimpleContainer input, Level level) {
        return input.getContainerSize() > 0
            && ingredient.map(ing -> ing.test(input.getItem(0))).orElse(false);
    }

    @Override
    public ItemStack assemble(SimpleContainer input, RegistryAccess registries) {
        return result();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredient.map(i -> NonNullList.of(Ingredient.EMPTY, i)).orElseGet(NonNullList::create);
    }

    public static final class Serializer implements RecipeSerializer<WoodenVatRecipe> {
        @Override
        public WoodenVatRecipe fromJson(ResourceLocation id, JsonObject json) {
            String mode = GsonHelper.getAsString(json, "mode", MODE_COAGULATE);
            String fluid = GsonHelper.getAsString(json, "fluid");
            Optional<Ingredient> ingredient = Optional.empty();
            if (json.has("ingredient")) {
                Ingredient parsed = Ingredient.fromJson(json.get("ingredient"));
                if (parsed.isEmpty()) {
                    throw new JsonSyntaxException("木桶配方 ingredient 不能为空");
                }
                ingredient = Optional.of(parsed);
            }
            int ingredientCount = json.has("ingredient_count")
                ? HotPotRecipes.intRange(GsonHelper.getAsInt(json, "ingredient_count"), 1, 64, "ingredient_count")
                : 1;
            boolean needsLid = GsonHelper.getAsBoolean(json, "needs_lid", false);
            int ticks = json.has("ticks")
                ? HotPotRecipes.intRange(GsonHelper.getAsInt(json, "ticks"), 1, 72000, "ticks")
                : DEFAULT_TICKS;
            ItemStack result = HotPotRecipes.itemStackFromJson(json.get("result"), false);
            int color = json.has("color")
                ? SoupBaseRecipe.COLOR_CODEC.parse(JsonOps.INSTANCE, json.get("color"))
                    .getOrThrow(false, msg -> { throw new JsonSyntaxException(msg); })
                : DEFAULT_COLOR;
            return new WoodenVatRecipe(id, mode, fluid, ingredient, ingredientCount, needsLid, ticks, result, color);
        }

        @Override
        public WoodenVatRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String mode = buf.readUtf();
            String fluid = buf.readUtf();
            Optional<Ingredient> ing = buf.readBoolean()
                ? Optional.of(Ingredient.fromNetwork(buf))
                : Optional.empty();
            return new WoodenVatRecipe(
                id,
                mode,
                fluid,
                ing,
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readItem(),
                buf.readInt()
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, WoodenVatRecipe r) {
            buf.writeUtf(r.mode);
            buf.writeUtf(r.fluid);
            buf.writeBoolean(r.ingredient.isPresent());
            r.ingredient.ifPresent(ing -> ing.toNetwork(buf));
            buf.writeVarInt(r.ingredientCount);
            buf.writeBoolean(r.needsLid);
            buf.writeVarInt(r.ticks);
            buf.writeItem(r.result);
            buf.writeInt(r.color);
        }
    }
}
