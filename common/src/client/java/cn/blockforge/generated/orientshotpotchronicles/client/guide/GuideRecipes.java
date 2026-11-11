// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

// mryt233 改（2026-11-11）：图标和排版职责迁出。

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 作者：mryt233；2026-11-11：指南配方绘制与单实例缓存。 */
public final class GuideRecipes {
    private static final ResourceLocation TEX = HotPot.id("textures/gui/guide_book.png");
    private static final int TEX_W = 512, TEX_H = 256;

    /** 两页文字区：左页 x 16、右页 x 144（相对书的左上角），宽 112，从 y 12 起高 144。 */
    private static final int COL_W = 112;
    private static final int MUTED = 0xFF7A6040, NUMBER = 0xFF1E6E78;

    /** 产物物品 → 做出它的配方；配方表换了（/reload、换世界）就整份重建。mryt233 改：每帧查表改成一次 HashMap.get。 */
    private Object recipeCacheOwner;
    /**
     * 建缓存时 {@code RecipeManager.getRecipes()} 返回的那个集合实例。
     * mryt233 改：客户端收到配方同步走的是 {@code RecipeManager.replaceRecipes}（原地换内容、实例不变），
     * 只比 RecipeManager 实例的话 /reload 之后缓存永远不刷新；{@code getRecipes()} 背后是 ImmutableMap 的
     * values（每次重建配方表都是新 map、新 view 实例），用它当「配方表换代了」的 O(1) 判据。
     */
    private Object recipeCacheKey;
    private Map<Item, RecipeHolder<? extends Recipe<?>>> recipeCache;

