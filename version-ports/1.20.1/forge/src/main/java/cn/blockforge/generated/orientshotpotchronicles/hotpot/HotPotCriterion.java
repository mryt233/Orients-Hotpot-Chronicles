// 1.20.1 JSON条件触发器，对应主线同名event。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class HotPotCriterion extends AbstractCriterion<HotPotCriterion.Conditions> {
    public static final HotPotCriterion INSTANCE = new HotPotCriterion();
    public static final Identifier ID = GeneratedMod.id("hotpot");
    public static void register() { Criteria.register(INSTANCE); }
    public static void fire(PlayerEntity player, String event) {
        if (player instanceof ServerPlayerEntity sp) INSTANCE.trigger(sp, c -> c.event.equals(event));
    }
    @Override
    public Identifier getId() { return ID; }
    @Override
    protected Conditions conditionsFromJson(JsonObject json, LootContextPredicate player,
                                               AdvancementEntityPredicateDeserializer deserializer) {
        return new Conditions(player, JsonHelper.getString(json, "event"));
    }
    public static final class Conditions extends AbstractCriterionConditions {
        private final String event;
        public Conditions(LootContextPredicate player, String event) {
            super(ID, player);
            this.event = event;
        }
        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer serializer) {
            JsonObject json = super.toJson(serializer);
            json.addProperty("event", event);
            return json;
        }
    }
}
