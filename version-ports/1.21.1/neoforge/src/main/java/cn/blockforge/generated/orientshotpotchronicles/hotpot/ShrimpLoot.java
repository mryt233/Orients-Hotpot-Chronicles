//  移植：0.1.4-alpha 冻结底本，保留原作者署名。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.loot.LootTable;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * 钓鱼钓到虾（2026-10-26 用户：加虾，目前靠钓鱼获得）：往原版「钓鱼 · 鱼」那张表里唯一的池子加一项虾，权重 20
 * （同池原版：鳕鱼 60、鲑鱼 25、河豚 13、热带鱼 2），钓到鱼的时候约 1/6 是虾。不额外多掉东西。
 *
 */
public final class ShrimpLoot {
    public static final int WEIGHT = 20;
    private static final Identifier FISHING_FISH = Identifier.ofVanilla("gameplay/fishing/fish");

    private ShrimpLoot() {
    }

    public static void register() {
        // ： 决定以原版Codec重建原池，同池加权20，不额外多掉一条鱼。
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            if (!event.getKey().getValue().equals(FISHING_FISH)) return;
            JsonObject table = LootTable.CODEC.encodeStart(JsonOps.INSTANCE, event.getTable()).getOrThrow().getAsJsonObject();
            if (!table.has("pools") || table.getAsJsonArray("pools").isEmpty()) return;
            JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
            JsonObject shrimp = new JsonObject();
            shrimp.addProperty("type", "minecraft:item");
            shrimp.addProperty("name", GeneratedMod.id("shrimp").toString());
            shrimp.addProperty("weight", WEIGHT);
            pool.getAsJsonArray("entries").add(shrimp);
            event.setTable(LootTable.CODEC.parse(JsonOps.INSTANCE, table).getOrThrow());
        });
    }
}
