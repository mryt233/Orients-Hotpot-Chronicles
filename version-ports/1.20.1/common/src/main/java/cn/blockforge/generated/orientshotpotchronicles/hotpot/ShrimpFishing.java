// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootItem;

/** 原鱼池中追加虾条目，不能另建额外掉落池（mryt233 改：1.20.1 适配）。 */
public final class ShrimpFishing {
    public static final int WEIGHT = 20;
    public static final ResourceLocation FISHING_FISH = new ResourceLocation("gameplay/fishing/fish");
    private ShrimpFishing() { }
    // mryt233 改（2026-11-13，4K）：改读配置中的钓鱼出虾权重；权重为 0 时仍保留条目，仅权重为 0 不会被抽中。
    public static LootItem.Builder<?> entry() {
        return LootItem.lootTableItem(HotPotContent.SHRIMP)
            .setWeight(Math.max(0, Config.lootShrimpFishingWeight()));
    }
}
