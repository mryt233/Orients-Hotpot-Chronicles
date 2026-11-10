package cn.blockforge.generated.orientshotpotchronicles.client.compat.jei;

import static cn.blockforge.generated.orientshotpotchronicles.compat.HotPotRecipeViewerData.*;

import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotRecipeViewerData;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.client.SauceStationScreen;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


/**
 * JEI 插件：把火锅涮煮、砧板切配、擀面杖拍打三类玩法配方交给 JEI 展示。
 * <p>
 * 配方内容来自核心层的 {@link HotPotRecipeViewerData}，本类只负责界面排版，因此 JEI 与 REI 两边展示的内容一致；
 * 没装 JEI 时这个类不会被实例化（Fabric 由 {@code jei_mod_plugin} 入口点、NeoForge 由 {@code @JeiPlugin} 注解扫描发现），
 * 模组照常运行、也不会因为缺少 JEI 报错。
 * 作者：mryt233 */
@JeiPlugin
// mryt233 改（2026-11-11）：共享配方文字和颜色。
public final class HotPotJeiPlugin implements IModPlugin {
    /** 火锅涮煮分类。 */
    public static final RecipeType<HotPotRecipeViewerData.CookingEntry> COOKING =
        RecipeType.create(HotPot.MOD_ID, "hotpot_cooking", HotPotRecipeViewerData.CookingEntry.class);
    /** 砧板切配分类。 */
    public static final RecipeType<HotPotRecipeViewerData.CuttingEntry> CUTTING =
        RecipeType.create(HotPot.MOD_ID, "cutting_board", HotPotRecipeViewerData.CuttingEntry.class);
    /** 擀面杖拍打分类。 */
    public static final RecipeType<HotPotRecipeViewerData.PoundingEntry> POUNDING =
        RecipeType.create(HotPot.MOD_ID, "pounding", HotPotRecipeViewerData.PoundingEntry.class);


