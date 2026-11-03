package cn.blockforge.orient.framework.loader;

/** 运行时钩子（服务端/游戏初始化之后）；平台负责时机，内容模块只挂回调。作者：mryt233 */
public interface RuntimeHooks {
    /** 服务端启动完成（专用服或单人世界）。 */
    void onServerStarted(Runnable callback);
}