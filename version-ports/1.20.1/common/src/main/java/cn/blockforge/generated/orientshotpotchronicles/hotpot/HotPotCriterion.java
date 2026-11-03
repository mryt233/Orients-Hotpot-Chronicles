package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;

/** 1.20.1 JSON 条件触发器，保留主线事件名。作者：mryt233 */
public class HotPotCriterion extends SimpleCriterionTrigger<HotPotCriterion.Conditions> {
    public static final HotPotCriterion INSTANCE = new HotPotCriterion();
    public static final ResourceLocation ID = HotPot.id("hotpot");
    public static void register() { cn.blockforge.orient.framework.compat.mixin.CriteriaTriggersAccessor.register(INSTANCE); }
    public static void fire(Player player, String event) {
        if (player instanceof ServerPlayer sp) INSTANCE.trigger(sp, c -> c.event.equals(event));
    }
    @Override public ResourceLocation getId() { return ID; }
    @Override protected Conditions createInstance(JsonObject json, ContextAwarePredicate player,
                                                   DeserializationContext context) {
        return new Conditions(player, GsonHelper.getAsString(json, "event"));
    }
    public static final class Conditions extends AbstractCriterionTriggerInstance {
        private final String event;
        public Conditions(ContextAwarePredicate player, String event) { super(ID, player); this.event = event; }
        @Override public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("event", event);
            return json;
        }
    }
}
