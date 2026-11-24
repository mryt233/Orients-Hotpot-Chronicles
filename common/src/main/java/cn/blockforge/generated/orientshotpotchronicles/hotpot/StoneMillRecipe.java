// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 小石磨数据包配方（{@code data/<命名空间>/recipe/stone_mill/<名>.json}）。
 * 支持干磨（花生碎、小米辣等）与湿磨（黄豆加水磨出豆浆容器），研磨圈数受配置倍率调节。
 * 作者：mryt233
 */
public record StoneMillRecipe(
    Ingredient ingredient,
    int ingredientCount,
    boolean requiresWater,
    Optional<Item> container,
    ItemStack result,
    int turns,
    int color
) implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TURNS = 4;
    public static final int DEFAULT_COLOR = 0xFFEAE2CE;

    public static final RecipeType<StoneMillRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return HotPot.MOD_ID + ":stone_mill";
        }
    };
    public static final Serializer SERIALIZER = new Serializer();

    public StoneMillRecipe {
        if (ingredient.isEmpty() || result.isEmpty() || ingredientCount < 1 || ingredientCount > 64
            || turns < 1 || turns > 64) {
            throw new IllegalArgumentException("石磨配方原料、产物与圈数必须合法");
        }
        result = result.copy();
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    public int effectiveTurns() {
        return Math.max(1, Math.round(turns * (float) Config.millTurnsMultiplier()));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
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
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    public static final class Serializer implements RecipeSerializer<StoneMillRecipe> {
        private static final MapCodec<StoneMillRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(StoneMillRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("ingredient_count", 1).forGetter(StoneMillRecipe::ingredientCount),
            Codec.BOOL.optionalFieldOf("requires_water", false).forGetter(StoneMillRecipe::requiresWater),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("container").forGetter(StoneMillRecipe::container),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(StoneMillRecipe::result),
            Codec.intRange(1, 64).optionalFieldOf("turns", DEFAULT_TURNS).forGetter(StoneMillRecipe::turns),
            SoupBaseRecipe.COLOR_CODEC.optionalFieldOf("color", DEFAULT_COLOR).forGetter(StoneMillRecipe::color)
        ).apply(i, StoneMillRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Item>> OPT_ITEM_STREAM =
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM));

        private static final StreamCodec<RegistryFriendlyByteBuf, StoneMillRecipe> STREAM = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient);
                buf.writeVarInt(r.ingredientCount);
                buf.writeBoolean(r.requiresWater);
                OPT_ITEM_STREAM.encode(buf, r.container);
                ItemStack.STREAM_CODEC.encode(buf, r.result);
                buf.writeVarInt(r.turns);
                buf.writeInt(r.color);
            },
            buf -> new StoneMillRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readBoolean(),
                OPT_ITEM_STREAM.decode(buf),
                ItemStack.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readInt()
            )
        );

        @Override
        public MapCodec<StoneMillRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StoneMillRecipe> streamCodec() {
            return STREAM;
        }
    }
}
