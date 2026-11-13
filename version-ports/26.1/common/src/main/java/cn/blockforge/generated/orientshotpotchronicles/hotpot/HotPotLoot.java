// 作者：mryt233
// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.advancements.criterion.EntityFlagsPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

/** 战利品共用构造片段，不保存随机状态。 */
public final class HotPotLoot {
    private HotPotLoot() { }

    public static LootItemCondition.Builder withoutShears() {
        return MatchTool.toolMatches(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, Items.SHEARS)).invert();
    }

    public static SmeltItemFunction.Builder smeltWhenBurning() {
        return SmeltItemFunction.smelted().when(LootItemEntityPropertyCondition.hasProperties(
            LootContext.EntityTarget.THIS,
            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true))));
    }
}
