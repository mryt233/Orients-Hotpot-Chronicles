// 1.20.1无序合成序列化接口，水桶仅在倒出汤底时返还。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import com.google.gson.JsonObject;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public class SoupBucketRecipe extends ShapelessRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    public SoupBucketRecipe(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.getCategory(), base.getOutput(null), base.getIngredients());
    }

    public static void register() {
        Registry.register(Registries.RECIPE_SERIALIZER, GeneratedMod.id("soup_bucket"), SERIALIZER);
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory input) {
        DefaultedList<ItemStack> out = super.getRemainder(input);
        for (int i = 0; i < out.size(); i++) {
            if (input.getStack(i).isOf(Items.WATER_BUCKET)) out.set(i, ItemStack.EMPTY);
        }
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return SERIALIZER; }

    public static class Serializer implements RecipeSerializer<SoupBucketRecipe> {
        @Override
        public SoupBucketRecipe read(Identifier id, JsonObject json) {
            return new SoupBucketRecipe(RecipeSerializer.SHAPELESS.read(id, json));
        }
        @Override
        public SoupBucketRecipe read(Identifier id, PacketByteBuf buf) {
            return new SoupBucketRecipe(RecipeSerializer.SHAPELESS.read(id, buf));
        }
        @Override
        public void write(PacketByteBuf buf, SoupBucketRecipe recipe) {
            RecipeSerializer.SHAPELESS.write(buf, recipe);
        }
    }
}
