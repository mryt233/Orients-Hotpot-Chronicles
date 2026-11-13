// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 4F-2：数据包汤底配方（{@code data/<命名空间>/recipes/soup/<名>.json}）。
 * 支持新增汤底，也支持按部分字段覆盖内置或 Java 扩展汤底（mryt233 改：1.20.1 适配）。作者：mryt233
 */
public final class SoupBaseRecipe implements Recipe<SimpleContainer> {
    public static final String PATH_PREFIX = "soup/";
    public static final RecipeType<SoupBaseRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":soup_base"; }
    };

    static final Codec<Item> ITEM_CODEC = ResourceLocation.CODEC.comapFlatMap(
        id -> BuiltInRegistries.ITEM.containsKey(id)
            ? DataResult.success(BuiltInRegistries.ITEM.get(id))
            : DataResult.error(() -> "未知物品 ID：" + id),
        BuiltInRegistries.ITEM::getKey
    );
    static final Codec<MobEffect> EFFECT_CODEC = ResourceLocation.CODEC.comapFlatMap(
        id -> BuiltInRegistries.MOB_EFFECT.containsKey(id)
            ? DataResult.success(BuiltInRegistries.MOB_EFFECT.get(id))
            : DataResult.error(() -> "未知状态效果 ID：" + id),
        BuiltInRegistries.MOB_EFFECT::getKey
    );
    static final Codec<Integer> COLOR_CODEC = Codec.either(
        Codec.INT,
        Codec.STRING.comapFlatMap(SoupBaseRecipe::parseColor, n -> String.format("#%08X", n))
    ).xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);

    public static final Serializer SERIALIZER = new Serializer();

    private final @Nullable ResourceLocation id;
    private final Optional<Item> source;
    private final Optional<Item> remainder;
    private final Optional<Item> additive;
    private final Optional<Float> speedMultiplier;
    private final Optional<BowlFood> bowlFood;
    private final Optional<EffectSpec> mainEffect;
    private final Optional<EffectSpec> secondEffect;
    private final Optional<EffectSpec> fireEffect;
    private final Optional<EffectSpec> hungerEffect;
    private final Optional<Boolean> clearsNausea;
    private final Optional<Boolean> spicy;
    private final Optional<String> langKey;
    private final Optional<String> bowlId;
    private final Optional<ScreenColors> screenColors;
    private final Optional<BubbleColors> bubbleColors;

    public SoupBaseRecipe(
        Optional<Item> source, Optional<Item> remainder, Optional<Item> additive,
        Optional<Float> speedMultiplier, Optional<BowlFood> bowlFood,
        Optional<EffectSpec> mainEffect, Optional<EffectSpec> secondEffect,
        Optional<EffectSpec> fireEffect, Optional<EffectSpec> hungerEffect,
        Optional<Boolean> clearsNausea, Optional<Boolean> spicy,
        Optional<String> langKey, Optional<String> bowlId,
        Optional<ScreenColors> screenColors, Optional<BubbleColors> bubbleColors
    ) {
        this(null, source, remainder, additive, speedMultiplier, bowlFood,
            mainEffect, secondEffect, fireEffect, hungerEffect,
            clearsNausea, spicy, langKey, bowlId, screenColors, bubbleColors);
    }

    public SoupBaseRecipe(
        @Nullable ResourceLocation id,
        Optional<Item> source, Optional<Item> remainder, Optional<Item> additive,
        Optional<Float> speedMultiplier, Optional<BowlFood> bowlFood,
        Optional<EffectSpec> mainEffect, Optional<EffectSpec> secondEffect,
        Optional<EffectSpec> fireEffect, Optional<EffectSpec> hungerEffect,
        Optional<Boolean> clearsNausea, Optional<Boolean> spicy,
        Optional<String> langKey, Optional<String> bowlId,
        Optional<ScreenColors> screenColors, Optional<BubbleColors> bubbleColors
    ) {
        this.id = id;
        this.source = Objects.requireNonNull(source);
        this.remainder = Objects.requireNonNull(remainder);
        this.additive = Objects.requireNonNull(additive);
        this.speedMultiplier = Objects.requireNonNull(speedMultiplier);
        this.bowlFood = Objects.requireNonNull(bowlFood);
        this.mainEffect = Objects.requireNonNull(mainEffect);
        this.secondEffect = Objects.requireNonNull(secondEffect);
        this.fireEffect = Objects.requireNonNull(fireEffect);
        this.hungerEffect = Objects.requireNonNull(hungerEffect);
        this.clearsNausea = Objects.requireNonNull(clearsNausea);
        this.spicy = Objects.requireNonNull(spicy);
        this.langKey = Objects.requireNonNull(langKey);
        this.bowlId = Objects.requireNonNull(bowlId);
        this.screenColors = Objects.requireNonNull(screenColors);
        this.bubbleColors = Objects.requireNonNull(bubbleColors);
    }

    public SoupBaseRecipe withId(ResourceLocation id) {
        return new SoupBaseRecipe(id, source, remainder, additive, speedMultiplier, bowlFood,
            mainEffect, secondEffect, fireEffect, hungerEffect,
            clearsNausea, spicy, langKey, bowlId, screenColors, bubbleColors);
    }

    public Optional<Item> source() { return source; }
    public Optional<Item> remainder() { return remainder; }
    public Optional<Item> additive() { return additive; }
    public Optional<Float> speedMultiplier() { return speedMultiplier; }
    public Optional<BowlFood> bowlFood() { return bowlFood; }
    public Optional<EffectSpec> mainEffect() { return mainEffect; }
    public Optional<EffectSpec> secondEffect() { return secondEffect; }
    public Optional<EffectSpec> fireEffect() { return fireEffect; }
    public Optional<EffectSpec> hungerEffect() { return hungerEffect; }
    public Optional<Boolean> clearsNausea() { return clearsNausea; }
    public Optional<Boolean> spicy() { return spicy; }
    public Optional<String> langKey() { return langKey; }
    public Optional<String> bowlId() { return bowlId; }
    public Optional<ScreenColors> screenColors() { return screenColors; }
    public Optional<BubbleColors> bubbleColors() { return bubbleColors; }

    public record BowlFood(int hunger, float saturation) {
        public static final Codec<BowlFood> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100).fieldOf("hunger").forGetter(BowlFood::hunger),
            Codec.floatRange(0.0F, 10.0F).fieldOf("saturation").forGetter(BowlFood::saturation)
        ).apply(i, BowlFood::new));

        static void write(FriendlyByteBuf buf, BowlFood v) {
            buf.writeVarInt(v.hunger);
            buf.writeFloat(v.saturation);
        }

        static BowlFood read(FriendlyByteBuf buf) {
            return new BowlFood(buf.readVarInt(), buf.readFloat());
        }
    }

    public record EffectSpec(MobEffect id, int duration) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            EFFECT_CODEC.fieldOf("id").forGetter(EffectSpec::id),
            Codec.intRange(0, 100000).fieldOf("duration").forGetter(EffectSpec::duration)
        ).apply(i, EffectSpec::new));

        static void write(FriendlyByteBuf buf, EffectSpec v) {
            writeEffect(buf, v.id);
            buf.writeVarInt(v.duration);
        }

        static EffectSpec read(FriendlyByteBuf buf) {
            return new EffectSpec(readEffect(buf), buf.readVarInt());
        }
    }

    public record ScreenColors(int base, int hi, int lo, List<Integer> bits) {
        public static final Codec<ScreenColors> CODEC = RecordCodecBuilder.create(i -> i.group(
            COLOR_CODEC.fieldOf("base").forGetter(ScreenColors::base),
            COLOR_CODEC.fieldOf("hi").forGetter(ScreenColors::hi),
            COLOR_CODEC.fieldOf("lo").forGetter(ScreenColors::lo),
            strictOptionalFieldOf(COLOR_CODEC.listOf(), "bits", List.of()).forGetter(ScreenColors::bits)
        ).apply(i, ScreenColors::new));

        static void write(FriendlyByteBuf buf, ScreenColors v) {
            buf.writeInt(v.base);
            buf.writeInt(v.hi);
            buf.writeInt(v.lo);
            buf.writeVarInt(v.bits.size());
            for (int bit : v.bits) buf.writeInt(bit);
        }

        static ScreenColors read(FriendlyByteBuf buf) {
            int base = buf.readInt();
            int hi = buf.readInt();
            int lo = buf.readInt();
            int n = buf.readVarInt();
            java.util.ArrayList<Integer> bits = new java.util.ArrayList<>(n);
            for (int i = 0; i < n; i++) bits.add(buf.readInt());
            return new ScreenColors(base, hi, lo, List.copyOf(bits));
        }
    }

    public record BubbleColors(int body, int hi) {
        public static final Codec<BubbleColors> CODEC = RecordCodecBuilder.create(i -> i.group(
            COLOR_CODEC.fieldOf("body").forGetter(BubbleColors::body),
            COLOR_CODEC.fieldOf("hi").forGetter(BubbleColors::hi)
        ).apply(i, BubbleColors::new));

        static void write(FriendlyByteBuf buf, BubbleColors v) {
            buf.writeInt(v.body);
            buf.writeInt(v.hi);
        }

        static BubbleColors read(FriendlyByteBuf buf) {
            return new BubbleColors(buf.readInt(), buf.readInt());
        }
    }

    static DataResult<Integer> parseColor(String raw) {
        String s = raw.trim();
        try {
            String hex;
            if (s.startsWith("#")) {
                hex = s.substring(1);
            } else if (s.startsWith("0x") || s.startsWith("0X")) {
                hex = s.substring(2);
            } else {
                return DataResult.success(Integer.parseInt(s));
            }
            if (hex.length() == 6) {
                return DataResult.success(0xFF000000 | Integer.parseUnsignedInt(hex, 16));
            }
            if (hex.length() == 8) {
                return DataResult.success((int) Long.parseUnsignedLong(hex, 16));
            }
            return DataResult.error(() -> "颜色格式应为 #RRGGBB 或 #AARRGGBB：" + raw);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "非法颜色值：" + raw);
        }
    }

    /** 去掉 {@code soup/} 目录前缀得到登记 ID；未放在 {@code recipes/soup/} 下时返回 null。 */
    public static ResourceLocation targetId(ResourceLocation recipeId) {
        if (recipeId == null) return null;
        String path = recipeId.getPath();
        if (!path.startsWith(PATH_PREFIX) || path.length() <= PATH_PREFIX.length()) {
            return null;
        }
        return new ResourceLocation(recipeId.getNamespace(), path.substring(PATH_PREFIX.length()));
    }

    /** 合并已有汤底（覆盖）或按新汤底构建；缺少必填项时记录警告并返回 null。 */
    public SoupDef resolve(ResourceLocation id, SoupDef base, Consumer<String> warn) {
        SoupDef.Builder b = base != null ? base.toBuilder() : SoupDef.builder(id);
        if (source.isPresent() || remainder.isPresent()) {
            java.util.function.Supplier<Item> src = source.<java.util.function.Supplier<Item>>map(item -> () -> item)
                .orElse(base != null ? base.sourceItem() : null);
            Item rem = remainder.orElse(base != null ? base.remainder() : Items.AIR);
            if (src != null) {
                b.source(src, rem);
            }
        }
        additive.ifPresent(item -> b.additive(() -> item));
        speedMultiplier.ifPresent(b::speed);
        bowlFood.ifPresent(bf -> b.bowlFood(bf.hunger(), bf.saturation()));
        mainEffect.ifPresent(e -> b.mainEffect(e.id(), e.duration()));
        secondEffect.ifPresent(e -> b.secondEffect(e.id(), e.duration()));
        fireEffect.ifPresent(e -> b.fireEffect(e.id(), e.duration()));
        hungerEffect.ifPresent(e -> b.hungerEffect(e.id(), e.duration()));
        clearsNausea.ifPresent(b::clearsNausea);
        spicy.ifPresent(b::spicy);
        langKey.ifPresent(b::langKey);
        bowlId.ifPresent(b::bowlId);
        screenColors.ifPresent(c -> {
            int[] bits = c.bits().stream().mapToInt(Integer::intValue).toArray();
            b.screenColors(c.base(), c.hi(), c.lo(), bits);
        });
        bubbleColors.ifPresent(c -> b.bubbleColors(c.body(), c.hi()));
        try {
            return b.build();
        } catch (IllegalStateException ex) {
            if (warn != null) {
                warn.accept("数据包汤底配方 " + id + " 无效，已跳过：" + ex.getMessage());
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

    static void writeItem(FriendlyByteBuf buf, Item item) {
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
    }

    static Item readItem(FriendlyByteBuf buf) {
        return BuiltInRegistries.ITEM.get(buf.readResourceLocation());
    }

    static void writeEffect(FriendlyByteBuf buf, MobEffect effect) {
        buf.writeResourceLocation(BuiltInRegistries.MOB_EFFECT.getKey(effect));
    }

    static MobEffect readEffect(FriendlyByteBuf buf) {
        return BuiltInRegistries.MOB_EFFECT.get(buf.readResourceLocation());
    }

    static <T> void writeOpt(FriendlyByteBuf buf, Optional<T> opt, BiConsumer<FriendlyByteBuf, T> writer) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(v -> writer.accept(buf, v));
    }

    static <T> Optional<T> readOpt(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
        return buf.readBoolean() ? Optional.of(reader.apply(buf)) : Optional.empty();
    }

    /** 1.20.1 的 DFU optionalFieldOf 会静默吞掉字段解析失败，显式提供严格可选字段以匹配高版本语义。 */
    static <A> com.mojang.serialization.MapCodec<Optional<A>> strictOptionalFieldOf(Codec<A> codec, String name) {
        return new com.mojang.serialization.MapCodec<>() {
            @Override
            public <T> DataResult<Optional<A>> decode(
                    com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.MapLike<T> input) {
                T val = input.get(name);
                if (val == null) {
                    return DataResult.success(Optional.empty());
                }
                return codec.parse(ops, val).map(Optional::of);
            }

            @Override
            public <T> com.mojang.serialization.RecordBuilder<T> encode(
                    Optional<A> input, com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.RecordBuilder<T> prefix) {
                if (input.isPresent()) {
                    prefix.add(name, codec.encodeStart(ops, input.get()));
                }
                return prefix;
            }

            @Override
            public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
                return java.util.stream.Stream.of(ops.createString(name));
            }
        };
    }

    static <A> com.mojang.serialization.MapCodec<A> strictOptionalFieldOf(Codec<A> codec, String name, A defaultValue) {
        return strictOptionalFieldOf(codec, name).xmap(
            opt -> opt.orElse(defaultValue),
            val -> Objects.equals(val, defaultValue) ? Optional.empty() : Optional.of(val)
        );
    }

    public static final class Serializer implements RecipeSerializer<SoupBaseRecipe> {
        public static final Codec<SoupBaseRecipe> CODEC = RecordCodecBuilder.create(i -> i.group(
            strictOptionalFieldOf(ITEM_CODEC, "source").forGetter(SoupBaseRecipe::source),
            strictOptionalFieldOf(ITEM_CODEC, "remainder").forGetter(SoupBaseRecipe::remainder),
            strictOptionalFieldOf(ITEM_CODEC, "additive").forGetter(SoupBaseRecipe::additive),
            strictOptionalFieldOf(Codec.floatRange(0.1F, 10.0F), "speed_multiplier").forGetter(SoupBaseRecipe::speedMultiplier),
            strictOptionalFieldOf(BowlFood.CODEC, "bowl_food").forGetter(SoupBaseRecipe::bowlFood),
            strictOptionalFieldOf(EffectSpec.CODEC, "main_effect").forGetter(SoupBaseRecipe::mainEffect),
            strictOptionalFieldOf(EffectSpec.CODEC, "second_effect").forGetter(SoupBaseRecipe::secondEffect),
            strictOptionalFieldOf(EffectSpec.CODEC, "fire_effect").forGetter(SoupBaseRecipe::fireEffect),
            strictOptionalFieldOf(EffectSpec.CODEC, "hunger_effect").forGetter(SoupBaseRecipe::hungerEffect),
            strictOptionalFieldOf(Codec.BOOL, "clears_nausea").forGetter(SoupBaseRecipe::clearsNausea),
            strictOptionalFieldOf(Codec.BOOL, "spicy").forGetter(SoupBaseRecipe::spicy),
            strictOptionalFieldOf(Codec.STRING, "lang_key").forGetter(SoupBaseRecipe::langKey),
            strictOptionalFieldOf(Codec.STRING, "bowl_id").forGetter(SoupBaseRecipe::bowlId),
            strictOptionalFieldOf(ScreenColors.CODEC, "screen_colors").forGetter(SoupBaseRecipe::screenColors),
            strictOptionalFieldOf(BubbleColors.CODEC, "bubble_colors").forGetter(SoupBaseRecipe::bubbleColors)
        ).apply(i, SoupBaseRecipe::new));

        @Override
        public SoupBaseRecipe fromJson(ResourceLocation id, JsonObject json) {
            return CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, msg -> { throw new JsonSyntaxException(msg); })
                .withId(id);
        }

        @Override
        public SoupBaseRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new SoupBaseRecipe(
                id,
                readOpt(buf, SoupBaseRecipe::readItem),
                readOpt(buf, SoupBaseRecipe::readItem),
                readOpt(buf, SoupBaseRecipe::readItem),
                readOpt(buf, FriendlyByteBuf::readFloat),
                readOpt(buf, BowlFood::read),
                readOpt(buf, EffectSpec::read),
                readOpt(buf, EffectSpec::read),
                readOpt(buf, EffectSpec::read),
                readOpt(buf, EffectSpec::read),
                readOpt(buf, FriendlyByteBuf::readBoolean),
                readOpt(buf, FriendlyByteBuf::readBoolean),
                readOpt(buf, FriendlyByteBuf::readUtf),
                readOpt(buf, FriendlyByteBuf::readUtf),
                readOpt(buf, ScreenColors::read),
                readOpt(buf, BubbleColors::read)
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SoupBaseRecipe r) {
            writeOpt(buf, r.source, SoupBaseRecipe::writeItem);
            writeOpt(buf, r.remainder, SoupBaseRecipe::writeItem);
            writeOpt(buf, r.additive, SoupBaseRecipe::writeItem);
            writeOpt(buf, r.speedMultiplier, FriendlyByteBuf::writeFloat);
            writeOpt(buf, r.bowlFood, BowlFood::write);
            writeOpt(buf, r.mainEffect, EffectSpec::write);
            writeOpt(buf, r.secondEffect, EffectSpec::write);
            writeOpt(buf, r.fireEffect, EffectSpec::write);
            writeOpt(buf, r.hungerEffect, EffectSpec::write);
            writeOpt(buf, r.clearsNausea, FriendlyByteBuf::writeBoolean);
            writeOpt(buf, r.spicy, FriendlyByteBuf::writeBoolean);
            writeOpt(buf, r.langKey, FriendlyByteBuf::writeUtf);
            writeOpt(buf, r.bowlId, FriendlyByteBuf::writeUtf);
            writeOpt(buf, r.screenColors, ScreenColors::write);
            writeOpt(buf, r.bubbleColors, BubbleColors::write);
        }
    }
}
