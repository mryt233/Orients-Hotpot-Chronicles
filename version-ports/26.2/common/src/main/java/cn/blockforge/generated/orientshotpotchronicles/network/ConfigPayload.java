package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 服务端配置快照；注册和传输留在加载器侧。作者：mryt233 */
public record ConfigPayload(String json, java.util.List<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes) implements CustomPacketPayload {
    public static final Type<ConfigPayload> ID = new Type<>(HotPot.id("config"));
    /** 同步的配置 JSON 最长字符数（配置文件本身上限 64KB，这里留足余量）。 */
    public static final int MAX_JSON = 262144;
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.composite(
        // 原版 STRING_UTF8 上限 32767 字符，配置文件允许保留未知键，写大了玩家进服会被踢；放宽到 256K（代码检查 W9，mryt233 2026-11-01）。
        // 长度前缀格式不变，只是上限，两端都是同一个模组版本。
        ByteBufCodecs.stringUtf8(MAX_JSON), ConfigPayload::json,
        net.minecraft.world.item.crafting.RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list(16384)),
        ConfigPayload::recipes, ConfigPayload::new);

    public ConfigPayload {
        recipes = java.util.List.copyOf(recipes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
