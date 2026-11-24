// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoneMillMotionTest {
    @Test
    void initialChunkAndLargeGapDoNotReplayOlderTurns() {
        StoneMillMotion motion = new StoneMillMotion();
        motion.update(27, 18_000_000.5);
        assertEquals(27, motion.quarterAt(18_000_000.5));
        assertEquals(0, StoneMillMotion.wetIntensity(motion.age(18_000_000.5), true, false, false));
        motion.update(56, 18_000_001.5);
        assertEquals(56, motion.quarterAt(18_000_001.5));
        assertEquals(0, StoneMillMotion.spillIntensity(motion.age(18_000_001.5), true, true));
    }

    @Test
    void rapidGrindsContinueFromCurrentAngleWithoutJumpOrBacktracking() {
        StoneMillMotion motion = new StoneMillMotion();
        motion.update(3, 99.5);
        motion.update(4, 100);
        double current = motion.quarterAt(102.25);
        assertTrue(current > 3 && current < 4);
        motion.update(5, 102.25);
        assertEquals(current, motion.quarterAt(102.25), 1e-12);
        double last = current;
        for (double n = 102.5; n <= 114; n += .25) {
            motion.update(5, n); // 同圈数的更新不应重启插值。
            double angle = motion.quarterAt(n);
            assertTrue(angle >= last - 1e-12 && angle <= 5);
            last = angle;
        }
        assertEquals(5, last);
    }

    @Test
    void wetGrindingAndFinalSpillRemainFiniteAndCollectStopsBoth() {
        assertTrue(StoneMillMotion.wetIntensity(4, true, false, false) > 0);
        assertEquals(0, StoneMillMotion.spillIntensity(4, false, false));
        assertEquals(0, StoneMillMotion.spillIntensity(2, true, true));
        assertTrue(StoneMillMotion.spillIntensity(6, true, true) > 0);
        assertEquals(0, StoneMillMotion.wetIntensity(6, true, true, false));
        assertEquals(0, StoneMillMotion.spillIntensity(6, true, false));
        assertEquals(0, StoneMillMotion.wetIntensity(StoneMillMotion.WET_TICKS, true, true, true));
        assertEquals(0, StoneMillMotion.spillIntensity(StoneMillMotion.SPILL_TICKS, true, true));
        assertEquals(0, StoneMillMotion.wetIntensity(4, false, false, false));
    }
}
