// mryt233 改：26.2 移植同步。
// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.network.HotPotNetwork;
import cn.blockforge.generated.orientshotpotchronicles.network.MinigameActionPayload.Action;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 火锅即时小游戏总控状态机与服务端会话（连打吃菜 + 4 轨音游多人对战）。作者：mryt233
 */
public final class HotPotMinigames {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<Integer, Invite> INVITES = new LinkedHashMap<>();
    private static final Map<Integer, Session> SESSIONS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> PLAYER_SESSION = new HashMap<>();
    private static final Map<UUID, Integer> PLAYER_INVITE = new HashMap<>();
    private static final Map<String, Integer> BEST_SCORES = new HashMap<>();
    private static int nextId = 1;
    private static long serverTicks = 0L;

    private HotPotMinigames() { }

    private static final class Invite {
        final int id;
        final UUID hostId;
        final String hostName;
        final ServerLevel level;
        final BlockPos potPos;
        final HotPotMinigameLogic.Mode mode;
        final boolean virtualPlate;
        final Identifier trackId;
        final long createdTick;
        final long expiresTick;
        final Set<UUID> targets = new HashSet<>();
        final Set<UUID> accepted = new HashSet<>();
        final Set<UUID> declined = new HashSet<>();

        Invite(int id, ServerPlayer host, ServerLevel level, BlockPos potPos,
               HotPotMinigameLogic.Mode mode, boolean virtualPlate, Identifier trackId, long now) {
            this.id = id;
            this.hostId = host.getUUID();
            this.hostName = host.getScoreboardName();
            this.level = level;
            this.potPos = potPos.immutable();
            this.mode = mode;
            this.virtualPlate = virtualPlate;
            this.trackId = trackId;
            this.createdTick = now;
            this.expiresTick = now + Config.minigameInviteTimeoutTicks();
            this.accepted.add(host.getUUID());
        }
    }

    private static final class Participant {
        final UUID uuid;
        final String name;
        int score;
        int combo;
        int maxCombo;
        int bites;
        int rushHits;
        int rateLimitedHits;
        int perfects;
        int goods;
        int misses;
        boolean finishedRhythm;
        final Deque<Long> recentPressTicks = new ArrayDeque<>();
        final Set<Integer> judgedNotes = new HashSet<>();

        Participant(ServerPlayer player) {
            this.uuid = player.getUUID();
            this.name = player.getScoreboardName();
        }
    }

    private static final class Session {
        final int id;
        final UUID hostId;
        final ServerLevel level;
        final BlockPos potPos;
        final HotPotMinigameLogic.Mode mode;
        final boolean virtualPlate;
        final HotPotTracks.HotPotTrack track;
        final long startTick;
        final long endTick;
        final Map<UUID, Participant> players = new LinkedHashMap<>();
        boolean finished;
        boolean potCleared;
        long lastSyncTick;

        Session(int id, UUID hostId, ServerLevel level, BlockPos potPos, HotPotMinigameLogic.Mode mode,
                boolean virtualPlate, HotPotTracks.HotPotTrack track, long startTick) {
            this.id = id;
            this.hostId = hostId;
            this.level = level;
            this.potPos = potPos.immutable();
            this.mode = mode;
            this.virtualPlate = virtualPlate;
            this.track = track;
            this.startTick = startTick;
            long durTicks = mode == HotPotMinigameLogic.Mode.RUSH
                ? Config.minigameRushDurationTicks()
                : Math.max(80L, (long) Math.ceil((track != null ? track.durationMs() : 16000) / 50.0) + 10L);
            this.endTick = startTick + durTicks;
            this.lastSyncTick = startTick;
        }
    }

    public static void clearAll() {
        INVITES.clear();
        SESSIONS.clear();
        PLAYER_SESSION.clear();
        PLAYER_INVITE.clear();
        BEST_SCORES.clear();
        nextId = 1;
        serverTicks = 0L;
    }

