package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.List;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.NonNullList;

/**
 * 汤底桶的合成（2026-10-26 用户：水桶 + 火锅底料在工作台直接合成番茄 / 三鲜汤底桶）：和原版无序合成一样，
 * 只是水桶不退空桶——原版用水桶合成会把空桶还回来，汤底桶倒进锅又还一个空桶，一个桶就变两个，能刷桶。
 * 水桶算是装进了汤底桶里，倒进锅时才还回来。配方 JSON 的 type 写 orients_hotpot_chronicles:soup_bucket，其余同无序合成。
 * 作者：mryt233 */
public class SoupBucketRecipe extends NormalCraftingRecipe {
    // mryt233 改：用原版无序配方做匹配和显示，独立保存编解码所需的公开数据。
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
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, HotPot.id("soup_bucket"), SERIALIZER);
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
