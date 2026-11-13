package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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
 * 布围裙换款配方：工作台里「任意一款布围裙 + 染料」换成指定的一款，保留全部组件。
 *
 * <p>mryt233 改（2026-11-13）：适配 26.1 {@link NormalCraftingRecipe} 与
 * {@link RegistrationApi} 登记入口。</p>
 * 作者：mryt233
 */
public class ApronRecolorRecipe extends NormalCraftingRecipe {
    private final Ingredient apron;
    private final Ingredient dye;
    private final Item result;
    private final ShapelessRecipe delegate;

    private static final MapCodec<ApronRecolorRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
        CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
        Ingredient.CODEC.fieldOf("apron").forGetter(r -> r.apron),
        Ingredient.CODEC.fieldOf("dye").forGetter(r -> r.dye),
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(r -> r.result)
    ).apply(i, ApronRecolorRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, ApronRecolorRecipe> PACKET =
        StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.apron,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.dye,
            ByteBufCodecs.VAR_INT.map(Item::byId, Item::getId), r -> r.result,
            ApronRecolorRecipe::new);

    public static final RecipeSerializer<ApronRecolorRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, PACKET);

    public ApronRecolorRecipe(Recipe.CommonInfo info, CraftingRecipe.CraftingBookInfo book,
                              Ingredient apron, Ingredient dye, Item result) {
        super(info, book);
        this.apron = apron;
        this.dye = dye;
        this.result = result;
        this.delegate = new ShapelessRecipe(info, book,
            new ItemStackTemplate(result), List.of(apron, dye));
    }

    public static void register() {
        register(Framework.platform().registration());
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register(HotPot.id("apron_recolor"), SERIALIZER);
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        boolean apronSeen = false;
        boolean dyeSeen = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (apron.test(stack)) {
                if (apronSeen) {
                    return false;
                }
                apronSeen = true;
            } else if (dye.test(stack)) {
                if (dyeSeen) {
                    return false;
                }
                dyeSeen = true;
            } else {
                return false;
            }
        }
        return apronSeen && dyeSeen;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (apron.test(stack)) {
                if (stack.is(result)) {
                    return stack.copy();
                }
                return stack.copy().transmuteCopy(result);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public List<RecipeDisplay> display() {
        return delegate.display();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<ApronRecolorRecipe> getSerializer() {
        return SERIALIZER;
    }
}
