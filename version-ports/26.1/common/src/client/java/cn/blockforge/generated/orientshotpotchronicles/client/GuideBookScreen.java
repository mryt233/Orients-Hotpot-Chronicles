package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 「火锅指南」界面（2026-10-25 用户要游戏内教程书；2026-10-26 用户：照真的指南做——摊开的两页书，左边一列分类书签、
 * 顶上一排条目书签，鼠标放上去书签伸出来、显示名字，点一下翻过去；要有立体感，书皮红色）。
 * mryt233 改（2026-11-05）：书里的内容改成和网页端书本教程一模一样（tools/gen_guide_book.py 直接从
 * tutorial/content 生成），一个条目里有小标题、段落、表格、提示框、列表、一排图标和配方，按源文件顺序
 * 从左页流到右页、放不下就翻到下一个跨页。
 * mryt233 改（2026-11-07）：顶部条目固定间距横向滚动，右侧指南针搜索全文；设置从模组列表进入。
 *
 * 内容来自 assets/orients_hotpot_chronicles/guide/&lt;语言&gt;.json：
 *   {"categories": [{"id", "icon", "title", "entries": [{"icon", "title", "blocks": [...]}, ...]}]}
 * 每个条目是一串按顺序排的块（k = sub / p / note / th / tr / bul / num / icons / recipes / code / rule），
 * 带 go 的行点了能跳到别的条目（本章目录）；旧版的 text / rows / recipes / steps / tip 字段仍然认。
 * 配方几个就轮流显示（2 秒一个），原料有几种可选的每秒轮换；指着物品显示物品提示。
 * 简体 / 繁体 / 英文，其他语言用英文；贴图 textures/gui/guide_book.png（tools/gen_guide_gui.py）。
 * 查看器 viewer/screens.js 里有同样的网页版。作者：mryt233 */
public class GuideBookScreen extends Screen {
    private static final Identifier TEX = HotPot.id("textures/gui/guide_book.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int BOOK_W = 272, BOOK_H = 180;
    /** 两页文字区：左页 x 16、右页 x 144（相对书的左上角），宽 112，从 y 12 起高 144。 */
    private static final int LEFT_X = 16, RIGHT_X = 144, COL_W = 112, COL_Y = 12, COL_H = 144;
    private static final int TEXT = 0xFF2A1A0E, HEADING = 0xFF5A2A10, MUTED = 0xFF7A6040, TIP = 0xFF2E5E1E,
        WARN = 0xFF8A3A12, NUMBER = 0xFF1E6E78, RULE = 0x40603820, CODE_BG = 0x18603820;
    private static final int RECIPE_H = 66;
    /** 书签贴图：左边 28×24，顶上 24×24（和 gen_guide_gui.py 一致）。 */
    private static final int CAT_W = 28, TAB_H = 24, ENTRY_W = 24;
    private static final int ENTRY_STEP = 26;
    private static final int SEARCH_W = 236, SEARCH_H = 148, SEARCH_ROW_H = 25, SEARCH_LIST_H = 100;
    /** 一排物品图标：每行 6 个，格距 18。 */
    private static final int ICON_STEP = 18, ICON_PER_ROW = 6;

    /** 记住上次看到哪（关了再打开还在那一页）。 */
    private static int lastCat, lastEntry;
    /** 读过的书（完整教程不小，改窗口大小时 init 会重跑，别每次都解析一遍）。 */
    private static String cachedLang;
    private static JsonObject cachedBook;
    /** 产物物品 → 做出它的配方；配方表换了（/reload、换世界）就整份重建。mryt233 改：每帧查表改成一次 HashMap.get。 */
    private Object recipeCacheOwner;
    /**
     * 建缓存时 {@code RecipeManager.getRecipes()} 返回的那个集合实例。
     * mryt233 改：客户端收到配方同步走的是 {@code RecipeManager.replaceRecipes}（原地换内容、实例不变），
     * 只比 RecipeManager 实例的话 /reload 之后缓存永远不刷新；{@code getRecipes()} 背后是 ImmutableMap 的
     * values（每次重建配方表都是新 map、新 view 实例），用它当「配方表换代了」的 O(1) 判据。
     */
    private Object recipeCacheKey;
    private Map<Item, RecipeHolder<? extends Recipe<?>>> recipeCache;

    private enum Kind { HEADER, TEXT, SUB, NOTE, TH, ROW, BUL, NUM, ICONS, RECIPE, CODE, RULE, GAP }

    /** 排版后的一块：画什么、占多高、点了跳哪。 */
    private static final class Elem {
        final Kind kind;
        final int height;
        List<FormattedCharSequence> lines = List.of();
        int color = TEXT;
        ItemStack icon = ItemStack.EMPTY;
        String number = "";
        List<Identifier> recipes = List.of();
        List<ItemStack> icons = List.of();
        int goCat = -1, goEntry = -1, indent;

        Elem(Kind kind, int height) {
            this.kind = kind;
            this.height = height;
        }

        Elem lines(List<FormattedCharSequence> v) {
            lines = v;
            return this;
        }

        Elem color(int v) {
            color = v;
            return this;
        }

        Elem icon(ItemStack v) {
            icon = v;
            return this;
        }

        Elem number(String v) {
            number = v;
            return this;
        }

        Elem recipes(List<Identifier> v) {
            recipes = v;
            return this;
        }

        Elem icons(List<ItemStack> v) {
            icons = v;
            return this;
        }

        Elem go(int c, int e) {
            goCat = c;
            goEntry = e;
            return this;
        }

        Elem indent(int v) {
            indent = v;
            return this;
        }
    }

    private record Entry(Component title, ItemStack icon, JsonObject json) {
    }

    private record Category(Component title, ItemStack icon, List<Entry> entries) {
    }

    private record SearchDocument(int cat, int entry, String title, String body) {
    }

    private record SearchHit(int cat, int entry, String snippet) {
    }

    private final List<Category> cats = new ArrayList<>();
    /** 当前条目排好的页（一页一列），两列一个跨页。 */
    private List<List<Elem>> columns = List.of();
    private int cat, entry, spread;
    private int bx, by;
    /** 已经放过「翻开书」的声音了（init 改窗口大小时会重跑）。 */
    private boolean opened;
    /** 这一帧鼠标能指到的物品（物品提示用）。 */
    private final List<int[]> hoverRects = new ArrayList<>();
    private final List<ItemStack> hoverStacks = new ArrayList<>();
    /** 这一帧能点的目录行（点了跳到那条），和上面对应的目标。 */
    private final List<int[]> linkRects = new ArrayList<>();
    private final List<int[]> linkTargets = new ArrayList<>();
    private float entryScroll, targetEntryScroll;
    private long lastScrollFrame;
    private boolean searchOpen, skipSearchSlash;
    private String searchQuery = "";
    private EditBox searchBox;
    private final List<SearchDocument> searchIndex = new ArrayList<>();
    private final List<SearchHit> searchHits = new ArrayList<>();
    private int searchScroll;

    public GuideBookScreen() {
        super(Component.translatable("item." + HotPot.MOD_ID + ".hotpot_guide"));
    }

    @Override
    protected void init() {
        cats.clear();
        for (JsonElement c : load().getAsJsonArray("categories")) {
            JsonObject co = c.getAsJsonObject();
            List<Entry> entries = new ArrayList<>();
            for (JsonElement e : co.getAsJsonArray("entries")) {
                JsonObject eo = e.getAsJsonObject();
                ItemStack icon = stack(eo.get("icon").getAsString());
                Component t = eo.has("title") ? Component.literal(eo.get("title").getAsString()) : icon.getHoverName();
                entries.add(new Entry(t, icon, eo));
            }
            cats.add(new Category(Component.literal(co.get("title").getAsString()), stack(co.get("icon").getAsString()), entries));
        }
        bx = (width - BOOK_W) / 2 + 12;
        by = Math.max(24, (height - BOOK_H) / 2 + 8);
        cat = Math.min(lastCat, Math.max(0, cats.size() - 1));
        open(cat, lastEntry);
        entryScroll = targetEntryScroll;
        lastScrollFrame = net.minecraft.util.Util.getMillis();
        buildSearchIndex();
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
        if (!opened) {   // init 在改窗口大小时也会再跑，只在第一次打开时响
            opened = true;
            sound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 0.8F);
        }
    }

