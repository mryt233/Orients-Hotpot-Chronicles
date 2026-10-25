package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * 蘸料台的补货界面（r103，用户：保留一个补货界面，只用来放原料、看还剩多少）。
 * 上面 9 个原料格 = 台上 9 个小碗（2026-10-23 起碗不固定：空碗放什么原料都行，有东西的只收同一种；调整），
 * 下面玩家背包。同步给客户端的数：前 9 个是每个碗已化开的份数，后 9 个是每个碗装的调料（序号 + 1，0 = 空碗）。
 * 网页预览 viewer/screens.js 的 sauceStation 与 client/SauceStationScreen 一一对应。
 * 作者：
 */
public class SauceStationScreenHandler extends ScreenHandler {
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

    private final Inventory raw;
    private final PropertyDelegate data;

    // 服务端
    public SauceStationScreenHandler(int syncId, PlayerInventory playerInventory, SauceStationBlockEntity be) {
        this(syncId, playerInventory, be.rawInventory(), new PropertyDelegate() {
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
            public int size() {
                return N * 2;
            }
        });
    }

    // 客户端（扩展数据里只带方块坐标）
    public SauceStationScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleInventory(N) {
            @Override
            public int getMaxCountPerStack() {
                return SauceStationBlockEntity.MAX_RAW;
            }
        }, new ArrayPropertyDelegate(N * 2));
    }

    private SauceStationScreenHandler(int syncId, PlayerInventory playerInventory, Inventory raw, PropertyDelegate data) {
        super(HotPotContent.SAUCE_STATION_SCREEN, syncId);
        this.raw = raw;
        this.data = data;
        raw.onOpen(playerInventory.player);
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
        addProperties(data);
    }

    /** 第 i 个碗还能打几次（客户端画份数条用）。 */
    public int available(int i) {
        return data.get(i) + raw.getStack(i).getCount() * SauceStationBlockEntity.PORTIONS_PER_RAW;
    }

    /** 第 i 个碗装的调料；空碗返回 null。 */
    public Seasoning kind(int i) {
        ItemStack stack = raw.getStack(i);
        if (!stack.isEmpty()) {
            return Seasoning.ofRaw(stack);
        }
        int k = data.get(N + i);
        return k > 0 && data.get(i) > 0 ? Seasoning.ALL[k - 1] : null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return raw.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (slotId < N) {
            if (!insertItem(stack, N, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 先补装着同一种的碗，再放进空碗（insertItem 第二轮只放 canInsert 的格子）
            if (Seasoning.ofRaw(stack) == null || !insertItem(stack, 0, N, false)) {
                return ItemStack.EMPTY;
            }
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
        raw.onClose(player);
    }

    /** 原料格：空碗什么原料都收，有东西的碗只收同一种调料的原料；最多 16 个（= 64 份）。 */
    private final class RawSlot extends Slot {
        RawSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            Seasoning s = Seasoning.ofRaw(stack);
            Seasoning k = kind(getIndex());
            return s != null && (k == null || k == s);
        }

        @Override
        public int getMaxItemCount() {
            return SauceStationBlockEntity.MAX_RAW;
        }
    }
}
