package cn.blockforge.orient.framework.platform.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** 独立内嵌前置的唯一初始化入口。作者：mryt233 */
@Mod("orient_framework")
public final class ForgeFrameworkRuntime {
    public ForgeFrameworkRuntime() { ForgeFrameworkPlatform.install(FMLJavaModLoadingContext.get().getModEventBus()); }
}
