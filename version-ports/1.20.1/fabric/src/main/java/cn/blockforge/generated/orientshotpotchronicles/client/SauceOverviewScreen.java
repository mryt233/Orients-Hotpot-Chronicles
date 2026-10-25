package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.OpenSauceRestockPayload;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 蘸料台总览（2026-10-23 用户要求）：右键蘸料台先打开这里，3×3 九张小卡片和台上的九个碗一一对应
 * （从最上层到最下层、每层从左到右），看每个碗装的什么、还剩多少；下面「补充蘸料台」按钮进补货界面（SauceStationScreen）。
 * 只读界面，数据直接看客户端的方块实体（存货变了服务端会同步过来）。原版灰色样式，同补货界面。
 * 网页预览 viewer/screens.js 的 sauceOverview 与本类一一对应。
 * 作者：
 */
public class SauceOverviewScreen extends Screen {
    public static final int W = 176, H = 176;
    public static final int CARD_X = 8, CARD_Y = 30, CARD_W = 52, CARD_H = 36, STEP_X = 54, STEP_Y = 38;
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, EDGE = 0xFF000000;
    private static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737, TEXT = 0xFF404040, HINT = 0xFF707070;
    private static final int MAX = SauceStationBlockEntity.MAX_RAW * SauceStationBlockEntity.PORTIONS_PER_RAW;
    private final BlockPos pos;
    private int x, y;

    public SauceOverviewScreen(BlockPos pos, Text title) {
        super(title);
        this.pos = pos;
    }

    @Override
    protected void init() {
        x = (width - W) / 2;
        y = (height - H) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable(HotPotBlockEntity.key("screen.sauce.restock")),
                b -> {
                    var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                    buf.writeBlockPos(pos);
                    ClientPlayNetworking.send(OpenSauceRestockPayload.ID, buf);
                })
            .dimensions(x + W / 2 - 50, y + H - 26, 100, 20).build());
    }

    private SauceStationBlockEntity station() {
        return client != null && client.world != null && client.world.getBlockEntity(pos) instanceof SauceStationBlockEntity be ? be : null;
    }

    @Override
    public void tick() {
        // 台子被拆了 / 人走远了就关掉
        if (station() == null || client.player == null || client.player.squaredDistanceTo(pos.toCenterPos()) > 64) {
            close();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        renderBackground(g); // 调整：1.20.1 Screen.render 不自动绘制背景。
        super.render(g, mouseX, mouseY, delta);
        SauceStationBlockEntity be = station();
        if (be == null) {
            return;
        }
        Text hover = null;
        List<Text> hoverLines = null;
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            int cx = x + CARD_X + (i % 3) * STEP_X, cy = y + CARD_Y + (i / 3) * STEP_Y;
            Seasoning s = be.kind(i);
            int left = Math.min(MAX, be.available(i));
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, SLOT_DARK);
            g.fill(cx + 1, cy + 1, cx + CARD_W, cy + CARD_H, LIGHT);
            g.fill(cx + 1, cy + 1, cx + CARD_W - 1, cy + CARD_H - 1, SLOT);
            if (s != null && left > 0) {
                g.drawItem(new ItemStack(s.rawShown(Util.getMeasuringTimeMs())), cx + 3, cy + 3);
                Text name = Text.translatable(s.nameKey());
                g.drawText(textRenderer, textRenderer.trimToWidth(name, CARD_W - 24).getString(), cx + 21, cy + 4, 0xFFFFFFFF, true);
                g.drawText(textRenderer, String.valueOf(left), cx + 21, cy + 14, 0xFFE0E0E0, true);
                // 剩余条
                int bw = CARD_W - 8, fw = Math.round(bw * left / (float) MAX);
                g.fill(cx + 4, cy + CARD_H - 8, cx + 4 + bw, cy + CARD_H - 4, SLOT_DARK);
                g.fill(cx + 4, cy + CARD_H - 8, cx + 4 + fw, cy + CARD_H - 4, s.color());
            } else {
                g.drawCenteredTextWithShadow(textRenderer, Text.translatable(HotPotBlockEntity.key("screen.sauce.empty_bowl")),
                    cx + CARD_W / 2, cy + CARD_H / 2 - 4, 0xFFC0C0C0);
            }
            if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                hoverLines = new ArrayList<>();
                if (s == null || left == 0) {
                    hoverLines.add(Text.translatable(HotPotBlockEntity.key("screen.sauce.empty")));
                } else {
                    hoverLines.add(Text.translatable(s.nameKey()));
                    hoverLines.add(Text.translatable(HotPotBlockEntity.key("seasoning." + s.id() + ".effect")).formatted(net.minecraft.util.Formatting.GRAY));
                }
                hoverLines.add(Text.translatable(HotPotBlockEntity.key("screen.sauce.left"), left, MAX).formatted(net.minecraft.util.Formatting.GRAY));
            }
        }
        if (hoverLines != null) {
            g.drawTooltip(textRenderer, hoverLines, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(DrawContext g) {
        super.renderBackground(g);
        g.fill(x + 1, y, x + W - 1, y + 1, EDGE);
        g.fill(x + 1, y + H - 1, x + W - 1, y + H, EDGE);
        g.fill(x, y + 1, x + 1, y + H - 1, EDGE);
        g.fill(x + W - 1, y + 1, x + W, y + H - 1, EDGE);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, BG);
        g.fill(x + 1, y + 1, x + W - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + H - 2, LIGHT);
        g.fill(x + 3, y + H - 3, x + W - 1, y + H - 1, DARK);
        g.fill(x + W - 3, y + 3, x + W - 1, y + H - 1, DARK);
        g.drawText(textRenderer, title, x + 8, y + 6, TEXT, false);
        g.drawText(textRenderer, Text.translatable(HotPotBlockEntity.key("screen.sauce.overview_hint")), x + 8, y + 17, HINT, false);
    }
}
