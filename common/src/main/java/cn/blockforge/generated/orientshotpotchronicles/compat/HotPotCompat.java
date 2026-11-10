package cn.blockforge.generated.orientshotpotchronicles.compat;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHeat;
import java.util.Objects;
import java.util.function.Predicate;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 联动兼容层：把第三方汤底、燃料和热源接入核心登记表。
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


    private HotPotCompat() {
    }

    /** 注册第三方可倒出的汤底来源（映射到现有汤底体系）。 */
    public static void registerBaseSource(Item item, SoupBase base) {
        SoupBase.registerSource(item, base); // 调整：转发核心实际查询的注册表。
    }

    /** 注册第三方燃料（映射到 木材/煤炭/岩浆 三档之一）。 */
    public static void registerFuel(Item item, HotPotFuel fuel) {
        HotPotFuel.registerCustomSource(item, fuel); // 调整：转发核心实际查询的注册表。
    }

    /** 注册第三方热源：返回 true 时，摆在这个方块上的锅算热锅（例如农夫乐事的炉灶）。 */
    public static void registerHeatSource(HotPotHeat.HeatSource source) {
        HotPotHeat.register(source);
    }

    // ------- 查询接口（由火锅/灶台逻辑调用） -------

    /** @deprecated mryt233 改（2026-11-10）：请用 SoupBase.customSource，保留旧二进制入口。 */
    @Deprecated
    public static SoupBase customBase(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return SoupBase.customSource(stack);
    }

    /** @deprecated mryt233 改（2026-11-10）：请用 HotPotFuel.customSource，保留旧二进制入口。 */
    @Deprecated
    public static HotPotFuel customFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return HotPotFuel.customSource(stack);
    }

    // 调整：由平台注册加载器检测，兼容模块本身保持加载器无关。
    private static Predicate<String> modLoaded = id -> false;

    public static void install(Predicate<String> detector) {
        modLoaded = Objects.requireNonNull(detector);
    }

    public static boolean isModLoaded(String modId) {
        return modLoaded.test(modId);
    }

    public static Item itemOf(String namespace, String path) {
        // mryt233 改（2026-11-11）：先查 ID 是否存在，不能用默认物品判断注册状态。
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    static void log(String message) {
        LOGGER.info("[Orient的火锅物语/兼容] {}", message);
    }

    /** 供 ModCompat 使用的日志出口。 */
    public static void info(String message) {
        log(message);
    }
}
