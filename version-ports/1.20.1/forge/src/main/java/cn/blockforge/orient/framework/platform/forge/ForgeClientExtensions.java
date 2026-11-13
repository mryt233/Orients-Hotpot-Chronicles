// 作者：mryt233
package cn.blockforge.orient.framework.platform.forge;

import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.20.1 Forge 没有 RegisterClientExtensionsEvent，由内容侧 ItemClientExtensionsMixin 读取这里的物品扩展。
 */
public final class ForgeClientExtensions {
    private static final Map<Item, IClientItemExtensions> EXTENSIONS = new ConcurrentHashMap<>();

    private ForgeClientExtensions() { }

    public static void register(Item item, IClientItemExtensions extension) {
        EXTENSIONS.put(Objects.requireNonNull(item), Objects.requireNonNull(extension));
    }

    public static IClientItemExtensions extension(Item item) {
        return EXTENSIONS.get(item);
    }
}
