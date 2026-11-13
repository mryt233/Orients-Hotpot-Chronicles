// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import cn.blockforge.generated.orientshotpotchronicles.client.GuiKit;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.*;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigValues.*;

/** 设置界面的绘制和命中表（mryt233 改：26.1 GuiGraphicsExtractor）。 */
public final class ConfigPainter {
    private static final int TAB_W = 100, TAB_H = 20, TAB_GAP = 4;
    private static final int BTN = 16, RESET_W = 14, CTRL_GAP = 2;
    private static final int BOTTOM_H = 20, SCROLL_W = 5;
    private static final int TEXT = 0xFFE8C88A, MUTED = 0xFFA88C5C, HOVER = 0xFF6A4418, STRIPE = 0x18FFFFFF, PANEL_BG = 0xFF2A190C, BAD = 0xFFFF6A6A, CONFIRM = 0xFFFF9A5A;
    public interface Host {
        Font font();
        int tab();
        int px();
        int py();
        int panelW();
        int panelH();
        int listTop();
        int listBottom();
        int listX();
        int listW();
        int scroll();
        Set<String> expanded();
        boolean confirmReset();
        List<int[]> clickRects();
        List<Runnable> clickActions();
        ConfigValues values();
        ConfigEditController editor();
        void setTab(int value);
        void setScroll(int value);
        void setHoverField(Field value);
        void setHoverTip(List<FormattedCharSequence> value);
        void setResetRectIndex(int value);
        void clearLayout();
        void applyTab();
        int valueWidth();
        List<Object> layout();
        int contentHeight();
        int maxScroll();
        void save();
        void resetAllClicked();
        void cancel();
        void nudge(Field field, int dir);
        void particle(int percent);
    }
    private final Host host;
    public ConfigPainter(Host host) { this.host = host; }
    /** 悬停说明：一句白话（管什么）+ 一行范围 / 默认值（= 配置文件的 _字段说明）。 */
    public List<FormattedCharSequence> tooltipFor(Field f) {
        List<FormattedCharSequence> lines = new java.util.ArrayList<>();
        lines.add(f.label().copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
        lines.addAll(host.font().split(Component.translatable(f.helpKey()).withStyle(ChatFormatting.GRAY), 200));
        lines.add(Component.translatable(k("tooltip.range"),
                formatValue(f, f.min()), formatValue(f, f.max()), formatValue(f, host.values().defaultValue(f)))
            .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        if (host.editor().bad.containsKey(f.key())) {
            lines.add(Component.translatable(k("msg.invalid_line")).withStyle(ChatFormatting.RED).getVisualOrderText());
        }
        return lines;
    }

    /** 一行文字的提示（快捷档 / 编辑框 / ↺ 这些自绘小按钮用）。 */
    private static List<FormattedCharSequence> tip(String key) {
        return List.of(Component.translatable(key).getVisualOrderText());
    }

    public void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String[] keys = {"common", "advanced"};
        int startX = host.px() + (host.panelW() - (TAB_W * 2 + TAB_GAP)) / 2;
        for (int i = 0; i < 2; i++) {
            int x = startX + i * (TAB_W + TAB_GAP);
            int y = host.py() + 24;
            boolean hover = hit(mouseX, mouseY, x, y, TAB_W, TAB_H);
            boolean active = i == host.tab();
            copperButton(g, x, y, TAB_W, TAB_H, active ? HOVER : (hover ? 0xFF4A2E12 : PANEL_BG), active ? GuiKit.BRASS_HI : GuiKit.BRASS);
            if (active) {
                g.fill(x, y + TAB_H - 2, x + TAB_W, y + TAB_H, GuiKit.BRASS_HI);
            }
            fittedText(g, host.font(), Component.translatable(k("tab." + keys[i])), x + TAB_W / 2, y + 6, TAB_W - 8,
                active ? GuiKit.TITLE : TEXT);
            final int target = i;
            clickable(x, y, TAB_W, TAB_H, () -> {
                if (host.tab() == target) {
                    return;
                }
                host.setTab(target);
                host.clearLayout();
                host.applyTab();
            });
        }
    }

    /** 粒子强度：滑块 + 一行白话提示 + 右侧三个快捷档（0 / 50 / 100%）。 */
    public void drawParticleBlock(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // mryt233 改（2026-11-09，r146a）：提示独占一行、给满宽度（不再被快捷档挤到截断）；
        // 三个快捷档放到下面一行，右对齐。
        int hintY = host.py() + 48 + 22;
        Component hint = Component.translatable(k("particles.hint"));
        g.text(host.font(), hint, host.px() + 12, hintY, MUTED, false);
        int qw = Math.max(36, Math.min(64, (host.panelW() - 24) / 6));
        int[] percents = {0, 50, 100};
        int totalW = percents.length * qw + (percents.length - 1) * 4;
        int x0 = host.px() + host.panelW() - 10 - totalW;
        int quickY = hintY + 13;
        for (int i = 0; i < percents.length; i++) {
            int x = x0 + i * (qw + 4);
            int y = quickY;
            boolean hover = hit(mouseX, mouseY, x, y, qw, 12);
            boolean chosen = Config.particleIntensity() == percents[i];
            copperButton(g, x, y, qw, 12, chosen ? HOVER : (hover ? 0xFF4A2E12 : PANEL_BG), GuiKit.BRASS);
            g.centeredText(host.font(), Component.literal(percents[i] + "%"), x + qw / 2, y + 2,
                chosen ? GuiKit.TITLE : TEXT);
            final int percent = percents[i];
            clickable(x, y, qw, 12, () -> host.particle(percent));
            if (hover) {
                host.setHoverTip(tip(k("common.quick")));
            }
        }
    }

    public void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Object> elements = host.layout();
        host.setScroll(Math.max(0, Math.min(host.scroll(), host.maxScroll())));
        int total = host.contentHeight();
        int view = Math.max(1, host.listBottom() - host.listTop());
        int y = host.listTop() - host.scroll();
        g.enableScissor(host.listX(), host.listTop(), host.listX() + host.listW() + SCROLL_W + 4, host.listBottom());
        for (Object element : elements) {
            int h = ConfigListModel.rowHeight(element);
            if (y + h > host.listTop() && y < host.listBottom()) {
                if (element instanceof Header header) {
                    drawHeader(g, header, y, h, mouseX, mouseY);
                } else {
                    drawRow(g, (Row) element, y, h, mouseX, mouseY);
                }
            }
            y += h;
            if (y > host.listBottom()) {
                break;
            }
        }
        g.disableScissor();
        if (total > view) {
            drawScrollbar(g, host.listX() + host.listW() + 3, host.listTop(), view, total);
        }
    }

