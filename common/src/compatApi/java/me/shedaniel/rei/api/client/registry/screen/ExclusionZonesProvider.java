package me.shedaniel.rei.api.client.registry.screen;
import me.shedaniel.math.Rectangle;
import java.util.Collection;
/** 编译期签名，不进入分发包。作者：mryt233 */
@FunctionalInterface
public interface ExclusionZonesProvider<T> {
    Collection<Rectangle> provide(T screen);
}
