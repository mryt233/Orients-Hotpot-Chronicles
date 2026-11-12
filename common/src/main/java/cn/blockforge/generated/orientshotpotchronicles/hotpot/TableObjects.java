// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 桌面物件门面与外置物品登记表：统一查询可摆上桌的物品、判定食物、以及处理上桌摆放逻辑。
 */
public final class TableObjects {
    private static final Map<Item, TableItem> REGISTERED = new ConcurrentHashMap<>();

    static {
        // 兜底登记原版碗并延迟读取 BOWL_BLOCK，避免未跑完内容注册的单测查不到或静态期拿到 null。
        registerItem(Items.BOWL, bowlTableItem());
    }

    private TableObjects() {
    }

    static TableItem bowlTableItem() {
        return new TableItem() {
            @Override
            public PlateBlock tableBlock(ItemStack stack) {
                return HotPotContent.BOWL_BLOCK;
            }

            @Override
            public boolean keepsSelf(ItemStack stack) {
                return false;
            }
        };
    }

    /** 为原版或外部物品登记桌面行为。 */
    public static void registerItem(Item item, TableItem tableItem) {
        REGISTERED.put(Objects.requireNonNull(item, "item"),
            Objects.requireNonNull(tableItem, "tableItem"));
    }

    static void unregisterItem(Item item) {
        if (item != null) {
            REGISTERED.remove(item);
        }
    }

