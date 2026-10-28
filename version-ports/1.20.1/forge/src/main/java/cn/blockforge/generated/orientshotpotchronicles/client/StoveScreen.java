package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 灶膛界面：像熔炉一样只有一个燃料槽，左边是砌砖灶膛。不显示温度和剩余秒数，火的状态全靠画面：
 * 在烧是火舌，有燃料没点火是一点明灭的余烬，什么都没有就只剩炉灰。
 * 火苗是几簇各自跳动的火舌（多层正弦叠加，非线性闪烁），火舌逐行收窄、尖端摆动；
 * 炭床明暗起伏，火星缓出上飘渐隐。燃料槽下方的火焰图标像原版熔炉一样随燃料烧尽。
 * 只负责显示，燃料能否放入、烧多久都由 {@link StoveScreenHandler} 决定。
 * 网页预览 viewer/screens.js 的 stove 与本类一一对应，改这里要同步改那边。
 */
public class StoveScreen extends HandledScreen<StoveScreenHandler> {
    // 灶膛砖面直接用原版砖块贴图，和灶台方块一致
    private static final Identifier STOVE_SIDE = new Identifier("textures/block/bricks.png");
    /** 灶台正面贴图（和方块用的同一张）：没烧 / 烧着（竖排 16 帧动画）；方块正面看得见的是最下面 11 行。 */
    private static final Identifier FRONT = GeneratedMod.id("textures/block/stove_front.png");
    private static final Identifier FRONT_LIT = GeneratedMod.id("textures/block/stove_front_lit.png");
    private static final int FRONT_FRAMES = 16, FRONT_ROWS = 11;

    private static final int W = 176;
    private static final int H = 166;
    // 灶膛（相对面板）
    // 灶膛 + 箭头 + 燃料槽作为一组，在面板里水平居中
    private static final int HX = 44;
    private static final int HY = 23;
    private static final int HW = 52;
    private static final int HH = 46;
    private static final int INV_X = 8;
    private static final int INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    /** 14x14 火焰图标：'#' 外焰，'o' 内焰。 */
    private static final String[] FLAME = {
        "      #       ",
        "      ##      ",
        "     ###      ",
        "     ###  #   ",
        "  #  ####  #  ",
        "  # #####  ## ",
        " ## ####o# ## ",
        " ######oo#### ",
        " #####ooo#### ",
        "######oooo####",
        "#####oooooo###",
        "####oooooooo##",
        " ###oooooooo# ",
        "  ##########  ",
    };

    /** 三层火舌：高度倍率、最大半宽、颜色（外焰 → 焰心）。 */

