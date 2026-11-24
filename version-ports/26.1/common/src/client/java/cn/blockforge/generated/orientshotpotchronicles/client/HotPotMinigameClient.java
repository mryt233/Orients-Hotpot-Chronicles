// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMinigameLogic;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTracks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveTopBlock;
import cn.blockforge.generated.orientshotpotchronicles.network.MinigameActionPayload;
import cn.blockforge.generated.orientshotpotchronicles.network.MinigameActionPayload.Action;
import cn.blockforge.orient.framework.client.ClientNetwork;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 客户端火锅小游戏 HUD 弹窗、音序音效播放与网络状态管理。作者：mryt233；mryt233 改（2026-11-16）：适配 26.1 API。 */
public final class HotPotMinigameClient {
    public record PlayerRow(String name, int score, int combo, int maxCombo, int bites,
                            int perfects, int goods, int misses, int rateLimitedHits) { }

    public record InviteState(int inviteId, String hostName, boolean isHost, boolean acceptedBySelf,
                              BlockPos potPos, HotPotMinigameLogic.Mode mode, boolean virtualPlate,
                              Identifier trackId, int remainingTicks, int totalTicks,
                              List<String> acceptedNames, List<String> targetNames, long receivedMs) { }

    public record SessionState(int sessionId, BlockPos potPos, HotPotMinigameLogic.Mode mode,
                               boolean virtualPlate, Identifier trackId,
                               int elapsedTicks, int remainingTicks, int totalTicks,
                               int cookedRemaining, String feedbackType, String feedbackPlayer,
                               List<PlayerRow> leaderboard) { }

    public record ResultState(int sessionId, HotPotMinigameLogic.Mode mode, boolean virtualPlate,
                              Identifier trackId, boolean potCleared, boolean multiplayer,
                              boolean isNewBest, int bestScore, String winnerName,
                              List<PlayerRow> leaderboard, long shownUntilMs) { }

    private static volatile InviteState pendingInvite;
    private static volatile SessionState activeSession;
    private static volatile ResultState lastResult;
    private static long sessionAnchorMs;
    private static int activeSoundSessionId = -1;
    private static int lastBeatIndex = -1;
    private static final Set<Integer> localJudgedNotes = new HashSet<>();
    private static boolean nKeyWasDown = false;
    private static long toastUntilMs = 0L;
    private static String toastText = "";

    private static String localPopup = "";
    private static int localPopupColor = 0xFFFFFFFF;
    private static long localPopupUntilMs = 0L;
    private static final long[] laneFlashMs = new long[4];
    private static long rushPressBounceMs = 0L;

    private HotPotMinigameClient() { }

    public static InviteState pendingInvite() {
        return pendingInvite;
    }

    public static SessionState activeSession() {
        return activeSession;
    }

    public static ResultState lastResult() {
        return lastResult;
    }

    public static String localPopup() {
        return Util.getMillis() <= localPopupUntilMs ? localPopup : "";
    }

    public static int localPopupColor() {
        return localPopupColor;
    }

    public static boolean isLaneFlashing(int lane) {
        return lane >= 0 && lane < 4 && (Util.getMillis() - laneFlashMs[lane]) < 130L;
    }

    public static boolean isNoteLocallyJudged(int idx) {
        return localJudgedNotes.contains(idx);
    }

    public static float rushBounce() {
        long dt = Util.getMillis() - rushPressBounceMs;
        if (dt < 0 || dt > 120L) {
            return 0F;
        }
        return 1F - dt / 120F;
    }

    public static long currentElapsedMs() {
        SessionState s = activeSession;
        if (s == null) {
            return 0L;
        }
        return Math.max(0L, Util.getMillis() - sessionAnchorMs);
    }

    public static void onDisconnect() {
        pendingInvite = null;
        activeSession = null;
        lastResult = null;
        activeSoundSessionId = -1;
        lastBeatIndex = -1;
        localJudgedNotes.clear();
        HotPotTracks.clearClient();
    }

    public static void clearLocalForfeit() {
        activeSession = null;
        activeSoundSessionId = -1;
        lastBeatIndex = -1;
        localJudgedNotes.clear();
    }

