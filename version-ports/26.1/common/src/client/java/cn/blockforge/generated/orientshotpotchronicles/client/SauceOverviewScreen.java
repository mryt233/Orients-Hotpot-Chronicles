// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.ChatFormatting;

import cn.blockforge.orient.framework.client.ClientNetwork;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.network.OpenSauceRestockPayload;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 蘸料台总览（2026-10-23 用户要求）：右键蘸料台先打开这里，3×3 九张小卡片和台上的九个碗一一对应
 * （从最上层到最下层、每层从左到右），看每个碗装的什么、还剩多少；下面「补充蘸料台」按钮进补货界面（SauceStationScreen）。
 * 只读界面，数据直接看客户端的方块实体（存货变了服务端会同步过来）。2026-10-26 起和补货界面一起改成灶台那样的木框、浅色字（用户：灰底深字糊）。
 * 网页预览 viewer/screens.js 的 sauceOverview 与本类一一对应。
 * 作者：mryt233 */
public class SauceOverviewScreen extends Screen {
    public static final int W = 176, H = 176;
    public static final int CARD_X = 8, CARD_Y = 30, CARD_W = 52, CARD_H = 36, STEP_X = 54, STEP_Y = 38;
    private static final int MAX = SauceStationBlockEntity.MAX_RAW * SauceStationBlockEntity.PORTIONS_PER_RAW;
    private final BlockPos pos;
    private int x, y;

    public SauceOverviewScreen(BlockPos pos, Component title) {
        super(title);
        this.pos = pos;
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable(HotPotBlockEntity.key("screen.sauce.restock")),
                b -> ClientNetwork.send(new OpenSauceRestockPayload(pos)))
            .bounds(x + W / 2 - 50, y + H - 26, 100, 20).build());
    }

    private SauceStationBlockEntity station() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof SauceStationBlockEntity be ? be : null;
    }

    @Override
    public void tick() {
        // 台子被拆了 / 人走远了就关掉
        if (station() == null || minecraft.player == null || minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        SauceStationBlockEntity be = station();
        if (be == null) {
            return;
        }
        Component hover = null;
        List<Component> hoverLines = null;
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            int cx = x + CARD_X + (i % 3) * STEP_X, cy = y + CARD_Y + (i / 3) * STEP_Y;
            SeasoningDef s = be.kindDef(i);
            int left = Math.min(MAX, be.available(i));
            // 卡片 = 放大的凹槽：左上压暗、右下透亮
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, SLOT_LIGHT);
            g.fill(cx, cy, cx + CARD_W - 1, cy + CARD_H - 1, SLOT_SHADOW);
            g.fill(cx + 1, cy + 1, cx + CARD_W - 1, cy + CARD_H - 1, SLOT_BG);
            if (s != null && left > 0) {
                g.item(new ItemStack(s.rawShown(Util.getMillis())), cx + 3, cy + 3);
                Component name = Component.translatable(s.nameKey());
                g.text(font, font.substrByWidth(name, CARD_W - 24).getString(), cx + 21, cy + 4, TITLE, true);
                g.text(font, String.valueOf(left), cx + 21, cy + 14, LABEL, true);
                // 剩余条
                int bw = CARD_W - 8, fw = Math.round(bw * left / (float) MAX);
                g.fill(cx + 4, cy + CARD_H - 8, cx + 4 + bw, cy + CARD_H - 4, SLOT_SHADOW);
                g.fill(cx + 4, cy + CARD_H - 8, cx + 4 + fw, cy + CARD_H - 4, s.color());
            } else {
                g.centeredText(font, Component.translatable(HotPotBlockEntity.key(be.kindIndex(i) == -2 ? "sauce.unknown" : "screen.sauce.empty_bowl")),
                    cx + CARD_W / 2, cy + CARD_H / 2 - 4, SauceStationScreen.HINT);
            }
            if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                hoverLines = new ArrayList<>();
                if (s == null || left == 0) {
                    hoverLines.add(Component.translatable(HotPotBlockEntity.key(be.kindIndex(i) == -2 ? "sauce.unknown" : "screen.sauce.empty")));
                } else {
                    hoverLines.add(Component.translatable(s.nameKey()));
                    hoverLines.add(Component.translatable(s.effectKey()).withStyle(ChatFormatting.GRAY));
                }
                hoverLines.add(Component.translatable(HotPotBlockEntity.key("screen.sauce.left"), left, MAX).withStyle(ChatFormatting.GRAY));
            }
        }
        if (hoverLines != null) {
            g.setComponentTooltipForNextFrame(font, hoverLines, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        SauceStationScreen.panel(g, x, y, W, H);
        // 标题 + 一道黄铜线（同灶台），下面一行提示
        g.text(font, title, x + 12, y + 7, TITLE, true);
        g.fill(x + 12, y + 17, x + W - 12, y + 18, BRASS_DARK);
        g.text(font, Component.translatable(HotPotBlockEntity.key("screen.sauce.overview_hint")), x + 12, y + 20, SauceStationScreen.HINT, true);
    }
}
