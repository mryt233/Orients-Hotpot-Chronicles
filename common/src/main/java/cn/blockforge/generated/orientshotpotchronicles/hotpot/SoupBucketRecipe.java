package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.core.NonNullList;

/**
 * 汤底桶的合成（2026-10-26 用户：水桶 + 火锅底料在工作台直接合成番茄 / 三鲜汤底桶）：和原版无序合成一样，
 * 只是水桶不退空桶——原版用水桶合成会把空桶还回来，汤底桶倒进锅又还一个空桶，一个桶就变两个，能刷桶。
 * 水桶算是装进了汤底桶里，倒进锅时才还回来。配方 JSON 的 type 写 orients_hotpot_chronicles:soup_bucket，其余同无序合成。
 * 作者：mryt233 */
public class SoupBucketRecipe extends ShapelessRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    public SoupBucketRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
    }

    // mryt233 改（2026-11-10）：登记由内容模块传入的注册能力完成，保留旧入口。
    public static void register() {
        register(Framework.platform().registration());
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register( HotPot.id("soup_bucket"), SERIALIZER);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = super.getRemainingItems(input);
        for (int i = 0; i < out.size(); i++) {
            if (input.getItem(i).is(Items.WATER_BUCKET)) {
                out.set(i, ItemStack.EMPTY);
            }
        }
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static class Serializer implements RecipeSerializer<SoupBucketRecipe> {
        private static final MapCodec<SoupBucketRecipe> CODEC =
            ((MapCodec<ShapelessRecipe>) RecipeSerializer.SHAPELESS_RECIPE.codec()).xmap(SoupBucketRecipe::new, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, SoupBucketRecipe> PACKET =
            ShapelessRecipe.Serializer.STREAM_CODEC.map(SoupBucketRecipe::new, r -> r);

        @Override
        public MapCodec<SoupBucketRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SoupBucketRecipe> streamCodec() {
            return PACKET;
        }
    }
}
