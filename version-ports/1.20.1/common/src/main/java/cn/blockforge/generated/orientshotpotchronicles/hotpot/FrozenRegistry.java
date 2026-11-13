package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「登记期可写、冻结后只读」的登记表。
 *
 * <p>mryt233 改（2026-11-10，r147）：锅专用产物表、煮过头表、默认涮煮时间表、第三方汤底来源表、
 * 第三方燃料表以前都是普通 {@code HashMap}，只在启动期写、之后客户端与服务端线程都会读。
 * 现在改成这张表：登记期（启动期）可写，{@link #freeze()} 之后变成不可变快照，
 * 再往里登记会抛 {@link IllegalStateException} 并说明「该在什么时候登记」。</p>
 *
 * <p>冻结时机见 {@link HotPotHooks#freezeRegistries()}：服务端开始启动时（单人和专用服都覆盖），
 * 此时各加载器的兼容模块（农夫乐事、森罗）都已经跑完。冻结一次就一直冻着——
 * 单人退出世界再进另一个世界不会重新登记，也就不需要「解冻 / 再冻结」，
 * 避免「刚进新世界那段窗口期被晚到的登记悄悄改掉」这种不确定状态。</p>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * 作者：mryt233
 */
public final class FrozenRegistry<K, V> {
    private final String name;
    private final String freezeHint;
    private Map<K, V> writable = new LinkedHashMap<>();
    private volatile Map<K, V> frozen;

    /**
     * @param name        这张表的名字（报错信息里用，比如「锅专用产物表」）
     * @param freezeHint  冻结后还来登记时该告诉作者什么（比如「要在启动期登记」）
     */
    public FrozenRegistry(String name, String freezeHint) {
        this.name = name;
        this.freezeHint = freezeHint;
    }

    /** 登记一项。冻结之后抛异常。 */
    public synchronized void put(K key, V value) {
        if (frozen != null) {
            throw new IllegalStateException(name + " 已在服务端启动时冻结，不能再登记（" + key + "）。" + freezeHint);
        }
        writable.put(key, value);
    }

    /** 查一项；注册 / 加载 / 冻结之后都可用（冻结前直接查可写表，冻结后查不可变快照）。 */
    public V get(K key) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.get(key) : writable.get(key);
    }

    /** 有没有这一项。 */
    public boolean containsKey(K key) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.containsKey(key) : writable.containsKey(key);
    }

    /** 有没这一项作为值（锅专用产物表用它判断「这是不是锅专用产物」）。 */
    public boolean containsValue(V value) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.containsValue(value) : writable.containsValue(value);
    }

    /** 只读视图（冻结后就是那张不可变快照）。 */
    public Map<K, V> view() {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot : Collections.unmodifiableMap(writable);
    }

    /** 冻结：之后只读、再登记抛异常。重复调用是幂等的（第二次什么都不做）。 */
    public synchronized void freeze() {
        if (frozen == null) {
            frozen = Collections.unmodifiableMap(new LinkedHashMap<>(writable));
            writable = null;
        }
    }

    public boolean isFrozen() {
        return frozen != null;
    }

    public String name() {
        return name;
    }

    /** 诊断用：现在有多少项。 */
    public int size() {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.size() : writable.size();
    }
}
