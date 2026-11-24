// 作者：mryt233
package cn.blockforge.orient.framework.game;

import org.jetbrains.annotations.Nullable;

/**
 * 供可换材质方块实体实现的渲染快照接口，保证区块构建工作线程安全读取不可变材质状态。
 * 作者：mryt233
 */
public interface MaterialRenderHost {
    /** 返回纯不可变快照对象（如材质 ID 槽位记录），供客户端烘焙模型读取。 */
    @Nullable Object materialRenderData();
}
