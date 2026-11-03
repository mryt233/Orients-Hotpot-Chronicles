package cn.blockforge.orient.framework.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** 原版 VarInt 和有界 UTF-8 字符串的等价编码。作者：mryt233 */
public final class ByteBufCodecs {
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
        (b, v) -> new FriendlyByteBuf(b).writeVarInt(v), b -> new FriendlyByteBuf(b).readVarInt());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    private ByteBufCodecs() { }

    public static StreamCodec<ByteBuf, String> stringUtf8(int maxLength) {
        return StreamCodec.of((b, s) -> new FriendlyByteBuf(b).writeUtf(s, maxLength),
                               b -> new FriendlyByteBuf(b).readUtf(maxLength));
    }

    public static <B extends ByteBuf, T> Function<StreamCodec<B, T>, StreamCodec<B, List<T>>> list(int maxSize) {
        return codec -> StreamCodec.of((buffer, values) -> {
            if (values.size() > maxSize) throw new IllegalArgumentException("List exceeds " + maxSize);
            new FriendlyByteBuf(buffer).writeVarInt(values.size());
            for (T value : values) codec.encode(buffer, value);
        }, buffer -> {
            int count = new FriendlyByteBuf(buffer).readVarInt();
            if (count < 0 || count > maxSize) throw new IllegalArgumentException("Invalid list size " + count);
            List<T> values = new ArrayList<>(count);
            for (int i = 0; i < count; i++) values.add(codec.decode(buffer));
            return List.copyOf(values);
        });
    }
}
