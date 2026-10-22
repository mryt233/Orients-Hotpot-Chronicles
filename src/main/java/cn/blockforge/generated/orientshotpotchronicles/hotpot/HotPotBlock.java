package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** 独立摆放的九宫格火锅；坐上灶台后由 {@link StoveBlock} 承载。 */
public class HotPotBlock extends Block implements BlockEntityProvider {
    private static final VoxelShape BODY = Block.createCuboidShape(1, 0, 1, 15, 5, 15);
    private static final VoxelShape RIM_NORTH = Block.createCuboidShape(0, 5, 0, 16, 7, 1.5);
    private static final VoxelShape RIM_SOUTH = Block.createCuboidShape(0, 5, 14.5, 16, 7, 16);
    private static final VoxelShape RIM_WEST = Block.createCuboidShape(0, 5, 1.5, 1.5, 7, 14.5);
    private static final VoxelShape RIM_EAST = Block.createCuboidShape(14.5, 5, 1.5, 16, 7, 14.5);
    public static final VoxelShape POT_SHAPE = VoxelShapes.union(
        BODY, RIM_NORTH, RIM_SOUTH, RIM_WEST, RIM_EAST).simplify();

    /** 保留旧存档使用的布尔状态，同时以 SOUP 记录具体汤底。 */
    public static final BooleanProperty HAS_SOUP = BooleanProperty.of("has_soup");
    public static final EnumProperty<SoupVisual> SOUP = EnumProperty.of("soup", SoupVisual.class);
    public static final BooleanProperty BOILING = BooleanProperty.of("boiling");
    /** 两只锅耳朵所在的轴：放下时耳朵在玩家左右两侧，不对着玩家。 */
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    private final HotPotMaterial material;

    public HotPotBlock(HotPotMaterial material) {
        super(settingsFor(material));
        this.material = material;
        setDefaultState(getDefaultState()
            .with(HAS_SOUP, false)
            .with(SOUP, SoupVisual.NONE)
            .with(BOILING, false)
            .with(AXIS, Direction.Axis.X));
    }

    /** 玩家朝向转 90° 就是耳朵的方向：面朝南北时耳朵在东西两侧，面朝东西时在南北两侧。 */
    public static Direction.Axis handleAxisFor(Direction playerFacing) {
        return playerFacing.rotateYClockwise().getAxis();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(AXIS, handleAxisFor(ctx.getHorizontalPlayerFacing()));
    }

    public HotPotMaterial material() {
        return material;
    }

    public static AbstractBlock.Settings settingsFor(HotPotMaterial material) {
        // 硬度、工具要求同原版铜块 / 铁块；挖掘工具与石镐要求见 data/minecraft/tags/block/
        return AbstractBlock.Settings.create()
            .mapColor(material.mapColor())
            .instrument(material.instrument())
            .requiresTool()
            .strength(material.hardness(), 6.0F)
            .sounds(material.sounds())
            .nonOpaque()
            .dynamicBounds();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HotPotBlockEntity(pos, state);
    }

    /** 直接把锅放在灶台上方时，转换为灶台同格的组合状态。 */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         net.minecraft.entity.LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient || !StoveBlock.isStove(world.getBlockState(pos.down()))) {
            return;
        }
        if (world.getBlockEntity(pos.down()) instanceof StoveBlockEntity stove
            && !world.getBlockState(pos.down()).get(StoveBlock.POT).hasPot()) {
            stove.attachPot(material, state.get(AXIS));
            stove.loadPotContents(stack);
            world.removeBlock(pos, false);
        }
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                                         BlockPos pos, ShapeContext context) {
        return POT_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.BlockView world,
                                           BlockPos pos, ShapeContext context) {
        return POT_SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HAS_SOUP, SOUP, BOILING, AXIS);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof HotPotBlockEntity be)) {
            return ActionResult.PASS;
        }
        int bigCell = HotPotInteraction.bigCellFromHit(pos, hit);
        int slot = be.firstCookedSlotInCell(bigCell);
        if (!world.isClient && !be.collectCookedSlot(player, slot)) {
            player.sendMessage(be.statusText(), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof HotPotBlockEntity be)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int bigCell = HotPotInteraction.bigCellFromHit(pos, hit);
        int cookedSlot = be.firstCookedSlotInCell(bigCell);

        if (isPlate(stack)) {
            if (!world.isClient) {
                be.pourPlate(player, hand);
            }
            return ItemActionResult.SUCCESS;
        }
        if (be.canScoopWith(stack)) {
            if (!world.isClient) {
                be.scoopSoup(player, hand);
            }
            return ItemActionResult.SUCCESS;
        }
        if (HotPotInteraction.isServingTool(stack)) {
            if (stack.isOf(HotPotContent.CHOPSTICKS)) {
                // 筷子上夹着盘子里的菜：放进准星对着的那一格；空筷子开面板（捞取交给碗）
                if (!world.isClient) {
                    if (!Chopsticks.held(stack).isEmpty()) {
                        be.insertHeld(player, stack, be.firstEmptySlotInCell(bigCell));
                    } else {
                        be.useWithChopsticks(player);
                    }
                }
                return ItemActionResult.SUCCESS;
            }
            if (!world.isClient && !be.collectCookedSlot(player, cookedSlot)) {
                player.sendMessage(net.minecraft.text.Text.translatable(
                    HotPotBlockEntity.key("msg.no_cooked_here")), true);
            }
            return ItemActionResult.SUCCESS;
        }
        if (SoupBase.fromStack(stack) != null) {
            if (!world.isClient && !be.pourSoup(player, hand)) {
                return ItemActionResult.FAIL;
            }
            return ItemActionResult.SUCCESS;
        }
        if (HotPotCooking.acceptable(world, stack)) {
            if (!world.isClient && !be.insertIngredient(player, hand, be.firstEmptySlotInCell(bigCell))) {
                return ItemActionResult.FAIL;
            }
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 手里拿的是盘子（点锅就是把盘里的菜倒进去）。 */
    public static boolean isPlate(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.item.BlockItem item && item.getBlock() instanceof PlateBlock;
    }

    /** 支撑消失时，整锅（连汤带食材）打包成一个锅物品掉落。 */
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                  BlockPos sourcePos, boolean notify) {
        if (world.isClient || !world.isAir(pos.down())) {
            return;
        }
        ItemStack drop = world.getBlockEntity(pos) instanceof HotPotBlockEntity be
            ? be.toPotItem(material) : new ItemStack(this);
        world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
        Block.dropStack(world, pos, drop);
    }

    /**
     * 正常挖下来时，汤底和食材由战利品表（copy_components）打包进锅物品，不再散落一地。
     * 只有空手等挖不下锅的情况，锅本身不掉，才把食材撒出来，免得白白消失。
     */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player != null && !player.isCreative() && !player.canHarvest(state)
            && world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            be.dropContents();
        }
        return super.onBreak(world, pos, state, player);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == HotPotContent.HOT_POT_BE) {
            return (w, p, s, be) -> HotPotBlockEntity.tick(w, p, s, (HotPotBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof HotPotBlockEntity be) {
            return be.comparatorLevel();
        }
        return 0;
    }
}
