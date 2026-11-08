package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.inventory.Slot;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 玻璃杯（2026-10-25 用户）。空杯一组 16 个；倒满的杯子装一口饮料，喝完变回空杯。
 * 倒饮料：物品栏里拿着饮料瓶右键点空杯，或者拿饮料瓶右键桌上摆着的空杯。杯子能像瓶子一样摆在桌上。
 * 水、牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒，桶不会空；空杯对着水源右键也能舀一杯水。
 * 一杯水 / 牛奶不回饥饿值（同原版），牛奶清掉所有效果。
 * <p>碰杯（干杯，清单 #118）：拿着装满饮品的玻璃杯，右键另一位持杯玩家、潜行对空气举杯、
 * 或者拿杯右键桌上摆着的满杯，都能碰一次杯；碰过的杯子上记 CHEERED 标记，喝下去多回 1 饥饿值。
 * <p>mryt233 改（0.2.2-alpha，1.20.1 适配）：
 * ① 物品标记走 ItemData（NBT），不是 1.21 的 DataComponentType；
 * ② {@code getUseDuration(ItemStack, LivingEntity)} 在 1.20.1 是 {@code getUseDuration(ItemStack)} 单参；
 * ③ {@code appendHoverText} 1.20.1 签名是 {@code (ItemStack, @Nullable Level, List, TooltipFlag)}；
 * ④ 1.20.1 没有 {@code FoodData#eat(FoodProperties)}，用 {@code eat(int, float)} 两参版。
 * 作者：mryt233 */
public class CupItem extends Item {
    @Nullable
    private final DrinkKind kind;

    public CupItem(@Nullable DrinkKind kind) {
        super(new Item.Properties().stacksTo(kind == null ? 16 : 1));
        this.kind = kind;
    }

    /** 装的是哪种饮料；空杯是 null。 */
    @Nullable
    public DrinkKind kind() {
        return kind;
    }

    /** 手里的是水桶 / 奶桶：能倒出哪种；别的东西是 null。 */
    @Nullable
    public static DrinkKind bucketKind(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.WATER_BUCKET) ? DrinkKind.WATER
            : stack.is(net.minecraft.world.item.Items.MILK_BUCKET) ? DrinkKind.MILK : null;
    }

    public static boolean isEmptyCup(ItemStack stack) {
        return stack.getItem() instanceof CupItem cup && cup.kind == null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return PlateItem.placeOn(context, HotPotContent.CUP_BLOCK, true);
    }

    /** 物品栏里：空杯放在格子里，鼠标拿着饮料瓶右键点它 = 倒一杯（像原版收纳袋那样的点法）。 */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player,
                                            SlotAccess cursorStackReference) {
        DrinkKind bucket = bucketKind(otherStack);
        if (kind != null || clickType != ClickAction.SECONDARY || !(otherStack.getItem() instanceof DrinkItem || bucket != null)
            || !slot.mayPickup(player)) {
            return false;
        }
        ItemStack full;
        if (otherStack.getItem() instanceof DrinkItem drink) {
            full = new ItemStack(HotPotContent.cupFor(drink.kind()));
            cursorStackReference.set(DrinkItem.pourOne(otherStack));
        } else {
            full = new ItemStack(HotPotContent.cupFor(bucket));   // 桶不变：原版的桶无限倒
        }
        if (stack.getCount() == 1) {
            slot.set(full);
        } else {
            stack.shrink(1);
            slot.setChanged();
            player.getInventory().placeItemBackInInventory(full);
        }
        player.playSound(SoundEvents.BOTTLE_EMPTY, 0.8F, 1.1F);
        return true;
    }

    /**
     * 潜行 + 对空气右键：和身边 4 格内最近的一位持杯玩家碰杯；周围没人也照样给自己举一次杯（单人时也能干杯）。
     * 不潜行时保持原来「举杯就喝」的手感。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (kind != null && user.isShiftKeyDown()) {
            if (!world.isClientSide) {
                Player partner = nearestCupHolder(world, user);
                if (partner != null) {
                    cheers(world, user, stack, partner, heldCup(partner), midPoint(user, partner));
                } else {
                    // 周围没有持杯的人：自己举杯欢呼一下，单人时也能随时干杯
                    cheers(world, user, stack, null, null, user.getEyePosition().add(user.getLookAngle().scale(0.4)));
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }
        if (kind != null) {
            return ItemUtils.startUsingInstantly(world, user, hand);
        }
        // 空杯对着水源：舀一杯水（同原版玻璃瓶）
        net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(world, user, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
            && world.mayInteract(user, hit.getBlockPos())
            && world.getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
            world.playSound(user, user.getX(), user.getY(), user.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
            world.gameEvent(user, net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, hit.getBlockPos());
            user.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, user, new ItemStack(HotPotContent.WATER_CUP)), world.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * 右键另一位玩家：两边手里都端着装满饮品的玻璃杯时碰一次杯。
     * 目标玩家主手 / 副手任一只端着满杯就行。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind == null || !(target instanceof Player other) || other == player) {
            return InteractionResult.PASS;
        }
        ItemStack otherCup = heldCup(other);
        if (otherCup.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            cheers(player.level(), player, stack, other, otherCup, midPoint(player, other));
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /** 这位玩家主手或副手里端着的满杯；没有返回空。 */
    public static ItemStack heldCup(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof CupItem cup && cup.kind() != null) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 以玩家为中心半径 4 格内最近的、主手或副手端着满杯的其他玩家；没有返回 null。 */
    @Nullable
    public static Player nearestCupHolder(Level world, Player player) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player other : world.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(4.0),
            p -> p != player && p.isAlive() && !p.isSpectator() && !heldCup(p).isEmpty())) {
            double d = player.distanceToSqr(other);
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }
        return best;
    }

    /** 两人视线高度之间的中点：碰杯的声光和粒子都放在这儿。 */
    public static Vec3 midPoint(Player player, Player partner) {
        Vec3 a = player.getEyePosition();
        Vec3 b = partner.getEyePosition();
        return new Vec3((a.x + b.x) / 2, (a.y + b.y) / 2, (a.z + b.z) / 2);
    }

    /** 碰杯冷却（tick）：三个入口（潜行对空气、右键玩家、右键桌上的杯子）共用一份。 */
    private static final int CHEER_COOLDOWN = 20;

    /**
     * 碰杯（干杯）：清脆的双层碰杯声 + 一小捧粒子，两支杯子都记上 CHEERED 标记，
     * 双方各得 30 秒幸运 I、5 秒生命恢复 I，ActionBar 报「干杯！」，并触发 cheers 进度。
     * partner / partnerCup 为 null 表示一个人举杯（对空气干杯）。
     * 只在服务端调用：声音、粒子、标记、效果都由服务端决定。
     * mryt233 改（2026-11-07）：加 1 秒冷却，以前连点右键能无限续幸运 / 生命恢复。
     */
    public static void cheers(Level world, Player player, ItemStack cupStack,
                              @Nullable Player partner, @Nullable ItemStack partnerCup, Vec3 effectPos) {
        // 冷却中就直接返回：不再续增益、不再发包、不再写标记（三个入口都走这里，加在这里最省事）
        if (player.getCooldowns().isOnCooldown(cupStack.getItem())) {
            return;
        }
        player.getCooldowns().addCooldown(cupStack.getItem(), CHEER_COOLDOWN);
        world.playSound(null, effectPos.x, effectPos.y, effectPos.z, SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS, 1.0F, 1.5F);
        world.playSound(null, effectPos.x, effectPos.y, effectPos.z, SoundEvents.GLASS_PLACE,
            SoundSource.PLAYERS, 0.6F, 1.7F);
        if (world instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, effectPos.x, effectPos.y, effectPos.z, 8, 0.25, 0.25, 0.25, 0.0);
            server.sendParticles(ParticleTypes.NOTE, effectPos.x, effectPos.y, effectPos.z, 4, 0.2, 0.2, 0.2, 0.0);
            server.sendParticles(ParticleTypes.BUBBLE_POP, effectPos.x, effectPos.y, effectPos.z, 6, 0.2, 0.2, 0.2, 0.0);
        }
        cn.blockforge.orient.framework.compat.ItemData.set(cupStack, HotPotContent.CHEERED, Boolean.TRUE);
        if (partnerCup != null && !partnerCup.isEmpty()) {
            cn.blockforge.orient.framework.compat.ItemData.set(partnerCup, HotPotContent.CHEERED, Boolean.TRUE);
        }
        applyCheerBuff(player);
        player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.cheers"))
            .withStyle(ChatFormatting.GOLD), true);
        HotPotCriterion.fire(player, "cheers");
        if (partner != null && partner != player) {
            applyCheerBuff(partner);
            HotPotCriterion.fire(partner, "cheers");
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** 干杯的加成：30 秒幸运 I + 5 秒生命恢复 I。 */
    private static void applyCheerBuff(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == null ? UseAnim.NONE : UseAnim.DRINK;
    }

    /** mryt233 改（1.20.1 适配）：1.20.1 是单参 {@code getUseDuration(ItemStack)}。 */
    @Override
    public int getUseDuration(ItemStack stack) {
        return kind == null ? 0 : 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (kind == null) {
            return stack;
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.GENERIC_DRINK,
            SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        if (world.isClientSide) {
            return stack;
        }
        kind.applySip(user);
        // 碰过杯的杯子：在原来那一口之外再多回 1 饥饿值（清单 #118）——喝完就清掉标记
        boolean cheered = Boolean.TRUE.equals(
            cn.blockforge.orient.framework.compat.ItemData.get(stack, HotPotContent.CHEERED));
        if (user instanceof Player player) {
            if (kind.hasBottle()) {
                // 水、牛奶不回饥饿值（同原版）；数值同饮料瓶，读配置（W7）
                player.getFoodData().eat(DrinkItem.sipFood().getNutrition(), DrinkItem.sipFood().getSaturationModifier());
                // mryt233 改（2026-11-06，清单 #124）：杯装饮品喝下触发成就 authors_pick
                HotPotCriterion.fire(player, "drink_author");
            }
            if (cheered) {
                player.getFoodData().eat(1, 0.4F);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player.getAbilities().instabuild) {
                return stack;
            }
        }
        return new ItemStack(HotPotContent.GLASS_CUP);
    }

    /**
     * 碰过杯的杯子上挂一行金色说明。
     * mryt233 改（1.20.1 适配）：1.20.1 签名是 {@code (ItemStack, @Nullable Level, List, TooltipFlag)}。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag type) {
        if (Boolean.TRUE.equals(
            cn.blockforge.orient.framework.compat.ItemData.get(stack, HotPotContent.CHEERED))) {
            tooltip.add(Component.translatable(HotPotBlockEntity.key("tooltip.cheered")).withStyle(ChatFormatting.GOLD));
        }
    }
}
