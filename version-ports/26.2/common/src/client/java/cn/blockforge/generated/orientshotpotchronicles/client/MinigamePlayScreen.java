// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotMinigameClient.PlayerRow;
import cn.blockforge.generated.orientshotpotchronicles.client.HotPotMinigameClient.ResultState;
import cn.blockforge.generated.orientshotpotchronicles.client.HotPotMinigameClient.SessionState;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMinigameLogic;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTracks;
import cn.blockforge.generated.orientshotpotchronicles.network.MinigameActionPayload;
import cn.blockforge.generated.orientshotpotchronicles.network.MinigameActionPayload.Action;
import cn.blockforge.orient.framework.client.ClientNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 透明非暂停游戏内小游戏 HUD 覆层：保持火锅 3D 热气现场可见，隔离 WASD / 空格位移误触。作者：mryt233
 */
public class MinigamePlayScreen extends Screen {
    private static final int[] LANE_COLORS = { 0xFFE29D3B, 0xFF79D86A, 0xFFE85D55, 0xFF56C6E8 };
    private static final int LEAD_WINDOW_MS = 1600;
    private final Set<Integer> heldKeys = new HashSet<>();

    public MinigamePlayScreen() {
        super(Component.literal("火锅即时挑战"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // 刻意不涂全屏模糊暗底，保证正前方的 3D 火锅与爆发热气粒子清晰可见
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        SessionState s = HotPotMinigameClient.activeSession();
        ResultState res = HotPotMinigameClient.lastResult();
        if (s != null) {
            renderActiveSession(g, s, mouseX, mouseY);
        } else if (res != null) {
            renderResultCard(g, res, mouseX, mouseY);
        } else {
            onClose();
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void renderActiveSession(GuiGraphicsExtractor g, SessionState s, int mouseX, int mouseY) {
        // 顶部状态栏
        int topW = 300;
        int tx = (width - topW) / 2;
        g.fill(tx, 10, tx + topW, 44, 0xCC160C05);
        g.fill(tx, 10, tx + topW, 11, BRASS_HI);

        float remainSec = s.remainingTicks() / 20.0F;
        String modeName = s.mode() == HotPotMinigameLogic.Mode.RUSH ? "连打吃菜狂飙" : "4 轨音游对决";
        String plateNote = s.virtualPlate() ? "虚拟练习盘" : "锅内熟食余 " + Math.max(0, s.cookedRemaining());
        fittedText(g, font, Component.literal(modeName + " · " + plateNote), tx + topW / 2, 16, topW - 80, TITLE);
        g.text(font, Component.literal(String.format("%.1fs", remainSec)), tx + topW - 38, 16, 0xFFFFD86B, true);

        float ratio = Math.max(0F, Math.min(1F, s.remainingTicks() / (float) s.totalTicks()));
        g.fill(tx + 12, 31, tx + topW - 12, 37, 0xFF0A0502);
        g.fill(tx + 12, 31, tx + 12 + Math.round((topW - 24) * ratio), 37, 0xFFE6782B);

        // 右侧实时排行榜
        renderLeaderboardCard(g, s.leaderboard(), width - 144, 54, 134);

        if (s.mode() == HotPotMinigameLogic.Mode.RUSH) {
            renderRushGauge(g, s);
        } else {
            renderRhythmLanes(g, s);
        }

        // 右下退出按钮
        int qx = width - 88;
        int qy = height - 28;
        boolean qHover = hit(mouseX, mouseY, qx, qy, 76, 18);
        copperButton(g, qx, qy, 76, 18, qHover ? 0xFF8B3322 : 0xCC4A1E14, BRASS);
        fittedText(g, font, Component.literal("退出 (Esc)"), qx + 38, qy + 5, 70, 0xFFFFFFFF);
    }

    private void renderRushGauge(GuiGraphicsExtractor g, SessionState s) {
        PlayerRow self = selfRow(s.leaderboard());
        int cx = width / 2;
        int cy = height - 92;
        int bw = 220;
        g.fill(cx - bw / 2, cy - 16, cx + bw / 2, cy + 56, 0xBB190E06);
        g.fill(cx - bw / 2, cy - 16, cx + bw / 2, cy - 15, BRASS_HI);

        int score = self != null ? self.score() : 0;
        int combo = self != null ? self.combo() : 0;
        int bites = self != null ? self.bites() : 0;
        fittedText(g, font, Component.literal("得分 " + score + "  |  连击 " + combo + "  |  已吃 " + bites + " 份"),
            cx, cy - 8, bw - 16, 0xFFFFE59A);

        int bounce = Math.round(HotPotMinigameClient.rushBounce() * 4F);
        int kx = cx - 72;
        int ky = cy + 8 + bounce;
        copperButton(g, kx, ky, 144, 24 - bounce, bounce > 0 ? 0xFFD97228 : COPPER, BRASS_HI);
        fittedText(g, font, Component.literal("狂按 [SPACE] 开吃！"), cx, ky + 7, 134, 0xFFFFFFFF);

        String popup = HotPotMinigameClient.localPopup();
        if (!popup.isEmpty()) {
            fittedText(g, font, Component.literal(popup), cx, cy - 30, 220, HotPotMinigameClient.localPopupColor());
        } else if (self != null && self.rateLimitedHits() > 0) {
            fittedText(g, font, Component.literal("防宏限速触发 " + self.rateLimitedHits() + " 次"),
                cx, cy + 38, bw - 16, 0xFFFF9B50);
        }
    }

    private void renderRhythmLanes(GuiGraphicsExtractor g, SessionState s) {
        HotPotTracks.HotPotTrack track = HotPotTracks.get(s.trackId(), true);
        if (track == null) {
            return;
        }
        int laneW = 38;
        int totalW = laneW * 4;
        int x0 = (width - totalW) / 2;
        int topY = 52;
        int botY = height - 26;
        int hitY = botY - 26;

        g.fill(x0 - 4, topY, x0 + totalW + 4, botY, 0xAA120A04);
        g.fill(x0 - 4, topY, x0 + totalW + 4, topY + 1, BRASS);
        g.fill(x0 - 4, botY - 1, x0 + totalW + 4, botY, BRASS_DARK);

        for (int lane = 0; lane < 4; lane++) {
            int lx = x0 + lane * laneW;
            boolean flash = HotPotMinigameClient.isLaneFlashing(lane);
            g.fill(lx + 1, topY + 2, lx + laneW - 1, botY - 2,
                flash ? alpha(LANE_COLORS[lane], 0.30) : (lane % 2 == 0 ? 0x4428180C : 0x331E1208));
            if (lane > 0) {
                g.fill(lx, topY + 2, lx + 1, botY - 2, 0x40FFF0C0);
            }
            // 按键区底座与动态按键文字
            copperButton(g, lx + 3, hitY + 4, laneW - 6, 18,
                flash ? LANE_COLORS[lane] : 0xCC2A190C, LANE_COLORS[lane]);
            fittedText(g, font, Component.literal(HotPotKeybinds.labelForLane(lane)),
                lx + laneW / 2, hitY + 9, laneW - 10, flash ? 0xFF140A04 : 0xFFFFFFFF);
        }

        // 判定线
        g.fill(x0, hitY, x0 + totalW, hitY + 2, 0xFFFFE59A);

        // 下落音符
        long elapsed = HotPotMinigameClient.currentElapsedMs();
        int trackHeight = hitY - (topY + 6);
        List<HotPotTracks.TrackNote> notes = track.notes();
        for (int i = 0; i < notes.size(); i++) {
            if (HotPotMinigameClient.isNoteLocallyJudged(i)) {
                continue;
            }
            HotPotTracks.TrackNote n = notes.get(i);
            long dt = n.timeMs() - elapsed;
            if (dt > LEAD_WINDOW_MS || dt < -240) {
                continue;
            }
            float progress = 1.0F - (dt / (float) LEAD_WINDOW_MS);
            int ny = (topY + 6) + Math.round(progress * trackHeight);
            if (ny < topY + 4 || ny > botY - 8) {
                continue;
            }
            int lx = x0 + n.lane() * laneW;
            int col = LANE_COLORS[n.lane()];
            g.fill(lx + 4, ny - 5, lx + laneW - 4, ny + 5, 0xFF140A04);
            g.fill(lx + 5, ny - 4, lx + laneW - 5, ny + 4, col);
            g.fill(lx + 5, ny - 4, lx + laneW - 5, ny - 2, 0x70FFFFFF);
        }

        // 左侧连击 / 判定浮字
        PlayerRow self = selfRow(s.leaderboard());
        if (self != null) {
            int sx = Math.max(12, x0 - 110);
            g.fill(sx, topY + 10, sx + 96, topY + 72, 0xBB160C05);
            g.fill(sx, topY + 10, sx + 96, topY + 11, BRASS);
            fittedText(g, font, Component.literal("得分 " + self.score()), sx + 48, topY + 18, 86, 0xFFFFE090);
            fittedText(g, font, Component.literal("连击 " + self.combo()), sx + 48, topY + 32, 86, 0xFF7CE86B);
            fittedText(g, font, Component.literal("P:" + self.perfects() + " G:" + self.goods() + " M:" + self.misses()),
                sx + 48, topY + 48, 86, 0xFFE8D8C0);
        }

        String popup = HotPotMinigameClient.localPopup();
        if (!popup.isEmpty()) {
            fittedText(g, font, Component.literal(popup), x0 + totalW / 2, hitY - 22, totalW, HotPotMinigameClient.localPopupColor());
        }
    }

    private void renderLeaderboardCard(GuiGraphicsExtractor g, List<PlayerRow> rows, int x, int y, int w) {
        if (rows.isEmpty()) {
            return;
        }
        int h = 22 + Math.min(5, rows.size()) * 14;
        g.fill(x, y, x + w, y + h, 0xBA180D05);
        g.fill(x, y, x + w, y + 1, BRASS);
        fittedText(g, font, Component.literal("实时战况"), x + w / 2, y + 5, w - 10, TITLE);
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            PlayerRow r = rows.get(i);
            String line = "#" + (i + 1) + " " + r.name() + " " + r.score();
            fittedText(g, font, Component.literal(line), x + w / 2, y + 19 + i * 14, w - 10,
                i == 0 ? 0xFFFFE07D : 0xFFE0D0B8);
        }
    }

    private void renderResultCard(GuiGraphicsExtractor g, ResultState res, int mouseX, int mouseY) {
        int cw = 250;
        int rowsCount = Math.min(5, Math.max(1, res.leaderboard().size()));
        int ch = 92 + rowsCount * 16;
        int cx = (width - cw) / 2;
        int cy = (height - ch) / 2;

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        frame(g, cw, ch, 2, true);
        String title = (res.multiplayer() ? "围桌对战结算" : "火锅挑战结算") + (res.potCleared() ? " · 全锅清盘!" : "");
        fittedText(g, font, Component.literal(title), cw / 2, 10, cw - 20, TITLE);
        g.fill(14, 22, cw - 14, 23, BRASS_DARK);

        String sub = (res.isNewBest() ? "★ 打破个人纪录！最高 " : "历史最高: ") + res.bestScore();
        fittedText(g, font, Component.literal(sub), cw / 2, 28, cw - 20,
            res.isNewBest() ? 0xFFFFE066 : 0xFFB8E29B);

        int ry = 46;
        for (int i = 0; i < Math.min(5, res.leaderboard().size()); i++) {
            PlayerRow row = res.leaderboard().get(i);
            String info = "#" + (i + 1) + " " + row.name() + "  ·  " + row.score() + " 分  (MaxCombo " + row.maxCombo() + ")";
            fittedText(g, font, Component.literal(info), cw / 2, ry, cw - 24,
                i == 0 ? 0xFFFFE082 : 0xFFE5D2B5);
            ry += 16;
        }

        int bx = cw / 2 - 42;
        int by = ch - 32;
        boolean hover = hit(mouseX - cx, mouseY - cy, bx, by, 84, 20);
        copperButton(g, bx, by, 84, 20, hover ? COPPER : BRASS_DARK, BRASS_HI);
        fittedText(g, font, Component.literal("完成关闭"), cw / 2, by + 6, 76, 0xFFFFFFFF);
        g.pose().popMatrix();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            forfeitOrClose();
            return true;
        }
        SessionState s = HotPotMinigameClient.activeSession();
        if (s == null) {
            return super.keyPressed(event);
        }
        if (!heldKeys.add(keyCode)) {
            return true;
        }
        if (s.mode() == HotPotMinigameLogic.Mode.RUSH) {
            if (keyCode == GLFW.GLFW_KEY_SPACE) {
                HotPotMinigameClient.handleRushTap();
                return true;
            }
        } else {
            int lane = HotPotKeybinds.laneForKey(event);
            if (lane >= 0) {
                HotPotMinigameClient.handleRhythmLaneTap(lane);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        heldKeys.remove(event.key());
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        SessionState s = HotPotMinigameClient.activeSession();
        if (s != null) {
            int qx = width - 88;
            int qy = height - 28;
            if (hit(event.x(), event.y(), qx, qy, 76, 18)) {
                forfeitOrClose();
                return true;
            }
            return true;
        }
        ResultState res = HotPotMinigameClient.lastResult();
        if (res != null) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void forfeitOrClose() {
        sendForfeitIfNeeded();
        onClose();
    }

    @Override
    public void removed() {
        sendForfeitIfNeeded();
        super.removed();
    }

    private void sendForfeitIfNeeded() {
        SessionState s = HotPotMinigameClient.activeSession();
        if (s != null) {
            BlockPos pos = s.potPos();
            HotPotMinigameClient.clearLocalForfeit();
            ClientNetwork.send(MinigameActionPayload.of(Action.FORFEIT, pos, s.sessionId(), ""));
        }
    }

    private PlayerRow selfRow(List<PlayerRow> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        if (minecraft != null && minecraft.player != null) {
            String name = minecraft.player.getScoreboardName();
            for (PlayerRow r : rows) {
                if (name.equals(r.name())) {
                    return r;
                }
            }
        }
        return rows.get(0);
    }
}
