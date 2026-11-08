/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api.callback;
import net.minecraft.world.phys.HitResult;
import snownee.jade.api.Accessor;
@FunctionalInterface
public interface JadeRayTraceCallback {
    Accessor<?> onRayTrace(HitResult hitResult, Accessor<?> accessor, Accessor<?> originalAccessor);
}
