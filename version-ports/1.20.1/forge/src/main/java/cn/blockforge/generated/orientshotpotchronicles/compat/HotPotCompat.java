package cn.blockforge.generated.orientshotpotchronicles.compat;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 联动兼容层（预留接口）。
 *
 * <p>核心模组不硬依赖任何其他模组。独立兼容模块（或本模组在检测到
 * 联动对象时）通过这里的静态注册接口，把第三方物品接入火锅与灶台：
 * 汤底来源、燃料档位。</p>
 *
 * <p>食材侧无需注册：任何"可食用"或有熔炉熟食配方的物品都能下锅，
 * 第三方模组的生肉/熟食天然就被支持。</p>
 */
public final class HotPotCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("HotPotCompat");

    private static final Map<Item, SoupBase> BASE_SOURCES = new HashMap<>();
    private static final Map<Item, HotPotFuel> FUEL_SOURCES = new HashMap<>();

    private HotPotCompat() {
    }

    /** 注册第三方可倒出的汤底来源（映射到现有汤底体系）。 */
    public static void registerBaseSource(Item item, SoupBase base) {
        BASE_SOURCES.put(item, base);
    }

    /** 注册第三方燃料（映射到 木材/煤炭/岩浆 三档之一）。 */
    public static void registerFuel(Item item, HotPotFuel fuel) {
        FUEL_SOURCES.put(item, fuel);
    }

    /** 注册第三方热源：返回 true 时，摆在这个方块上的锅算热锅（例如农夫乐事的炉灶）。 */
    public static void registerHeatSource(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHeat.HeatSource source) {
        cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHeat.register(source);
    }

    // ------- 查询接口（由火锅/灶台逻辑调用） -------

    public static SoupBase customBase(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return BASE_SOURCES.get(stack.getItem());
    }

    public static HotPotFuel customFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return FUEL_SOURCES.get(stack.getItem());
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Item itemOf(String namespace, String path) {
        Item item = Registries.ITEM.get(new Identifier(namespace, path));
        return item == net.minecraft.item.Items.AIR ? null : item;
    }

    static void log(String message) {
        LOGGER.info("[Orient的火锅物语/兼容] {}", message);
    }

    /** 供 ModCompat 使用的日志出口。 */
    public static void info(String message) {
        log(message);
    }
}
