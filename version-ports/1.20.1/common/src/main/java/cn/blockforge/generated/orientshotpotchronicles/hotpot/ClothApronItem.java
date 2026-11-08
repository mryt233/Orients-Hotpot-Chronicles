package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 布围裙（清单 #127）：胸甲部位的布制防具，防溅油、防烫嘴。
 *
 * <p>为什么不用原版皮革材质就完事：原版皮革会把皮革的护甲层贴图
 * （{@code textures/models/armor/leather_layer_1.png}）套在身上，那样穿布围裙看着就是一件皮甲。
 * 所以这里自己造一份「轻质布料」材质——防御数值照皮革（3 点）、耐久 80、附魔性 15、皮革装备音，
 * 但护甲层贴图指向本模组的 {@code textures/models/armor/cloth_apron_layer_1.png}，穿起来是布围裙的样子。</p>
 *
 * <p><b>mryt233 改（0.2.2-alpha，1.20.1 适配）</b>：
 * 1.21.1 的护甲材质是数据驱动静态注册表（{@code Registries.ARMOR_MATERIAL}），代码里能直接
 * {@code registerForHolder}。1.20.1 的 {@link ArmorMaterial} 还是<b>接口</b>，也没有
 * {@code ArmorMaterial.Layer}，所以这里在类内部实现一份材质，{@link #getName()} 返回
 * {@code "orients_hotpot_chronicles:cloth_apron"}——原版 {@code HumanoidArmorLayer} 会按
 * {@code textures/models/armor/<命名空间路径>/<名字>_layer_1.png} 去取贴图；
 * 构造交给 {@code super(material, ArmorItem.Type.CHESTPLATE, properties)}。</p>
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
public class ClothApronItem extends ArmorItem {
    /** 胸部防御点数（和原版皮革胸甲一致：3 点）。 */
    public static final int DEFENSE = 3;
    /** 耐久（和原版皮革胸甲一致：80）。 */
    public static final int DURABILITY = 80;
    /** 附魔性（皮革胸甲是 15）。 */
    public static final int ENCHANTMENT_VALUE = 15;
    /** 护甲层贴图的材质名：原版按 {@code <命名空间>:textures/models/armor/<名字>_layer_1.png} 找图。 */
    public static final String MATERIAL_NAME = HotPot.MOD_ID + ":cloth_apron";

    /** 轻质布料材质：1.20.1 是接口，只能自己实现一份。 */
    private static final ArmorMaterial MATERIAL = new ClothApronMaterial();

    public ClothApronItem() {
        super(MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(DURABILITY));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        // 用户定：白色羊毛或线都能修（和工作台配方一致）
        return repair.is(ItemTags.WOOL) || repair.is(net.minecraft.world.item.Items.STRING)
            || super.isValidRepairItem(stack, repair);
    }

    /**
     * mryt233 改（0.2.2-alpha）：1.20.1 的悬浮窗入口是
     * {@code appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)}，
     * 不是 1.21 的 {@code Item.TooltipContext}。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
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

    /**
     * 轻质布料材质（1.20.1 的 {@link ArmorMaterial} 接口实现）。
     * 数值照原版皮革胸甲：防御 3、耐久 80、附魔性 15、皮革装备音、白色羊毛修复；
     * 护甲层贴图靠 {@link #getName()} 指到本模组的 {@code cloth_apron_layer_1.png}。
     */
    private static final class ClothApronMaterial implements ArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.CHESTPLATE ? DURABILITY : 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.CHESTPLATE ? DEFENSE : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return ENCHANTMENT_VALUE;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            // 白色羊毛或线（配方就是 1 线 + 6 白羊毛）；两条都放进 Ingredient，铁砧修两种材料都认
            return Ingredient.of(net.minecraft.world.item.Items.WHITE_WOOL, net.minecraft.world.item.Items.STRING);
        }

        @Override
        public String getName() {
            return MATERIAL_NAME;
        }

        /** 韧性与击退抗性：布做的没有，和皮革一样是 0。 */
        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }
}