    @FunctionalInterface
    public interface HoverSink { void track(int x, int y, int w, int h, ItemStack stack); }
    public static List<ResourceLocation> ids(JsonArray a) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (JsonElement r : a) {
            ResourceLocation id = ResourceLocation.tryParse(r.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** 配方块：工作台画九宫格，切石机 / 熔炉一类画「原料 → 产物」两格（配方大全里有切石和烹饪）；
     *  下面一行写产物名；几个配方 2 秒换一个，标「1/5」，原料有几种可选的每秒轮换。 */
    public void draw(GuiGraphics g, net.minecraft.client.gui.Font font, Minecraft minecraft, int x, int y, List<ResourceLocation> ids, HoverSink sink) {
        if (minecraft == null || minecraft.level == null || ids.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int idx = (int) ((now / 2000) % ids.size());
        Item item = BuiltInRegistries.ITEM.getOptional(ids.get(idx)).orElse(Items.BARRIER);
        RecipeHolder<? extends Recipe<?>> holder = findRecipe(minecraft, item);
        Recipe<?> recipe = holder == null ? null : holder.value();
        boolean grid = recipe == null || recipe instanceof CraftingRecipe;
        int gx = x + 10, gy = y + 1;
        ItemStack out = recipe != null ? recipe.getResultItem(minecraft.level.registryAccess()) : new ItemStack(item);
        if (grid) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    g.blit(TEX, gx + c * 18, gy + r * 18, 340, 0, 18, 18, TEX_W, TEX_H);
                }
            }
            if (recipe != null) {
                int w = recipe instanceof ShapedRecipe s ? s.getWidth() : 3;
                List<Ingredient> ings = recipe.getIngredients();
                for (int i = 0; i < ings.size() && i < 9; i++) {
                    ItemStack[] stacks = ings.get(i).getItems();
                    if (stacks.length > 0) {
                        ItemStack s = stacks[(int) ((now / 1000) % stacks.length)];
                        int sx = gx + (i % w) * 18 + 1, sy = gy + (i / w) * 18 + 1;
                        g.renderItem(s, sx, sy);
                        sink.track(sx, sy, 16, 16, s);
                    }
                }
            }
            g.blit(TEX, gx + 58, gy + 23, 340, 18, 12, 8, TEX_W, TEX_H);
            int ox = gx + 74, oy = gy + 18;
            g.blit(TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
            g.renderItem(out, ox + 1, oy + 1);
            g.renderItemDecorations(font, out, ox + 1, oy + 1);
            sink.track(ox + 1, oy + 1, 16, 16, out);
        } else {
            // 切石机 / 熔炉：一格原料，箭头，一格产物；上面一行小字写是哪一种
            int sx = gx + 12, sy = gy + 4;
            g.blit(TEX, sx, sy, 340, 0, 18, 18, TEX_W, TEX_H);
            List<Ingredient> ings = recipe.getIngredients();
            if (!ings.isEmpty()) {
                ItemStack[] stacks = ings.get(0).getItems();
                if (stacks.length > 0) {
                    ItemStack in = stacks[(int) ((now / 1000) % stacks.length)];
                    g.renderItem(in, sx + 1, sy + 1);
                    sink.track(sx + 1, sy + 1, 16, 16, in);
                }
            }
            g.blit(TEX, sx + 26, sy + 5, 340, 18, 12, 8, TEX_W, TEX_H);
            int ox = gx + 44, oy = gy + 4;
            g.blit(TEX, ox, oy, 340, 0, 18, 18, TEX_W, TEX_H);
            g.renderItem(out, ox + 1, oy + 1);
            g.renderItemDecorations(font, out, ox + 1, oy + 1);
            sink.track(ox + 1, oy + 1, 16, 16, out);
            Component how = holder != null && holder.value().getType() == RecipeType.STONECUTTING
                ? Blocks.STONECUTTER.getName() : Blocks.FURNACE.getName();
            String label = how.getString();
            g.drawString(font, label, x + (COL_W - font.width(label)) / 2, gy + 28, MUTED, false);
        }
        Component name = out.getHoverName();
        String count = ids.size() > 1 ? "  " + (idx + 1) + "/" + ids.size() : "";
        FormattedCharSequence line = font.split(name, COL_W - font.width(count)).get(0);
        int lw = font.width(line) + font.width(count);
        int lx = x + (COL_W - lw) / 2;
        g.drawString(font, line, lx, y + 57, NUMBER, false);
        if (!count.isEmpty()) {
            g.drawString(font, count, lx + font.width(line), y + 57, MUTED, false);
        }
    }

    /**
     * 找做出这件东西的配方：先是工作台（没找到就只画产物），再找切石机和熔炉一类。
     * mryt233 改：按产物建一次索引后复用——以前每个配方块每一帧都要把 5 类配方整表取一遍
     * （{@code getAllRecipesFor} 每次都是 {@code List.copyOf}），翻到配方多的页时每帧都在造垃圾。
     */
    private RecipeHolder<? extends Recipe<?>> findRecipe(Minecraft minecraft, Item item) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        RecipeManager rm = minecraft.level.getRecipeManager();
        Object key = rm.getRecipes();
        if (recipeCache == null || recipeCacheOwner != rm || recipeCacheKey != key) {
            Map<Item, RecipeHolder<? extends Recipe<?>>> cache = new HashMap<>();
            HolderLookup.Provider registries = minecraft.level.registryAccess();
            // putIfAbsent = 先找到的那一类优先，和原来 5 个循环的先后顺序一致
            index(cache, rm, RecipeType.CRAFTING, registries);
            index(cache, rm, RecipeType.STONECUTTING, registries);
            index(cache, rm, RecipeType.SMELTING, registries);
            index(cache, rm, RecipeType.SMOKING, registries);
            index(cache, rm, RecipeType.CAMPFIRE_COOKING, registries);
            recipeCache = cache;
            recipeCacheOwner = rm;
            recipeCacheKey = key;
        }
        return recipeCache.get(item);
    }

    /** 把一类配方的「产物物品 → 配方」填进索引；先填进去的那一类优先。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void index(Map<Item, RecipeHolder<? extends Recipe<?>>> cache, RecipeManager rm,
                              RecipeType<? extends Recipe<?>> type, HolderLookup.Provider registries) {
        List<RecipeHolder<? extends Recipe<?>>> all =
            (List<RecipeHolder<? extends Recipe<?>>>) (List) rm.getAllRecipesFor((RecipeType) type);
        for (RecipeHolder<? extends Recipe<?>> holder : all) {
            ItemStack result = holder.value().getResultItem(registries);
            if (!result.isEmpty()) {
                cache.putIfAbsent(result.getItem(), holder);
            }
        }
    }

}
