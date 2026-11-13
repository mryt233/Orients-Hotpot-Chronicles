package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 布围裙换款配方（r146b，2026-11-09 用户定）：工作台里「任意一款布围裙 + 染料」换成指定的一款。
 *
 * <p>三款布围裙的数值、耐久、附魔性、修复材料、防护效果完全一样，只有护甲层贴图不同，
 * 所以换款不是「重新做一件」，而是<b>同一件换个样子</b>——耐久、附魔、自定义名字都要原样带过去。
 * 复制整叠物品时原样搬物品 NBT（自定义名字、附魔、耐久都在里面），只把物品本体换成目标款
 * （和原版「在铁砧上改名」「染色皮革甲」那种「换皮不换件」的语义一致）。</p>
 *
 * <p>配方 JSON 写法（{@code data/<命名空间>/recipes/} 下，1.20.1 用的是 {@code recipes} 复数目录）：</p>
 * <pre>
 * {
 *   "type": "orients_hotpot_chronicles:apron_recolor",
 *   "category": "equipment",
 *   "apron": { "tag": "orients_hotpot_chronicles:cloth_aprons" },
 *   "dye": { "item": "minecraft:blue_dye" },
 *   "result": "orients_hotpot_chronicles:blue_print_cloth_apron"
 * }
 * </pre>
 *
 * <p>mryt233 改（1.20.1 适配）：1.20.1 的 {@code CraftingRecipe} 走旧接口
 * （{@code RecipeSerializer#fromJson / fromNetwork / toNetwork}、{@code ResourceLocation getId()}、
 * {@code assemble(CraftingContainer, RegistryAccess)}），也没有 {@code ItemStack#transmuteCopy}，
 * 所以换款时手动「拷贝 + 换物品本体 + 搬 NBT」。{@code category} 只是合成书里的归类，读写都按名字。
 *
 * <p>作者：mryt233
 */
public class ApronRecolorRecipe implements CraftingRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;
    private final CraftingBookCategory category;
    private final Ingredient apron;
    private final Ingredient dye;
    private final Item result;

    public ApronRecolorRecipe(ResourceLocation id, CraftingBookCategory category, Ingredient apron,
                              Ingredient dye, Item result) {
        this.id = id;
        this.category = category;
        this.apron = apron;
        this.dye = dye;
        this.result = result;
    }

    // mryt233 改（2026-11-10）：登记由内容模块传入的注册能力完成，保留旧入口。
    public static void register() {
        register(Framework.platform().registration());
    }

    public static void register(RegistrationApi registration) {
        registration.recipeSerializers().register(HotPot.id("apron_recolor"), SERIALIZER);
    }

    /** 形状对不对：整体正好两样东西，一件布围裙 + 一份染料（多余的格子必须空着）。 */
    @Override
    public boolean matches(CraftingContainer input, Level world) {
        boolean apronSeen = false;
        boolean dyeSeen = false;
        for (int i = 0; i < input.getContainerSize(); i++) {
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

    /**
     * 换款：拷一份围裙再改成目标款。原版 1.20.1 没有 {@code transmuteCopy}，所以这里手动做：
     * 新造一叠目标款（数量照旧），再把原物品的 NBT 整块搬过去——自定义名字、附魔、耐久全在 NBT 里，
     * 一样不落。同款换同款时直接原样返回。
     */
    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (apron.test(stack)) {
                if (stack.is(result)) {
                    return stack.copy();   // 已经是这一款：不折腾，原样交回去
                }
                ItemStack out = new ItemStack(result, stack.getCount());
                if (stack.hasTag()) {
                    out.setTag(stack.getTag().copy());
                }
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return new ItemStack(result);
    }

    /** 换款不额外返还东西（染料是被用掉的，围裙变成了新样子）。 */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        return NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(apron);
        list.add(dye);
        return list;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    /** 1.20.1 的序列化器：JSON / 网络三条旧接口。 */
    public static final class Serializer implements RecipeSerializer<ApronRecolorRecipe> {
        @Override
        public ApronRecolorRecipe fromJson(ResourceLocation id, JsonObject json) {
            // mryt233 改（1.20.1 适配）：1.20.1 的 EnumCodec#byName 直接返回枚举，缺省给「杂项」
            CraftingBookCategory category = CraftingBookCategory.CODEC
                .byName(GsonHelper.getAsString(json, "category", "misc"), CraftingBookCategory.MISC);
            Ingredient apron = Ingredient.fromJson(GsonHelper.getNonNull(json, "apron"));
            Ingredient dye = Ingredient.fromJson(GsonHelper.getNonNull(json, "dye"));
            Item result = GsonHelper.getAsItem(json, "result");
            return new ApronRecolorRecipe(id, category, apron, dye, result);
        }

        @Override
        public ApronRecolorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient apron = Ingredient.fromNetwork(buffer);
            Ingredient dye = Ingredient.fromNetwork(buffer);
            Item result = Item.byId(buffer.readVarInt());
            return new ApronRecolorRecipe(id, category, apron, dye, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ApronRecolorRecipe recipe) {
            buffer.writeEnum(recipe.category);
            recipe.apron.toNetwork(buffer);
            recipe.dye.toNetwork(buffer);
            buffer.writeVarInt(Item.getId(recipe.result));
        }
    }
}
