package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** 汤底桶保留水桶，不额外生成空桶；其余行为沿用无序合成。作者：mryt233 */
public class SoupBucketRecipe extends ShapelessRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    public SoupBucketRecipe(ResourceLocation id, ShapelessRecipe base) {
        super(id, base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
    }

    // mryt233 改（2026-11-10）：登记由内容模块传入的注册能力完成，保留旧入口。
    public static void register() {
        register(Framework.platform().registration());
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register(HotPot.id("soup_bucket"), SERIALIZER);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
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

    public static final class Serializer implements RecipeSerializer<SoupBucketRecipe> {
        private final ShapelessRecipe.Serializer delegate = new ShapelessRecipe.Serializer();

        @Override
        public SoupBucketRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SoupBucketRecipe(id, delegate.fromJson(id, json));
        }

        @Override
        public SoupBucketRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SoupBucketRecipe(id, delegate.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SoupBucketRecipe recipe) {
            delegate.toNetwork(buffer, recipe);
        }
    }
}
