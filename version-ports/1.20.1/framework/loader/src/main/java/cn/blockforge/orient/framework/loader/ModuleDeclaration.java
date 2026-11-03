package cn.blockforge.orient.framework.loader;

import java.util.List;
import java.util.Set;

/** 内容模块声明：只描述身份与顺序，不产生注册副作用。作者：mryt233 */
public record ModuleDeclaration(String modId, String contentNamespace, Set<String> dependsOn,
                                List<String> configFiles) {
    public ModuleDeclaration {
        dependsOn = Set.copyOf(dependsOn);
        configFiles = List.copyOf(configFiles);
    }
}