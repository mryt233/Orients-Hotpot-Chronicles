// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

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
public class StoveScreen extends AbstractContainerScreen<StoveScreenHandler> {
    // 灶膛砖面直接用原版砖块贴图，和灶台方块一致
    private static final Identifier STOVE_SIDE = Identifier.withDefaultNamespace("textures/block/bricks.png");
    /**
     * 灶台正面贴图（和方块用的同一张）：没烧 / 烧着（竖排动画）；方块正面看得见的是最下面 11 行。
     * mryt233 改（2026-11-05，移植同步清单 #104）：一律从方块图集取精灵，不再直接 blit 贴图文件。
     * 以前写死 16×256 / 16 帧，玩家整合包的资源包把这张图换成单帧 16×16 后，
     * 切出来的只剩第一行被拉长，灶膛窗口就是满框粉白竖条纹（用户 1.20.1 Forge 整合包截图）。
     * 图集精灵会自己按资源包的实际帧数播放，换帧数 / 换贴图都不会再错位。
     */
    private static final Identifier FRONT = HotPot.id("block/stove_front");
    private static final Identifier FRONT_LIT = HotPot.id("block/stove_front_lit");
    private static final int FRONT_ROWS = 11;

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

    public StoveScreen(StoveScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, Component.translatable(HotPotBlockEntity.key("screen.stove")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        context.pose().pushMatrix();
        context.pose().translate(this.leftPos, this.topPos);

        frame(context, W, H, 3, false);
        drawHearth(context);

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
        drawFlameGauge(context, fx + 1, fy + 22, menu.burnTicks() > 0 ? menu.burnRatio() : 0);
        inventoryFrames(context, INV_X, INV_Y, HOTBAR_Y);

        context.pose().popMatrix();
    }

    /**
     * 灶膛窗口：直接画灶台方块正面的贴图（2026-10-26 用户：界面里的灶台用灶台本身的贴图；以前手画的大拱门和方块对不上，
     * 照方块拱门手画的版本用户也说怪。调整）。方块正面看得见的是贴图第 5～16 行（炉口在下面），放大 3 倍、
     * 居中，窗口只框这一块；烧着用 stove_front_lit、没烧用 stove_front，帧数和节奏由图集按资源包里的实际内容播放。
     */
    private void drawHearth(GuiGraphicsExtractor c) {
        // 窗口就是灶台正面那一块（16×11 放大 3 倍），不再另外铺砖——砖纹和贴图里的砖对不上，拼在一起很怪（用户截图）
        // mryt233 改（2026-11-05，移植同步清单 #104）：用方块图集里的精灵，只取下面 11/16 那一截；动画由图集按实际帧数处理。
        boolean burning = menu.burnTicks() > 0;
        int s = 3, w = 16 * s, h = FRONT_ROWS * s;
        int x = HX + (HW - w) / 2, y = HY + (HH - h) / 2;
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
            .get(new SpriteId(Sheets.BLOCKS_MAPPER.sheet(), burning ? FRONT_LIT : FRONT));
        // 按精灵自己的 UV 只切下面 11/16（高清资源包的 32、64 像素贴图也按比例切）。
        // 不用 blit(..., sprite, 四个小数) ——那是染色的重载，不是 UV；这里用图的归一化 UV 那一版。
        c.blit(sprite.atlasLocation(), x, y, x + w, y + h,
            sprite.getU(0F), sprite.getU(1F), sprite.getV((16 - FRONT_ROWS) / 16F), sprite.getV(1F));
        c.fill(x - 1, y - 1, x + w + 1, y, RIM_LINE);
        c.fill(x - 1, y + h, x + w + 1, y + h + 1, RIM_LINE);
        c.fill(x - 1, y, x, y + h, RIM_LINE);
        c.fill(x + w, y, x + w + 1, y + h, RIM_LINE);
    }

    /** 原版熔炉式火焰：燃料越少，从上往下熄掉越多。 */
    private static void drawFlameGauge(GuiGraphicsExtractor c, int x, int y, float ratio) {
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
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, 12, 7, TITLE, true);
        context.fill(12, 17, W - 12, 18, BRASS_DARK);

        context.text(this.font, Component.translatable(HotPotBlockEntity.key("screen.inventory")),
            INV_X, INV_Y - 11, LABEL, true);
    }

    /** 原版 AbstractContainerScreen.render 不画悬停提示，要子类自己调（漏了这步，格子里的东西指上去看不到名字和属性；2026-10-23 用户报，补充）。 */
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    /** 空燃料槽上悬停：说明能放什么（原先常驻的提示行挪到这里，给背包标签腾位置）。 */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Slot focused = this.hoveredSlot;
        if (focused == null || focused.index != 0 || focused.hasItem() || !menu.getCarried().isEmpty()) {
            super.extractTooltip(context, mouseX, mouseY);
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(HotPotBlockEntity.key("screen.stove.fuel")).withStyle(ChatFormatting.GOLD).getVisualOrderText());
        lines.addAll(this.font.split(
            Component.translatable(HotPotBlockEntity.key("screen.stove.hint")).withStyle(ChatFormatting.GRAY), 140));
        if (menu.burnTicks() <= 0 && !menu.hasFuelInSlot()) {
            lines.addAll(this.font.split(
                Component.translatable(HotPotBlockEntity.key("screen.stove.waiting")).withStyle(ChatFormatting.DARK_GRAY), 140));
        }
        context.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }
}
