package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 一种汤底的全部「定义」：它是谁、倒进去的是什么、喝下去有什么效果、界面和气泡长什么色；
 * 以及由这些定义算出来的玩法（加热速度、一碗汤的营养、风味加成效果）。
 *
 * <p>第四批 4B-1（mryt233，2026-11-11）：原来这些字段直接散落在 {@link SoupBase} 枚举里；
 * 现在抽成不可变记录，枚举的六个值各持有一份，并按顺序登记进 {@link Soups}（登记表的前 6 项）。
 * 4B-2：玩法方法（速度、营养、风味加成……）从枚举搬到这里，游戏里所有调用点改成问登记表拿到的定义；
 * 枚举保留同名转发方法，签名与数值不变，供单测、探针和联动兼容继续使用。以后（4F）Java 登记和数据包新增的汤底
 * 也是同一种记录。</p>
 *
 * <p>原料用 {@link Supplier}：新汤底的原料是本模组物品，登记时 {@code HotPotContent} 可能还没注册好。
 * 效果为 null 表示这条效果不存在。{@code screenBits} 在构造和读取时都拷贝，记录对外不可变。
 * 带 default 前缀的是「默认值」，实际生效的值由配置文件 {@code soup_speed.*}、{@code soup_bowl.*} 覆盖
 * （见 {@link #speedMul()}、{@link #bowlHunger()} 等）。</p>
 *
 * @param id                   汤底 ID；内置项是火锅命名空间下的短名（clear、spicy ……，同 {@code SoupBase.textureKey()}）
 * @param langKey              语言键后缀（text.&lt;modid&gt;.soup.xxx）
 * @param bowlId               一碗汤的物品 ID（路径部分）
 * @param defaultSpeedMul      加热速度倍率默认值（配置可覆盖）
 * @param sourceItem           倒进锅里的原料（水桶、牛奶桶、蘑菇煲、岩浆桶……）
 * @param remainderItem        舀出来时还回去的容器
 * @param additive             锅底料（加进清汤把它变成这种汤）；没有就是 null
 * @param defaultBowlHunger    一碗汤默认回多少饥饿
 * @param defaultBowlSaturation 一碗汤默认饱和度系数
 * @param mainEffect           主效果，及其默认时长
 * @param secondEffect         副效果，及其默认时长
 * @param fireEffect           抗火效果（红汤特辣以上），及其默认时长
 * @param hungerEffect         饥饿效果（红汤特辣以上），及其默认时长
 * @param clearsNausea         喝下 / 吃下时解掉「反胃」
 * @param spicy                是不是红汤
 * @param screenBase           锅里界面汤面色块：底色
 * @param screenHi             高光
 * @param screenLo             暗部
 * @param screenBits           食材小点颜色（几样轮换）
 * @param bubbleBody           沸腾气泡体色
 * @param bubbleHi             沸腾气泡高光
 * 作者：mryt233 */
public record SoupDef(
    ResourceLocation id, String langKey, String bowlId, float defaultSpeedMul,
    Supplier<Item> sourceItem, Item remainderItem, Supplier<Item> additive,
    int defaultBowlHunger, float defaultBowlSaturation,
    Holder<MobEffect> mainEffect, int defaultDuration,
    Holder<MobEffect> secondEffect, int defaultSecondDuration,
    Holder<MobEffect> fireEffect, int defaultFireDuration,
    Holder<MobEffect> hungerEffect, int defaultHungerDuration,
    boolean clearsNausea, boolean spicy,
    int screenBase, int screenHi, int screenLo, int[] screenBits,
    int bubbleBody, int bubbleHi) {

    public SoupDef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(langKey, "langKey");
        Objects.requireNonNull(bowlId, "bowlId");
        Objects.requireNonNull(sourceItem, "sourceItem");
        Objects.requireNonNull(remainderItem, "remainderItem");
        Objects.requireNonNull(screenBits, "screenBits");
        screenBits = screenBits.clone();
    }

    /** 食材小点颜色（返回拷贝）。 */
    @Override
    public int[] screenBits() {
        return screenBits.clone();
    }

    // ---------------------------------------------------------------- 名字与外观

    /**
     * 配置键 / 汤面贴图名：火锅自己的汤底是短名（clear、spicy），
     * 别的命名空间的汤底是完整 ID。内置项等于旧的 {@code SoupBase.textureKey()}。
     */
    public String key() {
        return HotPot.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    /** 完整语言键；若 {@code langKey} 已带 {@code text.} 前缀（第三方自定义）则直接返回，否则拼 {@code text.<modid>.<langKey>}。 */
    public String nameKey() {
        return (langKey.startsWith("text.") || (langKey.contains(".") && !langKey.startsWith("soup.")))
            ? langKey : "text." + HotPot.MOD_ID + "." + langKey;
    }

    /**
     * 方块状态里的汤面值。内置项按名字对应 {@link SoupVisual}（启动时核对过一一对得上）；
     * 其他汤底统一映射为 {@link SoupVisual#OTHER}（mryt233 改，4F-3）。
     */
    public SoupVisual visual() {
        for (SoupVisual candidate : SoupVisual.values()) {
            if (candidate != SoupVisual.NONE && candidate != SoupVisual.OTHER
                && candidate.getSerializedName().equals(key())) {
                return candidate;
            }
        }
        return SoupVisual.OTHER;
    }

    // ---------------------------------------------------------------- 原料

    /** 倒进锅里的原料（水桶、牛奶桶、蘑菇煲、岩浆桶），舀出来时原样还回去。 */
    public Item source() {
        return sourceItem.get();
    }

    public Item remainder() {
        return remainderItem;
    }

    /** 手里这件是不是这种汤的原料（直接倒的汤桶，或加进清汤的锅底料）。 */
    public boolean matchesSource(ItemStack stack) {
        return stack.is(sourceItem.get()) || (additive != null && stack.is(additive.get()));
    }

    /**
     * 手里这件是不是「加进清汤里」的锅底料（不是直接倒的汤）：番茄 / 三鲜锅底料，以及联动登记成番茄锅底的东西。
     */
    public boolean isAdditive(ItemStack stack) {
        return additive != null && !stack.is(sourceItem.get());
    }

    // ---------------------------------------------------------------- 配置覆盖后的数值

    /** 加热速度倍率；内置汤底按「配置显式修改 > 数据包 > 代码默认」，扩展与数据包汤底直接用定义值。 */
    public float speedMul() {
        return Soups.isBuiltin(id) ? Config.overriddenSoupSpeed(key(), defaultSpeedMul) : defaultSpeedMul;
    }

    /** 一碗汤回多少饥饿点数；配置显式修改优先于数据包与代码默认。 */
    public int bowlHunger() {
        return Soups.isBuiltin(id) ? Config.overriddenSoupBowlInt(key(), "hunger", defaultBowlHunger) : defaultBowlHunger;
    }

    /** 一碗汤的饱和度系数；配置显式修改优先于数据包与代码默认。 */
    public float bowlSaturation() {
        return Soups.isBuiltin(id)
            ? Config.overriddenSoupBowlFloat(key(), "saturation", defaultBowlSaturation) : defaultBowlSaturation;
    }

    // ---------------------------------------------------------------- 风味加成

    /** 煮熟食物被吃下 / 一碗汤喝下时附加的效果（不带辣度；红汤按微辣算）。 */
    public List<MobEffectInstance> dishBonusEffects() {
        return dishBonusEffects(0);
    }

    /**
     * 汤底给菜品 / 汤碗的风味加成（原样搬自 {@code SoupBase.dishBonusEffects}，详细的风味说明见那里）。
     * 时长与等级现读配置：duration / amplifier 是主效果，second_duration / second_amplifier 是副效果，
     * fire_* 与 hunger_* 只红汤用；任何一项配成 0 或不配（默认 0）就不加。辣度超出 0..3 夹回来。
     */
    public List<MobEffectInstance> dishBonusEffects(int spicyLevel) {
        if (spicy) {
            // 红汤：辣度越高越上头——这是红汤独有的「按辣度档位加强」算法，不是单纯的属性。
            return spicyBonusEffects(spicyLevel);
        }
        List<MobEffectInstance> list = new ArrayList<>();
        boolean builtin = Soups.isBuiltin(id);
        String name = key();
        int amplifier = builtin ? Config.overriddenSoupBowlInt(name, "amplifier", 0) : 0;
        addEffect(list, mainEffect,
            builtin ? Config.overriddenSoupBowlInt(name, "duration", defaultDuration) : defaultDuration, amplifier);
        addEffect(list, secondEffect,
            builtin ? Config.overriddenSoupBowlInt(name, "second_duration", defaultSecondDuration) : defaultSecondDuration,
            builtin ? Config.overriddenSoupBowlInt(name, "second_amplifier", 0) : 0);
        return list;
    }

    /** 红汤按辣度档位加强的那一段。 */
    List<MobEffectInstance> spicyBonusEffects(int spicyLevel) {
        List<MobEffectInstance> list = new ArrayList<>();
        boolean builtin = Soups.isBuiltin(id);
        String name = key();
        int ticks = builtin ? Config.overriddenSoupBowlInt(name, "duration", defaultDuration) : defaultDuration;
        int amplifier = builtin ? Config.overriddenSoupBowlInt(name, "amplifier", 0) : 0;
        int secondTicks = builtin
            ? Config.overriddenSoupBowlInt(name, "second_duration", defaultSecondDuration) : defaultSecondDuration;
        int secondAmp = builtin ? Config.overriddenSoupBowlInt(name, "second_amplifier", 0) : 0;
        int level = Math.max(0, Math.min(HotPotBlockEntity.SPICY_MAX, spicyLevel));
        // 越辣越上头：力量与速度的时长随档位递加，中辣起力量再升一级。
        // 配成 0 的那一项保持关闭（不加辣度带来的时长），和别的汤底一样「0 = 不加」。
        // mryt233 改（第四批 4J）：辣度递增系数改读 Config.spicy*()。
        if (ticks > 0) {
            ticks += level * Config.spicyLevelDurationStep();
            amplifier += level >= Config.spicyLevelAmplifierFrom() ? 1 : 0;
        }
        addEffect(list, mainEffect, ticks, amplifier);
        int speedAdd = level == 1 ? Config.spicySecondBonusL1()
            : (level == 2 ? Config.spicySecondBonusL2()
            : (level >= HotPotBlockEntity.SPICY_MAX ? Config.spicySecondBonusL3() : 0));
        addEffect(list, secondEffect, secondTicks > 0 ? secondTicks + speedAdd : 0, secondAmp);
        if (level >= Config.spicyFireFromLevel()) {
            // 特辣 / 变态辣：发汗驱寒（抗火，变态辣加倍）+ 开胃易饿（饥饿，越辣越饿）
            int fireTicks = builtin
                ? Config.overriddenSoupBowlInt(name, "fire_duration", defaultFireDuration) : defaultFireDuration;
            addEffect(list, fireEffect,
                level >= HotPotBlockEntity.SPICY_MAX ? fireTicks * Config.spicyFireTopMultiplier() : fireTicks,
                builtin ? Config.overriddenSoupBowlInt(name, "fire_amplifier", 0) : 0);
            int hungerTicks = builtin
                ? Config.overriddenSoupBowlInt(name, "hunger_duration", defaultHungerDuration) : defaultHungerDuration;
            // 配成 0 表示「红汤不加饥饿」：这时不再按辣度往上加，玩家的配置说了算
            if (hungerTicks > 0 && level >= HotPotBlockEntity.SPICY_MAX) {
                hungerTicks += Config.spicyHungerTopBonus();
            }
            addEffect(list, hungerEffect, hungerTicks,
                builtin ? Config.overriddenSoupBowlInt(name, "hunger_amplifier", 0) : 0);
        }
        return list;
    }

    /** 按配置的时长加效果；时长 &lt;= 0 或这条效果不存在就不加。 */
    private static void addEffect(List<MobEffectInstance> list, Holder<MobEffect> effect, int ticks, int amplifier) {
        if (effect == null || ticks <= 0) {
            return;
        }
        list.add(new MobEffectInstance(effect, ticks, amplifier, false, true));
    }

    /** 4F-1（mryt233）：构造自定义汤底定义。 */
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    /** 4F-2（mryt233）：基于当前定义创建可修改副本（供数据包叠加合并用）。 */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** 4F-1（mryt233）：汤底定义构造器（带合理默认值，支持延迟物品引用）。 */
    public static final class Builder {
        private final ResourceLocation id;
        private String langKey;
        private String bowlId;
        private float defaultSpeedMul = 1.0F;
        private Supplier<Item> sourceItem;
        private Item remainderItem = Items.AIR;
        private Supplier<Item> additive;
        private int defaultBowlHunger = SoupBase.DEFAULT_BOWL_HUNGER;
        private float defaultBowlSaturation = 0.6F;
        private Holder<MobEffect> mainEffect;
        private int defaultDuration;
        private Holder<MobEffect> secondEffect;
        private int defaultSecondDuration;
        private Holder<MobEffect> fireEffect;
        private int defaultFireDuration;
        private Holder<MobEffect> hungerEffect;
        private int defaultHungerDuration;
        private boolean clearsNausea;
        private boolean spicy;
        private int screenBase = 0xFFD9C7A3;
        private int screenHi = 0xFFF1E5C9;
        private int screenLo = 0xFFB8A37D;
        private int[] screenBits = {0xFF8C7A5B, 0xFFE6D6B8};
        private int bubbleBody = 0xFFF3EAD6;
        private int bubbleHi = 0xFFFFFFFF;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.langKey = "text." + id.getNamespace() + ".soup." + id.getPath();
            this.bowlId = id.getPath() + "_bowl";
        }

        private Builder(SoupDef base) {
            this.id = base.id;
            this.langKey = base.langKey;
            this.bowlId = base.bowlId;
            this.defaultSpeedMul = base.defaultSpeedMul;
            this.sourceItem = base.sourceItem;
            this.remainderItem = base.remainderItem;
            this.additive = base.additive;
            this.defaultBowlHunger = base.defaultBowlHunger;
            this.defaultBowlSaturation = base.defaultBowlSaturation;
            this.mainEffect = base.mainEffect;
            this.defaultDuration = base.defaultDuration;
            this.secondEffect = base.secondEffect;
            this.defaultSecondDuration = base.defaultSecondDuration;
            this.fireEffect = base.fireEffect;
            this.defaultFireDuration = base.defaultFireDuration;
            this.hungerEffect = base.hungerEffect;
            this.defaultHungerDuration = base.defaultHungerDuration;
            this.clearsNausea = base.clearsNausea;
            this.spicy = base.spicy;
            this.screenBase = base.screenBase;
            this.screenHi = base.screenHi;
            this.screenLo = base.screenLo;
            this.screenBits = base.screenBits.clone();
            this.bubbleBody = base.bubbleBody;
            this.bubbleHi = base.bubbleHi;
        }

        public Builder langKey(String langKey) { this.langKey = Objects.requireNonNull(langKey, "langKey"); return this; }
        public Builder bowlId(String bowlId) { this.bowlId = Objects.requireNonNull(bowlId, "bowlId"); return this; }
        public Builder speed(float speedMul) { this.defaultSpeedMul = speedMul; return this; }
        public Builder source(Supplier<Item> sourceItem) { return source(sourceItem, Items.AIR); }
        public Builder source(Supplier<Item> sourceItem, Item remainderItem) {
            this.sourceItem = Objects.requireNonNull(sourceItem, "sourceItem");
            this.remainderItem = Objects.requireNonNull(remainderItem, "remainderItem");
            return this;
        }
        public Builder additive(Supplier<Item> additive) { this.additive = Objects.requireNonNull(additive, "additive"); return this; }
        public Builder bowlFood(int hunger, float saturation) {
            this.defaultBowlHunger = hunger;
            this.defaultBowlSaturation = saturation;
            return this;
        }
        public Builder mainEffect(Holder<MobEffect> effect, int durationTicks) {
            this.mainEffect = effect; this.defaultDuration = durationTicks; return this;
        }
        public Builder secondEffect(Holder<MobEffect> effect, int durationTicks) {
            this.secondEffect = effect; this.defaultSecondDuration = durationTicks; return this;
        }
        public Builder fireEffect(Holder<MobEffect> effect, int durationTicks) {
            this.fireEffect = effect; this.defaultFireDuration = durationTicks; return this;
        }
        public Builder hungerEffect(Holder<MobEffect> effect, int durationTicks) {
            this.hungerEffect = effect; this.defaultHungerDuration = durationTicks; return this;
        }
        public Builder clearsNausea(boolean clearsNausea) { this.clearsNausea = clearsNausea; return this; }
        public Builder spicy(boolean spicy) { this.spicy = spicy; return this; }
        public Builder screenColors(int base, int hi, int lo, int... bits) {
            this.screenBase = base; this.screenHi = hi; this.screenLo = lo;
            if (bits != null && bits.length > 0) this.screenBits = bits.clone();
            return this;
        }
        public Builder bubbleColors(int body, int hi) {
            this.bubbleBody = body; this.bubbleHi = hi; return this;
        }

        public SoupDef build() {
            if (sourceItem == null && additive == null) {
                throw new IllegalStateException("汤底 " + id + " 必须设置倒入原料 source 或锅底料 additive");
            }
            Supplier<Item> resolvedSource = sourceItem != null ? sourceItem : () -> Items.AIR;
            return new SoupDef(id, langKey, bowlId, defaultSpeedMul, resolvedSource, remainderItem, additive,
                defaultBowlHunger, defaultBowlSaturation, mainEffect, defaultDuration, secondEffect, defaultSecondDuration,
                fireEffect, defaultFireDuration, hungerEffect, defaultHungerDuration, clearsNausea, spicy,
                screenBase, screenHi, screenLo, screenBits, bubbleBody, bubbleHi);
        }
    }
}
