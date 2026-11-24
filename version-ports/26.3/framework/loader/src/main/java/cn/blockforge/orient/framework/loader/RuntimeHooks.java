package cn.blockforge.orient.framework.loader;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

/** 运行时钩子（服务端/游戏初始化之后）；平台负责时机，内容模块只挂回调。作者：mryt233 */
public interface RuntimeHooks {
    default RuntimeEvents events() {
        return Framework.platform().events();
    }

    /** 服务端启动完成（专用服或单人世界）。 */
    void onServerStarted(Runnable callback);
}