package cn.blockforge.generated.orientshotpotchronicles.compat.modules;

import cn.blockforge.orient.framework.game.CompatModule;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 森罗物语一类模组的兼容（从 ModCompat 拆出来）。
 * 这类模组在不同发布渠道用过多个 mod id，任意一个存在就按同一套规则处理：
 * 灵木可以当灶台燃料（按煤那一档）。
 * <p>
 * 以后要加酒馆龙头联动之类的，直接在 {@link #register()} 里补。
 * 作者：mryt233 */
public final class FloraCompat implements CompatModule {
    /** 森罗物语类模组在不同发布渠道使用过多个 id，这里一并探测。 */
    private static final List<String> MOD_IDS = List.of("shinra", "forest_of_souls", "sylvanlands");

    @Override
    public List<String> modIds() {
        return MOD_IDS;
    }

    @Override
    public void register() {
        for (String id : MOD_IDS) {
            if (!HotPotCompat.isModLoaded(id)) {
                continue;
            }
            Item spiritLog = HotPotCompat.itemOf(id, "spirit_log");
            if (spiritLog != null) {
                HotPotCompat.registerFuel(spiritLog, HotPotFuel.COAL);
                HotPotCompat.info("检测到自然类联动模组（" + id + "），灵木可作灶台燃料。");
            }
            return;   // 只处理第一个存在的发行版
        }
    }
}
