// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 小石磨数据包配方（{@code data/<命名空间>/recipes/stone_mill/<名>.json}；1.20.1 适配）。
 * 支持干磨（花生碎、小米辣等）与湿磨（黄豆加水磨出豆浆容器），研磨圈数受配置倍率调节。
 * 作者：mryt233
 */
public record StoneMillRecipe(
    @Nullable ResourceLocation id,
    Ingredient ingredient,
    int ingredientCount,
    boolean requiresWater,
    Optional<Item> container,
    ItemStack result,
    int turns,
    int color
) implements Recipe<SimpleContainer> {
    public static final int DEFAULT_TURNS = 4;
    public static final int DEFAULT_COLOR = 0xFFEAE2CE;

    public static final RecipeType<StoneMillRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return HotPot.MOD_ID + ":stone_mill";
        }
    };
    public static final Serializer SERIALIZER = new Serializer();

    public StoneMillRecipe(Ingredient ingredient, int ingredientCount, boolean requiresWater,
                           Optional<Item> container, ItemStack result, int turns, int color) {
        this(null, ingredient, ingredientCount, requiresWater, container, result, turns, color);
    }

    public StoneMillRecipe {
        if (ingredient.isEmpty() || result.isEmpty() || ingredientCount < 1 || ingredientCount > 64
            || turns < 1 || turns > 64) {
            throw new IllegalArgumentException("石磨配方原料、产物与圈数必须合法");
        }
        result = result.copy();
    }

    public StoneMillRecipe withId(ResourceLocation id) {
        return new StoneMillRecipe(id, ingredient, ingredientCount, requiresWater, container, result, turns, color);
    }

    @Override
    public ResourceLocation getId() {
        return id != null ? id : HotPot.id("unbound");
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    public int effectiveTurns() {
        return Math.max(1, Math.round(turns * (float) Config.millTurnsMultiplier()));
    }

    @Override
    public boolean matches(SimpleContainer input, Level level) {
        return input.getContainerSize() > 0 && ingredient.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer input, RegistryAccess registries) {
        return result();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    public static final class Serializer implements RecipeSerializer<StoneMillRecipe> {
        @Override
        public StoneMillRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            if (ingredient.isEmpty()) {
                throw new JsonSyntaxException("石磨配方 ingredient 不能为空");
            }
            int ingredientCount = json.has("ingredient_count")
                ? HotPotRecipes.intRange(GsonHelper.getAsInt(json, "ingredient_count"), 1, 64, "ingredient_count")
                : 1;
            boolean requiresWater = GsonHelper.getAsBoolean(json, "requires_water", false);
            Optional<Item> container = Optional.empty();
            if (json.has("container")) {
                String rawId = GsonHelper.getAsString(json, "container");
                ResourceLocation key = ResourceLocation.tryParse(rawId);
                if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
                    throw new JsonSyntaxException("未知石磨容器物品：" + rawId);
                }
                Item item = BuiltInRegistries.ITEM.get(key);
                if (item == Items.AIR) {
                    throw new JsonSyntaxException("石磨容器物品不能为空气：" + rawId);
                }
                container = Optional.of(item);
            }
            ItemStack result = HotPotRecipes.itemStackFromJson(json.get("result"), false);
            int turns = json.has("turns")
                ? HotPotRecipes.intRange(GsonHelper.getAsInt(json, "turns"), 1, 64, "turns")
                : DEFAULT_TURNS;
            int color = json.has("color")
                ? SoupBaseRecipe.COLOR_CODEC.parse(JsonOps.INSTANCE, json.get("color"))
                    .getOrThrow(false, msg -> { throw new JsonSyntaxException(msg); })
                : DEFAULT_COLOR;
            return new StoneMillRecipe(id, ingredient, ingredientCount, requiresWater, container, result, turns, color);
        }

        @Override
        public StoneMillRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            int ingredientCount = buf.readVarInt();
            boolean requiresWater = buf.readBoolean();
            Optional<Item> container = SoupBaseRecipe.readOpt(buf, SoupBaseRecipe::readItem);
            ItemStack result = buf.readItem();
            int turns = buf.readVarInt();
            int color = buf.readInt();
            return new StoneMillRecipe(id, ingredient, ingredientCount, requiresWater, container, result, turns, color);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, StoneMillRecipe r) {
            r.ingredient.toNetwork(buf);
            buf.writeVarInt(r.ingredientCount);
            buf.writeBoolean(r.requiresWater);
            SoupBaseRecipe.writeOpt(buf, r.container, SoupBaseRecipe::writeItem);
            buf.writeItem(r.result);
            buf.writeVarInt(r.turns);
            buf.writeInt(r.color);
        }
    }
}
