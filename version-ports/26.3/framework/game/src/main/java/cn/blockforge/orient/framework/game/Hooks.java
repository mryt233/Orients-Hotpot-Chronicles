package cn.blockforge.orient.framework.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 通用钩子容器：优先级排序（大者先，同级按登记顺序）、可注销、单钩子异常隔离。
 * 作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 W1）：以前每次 {@link #all()} 都复制再排序，
 * 「能不能下锅」这类高频判断一直在重复排序。现在增删时排好一份不可变快照，读的时候直接用。</p>
 */
public final class Hooks<T> {
    public interface Registration {
        void unregister();
    }

    private record Entry<T>(int priority, long sequence, T listener) {
    }

    private static final Comparator<Entry<?>> ORDER =
        Comparator.<Entry<?>>comparingInt(Entry::priority).reversed().thenComparingLong(Entry::sequence);

    private final List<Entry<T>> entries = new ArrayList<>();
    private long sequence;
    private volatile List<T> sorted = List.of();

    public Registration add(T listener) {
        return add(0, listener);
    }

    public synchronized Registration add(int priority, T listener) {
        Entry<T> entry = new Entry<>(priority, sequence++, listener);
        entries.add(entry);
        rebuild();
        return () -> {
            synchronized (this) {
                if (entries.remove(entry)) {
                    rebuild();
                }
            }
        };
    }

    public synchronized boolean remove(T listener) {
        boolean removed = entries.removeIf(entry -> entry.listener() == listener);
        if (removed) {
            rebuild();
        }
        return removed;
    }

    private void rebuild() {
        List<Entry<T>> copy = new ArrayList<>(entries);
        copy.sort(ORDER);
        List<T> result = new ArrayList<>(copy.size());
        for (Entry<T> entry : copy) {
            result.add(entry.listener());
        }
        sorted = List.copyOf(result);
    }

    /** 按优先级从高到低的不可变快照（不复制、不排序，可在热路径上直接遍历）。 */
    public List<T> all() {
        return sorted;
    }

    public int size() {
        return sorted.size();
    }

    public boolean isEmpty() {
        return sorted.isEmpty();
    }

    /** 依次调用；单钩子抛错只记录并继续（异常隔离）。 */
    public void forEach(Consumer<T> action, BiConsumer<T, Throwable> onError) {
        for (T listener : sorted) {
            try {
                action.accept(listener);
            } catch (Throwable error) {
                onError.accept(listener, error);
            }
        }
    }
}
