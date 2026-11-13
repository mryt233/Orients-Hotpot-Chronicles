package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核心层自己的模组标识。
 * 作者：mryt233（mryt233 改：1.20.1 适配 ResourceLocation 构造）。
 */
public final class HotPot {
    public static final String MOD_ID = "orients_hotpot_chronicles";
    public static final Logger LOGGER = LoggerFactory.getLogger("OrientsHotpotChronicles");

    private HotPot() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
