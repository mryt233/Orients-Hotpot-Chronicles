package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Map;

/**
 * 用擀面杖（原叫木棒，2026-10-23 用户改）在砧板上捣丸子（2026-10-23 用户定，方案 A）：生牛肉 / 生猪排 / 生羊肉 / 生鳕鱼 / 生鲑鱼放砧板上，擀面杖捣三下：
 * 第 1 下捶碎、第 2 下成肉浆（砧板上的样子跟着变，用户要求），第 3 下掉出 3 个生丸子。
 * 进度记在 CuttingBoardBlockEntity 的 cuts 里（捣过的不能再用菜刀切）。阶段贴图 textures/block/pounding/&lt;meat&gt;_&lt;minced|paste&gt;。
 *
 */
public final class Pounding {
    public static final int HITS = 3;
    public static final int RESULT_COUNT = 3;

    /** 生肉 → 阶段贴图用的名字（beef / pork / mutton / fish）。 */
    private static final Map<Item, String> MEAT = Map.of(
        Items.BEEF, "beef", Items.PORKCHOP, "pork", Items.MUTTON, "mutton", Items.COD, "fish", Items.SALMON, "fish");

    private Pounding() {
    }

    public static String meatOf(ItemStack stack) {
        return MEAT.get(stack.getItem());
    }

    public static boolean poundable(ItemStack stack) {
        return meatOf(stack) != null;
    }

    private static final java.util.List<String> MEAT_ORDER = java.util.List.of("beef", "pork", "mutton", "fish");

    /**
     * 砧板上捣了 hits 下的样子：隐藏物品 pounded_meat + 自定义模型数据（1..8 = 牛 / 猪 / 羊 / 鱼 × 捶碎 / 肉浆，
     * 同 tools/gen_meatballs.py），按普通物品画就有厚度（2026-10-23 用户：要和原版食材放上去一样是立体的）。
     */
    public static ItemStack display(ItemStack meat, int hits) {
        String m = meatOf(meat);
        if (m == null || hits <= 0) {
            return meat;
        }
        ItemStack out = new ItemStack(HotPotContent.POUNDED_MEAT);
        out.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
            new net.minecraft.component.type.CustomModelDataComponent(MEAT_ORDER.indexOf(m) * 2 + (hits == 1 ? 1 : 2)));
        return out;
    }

    /** 这块肉捣出来的生丸子。 */
    public static Item result(ItemStack stack) {
        String meat = meatOf(stack);
        if (meat == null) {
            return null;
        }
        return switch (meat) {
            case "beef" -> HotPotContent.BEEF_MEATBALL;
            case "pork" -> HotPotContent.PORK_MEATBALL;
            case "mutton" -> HotPotContent.MUTTON_MEATBALL;
            default -> HotPotContent.FISH_BALL;
        };
    }
}
