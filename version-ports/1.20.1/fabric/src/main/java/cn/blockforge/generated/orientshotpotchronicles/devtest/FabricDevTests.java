package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;

/** Fabric 开发命令注册，只在开发环境反射调用，不进 jar。作者：mryt233 */
public final class FabricDevTests {
    private FabricDevTests() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            DevTestCommands.register(dispatcher, access, FakePlayer::get));
    }
}
