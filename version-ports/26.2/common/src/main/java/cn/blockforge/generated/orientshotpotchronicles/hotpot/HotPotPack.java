// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Arrays;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.SLOTS;

/** 整锅打包、组件与冷热查询；存读档仍由 HotPotStorage 负责。 */
final class HotPotPack {
    private HotPotPack() { }

    static boolean hasPotContents(HotPotBlockEntity be) {
        return be.hasSoup() || !be.slots.ingredients().isEmpty();
    }

    static ItemStack toPotItem(HotPotBlockEntity be, HotPotMaterial material) {
        ItemStack stack = new ItemStack(HotPotContent.itemFor(material));
        if (be.getLevel() != null && be.hasPotContents()) {
            CompoundTag data = new CompoundTag();
            HotPotStorage.write(be, data, be.getLevel().registryAccess());
            stack.set(HotPotContent.POT_CONTENTS, CustomData.of(data));
        }
        return stack;
    }

    static void loadPotContents(HotPotBlockEntity be, ItemStack stack) {
        CustomData data = stack.get(HotPotContent.POT_CONTENTS);
        if (data == null || be.getLevel() == null || be.getLevel().isClientSide()) {
            return;
        }
        HotPotStorage.read(be, data.copyTag(), be.getLevel().registryAccess());
        be.updateSoupVisual();
        be.markDirtyAndSync();
    }

    static void clearPotContents(HotPotBlockEntity be) {
        be.slots.ingredients().clearContent();
        for (int i = 0; i < SLOTS; i++) {
            be.resetSlot(i, ItemStack.EMPTY);
        }
        be.slots.pendingXp(0F);
        Arrays.fill(be.bases, "");
        Arrays.fill(be.spicyLevels, 0);   // mryt233 改（2026-11-05）：整锅打包后清空，辣度也清掉（r136a）
        be.setChanged();
    }

    static boolean isHeated(HotPotBlockEntity be) {
        if (be.getLevel() == null || !be.hasPot()) {
            return false;
        }
        StoveBlockEntity stove = be.stoveBelow();
        if (stove != null && stove.burning()) {
            return true;
        }
        return !(be instanceof StoveBlockEntity) && HotPotHeat.isHeatSource(be.getLevel(), be.getBlockPos().below());
    }

    static void collectImplicitComponents(HotPotBlockEntity be, DataComponentMap.Builder builder) {
        if (!(be instanceof StoveBlockEntity) && be.getLevel() != null && be.hasPotContents()) {
            CompoundTag data = new CompoundTag();
            HotPotStorage.write(be, data, be.getLevel().registryAccess());
            builder.set(HotPotContent.POT_CONTENTS, CustomData.of(data));
        }
    }

    static void applyImplicitComponents(HotPotBlockEntity be, CustomData data) {
        if (data != null && be.getLevel() != null && !(be instanceof StoveBlockEntity)) {
            HotPotStorage.read(be, data.copyTag(), be.getLevel().registryAccess());
            be.markDirtyAndSync();
        }
    }

    static void removeComponentsFromTag(CompoundTag nbt, String[] keys) {
        for (String key : keys) {
            nbt.remove(key);
        }
    }

    static void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput nbt, String[] keys) {
        for (String key : keys) {
            nbt.discard(key);
        }
    }
}
