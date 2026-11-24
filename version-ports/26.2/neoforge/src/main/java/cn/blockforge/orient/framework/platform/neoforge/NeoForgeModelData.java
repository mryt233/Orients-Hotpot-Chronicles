// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import net.neoforged.neoforge.model.data.ModelProperty;

/** NeoForge 区块渲染线程安全模型数据键，供 Common 渲染快照与客户端模型共用。作者：mryt233 */
public final class NeoForgeModelData {
    public static final ModelProperty<Object> SNAPSHOT = new ModelProperty<>();

    private NeoForgeModelData() {
    }
}
