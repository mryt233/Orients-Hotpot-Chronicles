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
 * 联动兼容层：把第三方汤底、燃料和热源接入核心登记表（mryt233 改：1.20.1 适配 ResourceLocation）。
 */
public final class HotPotCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("HotPotCompat");

    private HotPotCompat() {
    }

    /** 注册第三方可倒出的汤底来源（映射到现有汤底体系）。 */
    public static void registerBaseSource(Item item, SoupBase base) {
        SoupBase.registerSource(item, base);
    }

    /** 注册第三方燃料（映射到 木材/煤炭/岩浆 三档之一）。 */
    public static void registerFuel(Item item, HotPotFuel fuel) {
        HotPotFuel.registerCustomSource(item, fuel);
    }

    /** 注册第三方热源：返回 true 时，摆在这个方块上的锅算热锅（例如农夫乐事的炉灶）。 */
    public static void registerHeatSource(HotPotHeat.HeatSource source) {
        HotPotHeat.register(source);
    }

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

    private static Predicate<String> modLoaded = id -> false;

    public static void install(Predicate<String> detector) {
        modLoaded = Objects.requireNonNull(detector);
    }

    public static boolean isModLoaded(String modId) {
        return modLoaded.test(modId);
    }

    public static Item itemOf(String namespace, String path) {
        ResourceLocation id = new ResourceLocation(namespace, path);
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
