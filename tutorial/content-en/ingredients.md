<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：按 0.3.1-alpha 正式版整理英文食材、切配、石磨木桶与果树作物章节，并接入指南模式折叠。 -->
# Ingredients & Cutting

Food is the heart of hotpot. This page explains **where ingredients come from**, **how to prepare them**, and **how much hunger they restore**.

{{icons:tripe,fatty_beef_roll,mutton_roll,beef_loin_slice,potato_slice,cold_tofu_cube,beef_meatball,fish_ball,chicken_feet,scallion,cilantro,soybean}}

:::tip
**Every raw ingredient cooks in the hotpot** as well as in furnaces, smokers, and campfires — hunger values are listed below. **Cooked food cannot go back into the pot.**
:::

## Tripe {#tripe}

{{icons:tripe,cooked_tripe}}

- **Source**: dropped by cows and mooshrooms using vanilla beef rules (1–3 pieces, +0–1 per Looting level; killed by fire drops cooked tripe).
- **Raw tripe** restores 2 hunger; **cooked tripe** restores 5.
- Great for quick dipping: finishes in ~2.5 seconds (twice as fast as whole beef).

{{recipe:cooked_tripe}}

## Beef loin & loin slices {#loin}{{since:0.1.3-alpha-hotfix1}}

{{icons:beef_loin,cooked_beef_loin,beef_loin_slice,cooked_beef_loin_slice}}

