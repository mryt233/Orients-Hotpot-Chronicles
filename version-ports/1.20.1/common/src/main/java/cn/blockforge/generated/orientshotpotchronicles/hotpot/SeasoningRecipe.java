// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 4F-2：数据包蘸料配方（{@code data/<命名空间>/recipes/seasoning/<名>.json}）。
 * 支持新增蘸料，也支持按部分字段覆盖内置或 Java 扩展蘸料（mryt233 改：1.20.1 适配）。作者：mryt233
 */
public final class SeasoningRecipe implements Recipe<SimpleContainer> {
    public static final String PATH_PREFIX = "seasoning/";
    public static final RecipeType<SeasoningRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":seasoning"; }
    };

    private static final Codec<List<Item>> RAWS_CODEC = Codec.either(
        SoupBaseRecipe.ITEM_CODEC,
        SoupBaseRecipe.ITEM_CODEC.listOf()
    ).xmap(e -> e.map(List::of, List::copyOf), list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    public static final Serializer SERIALIZER = new Serializer();

    private final @Nullable ResourceLocation id;
    private final Optional<List<Item>> raws;
    private final Optional<Integer> color;
    private final Optional<EffectSpec> effect;
    private final Optional<Integer> hunger;
    private final Optional<List<MobEffect>> removes;
    private final Optional<Boolean> garnish;
    private final Optional<String> nameKey;
    private final Optional<String> effectKey;

    public SeasoningRecipe(
        Optional<List<Item>> raws, Optional<Integer> color, Optional<EffectSpec> effect,
        Optional<Integer> hunger, Optional<List<MobEffect>> removes,
        Optional<Boolean> garnish, Optional<String> nameKey, Optional<String> effectKey
    ) {
        this(null, raws, color, effect, hunger, removes, garnish, nameKey, effectKey);
    }

    public SeasoningRecipe(
        @Nullable ResourceLocation id,
        Optional<List<Item>> raws, Optional<Integer> color, Optional<EffectSpec> effect,
        Optional<Integer> hunger, Optional<List<MobEffect>> removes,
        Optional<Boolean> garnish, Optional<String> nameKey, Optional<String> effectKey
    ) {
        this.id = id;
        this.raws = Objects.requireNonNull(raws);
        this.color = Objects.requireNonNull(color);
        this.effect = Objects.requireNonNull(effect);
        this.hunger = Objects.requireNonNull(hunger);
        this.removes = Objects.requireNonNull(removes);
        this.garnish = Objects.requireNonNull(garnish);
        this.nameKey = Objects.requireNonNull(nameKey);
        this.effectKey = Objects.requireNonNull(effectKey);
    }

    public SeasoningRecipe withId(ResourceLocation id) {
        return new SeasoningRecipe(id, raws, color, effect, hunger, removes, garnish, nameKey, effectKey);
    }

    public Optional<List<Item>> raws() { return raws; }
    public Optional<Integer> color() { return color; }
    public Optional<EffectSpec> effect() { return effect; }
    public Optional<Integer> hunger() { return hunger; }
    public Optional<List<MobEffect>> removes() { return removes; }
    public Optional<Boolean> garnish() { return garnish; }
    public Optional<String> nameKey() { return nameKey; }
    public Optional<String> effectKey() { return effectKey; }

    public record EffectSpec(MobEffect id, int duration, int amplifier) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            SoupBaseRecipe.EFFECT_CODEC.fieldOf("id").forGetter(EffectSpec::id),
            Codec.intRange(0, 100000).fieldOf("duration").forGetter(EffectSpec::duration),
            SoupBaseRecipe.strictOptionalFieldOf(Codec.intRange(0, 9), "amplifier", 0).forGetter(EffectSpec::amplifier)
        ).apply(i, EffectSpec::new));

        static void write(FriendlyByteBuf buf, EffectSpec v) {
            SoupBaseRecipe.writeEffect(buf, v.id);
            buf.writeVarInt(v.duration);
            buf.writeVarInt(v.amplifier);
        }

        static EffectSpec read(FriendlyByteBuf buf) {
            return new EffectSpec(SoupBaseRecipe.readEffect(buf), buf.readVarInt(), buf.readVarInt());
        }
    }

    /** 去掉 {@code seasoning/} 目录前缀得到登记 ID；未放在 {@code recipes/seasoning/} 下时返回 null。 */
    public static ResourceLocation targetId(ResourceLocation recipeId) {
        if (recipeId == null) return null;
        String path = recipeId.getPath();
        if (!path.startsWith(PATH_PREFIX) || path.length() <= PATH_PREFIX.length()) {
            return null;
        }
        return new ResourceLocation(recipeId.getNamespace(), path.substring(PATH_PREFIX.length()));
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

    @Override public ResourceLocation getId() { return id != null ? id : HotPot.id("unbound"); }
    @Override public boolean matches(SimpleContainer input, Level level) { return false; }
    @Override public ItemStack assemble(SimpleContainer input, RegistryAccess registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public boolean isSpecial() { return true; }

    private static void writeItemList(FriendlyByteBuf buf, List<Item> items) {
        buf.writeVarInt(items.size());
        for (Item item : items) SoupBaseRecipe.writeItem(buf, item);
    }

    private static List<Item> readItemList(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        ArrayList<Item> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(SoupBaseRecipe.readItem(buf));
        return List.copyOf(list);
    }

    private static void writeEffectList(FriendlyByteBuf buf, List<MobEffect> effects) {
        buf.writeVarInt(effects.size());
        for (MobEffect e : effects) SoupBaseRecipe.writeEffect(buf, e);
    }

    private static List<MobEffect> readEffectList(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        ArrayList<MobEffect> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(SoupBaseRecipe.readEffect(buf));
        return List.copyOf(list);
    }

    public static final class Serializer implements RecipeSerializer<SeasoningRecipe> {
        public static final Codec<SeasoningRecipe> CODEC = RecordCodecBuilder.create(i -> i.group(
            SoupBaseRecipe.strictOptionalFieldOf(RAWS_CODEC, "raws").forGetter(SeasoningRecipe::raws),
            SoupBaseRecipe.strictOptionalFieldOf(SoupBaseRecipe.COLOR_CODEC, "color").forGetter(SeasoningRecipe::color),
            SoupBaseRecipe.strictOptionalFieldOf(EffectSpec.CODEC, "effect").forGetter(SeasoningRecipe::effect),
            SoupBaseRecipe.strictOptionalFieldOf(Codec.intRange(0, 100), "hunger").forGetter(SeasoningRecipe::hunger),
            SoupBaseRecipe.strictOptionalFieldOf(SoupBaseRecipe.EFFECT_CODEC.listOf(), "removes").forGetter(SeasoningRecipe::removes),
            SoupBaseRecipe.strictOptionalFieldOf(Codec.BOOL, "garnish").forGetter(SeasoningRecipe::garnish),
            SoupBaseRecipe.strictOptionalFieldOf(Codec.STRING, "name_key").forGetter(SeasoningRecipe::nameKey),
            SoupBaseRecipe.strictOptionalFieldOf(Codec.STRING, "effect_key").forGetter(SeasoningRecipe::effectKey)
        ).apply(i, SeasoningRecipe::new));

        @Override
        public SeasoningRecipe fromJson(ResourceLocation id, JsonObject json) {
            return CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, msg -> { throw new JsonSyntaxException(msg); })
                .withId(id);
        }

        @Override
        public SeasoningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new SeasoningRecipe(
                id,
                SoupBaseRecipe.readOpt(buf, SeasoningRecipe::readItemList),
                SoupBaseRecipe.readOpt(buf, FriendlyByteBuf::readInt),
                SoupBaseRecipe.readOpt(buf, EffectSpec::read),
                SoupBaseRecipe.readOpt(buf, FriendlyByteBuf::readVarInt),
                SoupBaseRecipe.readOpt(buf, SeasoningRecipe::readEffectList),
                SoupBaseRecipe.readOpt(buf, FriendlyByteBuf::readBoolean),
                SoupBaseRecipe.readOpt(buf, FriendlyByteBuf::readUtf),
                SoupBaseRecipe.readOpt(buf, FriendlyByteBuf::readUtf)
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SeasoningRecipe r) {
            SoupBaseRecipe.writeOpt(buf, r.raws, SeasoningRecipe::writeItemList);
            SoupBaseRecipe.writeOpt(buf, r.color, FriendlyByteBuf::writeInt);
            SoupBaseRecipe.writeOpt(buf, r.effect, EffectSpec::write);
            SoupBaseRecipe.writeOpt(buf, r.hunger, FriendlyByteBuf::writeVarInt);
            SoupBaseRecipe.writeOpt(buf, r.removes, SeasoningRecipe::writeEffectList);
            SoupBaseRecipe.writeOpt(buf, r.garnish, FriendlyByteBuf::writeBoolean);
            SoupBaseRecipe.writeOpt(buf, r.nameKey, FriendlyByteBuf::writeUtf);
            SoupBaseRecipe.writeOpt(buf, r.effectKey, FriendlyByteBuf::writeUtf);
        }
    }
}
