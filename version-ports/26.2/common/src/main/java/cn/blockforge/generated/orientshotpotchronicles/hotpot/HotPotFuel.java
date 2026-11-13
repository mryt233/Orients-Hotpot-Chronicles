// mryt233 改（2026-11-11）：第三方燃料改为 FrozenRegistry，适配 26.2 fuelValues。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Locale;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 燃料档位：只决定灶火强度（影响烹饪速度），燃烧时长直接取当前世界的原版燃料表。
 * 木材 < 煤炭/木炭 < 岩浆桶。
 */
public enum HotPotFuel {
    WOOD("fuel.wood", 0.65F, 300),
    COAL("fuel.coal", 1.00F, 1600),
    LAVA("fuel.lava", 1.60F, 20000);

    private static final HotPotFuel[] BY_ID = values();

    private static final FrozenRegistry<Item, HotPotFuel> CUSTOM_SOURCES = new FrozenRegistry<>(
        "第三方燃料表", "燃料档要在启动期登记（子模组在自己的兼容模块里调 registerCustomSource），冻结后不再接受登记。");

    /** 注册「某种第三方燃料算哪一档」。 */
    public static void registerCustomSource(Item item, HotPotFuel fuel) {
        CUSTOM_SOURCES.put(item, fuel);
    }

    /** 这件物品作为第三方燃料算哪一档；不是注册过的返回 null。 */
    public static HotPotFuel customSource(ItemStack stack) {
        return stack.isEmpty() ? null : CUSTOM_SOURCES.get(stack.getItem());
    }

    /** 冻结第三方燃料表（服务端启动时由 HotPotHooks 统一调，幂等）。 */
    static void freezeSources() {
        CUSTOM_SOURCES.freeze();
    }

    /** 诊断：第三方燃料表冻了没有。 */
    public static boolean sourcesFrozen() {
        return CUSTOM_SOURCES.isFrozen();
    }

    private final String nameKey;
    private final float power;
    private final int defaultTicks;

    HotPotFuel(String nameKey, float power, int defaultTicks) {
        this.nameKey = nameKey;
        this.power = power;
        this.defaultTicks = defaultTicks;
        Config.rememberFuel(configKey(), power, defaultTicks);
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "text." + HotPot.MOD_ID + "." + nameKey;
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

    /** 该物品在原版熔炉里的燃烧时长（tick）；不能当燃料时返回 0。 */
    public static int burnTicksOf(Level world, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        HotPotFuel custom = customSource(stack);
        if (custom != null) {
            return custom.defaultTicks;
        }
        return world == null ? 0 : world.fuelValues().burnDuration(stack);
    }

    /** 物品能否塞进灶膛当燃料。 */
    public static boolean isFuel(Level world, ItemStack stack) {
        return burnTicksOf(world, stack) > 0;
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