    public static void acceptServerJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();
            String kind = str(obj, "kind", "");
            switch (kind) {
                case "invite" -> {
                    BlockPos pos = new BlockPos(num(obj, "potX", 0), num(obj, "potY", 0), num(obj, "potZ", 0));
                    InviteState inv = new InviteState(
                        num(obj, "inviteId", 0),
                        str(obj, "hostName", ""),
                        bool(obj, "isHost", false),
                        bool(obj, "acceptedBySelf", false),
                        pos,
                        HotPotMinigameLogic.Mode.fromId(str(obj, "mode", "rush")),
                        bool(obj, "virtualPlate", false),
                        Identifier.tryParse(str(obj, "trackId", HotPotTracks.DEFAULT_TRACK_ID.toString())),
                        num(obj, "remainingTicks", 300),
                        Math.max(1, num(obj, "totalTicks", 300)),
                        strList(obj.getAsJsonArray("acceptedNames")),
                        strList(obj.getAsJsonArray("targetNames")),
                        Util.getMillis());
                    pendingInvite = inv;
                    activeSession = null;
                }
                case "session" -> {
                    int sid = num(obj, "sessionId", 0);
                    int elapsedTicks = num(obj, "elapsedTicks", 0);
                    BlockPos pos = new BlockPos(num(obj, "potX", 0), num(obj, "potY", 0), num(obj, "potZ", 0));
                    String fb = str(obj, "feedbackType", "");
                    SessionState s = new SessionState(
                        sid, pos,
                        HotPotMinigameLogic.Mode.fromId(str(obj, "mode", "rush")),
                        bool(obj, "virtualPlate", false),
                        Identifier.tryParse(str(obj, "trackId", "")),
                        elapsedTicks,
                        num(obj, "remainingTicks", 0),
                        Math.max(1, num(obj, "totalTicks", 1)),
                        num(obj, "cookedRemaining", -1),
                        fb,
                        str(obj, "feedbackPlayer", ""),
                        parseRows(obj.getAsJsonArray("leaderboard")));
                    pendingInvite = null;
                    lastResult = null;
                    activeSession = s;
                    if (activeSoundSessionId != sid) {
                        activeSoundSessionId = sid;
                        lastBeatIndex = -1;
                        localJudgedNotes.clear();
                        sessionAnchorMs = Util.getMillis() - elapsedTicks * 50L;
                    }
                    if ("rate_limited".equals(fb)) {
                        localPopup = "手速过快已触发防宏限制！";
                        localPopupColor = 0xFFFF8844;
                        localPopupUntilMs = Util.getMillis() + 900L;
                    }
                    Minecraft mc = Minecraft.getInstance();
                    if (!(mc.screen instanceof MinigamePlayScreen)) {
                        mc.setScreen(new MinigamePlayScreen());
                    }
                }
                case "result" -> {
                    pendingInvite = null;
                    activeSession = null;
                    activeSoundSessionId = -1;
                    lastBeatIndex = -1;
                    localJudgedNotes.clear();
                    lastResult = new ResultState(
                        num(obj, "sessionId", 0),
                        HotPotMinigameLogic.Mode.fromId(str(obj, "mode", "rush")),
                        bool(obj, "virtualPlate", false),
                        Identifier.tryParse(str(obj, "trackId", "")),
                        bool(obj, "potCleared", false),
                        bool(obj, "multiplayer", false),
                        bool(obj, "isNewBest", false),
                        num(obj, "bestScore", 0),
                        str(obj, "winnerName", ""),
                        parseRows(obj.getAsJsonArray("leaderboard")),
                        Util.getMillis() + 5000L);
                }
                case "clear" -> {
                    pendingInvite = null;
                    activeSession = null;
                    activeSoundSessionId = -1;
                    lastBeatIndex = -1;
                    localJudgedNotes.clear();
                    String reason = str(obj, "reason", "");
                    if (!reason.isEmpty()) {
                        toastText = reason;
                        toastUntilMs = Util.getMillis() + 3000L;
                    }
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.screen instanceof MinigamePlayScreen) {
                        mc.setScreen(null);
                    }
                }
                default -> { }
            }
        } catch (RuntimeException error) {
            HotPot.LOGGER.warn("[hotpot] 客户端解析小游戏状态失败", error);
        }
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        while (HotPotKeybinds.CHALLENGE.consumeClick()) {
            handleChallengeKey(mc);
        }

        // 无界面状态下收到邀约弹窗时，支持按 N 快捷拒绝
        boolean nDown = mc.screen == null && pendingInvite != null
            && InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_N);
        if (nDown && !nKeyWasDown) {
            InviteState inv = pendingInvite;
            if (inv != null) {
                ClientNetwork.send(MinigameActionPayload.of(
                    inv.isHost() ? Action.CANCEL : Action.DECLINE_INVITE,
                    inv.potPos(), inv.inviteId(), ""));
            }
        }
        nKeyWasDown = nDown;

        // 4 轨音游节拍音效与自动 Miss 检查
        SessionState s = activeSession;
        if (s != null && s.mode() == HotPotMinigameLogic.Mode.RHYTHM && s.trackId() != null) {
            HotPotTracks.HotPotTrack track = HotPotTracks.get(s.trackId(), true);
            if (track != null) {
                long elapsed = currentElapsedMs();
                if (elapsed >= track.leadInMs()) {
                    int beat = (int) ((elapsed - track.leadInMs()) * track.bpm() / 60000L);
                    if (beat > lastBeatIndex) {
                        lastBeatIndex = beat;
                        if (beat % 4 == 0) {
                            playSoundToken(mc, "chopsticks", 0.9F, 0.2F);
                        } else {
                            playSoundToken(mc, "boil", 1.0F, 0.16F);
                        }
                    }
                }
                int missWindow = Config.minigameRhythmMissMs();
                List<HotPotTracks.TrackNote> notes = track.notes();
                for (int i = 0; i < notes.size(); i++) {
                    if (localJudgedNotes.contains(i)) {
                        continue;
                    }
                    if (elapsed > notes.get(i).timeMs() + missWindow) {
                        localJudgedNotes.add(i);
                        localPopup = "MISS";
                        localPopupColor = 0xFFFF6666;
                        localPopupUntilMs = Util.getMillis() + 550L;
                        ClientNetwork.send(MinigameActionPayload.of(
                            Action.RHYTHM_MISS, s.potPos(), s.sessionId(), String.valueOf(i)));
                    }
                }
                if (localJudgedNotes.size() >= notes.size() && elapsed > track.durationMs()) {
                    ClientNetwork.send(MinigameActionPayload.of(
                        Action.RHYTHM_HIT, s.potPos(), s.sessionId(), "done"));
                }
            }
        }
    }

    public static void handleRushTap() {
        SessionState s = activeSession;
        if (s == null || s.mode() != HotPotMinigameLogic.Mode.RUSH) {
            return;
        }
        rushPressBounceMs = Util.getMillis();
        Minecraft mc = Minecraft.getInstance();
        playSoundToken(mc, "chopsticks", 1.15F, 0.35F);
        ClientNetwork.send(MinigameActionPayload.of(Action.RUSH_PRESS, s.potPos(), s.sessionId(), ""));
    }

    public static void handleRhythmLaneTap(int lane) {
        SessionState s = activeSession;
        if (s == null || s.mode() != HotPotMinigameLogic.Mode.RHYTHM || lane < 0 || lane > 3) {
            return;
        }
        laneFlashMs[lane] = Util.getMillis();
        Minecraft mc = Minecraft.getInstance();
        HotPotTracks.HotPotTrack track = HotPotTracks.get(s.trackId(), true);
        if (track == null) {
            return;
        }
        long elapsed = currentElapsedMs();
        int perfectMs = Config.minigameRhythmPerfectMs();
        int goodMs = Config.minigameRhythmGoodMs();
        int missMs = Config.minigameRhythmMissMs();

        int bestIdx = -1;
        int bestDist = Integer.MAX_VALUE;
        List<HotPotTracks.TrackNote> notes = track.notes();
        for (int i = 0; i < notes.size(); i++) {
            if (localJudgedNotes.contains(i)) {
                continue;
            }
            HotPotTracks.TrackNote n = notes.get(i);
            if (n.lane() != lane) {
                continue;
            }
            int dist = (int) Math.abs(n.timeMs() - elapsed);
            if (dist <= missMs && dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        if (bestIdx >= 0) {
            HotPotTracks.TrackNote n = notes.get(bestIdx);
            localJudgedNotes.add(bestIdx);
            HotPotMinigameLogic.RhythmJudge judge = HotPotMinigameLogic.judgeTiming(bestDist, perfectMs, goodMs, missMs);
            localPopup = switch (judge) {
                case PERFECT -> "PERFECT!";
                case GOOD -> "GOOD";
                case MISS -> "MISS";
            };
            localPopupColor = switch (judge) {
                case PERFECT -> 0xFFFFD84D;
                case GOOD -> 0xFF7CE86B;
                case MISS -> 0xFFFF6666;
            };
            localPopupUntilMs = Util.getMillis() + 600L;
            if (judge != HotPotMinigameLogic.RhythmJudge.MISS) {
                playSoundToken(mc, n.sound(), n.pitch(), 0.7F);
            } else {
                playSoundToken(mc, "chopsticks", 0.7F, 0.25F);
            }
            ClientNetwork.send(MinigameActionPayload.of(
                Action.RHYTHM_HIT, s.potPos(), s.sessionId(), bestIdx + "|" + elapsed));
        } else {
            playSoundToken(mc, "chopsticks", 0.8F, 0.2F);
        }
    }

    public static void renderHud(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        InviteState inv = pendingInvite;
        if (inv != null) {
            int bw = 246;
            int bh = 46;
            int bx = (sw - bw) / 2;
            int by = 14;
            g.fill(bx, by, bx + bw, by + bh, 0xE81C1007);
            g.fill(bx, by, bx + bw, by + 1, GuiKit.BRASS_HI);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, GuiKit.BRASS_DARK);
            g.fill(bx, by, bx + 1, by + bh, GuiKit.BRASS);
            g.fill(bx + bw - 1, by, bx + bw, by + bh, GuiKit.BRASS);

            int sec = (inv.remainingTicks() + 19) / 20;
            String modeLabel = inv.mode() == HotPotMinigameLogic.Mode.RHYTHM ? "4轨音游" : "连打吃菜";
            String header = inv.isHost()
                ? "火锅对战邀约 · " + modeLabel + " (" + inv.acceptedNames().size() + "人已准备 · " + sec + "s)"
                : inv.hostName() + " 邀请你热锅对决 · " + modeLabel + " (" + sec + "s)";
            GuiKit.fittedText(g, font, Component.literal(header), bx + bw / 2, by + 6, bw - 16, 0xFFFFE090);

            String keyG = HotPotKeybinds.CHALLENGE.getTranslatedKeyMessage().getString();
            String sub = inv.isHost()
                ? (inv.acceptedNames().size() >= 2 ? "按 [" + keyG + "] 立即开局 | [N] 取消邀约" : "等待同桌玩家应战… | [N] 取消")
                : (inv.acceptedBySelf() ? "已应战，等待房主开局…" : "按 [" + keyG + "] 应战！ | 按 [N] 拒绝");
            GuiKit.fittedText(g, font, Component.literal(sub), bx + bw / 2, by + 20, bw - 16, 0xFFE8D8C0);

            float ratio = Math.max(0F, Math.min(1F, inv.remainingTicks() / (float) inv.totalTicks()));
            g.fill(bx + 8, by + bh - 8, bx + bw - 8, by + bh - 4, 0xFF0E0804);
            g.fill(bx + 8, by + bh - 8, bx + 8 + Math.round((bw - 16) * ratio), by + bh - 4, 0xFFF0B240);
            return;
        }

        ResultState res = lastResult;
        if (res != null && !(mc.screen instanceof MinigamePlayScreen) && Util.getMillis() <= res.shownUntilMs()) {
            int bw = 240;
            int bx = (sw - bw) / 2;
            int by = 16;
            int score = res.leaderboard().isEmpty() ? res.bestScore() : res.leaderboard().get(0).score();
            g.fill(bx, by, bx + bw, by + 24, 0xDD1A0E06);
            g.fill(bx, by, bx + bw, by + 1, GuiKit.BRASS_HI);
            String msg = "火锅挑战结算 · " + (res.multiplayer() ? "胜者 " + res.winnerName() + " " : "得分 ") + score
                + (res.isNewBest() ? " (新纪录!)" : "");
            GuiKit.fittedText(g, font, Component.literal(msg), bx + bw / 2, by + 8, bw - 14, 0xFFFFE07D);
            return;
        }

        if (Util.getMillis() <= toastUntilMs && !toastText.isEmpty()) {
            int tw = Math.min(260, font.width(toastText) + 20);
            int tx = (sw - tw) / 2;
            g.fill(tx, 16, tx + tw, 36, 0xDD1A0E06);
            g.fill(tx, 16, tx + tw, 17, GuiKit.BRASS);
            GuiKit.fittedText(g, font, Component.literal(toastText), sw / 2, 22, tw - 12, 0xFFE8C88A);
        }
    }

    private static void handleChallengeKey(Minecraft mc) {
        InviteState inv = pendingInvite;
        if (inv != null) {
            if (inv.isHost()) {
                if (inv.acceptedNames().size() >= 2) {
                    ClientNetwork.send(MinigameActionPayload.of(
                        Action.START_INVITE_NOW, inv.potPos(), inv.inviteId(), ""));
                }
            } else if (!inv.acceptedBySelf()) {
                ClientNetwork.send(MinigameActionPayload.of(
                    Action.ACCEPT_INVITE, inv.potPos(), inv.inviteId(), ""));
            }
            return;
        }
        if (mc.screen != null) {
            return;
        }
        if (!Config.minigamesEnabled()) {
            mc.player.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_disabled"));
            return;
        }
        BlockPos target = findTargetOrNearbyPot(mc);
        if (target == null) {
            mc.player.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_need_pot"));
            return;
        }
        mc.setScreen(new MinigameLobbyScreen(target));
    }

    public static BlockPos findTargetOrNearbyPot(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        if (mc.hitResult instanceof BlockHitResult bhr) {
            BlockPos p = bhr.getBlockPos();
            if (mc.level.getBlockState(p).getBlock() instanceof StoveTopBlock) {
                p = p.below();
            }
            if (mc.level.getBlockEntity(p) instanceof HotPotBlockEntity be && be.hasPot()) {
                return p.immutable();
            }
        }
        BlockPos around = mc.player.getVehicle() instanceof SeatEntity seat
            ? seat.anchorPos() : mc.player.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(around.offset(-5, -2, -5), around.offset(5, 2, 5))) {
            if (mc.level.getBlockEntity(candidate) instanceof HotPotBlockEntity be && be.hasPot()) {
                double d = candidate.distSqr(around);
                if (d < bestDist) {
                    bestDist = d;
                    best = candidate.immutable();
                }
            }
        }
        return best;
    }

    public static void playSoundToken(Minecraft mc, String token, float pitch, float volume) {
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }
        SoundEvent sound = switch (token == null ? "" : token) {
            case "note_bass" -> SoundEvents.NOTE_BLOCK_BASS.value();
            case "note_bell" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "note_chime" -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case "note_xylophone" -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
            case "note_pling" -> SoundEvents.NOTE_BLOCK_PLING.value();
            case "chopsticks" -> SoundEvents.WOODEN_BUTTON_CLICK_ON;
            case "boil" -> SoundEvents.BUBBLE_COLUMN_BUBBLE_POP;
            case "sizzle" -> SoundEvents.FIRE_EXTINGUISH;
            case "note_harp", "" -> SoundEvents.NOTE_BLOCK_HARP.value();
            default -> {
                Identifier custom = Identifier.tryParse(token);
                SoundEvent resolved = custom == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(custom);
                yield resolved != null ? resolved : SoundEvents.NOTE_BLOCK_HARP.value();
            }
        };
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static List<PlayerRow> parseRows(JsonArray arr) {
        List<PlayerRow> rows = new ArrayList<>();
        if (arr == null) {
            return rows;
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            rows.add(new PlayerRow(
                str(o, "name", "?"),
                num(o, "score", 0),
                num(o, "combo", 0),
                num(o, "maxCombo", 0),
                num(o, "bites", 0),
                num(o, "perfects", 0),
                num(o, "goods", 0),
                num(o, "misses", 0),
                num(o, "rateLimitedHits", 0)));
        }
        return rows;
    }

    private static List<String> strList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr != null) {
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    out.add(el.getAsString());
                }
            }
        }
        return out;
    }

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : def;
    }

    private static int num(JsonObject o, String k, int def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : def;
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsBoolean() : def;
    }
}
