// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.*;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 作者：mryt233；2026-11-11：设置列表几何及布局缓存，不绘制。 */
public final class ConfigListModel {
    private static final int BTN = 16, VALUE_W = 108, RESET_W = 14, CTRL_GAP = 2;
    private static final int ROW_MIN_H = 30, HEADER_H = 20;
    private List<Object> layoutCache;
    private int layoutTab = -1, layoutW = -1;
    private JsonObject layoutDoc;
    private Set<String> layoutExpand;

    public void invalidate() {
        layoutCache = null;
        layoutTab = -1;
        layoutW = -1;
        layoutDoc = null;
        layoutExpand = null;
    }

    /**
     * 数值框宽度：屏幕很窄时先压缩数值框，保证「名字 + [- 数值 + ↺]」一行放得下。
     * mryt233 改（2026-11-08，r146a）：原来写死 108，窄窗口（GUI 宽度 320）会挤出面板。
     */
    public static int valueWidth(int listW) {
        int fixed = 2 * BTN + 3 * CTRL_GAP + RESET_W;   // 两个步进按钮 + 三个间距 + ↺
        // mryt233 改（2026-11-09，r146a）：名字区至少留 96 像素（原来只留 80），否则「煤炭 · 灶火功率」这种
        // 十个字左右的名字会一路顶到步进按钮左边；宽面板上仍按原宽度，不受影响。
        return Math.max(56, Math.min(VALUE_W, listW - 10 - 96 - fixed));
    }

    /** 名字区宽度：控件从左到右是「− 数值 ＋ ↺」，名字用剩下的宽度。 */
    public static int labelAreaWidth(int listW) {
        int controls = BTN + CTRL_GAP + valueWidth(listW) + CTRL_GAP + BTN + CTRL_GAP + RESET_W;
        return Math.max(60, listW - 10 - controls);
    }

    private static List<FormattedCharSequence> wrap(net.minecraft.client.gui.Font font, Component label, int maxW) {
        if (font.width(label) <= maxW) {
            return List.of(label.getVisualOrderText());
        }
        return font.split(label, maxW);
    }

    /** 当前页签的完整列表：分组标题 + （展开时的）数值行；行高按名字换行算。 */
    public List<Object> layout(ConfigSchema schema, JsonObject edited, int tab, Set<String> expanded, int panelW, int listW, net.minecraft.client.gui.Font font) {
        if (layoutCache != null && layoutTab == tab && layoutW == panelW && layoutDoc == edited
            && layoutExpand != null && layoutExpand.equals(expanded)) {
            return layoutCache;
        }
        int labelW = labelAreaWidth(listW);
        List<Object> out = new ArrayList<>();
        if (tab == 0) {
            for (Field f : schema.commonFields(edited)) {
                out.add(row(font, f, labelW));
            }
        } else {
            for (Section section : schema.sections(edited)) {
                out.add(new Header(section.titleKey(), section.fields().size(), HEADER_H));
                if (!expanded.contains(section.titleKey())) {
                    continue;
                }
                for (Field f : section.fields()) {
                    out.add(row(font, f, labelW));
                }
            }
        }
        layoutCache = out;
        layoutTab = tab;
        layoutW = panelW;
        layoutDoc = edited;
        layoutExpand = new HashSet<>(expanded);
        return out;
    }

    private static Row row(net.minecraft.client.gui.Font font, Field f, int labelW) {
        List<FormattedCharSequence> lines = wrap(font, f.label(), labelW);
        int h = Math.max(ROW_MIN_H, lines.size() * 10 + 10);
        return new Row(f, lines, h);
    }

    public static int rowHeight(Object element) {
        return element instanceof Header header ? header.height() : ((Row) element).height();
    }

    public static int contentHeight(List<Object> rows) {
        int total = 4;
        for (Object element : rows) {
            total += rowHeight(element);
        }
        return total;
    }

    public static int maxScroll(int contentHeight, int listTop, int listBottom) {
        return Math.max(0, contentHeight - Math.max(1, listBottom - listTop));
    }

}
