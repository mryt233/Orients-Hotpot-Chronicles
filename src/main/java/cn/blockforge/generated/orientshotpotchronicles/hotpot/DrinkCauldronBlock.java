package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

/**
 * 装着饮料的炼药锅（2026-10-27 用户：饮料能倒进炼药锅）。照原版水炼药锅：
 * <ul>
 *   <li>一锅只装一种饮料，最多 {@link #MAX} 口（= 3 满瓶，像原版 3 瓶水装满一锅），每 4 口水面高一档。</li>
 *   <li>倒进去：拿饮料瓶右键（瓶里剩几口倒几口，倒不下的留在瓶里），或拿装满的杯子右键（一口）。空炼药锅也能倒（{@link #registerEmptyCauldron}）。</li>
 *   <li>舀出来：空玻璃杯舀一口；原版玻璃瓶或这种饮料的空瓶舀一瓶（最多 4 口，不够 4 口就是没装满的瓶）。舀空变回原版空炼药锅。</li>
 * </ul>
 * 拆了掉原版炼药锅，饮料没了（同原版水炼药锅）。森罗物语：酒馆的龙头联动以后再做（用户：龙头接就无限接）。
 * 外观：原版炼药锅模型，水面换成按饮料上色的原版水面（tools/gen_drink_cauldron.py）。
 *
 */
public class DrinkCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<DrinkCauldronBlock> CODEC = createCodec(s -> new DrinkCauldronBlock());
    public static final int MAX = 3 * DrinkItem.SIPS;
    public static final EnumProperty<DrinkKind> KIND = EnumProperty.of("kind", DrinkKind.class, DrinkKind::hasBottle);
    public static final IntProperty SIPS = IntProperty.of("sips", 1, MAX);
    private static final CauldronBehavior.CauldronBehaviorMap BEHAVIOR = CauldronBehavior.createMap("orients_hotpot_chronicles_drink");

    public DrinkCauldronBlock() {
        super(AbstractBlock.Settings.copy(Blocks.CAULDRON), BEHAVIOR);
        setDefaultState(getDefaultState().with(KIND, DrinkKind.ORANGE_DRINK).with(SIPS, 1));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(KIND, SIPS);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.get(SIPS) == MAX;
    }

    /** 水面高度同原版：一档 9、二档 12、满 15 像素（每档 4 口）。 */
    @Override
    protected double getFluidHeight(BlockState state) {
        return (6.0 + 3.0 * ((state.get(SIPS) + DrinkItem.SIPS - 1) / DrinkItem.SIPS)) / 16.0;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(SIPS);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    /** 空炼药锅：拿饮料瓶 / 满杯右键就倒进去（加到原版空炼药锅的行为表里）。 */
    public static void registerEmptyCauldron() {
        CauldronBehavior pour = (state, world, pos, player, hand, stack) -> pourIn(null, world, pos, player, hand, stack);
        for (DrinkKind kind : DrinkKind.values()) {
            if (!kind.hasBottle()) {
                continue;   // 水杯、奶杯不往这里倒
            }
            CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map().put(HotPotContent.drinkFor(kind), pour);
            CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map().put(HotPotContent.cupFor(kind), pour);
        }
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        DrinkKind kind = state.get(KIND);
        int sips = state.get(SIPS);
        if (stack.getItem() instanceof DrinkItem || (stack.getItem() instanceof CupItem cup && cup.kind() != null)) {
            return pourIn(state, world, pos, player, hand, stack);
        }
        net.minecraft.item.Item used = stack.getItem();   // 换完手里的东西 stack 可能变空，统计要先记下
        if (CupItem.isEmptyCup(stack)) {
            if (!world.isClient) {
                player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(HotPotContent.cupFor(kind))));
                took(state, world, pos, player, used, 1);
            }
            return ItemActionResult.success(world.isClient);
        }
        if (stack.isOf(Items.GLASS_BOTTLE) || stack.isOf(HotPotContent.emptyBottle(kind))) {
            if (!world.isClient) {
                int n = Math.min(DrinkItem.SIPS, sips);
                ItemStack bottle = new ItemStack(HotPotContent.drinkFor(kind));
                bottle.setDamage(DrinkItem.SIPS - n);
                player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, bottle));
                took(state, world, pos, player, used, n);
            }
            return ItemActionResult.success(world.isClient);
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 倒进去。state 为 null 表示原版空炼药锅。 */
    private static ItemActionResult pourIn(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
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
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int sips = state == null ? 0 : state.get(SIPS);
        if (state != null && state.get(KIND) != kind) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.cauldron_other_drink")), true);
            }
            return ItemActionResult.FAIL;
        }
        int n = Math.min(MAX - sips, have);
        if (n <= 0) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClient) {
            net.minecraft.item.Item used = stack.getItem();
            if (!player.getAbilities().creativeMode) {
                if (stack.getItem() instanceof CupItem) {
                    player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(HotPotContent.GLASS_CUP)));
                } else if (n == have) {
                    player.setStackInHand(hand, new ItemStack(HotPotContent.emptyBottle(kind)));
                } else {
                    stack.setDamage(stack.getDamage() + n);
                }
            }
            player.incrementStat(Stats.USED.getOrCreateStat(used));
            player.incrementStat(Stats.FILL_CAULDRON);
            world.setBlockState(pos, HotPotContent.DRINK_CAULDRON.getDefaultState().with(KIND, kind).with(SIPS, sips + n));
            sound(world, pos, SoundEvents.ITEM_BOTTLE_EMPTY);
            world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return ItemActionResult.success(world.isClient);
    }

    /** 舀走 n 口：舀空变回原版空炼药锅。 */
    private static void took(BlockState state, World world, BlockPos pos, PlayerEntity player, net.minecraft.item.Item used, int n) {
        int left = state.get(SIPS) - n;
        world.setBlockState(pos, left <= 0 ? Blocks.CAULDRON.getDefaultState() : state.with(SIPS, left));
        player.incrementStat(Stats.USE_CAULDRON);
        player.incrementStat(Stats.USED.getOrCreateStat(used));
        sound(world, pos, SoundEvents.ITEM_BOTTLE_FILL);
        world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
    }

    private static void sound(World world, BlockPos pos, SoundEvent sound) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}
