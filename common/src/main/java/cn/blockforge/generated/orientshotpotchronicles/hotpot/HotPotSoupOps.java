// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.key;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.consumeHand;

/** 汤的操作与汤面状态；查询仍由 HotPotSoup 负责。 */
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
        // mryt233 改（2026-11-10，r147）：红汤用岩浆灌桶声，改成读 isSpicy() 属性
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
        // 鸳鸯 / 田字锅只盛和准星那一边同一种汤底里的菜（两边汤底一样就都能盛；用户定）
        int soupSide = be.layout().sideOfCell(Math.max(0, bigCell));
        String soup = soupSide == PotLayout.NONE ? "" : be.bases[soupSide];
        for (int i = 0; i < 9; i++) {
            int cell = (Math.max(0, bigCell) + i) % 9;
            if (be.layout().usesCell(cell) && (!be.layout().multiSoup() || SoupIds.same(be.bases[be.layout().sideOfCell(cell)], soup))) {
                order.add(cell);
            }
        }
        // 先盛你看到浮在准星那格上的那一样（用户 2026-10-26：和筷子一样；它还生着就跳过），再照原来的顺序盛
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
                // 一格里只有 1 个；万一有多个，多出来的放回去
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
            // mryt233 改（2026-11-05，r136a）：碗里盛到的熟虾也算「煮虾」成就。
            // pot_cooked 之外再记一次；豆腐块是煮过头的，盛到碗里不再另记（免得重复触发）
            for (ItemStack food : scooped) {
                if (food.is(HotPotContent.COOKED_SHRIMP) || food.is(HotPotContent.COOKED_PEELED_SHRIMP)
                    || food.is(HotPotContent.COOKED_SHRIMP_PASTE)) {
                    HotPotCriterion.fire(player, "cook_shrimp");
                }
            }
            result = new ItemStack(Items.BOWL);
            result.set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(scooped));
            PlateItem.syncPlateModel(result);
            if (xp > 0) {
                ExperienceOrb.award((ServerLevel) be.getLevel(),
                    Vec3.atCenterOf(be.getBlockPos()).add(0, 0.6, 0), Math.max(1, Math.round(xp)));
            }
            be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.2F);
        } else if (Soups.byIndex(be.bowlSoup(bigCell)) != null) {
            // mryt233 改（2026-11-05）：盛出来的红汤碗带着这一边的辣度档位，喝的时候按它给效果（r136a）
            // mryt233 改（2026-11-10，r147）：红汤判断改成读 isSpicy() 属性
            // mryt233 改（2026-11-13，4F-1）：支持子模组登记的专属汤碗；暂无专属碗（4F-3 前）不崩
            SoupDef bowl = Soups.byIndex(be.bowlSoup(bigCell));
            Item bowlItem = Soups.bowlItem(bowl);
            if (bowlItem == null) {
                player.displayClientMessage(Component.translatable(key("msg.bowl_nothing")), true);
                return false;
            }
            result = new ItemStack(bowlItem);
            if (bowl.spicy()) {
                int spicy = be.spicyOfSide(be.soupSideOf(bigCell, be.bowlSoup(bigCell)));
                if (spicy > 0) {
                    result.set(HotPotContent.SPICY_LEVEL, spicy);
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
            // 锅底料（番茄 / 三鲜）：只能加进清汤里，把这边的清汤变成这种汤；没汤先倒清汤，别的汤（含已经加过的）不能再加（用户 2026-10-26）
            int current = be.baseOfSide(side);
            if (current < 0) {
                player.displayClientMessage(Component.translatable(key("msg.base_needs_clear")), true);
                return false;
            }
            // mryt233 改（2026-11-10）：锅底料只能加进清汤——清汤是唯一允许当底子的汤，这里保留按具体汤底判断
            if (current != Soups.indexOf(Soups.clear())) {
                player.displayClientMessage(Component.translatable(key("msg.base_only_clear")), true);
                return false;
            }
            be.setBaseOfSide(side, Soups.indexOf(soup));
            be.setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：换汤底，辣度回到 0 档（r136a）
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
        be.setSpicyOfSide(side, 0);   // mryt233 改（2026-11-05）：倒新汤，辣度回到 0 档（微辣）（r136a）
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
