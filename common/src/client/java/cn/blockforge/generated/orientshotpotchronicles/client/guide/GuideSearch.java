// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

// mryt233 改（2026-11-11）：图标和排版职责迁出。
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideLayout.Elem;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideLayout.Kind;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent.Entry;
import cn.blockforge.generated.orientshotpotchronicles.client.guide.GuideContent.Category;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 作者：mryt233；2026-11-11：指南全文索引、命中与定位。 */
public final class GuideSearch {
    public record SearchDocument(int cat, int entry, String title, String body) {
    }

    public record SearchHit(int cat, int entry, String snippet) {
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
            text.append(GuideItems.of(ids.getAsString()).getHoverName().getString()).append(' ');
        }
    }

    public static List<SearchDocument> build(List<Category> cats) {
        List<SearchDocument> searchIndex = new ArrayList<>();
        for (int ci = 0; ci < cats.size(); ci++) {
            Category category = cats.get(ci);
            for (int ei = 0; ei < category.entries().size(); ei++) {
                Entry item = category.entries().get(ei);
                StringBuilder body = new StringBuilder(item.icon().getHoverName().getString()).append(' ');
                JsonObject json = item.json();
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
                searchIndex.add(new SearchDocument(ci, ei, category.title().getString() + " · " + item.title().getString(),
                    body.toString().strip()));
            }
        }
        return searchIndex;
    }

    public static List<SearchHit> run(List<SearchDocument> searchIndex, String searchQuery) {
        List<SearchHit> searchHits = new ArrayList<>();
        String query = searchQuery.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return searchHits;
        }
        // 标题命中优先，同分保留书中分类、条目的顺序。
        for (int rank = 0; rank < 2; rank++) {
            for (SearchDocument doc : searchIndex) {
                boolean titleHit = doc.title().toLowerCase(Locale.ROOT).contains(query);
                int at = doc.body.toLowerCase(Locale.ROOT).indexOf(query);
                if ((titleHit ? 0 : 1) != rank || (!titleHit && at < 0)) {
                    continue;
                }
                int from = Math.max(0, at - 12);
                String snippet = (from > 0 ? "…" : "") + doc.body.substring(from);
                searchHits.add(new SearchHit(doc.cat, doc.entry, snippet));
            }
        }
        return searchHits;
    }

    private static String elementText(Elem el) {
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
        for (ResourceLocation id : el.recipes) {
            text.append(' ').append(GuideItems.of(id.toString()).getHoverName().getString());
        }
        return text.toString();
    }

    public static int locate(List<List<Elem>> columns, String searchQuery, int spread) {
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
        return spread;
    }
}
