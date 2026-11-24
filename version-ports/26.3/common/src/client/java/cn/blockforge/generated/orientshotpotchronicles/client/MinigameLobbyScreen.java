// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
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
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants; // mryt233 改：26.3 SDL 按键

import java.util.List;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 火锅即时小游戏开局设置面板（模式、真菜/虚拟练习盘、音游曲目、单人开局 / 15 秒同桌对战邀约）。作者：mryt233
 */
public class MinigameLobbyScreen extends Screen {
    private static final int PW = 264;
    private static final int PH = 204;

    private final BlockPos potPos;
    private HotPotMinigameLogic.Mode mode = HotPotMinigameLogic.Mode.RUSH;
    private boolean virtualPlate = false;
    private int trackIndex = 0;
    private List<HotPotTracks.HotPotTrack> tracks = List.of();
    private int px, py;

    public MinigameLobbyScreen(BlockPos potPos) {
        super(Component.literal("火锅即时挑战"));
        this.potPos = potPos == null ? BlockPos.ZERO : potPos.immutable();
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        tracks = HotPotTracks.all(true);
        if (tracks.isEmpty()) {
            tracks = HotPotTracks.all(false);
        }
        if (trackIndex >= tracks.size()) {
            trackIndex = 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // 由 extractRenderState 自绘半透遮罩与黄铜木框，避免默认背景模糊盖在面板文字上方
    }

    public HotPotMinigameLogic.Mode selectedMode() {
        return mode;
    }

    public boolean selectedVirtual() {
        return virtualPlate;
    }

    public Identifier selectedTrackId() {
        if (tracks.isEmpty()) {
            return HotPotTracks.DEFAULT_TRACK_ID;
        }
        return tracks.get(Math.max(0, Math.min(tracks.size() - 1, trackIndex))).id();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x700A0502);
        g.pose().pushMatrix();
        g.pose().translate(px, py);

        frame(g, PW, PH, 3, true);
        fittedText(g, font, Component.literal("火锅即时挑战 · 聚餐对决"), PW / 2, 10, PW - 24, TITLE);
        g.fill(14, 22, PW - 14, 23, BRASS_DARK);

        int mx = mouseX - px;
        int my = mouseY - py;

        // 锅内熟食概览
        int cookedCount = cookedCountClient();
        String potInfo = cookedCount > 0
            ? "锅内现有熟食: " + cookedCount + " 份（默认真实消耗，绕开饥饿上限）"
            : "当前锅内暂无熟食（煮熟再战，或切换虚拟练习盘）";
        fittedText(g, font, Component.literal(potInfo), PW / 2, 28, PW - 28,
            cookedCount > 0 ? 0xFF9CE07A : 0xFFE8A068);

        // 1. 挑战模式
        g.text(font, Component.literal("挑战模式"), 16, 44, LABEL, true);
        drawTabButton(g, 16, 56, 112, 22, "连打吃菜 (SPACE)", mode == HotPotMinigameLogic.Mode.RUSH,
            hit(mx, my, 16, 56, 112, 22));
        drawTabButton(g, 136, 56, 112, 22, "4 轨音游 (D F J K)", mode == HotPotMinigameLogic.Mode.RHYTHM,
            hit(mx, my, 136, 56, 112, 22));

        // 2. 食物来源开关
        g.text(font, Component.literal("食物消耗方式"), 16, 86, LABEL, true);
        boolean allowVirtual = Config.minigameAllowVirtualPlate();
        drawTabButton(g, 16, 98, 112, 22, "真实消耗熟食", !virtualPlate,
            hit(mx, my, 16, 98, 112, 22));
        drawTabButton(g, 136, 98, 112, 22, allowVirtual ? "虚拟练习盘 (不耗菜)" : "虚拟盘已由服主禁用",
            virtualPlate && allowVirtual, allowVirtual && hit(mx, my, 136, 98, 112, 22));

        // 3. 音游曲目选择器 / 模式说明卡片
        g.fill(16, 127, PW - 16, 160, 0xC0160D06);
        g.fill(16, 127, PW - 16, 128, BRASS_DARK);
        if (mode == HotPotMinigameLogic.Mode.RHYTHM && !tracks.isEmpty()) {
            HotPotTracks.HotPotTrack track = tracks.get(trackIndex);
            drawArrowBtn(g, 20, 133, 20, 20, "<", hit(mx, my, 20, 133, 20, 20));
            drawArrowBtn(g, PW - 40, 133, 20, 20, ">", hit(mx, my, PW - 40, 133, 20, 20));
            Component titleComp = track.titleKey().isEmpty()
                ? Component.literal(track.title()) : Component.translatable(track.titleKey());
            fittedText(g, font, titleComp, PW / 2, 132, PW - 92, 0xFFFFE59A);
            String meta = track.bpm() + " BPM · " + track.difficulty().toUpperCase()
                + " · " + track.notes().size() + " 音符";
            fittedText(g, font, Component.literal(meta), PW / 2, 145, PW - 92, 0xFFB89E7A);
        } else {
            String desc = mode == HotPotMinigameLogic.Mode.RUSH
                ? "在 10 秒内狂按空格吃菜！服务端按 max_cps=" + Config.minigameRushMaxCps() + " 严格防宏。"
                : "正在加载数据包 hotpot_tracks 曲目列表…";
            fittedText(g, font, Component.literal(desc), PW / 2, 139, PW - 40, 0xFFD9C4A3);
        }

        // 4. 底部操作按钮
        drawActionBtn(g, 16, 168, 76, 22, "单人开局", 0xFF3E7835, 0xFF83D96F, hit(mx, my, 16, 168, 76, 22));
        drawActionBtn(g, 98, 168, 96, 22, "邀请同桌 (15s)", 0xFFA35B1A, 0xFFF0B240, hit(mx, my, 98, 168, 96, 22));
        drawActionBtn(g, 200, 168, 48, 22, "返回", 0xFF4A2E1A, 0xFFB47A1C, hit(mx, my, 200, 168, 48, 22));

        g.pose().popMatrix();
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // mryt233 改：26.3 鼠标事件使用 SDL 左键常量。
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) event.x() - px;
        int my = (int) event.y() - py;
        if (hit(mx, my, 16, 56, 112, 22)) {
            mode = HotPotMinigameLogic.Mode.RUSH;
            return true;
        }
        if (hit(mx, my, 136, 56, 112, 22)) {
            mode = HotPotMinigameLogic.Mode.RHYTHM;
            return true;
        }
        if (hit(mx, my, 16, 98, 112, 22)) {
            virtualPlate = false;
            return true;
        }
        if (Config.minigameAllowVirtualPlate() && hit(mx, my, 136, 98, 112, 22)) {
            virtualPlate = true;
            return true;
        }
        if (mode == HotPotMinigameLogic.Mode.RHYTHM && !tracks.isEmpty()) {
            if (hit(mx, my, 20, 133, 20, 20)) {
                trackIndex = (trackIndex - 1 + tracks.size()) % tracks.size();
                return true;
            }
            if (hit(mx, my, PW - 40, 133, 20, 20)) {
                trackIndex = (trackIndex + 1) % tracks.size();
                return true;
            }
        }
        if (hit(mx, my, 16, 168, 76, 22)) {
            sendLaunch(false);
            return true;
        }
        if (hit(mx, my, 98, 168, 96, 22)) {
            sendLaunch(true);
            return true;
        }
        if (hit(mx, my, 200, 168, 48, 22)) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void sendLaunch(boolean invite) {
        boolean useVirtual = virtualPlate && Config.minigameAllowVirtualPlate();
        String args = (useVirtual ? "v|" : "r|") + selectedTrackId();
        ClientNetwork.send(MinigameActionPayload.of(
            invite ? Action.SEND_INVITE : Action.START_SOLO,
            potPos, mode.ordinal(), args));
        onClose();
    }

