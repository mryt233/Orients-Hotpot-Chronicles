// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 蘸料台的补货界面（r103）：原版灰色容器样式（用户要原版的样子，不要金边）。9 个原料格 = 台上 9 个小碗，
 * 2026-10-23 起排成 3×3、和台上一一对应（左边标上层 / 中层 / 下层），每格右边一根竖的剩余条；由总览界面的「补充蘸料台」按钮打开。
 * 2026-10-23 用户改：碗不固定，空碗放什么原料都行，所以空格子不再显示该放什么；
 * 右上角加「?」按钮，点开右边一张「原料表」：每种原料做成什么调料、有什么效果（开关状态这次游戏里一直记着）。
 * 2026-10-26 用户：原版浅灰底上的深灰字在界面尺寸 3 时显得糊，改成和灶台 / 火锅界面一样的木框（GuiKit），浅色字带阴影。
 * 网页预览 viewer/screens.js 的 sauceStation 与本类一一对应，改这里要同步改那边。
 * 作者：mryt233 */
public class SauceStationScreen extends AbstractContainerScreen<SauceStationScreenHandler> {
    private static final int W = 176;
    private static final int H = 178;
    private static final int MAX = SauceStationBlockEntity.MAX_RAW * SauceStationBlockEntity.PORTIONS_PER_RAW;
    /** 次要的字（层名、效果）：标签色压暗一点。 */
    static final int HINT = 0xFFA88C5C;
    /**
     * 原料表：离主界面 2 像素，标题下每种一行 18 像素（图标 16×16 清晰不重叠），原料图标最多两个并排。
     *
     * <p>mryt233 改（2026-11-06，r137）：调料从 10 种涨到 14 种，按单列 16 行高画在 178 高的面板里会超出底边
     * （17 + 14 × 16 = 241）。改成多列：每列最多 8 行（22 + 7 × 18 = 148，图标底边 164 &lt; 178），列数按调料种数反推；
     * 10 种以内仍按原来的单列画（22 + 10 × 18 = 202 会超出，所以 10 种以内用原来的行高 16 / 起始 17）。
     * </p>
     * mryt233 改（2026-11-07）：列数还受窗口宽度限制——窄窗口（界面缩放 4）里两列会画到屏幕右边界之外，
     * 放不下就退成一列，再放不下就把效果文字截断（见 {@link #drawGuide}）。
     */
    private static final int GAP = 2, ICONS_W = 34;
    /** 多列时每行高度、第一行的纵坐标（8 行：22 + 7 × 18 = 148，图标底边 164 &lt; 178）。 */
    private static final int GUIDE_COL2_ROW_Y = 22, GUIDE_COL2_ROW_H = 18;
    /** 单列时沿用原来的行距（起点 17、行高 16，10 种以内放得下）。 */
    private static final int GUIDE_COL1_ROW_Y = 17, GUIDE_COL1_ROW_H = 16;
    /** 两列之间的距离（第 2 列的横坐标偏移 = colW + GUIDE_COL_GAP）。 */
    private static final int GUIDE_COL_GAP = 10;
    /** 每列最多几行：第 8 行在 22 + 7 × 18 = 148，图标底边 164，面板高 178 放得下；第 9 行底边 182 就顶出面板了。 */
    private static final int MAX_ROWS_PER_COL = 8;
    /** 一列最窄多宽：原料图标 34 + 名字至少留 10 像素（再窄名字就截成看不出是哪种调料了，只能到这道线为止）。 */
    private static final int MIN_COL_W = 44;
    private static boolean guideOpen;
    private int guideW;
    /** 单列一行的宽度（图标 + 名字 + 效果，取所有调料里最宽的一行）。 */
    private int colW;
    /** 实际排版用的每列宽度：窗口窄到放不下自然宽度时比 {@link #colW} 小，名字 / 效果文字按它截断。 */
    private int guideColW;
    /** 分几列（14 种时 2 列）、每列几行。 */
    private int cols = 1;
    private int rowsPerCol = Seasoning.ALL.length;
    /** 原料表第一行的纵坐标与行高（分列方式不同，取值不同，见 {@link #init}）。 */
    private int guideRowY = GUIDE_COL1_ROW_Y;
    private int guideRowH = GUIDE_COL1_ROW_H;