    private void drawHeader(GuiGraphicsExtractor g, Header header, int y, int h, int mouseX, int mouseY) {
        boolean open = host.expanded().contains(header.titleKey());
        boolean hover = hit(mouseX, mouseY, host.listX(), y, host.listW(), h);
        g.fill(host.listX(), y, host.listX() + host.listW(), y + h - 1, hover ? 0x30B47A1C : 0x204A2E12);
        g.fill(host.listX(), y + h - 1, host.listX() + host.listW(), y + h, GuiKit.BRASS_DARK);
        String mark = open ? "\u25BE " : "\u25B8 ";
        g.text(host.font(), mark + Component.translatable(header.titleKey()).getString()
            + "  (" + header.count() + ")", host.listX() + 6, y + (h - 8) / 2, GuiKit.TITLE, true);
        clickableInList(host.listX(), y, host.listW(), h, () -> {
            if (!host.expanded().remove(header.titleKey())) {
                host.expanded().add(header.titleKey());
            }
            host.clearLayout();
        });
    }

    private void drawRow(GuiGraphicsExtractor g, Row row, int y, int h, int mouseX, int mouseY) {
        Field f = row.field();
        boolean hover = hit(mouseX, mouseY, host.listX(), y, host.listW(), h);
        g.fill(host.listX(), y, host.listX() + host.listW(), y + h - 1, hover ? 0x30FFFFFF : STRIPE);
        int labelX = host.listX() + 6;
        if (!f.icon().isEmpty()) {
            g.item(f.icon(), host.listX() + 3, y + (h - 16) / 2);
            labelX = host.listX() + 24;
        }
        int textY = y + (h - row.lines().size() * 10) / 2;
        for (FormattedCharSequence line : row.lines()) {
            g.text(host.font(), line, labelX, textY, TEXT, false);
            textY += 10;
        }
        if (hover) {
            host.setHoverField(f);
        }
        drawFieldControls(g, f, y, h, mouseX, mouseY);
    }

    /** 一行的控件：[-  数值  +  ↺]，纵向居中。 */
    private void drawFieldControls(GuiGraphicsExtractor g, Field f, int y, int h, int mouseX, int mouseY) {
        int vw = host.valueWidth();
        int resetX = host.listX() + host.listW() - 6 - RESET_W;
        int plusX = resetX - CTRL_GAP - BTN;
        int valueX = plusX - CTRL_GAP - vw;
        int minusX = valueX - CTRL_GAP - BTN;
        int by = y + (h - BTN) / 2;
        drawButton(g, minusX, by, "-", hit(mouseX, mouseY, minusX, by, BTN, BTN));
        clickableInList(minusX, by, BTN, BTN, () -> host.nudge(f, -1));
        drawButton(g, plusX, by, "+", hit(mouseX, mouseY, plusX, by, BTN, BTN));
        clickableInList(plusX, by, BTN, BTN, () -> host.nudge(f, 1));
        drawValueBox(g, f, valueX, y + (h - 16) / 2, vw, mouseX, mouseY);
        // 只在当前值和默认值不同的时候才出「↺」
        if (!host.values().isDefault(f)) {
            boolean hover = hit(mouseX, mouseY, resetX, by, RESET_W, BTN);
            copperButton(g, resetX, by, RESET_W, BTN, hover ? HOVER : PANEL_BG, GuiKit.BRASS);
            g.centeredText(host.font(), Component.literal("\u21BA"), resetX + RESET_W / 2, by + 3,
                hover ? GuiKit.TITLE : MUTED);
            clickableInList(resetX, by, RESET_W, BTN, () -> {
                host.values().setValue(f, host.values().defaultValue(f));
                host.editor().bad.remove(f.key());
            });
            if (hover) {
                host.setHoverTip(tip(k("reset_one")));
            }
        }
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, String label, boolean hover) {
        copperButton(g, x, y, BTN, BTN, hover ? HOVER : PANEL_BG, GuiKit.BRASS);
        g.centeredText(host.font(), Component.literal(label), x + BTN / 2, y + 4, hover ? GuiKit.TITLE : TEXT);
    }

