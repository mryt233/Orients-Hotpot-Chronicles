/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
public interface Accessor<T extends HitResult> {
    Level getLevel();
    T getHitResult();
    CompoundTag getServerData();
    <D> Tag encodeAsNbt(StreamEncoder<RegistryFriendlyByteBuf, D> encoder, D value);
    boolean isServerConnected();
}
