// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.Optional;

/** 捣制配方；中间阶段使用完整物品栈，可携带原有自定义模型数据。 */
public record PoundingRecipe(Ingredient ingredient, ItemStack result, int hits,
                             ItemStack stage1, ItemStack stage2) implements Recipe<SingleRecipeInput> {
    public static final RecipeType<PoundingRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":pounding"; }
    };
    public static final Serializer SERIALIZER = new Serializer();
    public PoundingRecipe {
        if (ingredient.isEmpty() || result.isEmpty() || hits < 1 || hits > 100000) {
            throw new IllegalArgumentException("捣制原料和产物不能为空，次数必须为 1～100000");
        }
        result = result.copy(); stage1 = stage1.copy(); stage2 = stage2.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public ItemStack stage1() { return stage1.copy(); }
    @Override public ItemStack stage2() { return stage2.copy(); }
    public ItemStack display(ItemStack input, int progress) {
        ItemStack stage = progress >= 2 ? stage2 : progress == 1 ? stage1 : ItemStack.EMPTY;
        return stage.isEmpty() ? input.copy() : stage.copy();
    }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }

    public static final class Serializer implements RecipeSerializer<PoundingRecipe> {
        private static Optional<ItemStack> stage(ItemStack value) {
            return value.isEmpty() ? Optional.empty() : Optional.of(value.copy());
        }
        private static final MapCodec<PoundingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PoundingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PoundingRecipe::result),
            Codec.intRange(1, 100000).optionalFieldOf("hits", Pounding.HITS).forGetter(PoundingRecipe::hits),
            ItemStack.STRICT_CODEC.optionalFieldOf("stage1").forGetter(r -> stage(r.stage1)),
            ItemStack.STRICT_CODEC.optionalFieldOf("stage2").forGetter(r -> stage(r.stage2))
        ).apply(i, (in, out, hits, s1, s2) -> new PoundingRecipe(in, out, hits,
            s1.orElse(ItemStack.EMPTY), s2.orElse(ItemStack.EMPTY))));
        private static final StreamCodec<RegistryFriendlyByteBuf, PoundingRecipe> STREAM = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient);
                ItemStack.STREAM_CODEC.encode(buf, r.result); buf.writeVarInt(r.hits);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.stage1);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.stage2);
            }, buf -> new PoundingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));
        @Override public MapCodec<PoundingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PoundingRecipe> streamCodec() { return STREAM; }
    }
}
