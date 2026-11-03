package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** NeoForge 开发命令注册，只在开发环境反射调用，不进 jar。作者：mryt233 */
public final class NeoForgeDevTests {
    private NeoForgeDevTests() {
    }

    public static void register(RegisterCommandsEvent event) {
        DevTestCommands.connectionFactory = NeoForgeFakeConnection::new;
        DevTestCommands.register(event.getDispatcher(), event.getBuildContext(), FakePlayerFactory::get);
    }
}