    private void drawValueBox(GuiGraphicsExtractor g, Field f, int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 16);
        valueBox(g, x, y, w, 16, hover ? 0xFF3A2410 : 0xFF1A0E05, hover ? GuiKit.BRASS_HI : GuiKit.BRASS_DARK);
        if (host.editor().editBox != null && host.editor().editField == f) {
            return;   // 正在键入：EditBox 自己画
        }
        String typedBad = host.editor().bad.get(f.key());
        if (typedBad != null) {
            g.centeredText(host.font(), host.font().plainSubstrByWidth(typedBad, w - 4), x + w / 2, y + 4, BAD);
            return;
        }
        g.centeredText(host.font(), valueText(f, host.values().value(f), w), x + w / 2, y + 4, GuiKit.RIVET);
        if (hover) {
            host.setHoverTip(tip(k("edit.hint")));
        }
        clickableInList(x, y, w, 16, () -> host.editor().startEdit(host.font(), f, x, y, w, 16));
    }

    /** 数值 + 单位；tick 类的再折算成秒（放得下才显示，免得挤）。 */
    private Component valueText(Field f, double v, int w) {
        String num = formatValue(f, v);
        if (f.unit() == Unit.PLAIN) {
            return Component.literal(num);
        }
        MutableComponent text = Component.literal(num + " ").append(Component.translatable(f.unit().langKey()));
        if (f.unit() == Unit.TICK && v >= 20) {
            MutableComponent withSeconds = text.copy()
                .append(Component.literal(" ("
                    + String.format(Locale.ROOT, "%.1f", v / 20.0) + " ")
                    .withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(k("unit.seconds")).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
            if (host.font().width(withSeconds) <= w - 6) {
                return withSeconds;
            }
        }
        return text;
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int view, int total) {
        g.fill(x, top, x + SCROLL_W, top + view, 0x40FFFFFF);
        int thumbH = Math.max(12, view * view / Math.max(1, total));
        int max = Math.max(1, total - view);
        int thumbY = top + (view - thumbH) * host.scroll() / max;
        g.fill(x, thumbY, x + SCROLL_W, thumbY + thumbH, GuiKit.BRASS);
    }

    public void drawBottomBar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String[] keys = {"save", "reset", "cancel"};
        int gap = 6;
        int btnW = Math.max(70, Math.min(150, (host.panelW() - 24 - gap * 2) / 3));
        int startX = host.px() + (host.panelW() - (btnW * 3 + gap * 2)) / 2;
        int y = host.py() + host.panelH() - BOTTOM_H - 8;
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (btnW + gap);
            boolean hover = hit(mouseX, mouseY, x, y, btnW, BOTTOM_H);
            boolean confirming = i == 1 && host.confirmReset();
            copperButton(g, x, y, btnW, BOTTOM_H, confirming ? 0x60B47A1C : (hover ? 0xFF4A2E12 : PANEL_BG), confirming ? CONFIRM : GuiKit.BRASS);
            fittedText(g, host.font(), Component.translatable(k(confirming ? "reset.confirm" : keys[i])),
                x + btnW / 2, y + 6, btnW - 6, confirming ? CONFIRM : TEXT);
            if (i == 1) {
                host.setResetRectIndex(host.clickRects().size());
            }
            clickable(x, y, btnW, BOTTOM_H, switch (i) {
                case 0 -> host::save;
                case 1 -> host::resetAllClicked;
                default -> host::cancel;
            });
        }
    }

    private void clickable(int x, int y, int w, int h, Runnable action) {
        addClick(x, y, w, h, action);
    }

    /**
     * 列表里的控件：只有整块都落在可见的列表区域里才收进命中表。
     * 否则滚到一半的行会有一半贴在列表外面，和上方的快捷档 / 下方的按钮抢鼠标。
     */
    private void clickableInList(int x, int y, int w, int h, Runnable action) {
        if (y < host.listTop() || y + h > host.listBottom()) {
            return;
        }
        addClick(x, y, w, h, action);
    }

    private void addClick(int x, int y, int w, int h, Runnable action) {
        host.clickRects().add(new int[]{x, y, w, h});
        host.clickActions().add(action);
    }

}
