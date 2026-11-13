// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.client;

import net.minecraft.util.Util;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuidePager;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideRecipes;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideSearch;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideSearch.SearchDocument;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideSearch.SearchHit;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideLayout;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideLayout.Elem;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent.Entry;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent.Category;
import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 「火锅指南」界面。
 * 内容来自 assets/orients_hotpot_chronicles/guide/&lt;语言&gt;.json；
 * 配方与排版职责拆入 {@code client.guide.*}，26.2 配方来源走 {@code RecipeSync}。作者：mryt233
 */
public class GuideBookScreen extends Screen {
    private static final Identifier TEX = HotPot.id("textures/gui/guide_book.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int BOOK_W = 272, BOOK_H = 180;
    private static final int LEFT_X = 16, RIGHT_X = 144, COL_W = 112, COL_Y = 12;
    private static final int TEXT = 0xFF2A1A0E, HEADING = 0xFF5A2A10, MUTED = 0xFF7A6040, TIP = 0xFF2E5E1E,
        WARN = 0xFF8A3A12, NUMBER = 0xFF1E6E78, RULE = 0x40603820, CODE_BG = 0x18603820;
    private static final int CAT_W = 28, TAB_H = 24, ENTRY_W = 24;
    private static final int ENTRY_STEP = 26;
    private static final int SEARCH_W = 236, SEARCH_H = 148, SEARCH_ROW_H = 25, SEARCH_LIST_H = 100;
    private static final int ICON_STEP = 18, ICON_PER_ROW = 6;

    private List<Category> cats = List.of();
    private List<List<Elem>> columns = List.of();
    private final GuidePager pager = new GuidePager();
    private final GuideRecipes recipes = new GuideRecipes();
    private int bx, by;
    private boolean opened;
    private final List<int[]> hoverRects = new ArrayList<>();
    private final List<ItemStack> hoverStacks = new ArrayList<>();
    private final List<int[]> linkRects = new ArrayList<>();
    private final List<int[]> linkTargets = new ArrayList<>();
    private boolean searchOpen, skipSearchSlash;
    private String searchQuery = "";
    private EditBox searchBox;
    private List<SearchDocument> searchIndex = List.of();
    private List<SearchHit> searchHits = List.of();
    private int searchScroll;

    public GuideBookScreen() {
        super(Component.translatable("item." + HotPot.MOD_ID + ".hotpot_guide"));
    }

    @Override
    protected void init() {
        cats = GuideContent.build();
        bx = (width - BOOK_W) / 2 + 12;
        by = Math.max(24, (height - BOOK_H) / 2 + 8);
        pager.init(cats, font, bx, by);
        columns = pager.columns;
        searchIndex = GuideSearch.build(cats);
        searchBox = new EditBox(font, searchX() + 29, searchY() + 11, SEARCH_W - 54, 12,
            Component.literal(ui("搜索", "搜尋", "Search")));
        searchBox.setBordered(false);
        searchBox.setTextColor(TEXT);
        searchBox.setMaxLength(256);
        searchBox.setHint(Component.literal(ui("搜索：物品、玩法、配方……", "搜尋：物品、玩法、配方……", "Search: items, gameplay, recipes…"))
            .withStyle(ChatFormatting.GRAY));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query;
            runSearch();
        });
        searchBox.setFocused(searchOpen);
        runSearch();
        if (!opened) {
            opened = true;
            sound(SoundEvents.BOOK_PAGE_TURN, 0.8F);
        }
    }

    @Override
    public void removed() {
        sound(SoundEvents.BOOK_PUT, 1.0F);
        super.removed();
    }

    private void sound(SoundEvent event, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
        }
    }

    private void open(int c, int e) { pager.open(c, e); columns = pager.columns; }
    private void next() { pager.next(); columns = pager.columns; }
    private void prev() { pager.prev(); columns = pager.columns; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hoverRects.clear();
        hoverStacks.clear();
        linkRects.clear();
        linkTargets.clear();
        pager.tick(Util.getMillis());
        int hoverCat = -1, hoverEntry = -1;
        for (int i = 0; i < cats.size(); i++) {
            int ty = pager.catTabY(i);
            boolean hover = !searchOpen && hit(mouseX, mouseY, bx - CAT_W, ty, CAT_W, pager.catTabH());
            if (hover) {
                hoverCat = i;
            }
            boolean sel = i == pager.cat;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, bx - 24 - (sel || hover ? 4 : 0), ty, 280, sel ? 24 : 0, CAT_W, TAB_H, TEX_W, TEX_H);
        }
        List<Entry> entries = cats.isEmpty() ? List.of() : cats.get(pager.cat).entries();
        g.enableScissor(pager.stripLeft(), by - 26, pager.stripRight(), by);
        for (int i = 0; i < entries.size(); i++) {
            int tx = pager.entryTabX(i);
            boolean hover = !searchOpen && pager.inEntryStrip(mouseX, mouseY)
                && hit(mouseX, mouseY, tx, by - 23, ENTRY_W, 23);
            if (hover) {
                hoverEntry = i;
            }
            boolean sel = i == pager.entry;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, tx, by - 20 - (sel || hover ? 3 : 0), 312, sel ? 24 : 0, ENTRY_W, TAB_H, TEX_W, TEX_H);
        }
        g.disableScissor();
        boolean hoverSearch = hit(mouseX, mouseY, pager.searchTabX(), by - 23, ENTRY_W, 23);
        g.enableScissor(pager.searchTabX(), by - 26, pager.searchTabX() + ENTRY_W, by);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, pager.searchTabX(), by - 20 - (searchOpen || hoverSearch ? 3 : 0),
            312, searchOpen ? 24 : 0, ENTRY_W, TAB_H, TEX_W, TEX_H);
        g.disableScissor();
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, bx, by, 0, 0, BOOK_W + 3, BOOK_H + 3, TEX_W, TEX_H);

        for (int i = 0; i < cats.size(); i++) {
            boolean out = i == pager.cat || i == hoverCat;
            g.item(cats.get(i).icon(), bx - 19 - (out ? 4 : 0), pager.catTabY(i) + 4);
        }
        g.enableScissor(pager.stripLeft(), by - 26, pager.stripRight(), by);
        for (int i = 0; i < entries.size(); i++) {
            boolean out = i == pager.entry || i == hoverEntry;
            g.item(entries.get(i).icon(), pager.entryTabX(i) + 4, by - 18 - (out ? 3 : 0));
        }
        g.disableScissor();
        g.enableScissor(pager.searchTabX(), by - 26, pager.searchTabX() + ENTRY_W, by);
        g.item(new ItemStack(Items.COMPASS), pager.searchTabX() + 4, by - 18 - (searchOpen || hoverSearch ? 3 : 0));
        g.disableScissor();
        if (pager.maxEntryScroll() > 0) {
            g.text(font, "<", bx + 6, by - 16, pager.targetEntryScroll > 0 ? HEADING : MUTED, false);
            g.text(font, ">", pager.stripRight() + 1, by - 16,
                pager.targetEntryScroll < pager.maxEntryScroll() ? HEADING : MUTED, false);
        }
        int c0 = pager.spread * 2;
        for (int k = 0; k < 2 && c0 + k < columns.size(); k++) {
            drawColumn(g, columns.get(c0 + k), bx + (k == 0 ? LEFT_X : RIGHT_X), by + COL_Y);
        }
        if (!pager.atStart()) {
            boolean h = hit(mouseX, mouseY, bx + LEFT_X - 2, by + 158, 13, 11);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, bx + LEFT_X, by + 159, h ? 370 : 360, 0, 9, 9, TEX_W, TEX_H);
        }
        if (!pager.atEnd()) {
            boolean h = hit(mouseX, mouseY, bx + BOOK_W - LEFT_X - 11, by + 158, 13, 11);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, bx + BOOK_W - LEFT_X - 9, by + 159, h ? 370 : 360, 9, 9, 9, TEX_W, TEX_H);
        }
        if (pager.spreads() > 1) {
            String p = (pager.spread + 1) + " / " + pager.spreads();
            g.text(font, p, bx + RIGHT_X + COL_W / 2 - font.width(p) / 2, by + 160, MUTED, false);
        }
        if (searchOpen) {
            renderSearchOverlay(g, mouseX, mouseY, delta);
        }
        if (hoverSearch) {
            g.setTooltipForNextFrame(font, Component.literal(ui("搜索", "搜尋", "Search")), mouseX, mouseY);
        } else if (searchOpen) {
            return;
        } else if (hoverCat >= 0) {
            g.setTooltipForNextFrame(font, cats.get(hoverCat).title(), mouseX, mouseY);
        } else if (hoverEntry >= 0) {
            g.setTooltipForNextFrame(font, entries.get(hoverEntry).title(), mouseX, mouseY);
        } else {
            for (int i = 0; i < hoverRects.size(); i++) {
                int[] r = hoverRects.get(i);
                if (hit(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    g.setTooltipForNextFrame(font, hoverStacks.get(i), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void drawColumn(GuiGraphicsExtractor g, List<Elem> col, int x, int y) {
        for (Elem el : col) {
            switch (el.kind) {
                case HEADER -> {
                    g.item(el.icon, x, y);
                    track(x, y, 16, 16, el.icon);
                    int ty = y + (el.lines.size() == 1 ? 4 : 1);
                    for (var t : el.lines) {
                        g.text(font, t, x + 20, ty, HEADING, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 4, x + COL_W, y + el.height - 3, RULE);
                }
                case TEXT -> g.text(font, el.lines.get(0), x, y, el.color, false);
                case SUB -> {
                    int ty = y + 1;
                    for (var t : el.lines) {
                        g.text(font, t, x, ty, el.color, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 3, x + COL_W, y + el.height - 2, RULE);
                }
                case NOTE -> {
                    g.fill(x, y, x + 2, y + el.height - 2, el.color);
                    int ty = y + 1;
                    for (var t : el.lines) {
                        g.text(font, t, x + 6, ty, el.color, false);
                        ty += 9;
                    }
                }
                case TH -> {
                    int ty = y;
                    for (var t : el.lines) {
                        g.text(font, t, x, ty, el.color, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 3, x + COL_W, y + el.height - 2, RULE);
                }
                case ROW -> drawRow(g, el, x, y);
                case BUL, NUM -> {
                    int off = el.indent * 4;
                    g.text(font, el.number, x + off + 1, y, numberColor(el), false);
                    int textX = el.kind == GuideLayout.Kind.NUM ? 11 : 8;
                    if (!el.icon.isEmpty()) {
                        smallIcon(g, el.icon, x + off + (el.kind == GuideLayout.Kind.NUM ? 7 : 4), y);
                        textX = el.kind == GuideLayout.Kind.NUM ? 22 : 15;
                    }
                    int ty = y;
                    for (var t : el.lines) {
                        g.text(font, t, x + off + textX, ty, el.color, false);
                        ty += 9;
                    }
                }
                case ICONS -> {
                    for (int i = 0; i < el.icons.size(); i++) {
                        int ix = x + (i % ICON_PER_ROW) * ICON_STEP;
                        int iy = y + 1 + (i / ICON_PER_ROW) * ICON_STEP;
                        g.item(el.icons.get(i), ix, iy);
                        track(ix, iy, 16, 16, el.icons.get(i));
                    }
                }
                case RECIPE -> recipes.draw(g, font, minecraft, x, y, el.recipes, this::track);
                case CODE -> {
                    g.fill(x - 1, y, x + COL_W, y + el.height - 3, CODE_BG);
                    int ty = y + 2;
                    for (var t : el.lines) {
                        g.text(font, t, x + 2, ty, el.color, false);
                        ty += 9;
                    }
                }
                case RULE -> g.fill(x, y + 3, x + COL_W, y + 4, RULE);
                default -> {
                }
            }
            y += el.height;
        }
    }

    private static int numberColor(Elem el) {
        return el.kind == GuideLayout.Kind.NUM ? NUMBER : MUTED;
    }

    private void smallIcon(GuiGraphicsExtractor g, ItemStack st, int x, int y) {
        if (st.isEmpty()) {
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75F, 0.75F);
        g.item(st, 0, 0);
        g.pose().popMatrix();
        track(x, y, 12, 12, st);
    }

    private void drawRow(GuiGraphicsExtractor g, Elem el, int x, int y) {
        int tx = x + 15 + el.indent * 4;
        smallIcon(g, el.icon, x, y + 1);
        int ty = y + (el.lines.size() == 1 ? 3 : 2);
        for (var t : el.lines) {
            g.text(font, t, tx, ty, el.color, false);
            ty += 9;
        }
        if (el.goCat >= 0) {
            linkRects.add(new int[]{x, y, COL_W, el.height});
            linkTargets.add(new int[]{el.goCat, el.goEntry});
        }
    }

    private void track(int x, int y, int w, int h, ItemStack s) {
        if (!s.isEmpty()) {
            hoverRects.add(new int[]{x, y, w, h});
            hoverStacks.add(s);
        }
    }

    private String ui(String simplified, String traditional, String english) {
        return GuideContent.ui(simplified, traditional, english);
    }

    private int searchX() {
        return bx + (BOOK_W - SEARCH_W) / 2;
    }

    private int searchY() {
        return by + 16;
    }

    private void toggleSearch() {
        skipSearchSlash = false;
        searchOpen = !searchOpen;
        searchBox.setFocused(searchOpen);
        if (searchOpen) {
            runSearch();
        }
        click();
    }

    private void runSearch() {
        searchScroll = 0;
        searchHits = GuideSearch.run(searchIndex, searchQuery);
    }

    private void jumpToSearchHit(SearchHit hit) {
        open(hit.cat(), hit.entry());
        pager.spread = GuideSearch.locate(columns, searchQuery, pager.spread);
        searchOpen = false;
        searchBox.setFocused(false);
        click();
    }

    private String fitSearchText(String text, int maxWidth) {
        return font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
    }

    private void drawSearchSnippet(GuiGraphicsExtractor g, String snippet, int x, int y, int maxWidth) {
        String shown = fitSearchText(snippet, maxWidth);
        String query = searchQuery.strip().toLowerCase(Locale.ROOT);
        int at = shown.toLowerCase(Locale.ROOT).indexOf(query);
        if (at < 0 || query.isEmpty()) {
            g.text(font, shown, x, y, MUTED, false);
            return;
        }
        String before = shown.substring(0, at), match = shown.substring(at, Math.min(shown.length(), at + query.length()));
        int hx = x + font.width(before);
        g.fill(hx, y - 1, hx + font.width(match), y + 9, 0x50DFC9A3);
        g.text(font, before, x, y, MUTED, false);
        g.text(font, match, hx, y, HEADING, false);
        g.text(font, shown.substring(at + match.length()), hx + font.width(match), y, MUTED, false);
    }

    private void renderSearchOverlay(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = searchX(), y = searchY();
        g.pose().pushMatrix();
        g.pose().translate(0, 0);
        g.fill(x + 3, y + 3, x + SEARCH_W + 3, y + SEARCH_H + 3, 0x602A1A0E);
        g.fill(x, y, x + SEARCH_W, y + SEARCH_H, TEXT);
        g.fill(x + 1, y + 1, x + SEARCH_W - 1, y + SEARCH_H - 1, 0xFFF5ECD8);
        g.outline(x + 3, y + 3, SEARCH_W - 6, SEARCH_H - 6, 0xFFDFC9A3);
        g.item(new ItemStack(Items.COMPASS), x + 7, y + 7);
        g.fill(x + 6, y + 26, x + SEARCH_W - 6, y + 27, 0xFFDFC9A3);
        boolean closeHover = hit(mx, my, x + SEARCH_W - 20, y + 5, 16, 18);
        g.text(font, "×", x + SEARCH_W - 16, y + 10, closeHover ? WARN : HEADING, false);
        searchBox.extractWidgetRenderState(g, mx, my, delta);
        g.enableScissor(x + 5, y + 31, x + SEARCH_W - 5, y + 31 + SEARCH_LIST_H);
        for (int i = 0; i < searchHits.size(); i++) {
            int ry = y + 31 + i * SEARCH_ROW_H - searchScroll;
            if (ry + SEARCH_ROW_H <= y + 31 || ry >= y + 31 + SEARCH_LIST_H) {
                continue;
            }
            SearchHit hit = searchHits.get(i);
            if (hit(mx, my, x + 5, ry, SEARCH_W - 10, SEARCH_ROW_H)) {
                g.fill(x + 5, ry, x + SEARCH_W - 5, ry + SEARCH_ROW_H, 0x40DFC9A3);
            }
            Entry item = cats.get(hit.cat()).entries().get(hit.entry());
            g.item(item.icon(), x + 7, ry + 4);
            String title = cats.get(hit.cat()).title().getString() + " · " + item.title().getString();
            g.text(font, fitSearchText(title, SEARCH_W - 38), x + 28, ry + 2, HEADING, false);
            drawSearchSnippet(g, hit.snippet(), x + 28, ry + 13, SEARCH_W - 38);
        }
        if (searchHits.isEmpty()) {
            String hint = searchQuery.isBlank()
                ? ui("输入关键词查找物品、玩法或配方", "輸入關鍵字查找物品、玩法或配方", "Type to find items, gameplay or recipes")
                : ui("没有找到相关条目", "沒有找到相關條目", "No matching entries");
            g.text(font, fitSearchText(hint, SEARCH_W - 16), x + 8, y + 36, MUTED, false);
        }
        g.disableScissor();
        int maxScroll = Math.max(0, searchHits.size() * SEARCH_ROW_H - SEARCH_LIST_H);
        if (maxScroll > 0) {
            int thumb = Math.max(8, SEARCH_LIST_H * SEARCH_LIST_H / (searchHits.size() * SEARCH_ROW_H));
            int top = y + 31 + searchScroll * (SEARCH_LIST_H - thumb) / maxScroll;
            g.fill(x + SEARCH_W - 5, top, x + SEARCH_W - 4, top + thumb, MUTED);
        }
        g.text(font, searchHits.size() + ui(" 条结果 · 回车打开首项", " 條結果 · Enter 開啟首項", " results · Enter opens first"),
            x + 8, y + SEARCH_H - 13, MUTED, false);
        g.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == 0 && hit(mouseX, mouseY, pager.searchTabX(), by - 23, ENTRY_W, 23)) {
            toggleSearch();
            return true;
        }
        if (searchOpen) {
            if (button == 0) {
                int x = searchX(), y = searchY();
                if (hit(mouseX, mouseY, x + SEARCH_W - 20, y + 5, 16, 18)) {
                    toggleSearch();
                } else if (hit(mouseX, mouseY, x + 5, y + 31, SEARCH_W - 10, SEARCH_LIST_H)) {
                    int index = ((int) mouseY - y - 31 + searchScroll) / SEARCH_ROW_H;
                    if (index >= 0 && index < searchHits.size()) {
                        jumpToSearchHit(searchHits.get(index));
                    }
                } else {
                    searchBox.mouseClicked(event, doubleClick);
                    searchBox.setFocused(true);
                }
            }
            return true;
        }
        if (button == 0) {
            if (pager.maxEntryScroll() > 0) {
                if (hit(mouseX, mouseY, bx + 6, by - 26, 8, 26)) {
                    pager.targetEntryScroll = pager.clampEntryScroll(pager.targetEntryScroll - ENTRY_STEP * 3);
                    return true;
                }
                if (hit(mouseX, mouseY, pager.stripRight(), by - 26, 8, 26)) {
                    pager.targetEntryScroll = pager.clampEntryScroll(pager.targetEntryScroll + ENTRY_STEP * 3);
                    return true;
                }
            }
            for (int i = 0; i < cats.size(); i++) {
                if (hit(mouseX, mouseY, bx - CAT_W, pager.catTabY(i), CAT_W, pager.catTabH())) {
                    open(i, 0);
                    click();
                    return true;
                }
            }
            if (!cats.isEmpty()) {
                for (int i = 0; i < cats.get(pager.cat).entries().size(); i++) {
                    if (pager.inEntryStrip(mouseX, mouseY)
                        && hit(mouseX, mouseY, pager.entryTabX(i), by - 23, ENTRY_W, 23)) {
                        open(pager.cat, i);
                        click();
                        return true;
                    }
                }
            }
            for (int i = 0; i < linkRects.size(); i++) {
                int[] r = linkRects.get(i);
                if (hit(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    int[] target = linkTargets.get(i);
                    open(target[0], target[1]);
                    click();
                    return true;
                }
            }
            if (!pager.atStart() && hit(mouseX, mouseY, bx + LEFT_X - 2, by + 158, 13, 11)) {
                prev();
                click();
                return true;
            }
            if (!pager.atEnd() && hit(mouseX, mouseY, bx + BOOK_W - LEFT_X - 11, by + 158, 13, 11)) {
                next();
                click();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (searchOpen) {
            if (hit(mouseX, mouseY, searchX() + 5, searchY() + 31, SEARCH_W - 10, SEARCH_LIST_H)) {
                int maxScroll = Math.max(0, searchHits.size() * SEARCH_ROW_H - SEARCH_LIST_H);
                searchScroll = Math.max(0, Math.min(maxScroll, searchScroll - (int) Math.round(verticalAmount * SEARCH_ROW_H)));
            }
            return true;
        }
        if (hit(mouseX, mouseY, bx, by - 26, BOOK_W, 28)) {
            if (pager.maxEntryScroll() > 0) {
                double amount = verticalAmount != 0 ? verticalAmount : horizontalAmount;
                pager.targetEntryScroll = pager.clampEntryScroll(pager.targetEntryScroll - (float) amount * ENTRY_STEP * 2);
            }
            return true;
        }
        if (verticalAmount < 0) {
            next();
        } else if (verticalAmount > 0) {
            prev();
        }
        return true;
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 70 && event.hasControlDown()) {
            toggleSearch();
            return true;
        }
        if (!searchOpen && keyCode == 47 && !event.hasShiftDown() && !event.hasControlDown()) {
            toggleSearch();
            skipSearchSlash = true;
            return true;
        }
        if (searchOpen) {
            if (keyCode == 256) {
                toggleSearch();
            } else if (keyCode == 257 || keyCode == 335) {
                if (!searchHits.isEmpty()) {
                    jumpToSearchHit(searchHits.getFirst());
                }
            } else {
                searchBox.keyPressed(event);
            }
            return true;
        }
        if (keyCode == 266 || keyCode == 263) {
            prev();
            return true;
        }
        if (keyCode == 267 || keyCode == 262) {
            next();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchOpen) {
            boolean skip = skipSearchSlash && event.codepoint() == '/';
            skipSearchSlash = false;
            return skip || searchBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
