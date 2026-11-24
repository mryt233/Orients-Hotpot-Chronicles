/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import snownee.jade.api.Accessor;
import snownee.jade.api.IJadeProvider;
public interface IClientExtensionProvider<IN, OUT> extends IJadeProvider {
    List<ClientViewGroup<OUT>> getClientGroups(Accessor<?> accessor, List<ViewGroup<IN>> groups);
}
