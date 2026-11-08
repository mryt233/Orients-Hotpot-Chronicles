<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-10-30：同步正式版 #30–#71 和未发布 #72–#82；保留原译者署名。 -->
# Stoves & Hotpots

Hotpot has three essentials: a **stove** for heat, a **pot** for broth and ingredients, and a **broth base** that affects cooking speed. Here is how they all work.

## Stoves

{{icons:brick_stove,tall_brick_stove}}

### Short and tall stoves

- {{i:brick_stove}}: the short version. Its brick surface is level with a low hotpot table.
{{recipe:brick_stove}}
- {{i:tall_brick_stove}}: raises the firebox and pot by 10 pixels, to match a **tall hotpot table**. Its brick surface sits two pixels below the tabletop, while the pot rim extends three pixels above it for easy access.
{{recipe:tall_brick_stove}}

A tall stove also occupies the block above it with an invisible block. **Leave that space empty when placing one.**

{{img:stoves-boiling.webp|Pots on short and tall stoves, with lit fires and boiling broth}}

### Fuel and heat {#fuel}

Right-click the stove's brick body (or the stove itself when no pot is attached) to open the **firebox screen** and add fuel. **Any vanilla furnace fuel works**, with the same burn time as a furnace. There are three heat levels:

| Fuel | Heat | Burn time (same as a furnace) |
| --- | --- | --- |
| Wood items (planks, logs, sticks, wooden items, etc.) | ×0.65 | For example, planks: 15 seconds |
| Coal or charcoal | ×1.0 | 80 seconds |
| Lava bucket | ×1.6 | 1000 seconds |

:::tip
**Fuel is consumed only while raw ingredients need cooking.**
If the pot has broth and the firebox has fuel but there is no raw food, the fire stays lit and the broth keeps boiling **without consuming fuel**. Take your time setting the table; you will not waste coal. A pot without broth will not light the stove.
:::

The firebox screen has no temperature bar or remaining-time counter. The size of the flame shows how strongly it is burning.

## Pots {#pot}

### Types

