// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 火锅界面（GUI）动画的帧增量连续相位累加器：
 * 通过累加 {@code dt}（钳制防卡顿跳帧）驱动速率平滑与配料/气泡/蒸汽相位，避免用绝对时间直接乘动态热度引起的变速、倒转与闪烁。
 * 作者：mryt233
 */
public final class PotScreenAnim {
    public static final double MAX_DT_SEC = 0.05;
    public static final double HEAT_SMOOTH_RATE = 4.0;
    public static final int MAX_CLUSTERS = 12;
    public static final int MAX_LONE = 6;
    public static final int MAX_STEAM = 8;

    private boolean initialized;
    private long lastMs;
    private double elapsedSec;
    private double smoothHeat;
    private double spinPhase;
    private double boilPhase;

    public void advance(long nowMs, double targetHeat) {
        double clampedTarget = clamp01(targetHeat);
        if (!initialized) {
            initialized = true;
            lastMs = nowMs;
            smoothHeat = clampedTarget;
            return;
        }
        double dt = Math.max(0.0, Math.min(MAX_DT_SEC, (nowMs - lastMs) / 1000.0));
        lastMs = nowMs;
        if (dt <= 0.0) {
            return;
        }
        smoothHeat += (clampedTarget - smoothHeat) * (1.0 - Math.exp(-HEAT_SMOOTH_RATE * dt));
        if (Math.abs(clampedTarget - smoothHeat) < 1e-4) {
            smoothHeat = clampedTarget;
        }
        elapsedSec += dt;
        spinPhase += (0.1 + 0.6 * smoothHeat) * dt;
        boilPhase += (0.7 + 0.5 * smoothHeat) * dt;
    }

    public double elapsedSec() {
        return elapsedSec;
    }

    public double smoothHeat() {
        return smoothHeat;
    }

    public double spinPhase() {
        return spinPhase;
    }

    public double boilPhase() {
        return boilPhase;
    }

    public static double clusterGrow(double heat, int idx) {
        if (heat <= 0.01) {
            return 0.0;
        }
        return clamp01(4.0 + 8.0 * heat - idx);
    }

    public static double loneGrow(double heat, int idx) {
        if (heat <= 0.01) {
            return 0.0;
        }
        return clamp01(2.0 + 4.0 * heat - idx);
    }

    public static double steamGrow(double heat, int idx) {
        if (heat <= 0.01) {
            return 0.0;
        }
        return clamp01(3.0 + 5.0 * heat - idx);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
