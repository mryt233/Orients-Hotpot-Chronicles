// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Arrays;
import java.util.function.IntPredicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.SLOTS;

/** 81 格并行状态、槽位查找与 Container 规则；不负责存档格式。 */
final class HotPotSlots implements Container {
    private final HotPotBlockEntity pot;
    private final SimpleContainer ingredients = new SimpleContainer(SLOTS);
    private final int[] doneness = new int[SLOTS];
    private final int[] cookDur = new int[SLOTS];
    // mryt233 改（2026-11-10）：只在修订号变化时重算，doneness 始终保留千分比。
    private long cookRevision = -1;
    private final float[] xpValue = new float[SLOTS];
    private final boolean[] cooked = new boolean[SLOTS];
    /**
    * mryt233 改（2026-11-04）：熟了以后还在锅里继续煮的刻数（豆腐煮过头碎成豆腐块用）。
    * 只对登记过 `HotPotCooking.overcook` 的熟食累加；煮熟那一刻清 0，取走 / 换掉时一起重置。存档里是 Overcook 数组。
    */
    private final int[] overcook = new int[SLOTS];
    private final ItemStack[] tracked = new ItemStack[SLOTS];
    private float pendingXp = 0F;
    /**
    * 格子可能被外部动过（界面、漏斗、其他模组走 Container 接口，或原地改了物品数量后 setChanged）。
    * 只有它为 true 时服务端 tick 才逐格比较找变化，不再每刻扫 81 格（mryt233 2026-11-01，代码检查 W4）。
    */
    private boolean slotsTouched = true;

    HotPotSlots(HotPotBlockEntity pot) {
        this.pot = pot;
        Arrays.fill(tracked, ItemStack.EMPTY);
    }

    SimpleContainer ingredients() { return ingredients; }
    int[] doneness() { return doneness; }
    int[] cookDur() { return cookDur; }
    float[] xpValue() { return xpValue; }
    boolean[] cooked() { return cooked; }
    int[] overcook() { return overcook; }
    ItemStack[] tracked() { return tracked; }
    long cookRevision() { return cookRevision; }
    void cookRevision(long value) { cookRevision = value; }
    float pendingXp() { return pendingXp; }
    void pendingXp(float value) { pendingXp = value; }

    void touch() { slotsTouched = true; }
    @Override
    public void setChanged() { pot.setChanged(); }

    void trackSlotChanges() {
        if (!slotsTouched) {
            return;
        }
        slotsTouched = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getItem(i);
            ItemStack snapshot = tracked[i];
            if (snapshot == null || !ItemStack.isSameItemSameComponents(snapshot, stack)) {
                resetSlot(i, stack);
                pot.syncPending = true;
            }
        }
    }

    void recomputeCookTimes() {
        cookRevision = HotPotCooking.cookRevision();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getItem(i);
            if (!stack.isEmpty() && !cooked[i]) {
                cookDur[i] = HotPotCooking.cookTicks(pot.getLevel(), stack);
            }
        }
    }

    void resetSlot(int slot, ItemStack stack) {
        tracked[slot] = stack.copy();
        doneness[slot] = 0;
        cooked[slot] = false;
        overcook[slot] = 0;
        xpValue[slot] = 0F;
        cookDur[slot] = stack.isEmpty() ? 0 : HotPotCooking.cookTicks(pot.getLevel(), stack);
    }

    void onSlotStackChanged(int slot) {
        if (slot >= 0 && slot < SLOTS) {
            resetSlot(slot, ingredients.getItem(slot));
            pot.markDirtyAndSync();
        }
    }

    void onSlotEmptied(int slot) {
        if (slot >= 0 && slot < SLOTS && ingredients.getItem(slot).isEmpty()) {
            resetSlot(slot, ItemStack.EMPTY);
            pot.markDirtyAndSync();
        }
    }

    int filledSlots() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!ingredients.getItem(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    int firstEmptySlot() {
        PotLayout layout = pot.layout();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ingredients.getItem(slot).isEmpty() && layout.sideOfSlot(slot) != PotLayout.NONE) {
                return slot;
            }
        }
        return -1;
    }

    boolean hasRawIngredient() {
        for (int i = 0; i < SLOTS; i++) {
            if (!cooked[i] && !ingredients.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    int randomEmptySlot() {
        int[] open = new int[9];
        int n = 0;
        for (int cell = 0; cell < 9; cell++) {
            if (firstEmptySlotInCell(cell) >= 0) {
                open[n++] = cell;
            }
        }
        return n == 0 ? -1 : firstEmptySlotInCell(open[pot.getLevel().random.nextInt(n)]);
    }

    int firstEmptySlotInCell(int bigCell) {
        return firstSlotInCell(bigCell, slot -> ingredients.getItem(slot).isEmpty());
    }

    int firstCookedSlotInCell(int bigCell) {
        return firstSlotInCell(bigCell, slot -> cooked[slot] && !ingredients.getItem(slot).isEmpty());
    }

    int firstSlotInCell(int bigCell, IntPredicate matches) {
        PotLayout layout = pot.layout();
        if (bigCell < 0 || bigCell > 8 || !layout.usesCell(bigCell)) {
            return -1;
        }
        if (layout.multiSoup()) {
            // 鸳鸯 / 田字锅：锅里这个位置轮流显示的是归到它的那些小格（PotLayout.spotOfSlot）
            for (int slot = 0; slot < SLOTS; slot++) {
                if (layout.spotOfSlot(slot) == bigCell && matches.test(slot)) {
                    return slot;
                }
            }
            return -1;
        }
        int baseRow = (bigCell / 3) * 3;
        int baseCol = (bigCell % 3) * 3;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = (baseRow + row) * 9 + baseCol + col;
                if (matches.test(slot)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return ingredients.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ingredients.removeItem(slot, amount);
        if (!result.isEmpty()) {
            pot.markDirtyAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ingredients.removeItemNoUpdate(slot);
        if (!result.isEmpty()) {
            pot.markDirtyAndSync();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        // 面板的每个格子代表一份食材。无论调用方是普通点击、Shift
        // 快速移动还是旧存档恢复，都不允许把多份物品压进同一个物理槽。
        ItemStack single = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        ingredients.setItem(slot, single);
        onSlotStackChanged(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return !pot.isRemoved() && player.canInteractWithBlock(pot.getBlockPos(), 5.0);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return pot.getLevel() != null && slot >= 0 && slot < SLOTS && pot.hasPot()
            && ingredients.getItem(slot).isEmpty() && !cooked[slot]
            && pot.layout().sideOfSlot(slot) != PotLayout.NONE
            && HotPotCooking.acceptable(pot.getLevel(), stack);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOTS && cooked[slot];
    }

    @Override
    public void clearContent() {
        ingredients.clearContent();
        for (int i = 0; i < SLOTS; i++) {
            resetSlot(i, ItemStack.EMPTY);
        }
        pot.markDirtyAndSync();
    }
}
