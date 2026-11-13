// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent.Entry;

/** 作者：mryt233；2026-11-11：指南块排版，字体显式传入（26.2）。 */
public final class GuideLayout {
    private GuideLayout() { }
    private static final int COL_W = 112, COL_H = 144;
    private static final int TEXT = 0xFF2A1A0E, HEADING = 0xFF5A2A10, MUTED = 0xFF7A6040, TIP = 0xFF2E5E1E, WARN = 0xFF8A3A12, NUMBER = 0xFF1E6E78, RULE = 0x40603820;
    private static final int RECIPE_H = 66;

    /** 一排物品图标：每行 6 个，格距 18。 */
    private static final int ICON_STEP = 18, ICON_PER_ROW = 6;

    public enum Kind { HEADER, TEXT, SUB, NOTE, TH, ROW, BUL, NUM, ICONS, RECIPE, CODE, RULE, GAP }

    /** 排版后的一块：画什么、占多高、点了跳哪。 */
    public static final class Elem {
        public final Kind kind;
        public final int height;
        public List<FormattedCharSequence> lines = List.of();
        public int color = TEXT;
        public ItemStack icon = ItemStack.EMPTY;
        public String number = "";
        public List<Identifier> recipes = List.of();
        public List<ItemStack> icons = List.of();
        public int goCat = -1, goEntry = -1, indent;

        public Elem(Kind kind, int height) {
            this.kind = kind;
            this.height = height;
        }

        public Elem lines(List<FormattedCharSequence> v) {
            lines = v;
            return this;
        }

        public Elem color(int v) {
            color = v;
            return this;
        }

        public Elem icon(ItemStack v) {
            icon = v;
            return this;
        }

        public Elem number(String v) {
            number = v;
            return this;
        }

        public Elem recipes(List<Identifier> v) {
            recipes = v;
            return this;
        }

        public Elem icons(List<ItemStack> v) {
            icons = v;
            return this;
        }

        public Elem go(int c, int e) {
            goCat = c;
            goEntry = e;
            return this;
        }

        public Elem indent(int v) {
            indent = v;
            return this;
        }
    }

    /** 一个条目：标题块 + 按源文件顺序排的正文块，从左页流到右页、放不下就翻到下一个跨页。 */
    public static List<List<Elem>> layout(Entry e, net.minecraft.client.gui.Font font) {
        JsonObject j = e.json();
        List<Elem> flow = new ArrayList<>();
        List<FormattedCharSequence> title = split(font, e.title().copy().withStyle(ChatFormatting.BOLD), COL_W - 20);
        flow.add(new Elem(Kind.HEADER, Math.max(24, title.size() * 9 + 10)).lines(title).icon(e.icon()));
        if (j.has("blocks")) {
            for (JsonElement b : j.getAsJsonArray("blocks")) {
                block(font, flow, b.getAsJsonObject());
            }
        } else {
            legacy(font, flow, j);
        }
        return pour(flow);
    }

    /** 已弃用的旧版字段（仍保留资源包兼容）：说明、对照表、配方、步骤、提示。 */
    @Deprecated
    private static void legacy(net.minecraft.client.gui.Font font, List<Elem> flow, JsonObject j) {
        if (j.has("get")) {
            para(font, flow, j.get("get").getAsString(), MUTED, 3);
        }
        if (j.has("text")) {
            para(font, flow, j.get("text").getAsString(), TEXT, 5);
        }
        if (j.has("rows")) {
            for (JsonElement r : j.getAsJsonArray("rows")) {
                JsonArray a = r.getAsJsonArray();
                row(font, flow, GuideItems.of(a.get(0).getAsString()), a.get(1).getAsString(), TEXT, 0, -1, -1);
            }
            flow.add(new Elem(Kind.GAP, 3));
        }
        if (j.has("recipes")) {
            flow.add(new Elem(Kind.RECIPE, RECIPE_H).recipes(GuideRecipes.ids(j.getAsJsonArray("recipes"))));
        }
        if (j.has("steps")) {
            int n = 1;
            for (JsonElement s : j.getAsJsonArray("steps")) {
                List<FormattedCharSequence> lines = split(font, Component.literal(s.getAsString()), COL_W - 11);
                flow.add(new Elem(Kind.NUM, lines.size() * 9 + 3).lines(lines).number(String.valueOf(n++)).color(TEXT));
            }
            flow.add(new Elem(Kind.GAP, 3));
        }
        if (j.has("tip")) {
            note(font, flow, "", List.of(j.get("tip").getAsString()), "tip");
        }
    }

