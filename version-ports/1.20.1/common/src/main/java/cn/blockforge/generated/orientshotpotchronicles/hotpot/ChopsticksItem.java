package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：第三批 3G 右键结果改走适配小包（意图 → 原版常量），逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 竹筷（r103 起有自己的类）：夹着能吃的菜时，对着空气按住右键就把菜吃掉（用户同意的吃法），
 * 吃法、饱食度、声音全走原版 eatFood；菜蘸过蘸料的话效果在 LivingEntityEatMixin 里生效。
 * Shift+右键锅开锅界面在这里（useOnBlock）；夹菜 / 放菜 / 蘸桌上的蘸料在各个方块里处理。
 * 作者：mryt233
 */
public class ChopsticksItem extends Item implements TableItem {
    private static final ThreadLocal<LivingEntity> DURATION_USER = new ThreadLocal<>();

    public ChopsticksItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return HotPotContent.CHOPSTICK_REST_BLOCK;
    }

    @Override
    public boolean autoPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void playAddSound(Level world, BlockPos pos, ItemStack stack) {
        world.playSound(null, pos, SoundEvents.BAMBOO_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
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
            return Use.item(UseOutcome.PASS, context.getLevel().isClientSide);
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
            return Use.item(UseOutcome.PASS, world.isClientSide);
        }
        if (world.getBlockState(pos).is(HotPotContent.STOVE_TOP)) {
            // 高灶台的上半截：当成点在下面的灶台上
            pos = pos.below();
            hit = new BlockHitResult(hit.getLocation(), hit.getDirection(), pos, hit.isInside());
        }
        BlockState state = world.getBlockState(pos);
        // Shift+右键桌上的碗 / 盘子：筷子（连夹着的菜）架在碗沿上（2026-10-27 用户：只架碗和盘子，不架锅）
        if (state.is(HotPotContent.PLATE_HOLDER) && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity holder) {
            // mryt233 改（r146e）：以实际点击坐标决定碗沿，避免用第二次射线串选旁边餐具。
            double hx = (context.getClickLocation().x - pos.getX()) * 16;
            double hz = (context.getClickLocation().z - pos.getZ()) * 16;
            PlateHolderBlockEntity.Placed plate = holder.at(hx, hz);
            if (plate == null) {
                // 点在盘子格的空白位置：把这一双竹筷（连夹着的菜）直接摆在桌上，渲染器会在筷尖下画迷你筷架。
                // 空白处没有盘子可放菜，所以夹着菜也照摆（r136b，2026-11-05 用户，新增）
                return Use.item(TableObjects.placeInHolderOrOn(context, this), world.isClientSide);
            }
            // mryt233 改：潜行绕过方块 useItemOn；夹菜点中心仍放菜/蘸料，点沿口连菜一起架放。
            double dx = hx - plate.x, dz = hz - plate.z;
            boolean rim = Math.max(Math.abs(dx) / plate.halfX(), Math.abs(dz) / plate.halfZ()) >= 0.65;
            if (!Chopsticks.held(context.getItemInHand()).isEmpty() && !rim) {
                return ((PlateHolderBlock) state.getBlock()).useItemOn(context.getItemInHand(), state, world,
                    pos, player, context.getHand(), hit).result();
            }
            if (!plate.restsChopsticks() || !plate.chopsticks.isEmpty()) {
                return Use.item(UseOutcome.PASS, world.isClientSide);
            }
            if (!world.isClientSide) {
                plate.chopsticks = context.getItemInHand().copy();
                double yaw = rim ? Math.toDegrees(Math.atan2(dx, -dz)) : player.getYRot();
                plate.chopsticksYaw = (float) ((Math.round(yaw / 22.5) * 22.5 % 360 + 360) % 360);
                player.setItemInHand(context.getHand(), ItemStack.EMPTY);
                holder.changed();
                world.playSound(null, pos, SoundEvents.BAMBOO_PLACE,
                    SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return Use.item(UseOutcome.SUCCESS_SOFT, world.isClientSide);
        }
        boolean pot = state.getBlock() instanceof HotPotBlock
            || (StoveBlock.isStove(state) && StoveBlock.isPotHit(state, pos, hit));
        if (pot && world.getBlockEntity(pos) instanceof HotPotBlockEntity be && be.hasPot()) {
            if (!world.isClientSide) {
                be.useWithChopsticks(player);
            }
            return Use.item(UseOutcome.SUCCESS_SOFT, world.isClientSide);
        }
        // 锅、灶台（含没架锅的灶台、高灶台的上半截）上不摆筷子
        if (state.getBlock() instanceof HotPotBlock || StoveBlock.isStove(state) || state.getBlock() instanceof StoveTopBlock) {
            return Use.item(UseOutcome.PASS, world.isClientSide);
        }
        // 桌面 / 地面等方块的顶面：Shift+右键把这双筷子直接摆在上面（r136b，2026-11-05 用户，新增）
        return Use.item(TableObjects.placeInHolderOrOn(context, this), world.isClientSide);
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
            return Use.itemUse(UseOutcome.PASS, stack, world.isClientSide);
        }
        DURATION_USER.set(user);
        try {
            user.startUsingItem(hand);
        } finally {
            DURATION_USER.remove();
        }
        return Use.itemUse(UseOutcome.CONSUME, stack, world.isClientSide);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return heldFood(stack) != null || Chopsticks.held(stack).isEmpty() ? UseAnim.EAT : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        FoodProperties food = heldFood(stack);
        if (food == null) {
            LivingEntity user = DURATION_USER.get();
            if (user != null) {
                food = HotPotHooks.offhandFood(stack, user);
            }
        }
        return food == null ? 0 : food.isFastFood() ? 16 : 32;
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
        user.eat(world, eaten);
        if (!world.isClientSide) {
            Chopsticks.hold(stack, ItemStack.EMPTY);
            if (user instanceof Player player && !player.getAbilities().instabuild) {
                FoodCompat.returnContainer(player, eaten);
            }
        }
        return stack;
    }
}
