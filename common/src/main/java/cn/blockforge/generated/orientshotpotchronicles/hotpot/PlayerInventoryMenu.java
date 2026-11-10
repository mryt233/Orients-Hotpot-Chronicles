// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.function.IntUnaryOperator;

/** 菜单公共机械流程；接收规则、经验和槽清空通知由具体菜单保留。 */
public abstract class PlayerInventoryMenu extends AbstractContainerMenu {
    protected PlayerInventoryMenu(MenuType<?> type, int syncId) { super(type, syncId); }

    protected void addPlayerInventory(Inventory inventory, int x, int y, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, x + col * 18, hotbarY));
    }

    protected DataSlot[] bindData(int[] client, IntUnaryOperator server) {
        DataSlot[] data = new DataSlot[client.length];
        for (int i = 0; i < data.length; i++) {
            final int index = i;
            data[i] = addDataSlot(new DataSlot() {
                @Override
                public int get() { return server == null ? client[index] : server.applyAsInt(index); }
                @Override
                public void set(int value) { if (server == null) client[index] = value; }
            });
        }
        return data;
    }

    protected abstract int containerSlots();
    protected boolean fromContainer(Slot slot, int slotId) { return slotId < containerSlots(); }
    protected boolean acceptsQuickMove(ItemStack stack) { return true; }
    protected boolean requireMovedCount() { return false; }
    protected boolean quickMoveOutput(Slot slot, boolean fromContainer) { return false; }
    protected void onQuickMoved(Player player, Slot slot, boolean fromContainer, int moved, boolean cookedOutput) { }
    protected void onQuickEmptied(Slot slot, boolean fromContainer) { }
    protected void afterQuickMove() { }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int before = stack.getCount();
        boolean fromContainer = fromContainer(slot, slotId);
        boolean cookedOutput = quickMoveOutput(slot, fromContainer);
        if (fromContainer) {
            if (!moveItemStackTo(stack, containerSlots(), slots.size(), true)) return ItemStack.EMPTY;
        } else if (!acceptsQuickMove(stack) || !moveItemStackTo(stack, 0, containerSlots(), false)) {
            return ItemStack.EMPTY;
        }
        int moved = before - stack.getCount();
        if (requireMovedCount() && moved <= 0) return ItemStack.EMPTY;
        onQuickMoved(player, slot, fromContainer, moved, cookedOutput);
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            onQuickEmptied(slot, fromContainer);
        } else {
            slot.setChanged();
        }
        afterQuickMove();
        return original;
    }
}