    public static void onPlayerLeave(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        Integer invId = PLAYER_INVITE.remove(id);
        if (invId != null) {
            Invite inv = INVITES.get(invId);
            if (inv != null) {
                if (inv.hostId.equals(id)) {
                    cancelInvite(inv, "房主已离开游戏", player.level().getServer());
                } else {
                    inv.declined.add(id);
                    broadcastInvite(inv, player.level().getServer());
                }
            }
        }
        Integer sessId = PLAYER_SESSION.remove(id);
        if (sessId != null) {
            Session s = SESSIONS.get(sessId);
            if (s != null && !s.finished) {
                s.players.remove(id);
                if (s.players.isEmpty()) {
                    SESSIONS.remove(sessId);
                } else {
                    broadcastSession(s, "leave", player.getScoreboardName());
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        serverTicks++;

        // 1. 邀约超时与全员应战检查
        for (Invite inv : new ArrayList<>(INVITES.values())) {
            boolean allResponded = !inv.targets.isEmpty()
                && inv.targets.stream().allMatch(t -> inv.accepted.contains(t) || inv.declined.contains(t));
            if (allResponded && inv.accepted.size() >= 2) {
                startFromInvite(inv, server);
                continue;
            }
            if (serverTicks >= inv.expiresTick) {
                if (inv.accepted.size() >= 2) {
                    startFromInvite(inv, server);
                } else {
                    cancelInvite(inv, "15 秒邀约已超时", server);
                }
                continue;
            }
            if ((inv.expiresTick - serverTicks) % 20 == 0) {
                broadcastInvite(inv, server);
            }
        }

        // 2. 进行中会话定时结算与状态广播
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            if (s.finished) {
                SESSIONS.remove(s.id);
                continue;
            }
            boolean timedOut = serverTicks >= s.endTick;
            boolean allRhythmDone = s.mode == HotPotMinigameLogic.Mode.RHYTHM
                && !s.players.isEmpty()
                && s.players.values().stream().allMatch(p -> p.finishedRhythm);
            if (timedOut || allRhythmDone) {
                finishSession(s, server);
                continue;
            }
            if (serverTicks - s.lastSyncTick >= 5L) {
                s.lastSyncTick = serverTicks;
                broadcastSession(s, "", "");
            }
        }
    }

    public static void handleAction(ServerPlayer player, Action action, BlockPos rawPos, int intArg, String strArg) {
        if (player == null || action == null) {
            return;
        }
        ServerLevel level = player.level();
        switch (action) {
            case START_SOLO -> startSoloOrInvite(player, level, rawPos, intArg, strArg, false);
            case SEND_INVITE -> startSoloOrInvite(player, level, rawPos, intArg, strArg, true);
            case ACCEPT_INVITE -> respondInvite(player, intArg, true);
            case DECLINE_INVITE -> respondInvite(player, intArg, false);
            case START_INVITE_NOW -> startInviteEarly(player, intArg);
            case CANCEL -> cancelByPlayer(player);
            case RUSH_PRESS -> handleRushPress(player, intArg);
            case RHYTHM_HIT -> handleRhythmHit(player, intArg, strArg, true);
            case RHYTHM_MISS -> handleRhythmHit(player, intArg, strArg, false);
            case FORFEIT -> forfeitSession(player, intArg);
        }
    }

    public static boolean startCommandChallenge(ServerPlayer player, BlockPos pos, HotPotMinigameLogic.Mode mode,
                                                boolean virtualPlate, Identifier trackId, boolean invite) {
        return startSoloOrInvite(player, player.level(), pos, mode.ordinal(),
            (virtualPlate ? "v|" : "r|") + (trackId == null ? HotPotTracks.DEFAULT_TRACK_ID : trackId), invite);
    }

    public static boolean respondPendingInvite(ServerPlayer player, boolean accept) {
        Integer invId = PLAYER_INVITE.get(player.getUUID());
        if (invId == null) {
            return false;
        }
        respondInvite(player, invId, accept);
        return true;
    }

    public static String statusSummary(ServerPlayer player) {
        if (player != null) {
            Integer sid = PLAYER_SESSION.get(player.getUUID());
            if (sid != null && SESSIONS.containsKey(sid)) {
                Session s = SESSIONS.get(sid);
                Participant p = s.players.get(player.getUUID());
                return "session#" + s.id + " mode=" + s.mode.id() + " virtual=" + s.virtualPlate
                    + " score=" + (p != null ? p.score : 0) + " bites=" + (p != null ? p.bites : 0)
                    + " remainingTicks=" + Math.max(0L, s.endTick - serverTicks);
            }
            Integer iid = PLAYER_INVITE.get(player.getUUID());
            if (iid != null && INVITES.containsKey(iid)) {
                Invite inv = INVITES.get(iid);
                return "invite#" + inv.id + " mode=" + inv.mode.id() + " accepted=" + inv.accepted.size()
                    + " remainingTicks=" + Math.max(0L, inv.expiresTick - serverTicks);
            }
        }
        return "idle (sessions=" + SESSIONS.size() + ", invites=" + INVITES.size() + ")";
    }

    private static boolean startSoloOrInvite(ServerPlayer host, ServerLevel level, BlockPos rawPos,
                                             int modeOrd, String args, boolean invite) {
        if (!Config.minigamesEnabled()) {
            host.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_disabled")
                .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        if (PLAYER_SESSION.containsKey(host.getUUID()) || PLAYER_INVITE.containsKey(host.getUUID())) {
            host.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_busy")
                .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        BlockPos potPos = resolvePotPos(level, host, rawPos);
        HotPotBlockEntity be = potPos == null ? null : findPotEntity(level, potPos);
        if (be == null) {
            host.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_need_pot")
                .withStyle(ChatFormatting.RED));
            return false;
        }

        HotPotMinigameLogic.Mode mode = modeOrd == 1 ? HotPotMinigameLogic.Mode.RHYTHM : HotPotMinigameLogic.Mode.RUSH;
        boolean virtualPlate = false;
        Identifier trackId = HotPotTracks.DEFAULT_TRACK_ID;
        if (args != null && !args.isEmpty()) {
            int sep = args.indexOf('|');
            if (sep >= 0) {
                virtualPlate = "v".equals(args.substring(0, sep));
                Identifier parsed = Identifier.tryParse(args.substring(sep + 1));
                if (parsed != null) {
                    trackId = parsed;
                }
            }
        }
        if (virtualPlate && !Config.minigameAllowVirtualPlate()) {
            virtualPlate = false;
            host.sendSystemMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_virtual_disabled")
                .withStyle(ChatFormatting.YELLOW));
        }
        if (!virtualPlate && be.countCookedForMinigame() <= 0) {
            host.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_no_cooked")
                .withStyle(ChatFormatting.RED));
            return false;
        }

        HotPotTracks.HotPotTrack track = mode == HotPotMinigameLogic.Mode.RHYTHM
            ? HotPotTracks.get(trackId, false) : null;
        if (mode == HotPotMinigameLogic.Mode.RHYTHM && track == null) {
            host.sendOverlayMessage(Component.literal("未找到可用音游谱面"));
            return false;
        }

        if (!invite) {
            Session session = createSession(host.getUUID(), level, potPos, mode, virtualPlate, track, List.of(host));
            broadcastSession(session, "start", host.getScoreboardName());
            return true;
        }

        // 创建 15 秒邀约，通知同桌 / 近距离玩家
        Invite inv = new Invite(nextId++, host, level, potPos, mode, virtualPlate, trackId, serverTicks);
        double range = Config.minigameInviteRange();
        AABB box = new AABB(potPos).inflate(range);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, box,
            p -> p != host && p.isAlive() && !p.isSpectator()
                && !PLAYER_SESSION.containsKey(p.getUUID())
                && !PLAYER_INVITE.containsKey(p.getUUID()));
        for (ServerPlayer p : nearby) {
            inv.targets.add(p.getUUID());
            PLAYER_INVITE.put(p.getUUID(), inv.id);
        }
        PLAYER_INVITE.put(host.getUUID(), inv.id);
        INVITES.put(inv.id, inv);
        broadcastInvite(inv, host.level().getServer());
        return true;
    }

