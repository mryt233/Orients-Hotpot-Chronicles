package cn.blockforge.orient.framework.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

/** 锅内容保持旧移植版的原始 compound 结构。作者：mryt233 */
public final class CustomData {
    public static final Codec<CustomData> CODEC = CompoundTag.CODEC.xmap(CustomData::of, CustomData::copyTag);
    public static final CustomData EMPTY = of(new CompoundTag());
    private final CompoundTag tag;
    private CustomData(CompoundTag tag) { this.tag = tag.copy(); }
    public static CustomData of(CompoundTag tag) { return new CustomData(tag); }
    public CompoundTag copyTag() { return tag.copy(); }
    public boolean isEmpty() { return tag.isEmpty(); }
}
