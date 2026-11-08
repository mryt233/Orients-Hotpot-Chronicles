// mryt233 改：26.1 方块属性由注册器传入。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;

/**
 * 直接摆在桌面上的竹筷（连它们垫着的迷你筷架）。
 * <p>
 * 以前竹筷只能 Shift+右键架在碗沿 / 盘沿上；r136b（2026-11-05 用户）起，拿着竹筷 Shift+右键桌面的空白位置，
 * 就能把这一双（连同夹着的菜）直接摆在桌上，渲染器在筷子前端下方画一个迷你筷架把筷尖垫起来。
 * <p>
 * 它不注册成盘子物品，只借盘子格（{@link PlateHolderBlock}）摆：尺寸借用小碟的 5×5 像素，正好装下一双筷子加迷你筷架。
 * <b>有意不往 {@link PlateSize} 枚举里加常量</b>——{@link HotPotContent} 用 {@code PlateSize.values()} 循环注册盘子，
 * 枚举里多一个值就会多注册一个本不该存在的盘子（例如 quartz_chopsticks）。
 * 作者：mryt233 */
public class ChopstickRestBlock extends PlateBlock {
    public ChopstickRestBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super("bamboo", PlateSize.S1X1, MapColor.SAND, SoundType.BAMBOO_WOOD, properties);
    }
}
