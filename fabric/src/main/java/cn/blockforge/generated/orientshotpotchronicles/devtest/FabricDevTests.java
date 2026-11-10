package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;

/** Fabric 开发命令注册，只在开发环境反射调用，不进 jar。作者：mryt233 */
public final class FabricDevTests {
    private FabricDevTests() {
    }

    public static void register() {
        // mryt233 改（2026-11-10）：空连接无 Netty channelInactive，显式结束同一 networking addon 会话。
        DevTestCommands.fakeDisconnect = player -> {
            if (player.connection instanceof net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions extension) {
                extension.getAddon().handleDisconnect();
            }
        };
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            DevTestCommands.register(dispatcher, access, FakePlayer::get));
    }
}
