package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.world.phys.AABB;

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
        // mryt233 改（2026-11-10）：重载后先刷新耗时，熟度不回退；区块更新也通知客户端刷新。
        if (be.cookRevision != HotPotCooking.cookRevision()) {
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
        // mryt233 改（2026-11-07）：每边的汤底与速度只取一次。
        // 以前在 81 格循环里逐格 soup.speedMul()，等于每刻做 81 次配置树遍历。
        SoupBase[] soups = new SoupBase[PotLayout.MAX_SIDES];
        float[] speedMul = new float[PotLayout.MAX_SIDES];
        for (int side = 0; side < layout.sides() && side < PotLayout.MAX_SIDES; side++) {
            SoupBase base = SoupBase.byId(be.baseOfSide(side));
            soups[side] = base;
            speedMul[side] = base == null ? 1F : base.speedMul();
        }
        boolean dirty = false;
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            if (be.cooked[i]) {
                // mryt233 改（2026-11-04）：熟了的格子还要看「煮过头」——豆腐在锅里放久了会碎成豆腐块。
                if (overcook(be, world, i, layout, soups)) {
                    dirty = true;
                }
                continue;
            }
            ItemStack stack = be.ingredients.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            // 鸳鸯锅：每格用它那一边的汤；那边还没倒汤就不煮
            int side = layout.sideOfSlot(i);
            SoupBase soup = side == PotLayout.NONE || side >= PotLayout.MAX_SIDES ? null : soups[side];
            if (soup == null) {
                continue;
            }
            be.doneness[i] = Math.min(HotPotCooking.FULL_DONE,
                be.doneness[i] + HotPotCooking.progressPerTick(be.cookDur[i], power, speedMul[side]));
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
        // mryt233 改（2026-11-05，r136a）：熟了的菜记下自己是在哪种汤底里煮出来的，
        // 红汤煮的还记下那一格所在那一边的辣度档位；吃的时候按这两样给风味加成（LivingEntityEatMixin）。
        result.set(HotPotContent.COOKED_IN_SOUP, soup.textureKey());
        // mryt233 改（2026-11-10，r147）：红汤判断改成读 isSpicy() 属性
        if (soup.isSpicy()) {
            int spicy = be.spicyOfSlot(slot);
            if (spicy > 0) {
                result.set(HotPotContent.SPICY_LEVEL, spicy);
            }
        }
        // 库存与变化快照各持一份独立拷贝，任何一方被清空都不会连带抹掉另一方。
        ItemStack stored = result.copy();
        be.ingredients.setItem(slot, stored);
        be.tracked[slot] = stored.copy();
        be.cooked[slot] = true;
        be.doneness[slot] = HotPotCooking.FULL_DONE;
        be.overcook[slot] = 0;   // mryt233 改（2026-11-04）：煮熟那一刻重新开始算「煮过头」
        be.syncPending = true;   // 一刻结束统一同步（代码检查 W3）
    }

    /**
     * mryt233 改（2026-11-04）：熟食在锅里继续煮过头就换掉（豆腐 → 豆腐块）。
     * 只有登记过 {@link HotPotCooking#overcook} 的熟食会累加；到配置的总刻数就原地换成过头产物，份数不变。
     * 计时存在 {@code be.overcook[]}，会进存档，重启不丢。返回 true = 这一格变了（要标脏）。
     */
    private static boolean overcook(HotPotBlockEntity be, Level world, int slot, PotLayout layout,
                                    SoupBase[] soups) {
        ItemStack current = be.ingredients.getItem(slot);
        if (current.isEmpty()) {
            be.overcook[slot] = 0;
            return false;
        }
        // mryt233 改（2026-11-11）：久煮也保留配方物品栈的组件。
        ItemStack overcooked = HotPotCooking.overcookResult(world, current.getItem());
        if (overcooked.isEmpty()) {
            return false;   // 这样熟食不会煮过头，一直留在锅里
        }
        // 鸳鸯锅：这边没汤就不继续煮（和没熟的时候一致）；汤底与速度沿用 cook 里预算好的那一份
        int side = layout.sideOfSlot(slot);
        SoupBase soup = side == PotLayout.NONE || side >= PotLayout.MAX_SIDES ? null : soups[side];
        if (soup == null) {
            return false;
        }
        be.overcook[slot]++;
        // 「overcook_ticks」是从下锅算起的总刻数，减去煮熟本身用的那段才是煮熟后还要煮多久
        int total = HotPotCooking.overcookTicks(world, current.getItem());
        int afterCook = Math.max(1, total - Math.max(1, be.cookDur[slot]));
        if (be.overcook[slot] < afterCook) {
            return true;
        }
        int portions = Math.max(1, current.getCount());
        ItemStack result = overcooked.copyWithCount(portions);
        // mryt233 改（2026-11-05，r136a）：碎掉的豆腐块也是在原来那锅汤里煮出来的，汤底 / 辣度组件跟着带过去
        String cookedIn = current.get(HotPotContent.COOKED_IN_SOUP);
        if (cookedIn != null) {
            result.set(HotPotContent.COOKED_IN_SOUP, cookedIn);
            int spicy = current.getOrDefault(HotPotContent.SPICY_LEVEL, 0);
            if (spicy > 0) {
                result.set(HotPotContent.SPICY_LEVEL, spicy);
            }
        }
        // mryt233 改（2026-11-05，r136a）：热豆腐在锅里煮过头碎成豆腐块，给锅边 8 格内的玩家记一条「豆腐久煮」成就
        fireNearbyCriterion(world, be, result);
        be.ingredients.setItem(slot, result);
        be.tracked[slot] = result.copy();
        be.overcook[slot] = 0;
        be.doneness[slot] = HotPotCooking.FULL_DONE;
        be.syncPending = true;
        return true;
    }

    /**
     * mryt233 改（2026-11-05，r136a）：玩家从锅里把这件熟食拿走 / 夹走 / 盛进碗时补的成就——
     * 三样熟虾算「煮虾」，热豆腐块算「豆腐久煮（煮过头）」。事件名由成就树那边（r136d）配进度。
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

    /** 锅边 8 格内的玩家都记一次「这样东西是从锅里煮出来的」类成就（r136a，豆腐煮过头用）。 */
    private static void fireNearbyCriterion(Level world, HotPotBlockEntity be, ItemStack stack) {
        if (world.isClientSide || !stack.is(HotPotContent.HOT_TOFU_CUBE)) {
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
        if (world == null || world.isClientSide || count <= 0
            || slot < 0 || slot >= HotPotBlockEntity.SLOTS || !be.cooked[slot]) {
            return;
        }
        grant(be, be.xpValue[slot], count);
        HotPotCriterion.fire(player, "pot_cooked");
        // mryt233 改（2026-11-05，r136a）：煮虾 / 豆腐久煮成就。取出这一下可能已经从槽里拿走了，
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
