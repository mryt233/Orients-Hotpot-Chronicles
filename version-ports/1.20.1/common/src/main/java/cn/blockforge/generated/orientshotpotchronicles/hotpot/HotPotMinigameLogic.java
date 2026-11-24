// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Deque;
import java.util.Locale;

/** 火锅小游戏纯数值规则、连点防宏与音游判定；独立于世界与图形，便于单测复用。作者：mryt233 */
public final class HotPotMinigameLogic {
    private HotPotMinigameLogic() { }

    public enum Mode {
        RUSH("rush"),
        RHYTHM("rhythm");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Mode fromId(String raw) {
            if (raw != null && RHYTHM.id.equals(raw.trim().toLowerCase(Locale.ROOT))) {
                return RHYTHM;
            }
            return RUSH;
        }
    }

    public enum RhythmJudge {
        PERFECT("perfect", 100),
        GOOD("good", 60),
        MISS("miss", 0);

        private final String id;
        private final int basePoints;

        RhythmJudge(String id, int basePoints) {
            this.id = id;
            this.basePoints = basePoints;
        }

        public String id() {
            return id;
        }

        public int basePoints() {
            return basePoints;
        }
    }

    /**
     * 服务端防按键宏与高频连点校验（滑动 20 tick ≈ 1.0 秒窗口）。
     * 超出 {@code maxCps} 的敲击直接拒绝，不计入进度且提示防宏限速。
     */
    public static boolean acceptRushPress(Deque<Long> recentPressTicks, long serverTick, int maxCps) {
        if (recentPressTicks == null) {
            return false;
        }
        int cap = Math.max(1, maxCps);
        long windowStart = serverTick - 19L;
        while (!recentPressTicks.isEmpty() && recentPressTicks.peekFirst() < windowStart) {
            recentPressTicks.pollFirst();
        }
        if (recentPressTicks.size() >= cap) {
            return false;
        }
        recentPressTicks.addLast(serverTick);
        return true;
    }

    public static int rushPressPoints(int streakAfterHit) {
        return 10 + Math.min(20, Math.max(0, streakAfterHit) / 2);
    }

    public static int rushBiteBonus(int foodNutrition) {
        return 40 + Math.max(1, foodNutrition) * 10;
    }

    /**
     * 音游判定窗口（毫秒偏差绝对值）。
     * Miss 阈值外的脱拍击键也归为 MISS（若在判定窗口外忽略或空挥可在调用侧细分）。
     */
    public static RhythmJudge judgeTiming(int absDeltaMs, int perfectMs, int goodMs, int missMs) {
        int p = Math.max(1, perfectMs);
        int g = Math.max(p, goodMs);
        if (absDeltaMs <= p) {
            return RhythmJudge.PERFECT;
        }
        if (absDeltaMs <= g) {
            return RhythmJudge.GOOD;
        }
        return RhythmJudge.MISS;
    }

    public static boolean withinJudgeRange(int absDeltaMs, int missMs) {
        return absDeltaMs <= Math.max(1, missMs);
    }

    public static int rhythmPoints(RhythmJudge judge, int comboAfterHit, float comboMultiplier) {
        if (judge == null || judge == RhythmJudge.MISS) {
            return 0;
        }
        double bonus = 1.0 + Math.min(50, Math.max(0, comboAfterHit - 1)) * 0.02 * Math.max(0.0, comboMultiplier);
        return (int) Math.round(judge.basePoints() * bonus);
    }
}