    private static void respondInvite(ServerPlayer guest, int inviteId, boolean accept) {
        Invite inv = inviteId > 0 ? INVITES.get(inviteId) : findInviteFor(guest.getUUID());
        if (inv == null) {
            PLAYER_INVITE.remove(guest.getUUID());
            return;
        }
        if (inv.hostId.equals(guest.getUUID())) {
            if (!accept) {
                cancelInvite(inv, "房主取消了邀约", guest.level().getServer());
            }
            return;
        }
        if (accept) {
            if (!Config.minigamesEnabled() || (!inv.virtualPlate && countCookedAt(inv.level, inv.potPos) <= 0)) {
                guest.sendOverlayMessage(Component.translatable("text." + HotPot.MOD_ID + ".msg.minigame_no_cooked")
                    .withStyle(ChatFormatting.RED));
                inv.declined.add(guest.getUUID());
                PLAYER_INVITE.remove(guest.getUUID());
                broadcastInvite(inv, guest.level().getServer());
                return;
            }
            inv.accepted.add(guest.getUUID());
            broadcastInvite(inv, guest.level().getServer());
        } else {
            inv.declined.add(guest.getUUID());
            PLAYER_INVITE.remove(guest.getUUID());
            sendClear(guest, "已拒绝火锅挑战邀约");
            broadcastInvite(inv, guest.level().getServer());
        }
    }

