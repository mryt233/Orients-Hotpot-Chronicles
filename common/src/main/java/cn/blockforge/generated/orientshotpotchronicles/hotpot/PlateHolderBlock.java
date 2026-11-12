package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3G 右键结果改走适配小包（意图 → 原版常量），逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;

/**
 * 摆盘子的那一格（看不见，盘子和菜由 PlateHolderRenderer 画）。下面是火锅桌时整体下移贴着桌面。
 *
 * <ul>
 *   <li>拿盘子右键：在点的位置附近再摆一个（放不下会提示）。</li>
 *   <li>拿食物右键：放进点到的那个盘子。</li>
 *   <li>拿筷子右键：空筷子从点到的盘子夹走最后放的那样；筷子上有菜就放回这个盘子。</li>
 *   <li>空手右键：把点到的那个盘子连菜拿起来；最后一个拿走后这一格变回空气。</li>
 * </ul>
 * 挖掉这一格：所有盘子带着菜掉出来。
 */
public class PlateHolderBlock extends Block implements EntityBlock, PartOutline {
    /** 下面是火锅桌（桌面 11/16，盘子下移 5 像素）。 */
    public static final BooleanProperty ON_TABLE = BooleanProperty.create("on_table");
    /** 下面是高火锅桌（桌面 23/16，盘子上移 7 像素）。 */
    public static final BooleanProperty ON_TALL_TABLE = BooleanProperty.create("on_tall_table");

