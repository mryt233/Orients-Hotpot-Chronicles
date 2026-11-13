// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 捣制配方；中间阶段使用完整物品栈，可携带原有自定义模型数据（mryt233 改：1.20.1 适配）。 */
public final class PoundingRecipe implements Recipe<SimpleContainer> {
    public static final RecipeType<PoundingRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":pounding"; }
    };
    public static final Serializer SERIALIZER = new Serializer();

    private final @Nullable ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int hits;
    private final ItemStack stage1;
    private final ItemStack stage2;

    public PoundingRecipe(Ingredient ingredient, ItemStack result, int hits,
                          ItemStack stage1, ItemStack stage2) {
        this(null, ingredient, result, hits, stage1, stage2);
    }

    public PoundingRecipe(@Nullable ResourceLocation id, Ingredient ingredient, ItemStack result, int hits,
                          ItemStack stage1, ItemStack stage2) {
        if (ingredient == null || ingredient.isEmpty() || result == null || result.isEmpty() || hits < 1 || hits > 100000) {
            throw new IllegalArgumentException("捣制原料和产物不能为空，次数必须为 1～100000");
        }
        this.id = id;
        this.ingredient = ingredient;
        this.result = result.copy();
        this.hits = hits;
        this.stage1 = stage1 == null ? ItemStack.EMPTY : stage1.copy();
        this.stage2 = stage2 == null ? ItemStack.EMPTY : stage2.copy();
    }

    public PoundingRecipe withId(ResourceLocation id) {
        return new PoundingRecipe(id, ingredient, result, hits, stage1, stage2);
    }

    public Ingredient ingredient() { return ingredient; }
    public ItemStack result() { return result.copy(); }
    public int hits() { return hits; }
    public ItemStack stage1() { return stage1.copy(); }
    public ItemStack stage2() { return stage2.copy(); }

    public ItemStack display(ItemStack input, int progress) {
        ItemStack stage = progress >= 2 ? stage2 : progress == 1 ? stage1 : ItemStack.EMPTY;
        return stage.isEmpty() ? input.copy() : stage.copy();
    }

    @Override public ResourceLocation getId() { return id != null ? id : HotPot.id("unbound"); }
    @Override public boolean matches(SimpleContainer input, Level level) {
        return input.getContainerSize() > 0 && ingredient.test(input.getItem(0));
    }
    @Override public ItemStack assemble(SimpleContainer input, RegistryAccess registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return result(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }

    public static final class Serializer implements RecipeSerializer<PoundingRecipe> {
        @Override
        public PoundingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            ItemStack result = HotPotRecipes.itemStackFromJson(json.get("result"), false);
            int hits = json.has("hits")
                ? HotPotRecipes.intRange(GsonHelper.getAsInt(json, "hits"), 1, 100000, "hits")
                : Pounding.HITS;
            ItemStack s1 = json.has("stage1") ? HotPotRecipes.itemStackFromJson(json.get("stage1"), false) : ItemStack.EMPTY;
            ItemStack s2 = json.has("stage2") ? HotPotRecipes.itemStackFromJson(json.get("stage2"), false) : ItemStack.EMPTY;
            return new PoundingRecipe(id, ingredient, result, hits, s1, s2);
        }

        @Override
        public PoundingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new PoundingRecipe(id, Ingredient.fromNetwork(buf), buf.readItem(), buf.readVarInt(),
                buf.readItem(), buf.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, PoundingRecipe r) {
            r.ingredient.toNetwork(buf);
            buf.writeItem(r.result);
            buf.writeVarInt(r.hits);
            buf.writeItem(r.stage1);
            buf.writeItem(r.stage2);
        }
    }
}
