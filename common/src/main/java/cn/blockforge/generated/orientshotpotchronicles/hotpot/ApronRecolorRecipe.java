package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;


/**
 * 布围裙换款配方（r146b，2026-11-09 用户定）：工作台里「任意一款布围裙 + 染料」换成指定的一款。
 *
 * <p>三款布围裙的数值、耐久、附魔性、修复材料、防护效果完全一样，只有护甲层贴图不同，
 * 所以换款不是「重新做一件」，而是**同一件换个样子**——耐久、附魔、自定义名字都要原样带过去。
 * 用 {@link ItemStack#transmuteCopy} 复制整叠物品：它保留自定义名字、附魔、耐久等全部组件，
 * 只把物品本体换成目标款（和原版「在铁砧上改名」「染色皮革甲」那种「换皮不换件」的语义一致）。</p>
 *
 * <p>配方 JSON 写法（{@code data/<命名空间>/recipe/} 下）：</p>
 * <pre>
 * {
 *   "type": "orients_hotpot_chronicles:apron_recolor",
 *   "category": "equipment",
 *   "apron": { "tag": "orients_hotpot_chronicles:cloth_aprons" },
 *   "dye": { "item": "minecraft:blue_dye" },
 *   "result": "orients_hotpot_chronicles:blue_print_cloth_apron"
 * }
 * </pre>
 * 作者：mryt233
 */
public class ApronRecolorRecipe implements CraftingRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    private final CraftingBookCategory category;
    private final Ingredient apron;
    private final Ingredient dye;
    private final Item result;

    public ApronRecolorRecipe(CraftingBookCategory category, Ingredient apron, Ingredient dye, Item result) {
        this.category = category;
        this.apron = apron;
        this.dye = dye;
        this.result = result;
    }

    // mryt233 改（2026-11-10）：登记由内容模块传入的注册能力完成，保留旧入口。
    public static void register() {
        register(cn.blockforge.orient.framework.loader.Framework.platform().registration());
    }

    public static void register(cn.blockforge.orient.framework.loader.RegistrationApi registration) {
        registration.recipeSerializers().register( HotPot.id("apron_recolor"), SERIALIZER);
    }

    /** 形状对不对：整体正好两样东西，一件布围裙 + 一份染料（多余的格子必须空着）。 */
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

    /**
     * 换款：拷一份围裙再改成目标款。{@link ItemStack#transmuteCopy} 会保留全部组件
     * （自定义名字、附魔、耐久……），只有物品本体换掉；同款换同款时直接原样返回。
     */
    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (apron.test(stack)) {
                if (stack.is(result)) {
                    return stack.copy();   // 已经是这一款：不折腾，原样交回去
                }
                return stack.copy().transmuteCopy(result);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(result);
    }

    /** 换款不额外返还东西（染料是被用掉的，围裙变成了新样子）。 */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
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
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public static class Serializer implements RecipeSerializer<ApronRecolorRecipe> {
        private static final MapCodec<ApronRecolorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.EQUIPMENT)
                .forGetter(ApronRecolorRecipe::category),
            Ingredient.CODEC_NONEMPTY.fieldOf("apron").forGetter(r -> r.apron),
            Ingredient.CODEC_NONEMPTY.fieldOf("dye").forGetter(r -> r.dye),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(r -> r.result)
        ).apply(instance, ApronRecolorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ApronRecolorRecipe> PACKET =
            StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC, ApronRecolorRecipe::category,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.apron,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.dye,
                // 物品用注册表的流编解码：网络同步时两端都按注册名找同一个物品
                net.minecraft.network.codec.ByteBufCodecs.VAR_INT.map(
                    i -> Item.byId(i), Item::getId),
                r -> r.result,
                ApronRecolorRecipe::new);

        @Override
        public MapCodec<ApronRecolorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ApronRecolorRecipe> streamCodec() {
            return PACKET;
        }
    }
}
