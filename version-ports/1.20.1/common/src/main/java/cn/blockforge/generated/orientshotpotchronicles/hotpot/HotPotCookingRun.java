package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
                // mryt233 改（2026-11-04，清单 #108）：已经熟了但还留在锅里（有火、有汤）的，
                // 继续累加「煮过头」计时；到配置的时长就原地换成过头产物（热豆腐 → 热豆腐块）。
                // 没火 / 没汤的整段在上面就返回了，所以冷锅里不会继续煮老。
                dirty |= tickOvercook(be, i);
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

    /**
     * mryt233 改（2026-11-04，清单 #108）：熟了的格子继续在锅里煮，累加过头计时；到点原地换成过头产物。
     *
     * @return 这一格有没有变化（有变化就要同步 / 存盘）
     */
    private static boolean tickOvercook(HotPotBlockEntity be, int slot) {
        ItemStack current = be.ingredients.getItem(slot);
        if (current.isEmpty()) {
            return false;
        }
        Item product = HotPotCooking.overcookProduct(current.getItem());
        if (product == null) {
            return false;   // 这样熟食不会煮过头，一直留在锅里
        }
        be.overcook[slot]++;
        // 「总刻数」是从下锅算起的（含煮熟那一段），而这里只从熟了那一刻开始数，
        // 所以要把这一格已经花掉的煮熟时间补上，玩家改配置时看到的就是「从下锅算 30 秒」。
        int total = HotPotCooking.overcookTicks(be.getLevel(), current.getItem());
        int spent = Math.max(0, total - be.cookDur[slot]);
        if (be.overcook[slot] < spent) {
            return false;
        }
        // 原地换产物，份数不变（热豆腐 1 份碎成 1 份热豆腐块，用户定）
        ItemStack changed = new ItemStack(product, Math.max(1, current.getCount()));
        // mryt233 改（2026-11-05，清单 #126）：热豆腐在锅里煮过头碎成豆腐块，给锅边玩家记「豆腐久煮」成就
        fireNearbyCriterion(be.getLevel(), be, changed);
        // 换出来的产物仍然记得是在哪种汤里煮出来的，吃下去风味加成不丢
        stampSoup(changed, current);
        be.ingredients.setItem(slot, changed);
        be.tracked[slot] = changed.copy();
        be.overcook[slot] = 0;
        // 熟度仍是满的，cooked 保持 true：新产物是熟的（热豆腐块不能再放回锅里）
        be.doneness[slot] = HotPotCooking.FULL_DONE;
        be.syncPending = true;
        return true;
    }

    /**
     * mryt233 改（2026-11-05，清单 #117）：把「在哪锅汤里煮出来的」写到成品上，吃的时候据此给风味加成。
     * 红汤另外记辣度档位（0..3），辣度决定力量 / 速度 / 抗火 / 饥饿的强度（见 SoupBase#dishBonusEffects(int)）。
     * 1.20.1 走物品 NBT（ItemData / ItemDataKey），不是 1.21 的 DataComponentType。
     */
    private static void stampSoup(ItemStack result, SoupBase soup, int spicyLevel) {
        cn.blockforge.orient.framework.compat.ItemData.set(result, HotPotContent.COOKED_IN_SOUP, soup.textureKey());
        if (soup == SoupBase.SPICY) {
            cn.blockforge.orient.framework.compat.ItemData.set(result, HotPotContent.SPICY_LEVEL,
                Math.max(0, Math.min(3, spicyLevel)));
        }
    }

    /** 换产物时把原来那份上的汤底 / 辣度标记搬过去（没有标记就什么都不做）。 */
    private static void stampSoup(ItemStack result, ItemStack previous) {
        String soup = cn.blockforge.orient.framework.compat.ItemData.get(previous, HotPotContent.COOKED_IN_SOUP);
        if (soup == null) {
            return;
        }
        cn.blockforge.orient.framework.compat.ItemData.set(result, HotPotContent.COOKED_IN_SOUP, soup);
        Integer spicy = cn.blockforge.orient.framework.compat.ItemData.get(previous, HotPotContent.SPICY_LEVEL);
        if (spicy != null) {
            cn.blockforge.orient.framework.compat.ItemData.set(result, HotPotContent.SPICY_LEVEL, spicy);
        }
    }

    /** 煮熟瞬间原地换成熟食，成品等待玩家从同一槽位取走。 */
    private static void complete(HotPotBlockEntity be, Level world, int slot, SoupBase soup, ItemStack raw) {
        be.xpValue[slot] = HotPotCooking.xpFor(world, raw);
        ItemStack result = HotPotCooking.finish(world, raw, soup);
        // mryt233 改（2026-11-05，清单 #117）：记下这盘菜是在哪种汤底里煮出来的（红汤另记辣度档位），
        // 吃下去 / 用筷子夹着吃时按汤底给风味加成。
        int side = be.layout().sideOfSlot(slot);
        stampSoup(result, soup, side == PotLayout.NONE ? 0 : be.spicyLevels[side]);
        // 库存与变化快照各持一份独立拷贝，任何一方被清空都不会连带抹掉另一方。
        ItemStack stored = result.copy();
        be.ingredients.setItem(slot, stored);
        be.tracked[slot] = stored.copy();
        be.cooked[slot] = true;
        be.doneness[slot] = HotPotCooking.FULL_DONE;
        be.overcook[slot] = 0;   // mryt233 改（2026-11-04）：刚熟，过头计时从零开始
        be.syncPending = true;   // 一刻结束统一同步（代码检查 W3）
    }

    /**
     * mryt233 改（2026-11-05，清单 #121/#126）：玩家从锅里把这件熟食拿走 / 夹走 / 盛进碗时补的成就——
     * 三样熟虾算「偏爱（煮虾）」，热豆腐块算「豆腐久煮（煮过头）」。
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

    /** 锅边 8 格内的玩家都记一次「豆腐久煮」成就（豆腐在锅里煮过头碎成豆腐块时用）。 */
    private static void fireNearbyCriterion(Level world, HotPotBlockEntity be, ItemStack stack) {
        if (world.isClientSide || !stack.is(HotPotContent.HOT_TOFU_CUBE)) {
            return;
        }
        // mryt233 改（1.20.1 适配）：1.20.1 没有 AABB.encapsulatingFullBlocks，用两个角点构造同一个包围盒
        BlockPos min = be.getBlockPos().offset(-8, -8, -8);
        BlockPos max = be.getBlockPos().offset(8, 8, 8);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
            min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        for (Player nearby : world.getEntitiesOfClass(Player.class, box)) {
            HotPotCriterion.fire(nearby, "tofu_overcook");
        }
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
        // mryt233 改（2026-11-05，清单 #121/#126）：煮虾 / 豆腐久煮成就。取出这一下可能已经从槽里拿走了，
        // 用 tracked 里那份快照（煮熟 / 上次变化时记下的成品）判断取走的是什么。
        fireTakeCriteria(player, be.tracked[slot]);
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
