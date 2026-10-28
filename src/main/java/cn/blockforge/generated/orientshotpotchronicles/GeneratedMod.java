package cn.blockforge.generated.orientshotpotchronicles;

import cn.blockforge.generated.orientshotpotchronicles.compat.ModCompat;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orient's Hotpot Chronicles —— Fabric 1.21.1 可交互火锅模组入口。
 *
 * 核心玩法：砌一座灶台，往灶膛里添柴生火，把铜锅/铁锅坐上灶，
 * 倒汤底后用筷子打开 9x9 的九宫格面板下菜，像熔炉一样等它煮熟，
 * 直接取走带经验收益的熟食；汤底效果接口为后续扩展预留——全程只有气泡与蒸汽在变。
 */
public final class GeneratedMod implements ModInitializer {
    public static final String MOD_ID = "orients_hotpot_chronicles";
    public static final Logger LOGGER = LoggerFactory.getLogger("OrientsHotpotChronicles");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        HotPotContent.register();
        ModCompat.init();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // 开发环境才有的测试命令 /hpt（devtest 包不进 jar，所以用反射调； 2026-10-25）
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.DevTestCommands")
                    .getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("dev test commands unavailable", e);
            }
        }
    }
}
