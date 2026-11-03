package cn.blockforge.orient.framework.loader;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 模块宿主：声明 → 注册（平台窗口）→ 运行时 → 客户端 → 重载。
 * 声明只采集一次并缓存；构造时按 dependsOn 拓扑排序；缺依赖/重复 id/环 一律失败即停。
 * 作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 W6）：每个模组各建一个宿主，以前 dependsOn 只能写同一个宿主里的模块。
 * 现在所有宿主共用一份全局声明表：dependsOn 可以写<b>别的模组里已经建好的模块</b>（例如面条依赖火锅），
 * 模组之间的先后由加载器的依赖顺序保证（fabric.mod.json depends / neoforge.mods.toml ordering=AFTER）；
 * 跨模组的模块 id 也不能重复。</p>
 */
public final class ModuleHost {
    /** 所有宿主已声明的模块 id（跨模组依赖和重复检查用）。 */
    private static final Set<String> GLOBAL = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final List<ContentModule> ordered;
    private final Map<String, ModuleDeclaration> declarations;
    private final Map<ContentModule, ModuleDeclaration> declarationByModule = new IdentityHashMap<>();

    public ModuleHost(List<ContentModule> modules) {
        Map<String, ContentModule> moduleById = new LinkedHashMap<>();
        Map<String, ModuleDeclaration> collected = new LinkedHashMap<>();
        for (ContentModule module : modules) {
            ModuleDeclaration declaration = module.declaration();
            if (declaration == null || declaration.modId() == null || declaration.modId().isBlank()) {
                throw new IllegalStateException("content module declaration missing modId");
            }
            if (moduleById.putIfAbsent(declaration.modId(), module) != null
                || GLOBAL.contains(declaration.modId())) {
                throw new IllegalStateException("duplicate content module id: " + declaration.modId());
            }
            collected.put(declaration.modId(), declaration);
            declarationByModule.put(module, declaration);
            RuntimeStats.get().modulePhase(declaration.modId(), "declaration");
        }
        this.declarations = collected;
        this.ordered = sort(moduleById);
        for (String id : moduleById.keySet()) {
            if (!GLOBAL.add(id)) {
                throw new IllegalStateException("duplicate content module id: " + id);
            }
        }
    }

    /** 只给单元测试用：清空全局声明表（同一个 JVM 里多次建宿主）。 */
    static void resetForTests() {
        GLOBAL.clear();
    }

    /** 某个模块是否已在任意宿主里声明（跨模组可选联动时查询用）。 */
    public static boolean isDeclared(String moduleId) {
        return GLOBAL.contains(moduleId);
    }

    /** 声明缓存（只读）。 */
    public List<ModuleDeclaration> declarations() {
        return List.copyOf(declarations.values());
    }

    public List<String> order() {
        List<String> ids = new ArrayList<>(ordered.size());
        for (ContentModule module : ordered) {
            ids.add(declarationByModule.get(module).modId());
        }
        return List.copyOf(ids);
    }

    private List<ContentModule> sort(Map<String, ContentModule> moduleById) {
        List<ContentModule> out = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> done = new HashSet<>();
        for (String id : moduleById.keySet()) {
            visit(id, moduleById, visiting, done, out);
        }
        return List.copyOf(out);
    }

    private void visit(String id, Map<String, ContentModule> moduleById,
                       Set<String> visiting, Set<String> done, List<ContentModule> out) {
        if (done.contains(id)) {
            return;
        }
        if (!visiting.add(id)) {
            throw new IllegalStateException("content module dependency cycle at " + id);
        }
        for (String dependency : declarationByModule.get(moduleById.get(id)).dependsOn()) {
            if (moduleById.containsKey(dependency)) {
                visit(dependency, moduleById, visiting, done, out);
            } else if (!GLOBAL.contains(dependency)) {
                // 别的模组里的模块必须先建好宿主：检查加载器依赖顺序（depends / ordering=AFTER）
                throw new IllegalStateException("missing content module dependency: " + id + " -> " + dependency
                    + " (not declared in this host or any host created earlier)");
            }
        }
        visiting.remove(id);
        done.add(id);
        out.add(moduleById.get(id));
    }

    public void registerAll() {
        for (ContentModule module : ordered) {
            RuntimeStats.get().modulePhase(declarationByModule.get(module).modId(), "register");
            module.register(Framework.platform().registration());
        }
    }

    public void runtime(RuntimeHooks hooks) {
        for (ContentModule module : ordered) {
            RuntimeStats.get().modulePhase(declarationByModule.get(module).modId(), "runtime");
            module.runtime(hooks);
        }
    }

    public void client(ClientHooks hooks) {
        for (ContentModule module : ordered) {
            RuntimeStats.get().modulePhase(declarationByModule.get(module).modId(), "client");
            module.client(hooks);
        }
    }

    /** 可重复调用任意次。 */
    public void reload() {
        for (ContentModule module : ordered) {
            RuntimeStats.get().modulePhase(declarationByModule.get(module).modId(), "reload");
            module.onReload();
        }
    }
}