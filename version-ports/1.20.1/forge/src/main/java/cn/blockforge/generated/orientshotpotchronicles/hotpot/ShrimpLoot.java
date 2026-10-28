// （2026-10-29）。1.20.1 Forge 移植：读改写原版鱼池的 entries 数组，
// 把虾作为加权项加进同一个池子（等价于 Fabric 底本的 modifyPools，不额外多掉东西）。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.mixin.LootPoolAccessor;
import cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;

import java.util.Arrays;

/**
 * 钓鱼钓到虾（2026-10-26 用户：加虾，目前靠钓鱼获得）：往原版「钓鱼 · 鱼」那张表里唯一的池子加一项虾，权重 20
 * （同池原版：鳕鱼 60、鲑鱼 25、河豚 13、热带鱼 2），钓到鱼的时候约 1/6 是虾。不额外多掉东西。
 *
 */
public final class ShrimpLoot {
    public static final int WEIGHT = 20;
    private static final Identifier FISHING_FISH = new Identifier("gameplay/fishing/fish");

    private ShrimpLoot() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            if (!event.getName().equals(FISHING_FISH)) {
                return;
            }
            for (LootPool pool : ((LootTableAccessor) event.getTable()).getPools()) {
                LootPoolEntry[] entries = ((LootPoolAccessor) pool).getEntries();
                if (entries.length == 0) {
                    continue;
                }
                LootPoolEntry[] next = Arrays.copyOf(entries, entries.length + 1);
                next[entries.length] = ItemEntry.builder(HotPotContent.SHRIMP).weight(WEIGHT).build();
                ((LootPoolAccessor) pool).setEntries(next);
            }
        });
    }
}