    /** 合上书（关界面、换成别的界面都算）：原版把书放上讲台那声「啪」。 */
    @Override
    public void removed() {
        sound(net.minecraft.sounds.SoundEvents.BOOK_PUT, 1.0F);
        super.removed();
    }

    /** 翻开 / 合上书的音效（2026-10-26 用户：指南加打开、关闭的音效）。 */
    private void sound(net.minecraft.sounds.SoundEvent event, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(event, pitch));
        }
    }

    private JsonObject load() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        if (lang.equals(cachedLang) && cachedBook != null) {
            return cachedBook;
        }
        JsonObject book = read(lang);
        if (book != null) {
            cachedLang = lang;
            cachedBook = book;
            return book;
        }
        JsonObject empty = new JsonObject();
        empty.add("categories", new JsonArray());
        return empty;
    }

    private JsonObject read(String lang) {
        for (String l : new String[]{lang, lang.startsWith("zh") ? "zh_cn" : "en_us", "en_us"}) {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                .getResource(HotPot.id("guide/" + l + ".json"));
            if (res.isPresent()) {
                try (Reader r = res.get().openAsReader()) {
                    return JsonParser.parseReader(r).getAsJsonObject();
                } catch (Exception e) {
                    HotPot.LOGGER.warn("guide book {} unreadable", l, e);
                }
            }
        }
        return null;
    }

    /** 物品 ID 变图标；「锅ID#汤底」= 装着那种汤的锅（2026-10-26 用户：火锅分类的书签用番茄汤底的锅）；
     *  「…#lit」= 自定义模型数据 1（灶台物品模型据此换成烧着的 item/brick_stove_lit，用户要灶台书签是燃烧状态）。 */
    private static ItemStack stack(String id) {
        String[] parts = id.split("#", 2);
        Identifier ident = Identifier.tryParse(parts[0]);
        Item item = ident == null ? Items.BARRIER : BuiltInRegistries.ITEM.getOptional(ident).orElse(Items.BARRIER);
        ItemStack stack = new ItemStack(item);
        if (parts.length > 1 && parts[1].equals("lit")) {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new net.minecraft.world.item.component.CustomModelData(java.util.List.of(1F), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        } else if (parts.length > 1) {
            try {
                net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                nbt.putInt("Base", cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.valueOf(
                    parts[1].toUpperCase(java.util.Locale.ROOT)).ordinal());
                stack.set(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.POT_CONTENTS,
                    net.minecraft.world.item.component.CustomData.of(nbt));
            } catch (IllegalArgumentException e) {
                HotPot.LOGGER.warn("guide book: unknown soup in {}", id);
            }
        }
        return stack;
    }

    // ------------------------------------------------------------ 翻页

    private void open(int c, int e) {
        if (cats.isEmpty()) {
            return;
        }
        cat = Math.floorMod(c, cats.size());
        entry = Math.max(0, Math.min(e, cats.get(cat).entries.size() - 1));
        spread = 0;
        lastCat = cat;
        lastEntry = entry;
        columns = layout(cats.get(cat).entries.get(entry));
        targetEntryScroll = clampEntryScroll(entry * ENTRY_STEP + ENTRY_W / 2F - (stripRight() - stripLeft()) / 2F);
    }

    private int spreads() {
        return Math.max(1, (columns.size() + 1) / 2);
    }

    private boolean atStart() {
        return cat == 0 && entry == 0 && spread == 0;
    }

    private boolean atEnd() {
        return cats.isEmpty() || (cat == cats.size() - 1 && entry == cats.get(cat).entries.size() - 1 && spread == spreads() - 1);
    }

    private void next() {
        if (cats.isEmpty() || atEnd()) {
            return;
        }
        if (spread < spreads() - 1) {
            spread++;
        } else if (entry < cats.get(cat).entries.size() - 1) {
            open(cat, entry + 1);
        } else {
            open(cat + 1, 0);
        }
    }

    private void prev() {
        if (cats.isEmpty() || atStart()) {
            return;
        }
        if (spread > 0) {
            spread--;
        } else if (entry > 0) {
            open(cat, entry - 1);
            spread = spreads() - 1;
        } else {
            open(cat - 1, cats.get(Math.floorMod(cat - 1, cats.size())).entries.size() - 1);
            spread = spreads() - 1;
        }
    }

    // ------------------------------------------------------------ 排版

    /** 一个条目：标题块 + 按源文件顺序排的正文块，从左页流到右页、放不下就翻到下一个跨页。 */
    private List<List<Elem>> layout(Entry e) {
        JsonObject j = e.json;
        List<Elem> flow = new ArrayList<>();
        List<FormattedCharSequence> title = split(e.title.copy().withStyle(ChatFormatting.BOLD), COL_W - 20);
        flow.add(new Elem(Kind.HEADER, Math.max(24, title.size() * 9 + 10)).lines(title).icon(e.icon));
        if (j.has("blocks")) {
            for (JsonElement b : j.getAsJsonArray("blocks")) {
                block(flow, b.getAsJsonObject());
            }
        } else {
            legacy(flow, j);   // 旧版 guide_content.py 写出来的条目照样能看
        }
        return pour(flow);
    }

    /** 旧版字段：说明、对照表、配方、步骤、提示（新版 JSON 用 blocks，这里只做兼容）。 */
    private void legacy(List<Elem> flow, JsonObject j) {
        if (j.has("get")) {
            para(flow, j.get("get").getAsString(), MUTED, 3);
        }
        if (j.has("text")) {
            para(flow, j.get("text").getAsString(), TEXT, 5);
        }
        if (j.has("rows")) {
            for (JsonElement r : j.getAsJsonArray("rows")) {
                JsonArray a = r.getAsJsonArray();
                row(flow, stack(a.get(0).getAsString()), a.get(1).getAsString(), TEXT, 0, -1, -1);
            }
            flow.add(new Elem(Kind.GAP, 3));
        }
        if (j.has("recipes")) {
            flow.add(new Elem(Kind.RECIPE, RECIPE_H).recipes(recipeIds(j.getAsJsonArray("recipes"))));
        }
        if (j.has("steps")) {
            int n = 1;
            for (JsonElement s : j.getAsJsonArray("steps")) {
                List<FormattedCharSequence> lines = split(Component.literal(s.getAsString()), COL_W - 11);
                flow.add(new Elem(Kind.NUM, lines.size() * 9 + 3).lines(lines).number(String.valueOf(n++)).color(TEXT));
            }
            flow.add(new Elem(Kind.GAP, 3));
        }
        if (j.has("tip")) {
            note(flow, "", List.of(j.get("tip").getAsString()), "tip");
        }
    }

    private void block(List<Elem> flow, JsonObject o) {
        String k = o.get("k").getAsString();
        String t = o.has("t") ? o.get("t").getAsString() : "";
        switch (k) {
            case "sub" -> {
                List<FormattedCharSequence> lines = split(Component.literal(t), COL_W - 4);
                flow.add(new Elem(Kind.SUB, lines.size() * 9 + 5).lines(lines).color(HEADING));
            }
            case "p" -> para(flow, t, TEXT, 4);
            case "note" -> {
                List<String> body = new ArrayList<>();
                if (o.has("b")) {
                    for (JsonElement b : o.getAsJsonArray("b")) {
                        body.add(b.getAsString());
                    }
                }
                note(flow, t, body, o.has("s") ? o.get("s").getAsString() : "tip");
            }
            case "th" -> {
                List<FormattedCharSequence> lines = split(Component.literal(t), COL_W);
                flow.add(new Elem(Kind.TH, lines.size() * 9 + 4).lines(lines).color(HEADING));
            }
            case "tr" -> {
                ItemStack icon = o.has("icon") ? stack(o.get("icon").getAsString()) : ItemStack.EMPTY;
                int gc = -1, ge = -1;
                if (o.has("go") && !o.get("go").isJsonNull()) {
                    JsonArray go = o.getAsJsonArray("go");
                    gc = go.get(0).getAsInt();
                    ge = go.get(1).getAsInt();
                }
                row(flow, icon, t, gc >= 0 ? NUMBER : TEXT, 0, gc, ge);
            }
            case "bul", "num" -> {
                int indent = o.has("d") ? Math.max(0, Math.min(3, o.get("d").getAsInt())) : 0;
                String mark = k.equals("bul") ? "•" : (o.has("n") ? String.valueOf(o.get("n").getAsInt()) : "-");
                int x = indent * 4;
                ItemStack icon = o.has("icon") ? stack(o.get("icon").getAsString()) : ItemStack.EMPTY;
                int textX = icon.isEmpty() ? (k.equals("bul") ? 8 : 11) : (k.equals("bul") ? 15 : 22);
                List<FormattedCharSequence> lines = split(Component.literal(t), Math.max(40, COL_W - textX - x));
                Elem el = new Elem(k.equals("bul") ? Kind.BUL : Kind.NUM,
                    Math.max(icon.isEmpty() ? 0 : 12, lines.size() * 9 + 3))
                    .lines(lines).number(mark).color(TEXT).indent(x).icon(icon);
                flow.add(el);
            }
            case "icons" -> {
                List<ItemStack> stacks = new ArrayList<>();
                for (JsonElement i : o.getAsJsonArray("ids")) {
                    stacks.add(stack(i.getAsString()));
                }
                if (!stacks.isEmpty()) {
                    int rows = (stacks.size() + ICON_PER_ROW - 1) / ICON_PER_ROW;
                    flow.add(new Elem(Kind.ICONS, rows * ICON_STEP + 2).icons(stacks));
                }
            }
            case "recipes" -> flow.add(new Elem(Kind.RECIPE, RECIPE_H).recipes(recipeIds(o.getAsJsonArray("ids"))));
            case "code" -> {
                List<String> code = new ArrayList<>();
                for (JsonElement l : o.getAsJsonArray("l")) {
                    code.add(l.getAsString());
                }
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (String s : code) {
                    for (FormattedCharSequence f : split(Component.literal(s), COL_W - 6)) {
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

    private void para(List<Elem> flow, String text, int color, int after) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (FormattedCharSequence line : split(Component.literal(text), COL_W)) {
            flow.add(new Elem(Kind.TEXT, 9).lines(List.of(line)).color(color));
        }
        flow.add(new Elem(Kind.GAP, after));
    }

    /** 提示框：一条竖色条 + 小标题 + 几行字（和网页教程的 tip / warn / dev 框对应）。 */
    private void note(List<Elem> flow, String title, List<String> body, String kind) {
        int color = switch (kind) {
            case "warn" -> WARN;
            case "dev" -> NUMBER;
            case "quote" -> MUTED;
            default -> TIP;
        };
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            lines.addAll(split(Component.literal(title), COL_W - 10));
        }
        for (String s : body) {
            if (s != null && !s.isEmpty()) {
                lines.addAll(split(Component.literal(s), COL_W - 10));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        flow.add(new Elem(Kind.NOTE, lines.size() * 9 + 6).lines(lines).color(color));
    }

    /** 一行「图标 + 文字」；go 不是 -1 时点这行会跳过去（本章目录）。 */
    private void row(List<Elem> flow, ItemStack icon, String text, int color, int indent, int goCat, int goEntry) {
        int x = 15 + indent * 4;
        List<FormattedCharSequence> lines = split(Component.literal(text == null ? "" : text), COL_W - x);
        Elem el = new Elem(Kind.ROW, lines.size() <= 1 ? 14 : lines.size() * 9 + 5)
            .lines(lines).color(color).icon(icon).indent(indent);
        if (goCat >= 0) {
            el.go(goCat, goEntry);
        }
        flow.add(el);
    }

    private static List<Identifier> recipeIds(JsonArray a) {
        List<Identifier> ids = new ArrayList<>();
        for (JsonElement r : a) {
            Identifier id = Identifier.tryParse(r.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** 装页：一列装满就换下一列（两列 = 一个跨页）；一块比一页还高（英文长段落、长提示框）就切成几块接着排。 */
    private List<List<Elem>> pour(List<Elem> flow) {
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
                        continue;   // 新一页不以空白开头
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
            default -> 0;   // SUB / TH / CODE：行高各 9，标题下面的线算在最后一块
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
            if (i == 0) {   // 图标、编号只画在头一块上
                e.icon(el.icon).number(el.number);
            }
            if (el.goCat >= 0 && i == 0) {
                e.go(el.goCat, el.goEntry);
            }
            out.add(e);
        }
        return out;
    }

    private List<FormattedCharSequence> split(Component text, int width) {
        return font.split(text, width);
    }

    // ------------------------------------------------------------ 画

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hoverRects.clear();
        hoverStacks.clear();
        linkRects.clear();
        linkTargets.clear();
        long now = net.minecraft.util.Util.getMillis();
        float amount = 1 - (float) Math.exp(-Math.min(100, Math.max(0, now - lastScrollFrame)) / 65F);
        entryScroll += (targetEntryScroll - entryScroll) * amount;
        entryScroll = clampEntryScroll(entryScroll);
        lastScrollFrame = now;

        // 书签在书后面：先画书签，再画书
        int hoverCat = -1, hoverEntry = -1;
        for (int i = 0; i < cats.size(); i++) {
            int ty = catTabY(i);
            boolean hover = !searchOpen && inside(mouseX, mouseY, bx - CAT_W, ty, CAT_W, catTabH());
            if (hover) {
                hoverCat = i;
            }
            boolean sel = i == cat;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, bx - 24 - (sel || hover ? 4 : 0), ty, 280, sel ? 24 : 0, CAT_W, TAB_H, TEX_W, TEX_H);
        }
        List<Entry> entries = cats.isEmpty() ? List.of() : cats.get(cat).entries;
        g.enableScissor(stripLeft(), by - 26, stripRight(), by);
        for (int i = 0; i < entries.size(); i++) {
            int tx = entryTabX(i);
            boolean hover = !searchOpen && inEntryStrip(mouseX, mouseY)
                && inside(mouseX, mouseY, tx, by - 23, ENTRY_W, 23);
            if (hover) {
                hoverEntry = i;
            }
            boolean sel = i == entry;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, tx, by - 20 - (sel || hover ? 3 : 0), 312, sel ? 24 : 0, ENTRY_W, TAB_H, TEX_W, TEX_H);
        }
        g.disableScissor();
        boolean hoverSearch = inside(mouseX, mouseY, searchTabX(), by - 23, ENTRY_W, 23);
        g.enableScissor(searchTabX(), by - 26, searchTabX() + ENTRY_W, by);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, searchTabX(), by - 20 - (searchOpen || hoverSearch ? 3 : 0),
            312, searchOpen ? 24 : 0, ENTRY_W, TAB_H, TEX_W, TEX_H);
        g.disableScissor();
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, bx, by, 0, 0, BOOK_W + 3, BOOK_H + 3, TEX_W, TEX_H);

        // 书签上的图标（露在书外面的那截上）
        for (int i = 0; i < cats.size(); i++) {
            boolean out = i == cat || i == hoverCat;
            g.item(cats.get(i).icon, bx - 19 - (out ? 4 : 0), catTabY(i) + 4);
        }
        g.enableScissor(stripLeft(), by - 26, stripRight(), by);
        for (int i = 0; i < entries.size(); i++) {
            boolean out = i == entry || i == hoverEntry;
            g.item(entries.get(i).icon, entryTabX(i) + 4, by - 18 - (out ? 3 : 0));
        }
        g.disableScissor();
        g.enableScissor(searchTabX(), by - 26, searchTabX() + ENTRY_W, by);
        g.item(new ItemStack(Items.COMPASS), searchTabX() + 4, by - 18 - (searchOpen || hoverSearch ? 3 : 0));
        g.disableScissor();
        if (maxEntryScroll() > 0) {
            g.text(font, "<", bx + 6, by - 16, targetEntryScroll > 0 ? HEADING : MUTED, false);
            g.text(font, ">", stripRight() + 1, by - 16,
                targetEntryScroll < maxEntryScroll() ? HEADING : MUTED, false);
        }
        // 两页内容
        int c0 = spread * 2;
        for (int k = 0; k < 2 && c0 + k < columns.size(); k++) {
            drawColumn(g, columns.get(c0 + k), bx + (k == 0 ? LEFT_X : RIGHT_X), by + COL_Y);
        }
        // 翻页三角和页码
        if (!atStart()) {
            boolean h = inside(mouseX, mouseY, bx + LEFT_X - 2, by + 158, 13, 11);
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, bx + LEFT_X, by + 159, h ? 370 : 360, 0, 9, 9, TEX_W, TEX_H);
        }
        if (!atEnd()) {
            boolean h = inside(mouseX, mouseY, bx + BOOK_W - LEFT_X - 11, by + 158, 13, 11);
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, bx + BOOK_W - LEFT_X - 9, by + 159, h ? 370 : 360, 9, 9, 9, TEX_W, TEX_H);
        }
        if (spreads() > 1) {
            String p = (spread + 1) + " / " + spreads();
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
            g.setTooltipForNextFrame(font, cats.get(hoverCat).title, mouseX, mouseY);
        } else if (hoverEntry >= 0) {
            g.setTooltipForNextFrame(font, entries.get(hoverEntry).title, mouseX, mouseY);
        } else {
            for (int i = 0; i < hoverRects.size(); i++) {
                int[] r = hoverRects.get(i);
                if (inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
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
                    for (FormattedCharSequence t : el.lines) {
                        g.text(font, t, x + 20, ty, HEADING, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 4, x + COL_W, y + el.height - 3, RULE);
                }
                case TEXT -> g.text(font, el.lines.get(0), x, y, el.color, false);
                case SUB -> {
                    int ty = y + 1;
                    for (FormattedCharSequence t : el.lines) {
                        g.text(font, t, x, ty, el.color, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 3, x + COL_W, y + el.height - 2, RULE);
                }
                case NOTE -> {
                    g.fill(x, y, x + 2, y + el.height - 2, el.color);
                    int ty = y + 1;
                    for (FormattedCharSequence t : el.lines) {
                        g.text(font, t, x + 6, ty, el.color, false);
                        ty += 9;
                    }
                }
                case TH -> {
                    int ty = y;
                    for (FormattedCharSequence t : el.lines) {
                        g.text(font, t, x, ty, el.color, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 3, x + COL_W, y + el.height - 2, RULE);
                }
                case ROW -> drawRow(g, el, x, y);
                case BUL, NUM -> {
                    int off = el.indent * 4;
                    g.text(font, el.number, x + off + 1, y, numberColor(el), false);
                    int textX = el.kind == Kind.NUM ? 11 : 8;
                    if (!el.icon.isEmpty()) {
                        smallIcon(g, el.icon, x + off + (el.kind == Kind.NUM ? 7 : 4), y);
                        textX = el.kind == Kind.NUM ? 22 : 15;
                    }
                    int ty = y;
                    for (FormattedCharSequence t : el.lines) {
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
                case RECIPE -> drawRecipe(g, x, y, el.recipes);
                case CODE -> {
                    g.fill(x - 1, y, x + COL_W, y + el.height - 3, CODE_BG);
                    int ty = y + 2;
                    for (FormattedCharSequence t : el.lines) {
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
        return el.kind == Kind.NUM ? NUMBER : MUTED;
    }

    /** 12×12 的小图标（列表项旁边的物品）：16×16 画成 0.75 倍。 */
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
        for (FormattedCharSequence t : el.lines) {
            g.text(font, t, tx, ty, el.color, false);
            ty += 9;
        }
        if (el.goCat >= 0) {   // 本章目录：点这一行翻到那一条
            linkRects.add(new int[]{x, y, COL_W, el.height});
            linkTargets.add(new int[]{el.goCat, el.goEntry});
        }
    }

    /** 配方块：工作台画九宫格，切石机 / 熔炉一类画「原料 → 产物」两格（配方大全里有切石和烹饪）；
     *  下面一行写产物名；几个配方 2 秒换一个，标「1/5」，原料有几种可选的每秒轮换。 */
    private void drawRecipe(GuiGraphicsExtractor g, int x, int y, List<Identifier> ids) {
        if (minecraft == null || minecraft.level == null || ids.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int idx = (int) ((now / 2000) % ids.size());
        Item item = BuiltInRegistries.ITEM.getOptional(ids.get(idx)).orElse(Items.BARRIER);
        RecipeHolder<? extends Recipe<?>> holder = findRecipe(item);
        Recipe<?> recipe = holder == null ? null : holder.value();
        boolean grid = recipe == null || recipe instanceof CraftingRecipe;
        int gx = x + 10, gy = y + 1;
        // mryt233 改（26.1）：26.x 客户端不再持有完整配方对象，产物与原料都从 RecipeDisplay 里取。
        net.minecraft.util.context.ContextMap displayContext = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);
        var display = recipe != null && !recipe.display().isEmpty() ? recipe.display().get(0) : null;
        ItemStack out = display != null
            ? display.result().resolveForStacks(displayContext).stream().findFirst().orElseGet(() -> new ItemStack(item))
            : new ItemStack(item);
        if (grid) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, gx + c * 18, gy + r * 18, 340, 0, 18, 18, TEX_W, TEX_H);
                }
            }
            if (display instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
                int w = shaped.width();
                var ings = shaped.ingredients();
                for (int i = 0; i < ings.size() && i < 9; i++) {
                    ItemStack[] stacks = ings.get(i).resolveForStacks(displayContext).toArray(ItemStack[]::new);
                    if (stacks.length > 0) {
                        ItemStack s = stacks[(int) ((now / 1000) % stacks.length)];
                        int sx = gx + (i % w) * 18 + 1, sy = gy + (i / w) * 18 + 1;
                        g.item(s, sx, sy);
                        track(sx, sy, 16, 16, s);
                    }
                }
            } else if (display instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
                var ings = shapeless.ingredients();
                for (int i = 0; i < ings.size() && i < 9; i++) {
                    ItemStack[] stacks = ings.get(i).resolveForStacks(displayContext).toArray(ItemStack[]::new);
                    if (stacks.length > 0) {
                        ItemStack s = stacks[(int) ((now / 1000) % stacks.length)];
                        int sx = gx + (i % 3) * 18 + 1, sy = gy + (i / 3) * 18 + 1;
                        g.item(s, sx, sy);
                        track(sx, sy, 16, 16, s);
                    }
                }
            }
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, gx + 58, gy + 23, 340, 18, 12, 8, TEX_W, TEX_H);
            int ox = gx + 74, oy = gy + 18;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
            g.item(out, ox + 1, oy + 1);
            g.itemDecorations(font, out, ox + 1, oy + 1);
            track(ox + 1, oy + 1, 16, 16, out);
        } else {
            // 切石机 / 熔炉：一格原料，箭头，一格产物；上面一行小字写是哪一种
            int sx = gx + 12, sy = gy + 4;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, sx, sy, 340, 0, 18, 18, TEX_W, TEX_H);
            // mryt233 改（26.1）：RecipeDisplay 只有 result / craftingStation，原料要从具体显示类型里取。
            var firstIngredient = displayIngredient(display);
            if (firstIngredient != null) {
                ItemStack[] stacks = firstIngredient.resolveForStacks(displayContext).toArray(ItemStack[]::new);
                if (stacks.length > 0) {
                    ItemStack in = stacks[(int) ((now / 1000) % stacks.length)];
                    g.item(in, sx + 1, sy + 1);
                    track(sx + 1, sy + 1, 16, 16, in);
                }
            }
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, sx + 26, sy + 5, 340, 18, 12, 8, TEX_W, TEX_H);
            int ox = gx + 44, oy = gy + 4;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
            g.item(out, ox + 1, oy + 1);
            g.itemDecorations(font, out, ox + 1, oy + 1);
            track(ox + 1, oy + 1, 16, 16, out);
            Component how = holder != null && holder.value().getType() == RecipeType.STONECUTTING
                ? Blocks.STONECUTTER.getName() : Blocks.FURNACE.getName();
            String label = how.getString();
            g.text(font, label, x + (COL_W - font.width(label)) / 2, gy + 28, MUTED, false);
        }
        Component name = out.getHoverName();
        String count = ids.size() > 1 ? "  " + (idx + 1) + "/" + ids.size() : "";
        FormattedCharSequence line = font.split(name, COL_W - font.width(count)).get(0);
        int lw = font.width(line) + font.width(count);
        int lx = x + (COL_W - lw) / 2;
        g.text(font, line, lx, y + 57, NUMBER, false);
        if (!count.isEmpty()) {
            g.text(font, count, lx + font.width(line), y + 57, MUTED, false);
        }
    }

    /**
     * 找做出这件东西的配方：先是工作台（没找到就只画产物），再找切石机和熔炉一类。
     * mryt233 改（26.1）：26.x 客户端不再持有完整配方（getRecipeManager / getAllRecipesFor 都没了），
     * 改走 {@link cn.blockforge.generated.orientshotpotchronicles.hotpot.RecipeSync} 的配方视图，
     * 产物从 {@code RecipeDisplay.result()} 解析；索引按当前配方列表换一次就重建。
     */
    private RecipeHolder<? extends Recipe<?>> findRecipe(Item item) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Iterable<RecipeHolder<?>> view = cn.blockforge.generated.orientshotpotchronicles.hotpot.RecipeSync
            .recipes(minecraft.level);
        if (recipeCache == null || recipeCacheKey != view) {
            Map<Item, RecipeHolder<? extends Recipe<?>>> cache = new HashMap<>();
            net.minecraft.util.context.ContextMap displayContext = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);
            // putIfAbsent = 先找到的那一类优先，和原来 5 个循环的先后顺序一致
            index(cache, view, RecipeType.CRAFTING, displayContext);
            index(cache, view, RecipeType.STONECUTTING, displayContext);
            index(cache, view, RecipeType.SMELTING, displayContext);
            index(cache, view, RecipeType.SMOKING, displayContext);
            index(cache, view, RecipeType.CAMPFIRE_COOKING, displayContext);
            recipeCache = cache;
            recipeCacheKey = view;
        }
        return recipeCache.get(item);
    }

    /** 把一类配方的「产物物品 → 配方」填进索引；先填进去的那一类优先。 */
    private static void index(Map<Item, RecipeHolder<? extends Recipe<?>>> cache, Iterable<RecipeHolder<?>> view,
                              RecipeType<? extends Recipe<?>> type,
                              net.minecraft.util.context.ContextMap displayContext) {
        for (RecipeHolder<?> holder : view) {
            if (holder.value().getType() != type) {
                continue;
            }
            for (var display : holder.value().display()) {
                var stacks = display.result().resolveForStacks(displayContext);
                if (!stacks.isEmpty()) {
                    cache.putIfAbsent(stacks.get(0).getItem(), holder);
                }
            }
        }
    }

    private void track(int x, int y, int w, int h, ItemStack s) {
        if (!s.isEmpty()) {
            hoverRects.add(new int[]{x, y, w, h});
            hoverStacks.add(s);
        }
    }

    /** 左侧分类书签的间距：书高 180，9 个分类也在书框里（8 个间隔各 18 像素，最后一个刚好到书底）。 */
    private int catTabStep() {
        int n = cats.size();
        return n <= 1 ? TAB_H : Math.min(TAB_H, Math.max(9, (BOOK_H - TAB_H - 6) / (n - 1)));
    }

    private int catTabY(int i) {
        return by + 6 + i * catTabStep();
    }

    private int catTabH() {
        return TAB_H;
    }

    private int searchTabX() {
        return bx + BOOK_W - 6 - ENTRY_W;
    }

    private int entryStripWidth() {
        return cats.isEmpty() ? 0 : Math.max(0, cats.get(cat).entries.size() * ENTRY_STEP - 2);
    }

    private boolean entryOverflow() {
        return entryStripWidth() > searchTabX() - (bx + 6) - 6;
    }

    private int stripLeft() {
        return bx + (entryOverflow() ? 14 : 6);
    }

    private int stripRight() {
        return searchTabX() - (entryOverflow() ? 10 : 6);
    }

    private int maxEntryScroll() {
        return Math.max(0, entryStripWidth() - (stripRight() - stripLeft()));
    }

    private float clampEntryScroll(float scroll) {
        return Math.max(0, Math.min(maxEntryScroll(), scroll));
    }

    private boolean inEntryStrip(double x, double y) {
        return inside(x, y, stripLeft(), by - 26, stripRight() - stripLeft(), 26);
    }

    private int entryTabX(int i) {
        return stripLeft() + i * ENTRY_STEP - Math.round(entryScroll);
    }

    // ------------------------------------------------------------ 全文搜索（mryt233 改，2026-11-07）

    private String ui(String simplified, String traditional, String english) {
        String lang = minecraft.getLanguageManager().getSelected();
        return lang.equals("zh_tw") || lang.equals("zh_hk") ? traditional : lang.startsWith("zh") ? simplified : english;
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

    private static void appendText(StringBuilder text, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (value.isJsonArray()) {
            for (JsonElement part : value.getAsJsonArray()) {
                appendText(text, part);
            }
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            text.append(value.getAsString()).append(' ');
        }
    }

    private static void appendItemNames(StringBuilder text, JsonElement ids) {
        if (ids == null || ids.isJsonNull()) {
            return;
        }
        if (ids.isJsonArray()) {
            for (JsonElement id : ids.getAsJsonArray()) {
                appendItemNames(text, id);
            }
        } else if (ids.isJsonPrimitive()) {
            text.append(stack(ids.getAsString()).getHoverName().getString()).append(' ');
        }
    }

    private void buildSearchIndex() {
        searchIndex.clear();
        for (int ci = 0; ci < cats.size(); ci++) {
            Category category = cats.get(ci);
            for (int ei = 0; ei < category.entries.size(); ei++) {
                Entry item = category.entries.get(ei);
                StringBuilder body = new StringBuilder(item.icon.getHoverName().getString()).append(' ');
                JsonObject json = item.json;
                if (json.has("blocks")) {
                    for (JsonElement block : json.getAsJsonArray("blocks")) {
                        JsonObject b = block.getAsJsonObject();
                        for (String key : List.of("t", "b", "l")) {
                            appendText(body, b.get(key));
                        }
                        appendItemNames(body, b.get("icon"));
                        if (b.has("ids")) {
                            appendItemNames(body, b.get("ids"));
                        }
                    }
                } else {
                    for (String key : List.of("get", "text", "steps", "tip")) {
                        appendText(body, json.get(key));
                    }
                    if (json.has("rows")) {
                        for (JsonElement row : json.getAsJsonArray("rows")) {
                            JsonArray values = row.getAsJsonArray();
                            appendItemNames(body, values.get(0));
                            appendText(body, values.get(1));
                        }
                    }
                    appendItemNames(body, json.get("recipes"));
                }
                searchIndex.add(new SearchDocument(ci, ei, category.title.getString() + " · " + item.title.getString(),
                    body.toString().strip()));
            }
        }
    }

    private void runSearch() {
        searchHits.clear();
        searchScroll = 0;
        String query = searchQuery.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return;
        }
        // 标题命中优先，同分保留书中分类、条目的顺序。
        for (int rank = 0; rank < 2; rank++) {
            for (SearchDocument doc : searchIndex) {
                boolean titleHit = doc.title.toLowerCase(Locale.ROOT).contains(query);
                int at = doc.body.toLowerCase(Locale.ROOT).indexOf(query);
                if ((titleHit ? 0 : 1) != rank || (!titleHit && at < 0)) {
                    continue;
                }
                int from = Math.max(0, at - 12);
                String snippet = (from > 0 ? "…" : "") + doc.body.substring(from);
                searchHits.add(new SearchHit(doc.cat, doc.entry, snippet));
            }
        }
    }

    private String elementText(Elem el) {
        StringBuilder text = new StringBuilder();
        for (FormattedCharSequence line : el.lines) {
            line.accept((index, style, codePoint) -> {
                text.appendCodePoint(codePoint);
                return true;
            });
        }
        if (!el.icon.isEmpty()) {
            text.append(' ').append(el.icon.getHoverName().getString());
        }
        for (ItemStack icon : el.icons) {
            text.append(' ').append(icon.getHoverName().getString());
        }
        for (Identifier id : el.recipes) {
            text.append(' ').append(stack(id.toString()).getHoverName().getString());
        }
        return text.toString();
    }

    private void jumpToSearchHit(SearchHit hit) {
        open(hit.cat, hit.entry);
        String query = searchQuery.strip().toLowerCase(Locale.ROOT);
        StringBuilder text = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        for (List<Elem> column : columns) {
            offsets.add(text.length());
            for (Elem el : column) {
                text.append(elementText(el));
                if (el.kind != Kind.TEXT) {
                    text.append(' ');
                }
            }
        }
        int at = text.toString().toLowerCase(Locale.ROOT).indexOf(query);
        for (int col = 0; at >= 0 && col < offsets.size(); col++) {
            if (offsets.get(col) <= at) {
                spread = col / 2;
            }
        }
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
        boolean closeHover = inside(mx, my, x + SEARCH_W - 20, y + 5, 16, 18);
        g.text(font, "×", x + SEARCH_W - 16, y + 10, closeHover ? WARN : HEADING, false);
        searchBox.extractWidgetRenderState(g, mx, my, delta);
        g.enableScissor(x + 5, y + 31, x + SEARCH_W - 5, y + 31 + SEARCH_LIST_H);
        for (int i = 0; i < searchHits.size(); i++) {
            int ry = y + 31 + i * SEARCH_ROW_H - searchScroll;
            if (ry + SEARCH_ROW_H <= y + 31 || ry >= y + 31 + SEARCH_LIST_H) {
                continue;
            }
            SearchHit hit = searchHits.get(i);
            if (inside(mx, my, x + 5, ry, SEARCH_W - 10, SEARCH_ROW_H)) {
                g.fill(x + 5, ry, x + SEARCH_W - 5, ry + SEARCH_ROW_H, 0x40DFC9A3);
            }
            Entry item = cats.get(hit.cat).entries.get(hit.entry);
            g.item(item.icon, x + 7, ry + 4);
            String title = cats.get(hit.cat).title.getString() + " · " + item.title.getString();
            g.text(font, fitSearchText(title, SEARCH_W - 38), x + 28, ry + 2, HEADING, false);
            drawSearchSnippet(g, hit.snippet, x + 28, ry + 13, SEARCH_W - 38);
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

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------ 操作

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == 0 && inside(mouseX, mouseY, searchTabX(), by - 23, ENTRY_W, 23)) {
            toggleSearch();
            return true;
        }
        if (searchOpen) {
            if (button == 0) {
                int x = searchX(), y = searchY();
                if (inside(mouseX, mouseY, x + SEARCH_W - 20, y + 5, 16, 18)) {
                    toggleSearch();
                } else if (inside(mouseX, mouseY, x + 5, y + 31, SEARCH_W - 10, SEARCH_LIST_H)) {
                    int index = ((int) mouseY - y - 31 + searchScroll) / SEARCH_ROW_H;
                    if (index >= 0 && index < searchHits.size()) {
                        jumpToSearchHit(searchHits.get(index));
                    }
                } else {
                    searchBox.onClick(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
                    searchBox.setFocused(true);
                }
            }
            return true;
        }
        if (button == 0) {
            if (maxEntryScroll() > 0) {
                if (inside(mouseX, mouseY, bx + 6, by - 26, 8, 26)) {
                    targetEntryScroll = clampEntryScroll(targetEntryScroll - ENTRY_STEP * 3);
                    return true;
                }
                if (inside(mouseX, mouseY, stripRight(), by - 26, 8, 26)) {
                    targetEntryScroll = clampEntryScroll(targetEntryScroll + ENTRY_STEP * 3);
                    return true;
                }
            }
            for (int i = 0; i < cats.size(); i++) {
                if (inside(mouseX, mouseY, bx - CAT_W, catTabY(i), CAT_W, catTabH())) {
                    open(i, 0);
                    click();
                    return true;
                }
            }
            if (!cats.isEmpty()) {
                for (int i = 0; i < cats.get(cat).entries.size(); i++) {
                    if (inEntryStrip(mouseX, mouseY)
                        && inside(mouseX, mouseY, entryTabX(i), by - 23, ENTRY_W, 23)) {
                        open(cat, i);
                        click();
                        return true;
                    }
                }
            }
            for (int i = 0; i < linkRects.size(); i++) {   // 本章目录：点一行直接翻到那条
                int[] r = linkRects.get(i);
                if (inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    int[] target = linkTargets.get(i);
                    open(target[0], target[1]);
                    click();
                    return true;
                }
            }
            if (!atStart() && inside(mouseX, mouseY, bx + LEFT_X - 2, by + 158, 13, 11)) {
                prev();
                click();
                return true;
            }
            if (!atEnd() && inside(mouseX, mouseY, bx + BOOK_W - LEFT_X - 11, by + 158, 13, 11)) {
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
            if (inside(mouseX, mouseY, searchX() + 5, searchY() + 31, SEARCH_W - 10, SEARCH_LIST_H)) {
                int maxScroll = Math.max(0, searchHits.size() * SEARCH_ROW_H - SEARCH_LIST_H);
                searchScroll = Math.max(0, Math.min(maxScroll, searchScroll - (int) Math.round(verticalAmount * SEARCH_ROW_H)));
            }
            return true;
        }
        if (inside(mouseX, mouseY, bx, by - 26, BOOK_W, 28)) {
            if (maxEntryScroll() > 0) {
                double amount = verticalAmount != 0 ? verticalAmount : horizontalAmount;
                targetEntryScroll = clampEntryScroll(targetEntryScroll - (float) amount * ENTRY_STEP * 2);
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
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 70 && event.hasControlDown()) {   // Ctrl+F
            toggleSearch();
            return true;
        }
        if (!searchOpen && keyCode == 47 && !event.hasShiftDown() && !event.hasControlDown()) {
            toggleSearch();
            skipSearchSlash = true;
            return true;
        }
        if (searchOpen) {
            if (keyCode == 256) {   // ESC 优先关闭搜索
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
        if (keyCode == 266 || keyCode == 263) {   // Page Up / ←
            prev();
            return true;
        }
        if (keyCode == 267 || keyCode == 262) {   // Page Down / →
            next();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char codePoint = event.codepointAsString().charAt(0);
        int modifiers = 0;
        if (searchOpen) {
            boolean skip = skipSearchSlash && codePoint == '/';
            skipSearchSlash = false;
            return skip || searchBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 从一个配方显示里取第一样原料（工作台 / 切石机 / 熔炉都尽量兜住）；取不到返回 null。 */
    private static net.minecraft.world.item.crafting.display.SlotDisplay displayIngredient(
            net.minecraft.world.item.crafting.display.RecipeDisplay display) {
        if (display instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
            return shaped.ingredients().isEmpty() ? null : shaped.ingredients().get(0);
        }
        if (display instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.ingredients().isEmpty() ? null : shapeless.ingredients().get(0);
        }
        if (display instanceof net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay stonecutter) {
            return stonecutter.input();
        }
        if (display instanceof net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay furnace) {
            return furnace.ingredient();
        }
        return null;
    }
}
