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

/** 锅专用配方；未写的时间和经验继续使用原版配方与配置兜底。 */
public record PotBoilingRecipe(Ingredient ingredient, ItemStack result, Optional<Integer> cookTicks,
                               Optional<Float> experience, ItemStack overcookResult,
                               Optional<Integer> overcookTicks) implements Recipe<SingleRecipeInput> {
    public static final RecipeType<PotBoilingRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":pot_boiling"; }
    };
    public static final Serializer SERIALIZER = new Serializer();
    public PotBoilingRecipe {
        if (ingredient.isEmpty() || result.isEmpty() || result.getCount() != 1
            || (!overcookResult.isEmpty() && overcookResult.getCount() != 1)) {
            throw new IllegalArgumentException("锅配方原料不能为空，产物每份必须为一个");
        }
        result = result.copy(); overcookResult = overcookResult.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public ItemStack overcookResult() { return overcookResult.copy(); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }

    public static final class Serializer implements RecipeSerializer<PotBoilingRecipe> {
        private static final MapCodec<PotBoilingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PotBoilingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PotBoilingRecipe::result),
            Codec.intRange(1, 100000).optionalFieldOf("cook_ticks").forGetter(PotBoilingRecipe::cookTicks),
            Codec.floatRange(0, 100).optionalFieldOf("experience").forGetter(PotBoilingRecipe::experience),
            ItemStack.STRICT_CODEC.optionalFieldOf("overcook_result").forGetter(r ->
                r.overcookResult.isEmpty() ? Optional.empty() : Optional.of(r.overcookResult())),
            Codec.intRange(1, 100000).optionalFieldOf("overcook_ticks").forGetter(PotBoilingRecipe::overcookTicks)
        ).apply(i, (in, out, ticks, xp, over, total) -> new PotBoilingRecipe(in, out, ticks, xp,
            over.orElse(ItemStack.EMPTY), total)));
        private static final StreamCodec<RegistryFriendlyByteBuf, PotBoilingRecipe> STREAM = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient);
                ItemStack.STREAM_CODEC.encode(buf, r.result);
                buf.writeVarInt(r.cookTicks.orElse(0)); buf.writeFloat(r.experience.orElse(-1F));
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.overcookResult);
                buf.writeVarInt(r.overcookTicks.orElse(0));
            }, buf -> {
                Ingredient in = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                ItemStack out = ItemStack.STREAM_CODEC.decode(buf);
                int ticks = buf.readVarInt(); float xp = buf.readFloat();
                ItemStack over = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf); int total = buf.readVarInt();
                return new PotBoilingRecipe(in, out, ticks == 0 ? Optional.empty() : Optional.of(ticks),
                    xp < 0 ? Optional.empty() : Optional.of(xp), over,
                    total == 0 ? Optional.empty() : Optional.of(total));
            });
        @Override public MapCodec<PotBoilingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PotBoilingRecipe> streamCodec() { return STREAM; }
    }
}
