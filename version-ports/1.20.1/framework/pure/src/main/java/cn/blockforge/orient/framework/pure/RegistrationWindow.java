// 作者：mryt233
package cn.blockforge.orient.framework.pure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 注册窗口与排队 ID 预检；不依赖游戏类。 */
public final class RegistrationWindow {
    private final Set<String> closed = new HashSet<>();
    private final Map<String, String> owners = new HashMap<>();
    private boolean allClosed;

    public synchronized void claim(String registry, String id, String owner, boolean present) {
        if (allClosed || closed.contains(registry)) {
            throw RegistrationGuard.failure(registry, id, owner, "closed", null);
        }
        String key = registry + "/" + id;
        String previous = owners.get(key);
        if (present || previous != null) {
            throw RegistrationGuard.failure(registry, id, owner, "declare",
                new IllegalStateException("duplicate registration, previous owner="
                    + (previous == null ? "registry" : previous)));
        }
        owners.put(key, owner);
    }

    public synchronized void close(String registry) {
        closed.add(registry);
    }

    public synchronized void closeAll() {
        allClosed = true;
    }
}
