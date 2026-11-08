/** 作者：mryt233。Jade 15.10.6 编译期签名，成品与源码包均排除。 */
package snownee.jade.api;
import net.minecraft.resources.ResourceLocation;
public interface IJadeProvider {
    ResourceLocation getUid();
    default int getDefaultPriority() { return 0; }
}
