package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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
    public static void register() {
        cn.blockforge.orient.framework.compat.RecipeRegistrations.register(HotPot.id("soup_bucket"), SERIALIZER);
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        NonNullList<ItemStack> out = super.getRemainingItems(input);
        for (int i = 0; i < out.size(); i++) if (input.getItem(i).is(Items.WATER_BUCKET)) out.set(i, ItemStack.EMPTY);
        return out;
    }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    public static final class Serializer implements RecipeSerializer<SoupBucketRecipe> {
        private final ShapelessRecipe.Serializer delegate = new ShapelessRecipe.Serializer();
        @Override public SoupBucketRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SoupBucketRecipe(id, delegate.fromJson(id, json));
        }
        @Override public SoupBucketRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SoupBucketRecipe(id, delegate.fromNetwork(id, buffer));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, SoupBucketRecipe recipe) { delegate.toNetwork(buffer, recipe); }
    }
}
