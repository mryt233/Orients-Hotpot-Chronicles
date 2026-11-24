package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锅内 GUI 与 3D 连续相位积分不变式测试：确保长期开机/多帧率/点火熄火过渡时无变速倒转与跳变。
 * 作者：mryt233
 */
class PotAnimationContinuityTest {
    @Test
    void screenAnimInitializesCleanlyAndNeverReversesAcrossHeatTogglesAndUptime() {
        long now = 100_000_000L; // ~27.8h uptime
        PotScreenAnim anim = new PotScreenAnim();
        anim.advance(now, 1.0);
        assertEquals(1.0, anim.smoothHeat(), 1e-9);
        assertEquals(0.0, anim.elapsedSec(), 1e-9);
        assertEquals(0.0, anim.spinPhase(), 1e-9);
        assertEquals(0.0, anim.boilPhase(), 1e-9);

        // Steady burning: exact constant phase increments over 60fps & 144fps Mix
        double prevSpin = anim.spinPhase();
        double prevBoil = anim.boilPhase();
        long[] stepsMs = {16L, 17L, 7L, 16L, 33L, 8L};
        for (int frame = 0; frame < 180; frame++) {
            long step = stepsMs[frame % stepsMs.length];
            now += step;
            anim.advance(now, 1.0);
            double dSpin = anim.spinPhase() - prevSpin;
            double dBoil = anim.boilPhase() - prevBoil;
            assertTrue(dSpin > 0.0, "spinPhase stepped backwards at steady heat");
            assertTrue(dBoil > 0.0, "boilPhase stepped backwards at steady heat");
            double dt = step / 1000.0;
            assertEquals(0.7 * dt, dSpin, 1e-9);
            assertEquals(1.2 * dt, dBoil, 1e-9);
            prevSpin = anim.spinPhase();
            prevBoil = anim.boilPhase();
        }

        // Lag spike clamp (10s -> clamped to MAX_DT_SEC = 0.05s)
        double beforeSpike = anim.boilPhase();
        now += 10_000L;
        anim.advance(now, 1.0);
        assertEquals(1.2 * PotScreenAnim.MAX_DT_SEC, anim.boilPhase() - beforeSpike, 1e-9);

        // Toggle 1 -> 0 -> 1 over many frames: strictly monotonic, smooth, no jumps
        double lastSpin = anim.spinPhase();
        double lastBoil = anim.boilPhase();
        double prevHeat = anim.smoothHeat();
        for (int f = 0; f < 120; f++) {
            now += 16L;
            double target = f < 60 ? 0.0 : 1.0;
            anim.advance(now, target);
            assertTrue(anim.spinPhase() >= lastSpin, "spinPhase reversed during transition");
            assertTrue(anim.boilPhase() >= lastBoil, "boilPhase reversed during transition");
            assertTrue(Math.abs(anim.smoothHeat() - prevHeat) <= 0.1, "smoothHeat jumped abruptly");
            lastSpin = anim.spinPhase();
            lastBoil = anim.boilPhase();
            prevHeat = anim.smoothHeat();
        }
    }

    @Test
    void continuousGrowWeightsFadeInWithoutSuddenPopIn() {
        assertEquals(0.0, PotScreenAnim.clusterGrow(0.0, 0), 1e-9);
        assertEquals(0.0, PotScreenAnim.loneGrow(0.01, 0), 1e-9);
        assertEquals(0.0, PotScreenAnim.steamGrow(0.01, 0), 1e-9);
        for (int idx = 0; idx < PotScreenAnim.MAX_CLUSTERS; idx++) {
            double prev = 0.0;
            for (int step = 0; step <= 100; step++) {
                double heat = step / 100.0;
                double w = PotScreenAnim.clusterGrow(heat, idx);
                assertTrue(w >= prev - 1e-9 && w <= 1.0 + 1e-9);
                prev = w;
            }
            assertEquals(1.0, PotScreenAnim.clusterGrow(1.0, idx), 1e-9);
        }
    }

    @Test
    void worldCycleRateIsContinuousAcrossHeatAndSwapsAlignAtSineTrough() {
        double prevRate = HotPotBlockEntity.cycleRateFor(true, 0.0F);
        assertEquals(HotPotBlockEntity.BOB_COLD_OMEGA, prevRate, 1e-6);
        for (int i = 1; i <= 100; i++) {
            float heat = i / 100F;
            float rate = HotPotBlockEntity.cycleRateFor(true, heat);
            assertTrue(rate >= prevRate, "cycleRate decreased as heat increased");
            assertTrue(rate - prevRate < 0.005, "step discontinuity in cycleRate near heat=" + heat);
            prevRate = rate;
        }
        assertEquals(HotPotBlockEntity.BOB_OMEGA, HotPotBlockEntity.cycleRateFor(true, 1.0F), 1e-6);
        assertEquals(HotPotBlockEntity.BOB_OMEGA, HotPotBlockEntity.cycleRateFor(false, 0.0F), 1e-6);

        // Display swap boundary (integer cycle = k) occurs strictly at trough sin(angle) == -1
        for (int k : new int[]{0, 1, 2, 17, 500}) {
            double angle = 2.0 * Math.PI * k + 1.5 * Math.PI;
            assertEquals(-1.0, Math.sin(angle), 1e-9);
        }
    }
}
