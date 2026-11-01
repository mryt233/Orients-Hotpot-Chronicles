package cn.blockforge.generated.orientshotpotchronicles;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.orient.framework.loader.ContentModule;
import cn.blockforge.orient.framework.loader.ModuleDeclaration;
import cn.blockforge.orient.framework.loader.ModuleHost;
import cn.blockforge.orient.framework.loader.RegistrationApi;

import java.util.List;
import java.util.Set;

/** 火锅内容模块：声明 + 注册入口；玩法逻辑仍在 hotpot 包。作者：mryt233 */
public final class HotpotContentModule implements ContentModule {
    public static final HotpotContentModule MODULE = new HotpotContentModule();
    public static final ModuleHost HOST = new ModuleHost(List.of(MODULE));
    private static int declarationCalls;
    private static int clientCalls;
    private static int runtimeCalls;

    private HotpotContentModule() {
    }

    @Override
    public ModuleDeclaration declaration() {
        declarationCalls++;
        return new ModuleDeclaration(HotPot.MOD_ID, HotPot.MOD_ID, Set.of(),
            List.of(HotPot.MOD_ID + ".json"));
    }

    @Override
    public void register(RegistrationApi registration) {
        HotPotContent.register();
    }

    @Override
    public void runtime(cn.blockforge.orient.framework.loader.RuntimeHooks hooks) {
        runtimeCalls++;
    }

    @Override
    public void client(cn.blockforge.orient.framework.loader.ClientHooks hooks) {
        clientCalls++;
    }

    public static int declarationCalls() {
        return declarationCalls;
    }

    public static int clientCalls() {
        return clientCalls;
    }

    public static int runtimeCalls() {
        return runtimeCalls;
    }
}