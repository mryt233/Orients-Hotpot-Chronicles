// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 方块实体存读档的 26.1 版本适配入口：同时支持 {@link ValueOutput}/{@link ValueInput} 与 {@link CompoundTag}。
 *
 * <p>mryt233 改（2026-11-13）：方块实体 {@code saveAdditional} / {@code loadAdditional} 走
 * {@link ValueOutput} / {@link ValueInput}；单测与物品 {@code CompoundTag} 走保留的标签重载。</p>
 */
public final class BeIo {
    private BeIo() {
    }

    /** 写存档：业务把要写的键写在这里。 */
    public interface Body {
        void write(StoreWriter writer);
    }

    /** 读存档：业务把要读的键读在这里。 */
    public interface Loader {
        void read(StoreReader reader);
    }

    /** 26.1 方块实体写入口：直接包装 {@link ValueOutput}。 */
    public static void write(ValueOutput out, Body body) {
        body.write(new ValueOutputWriter(out));
    }

    /** 26.1 方块实体读入口：直接包装 {@link ValueInput}。 */
    public static void read(ValueInput in, Loader loader) {
        loader.read(new ValueInputReader(in));
    }

    /** 保留 {@code CompoundTag} 入口（供物品组件读写与单测调用）。 */
    public static void write(CompoundTag out, HolderLookup.Provider registries, Body body) {
        body.write(new CompoundWriter(out, registries));
    }

    /** 保留 {@code CompoundTag} 入口（供物品组件读写与单测调用）。 */
    public static void read(CompoundTag in, HolderLookup.Provider registries, Loader loader) {
        loader.read(new CompoundReader(in, registries));
    }
}
