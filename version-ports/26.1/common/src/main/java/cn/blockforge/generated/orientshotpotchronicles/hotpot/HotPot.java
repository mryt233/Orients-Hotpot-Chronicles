// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核心层自己的模组标识。
 * <p>
 * 核心代码不能依赖平台入口 {@code GeneratedMod}，否则 common 子工程编译不过。
 * 这里只放 ID、日志器和一个 id() 工具；平台入口继续转发到这里，两边保持同一份身份。
 * 作者：mryt233 */
public final class HotPot {
    public static final String MOD_ID = "orients_hotpot_chronicles";
    public static final Logger LOGGER = LoggerFactory.getLogger("OrientsHotpotChronicles");

    private HotPot() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
