// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RecipeSync;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 作者：mryt233；2026-11-11：指南配方绘制与单实例缓存（26.1 RecipeDisplay / RecipeSync / GuiGraphicsExtractor）。 */
public final class GuideRecipes {
    private static final Identifier TEX = HotPot.id("textures/gui/guide_book.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int COL_W = 112;
    private static final int MUTED = 0xFF7A6040, NUMBER = 0xFF1E6E78;

    private Object recipeCacheKey;
    private Map<Item, RecipeHolder<? extends Recipe<?>>> recipeCache;

    @FunctionalInterface
    public interface HoverSink { void track(int x, int y, int w, int h, ItemStack stack); }

    public static List<Identifier> ids(JsonArray a) {
        List<Identifier> ids = new ArrayList<>();
        for (JsonElement r : a) {
            Identifier id = Identifier.tryParse(r.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** 配方块：26.1 配方结果与原料走 {@code RecipeDisplay} / {@code SlotDisplay}，配方来源从 {@link RecipeSync} 取。 */
    public void draw(GuiGraphicsExtractor g, Font font, Minecraft minecraft, int x, int y, List<Identifier> ids, HoverSink sink) {
        if (minecraft == null || minecraft.level == null || ids.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int idx = (int) ((now / 2000) % ids.size());
        Item item = BuiltInRegistries.ITEM.getOptional(ids.get(idx)).orElse(Items.BARRIER);
        ContextMap displayContext = SlotDisplayContext.fromLevel(minecraft.level);
        RecipeDisplay display = findDisplay(minecraft, item, displayContext);
        RecipeHolder<? extends Recipe<?>> holder = findRecipe(minecraft, item);
        boolean grid = display == null || display instanceof ShapedCraftingRecipeDisplay
            || display instanceof ShapelessCraftingRecipeDisplay;
        int gx = x + 10, gy = y + 1;
        ItemStack out = display != null
            ? display.result().resolveForStacks(displayContext).stream().findFirst().orElseGet(() -> new ItemStack(item))
            : new ItemStack(item);
        if (grid) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    g.blit(RenderPipelines.GUI_TEXTURED, TEX, gx + c * 18, gy + r * 18, 340, 0, 18, 18, TEX_W, TEX_H);
                }
            }
            if (display != null) {
                int w = display instanceof ShapedCraftingRecipeDisplay shaped ? shaped.width() : 3;
                List<SlotDisplay> ings = display instanceof ShapedCraftingRecipeDisplay shaped
                    ? shaped.ingredients()
                    : display instanceof ShapelessCraftingRecipeDisplay shapeless
                    ? shapeless.ingredients()
                    : List.of();
                for (int i = 0; i < ings.size() && i < 9; i++) {
                    ItemStack[] stacks = ings.get(i).resolveForStacks(displayContext).toArray(ItemStack[]::new);
                    if (stacks.length > 0) {
                        ItemStack s = stacks[(int) ((now / 1000) % stacks.length)];
                        int sx = gx + (i % w) * 18 + 1, sy = gy + (i / w) * 18 + 1;
                        g.item(s, sx, sy);
                        sink.track(sx, sy, 16, 16, s);
                    }
                }
            }
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, gx + 58, gy + 23, 340, 18, 12, 8, TEX_W, TEX_H);
            int ox = gx + 74, oy = gy + 18;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
            g.item(out, ox + 1, oy + 1);
            g.itemDecorations(font, out, ox + 1, oy + 1);
            sink.track(ox + 1, oy + 1, 16, 16, out);
        } else {
            int sx = gx + 12, sy = gy + 4;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, sx, sy, 340, 0, 18, 18, TEX_W, TEX_H);
            List<SlotDisplay> ings = display instanceof StonecutterRecipeDisplay cutting
                ? List.of(cutting.input())
                : display instanceof FurnaceRecipeDisplay smelting
                ? List.of(smelting.ingredient())
                : List.of();
            if (!ings.isEmpty()) {
                ItemStack[] stacks = ings.get(0).resolveForStacks(displayContext).toArray(ItemStack[]::new);
                if (stacks.length > 0) {
                    ItemStack in = stacks[(int) ((now / 1000) % stacks.length)];
                    g.item(in, sx + 1, sy + 1);
                    sink.track(sx + 1, sy + 1, 16, 16, in);
                }
            }
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, sx + 26, sy + 5, 340, 18, 12, 8, TEX_W, TEX_H);
            int ox = gx + 44, oy = gy + 4;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
            g.item(out, ox + 1, oy + 1);
            g.itemDecorations(font, out, ox + 1, oy + 1);
            sink.track(ox + 1, oy + 1, 16, 16, out);
            Component how = holder != null && holder.value().getType() == RecipeType.STONECUTTING
                ? Blocks.STONECUTTER.getName() : Blocks.FURNACE.getName();
            String label = how.getString();
            g.text(font, label, x + (COL_W - font.width(label)) / 2, gy + 28, MUTED, false);
        }
        Component name = out.getHoverName();
        String count = ids.size() > 1 ? "  " + (idx + 1) + "/" + ids.size() : "";
        FormattedCharSequence line = font.split(name, COL_W - font.width(count)).get(0);
        int lw = font.width(line) + font.width(count);
        int lx = x + (COL_W - lw) / 2;
        g.text(font, line, lx, y + 57, NUMBER, false);
        if (!count.isEmpty()) {
            g.text(font, count, lx + font.width(line), y + 57, MUTED, false);
        }
    }

    private RecipeHolder<? extends Recipe<?>> findRecipe(Minecraft minecraft, Item item) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Object source = RecipeSync.source(minecraft.level);
        if (recipeCache == null || recipeCacheKey != source) {
            Map<Item, RecipeHolder<? extends Recipe<?>>> cache = new HashMap<>();
            ContextMap displayContext = SlotDisplayContext.fromLevel(minecraft.level);
            index(minecraft, cache, RecipeType.CRAFTING, displayContext);
            index(minecraft, cache, RecipeType.STONECUTTING, displayContext);
            index(minecraft, cache, RecipeType.SMELTING, displayContext);
            index(minecraft, cache, RecipeType.SMOKING, displayContext);
            index(minecraft, cache, RecipeType.CAMPFIRE_COOKING, displayContext);
            recipeCache = cache;
            recipeCacheKey = source;
        }
        return recipeCache.get(item);
    }

    private static void index(Minecraft minecraft, Map<Item, RecipeHolder<? extends Recipe<?>>> cache,
                              RecipeType<? extends Recipe<?>> type, ContextMap displayContext) {
        for (RecipeHolder<?> holder : RecipeSync.recipes(minecraft.level)) {
            if (holder.value().getType() != type) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                for (ItemStack result : display.result().resolveForStacks(displayContext)) {
                    if (!result.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        RecipeHolder<? extends Recipe<?>> typed = (RecipeHolder<? extends Recipe<?>>) holder;
                        cache.putIfAbsent(result.getItem(), typed);
                    }
                }
            }
        }
    }

    private static RecipeDisplay findDisplay(Minecraft minecraft, Item item, ContextMap displayContext) {
        for (RecipeHolder<?> entry : RecipeSync.recipes(minecraft.level)) {
            if (!(entry.value() instanceof CraftingRecipe) && entry.value().getType() != RecipeType.STONECUTTING
                && !(entry.value() instanceof AbstractCookingRecipe)) {
                continue;
            }
            for (RecipeDisplay display : entry.value().display()) {
                if (display.result().resolveForStacks(displayContext).stream().anyMatch(result -> result.is(item))) {
                    return display;
                }
            }
        }
        return null;
    }
}
