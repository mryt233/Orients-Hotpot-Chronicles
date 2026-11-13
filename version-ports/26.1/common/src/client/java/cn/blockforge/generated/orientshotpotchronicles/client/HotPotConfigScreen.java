// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.orient.framework.pure.ConfigStore;
import net.minecraft.util.Util;
import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigEditController;
import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigListModel;
import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigPainter;
import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigSchema;
import cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigValues;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigField.*;
import static cn.blockforge.generated.orientshotpotchronicles.client.config.ConfigValues.*;
import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 游戏内可视化设置界面（r135e，2026-11-05 初版；r146a，2026-11-08 重做）。
 * <p>
 * 配置读写、校验、网络同步、服务端管理员限制全部照旧：界面只改自己手上的 {@link #edited} 副本，
 * 点「保存并生效」才一次性交给 {@link Config#saveAndReload}（写回文件 + 热重载）。
 * 粒子滑块仍按原要求实时生效。
 * 作者：mryt233
 */
public class HotPotConfigScreen extends Screen {
    private static final int BOTTOM_H = 20, SCROLL_W = 5;
    private static final int TEXT = 0xFFE8C88A, BAD = 0xFFFF6A6A;

    private final Screen parent;
    private JsonObject edited;
    private final ConfigSchema schema = new ConfigSchema(HotPotConfigScreen::stackOf);
    private final ConfigValues values = new ConfigValues(() -> edited, Config::defaultsCopy);
    private int entryParticle = 100;
    private int tab;
    private final Set<String> expanded = new HashSet<>();
    private final ConfigEditController editor = new ConfigEditController(values, TEXT, BAD);

    private int panelW, panelH, px, py, listTop, listBottom, listX, listW;
    private int scroll;
    private boolean draggingBar;
    private double dragGrab;
    private ParticleSlider slider;

    private Field hoverField;
    private List<FormattedCharSequence> hoverTip;

    private Component saveNotice;
    private boolean confirmReset;
    private long confirmUntil;
    private int resetRectIndex = -1;

    private final List<int[]> clickRects = new ArrayList<>();
    private final List<Runnable> clickActions = new ArrayList<>();

    private final ConfigPainter painter = new ConfigPainter(new ConfigPainter.Host() {
        public Font font() { return font; }
        public int tab() { return tab; }
        public int px() { return px; }
        public int py() { return py; }
        public int panelW() { return panelW; }
        public int panelH() { return panelH; }
        public int listTop() { return listTop; }
        public int listBottom() { return listBottom; }
        public int listX() { return listX; }
        public int listW() { return listW; }
        public int scroll() { return scroll; }
        public Set<String> expanded() { return expanded; }
        public boolean confirmReset() { return confirmReset; }
        public List<int[]> clickRects() { return clickRects; }
        public List<Runnable> clickActions() { return clickActions; }
        public ConfigValues values() { return values; }
        public ConfigEditController editor() { return editor; }
        public void setTab(int value) { tab = value; }
        public void setScroll(int value) { scroll = value; }
        public void setHoverField(Field value) { hoverField = value; }
        public void setHoverTip(List<FormattedCharSequence> value) { hoverTip = value; }
        public void setResetRectIndex(int value) { resetRectIndex = value; }
        public void clearLayout() { HotPotConfigScreen.this.clearLayout(); }
        public void applyTab() { HotPotConfigScreen.this.applyTab(); }
        public int valueWidth() { return HotPotConfigScreen.this.valueWidth(); }
        public List<Object> layout() { return HotPotConfigScreen.this.layout(); }
        public int contentHeight() { return HotPotConfigScreen.this.contentHeight(); }
        public int maxScroll() { return HotPotConfigScreen.this.maxScroll(); }
        public void save() { HotPotConfigScreen.this.save(); }
        public void resetAllClicked() { HotPotConfigScreen.this.resetAllClicked(); }
        public void cancel() { HotPotConfigScreen.this.cancel(); }
        public void nudge(Field field, int dir) { HotPotConfigScreen.this.nudge(field, dir); }
        public void particle(int percent) {
            applyParticle(percent);
            if (slider != null) slider.syncFromIntensity(percent);
        }
    });

    public HotPotConfigScreen(Screen parent) {
        super(Component.translatable("text." + HotPot.MOD_ID + ".config.title"));
        this.parent = parent;
    }

    /** 模组列表等入口在拿不到上一级界面时用这个，关界面就直接回游戏。 */
    public HotPotConfigScreen() {
        this(null);
    }

    @Override
    protected void init() {
        if (edited == null) {
            edited = Config.editableCopy();
            entryParticle = Config.particleIntensity();
        }
        panelW = Math.max(240, Math.min(640, width - 16));
        panelH = Math.max(200, Math.min(520, height - 8));
        px = Math.max(2, (width - panelW) / 2);
        py = Math.max(2, (height - panelH) / 2);
        listX = px + 10;
        listW = panelW - 20 - SCROLL_W - 4;
        applyTab();
    }

    private void applyTab() {
        if (slider != null) {
            removeWidget(slider);
            slider = null;
        }
        editor.commitEdit();
        int contentTop = py + 48;
        if (tab == 0) {
            slider = new ParticleSlider(px + 10, contentTop, panelW - 20, 20);
            slider.syncFromIntensity(Config.particleIntensity());
            addRenderableWidget(slider);
            listTop = contentTop + 52;
        } else {
            listTop = contentTop;
        }
        listBottom = Math.max(listTop + 1, py + panelH - BOTTOM_H - 16);
        scroll = 0;
        confirmReset = false;
        hoverField = null;
        hoverTip = null;
    }

    private List<Section> sections() { return schema.sections(edited); }

    private final ConfigListModel model = new ConfigListModel();
    private void clearLayout() { model.invalidate(); }
    private int valueWidth() { return ConfigListModel.valueWidth(listW); }
    private List<Object> layout() { return model.layout(schema, edited, tab, expanded, panelW, listW, font); }
    private int contentHeight() { return ConfigListModel.contentHeight(layout()); }
    private int maxScroll() { return ConfigListModel.maxScroll(contentHeight(), listTop, listBottom); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        clickRects.clear();
        clickActions.clear();
        resetRectIndex = -1;
        hoverField = null;
        hoverTip = null;
        if (confirmReset && Util.getMillis() > confirmUntil) {
            confirmReset = false;
        }
        g.pose().pushMatrix();
        g.pose().translate(px, py);
        GuiKit.frame(g, panelW, panelH, 1, true);
        g.pose().popMatrix();
        g.text(font, title, px + 12, py + 7, GuiKit.TITLE, true);
        g.fill(px + 12, py + 19, px + panelW - 12, py + 20, GuiKit.BRASS_DARK);
        painter.drawTabs(g, mouseX, mouseY);
        if (tab == 0) {
            painter.drawParticleBlock(g, mouseX, mouseY);
        }
        List<FormattedCharSequence> noticeLines = saveNotice == null ? List.of() : font.split(saveNotice, panelW - 24);
        listBottom = Math.max(listTop + 1, py + panelH - BOTTOM_H - 16 - noticeLines.size() * 10);
        painter.drawList(g, mouseX, mouseY);
        for (int i = 0; i < noticeLines.size(); i++) {
            g.text(font, noticeLines.get(i), px + 12, listBottom + 3 + i * 10, BAD, true);
        }
        painter.drawBottomBar(g, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (editor.editBox != null) {
            boolean ok = parse(editor.editField, editor.typedEdit) != null;
            g.fill(editor.editX - 1, editor.editY - 1, editor.editX + editor.editW + 1, editor.editY + editor.editH + 1, ok ? GuiKit.BRASS : BAD);
            g.fill(editor.editX, editor.editY, editor.editX + editor.editW, editor.editY + editor.editH, 0xFF1A0E05);
            editor.editBox.extractWidgetRenderState(g, mouseX, mouseY, delta);
        }
        if (hoverTip != null) {
            g.setTooltipForNextFrame(font, hoverTip, mouseX, mouseY);
        } else if (hoverField != null) {
            g.setTooltipForNextFrame(font, painter.tooltipFor(hoverField), mouseX, mouseY);
        }
    }

    private void nudge(Field f, int dir) {
        values.nudge(f, dir, Minecraft.getInstance().hasShiftDown());
        editor.bad.remove(f.key());
    }

    /** 「恢复全部默认」点一次变确认态，再点一次才真的恢复（4 秒内有效）。 */
    private void resetAllClicked() {
        long now = Util.getMillis();
        if (confirmReset && now <= confirmUntil) {
            confirmReset = false;
            resetAll();
            return;
        }
        confirmReset = true;
        confirmUntil = now + 4000L;
    }

    private void resetAll() {
        edited = Config.defaultsCopy();
        editor.bad.clear();
        values.clearDefaults();
        clearLayout();
        schema.invalidate();
        applyParticle(100);
        if (slider != null) {
            slider.syncFromIntensity(100);
        }
        scroll = 0;
    }

    private void save() {
        editor.commitEdit();
        if (!editor.bad.isEmpty()) {
            message(Component.translatable(k("msg.invalid")));
            return;
        }
        Config.Loaded loaded = Config.saveAndReload(edited);
        if (loaded.saveStatus() == ConfigStore.SaveStatus.FAILED) {
            saveNotice = Component.translatable(k("msg.save_failed"), loaded.reason());
            return;
        }
        entryParticle = Config.particleIntensity();
        if (loaded.saveStatus() == ConfigStore.SaveStatus.MEMORY_ONLY) {
            saveNotice = Component.translatable(k("msg.memory_only"), loaded.reason());
            if (loaded.backup() != null) {
                saveNotice = saveNotice.copy().append(" ").append(Component.translatable(k("msg.broken_backup"), loaded.backup().getFileName().toString()));
            }
            return;
        }
        if (loaded.backup() != null) {
            saveNotice = Component.translatable(k("msg.broken_backup"), loaded.backup().getFileName().toString());
            return;
        }
        saveNotice = null;
        message(Component.translatable("text." + HotPot.MOD_ID + ".msg.config_reloaded", loaded.warnings().size()));
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    private void cancel() {
        editor.clearEdit();
        applyParticle(entryParticle);
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    private void message(Component text) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendOverlayMessage(text);
        }
    }

    private void applyParticle(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        Config.setParticleIntensity(p);
        node(edited, new String[]{"client"}, 1).addProperty("particle_intensity", p);
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (editor.editBox != null) {
            if (hit(mouseX, mouseY, editor.editX, editor.editY, editor.editW, editor.editH)) {
                editor.editBox.mouseClicked(event, doubleClick);
                return true;
            }
            editor.commitEdit();
        }
        if (button == 0) {
            if (grabScrollbar(mouseX, mouseY)) {
                return true;
            }
            for (int i = 0; i < clickRects.size(); i++) {
                int[] r = clickRects.get(i);
                if (hit(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                    if (i != resetRectIndex) {
                        confirmReset = false;
                    }
                    clickActions.get(i).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean grabScrollbar(double mouseX, double mouseY) {
        if (contentHeight() <= listBottom - listTop) {
            return false;
        }
        int x = listX + listW + 3;
        if (!hit(mouseX, mouseY, x, listTop, SCROLL_W, listBottom - listTop)) {
            return false;
        }
        draggingBar = true;
        dragGrab = mouseY - listTop;
        dragBar(mouseY);
        return true;
    }

    private void dragBar(double mouseY) {
        int view = Math.max(1, listBottom - listTop);
        int total = contentHeight();
        int thumbH = Math.max(12, view * view / Math.max(1, total));
        double span = Math.max(1, view - thumbH);
        double ratio = Math.max(0, Math.min(1, (mouseY - dragGrab - listTop) / span));
        scroll = (int) Math.round(ratio * Math.max(0, total - view));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseY = event.y();
        if (draggingBar) {
            dragBar(mouseY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingBar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        editor.commitEdit();
        int max = maxScroll();
        if (max > 0) {
            scroll = (int) Math.max(0, Math.min(max, scroll - scrollY * 24));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (editor.editBox != null) {
            if (keyCode == 257 || keyCode == 335) {
                editor.commitEdit();
                return true;
            }
            if (keyCode == 256) {
                editor.cancelEdit();
                return true;
            }
            editor.editBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editor.editBox != null) {
            return editor.editBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    public void probeAction(String action) {
        if ("advanced".equals(action)) {
            tab = 1;
            clearLayout();
            applyTab();
        } else if ("common".equals(action)) {
            tab = 0;
            clearLayout();
            applyTab();
        } else if (action.startsWith("expand:")) {
            int index = Integer.parseInt(action.substring("expand:".length()));
            List<Section> all = sections();
            if (index >= 0 && index < all.size()) {
                String key = all.get(index).titleKey();
                if (!expanded.remove(key)) {
                    expanded.add(key);
                }
                clearLayout();
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 一行的物品图标：物品不存在、或者 26.x 还没绑定物品的默认数据组件（主菜单阶段）时返回空物品，调用处不画图标。
     */
    private static ItemStack stackOf(String id) {
        Holder.Reference<Item> holder = BuiltInRegistries.ITEM.get(HotPot.id(id)).orElse(null);
        if (holder == null || !holder.areComponentsBound()) {
            return ItemStack.EMPTY;
        }
        Item item = holder.value();
        if (item == Items.BARRIER) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(item);
        } catch (NullPointerException e) {
            if ("Components not bound yet".equals(e.getMessage())) {
                return ItemStack.EMPTY;
            }
            throw e;
        }
    }

    /** 粒子强度滑块：0%～100%，0 时文案显示「已关闭」。 */
    private final class ParticleSlider extends AbstractSliderButton {
        ParticleSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), 1.0);
        }

        int percent() {
            return (int) Math.round(value * 100.0);
        }

        void syncFromIntensity(int percent) {
            value = Math.max(0, Math.min(100, percent)) / 100.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(percent() == 0
                ? Component.translatable("text." + HotPot.MOD_ID + ".config.particles.off")
                : Component.translatable("text." + HotPot.MOD_ID + ".config.particles.value", percent()));
        }

        @Override
        protected void applyValue() {
            applyParticle(percent());
        }
    }
}
