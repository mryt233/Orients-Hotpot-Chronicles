/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import snownee.jade.api.Accessor;
import snownee.jade.api.IJadeProvider;
public interface IServerExtensionProvider<T> extends IJadeProvider {
    List<ViewGroup<T>> getGroups(Accessor<?> accessor);
    default boolean shouldRequestData(Accessor<?> accessor) { return true; }
}
