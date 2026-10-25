// 作者：；1.21.1 NeoForge 移植。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.registries.RegisterEvent;

/** 保留原内容字段，所有注册写入延迟到对应 RegisterEvent。 */
final class NeoRegistrations {
    private static final List<Consumer<RegisterEvent>> ENTRIES = new ArrayList<>();

    private NeoRegistrations() {}

    static <T, V extends T> V queue(Registry<T> registry, Identifier id, V value) {
        ENTRIES.add(event -> event.register(registry.getKey(), id, () -> value));
        return value;
    }

    static <T, V extends T> V queue(Registry<T> registry, RegistryKey<T> key, V value) {
        return queue(registry, key.getValue(), value);
    }

    static void flush(RegisterEvent event) {
        ENTRIES.forEach(entry -> entry.accept(event));
    }
}
