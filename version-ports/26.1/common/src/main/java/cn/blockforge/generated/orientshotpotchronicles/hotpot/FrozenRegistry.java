package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「登记期可写、冻结后只读」的登记表。
 *
 * <p>mryt233 改（2026-11-10，r147）：锅专用产物表、煮过头表、默认涮煮时间表、第三方汤底来源表、
 * 第三方燃料表登记期可写，{@link #freeze()} 之后变成不可变快照，
 * 再往里登记会抛 {@link IllegalStateException} 并说明「该在什么时候登记」。</p>
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

    /** 查一项；注册 / 加载 / 冻结之后都可用。 */
    public V get(K key) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.get(key) : writable.get(key);
    }

    /** 有没有这一项。 */
    public boolean containsKey(K key) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.containsKey(key) : writable.containsKey(key);
    }

    /** 有没这一项作为值。 */
    public boolean containsValue(V value) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.containsValue(value) : writable.containsValue(value);
    }

    /** 只读视图（冻结后就是那张不可变快照）。 */
    public Map<K, V> view() {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot : Collections.unmodifiableMap(writable);
    }

    /** 冻结：之后只读、再登记抛异常。重复调用是幂等的。 */
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
