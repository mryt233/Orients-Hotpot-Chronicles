package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.pure.ConfigStore;
import net.minecraft.Util;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
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
 * mryt233 改（2026-11-08，r146a）：按任务单把原来四个页签的拥挤界面改成两块——
 * 「常用」只放玩家最常动的那几项（粒子强度 + 煤炭档灶火功率 + 涮煮兜底时间 + 三种主力汤底的加热倍率），
 * 「高级」把其余全部按原分类收进可折叠分组（默认收起）。面板宽度按屏幕自适应、每行只放一项、
 * 名字换行不截断、超出用滚动条与滚轮；每行 [- 数值 +]，Shift 点一次走 10 步，点数值可直接键入
 * （原版 EditBox，输错标红且不让保存），行尾「↺」只在当前值不等于默认值时出现。
 * <p>
 * 配置读写、校验、网络同步、服务端管理员限制全部照旧：界面只改自己手上的 {@link #edited} 副本，
 * 点「保存并生效」才一次性交给 {@link Config#saveAndReload}（写回文件 + 热重载）。
 * 粒子滑块仍按原要求实时生效。
 * 作者：mryt233
 */
public class HotPotConfigScreen extends Screen {
    // ------------------------------------------------------------ 版面（r146a：随屏幕尺寸算，不再写死面板大小）
    private static final int BOTTOM_H = 20, SCROLL_W = 5;
    private static final int TEXT = 0xFFE8C88A, BAD = 0xFFFF6A6A;

    // ------------------------------------------------------------ 状态
    private final Screen parent;
    private JsonObject edited;
    private final ConfigSchema schema = new ConfigSchema(HotPotConfigScreen::stackOf);
    private final ConfigValues values = new ConfigValues(() -> edited, Config::defaultsCopy);
    private int entryParticle = 100;
    private int tab;
    private final Set<String> expanded = new HashSet<>();
    // mryt233 改（2026-11-11）：编辑会话独立持有，界面仍负责绘制和输入转发。
    private final ConfigEditController editor = new ConfigEditController(values, TEXT, BAD);

    private int panelW, panelH, px, py, listTop, listBottom, listX, listW;
    private int scroll;
    private boolean draggingBar;
    private double dragGrab;
    private ParticleSlider slider;

    private Field hoverField;
    /** 这一帧要显示的悬停提示（已经排好版）；普通按钮 / 快捷档用它直接出提示。 */
    private List<FormattedCharSequence> hoverTip;

    // mryt233 改（2026-11-10）：保存失败提示留在当前页，主菜单无玩家时也能读到原因。
    private Component saveNotice;
    private boolean confirmReset;
    private long confirmUntil;
    /** 这一帧「恢复全部默认」按钮在命中表里的下标（-1 = 没画）。 */
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

    // 缓存：列表布局、分组内容、默认值（每帧重算名字换行和范围太浪费）
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
        // 改窗口大小会重跑 init，这时不能把玩家已经改了一半的数值丢掉
        if (edited == null) {
            edited = Config.editableCopy();
            entryParticle = Config.particleIntensity();
        }
        panelW = Math.max(240, Math.min(640, width - 16));
        // mryt233 改（2026-11-09，r146a）：小窗口（GUI 高度 240）下也尽量把高度用满，列表能多露一行
        panelH = Math.max(200, Math.min(520, height - 8));
        px = Math.max(2, (width - panelW) / 2);
        py = Math.max(2, (height - panelH) / 2);
        listX = px + 10;
        listW = panelW - 20 - SCROLL_W - 4;
        applyTab();
    }

    /** 页签切换 / 进界面时的公共部分：滑块增删、列表上下边界、滚动复位。 */
    private void applyTab() {
        if (slider != null) {
            removeWidget(slider);
            slider = null;
        }
        editor.commitEdit();   // 切页签时把正在键入的这一项先结算掉，别把输入丢了
        int contentTop = py + 48;
        if (tab == 0) {
            slider = new ParticleSlider(px + 10, contentTop, panelW - 20, 20);
            slider.syncFromIntensity(Config.particleIntensity());
            addRenderableWidget(slider);
            // mryt233 改（2026-11-09，r146a）：滑块 20 + 提示一行 12 + 快捷档一行 14，列表从 contentTop + 52 起。
            // 以前提示和快捷档挤在同一行，窄面板下提示会被截断（任务单要求「不截断成看不懂的残字」）。
            listTop = contentTop + 52;
        } else {
            listTop = contentTop;
        }
        listBottom = py + panelH - BOTTOM_H - 16;
        scroll = 0;
        confirmReset = false;
        hoverField = null;
        hoverTip = null;
    }

    private List<Field> commonFields() { return schema.commonFields(edited); }
    private List<Section> sections() { return schema.sections(edited); }

    // ------------------------------------------------------------ 数值读写

    // ------------------------------------------------------------ 列表布局

    private final ConfigListModel model = new ConfigListModel();
    private void clearLayout() { model.invalidate(); }
    private int valueWidth() { return ConfigListModel.valueWidth(listW); }
    private List<Object> layout() { return model.layout(schema, edited, tab, expanded, panelW, listW, font); }
    private int contentHeight() { return ConfigListModel.contentHeight(layout()); }
    private int maxScroll() { return ConfigListModel.maxScroll(contentHeight(), listTop, listBottom); }

    // ------------------------------------------------------------ 画

    // mryt233 改（0.2.2-alpha，1.20.1 适配）：1.20.1 的 {@code Screen#renderBackground} 是单参
    // {@code (GuiGraphics)}，没有鼠标坐标；面板与列表每帧的状态复位挪进 {@link #render}。
    @Override
    public void renderBackground(GuiGraphics g) {
        super.renderBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // mryt233 改（1.20.1 适配）：1.20.1 的自定义 Screen 要显式画背景遮罩（同 SauceOverviewScreen）
        renderBackground(g);
        clickRects.clear();
        clickActions.clear();
        resetRectIndex = -1;
        hoverField = null;
        hoverTip = null;
        if (confirmReset && Util.getMillis() > confirmUntil) {
            confirmReset = false;
        }
        // GuiKit.frame 以 (0,0) 为左上角，这里靠 pose 平移到面板位置
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        GuiKit.frame(g, panelW, panelH, 1, true);
        g.pose().popPose();
        g.drawString(font, title, px + 12, py + 7, GuiKit.TITLE, true);
        g.fill(px + 12, py + 19, px + panelW - 12, py + 20, GuiKit.BRASS_DARK);
        painter.drawTabs(g, mouseX, mouseY);
        if (tab == 0) {
            painter.drawParticleBlock(g, mouseX, mouseY);
        }
        List<FormattedCharSequence> noticeLines = saveNotice == null ? List.of() : font.split(saveNotice, panelW - 24);
        listBottom = py + panelH - BOTTOM_H - 16 - noticeLines.size() * 10;
        painter.drawList(g, mouseX, mouseY);
        for (int i = 0; i < noticeLines.size(); i++) {
            g.drawString(font, noticeLines.get(i), px + 12, listBottom + 3 + i * 10, BAD, true);
        }
        painter.drawBottomBar(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, delta);
        // 编辑框自己画（不挂进 renderables，免得被列表和滚动区盖住）
        if (editor.editBox != null) {
            boolean ok = parse(editor.editField, editor.typedEdit) != null;
            g.fill(editor.editX - 1, editor.editY - 1, editor.editX + editor.editW + 1, editor.editY + editor.editH + 1, ok ? GuiKit.BRASS : BAD);
            g.fill(editor.editX, editor.editY, editor.editX + editor.editW, editor.editY + editor.editH, 0xFF1A0E05);
            editor.editBox.render(g, mouseX, mouseY, delta);
        }
        if (hoverTip != null) {
            g.renderTooltip(font, hoverTip, mouseX, mouseY);
        } else if (hoverField != null) {
            g.renderTooltip(font, painter.tooltipFor(hoverField), mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------ 编辑与操作

    private void nudge(Field f, int dir) {
        values.nudge(f, dir, hasShiftDown());
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
            minecraft.setScreen(parent);
        }
    }

    private void cancel() {
        editor.clearEdit();
        applyParticle(entryParticle);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void message(Component text) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(text, true);
        }
    }

    /** 粒子强度：拖动滑块立刻生效，同时写进待保存的副本。 */
    private void applyParticle(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        Config.setParticleIntensity(p);
        node(edited, new String[]{"client"}, 1).addProperty("particle_intensity", p);
    }

    @Override
    public void onClose() {
        // ESC 等同「取消」：不保存，粒子强度退回进界面之前的值
        cancel();
    }

    // ------------------------------------------------------------ 鼠标 / 键盘

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editor.editBox != null) {
            if (hit(mouseX, mouseY, editor.editX, editor.editY, editor.editW, editor.editH)) {
                editor.editBox.mouseClicked(mouseX, mouseY, button);
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
                    // 点到别的地方就撤掉「恢复全部默认」的确认态，点它自己要留给它自己判断
                    if (i != resetRectIndex) {
                        confirmReset = false;
                    }
                    clickActions.get(i).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            dragBar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** mryt233 改（1.20.1 适配）：1.20.1 的 {@code mouseScrolled} 只有 (double,double,double) 三参。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        editor.commitEdit();   // 滚动时先把正在键入的这一项结算掉，免得输入框停在别的行上
        int max = maxScroll();
        if (max > 0) {
            scroll = (int) Math.max(0, Math.min(max, scroll - scrollY * 24));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editor.editBox != null) {
            if (keyCode == 257 || keyCode == 335) {   // 回车 / 小键盘回车：确认这一项的输入
                editor.commitEdit();
                return true;
            }
            if (keyCode == 256) {                     // Esc：撤掉输入，别把整个界面关掉
                editor.cancelEdit();
                return true;
            }
            editor.editBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editor.editBox != null) {
            return editor.editBox.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    /**
     * mryt233 改（2026-11-09，r146a）：开发用探针（devtest/ClientProbe）截图时切页签 / 展开分组用。
     * 只在 {@code -Dhotpot.clientProbe=true} 的开发客户端里由 /hpt 探针调用，正式玩法没有任何入口。
     * 接受的写法：{@code advanced} / {@code common} / {@code expand:0}（按高级页分组下标展开或收起）。
     */
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

    // ------------------------------------------------------------ 小工具

    private static ItemStack stackOf(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(HotPot.id(id)).orElse(Items.BARRIER);
        return item == Items.BARRIER ? ItemStack.EMPTY : new ItemStack(item);
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
