package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.loader.RegistrationApi;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 布围裙（清单 #127）：胸甲部位的布制防具，防溅油、防烫嘴。
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
 * {@code textures/models/armor/<款式>_layer_1.png}，穿起来是布围裙的样子。</p>
 *
 * <p><b>mryt233 改（0.2.2-alpha / r146b，1.20.1 适配）</b>：
 * 1.21.1 的护甲材质是数据驱动静态注册表（{@code Registries.ARMOR_MATERIAL}），代码里能直接
 * {@code registerForHolder}。1.20.1 的 {@link ArmorMaterial} 还是<b>接口</b>，也没有
 * {@code ArmorMaterial.Layer}，所以这里每种款式在类内部实现一份材质，{@link ArmorMaterial#getName()}
 * 返回 {@code "orients_hotpot_chronicles:<款式 ID>"}——原版 {@code HumanoidArmorLayer} 会按
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
 * 作者：mryt233
 */
public class ClothApronItem extends ArmorItem {
    /** 三款布围裙共用的物品标签：所有「穿没穿围裙」的判断都认它（r146b）。 */
    public static final TagKey<Item> APRON_TAG =
        TagKey.create(Registries.ITEM, HotPot.id("cloth_aprons"));
    /** 胸部防御点数（和原版皮革胸甲一致：3 点）。 */
    public static final int DEFENSE = 3;
    /** 耐久（和原版皮革胸甲一致：80）。 */
    public static final int DURABILITY = 80;
    /** 附魔性（皮革胸甲是 15）。 */
    public static final int ENCHANTMENT_VALUE = 15;

    /**
     * 三款布围裙（r146b，2026-11-09 用户定）。顺序 = 工作台里换款的颜色顺序（米白 → 蓝印花 → 棕边带兜）。
     * {@code id} 既是物品注册名，也是护甲层贴图名（{@code textures/models/armor/<id>_layer_1.png}）。
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

        /** 护甲材质名：原版按 {@code textures/models/armor/<名字>_layer_1.png} 找图，所以要带命名空间。 */
        public String materialName() {
            return HotPot.MOD_ID + ":" + id;
        }
    }

    /** 每款一份材质：1.20.1 的 {@link ArmorMaterial} 是接口，只能自己实现（r146b）。 */
    private static final Map<Style, ArmorMaterial> MATERIALS = new EnumMap<>(Style.class);

    /** 这一件是哪款（认不出来时按米白款算）。 */
    public Style style() {
        return style;
    }

    private final Style style;

    public ClothApronItem() {
        this(Style.LINEN);
    }

    public ClothApronItem(Style style) {
        super(material(style), ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(DURABILITY));
        this.style = style;
    }

    // mryt233 改（2026-11-10）：保留统一构造入口；1.20.1 材质不走注册表。
    public ClothApronItem(RegistrationApi registration, Style style) {
        this(style);
    }

    /** 取（必要时建）这一款的材质；材质只是接口实现，不占注册表。 */
    private static synchronized ArmorMaterial material(Style style) {
        return MATERIALS.computeIfAbsent(style, ClothApronMaterial::new);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        // 用户定：白色羊毛或线都能修（和工作台配方一致）
        return repair.is(ItemTags.WOOL) || repair.is(Items.STRING)
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
        // mryt233 改（2026-11-09，r146b）：三款都能用染料换款，说明里写清楚怎么换
        lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.cloth_apron_style"))
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 穿着布围裙（胸甲槽是它，任意一款）就返回 true；别的部位 / 没穿返回 false。
     * mryt233 改（2026-11-09，r146b）：认物品标签 {@code orients_hotpot_chronicles:cloth_aprons}，
     * 三款都在标签里，以后再加款也只用改标签、不用改这里的判断。
     */
    public static boolean wornBy(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return isApron(chest);
    }

    /** 这一叠物品是不是布围裙（任意一款）：认 {@code cloth_aprons} 标签。 */
    public static boolean isApron(ItemStack stack) {
        return !stack.isEmpty() && stack.is(APRON_TAG);
    }

    /** 这个玩家穿着布围裙。 */
    public static boolean wornBy(Player player) {
        return wornBy((LivingEntity) player);
    }

    /**
     * mryt233 改（2026-11-09，r146b）：Forge 1.20.1 的护甲贴图走 {@code IForgeItem#getArmorTexture}
     * （{@code ForgeHooksClient.getArmorTexture} 先问物品，返回 null 才用默认拼法）。
     * 这里按款式返回本模组的护甲层贴图；Fabric 侧不调这个方法，多这一个 public 方法不影响编译。
     */
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return HotPot.MOD_ID + ":textures/models/armor/" + style.id() + "_layer_"
            + (slot == EquipmentSlot.LEGS ? 2 : 1) + (type == null ? "" : "_" + type) + ".png";
    }

    /**
     * 轻质布料材质（1.20.1 的 {@link ArmorMaterial} 接口实现），每款一份。
     * 数值照原版皮革胸甲：防御 3、耐久 80、附魔性 15、皮革装备音、白色羊毛修复；
     * 护甲层贴图靠 {@link #getName()} 指到本模组的 {@code <款式>_layer_1.png}。
     */
    private static final class ClothApronMaterial implements ArmorMaterial {
        private final Style style;

        private ClothApronMaterial(Style style) {
            this.style = style;
        }

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
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            // 白色羊毛或线（配方就是 1 线 + 6 白羊毛）；两条都放进 Ingredient，铁砧修两种材料都认
            return Ingredient.of(Items.WHITE_WOOL, Items.STRING);
        }

        @Override
        public String getName() {
            // 三款各一层贴图：原版会去找 textures/models/armor/<这个名字去掉命名空间的路径>_layer_1.png
            return style.materialName();
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
