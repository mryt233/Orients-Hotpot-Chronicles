<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-10-30：同步正式版 #30–#71 和未发布 #72–#82；保留原译者署名。 -->
# Dipping Sauces

A good dipping sauce completes a hotpot meal. The mod uses a **self-service sauce station**: nine small bowls let you **mix your own sauce**. Each seasoning has its own effect, and **all effects stack**.

{{icons:oak_sauce_station,iron_sauce_station,dipping_sauce}}

## Sauce stations {#station}{{since:0.1.2-alpha}}

- **Appearance:** three ascending tiers, low at the front and high at the back, with three white porcelain bowls per tier: nine in total.
- **Height:** 1.5 blocks tall, occupying **two blocks**, including an invisible block above. You can **walk up the tiers**.
- **Materials:** 11 wood types plus iron, for 12 variants.
- **Facing:** the front faces the player who placed it.

### Recipes

Each recipe makes one station. Replace planks with iron ingots for iron, or use the corresponding planks for other woods.

{{recipe:oak_sauce_station,iron_sauce_station}}

### Restocking the bowls {#restock}

**Right-click the station with an empty hand** to open its restocking screen:

- **Nine slots** correspond to the nine bowls on the station (there are **15 seasonings**, with room for up to 9 kinds at once).
- **Each slot accepts one ingredient type.** The ingredient placed in an empty slot determines its seasoning. Multiple slots can hold the same ingredient.
- **One ingredient = four servings** of seasoning. A slot holds up to 16 ingredients, or 64 servings.
- The **?** in the top-right opens an **ingredient reference**, showing which ingredient makes which seasoning.
- Stocked bowls visibly show their seasoning on the station.

:::tip
**Development preview: the ingredient list now uses a single paginated column.** Clicking **?** opens a narrow panel matching the main interface width, displaying names and effects in full with text wrapping; turn pages using the bottom arrows or the mouse wheel over the panel, and reopening it remembers your last page. The main interface and ingredient list are centered together, and installed JEI / REI will avoid this area.<!--清单#140-->
:::

## 15 seasonings {#seasoning}

{{icons:oyster_sauce,sesame_paste,crushed_peanuts,millet_chili,peanut,minced_zhe_er_gen}}

| Seasoning | Ingredient | Effect when eaten |
| --- | --- | --- |
| MSG | {{i:minecraft:wheat}} | Restores two extra hunger points |
| Chili Oil | {{i:minecraft:blaze_powder}} | Fire Resistance for 10 seconds |
| Sweet and Sour Sauce | {{i:minecraft:sweet_berries}} | Speed for 10 seconds |
| Honey | {{i:minecraft:honey_bottle}} | Clears spice-induced Hunger and Poison |
| Sesame Oil | {{i:minecraft:sunflower}} or {{i:minecraft:pumpkin_seeds}} | Regeneration for five seconds |
| Vinegar | {{i:minecraft:apple}} or {{i:minecraft:beetroot}} | Clears Nausea and spice-induced Hunger |
| Sugar | {{i:minecraft:sugar}} | Haste for 10 seconds |
| Chopped Scallion | {{i:scallion}} | Flavor only; no effect |
| Minced Cilantro | {{i:cilantro}} | Flavor only; no effect |
| Garlic Paste | {{i:garlic_paste}} | Resistance for 8 seconds |
| Oyster Sauce | {{i:oyster_sauce}} | Water Breathing for 10 seconds |
| Sesame Paste | {{i:sesame_paste}} | Absorption I for 10 seconds |
| Crushed Peanuts | {{i:crushed_peanuts}} | Jump Boost I for 10 seconds |
| Chopped Millet Chili | {{i:millet_chili}} | Strength I for 8 seconds |
| Minced Houttuynia | {{i:minced_zhe_er_gen}} | Resistance I for 10 seconds, cures poison |

Since 0.1.4-alpha, garlic drops from grass. Pound it on a cutting board with a rolling pin 3 times to make 2 portions of garlic paste.

Each of the new ingredients has its own little story:

