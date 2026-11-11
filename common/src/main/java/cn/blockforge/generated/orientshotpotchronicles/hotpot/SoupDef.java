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
     * 配置键 / 菜上 {@code cooked_in_soup} 记的名字 / 汤面贴图名：火锅自己的汤底是短名（clear、spicy），
     * 别的命名空间的汤底是完整 ID。内置项等于旧的 {@code SoupBase.textureKey()}。
     */
    public String key() {
        return HotPot.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    /** 完整语言键 text.&lt;modid&gt;.soup.xxx。 */
    public String nameKey() {
        return "text." + HotPot.MOD_ID + "." + langKey;
    }

    /**
     * 方块状态里的汤面值。内置项按名字对应 {@link SoupVisual}（启动时核对过一一对得上）；
     * 其他汤底在 4F-3 有 {@code OTHER} 之前暂时映射为 {@link SoupVisual#NONE}（锅里不画汤面，不崩）。
     */
    public SoupVisual visual() {
        for (SoupVisual candidate : SoupVisual.values()) {
            if (candidate != SoupVisual.NONE && candidate.getSerializedName().equals(key())) {
                return candidate;
            }
        }
        return SoupVisual.NONE;
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

    /** 加热速度倍率；配置 soup_speed.&lt;名字&gt; 优先，没写就用默认值。 */
    public float speedMul() {
        return Config.soupSpeed(key(), defaultSpeedMul);
    }

    /** 一碗汤回多少饥饿点数；配置 soup_bowl.&lt;汤底&gt;.hunger 优先。 */
    public int bowlHunger() {
        return Config.soupBowlInt(key(), "hunger", defaultBowlHunger);
    }

    /** 一碗汤的饱和度系数；配置 soup_bowl.&lt;汤底&gt;.saturation 优先。 */
    public float bowlSaturation() {
        return Config.soupBowlFloat(key(), "saturation", defaultBowlSaturation);
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
        String name = key();
        int amplifier = Config.soupBowlInt(name, "amplifier", 0);
        addEffect(list, mainEffect, Config.soupBowlInt(name, "duration", defaultDuration), amplifier);
        addEffect(list, secondEffect, Config.soupBowlInt(name, "second_duration", defaultSecondDuration),
            Config.soupBowlInt(name, "second_amplifier", 0));
        return list;
    }

    /** 红汤按辣度档位加强的那一段。 */
    List<MobEffectInstance> spicyBonusEffects(int spicyLevel) {
        List<MobEffectInstance> list = new ArrayList<>();
        String name = key();
        int ticks = Config.soupBowlInt(name, "duration", defaultDuration);
        int amplifier = Config.soupBowlInt(name, "amplifier", 0);
        int secondTicks = Config.soupBowlInt(name, "second_duration", defaultSecondDuration);
        int secondAmp = Config.soupBowlInt(name, "second_amplifier", 0);
        int level = Math.max(0, Math.min(3, spicyLevel));
        // 越辣越上头：力量与速度的时长随档位递加，中辣起力量再升一级。
        // 配成 0 的那一项保持关闭（不加辣度带来的时长），和别的汤底一样「0 = 不加」。
        if (ticks > 0) {
            ticks += level * 60;
            amplifier += level >= 1 ? 1 : 0;
        }
        addEffect(list, mainEffect, ticks, amplifier);
        int speedAdd = level == 1 ? 60 : (level == 2 ? 80 : (level >= 3 ? 120 : 0));
        addEffect(list, secondEffect, secondTicks > 0 ? secondTicks + speedAdd : 0, secondAmp);
        if (level >= 2) {
            // 特辣 / 变态辣：发汗驱寒（抗火，变态辣加倍）+ 开胃易饿（饥饿，越辣越饿）
            int fireTicks = Config.soupBowlInt(name, "fire_duration", defaultFireDuration);
            addEffect(list, fireEffect, level >= 3 ? fireTicks * 2 : fireTicks,
                Config.soupBowlInt(name, "fire_amplifier", 0));
            int hungerTicks = Config.soupBowlInt(name, "hunger_duration", defaultHungerDuration);
            // 配成 0 表示「红汤不加饥饿」：这时不再按辣度往上加，玩家的配置说了算
            if (hungerTicks > 0 && level >= 3) {
                hungerTicks += 40;
            }
            addEffect(list, hungerEffect, hungerTicks, Config.soupBowlInt(name, "hunger_amplifier", 0));
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
}
