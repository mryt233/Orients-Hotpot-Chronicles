// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

/** 战利品共用构造片段，不保存随机状态。 */
public final class HotPotLoot {
    private HotPotLoot() { }

    public static LootItemCondition.Builder withoutShears(HolderLookup.Provider registries) {
        return MatchTool.toolMatches(ItemPredicate.Builder.item()
            .of(registries.lookupOrThrow(Registries.ITEM), Items.SHEARS)).invert();
    }

    public static SmeltItemFunction.Builder smeltWhenBurning() {
        return SmeltItemFunction.smelted().when(LootItemEntityPropertyCondition.hasProperties(
            LootContext.EntityTarget.THIS,
            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true))));
    }
}
