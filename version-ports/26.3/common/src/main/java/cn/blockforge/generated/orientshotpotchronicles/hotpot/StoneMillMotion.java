// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/** 小石磨客户端表现的时间轴。单位为游戏刻；不改变磨制进度，也不写入存档。 */
public final class StoneMillMotion {
    public static final int PUSH_TICKS = 9;
    public static final int WET_TICKS = 13;
    public static final int SPILL_TICKS = 20;

    private int target = -1;
    private double from;
    private double started = Double.NEGATIVE_INFINITY;

    public void update(int turns, double now) {
        if (target < 0 || turns < target || (long) turns - target > 12L || now < started) {
            // 刚载入、回退或远处跳过多次磨制：恢复摆放角，不重放历史动作。
            target = turns;
            from = turns;
            started = Double.NEGATIVE_INFINITY;
        } else if (turns > target) {
            // 还没推完又收到下一次推磨：用当前角度重新起步，不倒转或跳角。
            from = quarterAt(now);
            target = turns;
            started = now;
        }
    }

    public double quarterAt(double now) {
        double rest = 1 - unit((now - started) / PUSH_TICKS);
        return target - (target - from) * rest * rest * rest;
    }

    public float age(double now) {
        return (float) Math.max(0, now - started);
    }

    public static float wetIntensity(float age, boolean wet, boolean finished, boolean pending) {
        return wet && (!finished || pending)
            ? (float) (unit((age + 1) / 3) * unit((WET_TICKS - age) / 5)) : 0;
    }

    public static float spillIntensity(float age, boolean finishedWet, boolean pending) {
        return finishedWet && pending
            ? (float) (unit((age - 3) / 3) * unit((SPILL_TICKS - age) / 7)) : 0;
    }

    private static double unit(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
