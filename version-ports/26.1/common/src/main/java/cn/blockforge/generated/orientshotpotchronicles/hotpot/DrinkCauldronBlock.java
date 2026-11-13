// mryt233 改：26.1 原版 API；合入 Use 适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    public static final MapCodec<DrinkCauldronBlock> CODEC = simpleCodec(DrinkCauldronBlock::new);
    public static final int MAX = 3 * DrinkItem.SIPS;
    public static final EnumProperty<DrinkKind> KIND = EnumProperty.create("kind", DrinkKind.class, DrinkKind::hasBottle);
    public static final IntegerProperty SIPS = IntegerProperty.create("sips", 1, MAX);
    private static final CauldronInteraction.Dispatcher BEHAVIOR = new CauldronInteraction.Dispatcher();

    /**
     * 属性照原版炼药锅（金属音、不算红石导体），只把爆炸抗性抬到 6.0 与灶台、锅一致。
     * mryt233 改（2026-11-05）：挖掘工具见 data/minecraft/tags/block/mineable/pickaxe.json，
     * 拆了掉原版炼药锅（同原版水炼药锅，里面的饮料不返还）。
     */
    public DrinkCauldronBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F, 6.0F), BEHAVIOR);
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
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(SIPS);
    }

    /** mryt233 改：指示器标出炼药锅当前装的饮料。 */
    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        Player player = PlateHolderBlock.pickPlayer.get();
        if (player != null && player.level() != null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockState state = player.level().getBlockState(blockHit.getBlockPos());
                if (state.getBlock() instanceof DrinkCauldronBlock) {
                    DrinkKind kind = state.getValue(KIND);
                    return Blocks.CAULDRON.getName().append("（")
                        .append(Component.translatable(HotPotContent.drinkFor(kind).getDescriptionId())).append("）");
                }
            }
        }
        return super.getName();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Items.CAULDRON);
    }

    /** 空炼药锅：拿饮料瓶 / 满杯右键就倒进去（加到原版空炼药锅的行为表里）。 */
    public static void registerEmptyCauldron() {
        // mryt233 改：饮料和杯子在自己的 useOn 入口处理，不修改原版私有行为表。
    }

    public static InteractionResult pourIntoEmpty(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.CAULDRON)
            || stack.getItem() instanceof CupItem cup && (cup.kind() == null || !cup.kind().hasBottle())) {
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
        net.minecraft.world.item.Item used = stack.getItem();   // 换完手里的东西 stack 可能变空，统计要先记下
        if (CupItem.isEmptyCup(stack)) {
            if (!world.isClientSide()) {
                // mryt233 改（2026-11-07）：换手里的容器统一走 replaceHeld，创造模式也照常消耗，
                // 免得「倒进去 → 舀出来」在创造模式下把饮料翻倍
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
            // mryt233 改（2026-11-07）：锅满了就吃掉这次右键。以前回 PASS，原版接着调手里物品的
            // useOn → DrinkItem 会把整瓶饮料摆到锅盖上，玩家看到的是「倒不进去、还白丢一瓶」
            return Use.blockItem(UseOutcome.CONSUME, world.isClientSide());
        }
        if (!world.isClientSide()) {
            net.minecraft.world.item.Item used = stack.getItem();
            // mryt233 改（2026-11-07）：容器增减不再跳过创造模式——倒进去和舀出来必须是同一套
            // 消耗语义，否则「倒—舀」循环在创造模式下能把饮料翻倍
            if (stack.getItem() instanceof CupItem) {
                replaceHeld(player, hand, stack, new ItemStack(HotPotContent.GLASS_CUP));
            } else if (n == have) {
                replaceHeld(player, hand, stack, new ItemStack(HotPotContent.emptyBottle(kind)));
            } else {
                stack.setDamageValue(stack.getDamageValue() + n);   // 没倒完，留在瓶里
            }
            player.awardStat(Stats.ITEM_USED.get(used));
            player.awardStat(Stats.FILL_CAULDRON);
            world.setBlock(pos, HotPotContent.DRINK_CAULDRON.defaultBlockState().setValue(KIND, kind).setValue(SIPS, sips + n), 3);
            sound(world, pos, SoundEvents.BOTTLE_EMPTY);
            world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return Use.blockItem(UseOutcome.SUCCESS_SOFT, world.isClientSide());
    }

    /**
     * 把手里的这件容器换掉（mryt233 新增，2026-11-07）：数量 1 直接换、成组就减一只、换出来的进背包。
     * 创造模式也照常消耗——倒进去和舀出来必须是同一套语义，否则「倒—舀」循环能把饮料翻倍。
     * 作者：mryt233
     */
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
