package me.shedaniel.rei.api.client.registry.screen;
/** 编译期签名，不进入分发包。作者：mryt233 */
public interface ExclusionZones {
    <T> void register(Class<? extends T> type, ExclusionZonesProvider<? extends T> provider);
}
