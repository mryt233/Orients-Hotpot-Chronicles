// mryt233 改：26.1 护甲材质改为记录式 ArmorMaterial（不再走 ARMOR_MATERIAL 注册表），
// 护甲层贴图改由 assets/<ns>/equipment/cloth_apron.json 描述。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * 布围裙（2026-11-06 用户定）：胸甲部位的布制防具，防溅油、防烫嘴。
 *
 * <p>但护甲层贴图指向本模组的装备资源
 * {@code assets/orients_hotpot_chronicles/equipment/<款式>.json} →
 * {@code textures/entity/equipment/humanoid/<款式>.png}（浅亚麻布 + 白色挂脖绳），穿起来是布围裙的样子。</p>
 *
 * <p>mryt233 改（2026-11-09，r146b）：分成三款——米白粗布、蓝印花、棕边带兜。
 * 三款的数值、耐久、附魔性、修复材料、防护效果完全一样，只有装备资源和名字不同；
 * 用工作台加染料可以互相换款（配方见 {@link ApronRecolorRecipe}），换款保留耐久 / 附魔 / 自定义名字。
 * 判断「穿的是不是布围裙」统一用物品标签 {@code orients_hotpot_chronicles:cloth_aprons}，
 * 不要直接比 {@code HotPotContent.CLOTH_APRON} 这一个常量。</p>
 *
 * <p>mryt233 改（26.1）：26.x 的 {@link ArmorMaterial} 是记录（含耐久倍率、防御表、附魔性、装备音、
 * 韧性、击退抗性、修复标签、装备资源键），代码里直接构造即可，不再需要注册进 26.1 之前的
 * {@code ARMOR_MATERIAL} 静态注册表；护甲层贴图改由 {@code assets/orients_hotpot_chronicles/equipment/cloth_apron.json}
 * 描述（26.x 用 {@code EquipmentAsset} 注册表 + {@code EquipmentClientInfo} 资源）。</p>
 *
 * <p>防护特性（用户要的两条）：</p>
 * <ol>
 *   <li><b>免疫变态辣烫嘴</b>：红汤变态辣（辣度 3）菜品吃下去会在嘴上点 2 秒火苗
 *       （见 {@code LivingEntityEatMixin} / {@code SoupBowlItem}）；穿着布围裙时不再点火苗。</li>
 *   <li><b>防热油 / 沸水溅射</b>：站在烧着的灶台或点着的篝火（热源）旁不会因为明火受伤、不被点着，
 *       见 {@link ClothApronProtection#tickOne}。</li>
 * </ol>
 *
 * <p>修复：白色羊毛或线（材质里的 repairIngredient 标签 cloth_apron_repair），和工作台配方一致。
 * 作者：mryt233 */
public class ClothApronItem extends Item {
    /** 米白款装备资源的注册键（其余两款的 ID 是 {@code <款式 ID>}，见 {@link Style}）。 */
    public static final ResourceKey<EquipmentAsset> ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, HotPot.id("cloth_apron"));
    /** 三款布围裙共用的物品标签：所有「穿没穿围裙」的判断都认它（r146b）。 */
    public static final net.minecraft.tags.TagKey<Item> APRON_TAG =
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            HotPot.id("cloth_aprons"));
    /** 胸部防御点数（和原版皮革胸甲一致：3 点）。 */
    public static final int DEFENSE = 3;
    /** 耐久倍率：ArmorType.CHESTPLATE 的单位耐久 16 × 5 = 80，和原版皮革胸甲一致。 */
    public static final int DURABILITY_MULTIPLIER = 5;
    /** 附魔性（皮革胸甲是 15）。 */
    public static final int ENCHANTMENT_VALUE = 15;

    /** 修复材料标签：白色羊毛或线（和工作台配方一致）。 */
    private static final net.minecraft.tags.TagKey<Item> REPAIR_TAG =
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            HotPot.id("cloth_apron_repair"));

    /** 三款各一份轻质布料材质（纯 record，构造一次留着复用），key 是款式。 */
    private static final Map<Style, ArmorMaterial> MATERIALS = new EnumMap<>(Style.class);

    /**
     * 三款布围裙（r146b，2026-11-09 用户定）。顺序 = 工作台里换款的颜色顺序（米白 → 蓝印花 → 棕边带兜）。
     * <p>{@code id} 既是物品注册名，也是装备资产的名字
     * （{@code assets/<命名空间>/equipment/<id>.json} 指向 {@code textures/entity/equipment/humanoid/<id>.png}）。
     */
    public enum Style {
        /** 米白粗布：默认款，沿用原来的 ID。 */
        LINEN("cloth_apron"),
        /** 蓝印花：靛蓝底、白色十字小花。 */
        BLUE_PRINT("blue_print_cloth_apron"),
        /** 棕边带兜：米白底、深棕滚边、下面一个方框口袋。 */
        BROWN_TRIM("brown_trim_cloth_apron");

        private final String id;

        Style(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** 这一件是哪款（认不出来时按米白款算）。 */
    public Style style() {
        return style;
    }

    private final Style style;

    /** 按款式取材质：第一次用到时造一份，之后复用。 */
    private static synchronized ArmorMaterial material(Style style) {
        ArmorMaterial cached = MATERIALS.get(style);
        if (cached == null) {
            cached = new ArmorMaterial(
                DURABILITY_MULTIPLIER,
                java.util.Collections.unmodifiableMap(new EnumMap<>(Map.of(ArmorType.CHESTPLATE, DEFENSE))),
                ENCHANTMENT_VALUE,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                0.0F,   // 韧性：布做的没有
                0.0F,   // 击退抗性：同上
                REPAIR_TAG,   // 修复材料：白色羊毛或线
                ResourceKey.create(EquipmentAssets.ROOT_ID, HotPot.id(style.id())));
            MATERIALS.put(style, cached);
        }
        return cached;
    }

    /** 26.x 的注册走框架给的带注册键 Properties，构造器收 Properties（r146b 加款式参数）。 */
    public ClothApronItem(Style style, Item.Properties properties) {
        super(properties.humanoidArmor(material(style), ArmorType.CHESTPLATE));
        this.style = style;
    }

    /** 默认款（米白）：别的代码按无参 / 单参构造时用这一款。 */
    public ClothApronItem(Item.Properties properties) {
        this(Style.LINEN, properties);
    }

    // mryt233 改（26.1）：26.x 去掉了 Item.isValidRepairItem，修复材料改由材质的
    // repairIngredient 标签（orients_hotpot_chronicles:cloth_apron_repair = 羊毛 + 线）声明。

    /** 材料的防御点数（别处问数值时用）。 */
    public int getDefense() {
        return DEFENSE;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, lines, flag);
        lines.accept(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron"))
            .withStyle(ChatFormatting.GRAY));
        // mryt233 改（2026-11-09，r146b）：三款都能用染料换色，说明里写清怎么换
        lines.accept(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron_style"))
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 穿着布围裙（胸甲槽里是任意一款）就返回 true；别的部位 / 没穿返回 false。
     * mryt233 改（2026-11-09，r146b）：认物品标签 {@code orients_hotpot_chronicles:cloth_aprons}，
     * 三款都在标签里，以后再加款也只改标签，不用回来改这段判断。
     */
    public static boolean wornBy(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return isApron(chest);
    }

    /** 这一叠东西是不是布围裙（任意一款）：认 {@code cloth_aprons} 标签。 */
    public static boolean isApron(ItemStack stack) {
        return !stack.isEmpty() && stack.is(APRON_TAG);
    }

    /** 这个玩家穿着布围裙。 */
    public static boolean wornBy(Player player) {
        return wornBy((LivingEntity) player);
    }
}
