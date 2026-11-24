// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

// mryt233 改（2026-11-11）：图标和排版职责迁出；26.2 改用 net.minecraft.util.Util。
import net.minecraft.util.Util;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideLayout.Elem;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent.Category;
import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

import java.util.List;

/** 作者：mryt233；2026-11-11：指南阅读位置、书签几何与缓动。 */
public final class GuidePager {

    private static final int BOOK_W = 272, BOOK_H = 180;

    /** 书签贴图：左边 28×24，顶上 24×24（和 gen_guide_gui.py 一致）。 */
    private static final int TAB_H = 24, ENTRY_W = 24;
    private static final int ENTRY_STEP = 26;

    private static int lastCat, lastEntry;
    public int cat, entry, spread;
    public float entryScroll, targetEntryScroll;
    private long lastScrollFrame;
    private int bx, by;
    private net.minecraft.client.gui.Font font;
    private List<Category> cats = List.of();
    public List<List<Elem>> columns = List.of();
    public void init(List<Category> cats, net.minecraft.client.gui.Font font, int bx, int by) {
        this.cats = cats; this.font = font; this.bx = bx; this.by = by;
        cat = Math.min(lastCat, Math.max(0, cats.size() - 1));
        open(cat, lastEntry);
        entryScroll = targetEntryScroll;
        lastScrollFrame = Util.getMillis();
    }
    public void tick(long now) {
        float amount = 1 - (float) Math.exp(-Math.min(100, Math.max(0, now - lastScrollFrame)) / 65F);
        entryScroll += (targetEntryScroll - entryScroll) * amount;
        entryScroll = clampEntryScroll(entryScroll);
        lastScrollFrame = now;
    }
    public void open(int c, int e) {
        if (cats.isEmpty()) {
            return;
        }
        cat = Math.floorMod(c, cats.size());
        entry = Math.max(0, Math.min(e, cats.get(cat).entries().size() - 1));
        spread = 0;
        lastCat = cat;
        lastEntry = entry;
        columns = GuideLayout.layout(cats.get(cat).entries().get(entry), font);
        targetEntryScroll = clampEntryScroll(entry * ENTRY_STEP + ENTRY_W / 2F - (stripRight() - stripLeft()) / 2F);
    }

    public int spreads() {
        return Math.max(1, (columns.size() + 1) / 2);
    }

    public boolean atStart() {
        return cat == 0 && entry == 0 && spread == 0;
    }

    public boolean atEnd() {
        return cats.isEmpty() || (cat == cats.size() - 1 && entry == cats.get(cat).entries().size() - 1 && spread == spreads() - 1);
    }

    public void next() {
        if (cats.isEmpty() || atEnd()) {
            return;
        }
        if (spread < spreads() - 1) {
            spread++;
        } else if (entry < cats.get(cat).entries().size() - 1) {
            open(cat, entry + 1);
        } else {
            open(cat + 1, 0);
        }
    }

    public void prev() {
        if (cats.isEmpty() || atStart()) {
            return;
        }
        if (spread > 0) {
            spread--;
        } else if (entry > 0) {
            open(cat, entry - 1);
            spread = spreads() - 1;
        } else {
            open(cat - 1, cats.get(Math.floorMod(cat - 1, cats.size())).entries().size() - 1);
            spread = spreads() - 1;
        }
    }

    /** 左侧分类书签的间距：书高 180，9 个分类也在书框里。 */
    public int catTabStep() {
        int n = cats.size();
        return n <= 1 ? TAB_H : Math.min(TAB_H, Math.max(9, (BOOK_H - TAB_H - 6) / (n - 1)));
    }

    public int catTabY(int i) {
        return by + 6 + i * catTabStep();
    }

    public int catTabH() {
        return TAB_H;
    }

    public int searchTabX() {
        return bx + BOOK_W - 6 - ENTRY_W;
    }

    public int entryStripWidth() {
        return cats.isEmpty() ? 0 : Math.max(0, cats.get(cat).entries().size() * ENTRY_STEP - 2);
    }

    public boolean entryOverflow() {
        return entryStripWidth() > searchTabX() - (bx + 6) - 6;
    }

    public int stripLeft() {
        return bx + (entryOverflow() ? 14 : 6);
    }

    public int stripRight() {
        return searchTabX() - (entryOverflow() ? 10 : 6);
    }

    public int maxEntryScroll() {
        return Math.max(0, entryStripWidth() - (stripRight() - stripLeft()));
    }

    public float clampEntryScroll(float scroll) {
        return Math.max(0, Math.min(maxEntryScroll(), scroll));
    }

    public boolean inEntryStrip(double x, double y) {
        return hit(x, y, stripLeft(), by - 26, stripRight() - stripLeft(), 26);
    }

    public int entryTabX(int i) {
        return stripLeft() + i * ENTRY_STEP - Math.round(entryScroll);
    }

}
