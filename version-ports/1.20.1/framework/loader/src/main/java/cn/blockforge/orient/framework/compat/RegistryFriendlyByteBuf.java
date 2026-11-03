package cn.blockforge.orient.framework.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

/** 1.20.1 栈和坐标编码不要求注册表上下文。作者：mryt233 */
public final class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    public RegistryFriendlyByteBuf(ByteBuf buffer) { super(buffer); }
}
