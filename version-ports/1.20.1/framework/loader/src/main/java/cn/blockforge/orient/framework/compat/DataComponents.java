package cn.blockforge.orient.framework.compat;

import com.mojang.serialization.Codec;

/** 原版物品附属数据的 1.20.1 键名。作者：mryt233 */
public final class DataComponents {
    public static final ItemDataKey<ItemContainerContents> CONTAINER = new ItemDataKey<>("container", ItemContainerContents.CODEC);
    public static final ItemDataKey<CustomModelData> CUSTOM_MODEL_DATA = new ItemDataKey<>("CustomModelData",
        Codec.INT.xmap(CustomModelData::new, CustomModelData::value));
    private DataComponents() { }
}
