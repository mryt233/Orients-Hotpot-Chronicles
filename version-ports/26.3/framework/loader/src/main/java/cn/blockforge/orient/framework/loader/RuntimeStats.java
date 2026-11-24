package cn.blockforge.orient.framework.loader;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 运行时行为计数与摘要（P3 行为断言）：按进程、模块 ID、通道与方向统计初始化、注册、
 * 收发包与事件处理次数，用于验证「初始化一次 / 每个模块各注册一次 / 单次发包 / 事件各处理一次」。
 *
 * <p>不依赖任何加载器或客户端类；平台实现负责设置进程名、日志出口并触发摘要输出。
 * 作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 W2）：网络计数按通道对象缓存（{@link ChannelStats}），
 * 每收发一个包只做一次哈希查找和一次原子加，不再拼字符串键；摘要输出格式不变（行为断言脚本依赖它）。</p>
 */
public final class RuntimeStats {
    private static final RuntimeStats INSTANCE = new RuntimeStats();

    private final AtomicLong initCount = new AtomicLong();
    private final AtomicLong installCount = new AtomicLong();
    private final Map<String, AtomicLong> moduleCounters = new ConcurrentHashMap<>();
    private final Map<CustomPacketPayload.Type<?>, ChannelStats> channels = new ConcurrentHashMap<>();
    private volatile String process = "unknown";
    private volatile String platformName = "none";
    private volatile Consumer<String> sink = line -> {
    };

    private RuntimeStats() {
    }

    public static RuntimeStats get() {
        return INSTANCE;
    }

    /** 平台实现设置日志出口（通常转发到 slf4j）。 */
    public void setSink(Consumer<String> sink) {
        this.sink = sink != null ? sink : line -> {
        };
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String process() {
        return process;
    }

    /** 框架运行时入口初始化一次。 */
    public void runtimeInit() {
        initCount.incrementAndGet();
    }

    public long initCount() {
        return initCount.get();
    }

    public long installCount() {
        return installCount.get();
    }

    public void platformInstalled(String implementation) {
        platformName = implementation;
        installCount.incrementAndGet();
    }

    /** 模块生命周期计数；phase = declaration / register / runtime / client / reload。 */
    public void modulePhase(String moduleId, String phase) {
        bump(moduleCounters, moduleId + "|" + phase);
    }

    /** 方向：S2C = 服务端发往客户端，C2S = 客户端发往服务端。 */
    public enum Direction {
        S2C, C2S
    }

    /** 一个通道两个方向的计数；平台在注册时取一次，收发时直接用。 */
    public static final class ChannelStats {
        private final String id;
        private final AtomicLong[] registered = counters();
        private final AtomicLong[] sent = counters();
        private final AtomicLong[] handled = counters();
        private final AtomicLong[] skipped = counters();
        private final String[] policy = new String[Direction.values().length];

        private ChannelStats(String id) {
            this.id = id;
        }

        private static AtomicLong[] counters() {
            AtomicLong[] array = new AtomicLong[Direction.values().length];
            for (int i = 0; i < array.length; i++) {
                array[i] = new AtomicLong();
            }
            return array;
        }

        public void registered(Direction direction, String policyName) {
            policy[direction.ordinal()] = policyName;
            registered[direction.ordinal()].incrementAndGet();
        }

        public void sent(Direction direction) {
            sent[direction.ordinal()].incrementAndGet();
        }

        public void handled(Direction direction) {
            handled[direction.ordinal()].incrementAndGet();
        }

        public void skipped(Direction direction) {
            skipped[direction.ordinal()].incrementAndGet();
        }
    }

    /** 取通道计数（第一次创建，之后是同一个对象；热路径只有一次哈希查找）。 */
    public ChannelStats channel(CustomPacketPayload.Type<?> type) {
        ChannelStats stats = channels.get(type);
        return stats != null ? stats : channels.computeIfAbsent(type, key -> new ChannelStats(key.id().toString()));
    }

    /** 模块计数，键为「模块 id|阶段」。 */
    public long count(String key) {
        AtomicLong value = moduleCounters.get(key);
        return value == null ? 0L : value.get();
    }

    /** 稳定排序的摘要行（不含前缀）；供日志与命令输出。 */
    public List<String> summary() {
        List<String> lines = new ArrayList<>();
        lines.add("process=" + process + " platform=" + platformName
            + " init=" + initCount.get() + " installs=" + installCount.get());
        Map<String, Map<String, Long>> modules = new TreeMap<>();
        for (Map.Entry<String, AtomicLong> entry : moduleCounters.entrySet()) {
            String key = entry.getKey();
            int split = key.indexOf('|');
            if (split < 0) {
                continue;
            }
            modules.computeIfAbsent(key.substring(0, split), ignored -> new TreeMap<>())
                .put(key.substring(split + 1), entry.getValue().get());
        }
        for (Map.Entry<String, Map<String, Long>> entry : modules.entrySet()) {
            Map<String, Long> phases = entry.getValue();
            lines.add("module process=" + process + " id=" + entry.getKey()
                + " declaration=" + phases.getOrDefault("declaration", 0L)
                + " register=" + phases.getOrDefault("register", 0L)
                + " runtime=" + phases.getOrDefault("runtime", 0L)
                + " client=" + phases.getOrDefault("client", 0L)
                + " reload=" + phases.getOrDefault("reload", 0L));
        }
        Map<String, String> network = new TreeMap<>();
        for (ChannelStats stats : channels.values()) {
            for (Direction direction : Direction.values()) {
                int i = direction.ordinal();
                if (stats.registered[i].get() + stats.sent[i].get() + stats.handled[i].get() + stats.skipped[i].get() == 0) {
                    continue;
                }
                network.put(stats.id + "|" + direction, "net process=" + process + " channel=" + stats.id
                    + " direction=" + direction
                    + " registered=" + stats.registered[i].get()
                    + " policy=" + (stats.policy[i] == null ? "?" : stats.policy[i])
                    + " sent=" + stats.sent[i].get()
                    + " handled=" + stats.handled[i].get()
                    + " skipped=" + stats.skipped[i].get());
            }
        }
        lines.addAll(network.values());
        return lines;
    }

    /** 输出摘要；trigger 说明触发时机（server-started / server-stopped / command 等）。 */
    public void logSummary(String trigger) {
        Consumer<String> current = sink;
        current.accept("[ORIENT-FW] summary trigger=" + trigger);
        for (String line : summary()) {
            current.accept("[ORIENT-FW] " + line);
        }
    }

    private static void bump(Map<String, AtomicLong> counters, String key) {
        counters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
    }
}
