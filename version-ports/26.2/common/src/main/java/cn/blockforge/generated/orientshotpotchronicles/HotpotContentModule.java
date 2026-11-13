package cn.blockforge.generated.orientshotpotchronicles;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotRecipes;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotRuntime;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import cn.blockforge.orient.framework.loader.ClientHooks;
import cn.blockforge.orient.framework.loader.ContentModule;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.ModuleDeclaration;
import cn.blockforge.orient.framework.loader.ModuleHost;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.loader.RuntimeHooks;
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
        HotPotContent.register(registration);
        if (Boolean.getBoolean("hotpot.opt4f")) {
            development("Opt4fProbe", "install", new Class<?>[]{RegistrationApi.class}, registration);
        }
        if (Boolean.getBoolean("hotpot.opt4g")) {
            development("Opt4gProbe", "install", new Class<?>[]{RegistrationApi.class}, registration);
        }
    }

    @Override
    public void runtime(RuntimeHooks hooks) {
        runtimeCalls++;
        // mryt233 改（2026-11-10）：公共业务实际接入框架生命周期。
        HotPotRuntime.install(hooks);
    }

    @Override
    public void client(ClientHooks hooks) {
        clientCalls++;
        // mryt233 改（2026-11-10）：客户端类独立在 client 源集，服务端不链接客户端类型。
        hooks.onClientSetup(() -> {
            try {
                Class.forName("cn.blockforge.generated.orientshotpotchronicles.client.HotPotClientContent")
                    .getMethod("install").invoke(null);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("hotpot client initialization failed", error);
            }
        });
    }

    /** mryt233 改（2026-11-10）：数据包重载使已下锅的食材时长失效。 */
    @Override
    public void onReload() {
        HotPotCooking.reviseCookTimes();
        HotPotRecipes.clearCache();
        Soups.clearCache();
        Seasonings.clearCache();
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

    /** mryt233 改（2026-11-10）：开发探针统一按需反射，分发包缺类时安静跳过。 */
    public static void development(String name, String method, Class<?>[] types, Object... arguments) {
        if (!Framework.platform().isDevelopmentEnvironment()) return;
        try {
            Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest." + name)
                .getMethod(method, types).invoke(null, arguments);
        } catch (ClassNotFoundException ignored) {
            HotPot.LOGGER.info("dev test commands not packaged in this jar");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("development probe failed: " + name, error);
        }
    }
}
