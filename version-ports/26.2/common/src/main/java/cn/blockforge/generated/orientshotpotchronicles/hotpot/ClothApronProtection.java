// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 布围裙的防溅油 / 防烫保护（2026-11-06 用户定）。
 *
 * <p>用户要的效果：玩家穿戴布围裙时，「在火锅边操作时避免被热油 / 沸水溅射受伤」。</p>
 *
 * <p>判定拆成两半，全靠 {@link cn.blockforge.generated.orientshotpotchronicles.mixin.LivingEntityEatMixin}
 * 的两个注入点调用（核心层只放纯逻辑，不依赖加载器）：</p>
 * <ul>
 *   <li><b>挡伤害</b>：{@code LivingEntity.hurt} 的 HEAD 处，穿着布围裙且身边 4 格内有热源时，
 *       火焰 / 营火 / 热地板这几类烫伤直接取消（{@link #blocksBurn}）。</li>
 *   <li><b>灭火苗</b>：{@code LivingEntity.baseTick} 的 TAIL 处，穿着布围裙、身上真着了火且身边有热源时，
 *       把溅到身上的火苗清掉（{@link #tickOne}），并把「整装掌勺」成就补上。</li>
 * </ul>
 *
 * <p>只挡「站在火锅 / 灶台边被溅到」这一种：身边 4 格内没有热源时完全不干预。
 * 布围裙是布做的，挡的是溅起来的油花和蒸汽，不是整片火海。</p>
 *
 * 作者：mryt233 */
public final class ClothApronProtection {
    /** 挡溅射的范围（格）：布围裙挡的是近处的油花。 */
    public static final int RANGE = 4;

    /** 热源判定的缓存时长（刻）：同一处、两刻内的重复判定只扫一次 729 格。 */
    private static final long HEAT_CACHE_TICKS = 2L;

    /** 一次热源判定的结果（哪一刻、哪个维度、哪个方块位置、热不热）。 */
    private record HeatStamp(long gameTime, ResourceKey<Level> dimension, BlockPos pos, boolean hot) {
    }

    private static final Map<UUID, HeatStamp> HEAT_CACHE = new ConcurrentHashMap<>();

    /** mryt233 改（2026-11-10）：玩家下线、客户端退服和服务端停止时释放缓存。 */
    public static void removePlayer(UUID player) {
        HEAT_CACHE.remove(player);
    }

    public static void clearCache() {
        HEAT_CACHE.clear();
    }

    /** 成就「整装掌勺」的检查间隔（刻）：以前每 tick 都查一次成就表。 */
    private static final int CRITERION_INTERVAL = 20;

    /** 挡伤害时磨损围裙的间隔（刻）：同一段火里不必每 tick 掉一点耐久。 */
    private static final int WEAR_INTERVAL = 20;

    private ClothApronProtection() {
    }

    /**
     * 穿着布围裙时，这一下「烫伤」是否被挡住。给 {@code LivingEntity.hurt} 的注入点用。
     */
    public static boolean blocksBurn(Player player, DamageSource source) {
        if (player == null || source == null || !ClothApronItem.wornBy(player)) {
            return false;
        }
        // mryt233 改（2026-11-07）：岩浆伤害不豁免——布围裙挡的是溅过来的热油，挡不住整片岩浆
        if (source.is(DamageTypes.LAVA) || player.isInLava()) {
            return false;
        }
        if (!source.is(DamageTypes.IN_FIRE) && !source.is(DamageTypes.ON_FIRE)
            && !source.is(DamageTypes.HOT_FLOOR) && !source.is(DamageTypes.CAMPFIRE)) {
            return false;   // 不是烫伤，照常结算
        }
        if (!hasNearbyHeatSource(player)) {
            return false;
        }
        // mryt233 改（2026-11-07）：挡下来就要磨损（只是节流，围裙不再是「永久免费防溅射」）
        if (player.tickCount % WEAR_INTERVAL == 0) {
            wear(player, 1);
        }
        return true;
    }

    /**
     * 每 tick 对单个玩家调用（{@code LivingEntity.baseTick} 注入点）：穿着布围裙就补成就，
     * 身上真着了火、且身边有热源时把火苗灭掉。
     */
    public static void tickOne(Player player) {
        if (player == null || player.level().isClientSide() || !ClothApronItem.wornBy(player)) {
            return;
        }
        // 穿着布围裙就解锁「整装掌勺」，不必非在锅边；每 20 tick 查一次足够
        if (player.tickCount % CRITERION_INTERVAL == 0) {
            HotPotCriterion.fire(player, "wear_apron");
        }
        // 身上没着火就到此为止：下面那 729 格扫描只服务于「灭火苗」，一步都不该跑
        if (!player.isOnFire()) {
            return;
        }
        if (!hasNearbyHeatSource(player)) {
            return;
        }
        // 溅起来的油花 / 沸水：把身上的火灭掉，燃烧伤害自然不再结算；这次灭火也磨损围裙（每 20 tick 一次）
        if (player.tickCount % WEAR_INTERVAL == 0) {
            wear(player, 1);
        }
        player.clearFire();
    }

    /**
     * 这名玩家身边 4 格内有没有热源（带上「同一处两刻内复用」的缓存）。
     */
    private static boolean hasNearbyHeatSource(Player player) {
        Level world = player.level();
        long now = world.getGameTime();
        BlockPos pos = player.blockPosition();
        HeatStamp stamp = HEAT_CACHE.get(player.getUUID());
        // mryt233 改（2026-11-10）：跨维度和时间回退都必须重新检查。
        long elapsed = stamp == null ? -1 : now - stamp.gameTime();
        if (stamp != null && elapsed >= 0 && elapsed <= HEAT_CACHE_TICKS
            && stamp.dimension().equals(world.dimension()) && stamp.pos().equals(pos)) {
            return stamp.hot();
        }
        boolean hot = nearHeat(world, pos);
        HEAT_CACHE.put(player.getUUID(), new HeatStamp(now, world.dimension(), pos.immutable(), hot));
        return hot;
    }

    /**
     * 附近有没有热源：9×9×9 格逐个查（只有 {@link #hasNearbyHeatSource} 会调，已带缓存）。
     */
    private static boolean nearHeat(Level world, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RANGE, -RANGE, -RANGE),
            center.offset(RANGE, RANGE, RANGE))) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;   // 空气直接跳过：既省一次方块查询，也不去问扩展热源表
            }
            if (isHeatBlock(state) || HotPotHeat.isHeatSource(world, pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 单个方块是不是「会溅油的热源」：点燃的灶台、点燃的营火、火 / 灵魂火、岩浆块。
     */
    private static boolean isHeatBlock(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof StoveBlock) {
            // 灶台：只有点着的时候才算（熄火以后锅是凉的，没有溅射）
            return state.hasProperty(StoveBlock.LIT) && state.getValue(StoveBlock.LIT);
        }
        if (block instanceof CampfireBlock) {
            // 篝火 / 灵魂篝火：点着才算
            return state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT);
        }
        // 火方块（火、灵魂火）与岩浆块：本身就是明火 / 滚烫的石头
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK);
    }

    /** 护甲槽里的布围裙掉一点耐久（穿久了总会沾油；挡下烫伤、灭火苗时都会磨损）。 */
    public static void wear(Player player, int amount) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        // mryt233 改（2026-11-09，r146b）：三款都认（标签判断）
        if (ClothApronItem.isApron(chest)) {
            chest.hurtAndBreak(amount, player, EquipmentSlot.CHEST);
        }
    }
}
