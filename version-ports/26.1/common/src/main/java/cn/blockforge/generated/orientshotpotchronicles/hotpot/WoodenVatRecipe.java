// mryt233 改：26.1 同代移植同步。
// 作者：mryt233；mryt233 改：26.2 Recipe API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 开口大木桶数据包配方（{@code data/<命名空间>/recipe/wooden_vat/<名>.json}）。
 * 用豆浆与点卤材料压制冷豆腐。
 * 作者：mryt233
 */
public record WoodenVatRecipe(
    String mode,
    String fluid,
    Optional<Ingredient> ingredient,
    int ingredientCount,
    boolean needsLid,
    int ticks,
    ItemStackTemplate resultTemplate,
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

    // mryt233 改：配方解码阶段尚未绑定标签；使用原版配方原料编码，不提前展开标签成员。
    private static final MapCodec<WoodenVatRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.optionalFieldOf("mode", MODE_COAGULATE).forGetter(WoodenVatRecipe::mode),
        Codec.STRING.fieldOf("fluid").forGetter(WoodenVatRecipe::fluid),
        Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(WoodenVatRecipe::ingredient),
        Codec.intRange(1, 64).optionalFieldOf("ingredient_count", 1).forGetter(WoodenVatRecipe::ingredientCount),
        Codec.BOOL.optionalFieldOf("needs_lid", false).forGetter(WoodenVatRecipe::needsLid),
        Codec.intRange(1, 72000).optionalFieldOf("ticks", DEFAULT_TICKS).forGetter(WoodenVatRecipe::ticks),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(WoodenVatRecipe::resultTemplate),
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
            ItemStackTemplate.STREAM_CODEC.encode(buf, r.resultTemplate);
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
                ItemStackTemplate.STREAM_CODEC.decode(buf),
                buf.readInt()
            );
        }
    );

    public static final RecipeSerializer<WoodenVatRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM);

    public WoodenVatRecipe(String mode, String fluid, Optional<Ingredient> ingredient, int ingredientCount,
                           boolean needsLid, int ticks, ItemStack result, int color) {
        this(mode, fluid, ingredient, ingredientCount, needsLid, ticks,
            ItemStackTemplate.fromNonEmptyStack(result), color);
    }

    public WoodenVatRecipe {
        if (mode == null || mode.isBlank() || fluid == null || fluid.isBlank()
            || resultTemplate == null || ingredientCount < 1 || ingredientCount > 64 || ticks < 1 || ticks > 72000) {
            throw new IllegalArgumentException("开口大木桶配方模式、液体、产物与耗时必须合法");
        }
    }

    public ItemStack result() {
        return resultTemplate.create();
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
    public ItemStack assemble(SingleRecipeInput input) {
        return result();
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