    private int cookedCountClient() {
        if (minecraft == null || minecraft.level == null) {
            return 0;
        }
        if (minecraft.level.getBlockEntity(potPos) instanceof HotPotBlockEntity be) {
            return be.countCookedForMinigame();
        }
        return 0;
    }

    private void drawTabButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean selected, boolean hover) {
        int bg = selected ? COPPER : (hover ? 0xFF4F2F18 : 0xFF2B180B);
        int top = selected ? BRASS_HI : (hover ? BRASS : BRASS_DARK);
        copperButton(g, x, y, w, h, bg, top);
        if (selected) {
            g.fill(x, y + h - 2, x + w, y + h, BRASS_HI);
        }
        fittedText(g, font, Component.literal(label), x + w / 2, y + 7, w - 10,
            selected ? 0xFFFFFFFF : 0xFFCFBA9B);
    }

    private void drawArrowBtn(GuiGraphicsExtractor g, int x, int y, int w, int h, String txt, boolean hover) {
        copperButton(g, x, y, w, h, hover ? COPPER : 0xFF3E2412, hover ? BRASS_HI : BRASS_DARK);
        fittedText(g, font, Component.literal(txt), x + w / 2, y + 6, w - 4, TITLE);
    }

    private void drawActionBtn(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int baseBg, int top, boolean hover) {
        copperButton(g, x, y, w, h, hover ? mix(baseBg, 0xFFFFFFFF, 0.16) : baseBg, top);
        fittedText(g, font, Component.literal(label), x + w / 2, y + 7, w - 8, 0xFFFFFFFF);
    }
}