    @Override
    public ResourceLocation getPluginUid() {
        return HotPot.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new CookingCategory(guiHelper),
            new CuttingCategory(guiHelper),
            new PoundingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // mryt233 改（2026-11-11）：枚举当前客户端配方与配置，实际界面重建由查看器生命周期负责。
        registration.addRecipes(COOKING, HotPotRecipeViewerData.cookingRecipes(net.minecraft.client.Minecraft.getInstance().level));
        registration.addRecipes(CUTTING, HotPotRecipeViewerData.cuttingRecipes());
        registration.addRecipes(POUNDING, HotPotRecipeViewerData.poundingRecipes(net.minecraft.client.Minecraft.getInstance().level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ItemStack catalyst : HotPotRecipeViewerData.cookingCatalysts()) {
            registration.addRecipeCatalyst(catalyst, COOKING);
        }
        for (ItemStack catalyst : HotPotRecipeViewerData.cuttingCatalysts()) {
            registration.addRecipeCatalyst(catalyst, CUTTING);
        }
        for (ItemStack catalyst : HotPotRecipeViewerData.poundingCatalysts()) {
            registration.addRecipeCatalyst(catalyst, POUNDING);
        }
    }

    // mryt233 改（r146f）：实时读取面板边界，关闭原料表后返回空区域。
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!HotPotCompat.isModLoaded("jei")) return;
        registration.addGuiContainerHandler(SauceStationScreen.class, new IGuiContainerHandler<SauceStationScreen>() {
            @Override
            public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(SauceStationScreen screen) {
                return screen.guideAreas();
            }
        });
    }



    /** 在配方区域里画一行文字；超出可用宽度时按宽度截断，避免串到相邻槽位。 */
    private static void drawLine(GuiGraphics guiGraphics, Component text, int x, int y, int color, int maxWidth) {
        var font = Minecraft.getInstance().font;
        String line = maxWidth > 0 ? font.substrByWidth(text, maxWidth).getString() : text.getString();
        guiGraphics.drawString(font, line, x, y, color, false);
    }

    // ------------------------------------------------------------------ 火锅涮煮

    /** 火锅涮煮分类：生食材 →（煮 N 秒）熟食；整块冷豆腐还会提示久煮碎成热豆腐块。 */
    private static final class CookingCategory implements IRecipeCategory<HotPotRecipeViewerData.CookingEntry> {
        private final IDrawable icon;

        private CookingCategory(IGuiHelper guiHelper) {
            this.icon = guiHelper.createDrawableItemStack(new ItemStack(HotPotContent.COPPER_HOT_POT_ITEM));
        }

        @Override
        public RecipeType<HotPotRecipeViewerData.CookingEntry> getRecipeType() {
            return COOKING;
        }

        @Override
        public Component getTitle() {
            return recipeText("cooking");
        }

        @Override
        public int getWidth() {
            return 150;
        }

        @Override
        public int getHeight() {
            return 54;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, HotPotRecipeViewerData.CookingEntry recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 18).addItemStack(recipe.input());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 66, 18).addItemStack(recipe.output());
            if (!recipe.overcookOutput().isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 18).addItemStack(recipe.overcookOutput());
            }
        }

        @Override
        public void draw(HotPotRecipeViewerData.CookingEntry recipe, IRecipeSlotsView recipeSlotsView,
                         GuiGraphics guiGraphics, double mouseX, double mouseY) {
            Component time = cookingTime(recipe);
            drawLine(guiGraphics, time, 8, 4, TEXT, 134);
            if (!recipe.overcookOutput().isEmpty()) {
                // 「久煮」指煮熟之后再继续煮的时间：总刻数减去煮熟那一段
                Component overcook = overcookHint(recipe);
                drawLine(guiGraphics, overcook, 8, 40, WARN, 134);
            }
        }
    }

    // ------------------------------------------------------------------ 砧板切配

    /** 砧板切配分类：食材 + 菜刀 → 切好的产物，并注明一刀切完还是分刀切。 */
    private static final class CuttingCategory implements IRecipeCategory<HotPotRecipeViewerData.CuttingEntry> {
        private final IDrawable icon;

        private CuttingCategory(IGuiHelper guiHelper) {
            this.icon = guiHelper.createDrawableItemStack(new ItemStack(HotPotContent.CUTTING_BOARD_ITEM));
        }

        @Override
        public RecipeType<HotPotRecipeViewerData.CuttingEntry> getRecipeType() {
            return CUTTING;
        }

        @Override
        public Component getTitle() {
            return recipeText("cutting");
        }

        @Override
        public int getWidth() {
            return 150;
        }

        @Override
        public int getHeight() {
            return 54;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, HotPotRecipeViewerData.CuttingEntry recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 20).addItemStack(recipe.input());
            builder.addSlot(RecipeIngredientRole.CATALYST, 66, 20).addItemStack(recipe.tool());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 20).addItemStack(recipe.output());
        }

        @Override
        public void draw(HotPotRecipeViewerData.CuttingEntry recipe, IRecipeSlotsView recipeSlotsView,
                         GuiGraphics guiGraphics, double mouseX, double mouseY) {
            Component hint = cuttingHint(recipe);
            drawLine(guiGraphics, hint, 8, 4, HINT, 134);
        }
    }

    // ------------------------------------------------------------------ 擀面杖拍打

    /** 擀面杖拍打分类：食材 → 第 1 下 → 第 2 下 → 成品，标注拍打次数。 */
    private static final class PoundingCategory implements IRecipeCategory<HotPotRecipeViewerData.PoundingEntry> {
        private final IDrawable icon;

        private PoundingCategory(IGuiHelper guiHelper) {
            this.icon = guiHelper.createDrawableItemStack(new ItemStack(HotPotContent.ROLLING_PIN));
        }

        @Override
        public RecipeType<HotPotRecipeViewerData.PoundingEntry> getRecipeType() {
            return POUNDING;
        }

        @Override
        public Component getTitle() {
            return recipeText("pounding");
        }

        @Override
        public int getWidth() {
            return 150;
        }

        @Override
        public int getHeight() {
            return 64;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, HotPotRecipeViewerData.PoundingEntry recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 20).addItemStack(recipe.input());
            // 中间两个阶段只用来展示「砧板上现在是什么样」，不能当材料用
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 40, 20).addItemStack(recipe.stage1Display());
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 72, 20).addItemStack(recipe.stage2Display());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 20).addItemStack(recipe.output());
            builder.addSlot(RecipeIngredientRole.CATALYST, 8, 44).addItemStack(recipe.tool());
        }

        @Override
        public void draw(HotPotRecipeViewerData.PoundingEntry recipe, IRecipeSlotsView recipeSlotsView,
                         GuiGraphics guiGraphics, double mouseX, double mouseY) {
            Component hits = poundingHint(recipe);
            drawLine(guiGraphics, hits, 8, 4, HINT, 134);
        }
    }
}