- **Beef loin**: cows and mooshrooms have a **25%** chance to drop 1 bonus loin (+10% per Looting level; fire kills drop cooked beef loin). Restores 3 raw or 8 cooked (matching vanilla steak).
- **Beef loin slices**: slice beef loin on a [cutting board](#board) or stonecutter for **3 slices**. Restores 1 raw or 3 cooked.
- Beef loin slices cook **fastest of all meats** (half the time of fatty beef rolls).
- Cooked beef loin cannot be sliced.

{{recipe:beef_loin_slice,cooked_beef_loin}}

## Cutting board, knife & rolling pin {#board}{{since:0.1.2-alpha}}

Prepare these three tools before prepping ingredients:

### Cutting board

<!--guide-collapse-->
:::tip
Clean **placement facing**, **hand rendering**, **mining only the board's placed food**, and **bright inventory icons** were refined starting in 0.1.3-alpha-hotfix1.
:::
<!--/guide-collapse-->

{{recipe:cutting_board}}

- **Any 2 logs horizontally** craft **2 cutting boards**.
- Placed items rotate toward your facing direction.
- **Right-click with sliceable or poundable food** to place; **empty-hand right-click** picks back up.
- Mining the food drops only the food while leaving the board intact.

### Iron kitchen knife

{{recipe:iron_kitchen_knife}}

Square-tipped Chinese cleaver (250 durability, **also deals 4 attack damage**).

:::tip
**Locked texture orientation**{{since:0.2.2-alpha}}: handle bottom-left ↙, tip top-right ↗, blade edge bottom-right ↘ across inventory, hand, guidebook, web guide, and viewer alike.
:::

### Rolling pin

{{recipe:rolling_pin}}

120 durability; used on the cutting board to **pound meatballs, shrimp paste**, garlic paste, crushed peanuts, and minced houttuynia.

## Cutting {#cut}{{since:0.1.2-alpha}}

Place food on a cutting board and **right-click with an iron kitchen knife** (or use a **stonecutter** in one step):

| On the board | Cuts | Output | Cooked / output hunger |
| --- | --- | --- | --- |
| {{i:minecraft:beef}} | 1 cut | 3 × {{i:fatty_beef_roll}} | Cooked 3 |
| {{i:minecraft:mutton}} | 1 cut | 3 × {{i:mutton_roll}} | Cooked 3 |
| {{i:beef_loin}} | 1 cut | 3 slices {{i:beef_loin_slice}} | Cooked 3 |
| {{i:minecraft:potato}} | 3 cuts (1 per cut) | 3 portions {{i:potato_slice}} (3 slices each) | Cooked 2 |
| {{i:minecraft:porkchop}} | 1 cut | 1 × {{i:minced_pork}} | Intermediate ingredient |
| {{i:cold_tofu}} | 1 cut | 3 cubes {{i:cold_tofu_cube}} | Cooked 2 |
| {{i:hot_tofu}} | 1 cut | 3 cubes {{i:hot_tofu_cube}} (already cooked) | 2 |
| {{i:shrimp}} | 1 cut | 1 × {{i:peeled_shrimp}} | Cooked 3 |
| {{i:ham_sausage}} | 1 cut | 3 slices {{i:ham_sausage_slice}} | Reheated 2 |
| {{i:chili_pepper}} | 1 cut | 2 × {{i:millet_chili}} | Raw 1 (seasoning) |
| {{i:lotus_root}} | 3 cuts (1 per cut) | 3 slices {{i:lotus_root_slice}} | Cooked 3 |
| {{i:raw_squid}} | 1 cut | 3 portions {{i:squid_roll}} | Cooked 4 |
| {{i:zhe_er_gen}} | 1 cut | 3 pieces {{i:zhe_er_gen_slice}} | Cooked 3 |
| {{i:chicken_feet}} | 1 cut | 1 × {{i:boneless_chicken_feet}} | Cooked 4 |

{{recipe:fatty_beef_roll,mutton_roll,potato_slice,cold_tofu_cube}}

## Pounding meatballs {#meatball}{{since:0.1.2-alpha}}

**Poundable items**: raw beef, porkchop, mutton, cod, salmon, garlic, peeled shrimp, peanuts, and houttuynia root.

1. Place the raw ingredient on the cutting board.
2. **Right-click three times with a rolling pin**: 1st hit tenderizes, 2nd turns into paste, 3rd pops out the result.
3. 3D spherical meatballs and fish balls each restore **3** hunger cooked; garlic → **2 garlic paste**, peeled shrimp → **1 shrimp paste**, peanut → **2 crushed peanuts**, houttuynia → **2 minced houttuynia**.

| Ingredient | Raw balls / pounded output |
| --- | --- |
| Raw beef | {{i:beef_meatball}} ×3 |
| Raw porkchop | {{i:pork_meatball}} ×3 |
| Raw mutton | {{i:mutton_meatball}} ×3 |
| Raw cod / salmon | {{i:fish_ball}} ×3 |
| {{i:garlic}} / {{i:peeled_shrimp}} | {{i:garlic_paste}} ×2 / {{i:shrimp_paste}} ×1 |
| {{i:peanut}} / {{i:zhe_er_gen}} | {{i:crushed_peanuts}} ×2 / {{i:minced_zhe_er_gen}} ×2 |

### Cilantro meatballs

Combine 2 raw meatballs + 1 {{i:cilantro}} shapelessly for **2** cilantro meatballs (available for all four meat/fish balls). Cooked ones restore **4** hunger each.

{{recipe:cilantro_beef_meatball}}

## Shrimp, peeled shrimp & shrimp paste {#shrimp}{{since:0.1.4-alpha}}

{{icons:shrimp,peeled_shrimp,shrimp_paste}}

- **Shrimp**: caught by fishing (~1/6 weight).
- **Peeled shrimp**: slice shrimp on a cutting board or stonecutter.
- **Shrimp paste**: pound peeled shrimp 3 times with a rolling pin.
- All three restore **1** raw, or **3 / 3 / 4** cooked (cooked shrimp & peeled shrimp carry fun tooltip lines).

{{recipe:peeled_shrimp,cooked_shrimp_paste}}

<!-- mryt233 改，2026-11-02：0.2.0-alpha 发布，火腿肠链并入正文。 -->
## Minced pork & ham sausage {#sausage}{{since:0.2.0-alpha}}

{{icons:minced_pork,raw_ham_sausage,ham_sausage,ham_sausage_slice,cooked_ham_sausage_slice}}

1. Cut **raw porkchop** once on a cutting board or stonecutter for **1 minced pork** (pounding porkchop still makes pork meatballs).
2. Shapelessly craft **minced pork + 1 wheat** for **3 raw ham sausages**.
3. Cook in a pot, furnace, smoker, or campfire to obtain **ham sausage**.
4. Slice **ham sausage** once on a cutting board or stonecutter for **3 ham sausage slices** (raw sausages cannot be sliced).
5. Cook **ham sausage slices** once more into **cooked ham sausage slices** (cannot go back into the pot afterwards).

{{recipe:minced_pork,raw_ham_sausage,ham_sausage,ham_sausage_slice,cooked_ham_sausage_slice}}

:::tip
Chain order: **Raw Porkchop → Minced Pork → Raw Ham Sausage → Ham Sausage → Ham Sausage Slice → Cooked Ham Sausage Slice**. Minced pork is an intermediate ingredient and cannot be eaten directly.
:::

## Soybeans & tofu {#tofu}{{since:0.1.2-alpha}}

- **Soybeans**: [grow directly on farmland or harvest wild soybean clusters](#crops_trees), with a fallback grass drop (2% default; restores 15% in Simple Mode).
<!--guide-flag:mode.detailed_tofu_processing-->
- **Full Mode (default) 3-step traditional tofu workflow**{{since:0.3.1-alpha}}:
  1. **Wet-grind soymilk on a {{i:stone_mill}}**: crafted with vanilla cobblestone and uniform upper/lower shading under shaders, standing ~**1.5 blocks tall** so its front spout aligns with the rim of an adjacent open vat. Load `2` soybeans and pour a water bucket (1 bucket yields `3` wet grinds by default), then empty-hand right-click `4` full turns to grind soymilk — if the spout faces an **empty {{i:wooden_vat}}**, finished soymilk **automatically pours over the rim into the vat** (standalone mills collect soymilk into the annular trough for scooping into {{i:soymilk_bucket}}, which can also be drunk for 2 hunger); you can also **dry-grind** crushed peanuts (3 turns → 2), garlic paste (3 turns → 2), and chopped millet chili (2 turns → 2).
  2. **Coagulate & press in an {{i:wooden_vat}}**: once the vat holds soymilk, add {{i:brine_bottle}} (kelp or dried kelp + water bucket + glass bottle) and cover with a 3D {{i:wooden_press_lid}}; lid and liquid surface smoothly lower together over **4 seconds** to press **2 cold tofu** (empty hand retrieves lid and tofu; emptying the vat immediately triggers waiting soymilk from an adjacent mill to pour across).
  3. **Mode compatibility**: switching to **Simple Mode** or turning off "Detailed Tofu Processing" (`mode.detailed_tofu_processing`) re-enables direct crafting-grid cold tofu from `2–3` soybeans + water bucket while keeping the mill and vat usable.
<!--/guide-flag-->
<!--guide-fallback:tofu_simple-->
- Slice cold tofu once on a cutting board for 3 **cold tofu cubes** (cook for 2 hunger each), or cut cold tofu in a stonecutter into **2 dried tofu**.

{{recipe:cold_tofu,soymilk_bucket,stone_mill,wooden_vat,wooden_press_lid,brine_bottle}}

:::tip
**Cold tofu vs hot tofu**{{since:0.2.2-alpha}}:

{{icons:cold_tofu,hot_tofu,hot_tofu_cube}}

- Simmering whole **cold tofu** for **10 seconds** cooks it into **hot tofu** (already cooked — scoop with hand/chopsticks; cannot go back in).
- Leaving hot tofu simmering for **30 seconds** total **breaks it into hot tofu cubes** with equal count — fish out whole tofu early if you want whole blocks!
- Hot tofu also cuts into 3 **hot tofu cubes**; cold tofu cubes cook into hot cubes too.
- Server admins can adjust `cooking.tofu_cook_ticks` (default 200 ticks = 10 s) and `cooking.tofu_overcook_ticks` (default 600 ticks = 30 s) followed by `/orientshotpot reload`.
:::

## Crispy pork {#crispy_pork}{{since:0.2.2-alpha}}

{{icons:raw_crispy_pork,crispy_pork,boiled_crispy_pork}}

Crisp golden bites that always disappear off the table first:

1. Combine **minced pork + wheat + 1 egg** shapelessly for **3 raw crispy pork** (1 hunger; seasoned raw batter — best cooked first).
2. Cook in a **furnace (10 s) / smoker (5 s) / campfire (30 s)** or **hotpot** for **crispy pork** (**4 hunger** — crunchy straight off the tray).
3. Simmer crispy pork **one more time** in the pot (~**7.5 seconds** on Coal + Clear Broth) for **boiled crispy pork** (**6 hunger** — juicy inside, tender outside).
4. Boiled crispy pork is the final stage and cannot be simmered again.

{{recipe:raw_crispy_pork,crispy_pork}}

:::tip
Order: **Raw Porkchop → Minced Pork → Raw Crispy Pork → Crispy Pork → Boiled Crispy Pork**. Fry half to snack crisp and simmer the rest in broth!
:::

## Tofu products & noodles {#tofu_products_noodles}{{since:0.2.2-alpha}}

{{icons:dried_tofu,cooked_dried_tofu,tofu_skin,cooked_tofu_skin,wide_noodles,cooked_wide_noodles,sweet_potato_skin,cooked_sweet_potato_skin}}

Arrange cold tofu or potato slices horizontally or vertically to craft four broth-soaking favorites:

| Make | Pattern | Output | Raw hunger | Cooked hunger |
| --- | --- | --- | --- | --- |
| {{i:dried_tofu}} | 2 × **Cold Tofu side-by-side** (horizontal) | 3 | 2 | 4 |
| {{i:tofu_skin}} | 2 × **Cold Tofu stacked** (vertical) | 3 | 2 | 4 |
| {{i:wide_noodles}} | 2 × **Potato Slices side-by-side** (horizontal) | 3 | 1 | 4 |
| {{i:sweet_potato_skin}} | 2 × **Potato Slices stacked** (vertical) | 3 | 1 | 4 |

- **Dried tofu** stays chewy and nutty; **tofu skin** softens in seconds and traps broth inside its folds.
- **Wide noodles** are famously slippery with chopsticks; **sweet potato skin** brings gentle starch sweetness.
- All four cook in ~**5–7.5 seconds** (tofu skin & sweet potato skin 5 s; dried tofu & wide noodles 7.5 s), or via furnace / smoker / campfire.

{{recipe:dried_tofu,tofu_skin,wide_noodles,sweet_potato_skin,cooked_dried_tofu,cooked_tofu_skin,cooked_wide_noodles,cooked_sweet_potato_skin}}

:::tip
Orientation decides the product: horizontal cold tofu = **dried tofu**, vertical = **tofu skin**; horizontal potato slices = **wide noodles**, vertical = **sweet potato skin**.
:::

## Mushrooms, greens & seafood {#veggies_seafood}{{since:0.2.2-alpha}}

{{icons:enoki_mushroom,cooked_enoki_mushroom,greens,cooked_greens,lotus_root,lotus_root_slice,cooked_lotus_root_slice,raw_squid,squid_roll,cooked_squid_roll}}

- **Enoki mushroom**: craft from 2 brown mushrooms, or get 50% drops from breaking small **brown/red mushrooms**. Cooks in **2.5 seconds** (**3 hunger** cooked) — long bundles built to soak up broth.
- **Greens**: [farm on farmland or gather wild greens](#crops_trees) (2% sparse grass fallback; 15% in Simple Mode). Cooks whole in **2.5 seconds** (**3 hunger**) for the freshest bite at a spicy table.
- **Lotus root**: **50%** drop from breaking **lily pads** (shears drop lily pads instead). Slice whole roots on a [cutting board](#board) (**3 cuts, 1 slice each**) or stonecutter (**3 slices** at once). Slices cook in **5 seconds** (**3 hunger**) with crisp holes full of broth.
- **Raw squid**: dropped by squids and glow squids (1–2, +0–1 per Looting). Whole squid must be scored once into **3 squid rolls** before cooking (**5 seconds**, **4 hunger** cooked), curling up neatly in hot broth.
- All cook identically in furnaces (10 s), smokers (5 s), and campfires (30 s).

{{recipe:enoki_mushroom,lotus_root_slice,squid_roll,cooked_enoki_mushroom,cooked_greens,cooked_lotus_root_slice,cooked_squid_roll}}

:::tip
Enoki and greens tie with beef loin slices and tripe for **fastest cooking**; lotus slices and squid rolls take medium time; whole lotus root and whole squid prompt you to slice them first.
:::

## Egg fried rice {#egg_fried_rice}{{since:0.2.2-alpha}}

{{icons:egg_fried_rice}}

1. Shapelessly craft **1 wheat + 1 egg + 1 scallion + 1 bowl** for **1 Egg Fried Rice**.
2. Restores **7 hunger** — fluffy gold-and-white grains flecked with fresh green scallion, ideal for soothing spice.
3. Egg fried rice is **ready to eat** and does not go in the pot; the bowl is part of the meal.

{{recipe:egg_fried_rice}}

## Chili pepper {#chili_pepper}{{since:0.2.2-alpha}}

{{icons:chili_pepper,millet_chili}}

1. **Source**: [grow on farmland or harvest wild chili peppers in plains/jungles](#crops_trees), or break **grass** (2% sparse fallback; 15% in Simple Mode).
2. Eat raw for 1 hunger, or cut once on a [cutting board](#board) / stonecutter / dry-grind **2 turns on the Stone Mill** to get **2 Chopped Millet Chili**.
3. **Chopped Millet Chili** is the 14th seasoning at the [Sauce Station](sauce.html#seasoning), granting **Strength I for 8 s**.
4. Most importantly, **turn up Spicy Red Broth heat**: hold a chili pepper and **right-click the red broth area** up to Level 3 Extreme Spicy; cool down with ice (see [Stoves & Hotpots](hotpot.html#soup)).

{{recipe:millet_chili}}

:::warn
**Chili peppers and millet chili never go into the pot**: right-clicking a pot with them only adjusts red broth heat or prompts you.
:::

## Houttuynia root {#zhe_er_gen}{{since:0.2.2-alpha}}

{{icons:zhe_er_gen,zhe_er_gen_slice,cooked_zhe_er_gen_slice,minced_zhe_er_gen}}

1. **Source**: [grow on farmland or gather wild houttuynia in lush forests / riverbanks](#crops_trees), or break **grass** (2% sparse fallback; 15% in Simple Mode).
2. Crunchy white root edible raw (1 hunger) with an unmistakable botanical punch.
3. Slice once on a [cutting board](#board) or stonecutter into **3 houttuynia slices** (whole roots must be sliced before entering the pot).
4. **Houttuynia slices** simmer **10 seconds (200 ticks)** into **cooked houttuynia slices** (3 hunger), or roast normally.
5. Pound a whole root **3 times with a rolling pin** on the cutting board for **2 minced houttuynia**.
6. **Minced Houttuynia** is the 15th seasoning at the [Sauce Station](sauce.html#seasoning): grants **Resistance I for 10 s** and **cures Poison**.

{{recipe:zhe_er_gen_slice,cooked_zhe_er_gen_slice}}

:::tip
**Mellowed by simmering**: raw houttuynia is bold, whereas cooking for 10 seconds leaves a mild sweet finish.
:::

## Chicken feet & boneless chicken feet {#chicken_feet}{{since:0.3.1-alpha}}

{{icons:chicken_feet,cooked_chicken_feet,boneless_chicken_feet,cooked_boneless_chicken_feet}}

- **Source**: killing **adult chickens** has a **50%** base chance to drop 1–2 **Raw Chicken Feet** (+10% per Looting level; fire kills drop cooked chicken feet; configurable).
- **Deboning**: slice raw chicken feet once on a [cutting board](#board) or **stonecutter** to yield 1 **Boneless Chicken Feet**.
- **Cooked chicken feet** (7.5 s pot / furnace) restore **3** hunger; **cooked boneless chicken feet** (faster 5 s cook) restore **4** hunger — tender collagen made for dry dipping spices.

{{recipe:boneless_chicken_feet,cooked_chicken_feet,cooked_boneless_chicken_feet}}

## Standalone orange trees & 9 crops {#crops_trees}{{since:0.3.1-alpha}}

{{icons:orange_log,orange_wood,stripped_orange_log,stripped_orange_wood,orange_leaves,orange_sapling}}

- **Standalone orange trees**: naturally spawn in Forests, Flower Forests, Plains, Meadows, and Jungles — vanilla oak and dark oak leaves no longer drop oranges. Right-click **Orange Logs / Wood** with an axe to strip them (burn into charcoal or craft 4 oak planks). Trees grow into compact orchard canopies with **1 exposed trunk block** beneath a **`3×3×2` main crown** (optional **1 top leaf block**) reachable from the ground; **Orange Leaves** mature through green → flowering → unripe citrus → ripe fruit (bonemeal works), yielding **1–2 {{i:orange}}** on right-click. Azalea-style transparent leaf textures obey vanilla Fast / Fancy graphics, allow players and mobs to pass gently through leaves (~**1/5** cobweb drag), drop **Orange Saplings** when broken (recoverable intact via shears / Silk Touch), and wave gently under shaders alongside saplings.
- **9 plantable farmland crops**: {{i:scallion}}, {{i:cilantro}}, {{i:soybean}}, {{i:garlic}}, {{i:tomato}}, {{i:chili_pepper}}, {{i:peanut}}, {{i:greens}}, and {{i:zhe_er_gen}} **require no separate seeds** — recolored into vanilla moss/olive-green palettes with clear `stage 2 → stage 3` mature silhouettes (umbels, exposed peanut pods, garlic bulbs, leafy bok choy heads), simply right-click farmland with the crop itself (bonemeal supported; drops 2–4 upon maturity; growth rate & bonemeal toggled in Config under "Crops & Fruit Trees").
- **Wild plants & grass fallback**: **wild** clusters spawn naturally across plains/forest/savanna/jungle/riverbank biomes with vanilla small-flower tags for grass blending and shader waving, dropping 1–2 starter crops (shears / Silk Touch collect the decorative bush). With "Crops Replace Grass Drops" active (default on), breaking grass acts only as a sparse fallback (**2%** each); disabling it or switching to Simple Mode restores legacy grass drop rates (15%–25%). Enoki (mushrooms) and lotus root (lily pads) keep their original gathering methods.

<!--guide-fallback:crops_grass_fallback-->

## Scallions & cilantro {#garnish}{{since:0.1.2-alpha}}

- **Source**: scallions, cilantro, garlic, tomatoes, and soybeans now grow via [farmland farming & wild clusters](#crops_trees), keeping a 2% grass fallback (25% / 15% restored in Simple Mode).
- Both are edible raw (1 hunger), used as [Sauce Station](sauce.html) seasonings, and cilantro crafts cilantro meatballs.
- Grass, lily pad, mushroom, cow, and chicken drop chances can all be configured in [Configurable values](faq.html#config) under "Loot Drop Chances" (including the standalone `tomato` key; run `/reload` after editing).
- **Cooked kelp**: boil vanilla kelp in the hotpot for **cooked kelp** (furnaces/smokers/campfires still make dried kelp), restoring 2 hunger.

## Hunger restoration reference

| Food | Raw | Cooked |
| --- | --- | --- |
| Tripe | 2 | 5 |
| Beef loin | 3 | 8 |
| Beef loin slice | 1 | 3 |
| Fatty beef roll / Mutton roll | 1 | 3 |
| Potato slice | 1 | 2 |
| Tofu (cold / hot) / Cubes (cold / hot) | 2 / 1 | 2 / 2 |
| Soymilk bucket | 2 (returns empty bucket) | — |
| Beef / Pork / Mutton / Fish meatball | 1 | 3 |
| Cilantro meatballs (4 variants) | 1 | 4 |
| Soybean / Garlic / Garlic paste | 1 / 1 / 1 | — |
| Scallion / Cilantro / Tomato / Orange | 1 / 1 / 2 / 4 | — |
| Shrimp / Peeled shrimp / Shrimp paste | 1 / 1 / 1 | 3 / 3 / 4 |
| Cooked kelp | — | 2 |
| Raw ham sausage / Ham sausage | 1 | 3 |
| Ham sausage slice / Cooked slice | 1 | 2 |
| Raw / Crispy / Boiled crispy pork | 1 | 4 / 6 |
| Dried tofu / Cooked dried tofu | 2 | 4 |
| Tofu skin / Cooked tofu skin | 2 | 4 |
| Wide noodles / Cooked wide noodles | 1 | 4 |
| Sweet potato skin / Cooked skin | 1 | 4 |
| Enoki mushroom / Cooked enoki | 1 | 3 |
| Greens / Cooked greens | 1 | 3 |
| Lotus root / Slice / Cooked slice | 2 / 1 | 3 |
| Raw squid / Squid roll / Cooked roll | 2 / 1 | 4 |
| Egg fried rice | — | 7 |
| Chili pepper / Millet chili | 1 | — |
| Peanut / Crushed peanuts | 1 | — |
| Oyster sauce / Sesame paste | 1 / 2 | — |
| Houttuynia / Slice / Cooked slice | 1 / 1 | 3 |
| Minced houttuynia | 1 | — |
| Chicken feet / Cooked chicken feet | 1 | 3 |
| Boneless / Cooked boneless chicken feet | 1 | 4 |

:::tip
Unsure how long something cooks? Remember four tiers: **beef loin slices, tripe, enoki, greens** finish fastest; **rolls, potato slices, tofu cubes, sausage slices, peeled shrimp, lotus slices, squid rolls, tofu skin, sweet potato skin, boneless chicken feet** cook quickly; **dried tofu, wide noodles, meatballs, crispy pork, chicken feet** are medium; and **whole beef loin and houttuynia slices** take longest. Exact seconds are in [Recipe Collection](recipes.html) under "Cooking" and "Pot-Only Boiling".
:::
