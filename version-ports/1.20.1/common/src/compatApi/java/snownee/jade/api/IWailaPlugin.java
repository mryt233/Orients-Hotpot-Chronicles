/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api;
public interface IWailaPlugin {
    default void register(IWailaCommonRegistration registration) {}
    default void registerClient(IWailaClientRegistration registration) {}
}
