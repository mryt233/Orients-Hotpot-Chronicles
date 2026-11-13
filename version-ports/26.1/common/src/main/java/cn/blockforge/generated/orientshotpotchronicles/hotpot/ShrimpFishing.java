// 作者：mryt233
// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.entries.LootItem;

/** 原鱼池中追加虾条目，不能另建额外掉落池。 */
public final class ShrimpFishing {
    public static final int WEIGHT = 20;
    public static final Identifier FISHING_FISH = Identifier.withDefaultNamespace("gameplay/fishing/fish");
    private ShrimpFishing() { }

    // mryt233 改（2026-11-13，4K）：改读配置中的钓鱼出虾权重；权重为 0 时仍保留条目，仅权重为 0 不会被抽中。
    public static LootItem.Builder<?> entry() {
        return LootItem.lootTableItem(HotPotContent.SHRIMP)
            .setWeight(Math.max(0, Config.lootShrimpFishingWeight()));
    }
}
