package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotFuel;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 游戏内可视化设置界面（r135e，2026-11-05 初版；r146a，2026-11-08 重做）。
 * <p>
 * mryt233 改（2026-11-08，r146a）：按任务单把原来四个页签的拥挤界面改成两块——
 * 「常用」只放玩家最常动的那几项（粒子强度 + 煤炭档灶火功率 + 涮煮兜底时间 + 三种主力汤底的加热倍率），
 * 「高级」把其余全部按原分类收进可折叠分组（默认收起）。面板宽度按屏幕自适应、每行只放一项、
 * 名字换行不截断、超出用滚动条与滚轮；每行 [- 数值 +]，Shift 点一次走 10 步，点数值可直接键入
 * （原版 EditBox，输错标红且不让保存），行尾「↺」只在当前值不等于默认值时出现。
 * <p>
 * 配置读写、校验、网络同步、服务端管理员限制全部照旧：界面只改自己手上的 {@link #edited} 副本，
 * 点「保存并生效」才一次性交给 {@link Config#saveAndReload}（写回文件 + 热重载）。
 * 粒子滑块仍按原要求实时生效。
 * 作者：mryt233
 */
public class HotPotConfigScreen extends Screen {
    // ------------------------------------------------------------ 版面（r146a：随屏幕尺寸算，不再写死面板大小）
    private static final int TAB_W = 100, TAB_H = 20, TAB_GAP = 4;
    private static final int BTN = 16, VALUE_W = 108, RESET_W = 14, CTRL_GAP = 2;
    private static final int ROW_MIN_H = 30, HEADER_H = 20, BOTTOM_H = 20, SCROLL_W = 5;
    private static final int TEXT = 0xFFE8C88A, MUTED = 0xFFA88C5C, HOVER = 0xFF6A4418,
        STRIPE = 0x18FFFFFF, PANEL_BG = 0xFF2A190C, BAD = 0xFFFF6A6A, CONFIRM = 0xFFFF9A5A;

    /** 数值单位：决定数值后面的单位字（秒 / 倍 / 点 / 级 / tick）。 */
    private enum Unit {
        TICK("tick"), MULTIPLIER("multiplier"), POINTS("point"), LEVEL("level"), PLAIN("");

        private final String key;

        Unit(String key) {
            this.key = key;
        }

        String langKey() {
            return "text." + HotPot.MOD_ID + ".config.unit." + key;
        }
    }

    /** 一个可调的数值项：配置路径、整数与否、范围、单步、单位、行名、悬停说明的文案键、行图标。 */
    private record Field(String[] path, boolean integer, double min, double max, double step, Unit unit,
                         Component label, String helpKey, ItemStack icon) {
        String key() {
            return String.join(".", path);
        }
    }

    /** 「高级」页里的一个可折叠分组。 */
    private record Section(String titleKey, List<Field> fields) {
    }

    /** 列表里的一行（数值项），行高按名字换行后的行数算。 */
    private record Row(Field field, List<FormattedCharSequence> lines, int height) {
    }

    /** 列表里的分组标题行。 */
    private record Header(String titleKey, int count, int height) {
    }

    // ------------------------------------------------------------ 状态
    private final Screen parent;
    private JsonObject edited;
    private int entryParticle = 100;
    private int tab;
    private final Set<String> expanded = new HashSet<>();
    /** 键入后仍然不合法的项：配置路径 → 玩家输入的原文（标红、拦保存，不改配置）。 */
    private final Map<String, String> bad = new LinkedHashMap<>();

    private int panelW, panelH, px, py, listTop, listBottom, listX, listW;
    private int scroll;
    private boolean draggingBar;
    private double dragGrab;
    private ParticleSlider slider;

    private EditBox editBox;
    private Field editField;
    private String typedEdit;
    private int editX, editY, editW, editH;

    private Field hoverField;
    /** 这一帧要显示的悬停提示（已经排好版）；普通按钮 / 快捷档用它直接出提示。 */
    private List<FormattedCharSequence> hoverTip;

    // mryt233 改（2026-11-10）：保存失败提示留在当前页，主菜单无玩家时也能读到原因。
    private Component saveNotice;
    private boolean confirmReset;
    private long confirmUntil;
    /** 这一帧「恢复全部默认」按钮在命中表里的下标（-1 = 没画）。 */
    private int resetRectIndex = -1;

    private final List<int[]> clickRects = new ArrayList<>();
    private final List<Runnable> clickActions = new ArrayList<>();

    // 缓存：列表布局、分组内容、默认值（每帧重算名字换行和范围太浪费）
    private List<Object> layoutCache;
    private int layoutTab = -1, layoutW = -1;
    private JsonObject layoutDoc;
    private Set<String> layoutExpand;
    private List<Section> sectionsCache;
    private JsonObject sectionsDoc;
    private JsonObject defaultsCache;
    private final Map<String, Double> defaultValueCache = new HashMap<>();

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
        panelW = Math.max(240, Math.min(640, width - 16));
        // mryt233 改（2026-11-09，r146a）：小窗口（GUI 高度 240）下也尽量把高度用满，列表能多露一行
        panelH = Math.max(200, Math.min(520, height - 8));
        px = Math.max(2, (width - panelW) / 2);
        py = Math.max(2, (height - panelH) / 2);
        listX = px + 10;
        listW = panelW - 20 - SCROLL_W - 4;
        applyTab();
    }

    /** 页签切换 / 进界面时的公共部分：滑块增删、列表上下边界、滚动复位。 */
    private void applyTab() {
        if (slider != null) {
            removeWidget(slider);
            slider = null;
        }
        commitEdit();   // 切页签时把正在键入的这一项先结算掉，别把输入丢了
        int contentTop = py + 48;
        if (tab == 0) {
            slider = new ParticleSlider(px + 10, contentTop, panelW - 20, 20);
            slider.syncFromIntensity(Config.particleIntensity());
            addRenderableWidget(slider);
            // mryt233 改（2026-11-09，r146a）：滑块 20 + 提示一行 12 + 快捷档一行 14，列表从 contentTop + 52 起。
            // 以前提示和快捷档挤在同一行，窄面板下提示会被截断（任务单要求「不截断成看不懂的残字」）。
            listTop = contentTop + 52;
        } else {
            listTop = contentTop;
        }
        listBottom = py + panelH - BOTTOM_H - 16;
        scroll = 0;
        confirmReset = false;
        hoverField = null;
        hoverTip = null;
    }

    // ------------------------------------------------------------ 语言键

    private static String k(String suffix) {
        return "text." + HotPot.MOD_ID + ".config." + suffix;
    }

    private static Component prop(String name) {
        return Component.translatable(k("prop." + name));
    }

    private static final Component DOT = Component.literal(" · ");

    private static Component soupName(SoupBase base) {
        return Component.translatable("text." + HotPot.MOD_ID + ".soup." + base.textureKey());
    }

    private static Component fuelName(String id) {
        return Component.translatable("text." + HotPot.MOD_ID + ".fuel." + id);
    }

    private static Component seasoningName(Seasoning seasoning) {
        return Component.translatable("text." + HotPot.MOD_ID + ".seasoning." + seasoning.id());
    }

    private static Component drinkName(DrinkKind kind) {
        return Component.translatable("item." + HotPot.MOD_ID + "." + kind.id());
    }

    // ------------------------------------------------------------ 数值项

    private Field field(String path, boolean integer, double min, double max, double step, Unit unit,
                        Component label, String helpProp) {
        return field(path, integer, min, max, step, unit, label, helpProp, ItemStack.EMPTY);
    }

    private Field field(String path, boolean integer, double min, double max, double step, Unit unit,
                        Component label, String helpProp, ItemStack icon) {
        return new Field(path.split("\\."), integer, min, max, step, unit, label, k("help." + helpProp), icon);
    }

    /** 常用页：除粒子强度（上面的滑块）以外的几项最常改的数值。 */
    private List<Field> commonFields() {
        List<Field> out = new ArrayList<>();
        // 煤炭档的灶火功率：原版煤炭 / 木炭都算这一档，是「整体涮煮快慢」的总闸
        out.add(field("fuel.coal.power", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            fuelName("coal").copy().append(DOT).append(prop("power")), "power"));
        // 没单独登记时间的食材（原版 / 第三方）按这个兜底时间涮熟
        out.add(field("cooking.fallback_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("fallback_ticks"), "fallback_ticks"));
        // 三种主力汤底的加热倍率（汤底本身决定涮煮快慢）
        out.add(field("soup_speed.spicy", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            soupName(SoupBase.SPICY).copy().append(DOT).append(prop("speed")), "speed"));
        out.add(field("soup_speed.clear", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            soupName(SoupBase.CLEAR).copy().append(DOT).append(prop("speed")), "speed"));
        out.add(field("soup_speed.rich_stock", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
            soupName(SoupBase.RICH_STOCK).copy().append(DOT).append(prop("speed")), "speed"));
        return out;
    }

    /** 高级页的全部分组，按原来的分类：涮煮时间 / 食材 / 汤底 / 燃料 / 汤碗 / 蘸料 / 饮料 / 食物营养。 */
    private List<Section> sections() {
        if (sectionsCache != null && sectionsDoc == edited) {
            return sectionsCache;
        }
        List<Section> out = new ArrayList<>();

        List<Field> cooking = new ArrayList<>();
        cooking.add(field("cooking.fallback_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("fallback_ticks"), "fallback_ticks"));
        cooking.add(field("cooking.fallback_xp", false, 0, 100, 0.05, Unit.POINTS,
            prop("fallback_xp"), "fallback_xp"));
        cooking.add(field("cooking.xp_per_item", false, 0, 100, 0.05, Unit.POINTS,
            prop("xp_per_item"), "xp_per_item"));
        cooking.add(field("cooking.tofu_cook_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("tofu_cook_ticks"), "tofu_cook_ticks"));
        cooking.add(field("cooking.tofu_overcook_ticks", true, 1, 100000, 10, Unit.TICK,
            prop("tofu_overcook_ticks"), "tofu_overcook_ticks"));
        out.add(new Section(k("group.cooking"), cooking));

        // 本模组登记过涮煮时间的生食材（cooking.items.<物品注册路径>）
        List<Field> items = new ArrayList<>();
        JsonObject itemsNode = node(edited, new String[]{"cooking", "items"}, 2);
        for (String id : List.copyOf(itemsNode.keySet())) {
            ItemStack icon = stackOf(id);
            Component name = icon.isEmpty() ? Component.literal(id) : icon.getHoverName();
            items.add(field("cooking.items." + id, true, 1, 100000, 20, Unit.TICK,
                name.copy().append(DOT).append(prop("cook_ticks")), "cook_ticks", icon));
        }
        out.add(new Section(k("group.items"), items));

        List<Field> soup = new ArrayList<>();
        for (SoupBase base : SoupBase.values()) {
            soup.add(field("soup_speed." + base.textureKey(), false, 0.1, 10, 0.05, Unit.MULTIPLIER,
                soupName(base).copy().append(DOT).append(prop("speed")), "speed"));
        }
        out.add(new Section(k("group.soup"), soup));

        List<Field> fuel = new ArrayList<>();
        for (HotPotFuel grade : HotPotFuel.values()) {
            Component name = fuelName(grade.configKey());
            fuel.add(field("fuel." + grade.configKey() + ".power", false, 0.1, 10, 0.05, Unit.MULTIPLIER,
                name.copy().append(DOT).append(prop("power")), "power"));
            fuel.add(field("fuel." + grade.configKey() + ".burn_ticks", true, 0, 100000, 200, Unit.TICK,
                name.copy().append(DOT).append(prop("burn_ticks")), "burn_ticks"));
        }
        out.add(new Section(k("group.fuel"), fuel));

        List<Field> bowl = new ArrayList<>();
        for (SoupBase base : SoupBase.values()) {
            for (String key : new String[]{"hunger", "saturation", "duration", "amplifier",
                "second_duration", "second_amplifier", "fire_duration", "fire_amplifier",
                "hunger_duration", "hunger_amplifier"}) {
                bowl.add(bowlField(base, key));
            }
        }
        out.add(new Section(k("group.bowl"), bowl));

        List<Field> seasoning = new ArrayList<>();
        for (Seasoning s : Seasoning.ALL) {
            Component name = seasoningName(s);
            if (s == Seasoning.MSG) {
                // 味精只读 hunger（额外饥饿点数），另外两个键对它没有意义
                seasoning.add(field("seasoning.msg.hunger", true, 0, 100, 1, Unit.POINTS,
                    name.copy().append(DOT).append(prop("hunger")), "hunger"));
            } else {
                seasoning.add(field("seasoning." + s.id() + ".duration", true, 0, 100000, 20, Unit.TICK,
                    name.copy().append(DOT).append(prop("duration")), "duration"));
                seasoning.add(field("seasoning." + s.id() + ".amplifier", true, 0, 9, 1, Unit.LEVEL,
                    name.copy().append(DOT).append(prop("amplifier")), "amplifier"));
            }
        }
        out.add(new Section(k("group.seasoning"), seasoning));

        List<Field> drink = new ArrayList<>();
        drink.add(field("drink.sip.hunger", true, 0, 100, 1, Unit.POINTS,
            prop("sip_hunger"), "sip_hunger"));
        drink.add(field("drink.sip.saturation", false, 0, 10, 0.05, Unit.PLAIN,
            prop("sip_saturation"), "sip_saturation"));
        for (DrinkKind kind : DrinkKind.values()) {
            if (kind == DrinkKind.WATER || kind == DrinkKind.MILK) {
                continue;
            }
            Component name = drinkName(kind);
            drink.add(field("drink." + kind.id() + ".duration", true, 0, 100000, 20, Unit.TICK,
                name.copy().append(DOT).append(prop("duration")), "duration"));
            drink.add(field("drink." + kind.id() + ".amplifier", true, 0, 9, 1, Unit.LEVEL,
                name.copy().append(DOT).append(prop("amplifier")), "amplifier"));
        }
        out.add(new Section(k("group.drink"), drink));

        List<Field> food = new ArrayList<>();
        JsonObject foodNode = node(edited, new String[]{"food"}, 1);
        for (String id : List.copyOf(foodNode.keySet())) {
            ItemStack icon = stackOf(id);
            Component name = icon.isEmpty() ? Component.literal(id) : icon.getHoverName();
            food.add(field("food." + id + ".hunger", true, 0, 100, 1, Unit.POINTS,
                name.copy().append(DOT).append(prop("hunger")), "hunger", icon));
            food.add(field("food." + id + ".saturation", false, 0, 10, 0.05, Unit.PLAIN,
                name.copy().append(DOT).append(prop("saturation")), "saturation", icon));
        }
        out.add(new Section(k("group.food"), food));

        sectionsCache = out;
        sectionsDoc = edited;
        return out;
    }

    /** 汤碗一项：路径 soup_bowl.&lt;汤底&gt;.&lt;键&gt;，范围和单位按后缀分。 */
    private Field bowlField(SoupBase base, String key) {
        Component label = soupName(base).copy().append(DOT).append(prop(key));
        String[] path = {"soup_bowl", base.textureKey(), key};
        return switch (key) {
            case "hunger" -> new Field(path, true, 0, 100, 1, Unit.POINTS, label, k("help.hunger"), ItemStack.EMPTY);
            case "saturation" -> new Field(path, false, 0, 10, 0.05, Unit.PLAIN, label,
                k("help.saturation"), ItemStack.EMPTY);
            case "amplifier", "second_amplifier", "fire_amplifier", "hunger_amplifier" ->
                new Field(path, true, 0, 9, 1, Unit.LEVEL, label, k("help.amplifier"), ItemStack.EMPTY);
            default -> new Field(path, true, 0, 100000, 20, Unit.TICK, label, k("help.duration"), ItemStack.EMPTY);
        };
    }

    // ------------------------------------------------------------ 数值读写

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

    /** 默认值从配置模板取（不在这里另写一份，改默认值只要改 rememberXxx 那一处）。 */
    private double defaultValue(Field f) {
        Double cached = defaultValueCache.get(f.key());
        if (cached != null) {
            return cached;
        }
        if (defaultsCache == null) {
            defaultsCache = Config.defaultsCopy();
        }
        JsonObject parentNode = node(defaultsCache, f.path(), f.path().length - 1);
        JsonElement el = parentNode.get(f.path()[f.path().length - 1]);
        double v = el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()
            ? el.getAsDouble() : f.min();
        defaultValueCache.put(f.key(), v);
        return v;
    }

    private boolean isDefault(Field f) {
        return Math.abs(value(f) - defaultValue(f)) < 0.0005;
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

    // ------------------------------------------------------------ 列表布局

    private void clearLayout() {
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
    private int valueWidth() {
        int fixed = 2 * BTN + 3 * CTRL_GAP + RESET_W;   // 两个步进按钮 + 三个间距 + ↺
        // mryt233 改（2026-11-09，r146a）：名字区至少留 96 像素（原来只留 80），否则「煤炭 · 灶火功率」这种
        // 十个字左右的名字会一路顶到步进按钮左边；宽面板上仍按原宽度，不受影响。
        return Math.max(56, Math.min(VALUE_W, listW - 10 - 96 - fixed));
    }

    /** 名字区宽度：控件从左到右是「− 数值 ＋ ↺」，名字用剩下的宽度。 */
    private int labelAreaWidth() {
        int controls = BTN + CTRL_GAP + valueWidth() + CTRL_GAP + BTN + CTRL_GAP + RESET_W;
        return Math.max(60, listW - 10 - controls);
    }

    private List<FormattedCharSequence> wrap(Component label, int maxW) {
        if (font.width(label) <= maxW) {
            return List.of(label.getVisualOrderText());
        }
        return font.split(label, maxW);
    }

    /** 当前页签的完整列表：分组标题 + （展开时的）数值行；行高按名字换行算。 */
    private List<Object> layout() {
        if (layoutCache != null && layoutTab == tab && layoutW == panelW && layoutDoc == edited
            && layoutExpand != null && layoutExpand.equals(expanded)) {
            return layoutCache;
        }
        int labelW = labelAreaWidth();
        List<Object> out = new ArrayList<>();
        if (tab == 0) {
            for (Field f : commonFields()) {
                out.add(row(f, labelW));
            }
        } else {
            for (Section section : sections()) {
                out.add(new Header(section.titleKey(), section.fields().size(), HEADER_H));
                if (!expanded.contains(section.titleKey())) {
                    continue;
                }
                for (Field f : section.fields()) {
                    out.add(row(f, labelW));
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

    private Row row(Field f, int labelW) {
        List<FormattedCharSequence> lines = wrap(f.label(), labelW);
        int h = Math.max(ROW_MIN_H, lines.size() * 10 + 10);
        return new Row(f, lines, h);
    }

    private static int rowHeight(Object element) {
        return element instanceof Header header ? header.height() : ((Row) element).height();
    }

    private int contentHeight() {
        int total = 4;
        for (Object element : layout()) {
            total += rowHeight(element);
        }
        return total;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - Math.max(1, listBottom - listTop));
    }

    // ------------------------------------------------------------ 画

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
        clickRects.clear();
        clickActions.clear();
        resetRectIndex = -1;
        hoverField = null;
        hoverTip = null;
        if (confirmReset && net.minecraft.Util.getMillis() > confirmUntil) {
            confirmReset = false;
        }
        // GuiKit.frame 以 (0,0) 为左上角，这里靠 pose 平移到面板位置
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        GuiKit.frame(g, panelW, panelH, 1, true);
        g.pose().popPose();
        g.drawString(font, title, px + 12, py + 7, GuiKit.TITLE, true);
        g.fill(px + 12, py + 19, px + panelW - 12, py + 20, GuiKit.BRASS_DARK);
        drawTabs(g, mouseX, mouseY);
        if (tab == 0) {
            drawParticleBlock(g, mouseX, mouseY);
        }
        List<FormattedCharSequence> noticeLines = saveNotice == null ? List.of() : font.split(saveNotice, panelW - 24);
        listBottom = py + panelH - BOTTOM_H - 16 - noticeLines.size() * 10;
        drawList(g, mouseX, mouseY);
        for (int i = 0; i < noticeLines.size(); i++) {
            g.drawString(font, noticeLines.get(i), px + 12, listBottom + 3 + i * 10, BAD, true);
        }
        drawBottomBar(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        // 编辑框自己画（不挂进 renderables，免得被列表和滚动区盖住）
        if (editBox != null) {
            boolean ok = parse(editField, typedEdit) != null;
            g.fill(editX - 1, editY - 1, editX + editW + 1, editY + editH + 1, ok ? GuiKit.BRASS : BAD);
            g.fill(editX, editY, editX + editW, editY + editH, 0xFF1A0E05);
            editBox.render(g, mouseX, mouseY, delta);
        }
        if (hoverTip != null) {
            g.renderTooltip(font, hoverTip, mouseX, mouseY);
        } else if (hoverField != null) {
            g.renderTooltip(font, tooltipFor(hoverField), mouseX, mouseY);
        }
    }

    /** 悬停说明：一句白话（管什么）+ 一行范围 / 默认值（= 配置文件的 _字段说明）。 */
    private List<FormattedCharSequence> tooltipFor(Field f) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(f.label().copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
        lines.addAll(font.split(Component.translatable(f.helpKey()).withStyle(ChatFormatting.GRAY), 200));
        lines.add(Component.translatable(k("tooltip.range"),
                formatValue(f, f.min()), formatValue(f, f.max()), formatValue(f, defaultValue(f)))
            .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        if (bad.containsKey(f.key())) {
            lines.add(Component.translatable(k("msg.invalid_line")).withStyle(ChatFormatting.RED).getVisualOrderText());
        }
        return lines;
    }

    /** 一行文字的提示（快捷档 / 编辑框 / ↺ 这些自绘小按钮用）。 */
    private static List<FormattedCharSequence> tip(String key) {
        return List.of(Component.translatable(key).getVisualOrderText());
    }

    private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
        String[] keys = {"common", "advanced"};
        int startX = px + (panelW - (TAB_W * 2 + TAB_GAP)) / 2;
        for (int i = 0; i < 2; i++) {
            int x = startX + i * (TAB_W + TAB_GAP);
            int y = py + 24;
            boolean hover = inside(mouseX, mouseY, x, y, TAB_W, TAB_H);
            boolean active = i == tab;
            g.fill(x, y, x + TAB_W, y + TAB_H, active ? HOVER : (hover ? 0xFF4A2E12 : PANEL_BG));
            g.fill(x, y, x + TAB_W, y + 1, active ? GuiKit.BRASS_HI : GuiKit.BRASS);
            if (active) {
                g.fill(x, y + TAB_H - 2, x + TAB_W, y + TAB_H, GuiKit.BRASS_HI);
            }
            drawFitted(g, Component.translatable(k("tab." + keys[i])), x + TAB_W / 2, y + 6, TAB_W - 8,
                active ? GuiKit.TITLE : TEXT);
            final int target = i;
            clickable(x, y, TAB_W, TAB_H, () -> {
                if (tab == target) {
                    return;
                }
                tab = target;
                clearLayout();
                applyTab();
            });
        }
    }

    /** 居中画一行字；宽度超过 max 就等比缩小，不截断成看不懂的残字。 */
    private void drawFitted(GuiGraphics g, Component text, int centerX, int y, int max, int color) {
        int w = font.width(text);
        if (w <= max) {
            g.drawCenteredString(font, text, centerX, y, color);
            return;
        }
        float scale = Math.max(0.6F, max / (float) w);
        g.pose().pushPose();
        g.pose().translate(centerX, y + 4, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawCenteredString(font, text, 0, -4, color);
        g.pose().popPose();
    }

    /** 粒子强度：滑块 + 一行白话提示 + 右侧三个快捷档（0 / 50 / 100%）。 */
    private void drawParticleBlock(GuiGraphics g, int mouseX, int mouseY) {
        // mryt233 改（2026-11-09，r146a）：提示独占一行、给满宽度（不再被快捷档挤到截断）；
        // 三个快捷档放到下面一行，右对齐。
        int hintY = py + 48 + 22;
        Component hint = Component.translatable(k("particles.hint"));
        g.drawString(font, hint, px + 12, hintY, MUTED, false);
        int qw = Math.max(36, Math.min(64, (panelW - 24) / 6));
        int[] percents = {0, 50, 100};
        int totalW = percents.length * qw + (percents.length - 1) * 4;
        int x0 = px + panelW - 10 - totalW;
        int quickY = hintY + 13;
        for (int i = 0; i < percents.length; i++) {
            int x = x0 + i * (qw + 4);
            int y = quickY;
            boolean hover = inside(mouseX, mouseY, x, y, qw, 12);
            boolean chosen = Config.particleIntensity() == percents[i];
            g.fill(x, y, x + qw, y + 12, chosen ? HOVER : (hover ? 0xFF4A2E12 : PANEL_BG));
            g.fill(x, y, x + qw, y + 1, GuiKit.BRASS);
            g.drawCenteredString(font, Component.literal(percents[i] + "%"), x + qw / 2, y + 2,
                chosen ? GuiKit.TITLE : TEXT);
            final int percent = percents[i];
            clickable(x, y, qw, 12, () -> {
                applyParticle(percent);
                if (slider != null) {
                    slider.syncFromIntensity(percent);
                }
            });
            if (hover) {
                hoverTip = tip(k("common.quick"));
            }
        }
    }

    private void drawList(GuiGraphics g, int mouseX, int mouseY) {
        List<Object> elements = layout();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int total = contentHeight();
        int view = Math.max(1, listBottom - listTop);
        int y = listTop - scroll;
        g.enableScissor(listX, listTop, listX + listW + SCROLL_W + 4, listBottom);
        for (Object element : elements) {
            int h = rowHeight(element);
            if (y + h > listTop && y < listBottom) {
                if (element instanceof Header header) {
                    drawHeader(g, header, y, h, mouseX, mouseY);
                } else {
                    drawRow(g, (Row) element, y, h, mouseX, mouseY);
                }
            }
            y += h;
            if (y > listBottom) {
                break;
            }
        }
        g.disableScissor();
        if (total > view) {
            drawScrollbar(g, listX + listW + 3, listTop, view, total);
        }
    }

    private void drawHeader(GuiGraphics g, Header header, int y, int h, int mouseX, int mouseY) {
        boolean open = expanded.contains(header.titleKey());
        boolean hover = inside(mouseX, mouseY, listX, y, listW, h);
        g.fill(listX, y, listX + listW, y + h - 1, hover ? 0x30B47A1C : 0x204A2E12);
        g.fill(listX, y + h - 1, listX + listW, y + h, GuiKit.BRASS_DARK);
        String mark = open ? "\u25BE " : "\u25B8 ";
        g.drawString(font, mark + Component.translatable(header.titleKey()).getString()
            + "  (" + header.count() + ")", listX + 6, y + (h - 8) / 2, GuiKit.TITLE, true);
        clickableInList(listX, y, listW, h, () -> {
            if (!expanded.remove(header.titleKey())) {
                expanded.add(header.titleKey());
            }
            clearLayout();
        });
    }

    private void drawRow(GuiGraphics g, Row row, int y, int h, int mouseX, int mouseY) {
        Field f = row.field();
        boolean hover = inside(mouseX, mouseY, listX, y, listW, h);
        g.fill(listX, y, listX + listW, y + h - 1, hover ? 0x30FFFFFF : STRIPE);
        int labelX = listX + 6;
        if (!f.icon().isEmpty()) {
            g.renderItem(f.icon(), listX + 3, y + (h - 16) / 2);
            labelX = listX + 24;
        }
        int textY = y + (h - row.lines().size() * 10) / 2;
        for (FormattedCharSequence line : row.lines()) {
            g.drawString(font, line, labelX, textY, TEXT, false);
            textY += 10;
        }
        if (hover) {
            hoverField = f;
        }
        drawFieldControls(g, f, y, h, mouseX, mouseY);
    }

    /** 一行的控件：[-  数值  +  ↺]，纵向居中。 */
    private void drawFieldControls(GuiGraphics g, Field f, int y, int h, int mouseX, int mouseY) {
        int vw = valueWidth();
        int resetX = listX + listW - 6 - RESET_W;
        int plusX = resetX - CTRL_GAP - BTN;
        int valueX = plusX - CTRL_GAP - vw;
        int minusX = valueX - CTRL_GAP - BTN;
        int by = y + (h - BTN) / 2;
        drawButton(g, minusX, by, "-", inside(mouseX, mouseY, minusX, by, BTN, BTN));
        clickableInList(minusX, by, BTN, BTN, () -> nudge(f, -1));
        drawButton(g, plusX, by, "+", inside(mouseX, mouseY, plusX, by, BTN, BTN));
        clickableInList(plusX, by, BTN, BTN, () -> nudge(f, 1));
        drawValueBox(g, f, valueX, y + (h - 16) / 2, vw, mouseX, mouseY);
        // 只在当前值和默认值不同的时候才出「↺」
        if (!isDefault(f)) {
            boolean hover = inside(mouseX, mouseY, resetX, by, RESET_W, BTN);
            g.fill(resetX, by, resetX + RESET_W, by + BTN, hover ? HOVER : PANEL_BG);
            g.fill(resetX, by, resetX + RESET_W, by + 1, GuiKit.BRASS);
            g.drawCenteredString(font, Component.literal("\u21BA"), resetX + RESET_W / 2, by + 3,
                hover ? GuiKit.TITLE : MUTED);
            clickableInList(resetX, by, RESET_W, BTN, () -> {
                setValue(f, defaultValue(f));
                bad.remove(f.key());
            });
            if (hover) {
                hoverTip = tip(k("reset_one"));
            }
        }
    }

    private void drawButton(GuiGraphics g, int x, int y, String label, boolean hover) {
        g.fill(x, y, x + BTN, y + BTN, hover ? HOVER : PANEL_BG);
        g.fill(x, y, x + BTN, y + 1, GuiKit.BRASS);
        g.drawCenteredString(font, Component.literal(label), x + BTN / 2, y + 4, hover ? GuiKit.TITLE : TEXT);
    }

    private void drawValueBox(GuiGraphics g, Field f, int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, hover ? 0xFF3A2410 : 0xFF1A0E05);
        g.fill(x, y, x + w, y + 1, hover ? GuiKit.BRASS_HI : GuiKit.BRASS_DARK);
        if (editBox != null && editField == f) {
            return;   // 正在键入：EditBox 自己画
        }
        String typedBad = bad.get(f.key());
        if (typedBad != null) {
            g.drawCenteredString(font, font.plainSubstrByWidth(typedBad, w - 4), x + w / 2, y + 4, BAD);
            return;
        }
        g.drawCenteredString(font, valueText(f, value(f), w), x + w / 2, y + 4, GuiKit.RIVET);
        if (hover) {
            hoverTip = tip(k("edit.hint"));
        }
        clickableInList(x, y, w, 16, () -> startEdit(f, x, y, w, 16));
    }

    /** 数值 + 单位；tick 类的再折算成秒（放得下才显示，免得挤）。 */
    private Component valueText(Field f, double v, int w) {
        String num = formatValue(f, v);
        if (f.unit() == Unit.PLAIN) {
            return Component.literal(num);
        }
        MutableComponent text = Component.literal(num + " ").append(Component.translatable(f.unit().langKey()));
        if (f.unit() == Unit.TICK && v >= 20) {
            MutableComponent withSeconds = text.copy()
                .append(Component.literal(" ("
                    + String.format(Locale.ROOT, "%.1f", v / 20.0) + " ")
                    .withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(k("unit.seconds")).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
            if (font.width(withSeconds) <= w - 6) {
                return withSeconds;
            }
        }
        return text;
    }

    private void drawScrollbar(GuiGraphics g, int x, int top, int view, int total) {
        g.fill(x, top, x + SCROLL_W, top + view, 0x40FFFFFF);
        int thumbH = Math.max(12, view * view / Math.max(1, total));
        int max = Math.max(1, total - view);
        int thumbY = top + (view - thumbH) * scroll / max;
        g.fill(x, thumbY, x + SCROLL_W, thumbY + thumbH, GuiKit.BRASS);
    }

    private void drawBottomBar(GuiGraphics g, int mouseX, int mouseY) {
        String[] keys = {"save", "reset", "cancel"};
        int gap = 6;
        int btnW = Math.max(70, Math.min(150, (panelW - 24 - gap * 2) / 3));
        int startX = px + (panelW - (btnW * 3 + gap * 2)) / 2;
        int y = py + panelH - BOTTOM_H - 8;
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (btnW + gap);
            boolean hover = inside(mouseX, mouseY, x, y, btnW, BOTTOM_H);
            boolean confirming = i == 1 && confirmReset;
            g.fill(x, y, x + btnW, y + BOTTOM_H, confirming ? 0x60B47A1C : (hover ? 0xFF4A2E12 : PANEL_BG));
            g.fill(x, y, x + btnW, y + 1, confirming ? CONFIRM : GuiKit.BRASS);
            drawFitted(g, Component.translatable(k(confirming ? "reset.confirm" : keys[i])),
                x + btnW / 2, y + 6, btnW - 6, confirming ? CONFIRM : TEXT);
            if (i == 1) {
                resetRectIndex = clickRects.size();
            }
            clickable(x, y, btnW, BOTTOM_H, switch (i) {
                case 0 -> this::save;
                case 1 -> this::resetAllClicked;
                default -> this::cancel;
            });
        }
    }

    // ------------------------------------------------------------ 编辑与操作

    private void nudge(Field f, int dir) {
        double step = f.step() * (hasShiftDown() ? 10 : 1) * dir;
        setValue(f, value(f) + step);
        bad.remove(f.key());
    }

    private void startEdit(Field f, int x, int y, int w, int h) {
        commitEdit();
        editField = f;
        editX = x;
        editY = y;
        editW = w;
        editH = h;
        String current = bad.getOrDefault(f.key(), formatValue(f, value(f)));
        editBox = new EditBox(font, x + 3, y + 2, w - 6, 12, Component.empty());
        editBox.setMaxLength(24);
        editBox.setBordered(false);
        editBox.setTextColor(TEXT);
        editBox.setValue(current);
        editBox.setFocused(true);
        editBox.setResponder(this::onTyped);
        typedEdit = current;
        onTyped(current);
    }

    private void onTyped(String text) {
        typedEdit = text;
        if (editField != null && editBox != null) {
            editBox.setTextColor(parse(editField, text) == null ? BAD : TEXT);
        }
    }

    /** 回车 / 点到别处：合法就写进副本，不合法就标红留着（不改配置，也不让保存）。 */
    private void commitEdit() {
        if (editBox == null || editField == null) {
            clearEdit();
            return;
        }
        Field f = editField;
        String text = typedEdit != null ? typedEdit : editBox.getValue();
        text = text.trim();
        if (text.isEmpty()) {
            bad.remove(f.key());
            clearEdit();
            return;
        }
        Double parsed = parse(f, text);
        if (parsed == null) {
            bad.put(f.key(), text);
        } else {
            bad.remove(f.key());
            setValue(f, parsed);
        }
        clearEdit();
    }

    /** Esc：撤掉这一项的输入，回到配置里原来的值。 */
    private void cancelEdit() {
        if (editField != null) {
            bad.remove(editField.key());
        }
        clearEdit();
    }

    private void clearEdit() {
        editBox = null;
        editField = null;
        typedEdit = null;
    }

    /** 不是数字 / 不是整数 / 超出范围都算不合法。 */
    private static Double parse(Field f, String text) {
        if (f == null || text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(trimmed);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            if (f.integer() && Math.abs(v - Math.rint(v)) > 1e-6) {
                return null;
            }
            if (v < f.min() - 1e-6 || v > f.max() + 1e-6) {
                return null;
            }
            return v;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /** 「恢复全部默认」点一次变确认态，再点一次才真的恢复（4 秒内有效）。 */
    private void resetAllClicked() {
        long now = net.minecraft.Util.getMillis();
        if (confirmReset && now <= confirmUntil) {
            confirmReset = false;
            resetAll();
            return;
        }
        confirmReset = true;
        confirmUntil = now + 4000L;
    }

    private void resetAll() {
        edited = Config.defaultsCopy();
        bad.clear();
        defaultValueCache.clear();
        clearLayout();
        sectionsCache = null;
        applyParticle(100);
        if (slider != null) {
            slider.syncFromIntensity(100);
        }
        scroll = 0;
    }

    private void save() {
        commitEdit();
        if (!bad.isEmpty()) {
            message(Component.translatable(k("msg.invalid")));
            return;
        }
        Config.Loaded loaded = Config.saveAndReload(edited);
        if (loaded.saveStatus() == cn.blockforge.orient.framework.pure.ConfigStore.SaveStatus.FAILED) {
            saveNotice = Component.translatable(k("msg.save_failed"), loaded.reason());
            return;
        }
        entryParticle = Config.particleIntensity();
        if (loaded.saveStatus() == cn.blockforge.orient.framework.pure.ConfigStore.SaveStatus.MEMORY_ONLY) {
            saveNotice = Component.translatable(k("msg.memory_only"), loaded.reason());
            if (loaded.backup() != null) {
                saveNotice = saveNotice.copy().append(" ").append(Component.translatable(k("msg.broken_backup"), loaded.backup().getFileName().toString()));
            }
            return;
        }
        if (loaded.backup() != null) {
            saveNotice = Component.translatable(k("msg.broken_backup"), loaded.backup().getFileName().toString());
            return;
        }
        saveNotice = null;
        message(Component.translatable("text." + HotPot.MOD_ID + ".msg.config_reloaded", loaded.warnings().size()));
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void cancel() {
        clearEdit();
        applyParticle(entryParticle);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void message(Component text) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(text, true);
        }
    }

    /** 粒子强度：拖动滑块立刻生效，同时写进待保存的副本。 */
    private void applyParticle(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        Config.setParticleIntensity(p);
        node(edited, new String[]{"client"}, 1).addProperty("particle_intensity", p);
    }

    @Override
    public void onClose() {
        // ESC 等同「取消」：不保存，粒子强度退回进界面之前的值
        cancel();
    }

    // ------------------------------------------------------------ 鼠标 / 键盘

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editBox != null) {
            if (inside(mouseX, mouseY, editX, editY, editW, editH)) {
                editBox.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            commitEdit();
        }
        if (button == 0) {
            if (grabScrollbar(mouseX, mouseY)) {
                return true;
            }
            for (int i = 0; i < clickRects.size(); i++) {
                int[] r = clickRects.get(i);
                if (inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    // 点到别的地方就撤掉「恢复全部默认」的确认态，点它自己要留给它自己判断
                    if (i != resetRectIndex) {
                        confirmReset = false;
                    }
                    clickActions.get(i).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean grabScrollbar(double mouseX, double mouseY) {
        if (contentHeight() <= listBottom - listTop) {
            return false;
        }
        int x = listX + listW + 3;
        if (!inside(mouseX, mouseY, x, listTop, SCROLL_W, listBottom - listTop)) {
            return false;
        }
        draggingBar = true;
        dragGrab = mouseY - listTop;
        dragBar(mouseY);
        return true;
    }

    private void dragBar(double mouseY) {
        int view = Math.max(1, listBottom - listTop);
        int total = contentHeight();
        int thumbH = Math.max(12, view * view / Math.max(1, total));
        double span = Math.max(1, view - thumbH);
        double ratio = Math.max(0, Math.min(1, (mouseY - dragGrab - listTop) / span));
        scroll = (int) Math.round(ratio * Math.max(0, total - view));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            dragBar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        commitEdit();   // 滚动时先把正在键入的这一项结算掉，免得输入框停在别的行上
        int max = maxScroll();
        if (max > 0) {
            scroll = (int) Math.max(0, Math.min(max, scroll - scrollY * 24));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editBox != null) {
            if (keyCode == 257 || keyCode == 335) {   // 回车 / 小键盘回车：确认这一项的输入
                commitEdit();
                return true;
            }
            if (keyCode == 256) {                     // Esc：撤掉输入，别把整个界面关掉
                cancelEdit();
                return true;
            }
            editBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editBox != null) {
            return editBox.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    /**
     * mryt233 改（2026-11-09，r146a）：开发用探针（devtest/ClientProbe）截图时切页签 / 展开分组用。
     * 只在 {@code -Dhotpot.clientProbe=true} 的开发客户端里由 /hpt 探针调用，正式玩法没有任何入口。
     * 接受的写法：{@code advanced} / {@code common} / {@code expand:0}（按高级页分组下标展开或收起）。
     */
    public void probeAction(String action) {
        if ("advanced".equals(action)) {
            tab = 1;
            clearLayout();
            applyTab();
        } else if ("common".equals(action)) {
            tab = 0;
            clearLayout();
            applyTab();
        } else if (action.startsWith("expand:")) {
            int index = Integer.parseInt(action.substring("expand:".length()));
            List<Section> all = sections();
            if (index >= 0 && index < all.size()) {
                String key = all.get(index).titleKey();
                if (!expanded.remove(key)) {
                    expanded.add(key);
                }
                clearLayout();
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ 小工具

    private void clickable(int x, int y, int w, int h, Runnable action) {
        addClick(x, y, w, h, action);
    }

    /**
     * 列表里的控件：只有整块都落在可见的列表区域里才收进命中表。
     * 否则滚到一半的行会有一半贴在列表外面，和上方的快捷档 / 下方的按钮抢鼠标。
     */
    private void clickableInList(int x, int y, int w, int h, Runnable action) {
        if (y < listTop || y + h > listBottom) {
            return;
        }
        addClick(x, y, w, h, action);
    }

    private void addClick(int x, int y, int w, int h, Runnable action) {
        clickRects.add(new int[]{x, y, w, h});
        clickActions.add(action);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static ItemStack stackOf(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(HotPot.id(id)).orElse(Items.BARRIER);
        return item == Items.BARRIER ? ItemStack.EMPTY : new ItemStack(item);
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
