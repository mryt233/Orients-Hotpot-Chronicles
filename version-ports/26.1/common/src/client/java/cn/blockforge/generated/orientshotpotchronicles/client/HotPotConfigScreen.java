package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 游戏内可视化设置界面（r135e，2026-11-05）：把 config/orients_hotpot_chronicles.json 里的数值搬进游戏，
 * 四个页签——特效与常规（含火锅粒子强度滑块 0%～100%）、涮煮时间、汤碗与调料、食物营养。
 * <p>
 * mryt233 改（2026-11-07）：从 NeoForge 模组列表的「Config」按钮进入；Fabric 安装 Mod Menu 后从模组列表进入。
 * <p>
 * 所有数值都读写自己手上的 {@link #edited} 副本，点「保存并生效」才一次性交给
 * {@link Config#saveAndReload}（写回文件 + 热重载）；拖动粒子滑块例外，任务要求实时生效。
 * 作者：mryt233
 */
public class HotPotConfigScreen extends Screen {
    /** 面板尺寸（像素）；内容区从 46 到 174，底部操作栏在 180。 */
    private static final int W = 312, H = 206;
    private static final int TAB_W = 70, TAB_H = 18, TAB_GAP = 2, TAB_Y = 23;
    private static final int CONTENT_TOP = 46, LIST_BOTTOM = 174;
    /** 页签 1 上方：滑块 48、提示 71、快捷档 82，下面的列表从 106 起。 */
    private static final int SLIDER_Y = 48, HINT_Y = 71, QUICK_Y = 82, QUICK_H = 18, LIST_TOP_GENERAL = 106;
    private static final int BOTTOM_Y = 180, BOTTOM_H = 20, BOTTOM_W = 92, BOTTOM_GAP = 6;
    private static final int ROW_H = 16, BTN_W = 14, BTN_GAP = 2, SCROLLBAR_W = 4;
    private static final int TEXT = 0xFFE8C88A, MUTED = 0xFFA88C5C, HOVER = 0xFF6A4418,
        STRIPE = 0x18FFFFFF, ROW_BG = 0x30201A10, PANEL_BG = 0xFF2A190C;

    /**
     * 一个可调的数值项：配置路径、整数与否、范围、步进按钮（负到正）、数值显示宽度，
     * 以及「这个数按 tick 算、右边要折算成秒」的标记（只有涮煮时间用）。
     */
    private record Field(String[] path, boolean integer, double min, double max, double[] steps,
                         int valueWidth, boolean seconds, int valueAfter) {
        Field(String[] path, boolean integer, double min, double max, double[] steps, int valueWidth) {
            this(path, integer, min, max, steps, valueWidth, false, -1);
        }

        Field withSeconds() {
            return new Field(path, integer, min, max, steps, valueWidth, true, valueAfter);
        }

        /** 数值显示插在第几个步进按钮之后（-1 = 全部按钮之后）；涮煮时间照任务单写成「-20 -5 数值 +5 +20」。 */
        Field withValueAfter(int index) {
            return new Field(path, integer, min, max, steps, valueWidth, seconds, index);
        }
    }

    /** 列表一行：图标（可空）+ 名字 + 右侧若干数值项。 */
    private record Row(Component label, ItemStack icon, List<Field> fields) {
    }

    private final Screen parent;
    private JsonObject edited;
    private int entryParticle = 100;
    private int tab;
    private int px, py;
    private int listTop;
    private int scroll;
    private boolean draggingBar;
    private double dragGrab;
    private ParticleSlider slider;

    /** 这一帧可点的区域和对应动作（自绘控件都走这里，避免一堆 Button 抢鼠标滚轮）。 */
    private final List<int[]> clickRects = new ArrayList<>();
    private final List<Runnable> clickActions = new ArrayList<>();

    /** 列表行只在切换页签 / 重置时重建，别每帧都拼一遍（食物页有 50 行、调料页接近 90 行）。 */
    private List<Row> cachedRows;
    private int cachedTab = -1;
    private JsonObject cachedFor;

    public HotPotConfigScreen(Screen parent) {
        super(Component.translatable("text." + HotPot.MOD_ID + ".config.title"));
        this.parent = parent;
    }

    /** 模组列表等入口在拿不到上一级界面时用这个，关界面就直接回游戏。 */
    public HotPotConfigScreen() {
        this(null);
    }

    @Override
    protected void init() {
        // 改窗口大小会重跑 init，这时不能把玩家已经改了一半的数值丢掉
        if (edited == null) {
            edited = Config.editableCopy();
            entryParticle = Config.particleIntensity();
        }
        px = (width - W) / 2;
        py = Math.max(4, (height - H) / 2);
        rebuildPageWidgets();
    }

    /**
     * 只有页签 1 用原生控件（滑块）；其余全部自绘，切换页签时同步增删，并把列表上边界一起算好。
     * mryt233 改：{@code listTop} 以前只在 init() 里算一次，切换页签不重跑 init，于是页签 0 → 1 时列表被压到
     * 68 像素高（只露 4 行）、页签 1 → 0 时列表第一行压在粒子强度滑块下面，滚动条的抓取区和命中区一起错位。
     */
    private void rebuildPageWidgets() {
        if (slider != null) {
            removeWidget(slider);
            slider = null;
        }
        if (tab == 0) {
            slider = new ParticleSlider(px + 12, py + SLIDER_Y, W - 24, 20);
            slider.syncFromIntensity(Config.particleIntensity());
            addRenderableWidget(slider);
        }
        // 列表上边界跟着页签走：页签 0 上面有滑块 / 提示 / 快捷档，页签 1~3 从 CONTENT_TOP 起。
        listTop = py + (tab == 0 ? LIST_TOP_GENERAL : CONTENT_TOP);
        draggingBar = false;   // 拖到一半点了页签：旧 listTop 算的比例作废，别让下一次拖动跳格
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    // ------------------------------------------------------------ 数值读写

    /** 粒子强度：拖动滑块立刻生效，同时写进待保存的副本。 */
    private void applyParticle(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        Config.setParticleIntensity(p);
        node(edited, new String[]{"client"}, 1).addProperty("particle_intensity", p);
    }

    /** 取出（必要时创建）配置路径的前 upto 段对象。 */
    private static JsonObject node(JsonObject root, String[] path, int upto) {
        JsonObject cur = root;
        for (int i = 0; i < upto; i++) {
            JsonElement el = cur.get(path[i]);
            if (el == null || !el.isJsonObject()) {
                JsonObject next = new JsonObject();
                cur.add(path[i], next);
                cur = next;
            } else {
                cur = el.getAsJsonObject();
            }
        }
        return cur;
    }

    private double value(Field f) {
        JsonObject parentNode = node(edited, f.path(), f.path().length - 1);
        JsonElement el = parentNode.get(f.path()[f.path().length - 1]);
        return el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()
            ? f.min() : el.getAsDouble();
    }

    private void setValue(Field f, double raw) {
        double v = Math.max(f.min(), Math.min(f.max(), raw));
        JsonObject parentNode = node(edited, f.path(), f.path().length - 1);
        String key = f.path()[f.path().length - 1];
        if (f.integer()) {
            parentNode.addProperty(key, (int) Math.round(v));
        } else {
            parentNode.addProperty(key, Math.round(v * 100.0) / 100.0);
        }
    }

    private static String formatValue(Field f, double v) {
        return f.integer() ? String.valueOf((int) Math.round(v)) : trim(v);
    }

    /** 小数去掉多余的尾零（0.85 保留，5.00 → 5）。 */
    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    // ------------------------------------------------------------ 列表内容

    private List<Row> rows() {
        if (cachedRows != null && cachedTab == tab && cachedFor == edited) {
            return cachedRows;
        }
        List<Row> rows = new ArrayList<>();
        switch (tab) {
            case 0 -> generalRows(rows);
            case 1 -> cookingRows(rows);
            case 2 -> effectRows(rows);
            default -> foodRows(rows);
        }
        cachedRows = rows;
        cachedTab = tab;
        cachedFor = edited;
        return rows;
    }

    /** 页签 1 的下半部分：烹饪兜底数值 + 6 种汤底的加热倍率。 */
    private void generalRows(List<Row> rows) {
        rows.add(singleLiteral("cooking.fallback_ticks", new String[]{"cooking", "fallback_ticks"}, true, 1, 100000, 10));
        rows.add(singleLiteral("cooking.fallback_xp", new String[]{"cooking", "fallback_xp"}, false, 0, 100, 0.05));
        rows.add(singleLiteral("cooking.xp_per_item", new String[]{"cooking", "xp_per_item"}, false, 0, 100, 0.05));
        rows.add(singleLiteral("cooking.tofu_cook_ticks", new String[]{"cooking", "tofu_cook_ticks"}, true, 1, 100000, 10));
        rows.add(singleLiteral("cooking.tofu_overcook_ticks", new String[]{"cooking", "tofu_overcook_ticks"}, true, 1, 100000, 10));
        for (SoupBase base : SoupBase.values()) {
            rows.add(new Row(soupName(base), ItemStack.EMPTY, List.of(new Field(
                new String[]{"soup_speed", base.textureKey()}, false, 0.1, 10,
                new double[]{-0.05, 0.05}, 40))));
        }
    }

    /** 页签 2：本模组登记过的生食材涮煮刻数，-20 / -5 / +5 / +20 四档。 */
    private void cookingRows(List<Row> rows) {
        JsonElement itemsNode = node(edited, new String[]{"cooking"}, 1).get("items");
        if (itemsNode == null || !itemsNode.isJsonObject()) {
            return;
        }
        JsonObject items = itemsNode.getAsJsonObject();
        for (String id : List.copyOf(items.keySet())) {
            ItemStack icon = stackOf(id);
            Component name = icon.isEmpty() ? Component.literal(id) : icon.getHoverName();
            rows.add(new Row(name, icon, List.of(new Field(new String[]{"cooking", "items", id},
                true, 1, 100000, new double[]{-20, -5, 5, 20}, 92)
                .withSeconds().withValueAfter(2))));
        }
    }

    /**
     * 页签 3：汤碗（6 种汤底 × 10 项）、蘸料（每种 3 项）、饮料（每口营养与每种的效果）。
     * mryt233 改（2026-11-06，r139）：汤碗项跟着配置改成 second_*（副效果：高汤伤害吸收、红汤速度、
     * 三鲜幸运、清汤饱和）与 fire_*（红汤抗火），旧的 slow_* 随「高汤不再缓慢」一起去掉。
     */
    private void effectRows(List<Row> rows) {
        for (SoupBase base : SoupBase.values()) {
            for (String key : new String[]{"hunger", "saturation", "duration", "amplifier",
                "second_duration", "second_amplifier", "fire_duration", "fire_amplifier",
                "hunger_duration", "hunger_amplifier"}) {
                rows.add(new Row(soupName(base).copy().append(Component.literal(" · " + key)),
                    ItemStack.EMPTY, List.of(bowlField(base.textureKey(), key))));
            }
        }
        for (Seasoning s : Seasoning.ALL) {
            MutableComponent name = Component.translatable(
                "text." + HotPot.MOD_ID + ".seasoning." + s.id()).copy();
            for (String key : new String[]{"duration", "amplifier", "hunger"}) {
                rows.add(new Row(name.copy().append(Component.literal(" · " + key)),
                    ItemStack.EMPTY, List.of(seasoningField(s.id(), key))));
            }
        }
        rows.add(singleLiteral("drink.sip.hunger", new String[]{"drink", "sip", "hunger"}, true, 0, 100, 1));
        rows.add(singleLiteral("drink.sip.saturation", new String[]{"drink", "sip", "saturation"}, false, 0, 10, 0.05));
        for (DrinkKind kind : DrinkKind.values()) {
            if (kind == DrinkKind.WATER || kind == DrinkKind.MILK) {
                continue;
            }
            ItemStack icon = stackOf(kind.id());
            MutableComponent name = Component.translatable("item." + HotPot.MOD_ID + "." + kind.id()).copy();
            rows.add(new Row(name.copy().append(Component.literal(" · duration")), icon,
                List.of(new Field(new String[]{"drink", kind.id(), "duration"}, true, 0, 100000,
                    new double[]{-10, 10}, 46))));
            rows.add(new Row(name.copy().append(Component.literal(" · amplifier")), icon,
                List.of(new Field(new String[]{"drink", kind.id(), "amplifier"}, true, 0, 9,
                    new double[]{-1, 1}, 46))));
        }
    }

    /** 页签 4：全部食物，一行同时调饥饿值与饱和度。 */
    private void foodRows(List<Row> rows) {
        JsonObject food = node(edited, new String[]{"food"}, 1);
        for (String id : List.copyOf(food.keySet())) {
            ItemStack icon = stackOf(id);
            Component name = icon.isEmpty() ? Component.literal(id) : icon.getHoverName();
            rows.add(new Row(name, icon, List.of(
                new Field(new String[]{"food", id, "hunger"}, true, 0, 100, new double[]{-1, 1}, 26),
                new Field(new String[]{"food", id, "saturation"}, false, 0, 10,
                    new double[]{-0.05, 0.05}, 32))));
        }
    }

    private Row singleLiteral(String label, String[] path, boolean integer, double min, double max, double step) {
        return new Row(Component.literal(label), ItemStack.EMPTY,
            List.of(new Field(path, integer, min, max, new double[]{-step, step}, 46)));
    }

    private static Field bowlField(String base, String key) {
        return switch (key) {
            case "saturation" -> new Field(new String[]{"soup_bowl", base, key}, false, 0, 10,
                new double[]{-0.05, 0.05}, 40);
            case "duration", "second_duration", "fire_duration", "hunger_duration" -> new Field(
                new String[]{"soup_bowl", base, key}, true, 0, 100000, new double[]{-10, 10}, 40);
            case "amplifier", "second_amplifier", "fire_amplifier", "hunger_amplifier" -> new Field(
                new String[]{"soup_bowl", base, key}, true, 0, 9, new double[]{-1, 1}, 40);
            default -> new Field(new String[]{"soup_bowl", base, key}, true, 0, 100,
                new double[]{-1, 1}, 40);
        };
    }

    private static Field seasoningField(String id, String key) {
        return switch (key) {
            case "duration" -> new Field(new String[]{"seasoning", id, key}, true, 0, 100000,
                new double[]{-10, 10}, 40);
            case "amplifier" -> new Field(new String[]{"seasoning", id, key}, true, 0, 9,
                new double[]{-1, 1}, 40);
            default -> new Field(new String[]{"seasoning", id, key}, true, 0, 100,
                new double[]{-1, 1}, 40);
        };
    }

    private static MutableComponent soupName(SoupBase base) {
        return Component.translatable("text." + HotPot.MOD_ID + ".soup." + base.textureKey());
    }

    private static ItemStack stackOf(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(HotPot.id(id)).orElse(Items.BARRIER);
        return item == Items.BARRIER ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ------------------------------------------------------------ 画

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        // GuiKit.frame 以 (0,0) 为左上角，这里靠 pose 平移到面板位置
        g.pose().pushMatrix();
        g.pose().translate(px, py);
        GuiKit.frame(g, W, H, 1, true);
        g.pose().popMatrix();
        g.text(font, title, px + 12, py + 7, GuiKit.TITLE, true);
        g.fill(px + 12, py + 19, px + W - 12, py + 20, GuiKit.BRASS_DARK);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        clickRects.clear();
        clickActions.clear();
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawTabs(g, mouseX, mouseY);
        if (tab == 0) {
            drawParticleBlock(g, mouseX, mouseY);
        }
        drawList(g, mouseX, mouseY);
        drawBottomBar(g, mouseX, mouseY);
    }

    private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String[] keys = {"general", "cooking", "effects", "food"};
        int startX = px + (W - (TAB_W * 4 + TAB_GAP * 3)) / 2;
        for (int i = 0; i < 4; i++) {
            int x = startX + i * (TAB_W + TAB_GAP);
            int y = py + TAB_Y;
            boolean hover = inside(mouseX, mouseY, x, y, TAB_W, TAB_H);
            boolean active = i == tab;
            g.fill(x, y, x + TAB_W, y + TAB_H, active ? HOVER : (hover ? 0xFF4A2E12 : PANEL_BG));
            g.fill(x, y, x + TAB_W, y + 1, GuiKit.BRASS);
            if (active) {
                g.fill(x, y + TAB_H - 2, x + TAB_W, y + TAB_H, GuiKit.BRASS_HI);
            }
            // 英文的页签名比中文长（Effects & General），放不下就整体缩小一点，别糊出边界
            drawFitted(g, Component.translatable("text." + HotPot.MOD_ID + ".config.tab." + keys[i]),
                x + TAB_W / 2, y + 5, TAB_W - 6, active ? GuiKit.TITLE : TEXT);
            final int target = i;
            clickable(x, y, TAB_W, TAB_H, () -> {
                tab = target;
                scroll = 0;
                rebuildPageWidgets();
            });
        }
    }

    /** 居中画一行字；宽度超过 max 就等比缩小（1.0 → 0.6 为止），不截断成看不懂的残字。 */
    private void drawFitted(GuiGraphicsExtractor g, Component text, int centerX, int y, int max, int color) {
        int w = font.width(text);
        if (w <= max) {
            g.centeredText(font, text, centerX, y, color);
            return;
        }
        float scale = Math.max(0.6F, max / (float) w);
        g.pose().pushMatrix();
        g.pose().translate(centerX, y + 4);
        g.pose().scale(scale, scale);
        g.centeredText(font, text, 0, -4, color);
        g.pose().popMatrix();
    }

    private void drawParticleBlock(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("text." + HotPot.MOD_ID + ".config.particles.hint"),
            px + 12, py + HINT_Y, MUTED, false);
        int qw = (W - 24 - BOTTOM_GAP * 2) / 3;
        int[] percents = {0, 50, 100};
        for (int i = 0; i < 3; i++) {
            int x = px + 12 + i * (qw + BOTTOM_GAP);
            int y = py + QUICK_Y;
            boolean hover = inside(mouseX, mouseY, x, y, qw, QUICK_H);
            boolean chosen = Config.particleIntensity() == percents[i];
            g.fill(x, y, x + qw, y + QUICK_H, chosen ? HOVER : (hover ? 0xFF4A2E12 : PANEL_BG));
            g.fill(x, y, x + qw, y + 1, GuiKit.BRASS);
            g.centeredText(font, Component.literal(percents[i] + "%"), x + qw / 2, y + 5,
                chosen ? GuiKit.TITLE : TEXT);
            final int percent = percents[i];
            clickable(x, y, qw, QUICK_H, () -> {
                applyParticle(percent);
                if (slider != null) {
                    slider.syncFromIntensity(percent);
                }
            });
            if (hover) {
                g.setTooltipForNextFrame(font, percents[i] == 0
                    ? Component.translatable("text." + HotPot.MOD_ID + ".config.particles.off")
                    : Component.translatable("text." + HotPot.MOD_ID + ".config.particles.value", percent),
                    mouseX, mouseY);
            }
        }
    }

    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Row> rows = rows();
        int x = px + 12;
        int w = W - 24 - SCROLLBAR_W - 4;
        int bottom = py + LIST_BOTTOM;
        int innerHeight = Math.max(0, bottom - listTop);
        int total = rows.size() * ROW_H;
        scroll = clampScroll();
        g.enableScissor(x, listTop, x + w + SCROLLBAR_W + 4, bottom);
        for (int i = 0; i < rows.size(); i++) {
            int rowY = listTop - scroll + i * ROW_H;
            if (rowY + ROW_H <= listTop || rowY >= bottom) {
                continue;
            }
            drawRow(g, rows.get(i), x, rowY, w, i, mouseX, mouseY);
        }
        g.disableScissor();
        if (total > innerHeight) {
            drawScrollbar(g, x + w + 3, listTop, innerHeight, total);
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int height, int total) {
        g.fill(x, top, x + SCROLLBAR_W, top + height, 0x40FFFFFF);
        int thumbH = Math.max(12, height * height / total);
        int maxScroll = Math.max(1, total - height);
        int thumbY = top + (height - thumbH) * scroll / maxScroll;
        g.fill(x, thumbY, x + SCROLLBAR_W, thumbY + thumbH, GuiKit.BRASS);
    }

    private void drawRow(GuiGraphicsExtractor g, Row row, int x, int y, int w, int index, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + ROW_H - 1, (index % 2 == 0) ? ROW_BG : STRIPE);
        int labelX = x + 2;
        if (!row.icon().isEmpty()) {
            g.item(row.icon(), x + 2, y);
            labelX = x + 20;
        }
        int fieldsW = 0;
        for (int i = 0; i < row.fields().size(); i++) {
            fieldsW += fieldWidth(row.fields().get(i)) + (i > 0 ? 6 : 0);
        }
        int fieldX = x + w - fieldsW;
        String text = font.plainSubstrByWidth(row.label().getString(), Math.max(8, fieldX - labelX - 4));
        g.text(font, text, labelX, y + 4, TEXT, false);
        for (Field f : row.fields()) {
            drawField(g, f, fieldX, y, mouseX, mouseY);
            fieldX += fieldWidth(f) + 6;
        }
    }

    private static int fieldWidth(Field f) {
        return f.steps().length * (BTN_W + BTN_GAP) + f.valueWidth();
    }

    private void drawField(GuiGraphicsExtractor g, Field f, int x, int y, int mouseX, int mouseY) {
        int by = y + 1;
        int after = f.valueAfter() < 0 ? f.steps().length : f.valueAfter();
        for (int i = 0; i < f.steps().length; i++) {
            if (i == after) {
                x = drawFieldValue(g, f, x, y);
            }
            double d = f.steps()[i];
            boolean hover = inside(mouseX, mouseY, x, by, BTN_W, BTN_W);
            g.fill(x, by, x + BTN_W, by + BTN_W, hover ? HOVER : PANEL_BG);
            g.fill(x, by, x + BTN_W, by + 1, GuiKit.BRASS);
            g.centeredText(font, Component.literal((d > 0 ? "+" : "-") + trim(Math.abs(d))),
                x + BTN_W / 2, by + 4, hover ? GuiKit.TITLE : TEXT);
            clickable(x, by, BTN_W, BTN_W, () -> setValue(f, value(f) + d));
            x += BTN_W + BTN_GAP;
        }
        if (after >= f.steps().length) {
            drawFieldValue(g, f, x, y);
        }
    }

    /** 画一项的当前数值；返回下一个控件的 x。 */
    private int drawFieldValue(GuiGraphicsExtractor g, Field f, int x, int y) {
        double v = value(f);
        // 涮煮时间按任务单的写法显示「100 tick (5.0s)」，一眼能看出是几秒
        String shown = f.seconds()
            ? formatValue(f, v) + " tick (" + String.format(Locale.ROOT, "%.1f", v / 20.0) + "s)"
            : formatValue(f, v);
        g.centeredText(font, Component.literal(shown), x + f.valueWidth() / 2, y + 4, GuiKit.RIVET);
        return x + f.valueWidth();
    }

    private void drawBottomBar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int startX = px + (W - (BOTTOM_W * 3 + BOTTOM_GAP * 2)) / 2;
        String[] keys = {"save", "reset", "cancel"};
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (BOTTOM_W + BOTTOM_GAP);
            int y = py + BOTTOM_Y;
            boolean hover = inside(mouseX, mouseY, x, y, BOTTOM_W, BOTTOM_H);
            g.fill(x, y, x + BOTTOM_W, y + BOTTOM_H, hover ? 0xFF4A2E12 : PANEL_BG);
            g.fill(x, y, x + BOTTOM_W, y + 1, GuiKit.BRASS);
            g.centeredText(font, Component.translatable(
                "text." + HotPot.MOD_ID + ".config." + keys[i]), x + BOTTOM_W / 2, y + 6, TEXT);
            clickable(x, y, BOTTOM_W, BOTTOM_H, switch (i) {
                case 0 -> this::save;
                case 1 -> this::resetAll;
                default -> this::cancel;
            });
        }
    }

    // ------------------------------------------------------------ 操作

    private void save() {
        Config.Loaded loaded = Config.saveAndReload(edited);
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable(
                "text." + HotPot.MOD_ID + ".msg.config_reloaded", loaded.warnings().size()));
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void resetAll() {
        edited = Config.defaultsCopy();
        cachedRows = null;   // 换了副本，行要按默认值重新拼一遍
        applyParticle(100);
        if (slider != null) {
            slider.syncFromIntensity(100);
        }
        scroll = 0;
    }

    private void cancel() {
        applyParticle(entryParticle);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        // ESC 等同「取消」：不保存，粒子强度退回进界面之前的值
        cancel();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            if (grabScrollbar(mouseX, mouseY)) {
                return true;
            }
            for (int i = 0; i < clickRects.size(); i++) {
                int[] r = clickRects.get(i);
                if (inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    clickActions.get(i).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean grabScrollbar(double mouseX, double mouseY) {
        int x = px + 12 + (W - 24 - SCROLLBAR_W - 4) + 3;
        int innerHeight = Math.max(0, py + LIST_BOTTOM - listTop);
        if (rows().size() * ROW_H <= innerHeight || !inside(mouseX, mouseY, x, listTop, SCROLLBAR_W, innerHeight)) {
            return false;
        }
        draggingBar = true;
        dragGrab = mouseY - listTop;
        dragBar(mouseY);
        return true;
    }

    private void dragBar(double mouseY) {
        int innerHeight = Math.max(1, py + LIST_BOTTOM - listTop);
        int total = rows().size() * ROW_H;
        int thumbH = Math.max(12, innerHeight * innerHeight / Math.max(1, total));
        double span = Math.max(1, innerHeight - thumbH);
        double ratio = Math.max(0, Math.min(1, (mouseY - dragGrab - listTop) / span));
        scroll = (int) Math.round(ratio * Math.max(0, total - innerHeight));
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(), mouseY = event.y();
        if (draggingBar) {
            dragBar(mouseY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        draggingBar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - scrollY * ROW_H));
        }
        return true;
    }

    private int maxScroll() {
        int innerHeight = Math.max(0, py + LIST_BOTTOM - listTop);
        return Math.max(0, rows().size() * ROW_H - innerHeight);
    }

    private int clampScroll() {
        return Math.max(0, Math.min(scroll, maxScroll()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void clickable(int x, int y, int w, int h, Runnable action) {
        clickRects.add(new int[]{x, y, w, h});
        clickActions.add(action);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** 粒子强度滑块：0%～100%，0 时文案显示「已关闭」。 */
    private final class ParticleSlider extends AbstractSliderButton {
        ParticleSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), 1.0);
        }

        int percent() {
            return (int) Math.round(value * 100.0);
        }

        void syncFromIntensity(int percent) {
            value = Math.max(0, Math.min(100, percent)) / 100.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(percent() == 0
                ? Component.translatable("text." + HotPot.MOD_ID + ".config.particles.off")
                : Component.translatable("text." + HotPot.MOD_ID + ".config.particles.value", percent()));
        }

        @Override
        protected void applyValue() {
            applyParticle(percent());
        }
    }
}
