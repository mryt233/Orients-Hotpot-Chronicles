package cn.blockforge.generated.orientshotpotchronicles.client.compat.rei;

import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotRecipeViewerData;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.client.SauceStationScreen;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.category.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * REI 客户端插件：把火锅涮煮、砧板切配、擀面杖拍打三类玩法配方交给 REI 展示。
 * <p>
 * 配方内容同样来自核心层的 {@link HotPotRecipeViewerData}，展示内容和 JEI 那套完全对等；
 * 没装 REI 时这个类不会被实例化（Fabric 由 {@code rei_client} 入口点、NeoForge 由 {@code @REIPluginClient}
 * 注解扫描发现），模组照常运行、也不会因为缺少 REI 报错。
 * 作者：mryt233 */
@REIPluginClient
public final class HotPotReiClientPlugin implements REIClientPlugin {
    // mryt233 改（r146f）：已有配方插件同时登记动态原料表占用区。
    @Override
    public void registerScreens(ScreenRegistry registry) {
        if (!HotPotCompat.isModLoaded("roughlyenoughitems")) return;
        registry.exclusionZones().register(SauceStationScreen.class, screen -> screen.guideAreas().stream()
            .map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight())).toList());
    }

    /** 火锅涮煮类别。 */
    public static final CategoryIdentifier<CookingDisplay> COOKING = CategoryIdentifier.of(HotPot.id("hotpot_cooking"));
    /** 砧板切配类别。 */
    public static final CategoryIdentifier<CuttingDisplay> CUTTING = CategoryIdentifier.of(HotPot.id("cutting_board"));
    /** 擀面杖拍打类别。 */
    public static final CategoryIdentifier<PoundingDisplay> POUNDING = CategoryIdentifier.of(HotPot.id("pounding"));

    /** 正文颜色（深灰）。 */
    private static final int TEXT = 0xFF404040;
    /** 正文颜色的暗色版本（REI 的 shadow 参数）。 */
    private static final int TEXT_DARK = 0xFF303030;
    /** 提示颜色（偏暖的深棕）。 */
    private static final int HINT = 0xFF8A5524;
    /** 「久煮会煮老」提示色。 */
    private static final int HINT_DARK = 0xFF6E421B;
    /** 警告颜色。 */
    private static final int WARN = 0xFFA63D2A;
    /** 警告颜色的暗色版本。 */
    private static final int WARN_DARK = 0xFF7C2A1C;

    /** 把刻数换算成「秒」的显示文本（一位小数，固定用点号）。 */
    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20F);
    }

    // ------------------------------------------------------------------ 注册

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new CookingCategory(), new CuttingCategory(), new PoundingCategory());
        for (ItemStack catalyst : HotPotRecipeViewerData.cookingCatalysts()) {
            registry.addWorkstations(COOKING, EntryStacks.of(catalyst));
        }
        for (ItemStack catalyst : HotPotRecipeViewerData.cuttingCatalysts()) {
            registry.addWorkstations(CUTTING, EntryStacks.of(catalyst));
        }
        for (ItemStack catalyst : HotPotRecipeViewerData.poundingCatalysts()) {
            registry.addWorkstations(POUNDING, EntryStacks.of(catalyst));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        // 每次触发都重新枚举，刻数 / 经验是现读配置的，reload 后重新打开 REI 就是新数值
        for (HotPotRecipeViewerData.CookingEntry recipe : HotPotRecipeViewerData.cookingRecipes()) {
            registry.add(new CookingDisplay(recipe));
        }
        for (HotPotRecipeViewerData.CuttingEntry recipe : HotPotRecipeViewerData.cuttingRecipes()) {
            registry.add(new CuttingDisplay(recipe));
        }
        for (HotPotRecipeViewerData.PoundingEntry recipe : HotPotRecipeViewerData.poundingRecipes()) {
            registry.add(new PoundingDisplay(recipe));
        }
    }

    // ------------------------------------------------------------------ 火锅涮煮

    /** 火锅涮煮的展示数据。 */
    public record CookingDisplay(HotPotRecipeViewerData.CookingEntry recipe) implements Display {
        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(EntryIngredients.of(recipe.input()));
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            List<EntryIngredient> outputs = new ArrayList<>(2);
            outputs.add(EntryIngredients.of(recipe.output()));
            if (!recipe.overcookOutput().isEmpty()) {
                outputs.add(EntryIngredients.of(recipe.overcookOutput()));
            }
            return outputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return COOKING;
        }
    }

    /** 火锅涮煮类别：左输入 → 箭头 → 熟食；整块冷豆腐额外展示久煮碎成的热豆腐块，并写明时间。 */
    private static final class CookingCategory implements DisplayCategory<CookingDisplay> {
        @Override
        public CategoryIdentifier<? extends CookingDisplay> getCategoryIdentifier() {
            return COOKING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("text.orients_hotpot_chronicles.recipe.cooking");
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(new ItemStack(HotPotContent.COPPER_HOT_POT_ITEM));
        }

        @Override
        public int getDisplayHeight() {
            return 66;
        }

        @Override
        public List<Widget> setupDisplay(CookingDisplay display, Rectangle bounds) {
            HotPotRecipeViewerData.CookingEntry recipe = display.recipe();
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 8, bounds.getY() + 20))
                .entries(EntryIngredients.of(recipe.input()))
                .markInput());
            widgets.add(Widgets.createArrow(new Point(bounds.getX() + 33, bounds.getY() + 21)));
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 61, bounds.getY() + 20))
                .entries(EntryIngredients.of(recipe.output()))
                .markOutput());
            Component time = Component.translatable("text.orients_hotpot_chronicles.recipe.cooking.time",
                seconds(recipe.cookTicks()), String.valueOf(recipe.cookTicks()));
            widgets.add(Widgets.createLabel(new Point(bounds.getX() + 8, bounds.getY() + 5), time)
                .leftAligned().noShadow().color(TEXT, TEXT_DARK));
            if (!recipe.overcookOutput().isEmpty()) {
                // 「久煮」指煮熟之后再继续煮的时间：总刻数减去煮熟那一段
                int extra = Math.max(0, recipe.overcookTotalTicks() - recipe.cookTicks());
                widgets.add(Widgets.createSlot(new Point(bounds.getX() + 91, bounds.getY() + 20))
                    .entries(EntryIngredients.of(recipe.overcookOutput()))
                    .markOutput());
                Component overcook = Component.translatable("text.orients_hotpot_chronicles.recipe.cooking.overcook",
                    seconds(extra), recipe.overcookOutput().getHoverName());
                widgets.add(Widgets.createLabel(new Point(bounds.getX() + 8, bounds.getY() + 48), overcook)
                    .leftAligned().noShadow().color(WARN, WARN_DARK));
            }
            return widgets;
        }
    }

    // ------------------------------------------------------------------ 砧板切配

    /** 砧板切配的展示数据：输入食材 + 菜刀 → 产物。 */
    public record CuttingDisplay(HotPotRecipeViewerData.CuttingEntry recipe) implements Display {
        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(EntryIngredients.of(recipe.input()), EntryIngredients.of(recipe.tool()));
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return List.of(EntryIngredients.of(recipe.output()));
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return CUTTING;
        }
    }

    /** 砧板切配类别：食材 + 菜刀 → 切好的产物，注明一刀切完还是分刀切。 */
    private static final class CuttingCategory implements DisplayCategory<CuttingDisplay> {
        @Override
        public CategoryIdentifier<? extends CuttingDisplay> getCategoryIdentifier() {
            return CUTTING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("text.orients_hotpot_chronicles.recipe.cutting");
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(new ItemStack(HotPotContent.CUTTING_BOARD_ITEM));
        }

        @Override
        public List<Widget> setupDisplay(CuttingDisplay display, Rectangle bounds) {
            HotPotRecipeViewerData.CuttingEntry recipe = display.recipe();
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 8, bounds.getY() + 22))
                .entries(EntryIngredients.of(recipe.input()))
                .markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 32, bounds.getY() + 22))
                .entries(EntryIngredients.of(recipe.tool()))
                .markInput());
            widgets.add(Widgets.createArrow(new Point(bounds.getX() + 57, bounds.getY() + 23)));
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 85, bounds.getY() + 22))
                .entries(EntryIngredients.of(recipe.output()))
                .markOutput());
            Component hint = recipe.pieceByPiece()
                ? Component.translatable("text.orients_hotpot_chronicles.recipe.cutting.multi",
                    String.valueOf(recipe.output().getCount()))
                : Component.translatable("text.orients_hotpot_chronicles.recipe.cutting.single");
            widgets.add(Widgets.createLabel(new Point(bounds.getX() + 8, bounds.getY() + 5), hint)
                .leftAligned().noShadow().color(HINT, HINT_DARK));
            return widgets;
        }
    }

    // ------------------------------------------------------------------ 擀面杖拍打

    /** 擀面杖拍打的展示数据：输入食材 → 成品；中间两下砧板上的样子只用于展示。 */
    public record PoundingDisplay(HotPotRecipeViewerData.PoundingEntry recipe) implements Display {
        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(EntryIngredients.of(recipe.input()), EntryIngredients.of(recipe.tool()));
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return List.of(EntryIngredients.of(recipe.output()));
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return POUNDING;
        }
    }

    /** 擀面杖拍打类别：食材 → 第 1 下 → 第 2 下 → 成品，并标注拍打次数。 */
    private static final class PoundingCategory implements DisplayCategory<PoundingDisplay> {
        @Override
        public CategoryIdentifier<? extends PoundingDisplay> getCategoryIdentifier() {
            return POUNDING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("text.orients_hotpot_chronicles.recipe.pounding");
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(new ItemStack(HotPotContent.ROLLING_PIN));
        }

        @Override
        public int getDisplayHeight() {
            return 66;
        }

        @Override
        public List<Widget> setupDisplay(PoundingDisplay display, Rectangle bounds) {
            HotPotRecipeViewerData.PoundingEntry recipe = display.recipe();
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 8, bounds.getY() + 20))
                .entries(EntryIngredients.of(recipe.input()))
                .markInput());
            // 中间两个阶段只是「砧板上现在长什么样」，所以不加输入 / 输出标记
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 32, bounds.getY() + 20))
                .entries(EntryIngredients.of(recipe.stage1Display()))
                .disableBackground());
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 56, bounds.getY() + 20))
                .entries(EntryIngredients.of(recipe.stage2Display()))
                .disableBackground());
            widgets.add(Widgets.createArrow(new Point(bounds.getX() + 79, bounds.getY() + 21)));
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 101, bounds.getY() + 20))
                .entries(EntryIngredients.of(recipe.output()))
                .markOutput());
            widgets.add(Widgets.createSlot(new Point(bounds.getX() + 8, bounds.getY() + 44))
                .entries(EntryIngredients.of(recipe.tool()))
                .markInput());
            Component hits = Component.translatable("text.orients_hotpot_chronicles.recipe.pounding.hits",
                String.valueOf(recipe.hits()));
            widgets.add(Widgets.createLabel(new Point(bounds.getX() + 32, bounds.getY() + 48), hits)
                .leftAligned().noShadow().color(HINT, HINT_DARK));
            return widgets;
        }
    }
}