    public SauceStationScreen(SauceStationScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, W, H);
        this.titleLabelX = 12;
        this.titleLabelY = 7;
        this.inventoryLabelY = SauceStationScreenHandler.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        // mryt233 改（2026-11-06，r137）：先算单列一行的宽度，再决定分几列（14 种调料 → 2 列 × 7 行）。
        colW = 0;
        for (Seasoning s : Seasoning.ALL) {
            colW = Math.max(colW, ICONS_W + 4 + font.width(Component.translatable(s.nameKey()))
                + 4 + font.width(effect(s)));
        }
        // 原料表要画 Seasoning.ALL.length 行，每列最多 MAX_ROWS_PER_COL 行；超过就加一列，别把行往下挤。
        // mryt233 改（2026-11-07）：以前写死「超过 10 种就 2 列」，调料加到 17 种时第二列会溢出面板底边；
        // 而且窄窗口（界面缩放 4）里两列会整张画到屏幕右边界之外，现在按窗口宽度收窄列宽、截断名字和效果文字。
        int maxW = guideMaxWidth();
        int len = Seasoning.ALL.length;
        // 列数只由「每列最多几行」决定：减列会把行数顶到面板底边外去（宁可横向截断，也不能纵向溢出）
        cols = Math.max(1, (len + MAX_ROWS_PER_COL - 1) / MAX_ROWS_PER_COL);
        rowsPerCol = (len + cols - 1) / cols;
        boolean multi = cols > 1 || len > 8;
        guideRowY = multi ? GUIDE_COL2_ROW_Y : GUIDE_COL1_ROW_Y;
        guideRowH = multi ? GUIDE_COL2_ROW_H : GUIDE_COL1_ROW_H;
        int titleW = 12 + font.width(guideTitle()) + 14;
        int natural = 14 + colW * cols + GUIDE_COL_GAP * (cols - 1);
        // 自然宽度超过窗口能给的就压到 maxW（窄窗口里表不画到屏幕右边界之外）；
        // 但面板不能比「每列最窄 MIN_COL_W」的内容还窄，否则文字会从面板右边溢出去
        int narrowest = 14 + MIN_COL_W * cols + GUIDE_COL_GAP * (cols - 1);
        guideW = Math.max(narrowest, Math.min(maxW, Math.max(natural, titleW)));
        int available = (guideW - 14 - GUIDE_COL_GAP * (cols - 1)) / cols;
        guideColW = Math.max(MIN_COL_W, Math.min(colW, available));
        if (guideOpen) {
            // 整块（主面板 + 表）居中；窗口放不下就一路往左推，宁可主面板从左边出去，也别让表画到屏幕外
            int centered = (width - W - GAP - guideW) / 2;
            leftPos = Math.min(centered, width - 2 - W - GAP - guideW);
        }
        addRenderableWidget(Button.builder(Component.literal("?"), b -> {
                guideOpen = !guideOpen;
                rebuildWidgets();
            })
            .bounds(leftPos + W - 26, topPos + 4, 12, 12)
            .tooltip(Tooltip.create(guideTitle()))
            .build());
    }

    private static Component guideTitle() {
        return Component.translatable(HotPotBlockEntity.key("screen.sauce.guide"));
    }

    private static Component effect(Seasoning s) {
        return Component.translatable(HotPotBlockEntity.key("seasoning." + s.id() + ".effect"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        int x = leftPos, y = topPos;
        panel(g, x, y, W, H);
        for (Slot slot : menu.slots) {
            slotFrame(g, x + slot.x, y + slot.y);
        }
        String[] tiers = {"screen.sauce.tier_top", "screen.sauce.tier_mid", "screen.sauce.tier_low"};
        for (int row = 0; row < 3; row++) {
            g.text(font, Component.translatable(HotPotBlockEntity.key(tiers[row])), x + 8,
                y + SauceStationScreenHandler.GRID_Y + row * SauceStationScreenHandler.STEP_Y + 4, HINT, true);
        }
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            int sx = x + SauceStationScreenHandler.slotX(i), sy = y + SauceStationScreenHandler.slotY(i);
            Seasoning s = menu.kind(i);
            // 格子里原料用完、碗里还剩份数：淡淡显示是哪种
            if (menu.getSlot(i).getItem().isEmpty() && s != null) {
                g.item(new ItemStack(s.rawShown(Util.getMillis())), sx, sy);
                g.fill(sx, sy, sx + 16, sy + 16, alpha(SLOT_BG, 0.7));
            }
            // 格子右边一根竖的剩余条（调料颜色，最多 64 份）
            int p = Math.min(MAX, menu.available(i));
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

    /** 原料表在窗口里最多能占多宽（主面板 176 + 间隔 2，两边各留 2 像素）。 */
    private int guideMaxWidth() {
        return Math.max(120, width - W - GAP - 4);
    }

    /**
     * 原料表：每种调料一行——原料图标（几种并排）、调料名、效果（灰字）。
     * mryt233 改（2026-11-06，r137）：分成左右几列，每列最多 {@link #rowsPerCol} 行，
     * 第 i 种调料在第 i / rowsPerCol 列、第 i % rowsPerCol 行。
     * mryt233 改（2026-11-07）：效果文字按这一列剩下的宽度截断，窄窗口里不会画到屏幕右边界之外。
     */
    private void drawGuide(GuiGraphicsExtractor g, int px, int py) {
        panel(g, px, py, guideW, H);
        g.text(font, guideTitle(), px + 12, py + 7, TITLE, true);
        for (int i = 0; i < Seasoning.ALL.length; i++) {
            Seasoning s = Seasoning.ALL[i];
            int col = i / rowsPerCol;
            int row = i % rowsPerCol;
            int rx = px + 7 + col * (guideColW + GUIDE_COL_GAP);
            int ry = py + guideRowY + row * guideRowH;
            List<Item> raws = s.raws();
            for (int k = 0; k < raws.size() && k < 2; k++) {
                g.item(new ItemStack(raws.get(k)), rx + k * 17, ry);
            }
            int tx = rx + ICONS_W + 4;
            // 名字在这一列里放不下就先截名字（窄窗口），别让名字把效果文字整个顶出列外
            String name = font.plainSubstrByWidth(Component.translatable(s.nameKey()).getString(),
                Math.max(8, rx + guideColW - 3 - tx));
            int nameW = font.width(name);
            g.text(font, name, tx, ry + 4, LABEL, true);
            // 效果文字的可用宽度 = 这一列右边界 - 现在的位置（两边各留 3 像素），放不下就截断
            int room = rx + guideColW - 3 - (tx + nameW + 4);
            if (room > 12) {
                String text = font.substrByWidth(effect(s), room).getString();
                if (!text.isEmpty()) {
                    g.text(font, text, tx + nameW + 4, ry + 4, HINT, true);
                }
            }
        }
    }

    /** 木框面板（GuiKit.frame 画在 (x, y)，下面不打铆钉，同灶台；不画木缝——会穿过层名、原料表的字）。 */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        frame(g, w, h, 0, false);
        g.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TITLE, true);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        // 指着空的原料格：这个碗装的什么（或者是空碗）、还剩几份
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            Slot slot = menu.getSlot(i);
            if (slot.getItem().isEmpty() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                Seasoning s = menu.kind(i);
                List<Component> lines = new ArrayList<>();
                if (s == null) {
                    lines.add(Component.translatable(HotPotBlockEntity.key("screen.sauce.empty")));
                } else {
                    lines.add(Component.translatable(s.nameKey()));
                    lines.add(Component.translatable(HotPotBlockEntity.key("screen.sauce.slot"), s.rawNames()).withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable(HotPotBlockEntity.key("screen.sauce.left"), menu.available(i), MAX).withStyle(ChatFormatting.GRAY));
                g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            }
        }
    }
}
