// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import cn.blockforge.orient.framework.compat.DataComponents;
import cn.blockforge.orient.framework.compat.ItemContainerContents;
import cn.blockforge.orient.framework.compat.ItemData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.consumeHand;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.key;

/** 汤的操作与汤面状态；查询仍由 HotPotSoup 负责（1.20.1 适配）。 */
final class HotPotSoupOps {
    private HotPotSoupOps() { }

    static void clearSoupState(HotPotBlockEntity be) {
        Arrays.fill(be.bases, "");
        Arrays.fill(be.spicyLevels, 0);   // mryt233 改（2026-11-05）：汤底没了，辣度一起归零（r136a）
        be.updateSoupVisual();
        be.markDirtyAndSync();
    }

    static void updateSoupVisual(HotPotBlockEntity be) {
        if (be.getLevel() == null) {
            return;
        }
        SoupVisual visual = be.blockSoupVisual();
        BlockState state = be.getLevel().getBlockState(be.getBlockPos());
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.getValue(StoveBlock.POT);
            BlockState updated = state.setValue(StoveBlock.POT, pot.withSoup(be.hasSoup()))
                .setValue(StoveBlock.SOUP, visual);
            be.getLevel().setBlock(be.getBlockPos(), updated, Block.UPDATE_CLIENTS);
        } else if (state.getBlock() instanceof HotPotBlock) {
            BlockState updated = state.setValue(HotPotBlock.HAS_SOUP, be.hasSoup())
                .setValue(HotPotBlock.SOUP, visual)
                .setValue(HotPotBlock.BOILING, be.hasFire() && be.hasSoup());
            be.getLevel().setBlock(be.getBlockPos(), updated, Block.UPDATE_CLIENTS);
        }
    }

    static void useWithChopsticks(HotPotBlockEntity be, Player player) {
        if (be.getLevel() == null || be.getLevel().isClientSide || !be.hasPot()) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            HotpotScreens.openPot(sp, be);
        }
    }

    static boolean canScoopWith(HotPotBlockEntity be, ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    static boolean scoopSoup(HotPotBlockEntity be, Player player, InteractionHand hand) {
        return be.scoopSoup(player, hand, PotLayout.SIDE_A);
    }

    static boolean scoopSoup(HotPotBlockEntity be, Player player, InteractionHand hand, int side) {
        if (be.getLevel() == null || be.getLevel().isClientSide || !be.hasPot()) {
            return false;
        }
        if (!be.layout().multiSoup()) {
            side = PotLayout.SIDE_A;
        }
        ItemStack stack = player.getItemInHand(hand);
        SoupDef soup = Soups.byIndex(be.baseOfSide(side));
        if (soup == null && be.baseOfSide(side) == SoupIds.UNKNOWN) {
            be.dropContents(side);
            be.setBaseOfSide(side, SoupIds.EMPTY);
            be.setSpicyOfSide(side, 0);
            be.updateSoupVisual();
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.0F);
            be.markDirtyAndSync();
            return true;
        }
        if (soup == null) {
            player.displayClientMessage(Component.translatable(key("msg.no_soup_to_scoop")), true);
            return false;
        }
        if (!stack.is(soup.remainder())) {
            // 菌汤：桶装不了蘑菇煲，就是把汤倒掉，桶还是空桶（r99）
            be.dropContents(side);
            be.setBaseOfSide(side, -1);
            be.setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：倒掉汤底，辣度归零（r136a）
            be.updateSoupVisual();
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.0F);
            be.markDirtyAndSync();
            return true;
        }
        be.dropContents(side);
        be.setBaseOfSide(side, -1);
        be.setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：舀走汤底，辣度归零（r136a）
        be.updateSoupVisual();
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(soup.source())));
        SoundEvent sound = soup.spicy() ? SoundEvents.BUCKET_FILL_LAVA
            : soup.remainder() == Items.BOWL ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL;
        be.getLevel().playSound(null, be.getBlockPos(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        be.markDirtyAndSync();
        return true;
    }

    static boolean serveWithBowl(HotPotBlockEntity be, Player player, InteractionHand hand, int bigCell) {
        if (be.getLevel() == null || be.getLevel().isClientSide || !be.hasPot()) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        List<ItemStack> scooped = new ArrayList<>();
        float xp = 0;
        List<Integer> order = new ArrayList<>();
        int soupSide = be.layout().sideOfCell(Math.max(0, bigCell));
        String soup = soupSide == PotLayout.NONE ? "" : be.bases[soupSide];
        for (int i = 0; i < 9; i++) {
            int cell = (Math.max(0, bigCell) + i) % 9;
            if (be.layout().usesCell(cell) && (!be.layout().multiSoup() || SoupIds.same(be.bases[be.layout().sideOfCell(cell)], soup))) {
                order.add(cell);
            }
        }
        int shown = be.shownIn(player, bigCell);
        if (shown >= 0 && be.slots.cooked()[shown]) {
            ItemStack food = be.slots.ingredients().getItem(shown).copy();
            xp += be.slots.xpValue()[shown] * food.getCount();
            be.slots.ingredients().setItem(shown, ItemStack.EMPTY);
            be.resetSlot(shown, ItemStack.EMPTY);
            scooped.add(food.copyWithCount(1));
            if (food.getCount() > 1) {
                be.slots.ingredients().setItem(shown, food.copyWithCount(food.getCount() - 1));
            }
        }
        outer:
        for (int cell : order) {
            int slot;
            while (scooped.size() < 3 && (slot = be.firstCookedSlotInCell(cell)) >= 0) {
                ItemStack food = be.slots.ingredients().getItem(slot).copy();
                xp += be.slots.xpValue()[slot] * food.getCount();
                be.slots.ingredients().setItem(slot, ItemStack.EMPTY);
                be.resetSlot(slot, ItemStack.EMPTY);
                scooped.add(food.copyWithCount(1));
                if (food.getCount() > 1) {
                    be.slots.ingredients().setItem(slot, food.copyWithCount(food.getCount() - 1));
                }
                if (scooped.size() >= 3) {
                    break outer;
                }
            }
        }
        ItemStack result;
        if (!scooped.isEmpty()) {
            HotPotCriterion.fire(player, "pot_cooked");
            for (ItemStack food : scooped) {
                if (food.is(HotPotContent.COOKED_SHRIMP) || food.is(HotPotContent.COOKED_PEELED_SHRIMP)
                    || food.is(HotPotContent.COOKED_SHRIMP_PASTE)) {
                    HotPotCriterion.fire(player, "cook_shrimp");
                }
            }
            result = new ItemStack(Items.BOWL);
            ItemData.set(result, DataComponents.CONTAINER,
                ItemContainerContents.fromItems(scooped));
            PlateItem.syncPlateModel(result);
            if (xp > 0) {
                ExperienceOrb.award((ServerLevel) be.getLevel(),
                    Vec3.atCenterOf(be.getBlockPos()).add(0, 0.6, 0), Math.max(1, Math.round(xp)));
            }
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.2F);
        } else if (Soups.byIndex(be.bowlSoup(bigCell)) != null) {
            SoupDef bowl = Soups.byIndex(be.bowlSoup(bigCell));
            ItemStack bowlStack = Soups.bowlStack(bowl);
            if (bowlStack.isEmpty()) {
                player.displayClientMessage(Component.translatable(key("msg.bowl_nothing")), true);
                return false;
            }
            result = bowlStack;
            if (bowl.spicy()) {
                int spicy = be.spicyOfSide(be.soupSideOf(bigCell, be.bowlSoup(bigCell)));
                if (spicy > 0) {
                    ItemData.set(result, HotPotContent.SPICY_LEVEL, spicy);
                }
            }
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable(key("msg.bowl_nothing")), true);
            return false;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
        be.markDirtyAndSync();
        return true;
    }

    static boolean pourSoup(HotPotBlockEntity be, Player player, InteractionHand hand) {
        return be.pourSoup(player, hand, PotLayout.SIDE_A);
    }

    static boolean pourSoup(HotPotBlockEntity be, Player player, InteractionHand hand, int side) {
        if (be.getLevel() == null || be.getLevel().isClientSide || !be.hasPot()) {
            return false;
        }
        if (!be.layout().multiSoup()) {
            side = PotLayout.SIDE_A;
        }
        ItemStack stack = player.getItemInHand(hand);
        SoupDef soup = Soups.fromStack(stack);
        if (soup == null) {
            return false;
        }
        if (soup.isAdditive(stack)) {
            int current = be.baseOfSide(side);
            if (current < 0) {
                player.displayClientMessage(Component.translatable(key("msg.base_needs_clear")), true);
                return false;
            }
            if (current != Soups.indexOf(Soups.clear())) {
                player.displayClientMessage(Component.translatable(key("msg.base_only_clear")), true);
                return false;
            }
            be.setBaseOfSide(side, Soups.indexOf(soup));
            be.setSpicyOfSide(side, 0);
            consumeHand(player, hand, Items.AIR);
            be.updateSoupVisual();
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.3F);
            be.markDirtyAndSync();
            return true;
        }
        if (be.baseOfSide(side) != SoupIds.EMPTY) {
            player.displayClientMessage(Component.translatable(key("msg.already_soup")), true);
            return false;
        }
        be.setBaseOfSide(side, Soups.indexOf(soup));
        be.setSpicyOfSide(side, 0);
        consumeHand(player, hand, soup.remainder());
        be.updateSoupVisual();
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.BUCKET_EMPTY,
            SoundSource.BLOCKS, 1.0F, 1.15F);
        be.markDirtyAndSync();
        return true;
    }

    static int soupSideOf(HotPotBlockEntity be, int bigCell, int soupId) {
        PotLayout layout = be.layout();
        int aimed = layout.sideOfCell(Math.max(0, bigCell));
        if (aimed != PotLayout.NONE && be.baseOfSide(aimed) == soupId) {
            return aimed;
        }
        for (int side = 0; side < layout.sides(); side++) {
            if (be.baseOfSide(side) == soupId) {
                return side;
            }
        }
        return aimed;
    }

    static SoupVisual soupVisual(HotPotBlockEntity be) {
        return HotPotSoup.visual(be);
    }

    static int baseOfSide(HotPotBlockEntity be, int side) {
        return HotPotSoup.baseOfSide(be, side);
    }

    static void setBaseOfSide(HotPotBlockEntity be, int side, int id) {
        HotPotSoup.setBaseOfSide(be, side, id);
    }

    static SoupDef soupForSlot(HotPotBlockEntity be, int slot) {
        return HotPotSoup.forSlot(be, slot);
    }

    static int bowlSoup(HotPotBlockEntity be, int bigCell) {
        return HotPotSoup.bowlSoup(be, bigCell);
    }

    static SoupVisual blockSoupVisual(HotPotBlockEntity be) {
        return HotPotSoup.blockVisual(be);
    }
}
