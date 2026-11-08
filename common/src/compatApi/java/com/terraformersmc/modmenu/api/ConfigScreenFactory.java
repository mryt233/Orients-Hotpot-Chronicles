package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/** Mod Menu 配置界面工厂的编译期签名桩，不进分发包。作者：mryt233 */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}
