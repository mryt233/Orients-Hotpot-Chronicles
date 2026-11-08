package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料档位：只决定灶火强度（影响烹饪速度），燃烧时长则完全沿用
 * 原版熔炉的燃料数据（{@link AbstractFurnaceBlockEntity#getFuel()}；1.21.1 里这张表由原版自己
 * 缓存并在数据包重载时由 {@link AbstractFurnaceBlockEntity#invalidateCache()} 重建）。
 * 木材 &lt; 煤炭/木炭 &lt; 岩浆桶。
 *
 * <p>同一个档位里所有燃料的燃烧时长各不相同，比如木板 300 tick、原木 300 tick、
 * 煤炭 1600 tick、岩浆桶 20000 tick，全部直接取原版数值。</p>
 */
public enum HotPotFuel {
    WOOD("fuel.wood", 0.65F, 300),
    COAL("fuel.coal", 1.00F, 1600),
    LAVA("fuel.lava", 1.60F, 20000);

    private static final HotPotFuel[] BY_ID = values();

    /** 第三方燃料映射到哪一档（兼容层注册进来）。 */
    private static final java.util.Map<Item, HotPotFuel> CUSTOM_SOURCES = new java.util.HashMap<>();

    /** 注册「某种第三方燃料算哪一档」。 */
    public static void registerCustomSource(Item item, HotPotFuel fuel) {
        CUSTOM_SOURCES.put(item, fuel);
    }

    /** 这件物品作为第三方燃料算哪一档；不是注册过的返回 null。 */
    public static HotPotFuel customSource(ItemStack stack) {
        return stack.isEmpty() ? null : CUSTOM_SOURCES.get(stack.getItem());
    }

    private final String nameKey;
    private final float power;
    private final int defaultTicks;

    HotPotFuel(String nameKey, float power, int defaultTicks) {
        this.nameKey = nameKey;
        this.power = power;
        this.defaultTicks = defaultTicks;
        // 默认值记进配置模板，配置文件里同名项可以覆盖
        Config.rememberFuel(configKey(), power, defaultTicks);
    }

    /** 配置文件里用的键，和枚举名一致的小写形式。 */
    public String configKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String nameKey() {
        return "text." + cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot.MOD_ID + "." + nameKey;
    }

    public float power() {
        return Config.fuelPower(configKey(), power);
    }

    /** 联动模组注册燃料时使用的默认燃烧时长（tick）。 */
    public int defaultTicks() {
        return Config.fuelTicks(configKey(), defaultTicks);
    }

    public static HotPotFuel byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }

    /**
     * 该物品在原版熔炉里的燃烧时长（tick）；不能当燃料时返回 0。
     * <p>mryt233 改（2026-11-07）：不再自己缓存一份燃料表。原版 AbstractFurnaceBlockEntity.getFuel()
     * 内部已经是 volatile 缓存并在数据包重载时由 invalidateCache() 重建；
     * 这里再存一层 Map 引用会永远拿第一次的旧表，导致 reload 后新增 / 移除的燃料不生效。</p>
     */
    public static int burnTicksOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        HotPotFuel custom = customSource(stack);
        if (custom != null) {
            return custom.defaultTicks;
        }
        Integer ticks = AbstractFurnaceBlockEntity.getFuel().get(stack.getItem());
        return ticks == null ? 0 : ticks;
    }

    /** 物品能否塞进灶膛当燃料。 */
    public static boolean isFuel(ItemStack stack) {
        return burnTicksOf(stack) > 0;
    }

    /** 按原版燃烧时长划分灶火档位，供烹饪速度与状态文字使用。 */
    public static HotPotFuel gradeForTicks(int ticks) {
        if (ticks >= 8000) {
            return LAVA;
        }
        if (ticks >= 1000) {
            return COAL;
        }
        return WOOD;
    }
}
