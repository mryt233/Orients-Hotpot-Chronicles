//  移植：0.1.4-alpha 冻结底本，保留原作者署名。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.collection.DefaultedList;

/**
 * 汤底桶的合成（2026-10-26 用户：水桶 + 火锅底料在工作台直接合成番茄 / 三鲜汤底桶）：和原版无序合成一样，
 * 只是水桶不退空桶——原版用水桶合成会把空桶还回来，汤底桶倒进锅又还一个空桶，一个桶就变两个，能刷桶。
 * 水桶算是装进了汤底桶里，倒进锅时才还回来。配方 JSON 的 type 写 orients_hotpot_chronicles:soup_bucket，其余同无序合成。
 *
 */
public class SoupBucketRecipe extends ShapelessRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    public SoupBucketRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.getCategory(), base.getResult(null), base.getIngredients());
    }

    public static void register() {
        NeoRegistrations.queue(Registries.RECIPE_SERIALIZER, GeneratedMod.id("soup_bucket"), SERIALIZER);
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
        DefaultedList<ItemStack> out = super.getRemainder(input);
        for (int i = 0; i < out.size(); i++) {
            if (input.getStackInSlot(i).isOf(Items.WATER_BUCKET)) {
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
            ((MapCodec<ShapelessRecipe>) RecipeSerializer.SHAPELESS.codec()).xmap(SoupBucketRecipe::new, r -> r);
        private static final PacketCodec<RegistryByteBuf, SoupBucketRecipe> PACKET =
            ShapelessRecipe.Serializer.PACKET_CODEC.xmap(SoupBucketRecipe::new, r -> r);

        @Override
        public MapCodec<SoupBucketRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, SoupBucketRecipe> packetCodec() {
            return PACKET;
        }
    }
}
