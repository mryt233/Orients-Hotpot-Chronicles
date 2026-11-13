// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.config;

import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.Field;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigValues.formatValue;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigValues.parse;

/** 设置界面的编辑会话与非法输入账本。 */
public final class ConfigEditController {
    private final ConfigValues values;
    private final int TEXT, BAD;
    public final Map<String, String> bad = new LinkedHashMap<>();
    public EditBox editBox;
    public Field editField;
    public String typedEdit;
    public int editX, editY, editW, editH;

    public ConfigEditController(ConfigValues values, int text, int badColor) {
        this.values = values;
        this.TEXT = text;
        this.BAD = badColor;
    }

    public void startEdit(Font font, Field f, int x, int y, int w, int h) {
        commitEdit();
        editField = f;
        editX = x;
        editY = y;
        editW = w;
        editH = h;
        String current = bad.getOrDefault(f.key(), formatValue(f, values.value(f)));
        editBox = new EditBox(font, x + 3, y + 2, w - 6, 12, Component.empty());
        editBox.setMaxLength(24);
        editBox.setBordered(false);
        editBox.setTextColor(TEXT);
        editBox.setValue(current);
        editBox.setFocused(true);
        editBox.setResponder(this::onTyped);
        typedEdit = current;
        onTyped(current);
    }

    private void onTyped(String text) {
        typedEdit = text;
        if (editField != null && editBox != null) {
            editBox.setTextColor(parse(editField, text) == null ? BAD : TEXT);
        }
    }

    /** 回车 / 点到别处：合法就写进副本，不合法就标红留着（不改配置，也不让保存）。 */
    public void commitEdit() {
        if (editBox == null || editField == null) {
            clearEdit();
            return;
        }
        Field f = editField;
        String text = typedEdit != null ? typedEdit : editBox.getValue();
        text = text.trim();
        if (text.isEmpty()) {
            bad.remove(f.key());
            clearEdit();
            return;
        }
        Double parsed = parse(f, text);
        if (parsed == null) {
            bad.put(f.key(), text);
        } else {
            bad.remove(f.key());
            values.setValue(f, parsed);
        }
        clearEdit();
    }

    /** Esc：撤掉这一项的输入，回到配置里原来的值。 */
    public void cancelEdit() {
        if (editField != null) {
            bad.remove(editField.key());
        }
        clearEdit();
    }

    public void clearEdit() {
        editBox = null;
        editField = null;
        typedEdit = null;
    }

}
