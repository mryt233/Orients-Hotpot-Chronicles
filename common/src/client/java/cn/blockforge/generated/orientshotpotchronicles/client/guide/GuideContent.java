// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.network.chat.Component;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 作者：mryt233；2026-11-11：指南目录的数据记录。 */
public final class GuideContent {
    private GuideContent() { }
    public record Entry(Component title, ItemStack icon, JsonObject json) {
    }

    public record Category(Component title, ItemStack icon, List<Entry> entries) {
    }

    /** 读过的书（完整教程不小，改窗口大小时 init 会重跑，别每次都解析一遍）。 */
    private static String cachedLang;
    private static JsonObject cachedBook;
    public static JsonObject load() {
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

    private static JsonObject read(String lang) {
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

    public static List<Category> build() {
        List<Category> cats = new ArrayList<>();
        for (JsonElement c : load().getAsJsonArray("categories")) {
            JsonObject co = c.getAsJsonObject();
            List<Entry> entries = new ArrayList<>();
            for (JsonElement e : co.getAsJsonArray("entries")) {
                JsonObject eo = e.getAsJsonObject();
                ItemStack icon = GuideItems.of(eo.get("icon").getAsString());
                Component t = eo.has("title") ? Component.literal(eo.get("title").getAsString()) : icon.getHoverName();
                entries.add(new Entry(t, icon, eo));
            }
            cats.add(new Category(Component.literal(co.get("title").getAsString()), GuideItems.of(co.get("icon").getAsString()), entries));
        }
        return cats;
    }

    public static String ui(String simplified, String traditional, String english) {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        return lang.equals("zh_tw") || lang.equals("zh_hk") ? traditional : lang.startsWith("zh") ? simplified : english;
    }
}
