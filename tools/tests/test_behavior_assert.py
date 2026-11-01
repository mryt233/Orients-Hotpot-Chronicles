"""行为断言脚本的离线回归（不启动游戏）。作者：mryt233。

针对复核指出的两个假阳性：摘要块外的重复通道告警、init=10 被子串匹配误放行。
"""
import sys
from pathlib import Path
import unittest

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import dev_behavior_assert as behavior
import run_matrix

MODULE_LINE = ("[ORIENT-FW] module process={process} id=orients_hotpot_chronicles "
               "declaration=1 register=1 runtime=1 client={client} reload=0")


def summary(process="server", init="1", installs="1", client="0"):
    net = [
        f"[ORIENT-FW] net process={process} channel=orients_hotpot_chronicles:config direction=S2C registered=1 policy=OPTIONAL sent=1 handled=0 skipped=0",
        f"[ORIENT-FW] net process={process} channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=1 skipped=0",
        f"[ORIENT-FW] net process={process} channel=orients_hotpot_chronicles:open_sauce_restock direction=C2S registered=1 policy=REQUIRED sent=0 handled=0 skipped=0",
    ]
    return "\n".join([
        "[ORIENT-FW] summary trigger=server-stopped" if process == "server"
        else "[ORIENT-FW] summary trigger=client-disconnect",
        f"[ORIENT-FW] process={process} platform=P init={init} installs={installs}",
        MODULE_LINE.format(process=process, client=client),
    ] + net)


class StatsAssertTests(unittest.TestCase):
    def parse(self, text, trigger="server-stopped", process="server"):
        stats = behavior.find_summary("fabric", trigger, text)
        stats["duplicates"] = [line for line in text.splitlines()
                               if "duplicate channel registration ignored" in line]
        return behavior.stats_problems(process, stats)

    def test_clean_summary_passes(self):
        self.assertEqual(self.parse(summary()), [])

    def test_init_ten_must_fail(self):
        problems = self.parse(summary(init="10"))
        self.assertTrue(any("init/installs" in problem for problem in problems), problems)

    def test_installs_ten_must_fail(self):
        problems = self.parse(summary(installs="10"))
        self.assertTrue(any("init/installs" in problem for problem in problems), problems)

    def test_duplicate_warning_outside_summary_block_must_fail(self):
        text = ("[ORIENT-FW] duplicate channel registration ignored: S2C orients_hotpot_chronicles:config\n"
                + summary())
        problems = self.parse(text)
        self.assertTrue(any("重复通道登记" in problem for problem in problems), problems)

    def test_repeated_disconnect_summaries_count_once(self):
        text = summary() + "\n" + summary()
        stats = behavior.find_summary("fabric", "server-stopped", text)
        self.assertEqual(len(stats["init"]), 1)
        self.assertEqual(behavior.stats_problems("server", stats), [])

    def test_missing_channel_in_summary_must_fail(self):
        text = chr(10).join([
            summary().splitlines()[0],
            summary().splitlines()[1],
            MODULE_LINE.format(process="server", client="0"),
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:config direction=S2C registered=1 policy=OPTIONAL sent=1 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=1 skipped=0",
        ])
        problems = self.parse(text)
        self.assertTrue(any("缺少通道" in problem and "open_sauce_restock" in problem for problem in problems), problems)

    def test_error_line_in_log_must_fail(self):
        problems = behavior.log_problems("server", "[12:00:00] [Server thread/ERROR] (Minecraft) boom")
        self.assertTrue(problems)


class MatrixAssertTests(unittest.TestCase):
    def stats_of(self, text):
        return run_matrix.parse_runtime_stats(text)

    def test_matrix_must_not_fill_missing_channel_from_earlier_summary(self):
        early = chr(10).join([
            "[ORIENT-FW] summary trigger=server-started",
            "[ORIENT-FW] process=server platform=P init=1 installs=1",
            MODULE_LINE.format(process="server", client="0"),
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:config direction=S2C registered=1 policy=OPTIONAL sent=0 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:open_sauce_restock direction=C2S registered=1 policy=REQUIRED sent=0 handled=0 skipped=0",
        ])
        late = chr(10).join([
            "[ORIENT-FW] summary trigger=server-stopped",
            "[ORIENT-FW] process=server platform=P init=1 installs=1",
            MODULE_LINE.format(process="server", client="0"),
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:config direction=S2C registered=1 policy=OPTIONAL sent=1 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=1 skipped=0",
        ])
        ok, problems = run_matrix.stats_ok(self.stats_of(early + chr(10) + late))
        self.assertFalse(ok, problems)
        self.assertTrue(any("缺少通道" in problem and "open_sauce_restock" in problem for problem in problems), problems)

    def test_matrix_channel_missing_registered_field_must_fail(self):
        text = chr(10).join([
            "[ORIENT-FW] summary trigger=server-stopped",
            "[ORIENT-FW] process=server platform=P init=1 installs=1",
            MODULE_LINE.format(process="server", client="0"),
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:config direction=S2C sent=1 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=1 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:open_sauce_restock direction=C2S registered=1 policy=REQUIRED sent=0 handled=0 skipped=0",
        ])
        ok, problems = run_matrix.stats_ok(self.stats_of(text))
        self.assertFalse(ok, problems)

    def test_matrix_summary_without_head_line_must_fail_even_with_entry_line(self):
        text = chr(10).join([
            "[ORIENT-FW] init process=server platform=P count=1 installs=1",
            "[ORIENT-FW] summary trigger=server-stopped",
            MODULE_LINE.format(process="server", client="0"),
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:config direction=S2C registered=1 policy=OPTIONAL sent=1 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=1 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:open_sauce_restock direction=C2S registered=1 policy=REQUIRED sent=0 handled=0 skipped=0",
        ])
        ok, problems = run_matrix.stats_ok(self.stats_of(text))
        self.assertFalse(ok, problems)
        self.assertTrue(any("init" in problem for problem in problems), problems)

    def test_matrix_requires_registered_one(self):
        text = chr(10).join([
            "[ORIENT-FW] summary trigger=server-stopped",
            "[ORIENT-FW] process=server platform=P init=1 installs=1",
            MODULE_LINE.format(process="server", client="0"),
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:config direction=S2C registered=2 policy=OPTIONAL sent=1 handled=0 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:chopsticks_aim direction=C2S registered=1 policy=REQUIRED sent=0 handled=1 skipped=0",
            "[ORIENT-FW] net process=server channel=orients_hotpot_chronicles:open_sauce_restock direction=C2S registered=1 policy=REQUIRED sent=0 handled=0 skipped=0",
        ])
        ok, problems = run_matrix.stats_ok(self.stats_of(text))
        self.assertFalse(ok, problems)


if __name__ == "__main__":
    unittest.main()
