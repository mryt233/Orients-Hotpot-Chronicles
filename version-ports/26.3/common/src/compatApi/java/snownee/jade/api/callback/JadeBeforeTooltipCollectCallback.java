/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api.callback;
import org.apache.commons.lang3.mutable.MutableObject;
import snownee.jade.api.Accessor;
import snownee.jade.api.theme.Theme;
@FunctionalInterface
public interface JadeBeforeTooltipCollectCallback {
    boolean beforeCollecting(MutableObject<Theme> theme, Accessor<?> accessor);
}
