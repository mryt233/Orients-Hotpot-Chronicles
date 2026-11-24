package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「登记期可写、冻结后只读」的登记表。
 *
 * <p>mryt233 改（2026-11-10，r147）：登记期（启动期）可写，{@link #freeze()} 之后变成不可变快照，
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

    public synchronized void put(K key, V value) {
        if (frozen != null) {
            throw new IllegalStateException(name + " 已在服务端启动时冻结，不能再登记（" + key + "）。" + freezeHint);
        }
        writable.put(key, value);
    }

    public V get(K key) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.get(key) : writable.get(key);
    }

    public boolean containsKey(K key) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.containsKey(key) : writable.containsKey(key);
    }

    public boolean containsValue(V value) {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.containsValue(value) : writable.containsValue(value);
    }

    public Map<K, V> view() {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot : Collections.unmodifiableMap(writable);
    }

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

    public int size() {
        Map<K, V> snapshot = frozen;
        return snapshot != null ? snapshot.size() : writable.size();
    }
}
