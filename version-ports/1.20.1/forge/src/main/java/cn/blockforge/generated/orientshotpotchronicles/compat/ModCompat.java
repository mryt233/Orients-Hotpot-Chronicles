package cn.blockforge.generated.orientshotpotchronicles.compat;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.item.Item;

/**
 * 启动时挂载的可选联动模块。全部是运行时探测，核心玩法不依赖这些模组；
 * 模组缺失时对应注册自动跳过。
 *
 * <p>食材本身无需注册：凡是可食用或有熔炉熟食配方的物品都能下锅，
 * 联动模组（如农夫乐事）的生鲜/熟食天然兼容。这里只登记
 * "能被倒进锅里的汤底"与"能塞进灶台的燃料"。</p>
 */
public final class ModCompat {
    private ModCompat() {
    }

    public static void init() {
        registerFarmersDelight();
        registerFloraMods();
    }

    private static void registerFarmersDelight() {
        if (!HotPotCompat.isModLoaded("farmersdelight")) {
            return;
        }
        int n = 0;
        Item tomato = HotPotCompat.itemOf("farmersdelight", "tomato");
        if (tomato != null) {
            HotPotCompat.registerBaseSource(tomato, SoupBase.SPICY);
            n++;
        }
        Item richSoup = HotPotCompat.itemOf("farmersdelight", "rich_soup");
        if (richSoup != null) {
            HotPotCompat.registerBaseSource(richSoup, SoupBase.MUSHROOM);
            n++;
        }
        Item cabbage = HotPotCompat.itemOf("farmersdelight", "cabbage_leaf");
        if (cabbage != null) {
            // 卷心菜叶可当柴（很快烧完），算最低档燃料
            HotPotCompat.registerFuel(cabbage, HotPotFuel.WOOD);
        }
        if (n > 0) {
            HotPotCompat.log("检测到农夫乐事，已接入 " + n + " 种汤底来源。");
        }
    }

    private static void registerFloraMods() {
        String loaded = null;
        for (String id : FLORA_MOD_IDS) {
            if (HotPotCompat.isModLoaded(id)) {
                loaded = id;
                break;
            }
        }
        if (loaded == null) {
            return;
        }
        Item spiritLog = HotPotCompat.itemOf(loaded, "spirit_log");
        if (spiritLog != null) {
            HotPotCompat.registerFuel(spiritLog, HotPotFuel.COAL);
            HotPotCompat.log("检测到自然类联动模组（" + loaded + "），灵木可作灶台燃料。");
        }
    }

    /** 森罗物语类模组在不同发布渠道使用过多个 id，这里一并探测。 */
    private static final String[] FLORA_MOD_IDS = {"shinra", "forest_of_souls", "sylvanlands"};
}
