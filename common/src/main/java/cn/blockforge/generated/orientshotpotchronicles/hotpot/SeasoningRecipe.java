// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 4F-2：数据包蘸料配方（{@code data/<命名空间>/recipe/seasoning/<名>.json}）。
 * 支持新增蘸料，也支持按部分字段覆盖内置或 Java 扩展蘸料。作者：mryt233
 */
public record SeasoningRecipe(
    Optional<List<Item>> raws,
    Optional<Integer> color,
    Optional<EffectSpec> effect,
    Optional<Integer> hunger,
    Optional<List<Holder<MobEffect>>> removes,
    Optional<Boolean> garnish,
    Optional<String> nameKey,
    Optional<String> effectKey
) implements Recipe<SingleRecipeInput> {
    public static final String PATH_PREFIX = "seasoning/";
    public static final RecipeType<SeasoningRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":seasoning"; }
    };

    private static final Codec<List<Item>> RAWS_CODEC = Codec.either(
        SoupBaseRecipe.ITEM_CODEC,
        SoupBaseRecipe.ITEM_CODEC.listOf()
    ).xmap(e -> e.map(List::of, List::copyOf), list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Item>> ITEM_LIST_STREAM =
        SoupBaseRecipe.ITEM_STREAM.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Holder<MobEffect>>> EFFECT_LIST_STREAM =
        SoupBaseRecipe.EFFECT_STREAM.apply(ByteBufCodecs.list());

    public static final Serializer SERIALIZER = new Serializer();

    public record EffectSpec(Holder<MobEffect> id, int duration, int amplifier) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            SoupBaseRecipe.EFFECT_CODEC.fieldOf("id").forGetter(EffectSpec::id),
            Codec.intRange(0, 100000).fieldOf("duration").forGetter(EffectSpec::duration),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier)
        ).apply(i, EffectSpec::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EffectSpec> STREAM = StreamCodec.composite(
            SoupBaseRecipe.EFFECT_STREAM, EffectSpec::id,
            ByteBufCodecs.VAR_INT, EffectSpec::duration,
            ByteBufCodecs.VAR_INT, EffectSpec::amplifier,
            EffectSpec::new
        );
    }

    /** 去掉 {@code seasoning/} 目录前缀得到登记 ID；未放在 {@code recipe/seasoning/} 下时返回 null。 */
    public static ResourceLocation targetId(ResourceLocation recipeId) {
        if (recipeId == null) return null;
        String path = recipeId.getPath();
        if (!path.startsWith(PATH_PREFIX) || path.length() <= PATH_PREFIX.length()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(), path.substring(PATH_PREFIX.length()));
    }

    /** 合并已有蘸料（覆盖）或按新蘸料构建；缺少原料等必填项时记录警告并返回 null。 */
    public SeasoningDef resolve(ResourceLocation id, SeasoningDef base, Consumer<String> warn) {
        SeasoningDef.Builder b = base != null ? base.toBuilder() : SeasoningDef.builder(id);
        raws.ifPresent(items -> b.replaceRaws(items.stream().<Supplier<Item>>map(item -> () -> item).toList()));
        color.ifPresent(b::color);
        effect.ifPresent(e -> b.effect(e.id(), e.duration(), e.amplifier()));
        hunger.ifPresent(b::hunger);
        removes.ifPresent(b::removes);
        garnish.ifPresent(b::garnish);
        nameKey.ifPresent(b::nameKey);
        effectKey.ifPresent(b::effectKey);
        try {
            return b.build();
        } catch (IllegalArgumentException ex) {
            if (warn != null) {
                warn.accept("数据包蘸料配方 " + id + " 无效，已跳过：" + ex.getMessage());
            }
            return null;
        }
    }

    @Override public boolean matches(SingleRecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<SeasoningRecipe> {
        private static final MapCodec<SeasoningRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RAWS_CODEC.optionalFieldOf("raws").forGetter(SeasoningRecipe::raws),
            SoupBaseRecipe.COLOR_CODEC.optionalFieldOf("color").forGetter(SeasoningRecipe::color),
            EffectSpec.CODEC.optionalFieldOf("effect").forGetter(SeasoningRecipe::effect),
            Codec.intRange(0, 100).optionalFieldOf("hunger").forGetter(SeasoningRecipe::hunger),
            SoupBaseRecipe.EFFECT_CODEC.listOf().optionalFieldOf("removes").forGetter(SeasoningRecipe::removes),
            Codec.BOOL.optionalFieldOf("garnish").forGetter(SeasoningRecipe::garnish),
            Codec.STRING.optionalFieldOf("name_key").forGetter(SeasoningRecipe::nameKey),
            Codec.STRING.optionalFieldOf("effect_key").forGetter(SeasoningRecipe::effectKey)
        ).apply(i, SeasoningRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SeasoningRecipe> STREAM = StreamCodec.of(
            (buf, r) -> {
                SoupBaseRecipe.writeOpt(buf, r.raws, ITEM_LIST_STREAM);
                SoupBaseRecipe.writeOpt(buf, r.color, ByteBufCodecs.INT);
                SoupBaseRecipe.writeOpt(buf, r.effect, EffectSpec.STREAM);
                SoupBaseRecipe.writeOpt(buf, r.hunger, ByteBufCodecs.VAR_INT);
                SoupBaseRecipe.writeOpt(buf, r.removes, EFFECT_LIST_STREAM);
                SoupBaseRecipe.writeOpt(buf, r.garnish, ByteBufCodecs.BOOL);
                SoupBaseRecipe.writeOpt(buf, r.nameKey, ByteBufCodecs.STRING_UTF8);
                SoupBaseRecipe.writeOpt(buf, r.effectKey, ByteBufCodecs.STRING_UTF8);
            },
            buf -> new SeasoningRecipe(
                SoupBaseRecipe.readOpt(buf, ITEM_LIST_STREAM),
                SoupBaseRecipe.readOpt(buf, ByteBufCodecs.INT),
                SoupBaseRecipe.readOpt(buf, EffectSpec.STREAM),
                SoupBaseRecipe.readOpt(buf, ByteBufCodecs.VAR_INT),
                SoupBaseRecipe.readOpt(buf, EFFECT_LIST_STREAM),
                SoupBaseRecipe.readOpt(buf, ByteBufCodecs.BOOL),
                SoupBaseRecipe.readOpt(buf, ByteBufCodecs.STRING_UTF8),
                SoupBaseRecipe.readOpt(buf, ByteBufCodecs.STRING_UTF8)
            )
        );

        @Override public MapCodec<SeasoningRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SeasoningRecipe> streamCodec() { return STREAM; }
    }
}
