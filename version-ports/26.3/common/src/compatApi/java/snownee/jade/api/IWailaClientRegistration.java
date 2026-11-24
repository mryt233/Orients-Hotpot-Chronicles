/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.callback.JadeRayTraceCallback;
import snownee.jade.api.callback.JadeBeforeTooltipCollectCallback;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ItemView;
public interface IWailaClientRegistration {
    BlockAccessor.Builder blockAccessor();
    CompoundTag getServerData();
    default void addRayTraceCallback(JadeRayTraceCallback callback) { addRayTraceCallback(0, callback); }
    void addRayTraceCallback(int priority, JadeRayTraceCallback callback);
    default void addBeforeTooltipCollectCallback(JadeBeforeTooltipCollectCallback callback) { addBeforeTooltipCollectCallback(0, callback); }
    void addBeforeTooltipCollectCallback(int priority, JadeBeforeTooltipCollectCallback callback);
    void registerItemStorageClient(IClientExtensionProvider<ItemStack, ItemView> provider);
    void registerBlockComponent(IComponentProvider<BlockAccessor> provider, Class<? extends Block> blockClass);
}
