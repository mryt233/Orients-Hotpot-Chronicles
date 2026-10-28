package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 「火锅指南」界面（2026-10-25 用户要游戏内教程书；2026-10-26 用户：照真的指南做——摊开的两页书，左边一列分类书签、
 * 顶上一排条目书签，鼠标放上去书签伸出来、显示名字，点一下翻过去；要有立体感，书皮红色）。
 * 每个条目一个跨页：左页 = 图标 + 标题、获得、说明（条目有配方 / 步骤时对照表也在左页）；右页 = 对照表、配方、步骤、提示；
 * 放不下就接着往后翻。配方几个就轮流显示（2 秒一个），原料有几种可选的每秒轮换；指着物品显示物品提示。
 * 内容来自 assets/orients_hotpot_chronicles/guide/&lt;语言&gt;.json（tools/gen_guide_book.py 从 tools/guide_content.py 生成，
 * 简体 / 繁体 / 英文，其他语言用英文）；贴图 textures/gui/guide_book.png（tools/gen_guide_gui.py）。
 * 查看器 viewer/screens.js 里有同样的网页版。
 */
public class GuideBookScreen extends Screen {
    private static final Identifier TEX = GeneratedMod.id("textures/gui/guide_book.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int BOOK_W = 272, BOOK_H = 180;
    /** 两页文字区：左页 x 16、右页 x 144（相对书的左上角），宽 112，从 y 12 起高 144。 */
    private static final int LEFT_X = 16, RIGHT_X = 144, COL_W = 112, COL_Y = 12, COL_H = 144;
    private static final int TEXT = 0xFF2A1A0E, HEADING = 0xFF5A2A10, MUTED = 0xFF7A6040, TIP = 0xFF2E5E1E,
        NUMBER = 0xFF1E6E78, RULE = 0x40603820;
    private static final int RECIPE_H = 66;

    /** 记住上次看到哪（关了再打开还在那一页）。 */
    private static int lastCat, lastEntry;

    private enum Kind { HEADER, TEXT, ROW, RECIPE, STEP, TIP, GAP }

    /** 排版后的一块：画什么、占多高。 */
    private record Elem(Kind kind, int height, List<OrderedText> lines, int color, ItemStack icon, String number,
                        List<Identifier> recipes) {
    }

    private record Entry(Text title, ItemStack icon, JsonObject json) {
    }

    private record Category(Text title, ItemStack icon, List<Entry> entries) {
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

    public GuideBookScreen() {
        super(Text.translatable("item." + GeneratedMod.MOD_ID + ".hotpot_guide"));
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
                Text t = eo.has("title") ? Text.literal(eo.get("title").getAsString()) : icon.getName();
                entries.add(new Entry(t, icon, eo));
            }
            cats.add(new Category(Text.literal(co.get("title").getAsString()), stack(co.get("icon").getAsString()), entries));
        }
        bx = (width - BOOK_W) / 2 + 12;
        by = Math.max(24, (height - BOOK_H) / 2 + 8);
        cat = Math.min(lastCat, Math.max(0, cats.size() - 1));
        open(cat, lastEntry);
        if (!opened) {   // init 在改窗口大小时也会再跑，只在第一次打开时响
            opened = true;
            sound(net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN, 0.8F);
        }
    }

    /** 合上书（关界面、换成别的界面都算）：原版把书放上讲台那声「啪」。 */
    @Override
    public void removed() {
        sound(net.minecraft.sound.SoundEvents.ITEM_BOOK_PUT, 1.0F);
        super.removed();
    }

