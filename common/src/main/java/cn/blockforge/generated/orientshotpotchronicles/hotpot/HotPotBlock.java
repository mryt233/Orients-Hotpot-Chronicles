package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

/** 独立摆放的九宫格火锅；坐上灶台后由 {@link StoveBlock} 承载。 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class HotPotBlock extends Block implements EntityBlock {
    private static final VoxelShape BODY = Block.box(1, 0, 1, 15, 5, 15);
    private static final VoxelShape RIM_NORTH = Block.box(0, 5, 0, 16, 7, 1.5);
    private static final VoxelShape RIM_SOUTH = Block.box(0, 5, 14.5, 16, 7, 16);
    private static final VoxelShape RIM_WEST = Block.box(0, 5, 1.5, 1.5, 7, 14.5);
    private static final VoxelShape RIM_EAST = Block.box(14.5, 5, 1.5, 16, 7, 14.5);
    public static final VoxelShape POT_SHAPE = Shapes.or(
        BODY, RIM_NORTH, RIM_SOUTH, RIM_WEST, RIM_EAST).optimize();

    /** 保留旧存档使用的布尔状态，同时以 SOUP 记录具体汤底。 */
    public static final BooleanProperty HAS_SOUP = BooleanProperty.create("has_soup");
    public static final EnumProperty<SoupVisual> SOUP = EnumProperty.create("soup", SoupVisual.class);
    public static final BooleanProperty BOILING = BooleanProperty.create("boiling");
    /** 两只锅耳朵所在的轴：放下时耳朵在玩家左右两侧，不对着玩家。 */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /**
     * 下面是篝火 / 灵魂篝火：整口锅往下沉 {@link #CAMPFIRE_DROP} 像素，锅底坐在篝火顶上（篝火 7 像素高；
     * 0.1.2-alpha 后用户报锅浮在篝火上方，新增）。模型用 tools/gen_pot_on_campfire.py 生成的下沉副本，
     * 汤面 / 菜 / 热气按 StoveBlock.liftOf 一起下沉。
     */
    public static final BooleanProperty ON_CAMPFIRE = BooleanProperty.create("on_campfire");
    // 第一版下沉 9 像素，但原版篝火火苗 17 像素高，会从锅底穿进锅里、锅也不好挖（用户报）；
    // 改成不下沉，模型里加四根铁支架踩到篝火上（tools/gen_pot_on_campfire.py）。保留常量和 liftOf 的分支，要调高度改这里。
    public static final double CAMPFIRE_DROP = 4.0;   // 2026-10-26 用户：篝火上的锅调矮（火苗尖在汤面下面；tools/gen_pot_on_campfire.py 的 DROP 一致）
    private static final VoxelShape POT_SHAPE_LOW = POT_SHAPE.move(0, -CAMPFIRE_DROP / 16.0, 0);

    public static boolean campfireBelow(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.CAMPFIRES);
    }

    private static VoxelShape shapeOf(BlockState state) {
        return state.getValue(ON_CAMPFIRE) ? POT_SHAPE_LOW : POT_SHAPE;
    }

    private final HotPotMaterial material;

    public HotPotBlock(HotPotMaterial material) {
        super(settingsFor(material));
        this.material = material;
        registerDefaultState(defaultBlockState()
            .setValue(HAS_SOUP, false)
            .setValue(SOUP, SoupVisual.NONE)
            .setValue(BOILING, false)
            .setValue(AXIS, Direction.Axis.X)
            .setValue(ON_CAMPFIRE, false));
    }

    /** 玩家朝向转 90° 就是耳朵的方向：面朝南北时耳朵在东西两侧，面朝东西时在南北两侧。 */
    public static Direction.Axis handleAxisFor(Direction playerFacing) {
        return playerFacing.getClockWise().getAxis();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, handleAxisFor(ctx.getHorizontalDirection()))
            .setValue(ON_CAMPFIRE, campfireBelow(ctx.getLevel(), ctx.getClickedPos()));
    }

    public HotPotMaterial material() {
        return material;
    }

    /**
     * mryt233 改（2026-11-05）：锅统一硬度 2.0、抗性 6.0（12 口锅，铜铁一样），挖掘工具见
     * data/minecraft/tags/block/mineable/pickaxe.json。
     *
     * <p>以前铜锅照铜块 3.0、铁锅照铁块 5.0，并且强制要求正确工具：铁锅空手挖极慢，而且
     * 挖不下时会走 {@link #playerWillDestroy} 的「撒出食材」分支。现在不强制工具，镐子只加速，
     * 空手也能把锅挖下来，整锅（汤底 + 食材）由战利品表的 copy_components 原样打包，一件不丢。</p>
     */
    public static BlockBehaviour.Properties settingsFor(HotPotMaterial material) {
        return BlockBehaviour.Properties.of()
            .mapColor(material.mapColor())
            .instrument(material.instrument())
            .strength(2.0F, 6.0F)
            .sound(material.sounds())
            .noOcclusion()
            .dynamicShape();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HotPotBlockEntity(pos, state);
    }

    /** 直接把锅放在灶台上方时，转换为灶台同格的组合状态。 */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.isClientSide || !StoveBlock.isStove(world.getBlockState(pos.below()))) {
            return;
        }
        // mryt233 改（2026-11-07）：看 attachPot 的返回值——只有真装上了才读锅内容、拆掉这只独立的锅。
        if (world.getBlockEntity(pos.below()) instanceof StoveBlockEntity stove
            && stove.attachPot(material, state.getValue(AXIS))) {
            stove.loadPotContents(stack);
            world.removeBlock(pos, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world,
                                  BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world,
                                           BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    /** 下面的方块变了（篝火放上 / 拆掉）：跟着沉下去或抬回来。 */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.setValue(ON_CAMPFIRE, neighborState.is(net.minecraft.tags.BlockTags.CAMPFIRES));
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SOUP, SOUP, BOILING, AXIS, ON_CAMPFIRE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof HotPotBlockEntity be)) {
            return InteractionResult.PASS;
        }
        // 空手潜行右键 = 把锅连汤带食材拿进背包，和灶台上拆锅一样（用户报：篝火上的锅 Shift+右键拿不下来；新增）
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                ItemStack pot = be.toPotItem(material);
                HotPotCriterion.fire(player, "carry_pot");
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (!player.getInventory().add(pot)) {
                    player.drop(pot, false);
                }
                world.playSound((Player) null, pos, SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS, 0.25F, 0.75F);
            }
            return InteractionResult.SUCCESS;
        }
        int bigCell = HotPotInteraction.cellFromHit(be, player, pos, hit, HotPotInteraction.STANDALONE_INNER_MIN, HotPotInteraction.STANDALONE_INNER_SIZE);
        if (world.isClientSide) {
            HotPotInteraction.clientChopsticksAim.accept(be, bigCell);   // 空手也捞看到的那样（2026-10-26）
        }
        if (!world.isClientSide && !be.collectShown(player, bigCell)) {
            player.displayClientMessage(be.statusText(), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof HotPotBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 其他模组的东西先处理，比如空碗要先盛汤（见 HotPotHooks）
        ItemInteractionResult hooked = HotPotHooks.blockUse(world, player, hand, stack, pos, state, hit);
        if (hooked != null) {
            return hooked;
        }
        return HotPotInteraction.useItem(be, stack, world, pos, player, hand, hit,
            HotPotInteraction.STANDALONE_INNER_MIN, HotPotInteraction.STANDALONE_INNER_SIZE);
    }

    /** 手里拿的是盘子（点锅就是把盘里的菜倒进去）。 */
    public static boolean isPlate(ItemStack stack) {
        if (stack.is(Items.BOWL)) {
            // 装着菜的碗也是整碗倒进锅（r99）；空碗是盛
            return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyStream().findAny().isPresent();
        }
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof PlateBlock;
    }

    /**
     * 鼠标中键：创造模式复制整锅（汤底 + 食材，和 Shift+右键端下来的一样；用户 2026-10-26 报：端下来的锅放地上，中键只复制空锅）。
     * 生存模式照原版在物品栏里找同样的锅（给带内容的锅就对不上普通的空锅）。新增
     */
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        Player player = PlateHolderBlock.pickPlayer.get();
        if (player != null && player.isCreative() && world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            return be.toPotItem(material);
        }
        return super.getCloneItemStack(world, pos, state);
    }

    // 以前下面是空气就整锅掉成物品（像脚手架），悬空摆的锅一碰邻居更新就没了；
    // 2026-10-26 用户报 bug，去掉：锅和普通方块一样能悬空放。调整

    /**
     * 正常挖下来时，汤底和食材由战利品表（copy_components）打包进锅物品，不再散落一地。
     * 只有挖不下锅（{@link #settingsFor} 里没有强制工具，本模组的锅现在走不到这一步；留着给以后
     * 万一加上工具限制的锅）的情况，锅本身不掉，才把食材撒出来，免得白白消失。
     * mryt233 改（2026-11-05）：锅不再要求正确工具，空手挖也算「挖得下来」，整锅打包。
     */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && player != null && !player.isCreative() && !player.hasCorrectToolForDrops(state)
            && world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            be.dropContents();
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == HotPotContent.HOT_POT_BE) {
            return (w, p, s, be) -> HotPotBlockEntity.tick(w, p, s, (HotPotBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            return be.comparatorLevel();
        }
        return 0;
    }
}