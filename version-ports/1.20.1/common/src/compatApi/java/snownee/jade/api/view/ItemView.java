/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
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
