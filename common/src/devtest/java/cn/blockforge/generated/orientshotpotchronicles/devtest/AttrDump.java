package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 属性快照导出：把汤底、蘸料、饮料、锅材质的全部外观 / 数值 / 效果属性、
 * 已注册方块与物品的 ID 顺序、创造栏顺序导成一份 JSON，用于重构前后逐项比对。
 *
 * <p>只在开发环境（runServer / runClient）注册，整个 devtest 包不进 jar。</p>
 * 作者：mryt233
 */
public final class AttrDump {
    private AttrDump() {
    }

    /** /hpt dumpattrs &lt;名字&gt;：写到服务端运行目录的 hotpot-attrs-&lt;名字&gt;.json。 */
    public static String write(String name, ServerLevel level, LivingEntity subject) {
        JsonObject root = new JsonObject();
        root.add("soups", soups(subject));
        root.add("seasonings", seasonings(subject));
        root.add("drinks", drinks(subject));
        root.add("pot_materials", potMaterials());
        root.add("plate_materials", plateMaterials());
        root.add("soup_visuals", soupVisuals());
        root.add("block_ids", blockIds());
        root.add("item_ids", itemIds());
        root.add("creative_tab", creativeTab(level));
        root.add("config_defaults", cn.blockforge.generated.orientshotpotchronicles.hotpot.Config.editableCopy());
        String text = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
        Path out = Path.of("hotpot-attrs-" + name + ".json");
        try {
            Files.writeString(out, text, StandardCharsets.UTF_8);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
        return out.toAbsolutePath().toString();
    }

    private static JsonArray soups(LivingEntity subject) {
        JsonArray list = new JsonArray();
        for (SoupBase soup : SoupBase.values()) {
            JsonObject row = new JsonObject();
            row.addProperty("ordinal", soup.ordinal());
            row.addProperty("name", soup.name());
            row.addProperty("texture_key", soup.textureKey());
            row.addProperty("name_key", soup.nameKey());
            row.addProperty("speed", f(soup.speedMul()));
            row.addProperty("source", id(soup.source()));
            row.addProperty("remainder", id(soup.remainder()));
            row.addProperty("bowl_id", soup.bowlId());
            row.addProperty("bowl_hunger", soup.bowlHunger());
            row.addProperty("bowl_saturation", f(soup.bowlSaturation()));
            JsonArray effects = new JsonArray();
            for (int spicy = 0; spicy <= 3; spicy++) {
                JsonObject per = new JsonObject();
                per.addProperty("spicy", spicy);
                JsonArray entries = new JsonArray();
                for (MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("effect", effect.getEffect().getRegisteredName());
                    entry.addProperty("duration", effect.getDuration());
                    entry.addProperty("amplifier", effect.getAmplifier());
                    entry.addProperty("ambient", effect.isAmbient());
                    entry.addProperty("visible", effect.isVisible());
                    entries.add(entry);
                }
                per.add("effects", sorted(entries));
                effects.add(per);
            }
            row.add("bonus_effects_by_spicy", effects);
            // 汤碗注册时带在物品上的营养组件（客户端显示的默认值）
            for (Object value : HotPotContent.SOUP_BOWLS) {
                if (value instanceof SoupBowlItem bowl && bowl.soup() == soup) {
                    var food = new ItemStack(bowl).get(net.minecraft.core.component.DataComponents.FOOD);
                    JsonObject registered = new JsonObject();
                    registered.addProperty("nutrition", food == null ? -1 : food.nutrition());
                    registered.addProperty("saturation", food == null ? "-1" : f(food.saturation()));
                    row.add("registered_food", registered);
                }
            }
            row.addProperty("is_additive_stack", soup.isAdditive(new ItemStack(soup.remainder())));
            list.add(row);
        }
        return list;
    }

    private static JsonArray seasonings(LivingEntity subject) {
        JsonArray list = new JsonArray();
        for (Seasoning seasoning : Seasoning.ALL) {
            JsonObject row = new JsonObject();
            row.addProperty("ordinal", seasoning.ordinal());
            row.addProperty("id", seasoning.id());
            row.addProperty("color", String.format("#%08X", seasoning.color()));
            row.addProperty("bit", seasoning.bit());
            row.addProperty("name_key", seasoning.nameKey());
            row.addProperty("effect_key", seasoning.effectKey());
            JsonArray raws = new JsonArray();
            for (var raw : seasoning.raws()) {
                raws.add(id(raw));
            }
            row.add("raws", raws);
            // 实际吃下去的效果 + 多回的饥饿值
            subject.removeAllEffects();
            if (subject instanceof net.minecraft.world.entity.player.Player player) {
                player.getFoodData().setFoodLevel(10);
            }
            seasoning.apply(subject);
            JsonObject applied = new JsonObject();
            applied.addProperty("food_level", subject instanceof net.minecraft.world.entity.player.Player player
                ? player.getFoodData().getFoodLevel() : -1);
            JsonArray effects = new JsonArray();
            for (MobEffectInstance effect : subject.getActiveEffects()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("effect", effect.getEffect().getRegisteredName());
                entry.addProperty("duration", effect.getDuration());
                entry.addProperty("amplifier", effect.getAmplifier());
                effects.add(entry);
            }
            applied.add("effects", sorted(effects));
            subject.removeAllEffects();
            row.add("applied", applied);
            list.add(row);
        }
        return list;
    }

    /** 效果列表按「效果名」排一次：容器顺序不保证稳定，排序后才好逐项比对。 */
    private static JsonArray sorted(JsonArray effects) {
        java.util.List<com.google.gson.JsonElement> list = new java.util.ArrayList<>();
        effects.forEach(list::add);
        list.sort(java.util.Comparator.comparing(e -> e.getAsJsonObject().get("effect").getAsString()));
        JsonArray out = new JsonArray();
        list.forEach(out::add);
        return out;
    }

    private static JsonArray drinks(LivingEntity subject) {
        JsonArray list = new JsonArray();
        for (DrinkKind kind : DrinkKind.values()) {
            JsonObject row = new JsonObject();
            row.addProperty("ordinal", kind.ordinal());
            row.addProperty("id", kind.id());
            row.addProperty("bottle_height", f(kind.bottleHeight()));
            row.addProperty("has_bottle", kind.hasBottle());
            row.addProperty("serialized", kind.getSerializedName());
            row.addProperty("cup", id(HotPotContent.cupFor(kind)));
            row.addProperty("drink", kind.hasBottle() ? id(HotPotContent.drinkFor(kind)) : "none");
            row.addProperty("empty_bottle", id(HotPotContent.emptyBottle(kind)));
            subject.removeAllEffects();
            if (subject instanceof net.minecraft.world.entity.player.Player player) {
                player.getFoodData().setFoodLevel(10);
            }
            subject.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 200, 0));
            subject.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 200, 0));
            kind.applySip(subject);
            JsonObject applied = new JsonObject();
            JsonArray effects = new JsonArray();
            for (MobEffectInstance effect : subject.getActiveEffects()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("effect", effect.getEffect().getRegisteredName());
                entry.addProperty("duration", effect.getDuration());
                entry.addProperty("amplifier", effect.getAmplifier());
                effects.add(entry);
            }
            applied.add("effects", sorted(effects));
            subject.removeAllEffects();
            row.add("applied", applied);
            list.add(row);
        }
        return list;
    }

    private static JsonArray potMaterials() {
        JsonArray list = new JsonArray();
        for (HotPotMaterial material : HotPotMaterial.values()) {
            JsonObject row = new JsonObject();
            row.addProperty("ordinal", material.ordinal());
            row.addProperty("name", material.name());
            row.addProperty("id_path", material.idPath());
            row.addProperty("texture_key", material.textureKey());
            row.addProperty("metal", material.metal().name());
            row.addProperty("layout", material.layout().name());
            row.addProperty("model_key", material.modelKey());
            row.addProperty("instrument", material.instrument().name());
            row.addProperty("map_color", mapColor(material.mapColor()));
            row.addProperty("sound", sound(material.sounds()));
            row.addProperty("hardness", f(material.hardness()));
            row.addProperty("block", id(HotPotContent.blockFor(material)));
            row.addProperty("item", id(HotPotContent.itemFor(material)));
            list.add(row);
        }
        return list;
    }

    private static JsonArray plateMaterials() {
        JsonArray list = new JsonArray();
        for (PlateBlock plate : HotPotContent.PLATES) {
            if (plate.size() != PlateSize.S1X1) {
                continue;   // 每种材质取最小那一档看方块属性
            }
            JsonObject row = new JsonObject();
            row.addProperty("material", plate.material());
            var state = plate.defaultBlockState();
            row.addProperty("map_color", mapColor(state.getMapColor(null, null)));
            row.addProperty("sound", sound(state.getSoundType()));
            row.addProperty("hardness", f(state.getDestroySpeed(null, null)));
            row.addProperty("item", id(HotPotContent.PLATE_ITEMS.get(HotPotContent.PLATES.indexOf(plate))));
            list.add(row);
        }
        return list;
    }

    private static JsonArray soupVisuals() {
        JsonArray list = new JsonArray();
        for (SoupVisual visual : SoupVisual.values()) {
            JsonObject row = new JsonObject();
            row.addProperty("ordinal", visual.ordinal());
            row.addProperty("name", visual.name());
            row.addProperty("serialized", visual.getSerializedName());
            row.addProperty("has_soup", visual.hasSoup());
            list.add(row);
        }
        return list;
    }

    private static JsonArray blockIds() {
        JsonArray list = new JsonArray();
        for (ResourceLocation key : BuiltInRegistries.BLOCK.keySet()) {
            if (HotPot.MOD_ID.equals(key.getNamespace())) {
                list.add(key.toString());
            }
        }
        return list;
    }

    private static JsonArray itemIds() {
        JsonArray list = new JsonArray();
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            if (HotPot.MOD_ID.equals(key.getNamespace())) {
                list.add(key.toString());
            }
        }
        return list;
    }

    private static JsonArray creativeTab(ServerLevel level) {
        List<ItemStack> collected = new ArrayList<>();
        var parameters = new CreativeModeTab.ItemDisplayParameters(
            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, true, level.registryAccess());
        HotPotContent.fillCreativeTab(parameters, new CreativeModeTab.Output() {
            @Override
            public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
                collected.add(stack);
            }
        });
        JsonArray list = new JsonArray();
        for (ItemStack stack : collected) {
            list.add(id(stack.getItem()) + (stack.getCount() == 1 ? "" : " x" + stack.getCount()));
        }
        return list;
    }

    /** 地图颜色：原版 MapColor 是静态常量、没有名字，按字段名反查 + RGB 一起记，比对最稳。 */
    private static String mapColor(net.minecraft.world.level.material.MapColor color) {
        String name = "unknown";
        for (java.lang.reflect.Field field : net.minecraft.world.level.material.MapColor.class.getFields()) {
            try {
                if (field.getType() == net.minecraft.world.level.material.MapColor.class && field.get(null) == color) {
                    name = field.getName();
                    break;
                }
            } catch (IllegalAccessException ignored) {
                // 公开静态字段，取不到就继续
            }
        }
        return name + " " + String.format(java.util.Locale.ROOT, "#%06X",
            color.calculateRGBColor(net.minecraft.world.level.material.MapColor.Brightness.NORMAL) & 0xFFFFFF);
    }

    /** 声音类型：用破坏音的资源位置代表整个 SoundType（同一个 SoundType 所有音一致）。 */
    private static String sound(net.minecraft.world.level.block.SoundType type) {
        return type.getBreakSound().getLocation().toString();
    }

    private static String f(double value) {
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }

    private static String id(Object value) {
        if (value instanceof Block block) {
            return BuiltInRegistries.BLOCK.getKey(block).toString();
        }
        if (value instanceof net.minecraft.world.item.Item item) {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        return String.valueOf(value);
    }
}
