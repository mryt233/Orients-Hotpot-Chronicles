/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
public class ViewGroup<T> {
    public List<T> views;
    public ViewGroup(List<T> views) {}
    public static <T> java.util.function.BiConsumer<FriendlyByteBuf, T> itemEncoder() { return (buf, value) -> { }; }
}
