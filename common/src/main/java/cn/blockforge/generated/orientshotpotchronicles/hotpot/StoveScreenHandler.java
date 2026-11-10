package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.BlockPos;

/**
 * 灶膛界面：像熔炉一样只有一个燃料槽，可放入任何原版熔炉燃料。
 * 燃烧进度通过两个同步属性（余燃 / 总燃）送到客户端画火苗。
 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class StoveScreenHandler extends PlayerInventoryMenu {
    /** 燃料槽相对屏幕的位置。 */
    public static final int FUEL_X = 112;
    public static final int FUEL_Y = 35;

    private final Container fuel;
    /** 服务端的灶台（客户端为 null）：判断界面是否还有效用。 */
    private final StoveBlockEntity stove;
    private final DataSlot[] properties;
    private final int[] clientData = new int[2];

    // 服务端构造
    public StoveScreenHandler(int syncId, Inventory playerInventory, StoveBlockEntity be) {
        this(syncId, playerInventory, be.fuelInventory(), be);
    }

    // 客户端构造（扩展数据里只带方块坐标）
    public StoveScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, clientFuel(playerInventory, pos), null);
    }

    private StoveScreenHandler(int syncId, Inventory playerInventory, Container fuel,
                               StoveBlockEntity be) {
        super(HotPotContent.STOVE_SCREEN, syncId);
        this.fuel = fuel;
        this.stove = be;
        fuel.startOpen(playerInventory.player);

        addSlot(new FuelSlot(fuel, 0, FUEL_X, FUEL_Y));
        addPlayerInventory(playerInventory, 8, 84, 142);

        properties = bindData(clientData, be == null ? null : index -> index == 0 ? be.displayBurnTicks() : be.displayBurnTotal());
    }

    private static Container clientFuel(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof StoveBlockEntity be) {
            return be.fuelInventory();
        }
        return new SimpleContainer(StoveBlockEntity.FUEL_SLOTS);
    }

    // ------------------------------------------------------------------ UI 数据

    public int burnTicks() {
        return properties[0].get();
    }

    public int burnTotal() {
        return properties[1].get();
    }

    /** 剩余燃烧比例 [0..1]。 */
    public float burnRatio() {
        int total = Math.max(1, burnTotal());
        return Math.max(0F, Math.min(1F, burnTicks() / (float) total));
    }

    public boolean hasFuelInSlot() {
        return fuel.getItem(0) != null && !fuel.getItem(0).isEmpty();
    }

    // ------------------------------------------------------------------ 行为

    @Override
    public boolean stillValid(Player player) {
        // 以前走 SimpleContainer.stillValid（恒为 true）：走远不关界面，灶台被挖掉后往里放的东西直接消失
        // （mryt233 2026-11-01，代码检查 B2）。改成和原版熔炉一样的判断。
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

    /** 灶膛槽：只接受原版熔炉燃料。 */
    private static final class FuelSlot extends Slot {
        FuelSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return HotPotFuel.isFuel(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    @Override
    protected int containerSlots() { return 1; }
}
