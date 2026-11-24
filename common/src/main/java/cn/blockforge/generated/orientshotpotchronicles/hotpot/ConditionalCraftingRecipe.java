// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 按模式或细分开关分流工作台配方：物品与配方 ID 保持永久注册，切换模式或热重载配置即刻生效。
 * 作者：mryt233
 */
public final class ConditionalCraftingRecipe implements CraftingRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    private final String flag;
    private final boolean expected;
    private final CraftingRecipe inner;

    public ConditionalCraftingRecipe(String flag, boolean expected, CraftingRecipe inner) {
        this.flag = flag;
        this.expected = expected;
        this.inner = inner;
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register(HotPot.id("conditional_crafting"), SERIALIZER);
    }

    public String flag() {
        return flag;
    }

    public boolean expected() {
        return expected;
    }

    public CraftingRecipe inner() {
        return inner;
    }

    public boolean isConditionMet() {
        return Config.modeCondition(flag, expected);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return isConditionMet() && inner.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return isConditionMet() ? inner.assemble(input, registries) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return inner.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return inner.getResultItem(registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return inner.getRemainingItems(input);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public boolean isIncomplete() {
        return inner.isIncomplete();
    }

    @Override
    public boolean isSpecial() {
        return inner.isSpecial();
    }

    @Override
    public boolean showNotification() {
        return inner.showNotification();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public CraftingBookCategory category() {
        return inner.category();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static final class Serializer implements RecipeSerializer<ConditionalCraftingRecipe> {
        private static final Codec<CraftingRecipe> INNER_CODEC = Recipe.CODEC.comapFlatMap(
            recipe -> recipe instanceof CraftingRecipe crafting
                ? DataResult.success(crafting)
                : DataResult.error(() -> "Inner recipe must be a CraftingRecipe: " + recipe.getType()),
            crafting -> crafting
        );

        private static final MapCodec<ConditionalCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("flag").forGetter(ConditionalCraftingRecipe::flag),
            Codec.BOOL.optionalFieldOf("expected", true).forGetter(ConditionalCraftingRecipe::expected),
            INNER_CODEC.fieldOf("recipe").forGetter(ConditionalCraftingRecipe::inner)
        ).apply(i, ConditionalCraftingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CraftingRecipe> INNER_STREAM =
            Recipe.STREAM_CODEC.map(
                recipe -> {
                    if (recipe instanceof CraftingRecipe crafting) {
                        return crafting;
                    }
                    throw new IllegalStateException("Expected CraftingRecipe in conditional_crafting packet: " + recipe);
                },
                crafting -> crafting
            );

        private static final StreamCodec<RegistryFriendlyByteBuf, ConditionalCraftingRecipe> STREAM =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ConditionalCraftingRecipe::flag,
                ByteBufCodecs.BOOL, ConditionalCraftingRecipe::expected,
                INNER_STREAM, ConditionalCraftingRecipe::inner,
                ConditionalCraftingRecipe::new
            );

        @Override
        public MapCodec<ConditionalCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConditionalCraftingRecipe> streamCodec() {
            return STREAM;
        }
    }
}
