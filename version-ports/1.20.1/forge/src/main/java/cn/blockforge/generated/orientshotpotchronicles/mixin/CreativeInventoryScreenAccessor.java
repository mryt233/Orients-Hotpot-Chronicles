package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 读当前选中的分类页、重新选中某一页（子类按钮切换后刷新物品列表用）。*/
@Mixin(CreativeInventoryScreen.class)
public interface CreativeInventoryScreenAccessor {
    @Accessor("selectedTab")
    static ItemGroup getSelectedTab() {
        throw new AssertionError();
    }

    @Invoker("setSelectedTab")
    void invokeSetSelectedTab(ItemGroup group);
}
