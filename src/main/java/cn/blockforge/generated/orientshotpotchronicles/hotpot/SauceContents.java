package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * 一碗蘸料里有什么（物品组件 sauce，r103）：mask = 放了哪几种调料（{@link Seasoning#bit()}），bites = 还能蘸几口。
 * 用户定：怎么配都行，同一种一碗只能放一次；一碗 9 口；吃到一半能加没放过的，剩余口数不变。
 * 作者：Claude
 */
public record SauceContents(int mask, int bites) {
    public static final int FULL_BITES = 9;

    public static final Codec<SauceContents> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("mask").forGetter(SauceContents::mask),
        Codec.INT.fieldOf("bites").forGetter(SauceContents::bites)
    ).apply(i, SauceContents::new));

    public static final PacketCodec<ByteBuf, SauceContents> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, SauceContents::mask,
        PacketCodecs.VAR_INT, SauceContents::bites,
        SauceContents::new);

    public boolean has(Seasoning s) {
        return (mask & s.bit()) != 0;
    }

    public SauceContents with(Seasoning s) {
        return new SauceContents(mask | s.bit(), bites);
    }

    public SauceContents bitten() {
        return new SauceContents(mask, bites - 1);
    }
}
