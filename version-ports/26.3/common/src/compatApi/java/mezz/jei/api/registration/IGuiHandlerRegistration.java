package mezz.jei.api.registration;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
/** 编译期签名，不进入分发包。作者：mryt233 */
public interface IGuiHandlerRegistration {
    <T extends AbstractContainerScreen<?>> void addGuiContainerHandler(Class<? extends T> type, IGuiContainerHandler<T> handler);
}
