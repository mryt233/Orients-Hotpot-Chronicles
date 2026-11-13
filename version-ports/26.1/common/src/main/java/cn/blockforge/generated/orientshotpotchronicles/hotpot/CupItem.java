// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 玻璃杯（2026-10-25 用户：杯子）。空杯一组 16 个；倒满的杯子装一口饮料（和瓶子的一口一样：回 2 饥饿 + 那种饮料的效果），
 * 喝完变回空杯。倒饮料：物品栏里拿着饮料瓶右键点空杯，或者拿饮料瓶右键桌上摆着的空杯（PlateHolderBlock），瓶子少一口。
 * 杯子能像瓶子一样摆在桌上。
 * 水、牛奶（2026-10-27 用户）：拿水桶 / 奶桶往空杯里倒（物品栏里点、或右键桌上的空杯），桶不会空；空杯对着水源右键舀一杯水（同原版玻璃瓶）。
 * 碰杯（干杯，r136b，2026-11-05 用户）：拿着装满饮品的玻璃杯，右键另一位持杯玩家、潜行对空气举杯、
 * 或者拿杯右键桌上摆着的满杯，都能碰一次杯；碰过的杯子上记 CHEERED 组件。作者：mryt233 */
public class CupItem extends Item implements TableItem {
    @Nullable
    private final DrinkKind kind;

    public CupItem(@Nullable DrinkKind kind, Item.Properties properties) {
        super(properties.stacksTo(kind == null ? 16 : 1));
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
        return stack.is(Items.WATER_BUCKET) ? DrinkKind.WATER
            : stack.is(Items.MILK_BUCKET) ? DrinkKind.MILK : null;
    }

    public static boolean isEmptyCup(ItemStack stack) {
        return stack.getItem() instanceof CupItem cup && cup.kind == null;
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return HotPotContent.CUP_BLOCK;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult poured = DrinkCauldronBlock.pourIntoEmpty(context);
        if (poured != InteractionResult.PASS) {
            return poured;
        }
        return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide());
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
            full = new ItemStack(HotPotContent.cupFor(bucket));   // 桶不变：原版的桶无限倒（用户）
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
     */
    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (kind != null && user.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                Player partner = nearestCupHolder(world, user);
                if (partner != null) {
                    cheers(world, user, stack, partner, heldCup(partner), midPoint(user, partner));
                } else {
                    cheers(world, user, stack, null, null, user.getEyePosition().add(user.getLookAngle().scale(0.4)));
                }
            }
            return Use.itemUse(UseOutcome.SUCCESS_SOFT, stack, world.isClientSide());
        }
        if (kind != null) {
            return ItemUtils.startUsingInstantly(world, user, hand);
        }
        // 空杯对着水源：舀一杯水（同原版玻璃瓶）
        BlockHitResult hit = getPlayerPOVHitResult(world, user, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK
            && world.mayInteract(user, hit.getBlockPos())
            && world.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            world.playSound(user, user.getX(), user.getY(), user.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
            world.gameEvent(user, GameEvent.FLUID_PICKUP, hit.getBlockPos());
            user.awardStat(Stats.ITEM_USED.get(this));
            return Use.itemUse(UseOutcome.SUCCESS_SOFT,
                ItemUtils.createFilledResult(stack, user, new ItemStack(HotPotContent.WATER_CUP)), world.isClientSide());
        }
        return Use.itemUse(UseOutcome.PASS, stack, world.isClientSide());
    }

    /**
     * 右键另一位玩家：两边手里都端着装满饮品的玻璃杯时碰一次杯。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind == null || !(target instanceof Player other) || other == player) {
            return Use.item(UseOutcome.PASS, player.level().isClientSide());
        }
        ItemStack otherCup = heldCup(other);
        if (otherCup.isEmpty()) {
            return Use.item(UseOutcome.PASS, player.level().isClientSide());
        }
        if (!player.level().isClientSide()) {
            cheers(player.level(), player, stack, other, otherCup, midPoint(player, other));
        }
        return Use.item(UseOutcome.SUCCESS_SOFT, player.level().isClientSide());
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

    /**
     * 碰杯（干杯）：清脆的双层碰杯声 + 一小捧粒子，两支杯子都记上 CHEERED 组件，
     * 双方各得幸运与生命恢复（时长与等级由 Config.cheers* 控制），ActionBar 报「干杯！」，并触发 cheers 进度。
     */
    public static void cheers(Level world, Player player, ItemStack cupStack,
                              @Nullable Player partner, @Nullable ItemStack partnerCup, Vec3 effectPos) {
        if (player.getCooldowns().isOnCooldown(cupStack)) {
            return;
        }
        player.getCooldowns().addCooldown(cupStack, Math.max(1, Config.cheersCooldownTicks()));
        world.playSound(null, effectPos.x, effectPos.y, effectPos.z, SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS, 1.0F, 1.5F);
        world.playSound(null, effectPos.x, effectPos.y, effectPos.z, SoundEvents.GLASS_PLACE,
            SoundSource.PLAYERS, 0.6F, 1.7F);
        if (world instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, effectPos.x, effectPos.y, effectPos.z, 8, 0.25, 0.25, 0.25, 0.0);
            server.sendParticles(ParticleTypes.NOTE, effectPos.x, effectPos.y, effectPos.z, 4, 0.2, 0.2, 0.2, 0.0);
            server.sendParticles(ParticleTypes.BUBBLE_POP, effectPos.x, effectPos.y, effectPos.z, 6, 0.2, 0.2, 0.2, 0.0);
        }
        cupStack.set(HotPotContent.CHEERED, true);
        if (partnerCup != null && !partnerCup.isEmpty()) {
            partnerCup.set(HotPotContent.CHEERED, true);
        }
        applyCheerBuff(player);
        player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.cheers"))
            .withStyle(ChatFormatting.GOLD));
        HotPotCriterion.fire(player, "cheers");
        if (partner != null && partner != player) {
            applyCheerBuff(partner);
            HotPotCriterion.fire(partner, "cheers");
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** 干杯的加成：默认 30 秒幸运 I + 5 秒生命恢复 I，duration <= 0 时跳过对应效果（mryt233 改）。 */
    private static void applyCheerBuff(Player player) {
        int luckDuration = Config.cheersLuckDuration();
        if (luckDuration > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, luckDuration, Math.max(0, Config.cheersLuckAmplifier())));
        }
        int regenDuration = Config.cheersRegenDuration();
        if (regenDuration > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, Math.max(0, Config.cheersRegenAmplifier())));
        }
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return kind == null ? ItemUseAnimation.NONE : ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return kind == null ? 0 : 32;
    }

    // mryt233 改：26.x 进食过程的声音和碎渣只认物品自带的 CONSUMABLE
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (kind == null) {
            return;
        }
        Consumable drink = Consumables.DEFAULT_DRINK;
        if (drink.shouldEmitParticlesAndSounds(ticksRemaining)) {
            drink.emitParticlesAndSounds(user.getRandom(), user, stack, 5);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        return DrinkSip.finish(stack, world, user, kind, this, false);
    }

    /** 碰过杯的杯子上挂一行金色说明（r136b，2026-11-05 用户，新增）。 */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        if (Boolean.TRUE.equals(stack.get(HotPotContent.CHEERED))) {
            tooltip.accept(Component.translatable(HotPotBlockEntity.key("tooltip.cheered")).withStyle(ChatFormatting.GOLD));
        }
    }
}