    /** 查这件物品对应的 {@link TableItem}；不是桌面物件返回 null。 */
    @Nullable
    public static TableItem item(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof TableItem tableItem) {
            return tableItem;
        }
        return REGISTERED.get(stack.getItem());
    }

    /**
     * 仅对允许常规自动上桌（{@link #canAutoPlace}）的物品返回对应占位方块；
     * 竹筷与蘸料瓶返回 null，避免误入自动补摆、倒盘进锅与食材过滤之外的流程。
     */
    @Nullable
    public static PlateBlock placedFor(ItemStack stack) {
        TableItem tableItem = item(stack);
        return tableItem != null && tableItem.autoPlace(stack) ? tableItem.tableBlock(stack) : null;
    }

    /** 是否参与常规自动上桌（右键已有盘子格补摆、倒盘、盘子提示等）。 */
    public static boolean canAutoPlace(ItemStack stack) {
        TableItem tableItem = item(stack);
        return tableItem != null && tableItem.autoPlace(stack);
    }

    /** 摆上桌后是否整件保留原物品栈。 */
    public static boolean keepsSelf(ItemStack stack) {
        TableItem tableItem = item(stack);
        return tableItem != null && tableItem.keepsSelf(stack);
    }

    /** 能不能作为菜放进桌上的盘子或碗：所有 {@link TableItem} 一律不能当成菜塞进盘子。 */
    public static boolean holdsFood(Level world, ItemStack stack) {
        return stack != null && !stack.isEmpty() && item(stack) == null
            && (stack.has(DataComponents.FOOD) || HotPotCooking.acceptable(world, stack));
    }

    /** 摆上桌前做无副作用规整。 */
    public static ItemStack prepareForTable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        TableItem tableItem = item(stack);
        return tableItem == null ? stack : tableItem.prepareForTable(stack);
    }

    /** 播放桌面物件上桌音效；优先委托给对应的 {@link TableItem}。 */
    public static void playPlaceSound(Level world, BlockPos pos, PlateBlock block, ItemStack stack) {
        TableItem tableItem = item(stack);
        if (tableItem != null) {
            tableItem.playPlaceSound(world, pos, stack);
            return;
        }
        if (block instanceof DrinkBlock) {
            DrinkBlock.playPlaceSound(world, pos, stack);
            return;
        }
        if (block != null) {
            world.playSound(null, pos, block.defaultBlockState().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** 点在方块顶面：在上面那一格的 {@link PlateHolderBlock} 里摆入 {@code tableItem}。 */
    public static UseOutcome placeOn(UseOnContext context, TableItem tableItem) {
        if (tableItem == null) {
            return UseOutcome.PASS;
        }
        ItemStack stack = context.getItemInHand();
        PlateBlock plate = tableItem.tableBlock(stack);
        if (plate == null) {
            return UseOutcome.PASS;
        }
        return placeOn(context, plate, tableItem.keepsSelf(stack), tableItem);
    }

    /** 点在方块顶面：按指定占位方块与保留策略在上方那一格摆入。 */
    public static UseOutcome placeOn(UseOnContext context, PlateBlock plate, boolean keepSelf) {
        return placeOn(context, plate, keepSelf, item(context.getItemInHand()));
    }

    private static UseOutcome placeOn(UseOnContext context, PlateBlock plate, boolean keepSelf,
                                      @Nullable TableItem tableItem) {
        Level world = context.getLevel();
        if (plate == null || context.getClickedFace() != Direction.UP || context.getPlayer() == null) {
            return UseOutcome.PASS;
        }
        BlockState clicked = world.getBlockState(context.getClickedPos());
        if (clicked.getBlock() instanceof HotPotBlock || StoveBlock.isStove(clicked)
            || clicked.getBlock() instanceof StoveTopBlock) {
            return UseOutcome.PASS;
        }
        BlockPos target = context.getClickedPos().above();
        // 桌面在方块中间（11/16），点在桌面上时目标是桌子上面那一格
        BlockState existing = world.getBlockState(target);
        if (!existing.is(HotPotContent.PLATE_HOLDER)) {
            BlockPlaceContext placement = new BlockPlaceContext(context);
            if (!existing.canBeReplaced(placement) || !PlateHolderBlock.supported(world, target)) {
                return UseOutcome.PASS;
            }
        }
        if (world.isClientSide) {
            return UseOutcome.SUCCESS;
        }
        if (!existing.is(HotPotContent.PLATE_HOLDER)) {
            world.setBlock(target, PlateHolderBlock.withSurface(HotPotContent.PLATE_HOLDER.defaultBlockState(), world, target),
                Block.UPDATE_ALL);
        }
        if (!(world.getBlockEntity(target) instanceof PlateHolderBlockEntity be)) {
            return UseOutcome.FAIL;
        }
        double hx = (context.getClickLocation().x - target.getX()) * 16;
        double hz = (context.getClickLocation().z - target.getZ()) * 16;
        ItemStack stack = context.getItemInHand();
        Direction.Axis axis = HotPotBlock.handleAxisFor(context.getHorizontalDirection());
        if (!be.tryAdd(plate, axis, hx, hz, stack, keepSelf, context.getHorizontalDirection())) {
            context.getPlayer().displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
            if (be.plates().isEmpty()) {
                world.removeBlock(target, false);
            }
            return UseOutcome.FAIL;
        }
        playOnPlaced(world, target, plate, stack, tableItem);
        stack.consume(1, context.getPlayer());
        return UseOutcome.SUCCESS;
    }

    /**
     * 供需要 Shift+右键摆上桌的物件（蘸料瓶、竹筷）共用：
     * 点在已存在 {@link PlateHolderBlock} 顶面时直接摆入眼前这一格，否则走 {@link #placeOn}。
     */
    public static UseOutcome placeInHolderOrOn(UseOnContext context, TableItem tableItem) {
        if (tableItem == null) {
            return UseOutcome.PASS;
        }
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        PlateBlock plate = tableItem.tableBlock(stack);
        if (plate == null) {
            return UseOutcome.PASS;
        }
        if (context.getClickedFace() == Direction.UP
            && world.getBlockState(pos).is(HotPotContent.PLATE_HOLDER)
            && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity holder) {
            if (world.isClientSide) {
                return UseOutcome.SUCCESS;
            }
            double hx = (context.getClickLocation().x - pos.getX()) * 16;
            double hz = (context.getClickLocation().z - pos.getZ()) * 16;
            Direction.Axis axis = HotPotBlock.handleAxisFor(context.getHorizontalDirection());
            if (holder.tryAdd(plate, axis, hx, hz, stack, tableItem.keepsSelf(stack),
                context.getHorizontalDirection())) {
                tableItem.playAddSound(world, pos, stack);
                stack.consume(1, context.getPlayer());
                return UseOutcome.SUCCESS;
            }
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                    Component.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
            }
            return UseOutcome.FAIL;
        }
        return placeOn(context, plate, tableItem.keepsSelf(stack), tableItem);
    }

    private static void playOnPlaced(Level world, BlockPos pos, PlateBlock plate, ItemStack stack,
                                     @Nullable TableItem tableItem) {
        if (tableItem != null) {
            tableItem.playPlaceSound(world, pos, stack);
        } else {
            playPlaceSound(world, pos, plate, stack);
        }
    }
}
