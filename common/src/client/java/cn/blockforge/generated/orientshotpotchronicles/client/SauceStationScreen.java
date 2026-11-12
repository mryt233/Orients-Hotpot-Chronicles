package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.FormattedCharSequence;

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
    // mryt233 改（r146f）：固定单列，按完整文字的换行高度分页；页码在本次运行中保留。
    private static final int GAP = 2, GUIDE_W = 176, ICONS_W = 34;
    private static final int GUIDE_TOP = 22, GUIDE_BOTTOM = H - 24;
    private static final int TEXT_W = GUIDE_W - 14 - ICONS_W - 4;
    private static boolean guideOpen;
    private static int guidePage;
    private final List<List<GuideRow>> guidePages = new ArrayList<>();
    private Button previousPage, nextPage;

    private record GuideRow(SeasoningDef seasoning, List<FormattedCharSequence> names,
                            List<FormattedCharSequence> effects, boolean inline, int height) { }

    public SauceStationScreen(SauceStationScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = 12;
        this.titleLabelY = 7;
        this.inventoryLabelY = SauceStationScreenHandler.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        buildGuidePages();
        guidePage = Math.max(0, Math.min(guidePage, guidePages.size() - 1));
        if (guideOpen) leftPos = (width - W - GAP - GUIDE_W) / 2;
        addRenderableWidget(Button.builder(Component.literal("?"), b -> {
                guideOpen = !guideOpen;
                rebuildWidgets();
            })
            .bounds(leftPos + W - 26, topPos + 4, 12, 12)
            .tooltip(Tooltip.create(guideTitle()))
            .build());
        if (guideOpen) {
            int px = leftPos + W + GAP, by = topPos + H - 21;
            previousPage = addRenderableWidget(Button.builder(Component.literal("<"), b -> changeGuidePage(-1))
                .bounds(px + 7, by, 16, 14)
                .tooltip(Tooltip.create(Component.translatable(HotPotBlockEntity.key("screen.sauce.previous_page"))))
                .build());
            nextPage = addRenderableWidget(Button.builder(Component.literal(">"), b -> changeGuidePage(1))
                .bounds(px + GUIDE_W - 23, by, 16, 14)
                .tooltip(Tooltip.create(Component.translatable(HotPotBlockEntity.key("screen.sauce.next_page"))))
                .build());
            updatePageButtons();
        }

    }

    private static Component guideTitle() {
        return Component.translatable(HotPotBlockEntity.key("screen.sauce.guide"));
    }

    private static Component effect(SeasoningDef s) {
        return Component.translatable(HotPotBlockEntity.key("seasoning." + s.id() + ".effect"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(g, x, y, W, H);
        for (Slot slot : menu.slots) {
            slotFrame(g, x + slot.x, y + slot.y);
        }
        String[] tiers = {"screen.sauce.tier_top", "screen.sauce.tier_mid", "screen.sauce.tier_low"};
        for (int row = 0; row < 3; row++) {
            g.drawString(font, Component.translatable(HotPotBlockEntity.key(tiers[row])), x + 8,
                y + SauceStationScreenHandler.GRID_Y + row * SauceStationScreenHandler.STEP_Y + 4, HINT, true);
        }
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            int sx = x + SauceStationScreenHandler.slotX(i), sy = y + SauceStationScreenHandler.slotY(i);
            SeasoningDef s = menu.kindDef(i);
            // 格子里原料用完、碗里还剩份数：淡淡显示是哪种
            if (menu.getSlot(i).getItem().isEmpty() && s != null) {
                g.renderItem(new ItemStack(s.rawShown(Util.getMillis())), sx, sy);
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

    private void buildGuidePages() {
        guidePages.clear();
        List<GuideRow> page = new ArrayList<>();
        guidePages.add(page);
        int used = 0;
        for (SeasoningDef seasoning : Seasonings.all()) {
            Component name = Component.translatable(seasoning.nameKey());
            Component description = effect(seasoning);
            boolean inline = font.width(name) + 4 + font.width(description) <= TEXT_W;
            List<FormattedCharSequence> names = font.split(name, TEXT_W);
            List<FormattedCharSequence> effects = font.split(description, TEXT_W);
            int lines = inline ? 1 : names.size() + effects.size();
            int rowHeight = Math.max(18, lines * font.lineHeight + 4);
            if (!page.isEmpty() && used + rowHeight > GUIDE_BOTTOM - GUIDE_TOP) {
                page = new ArrayList<>();
                guidePages.add(page);
                used = 0;
            }
            page.add(new GuideRow(seasoning, names, effects, inline, rowHeight));
            used += rowHeight;
        }
    }

    private void changeGuidePage(int amount) {
        guidePage = Math.max(0, Math.min(guidePages.size() - 1, guidePage + amount));
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPage != null) previousPage.active = guidePage > 0;
        if (nextPage != null) nextPage.active = guidePage < guidePages.size() - 1;
    }

    /** mryt233 改（r146f）：兼容插件只读取原版矩形；关闭后立即撤销占用，不依赖第三方类型。 */
    public List<Rect2i> guideAreas() {
        return guideOpen ? List.of(new Rect2i(leftPos + W + GAP, topPos, GUIDE_W, H)) : List.of();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (guideOpen && scrollY != 0 && mouseX >= leftPos + W + GAP
            && mouseX < leftPos + W + GAP + GUIDE_W && mouseY >= topPos && mouseY < topPos + H) {
            changeGuidePage(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 单列保留所有原料图标、名字和效果，换行后按实际行高排列。 */
    private void drawGuide(GuiGraphics g, int px, int py) {
        panel(g, px, py, GUIDE_W, H);
        g.drawString(font, guideTitle(), px + 12, py + 7, TITLE, true);
        int ry = py + GUIDE_TOP;
        for (GuideRow row : guidePages.get(guidePage)) {
            List<Item> raws = row.seasoning().raws();
            for (int k = 0; k < raws.size() && k < 2; k++) {
                g.renderItem(new ItemStack(raws.get(k)), px + 7 + k * 17, ry);
            }
            int tx = px + 7 + ICONS_W + 4, ty = ry + 2;
            for (FormattedCharSequence name : row.names()) {
                g.drawString(font, name, tx, ty, LABEL, true);
                ty += font.lineHeight;
            }
            if (row.inline()) {
                g.drawString(font, row.effects().getFirst(),
                    tx + font.width(row.names().getFirst()) + 4, ry + 2, HINT, true);
            } else {
                for (FormattedCharSequence description : row.effects()) {
                    g.drawString(font, description, tx, ty, HINT, true);
                    ty += font.lineHeight;
                }
            }
            ry += row.height();
        }
        Component page = Component.translatable(HotPotBlockEntity.key("screen.sauce.page"), guidePage + 1, guidePages.size());
        g.drawString(font, page, px + (GUIDE_W - font.width(page)) / 2, py + H - 18, LABEL, true);
    }

    /** 木框面板（GuiKit.frame 画在 (x, y)，下面不打铆钉，同灶台；不画木缝——会穿过层名、原料表的字）。 */
    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        frame(g, w, h, 0, false);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TITLE, true);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
        // 指着空的原料格：这个碗装的什么（或者是空碗）、还剩几份
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            Slot slot = menu.getSlot(i);
            if (slot.getItem().isEmpty() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                SeasoningDef s = menu.kindDef(i);
                List<Component> lines = new ArrayList<>();
                if (s == null) {
                    lines.add(Component.translatable(HotPotBlockEntity.key(menu.unknownKind(i) ? "sauce.unknown" : "screen.sauce.empty")));
                } else {
                    lines.add(Component.translatable(s.nameKey()));
                    lines.add(Component.translatable(HotPotBlockEntity.key("screen.sauce.slot"), s.rawNames()).withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable(HotPotBlockEntity.key("screen.sauce.left"), menu.available(i), MAX).withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }
}
