// 1.20.1 Forge 移植：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.BenchBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTableBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import cn.blockforge.generated.orientshotpotchronicles.mixin.CreativeInventoryScreenAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 创造模式「之晗的火锅物语」分类页左边的一列子类标签：全部 / 厨具 / 餐具 / 桌子 / 凳子（r86 用户要求，不再细分）/ 食物（r98）。
 * r98：一次显示 5 个，上下各一个翻页按钮，鼠标在这一列上滚轮也能翻（用户要求）。
 * r88 美化（用户要求）：用原版分类页标签的贴图横过来贴在物品栏左边，标签上画图标、鼠标停上去显示名字；
 * 位置按物品栏背景算，换界面尺寸、窗口大小都贴着物品栏。选中某一类时右边只显示这一类（过滤在 CreativeInventoryScreenMixin）。
 * 作者：Claude
 */
public final class CreativeSubTabs {
    public enum Sub {
        ALL("all"), COOKWARE("cookware"), TABLEWARE("tableware"), TABLES("tables"), SEATS("seats"), FOOD("food");

        final String key;

        Sub(String key) {
            this.key = key;
        }
    }

    /** 当前选中的子类（关掉物品栏也记着，下次打开还是这一类）。 */
    public static Sub current = Sub.ALL;

    /** 这一列往下翻了几个（同样关掉物品栏也记着）。 */
    public static int offset = 0;
    /** 一次显示几个标签：5 个正好是物品栏背景的高度。 */
    private static final int VISIBLE = 5;
    /** 翻页按钮大小。 */
    private static final int ARROW_W = 28;
    private static final int ARROW_H = 14;

    /** 原版顶上分类标签的贴图（26 × 32，开口朝下），转 90° 后变成 32 × 26、开口朝右贴在物品栏左边。 */
    // 1.20.1 移植（Claude 改）：1.20.1 还没有 GUI 精灵图，标签贴图在 tabs.png 里（第 2 列：未选中 v=0，选中 v=32）
    private static final Identifier TABS = new Identifier("textures/gui/container/creative_inventory/tabs.png");
    private static final int TAB_W = 32;
    private static final int TAB_H = 26;
    /** 原版标签之间的间距（顶上那排是 27）。 */
    private static final int STEP = 27;
    /** 选中的标签往物品栏里压几像素，盖住物品栏左边框，看起来和物品栏连成一体（同原版选中标签）。 */
    private static final int SELECTED_OVERLAP = 3;
    /** 原版创造物品栏背景的大小（195 × 136）。 */
    private static final int BG_W = 195;
    private static final int BG_H = 136;

    private CreativeSubTabs() {
    }

    public static Sub categoryOf(ItemStack stack) {
        if (stack.isOf(HotPotContent.CHOPSTICKS) || stack.getItem() instanceof PlateItem) {
            return Sub.TABLEWARE;
        }
        if (stack.isOf(HotPotContent.RAW_TRIPE) || stack.isOf(HotPotContent.COOKED_TRIPE)
            || stack.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkItem
            || stack.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.EmptyBottleItem
            || (!(stack.getItem() instanceof BlockItem) && stack.isFood())) {
            return Sub.FOOD;
        }
        if (stack.getItem() instanceof BlockItem item) {
            if (item.getBlock() instanceof StoveBlock || item.getBlock() instanceof HotPotBlock) {
                return Sub.COOKWARE;
            }
            if (item.getBlock() instanceof HotPotTableBlock) {
                return Sub.TABLES;
            }
            if (item.getBlock() instanceof BenchBlock) {
                return Sub.SEATS;
            }
        }
        return Sub.ALL;
    }

    /** 按当前子类筛一遍物品列表（只在本模组分类页时调用）。 */
    public static void filter(List<ItemStack> items) {
        if (current != Sub.ALL) {
            items.removeIf(stack -> categoryOf(stack) != current);
        }
    }

    public static boolean ourTabSelected() {
        return CreativeInventoryScreenAccessor.getSelectedTab() == HotPotContent.ITEM_GROUP;
    }

    /** 每个子类标签上的图标。全部 = 分类页图标（煮着红汤的铁锅），厨具 = 点着火、灶膛口朝外的灶台（物品自定义模型 1）。 */
    private static ItemStack iconOf(Sub sub) {
        return switch (sub) {
            case ALL -> HotPotContent.ITEM_GROUP.getIcon();
            case COOKWARE -> new ItemStack(HotPotContent.IRON_HOT_POT_ITEM);   // 空的铁锅（r91 用户：灶台看着怪）
            case TABLEWARE -> new ItemStack(HotPotContent.CHOPSTICKS);
            case TABLES -> new ItemStack(HotPotContent.TALL_TABLE_ITEMS.get(0));   // 高的橡木火锅桌（r90 用户要求）
            case SEATS -> new ItemStack(HotPotContent.TALL_BENCH_ITEMS.get(0));
            case FOOD -> new ItemStack(HotPotContent.RAW_TRIPE);   // 生毛肚（r98 用户要求）
        };
    }

    /** 一个横过来的分类标签。 */
    private static final class SubTab extends ClickableWidget {
        private final Sub sub;
        private final int baseX;
        private final ItemStack icon;
        private final Runnable onSelect;

