// （2026-10-29）。1.20.1 Forge 移植：读 LootTable 里私有的 pools 列表，
// 用来给钓鱼表加虾（ShrimpLoot）。Forge 的 1.20.1 把 pools 改成了 List<LootPool>。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootTable.class)
public interface LootTableAccessor {
    @Accessor("pools")
    java.util.List<LootPool> getPools();
}