    private static void startInviteEarly(ServerPlayer host, int inviteId) {
        Invite inv = inviteId > 0 ? INVITES.get(inviteId) : findInviteFor(host.getUUID());
        if (inv == null || !inv.hostId.equals(host.getUUID())) {
            return;
        }
        startFromInvite(inv, host.level().getServer());
    }

    private static void cancelByPlayer(ServerPlayer player) {
        Integer invId = PLAYER_INVITE.get(player.getUUID());
        if (invId != null) {
            Invite inv = INVITES.get(invId);
            if (inv != null) {
                if (inv.hostId.equals(player.getUUID())) {
                    cancelInvite(inv, "房主已取消邀约", player.level().getServer());
                } else {
                    respondInvite(player, invId, false);
                }
            }
        }
    }

    private static void startFromInvite(Invite inv, MinecraftServer server) {
        INVITES.remove(inv.id);
        for (UUID uid : inv.targets) {
            if (!inv.accepted.contains(uid)) {
                PLAYER_INVITE.remove(uid);
                ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(uid);
                if (p != null) {
                    sendClear(p, "对战已开始");
                }
            }
        }
        PLAYER_INVITE.remove(inv.hostId);
        List<ServerPlayer> participants = new ArrayList<>();
        if (server != null) {
            for (UUID uid : inv.accepted) {
                PLAYER_INVITE.remove(uid);
                ServerPlayer p = server.getPlayerList().getPlayer(uid);
                if (p != null && p.isAlive()) {
                    participants.add(p);
                }
            }
        }
        if (participants.isEmpty()) {
            return;
        }
        if (!inv.virtualPlate && countCookedAt(inv.level, inv.potPos) <= 0) {
            for (ServerPlayer p : participants) {
                sendClear(p, "锅内熟食已空，无法开局");
            }
            return;
        }
        HotPotTracks.HotPotTrack track = inv.mode == HotPotMinigameLogic.Mode.RHYTHM
            ? HotPotTracks.get(inv.trackId, false) : null;
        Session session = createSession(inv.hostId, inv.level, inv.potPos, inv.mode, inv.virtualPlate, track, participants);
        broadcastSession(session, "start", inv.hostName);
    }

    private static Session createSession(UUID hostId, ServerLevel level, BlockPos potPos, HotPotMinigameLogic.Mode mode,
                                         boolean virtualPlate, HotPotTracks.HotPotTrack track, List<ServerPlayer> members) {
        Session session = new Session(nextId++, hostId, level, potPos, mode, virtualPlate, track, serverTicks);
        for (ServerPlayer sp : members) {
            session.players.put(sp.getUUID(), new Participant(sp));
            PLAYER_SESSION.put(sp.getUUID(), session.id);
        }
        SESSIONS.put(session.id, session);
        return session;
    }

    private static void handleRushPress(ServerPlayer player, int sessionId) {
        Session s = sessionId > 0 ? SESSIONS.get(sessionId) : sessionOf(player);
        if (s == null || s.finished || s.mode != HotPotMinigameLogic.Mode.RUSH) {
            return;
        }
        Participant p = s.players.get(player.getUUID());
        if (p == null) {
            return;
        }
        int maxCps = Config.minigameRushMaxCps();
        if (!HotPotMinigameLogic.acceptRushPress(p.recentPressTicks, serverTicks, maxCps)) {
            p.rateLimitedHits++;
            p.combo = 0;
            broadcastSession(s, "rate_limited", p.name);
            return;
        }

        p.rushHits++;
        p.combo++;
        if (p.combo > p.maxCombo) {
            p.maxCombo = p.combo;
        }
        p.score += HotPotMinigameLogic.rushPressPoints(p.combo);

        int hitsPerBite = Config.minigameRushHitsPerBite();
        String feedback = "press";
        if (p.rushHits % hitsPerBite == 0) {
            p.bites++;
            if (s.virtualPlate) {
                p.score += HotPotMinigameLogic.rushBiteBonus(4);
                spawnPotParticles(s.level, s.potPos, ItemStack.EMPTY);
                s.level.playSound(null, s.potPos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.45F, 1.15F);
                feedback = "bite";
            } else {
                HotPotBlockEntity be = findPotEntity(s.level, s.potPos);
                ItemStack eaten = be == null ? ItemStack.EMPTY : be.consumeOneCookedForMinigame(player);
                if (!eaten.isEmpty()) {
                    int nutrition = consumeFoodForPlayer(s.level, s.potPos, player, eaten);
                    p.score += HotPotMinigameLogic.rushBiteBonus(nutrition);
                    HotPotCriterion.fire(player, "minigame_rush_eat");
                    feedback = "bite";
                    if (be.countCookedForMinigame() <= 0) {
                        p.score += 150;
                        s.potCleared = true;
                        finishSession(s, player.level().getServer());
                        return;
                    }
                } else {
                    s.potCleared = true;
                    finishSession(s, player.level().getServer());
                    return;
                }
            }
        }
        broadcastSession(s, feedback, p.name);
    }

