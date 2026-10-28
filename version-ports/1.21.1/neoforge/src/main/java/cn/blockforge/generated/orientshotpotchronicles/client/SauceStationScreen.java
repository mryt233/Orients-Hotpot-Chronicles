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

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 蘸料台的补货界面（r103）：原版灰色容器样式（用户要原版的样子，不要金边）。9 个原料格 = 台上 9 个小碗，
 * 2026-10-23 起排成 3×3、和台上一一对应（左边标上层 / 中层 / 下层），每格右边一根竖的剩余条；由总览界面的「补充蘸料台」按钮打开。
 * 2026-10-23 用户改（）：碗不固定，空碗放什么原料都行，所以空格子不再显示该放什么；
 * 右上角加「?」按钮，点开右边一张「原料表」：每种原料做成什么调料、有什么效果（开关状态这次游戏里一直记着）。
 * 2026-10-26 用户：原版浅灰底上的深灰字在界面尺寸 3 时显得糊，改成和灶台 / 火锅界面一样的木框（GuiKit），浅色字带阴影（）。
 * 网页预览 viewer/screens.js 的 sauceStation 与本类一一对应，改这里要同步改那边。
 *
 */
public class SauceStationScreen extends HandledScreen<SauceStationScreenHandler> {
    private static final int W = 176;
    private static final int H = 178;
    private static final int MAX = SauceStationBlockEntity.MAX_RAW * SauceStationBlockEntity.PORTIONS_PER_RAW;
    /** 次要的字（层名、效果）：标签色压暗一点。 */
    static final int HINT = 0xFFA88C5C;
    /** 原料表：离主界面 2 像素，标题下每种一行 16 像素，原料图标最多两个并排。 */
    // 原料表 10 行（2026-10-25 加蒜泥）：第一行从 17 开始才放得下（17 + 10 × 16 = 177，面板高 178；原来 18）
    private static final int GAP = 2, GUIDE_ROW_Y = 17, GUIDE_ROW_H = 16, ICONS_W = 34;
    private static boolean guideOpen;
    private int guideW;

    public SauceStationScreen(SauceStationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = 12;
        this.titleY = 7;
        this.playerInventoryTitleY = SauceStationScreenHandler.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int textW = textRenderer.getWidth(guideTitle());
        for (Seasoning s : Seasoning.ALL) {
            textW = Math.max(textW, ICONS_W + 4 + textRenderer.getWidth(Text.translatable(s.nameKey()))
                + 4 + textRenderer.getWidth(effect(s)));
        }
        guideW = Math.max(7 + textW + 7, 12 + textRenderer.getWidth(guideTitle()) + 14);
        if (guideOpen) {
            x = (width - W - GAP - guideW) / 2;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), b -> {
                guideOpen = !guideOpen;
                clearAndInit();
            })
            .dimensions(x + W - 26, y + 4, 12, 12)
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
                y + SauceStationScreenHandler.GRID_Y + row * SauceStationScreenHandler.STEP_Y + 4, HINT, true);
        }
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            int sx = x + SauceStationScreenHandler.slotX(i), sy = y + SauceStationScreenHandler.slotY(i);
            Seasoning s = handler.kind(i);
            // 格子里原料用完、碗里还剩份数：淡淡显示是哪种
            if (handler.getSlot(i).getStack().isEmpty() && s != null) {
                g.drawItem(new ItemStack(s.rawShown(Util.getMeasuringTimeMs())), sx, sy);
                g.fill(sx, sy, sx + 16, sy + 16, alpha(SLOT_BG, 0.7));
            }
            // 格子右边一根竖的剩余条（调料颜色，最多 64 份）
            int p = Math.min(MAX, handler.available(i));
            int bx = sx + 18, by = sy;
            g.fill(bx, by - 1, bx + 5, by + 17, SLOT_SHADOW);
            g.fill(bx + 1, by, bx + 4, by + 16, SLOT_BG);
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
        g.drawText(textRenderer, guideTitle(), px + 12, py + 7, TITLE, true);
        for (Seasoning s : Seasoning.ALL) {
            int ry = py + GUIDE_ROW_Y + s.ordinal() * GUIDE_ROW_H;
            List<Item> raws = s.raws();
            for (int k = 0; k < raws.size() && k < 2; k++) {
                g.drawItem(new ItemStack(raws.get(k)), px + 7 + k * 17, ry);
            }
            int tx = px + 7 + ICONS_W + 4;
            Text name = Text.translatable(s.nameKey());
            g.drawText(textRenderer, name, tx, ry + 4, LABEL, true);
            g.drawText(textRenderer, effect(s), tx + textRenderer.getWidth(name) + 4, ry + 4, HINT, true);
        }
    }

    /** 木框面板（GuiKit.frame 画在 (x, y)，下面不打铆钉，同灶台；不画木缝——会穿过层名、原料表的字）。 */
    static void panel(DrawContext g, int x, int y, int w, int h) {
        g.getMatrices().push();
        g.getMatrices().translate(x, y, 0);
        frame(g, w, h, 0, false);
        g.getMatrices().pop();
    }

    @Override
    protected void drawForeground(DrawContext g, int mouseX, int mouseY) {
        g.drawText(textRenderer, title, titleX, titleY, TITLE, true);
        g.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, LABEL, true);
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
