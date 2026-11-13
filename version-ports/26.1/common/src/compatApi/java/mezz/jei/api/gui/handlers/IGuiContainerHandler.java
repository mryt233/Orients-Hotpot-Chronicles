package mezz.jei.api.gui.handlers;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import java.util.List;
/** 编译期签名，不进入分发包。作者：mryt233 */
public interface IGuiContainerHandler<T extends AbstractContainerScreen<?>> {
    default List<Rect2i> getGuiExtraAreas(T screen) { return List.of(); }
}