    private static void handleRhythmHit(ServerPlayer player, int sessionId, String rawArg, boolean isHit) {
        Session s = sessionId > 0 ? SESSIONS.get(sessionId) : sessionOf(player);
        if (s == null || s.finished || s.mode != HotPotMinigameLogic.Mode.RHYTHM || s.track == null) {
            return;
        }
        Participant p = s.players.get(player.getUUID());
        if (p == null) {
            return;
        }
        if ("done".equals(rawArg)) {
            p.finishedRhythm = true;
            broadcastSession(s, "rhythm_done", p.name);
            return;
        }
        int noteIdx = -1;
        int clientElapsed = (int) ((serverTicks - s.startTick) * 50L);
        if (rawArg != null && !rawArg.isEmpty()) {
            int bar = rawArg.indexOf('|');
            try {
                if (bar >= 0) {
                    noteIdx = Integer.parseInt(rawArg.substring(0, bar));
                    clientElapsed = Integer.parseInt(rawArg.substring(bar + 1));
                } else {
                    noteIdx = Integer.parseInt(rawArg);
                }
            } catch (NumberFormatException ignored) {
                return;
            }
        }
        List<HotPotTracks.TrackNote> notes = s.track.notes();
        if (noteIdx < 0 || noteIdx >= notes.size() || !p.judgedNotes.add(noteIdx)) {
            return;
        }

        if (!isHit) {
            p.misses++;
            p.combo = 0;
            broadcastSession(s, "miss", p.name);
            return;
        }

        int serverElapsed = (int) ((serverTicks - s.startTick) * 50L);
        // 客户端精度只在与服务端时钟偏差 <= 350ms 时采纳，既允许毫秒级 Perfect 判定，又防止改时间作弊
        int effectiveElapsed = Math.abs(clientElapsed - serverElapsed) <= 350 ? clientElapsed : serverElapsed;
        int absDelta = Math.abs(effectiveElapsed - notes.get(noteIdx).timeMs());
        HotPotMinigameLogic.RhythmJudge judge = HotPotMinigameLogic.judgeTiming(absDelta,
            Config.minigameRhythmPerfectMs(), Config.minigameRhythmGoodMs(), Config.minigameRhythmMissMs());

        if (judge == HotPotMinigameLogic.RhythmJudge.MISS) {
            p.misses++;
            p.combo = 0;
        } else {
            if (judge == HotPotMinigameLogic.RhythmJudge.PERFECT) {
                p.perfects++;
            } else {
                p.goods++;
            }
            p.combo++;
            if (p.combo > p.maxCombo) {
                p.maxCombo = p.combo;
            }
            int pts = HotPotMinigameLogic.rhythmPoints(judge, p.combo, Config.minigameRhythmComboMultiplier());
            p.score += pts;

            int totalHit = p.perfects + p.goods;
            if (totalHit == 1 || totalHit % 4 == 0) {
                p.bites++;
                if (s.virtualPlate) {
                    spawnPotParticles(s.level, s.potPos, ItemStack.EMPTY);
                } else {
                    HotPotBlockEntity be = findPotEntity(s.level, s.potPos);
                    ItemStack eaten = be == null ? ItemStack.EMPTY : be.consumeOneCookedForMinigame(player);
                    if (!eaten.isEmpty()) {
                        consumeFoodForPlayer(s.level, s.potPos, player, eaten);
                        p.score += 30;
                    }
                }
            }
        }
        if (p.judgedNotes.size() >= notes.size()) {
            p.finishedRhythm = true;
        }
        broadcastSession(s, judge.id(), p.name);
    }