    /** 翻开 / 合上书的音效（2026-10-26 用户：指南加打开、关闭的音效）。 */
    private void sound(net.minecraft.sound.SoundEvent event, float pitch) {
        if (client != null) {
            client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(event, pitch));
        }
    }

    private JsonObject load() {
        String lang = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        for (String l : new String[]{lang, lang.startsWith("zh") ? "zh_cn" : "en_us", "en_us"}) {
            Optional<Resource> res = MinecraftClient.getInstance().getResourceManager()
                .getResource(GeneratedMod.id("guide/" + l + ".json"));
            if (res.isPresent()) {
                try (Reader r = res.get().getReader()) {
                    return JsonParser.parseReader(r).getAsJsonObject();
                } catch (Exception e) {
                    GeneratedMod.LOGGER.warn("guide book {} unreadable", l, e);
                }
            }
        }
        JsonObject empty = new JsonObject();
        empty.add("categories", new JsonArray());
        return empty;
    }

    /** 物品 ID 变图标；「锅ID#汤底」= 装着那种汤的锅（2026-10-26 用户：火锅分类的书签用番茄汤底的锅）；
     *  「…#lit」= 自定义模型数据 1（灶台物品模型据此换成烧着的 item/brick_stove_lit，用户要灶台书签是燃烧状态）。 */
    private static ItemStack stack(String id) {
        String[] parts = id.split("#", 2);
        Identifier ident = Identifier.tryParse(parts[0]);
        Item item = ident == null ? Items.BARRIER : Registries.ITEM.getOrEmpty(ident).orElse(Items.BARRIER);
        ItemStack stack = new ItemStack(item);
        if (parts.length > 1 && parts[1].equals("lit")) {
            stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
                new net.minecraft.component.type.CustomModelDataComponent(1));
        } else if (parts.length > 1) {
            try {
                net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
                nbt.putInt("Base", cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.valueOf(
                    parts[1].toUpperCase(java.util.Locale.ROOT)).ordinal());
                stack.set(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.POT_CONTENTS,
                    net.minecraft.component.type.NbtComponent.of(nbt));
            } catch (IllegalArgumentException e) {
                GeneratedMod.LOGGER.warn("guide book: unknown soup in {}", id);
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
    }

    private int spreads() {
        return Math.max(1, (columns.size() + 1) / 2);
    }

    private boolean atStart() {
        return cat == 0 && entry == 0 && spread == 0;
    }

    private boolean atEnd() {
        return cat == cats.size() - 1 && entry == cats.get(cat).entries.size() - 1 && spread == spreads() - 1;
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

    private List<List<Elem>> layout(Entry e) {
        JsonObject j = e.json;
        boolean rowsLeft = j.has("recipes") || j.has("steps");
        List<Elem> left = new ArrayList<>(), right = new ArrayList<>();
        // 左页：标题、获得、说明
        List<OrderedText> title = textRenderer.wrapLines(e.title.copy().formatted(Formatting.BOLD), COL_W - 20);
        left.add(new Elem(Kind.HEADER, Math.max(18, title.size() * 9 + 6) + 6, title, HEADING, e.icon, null, null));
        if (j.has("get")) {
            left.addAll(wrapped(Text.literal(j.get("get").getAsString()).formatted(Formatting.ITALIC), MUTED, 0));
            left.add(gap(3));
        }
        if (j.has("text")) {
            left.addAll(wrapped(Text.literal(j.get("text").getAsString()), TEXT, 0));
            left.add(gap(5));
        }
        // 对照表：有配方 / 步骤的条目放左页，不然放右页
        List<Elem> rows = new ArrayList<>();
        if (j.has("rows")) {
            for (JsonElement r : j.getAsJsonArray("rows")) {
                JsonArray a = r.getAsJsonArray();
                List<OrderedText> lines = textRenderer.wrapLines(Text.literal(a.get(1).getAsString()), COL_W - 15);
                rows.add(new Elem(Kind.ROW, lines.size() == 1 ? 14 : lines.size() * 9 + 5, lines, TEXT,
                    stack(a.get(0).getAsString()), null, null));
            }
            rows.add(gap(3));
        }
        (rowsLeft ? left : right).addAll(rows);
        if (j.has("recipes")) {
            List<Identifier> ids = new ArrayList<>();
            for (JsonElement r : j.getAsJsonArray("recipes")) {
                ids.add(Identifier.of(r.getAsString()));
            }
            right.add(new Elem(Kind.RECIPE, RECIPE_H, List.of(), TEXT, ItemStack.EMPTY, null, ids));
        }
        if (j.has("steps")) {
            int n = 1;
            for (JsonElement s : j.getAsJsonArray("steps")) {
                List<OrderedText> lines = textRenderer.wrapLines(Text.literal(s.getAsString()), COL_W - 11);
                right.add(new Elem(Kind.STEP, lines.size() * 9 + 3, lines, TEXT, ItemStack.EMPTY, String.valueOf(n++), null));
            }
            right.add(gap(3));
        }
        if (j.has("tip")) {
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(j.get("tip").getAsString()), COL_W - 6);
            right.add(new Elem(Kind.TIP, lines.size() * 9 + 2, lines, TIP, ItemStack.EMPTY, null, null));
        }
        // 装页：左页的东西从第一列起，右页的东西从下一列起；一列装满就换下一列
        List<List<Elem>> cols = new ArrayList<>();
        pour(cols, left, true);
        pour(cols, right, cols.size() % 2 == 1);
        return cols;
    }

    private void pour(List<List<Elem>> cols, List<Elem> elems, boolean newColumn) {
        if (elems.isEmpty()) {
            return;
        }
        List<Elem> cur;
        if (newColumn || cols.isEmpty()) {
            cur = new ArrayList<>();
            cols.add(cur);
        } else {
            cur = cols.get(cols.size() - 1);
        }
        int used = cur.stream().mapToInt(Elem::height).sum();
        for (Elem el : elems) {
            if (used + el.height > COL_H && !cur.isEmpty()) {
                cur = new ArrayList<>();
                cols.add(cur);
                used = 0;
                if (el.kind == Kind.GAP) {
                    continue;   // 新一页不以空白开头
                }
            }
            cur.add(el);
            used += el.height;
        }
    }

    private List<Elem> wrapped(Text text, int color, int indent) {
        List<Elem> out = new ArrayList<>();
        for (OrderedText t : textRenderer.wrapLines(text, COL_W - indent)) {
            out.add(new Elem(Kind.TEXT, 9, List.of(t), color, ItemStack.EMPTY, null, null));
        }
        return out;
    }

    private static Elem gap(int h) {
        return new Elem(Kind.GAP, h, List.of(), 0, ItemStack.EMPTY, null, null);
    }

    // ------------------------------------------------------------ 画

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        hoverRects.clear();
        hoverStacks.clear();
        RenderSystem.enableBlend();
        // 书签在书后面：先画书签，再画书
        int hoverCat = -1, hoverEntry = -1;
        for (int i = 0; i < cats.size(); i++) {
            int ty = leftTabY(i);
            boolean hover = inside(mouseX, mouseY, bx - 28, ty, 28, 24);
            if (hover) {
                hoverCat = i;
            }
            boolean sel = i == cat;
            g.drawTexture(TEX, bx - 24 - (sel || hover ? 4 : 0), ty, 280, sel ? 24 : 0, 28, 24, TEX_W, TEX_H);
        }
        List<Entry> entries = cats.isEmpty() ? List.of() : cats.get(cat).entries;
        for (int i = 0; i < entries.size(); i++) {
            int tx = topTabX(i);
            boolean hover = inside(mouseX, mouseY, tx, by - 24, 24, 24);
            if (hover) {
                hoverEntry = i;
            }
            boolean sel = i == entry;
            g.drawTexture(TEX, tx, by - 20 - (sel || hover ? 3 : 0), 312, sel ? 24 : 0, 24, 24, TEX_W, TEX_H);
        }
        g.drawTexture(TEX, bx, by, 0, 0, BOOK_W + 3, BOOK_H + 3, TEX_W, TEX_H);
        RenderSystem.disableBlend();
        // 书签上的图标（露在书外面的那截上）
        for (int i = 0; i < cats.size(); i++) {
            boolean out = i == cat || i == hoverCat;
            g.drawItem(cats.get(i).icon, bx - 19 - (out ? 4 : 0), leftTabY(i) + 4);
        }
        for (int i = 0; i < entries.size(); i++) {
            boolean out = i == entry || i == hoverEntry;
            g.drawItem(entries.get(i).icon, topTabX(i) + 4, by - 16 - (out ? 3 : 0));
        }
        // 两页内容
        int c0 = spread * 2;
        for (int k = 0; k < 2 && c0 + k < columns.size(); k++) {
            drawColumn(g, columns.get(c0 + k), bx + (k == 0 ? LEFT_X : RIGHT_X), by + COL_Y, mouseX, mouseY);
        }
        // 翻页三角和页码
        if (!atStart()) {
            boolean h = inside(mouseX, mouseY, bx + LEFT_X - 2, by + 158, 13, 11);
            g.drawTexture(TEX, bx + LEFT_X, by + 159, h ? 370 : 360, 0, 9, 9, TEX_W, TEX_H);
        }
        if (!atEnd()) {
            boolean h = inside(mouseX, mouseY, bx + BOOK_W - LEFT_X - 11, by + 158, 13, 11);
            g.drawTexture(TEX, bx + BOOK_W - LEFT_X - 9, by + 159, h ? 370 : 360, 9, 9, 9, TEX_W, TEX_H);
        }
        if (spreads() > 1) {
            String p = (spread + 1) + " / " + spreads();
            g.drawText(textRenderer, p, bx + RIGHT_X + COL_W / 2 - textRenderer.getWidth(p) / 2, by + 160, MUTED, false);
        }
        // 提示：书签名字、物品
        if (hoverCat >= 0) {
            g.drawTooltip(textRenderer, cats.get(hoverCat).title, mouseX, mouseY);
        } else if (hoverEntry >= 0) {
            g.drawTooltip(textRenderer, entries.get(hoverEntry).title, mouseX, mouseY);
        } else {
            for (int i = 0; i < hoverRects.size(); i++) {
                int[] r = hoverRects.get(i);
                if (inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    g.drawItemTooltip(textRenderer, hoverStacks.get(i), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void drawColumn(DrawContext g, List<Elem> col, int x, int y, int mouseX, int mouseY) {
        for (Elem el : col) {
            switch (el.kind) {
                case HEADER -> {
                    g.drawItem(el.icon, x, y);
                    track(x, y, 16, 16, el.icon);
                    int ty = y + (el.lines.size() == 1 ? 4 : 1);
                    for (OrderedText t : el.lines) {
                        g.drawText(textRenderer, t, x + 20, ty, el.color, false);
                        ty += 9;
                    }
                    g.fill(x, y + el.height - 4, x + COL_W, y + el.height - 3, RULE);
                }
                case TEXT -> g.drawText(textRenderer, el.lines.get(0), x, y, el.color, false);
                case ROW -> {
                    g.getMatrices().push();
                    g.getMatrices().translate(x, y + 1, 0);
                    g.getMatrices().scale(0.75F, 0.75F, 1);
                    g.drawItem(el.icon, 0, 0);
                    g.getMatrices().pop();
                    track(x, y + 1, 12, 12, el.icon);
                    int ty = y + (el.lines.size() == 1 ? 3 : 2);
                    for (OrderedText t : el.lines) {
                        g.drawText(textRenderer, t, x + 15, ty, el.color, false);
                        ty += 9;
                    }
                }
                case STEP -> {
                    g.drawText(textRenderer, el.number, x + 1, y, NUMBER, false);
                    int ty = y;
                    for (OrderedText t : el.lines) {
                        g.drawText(textRenderer, t, x + 11, ty, el.color, false);
                        ty += 9;
                    }
                }
                case TIP -> {
                    g.fill(x, y, x + 2, y + el.height - 2, TIP);
                    int ty = y;
                    for (OrderedText t : el.lines) {
                        g.drawText(textRenderer, t, x + 6, ty, el.color, false);
                        ty += 9;
                    }
                }
                case RECIPE -> drawRecipe(g, x, y, el.recipes);
                default -> {
                }
            }
            y += el.height;
        }
    }

    /** 工作台九宫格 + 箭头 + 产物，下面一行写产物名；几个配方 2 秒换一个，标「1/5」。 */
    private void drawRecipe(DrawContext g, int x, int y, List<Identifier> ids) {
        if (client == null || client.world == null || ids.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int idx = (int) ((now / 2000) % ids.size());
        Item item = Registries.ITEM.getOrEmpty(ids.get(idx)).orElse(Items.BARRIER);
        CraftingRecipe recipe = null;
        for (RecipeEntry<CraftingRecipe> e : client.world.getRecipeManager().listAllOfType(RecipeType.CRAFTING)) {
            if (e.value().getResult(client.world.getRegistryManager()).isOf(item)) {
                recipe = e.value();
                break;
            }
        }
        int gx = x + 10, gy = y + 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.drawTexture(TEX, gx + c * 18, gy + r * 18, 340, 0, 18, 18, TEX_W, TEX_H);
            }
        }
        if (recipe != null) {
            int w = recipe instanceof ShapedRecipe s ? s.getWidth() : 3;
            List<Ingredient> ings = recipe.getIngredients();
            for (int i = 0; i < ings.size() && i < 9; i++) {
                ItemStack[] stacks = ings.get(i).getMatchingStacks();
                if (stacks.length > 0) {
                    ItemStack s = stacks[(int) ((now / 1000) % stacks.length)];
                    int sx = gx + (i % w) * 18 + 1, sy = gy + (i / w) * 18 + 1;
                    g.drawItem(s, sx, sy);
                    track(sx, sy, 16, 16, s);
                }
            }
        }
        g.drawTexture(TEX, gx + 58, gy + 23, 340, 18, 12, 8, TEX_W, TEX_H);
        int ox = gx + 74, oy = gy + 18;
        g.drawTexture(TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
        ItemStack out = recipe != null ? recipe.getResult(client.world.getRegistryManager()) : new ItemStack(item);
        g.drawItem(out, ox + 1, oy + 1);
        g.drawItemInSlot(textRenderer, out, ox + 1, oy + 1);
        track(ox + 1, oy + 1, 16, 16, out);
        Text name = out.getName();
        String count = ids.size() > 1 ? "  " + (idx + 1) + "/" + ids.size() : "";
        OrderedText line = textRenderer.wrapLines(name, COL_W - textRenderer.getWidth(count)).get(0);
        int lw = textRenderer.getWidth(line) + textRenderer.getWidth(count);
        int lx = x + (COL_W - lw) / 2;
        g.drawText(textRenderer, line, lx, y + 57, NUMBER, false);
        if (!count.isEmpty()) {
            g.drawText(textRenderer, count, lx + textRenderer.getWidth(line), y + 57, MUTED, false);
        }
    }

    private void track(int x, int y, int w, int h, ItemStack s) {
        if (!s.isEmpty()) {
            hoverRects.add(new int[]{x, y, w, h});
            hoverStacks.add(s);
        }
    }

    private int leftTabY(int i) {
        return by + 10 + i * 24;
    }

    private int topTabX(int i) {
        return bx + 14 + i * 26;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------ 操作

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < cats.size(); i++) {
                if (inside(mouseX, mouseY, bx - 28, leftTabY(i), 28, 24)) {
                    open(i, 0);
                    click();
                    return true;
                }
            }
            if (!cats.isEmpty()) {
                for (int i = 0; i < cats.get(cat).entries.size(); i++) {
                    if (inside(mouseX, mouseY, topTabX(i), by - 24, 24, 24)) {
                        open(cat, i);
                        click();
                        return true;
                    }
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0) {
            next();
        } else if (verticalAmount > 0) {
            prev();
        }
        return true;
    }

    private void click() {
        if (client != null) {
            client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 266 || keyCode == 263) {   // Page Up / ←
            prev();
            return true;
        }
        if (keyCode == 267 || keyCode == 262) {   // Page Down / →
            next();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
