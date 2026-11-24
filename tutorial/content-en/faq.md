<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：按 0.3.1-alpha 正式版重写英文常见问题，补充模式切换、石磨木桶、橙子树/作物、光影/REI16 兼容与指令说明。 -->
# Frequently Asked Questions

## Pots and stoves

**Q: There is broth in the pot. Why won't the stove light?**
Check three things: ① fuel in the firebox (right-click brick body to inspect); ② pot placed on the stove; ③ broth actually poured inside (pots without broth will not ignite). Lit pots give off steam and boil.

**Q: The stove is lit, but fuel isn't draining?**
That's intended. Fuel is only spent while **raw ingredients are cooking** — heating broth alone is "idle simmer" with zero fuel drain (see [Stoves & Hotpots](hotpot.html#fuel)).

**Q: I can't put an item into the pot.**
Only **raw cookable ingredients** fit inside — cooked food, chili peppers / millet chili, and items tagged `orients_hotpot_chronicles:not_pot_ingredient` are rejected. Also check whether your targeted large cell already holds 9 small portions.

**Q: How do I switch broths?**
**Right-click the pot with an empty bucket** to scoop broth back out (remove all food first). Mushroom broth is scooped with a **bowl** when empty of food.

**Q: Why is food cooking slowly or not at all?**
- Confirm the pot isn't a "cold pot" sitting off a lit stove or campfire.
- Check heat (wood ×0.65, coal ×1.0, lava bucket ×1.6) and broth (Rich Stock is slowest; Spicy Red Broth is fastest). Wood + Rich Stock cooks noticeably slowly.
- Items have different base times: whole beef takes 5 s, fatty beef rolls 2.5 s, beef loin slices ~1 s+ (check Cooking and Pot-Only Boiling in [Recipe Collection](recipes.html)).

**Q: Why did eating give me Nausea?**
With penalties active (Full Mode default), eating from an unheated **cold pot** has a 30% chance of 15 s Nausea. Clear it with **Plant Protein Drink**, a bowl of **Clear Broth**, or **vinegar** dip — or switch to **Simple Mode** / toggle off "Cold-Pot Nausea & Spice Penalties" under `Custom Toggles`.

**Q: Tall stove says blocked / clicking with a pot mounted misses the firebox?**
Clear the block directly above the tall stove when placing it. With a pot on top, right-click the **lower brick body** of the tall stove to reliably open the firebox screen.

**Q: How do I pick up the whole pot?**
On a stove: **empty-hand Shift + right-click** packs the whole pot with broth and ingredients intact; mining also drops it from the pot's height with matching particles.

## Tables, prep racks & seating

