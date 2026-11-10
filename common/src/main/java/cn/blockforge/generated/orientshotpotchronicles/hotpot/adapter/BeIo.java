// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * 方块实体存读档的版本适配入口：业务只写「写什么 / 读什么」，标签怎么读写由这里决定。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。1.21.1 的实现是 {@link CompoundWriter} /
 * {@link CompoundReader}。26.x 直接包装 {@code ValueOutput} / {@code ValueInput}；
 * 只有保留 CompoundTag 的入口才借 TagValueOutput / TagValueInput 桥接；1.20.1 去掉
 * {@code HolderLookup.Provider} 参数。</p>
 *
 * <p>只包「读写形状」，不包「机制」：{@code saveAdditional} / {@code loadAdditional} 的方法头签名、
 * 「什么时候同步」都由各版本和 3F 的 {@code SyncedBlockEntity} 负责，这里不碰。</p>
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

    /** 1.21.1：直接往传入的 {@code CompoundTag} 里写。 */
    public static void write(CompoundTag out, HolderLookup.Provider registries, Body body) {
        body.write(new CompoundWriter(out, registries));
    }

    /** 1.21.1：直接从传入的 {@code CompoundTag} 读。 */
    public static void read(CompoundTag in, HolderLookup.Provider registries, Loader loader) {
        loader.read(new CompoundReader(in, registries));
    }
}
