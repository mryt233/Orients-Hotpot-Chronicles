package cn.blockforge.generated.orientshotpotchronicles.compat.modules;

import cn.blockforge.orient.framework.game.CompatModule;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 农夫乐事兼容（从 ModCompat 拆出来）。
 * 用它们的番茄当番茄锅底料、浓汤当菌汤底料、卷心菜叶当柴。
 * 食材本身不用登记：能吃或有烹饪配方的物品天然能下锅。
 * 作者：mryt233 */
public final class FarmersDelightCompat implements CompatModule {
    @Override
    public List<String> modIds() {
        return List.of("farmersdelight");
    }

    @Override
    public void register() {
        int n = 0;
        Item tomato = HotPotCompat.itemOf("farmersdelight", "tomato");
        if (tomato != null) {
            HotPotCompat.registerBaseSource(tomato, SoupBase.TOMATO);   // 2026-10-25 有了番茄锅底后改成倒出番茄锅
            n++;
        }
        Item richSoup = HotPotCompat.itemOf("farmersdelight", "rich_soup");
        if (richSoup != null) {
            HotPotCompat.registerBaseSource(richSoup, SoupBase.MUSHROOM);
            n++;
        }
        Item cabbage = HotPotCompat.itemOf("farmersdelight", "cabbage_leaf");
        if (cabbage != null) {
            HotPotCompat.registerFuel(cabbage, HotPotFuel.WOOD);   // 卷心菜叶可当柴，很快烧完
        }
        if (n > 0) {
            HotPotCompat.info("检测到农夫乐事，已接入 " + n + " 种汤底来源。");
        }
    }
}
