package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 布围裙（2026-11-06 用户定）：胸甲部位的布制防具，防溅油、防烫嘴。
 *
 * <p>为什么不用 {@code new ArmorItem(ArmorMaterials.LEATHER, Type.CHESTPLATE, props)} 就完事：
 * 原版皮革材质会把皮革的护甲层贴图（{@code textures/models/armor/leather_layer_1.png}）套在身上，
 * 那样穿布围裙看着就是一件皮甲。所以这里自己造一份「轻质布料」材质——防御数值照皮革（3 点）、
 * 耐久 80、附魔性 15、皮革装备音，但护甲层贴图指向本模组的
 * {@code textures/models/armor/cloth_apron_layer_1.png}（浅亚麻布 + 白色挂脖绳），穿起来是布围裙的样子。</p>
 *
 * <p>材质在代码里用 {@link Registry#register} 直接注册进 {@code Registries.ARMOR_MATERIAL}
 * （1.21.1 的护甲材质是数据驱动的静态注册表，代码里能直接注册，不依赖数据包）。</p>
 *
 * <p>防护特性（用户要的两条）：</p>
 * <ol>
 *   <li><b>免疫变态辣烫嘴</b>：没有别的模组干扰时，红汤变态辣（辣度 3）菜品吃下去会在嘴上点 2 秒火苗
 *       （见 {@code LivingEntityEatMixin} / {@code SoupBowlItem}）；穿着布围裙时不再点火苗。</li>
 *   <li><b>防热油 / 沸水溅射</b>：站在烧着的灶台或点着的篝火（热源）旁不会因为明火受伤、不被点着，
 *       见 {@link ClothApronProtection#tickOne}（mryt233 改：方法已经改名，这里跟着修好指向）。</li>
 * </ol>
 *
 * <p>修复：白色羊毛或线（{@link #isValidRepairItem}），和工作台配方一致。
 * 作者：mryt233 */
public class ClothApronItem extends ArmorItem {
    /** 布围裙材质的注册 ID。 */
    public static final ResourceLocation MATERIAL_ID = HotPot.id("cloth_apron");
    /** 胸部防御点数（和原版皮革胸甲一致：3 点）。 */
    public static final int DEFENSE = 3;
    /** 耐久（和原版皮革胸甲一致：80）。 */
    public static final int DURABILITY = 80;
    /** 附魔性（皮革胸甲是 15）。 */
    public static final int ENCHANTMENT_VALUE = 15;

    /**
     * 轻质布料材质：只有一层不染色的护甲层贴图，贴图路径就是本模组的
     * {@code textures/models/armor/cloth_apron_layer_1.png}（{@link ArmorMaterial.Layer} 会用
     * {@code textures/models/armor/<路径>_layer_<1|2>.png} 去找）。null 表示还没注册（注册顺序兜底）。
     */
    private static net.minecraft.core.Holder<ArmorMaterial> material;

    public ClothApronItem() {
        super(material(), ArmorItem.Type.CHESTPLATE, new Item.Properties()
            .durability(DURABILITY)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers()));
    }

    /** 胸甲属性：护甲 +3、无韧性无击退抗性（布做的，和皮革一样）。 */
    private static ItemAttributeModifiers modifiers() {
        return ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR,
                new AttributeModifier(HotPot.id("cloth_apron_armor"), DEFENSE, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.CHEST)
            .build();
    }

    /**
     * 取材质 Holder：第一次用到时把材质注册进静态注册表。材质里的护甲层贴图在本模组命名空间下，
     * 修复材料是白色羊毛或线（和配方一致）。
     */
    private static synchronized net.minecraft.core.Holder<ArmorMaterial> material() {
        if (material == null) {
            ArmorMaterial value = new ArmorMaterial(
                new EnumMap<>(Map.of(
                    ArmorItem.Type.CHESTPLATE, DEFENSE)),
                ENCHANTMENT_VALUE,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("white_wool"))),
                List.of(new ArmorMaterial.Layer(MATERIAL_ID)),
                0.0F,   // 韧性：布做的没有
                0.0F);  // 击退抗性：同上
            // 注册进静态注册表并直接拿到 Holder（ArmorItem 的构造要的是 Holder，不是材质本体）
            material = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, MATERIAL_ID, value);
        }
        return material;
    }

    /** 材料的防御只写了胸甲这一项，别的部位（万一以后加）按 0 算，不会抛异常。 */
    @Override
    public int getDefense() {
        return DEFENSE;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        // 用户定：白色羊毛或线都能修（和工作台配方一致）
        return repair.is(ItemTags.WOOL) || repair.is(net.minecraft.world.item.Items.STRING)
            || super.isValidRepairItem(stack, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron"))
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
