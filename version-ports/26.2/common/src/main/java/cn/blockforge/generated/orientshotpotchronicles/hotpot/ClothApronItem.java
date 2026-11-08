package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 布围裙（2026-11-06 用户定）：胸甲部位的布制防具，防溅油、防烫嘴。
 *
 * <p>26.x 没有 {@code ArmorItem} 类了，护甲改成「物品属性上挂装备信息 + 自带护甲材质」：
 * {@code properties.humanoidArmor(ArmorMaterial, ArmorType)} 一次给齐装备槽、装备音、耐久与护甲属性，
 * 所以本类直接继承 {@link Item}，自己造一份「轻质布料」材质——防御照皮革（3 点）、耐久 80、
 * 附魔性 15、皮革装备音，护甲层贴图指向本模组的
 * {@code assets/orients_hotpot_chronicles/equipment/cloth_apron.json} →
 * {@code textures/entity/equipment/humanoid/cloth_apron.png}（浅亚麻布 + 白色挂脖绳），
 * 穿起来是布围裙的样子。</p>
 *
 * <p>mryt233 改（2026-11-08）：26.x 的 {@link ArmorMaterial} 是纯 record（不再进注册表，直接当参数传），
 * 护甲层贴图从 {@code textures/models/armor/<名字>_layer_N.png} 挪到装备资产
 * {@code assets/<命名空间>/equipment/<名字>.json} 指向的 {@code textures/entity/equipment/<类型>/<贴图>.png}。</p>
 *
 * <p>防护特性（用户要的两条）：</p>
 * <ol>
 *   <li><b>免疫变态辣烫嘴</b>：没有别的模组干扰时，红汤变态辣（辣度 3）菜品吃下去会在嘴上点 2 秒火苗
 *       （见 {@code FoodConsumeMixin} / {@code SoupBowlItem}）；穿着布围裙时不再点火苗。</li>
 *   <li><b>防热油 / 沸水溅射</b>：站在烧着的灶台或点着的篝火（热源）旁不会因为明火受伤、不被点着，
 *       见 {@link ClothApronProtection#tickOne}。</li>
 * </ol>
 *
 * <p>修复：白色羊毛或线（{@link #isValidRepairItem}），和工作台配方一致。
 * 作者：mryt233 */
public class ClothApronItem extends Item {
    /** 布围裙材质 / 装备资产的注册 ID。 */
    public static final Identifier MATERIAL_ID = HotPot.id("cloth_apron");
    /** 胸部防御点数（和原版皮革胸甲一致：3 点）。 */
    public static final int DEFENSE = 3;
    /** 耐久（和原版皮革胸甲一致：80）。 */
    public static final int DURABILITY = 80;
    /** 附魔性（皮革胸甲是 15）。 */
    public static final int ENCHANTMENT_VALUE = 15;

    /** 修复材料标签：白色羊毛或线（和工作台配方一致）。 */
    private static final net.minecraft.tags.TagKey<Item> REPAIR_TAG =
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, HotPot.id("cloth_apron_repair"));

    /** 轻质布料材质（纯 record，构造一次留着复用）。 */
    private static final ArmorMaterial MATERIAL = new ArmorMaterial(
        DURABILITY,
        Map.of(ArmorType.CHESTPLATE, DEFENSE),
        ENCHANTMENT_VALUE,
        SoundEvents.ARMOR_EQUIP_LEATHER,
        0.0F,   // 韧性：布做的没有
        0.0F,   // 击退抗性：同上
        REPAIR_TAG,   // 修复材料：白色羊毛或线（见 data/orients_hotpot_chronicles/tags/item/cloth_apron_repair.json）
        ResourceKey.create(EquipmentAssets.ROOT_ID, MATERIAL_ID));

    public ClothApronItem(Item.Properties properties) {
        super(properties
            .durability(DURABILITY)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers())
            .humanoidArmor(MATERIAL, ArmorType.CHESTPLATE));
    }

    /** 胸甲属性：护甲 +3、无韧性无击退抗性（布做的，和皮革一样）。 */
    private static ItemAttributeModifiers modifiers() {
        return ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR,
                new AttributeModifier(HotPot.id("cloth_apron_armor"), DEFENSE, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.CHEST)
            .build();
    }

    /** 材料的防御点数（别处问数值时用）。 */
    public int getDefense() {
        return DEFENSE;
    }

    // mryt233 改（2026-11-08）：26.x 去掉了 Item.isValidRepairItem，修复材料改由材质的
    // repairIngredient 标签（orients_hotpot_chronicles:cloth_apron_repair = 羊毛 + 线）声明。

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron"))
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