**Q: Where did the 40 preset wood/stone tables go, and how do I customize looks now?**
Starting in `0.3.1-alpha`, all 40 preset tables consolidated into unified **Hotpot Table** ({{i:hotpot_table}}) and **Tall Hotpot Table** ({{i:tall_hotpot_table}}). Legacy placed and inventory tables upgrade seamlessly keeping their appearances. Hold **any solid opaque block and right-click the upper top or lower base** for 1-in-1-out swapping (sneak + empty hand ejects it back) — see [Tables, Prep Racks & Seating](tables.html#swap_materials).

**Q: I can't sit on a stool or bench.**
Right-click empty-handed (or holding a non-block item, including mod crops). Leave at least one full block between seated players.

**Q: I can't place another plate on this block.**
Plates cannot overlap: one block fits up to 5 small dishes, 2 long plates, or 1 square plate depending on space used.

**Q: Items on a tall table are hard to target.**
The tall table explicitly routes aim to tableware resting on top — aim slightly sideways or sneak if an edge angle misses.

**Q: How do I pick up a whole plate with food?**
Empty-hand right-click lifts the whole plate and contents; mining breaks only that plate.

## Ingredients, fruit trees & processing

**Q: How do I obtain tripe, beef loin, and raw chicken feet?**
Cows and mooshrooms drop **tripe** (+25% **beef loin**); adult chickens have a 50% chance to drop 1–2 **raw chicken feet** (fire kills drop cooked chicken feet).

**Q: Why did vanilla oak leaves stop dropping oranges, and how do I get soybeans/scallions/cilantro?**
`0.3.1-alpha` introduced **Standalone Orange Trees** and **9 farmable crops**:
- **Oranges**: find dwarf orange trees in Forests, Flower Forests, Plains, Meadows, and Jungles (or plant an Orange Sapling) and right-click ripe fruiting leaves directly.
- **9 Crops** (scallion, cilantro, soybean, garlic, tomato, chili pepper, peanut, greens, houttuynia): harvest **wild plants** in matching biomes or right-click **farmland directly with the crop** (bonemeal works); with "Crops Replace Grass Drops" active (Full Mode default), breaking grass keeps only a **2%** fallback. Disable that toggle or switch to **Simple Mode** to restore legacy 15%–25% grass drops.

**Q: How do I use the Stone Mill and auto-pour soymilk into the Open Wooden Vat?**
1. Insert 2 soybeans into the Stone Mill hopper, pour in water (3 wet grinds per bucket by default), and push 4 empty-hand turns to mill soymilk (dry-grinding peanuts, chilies, or garlic takes no water).
2. Place an **empty {{i:wooden_vat}} directly in front of the Stone Mill spout** to auto-pour finished soymilk cleanly across the rim into the vat without buckets; otherwise scoop {{i:soymilk_bucket}} out with an empty bucket and pour manually. Add {{i:brine_bottle}} and cap with {{i:wooden_press_lid}} for 4 s to press 2 Cold Tofu (or craft directly from soybeans + water in Simple Mode / with "Detailed Tofu Processing" off).

**Q: Nothing will go onto the cutting board.**
Only sliceable or poundable raw ingredients go on the board: slicing handles beef, mutton, porkchop, beef loin, potato, cold/hot tofu, shrimp, ham sausage, chili pepper, lotus root, raw squid, houttuynia, and chicken feet; pounding handles meats/fish, garlic, peeled shrimp, peanuts, and houttuynia. Other cooked/finished foods are blocked.

## Shaders, graphics & mod compatibility

**Q: Any special handling for shader packs and performance mods?**
- Hard surfaces, stoves (including firebox ember emission), tableware, and broth surfaces bundle `LabPBR 1.3` normal/specular (`_n` / `_s`) maps tuned for vanilla, Sodium, and Iris / BSL;
- Orange leaves follow vanilla **Fast / Fancy** graphics between solid foliage and transparent cutouts, and automatically inject Iris foliage-waving properties for orange leaves, saplings, all 9 crops, and wild plants.

**Q: Are JEI, REI 16, Jade, and Axiom supported?**
Fully supported: JEI and **REI 16** (Fabric & NeoForge) show all 5 categories (hotpot simmering, board cutting, rolling-pin pounding, stone mill, wooden vat) plus Sauce Station exclusion zones; **Jade** inspects pot broth/progress/spice, swapped table/rack materials, and vat liquid levels; Creative filter subtabs dodge Axiom and external mod buttons cleanly.

## Saves & versions

**Q: Can old saves upgrade cleanly to 0.3.1-alpha?**
- `0.2.2-alpha` / `0.3.0-alpha` worlds and configs **upgrade seamlessly**: all 40 legacy tables migrate cleanly while retaining their appearance, and removed `soaked_*` config keys are silently pruned with zero reload warnings;
- Still back up before leaping from ultra-early `0.1.0-alpha.1` worlds.

**Q: What integration & datapack hooks exist?**
- Blocks in `#orients_hotpot_chronicles:heat_sources` heat pots; `#not_pot_ingredient` blocks pot insertion;
- Datapacks can add or override **broths** (`recipe/soup/`), **seasonings** (`recipe/seasoning/`), **pot boiling** (`pot_boiling`), **pounding** (`pounding`), **Stone Mill / Wooden Vat** (`stone_mill` / `wooden_vat`), and **rhythm tracks** (`hotpot_tracks/*.json`).

**Q: How do 1.20.1, 26.1.x, 26.2, and 26.3 compare with 1.21.1?**
In `0.3.1-alpha`, **1.21.1 (Fabric / NeoForge), 1.20.1 (Fabric / Forge), 26.1.x (Fabric / NeoForge), 26.2 (Fabric / NeoForge), and 26.3 (Fabric / NeoForge)** share identical gameplay and content — see [Downloads, Installation & Feedback](install.html#ports).

**Q: Where do I report bugs?**
Open an Issue on GitHub <https://github.com/mryt233/Orients-Hotpot-Chronicles> (or join **QQ group 248314791**; see [install details](install.html#feedback)). Include Minecraft version, loader, mod version, reproduction steps, screenshots, and `latest.log`.

<!-- mryt233 改，2026-11-02：0.2.0-alpha 发布，数值配置转正式。 -->
## Full / Simple modes & configurable values {#config}{{since:0.2.0-alpha}}

**Q: Can I simplify steps or customize numbers?**
Yes — select the mod in **Mods** (needs Mod Menu on Fabric; built-in on NeoForge / Forge) and click **Config**:
- **Mode Switching (`0.3.1-alpha`)**{{since:0.3.1-alpha}}: use the top **Full Mode / Simple Mode** preset toggle or expand **▸ Custom Toggles** for 6 granular switches (`Detailed Tofu Processing`, `Cold-Pot & Spice Penalties`, `Crops Replace Grass Drops`, `Real-Time Minigames`, `Prep Rack Material Swap`, `Collapse Secondary Guide Notes`); switching modes **never deletes items or corrupts saves** and live-updates the guidebook's bottom badge and foldable content. Admins can also run `/orientshotpot mode [full|simple]`.
- **Editing JSON directly**: edit `config/orients_hotpot_chronicles.json` and run **`/orientshotpot reload`** (removed legacy keys clean up automatically).
- **Multiplayer**: edit the server file only; changes sync automatically on join, mode switches, and reloads.
- Bad keys fall back safely with warnings while preserving broken files intact.
- Datapack recipes, tracks, and `loot.*` drop edits apply after running vanilla `/reload`.
- Reloading preserves cooking timers on food already in the pot; `cooking.items.<item>` governs this mod's ingredients only.

### Using the settings screen {#screen}

{{since:0.2.2-alpha}}Below the top **Mode Presets & Custom Toggles drawer**, settings split into **Common** (opens first) and **Advanced**:

- **Common**: **Smoke Particle Intensity** slider (with 0% / 50% / 100% shortcuts), **Coal Stove Power**, **Fallback Cook Time**, and **Spicy / Clear / Rich Stock Speed Multipliers**.
- **Advanced**: organizes remaining entries into **18 collapsible groups** (Modes & Mechanics, Crops & Fruit Trees, Ingredient Processing, Tables & Rack Materials, Real-Time Minigames, Cooking Times, Item Cooking Times, Broth Speed Multipliers, Fuel Grades, Soup Bowls, Seasonings, Drinks, Food Nutrition, Pot Warmth, Cold-Pot Nausea, Cheers, Spicy Broth, Loot Drop Chances).
- **Controls**: `- value unit +` steps by 1 (**Shift-click** steps by 10), or click boxes directly to type numbers (invalid values highlight red and block saving).
- **Units**: time in ticks (20 ticks = 1 s, plus seconds where space fits), multipliers as `×`, hunger/XP in points, effect levels, and radii in blocks.
- **Reset**: modified rows show per-row **↺** reset; bottom Reset requires a 4-second second click to reset everything cleanly.
- **Save failure notices**: failed disk writes pause on-screen with clear reasons and preserve/back up corrupt files as `.broken-<timestamp>`.

<!-- mryt233 改（2026-11-13，r148 第四批 4F-1～4F-3） -->
## Adding broths and seasonings via data packs / add-on mods {{since:0.3.0-alpha}} {#custom_soup_sauce}<!--清单#154-->

Starting in **0.3.0-alpha**, broths and seasonings can also be added or overridden via datapacks or Java add-ons:

- **Datapack paths & reload**: place `data/<ns>/recipe/soup/<name>.json` (`orients_hotpot_chronicles:soup_base`) or `data/<ns>/recipe/seasoning/<name>.json` (`orients_hotpot_chronicles:seasoning`) and run `/reload` (auto-syncs to multiplayer clients; hidden from vanilla recipe book).
- **Override priority**: explicit config overrides beat datapacks (`config > datapack > defaults`), while third-party namespaces never pollute built-in config.
- **Generic soup bowl & tinting**: custom broths without a dedicated bowl item use {{i:soup_bowl}}`orients_hotpot_chronicles:soup_bowl` storing `orients_hotpot_chronicles:soup`; pure JSON datapacks automatically tint the clear-broth surface and bubbles, and removing packs later safely shows unknown placeholders without losing data.
- **Java add-on registration**: call `Soups.register(...)` / `Seasonings.register(...)` prior to registry freeze; see `mod/docs/开放登记说明.md` (`mod/content/materials.json` remains solely for built-in static generators).

## Useful commands

```
/orientshotpot reload
/orientshotpot mode full
/orientshotpot mode simple
/orientshotpot minigame solo rush real
/orientshotpot minigame invite rhythm real midnight_boil
/orientshotpot minigame tracks
/give @s orients_hotpot_chronicles:brick_stove
/give @s orients_hotpot_chronicles:iron_nine_grid_hotpot
/give @s orients_hotpot_chronicles:stone_mill
/give @s orients_hotpot_chronicles:wooden_vat
```

Full item IDs are listed in the [Item Catalog](items.html) formatted as `orients_hotpot_chronicles:<ID>`.
