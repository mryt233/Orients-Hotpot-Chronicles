// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，适配 26.2 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 灶膛界面：像熔炉一样只有一个燃料槽，可放入任何原版熔炉燃料。
 * 燃烧进度通过两个同步属性（余燃 / 总燃）送到客户端画火苗。
 */
public class StoveScreenHandler extends PlayerInventoryMenu {
    public static final int FUEL_X = 112;
    public static final int FUEL_Y = 35;

    private final Container fuel;
    private final StoveBlockEntity stove;
    private final DataSlot[] properties;
    private final int[] clientData = new int[2];

    public StoveScreenHandler(int syncId, Inventory playerInventory, StoveBlockEntity be) {
        this(syncId, playerInventory, be.fuelInventory(), be);
    }

    public StoveScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, clientFuel(playerInventory, pos), null);
    }

    private StoveScreenHandler(int syncId, Inventory playerInventory, Container fuel,
                               StoveBlockEntity be) {
        super(HotPotContent.STOVE_SCREEN, syncId);
        this.fuel = fuel;
        this.stove = be;
        fuel.startOpen(playerInventory.player);

        addSlot(new FuelSlot(fuel, 0, FUEL_X, FUEL_Y, playerInventory.player.level()));
        addPlayerInventory(playerInventory, 8, 84, 142);

        properties = bindData(clientData, be == null ? null : index -> index == 0 ? be.displayBurnTicks() : be.displayBurnTotal());
    }

    private static Container clientFuel(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof StoveBlockEntity be) {
            return be.fuelInventory();
        }
        return new SimpleContainer(StoveBlockEntity.FUEL_SLOTS);
    }

    public int burnTicks() {
        return properties[0].get();
    }

    public int burnTotal() {
        return properties[1].get();
    }

    public float burnRatio() {
        int total = Math.max(1, burnTotal());
        return Math.max(0F, Math.min(1F, burnTicks() / (float) total));
    }

    public boolean hasFuelInSlot() {
        return fuel.getItem(0) != null && !fuel.getItem(0).isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return stove == null || Container.stillValidBlockEntity(stove, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        return super.quickMoveStack(player, slotId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        fuel.stopOpen(player);
    }

    private static final class FuelSlot extends Slot {
        private final Level world;

        FuelSlot(Container inventory, int index, int x, int y, Level world) {
            super(inventory, index, x, y);
            this.world = world;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return HotPotFuel.isFuel(world, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    @Override
    protected int containerSlots() {
        return 1;
    }
}
