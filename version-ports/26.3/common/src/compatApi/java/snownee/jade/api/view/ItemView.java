/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api.view;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
public class ItemView {
    public ItemView(ItemStack stack) {}
    public static List<ViewGroup<ItemStack>> groupOf(Container container, Accessor<?> accessor) {
        throw new UnsupportedOperationException();
    }
}
