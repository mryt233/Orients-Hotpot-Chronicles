// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootItem;

/** 原鱼池中追加虾条目，不能另建额外掉落池。 */
public final class ShrimpFishing {
    public static final int WEIGHT = 20;
    public static final ResourceLocation FISHING_FISH = ResourceLocation.withDefaultNamespace("gameplay/fishing/fish");
    private ShrimpFishing() { }
    public static LootItem.Builder<?> entry() {
        return LootItem.lootTableItem(HotPotContent.SHRIMP).setWeight(WEIGHT);
    }
}
