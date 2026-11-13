// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

/** 捣制配方；中间阶段使用完整物品栈，可携带原有自定义模型数据。 */
public record PoundingRecipe(Ingredient ingredient, ItemStackTemplate resultTemplate, int hits,
                             Optional<ItemStackTemplate> stage1Template,
                             Optional<ItemStackTemplate> stage2Template) implements Recipe<SingleRecipeInput> {
    public static final RecipeType<PoundingRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":pounding"; }
    };

    private static final Codec<Ingredient> NON_EMPTY_INGREDIENT = Ingredient.CODEC.validate(
        in -> in.isEmpty() ? DataResult.error(() -> "捣制原料不能为空") : DataResult.success(in));

    private static final MapCodec<PoundingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        NON_EMPTY_INGREDIENT.fieldOf("ingredient").forGetter(PoundingRecipe::ingredient),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(PoundingRecipe::resultTemplate),
        Codec.intRange(1, 100000).optionalFieldOf("hits", Pounding.HITS).forGetter(PoundingRecipe::hits),
        ItemStackTemplate.CODEC.optionalFieldOf("stage1").forGetter(PoundingRecipe::stage1Template),
        ItemStackTemplate.CODEC.optionalFieldOf("stage2").forGetter(PoundingRecipe::stage2Template)
    ).apply(i, PoundingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, PoundingRecipe> STREAM = StreamCodec.of(
        (buf, r) -> {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient);
            ItemStackTemplate.STREAM_CODEC.encode(buf, r.resultTemplate);
            buf.writeVarInt(r.hits);
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buf, r.stage1Template);
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buf, r.stage2Template);
        }, buf -> new PoundingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
            ItemStackTemplate.STREAM_CODEC.decode(buf), buf.readVarInt(),
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buf),
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buf)));

    public static final RecipeSerializer<PoundingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM);

    public PoundingRecipe(Ingredient ingredient, ItemStack result, int hits, ItemStack stage1, ItemStack stage2) {
        this(ingredient, nonEmptyTemplate(result), hits, optionalTemplate(stage1), optionalTemplate(stage2));
    }

    private static ItemStackTemplate nonEmptyTemplate(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("捣制原料和产物不能为空，次数必须为 1～100000");
        return ItemStackTemplate.fromNonEmptyStack(stack);
    }

    private static Optional<ItemStackTemplate> optionalTemplate(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Optional.of(ItemStackTemplate.fromNonEmptyStack(stack));
    }

    public PoundingRecipe {
        if (ingredient.isEmpty() || resultTemplate == null || hits < 1 || hits > 100000) {
            throw new IllegalArgumentException("捣制原料和产物不能为空，次数必须为 1～100000");
        }
    }

    public ItemStack result() { return resultTemplate.create(); }
    public ItemStack stage1() { return stage1Template.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY); }
    public ItemStack stage2() { return stage2Template.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY); }

    public ItemStack display(ItemStack input, int progress) {
        Optional<ItemStackTemplate> stage = progress >= 2 ? stage2Template : progress == 1 ? stage1Template : Optional.empty();
        return stage.map(ItemStackTemplate::create).orElseGet(input::copy);
    }

    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input) { return result(); }
    @Override public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<? extends Recipe<SingleRecipeInput>> getType() { return TYPE; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public String group() { return ""; }
    @Override public boolean showNotification() { return false; }
    @Override public boolean isSpecial() { return true; }
}
