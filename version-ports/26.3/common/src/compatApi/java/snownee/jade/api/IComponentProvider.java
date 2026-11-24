/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api;
import snownee.jade.api.config.IPluginConfig;
public interface IComponentProvider<T extends Accessor<?>> extends IJadeProvider {
    void appendTooltip(ITooltip tooltip, T accessor, IPluginConfig config);
}
