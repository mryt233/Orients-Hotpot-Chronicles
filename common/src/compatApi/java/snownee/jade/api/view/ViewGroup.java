/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api.view;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
public class ViewGroup<T> {
    public List<T> views;
    public ViewGroup(List<T> views) {}
    public static <B extends ByteBuf, T> StreamCodec<B, Map.Entry<ResourceLocation, List<ViewGroup<T>>>> listCodec(StreamCodec<B, T> codec) { return null; }
}