| Pot | Description |
| --- | --- |
| {{i:iron_nine_grid_hotpot}} / {{i:copper_nine_grid_hotpot}} | Nine-grid pot: one broth across nine large cells. Copper and iron differ only in appearance. |
| Yuanyang and four-grid pots | Available since 0.1.3-alpha-hotfix1; see [Yuanyang and four-grid pots](#yuanyang) below. |

Choose copper or iron, then select a pot shape below:

{{potviewer}}

{{recipe:iron_nine_grid_hotpot,copper_nine_grid_hotpot}}

A pot is a separate block. It can sit **on a stove**, **on the ground**, or **on a lit campfire** (see below).

### Capacity

A nine-grid pot has nine large cells, each containing a 3×3 set of smaller slots: **up to 81 portions per pot**. Ingredients go into the large cell under your crosshair. If that cell is full, you cannot add more there.

### Broth bases {#soup}

| Add this | Broth | Cooking speed | Notes |
| --- | --- | --- | --- |
| {{i:minecraft:water_bucket}} | Clear Broth | ×1.00 | Clear and light |
| {{i:minecraft:milk_bucket}} | Rich Stock | ×0.85 | Milky white; slowest |
| {{i:minecraft:mushroom_stew}} | Mushroom Broth | ×1.15 | Pour in the stew; the bowl is returned |
| {{i:minecraft:lava_bucket}} | Spicy Red Broth | ×1.30 | Bright red; fastest |
| {{i:tomato_soup_bucket}} | Tomato Broth | ×1.10 | Craft a broth bucket from hotpot base + water bucket, or add base to Clear Broth |
| {{i:three_fresh_soup_bucket}} | Three Delicacies Broth | ×1.00 | Same method with Three Delicacies Hotpot Base |

**Hotpot bases:** {{i:tomato_hotpot_base}} uses tomatoes; {{i:three_fresh_hotpot_base}} uses fish, mushroom, carrot, and kelp (see the [Recipe Collection](recipes.html)). Add base to Clear Broth in the pot, or pour in a prepared broth bucket. Added in 0.1.4-alpha.

- Add broth: **hold the bucket or mushroom stew and right-click the pot**. A regular pot holds only one broth at a time; divided pots are different.
- **Remove broth:** **right-click with an empty bucket** to recover its original contents (water returns as a water bucket). Mushroom broth requires a **bowl** and **no food remaining in the pot**; take all food out first.
- **Serve soup to drink:** right-click with a bowl. Cooked food is collected first; if there is none, you get soup. See [Tableware & Drinks](tableware.html#bowl).

### Four heat levels for Spicy Red Broth {#spicy_level}{{since:0.2.2-alpha}}

Spicy Red Broth can be "turned up" — not with chili powder, but by **actually throwing chili peppers into the pot**.

| Level | Effect on food cooked in that pot |
| --- | --- |
| **Mild** (just poured) | Strength I for 6 s, Speed I for 6 s, no Hunger |
| **Medium Spicy** | Strength II for 9 s, Speed I for 9 s |
| **Extra Spicy** | Strength II for 12 s, Speed I for 10 s, Fire Resistance 15 s and Hunger I 6 s (spice opens the appetite and burns it off) |
| **Level 3 Extreme Spicy** | Strength II for 15 s, Speed I for 12 s, Fire Resistance 30 s, Hunger I 8 s; your mouth also catches fire for 2 s afterwards (you have Fire Resistance, so no damage; wearing a cloth apron prevents the flame) |

How to adjust it:

1. **Hold a {{i:chili_pepper}} chili pepper and right-click the red broth area**: the heat goes up one level, a burst of flame rises from the pot, and the action bar reports something like "Broth spiciness increased to: Extra Spicy". At Insane Spicy it only shows a hint and the pepper is not used up.
2. **Ice to the rescue**: plain **Ice** lowers the heat by 1 level, **Packed Ice** by 2, and **Blue Ice** drops it straight back to Mild. White steam and splashes rise from the pot.
3. Clicking **a different broth** with chili or ice shows "Only spicy red broth can be adjusted with chili or ice" and nothing is put into the pot.
4. Yuanyang and four-grid pots **track each side separately**: only the side you click changes. Pouring broth, scooping it, swapping broths or emptying the pot all reset the heat to Mild.

A bowl of {{i:spicy_red_broth_bowl}} scooped from the pot also keeps **the heat level at the moment it was scooped** and gives the matching effects when drunk. Cooked food from the pot shows a line like "Broth: Spicy Red Broth · Extra Spicy" in its tooltip.

:::tip
Play the heat levels against **chili oil**, **honey** and **vinegar**: chili oil adds Fire Resistance, while honey and vinegar clear the Hunger that spicy broth brings. Food dipped in honey or vinegar will not leave you hungry from the heat, and your mouth will not catch fire.
:::

### What the broth adds to your food {#dish_flavor}{{since:0.2.2-alpha}}

Food cooked in a pot **carries the flavour of that broth** — hover over it and the tooltip adds a line like "**Broth: Mushroom Broth**" (spicy red broth also shows the heat level). Eating it grants an extra effect:

| Broth | Extra effect from food cooked in it |
| --- | --- |
| **Clear broth** | Speed I for 15 s (clean and light on your feet) plus 1 tick of Saturation |
| **Rich stock** | Resistance I for 15 s plus Absorption I for 30 s (beef bones and old hen, sturdy and nourishing — **no more Slowness**) |
| **Mushroom broth** | Regeneration I for 10 s (mountain mushrooms, gets the blood moving) |
| **Spicy red broth** | Strength plus Speed by the heat level of that side; Extra Spicy and above add Fire Resistance and Hunger, see the table above |
| **Tomato broth** | Haste I for 15 s (sweet and tart, keeps you sharp), and clears Nausea |
| **Three Delicacies Broth** | Water Breathing for 20 s plus Luck I for 30 s (dried scallops and shrimp, fortune smiles) |

It works the same whether you pick it up by hand, eat it with chopsticks, or put it in a bowl first. Simmer a batch in mushroom broth to keep healing, or cook a pot of Three Delicacies Broth and take it diving.

<!-- mryt233 改，2026-11-06（清单#124）：the tooltip now lists each effect and its duration. -->
Hovering over a dish or soup bowl, the item tooltip **indents and lists every status effect it grants and how long it lasts** right below the broth name, one per line, such as "Strength II (0:15)" or "Speed I (0:12)" — blue for benefits, red for drawbacks (Hunger from spicy red broth). Food dipped in honey or vinegar no longer adds Hunger, so it is not listed either. A bowl of broth scooped from the pot lists its effects the same way.

### How food cooks

Raw ingredients turn into cooked food using their **smoker / campfire recipes**. Anything that can be cooked in a smoker can be cooked in the pot. Cooking time is:

> **Time per portion = smoker cooking time ÷ (fuel heat × broth multiplier)**

For example, raw beef takes five seconds in a smoker. Coal + Clear Broth also takes five seconds; Spicy Red Broth (×1.3) takes about 3.8 seconds, while wood (×0.65) takes about 7.7 seconds. **Small ingredients** such as fatty beef rolls, potato slices, and meatballs take half as long as a whole cut of meat, making them quick to cook.

See the cooking section of the [Recipe Collection](recipes.html) for each ingredient's cooking times.

:::warn
**Only put food in the pot.** In 0.1.2-alpha, wood, ores, and iron items could also be added and “cooked” into charcoal or iron nuggets using furnace recipes. Avoid doing this. **From 0.1.3-alpha-hotfix1 onward, pots only accept food:** edible items or items with smoker / campfire recipes. Cooked food cannot be added again.
:::

### Ways to add and remove things

| What you are holding | Right-clicking the pot does this |
| --- | --- |
| Raw ingredient | Adds it to the large cell under your crosshair |
| A [plate](tableware.html) of food | Tips **raw** ingredients into randomly chosen large cells with space; cooked food stays on the plate |
| Bamboo chopsticks holding **raw** ingredient | Places that food in the targeted large cell (into the pot) |
| Bamboo chopsticks holding **cooked** food | Eats it (works even when full; sneak + right-click a bowl or plate to put it back) |
| Bamboo chopsticks (empty) | **Picks up a cooked portion** from the targeted cell (notifies you if there is none); hold right-click to eat once picked up |
| Bamboo chopsticks + sneak (Shift) | **Opens the hotpot screen** |
| Bucket | Adds broth to an empty pot, or scoops broth with an empty bucket |
| Bowl | Collects cooked food or serves soup |
| Empty hand | Collects cooked food from the targeted cell |
| Empty hand + sneak (Shift) | Packs a stove-mounted pot, including broth and food, into your inventory (also works for standalone / campfire pots from 0.1.3-alpha-hotfix1) |

:::tip
**Chopsticks and empty hands take the food currently visible at the surface**; if it is still raw, you are told it is not cooked yet. A bowl first takes the visible cooked portion, then other cooked food, up to 3 portions.
:::

### Hoppers and pots {#hopper}{{since:0.2.0-alpha}}

- **A hopper above the pot** only inserts **raw ingredients that can be cooked**, one portion per slot; anything else stays in the hopper.
- **A hopper below the pot (or stove)** only pulls out **cooked food**; raw food stays in the pot to keep cooking.

## The hotpot screen {#screen}

{{recipe:bamboo_chopsticks}}

**Hold bamboo chopsticks and Shift + right-click the pot** to open it (right-clicking directly picks up a cooked portion without opening the screen). Inside the screen:

- Every portion is visible, with different appearances for raw and cooked food.
- With chopsticks on your cursor, **left-click cooked food** to eat it in one bite, restoring hunger and saturation just like vanilla food (works even when full).
- Double-click to collect matching cooked food onto your cursor, like double-clicking in the vanilla inventory.
- **Hover over food** to see its name. Dipped food also lists “Dipped: …”.
- Holding [dipping sauce](sauce.html) in your **main hand or off hand** will **automatically dip each bite** eaten in this screen.

### Cold pots

A pot is **cold** when it is **not on a burning stove or lit campfire**. The broth is still and ingredients float in it. Eating from a cold pot with chopsticks has a 30% chance of causing **Nausea for 15 seconds**—avoid cold food to keep from upsetting your stomach. Drinking a [Plant Protein Drink](tableware.html#drink), a bowl of clear broth, or dipping with vinegar clears it.

## Cooking on a campfire {#campfire}{{since:0.1.1-alpha}}

You can cook without a stove by placing the pot **on a lit campfire or soul campfire**:

- In 0.1.2-alpha, the pot sits directly on the campfire. From 0.1.3-alpha-hotfix1, four slender iron supports rest on the campfire logs, so the pot no longer appears to float and flames no longer pass through it.
- Heat equals coal, **no fuel is consumed**, and nearby players are healed too; the screen shows that the pot is heating.
- Extinguishing or breaking the campfire makes the pot cold.
- In 0.1.2-alpha, you had to **mine** this pot to pick it up. From 0.1.3-alpha-hotfix1, **Shift + right-click with an empty hand** packs it with broth and food, just like a stove-mounted pot.

## Benefits of eating together {#heal}

A **heated pot** refreshes **Regeneration I** for **10 seconds** every **four seconds** on players **within six blocks**, like a beacon's area effect. Staying beside a pot keeps restoring health, and sharing a table makes it even livelier.

## Yuanyang and four-grid pots {#yuanyang}{{since:0.1.3-alpha-hotfix1}}

Craft a divided pot from a **nine-grid pot** and matching copper / iron ingots: **ingot to the left = Taiji Yuanyang; below = Round-Center; above = straight divider; one on each side = four-grid pot; one above and one below = Square-Center Yuanyang**. Each compartment holds **its own broth**. Aim at a compartment to add or remove its broth. Each food slot cooks with **that compartment's broth**; a compartment without broth will not cook. Like nine-grid pots, these work on the ground, campfires, short stoves, and tall stoves.

| Pot | Divider | Broths | Capacity per compartment |
| --- | --- | --- | --- |
| Taiji Yuanyang | S-shaped, like a yin-yang symbol | 2 | 28 on each side |
| Round-Center Yuanyang | A ring in the middle | 2 | 44 outside, 9 inside |
| Straight-divider Yuanyang | One straight central divider | 2 | 36 on each side |
| Four-grid hotpot | A cross dividing four compartments | 4 | 16 per compartment |
| Square-Center Yuanyang | A square ring in the middle | 2 | 44 outside, 9 inside |

:::warn
In 0.1.4-alpha, S-shaped and O-shaped pots were renamed **Taiji Yuanyang** and **Round-Center Yuanyang**, including their item IDs. **The four renamed pots already placed in worlds from 0.1.3 or earlier will disappear** (two each in copper and iron).
:::

Open the screen with bamboo chopsticks (Shift + right-click the pot). As with a nine-grid pot, small slots fill the pot from the edges inward. **Spaces too close to a divider to fit a full slot are left empty**, showing only broth and the pot bottom. Food floats at several positions in each compartment (S: four per side; O: six outside and two inside; straight: three per side; four-grid: two per compartment). Items sharing a position take turns bobbing to the surface.

In the screen, **double-clicking matching cooked food or collecting it with a bowl only combines food from the same broth**. Compartments with matching broths can be collected together; otherwise, only the selected side is collected.

{{recipe:copper_taiji_yuanyang_hotpot,copper_round_yuanyang_hotpot,copper_split_yuanyang_hotpot,copper_four_grid_hotpot,copper_square_yuanyang_hotpot}}

Iron recipes are the same: replace copper ingots and the copper pot with iron equivalents.

## Mining and removal

- **Mine the pot:** only the pot is removed. Broth and food are packed into its item and restored when placed again. In Creative mode, **middle-click** also copies the complete pot with broth and food.
- Standalone pots **stay in place even with air underneath**.
- **Mine the stove's brick body:** the whole stove is removed, and the packed pot drops too.
- The pot item's tooltip lists its **broth**, **food portions**, and how many are cooked.
- To remove a pot without mining the stove, **Shift + right-click the pot with an empty hand**.
