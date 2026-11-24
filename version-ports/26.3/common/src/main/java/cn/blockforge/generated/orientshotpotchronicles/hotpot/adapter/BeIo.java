// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 方块实体存档读写入口：把 {@code ValueOutput}/{@code ValueInput} 与 {@code CompoundTag}
 * （用于整锅打包组件、同步快照）统一转成 {@link StoreWriter} / {@link StoreReader}。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层，26.2 桥接 {@code ValueInput}/{@code ValueOutput}。</p>
 */
public final class BeIo {
    private BeIo() {
    }

    @FunctionalInterface
    public interface WriterCallback {
        void write(StoreWriter writer);
    }

    @FunctionalInterface
    public interface ReaderCallback {
        void read(StoreReader reader);
    }

    public static void write(ValueOutput output, WriterCallback body) {
        body.write(new CompoundWriter(output));
    }

    public static void read(ValueInput input, ReaderCallback body) {
        body.read(new CompoundReader(input));
    }

    public static void write(CompoundTag tag, HolderLookup.Provider registries, WriterCallback body) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        body.write(new CompoundWriter(output));
        tag.merge(output.buildResult());
    }

    public static void read(CompoundTag tag, HolderLookup.Provider registries, ReaderCallback body) {
        body.read(new CompoundReader(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag)));
    }

    public static CompoundTag updateTag(BlockEntity entity, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        entity.saveWithoutMetadata(output);
        return output.buildResult();
    }
}
