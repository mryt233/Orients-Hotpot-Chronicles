package cn.blockforge.orient.framework.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** 方块坐标仍使用原版 long 编码。作者：mryt233 */
public final class BlockPosCodecs {
    public static final StreamCodec<FriendlyByteBuf, BlockPos> STREAM_CODEC =
        StreamCodec.of(FriendlyByteBuf::writeBlockPos, FriendlyByteBuf::readBlockPos);
    private BlockPosCodecs() { }
}
