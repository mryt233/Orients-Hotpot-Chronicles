package cn.blockforge.orient.framework.game;

import cn.blockforge.orient.framework.pure.ConfigStore;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * 配置门面：服务端/客户端快照隔离与线程判定；文件格式与校验在纯内核 ConfigStore。作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 F3）：以前是「不在服务端主线程就读客户端那份」，单人游戏里
 * 服务端的区块加载、世界生成等工作线程会拿到客户端快照。现在反过来：只有<b>客户端主线程</b>
 * 读客户端快照，其余线程只要服务端在跑就读服务端快照。客户端主线程由平台在客户端初始化时用
 * {@link #setClientThreadCheck} 告知（游戏层不能引用客户端类）。</p>
 */
public final class ConfigBase {
    private static volatile BooleanSupplier clientThread = () -> false;

    private final ConfigStore store;
    private volatile MinecraftServer server;
    private volatile boolean dedicatedServer;

    public ConfigBase(ConfigStore store) {
        this.store = store;
    }

    /** 平台客户端入口调用一次：判断当前线程是不是客户端主线程。专用服不调用。 */
    public static void setClientThreadCheck(BooleanSupplier check) {
        clientThread = check;
    }

    public void init(Path directory, boolean dedicated) {
        dedicatedServer = dedicated;
        store.init(directory);
        load();
    }

    /** 注册完成后首次读取。 */
    public void load() {
        store.load();
    }

    public ConfigStore.Loaded reload() {
        return store.reload();
    }

    public void setServer(MinecraftServer value) {
        server = value;
        if (value == null) {
            store.resetServerToDefaults();
        } else {
            store.reload();
            store.saveDefaultsIfNeeded();
        }
    }

    /** 当前线程该用的快照（只读）。 */
    public JsonObject current() {
        if (dedicatedServer) {
            return store.serverData();
        }
        MinecraftServer active = server;
        if (active != null && (active.isSameThread() || !clientThread.getAsBoolean())) {
            return store.serverData();
        }
        return store.clientData();
    }

    public JsonObject serverSnapshot() {
        return store.serverData().deepCopy();
    }

    public void acceptServer(JsonObject data) {
        store.acceptClient(data);
    }

    public void clearClient() {
        store.clearClient();
    }

    public ConfigStore store() {
        return store;
    }
}
