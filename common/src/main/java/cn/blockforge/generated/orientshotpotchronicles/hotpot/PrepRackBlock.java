// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 食材架 / 备菜架统一基类（多层落地菜架 6 格、桌面备菜架 4 格、挂架两态 4 格）。
 * 全面复用火锅桌同款换材质架构、{@link TableMaterials} 规则及无 GUI 准星分格直接放取。作者：mryt233
 */
public class PrepRackBlock extends Block implements EntityBlock, PartOutline {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
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
        VoxelShape frame = frameShape(state);
        if (kind != Kind.HANGING || !(world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack) || rack.isRemoved()) return frame;
        // 挂件可垂到架体以下；只扩大射线形状，不扩大碰撞。
        for (int i=0; i<kind.slots(); i++) if (!rack.itemAt(i).isEmpty()) {
            frame = Shapes.or(frame, PrepRackGeometry.shape(PrepRackGeometry.cell(kind,state.getValue(MOUNT),i),state.getValue(FACING),0));
        }
        return frame;
    }

    private VoxelShape frameShape(BlockState state) {
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
        return frameShape(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrepRackBlockEntity(pos, state);
    }

    /** 将世界命中坐标转换成局部坐标系 [0..1]（X 向右、Z 向前、Y 向上），按架型精确选择最近子格。 */
    public int slotAt(BlockState state, BlockPos pos, Vec3 hit, @Nullable BlockGetter world) {
        Vec3 local = localPoint(state, pos, hit, world);
        double ly = local.y;
        double lx = local.x + .5;
        double lz = local.z + .5;
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

    Vec3 localPoint(BlockState state, BlockPos pos, Vec3 hit, @Nullable BlockGetter world) {
        double lift = world == null ? 0 : TableDecor.liftOf(world,pos,state);
        return PrepRackGeometry.local(state.getValue(FACING),pos,lift,hit);
    }

    public boolean canPlaceItem(Level world, ItemStack stack) { return !stack.isEmpty(); }

    /** mryt233 改：空主手优先取菜；取不到时，副手照常使用而不被塞上架。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide);
        if (player.isSecondaryUseActive()) {
            return isSwapMaterial(stack) && swapMaterial(world,pos,player,stack)
                ? Use.blockItem(UseOutcome.SUCCESS_SOFT,world.isClientSide)
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().isEmpty()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack && !rack.isRemoved() && !world.isClientSide) {
            int slot = slotAt(state,pos,hit.getLocation(),world);
            if (!rack.tryInsert(player,hand,stack,slot,localPoint(state,pos,hit.getLocation(),world))) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.prep_rack_full"),"No available space on this rack"),true);
            }
        }
        // 始终交由服务端判定，不用可能过期的客户端上限挡住原版使用包。
        return Use.blockItem(UseOutcome.SUCCESS_SOFT,world.isClientSide);
    }

    /** 平台在原版潜行跳过方块分支前，仅为合格材质接管备菜架交互。 */
    public static InteractionResult onSneakUse(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        ItemStack held = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || player.isSpectator()
            || !(world.getBlockState(pos).getBlock() instanceof PrepRackBlock) || !isSwapMaterial(held)) return InteractionResult.PASS;
        if (!world.mayInteract(player,pos)) return InteractionResult.FAIL;
        return swapMaterial(world,pos,player,held) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
    }

    static boolean isSwapMaterial(ItemStack stack) {
        if (stack.isEmpty() || !Config.rackSwapMaterials() || !(stack.getItem() instanceof BlockItem item)) return false;
        BlockState candidate = item.getBlock().defaultBlockState();
        return !candidate.is(TableDecor.TABLE_DECORATIONS) && TableMaterials.accepts(candidate);
    }

    private static boolean swapMaterial(Level world, BlockPos pos, Player player, ItemStack stack) {
        if (!(world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack) || rack.isRemoved()) return false;
        return world.isClientSide || rack.applyMaterial(player,stack,((BlockItem) stack.getItem()).getBlock());
    }

    static boolean canUseEmptyHand(boolean sneaking, ItemStack main, ItemStack off) {
        return main.isEmpty() && (!sneaking || off.isEmpty());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        boolean sneaking = player.isSecondaryUseActive();
        if (!canUseEmptyHand(sneaking,player.getMainHandItem(),player.getOffhandItem())) return InteractionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack) || rack.isRemoved()) return InteractionResult.PASS;
        boolean used;
        if (sneaking) used = rack.tryPopMaterial(player);
        else if (world.isClientSide) used = rack.hasStoredItems();
        else used = rack.tryTake(player,slotAt(state,pos,hit.getLocation(),world),localPoint(state,pos,hit.getLocation(),world));
        return used ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
    }

    private int aimedItem(BlockState state, BlockGetter world, BlockPos pos, Player player, PrepRackBlockEntity rack) {
        if (rack.isRemoved()) return -1;
        double reach = player.blockInteractionRange();
        if (!(player.pick(reach,1,false) instanceof BlockHitResult hit) || hit.getType()!=HitResult.Type.BLOCK
            || !hit.getBlockPos().equals(pos)) return -1;
        Vec3 eye = player.getEyePosition();
        Vec3 start = localPoint(state,pos,eye,world);
        Vec3 end = localPoint(state,pos,eye.add(player.getViewVector(1).scale(reach)),world);
        int best = -1;
        double distance = Double.MAX_VALUE;
        for (int i=0; i<kind.slots(); i++) {
            if (rack.itemAt(i).isEmpty()) continue;
            var clip = PrepRackGeometry.cell(kind,state.getValue(MOUNT),i).box().clip(start,end);
            if (clip.isPresent() && start.distanceToSqr(clip.get())<distance) {
                best=i;
                distance=start.distanceToSqr(clip.get());
            }
        }
        return best;
    }

    @Override public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack)) return null;
        int slot = aimedItem(state,world,pos,player,rack);
        double lift = TableDecor.liftOf(world,pos,state);
        if (slot < 0) return frameShape(state).move(0,lift,0);
        // mryt233 改：分部描边直接画返回形状，必须和桌面抬升后的渲染处在同一高度。
        return PrepRackGeometry.shape(PrepRackGeometry.cell(kind,state.getValue(MOUNT),slot),state.getValue(FACING),lift);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack
            && !rack.materials().top().isEmpty()) {
            BlockState matState = rack.frameBlockState();
            world.playSound(null, pos, matState.getSoundType().getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack) {
            rack.dropStoredItems();
        }
        super.onRemove(state, world, pos, newState, moved);
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
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (world.getBlockEntity(pos) instanceof PrepRackBlockEntity rack && !rack.isRemoved()) {
            Player player = PlateHolderBlock.pickPlayer.get();
            int slot = player == null ? -1 : aimedItem(state,world,pos,player,rack);
            if (slot >= 0) return rack.itemAt(slot).copyWithCount(1);
            if (rack.materials().hasAny()) stack.set(HotPotContent.TABLE_MATERIALS,rack.materials());
        }
        return stack;
    }
}