    public PlateHolderBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .strength(0.3F)
            .sound(SoundType.DECORATED_POT)
            .noOcclusion()
            .noLootTable()
            // 形状随方块实体（摆了哪些盘子）变、矮桌上还伸出本格：不让原版提前缓存形状（同潜影盒），
            // 否则缓存的是空世界里的默认小方块，和真实碰撞对不上，站上去会抽搐
            .dynamicShape()
            .pushReaction(PushReaction.DESTROY));
        registerDefaultState(defaultBlockState().setValue(ON_TABLE, false).setValue(ON_TALL_TABLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ON_TABLE, ON_TALL_TABLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlateHolderBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 盘子底面离这一格底面多高（像素）：火锅桌上 -5（桌面 11/16），高火锅桌上 +7（桌面 23/16），地上 0。 */
    public static double lift(BlockState state) {
        if (state.getValue(ON_TALL_TABLE)) {
            return 7.0;
        }
        return state.getValue(ON_TABLE) ? -PlateSize.TABLE_DROP : 0;
    }

    /** 按下面的方块定好 ON_TABLE / ON_TALL_TABLE。 */
    public static BlockState withSurface(BlockState state, BlockGetter world, BlockPos pos) {
        Block below = world.getBlockState(pos.below()).getBlock();
        boolean tall = below instanceof TallHotPotTableBlock;
        return state.setValue(ON_TALL_TABLE, tall).setValue(ON_TABLE, !tall && below instanceof HotPotTableBlock);
    }

    /** 下面是火锅桌，或者顶面能放东西的方块。 */
    public static boolean supported(LevelReader world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        return below.getBlock() instanceof HotPotTableBlock
            || Block.canSupportCenter(world, pos.below(), Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return supported(world, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            if (!supported(world, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return withSurface(state, world, pos);
        }
        return state;
    }

    private static VoxelShape shapeOf(BlockState state, BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be
            ? be.shape(lift(state))
            : Block.box(6, 0, 6, 10, 1, 10);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state, world, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state, world, pos);
    }

    /** 挖掉或被冲掉时，所有盘子带着菜掉出来（这一格本身不掉东西）。 */
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !world.isClientSide
            && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be) {
            for (PlateHolderBlockEntity.Placed p : be.plates()) {
                Block.popResource(world, pos, p.toItem());
                if (!p.chopsticks.isEmpty()) {
                    Block.popResource(world, pos, p.chopsticks);
                }
            }
            be.plates().clear();
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    /** 客户端填：当前玩家（鼠标中键选方块时用来找准星对着哪个盘子）；服务端一直是 null。 */
    public static Supplier<Player> pickPlayer = () -> null;

    /** mryt233 改：指示器名称与选中框、复制物品共用餐具命中判定。 */
    @Override
    public MutableComponent getName() {
        Player player = pickPlayer.get();
        if (player != null && player.level() != null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && player.level().getBlockEntity(blockHit.getBlockPos()) instanceof PlateHolderBlockEntity be) {
                PlateHolderBlockEntity.Placed plate = aimedAt(blockHit.getBlockPos(), blockHit, be);
                if (plate != null) {
                    ItemStack item = plate.toItem();
                    if (!item.isEmpty()) {
                        return item.getHoverName().copy();
                    }
                }
            }
        }
        return super.getName();
    }

    /** 鼠标中键：复制准星对着的那个盘子，里面的菜一起带上（r94）。 */
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        Player player = pickPlayer.get();
        if (player != null && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be) {
            PlateHolderBlockEntity.Placed plate = null;
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && blockHit.getBlockPos().equals(pos)) {
                plate = aimedAt(pos, blockHit, be);
            }
            if (plate == null && !be.plates().isEmpty()) {
                plate = be.plates().get(0);
            }
            if (plate != null) {
                return plate.toItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /** mryt233 改：名称、复制物品与 Jade 共用当前准星命中的餐具。 */
    @Nullable
    public static PlateHolderBlockEntity.Placed aimedAt(BlockPos pos, BlockHitResult hit, PlateHolderBlockEntity be) {
        double[] at = local(pos, hit);
        PlateHolderBlockEntity.Placed plate = be.at(at[0], at[1]);
        if (plate == null && !be.plates().isEmpty()) {
            plate = be.plates().get(0);
        }
        return plate;
    }

    /** 选中框只框准星对着的那个盘子（连盘里的菜 / 那瓶饮料 / 那只碗）。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be) || be.plates().size() < 2) {
            return null;
        }
        PlateHolderBlockEntity.Placed plate = aimed(player, pos, be);
        return plate == null ? null : PlateHolderBlockEntity.partShape(plate, lift(state));
    }

    /**
     * 这个盘子长什么样：盘子按方向；汤碗按汤底、空瓶换空瓶模型（r99）、饮料按种类。渲染和挖掉时的碎屑共用
     * （：以前碎屑用默认状态，植物蛋白饮料挖掉飞出的是橙子饮料的碎屑，用户报）。
     */
    public static BlockState modelState(PlateHolderBlockEntity.Placed p) {
        // mryt233 改（4H）：委托给 TableBlock#modelState
        return p.plate.modelState(p);
    }

    /** 准星对着的是这一格里的哪个盘子（服务端和客户端同一套射线）。 */
    @Nullable
    public static PlateHolderBlockEntity.Placed aimed(Player player, BlockPos pos, PlateHolderBlockEntity be) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        double[] at = local(pos, blockHit);
        return be.at(at[0], at[1]);
    }

    /**
     * 挖盘子：只挖准星对着的那一个，带着菜掉出来（创造模式直接拿掉），别的盘子留着；最后一个挖掉后这一格变空气。
     * 准星没对着任何盘子就什么都不做。
     */
    public static void breakOne(Level world, BlockPos pos, PlateHolderBlockEntity be, Player player) {
        PlateHolderBlockEntity.Placed plate = aimed(player, pos, be);
        if (plate == null) {
            return;
        }
        // mryt233 改（4H）：碎屑粒子方块状态委托给 TableBlock#breakParticleState
        BlockState plateState = plate.plate.breakParticleState(plate);
        world.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(plateState));
        if (!player.isCreative()) {
            Block.popResource(world, pos, plate.toItem());
        }
        if (!plate.chopsticks.isEmpty()) {
            Block.popResource(world, pos, plate.chopsticks);   // 架着的筷子创造模式也掉，免得没了
        }
        be.remove(plate);
        if (be.plates().isEmpty()) {
            world.removeBlock(pos, false);
        }
    }

    // ------------------------------------------------------------------ 交互

    /** 点到的位置在这一格里的 x、z（像素）。 */
    private static double[] local(BlockPos pos, BlockHitResult hit) {
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 v = hit.getLocation().subtract(n.scale(0.001)).subtract(Vec3.atLowerCornerOf(pos)).scale(16);
        return new double[]{v.x, v.z};
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide);
        }
        double[] at = local(pos, hit);
        // mryt233 改（4H）：对桌上已摆物件的点按交互（倒饮料/牛奶/水进空杯、满杯碰杯、筷子蘸料等）
        // 统一委托给 TableBlock#useOnPlaced，且必须在自动补摆之前判断，避免拿着饮料右键空杯变成再摆一瓶。
        PlateHolderBlockEntity.Placed target = be.at(at[0], at[1]);
        if (target != null) {
            UseOutcome placedOutcome = target.plate.useOnPlaced(target, world, pos, be, player, hand, stack);
            if (placedOutcome != UseOutcome.PASS) {
                return Use.blockItem(placedOutcome, world.isClientSide);
            }
        }
        PlateBlock placing = TableObjects.placedFor(stack);
        if (placing != null) {
            // 盘子、碗、汤碗、橙子饮料：在这一格再摆一个（r99 起碗和瓶子也走这里）
            if (!world.isClientSide) {
                if (be.tryAdd(placing, HotPotBlock.handleAxisFor(player.getDirection()), at[0], at[1], stack,
                    TableObjects.keepsSelf(stack))) {
                    // mryt233 改（r146d）：先按原物品辨认空瓶 / 饮料，再扣除，避免最后一个物品变成空栈。
                    TableObjects.playPlaceSound(world, pos, placing, stack);
                    stack.consume(1, player);
                } else {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
                }
            }
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (target == null) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide);
        }
        PlateHolderBlockEntity.Placed plate = target;
        if (!plate.holdsFood() && (stack.is(HotPotContent.CHOPSTICKS) || TableObjects.holdsFood(world, stack))) {
            // 汤碗、橙子饮料里放不了菜，也夹不出菜
            if (!world.isClientSide) {
                player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.cannot_hold_food")), true);
            }
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            // 夹着菜、肚子还吃得下：不放回去，交给筷子吃。以前夹起来以后按住右键会被当成连点，
            // 菜在碗和筷子之间来回放、夹，看着像夹不起来（用户报，2026-10-25 调整）；吃饱了才放回碗盘里
            ItemStack inHand = Chopsticks.held(stack);
            FoodProperties food = inHand.get(DataComponents.FOOD);
            // 饱食度满了也让他吃（玩家反馈：装了 Always Eat 就吃不了筷子夹的菜）。
            // 想放回碗里就潜行右键——以前是「吃饱了自动放回」，改成随时能吃以后用潜行保留这个功能。
            if (food != null && !player.isShiftKeyDown() && player.canEat(true)) {
                return Use.blockItem(UseOutcome.SKIPPED, world.isClientSide);
            }
            if (!world.isClientSide) {
                ItemStack held = Chopsticks.held(stack);
                if (held.isEmpty()) {
                    ItemStack taken = plate.takeLast();
                    if (taken.isEmpty()) {
                        player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_empty")), true);
                    } else {
                        Chopsticks.hold(stack, taken);
                        be.changed();
                        world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.5F);
                    }
                } else if (plate.add(held)) {
                    Chopsticks.hold(stack, ItemStack.EMPTY);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.2F);
                } else {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_full")), true);
                }
            }
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (TableObjects.holdsFood(world, stack)) {
            if (!world.isClientSide) {
                if (plate.add(stack)) {
                    stack.consume(1, player);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.2F);
                } else {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_full")), true);
                }
            }
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide);
    }

    /** 空手右键：把点到的那个盘子连菜拿起来；潜行时改成取出最后放的那样菜。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be)) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide);
        }
        double[] at = local(pos, hit);
        PlateHolderBlockEntity.Placed plate = be.at(at[0], at[1]);
        if (plate == null) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide);
        }
        // 空手右键架着筷子的碗 / 盘子：先把筷子（连夹着的菜）拿回来（2026-10-27）
        if (!plate.chopsticks.isEmpty() && !player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                player.setItemInHand(InteractionHand.MAIN_HAND, plate.chopsticks);
                plate.chopsticks = ItemStack.EMPTY;
                be.changed();
                world.playSound(null, pos, SoundEvents.BAMBOO_HIT, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide);
        }
        // 潜行 + 空手右键：按「最后放进去的先出来」取一样菜进背包，按一下取一个（r96）
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                ItemStack taken = plate.takeLast();
                if (taken.isEmpty()) {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_empty")), true);
                } else {
                    player.getInventory().placeItemBackInInventory(taken);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.5F);
                }
            }
            return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (!world.isClientSide) {
            be.remove(plate);
            player.setItemInHand(InteractionHand.MAIN_HAND, plate.toItem());
            world.playSound(null, pos, plate.plate.defaultBlockState().getSoundType().getHitSound(),
                SoundSource.BLOCKS, 0.8F, 1.2F);
            if (be.plates().isEmpty()) {
                world.removeBlock(pos, false);
            }
        }
        return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide);
    }
}
