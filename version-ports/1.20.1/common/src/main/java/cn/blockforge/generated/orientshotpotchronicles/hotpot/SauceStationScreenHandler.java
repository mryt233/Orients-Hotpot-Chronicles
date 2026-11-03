package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.BlockPos;

/**
 * 蘸料台的补货界面（r103，用户：保留一个补货界面，只用来放原料、看还剩多少）。
 * 上面 9 个原料格 = 台上 9 个小碗（2026-10-23 起碗不固定：空碗放什么原料都行，有东西的只收同一种；调整），
 * 下面玩家背包。同步给客户端的数：前 9 个是每个碗已化开的份数，后 9 个是每个碗装的调料（序号 + 1，0 = 空碗）。
 * 网页预览 viewer/screens.js 的 sauceStation 与 client/SauceStationScreen 一一对应。
 * 作者：mryt233 */
public class SauceStationScreenHandler extends AbstractContainerMenu {
    /** 9 个原料格排成 3×3，和台上的碗一一对应（第一行 = 最上层，从左到右；2026-10-23 用户要求）。 */
    public static final int GRID_X = 44;
    public static final int GRID_Y = 18;
    public static final int STEP_X = 30;
    public static final int STEP_Y = 22;
    public static final int INV_Y = 96;
    public static final int HOTBAR_Y = 154;

    public static int slotX(int i) {
        return GRID_X + (i % 3) * STEP_X;
    }

    public static int slotY(int i) {
        return GRID_Y + (i / 3) * STEP_Y;
    }
    private static final int N = SauceStationBlockEntity.BOWLS;

    private final Container raw;
    private final ContainerData data;
    /** 服务端的蘸料台（客户端为 null）：判断界面是否还有效用。 */
    private SauceStationBlockEntity station;

    // 服务端
    public SauceStationScreenHandler(int syncId, Inventory playerInventory, SauceStationBlockEntity be) {
        this(syncId, playerInventory, be.rawInventory(), new ContainerData() {
            @Override
            public int get(int index) {
                if (index < N) {
                    return be.portions(index);
                }
                Seasoning s = be.kind(index - N);
                return s == null ? 0 : s.ordinal() + 1;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return N * 2;
            }
        });
        this.station = be;
    }

    // 客户端（扩展数据里只带方块坐标）
    public SauceStationScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleContainer(N) {
            @Override
            public int getMaxStackSize() {
                return SauceStationBlockEntity.MAX_RAW;
            }
        }, new SimpleContainerData(N * 2));
    }

    private SauceStationScreenHandler(int syncId, Inventory playerInventory, Container raw, ContainerData data) {
        super(HotPotContent.SAUCE_STATION_SCREEN, syncId);
        this.raw = raw;
        this.data = data;
        raw.startOpen(playerInventory.player);
        for (int i = 0; i < N; i++) {
            addSlot(new RawSlot(raw, i, slotX(i), slotY(i)));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    /** 第 i 个碗还能打几次（客户端画份数条用）。 */
    public int available(int i) {
        return data.get(i) + raw.getItem(i).getCount() * SauceStationBlockEntity.PORTIONS_PER_RAW;
    }

    /** 第 i 个碗装的调料；空碗返回 null。 */
    public Seasoning kind(int i) {
        ItemStack stack = raw.getItem(i);
        if (!stack.isEmpty()) {
            return Seasoning.ofRaw(stack);
        }
        int k = data.get(N + i);
        return k > 0 && data.get(i) > 0 ? Seasoning.ALL[k - 1] : null;
    }

    @Override
    public boolean stillValid(Player player) {
        // 同灶台界面（代码检查 B2，mryt233 2026-11-01）：以前恒为 true
        return station == null || Container.stillValidBlockEntity(station, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotId < N) {
            if (!moveItemStackTo(stack, N, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 先补装着同一种的碗，再放进空碗（insertItem 第二轮只放 canInsert 的格子）
            if (Seasoning.ofRaw(stack) == null || !moveItemStackTo(stack, 0, N, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        raw.stopOpen(player);
    }

    /** 原料格：空碗什么原料都收，有东西的碗只收同一种调料的原料；最多 16 个（= 64 份）。 */
    private final class RawSlot extends Slot {
        RawSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            Seasoning s = Seasoning.ofRaw(stack);
            Seasoning k = kind(getContainerSlot());
            return s != null && (k == null || k == s);
        }

        @Override
        public int getMaxStackSize() {
            return SauceStationBlockEntity.MAX_RAW;
        }
    }
}
