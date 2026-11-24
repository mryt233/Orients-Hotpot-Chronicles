// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.pure.NumericConfig;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.3.1-alpha Phase 5：连打防宏滑动窗口校验、4 轨音游毫秒判定与连击计分、
 * hotpot_tracks 数据包谱面解析/拍号换算/网络同步、以及 minigame.* 配置规则验证。作者：mryt233
 */
class Phase5MinigameTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
        Config.rememberMinigameDefaults();
    }

    @Test
    void rushAntiMacroWindowCapsWithin20TicksAndRecoversAfterWindow() {
        Deque<Long> window = new ArrayDeque<>();
        int maxCps = 10;
        for (int i = 0; i < 10; i++) {
            assertTrue(HotPotMinigameLogic.acceptRushPress(window, 100L + (i / 2), maxCps));
        }
        // 第 11～15 次落在同一秒 [81..104] 窗口内，全部被防宏拦截
        assertFalse(HotPotMinigameLogic.acceptRushPress(window, 104L, maxCps));
        assertFalse(HotPotMinigameLogic.acceptRushPress(window, 110L, maxCps));

        // 第一个时间戳 100L 滑出 20 tick 窗口（100 <= 120 - 19 = 101）后恢复接收
        assertTrue(HotPotMinigameLogic.acceptRushPress(window, 120L, maxCps));
        assertTrue(HotPotMinigameLogic.acceptRushPress(window, 120L, maxCps));
        assertFalse(HotPotMinigameLogic.acceptRushPress(window, 120L, maxCps));
    }

    @Test
    void rhythmJudgeWindowsAndComboMultiplierBehaveAccurately() {
        assertEquals(HotPotMinigameLogic.RhythmJudge.PERFECT,
            HotPotMinigameLogic.judgeTiming(0, 60, 130, 220));
        assertEquals(HotPotMinigameLogic.RhythmJudge.PERFECT,
            HotPotMinigameLogic.judgeTiming(60, 60, 130, 220));
        assertEquals(HotPotMinigameLogic.RhythmJudge.GOOD,
            HotPotMinigameLogic.judgeTiming(61, 60, 130, 220));
        assertEquals(HotPotMinigameLogic.RhythmJudge.GOOD,
            HotPotMinigameLogic.judgeTiming(130, 60, 130, 220));
        assertEquals(HotPotMinigameLogic.RhythmJudge.MISS,
            HotPotMinigameLogic.judgeTiming(131, 60, 130, 220));

        assertEquals(100, HotPotMinigameLogic.rhythmPoints(HotPotMinigameLogic.RhythmJudge.PERFECT, 1, 1.0F));
        assertTrue(HotPotMinigameLogic.rhythmPoints(HotPotMinigameLogic.RhythmJudge.PERFECT, 11, 1.0F) > 100);
        assertEquals(0, HotPotMinigameLogic.rhythmPoints(HotPotMinigameLogic.RhythmJudge.MISS, 10, 2.0F));
    }

    @Test
    void tracksBuiltinFallbackAndDatapackBeatParsingAndNetworkRoundTrip() {
        List<HotPotTracks.HotPotTrack> builtins = HotPotTracks.all(false);
        assertTrue(builtins.size() >= 3);
        assertNotNull(HotPotTracks.get(HotPot.id("midnight_boil"), false));
        assertNotNull(HotPotTracks.get(HotPot.id("bamboo_chopsticks"), false));
        assertNotNull(HotPotTracks.get(HotPot.id("red_soup_rush"), false));

        String customJson = """
            {
              "title": "Test Pack Track",
              "bpm": 120,
              "lead_in_ms": 1000,
              "difficulty": "hard",
              "notes": [
                { "beat": 2.0, "lane": 3, "sound": "note_bell", "pitch": 1.5 },
                { "time_ms": 1500, "lane": 0, "sound": "chopsticks" }
              ]
            }
            """;
        HotPotTracks.HotPotTrack parsed = HotPotTracks.parseTrack(
            ResourceLocation.parse("example:test_pack"),
            JsonParser.parseString(customJson).getAsJsonObject());
        assertNotNull(parsed);
        assertEquals(2, parsed.notes().size());
        // 按 time_ms 升序：1500ms (lane 0) 在 1000 + 2 * 500 = 2000ms (lane 3) 前面
        assertEquals(1500, parsed.notes().get(0).timeMs());
        assertEquals(0, parsed.notes().get(0).lane());
        assertEquals(2000, parsed.notes().get(1).timeMs());
        assertEquals(3, parsed.notes().get(1).lane());

        String netJson = HotPotTracks.toNetworkJson();
        HotPotTracks.acceptClientJson(netJson);
        assertEquals(builtins.size(), HotPotTracks.all(true).size());
        HotPotTracks.clearClient();
    }

    @Test
    void minigameConfigDefaultsAndBoundsValidateCleanly() {
        var defaults = Config.defaultsCopy();
        var direct = NumericConfig.parse(defaults.toString(), defaults, Config::rule, Config::typedRule, 1);
        assertTrue(direct.warnings().isEmpty(), "Unexpected warnings: " + direct.warnings());
        assertTrue(Config.minigameAllowVirtualPlate());
        assertEquals(300, Config.minigameInviteTimeoutTicks());
        assertEquals(8, Config.minigameInviteRange());
        assertEquals(200, Config.minigameRushDurationTicks());
        assertEquals(16, Config.minigameRushMaxCps());
        assertEquals(5, Config.minigameRushHitsPerBite());
        assertEquals(60, Config.minigameRhythmPerfectMs());
        assertEquals(130, Config.minigameRhythmGoodMs());
        assertEquals(220, Config.minigameRhythmMissMs());
        assertEquals(1.0F, Config.minigameRhythmComboMultiplier(), 0.001F);

        assertNotNull(Config.typedRule("minigame.allow_virtual_plate"));
        assertEquals(4.0, Config.rule("minigame.rush_max_cps").min().doubleValue(), 0.01);
        assertEquals(40.0, Config.rule("minigame.rush_max_cps").max().doubleValue(), 0.01);
        assertEquals(60.0, Config.rule("minigame.invite_timeout_ticks").min().doubleValue(), 0.01);
        assertEquals(5.0, Config.rule("minigame.rhythm_combo_multiplier").max().doubleValue(), 0.01);
    }
}
