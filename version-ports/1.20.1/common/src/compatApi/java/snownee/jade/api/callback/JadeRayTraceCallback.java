/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api.callback;
import net.minecraft.world.phys.HitResult;
import snownee.jade.api.Accessor;
@FunctionalInterface
public interface JadeRayTraceCallback {
    Accessor<?> onRayTrace(HitResult hitResult, Accessor<?> accessor, Accessor<?> originalAccessor);
}
