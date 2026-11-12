package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;

/**
 * 直接摆在桌面上的竹筷（连它们垫着的迷你筷架）。
 * <p>
 * 以前竹筷只能 Shift+右键架在碗沿 / 盘沿上；r136b（2026-11-05 用户）起，拿着竹筷 Shift+右键桌面的空白位置，
 * 就能把这一双（连同夹着的菜）直接摆在桌上，渲染器在筷子前端下方画一个迷你筷架把筷尖垫起来。
 * <p>
 * 它不注册成盘子物品，只借盘子格（{@link PlateHolderBlock}）摆；mryt233 改（r146e）：独立占地 9.5×4.5 像素。
 * <b>有意不往 {@link PlateSize} 枚举里加常量</b>——{@link HotPotContent} 用 {@code PlateSize.values()} 循环注册盘子，
 * 枚举里多一个值就会多注册一个本不该存在的盘子（例如 quartz_chopsticks）。
 * 作者：mryt233 */
public class ChopstickRestBlock extends PlateBlock {
    public ChopstickRestBlock() {
        super("bamboo", PlateSize.S1X1, MapColor.SAND, SoundType.BAMBOO_WOOD);
    }

    @Override public double footprintLength() { return 9.5; }
    @Override public double footprintWidth() { return 4.5; }

    // mryt233 改：桌面竹筷的高度、破坏粒子、朝向轴与不可叠架收口到 TableBlock 接口。
    @Override
    public boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
        return false;
    }

    @Override
    public double tableHeight(PlateHolderBlockEntity.Placed placed) {
        return PlateHolderBlockEntity.TABLE_CHOPSTICKS_H
            + (Chopsticks.held(placed.self).isEmpty() ? 0 : PlateHolderBlockEntity.FOOD_H);
    }

    @Override
    public BlockState breakParticleState(PlateHolderBlockEntity.Placed placed) {
        // 迷你筷架没有单独的方块模型 json（由渲染器按代码画），挖掉时的碎屑改用竹板。
        return Blocks.BAMBOO_PLANKS.defaultBlockState();
    }

    @Override
    public void onLoadedOrAdded(PlateHolderBlockEntity.Placed placed, Direction facing) {
        placed.axis = placed.facing.getAxis();
        placed.chopsticksYaw = placed.axis == Direction.Axis.Z ? 135F : 45F;
    }
}
