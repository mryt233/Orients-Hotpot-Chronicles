// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.CupItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.ShrimpFishing;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第四批 4I / 4J / 4K 服务端实时探针（仅在 {@code devtest} 构建源生效，不打进发布 jar）。作者：mryt233
 */
public final class Opt4iProbe {
    private static final String[] GARNISH_IDS = {
        "scallion", "cilantro", "soybean", "garlic", "tomato",
        "chili_pepper", "peanut", "greens", "zhe_er_gen"
    };

    private Opt4iProbe() {
    }

    public static String run(ServerPlayer player, String action, BlockPos pos) {
        JsonObject out = switch (action) {
            case "config_state" -> configState(player);
            case "cheers" -> testCheers(player);
            case "coldpot" -> testColdPot(player, pos);
            case "spicy" -> testSpicy(player);
            case "loot" -> testLoot(player, pos, 2000);
            default -> {
                JsonObject err = new JsonObject();
                err.addProperty("error", "unknown action=" + action);
                yield err;
            }
        };
        return out.toString();
    }

    private static JsonObject configState(ServerPlayer player) {
        JsonObject out = new JsonObject();
        out.addProperty("warmth.interval_ticks", Config.warmthIntervalTicks());
        out.addProperty("warmth.range", Config.warmthRange());
        out.addProperty("warmth.duration", Config.warmthDuration());
        out.addProperty("warmth.amplifier", Config.warmthAmplifier());
        out.addProperty("cold_pot.nausea_chance", Config.coldPotNauseaChance());
        out.addProperty("cold_pot.nausea_duration", Config.coldPotNauseaDuration());
        out.addProperty("cheers.cooldown_ticks", Config.cheersCooldownTicks());
        out.addProperty("cheers.luck_duration", Config.cheersLuckDuration());
        out.addProperty("cheers.luck_amplifier", Config.cheersLuckAmplifier());
        out.addProperty("cheers.regen_duration", Config.cheersRegenDuration());
        out.addProperty("cheers.regen_amplifier", Config.cheersRegenAmplifier());
        out.addProperty("cheers.bonus.hunger", Config.cheersBonusHunger());
        out.addProperty("cheers.bonus.saturation", Config.cheersBonusSaturation());
        out.addProperty("spicy.level_duration_step", Config.spicyLevelDurationStep());
        out.addProperty("spicy.level_amplifier_from", Config.spicyLevelAmplifierFrom());
        out.addProperty("spicy.second_bonus_l1", Config.spicySecondBonusL1());
        out.addProperty("spicy.second_bonus_l2", Config.spicySecondBonusL2());
        out.addProperty("spicy.second_bonus_l3", Config.spicySecondBonusL3());
        out.addProperty("spicy.fire_from_level", Config.spicyFireFromLevel());
        out.addProperty("spicy.fire_top_multiplier", Config.spicyFireTopMultiplier());
        out.addProperty("spicy.hunger_top_bonus", Config.spicyHungerTopBonus());
        out.addProperty("spicy.mouth_fire_ticks", Config.spicyMouthFireTicks());
        out.addProperty("spicy.mouth_fire_from_level", Config.spicyMouthFireFromLevel());
        out.addProperty("loot.shrimp_fishing.weight", Config.lootShrimpFishingWeight());
        out.addProperty("loot.lily_pad.lotus_root.chance", Config.lootLilyPadLotusRootChance());
        out.addProperty("loot.mushroom.enoki.chance", Config.lootMushroomEnokiChance());
        out.addProperty("loot.cow.beef_loin.chance", Config.lootCowBeefLoinChance());
        out.addProperty("loot.cow.beef_loin.looting_bonus", Config.lootCowBeefLoinLootingBonus());
        JsonObject garnish = new JsonObject();
        for (String id : GARNISH_IDS) {
            garnish.addProperty(id, Config.lootGarnishChance(id, -1F));
        }
        out.add("loot.garnish", garnish);
        JsonArray effs = new JsonArray();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", inst.getEffect().getRegisteredName());
            e.addProperty("duration", inst.getDuration());
            e.addProperty("amplifier", inst.getAmplifier());
            e.addProperty("ambient", inst.isAmbient());
            effs.add(e);
        }
        out.add("effects", effs);
        out.addProperty("x", player.getX());
        out.addProperty("y", player.getY());
        out.addProperty("z", player.getZ());
        out.addProperty("fire_ticks", player.getRemainingFireTicks());
        return out;
    }

    private static JsonObject testCheers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.removeAllEffects();
        ItemStack cup = new ItemStack(DrinkKind.values()[0].cup());
        player.getCooldowns().removeCooldown(cup.getItem());
        CupItem.cheers(level, player, cup, null, null, player.getEyePosition());
        boolean onCooldown = player.getCooldowns().isOnCooldown(cup.getItem());
        boolean cheeredComp = Boolean.TRUE.equals(cup.get(HotPotContent.CHEERED));
        MobEffectInstance luck = player.getEffect(MobEffects.LUCK);
        MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);

        // 冷却期间再次干杯：应被冷却拦住，不再重加效果
        player.removeAllEffects();
        CupItem.cheers(level, player, cup, null, null, player.getEyePosition());
        boolean secondBlocked = !player.hasEffect(MobEffects.LUCK) && !player.hasEffect(MobEffects.REGENERATION);

        player.getFoodData().setFoodLevel(4);
        player.getFoodData().setSaturation(0F);
        ItemStack afterDrink = cup.finishUsingItem(level, player);
        int foodAfter = player.getFoodData().getFoodLevel();
        float satAfter = player.getFoodData().getSaturationLevel();

        player.getCooldowns().removeCooldown(cup.getItem());
        player.removeAllEffects();

        JsonObject out = new JsonObject();
        out.addProperty("on_cooldown", onCooldown);
        out.addProperty("cheered_comp", cheeredComp);
        out.addProperty("luck_present", luck != null);
        out.addProperty("luck_duration", luck != null ? luck.getDuration() : 0);
        out.addProperty("luck_amplifier", luck != null ? luck.getAmplifier() : -1);
        out.addProperty("regen_present", regen != null);
        out.addProperty("regen_duration", regen != null ? regen.getDuration() : 0);
        out.addProperty("regen_amplifier", regen != null ? regen.getAmplifier() : -1);
        out.addProperty("second_blocked", secondBlocked);
        out.addProperty("food_after", foodAfter);
        out.addProperty("saturation_after", satAfter);
        out.addProperty("returned_item", BuiltInRegistries.ITEM.getKey(afterDrink.getItem()).toString());
        return out;
    }

    private static JsonObject testColdPot(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!(level.getBlockEntity(pos) instanceof HotPotBlockEntity be)) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "no HotPotBlockEntity at " + pos);
            return err;
        }
        player.removeAllEffects();
        player.getFoodData().setFoodLevel(4);
        player.getFoodData().setSaturation(0F);

        HotPotScreenHandler menu = new HotPotScreenHandler(1, player.getInventory(), be);
        int cookedSlot = -1;
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            if (!menu.slots.get(i).getItem().isEmpty() && menu.isCooked(i)) {
                cookedSlot = i;
                break;
            }
        }
        if (cookedSlot < 0) {
            menu.removed(player);
            JsonObject err = new JsonObject();
            err.addProperty("error", "no cooked slot at " + pos);
            return err;
        }
        menu.setCarried(new ItemStack(HotPotContent.CHOPSTICKS));
        menu.clicked(cookedSlot, 0, ClickType.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);
        menu.removed(player);

        MobEffectInstance nausea = player.getEffect(MobEffects.CONFUSION);
        JsonObject out = new JsonObject();
        out.addProperty("heated", be.isHeated());
        out.addProperty("slot", cookedSlot);
        out.addProperty("has_nausea", nausea != null);
        out.addProperty("nausea_duration", nausea != null ? nausea.getDuration() : 0);
        player.removeAllEffects();
        return out;
    }

    private static JsonObject testSpicy(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.removeAllEffects();
        player.clearFire();
        player.getFoodData().setFoodLevel(4);
        player.getFoodData().setSaturation(0F);
        ItemStack bowl = Soups.bowlStack(SoupBase.SPICY.def());
        bowl.set(HotPotContent.SPICY_LEVEL, 3);
        bowl.finishUsingItem(level, player);

        JsonObject out = new JsonObject();
        out.addProperty("fire_ticks", player.getRemainingFireTicks());
        out.addProperty("food_after", player.getFoodData().getFoodLevel());
        out.addProperty("saturation_after", player.getFoodData().getSaturationLevel());
        JsonArray effs = new JsonArray();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", inst.getEffect().getRegisteredName());
            e.addProperty("duration", inst.getDuration());
            e.addProperty("amplifier", inst.getAmplifier());
            effs.add(e);
        }
        JsonArray dishBonus = new JsonArray();
        for (MobEffectInstance inst : SoupBase.SPICY.def().dishBonusEffects(3)) {
            JsonObject e = new JsonObject();
            e.addProperty("id", inst.getEffect().getRegisteredName());
            e.addProperty("duration", inst.getDuration());
            e.addProperty("amplifier", inst.getAmplifier());
            dishBonus.add(e);
        }
        out.add("dish_bonus", dishBonus);
        out.add("effects", effs);
        player.removeAllEffects();
        player.clearFire();
        return out;
    }

    private static JsonObject testLoot(ServerPlayer player, BlockPos pos, int runs) {
        ServerLevel level = player.serverLevel();
        var registries = level.getServer().reloadableRegistries();
        Vec3 center = Vec3.atCenterOf(pos);
        ItemStack hoe = new ItemStack(Items.WOODEN_HOE);
        ItemStack rod = new ItemStack(Items.FISHING_ROD);

        LootTable grassTable = registries.getLootTable(Blocks.SHORT_GRASS.getLootTable());
        LootParams grassParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, center)
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.SHORT_GRASS.defaultBlockState())
            .withParameter(LootContextParams.TOOL, hoe)
            .create(LootContextParamSets.BLOCK);

        LootTable lilyTable = registries.getLootTable(Blocks.LILY_PAD.getLootTable());
        LootParams lilyParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, center)
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.LILY_PAD.defaultBlockState())
            .withParameter(LootContextParams.TOOL, hoe)
            .create(LootContextParamSets.BLOCK);

        LootTable mushroomTable = registries.getLootTable(Blocks.BROWN_MUSHROOM.getLootTable());
        LootParams mushroomParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, center)
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.BROWN_MUSHROOM.defaultBlockState())
            .withParameter(LootContextParams.TOOL, hoe)
            .create(LootContextParamSets.BLOCK);

        Cow cow = EntityType.COW.create(level);
        LootTable cowTable = registries.getLootTable(EntityType.COW.getDefaultLootTable());
        LootParams cowParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, cow)
            .withParameter(LootContextParams.ORIGIN, center)
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
            .create(LootContextParamSets.ENTITY);

        LootTable fishTable = registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ShrimpFishing.FISHING_FISH));
        LootParams fishParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, center)
            .withParameter(LootContextParams.TOOL, rod)
            .create(LootContextParamSets.FISHING);

        Map<String, Integer> grassCounts = sample(grassTable, grassParams, runs);
        Map<String, Integer> lilyCounts = sample(lilyTable, lilyParams, runs);
        Map<String, Integer> mushCounts = sample(mushroomTable, mushroomParams, runs);
        Map<String, Integer> cowCounts = sample(cowTable, cowParams, runs);
        Map<String, Integer> fishCounts = sample(fishTable, fishParams, runs);
        if (cow != null) {
            cow.discard();
        }

        JsonObject out = new JsonObject();
        out.addProperty("runs", runs);
        out.add("short_grass", toJson(grassCounts));
        out.add("lily_pad", toJson(lilyCounts));
        out.add("brown_mushroom", toJson(mushCounts));
        out.add("cow", toJson(cowCounts));
        out.add("fishing_fish", toJson(fishCounts));
        return out;
    }

    private static Map<String, Integer> sample(LootTable table, LootParams params, int runs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < runs; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private static JsonObject toJson(Map<String, Integer> counts) {
        JsonObject obj = new JsonObject();
        counts.forEach(obj::addProperty);
        return obj;
    }
}
