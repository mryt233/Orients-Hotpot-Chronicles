// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
 * 默认回 4 点饥饿值，高汤、三鲜汤饱和度高一些。风味效果见 {@link SoupBase#dishBonusEffects(int)}。
 * 喝完还一只空碗（同原版蘑菇煲）。
 * mryt233 改（2026-11-05，r136a）：六种汤碗都按 SoupBase.dishBonusEffects(辣度) 结算，
 * 红汤碗还带着盛出时的辣度档位（SPICY_LEVEL 组件），3 级特辣喝完嘴上冒火。
 * 作者：mryt233 */
public class SoupBowlItem extends Item implements TableItem {
    private final Identifier soupId;
    private final Function<Identifier, SoupDef> resolver;
    private volatile SoupDef soup;

    /** 4F-3（mryt233）：通用汤碗构造器，具体汤底由物品栈上的 {@link HotPotContent#SOUP} 组件指定。 */
    public SoupBowlItem() {
        this(new Item.Properties());
    }

    public SoupBowlItem(Item.Properties properties) {
        super(properties.stacksTo(1).food(foodOf((SoupDef) null)).usingConvertsTo(Items.BOWL));
        this.soupId = null;
        this.resolver = Soups::get;
        this.soup = null;
    }

    /** mryt233 改（2026-11-11，4B-2）：保留枚举入口给探针和联动兼容。 */
    public SoupBowlItem(SoupBase soup) {
        this(soup.def());
    }

    public SoupBowlItem(SoupBase soup, Item.Properties properties) {
        this(soup.def(), properties);
    }

    public SoupBowlItem(SoupDef soup) {
        this(soup, new Item.Properties());
    }

    public SoupBowlItem(SoupDef soup, Item.Properties properties) {
        super(properties.stacksTo(1).food(foodOf(Objects.requireNonNull(soup, "soup"))).usingConvertsTo(Items.BOWL));
        this.soupId = soup.id();
        this.resolver = Soups::get;
        this.soup = soup;
    }

    /** 4F-1（mryt233）：子模组专属汤碗构造器，按汤底 ID 登记专属碗并延迟解析定义。 */
    public SoupBowlItem(Identifier soupId) {
        this(soupId, new Item.Properties());
    }

    public SoupBowlItem(Identifier soupId, Item.Properties properties) {
        this(soupId, Soups::get, true, properties);
    }

    /** 供单测隔离登记表验证延迟解析用。 */
    SoupBowlItem(Identifier soupId, Function<Identifier, SoupDef> resolver, boolean registerGlobal) {
        this(soupId, resolver, registerGlobal, new Item.Properties());
    }

    SoupBowlItem(Identifier soupId, Function<Identifier, SoupDef> resolver, boolean registerGlobal, Item.Properties properties) {
        super(properties.stacksTo(1).food(foodOf(
            Objects.requireNonNull(resolver, "resolver").apply(Objects.requireNonNull(soupId, "soupId"))))
            .usingConvertsTo(Items.BOWL));
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
            .saturationModifier(saturation)
            .alwaysEdible()   // 饱了也能喝（同本模组其他食物，mryt233 改 2026-11-01）
            .build();
    }

    /**
     * mryt233 改（2026-11-05；4F-3 兼容通用汤碗）：喝的那一刻按配置重建营养组件（饥饿值 + 饱和度系数），
     * 所以改完 config/orients_hotpot_chronicles.json 执行 /orientshotpot reload 就能立即生效；
     * 未知汤底（如卸载数据包后残留的通用汤碗）回退安全默认值。
     */
    static FoodProperties eating(@Nullable SoupDef def) {
        int hunger = def != null ? def.bowlHunger() : SoupBase.DEFAULT_BOWL_HUNGER;
        float saturation = def != null ? def.bowlSaturation() : 0.6F;
        // 26.x 的 FoodProperties.Builder 没有 usingConvertsTo：留空碗由 Item.Properties.usingConvertsTo(Items.BOWL) 负责
        return new FoodProperties.Builder()
            .nutrition(hunger)
            .saturationModifier(saturation)
            .alwaysEdible()
            .build();
    }

    /** 汤底 ID；通用汤碗返回 null。 */
    @Nullable
    public Identifier soupId() {
        return soupId;
    }

    /** 物品栈上记录的原始汤底 ID 文本（固定汤碗返回自身 ID，通用汤碗读 {@link HotPotContent#SOUP} 组件）。 */
    @Nullable
    public String rawSoupId(@Nullable ItemStack stack) {
        return rawSoupId(soupId, stack, HotPotContent.SOUP);
    }

    @Nullable
    static String rawSoupId(@Nullable Identifier fixedSoupId, @Nullable ItemStack stack,
                            @Nullable DataComponentType<String> soupComponent) {
        if (fixedSoupId != null) {
            return fixedSoupId.toString();
        }
        if (stack == null || soupComponent == null) {
            return null;
        }
        return stack.get(soupComponent);
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
                               @Nullable Item genericBowl, @Nullable DataComponentType<String> soupComponent) {
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
        if (soupComponent != null) {
            stack.set(soupComponent, soup.id().toString());
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

    static SoupDef requireSoupDef(Identifier soupId, Function<Identifier, SoupDef> resolver) {
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
        return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide());
    }

    /**
     * mryt233 改（2026-11-07；4F-3 兼容通用汤碗）：汤底定义与辣度必须在吃之前读。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        SoupDef def = soupDef(stack);
        int spicy = HotPotContent.SPICY_LEVEL != null
            ? Math.max(0, Math.min(HotPotBlockEntity.SPICY_MAX, stack.getOrDefault(HotPotContent.SPICY_LEVEL, 0)))
            : 0;
        // mryt233 改（26.x）：营养按配置现算——临时换上 eating() 的 FOOD 组件再走原版结算，结算完换回去
        FoodProperties original = stack.get(DataComponents.FOOD);
        stack.set(DataComponents.FOOD, eating(def));
        ItemStack result;
        try {
            result = super.finishUsingItem(stack, world, user);
        } finally {
            stack.set(DataComponents.FOOD, original);
        }
        if (!world.isClientSide() && def != null) {
            if (def.clearsNausea()) {
                user.removeEffect(MobEffects.NAUSEA);
            }
            for (MobEffectInstance effect : def.dishBonusEffects(spicy)) {
                user.addEffect(effect);
            }
            int mouthFireTicks = Config.spicyMouthFireTicks();
            if (def.spicy() && mouthFireTicks > 0 && spicy >= Config.spicyMouthFireFromLevel()
                && !ClothApronItem.wornBy(user)) {
                user.setRemainingFireTicks(Math.max(user.getRemainingFireTicks(), mouthFireTicks));
            }
        }
        return result;
    }
}
