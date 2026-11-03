package cn.blockforge.orient.framework.loader;

/** 客户端钩子：只在客户端入口调用；平台保证专用服不加载。作者：mryt233 */
public interface ClientHooks {
    /** 客户端初始化完成后执行。 */
    void onClientSetup(Runnable callback);
}