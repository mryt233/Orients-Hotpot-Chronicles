// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

/**
 * 1.20.x 移植版专用：1.21 用的物品组件（container、held_food、pot_contents、custom_model_data）
 * 在 1.20 还不存在，全部改存物品 NBT，字段名照组件取。其他代码只通过这里读写。
 * 作者：Claude
 */
public final class ItemData {
    private static final String CONTAINER = "container";
    private static final String HELD_FOOD = "orients_hotpot_chronicles:held_food";
    private static final String POT_CONTENTS = "orients_hotpot_chronicles:pot_contents";
    private static final String MODEL = "CustomModelData";

    private ItemData() {
    }

    // ------------------------------------------------------------------ 盘子 / 碗里装的菜

    /** 物品里装的东西（按格子顺序，空格是 EMPTY）；没有就返回空列表。 */
    public static List<ItemStack> contents(ItemStack stack) {
        List<ItemStack> out = new ArrayList<>();
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(CONTAINER, NbtElement.LIST_TYPE)) {
            return out;
        }
        NbtList list = nbt.getList(CONTAINER, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            int slot = entry.getInt("slot");
            while (out.size() <= slot) {
                out.add(ItemStack.EMPTY);
            }
            out.set(slot, ItemStack.fromNbt(entry.getCompound("item")));
        }
        return out;
    }

    public static boolean hasContents(ItemStack stack) {
        return contents(stack).stream().anyMatch(s -> !s.isEmpty());
    }

    /** 照 1.21 的 ContainerComponent.copyTo：按顺序拷进目标列表，多出来的丢掉。 */
    public static void copyContentsTo(ItemStack stack, List<ItemStack> target) {
        List<ItemStack> items = contents(stack);
        for (int i = 0; i < target.size(); i++) {
            target.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        }
    }

    /** 写入装的东西；全空就去掉这个字段。 */
    public static void setContents(ItemStack stack, List<ItemStack> items) {
        NbtList list = new NbtList();
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                NbtCompound entry = new NbtCompound();
                entry.putInt("slot", i);
                entry.put("item", s.writeNbt(new NbtCompound()));
                list.add(entry);
            }
        }
        if (list.isEmpty()) {
            stack.removeSubNbt(CONTAINER);
        } else {
            stack.getOrCreateNbt().put(CONTAINER, list);
        }
    }

    // ------------------------------------------------------------------ 筷子夹着的菜

    public static ItemStack heldFood(ItemStack chopsticks) {
        NbtCompound nbt = chopsticks.getSubNbt(HELD_FOOD);
        return nbt == null ? ItemStack.EMPTY : ItemStack.fromNbt(nbt);
    }

    public static void setHeldFood(ItemStack chopsticks, ItemStack food) {
        if (food.isEmpty()) {
            chopsticks.removeSubNbt(HELD_FOOD);
        } else {
            chopsticks.setSubNbt(HELD_FOOD, food.copyWithCount(1).writeNbt(new NbtCompound()));
        }
    }

    // ------------------------------------------------------------------ 装着东西的锅

    /** 锅物品里打包的汤底和食材（格式同锅方块实体存档）；没有返回 null。 */
    public static NbtCompound potData(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt(POT_CONTENTS);
        return nbt == null ? null : nbt.copy();
    }

    public static void setPotData(ItemStack stack, NbtCompound data) {
        stack.setSubNbt(POT_CONTENTS, data);
    }

    // ------------------------------------------------------------------ 自定义模型数据

    public static void setModel(ItemStack stack, int model) {
        stack.getOrCreateNbt().putInt(MODEL, model);
    }

    public static void clearModel(ItemStack stack) {
        stack.removeSubNbt(MODEL);
    }
    // Codex 改：1.20.1 中用 NBT 保存蘸料组件，随物品一起同步和持久化。
    public static SauceContents sauce(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt("orients_hotpot_chronicles:sauce");
        return nbt == null ? null : new SauceContents(nbt.getInt("mask"), nbt.getInt("bites"));
    }

    public static void setSauce(ItemStack stack, SauceContents sauce) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("mask", sauce.mask());
        nbt.putInt("bites", sauce.bites());
        stack.setSubNbt("orients_hotpot_chronicles:sauce", nbt);
    }

    public static int dipped(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt("orients_hotpot_chronicles:dipped");
    }

    public static void setDipped(ItemStack stack, int mask) {
        stack.getOrCreateNbt().putInt("orients_hotpot_chronicles:dipped", mask);
    }
}
