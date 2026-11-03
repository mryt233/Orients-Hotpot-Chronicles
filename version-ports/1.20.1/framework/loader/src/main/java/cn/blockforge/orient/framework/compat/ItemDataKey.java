package cn.blockforge.orient.framework.compat;

import com.mojang.serialization.Codec;
import java.util.Objects;

/** 有类型的物品 NBT 键，不占用原版注册表。作者：mryt233 */
public record ItemDataKey<T>(String name, Codec<T> codec) {
    public ItemDataKey {
        Objects.requireNonNull(name);
        Objects.requireNonNull(codec);
    }
}
