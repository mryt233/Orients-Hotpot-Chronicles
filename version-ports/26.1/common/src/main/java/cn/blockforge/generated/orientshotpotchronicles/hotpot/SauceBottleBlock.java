package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;

/**
 * 摆在桌上的芝麻酱 / 蚝油瓶（r146a，2026-11-08 用户：两种调料要能摆上桌，只做摆设）。
 * <p>
 * 和瓶子（{@link DrinkBlock}）、碗、桌上的竹筷一样只带模型：真正摆在哪由 {@link PlateHolderBlock}
 * 的「盘子格」记着，这个方块只管模型（{@code kind} 决定画哪种瓶子）和名字。
 * <p>
 * <b>有意不往 {@link PlateSize} 枚举里加常量</b>（理由同 {@link ChopstickRestBlock}）：
 * {@link HotPotContent} 用 {@code PlateSize.values()} 循环注册盘子，枚举里多一个值就会多注册一件本不该存在的盘子。
 * 这里直接复用 {@link PlateSize#S1X1} 的 5×5 像素占地，模型也做成 5×5，正好和同一格里的蘸料碗（7×7）配得上。
 * <p>
 * 整瓶物品存在 {@link PlateHolderBlockEntity.Placed#self} 里，拿起来 / 挖掉时原样还回去
 * （mryt233 改 2026-11-09，r146c：每瓶次数已去掉，不再有 used 组件）。
 * 作者：mryt233 */
public class SauceBottleBlock extends PlateBlock {
    /** 摆的是哪种调料瓶；方块状态取值见 {@link SauceBottleKind}。 */
    public static final EnumProperty<SauceBottleKind> KIND =
        EnumProperty.create("kind", SauceBottleKind.class);

    /** mryt233 改（2026-11-09，26.1 移植）：26.x 的方块注册走框架给的带注册键 Properties。 */
    public SauceBottleBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super("sauce_bottle", PlateSize.S1X1, MapColor.COLOR_BROWN, SoundType.GLASS, properties);
        registerDefaultState(defaultBlockState().setValue(KIND, SauceBottleKind.SESAME_PASTE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(KIND);
    }

    /** 摆着的那瓶有多高（像素）：取自它摆的是哪种瓶子，选中框和碰撞按它算。 */
    public static double heightOf(SauceBottleKind kind) {
        return kind == null ? 8.0 : kind.bottleHeight();
    }

    // mryt233 改：调料瓶桌面高度、状态与不可架筷收口到 TableBlock 接口。
    @Override
    public boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
        return false;
    }

    @Override
    public double tableHeight(PlateHolderBlockEntity.Placed placed) {
        return heightOf(SauceBottleItem.kindOf(placed.self));
    }

    @Override
    public BlockState modelState(PlateHolderBlockEntity.Placed placed) {
        SauceBottleKind kind = SauceBottleItem.kindOf(placed.self);
        return super.modelState(placed).setValue(KIND, kind == null ? SauceBottleKind.SESAME_PASTE : kind);
    }
}
