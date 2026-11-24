// 作者：mryt233
package cn.blockforge.orient.framework.pure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 兼容客户端模块回调与加载器事件的先后顺序；后续资源重载可重放登记。 */
public final class DeferredRegistrations<E> {
    private final List<Consumer<E>> registrations = new ArrayList<>();
    private E current;

    public synchronized void add(Consumer<E> registration) {
        Objects.requireNonNull(registration);
        if (current != null) registration.accept(current);
        registrations.add(registration);
    }

    public synchronized void bind(E event) {
        current = Objects.requireNonNull(event);
        for (Consumer<E> registration : List.copyOf(registrations)) registration.accept(event);
    }
}
