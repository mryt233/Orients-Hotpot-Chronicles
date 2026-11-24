package com.terraformersmc.modmenu.api;

/** Mod Menu 入口接口的编译期签名桩，不进分发包。作者：mryt233 */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> null;
    }
}
