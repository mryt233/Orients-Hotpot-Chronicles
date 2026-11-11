// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.key;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.consumeHand;

/** 红汤辣度读写、调整及反馈；保留原先的边界判断。 */
final class HotPotSpice {
    private HotPotSpice() { }

    static final int SPICY_MAX = 3;

    static int spicyOfSide(HotPotBlockEntity be, int side) {
        return side >= 0 && side < be.spicyLevels.length ? be.spicyLevels[side] : 0;
    }

    static void setSpicyOfSide(HotPotBlockEntity be, int side, int level) {
        if (side >= 0 && side < be.spicyLevels.length) {
            be.spicyLevels[side] = Math.max(0, Math.min(SPICY_MAX, level));
        }
    }

    static Component spicyLevelText(int level) {
        return Component.translatable(key("spicy." + Math.max(0, Math.min(SPICY_MAX, level))));
    }

    static boolean isSpicyModifier(ItemStack stack) {
        return stack.is(HotPotContent.CHILI_PEPPER) || stack.is(Items.ICE)
            || stack.is(Items.PACKED_ICE) || stack.is(Items.BLUE_ICE);
    }

    static int iceDrop(ItemStack stack) {
        if (stack.is(Items.BLUE_ICE)) {
            return SPICY_MAX;
        }
        if (stack.is(Items.PACKED_ICE)) {
            return 2;
        }
        return 1;
    }

    static boolean adjustSpiciness(HotPotBlockEntity be, Player player, InteractionHand hand, int side) {
        if (be.getLevel() == null || be.getLevel().isClientSide || !be.hasPot()) {
            return false;
        }
        if (!be.layout().multiSoup()) {
            side = PotLayout.SIDE_A;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean chili = stack.is(HotPotContent.CHILI_PEPPER);
        boolean ice = stack.is(Items.ICE)
            || stack.is(Items.PACKED_ICE)
            || stack.is(Items.BLUE_ICE);
        if (!chili && !ice) {
            return false;
        }
        // 不是红汤（含这一边还没倒汤）：提示一句就吃掉这次点击，别让辣椒 / 冰块被当成食材塞进锅槽
        // mryt233 改（2026-11-10）：调辣度只对红汤有效，判定改走 isSpicy() 属性
        SoupBase sideSoup = SoupBase.byId(be.baseOfSide(side));
        if (sideSoup == null || !sideSoup.isSpicy()) {
            player.displayClientMessage(Component.translatable(key("msg.spicy_need_red_broth")), true);
            return true;
        }
        int spicy = be.spicyOfSide(side);
        if (chili) {
            if (spicy >= SPICY_MAX) {
                player.displayClientMessage(Component.translatable(key("msg.spicy_max")), true);
                return true;
            }
            be.setSpicyOfSide(side, spicy + 1);
            consumeHand(player, hand, Items.AIR);
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5F, 1.2F);
            be.spawnSpicyParticles(true);
            player.displayClientMessage(Component.translatable(key("msg.spicy_up"), spicyLevelText(spicy + 1)), true);
            if (spicy + 1 >= SPICY_MAX) {
                HotPotCriterion.fire(player, "max_spicy");   // 成就：吃辣吃到 3 级特辣（r136d 里配进度）
            }
        } else {
            if (spicy <= 0) {
                player.displayClientMessage(Component.translatable(key("msg.spicy_min")), true);
                return true;
            }
            int next = Math.max(0, spicy - iceDrop(stack));
            be.setSpicyOfSide(side, next);
            consumeHand(player, hand, Items.AIR);
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.3F);
            be.spawnSpicyParticles(false);
            player.displayClientMessage(Component.translatable(key("msg.spicy_down"), spicyLevelText(next)), true);
        }
        be.markDirtyAndSync();
        return true;
    }

    static void spawnSpicyParticles(HotPotBlockEntity be, boolean up) {
        if (!(be.getLevel() instanceof ServerLevel server)) {
            return;
        }
        Vec3 at = Vec3.atCenterOf(be.getBlockPos()).add(0, 0.95, 0);
        if (up) {
            server.sendParticles(ParticleTypes.FLAME,
                at.x, at.y, at.z, 14, 0.38, 0.12, 0.38, 0.02);
            server.sendParticles(ParticleTypes.LAVA,
                at.x, at.y, at.z, 3, 0.3, 0.1, 0.3, 0.0);
        } else {
            server.sendParticles(ParticleTypes.CLOUD,
                at.x, at.y, at.z, 14, 0.38, 0.12, 0.38, 0.02);
            server.sendParticles(ParticleTypes.SPLASH,
                at.x, at.y, at.z, 8, 0.3, 0.1, 0.3, 0.0);
        }
    }

    static int spicyOfSlot(HotPotBlockEntity be, int slot) {
        int side = be.layout().sideOfSlot(slot);
        return side == PotLayout.NONE ? 0 : be.spicyOfSide(side);
    }
}
