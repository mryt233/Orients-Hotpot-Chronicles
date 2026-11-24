// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkKind;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 作者：mryt233；2026-11-11：设置字段规格及文案。 */
public final class ConfigField {
    private ConfigField() { }
    /** 数值单位：决定数值后面的单位字（秒 / 倍 / 点 / 级 / 格 / tick）。 */
    // mryt233 改：新增 BLOCK("block") 单位。
    public enum Unit {
        TICK("tick"), MULTIPLIER("multiplier"), POINTS("point"), LEVEL("level"), BLOCK("block"), PLAIN("");

        private final String key;

        Unit(String key) {
            this.key = key;
        }

        public String langKey() {
            return "text." + HotPot.MOD_ID + ".config.unit." + key;
        }
    }

    /** 字段控件类型：数值、布尔开关、枚举选择。 */
    public enum Kind {
        NUMBER, BOOLEAN, CHOICE
    }

    /** 一个可调的配置项：路径、控件类型、范围/选项、单位、行名、悬停说明键、行图标。 */
    public record Field(String[] path, Kind kind, boolean integer, double min, double max, double step, Unit unit,
                        String[] options, Component label, String helpKey, ItemStack icon) {
        public Field(String[] path, boolean integer, double min, double max, double step, Unit unit,
                     Component label, String helpKey, ItemStack icon) {
            this(path, Kind.NUMBER, integer, min, max, step, unit, null, label, helpKey, icon);
        }

        public String key() {
            return String.join(".", path);
        }

        public boolean isNumeric() {
            return kind == Kind.NUMBER;
        }
    }

    /** 「高级」页里的一个可折叠分组。 */
    public record Section(String titleKey, List<Field> fields) {
    }

    /** 列表里的一行（数值项），行高按名字换行后的行数算。 */
    public record Row(Field field, List<FormattedCharSequence> lines, int height) {
    }

    /** 列表里的分组标题行。 */
    public record Header(String titleKey, int count, int height) {
    }

    // ------------------------------------------------------------ 语言键

    public static String k(String suffix) {
        return "text." + HotPot.MOD_ID + ".config." + suffix;
    }

    public static Component prop(String name) {
        return Component.translatable(k("prop." + name));
    }

    public static final Component DOT = Component.literal(" · ");

    public static Component soupName(SoupDef base) {
        return Component.translatable("text." + HotPot.MOD_ID + ".soup." + base.key());
    }

    public static Component fuelName(String id) {
        return Component.translatable("text." + HotPot.MOD_ID + ".fuel." + id);
    }

    public static Component seasoningName(Seasoning seasoning) {
        return Component.translatable("text." + HotPot.MOD_ID + ".seasoning." + seasoning.id());
    }

    public static Component drinkName(DrinkKind kind) {
        return Component.translatable("item." + HotPot.MOD_ID + "." + kind.id());
    }

    // ------------------------------------------------------------ 数值项

    public static Field field(String path, boolean integer, double min, double max, double step, Unit unit,
                        Component label, String helpProp) {
        return field(path, integer, min, max, step, unit, label, helpProp, ItemStack.EMPTY);
    }

    public static Field field(String path, boolean integer, double min, double max, double step, Unit unit,
                        Component label, String helpProp, ItemStack icon) {
        return new Field(path.split("\\."), Kind.NUMBER, integer, min, max, step, unit, null, label, k("help." + helpProp), icon);
    }

    public static Field toggle(String path, Component label, String helpProp) {
        return new Field(path.split("\\."), Kind.BOOLEAN, true, 0, 1, 1, Unit.PLAIN, null, label, k("help." + helpProp), ItemStack.EMPTY);
    }

    public static Field choice(String path, String[] options, Component label, String helpProp) {
        return new Field(path.split("\\."), Kind.CHOICE, true, 0, Math.max(0, options.length - 1), 1, Unit.PLAIN,
            options, label, k("help." + helpProp), ItemStack.EMPTY);
    }

}
