package cn.blockforge.orient.framework.loader;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 跨注册表引用的薄封装（EXP-REG 结论：不自研注册系统）。
 * 显式区分「是否存在」与「取值」：缺失时 get() 抛带 id 的诊断异常，不返回 AIR/null。
 * 作者：mryt233
 */
public final class HolderRef<T> {
    private final String id;
    private final BooleanSupplier present;
    private final Supplier<T> supplier;

    private HolderRef(String id, BooleanSupplier present, Supplier<T> supplier) {
        this.id = id;
        this.present = present;
        this.supplier = supplier;
    }

    public static <T> HolderRef<T> of(String id, BooleanSupplier present, Supplier<T> supplier) {
        return new HolderRef<>(id, present, supplier);
    }

    public String id() {
        return id;
    }

    public boolean isPresent() {
        return present.getAsBoolean();
    }

    /** 缺失时抛异常而不是返回 AIR/null，避免把「没注册」当成合法值。 */
    public T get() {
        if (!present.getAsBoolean()) {
            throw new IllegalStateException("unresolved holder: " + id);
        }
        T value = supplier.get();
        if (value == null) {
            throw new IllegalStateException("holder resolved to null: " + id);
        }
        return value;
    }
}