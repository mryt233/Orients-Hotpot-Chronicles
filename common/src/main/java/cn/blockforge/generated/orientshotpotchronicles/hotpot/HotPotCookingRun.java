package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * 服务端烹饪推进与出锅结算（从 {@link HotPotBlockEntity} 抽出）。
 * 只负责「熟度怎么涨、熟了怎么换、经验怎么给」，不碰渲染、不碰存档格式。
 * 状态仍存在 {@link HotPotBlockEntity} 的槽位数组里（同包可见）。
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
        be.trackSlotChanges();
        cook(be, world);
        if (be.syncPending) {
            be.markDirtyAndSync();     // 这一刻的改动只同步一次（代码检查 W3）
        }
    }

    private static void cook(HotPotBlockEntity be, Level world) {
        StoveBlockEntity stove = be.stoveBelow();
        boolean stoveHeat = stove != null && stove.burning();
        // r102：独立锅摆在点着的篝火（热源标签）上也能煮，速度同灶台烧煤，不耗燃料
        boolean openFire = stove == null && be.heatSourceBelow();
        be.updateStandaloneBoilingVisual((stoveHeat || openFire) && be.hasSoup());
        if (!be.hasPot() || !(stoveHeat || openFire) || !be.hasSoup()) {
            return;
        }
        if (openFire && world.getGameTime() % StoveBlockEntity.WARMTH_INTERVAL == 0L) {
            StoveBlockEntity.giveWarmth(world, be.getBlockPos());   // 围锅回血，同灶台（用户要）
        }
        float power = stoveHeat ? stove.power() : HotPotBlockEntity.OPEN_FIRE_POWER;
        PotLayout layout = be.layout();   // 这一刻的格局只取一次（代码检查 W2）
        boolean dirty = false;
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            if (be.cooked[i]) {
                continue;
            }
            ItemStack stack = be.ingredients.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            // 鸳鸯锅：每格用它那一边的汤；那边还没倒汤就不煮
            int side = layout.sideOfSlot(i);
            SoupBase soup = side == PotLayout.NONE ? null : SoupBase.byId(be.baseOfSide(side));
            if (soup == null) {
                continue;
            }
            be.doneness[i] = Math.min(HotPotCooking.FULL_DONE,
                be.doneness[i] + HotPotCooking.progressPerTick(be.cookDur[i], power, soup.speedMul()));
            dirty = true;
            if (be.doneness[i] >= HotPotCooking.FULL_DONE) {
                complete(be, world, i, soup, stack);
            }
        }
        if (dirty && world.getGameTime() % 10L == 0L) {
            be.setChanged();
        }
    }

    /** 煮熟瞬间原地换成熟食，成品等待玩家从同一槽位取走。 */
    private static void complete(HotPotBlockEntity be, Level world, int slot, SoupBase soup, ItemStack raw) {
        be.xpValue[slot] = HotPotCooking.xpFor(world, raw);
        ItemStack result = HotPotCooking.finish(world, raw, soup);
        // 库存与变化快照各持一份独立拷贝，任何一方被清空都不会连带抹掉另一方。
        ItemStack stored = result.copy();
        be.ingredients.setItem(slot, stored);
        be.tracked[slot] = stored.copy();
        be.cooked[slot] = true;
        be.doneness[slot] = HotPotCooking.FULL_DONE;
        be.syncPending = true;   // 一刻结束统一同步（代码检查 W3）
    }

    /** 取出成品时结算经验，经验球在锅的位置生成。 */
    static void grantTakeXp(HotPotBlockEntity be, Player player, int slot, int count) {
        Level world = be.getLevel();
        if (world == null || world.isClientSide || count <= 0
            || slot < 0 || slot >= HotPotBlockEntity.SLOTS || !be.cooked[slot]) {
            return;
        }
        grant(be, be.xpValue[slot], count);
        HotPotCriterion.fire(player, "pot_cooked");
        be.tracked[slot] = be.ingredients.getItem(slot).copy();
        be.setChanged();
    }

    /** 直接按份给经验（已经知道自己取走了几份时用）。 */
    static void grantExperience(HotPotBlockEntity be, float perItem, int count) {
        grant(be, perItem, count);
    }

    private static void grant(HotPotBlockEntity be, float perItem, int count) {
        be.pendingXp += perItem * count;
        int whole = (int) be.pendingXp;
        if (whole > 0) {
            be.pendingXp -= whole;
            Level world = be.getLevel();
            if (world instanceof ServerLevel server) {
                ExperienceOrb.award(server, Vec3.atCenterOf(be.getBlockPos()).add(0.0, 0.7, 0.0), whole);
            }
        }
    }
}
