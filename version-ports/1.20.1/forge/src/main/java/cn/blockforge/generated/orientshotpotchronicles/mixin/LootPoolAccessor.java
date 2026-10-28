// （2026-10-29）。1.20.1 Forge 移植：读写 LootPool 里私有的 entries 数组，
// 把虾作为加权项加进原版鱼池（和 Fabric 底本的 modifyPools 等价，不额外多掉东西）。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.LootPoolEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    LootPoolEntry[] getEntries();

    @Mutable
    @Accessor("entries")
    void setEntries(LootPoolEntry[] entries);
}
