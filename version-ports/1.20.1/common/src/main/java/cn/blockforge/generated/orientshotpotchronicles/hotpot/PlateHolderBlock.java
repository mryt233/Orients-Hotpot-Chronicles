package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.compat.ItemInteractionBlock;
import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import cn.blockforge.orient.framework.compat.ItemStackCompat;
import cn.blockforge.orient.framework.compat.PlayerCompat;
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
public class PlateHolderBlock extends Block implements EntityBlock, PartOutline, ItemInteractionBlock {
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
            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
        registerDefaultState(defaultBlockState().setValue(ON_TABLE, false).setValue(ON_TALL_TABLE, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ON_TABLE, ON_TALL_TABLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlateHolderBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
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
        net.minecraft.world.level.block.Block below = world.getBlockState(pos.below()).getBlock();
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
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return supported(world, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            if (!supported(world, pos)) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
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
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state, world, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state, world, pos);
    }

    /** 挖掉或被冲掉时，所有盘子带着菜掉出来（这一格本身不掉东西）。 */
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
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
    public static java.util.function.Supplier<Player> pickPlayer = () -> null;

    /** 鼠标中键：复制准星对着的那个盘子，里面的菜一起带上（r94）。mryt233 改（清单 #129）：与名称共用 aimedAt。 */
    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        Player player = pickPlayer.get();
        if (player != null && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be) {
            PlateHolderBlockEntity.Placed plate = null;
            HitResult hit = player.pick(PlayerCompat.blockInteractionRange(player), 1.0F, false);
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

    /**
     * mryt233 改（2026-11-07，清单 #129）：复合方块按准星部位显示名称——指着桌上哪只碗 / 盘子 / 杯子 /
     * 竹筷，指示器就显示那样东西自己的名字，而不是笼统的「盘子格」。
     * 名称通过客户端玩家供应器取准星，专用服务端或未命中时回退静态名。
     */
    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        Player player = pickPlayer.get();
        if (player != null && player.level() != null) {
            HitResult hit = player.pick(PlayerCompat.blockInteractionRange(player), 1.0F, false);
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

    /** mryt233 改（清单 #129）：名称、复制物品与 Jade 共用当前准星命中的餐具。 */
    @org.jetbrains.annotations.Nullable
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
    public net.minecraft.world.phys.shapes.VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
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
        BlockState state = p.plate.defaultBlockState().setValue(cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock.AXIS, p.axis);
        if (p.plate instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.BowlBlock
            && p.self.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBowlItem bowl) {
            state = state.setValue(cn.blockforge.generated.orientshotpotchronicles.hotpot.BowlBlock.SOUP,
                cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual.fromBase(bowl.soup()));
        }
        if (p.plate instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkBlock) {
            boolean empty = p.self.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.EmptyBottleItem;
            cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind kind = p.self.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.EmptyBottleItem e ? e.kind()
                : p.self.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkItem d ? d.kind() : cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind.ORANGE_DRINK;
            state = state.setValue(cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkBlock.EMPTY, empty).setValue(cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkBlock.KIND, kind);
        }
        if (p.plate instanceof CupBlock) {
            DrinkKind kind = p.self.getItem() instanceof CupItem cup ? cup.kind() : null;
            state = state.setValue(DrinkBlock.EMPTY, kind == null).setValue(CupBlock.KIND, kind == null ? DrinkKind.ORANGE_DRINK : kind);
        }
        return state;
    }

    /** 准星对着的是这一格里的哪个盘子（服务端和客户端同一套射线）。 */
    @org.jetbrains.annotations.Nullable
    public static PlateHolderBlockEntity.Placed aimed(Player player, BlockPos pos, PlateHolderBlockEntity be) {
        HitResult hit = player.pick(PlayerCompat.blockInteractionRange(player), 1.0F, false);
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
        BlockState plateState = modelState(plate);
        world.levelEvent(null, net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(plateState));
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
        Vec3 v = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)).scale(16);
        return new double[]{v.x, v.z};
    }

