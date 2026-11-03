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

    static <B, A, C, R> StreamCodec<B, R> composite(StreamCodec<? super B, A> first,
            Function<R, A> getterA, StreamCodec<? super B, C> second, Function<R, C> getterC,
            BiFunction<A, C, R> factory) {
        return of((b, v) -> { first.encode(b, getterA.apply(v)); second.encode(b, getterC.apply(v)); },
                  b -> factory.apply(first.decode(b), second.decode(b)));
    }
}
