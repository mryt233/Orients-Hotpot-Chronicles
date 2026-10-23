package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * 灶膛界面：像熔炉一样只有一个燃料槽，可放入任何原版熔炉燃料。
 * 燃烧进度通过两个同步属性（余燃 / 总燃）送到客户端画火苗。
 */
public class StoveScreenHandler extends ScreenHandler {
    /** 燃料槽相对屏幕的位置。 */
    public static final int FUEL_X = 112;
    public static final int FUEL_Y = 35;

    private final Inventory fuel;
    private final Property[] properties = new Property[2];
    private final int[] clientData = new int[2];

    // 服务端构造
    public StoveScreenHandler(int syncId, PlayerInventory playerInventory, StoveBlockEntity be) {
        this(syncId, playerInventory, be.fuelInventory(), be);
    }

    // 客户端构造（扩展数据里只带方块坐标）
    public StoveScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, clientFuel(playerInventory, pos), null);
    }

    private StoveScreenHandler(int syncId, PlayerInventory playerInventory, Inventory fuel,
                               StoveBlockEntity be) {
        super(HotPotContent.STOVE_SCREEN, syncId);
        this.fuel = fuel;
        fuel.onOpen(playerInventory.player);

        addSlot(new FuelSlot(fuel, 0, FUEL_X, FUEL_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        for (int i = 0; i < properties.length; i++) {
            final int index = i;
            if (be != null) {
                properties[i] = addProperty(new Property() {
                    @Override
                    public int get() {
                        return index == 0 ? be.displayBurnTicks() : be.displayBurnTotal();
                    }

                    @Override
                    public void set(int value) {
                    }
                });
            } else {
                properties[i] = addProperty(new Property() {
                    @Override
                    public int get() {
                        return clientData[index];
                    }

                    @Override
                    public void set(int value) {
                        clientData[index] = value;
                    }
                });
            }
        }
    }

    private static Inventory clientFuel(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof StoveBlockEntity be) {
            return be.fuelInventory();
        }
        return new SimpleInventory(StoveBlockEntity.FUEL_SLOTS);
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
        return fuel.getStack(0) != null && !fuel.getStack(0).isEmpty();
    }

    // ------------------------------------------------------------------ 行为

    @Override
    public boolean canUse(PlayerEntity player) {
        return fuel.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotId);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (slotId == 0) {
            if (!insertItem(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!insertItem(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        fuel.onClose(player);
    }

    /** 灶膛槽：只接受原版熔炉燃料。 */
    private static final class FuelSlot extends Slot {
        FuelSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return HotPotFuel.isFuel(stack);
        }

        @Override
        public int getMaxItemCount() {
            return 64;
        }
    }
}
