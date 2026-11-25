/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
public class ViewGroup<T> {
    public List<T> views;
    public ViewGroup(List<T> views) {}
    public static <T> boolean saveList(net.minecraft.nbt.CompoundTag tag, String key, List<ViewGroup<T>> groups,
                                       java.util.function.Function<T, net.minecraft.nbt.CompoundTag> saver) { return false; }
}
