package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * 汤底桶的合成：水桶 + 火锅底料直接合成番茄 / 三鲜汤底桶，水桶不退空桶。
 * 作者：mryt233
 */
public class SoupBucketRecipe extends NormalCraftingRecipe {
    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final ShapelessRecipe delegate;
    private static final MapCodec<SoupBucketRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
        CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
        Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients)
    ).apply(i, SoupBucketRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, SoupBucketRecipe> PACKET = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
        ItemStackTemplate.STREAM_CODEC, r -> r.result,
        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
        SoupBucketRecipe::new);
    public static final RecipeSerializer<SoupBucketRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, PACKET);

    public SoupBucketRecipe(Recipe.CommonInfo info, CraftingRecipe.CraftingBookInfo book,
                            ItemStackTemplate result, List<Ingredient> ingredients) {
        super(info, book);
        this.result = result;
        this.ingredients = ingredients;
        this.delegate = new ShapelessRecipe(info, book, result, ingredients);
    }

    @Override
    protected PlacementInfo createPlacementInfo() { return delegate.placementInfo(); }

    @Override
    public boolean matches(CraftingInput input, Level level) { return delegate.matches(input, level); }

    @Override
    public ItemStack assemble(CraftingInput input) { return delegate.assemble(input); }

    @Override
    public List<RecipeDisplay> display() { return delegate.display(); }

    public static void register() {
        register(Framework.platform().registration());
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register(HotPot.id("soup_bucket"), SERIALIZER);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = CraftingRecipe.defaultCraftingReminder(input);
        for (int i = 0; i < out.size(); i++) {
            if (input.getItem(i).is(Items.WATER_BUCKET)) {
                out.set(i, ItemStack.EMPTY);
            }
        }
        return out;
    }

    @Override
    public RecipeSerializer<SoupBucketRecipe> getSerializer() {
        return SERIALIZER;
    }
}
