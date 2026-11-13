// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端烹饪推进与出锅结算（从 {@link HotPotBlockEntity} 抽出）。
 * 只负责「熟度怎么涨、熟了怎么换、经验怎么给」，不碰渲染、不碰存档格式。
 * 状态由 {@link HotPotSlots} 持有，通过部件访问。
 * 作者：mryt233 */
final class HotPotCookingRun {
    private HotPotCookingRun() {
    }

    /** 每服务端 tick 调用：修状态、跟槽位变化、按热源和汤底推进熟度。 */
    static void tick(HotPotBlockEntity be, Level world) {
        if (be.needsVisualRepair) {
            be.needsVisualRepair = false;
            be.repairVisualState();
            be.recomputeCookTimes();   // 读档时查不到配方，这里补算（代码检查 B3）
        }
        if (be.slots.cookRevision() != HotPotCooking.cookRevision()) {
            be.recomputeCookTimes();
            be.syncPending = true;
        }
        be.trackSlotChanges();
        cook(be, world);
        if (be.syncPending) {
            be.markDirtyAndSync();     // 这一刻的改动只同步一次（代码检查 W3）
        }
    }

    private static void cook(HotPotBlockEntity be, Level world) {
        StoveBlockEntity stove = be.stoveBelow();
        boolean stoveHeat = stove != null && stove.burning();
        boolean openFire = stove == null && be.heatSourceBelow();
        be.updateStandaloneBoilingVisual((stoveHeat || openFire) && be.hasSoup());
        if (!be.hasPot() || !(stoveHeat || openFire) || !be.hasSoup()) {
            return;
        }
        int warmthDuration = Config.warmthDuration();
        if (openFire && warmthDuration > 0 && world.getGameTime() % Math.max(1, Config.warmthIntervalTicks()) == 0L) {
            StoveBlockEntity.giveWarmth(world, be.getBlockPos(), warmthDuration);
        }
        float power = stoveHeat ? stove.power() : HotPotBlockEntity.OPEN_FIRE_POWER;
        PotLayout layout = be.layout();
        SoupDef[] soups = new SoupDef[PotLayout.MAX_SIDES];
        float[] speedMul = new float[PotLayout.MAX_SIDES];
        for (int side = 0; side < layout.sides() && side < PotLayout.MAX_SIDES; side++) {
            SoupDef base = Soups.byIndex(be.baseOfSide(side));
            soups[side] = base;
            speedMul[side] = base == null ? 1F : base.speedMul();
        }
        boolean dirty = false;
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            if (be.slots.cooked()[i]) {
                if (overcook(be, world, i, layout, soups)) {
                    dirty = true;
                }
                continue;
            }
            ItemStack stack = be.slots.ingredients().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            int side = layout.sideOfSlot(i);
            SoupDef soup = side == PotLayout.NONE || side >= PotLayout.MAX_SIDES ? null : soups[side];
            if (soup == null) {
                continue;
            }
            be.slots.doneness()[i] = Math.min(HotPotCooking.FULL_DONE,
                be.slots.doneness()[i] + HotPotCooking.progressPerTick(be.slots.cookDur()[i], power, speedMul[side]));
            dirty = true;
            if (be.slots.doneness()[i] >= HotPotCooking.FULL_DONE) {
                complete(be, world, i, soup, stack);
            }
        }
        if (dirty && world.getGameTime() % 10L == 0L) {
            be.setChanged();
        }
    }

    /** 煮熟瞬间原地换成熟食，成品等待玩家从同一槽位取走。 */
    private static void complete(HotPotBlockEntity be, Level world, int slot, SoupDef soup, ItemStack raw) {
        be.slots.xpValue()[slot] = HotPotCooking.xpFor(world, raw);
        ItemStack result = HotPotCooking.finishWithSoup(world, raw, soup);
        result.set(HotPotContent.COOKED_IN_SOUP, SoupIds.cookedKey(soup.id()));
        if (soup.spicy()) {
            int spicy = be.spicyOfSlot(slot);
            if (spicy > 0) {
                result.set(HotPotContent.SPICY_LEVEL, spicy);
            }
        }
        ItemStack stored = result.copy();
        be.slots.ingredients().setItem(slot, stored);
        be.slots.tracked()[slot] = stored.copy();
        be.slots.cooked()[slot] = true;
        be.slots.doneness()[slot] = HotPotCooking.FULL_DONE;
        be.slots.overcook()[slot] = 0;
        be.syncPending = true;
    }

    /**
     * 熟食在锅里继续煮过头就换掉（豆腐 → 豆腐块）。
     */
    private static boolean overcook(HotPotBlockEntity be, Level world, int slot, PotLayout layout,
                                    SoupDef[] soups) {
        ItemStack current = be.slots.ingredients().getItem(slot);
        if (current.isEmpty()) {
            be.slots.overcook()[slot] = 0;
            return false;
        }
        ItemStack overcooked = HotPotCooking.overcookResult(world, current.getItem());
        if (overcooked.isEmpty()) {
            return false;
        }
        int side = layout.sideOfSlot(slot);
        SoupDef soup = side == PotLayout.NONE || side >= PotLayout.MAX_SIDES ? null : soups[side];
        if (soup == null) {
            return false;
        }
        be.slots.overcook()[slot]++;
        int total = HotPotCooking.overcookTicks(world, current.getItem());
        int afterCook = Math.max(1, total - Math.max(1, be.slots.cookDur()[slot]));
        if (be.slots.overcook()[slot] < afterCook) {
            return true;
        }
        int portions = Math.max(1, current.getCount());
        ItemStack result = overcooked.copyWithCount(portions);
        String cookedIn = current.get(HotPotContent.COOKED_IN_SOUP);
        if (cookedIn != null) {
            result.set(HotPotContent.COOKED_IN_SOUP, cookedIn);
            int spicy = current.getOrDefault(HotPotContent.SPICY_LEVEL, 0);
            if (spicy > 0) {
                result.set(HotPotContent.SPICY_LEVEL, spicy);
            }
        }
        fireNearbyCriterion(world, be, result);
        be.slots.ingredients().setItem(slot, result);
        be.slots.tracked()[slot] = result.copy();
        be.slots.overcook()[slot] = 0;
        be.slots.doneness()[slot] = HotPotCooking.FULL_DONE;
        be.syncPending = true;
        return true;
    }

    /**
     * 玩家从锅里把这件熟食拿走 / 夹走 / 盛进碗时补的成就。
     */
    static void fireTakeCriteria(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) {
            return;
        }
        if (stack.is(HotPotContent.COOKED_SHRIMP) || stack.is(HotPotContent.COOKED_PEELED_SHRIMP)
            || stack.is(HotPotContent.COOKED_SHRIMP_PASTE)) {
            HotPotCriterion.fire(player, "cook_shrimp");
        } else if (stack.is(HotPotContent.HOT_TOFU_CUBE)) {
            HotPotCriterion.fire(player, "tofu_overcook");
        }
    }

    /** 锅边 8 格内的玩家都记一次「这样东西是从锅里煮出来的」类成就（豆腐煮过头用）。 */
    private static void fireNearbyCriterion(Level world, HotPotBlockEntity be, ItemStack stack) {
        if (world.isClientSide() || !stack.is(HotPotContent.HOT_TOFU_CUBE)) {
            return;
        }
        AABB box = AABB.encapsulatingFullBlocks(
            be.getBlockPos().offset(-8, -8, -8), be.getBlockPos().offset(8, 8, 8));
        for (Player nearby : world.getEntitiesOfClass(Player.class, box)) {
            HotPotCriterion.fire(nearby, "tofu_overcook");
        }
    }

    /** 取出成品时结算经验，经验球在锅的位置生成。 */
    static void grantTakeXp(HotPotBlockEntity be, Player player, int slot, int count) {
        Level world = be.getLevel();
        if (world == null || world.isClientSide() || count <= 0
            || slot < 0 || slot >= HotPotBlockEntity.SLOTS || !be.slots.cooked()[slot]) {
            return;
        }
        grant(be, be.slots.xpValue()[slot], count);
        HotPotCriterion.fire(player, "pot_cooked");
        fireTakeCriteria(player, be.slots.tracked()[slot]);
        be.slots.tracked()[slot] = be.slots.ingredients().getItem(slot).copy();
        be.setChanged();
    }

    /** 直接按份给经验（已经知道自己取走了几份时用）。 */
    static void grantExperience(HotPotBlockEntity be, float perItem, int count) {
        grant(be, perItem, count);
    }

    private static void grant(HotPotBlockEntity be, float perItem, int count) {
        be.slots.pendingXp(be.slots.pendingXp() + perItem * count);
        int whole = (int) be.slots.pendingXp();
        if (whole > 0) {
            be.slots.pendingXp(be.slots.pendingXp() - whole);
            Level world = be.getLevel();
            if (world instanceof ServerLevel server) {
                ExperienceOrb.award(server, Vec3.atCenterOf(be.getBlockPos()).add(0.0, 0.7, 0.0), whole);
            }
        }
    }
}