    public StoveScreen(StoveScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.translatable(HotPotBlockEntity.key("screen.stove")));
        this.backgroundWidth = W;
        this.backgroundHeight = H;
    }

    @Override
    public void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        double t = Util.getMeasuringTimeMs() / 1000.0;
        context.getMatrices().push();
        context.getMatrices().translate(this.x, this.y, 0);

        frame(context, W, H, 3, false);
        drawHearth(context, t);

        int fx = StoveScreenHandler.FUEL_X;
        int fy = StoveScreenHandler.FUEL_Y;
        context.fill(fx - 4, fy - 4, fx + 20, fy + 20, RIM_LINE);
        context.fill(fx - 3, fy - 3, fx + 19, fy + 19, BRASS_DARK);
        context.fill(fx - 2, fy - 2, fx + 18, fy + 18, BRASS);
        context.fill(fx - 2, fy - 2, fx + 18, fy - 1, BRASS_HI);
        slotFrame(context, fx, fy);
        // 燃料槽 → 灶膛的黄铜箭头（朝左）
        int ax = HX + HW + 3;
        int ay = 39;
        context.fill(ax + 5, ay + 1, ax + 8, ay + 8, BRASS);
        context.fill(ax + 3, ay + 3, ax + 8, ay + 6, BRASS_HI);
        context.fill(ax + 1, ay + 4, ax + 8, ay + 5, BRASS_HI);
        drawFlameGauge(context, fx + 1, fy + 22, handler.burnTicks() > 0 ? handler.burnRatio() : 0);
        inventoryFrames(context, INV_X, INV_Y, HOTBAR_Y);

        context.getMatrices().pop();
    }

    /**
     * 灶膛窗口：直接画灶台方块正面的贴图（2026-10-26 用户：界面里的灶台用灶台本身的贴图；以前手画的大拱门和方块对不上，
     * 照方块拱门手画的版本用户也说怪。）。方块正面看得见的是贴图第 5～16 行（炉口在下面），放大 3 倍、
     * 居中，窗口只框这一块；烧着用 stove_front_lit（16 帧，每帧 2 刻，和方块一样跳），没烧用 stove_front。
     */
    private void drawHearth(DrawContext c, double t) {
        // 窗口就是灶台正面那一块（16×11 放大 3 倍），不再另外铺砖——砖纹和贴图里的砖对不上，拼在一起很怪（用户截图）
        boolean burning = handler.burnTicks() > 0;
        int s = 3, w = 16 * s, h = FRONT_ROWS * s;
        int x = HX + (HW - w) / 2, y = HY + (HH - h) / 2;
        int frame = burning ? (int) ((Util.getMeasuringTimeMs() / 100) % FRONT_FRAMES) : 0;
        c.drawTexture(burning ? FRONT_LIT : FRONT, x, y, w, h, 0, frame * 16 + (16 - FRONT_ROWS), 16, FRONT_ROWS,
            16, burning ? 16 * FRONT_FRAMES : 16);
        c.fill(x - 1, y - 1, x + w + 1, y, RIM_LINE);
        c.fill(x - 1, y + h, x + w + 1, y + h + 1, RIM_LINE);
        c.fill(x - 1, y, x, y + h, RIM_LINE);
        c.fill(x + w, y, x + w + 1, y + h, RIM_LINE);
    }

    /** 原版熔炉式火焰：燃料越少，从上往下熄掉越多。 */
    private static void drawFlameGauge(DrawContext c, int x, int y, float ratio) {
        int lit = Math.round(14 * ratio);
        for (int ry = 0; ry < 14; ry++) {
            String row = FLAME[ry];
            for (int rx = 0; rx < 14; rx++) {
                char ch = row.charAt(rx);
                if (ch == ' ') {
                    continue;
                }
                boolean on = ry >= 14 - lit;
                c.fill(x + rx, y + ry, x + rx + 1, y + ry + 1,
                    on ? (ch == 'o' ? 0xFFFBE08A : 0xFFE2582B) : 0xFF24160A);
            }
        }
    }

    @Override
    public void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title, 12, 7, TITLE, true);
        context.fill(12, 17, W - 12, 18, BRASS_DARK);

        context.drawText(textRenderer, Text.translatable(HotPotBlockEntity.key("screen.inventory")),
            INV_X, INV_Y - 11, LABEL, true);
    }

    /** 原版 HandledScreen.render 不画悬停提示，要子类自己调（漏了这步，格子里的东西指上去看不到名字和属性；2026-10-23 用户报， 补）。 */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    /** 空燃料槽上悬停：说明能放什么（原先常驻的提示行挪到这里，给背包标签腾位置）。 */
    @Override
    public void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        Slot focused = this.focusedSlot;
        if (focused == null || focused.id != 0 || focused.hasStack() || !handler.getCursorStack().isEmpty()) {
            super.drawMouseoverTooltip(context, mouseX, mouseY);
            return;
        }
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.translatable(HotPotBlockEntity.key("screen.stove.fuel")).formatted(Formatting.GOLD).asOrderedText());
        lines.addAll(textRenderer.wrapLines(
            Text.translatable(HotPotBlockEntity.key("screen.stove.hint")).formatted(Formatting.GRAY), 140));
        if (handler.burnTicks() <= 0 && !handler.hasFuelInSlot()) {
            lines.addAll(textRenderer.wrapLines(
                Text.translatable(HotPotBlockEntity.key("screen.stove.waiting")).formatted(Formatting.DARK_GRAY), 140));
        }
        context.drawOrderedTooltip(textRenderer, lines, mouseX, mouseY);
    }
}
