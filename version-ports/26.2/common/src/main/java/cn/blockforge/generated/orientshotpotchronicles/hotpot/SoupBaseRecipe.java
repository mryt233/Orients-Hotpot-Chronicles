// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 4F-2：数据包汤底配方（{@code data/<命名空间>/recipe/soup/<名>.json}）。
 * 支持新增汤底，也支持按部分字段覆盖内置或 Java 扩展汤底。作者：mryt233
 */
public record SoupBaseRecipe(
    Optional<Item> source,
    Optional<Item> remainder,
    Optional<Item> additive,
    Optional<Float> speedMultiplier,
    Optional<BowlFood> bowlFood,
    Optional<EffectSpec> mainEffect,
    Optional<EffectSpec> secondEffect,
    Optional<EffectSpec> fireEffect,
    Optional<EffectSpec> hungerEffect,
    Optional<Boolean> clearsNausea,
    Optional<Boolean> spicy,
    Optional<String> langKey,
    Optional<String> bowlId,
    Optional<ScreenColors> screenColors,
    Optional<BubbleColors> bubbleColors
) implements Recipe<SingleRecipeInput> {
    public static final String PATH_PREFIX = "soup/";
    public static final RecipeType<SoupBaseRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return HotPot.MOD_ID + ":soup_base"; }
    };

    static final Codec<Item> ITEM_CODEC = BuiltInRegistries.ITEM.byNameCodec();
    static final Codec<Holder<MobEffect>> EFFECT_CODEC = BuiltInRegistries.MOB_EFFECT.holderByNameCodec();
    static final Codec<Integer> COLOR_CODEC = Codec.either(
        Codec.INT,
        Codec.STRING.comapFlatMap(SoupBaseRecipe::parseColor, n -> String.format("#%08X", n))
    ).xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);

    static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM_STREAM =
        ByteBufCodecs.registry(Registries.ITEM);
    static final StreamCodec<RegistryFriendlyByteBuf, Holder<MobEffect>> EFFECT_STREAM =
        ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT);

    public record BowlFood(int hunger, float saturation) {
        public static final Codec<BowlFood> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100).fieldOf("hunger").forGetter(BowlFood::hunger),
            Codec.floatRange(0.0F, 10.0F).fieldOf("saturation").forGetter(BowlFood::saturation)
        ).apply(i, BowlFood::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, BowlFood> STREAM = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BowlFood::hunger,
            ByteBufCodecs.FLOAT, BowlFood::saturation,
            BowlFood::new
        );
    }

    public record EffectSpec(Holder<MobEffect> id, int duration) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            EFFECT_CODEC.fieldOf("id").forGetter(EffectSpec::id),
            Codec.intRange(0, 100000).fieldOf("duration").forGetter(EffectSpec::duration)
        ).apply(i, EffectSpec::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EffectSpec> STREAM = StreamCodec.composite(
            EFFECT_STREAM, EffectSpec::id,
            ByteBufCodecs.VAR_INT, EffectSpec::duration,
            EffectSpec::new
        );
    }

    public record ScreenColors(int base, int hi, int lo, List<Integer> bits) {
        public static final Codec<ScreenColors> CODEC = RecordCodecBuilder.create(i -> i.group(
            COLOR_CODEC.fieldOf("base").forGetter(ScreenColors::base),
            COLOR_CODEC.fieldOf("hi").forGetter(ScreenColors::hi),
            COLOR_CODEC.fieldOf("lo").forGetter(ScreenColors::lo),
            COLOR_CODEC.listOf().optionalFieldOf("bits", List.of()).forGetter(ScreenColors::bits)
        ).apply(i, ScreenColors::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScreenColors> STREAM = StreamCodec.composite(
            ByteBufCodecs.INT, ScreenColors::base,
            ByteBufCodecs.INT, ScreenColors::hi,
            ByteBufCodecs.INT, ScreenColors::lo,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ScreenColors::bits,
            ScreenColors::new
        );
    }

    public record BubbleColors(int body, int hi) {
        public static final Codec<BubbleColors> CODEC = RecordCodecBuilder.create(i -> i.group(
            COLOR_CODEC.fieldOf("body").forGetter(BubbleColors::body),
            COLOR_CODEC.fieldOf("hi").forGetter(BubbleColors::hi)
        ).apply(i, BubbleColors::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, BubbleColors> STREAM = StreamCodec.composite(
            ByteBufCodecs.INT, BubbleColors::body,
            ByteBufCodecs.INT, BubbleColors::hi,
            BubbleColors::new
        );
    }

    static <T> void writeOpt(RegistryFriendlyByteBuf buf, Optional<T> opt, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        buf.writeBoolean(opt.isPresent());
        opt.ifPresent(v -> codec.encode(buf, v));
    }

    static <T> Optional<T> readOpt(RegistryFriendlyByteBuf buf, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return buf.readBoolean() ? Optional.of(codec.decode(buf)) : Optional.empty();
    }

    private static final MapCodec<SoupBaseRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ITEM_CODEC.optionalFieldOf("source").forGetter(SoupBaseRecipe::source),
        ITEM_CODEC.optionalFieldOf("remainder").forGetter(SoupBaseRecipe::remainder),
        ITEM_CODEC.optionalFieldOf("additive").forGetter(SoupBaseRecipe::additive),
        Codec.floatRange(0.1F, 10.0F).optionalFieldOf("speed_multiplier").forGetter(SoupBaseRecipe::speedMultiplier),
        BowlFood.CODEC.optionalFieldOf("bowl_food").forGetter(SoupBaseRecipe::bowlFood),
        EffectSpec.CODEC.optionalFieldOf("main_effect").forGetter(SoupBaseRecipe::mainEffect),
        EffectSpec.CODEC.optionalFieldOf("second_effect").forGetter(SoupBaseRecipe::secondEffect),
        EffectSpec.CODEC.optionalFieldOf("fire_effect").forGetter(SoupBaseRecipe::fireEffect),
        EffectSpec.CODEC.optionalFieldOf("hunger_effect").forGetter(SoupBaseRecipe::hungerEffect),
        Codec.BOOL.optionalFieldOf("clears_nausea").forGetter(SoupBaseRecipe::clearsNausea),
        Codec.BOOL.optionalFieldOf("spicy").forGetter(SoupBaseRecipe::spicy),
        Codec.STRING.optionalFieldOf("lang_key").forGetter(SoupBaseRecipe::langKey),
        Codec.STRING.optionalFieldOf("bowl_id").forGetter(SoupBaseRecipe::bowlId),
        ScreenColors.CODEC.optionalFieldOf("screen_colors").forGetter(SoupBaseRecipe::screenColors),
        BubbleColors.CODEC.optionalFieldOf("bubble_colors").forGetter(SoupBaseRecipe::bubbleColors)
    ).apply(i, SoupBaseRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, SoupBaseRecipe> STREAM = StreamCodec.of(
        (buf, r) -> {
            writeOpt(buf, r.source, ITEM_STREAM);
            writeOpt(buf, r.remainder, ITEM_STREAM);
            writeOpt(buf, r.additive, ITEM_STREAM);
            writeOpt(buf, r.speedMultiplier, ByteBufCodecs.FLOAT);
            writeOpt(buf, r.bowlFood, BowlFood.STREAM);
            writeOpt(buf, r.mainEffect, EffectSpec.STREAM);
            writeOpt(buf, r.secondEffect, EffectSpec.STREAM);
            writeOpt(buf, r.fireEffect, EffectSpec.STREAM);
            writeOpt(buf, r.hungerEffect, EffectSpec.STREAM);
            writeOpt(buf, r.clearsNausea, ByteBufCodecs.BOOL);
            writeOpt(buf, r.spicy, ByteBufCodecs.BOOL);
            writeOpt(buf, r.langKey, ByteBufCodecs.STRING_UTF8);
            writeOpt(buf, r.bowlId, ByteBufCodecs.STRING_UTF8);
            writeOpt(buf, r.screenColors, ScreenColors.STREAM);
            writeOpt(buf, r.bubbleColors, BubbleColors.STREAM);
        },
        buf -> new SoupBaseRecipe(
            readOpt(buf, ITEM_STREAM),
            readOpt(buf, ITEM_STREAM),
            readOpt(buf, ITEM_STREAM),
            readOpt(buf, ByteBufCodecs.FLOAT),
            readOpt(buf, BowlFood.STREAM),
            readOpt(buf, EffectSpec.STREAM),
            readOpt(buf, EffectSpec.STREAM),
            readOpt(buf, EffectSpec.STREAM),
            readOpt(buf, EffectSpec.STREAM),
            readOpt(buf, ByteBufCodecs.BOOL),
            readOpt(buf, ByteBufCodecs.BOOL),
            readOpt(buf, ByteBufCodecs.STRING_UTF8),
            readOpt(buf, ByteBufCodecs.STRING_UTF8),
            readOpt(buf, ScreenColors.STREAM),
            readOpt(buf, BubbleColors.STREAM)
        )
    );

    public static final RecipeSerializer<SoupBaseRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM);

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

    public static Identifier targetId(ResourceKey<Recipe<?>> recipeKey) {
        return recipeKey == null ? null : targetId(recipeKey.identifier());
    }

    /** 去掉 {@code soup/} 目录前缀得到登记 ID；未放在 {@code recipe/soup/} 下时返回 null。 */
    public static Identifier targetId(Identifier recipeId) {
        if (recipeId == null) return null;
        String path = recipeId.getPath();
        if (!path.startsWith(PATH_PREFIX) || path.length() <= PATH_PREFIX.length()) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(recipeId.getNamespace(), path.substring(PATH_PREFIX.length()));
    }

    /** 合并已有汤底（覆盖）或按新汤底构建；缺少必填项时记录警告并返回 null。 */
    public SoupDef resolve(Identifier id, SoupDef base, Consumer<String> warn) {
        SoupDef.Builder b = base != null ? base.toBuilder() : SoupDef.builder(id);
        if (source.isPresent() || remainder.isPresent()) {
            Supplier<Item> src = source.<Supplier<Item>>map(item -> () -> item)
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

    @Override public boolean matches(SingleRecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(SingleRecipeInput input) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<? extends Recipe<SingleRecipeInput>> getType() { return TYPE; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public String group() { return ""; }
    @Override public boolean showNotification() { return false; }
    @Override public boolean isSpecial() { return true; }
}
