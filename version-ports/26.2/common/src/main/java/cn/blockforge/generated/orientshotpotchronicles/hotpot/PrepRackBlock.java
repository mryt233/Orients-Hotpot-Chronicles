// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 食材架 / 备菜架统一基类（多层落地菜架 6 格、桌面备菜架 4 格、挂架两态 4 格）。
 * 全面复用火锅桌同款换材质架构、{@link TableMaterials} 规则及无 GUI 准星分格直接放取。作者：mryt233
 */
public class PrepRackBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);

    public static final TagKey<Item> RACK_PLATES_TAG = TagKey.create(Registries.ITEM, HotPot.id("rack_plates"));
    public static final TagKey<Item> RACK_HANGABLES_TAG = TagKey.create(Registries.ITEM, HotPot.id("rack_hangables"));

    public enum Kind {
        MULTI_TIER(6),
        DESKTOP(4),
        HANGING(4);

        private final int slots;

        Kind(int slots) {
            this.slots = slots;
        }

        public int slots() {
            return slots;
        }
    }

    public enum Mount implements StringRepresentable {
        WALL("wall"),
        CEILING("ceiling");

        private final String name;

        Mount(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final VoxelShape MULTI_SHAPE_Z = Block.box(1, 0, 2, 15, 16, 14);
    private static final VoxelShape MULTI_SHAPE_X = Block.box(2, 0, 1, 14, 16, 15);
    private static final VoxelShape DESKTOP_SHAPE_Z = Block.box(1, 0, 3, 15, 11, 13);
    private static final VoxelShape DESKTOP_SHAPE_X = Block.box(3, 0, 1, 13, 11, 15);
    private static final VoxelShape HANG_WALL_NORTH = Block.box(1, 7, 11, 15, 15, 16);
    private static final VoxelShape HANG_WALL_SOUTH = Block.box(1, 7, 0, 15, 15, 5);
    private static final VoxelShape HANG_WALL_WEST = Block.box(11, 7, 1, 16, 15, 15);
    private static final VoxelShape HANG_WALL_EAST = Block.box(0, 7, 1, 5, 15, 15);
    private static final VoxelShape HANG_CEILING_Z = Block.box(1, 9, 6, 15, 16, 10);
    private static final VoxelShape HANG_CEILING_X = Block.box(6, 9, 1, 10, 16, 15);

    private final Kind kind;

    public PrepRackBlock(Kind kind, BlockBehaviour.Properties settings) {
        super(settings);
        this.kind = kind;
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.SOUTH)
            .setValue(MOUNT, Mount.WALL));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MOUNT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Mount mount = ctx.getClickedFace() == Direction.DOWN ? Mount.CEILING : Mount.WALL;
        Direction facing = ctx.getClickedFace().getAxis().isHorizontal() && mount == Mount.WALL
            ? ctx.getClickedFace()
            : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing).setValue(MOUNT, mount);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction f = state.getValue(FACING);
        return switch (kind) {
            case MULTI_TIER -> f.getAxis() == Direction.Axis.Z ? MULTI_SHAPE_Z : MULTI_SHAPE_X;
            case DESKTOP -> f.getAxis() == Direction.Axis.Z ? DESKTOP_SHAPE_Z : DESKTOP_SHAPE_X;
            case HANGING -> state.getValue(MOUNT) == Mount.CEILING
                ? (f.getAxis() == Direction.Axis.Z ? HANG_CEILING_Z : HANG_CEILING_X)
                : switch (f) {
                    case NORTH -> HANG_WALL_NORTH;
                    case SOUTH -> HANG_WALL_SOUTH;
                    case WEST -> HANG_WALL_WEST;
                    default -> HANG_WALL_EAST;
                };
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrepRackBlockEntity(pos, state);
    }

    /** 将世界命中坐标转换成局部坐标系 [0..1]（X 向右、Z 向前、Y 向上），按架型精确选择最近子格。 */
    public int slotAt(BlockState state, BlockPos pos, Vec3 hit, @Nullable BlockGetter world) {
        Direction f = state.getValue(FACING);
        double lift = world == null ? 0.0 : TableDecor.liftOf(world, pos, state);
        double dx = hit.x - pos.getX() - 0.5;
        double dz = hit.z - pos.getZ() - 0.5;
        double ly = hit.y - pos.getY() - lift;
        // 旋转回基准朝向（facing=SOUTH：前侧 +Z、左侧 -X）
        double rx;
        double rz;
        switch (f) {
            case NORTH -> { rx = -dx; rz = -dz; }
            case EAST -> { rx = -dz; rz = dx; }
            case WEST -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        double lx = rx + 0.5;
        double lz = rz + 0.5;
        return switch (kind) {
            case MULTI_TIER -> {
                int tier = ly < (6.5 / 16.0) ? 0 : (ly < (12.5 / 16.0) ? 1 : 2);
                int col = lx < 0.5 ? 0 : 1;
                yield tier * 2 + col;
            }
            case DESKTOP -> {
                int row = (ly >= (5.5 / 16.0) || lz < 0.5) ? 1 : 0;
                int col = lx < 0.5 ? 0 : 1;
                yield row * 2 + col;
            }
            case HANGING -> {
                int idx = (int) Math.floor(lx * 4.0);
                yield Math.max(0, Math.min(3, idx));
            }
        };
    }

    public boolean canPlaceItem(Level world, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (kind == Kind.HANGING) {
            return stack.is(RACK_HANGABLES_TAG) || ClothApronItem.isApron(stack);
        }
        if (stack.is(RACK_PLATES_TAG) || TableObjects.item(stack) != null) {
            return true;
        }
        boolean edible = stack.has(DataComponents.FOOD) || stack.getItem() instanceof ConfigurableFood;
        if (edible) {
            return true;
        }
        if (stack.getItem() instanceof BlockItem) {
            return false;
        }
        return HotPotCooking.acceptable(world, stack);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        if (!(world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        int slot = slotAt(state, pos, hit.getLocation(), world);
        if (canPlaceItem(world, stack) && rack.tryInsert(player, hand, stack, slot)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide());
        }
        if (stack.getItem() instanceof BlockItem blockItem && Config.rackSwapMaterials()) {
            Block targetBlock = blockItem.getBlock();
            BlockState candidate = targetBlock.defaultBlockState();
            if (!candidate.is(TableDecor.TABLE_DECORATIONS) && TableMaterials.accepts(candidate)) {
                if (rack.applyMaterial(player, stack, targetBlock)) {
                    return Use.blockItem(UseOutcome.SUCCESS_SOFT, world.isClientSide());
                }
            }
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack)) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
        }
        if (player.isSecondaryUseActive()) {
            return Use.blockEmpty(rack.tryPopMaterial(player) ? UseOutcome.SUCCESS : UseOutcome.PASS, world.isClientSide());
        }
        int slot = slotAt(state, pos, hit.getLocation(), world);
        return Use.blockEmpty(rack.tryTake(player, slot) ? UseOutcome.SUCCESS : UseOutcome.PASS, world.isClientSide());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack
            && !rack.materials().top().isEmpty()) {
            BlockState matState = rack.frameBlockState();
            world.playSound(null, pos, matState.getSoundType().getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        BlockState mat = world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack
            ? rack.frameBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        return TableMaterials.destroyProgress(mat, player, world, pos);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        BlockState mat = level.getBlockEntity(pos) instanceof PrepRackBlockEntity rack
            ? rack.frameBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        level.levelEvent(player, 2001, pos, Block.getId(mat));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(this);
        if (world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack && rack.materials().hasAny()) {
            stack.set(HotPotContent.TABLE_MATERIALS, rack.materials());
        }
        return stack;
    }
}