    private static void block(net.minecraft.client.gui.Font font, List<Elem> flow, JsonObject o) {
        String k = o.get("k").getAsString();
        String t = o.has("t") ? o.get("t").getAsString() : "";
        switch (k) {
            case "sub" -> {
                List<FormattedCharSequence> lines = split(font, Component.literal(t), COL_W - 4);
                flow.add(new Elem(Kind.SUB, lines.size() * 9 + 5).lines(lines).color(HEADING));
            }
            case "p" -> para(font, flow, t, TEXT, 4);
            case "note" -> {
                List<String> body = new ArrayList<>();
                if (o.has("b")) {
                    for (JsonElement b : o.getAsJsonArray("b")) {
                        body.add(b.getAsString());
                    }
                }
                note(font, flow, t, body, o.has("s") ? o.get("s").getAsString() : "tip");
            }
            case "th" -> {
                List<FormattedCharSequence> lines = split(font, Component.literal(t), COL_W);
                flow.add(new Elem(Kind.TH, lines.size() * 9 + 4).lines(lines).color(HEADING));
            }
            case "tr" -> {
                ItemStack icon = o.has("icon") ? GuideItems.of(o.get("icon").getAsString()) : ItemStack.EMPTY;
                int gc = -1, ge = -1;
                if (o.has("go") && !o.get("go").isJsonNull()) {
                    JsonArray go = o.getAsJsonArray("go");
                    gc = go.get(0).getAsInt();
                    ge = go.get(1).getAsInt();
                }
                row(font, flow, icon, t, gc >= 0 ? NUMBER : TEXT, 0, gc, ge);
            }
            case "bul", "num" -> {
                int indent = o.has("d") ? Math.max(0, Math.min(3, o.get("d").getAsInt())) : 0;
                String mark = k.equals("bul") ? "•" : (o.has("n") ? String.valueOf(o.get("n").getAsInt()) : "-");
                int x = indent * 4;
                ItemStack icon = o.has("icon") ? GuideItems.of(o.get("icon").getAsString()) : ItemStack.EMPTY;
                int textX = icon.isEmpty() ? (k.equals("bul") ? 8 : 11) : (k.equals("bul") ? 15 : 22);
                List<FormattedCharSequence> lines = split(font, Component.literal(t), Math.max(40, COL_W - textX - x));
                Elem el = new Elem(k.equals("bul") ? Kind.BUL : Kind.NUM,
                    Math.max(icon.isEmpty() ? 0 : 12, lines.size() * 9 + 3))
                    .lines(lines).number(mark).color(TEXT).indent(x).icon(icon);
                flow.add(el);
            }
            case "icons" -> {
                List<ItemStack> stacks = new ArrayList<>();
                for (JsonElement i : o.getAsJsonArray("ids")) {
                    stacks.add(GuideItems.of(i.getAsString()));
                }
                if (!stacks.isEmpty()) {
                    int rows = (stacks.size() + ICON_PER_ROW - 1) / ICON_PER_ROW;
                    flow.add(new Elem(Kind.ICONS, rows * ICON_STEP + 2).icons(stacks));
                }
            }
            case "recipes" -> flow.add(new Elem(Kind.RECIPE, RECIPE_H).recipes(GuideRecipes.ids(o.getAsJsonArray("ids"))));
            case "code" -> {
                List<String> code = new ArrayList<>();
                for (JsonElement l : o.getAsJsonArray("l")) {
                    code.add(l.getAsString());
                }
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (String s : code) {
                    for (FormattedCharSequence f : split(font, Component.literal(s), COL_W - 6)) {
                        lines.add(f);
                    }
                }
                flow.add(new Elem(Kind.CODE, lines.size() * 9 + 6).lines(lines).color(MUTED));
            }
            case "rule" -> flow.add(new Elem(Kind.RULE, 7));
            default -> {
            }
        }
    }

