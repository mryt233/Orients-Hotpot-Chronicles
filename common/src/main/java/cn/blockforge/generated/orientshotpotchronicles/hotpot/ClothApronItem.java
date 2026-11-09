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
 * <p>mryt233 改（2026-11-09，r146b）：分成三款——米白粗布、蓝印花、棕边带兜。
 * 三款的数值、耐久、附魔性、修复材料、防护效果完全一样，只有护甲层贴图和名字不同；
 * 用工作台加染料可以互相换款（配方见 {@link ApronRecolorRecipe}），换款保留耐久 / 附魔 / 自定义名字。
 * 判断「穿的是不是布围裙」统一用物品标签 {@code orients_hotpot_chronicles:cloth_aprons}，
 * 不要直接比 {@code HotPotContent.CLOTH_APRON} 这一个常量。</p>
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
    /** 米白款材质的注册 ID（其余两款的 ID 是 {@code <款式 ID>}，见 {@link Style}）。 */
    public static final ResourceLocation MATERIAL_ID = HotPot.id("cloth_apron");
    /** 三款布围裙共用的物品标签：所有「穿没穿围裙」的判断都认它（r146b）。 */
    public static final net.minecraft.tags.TagKey<Item> APRON_TAG =
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            HotPot.id("cloth_aprons"));
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
    private static final Map<Style, net.minecraft.core.Holder<ArmorMaterial>> MATERIALS =
        new EnumMap<>(Style.class);

    /**
     * 三款布围裙（r146b，2026-11-09 用户定）。顺序 = 工作台里换款的颜色顺序（米白 → 蓝印花 → 棕边带兜）。
     * {@code id} 既是物品注册名，也是盔甲材质的注册名和护甲层贴图名（{@code textures/models/armor/<id>_layer_1.png}）。
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

    public ClothApronItem() {
        this(Style.LINEN);
    }

    public ClothApronItem(Style style) {
        super(material(style), ArmorItem.Type.CHESTPLATE, new Item.Properties()
            .durability(DURABILITY)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers()));
        this.style = style;
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
    private static synchronized net.minecraft.core.Holder<ArmorMaterial> material(Style style) {
        net.minecraft.core.Holder<ArmorMaterial> cached = MATERIALS.get(style);
        if (cached == null) {
            ArmorMaterial value = new ArmorMaterial(
                new EnumMap<>(Map.of(
                    ArmorItem.Type.CHESTPLATE, DEFENSE)),
                ENCHANTMENT_VALUE,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("white_wool"))),
                // 三款各一层贴图：ArmorMaterial.Layer 会去找 textures/models/armor/<id>_layer_1.png
                List.of(new ArmorMaterial.Layer(HotPot.id(style.id()))),
                0.0F,   // 韧性：布做的没有
                0.0F);  // 击退抗性：同上
            // 注册进静态注册表并直接拿到 Holder（ArmorItem 的构造要的是 Holder，不是材质本体）
            cached = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, HotPot.id(style.id()), value);
            MATERIALS.put(style, cached);
        }
        return cached;
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
        // mryt233 改（2026-11-09，r146b）：三款都能用染料换色，说明里写清怎么换
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron_style"))
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
