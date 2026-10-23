package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * 蘸料台的补货界面（r103）：原版灰色容器样式（用户要原版的样子，不要金边）。9 个原料格 = 台上 9 个小碗，
 * 2026-10-23 起排成 3×3、和台上一一对应（左边标上层 / 中层 / 下层），每格右边一根竖的剩余条；由总览界面的「补充蘸料台」按钮打开。
 * 2026-10-23 用户改（Claude 改）：碗不固定，空碗放什么原料都行，所以空格子不再显示该放什么；
 * 右上角加「?」按钮，点开右边一张「原料表」：每种原料做成什么调料、有什么效果（开关状态这次游戏里一直记着）。
 * 网页预览 viewer/screens.js 的 sauceStation 与本类一一对应，改这里要同步改那边。
 * 作者：Claude
 */
public class SauceStationScreen extends HandledScreen<SauceStationScreenHandler> {
    private static final int W = 176;
    private static final int H = 178;
    private static final int MAX = SauceStationBlockEntity.MAX_RAW * SauceStationBlockEntity.PORTIONS_PER_RAW;
    private static final int BG = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, EDGE = 0xFF000000;
    private static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737, TEXT = 0xFF404040, HINT = 0xFF707070;
    /** 原料表：离主界面 2 像素，标题下每种一行 16 像素，原料图标最多两个并排。 */
    private static final int GAP = 2, GUIDE_ROW_Y = 18, GUIDE_ROW_H = 16, ICONS_W = 34;
    private static boolean guideOpen;
    private int guideW;

    public SauceStationScreen(SauceStationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.playerInventoryTitleY = SauceStationScreenHandler.INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        int textW = textRenderer.getWidth(guideTitle());
        for (Seasoning s : Seasoning.ALL) {
            textW = Math.max(textW, ICONS_W + 4 + textRenderer.getWidth(Text.translatable(s.nameKey()))
                + 4 + textRenderer.getWidth(effect(s)));
        }
        guideW = 7 + textW + 7;
        if (guideOpen) {
            x = (width - W - GAP - guideW) / 2;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), b -> {
                guideOpen = !guideOpen;
                clearAndInit();
            })
            .dimensions(x + W - 17, y + 4, 12, 12)
            .tooltip(Tooltip.of(guideTitle()))
            .build());
    }

    private static Text guideTitle() {
        return Text.translatable(HotPotBlockEntity.key("screen.sauce.guide"));
    }

    private static Text effect(Seasoning s) {
        return Text.translatable(HotPotBlockEntity.key("seasoning." + s.id() + ".effect"));
    }

    @Override
    protected void drawBackground(DrawContext g, float delta, int mouseX, int mouseY) {
        int x = this.x, y = this.y;
        panel(g, x, y, W, H);
        for (Slot slot : handler.slots) {
            slotFrame(g, x + slot.x, y + slot.y);
        }
        String[] tiers = {"screen.sauce.tier_top", "screen.sauce.tier_mid", "screen.sauce.tier_low"};
        for (int row = 0; row < 3; row++) {
            g.drawText(textRenderer, Text.translatable(HotPotBlockEntity.key(tiers[row])), x + 8,
                y + SauceStationScreenHandler.GRID_Y + row * SauceStationScreenHandler.STEP_Y + 4, HINT, false);
        }
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            int sx = x + SauceStationScreenHandler.slotX(i), sy = y + SauceStationScreenHandler.slotY(i);
            Seasoning s = handler.kind(i);
            // 格子里原料用完、碗里还剩份数：淡淡显示是哪种
            if (handler.getSlot(i).getStack().isEmpty() && s != null) {
                g.drawItem(new ItemStack(s.rawShown(Util.getMeasuringTimeMs())), sx, sy);
                g.fill(sx, sy, sx + 16, sy + 16, 0xB08B8B8B);
            }
            // 格子右边一根竖的剩余条（调料颜色，最多 64 份）
            int p = Math.min(MAX, handler.available(i));
            int bx = sx + 18, by = sy;
            g.fill(bx, by - 1, bx + 5, by + 17, SLOT_DARK);
            g.fill(bx + 1, by, bx + 4, by + 16, SLOT);
            int fh = Math.round(16 * p / (float) MAX);
            if (fh > 0 && s != null) {
                g.fill(bx + 1, by + 16 - fh, bx + 4, by + 16, s.color());
            }
        }
        if (guideOpen) {
            drawGuide(g, x + W + GAP, y);
        }
    }

    /** 原料表：每种调料一行——原料图标（几种并排）、调料名、效果（灰字）。 */
    private void drawGuide(DrawContext g, int px, int py) {
        panel(g, px, py, guideW, H);
        g.drawText(textRenderer, guideTitle(), px + 7, py + 6, TEXT, false);
        for (Seasoning s : Seasoning.ALL) {
            int ry = py + GUIDE_ROW_Y + s.ordinal() * GUIDE_ROW_H;
            List<Item> raws = s.raws();
            for (int k = 0; k < raws.size() && k < 2; k++) {
                g.drawItem(new ItemStack(raws.get(k)), px + 7 + k * 17, ry);
            }
            int tx = px + 7 + ICONS_W + 4;
            Text name = Text.translatable(s.nameKey());
            g.drawText(textRenderer, name, tx, ry + 4, TEXT, false);
            g.drawText(textRenderer, effect(s), tx + textRenderer.getWidth(name) + 4, ry + 4, HINT, false);
        }
    }

    private static void panel(DrawContext g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + 1, EDGE);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, EDGE);
        g.fill(x, y + 1, x + 1, y + h - 1, EDGE);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, DARK);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, DARK);
    }

    private static void slotFrame(DrawContext g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, LIGHT);
        g.fill(x - 1, y - 1, x + 16, y + 16, SLOT_DARK);
        g.fill(x, y, x + 16, y + 16, SLOT);
    }

    @Override
    protected void drawForeground(DrawContext g, int mouseX, int mouseY) {
        g.drawText(textRenderer, title, titleX, titleY, TEXT, false);
        g.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, TEXT, false);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        drawMouseoverTooltip(g, mouseX, mouseY);
        // 指着空的原料格：这个碗装的什么（或者是空碗）、还剩几份
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            Slot slot = handler.getSlot(i);
            if (slot.getStack().isEmpty() && isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                Seasoning s = handler.kind(i);
                List<Text> lines = new ArrayList<>();
                if (s == null) {
                    lines.add(Text.translatable(HotPotBlockEntity.key("screen.sauce.empty")));
                } else {
                    lines.add(Text.translatable(s.nameKey()));
                    lines.add(Text.translatable(HotPotBlockEntity.key("screen.sauce.slot"), s.rawNames()).formatted(Formatting.GRAY));
                }
                lines.add(Text.translatable(HotPotBlockEntity.key("screen.sauce.left"), handler.available(i), MAX).formatted(Formatting.GRAY));
                g.drawTooltip(textRenderer, lines, mouseX, mouseY);
            }
        }
    }
}
