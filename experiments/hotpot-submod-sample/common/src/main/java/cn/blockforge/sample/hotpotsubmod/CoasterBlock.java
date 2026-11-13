// 作者：mryt233
package cn.blockforge.sample.hotpotsubmod;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 自定义桌面竹垫方块，借 PlateBlock 与 TableBlock 接口接入 PlateHolderBlock 桌面体系。 */
public final class CoasterBlock extends PlateBlock {
    public CoasterBlock() {
        super("bamboo_coaster", PlateSize.S1X1, MapColor.WOOD, SoundType.BAMBOO);
    }

    @Override
    public boolean holdsFood(PlateHolderBlockEntity.Placed placed) {
        return false;
    }

    @Override
    public boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
        return false;
    }

    @Override
    public double tableHeight(PlateHolderBlockEntity.Placed placed) {
        return 2.0;
    }
}