    private static void forfeitSession(ServerPlayer player, int sessionId) {
        Session s = sessionId > 0 ? SESSIONS.get(sessionId) : sessionOf(player);
        if (s == null) {
            return;
        }
        PLAYER_SESSION.remove(player.getUUID());
        s.players.remove(player.getUUID());
        sendClear(player, "已退出本次小游戏");
        if (s.players.isEmpty()) {
            s.finished = true;
            SESSIONS.remove(s.id);
        } else {
            broadcastSession(s, "forfeit", player.getScoreboardName());
        }
    }

    private static void finishSession(Session s, MinecraftServer server) {
        if (s.finished) {
            return;
        }
        s.finished = true;
        SESSIONS.remove(s.id);

        List<Participant> ranked = new ArrayList<>(s.players.values());
        ranked.sort(Comparator.comparingInt((Participant p) -> p.score)
            .thenComparingInt((Participant p) -> p.maxCombo)
            .reversed());
        String winnerName = ranked.isEmpty() ? "" : ranked.get(0).name;
        boolean multiplayer = ranked.size() >= 2;
        String trackKey = s.mode == HotPotMinigameLogic.Mode.RUSH
            ? "rush" : (s.track != null ? s.track.id().toString() : "rhythm");

        for (Participant p : ranked) {
            PLAYER_SESSION.remove(p.uuid);
            String bestKey = p.uuid + "|" + s.mode.id() + "|" + trackKey + "|" + (s.virtualPlate ? "v" : "r");
            int prevBest = BEST_SCORES.getOrDefault(bestKey, 0);
            boolean isNewBest = p.score > prevBest && p.score > 0;
            if (isNewBest) {
                BEST_SCORES.put(bestKey, p.score);
            }
            int finalBest = Math.max(prevBest, p.score);

            ServerPlayer sp = server == null ? null : server.getPlayerList().getPlayer(p.uuid);
            if (sp != null) {
                if (s.mode == HotPotMinigameLogic.Mode.RUSH && p.score > 0) {
                    HotPotCriterion.fire(sp, "minigame_rush");
                }
                if (s.mode == HotPotMinigameLogic.Mode.RHYTHM && p.score > 0) {
                    HotPotCriterion.fire(sp, "minigame_rhythm");
                }
                if (multiplayer && p.score > 0) {
                    HotPotCriterion.fire(sp, "minigame_table_feast");
                }
                if (sp.getVehicle() instanceof SeatEntity) {
                    HotPotCriterion.fire(sp, "eat_seated");
                    HotPotCriterion.fire(sp, "eat_seated_bench");
                }
                JsonObject env = new JsonObject();
                env.addProperty("kind", "result");
                env.addProperty("sessionId", s.id);
                env.addProperty("mode", s.mode.id());
                env.addProperty("virtualPlate", s.virtualPlate);
                env.addProperty("trackId", s.track != null ? s.track.id().toString() : "");
                env.addProperty("potCleared", s.potCleared);
                env.addProperty("multiplayer", multiplayer);
                env.addProperty("isNewBest", isNewBest);
                env.addProperty("bestScore", finalBest);
                env.addProperty("winnerName", winnerName);
                env.add("leaderboard", serializeLeaderboard(ranked));
                HotPotNetwork.sendMinigameSync(sp, GSON.toJson(env));

                if (multiplayer) {
                    sp.sendSystemMessage(Component.literal(
                        "[火锅对决] 冠军: " + winnerName + " | 你的得分: " + p.score
                            + (s.potCleared ? "（全锅清盘！）" : ""))
                        .withStyle(ChatFormatting.GOLD));
                } else {
                    sp.sendOverlayMessage(Component.literal(
                        "[火锅挑战] 得分: " + p.score + (isNewBest ? "（新纪录！）" : "（最佳: " + finalBest + "）"))
                        .withStyle(ChatFormatting.GOLD));
                }
            }
        }
        s.level.playSound(null, s.potPos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.55F, 1.2F);
    }

