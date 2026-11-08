// mryt233 改：26.1 护甲材质改为记录式 ArmorMaterial（不再走 ARMOR_MATERIAL 注册表），
// 护甲层贴图改由 assets/<ns>/equipment/cloth_apron.json 描述。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
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
import java.util.List;
import java.util.Map;

/**
 * 布围裙（2026-11-06 用户定）：胸甲部位的布制防具，防溅油、防烫嘴。
 *
 * <p>为什么不用原版皮革材质：原版皮革会把皮革的护甲层贴图套在身上，那样穿布围裙看着就是一件皮甲。
 * 所以这里自己造一份「轻质布料」材质——防御数值照皮革（3 点）、耐久 80、附魔性 15、皮革装备音，
 * 但护甲层贴图指向本模组的 {@code textures/entity/equipment/humanoid/cloth_apron.png}
 * （浅亚麻布 + 白色挂脖绳），穿起来是布围裙的样子。</p>
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
 * <p>修复：白色羊毛或线（{@link #isValidRepairItem}），和工作台配方一致。
 * 作者：mryt233 */
public class ClothApronItem extends Item {
    /** 布围裙装备资源的注册键（对应 assets/orients_hotpot_chronicles/equipment/cloth_apron.json）。 */
    public static final ResourceKey<EquipmentAsset> ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, HotPot.id("cloth_apron"));

    /** 胸部防御点数（和原版皮革胸甲一致：3 点）。 */
    public static final int DEFENSE = 3;
    /** 耐久倍率：ArmorType.CHESTPLATE 的单位耐久 16 × 5 = 80，和原版皮革胸甲一致。 */
    public static final int DURABILITY_MULTIPLIER = 5;
    /** 附魔性（皮革胸甲是 15）。 */
    public static final int ENCHANTMENT_VALUE = 15;

    /** 轻质布料材质：只有胸甲一层防御，护甲层贴图由 {@link #ASSET} 指向本模组的装备资源。 */
    private static final ArmorMaterial MATERIAL = new ArmorMaterial(
        DURABILITY_MULTIPLIER,
        java.util.Collections.unmodifiableMap(new EnumMap<>(Map.of(ArmorType.CHESTPLATE, DEFENSE))),
        ENCHANTMENT_VALUE,
        SoundEvents.ARMOR_EQUIP_LEATHER,
        0.0F,   // 韧性：布做的没有
        0.0F,   // 击退抗性：同上
        ItemTags.WOOL,   // 修复材料：白色羊毛 / 线（isValidRepairItem 里再放行线）
        ASSET);

    /** 物品属性里要带的护甲材质（HotPotContent 注册时用 humanoidArmor 挂上去）。 */
    public static ArmorMaterial material() {
        return MATERIAL;
    }

    public ClothApronItem(Item.Properties properties) {
        super(properties.humanoidArmor(MATERIAL, ArmorType.CHESTPLATE));
    }

    // mryt233 改（26.1）：26.x 的修复材料由 ArmorMaterial.repairIngredient 标签决定（已设为 ItemTags.WOOL），
    // isValidRepairItem 不再存在；线上下面用原版羊毛修，线由重写 repairIngredient 做不到，故只保留羊毛与超类判定。

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, lines, flag);
        lines.accept(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron"))
            .withStyle(ChatFormatting.GRAY));
    }

    /** 穿着布围裙（胸甲槽是它）就返回 true；别的部位 / 没穿返回 false。 */
    public static boolean wornBy(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof ClothApronItem;
    }

    /** 这个玩家穿着布围裙。 */
    public static boolean wornBy(Player player) {
        return wornBy((LivingEntity) player);
    }
}
