// 作者：；保持主线菜刀总攻击 4、攻击速度 1.8 和耐久 250。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;

public final class KitchenKnifeItem extends Item {
    private final Multimap<EntityAttribute, EntityAttributeModifier> modifiers = ImmutableMultimap.of(
        EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID,
            "Weapon modifier", 3.0, EntityAttributeModifier.Operation.ADDITION),
        EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID,
            "Weapon modifier", -2.2, EntityAttributeModifier.Operation.ADDITION));
    public KitchenKnifeItem() { super(new Item.Settings().maxDamage(250)); }
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? modifiers : ImmutableMultimap.of();
    }
}
