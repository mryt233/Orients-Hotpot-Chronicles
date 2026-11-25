/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.IJadeProvider;
public interface IServerExtensionProvider<IN, OUT> extends IJadeProvider {
    List<ViewGroup<OUT>> getGroups(ServerPlayer player, ServerLevel level, IN target, boolean showDetails);
}
