// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.nbt.CompoundTag;

/**
 * 方块实体存读档的版本适配入口：业务只写「写什么 / 读什么」，标签怎么读写由这里决定。
 *
 * <p>mryt233 改（1.20.1 适配）：1.20.1 直接使用 {@code CompoundTag}，无 {@code HolderLookup.Provider} 参数。</p>
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

    /** 1.20.1：直接往传入的 {@code CompoundTag} 里写。 */
    public static void write(CompoundTag out, Body body) {
        body.write(new CompoundWriter(out));
    }

    /** 1.20.1：直接从传入的 {@code CompoundTag} 读。 */
    public static void read(CompoundTag in, Loader loader) {
        loader.read(new CompoundReader(in));
    }
}
