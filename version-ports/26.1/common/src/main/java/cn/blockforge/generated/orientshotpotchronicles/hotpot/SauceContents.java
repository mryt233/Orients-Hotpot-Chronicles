package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * 一碗蘸料里有什么（物品组件 sauce，r103）：ids = 放了哪几种调料（规范化的 ID 列表），bites = 还能蘸几口。
 * 用户定：怎么配都行，同一种一碗只能放一次；一碗 9 口；吃到一半能加没放过的，剩余口数不变。
 * mryt233 改（4E-1）：位掩码改成去重、规范排序的 ID 列表；解码新格式 {seasonings, bites} 为主，
 * 旧格式 {mask, bites} 只读备用（主备顺序写死，保留到下个大版本）。
 * 作者：mryt233 */
public record SauceContents(List<String> ids, int bites) {
    public static final int FULL_BITES = 9;

    public SauceContents {
        ids = SauceIds.normalize(ids);
    }

    /** 旧调用方兼容入口；内部数据仍以 ID 为准。 */
    @Deprecated public SauceContents(int mask, int bites) { this(SauceIds.fromMask(mask), bites); }
    @Deprecated public int mask() {
        int result = 0;
        for (Seasoning s : Seasoning.ALL) if (has(s)) result |= s.bit();
        return result;
    }

    private static final Codec<SauceContents> NEW_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().fieldOf("seasonings").forGetter(SauceContents::ids),
        Codec.INT.fieldOf("bites").forGetter(SauceContents::bites)
    ).apply(i, SauceContents::new));

    /** 旧格式 {mask, bites}：只用于读取。 */
    private static final Codec<SauceContents> OLD_CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("mask").forGetter(c -> 0),
        Codec.INT.fieldOf("bites").forGetter(SauceContents::bites)
    ).apply(i, (mask, bites) -> new SauceContents(SauceIds.fromMask(mask), bites)));

    public static final Codec<SauceContents> CODEC = Codec.withAlternative(NEW_CODEC, OLD_CODEC);

    public static final StreamCodec<ByteBuf, SauceContents> PACKET_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SauceContents::ids,
        ByteBufCodecs.VAR_INT, SauceContents::bites,
        SauceContents::new);

    /** 蘸过的菜 dipped 组件：新格式为字符串 ID 列表，旧格式为整数位掩码（只读），读写都规范化。 */
    public static final Codec<List<String>> DIPPED_CODEC = Codec.withAlternative(
        Codec.STRING.listOf(), Codec.INT.xmap(SauceIds::fromMask, ids -> 0))
        .xmap(SauceIds::normalize, SauceIds::normalize);

    public static final StreamCodec<ByteBuf, List<String>> DIPPED_PACKET_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(SauceIds::normalize, SauceIds::normalize);

    public static SauceContents empty() {
        return new SauceContents(List.of(), 0);
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    public boolean has(Seasoning s) {
        return SauceIds.containsDef(ids, s);
    }

    public boolean has(String id) {
        return ids.contains(SauceIds.full(id));
    }

    public SauceContents with(Seasoning s) {
        return with(SauceIds.fullId(s.def()));
    }

    public SauceContents with(String id) {
        ArrayList<String> next = new ArrayList<>(ids);
        next.add(id);
        return new SauceContents(next, bites);
    }

    public SauceContents bitten() {
        return new SauceContents(ids, bites - 1);
    }
}
