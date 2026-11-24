/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.view.IServerExtensionProvider;
public interface IWailaCommonRegistration {
    <T> void registerItemStorage(IServerExtensionProvider<ItemStack> provider, Class<? extends T> type);
}
