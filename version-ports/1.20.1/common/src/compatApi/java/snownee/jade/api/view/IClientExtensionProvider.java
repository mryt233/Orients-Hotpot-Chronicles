/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import snownee.jade.api.Accessor;
import snownee.jade.api.IJadeProvider;
public interface IClientExtensionProvider<IN, OUT> extends IJadeProvider {
    List<ClientViewGroup<OUT>> getClientGroups(Accessor<?> accessor, List<ViewGroup<IN>> groups);
}