        SubTab(Sub sub, int x, int y, Runnable onSelect) {
            super(x, y, TAB_W, TAB_H, Text.translatable("itemGroup.orients_hotpot_chronicles.sub." + sub.key));
            this.sub = sub;
            this.baseX = x;
            this.icon = iconOf(sub);
            this.onSelect = onSelect;
            setTooltip(Tooltip.of(getMessage()));
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean selected = current == sub;
            setX(selected ? baseX + SELECTED_OVERLAP : baseX);
            // 贴图 26 × 32 绕左上角转 -90°：贴图的底边（开口）转到右边，正好对着物品栏
            context.getMatrices().push();
            context.getMatrices().translate(getX(), getY() + TAB_H, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90F));
            context.drawTexture(TABS, 0, 0, TAB_H, selected ? TAB_W : 0, TAB_H, TAB_W);
            context.getMatrices().pop();
            context.drawItem(icon, getX() + 9, getY() + 5);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onSelect.run();
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    // Codex 改：按屏幕保存控件；全局事件仅注册一次，避免反复开界面累积监听器。
    private record Widgets(List<SubTab> tabs, ButtonWidget up, ButtonWidget down,
                           int left, int top, int maxOffset) {}
    private static final java.util.Map<net.minecraft.client.gui.screen.Screen, Widgets> WIDGETS = new java.util.WeakHashMap<>();

    public static void register() {
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(CreativeSubTabs::onInit);
        bus.addListener(CreativeSubTabs::onScroll);
        bus.addListener(CreativeSubTabs::onRender);
    }

    private static void onInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (!(screen instanceof CreativeInventoryScreen creative)) return;
        int width = screen.width;
        int height = screen.height;
        // 物品栏背景左上角（原版 HandledScreen 也是这样居中算的），标签开口贴着它的左边
        int left = (width - BG_W) / 2;
        int top = (height - BG_H) / 2;
        List<SubTab> tabs = new ArrayList<>();
        var screenRef = new java.lang.ref.WeakReference<>(creative);
        for (Sub sub : Sub.values()) {
            SubTab tab = new SubTab(sub, left - TAB_W, top, () -> {
                var targetScreen = screenRef.get();
                if (targetScreen == null) return;
                current = sub;
                // 重新选一次本分类页，让物品列表按新的子类刷新
                ((CreativeInventoryScreenAccessor) targetScreen).invokeSetSelectedTab(HotPotContent.ITEM_GROUP);
            });
            tabs.add(tab);
            event.addListener(tab);
        }
        int maxOffset = Math.max(0, tabs.size() - VISIBLE);
        offset = Math.min(Math.max(offset, 0), maxOffset);
        // 翻页按钮：原版按钮样式，上面那个贴着第一个标签的上沿，下面那个贴着最后一个可见标签的下沿
        ButtonWidget up = ButtonWidget.builder(Text.literal("▲"), b -> offset = Math.max(0, offset - 1))
            .dimensions(left - TAB_W + 2, top - ARROW_H - 1, ARROW_W, ARROW_H)
            .tooltip(Tooltip.of(Text.translatable("itemGroup.orients_hotpot_chronicles.sub.up"))).build();
        ButtonWidget down = ButtonWidget.builder(Text.literal("▼"), b -> offset = Math.min(maxOffset, offset + 1))
            .dimensions(left - TAB_W + 2, top + VISIBLE * STEP + 1, ARROW_W, ARROW_H)
            .tooltip(Tooltip.of(Text.translatable("itemGroup.orients_hotpot_chronicles.sub.down"))).build();
        event.addListener(up);
        event.addListener(down);
        WIDGETS.put(screen, new Widgets(tabs, up, down, left, top, maxOffset));
        update(WIDGETS.get(screen));
    }

    private static void onScroll(net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre event) {
        Widgets widgets = WIDGETS.get(event.getScreen());
        if (widgets == null || !ourTabSelected()) return;
        double mouseX = event.getMouseX(), mouseY = event.getMouseY(), vertical = event.getScrollDelta();
        if (vertical == 0 || mouseX < widgets.left - TAB_W || mouseX >= widgets.left
            || mouseY < widgets.top - ARROW_H - 1 || mouseY >= widgets.top + VISIBLE * STEP + 1 + ARROW_H) return;
        offset = Math.min(Math.max(offset + (vertical > 0 ? -1 : 1), 0), widgets.maxOffset);
        event.setCanceled(true);
    }

    private static void onRender(net.minecraftforge.client.event.ScreenEvent.Render.Pre event) {
        Widgets widgets = WIDGETS.get(event.getScreen());
        if (widgets != null) update(widgets);
    }

    private static void update(Widgets widgets) {
        boolean show = ourTabSelected();
        for (SubTab t : widgets.tabs) {
            int row = t.sub.ordinal() - offset;
            t.visible = show && row >= 0 && row < VISIBLE;
            t.setY(widgets.top + row * STEP);
        }
        widgets.up.visible = show && widgets.maxOffset > 0;
        widgets.down.visible = show && widgets.maxOffset > 0;
        widgets.up.active = offset > 0;
        widgets.down.active = offset < widgets.maxOffset;
    }
}