    /** mryt233 改（1.20.1 适配）：来自 {@link ItemInteractionBlock} 的接口默认方法。 */
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        double[] at = local(pos, hit);
        // 拿饮料瓶右键桌上的空杯 = 倒一杯（2026-10-25，新增）；要在「再摆一个」之前判断，不然瓶子会被摆上去
        PlateHolderBlockEntity.Placed target = be.at(at[0], at[1]);
        if (target != null && stack.getItem() instanceof DrinkItem drink && CupItem.isEmptyCup(target.self)) {
            if (!world.isClientSide) {
                target.self = new ItemStack(HotPotContent.cupFor(drink.kind()));
                player.setItemInHand(hand, DrinkItem.pourOne(stack));
                be.changed();
                world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.1F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        // 拿水桶 / 奶桶右键桌上的空杯 = 倒一杯，桶不会空（2026-10-27 用户：原版的桶就无限倒）
        DrinkKind fromBucket = CupItem.bucketKind(stack);
        if (target != null && fromBucket != null && CupItem.isEmptyCup(target.self)) {
            if (!world.isClientSide) {
                target.self = new ItemStack(HotPotContent.cupFor(fromBucket));
                be.changed();
                world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        PlateBlock placing = PlateItem.placedFor(stack);
        if (placing != null) {
            // 盘子、碗、汤碗、橙子饮料：在这一格再摆一个（r99 起碗和瓶子也走这里）
            if (!world.isClientSide) {
                if (be.tryAdd(placing, HotPotBlock.handleAxisFor(player.getDirection()), at[0], at[1], stack,
                    PlateItem.keepsSelf(stack))) {
                    ItemStackCompat.consume(stack, 1, player);
                    world.playSound(null, pos, placing.defaultBlockState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        PlateHolderBlockEntity.Placed plate = be.at(at[0], at[1]);
        if (plate == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 筷子夹着菜点桌上的蘸料碗 = 蘸一口，配料记在菜上，吃的时候生效
        // （r103 有过，2026-10-23 去掉；2026-10-25 用户在开发计划「筷子新交互」里要回来。调整）
        if (stack.is(HotPotContent.CHOPSTICKS) && plate.self.getItem() instanceof DippingSauceItem) {
            if (!world.isClientSide) {
                ItemStack held = Chopsticks.held(stack);
                if (held.isEmpty() || !held.isEdible()) {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.dip_need_food")), true);
                } else {
                    ItemStack dipped = held.copy();
                    ItemStack left = DippingSauceItem.dipInto(plate.self, dipped);
                    // 蘸完最后一口变回空碗：桌上这只碗变成能装菜的普通碗
                    plate.self = left.getItem() instanceof DippingSauceItem ? left : ItemStack.EMPTY;
                    Chopsticks.hold(stack, dipped);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.HONEY_BLOCK_STEP, SoundSource.BLOCKS, 0.4F, 1.4F);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (!plate.holdsFood() && (stack.is(HotPotContent.CHOPSTICKS) || PlateItem.holdsFood(world, stack))) {
            // 汤碗、橙子饮料里放不了菜，也夹不出菜
            if (!world.isClientSide) {
                player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.cannot_hold_food")), true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            // 夹着菜、肚子还吃得下：不放回去，交给筷子吃。以前夹起来以后按住右键会被当成连点，
            // 菜在碗和筷子之间来回放、夹，看着像夹不起来（用户报，2026-10-25 调整）；吃饱了才放回碗盘里
            ItemStack inHand = Chopsticks.held(stack);
            net.minecraft.world.food.FoodProperties food = inHand.getItem().getFoodProperties();
            // 饱食度满了也让他吃（玩家反馈：装了 Always Eat 就吃不了筷子夹的菜）。
            // 想放回碗里就潜行右键——以前是「吃饱了自动放回」，改成随时能吃以后用潜行保留这个功能。
            if (food != null && !player.isShiftKeyDown() && player.canEat(true)) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
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
            return ItemInteractionResult.SUCCESS;
        }
        if (PlateItem.holdsFood(world, stack)) {
            if (!world.isClientSide) {
                if (plate.add(stack)) {
                    ItemStackCompat.consume(stack, 1, player);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.2F);
                } else {
                    player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_full")), true);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键：把点到的那个盘子连菜拿起来；潜行时改成取出最后放的那样菜。 */
    /** mryt233 改（1.20.1 适配）：来自 {@link ItemInteractionBlock} 的接口默认方法。 */
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be)) {
            return InteractionResult.PASS;
        }
        double[] at = local(pos, hit);
        PlateHolderBlockEntity.Placed plate = be.at(at[0], at[1]);
        if (plate == null) {
            return InteractionResult.PASS;
        }
        // 空手右键架着筷子的碗 / 盘子：先把筷子（连夹着的菜）拿回来（2026-10-27）
        if (!plate.chopsticks.isEmpty() && !player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                player.setItemInHand(InteractionHand.MAIN_HAND, plate.chopsticks);
                plate.chopsticks = ItemStack.EMPTY;
                be.changed();
                world.playSound(null, pos, SoundEvents.BAMBOO_HIT, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return InteractionResult.SUCCESS;
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
            return InteractionResult.SUCCESS;
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
        return InteractionResult.SUCCESS;
    }
    /**
     * mryt233 改（0.2.2-alpha，1.20.1 适配）：1.20.1 只有单个
     * {@code use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)} 回调，
     * 没有 1.21.1 分开的 useItemOn / useWithoutItem；这里转给框架的
     * {@link ItemInteractionBlock#dispatchUse}（按物品 → 空手的顺序分派）。
     */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return dispatchUse(state, world, pos, player, hand, hit);
    }
}