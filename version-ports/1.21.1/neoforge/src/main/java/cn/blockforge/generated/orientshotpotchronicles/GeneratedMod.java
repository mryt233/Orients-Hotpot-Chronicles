package cn.blockforge.generated.orientshotpotchronicles;

import cn.blockforge.generated.orientshotpotchronicles.compat.ModCompat;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
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
// 1.21.1 NeoForge 移植：调整。
@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "orients_hotpot_chronicles";
    public static final Logger LOGGER = LoggerFactory.getLogger("OrientsHotpotChronicles");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public GeneratedMod(IEventBus modBus) {
        // 首次触碰内容类必须在注册事件内，避免创建冻结注册表的 intrusive holder。
        modBus.addListener((RegisterEvent event) -> HotPotContent.registerEntries(event));
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            HotPotContent.register();
            ModCompat.init();
        }));
        modBus.addListener(cn.blockforge.generated.orientshotpotchronicles.hotpot.OpenSauceRestockPayload::register);
        NeoForge.EVENT_BUS.addListener(HotPotContent::beforeBreak);
    }
}
