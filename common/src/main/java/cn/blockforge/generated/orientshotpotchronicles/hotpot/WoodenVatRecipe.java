// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 开口大木桶数据包配方（{@code data/<命名空间>/recipe/wooden_vat/<名>.json}）。
 * 统一驱动两类工序：豆腐点卤压制（{@code coagulate}，需木压板）与干货清水泡发（{@code soak}）。
 * 作者：mryt233
 */
public record WoodenVatRecipe(
    String mode,
    String fluid,
    Optional<Ingredient> ingredient,
    int ingredientCount,
    boolean needsLid,
    int ticks,
    ItemStack result,
    int color
) implements Recipe<SingleRecipeInput> {
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

    public WoodenVatRecipe {
        if (mode == null || mode.isBlank() || fluid == null || fluid.isBlank()
            || result.isEmpty() || ingredientCount < 1 || ingredientCount > 64 || ticks < 1 || ticks > 72000) {
            throw new IllegalArgumentException("开口大木桶配方模式、液体、产物与耗时必须合法");
        }
        result = result.copy();
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
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.map(ing -> ing.test(input.item())).orElse(false);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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
        private static final MapCodec<WoodenVatRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("mode", MODE_COAGULATE).forGetter(WoodenVatRecipe::mode),
            Codec.STRING.fieldOf("fluid").forGetter(WoodenVatRecipe::fluid),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient").forGetter(WoodenVatRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("ingredient_count", 1).forGetter(WoodenVatRecipe::ingredientCount),
            Codec.BOOL.optionalFieldOf("needs_lid", false).forGetter(WoodenVatRecipe::needsLid),
            Codec.intRange(1, 72000).optionalFieldOf("ticks", DEFAULT_TICKS).forGetter(WoodenVatRecipe::ticks),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WoodenVatRecipe::result),
            SoupBaseRecipe.COLOR_CODEC.optionalFieldOf("color", DEFAULT_COLOR).forGetter(WoodenVatRecipe::color)
        ).apply(i, WoodenVatRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WoodenVatRecipe> STREAM = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.mode);
                buf.writeUtf(r.fluid);
                buf.writeBoolean(r.ingredient.isPresent());
                r.ingredient.ifPresent(ing -> Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing));
                buf.writeVarInt(r.ingredientCount);
                buf.writeBoolean(r.needsLid);
                buf.writeVarInt(r.ticks);
                ItemStack.STREAM_CODEC.encode(buf, r.result);
                buf.writeInt(r.color);
            },
            buf -> {
                String mode = buf.readUtf();
                String fluid = buf.readUtf();
                Optional<Ingredient> ing = buf.readBoolean()
                    ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buf))
                    : Optional.empty();
                return new WoodenVatRecipe(
                    mode,
                    fluid,
                    ing,
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    ItemStack.STREAM_CODEC.decode(buf),
                    buf.readInt()
                );
            }
        );

        @Override
        public MapCodec<WoodenVatRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WoodenVatRecipe> streamCodec() {
            return STREAM;
        }
    }
}
