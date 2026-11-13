package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 打草掉小葱、香菜等配菜（mryt233 改：1.20.1 适配 Blocks.GRASS 与 3 参数 LootEventBridge）。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class GarnishLoot {
    public static final float CHANCE = 0.25F;
    public static final float SOYBEAN_CHANCE = 0.15F;
    public static final float GARLIC_CHANCE = 0.15F;
    public static final float CHILI_CHANCE = 0.15F;
    public static final float TOMATO_CHANCE = 0.15F;
    public static final float PEANUT_CHANCE = 0.15F;
    public static final float GREENS_CHANCE = 0.15F;
    public static final float ZHE_ER_GEN_CHANCE = 0.15F;

    private record GarnishDrop(Item item, String id, float fallback) {
    }

    private GarnishLoot() {
    }

    private static GarnishDrop[] drops() {
        return new GarnishDrop[]{
            new GarnishDrop(HotPotContent.SCALLION, "scallion", CHANCE),
            new GarnishDrop(HotPotContent.CILANTRO, "cilantro", CHANCE),
            new GarnishDrop(HotPotContent.SOYBEAN, "soybean", SOYBEAN_CHANCE),
            new GarnishDrop(HotPotContent.GARLIC, "garlic", GARLIC_CHANCE),
            new GarnishDrop(HotPotContent.TOMATO, "tomato", TOMATO_CHANCE),
            new GarnishDrop(HotPotContent.CHILI_PEPPER, "chili_pepper", CHILI_CHANCE),
            new GarnishDrop(HotPotContent.PEANUT, "peanut", PEANUT_CHANCE),
            new GarnishDrop(HotPotContent.GREENS, "greens", GREENS_CHANCE),
            new GarnishDrop(HotPotContent.ZHE_ER_GEN, "zhe_er_gen", ZHE_ER_GEN_CHANCE)
        };
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin) -> {
            if (!builtin) {
                return;
            }
            boolean shortGrass = key.equals(Blocks.GRASS.getLootTable());
            boolean tallGrass = key.equals(Blocks.TALL_GRASS.getLootTable());
            if (!shortGrass && !tallGrass) {
                return;
            }
            // mryt233 改（2026-11-13，4K）：9 种配菜改读各自配置键，番茄独立为 loot.garnish.tomato.chance；
            // 概率为 0 时仍保留池子仅条件不中，便于 /reload 恢复。
            for (GarnishDrop drop : drops()) {
                float chance = Math.max(0F, Math.min(1F, Config.lootGarnishChance(drop.id(), drop.fallback())));
                LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(HotPotLoot.withoutShears())
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(drop.item()));
                if (tallGrass) {
                    pool.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
                }
                tableBuilder.withPool(pool);
            }
        });
    }
}
