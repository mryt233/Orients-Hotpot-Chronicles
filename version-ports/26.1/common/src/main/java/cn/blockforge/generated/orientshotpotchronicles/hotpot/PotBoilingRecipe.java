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

/**
 * 锅专用配方；未写的时间和经验继续使用原版配方与配置兜底。
 *
 * <p>mryt233 改（2026-11-13）：适配 26.1 {@link ItemStackTemplate} 与 {@link RecipeSerializer}，避免早于组件绑定时解码 {@link ItemStack} 失败。</p>
 */
public record PotBoilingRecipe(Ingredient ingredient, ItemStackTemplate resultTemplate, Optional<Integer> cookTicks,
                               Optional<Float> experience, Optional<ItemStackTemplate> overcookTemplate,
                               Optional<Integer> overcookTicks) implements Recipe<SingleRecipeInput> {
    public static final RecipeType<PotBoilingRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":pot_boiling"; }
    };

    private static final Codec<Ingredient> NON_EMPTY_INGREDIENT = Ingredient.CODEC.validate(
        in -> in.isEmpty() ? DataResult.error(() -> "锅配方原料不能为空") : DataResult.success(in));
    private static final Codec<ItemStackTemplate> SINGLE_TEMPLATE = ItemStackTemplate.CODEC.validate(
        t -> t.count() == 1 ? DataResult.success(t) : DataResult.error(() -> "锅配方产物每份必须为一个"));

    private static final MapCodec<PotBoilingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        NON_EMPTY_INGREDIENT.fieldOf("ingredient").forGetter(PotBoilingRecipe::ingredient),
        SINGLE_TEMPLATE.fieldOf("result").forGetter(PotBoilingRecipe::resultTemplate),
        Codec.intRange(1, 100000).optionalFieldOf("cook_ticks").forGetter(PotBoilingRecipe::cookTicks),
        Codec.floatRange(0, 100).optionalFieldOf("experience").forGetter(PotBoilingRecipe::experience),
        SINGLE_TEMPLATE.optionalFieldOf("overcook_result").forGetter(PotBoilingRecipe::overcookTemplate),
        Codec.intRange(1, 100000).optionalFieldOf("overcook_ticks").forGetter(PotBoilingRecipe::overcookTicks)
    ).apply(i, PotBoilingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, PotBoilingRecipe> STREAM = StreamCodec.of(
        (buf, r) -> {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient);
            ItemStackTemplate.STREAM_CODEC.encode(buf, r.resultTemplate);
            buf.writeVarInt(r.cookTicks.orElse(0));
            buf.writeFloat(r.experience.orElse(-1F));
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buf, r.overcookTemplate);
            buf.writeVarInt(r.overcookTicks.orElse(0));
        }, buf -> {
            Ingredient in = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStackTemplate out = ItemStackTemplate.STREAM_CODEC.decode(buf);
            int ticks = buf.readVarInt();
            float xp = buf.readFloat();
            Optional<ItemStackTemplate> over = ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buf);
            int total = buf.readVarInt();
            return new PotBoilingRecipe(in, out, ticks == 0 ? Optional.empty() : Optional.of(ticks),
                xp < 0 ? Optional.empty() : Optional.of(xp), over,
                total == 0 ? Optional.empty() : Optional.of(total));
        });

    public static final RecipeSerializer<PotBoilingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM);

    public PotBoilingRecipe(Ingredient ingredient, ItemStack result, Optional<Integer> cookTicks,
                            Optional<Float> experience, ItemStack overcookResult,
                            Optional<Integer> overcookTicks) {
        this(ingredient, nonEmptyTemplate(result), cookTicks, experience,
            overcookResult.isEmpty() ? Optional.empty() : Optional.of(nonEmptyTemplate(overcookResult)),
            overcookTicks);
    }

    private static ItemStackTemplate nonEmptyTemplate(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("锅配方原料不能为空，产物每份必须为一个");
        return ItemStackTemplate.fromNonEmptyStack(stack);
    }

    public PotBoilingRecipe {
        if (ingredient.isEmpty() || resultTemplate == null || resultTemplate.count() != 1
            || overcookTemplate.map(t -> t.count() != 1).orElse(false)) {
            throw new IllegalArgumentException("锅配方原料不能为空，产物每份必须为一个");
        }
    }

    public ItemStack result() { return resultTemplate.create(); }
    public ItemStack overcookResult() { return overcookTemplate.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input) { return result(); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public RecipeSerializer<PotBoilingRecipe> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<PotBoilingRecipe> getType() { return TYPE; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CAMPFIRE; }
}
