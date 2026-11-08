/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
public interface Accessor<T extends HitResult> {
    Level getLevel();
    T getHitResult();
    CompoundTag getServerData();
    <D> Tag encodeAsNbt(Encoder<D> encoder, D value);
    /** 1.20.1 的编解码函数签名（FriendlyByteBuf 写入）。 */
    @FunctionalInterface
    interface Encoder<D> { void encode(FriendlyByteBuf buf, D value); }
    boolean isServerConnected();
}