    private static void para(net.minecraft.client.gui.Font font, List<Elem> flow, String text, int color, int after) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (FormattedCharSequence line : split(font, Component.literal(text), COL_W)) {
            flow.add(new Elem(Kind.TEXT, 9).lines(List.of(line)).color(color));
        }
        flow.add(new Elem(Kind.GAP, after));
    }

    /** 提示框：一条竖色条 + 小标题 + 几行字（和网页教程的 tip / warn / dev 框对应）。 */
    private static void note(net.minecraft.client.gui.Font font, List<Elem> flow, String title, List<String> body, String kind) {
        int color = switch (kind) {
            case "warn" -> WARN;
            case "dev" -> NUMBER;
            case "quote" -> MUTED;
            default -> TIP;
        };
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            lines.addAll(split(font, Component.literal(title), COL_W - 10));
        }
        for (String s : body) {
            if (s != null && !s.isEmpty()) {
                lines.addAll(split(font, Component.literal(s), COL_W - 10));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        flow.add(new Elem(Kind.NOTE, lines.size() * 9 + 6).lines(lines).color(color));
    }

    /** 一行「图标 + 文字」；go 不是 -1 时点这行会跳过去（本章目录）。 */
    private static void row(net.minecraft.client.gui.Font font, List<Elem> flow, ItemStack icon, String text, int color, int indent, int goCat, int goEntry) {
        int x = 15 + indent * 4;
        List<FormattedCharSequence> lines = split(font, Component.literal(text == null ? "" : text), COL_W - x);
        Elem el = new Elem(Kind.ROW, lines.size() <= 1 ? 14 : lines.size() * 9 + 5)
            .lines(lines).color(color).icon(icon).indent(indent);
        if (goCat >= 0) {
            el.go(goCat, goEntry);
        }
        flow.add(el);
    }

    /** 装页：一列装满就换下一列（两列 = 一个跨页）；一块比一页还高（英文长段落、长提示框）就切成几块接着排。 */
    private static List<List<Elem>> pour(List<Elem> flow) {
        List<List<Elem>> cols = new ArrayList<>();
        List<Elem> cur = new ArrayList<>();
        cols.add(cur);
        int used = 0;
        for (Elem el : flow) {
            for (Elem part : splitBlock(el)) {
                if (used + part.height > COL_H && !cur.isEmpty()) {
                    cur = new ArrayList<>();
                    cols.add(cur);
                    used = 0;
                    if (part.kind == Kind.GAP) {
                        continue;
                    }
                }
                cur.add(part);
                used += part.height;
            }
        }
        return cols;
    }

    /** 一块比一页高就按行切开（提示框、列表、表格行、标题、代码），一排图标按行切。 */
    private static List<Elem> splitBlock(Elem el) {
        if (el.height <= COL_H) {
            return List.of(el);
        }
        if (el.kind == Kind.ICONS) {
            List<Elem> out = new ArrayList<>();
            int per = Math.max(1, (COL_H - 2) / ICON_STEP * ICON_PER_ROW);
            for (int i = 0; i < el.icons.size(); i += per) {
                List<ItemStack> part = el.icons.subList(i, Math.min(el.icons.size(), i + per));
                out.add(new Elem(Kind.ICONS, ((part.size() + ICON_PER_ROW - 1) / ICON_PER_ROW) * ICON_STEP + 2).icons(part));
            }
            return out;
        }
        int pad = switch (el.kind) {
            case NOTE -> 6;
            case ROW -> 5;
            case BUL, NUM -> 3;
            default -> 0;
        };
        int maxLines = Math.max(1, (COL_H - pad) / 9);
        if (el.lines.size() <= maxLines) {
            return List.of(el);
        }
        List<Elem> out = new ArrayList<>();
        for (int i = 0; i < el.lines.size(); i += maxLines) {
            List<FormattedCharSequence> part = el.lines.subList(i, Math.min(el.lines.size(), i + maxLines));
            Elem e = new Elem(el.kind, Math.max(pad + part.size() * 9, el.kind == Kind.ROW ? 9 : 0))
                .lines(part).color(el.color).indent(el.indent);
            if (i == 0) {
                e.icon(el.icon).number(el.number);
            }
            if (el.goCat >= 0 && i == 0) {
                e.go(el.goCat, el.goEntry);
            }
            out.add(e);
        }
        return out;
    }

    private static List<FormattedCharSequence> split(net.minecraft.client.gui.Font font, Component text, int width) {
        return font.split(text, width);
    }
}
