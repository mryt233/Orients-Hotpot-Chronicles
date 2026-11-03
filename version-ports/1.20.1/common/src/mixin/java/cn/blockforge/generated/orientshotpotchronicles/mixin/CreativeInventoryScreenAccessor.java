package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 读当前选中的分类页、重新选中某一页（子类按钮切换后刷新物品列表用）。作者：mryt233*/
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeInventoryScreenAccessor {
    @Accessor("selectedTab")
    static CreativeModeTab getSelectedTab() {
        throw new AssertionError();
    }

    @Invoker("selectTab")
    void invokeSetSelectedTab(CreativeModeTab group);
}
