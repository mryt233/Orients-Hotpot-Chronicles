<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：按 0.3.1-alpha 正式版重写英文首页总览与版本支持范围。 -->
# Minecraft [Orient's Hotpot Chronicles] Mod — Gameplay Guide

:::tip
**This guide covers packaged 0.3.1-alpha, demonstrated on Minecraft 1.21.1 Fabric** (1.21.1 NeoForge, 1.20.1 Fabric / Forge, and Fabric / NeoForge on 26.1.x, 26.2, and 26.3 play the same; see [Downloads, Installation & Feedback](install.html)). This release introduces **Full Mode / Simple Mode** switching, **Chicken Feet & Boneless Chicken Feet**, **Standalone Orange Trees** (`1+3×3×2(+1)` compact trees, azalea-style transparent orange leaves, shader waving, and light 1/5 pass-through slowdown), **9 farmland crops & wild clusters**, a **Stone Mill** (1.5-block stand with automatic pour into adjacent wooden vats), an **Open Wooden Vat** tofu workflow, **40 legacy tables consolidated into 2 universal material-swappable Hotpot Tables**, **3 GUI-less Prep Racks**, **Speed Eating & 4-Track Rhythm minigames**, **LabPBR 1.3** shader maps for hard surfaces, and REI 16 / shader / rendering / migration fixes.
:::

{{icons:brick_stove,iron_nine_grid_hotpot,bamboo_chopsticks,hotpot_table,multi_tier_prep_rack,oak_stool,quartz_eight_slot_platter,oak_sauce_station,stone_mill,wooden_vat,cutting_board,orange_sapling}}

Welcome to **Orient's Hotpot Chronicles** — a Minecraft mod about **gathering around a bubbling hotpot**. Build a stove, place a pot, pour in the broth, grind soybeans and press fresh tofu, harvest nine farmable seasonings and greens, simmer tripe and fatty beef rolls, grab bamboo chopsticks, mix your dipping sauce, and toast friends at the table.

The guide has two parts: **How to Play** and **Reference**:

| What you want to do | Where to look |
| --- | --- |
| Download, install, check supported versions, give feedback | [Downloads, Installation & Feedback](install.html) |
| First time playing and want your first bite fast | [Hotpot in Ten Minutes](quickstart.html) |
| Stoves, pots, broths, heat levels, pot screen & minigames | [Stoves & Hotpots](hotpot.html) |
| Swapping table materials, three prep racks, stools & benches | [Tables, Prep Racks & Seating](tables.html) |
| Plates, bowls, soup bowls, drinks, toasts, aprons & chopsticks | [Tableware & Drinks](tableware.html) |
| Tripe, meats, mill & vat tofu, orange trees & 9 crops | [Ingredients & Cutting](ingredients.html) |
| Self-service sauce station and 15 seasonings | [Dipping Sauces](sauce.html) |
| Mode toggles, config tuning, data packs & common questions | [Frequently Asked Questions](faq.html) |
| Crafting, pounding, milling, vatting, or cooking recipes | [Recipe Collection](recipes.html) |
| Browse every item and block in the mod | [Item Catalog](items.html) |
| Upcoming work and release history | [Development Preview](preview.html) / [Changelog](changelog.html) |

{{img:stoves-boiling.webp|Copper and iron pots boiling over lit stoves}}

## Mod overview

- **Stoves & Hotpots**: short and tall stoves; copper and iron pots (nine-grid, multiple Yuanyang dividers, four-grid); 6 broths and 4 Spicy Red Broth heat levels; fuel with wood, coal, lava buckets, or lit campfires, plus press `G` to start Speed Eating or 4-Track Rhythm minigames.
- **Tables, Racks & Seating**: universal low and tall hotpot tables (default plank top + brick base; swap tabletop and base materials independently with any solid block, uniformly non-flammable) and three GUI-less prep racks (floor / desktop / wall-or-ceiling hanging rack); 11 wood styles of stools and benches connecting up to 3 blocks and stacking as ladders.
- **Tableware & Aprons**: 5 materials × 6 plate sizes; vanilla bowls plus 6 flavored soup bowls and a generic soup bowl; three drinks, glass cups, and toasting; three recolorable cloth aprons blocking spice flames and nearby splashes; bamboo chopsticks (with mini tabletop rests or rim resting).
- **Ingredients & Farming**: tripe, fatty beef rolls, mutton rolls, beef loin slices, chicken feet & boneless chicken feet, shrimp paste, ham sausage, crispy pork, dried tofu / tofu skin / wide noodles / sweet potato skin, egg fried rice, and more; Full Mode uses the Stone Mill (auto-pouring into an adjacent empty vat) and Open Wooden Vat to press cold tofu, while Simple Mode restores quick crafting; plus standalone orange trees and 9 farmland crops with wild clusters.
- **Sauces & Modes**: self-service sauce station with 15 stackable seasonings; lit pots grant ongoing Regeneration within 6 blocks; one-click **Full Mode / Simple Mode** presets and 18 hot-reloadable config groups, alongside deep support for shaders (LabPBR 1.3 maps, transparent waving orange leaves) and JEI / REI 16 / Jade.

:::tip
Every **recipe card** and **catalog entry** in this guide is generated directly from the game's recipes and language files so nothing goes out of sync. Version badges beside headings show **when each feature was added**.
:::

<!-- mryt233 改，2026-10-30：补 #41、#52 游戏内指南的入口和配方。 -->
## In-game hotpot guide {#guide}{{since:0.1.4-alpha}}

{{recipe:hotpot_guide}}

Shapelessly craft **Iron Kitchen Knife + Book** to make the Hotpot Guide (consumes the knife), then right-click to open the red guidebook. Pick a category on the left, choose an entry across the top (scroll or drag horizontally when tabs overflow), or click the top-right compass (or press `Ctrl + F` / `/`) for full-text search; turn pages with arrows, arrow keys, or the mouse wheel, and reopening remembers your last page. The bottom-left badge shows **Full Mode** or **Simple Mode**, dynamically switching or collapsing matching notes and recipes inside the book. The book collects all 7 categories, 64 core entries, tables, and recipes directly from this web book source.

## Supported versions and downloads

**0.3.1-alpha** targets **Minecraft 1.21.1 (Fabric, NeoForge)**, **1.20.1 (Fabric, Forge)**, **26.1.x (Fabric, NeoForge)**, **26.2 (Fabric, NeoForge)**, and **26.3 (Fabric, NeoForge)** simultaneously; **this guide demonstrates 1.21.1 Fabric**. See [Downloads, Installation & Feedback](install.html) for download links (CurseForge, Modrinth, MCMOD, GitHub), installation steps, and contact info (GitHub, QQ group).

:::warn
This mod is still in **alpha**. `0.3.1-alpha` losslessly migrates the 40 legacy preset tables in existing worlds to universal hotpot tables and silently cleans up removed legacy config keys; still, if upgrading from very early builds such as `0.1.0-alpha.1`, **always back up your world first**.
:::

**Questions or suggestions?** Join QQ group **248314791** or open an issue on [GitHub](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose).
