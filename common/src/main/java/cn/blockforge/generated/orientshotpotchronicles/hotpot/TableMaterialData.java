// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 桌子与备菜架的可换材质物品组件载荷：记录桌面（{@code top}）、底座（{@code base}）、
 * 迁移保留高桌中圈（{@code legacyRing}）与旧预设防刷锁（{@code topLegacy} / {@code baseLegacy}）。
 * 未知第三方模组方块 ID 原样持久保存不丢失。作者：mryt233
 */
public record TableMaterialData(
    String top,
    String base,
    String legacyRing,
    boolean topLegacy,
    boolean baseLegacy
) {
    public static final TableMaterialData EMPTY = new TableMaterialData("", "", "", false, false);

    public static final Codec<TableMaterialData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("top", "").forGetter(TableMaterialData::top),
            Codec.STRING.optionalFieldOf("base", "").forGetter(TableMaterialData::base),
            Codec.STRING.optionalFieldOf("legacy_ring", "").forGetter(TableMaterialData::legacyRing),
            Codec.BOOL.optionalFieldOf("top_legacy", false).forGetter(TableMaterialData::topLegacy),
            Codec.BOOL.optionalFieldOf("base_legacy", false).forGetter(TableMaterialData::baseLegacy)
        ).apply(instance, TableMaterialData::new));

    public static final StreamCodec<ByteBuf, TableMaterialData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, TableMaterialData::top,
        ByteBufCodecs.STRING_UTF8, TableMaterialData::base,
        ByteBufCodecs.STRING_UTF8, TableMaterialData::legacyRing,
        ByteBufCodecs.BOOL, TableMaterialData::topLegacy,
        ByteBufCodecs.BOOL, TableMaterialData::baseLegacy,
        TableMaterialData::new);

    public TableMaterialData {
        top = safeId(top);
        base = safeId(base);
        legacyRing = safeId(legacyRing);
    }

    private static String safeId(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public boolean hasAny() {
        return !top.isEmpty() || !base.isEmpty() || !legacyRing.isEmpty() || topLegacy || baseLegacy;
    }

    public static TableMaterialData fromLegacyPreset(TableMaterials.Preset preset, boolean tall) {
        Objects.requireNonNull(preset, "preset");
        if (tall) {
            return new TableMaterialData(preset.topId(), preset.tallWallId(), preset.tallRingId(), true, true);
        }
        return new TableMaterialData(preset.topId(), preset.lowBaseId(), "", true, true);
    }

    public TableMaterialData withTop(String topId, boolean legacy) {
        return new TableMaterialData(topId, base, legacyRing, legacy, baseLegacy);
    }

    public TableMaterialData withBase(String baseId, boolean legacy) {
        return new TableMaterialData(top, baseId, "", topLegacy, legacy);
    }
}
