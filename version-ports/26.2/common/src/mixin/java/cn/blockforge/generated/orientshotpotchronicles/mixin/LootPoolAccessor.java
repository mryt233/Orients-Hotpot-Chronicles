package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

/** 在原池追加虾，不另建会多掉一件的池；NeoForge 战利品接线使用。作者：mryt233 */
@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    List<LootPoolEntryContainer> hotpot$getEntries();

    @Mutable
    @Accessor("entries")
    void hotpot$setEntries(List<LootPoolEntryContainer> entries);
}
