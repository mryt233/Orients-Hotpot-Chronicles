/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.callback;
import org.apache.commons.lang3.mutable.MutableObject;
import snownee.jade.api.Accessor;
import snownee.jade.api.theme.Theme;
@FunctionalInterface
public interface JadeBeforeTooltipCollectCallback {
    boolean beforeCollecting(MutableObject<Theme> theme, Accessor<?> accessor);
}
