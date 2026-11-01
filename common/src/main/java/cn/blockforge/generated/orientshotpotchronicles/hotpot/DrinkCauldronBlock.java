package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.stats.Stats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * 装着饮料的炼药锅（2026-10-27 用户：饮料能倒进炼药锅）。照原版水炼药锅：
 * <ul>
 *   <li>一锅只装一种饮料，最多 {@link #MAX} 口（= 3 满瓶，像原版 3 瓶水装满一锅），每 4 口水面高一档。</li>
 *   <li>倒进去：拿饮料瓶右键（瓶里剩几口倒几口，倒不下的留在瓶里），或拿装满的杯子右键（一口）。空炼药锅也能倒（{@link #registerEmptyCauldron}）。</li>
 *   <li>舀出来：空玻璃杯舀一口；原版玻璃瓶或这种饮料的空瓶舀一瓶（最多 4 口，不够 4 口就是没装满的瓶）。舀空变回原版空炼药锅。</li>
 * </ul>
 * 拆了掉原版炼药锅，饮料没了（同原版水炼药锅）。森罗物语：酒馆的龙头联动以后再做（用户：龙头接就无限接）。
 * 外观：原版炼药锅模型，水面换成按饮料上色的原版水面（tools/gen_drink_cauldron.py）。
 * 作者：mryt233 */
public class DrinkCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<DrinkCauldronBlock> CODEC = simpleCodec(s -> new DrinkCauldronBlock());
    public static final int MAX = 3 * DrinkItem.SIPS;
    public static final EnumProperty<DrinkKind> KIND = EnumProperty.create("kind", DrinkKind.class, DrinkKind::hasBottle);
    public static final IntegerProperty SIPS = IntegerProperty.create("sips", 1, MAX);
    private static final CauldronInteraction.InteractionMap BEHAVIOR = CauldronInteraction.newInteractionMap("orients_hotpot_chronicles_drink");

    public DrinkCauldronBlock() {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON), BEHAVIOR);
        registerDefaultState(defaultBlockState().setValue(KIND, DrinkKind.ORANGE_DRINK).setValue(SIPS, 1));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
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
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return state.getValue(SIPS);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    /** 空炼药锅：拿饮料瓶 / 满杯右键就倒进去（加到原版空炼药锅的行为表里）。 */
    public static void registerEmptyCauldron() {
        CauldronInteraction pour = (state, world, pos, player, hand, stack) -> pourIn(null, world, pos, player, hand, stack);
        for (DrinkKind kind : DrinkKind.values()) {
            if (!kind.hasBottle()) {
                continue;   // 水杯、奶杯不往这里倒
            }
            CauldronInteraction.EMPTY.map().put(HotPotContent.drinkFor(kind), pour);
            CauldronInteraction.EMPTY.map().put(HotPotContent.cupFor(kind), pour);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        DrinkKind kind = state.getValue(KIND);
        int sips = state.getValue(SIPS);
        if (stack.getItem() instanceof DrinkItem || (stack.getItem() instanceof CupItem cup && cup.kind() != null)) {
            return pourIn(state, world, pos, player, hand, stack);
        }
        net.minecraft.world.item.Item used = stack.getItem();   // 换完手里的东西 stack 可能变空，统计要先记下
        if (CupItem.isEmptyCup(stack)) {
            if (!world.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(HotPotContent.cupFor(kind))));
                took(state, world, pos, player, used, 1);
            }
            return ItemInteractionResult.sidedSuccess(world.isClientSide);
        }
        if (stack.is(Items.GLASS_BOTTLE) || stack.is(HotPotContent.emptyBottle(kind))) {
            if (!world.isClientSide) {
                int n = Math.min(DrinkItem.SIPS, sips);
                ItemStack bottle = new ItemStack(HotPotContent.drinkFor(kind));
                bottle.setDamageValue(DrinkItem.SIPS - n);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, bottle));
                took(state, world, pos, player, used, n);
            }
            return ItemInteractionResult.sidedSuccess(world.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 倒进去。state 为 null 表示原版空炼药锅。 */
    private static ItemInteractionResult pourIn(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
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
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int sips = state == null ? 0 : state.getValue(SIPS);
        if (state != null && state.getValue(KIND) != kind) {
            if (!world.isClientSide) {
                player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.cauldron_other_drink")), true);
            }
            return ItemInteractionResult.FAIL;
        }
        int n = Math.min(MAX - sips, have);
        if (n <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClientSide) {
            net.minecraft.world.item.Item used = stack.getItem();
            if (!player.getAbilities().instabuild) {
                if (stack.getItem() instanceof CupItem) {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(HotPotContent.GLASS_CUP)));
                } else if (n == have) {
                    player.setItemInHand(hand, new ItemStack(HotPotContent.emptyBottle(kind)));
                } else {
                    stack.setDamageValue(stack.getDamageValue() + n);
                }
            }
            player.awardStat(Stats.ITEM_USED.get(used));
            player.awardStat(Stats.FILL_CAULDRON);
            world.setBlock(pos, HotPotContent.DRINK_CAULDRON.defaultBlockState().setValue(KIND, kind).setValue(SIPS, sips + n), 3);
            sound(world, pos, SoundEvents.BOTTLE_EMPTY);
            world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return ItemInteractionResult.sidedSuccess(world.isClientSide);
    }

    /** 舀走 n 口：舀空变回原版空炼药锅。 */
    private static void took(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.item.Item used, int n) {
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