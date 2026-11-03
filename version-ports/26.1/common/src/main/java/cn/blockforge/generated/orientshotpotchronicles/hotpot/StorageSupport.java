// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

/** 方块实体同步和物品组件共用原版的带注册表存档上下文。 */
final class StorageSupport {
    private StorageSupport() { }

    static TagValueOutput output(HolderLookup.Provider registries) {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
    }

    static CompoundTag snapshot(BlockEntity entity, HolderLookup.Provider registries) {
        TagValueOutput output = output(registries);
        entity.saveWithoutMetadata(output);
        return output.buildResult();
    }
}
