package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;

/** 菜刀仍是普通物品，只提供原有铁质攻击属性。作者：mryt233 */
public final class KitchenKnifeItem extends Item {
    private final Multimap<Attribute, AttributeModifier> attributes = ImmutableMultimap.of(
        Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier",
            1 + Tiers.IRON.getAttackDamageBonus(), AttributeModifier.Operation.ADDITION),
        Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier",
            -2.2F, AttributeModifier.Operation.ADDITION));
    public KitchenKnifeItem() { super(new Item.Properties().durability(250)); }
    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributes : super.getDefaultAttributeModifiers(slot);
    }
}