- **Oyster Sauce**: shapelessly craft vanilla **kelp + sugar + glass bottle** for 2 bottles. The kelp you pick up by the sea finally has a purpose — dip in it and you can stay underwater without holding your breath.
- **Sesame Paste**: **peanut + wheat seeds + bowl** makes 2 bowls. One rich spoonful takes the edge off the heat.
- **Crushed Peanuts**: breaking grass has a **15%** chance to drop a **peanut**. Put the peanut on the [cutting board](ingredients.html#board) and **pound it 3 times with a rolling pin** for 2 portions of crushed peanuts (from the second hit the board already shows crumbs).
- **Chopped Millet Chili**: one **chili pepper** cut once on the cutting board (or in a stonecutter) makes 2 portions. Chili peppers drop from grass. See [Ingredients](ingredients.html#chili_pepper).
- **Minced Houttuynia**: breaking grass has a **15%** chance to drop a **houttuynia root**. Put the whole root on the [cutting board](ingredients.html#board) and **pound it 3 times with a rolling pin** for 2 portions of mince. Houttuynia cools and detoxifies, so this dip cures poison and adds some resistance. See [Ingredients](ingredients.html#zhe_er_gen).

{{recipe:oyster_sauce,sesame_paste}}

“Cooling the spice” means clearing the Hunger effect from Spicy Red Broth. **Every seasoning in your bowl contributes its effect when you eat dipped food.**

## Placing sesame paste and oyster sauce {#bottle}

{{icons:sesame_paste,oyster_sauce}}

Besides being used to mix sauces at the station, sesame paste and oyster sauce can also be **placed on the table as a decoration**.

1. **Place it**: hold sesame paste or oyster sauce and **Shift + right-click** an empty spot on a hotpot table or tall hotpot table. Like chopsticks and sauce bowls it takes one plate slot, and several things can share that slot. It appears as a small 3D bottle matching its icon: **sesame paste** is a short labelled glass jar, **oyster sauce** a slim dark bottle.
2. **Pick it back up**: **right-click it empty-handed** to put it back in your inventory; mining the bottle takes only that bottle. Mining the table drops every piece of tableware in that slot.
3. **Decoration only**: a bottle on the table **cannot be dipped into and cannot season food**. To add this seasoning to a dish, go to the [sauce station](sauce.html#make) and mix a bowl as before (or dip at a sauce bowl on the table with chopsticks).
4. The bottle can still be eaten as before (same hunger value); placing it is just for looks.

:::tip
Fill a slot if it fits: sesame paste, oyster sauce, sauce bowls and plates can share one slot — a row of bottles makes the table look like a real hotpot restaurant.
:::


## Mixing a sauce {#make}

1. **Hold a vanilla bowl** ({{i:minecraft:bowl}}) and **right-click a stocked seasoning bowl** on the station. It takes one serving and becomes **Dipping Sauce**.
2. Want another seasoning? **Hold that sauce bowl and right-click a different seasoning bowl** to add it.
3. Rules:
   - **Each seasoning can be added only once per bowl.**
   - One sauce bowl lasts for **nine dips**.
   - After using some dips, you can still add **seasonings you have not added yet**. **The remaining dip count stays the same.**
   - An empty station bowl shows an “empty” message; restock it first.
4. The tooltip lists the seasonings and remaining dips.

:::tip
Try **sesame oil + vinegar + chopped scallions + chopped cilantro** for Regeneration, relief from spice and Nausea, and a fresh onion flavor. Pair Spicy Red Broth with **honey** or **vinegar** to clear its spice effect.
:::

## Dipping food {#dip}

There are three ways:

### ① Dip at the table

1. **Place your sauce bowl on a table** by right-clicking the tabletop, just like placing a plate.
2. **Hold food with bamboo chopsticks** and right-click the sauce bowl on the table to take a dip (clicking the sauce bowl with empty chopsticks will prompt you to pick up food first).
3. The food now carries the sauce's effects; hover over it to see **“Dipped: …”**. After taking the last dip, the sauce bowl turns back into an empty bowl.

### ② Automatic dipping at the pot

Hold dipping sauce in your **main hand or off hand** (either hand works); eating with chopsticks in the hotpot screen will **automatically dip each bite**, without taking it out first. Eating food held by chopsticks while aiming into the air (see “③” below) also automatically dips if you are holding sauce in hand.

### ③ Eat directly with chopsticks

Hold dipped food with chopsticks and **hold right-click while aiming into the air** to eat; the sauce's effects still apply. **However you eat it**—in the screen, with chopsticks, or from the table—dipped food keeps its effects.

Each dip uses one serving from the sauce bowl; after nine dips, the dipping sauce becomes an **empty bowl**.
