// 作者：mryt233；mryt233 改：26.2 Recipe API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
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
 * 小石磨数据包配方（{@code data/<命名空间>/recipe/stone_mill/<名>.json}）。
 * 支持干磨（花生碎、小米辣等）与湿磨（黄豆加水磨出豆浆容器），研磨圈数受配置倍率调节。
 * 作者：mryt233
 */
public record StoneMillRecipe(
    Ingredient ingredient,
    int ingredientCount,
    boolean requiresWater,
    Optional<Item> container,
    ItemStackTemplate resultTemplate,
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

    // mryt233 改：配方解码阶段尚未绑定标签；使用原版配方原料编码，不提前展开标签成员。
    private static final MapCodec<StoneMillRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(StoneMillRecipe::ingredient),
        Codec.intRange(1, 64).optionalFieldOf("ingredient_count", 1).forGetter(StoneMillRecipe::ingredientCount),
        Codec.BOOL.optionalFieldOf("requires_water", false).forGetter(StoneMillRecipe::requiresWater),
        BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("container").forGetter(StoneMillRecipe::container),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(StoneMillRecipe::resultTemplate),
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
            ItemStackTemplate.STREAM_CODEC.encode(buf, r.resultTemplate);
            buf.writeVarInt(r.turns);
            buf.writeInt(r.color);
        },
        buf -> new StoneMillRecipe(
            Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
            buf.readVarInt(),
            buf.readBoolean(),
            OPT_ITEM_STREAM.decode(buf),
            ItemStackTemplate.STREAM_CODEC.decode(buf),
            buf.readVarInt(),
            buf.readInt()
        )
    );

    public static final RecipeSerializer<StoneMillRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM);

    public StoneMillRecipe(Ingredient ingredient, int ingredientCount, boolean requiresWater,
                           Optional<Item> container, ItemStack result, int turns, int color) {
        this(ingredient, ingredientCount, requiresWater, container,
            ItemStackTemplate.fromNonEmptyStack(result), turns, color);
    }

    public StoneMillRecipe {
        if (ingredient == null || resultTemplate == null || ingredientCount < 1 || ingredientCount > 64
            || turns < 1 || turns > 64) {
            throw new IllegalArgumentException("石磨配方原料、产物与圈数必须合法");
        }
    }

    public ItemStack result() {
        return resultTemplate.create();
    }

    public int effectiveTurns() {
        return Math.max(1, Math.round(turns * (float) Config.millTurnsMultiplier()));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
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
