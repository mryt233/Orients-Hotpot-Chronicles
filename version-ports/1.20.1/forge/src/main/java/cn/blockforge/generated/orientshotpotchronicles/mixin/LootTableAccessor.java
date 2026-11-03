package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

/** 读取原版钓鱼表既有池，不替换整张表。作者：mryt233 */
@Mixin(LootTable.class)
public interface LootTableAccessor {
    @Accessor("pools")
    java.util.List<LootPool> hotpot$getPools();
}
