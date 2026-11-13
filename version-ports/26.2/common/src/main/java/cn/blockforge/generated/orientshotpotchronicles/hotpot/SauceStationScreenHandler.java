// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/**
 * 蘸料台的补货界面：上面 9 个原料格 = 台上 9 个小碗，下面玩家背包。
 * 作者：mryt233
 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class SauceStationScreenHandler extends PlayerInventoryMenu {
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
    private SauceStationBlockEntity station;

    // 服务端
    public SauceStationScreenHandler(int syncId, Inventory playerInventory, SauceStationBlockEntity be) {
        this(syncId, playerInventory, be.rawInventory(), new ContainerData() {
            @Override
            public int get(int index) {
                if (index < N) {
                    return be.portions(index);
                }
                int kind = be.kindIndex(index - N);
                return kind == -2 ? -1 : kind + 1;
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
        addPlayerInventory(playerInventory, 8, INV_Y, HOTBAR_Y);
        addDataSlots(data);
    }

    public int available(int i) {
        return data.get(i) + raw.getItem(i).getCount() * SauceStationBlockEntity.PORTIONS_PER_RAW;
    }

    public Seasoning kind(int i) {
        SeasoningDef def = kindDef(i);
        return def == null ? null : def.legacy();
    }

    public boolean unknownKind(int i) {
        return data.get(N + i) == -1;
    }

    public SeasoningDef kindDef(int i) {
        if (station != null) {
            return station.kindDef(i);
        }
        if (unknownKind(i)) {
            return null;
        }
        int k = data.get(N + i);
        if (data.get(i) > 0 && k > 0) {
            return Seasonings.byIndex(k - 1);
        }
        ItemStack stack = raw.getItem(i);
        return !stack.isEmpty() ? Seasonings.ofRaw(stack) : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return station == null || Container.stillValidBlockEntity(station, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        return super.quickMoveStack(player, slotId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        raw.stopOpen(player);
    }

    private final class RawSlot extends Slot {
        RawSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            SeasoningDef s = Seasonings.ofRaw(stack);
            SeasoningDef k = kindDef(getContainerSlot());
            return s != null && !unknownKind(getContainerSlot()) && (k == null || k == s);
        }

        @Override
        public int getMaxStackSize() {
            return SauceStationBlockEntity.MAX_RAW;
        }
    }

    @Override
    protected int containerSlots() {
        return N;
    }

    @Override
    protected boolean acceptsQuickMove(ItemStack stack) {
        return Seasonings.ofRaw(stack) != null;
    }
}