    private static int consumeFoodForPlayer(ServerLevel level, BlockPos potPos, ServerPlayer player, ItemStack eaten) {
        FoodProperties food = ConfiguredFoodItem.food(eaten);
        if (food == null) {
            food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build();
        }
        ItemStack copy = eaten.copy();
        DippingSauceItem.dipFromHands(player, copy);
        HotPotHooks.onEating(level, player, copy);
        FoodConsumption.consume(level, player, copy, food);
        HotPotCriterion.fire(player, "minigame_eat");
        if (player.getVehicle() instanceof SeatEntity) {
            HotPotCriterion.fire(player, "eat_seated");
            HotPotCriterion.fire(player, "eat_seated_bench");
        }
        spawnPotParticles(level, potPos, eaten);
        level.playSound(null, potPos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.45F, 1.1F);
        return food.nutrition();
    }

    private static void spawnPotParticles(ServerLevel level, BlockPos potPos, ItemStack food) {
        double cx = potPos.getX() + 0.5;
        double cy = potPos.getY() + 0.95;
        double cz = potPos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy, cz, 5, 0.22, 0.16, 0.22, 0.03);
        level.sendParticles(ParticleTypes.FLAME, cx, cy - 0.1, cz, 4, 0.2, 0.1, 0.2, 0.015);
        ItemStack icon = food.isEmpty() ? new ItemStack(Items.COOKED_BEEF) : food;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(icon.copyWithCount(1))),
            cx, cy + 0.1, cz, 6, 0.18, 0.12, 0.18, 0.05);
    }

    private static void cancelInvite(Invite inv, String reason, MinecraftServer server) {
        INVITES.remove(inv.id);
        PLAYER_INVITE.remove(inv.hostId);
        for (UUID uid : inv.targets) {
            PLAYER_INVITE.remove(uid);
        }
        if (server != null) {
            ServerPlayer host = server.getPlayerList().getPlayer(inv.hostId);
            if (host != null) {
                sendClear(host, reason);
            }
            for (UUID uid : inv.targets) {
                ServerPlayer g = server.getPlayerList().getPlayer(uid);
                if (g != null) {
                    sendClear(g, reason);
                }
            }
        }
    }

    private static void broadcastInvite(Invite inv, MinecraftServer server) {
        if (server == null) {
            return;
        }
        long remaining = Math.max(0L, inv.expiresTick - serverTicks);
        JsonArray acceptedNames = new JsonArray();
        for (UUID uid : inv.accepted) {
            ServerPlayer p = server.getPlayerList().getPlayer(uid);
            acceptedNames.add(p != null ? p.getScoreboardName() : uid.toString().substring(0, 6));
        }
        JsonArray targetNames = new JsonArray();
        for (UUID uid : inv.targets) {
            ServerPlayer p = server.getPlayerList().getPlayer(uid);
            targetNames.add(p != null ? p.getScoreboardName() : uid.toString().substring(0, 6));
        }

        List<UUID> recipients = new ArrayList<>();
        recipients.add(inv.hostId);
        for (UUID uid : inv.targets) {
            if (!inv.declined.contains(uid)) {
                recipients.add(uid);
            }
        }
        for (UUID uid : recipients) {
            ServerPlayer sp = server.getPlayerList().getPlayer(uid);
            if (sp == null) {
                continue;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("kind", "invite");
            obj.addProperty("inviteId", inv.id);
            obj.addProperty("hostName", inv.hostName);
            obj.addProperty("isHost", inv.hostId.equals(uid));
            obj.addProperty("acceptedBySelf", inv.accepted.contains(uid));
            obj.addProperty("potX", inv.potPos.getX());
            obj.addProperty("potY", inv.potPos.getY());
            obj.addProperty("potZ", inv.potPos.getZ());
            obj.addProperty("mode", inv.mode.id());
            obj.addProperty("virtualPlate", inv.virtualPlate);
            obj.addProperty("trackId", inv.trackId.toString());
            obj.addProperty("remainingTicks", remaining);
            obj.addProperty("totalTicks", Config.minigameInviteTimeoutTicks());
            obj.add("acceptedNames", acceptedNames);
            obj.add("targetNames", targetNames);
            HotPotNetwork.sendMinigameSync(sp, GSON.toJson(obj));
        }
    }

    private static void broadcastSession(Session s, String feedbackType, String feedbackPlayer) {
        MinecraftServer server = s.level.getServer();
        if (server == null) {
            return;
        }
        List<Participant> ranked = new ArrayList<>(s.players.values());
        ranked.sort(Comparator.comparingInt((Participant p) -> p.score)
            .thenComparingInt((Participant p) -> p.maxCombo)
            .reversed());

        JsonObject obj = new JsonObject();
        obj.addProperty("kind", "session");
        obj.addProperty("sessionId", s.id);
        obj.addProperty("potX", s.potPos.getX());
        obj.addProperty("potY", s.potPos.getY());
        obj.addProperty("potZ", s.potPos.getZ());
        obj.addProperty("mode", s.mode.id());
        obj.addProperty("virtualPlate", s.virtualPlate);
        obj.addProperty("trackId", s.track != null ? s.track.id().toString() : "");
        obj.addProperty("elapsedTicks", Math.max(0L, serverTicks - s.startTick));
        obj.addProperty("remainingTicks", Math.max(0L, s.endTick - serverTicks));
        obj.addProperty("totalTicks", Math.max(1L, s.endTick - s.startTick));
        obj.addProperty("cookedRemaining", s.virtualPlate ? -1 : countCookedAt(s.level, s.potPos));
        obj.addProperty("feedbackType", feedbackType == null ? "" : feedbackType);
        obj.addProperty("feedbackPlayer", feedbackPlayer == null ? "" : feedbackPlayer);
        obj.add("leaderboard", serializeLeaderboard(ranked));

        String payload = GSON.toJson(obj);
        for (Participant p : ranked) {
            ServerPlayer sp = server.getPlayerList().getPlayer(p.uuid);
            if (sp != null) {
                HotPotNetwork.sendMinigameSync(sp, payload);
            }
        }
    }

    private static JsonArray serializeLeaderboard(List<Participant> ranked) {
        JsonArray arr = new JsonArray();
        for (Participant p : ranked) {
            JsonObject row = new JsonObject();
            row.addProperty("name", p.name);
            row.addProperty("score", p.score);
            row.addProperty("combo", p.combo);
            row.addProperty("maxCombo", p.maxCombo);
            row.addProperty("bites", p.bites);
            row.addProperty("perfects", p.perfects);
            row.addProperty("goods", p.goods);
            row.addProperty("misses", p.misses);
            row.addProperty("rateLimitedHits", p.rateLimitedHits);
            arr.add(row);
        }
        return arr;
    }

    private static void sendClear(ServerPlayer player, String reason) {
        JsonObject obj = new JsonObject();
        obj.addProperty("kind", "clear");
        obj.addProperty("reason", reason == null ? "" : reason);
        HotPotNetwork.sendMinigameSync(player, GSON.toJson(obj));
    }

    private static Session sessionOf(ServerPlayer player) {
        Integer id = PLAYER_SESSION.get(player.getUUID());
        return id == null ? null : SESSIONS.get(id);
    }

    private static Invite findInviteFor(UUID playerId) {
        Integer id = PLAYER_INVITE.get(playerId);
        return id == null ? null : INVITES.get(id);
    }

    private static int countCookedAt(ServerLevel level, BlockPos potPos) {
        HotPotBlockEntity be = findPotEntity(level, potPos);
        return be == null ? 0 : be.countCookedForMinigame();
    }

    private static HotPotBlockEntity findPotEntity(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof HotPotBlockEntity be && be.hasPot()) {
            return be;
        }
        if (level.getBlockState(pos).getBlock() instanceof StoveTopBlock
            && level.getBlockEntity(pos.below()) instanceof HotPotBlockEntity be && be.hasPot()) {
            return be;
        }
        return null;
    }

    private static BlockPos resolvePotPos(ServerLevel level, ServerPlayer player, BlockPos rawPos) {
        if (rawPos != null && !BlockPos.ZERO.equals(rawPos)) {
            if (level.getBlockState(rawPos).getBlock() instanceof StoveTopBlock) {
                rawPos = rawPos.below();
            }
            if (findPotEntity(level, rawPos) != null) {
                return rawPos.immutable();
            }
        }
        BlockPos around = BlockPos.containing(player.getX(), player.getY() + 0.5, player.getZ());
        HotPotBlockEntity nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(around.offset(-5, -2, -5), around.offset(5, 2, 5))) {
            HotPotBlockEntity be = findPotEntity(level, candidate);
            if (be != null) {
                double d = candidate.distSqr(around);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = be;
                }
            }
        }
        return nearest == null ? null : nearest.getBlockPos().immutable();
    }
}
