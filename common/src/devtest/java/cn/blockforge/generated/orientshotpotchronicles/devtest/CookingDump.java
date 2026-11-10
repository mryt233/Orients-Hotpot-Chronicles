// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotRecipeViewerData;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.nio.file.*;

/** 全物品烹饪规则与配方展示快照，仅开发环境使用。 */
public final class CookingDump {
    private CookingDump() { }
    public static String write(String name, ServerLevel level) {
        JsonObject root = new JsonObject();
        JsonObject items = new JsonObject();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack in = new ItemStack(item);
            JsonObject row = new JsonObject();
            row.addProperty("acceptable", HotPotCooking.acceptable(level, in));
            row.addProperty("output", stack(HotPotCooking.finish(level, in, SoupBase.CLEAR)));
            row.addProperty("ticks", HotPotCooking.cookTicks(level, in));
            row.addProperty("xp", HotPotCooking.xpFor(level, in));
            var over = HotPotCooking.overcookProduct(item);
            row.addProperty("overcook", over == null ? "" : BuiltInRegistries.ITEM.getKey(over).toString());
            row.addProperty("overcook_ticks", HotPotCooking.overcookTicks(level, item));
            var pound = Pounding.result(in);
            row.addProperty("pounding", pound == null ? "" : BuiltInRegistries.ITEM.getKey(pound).toString());
            row.addProperty("pounding_count", Pounding.resultCount(in));
            items.add(BuiltInRegistries.ITEM.getKey(item).toString(), row);
        }
        root.add("items", items);
        JsonArray cooking = new JsonArray();
        for (var e : HotPotRecipeViewerData.cookingRecipes(level)) {
            JsonObject row = new JsonObject();
            row.addProperty("id", e.id().toString()); row.addProperty("input", stack(e.input()));
            row.addProperty("output", stack(e.output())); row.addProperty("ticks", e.cookTicks());
            row.addProperty("xp", e.xp()); row.addProperty("overcook", stack(e.overcookOutput()));
            row.addProperty("overcook_ticks", e.overcookTotalTicks()); cooking.add(row);
        }
        root.add("viewer_cooking", cooking);
        JsonArray pounding = new JsonArray();
        for (var e : HotPotRecipeViewerData.poundingRecipes(level)) {
            JsonObject row = new JsonObject();
            row.addProperty("id", e.id().toString()); row.addProperty("input", stack(e.input()));
            row.addProperty("output", stack(e.output())); row.addProperty("hits", e.hits());
            row.addProperty("stage1", stack(e.stage1Display())); row.addProperty("stage2", stack(e.stage2Display()));
            pounding.add(row);
        }
        root.add("viewer_pounding", pounding);
        Path out = Path.of("hotpot-cooking-" + name + ".json");
        try { Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(root)); }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
        return out.toAbsolutePath().toString();
    }
    private static String stack(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + "*" + stack.getCount()
            + ":" + stack.getComponentsPatch();
    }
}
