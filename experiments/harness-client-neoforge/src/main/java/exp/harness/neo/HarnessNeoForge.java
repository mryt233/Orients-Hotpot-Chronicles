package exp.harness.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge 生产候选包的端到端测试助爪；只在客户端安装，不进发布产物。作者：mryt233。
 * 客户端专用代码隔离在 {@link ClientTickHandler}，专用服不会加载。
 */
@Mod("hotpot_harness_neo")
public final class HarnessNeoForge {
    public HarnessNeoForge(IEventBus modBus, ModContainer container) {
        if (FMLEnvironment.dist.isClient()) {
            ClientTickHandler.install();
        }
    }
}
