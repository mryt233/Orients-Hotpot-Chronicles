package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.BenchBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTableBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import cn.blockforge.generated.orientshotpotchronicles.mixin.CreativeInventoryScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
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
 * 作者：
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
    private static final Identifier TAB_UNSELECTED = Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_2");
    private static final Identifier TAB_SELECTED = Identifier.ofVanilla("container/creative_inventory/tab_top_selected_2");
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
        if (stack.isOf(HotPotContent.KITCHEN_KNIFE) || stack.isOf(HotPotContent.CUTTING_BOARD_ITEM)
            || stack.isOf(HotPotContent.ROLLING_PIN)) {
            return Sub.COOKWARE;   // 菜刀、砧板算厨具（2026-10-23）
        }
        if (stack.isOf(HotPotContent.RAW_TRIPE) || stack.isOf(HotPotContent.COOKED_TRIPE)
            || stack.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DrinkItem
            || stack.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.EmptyBottleItem
            || stack.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem
            || (!(stack.getItem() instanceof BlockItem) && stack.contains(DataComponentTypes.FOOD))) {
            return Sub.FOOD;
        }
        if (stack.getItem() instanceof BlockItem item) {
            if (item.getBlock() instanceof StoveBlock || item.getBlock() instanceof HotPotBlock
                || item.getBlock() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlock) {   // 蘸料台算厨具（r103）
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
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean selected = current == sub;
            setX(selected ? baseX + SELECTED_OVERLAP : baseX);
            // 贴图 26 × 32 绕左上角转 -90°：贴图的底边（开口）转到右边，正好对着物品栏
            context.getMatrices().push();
            context.getMatrices().translate(getX(), getY() + TAB_H, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90F));
            context.drawGuiTexture(selected ? TAB_SELECTED : TAB_UNSELECTED, 0, 0, TAB_H, TAB_W);
            context.getMatrices().pop();
            context.drawItem(icon, getX() + 9, getY() + 5);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onSelect.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof CreativeInventoryScreen creative)) {
                return;
            }
            // 物品栏背景左上角（原版 HandledScreen 也是这样居中算的），标签开口贴着它的左边
            int left = (width - BG_W) / 2;
            int top = (height - BG_H) / 2;
            List<SubTab> tabs = new ArrayList<>();
            for (Sub sub : Sub.values()) {
                SubTab tab = new SubTab(sub, left - TAB_W, top, () -> {
                    current = sub;
                    // 重新选一次本分类页，让物品列表按新的子类刷新
                    ((CreativeInventoryScreenAccessor) creative).invokeSetSelectedTab(HotPotContent.ITEM_GROUP);
                });
                tabs.add(tab);
                Screens.getButtons(screen).add(tab);
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
            Screens.getButtons(screen).add(up);
            Screens.getButtons(screen).add(down);
            // 鼠标在这一列（含两个按钮）上滚轮：往上滚往前翻，往下滚往后翻；别处的滚轮照原版翻物品
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> {
                if (!ourTabSelected() || vertical == 0
                    || mouseX < left - TAB_W || mouseX >= left
                    || mouseY < top - ARROW_H - 1 || mouseY >= top + VISIBLE * STEP + 1 + ARROW_H) {
                    return true;
                }
                offset = Math.min(Math.max(offset + (vertical > 0 ? -1 : 1), 0), maxOffset);
                return false;
            });
            // 只在选中本模组分类页时显示；每帧按翻到的位置摆标签
            ScreenEvents.beforeRender(screen).register((s, ctx, mx, my, delta) -> {
                boolean show = ourTabSelected();
                for (SubTab t : tabs) {
                    int row = t.sub.ordinal() - offset;
                    t.visible = show && row >= 0 && row < VISIBLE;
                    t.setY(top + row * STEP);
                }
                up.visible = show && maxOffset > 0;
                down.visible = show && maxOffset > 0;
                up.active = offset > 0;
                down.active = offset < maxOffset;
            });
        });
    }
}
