<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：按 0.3.1-alpha 正式版统一英文作物获取与小石磨干磨说明。 -->
# Dipping Sauces

A great dipping sauce completes hotpot. Use the **self-service sauce station**: 9 porcelain bowls let you **mix any combination** — every seasoning's buff **stacks together**.

{{icons:oak_sauce_station,iron_sauce_station,dipping_sauce}}

## Sauce stations {#station}{{since:0.1.2-alpha}}

- **Appearance**: three ascending stepped tiers holding 3 porcelain bowls each (9 total).
- **Height**: 1.5 blocks tall, occupying **2 blocks** cleanly (step right up onto each tier).
- **Materials**: 11 wood styles plus iron (12 variants total).
- **Facing**: faces the player who placed it.

### Recipes

Crafts 1 station per recipe. Swap planks with iron ingots for iron, or use matching wood planks.

{{recipe:oak_sauce_station,iron_sauce_station}}

### Restocking the bowls {#restock}

**Right-click the sauce station with an empty hand** to open its restocking screen:

- **9 slots** map to the 9 bowls on the station (**15 seasonings** exist in total; stock up to 9 kinds at once).
- **Each slot holds one ingredient type** (multiple slots can repeat the same seasoning).
- **1 ingredient = 4 servings** of seasoning, up to 16 ingredients (64 servings) per slot.
- Click top-right **?** for the **ingredient reference** sheet.
- Stocked bowls show matching spices directly in the station model.

:::tip
**{{since:0.3.0-alpha}}The ingredient reference uses a single paginated column.** Clicking **?** opens a narrow panel matching the main GUI width with wrapped names and effects; page via bottom arrows or mouse wheel, and reopening remembers your last page. JEI and REI automatically keep clear of the combined panel area.<!--清单#140-->
:::


## 15 seasonings {#seasoning}

{{icons:oyster_sauce,sesame_paste,crushed_peanuts,millet_chili,peanut,minced_zhe_er_gen}}

| Seasoning | Ingredient | Effect when eaten |
| --- | --- | --- |
| MSG | {{i:minecraft:wheat}} | Restores 2 extra hunger |
| Chili Oil | {{i:minecraft:blaze_powder}} | Fire Resistance for 10 s |
| Sweet and Sour Sauce | {{i:minecraft:sweet_berries}} | Speed for 10 s |
| Honey | {{i:minecraft:honey_bottle}} | Clears spice Hunger and Poison |
| Sesame Oil | {{i:minecraft:sunflower}} or {{i:minecraft:pumpkin_seeds}} | Regeneration for 5 s |
| Vinegar | {{i:minecraft:apple}} or {{i:minecraft:beetroot}} | Clears Nausea and spice Hunger |
| Sugar | {{i:minecraft:sugar}} | Haste for 10 s |
| Chopped Scallion | {{i:scallion}} | Flavor only, no effect |
| Minced Cilantro | {{i:cilantro}} | Flavor only, no effect |
| Garlic Paste | {{i:garlic_paste}} | Resistance for 8 s |
| Oyster Sauce | {{i:oyster_sauce}} | Water Breathing for 10 s |
| Sesame Paste | {{i:sesame_paste}} | Absorption I for 10 s |
| Crushed Peanuts | {{i:crushed_peanuts}} | Jump Boost I for 10 s |
| Chopped Millet Chili | {{i:millet_chili}} | Strength I for 8 s |
| Minced Houttuynia | {{i:minced_zhe_er_gen}} | Resistance I for 10 s, cures Poison |

Garlic, peanuts, chili peppers, and houttuynia can be [grown directly on farmland or gathered from wild plants](ingredients.html#crops_trees) (grass retains a 2% fallback under Crops Replace Grass, or 15% in Simple Mode). Make **garlic paste** by pounding garlic 3 times with a rolling pin or **dry-grinding 3 turns on the [Stone Mill](ingredients.html#tofu)** for 2 portions; the last five are prepared as follows:

- **Oyster Sauce**: shapelessly craft vanilla **kelp + sugar + glass bottle** for 2 bottles — great for underwater foraging.
- **Sesame Paste**: **peanut + wheat seeds + bowl** crafts 2 bowls of rich soothing paste.
- **Crushed Peanuts**: pound a **peanut** 3 times on a [cutting board](ingredients.html#board) with a rolling pin or **dry-grind 3 turns on the Stone Mill** for 2 portions.
- **Chopped Millet Chili**: slice a **chili pepper** once on a cutting board / stonecutter or **dry-grind 2 turns on the Stone Mill** for 2 portions (see [Ingredients & Cutting](ingredients.html#chili_pepper)).
- **Minced Houttuynia**: pound a whole **houttuynia root** 3 times on a [cutting board](ingredients.html#board) for 2 portions of detoxifying spice (see [Ingredients & Cutting](ingredients.html#zhe_er_gen)).

{{recipe:oyster_sauce,sesame_paste}}

“Cools spice” removes Hunger triggered by Spicy Red Broth. **Dipped food carries every seasoning's effect from that bowl simultaneously.**

## Placing sesame paste & oyster sauce {#bottle}

{{icons:sesame_paste,oyster_sauce}}

Beyond seasoning bowls, sesame paste jars and oyster sauce bottles can sit **on tables as decor**:

1. **Place**: hold either bottle and **Shift + right-click** an empty spot on a low/tall hotpot table — shares plate slots with bowls and chopsticks (**sesame paste** is a short white-labeled jar; **oyster sauce** is a tall dark bottle).
2. **Pick up**: **empty-hand right-click** returns the bottle; mining takes only that bottle. Breaking the table drops all tableware in that cell.
3. **Purely decorative**: table bottles cannot season or dip food directly — mix sauces at the [sauce station](sauce.html#make) or dip at a placed sauce bowl.
4. Bottles remain edible directly as food too.

:::tip
Mix bottles, sauce bowls, chopsticks, and plates in one grid space to build an authentic hotpot tabletop!
:::


## Mixing a sauce {#make}

1. **Hold an empty bowl** ({{i:minecraft:bowl}}) and **right-click a stocked seasoning bowl** on the station to create **Dipping Sauce**.
2. **Hold that dipping sauce and right-click other stocked bowls** to layer more seasonings.
3. Rules:
   - **Each seasoning adds once per bowl.**
   - One bowl holds **9 dips**.
   - Mid-use bowls can still accept new unadded seasonings **without changing remaining dips**.
   - Clicking an empty station slot prompts you to restock.
4. Tooltips list every added seasoning and remaining dips.

:::tip
Classic combo: **Sesame Oil + Vinegar + Chopped Scallion + Minced Cilantro** for healing, clearing spice/Nausea, and aroma; pair Spicy Red Broth with **Honey** or **Vinegar**.
:::

## Dipping food {#dip}

Three easy ways to dip:

### ① Dip at the table

1. **Set your dipping sauce on a table** (right-click tabletop like a plate).
2. Right-click that sauce bowl while **holding food with chopsticks** (empty chopsticks warn you first).
3. Dipped food displays **“Dipped: …”**; the final dip leaves an empty bowl.

### ② Auto-dip while holding sauce

Hold dipping sauce in your **main or off hand**: eating food via chopsticks in the hotpot screen — or eating chopstick-held food in midair — **automatically dips each bite**.

### ③ Eat dipped food directly

Hold dipped food on chopsticks and **hold right-click in midair** to eat — effects apply regardless of how you consume dipped bites.

Each dip spends 1 charge out of 9 before returning an **empty bowl**.
