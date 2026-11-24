package cn.blockforge.orient.framework.compat;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/** 只提供框架及内容使用的组合编码能力，字段顺序保持调用顺序。作者：mryt233 */
public interface StreamCodec<B, V> {
    V decode(B buffer);
    void encode(B buffer, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> writer, Function<B, V> reader) {
        return new StreamCodec<>() {
            public V decode(B buffer) { return reader.apply(buffer); }
            public void encode(B buffer, V value) { writer.accept(buffer, value); }
        };
    }

    default <R> StreamCodec<B, R> map(Function<? super V, ? extends R> from,
                                     Function<? super R, ? extends V> to) {
        return of((buffer, value) -> encode(buffer, to.apply(value)), buffer -> from.apply(decode(buffer)));
    }

    default <S extends B> StreamCodec<S, V> cast() {
        return of(this::encode, this::decode);
    }

    default <R> StreamCodec<B, R> apply(Function<StreamCodec<B, V>, StreamCodec<B, R>> operation) {
        return operation.apply(this);
    }

    static <B, A, R> StreamCodec<B, R> composite(StreamCodec<? super B, A> codec,
            Function<R, A> getter, Function<A, R> factory) {
        return of((b, v) -> codec.encode(b, getter.apply(v)), b -> factory.apply(codec.decode(b)));
    }

    @FunctionalInterface
    interface Function3<A, C, D, R> {
        R apply(A a, C c, D d);
    }

    @FunctionalInterface
    interface Function4<A, C, D, E, R> {
        R apply(A a, C c, D d, E e);
    }

    @FunctionalInterface
    interface Function5<A, C, D, E, F, R> {
        R apply(A a, C c, D d, E e, F f);
    }

    static <B, A, C, R> StreamCodec<B, R> composite(StreamCodec<? super B, A> first,
            Function<R, A> getterA, StreamCodec<? super B, C> second, Function<R, C> getterC,
            BiFunction<A, C, R> factory) {
        return of((b, v) -> { first.encode(b, getterA.apply(v)); second.encode(b, getterC.apply(v)); },
                  b -> factory.apply(first.decode(b), second.decode(b)));
    }

    static <B, A, C, D, R> StreamCodec<B, R> composite(
            StreamCodec<? super B, A> c1, Function<R, A> g1,
            StreamCodec<? super B, C> c2, Function<R, C> g2,
            StreamCodec<? super B, D> c3, Function<R, D> g3,
            Function3<A, C, D, R> factory) {
        return of((b, v) -> { c1.encode(b, g1.apply(v)); c2.encode(b, g2.apply(v)); c3.encode(b, g3.apply(v)); },
                  b -> factory.apply(c1.decode(b), c2.decode(b), c3.decode(b)));
    }

    static <B, A, C, D, E, R> StreamCodec<B, R> composite(
            StreamCodec<? super B, A> c1, Function<R, A> g1,
            StreamCodec<? super B, C> c2, Function<R, C> g2,
            StreamCodec<? super B, D> c3, Function<R, D> g3,
            StreamCodec<? super B, E> c4, Function<R, E> g4,
            Function4<A, C, D, E, R> factory) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
        }, b -> factory.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b)));
    }

    static <B, A, C, D, E, F, R> StreamCodec<B, R> composite(
            StreamCodec<? super B, A> c1, Function<R, A> g1,
            StreamCodec<? super B, C> c2, Function<R, C> g2,
            StreamCodec<? super B, D> c3, Function<R, D> g3,
            StreamCodec<? super B, E> c4, Function<R, E> g4,
            StreamCodec<? super B, F> c5, Function<R, F> g5,
            Function5<A, C, D, E, F, R> factory) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
            c5.encode(b, g5.apply(v));
        }, b -> factory.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b), c5.decode(b)));
    }
}
