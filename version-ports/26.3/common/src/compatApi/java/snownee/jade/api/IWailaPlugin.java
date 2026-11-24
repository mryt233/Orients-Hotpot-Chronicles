/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api;
public interface IWailaPlugin {
    default void register(IWailaCommonRegistration registration) {}
    default void registerClient(IWailaClientRegistration registration) {}
}
