package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 竹筷（r103 起有自己的类）：夹着能吃的菜时，对着空气按住右键就把菜吃掉（用户同意的吃法），
 * 吃法、饱食度、声音全走原版 eatFood；菜蘸过蘸料的话效果在 LivingEntityEatMixin 里生效。
 * Shift+右键锅开锅界面在这里（useOnBlock）；夹菜 / 放菜 / 蘸桌上的蘸料在各个方块里处理。
 * 作者：mryt233 */
public class ChopsticksItem extends Item {
    public ChopsticksItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
    }

    private static FoodProperties heldFood(ItemStack stack) {
        ItemStack held = Chopsticks.held(stack);
        return held.isEmpty() ? null : ConfiguredFoodItem.food(held);
    }

    /** 主手筷子、副手容器时的食物（面条碗等）。 */
    private static FoodProperties offhandFood(ItemStack stack, LivingEntity user) {
        return HotPotHooks.offhandFood(stack, user);
    }

    /**
     * 拿筷子 Shift+右键锅 = 开锅界面（2026-10-25 用户定，代替原来的直接右键开锅；直接右键改成夹菜，见 HotPotBlock / StoveBlock）。
     * 潜行又拿着东西时原版不调方块的右键，只调物品的 useOnBlock，所以放在这里。新增。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
        // 其他模组的方块先处理，比如面条锅用它打开自己的界面（见 HotPotHooks）
        InteractionResult hooked = HotPotHooks.chopsticksUse(world, player, context.getHand(), context.getItemInHand(),
            pos, world.getBlockState(pos), hit);
        if (hooked != null) {
            return hooked;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.getBlockState(pos).is(HotPotContent.STOVE_TOP)) {
            // 高灶台的上半截：当成点在下面的灶台上
            pos = pos.below();
            hit = new BlockHitResult(hit.getLocation(), hit.getDirection(), pos, hit.isInside());
        }
        BlockState state = world.getBlockState(pos);
        // Shift+右键桌上的碗 / 盘子：筷子（连夹着的菜）架在碗沿上（2026-10-27 用户：只架碗和盘子，不架锅）
        if (state.is(HotPotContent.PLATE_HOLDER) && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity holder) {
            PlateHolderBlockEntity.Placed plate = PlateHolderBlock.aimed(player, pos, holder);
            // 修复：潜行会绕过方块 useItemOn；有菜时先走原本放菜/蘸料分支，空筷子才架碗沿。
            if (!Chopsticks.held(context.getItemInHand()).isEmpty()) {
                return ((PlateHolderBlock) state.getBlock()).useItemOn(context.getItemInHand(), state, world,
                    pos, player, context.getHand(), hit).result();
            }
            if (plate == null || !plate.restsChopsticks() || !plate.chopsticks.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!world.isClientSide) {
                plate.chopsticks = context.getItemInHand().copy();
                player.setItemInHand(context.getHand(), ItemStack.EMPTY);
                holder.changed();
                world.playSound(null, pos, SoundEvents.BAMBOO_PLACE,
                    SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        boolean pot = state.getBlock() instanceof HotPotBlock
            || (StoveBlock.isStove(state) && StoveBlock.isPotHit(state, pos, hit));
        if (!pot || !(world.getBlockEntity(pos) instanceof HotPotBlockEntity be) || !be.hasPot()) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide) {
            be.useWithChopsticks(player);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        FoodProperties food = heldFood(stack);
        if (food == null && hand == InteractionHand.MAIN_HAND) {
            food = HotPotHooks.offhandFood(stack, user);
        }
        // 饱食度满了也让吃（玩家反馈：装了 Always Eat 这类「随时能吃」的模组，
        // 筷子夹的菜还是吃不了）。筷子夹菜是本模组自己的吃法，不再按原版饥饿值拦；
        // 饱食度满时饥饿条不再涨，饱和度照常加（原版规则：最多加到当前饥饿值，即 20）。
        // 装了 Always Eat 也不冲突：它只让带食物组件的物品随时能吃，筷子本身不带食物组件（mryt233 改，2026-11-01）。
        if (food == null || !user.canEat(true)) {
            return InteractionResultHolder.pass(stack);
        }
        user.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return heldFood(stack) != null || Chopsticks.held(stack).isEmpty() ? UseAnim.EAT : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        FoodProperties food = heldFood(stack);
        if (food == null) {
            food = HotPotHooks.offhandFood(stack, user);
        }
        return food == null ? 0 : food.eatDurationTicks();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        ItemStack held = Chopsticks.held(stack);
        if (held.isEmpty()) {
            // 副手容器的吃法完全交给注册钩子的模组处理（面条碗等），火锅本体不处理。
            if (offhandFood(stack, user) != null) {
                HotPotHooks.beforeEating(stack, user);
            }
            return stack;
        }
        FoodProperties food = heldFood(stack);
        if (food == null) {
            return stack;
        }
        // 外部可以接管这次进食；只有真要吃时才回调，取消/长按未完成不扣。
        if (HotPotHooks.beforeEating(stack, user)) {
            return stack;
        }
        ItemStack eaten = held.copy();
        // 另一只手（主手或副手）拿着蘸料：吃之前自动蘸一口（2026-10-25 用户：主手副手都行；调整）
        if (!world.isClientSide) {
            DippingSauceItem.dipFromHands(user, eaten);
        }
        HotPotHooks.onEating(world, user, eaten);   // 结算前通知外部（物品栈还没变）
        user.eat(world, eaten, food);
        if (!world.isClientSide) {
            Chopsticks.hold(stack, ItemStack.EMPTY);
            if (user instanceof Player player && !player.hasInfiniteMaterials()) {
                food.usingConvertsTo().ifPresent(rest -> player.getInventory().placeItemBackInInventory(rest.copy()));
            }
        }
        return stack;
    }
}