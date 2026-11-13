package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.orient.framework.compat.ItemData;
import cn.blockforge.orient.framework.compat.ItemDataKey;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 一碗汤（r99，用户要求）：拿碗点有汤的锅盛出来（锅里的汤不会少），能喝、能像盘子一样摆在桌上。
 * 营养和效果都能在配置文件 soup_bowl.&lt;汤底&gt; 里改（mryt233 改 2026-11-05）：
 * 默认回 4 点饥饿值，高汤、三鲜汤饱和度高一些。风味效果见 {@link SoupBase#dishBonusEffects(int)}
 * （mryt233 改 2026-11-06，清单 #125：清汤速度 + 饱和、高汤抗性 + 伤害吸收且不再缓慢、菌汤生命恢复、
 * 红汤力量 + 速度且特辣起加抗火与饥饿、番茄汤急迫、三鲜汤水下呼吸 + 幸运）；清汤、番茄汤喝下还解反胃。
 * 喝完还一只空碗（同原版蘑菇煲）。
 * mryt233 改（2026-11-05，清单 #116）：六种汤碗都按 SoupBase.dishBonusEffects(辣度) 结算，
 * 红汤碗还带着盛出时的辣度档位，3 级特辣喝完嘴上冒火（2 秒无伤火苗）。
 * <p>mryt233 改（0.2.2-alpha，1.20.1 适配）：
 * ① 饱和度用 {@code saturationMod(float)}、饱食可吃用 {@code alwaysEat()}；
 * ② 辣度与汤底标记走物品 NBT（ItemData / ItemDataKey），不是 1.21 的 DataComponentType；
 * ③ 1.20.1 没有 {@code LivingEntity#eat(Level, ItemStack, FoodProperties)} 这个三参重载，
 * 固定汤碗通过覆写 {@link #getFoodProperties()} 现读配置——和 {@link ConfiguredFoodItem} 同一套做法，
 * 改完配置 /orientshotpot reload 立即生效；
 * ④ 喝完留下的空碗仍由 {@link #finishUsingItem} 自己换（1.20.1 没有 {@code usingConvertsTo}）。
 * 作者：mryt233
 */
public class SoupBowlItem extends Item implements TableItem {
    private final ResourceLocation soupId;
    private final Function<ResourceLocation, SoupDef> resolver;
    private volatile SoupDef soup;

    /** 4F-3（mryt233）：通用汤碗构造器，具体汤底由物品栈上的 {@link HotPotContent#SOUP} NBT 键指定。 */
    public SoupBowlItem() {
        super(new Item.Properties().stacksTo(1).food(foodOf((SoupDef) null)));
        this.soupId = null;
        this.resolver = Soups::get;
        this.soup = null;
    }

    /** mryt233 改（2026-11-11，4B-2）：保留枚举入口给探针和联动兼容。 */
    public SoupBowlItem(SoupBase soup) {
        this(soup.def());
    }

    public SoupBowlItem(SoupDef soup) {
        super(new Item.Properties().stacksTo(1).food(foodOf(Objects.requireNonNull(soup, "soup"))));
        this.soupId = soup.id();
        this.resolver = Soups::get;
        this.soup = soup;
    }

    /** 4F-1（mryt233）：子模组专属汤碗构造器，按汤底 ID 登记专属碗并延迟解析定义。 */
    public SoupBowlItem(ResourceLocation soupId) {
        this(soupId, Soups::get, true);
    }

    /** 供单测隔离登记表验证延迟解析用。 */
    SoupBowlItem(ResourceLocation soupId, Function<ResourceLocation, SoupDef> resolver, boolean registerGlobal) {
        super(new Item.Properties().stacksTo(1).food(foodOf(
            Objects.requireNonNull(resolver, "resolver").apply(Objects.requireNonNull(soupId, "soupId")))));
        this.soupId = soupId;
        this.resolver = resolver;
        this.soup = resolver.apply(soupId);
        if (registerGlobal) {
            Soups.registerBowl(soupId, this);
        }
    }

    /** 注册时的数值只作默认值和客户端显示用；真正的结算在 {@link #eating}（照 ConfiguredFoodItem 的思路）。 */
    private static FoodProperties foodOf(SoupDef soup) {
        float saturation = soup != null ? soup.defaultBowlSaturation() : 0.6F;
        return new FoodProperties.Builder()
            .nutrition(SoupBase.DEFAULT_BOWL_HUNGER)
            // mryt233 改（2026-11-10，r147）：饱和度系数改读汤底定义上的属性（以前是按具体汤底写的判断）
            .saturationMod(saturation)
            .alwaysEat()   // 饱了也能喝（同本模组其他食物，mryt233 改 2026-11-01）
            .build();
    }

    /**
     * mryt233 改（2026-11-05；4F-3 兼容通用汤碗）：喝的那一刻按配置重建营养配置（饥饿值 + 饱和度系数），
     * 所以改完 config/orients_hotpot_chronicles.json 执行 /orientshotpot reload 就能立即生效；
     * 未知汤底（如卸载数据包后残留的通用汤碗）回退安全默认值。
     */
    static FoodProperties eating(@Nullable SoupDef def) {
        int hunger = def != null ? def.bowlHunger() : SoupBase.DEFAULT_BOWL_HUNGER;
        float saturation = def != null ? def.bowlSaturation() : 0.6F;
        return new FoodProperties.Builder()
            .nutrition(hunger)
            .saturationMod(saturation)
            .alwaysEat()
            .build();
    }

    @Override
    public FoodProperties getFoodProperties() {
        return soupId != null ? eating(soupDef()) : super.getFoodProperties();
    }

    /** 汤底 ID；通用汤碗返回 null。 */
    @Nullable
    public ResourceLocation soupId() {
        return soupId;
    }

    /** 物品栈上记录的原始汤底 ID 文本（固定汤碗返回自身 ID，通用汤碗读 {@link HotPotContent#SOUP} 键）。 */
    @Nullable
    public String rawSoupId(@Nullable ItemStack stack) {
        return rawSoupId(soupId, stack, HotPotContent.SOUP);
    }

    @Nullable
    static String rawSoupId(@Nullable ResourceLocation fixedSoupId, @Nullable ItemStack stack,
                            @Nullable ItemDataKey<String> soupKey) {
        if (fixedSoupId != null) {
            return fixedSoupId.toString();
        }
        if (stack == null || soupKey == null) {
            return null;
        }
        return ItemData.get(stack, soupKey);
    }

    /** 按物品栈解析汤底定义：固定汤碗走延迟解析；通用汤碗按 {@link HotPotContent#SOUP} 查询，解析不到返回 null。 */
    @Nullable
    public SoupDef soupDef(@Nullable ItemStack stack) {
        if (soupId != null) {
            return soupDef();
        }
        return resolveGenericSoup(rawSoupId(stack), Soups::get);
    }

    @Nullable
    static SoupDef resolveGenericSoup(@Nullable String raw, Function<String, SoupDef> lookup) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return lookup.apply(raw);
    }

    /** 按汤底构造可盛出的汤碗物品栈：优先专属碗，否则使用通用汤碗并写入 {@link HotPotContent#SOUP}。 */
    public static ItemStack createFor(@Nullable SoupDef soup) {
        return createFor(soup, Soups.bowlItem(soup), HotPotContent.SOUP_BOWL, HotPotContent.SOUP);
    }

    static ItemStack createFor(@Nullable SoupDef soup, @Nullable Item dedicatedBowl,
                               @Nullable Item genericBowl, @Nullable ItemDataKey<String> soupKey) {
        if (soup == null) {
            return ItemStack.EMPTY;
        }
        if (dedicatedBowl != null && dedicatedBowl != genericBowl) {
            return new ItemStack(dedicatedBowl);
        }
        if (genericBowl == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(genericBowl);
        if (soupKey != null) {
            ItemData.set(stack, soupKey, soup.id().toString());
        }
        return stack;
    }

    /** 这碗汤对应的定义（随当前世界重载动态更新；通用汤碗请调用 {@link #soupDef(ItemStack)}）。 */
    @Nullable
    public SoupDef soupDef() {
        if (soupId == null) {
            return null;
        }
        SoupDef resolved = resolver.apply(soupId);
        if (resolved != null) {
            soup = resolved;
            return resolved;
        }
        SoupDef cached = soup;
        if (cached != null) {
            return cached;
        }
        return requireSoupDef(soupId, resolver);
    }

    static SoupDef requireSoupDef(ResourceLocation soupId, Function<ResourceLocation, SoupDef> resolver) {
        SoupDef def = Objects.requireNonNull(resolver, "resolver").apply(Objects.requireNonNull(soupId, "soupId"));
        if (def == null) {
            throw new IllegalStateException("专属汤碗对应的汤底未登记：" + soupId);
        }
        return def;
    }

    /** 对应的内置枚举值；非内置汤底返回 null。保留给探针和联动兼容，游戏逻辑请用 {@link #soupDef()}。 */
    @Nullable
    public SoupBase soup() {
        SoupDef def = soupDef();
        return def == null ? null : SoupBase.byTextureKey(def.key());
    }

    @Override
    public Component getName(ItemStack stack) {
        if (soupId != null) {
            return super.getName(stack);
        }
        SoupDef def = soupDef(stack);
        String raw = def == null ? rawSoupId(stack) : null;
        return bowlName(def, raw, super.getName(stack));
    }

    static Component bowlName(@Nullable SoupDef def, @Nullable String rawSoupId, Component fallback) {
        if (def != null) {
            return Component.translatable(
                "item.orients_hotpot_chronicles.soup_bowl.named",
                Component.translatable(def.nameKey()));
        }
        if (rawSoupId != null && !rawSoupId.isEmpty()) {
            return Component.translatable(
                "item.orients_hotpot_chronicles.soup_bowl.unknown",
                rawSoupId);
        }
        return fallback;
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return HotPotContent.BOWL_BLOCK;
    }

    /** 点在方块顶面：像盘子一样摆上去；别的情况交给原版（对着空气 / 侧面右键就是喝）。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide);
    }

    /**
     * mryt233 改（2026-11-07；4F-3 兼容通用汤碗）：汤底定义与辣度必须在吃之前读。
     * <p>finishUsingItem / user.eat 内部会把这碗吃到 0 个；空栈读取 SPICY_LEVEL 或 SOUP 都会失效。
     * 未知汤底（def == null）按安全默认值吃掉并返还空碗，不施加未知效果。</p>
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        // mryt233 改（2026-11-07）：先把汤底定义与辣度取成快照，再交给原版结算
        SoupDef def = soupDef(stack);
        int spicy = HotPotContent.SPICY_LEVEL != null
            ? Math.max(0, Math.min(HotPotBlockEntity.SPICY_MAX, ItemData.getOrDefault(stack, HotPotContent.SPICY_LEVEL, 0)))
            : 0;
        // mryt233 改（1.20.1 适配）：营养走 getFoodProperties() 现读配置，这里交给原版结算
        ItemStack result = super.finishUsingItem(stack, world, user);
        if (!world.isClientSide && def != null) {
            // mryt233 改（2026-11-10，r147）：「喝下解反胃」改成汤底自己的属性（清汤、番茄汤为 true）
            if (def.clearsNausea()) {
                user.removeEffect(MobEffects.CONFUSION);
            }
            // mryt233 改（2026-11-05，r136a）：汤碗统一按 SoupDef.dishBonusEffects(辣度) 结算，
            // 红汤碗的辣度是盛出来时装在 SPICY_LEVEL 里的（0 档 = 微辣，和以前一样）。
            for (MobEffectInstance effect : def.dishBonusEffects(spicy)) {
                user.addEffect(effect);
            }
            // 3 级特辣：嘴上冒火苗，纯节目效果（前面刚加的抗火罩着，不会扣血）
            // mryt233 改（2026-11-06，r139）：穿着布围裙时不再点火苗（防溅油装备挡滚油与明火）。
            // mryt233 改（2026-11-10）：红汤判断改成 spicy() 属性；mryt233 改（第四批 4J）：改读 Config.spicyMouthFire*
            int mouthFireTicks = Config.spicyMouthFireTicks();
            if (def.spicy() && mouthFireTicks > 0 && spicy >= Config.spicyMouthFireFromLevel()
                && !ClothApronItem.wornBy(user)) {
                user.setRemainingFireTicks(Math.max(user.getRemainingFireTicks(), mouthFireTicks));
            }
        }
        return user instanceof Player player && player.getAbilities().instabuild
            ? result : new ItemStack(Items.BOWL);
    }
}
