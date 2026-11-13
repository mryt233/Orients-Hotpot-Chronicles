// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 装着饮料的炼药锅（2026-10-27 用户：饮料能倒进炼药锅）。照原版水炼药锅：
 * <ul>
 *   <li>一锅只装一种饮料，最多 {@link #MAX} 口（= 3 满瓶），每 4 口水面高一档。</li>
 *   <li>倒进去：拿饮料瓶右键或拿装满的杯子右键；空炼药锅也能倒。</li>
 *   <li>舀出来：空玻璃杯舀一口；原版玻璃瓶或这种饮料的空瓶舀一瓶（最多 4 口）。舀空变回原版空炼药锅。</li>
 * </ul>
 * 作者：mryt233 */
public class DrinkCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<DrinkCauldronBlock> CODEC = simpleCodec(DrinkCauldronBlock::new);
    public static final int MAX = 3 * DrinkItem.SIPS;
    public static final EnumProperty<DrinkKind> KIND = EnumProperty.create("kind", DrinkKind.class, DrinkKind::hasBottle);
    public static final IntegerProperty SIPS = IntegerProperty.create("sips", 1, MAX);
    private static final CauldronInteraction.Dispatcher BEHAVIOR = new CauldronInteraction.Dispatcher();

    public DrinkCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F, 6.0F), BEHAVIOR);
        registerDefaultState(defaultBlockState().setValue(KIND, DrinkKind.ORANGE_DRINK).setValue(SIPS, 1));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, SIPS);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(SIPS) == MAX;
    }

    /** 水面高度同原版：一档 9、二档 12、满 15 像素（每档 4 口）。 */
    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + 3.0 * ((state.getValue(SIPS) + DrinkItem.SIPS - 1) / DrinkItem.SIPS)) / 16.0;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return state.getValue(SIPS);
    }

    /** mryt233 改：指示器标出炼药锅当前装的饮料。 */
    @Override
    public MutableComponent getName() {
        Player player = PlateHolderBlock.pickPlayer.get();
        if (player != null && player.level() != null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockState state = player.level().getBlockState(blockHit.getBlockPos());
                if (state.getBlock() instanceof DrinkCauldronBlock) {
                    DrinkKind kind = state.getValue(KIND);
                    return Blocks.CAULDRON.getName().append("（")
                        .append(HotPotContent.drinkFor(kind).getName(new ItemStack(HotPotContent.drinkFor(kind)))).append("）");
                }
            }
        }
        return super.getName();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Items.CAULDRON);
    }

    /** 空炼药锅：饮料和杯子在自己的 useOn 入口调用 {@link #pourIntoEmpty}，不修改原版私有行为表。 */
    public static void registerEmptyCauldron() {
    }

    public static InteractionResult pourIntoEmpty(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.CAULDRON)) {
            return InteractionResult.PASS;
        }
        boolean canPour = stack.getItem() instanceof DrinkItem
            || (stack.getItem() instanceof CupItem cup && cup.kind() != null && cup.kind().hasBottle());
        if (!canPour) {
            return InteractionResult.PASS;
        }
        return pourIn(null, context.getLevel(), context.getClickedPos(), player, context.getHand(), stack);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        DrinkKind kind = state.getValue(KIND);
        int sips = state.getValue(SIPS);
        if (stack.getItem() instanceof DrinkItem || (stack.getItem() instanceof CupItem cup && cup.kind() != null)) {
            return pourIn(state, world, pos, player, hand, stack);
        }
        Item used = stack.getItem();
        if (CupItem.isEmptyCup(stack)) {
            if (!world.isClientSide()) {
                replaceHeld(player, hand, stack, new ItemStack(HotPotContent.cupFor(kind)));
                took(state, world, pos, player, used, 1);
            }
            return Use.blockItem(UseOutcome.SUCCESS_SOFT, world.isClientSide());
        }
        if (stack.is(Items.GLASS_BOTTLE) || stack.is(HotPotContent.emptyBottle(kind))) {
            if (!world.isClientSide()) {
                int n = Math.min(DrinkItem.SIPS, sips);
                ItemStack bottle = new ItemStack(HotPotContent.drinkFor(kind));
                bottle.setDamageValue(DrinkItem.SIPS - n);
                replaceHeld(player, hand, stack, bottle);
                took(state, world, pos, player, used, n);
            }
            return Use.blockItem(UseOutcome.SUCCESS_SOFT, world.isClientSide());
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
    }

    /** 倒进去。state 为 null 表示原版空炼药锅。 */
    private static InteractionResult pourIn(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                            ItemStack stack) {
        DrinkKind kind;
        int have;
        if (stack.getItem() instanceof DrinkItem drink) {
            kind = drink.kind();
            have = DrinkItem.sipsLeft(stack);
        } else if (stack.getItem() instanceof CupItem cup && cup.kind() != null && cup.kind().hasBottle()) {
            kind = cup.kind();
            have = 1;
        } else {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        int sips = state == null ? 0 : state.getValue(SIPS);
        if (state != null && state.getValue(KIND) != kind) {
            if (!world.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.cauldron_other_drink")));
            }
            return Use.blockItem(UseOutcome.FAIL, world.isClientSide());
        }
        int n = Math.min(MAX - sips, have);
        if (n <= 0) {
            return Use.blockItem(UseOutcome.CONSUME, world.isClientSide());
        }
        if (!world.isClientSide()) {
            Item used = stack.getItem();
            if (stack.getItem() instanceof CupItem) {
                replaceHeld(player, hand, stack, new ItemStack(HotPotContent.GLASS_CUP));
            } else if (n == have) {
                replaceHeld(player, hand, stack, new ItemStack(HotPotContent.emptyBottle(kind)));
            } else {
                stack.setDamageValue(stack.getDamageValue() + n);
            }
            player.awardStat(Stats.ITEM_USED.get(used));
            player.awardStat(Stats.FILL_CAULDRON);
            world.setBlock(pos, HotPotContent.DRINK_CAULDRON.defaultBlockState().setValue(KIND, kind).setValue(SIPS, sips + n), 3);
            sound(world, pos, SoundEvents.BOTTLE_EMPTY);
            world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return Use.blockItem(UseOutcome.SUCCESS_SOFT, world.isClientSide());
    }

    private static void replaceHeld(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (held.getCount() <= 1) {
            player.setItemInHand(hand, result);
        } else {
            held.shrink(1);
            player.getInventory().placeItemBackInInventory(result);
        }
        player.containerMenu.broadcastChanges();
    }

    /** 舀走 n 口：舀空变回原版空炼药锅。 */
    private static void took(BlockState state, Level world, BlockPos pos, Player player, Item used, int n) {
        int left = state.getValue(SIPS) - n;
        world.setBlock(pos, left <= 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(SIPS, left), 3);
        player.awardStat(Stats.USE_CAULDRON);
        player.awardStat(Stats.ITEM_USED.get(used));
        sound(world, pos, SoundEvents.BOTTLE_FILL);
        world.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
    }

    private static void sound(Level world, BlockPos pos, SoundEvent sound) {
        world.playSound((Player) null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
