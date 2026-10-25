<!-- 作者：；英文翻译。 -->
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
| Yuanyang and four-grid pots | Coming in 0.1.3; see [Yuanyang and four-grid pots](#yuanyang) below. |

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

- Add broth: **hold the bucket or mushroom stew and right-click the pot**. A regular pot holds only one broth at a time; divided pots are different.
- **Remove broth:** **right-click with an empty bucket** to recover its original contents (water returns as a water bucket). Mushroom broth requires a **bowl** and **no food remaining in the pot**; take all food out first.
- **Serve soup to drink:** right-click with a bowl. Cooked food is collected first; if there is none, you get soup. See [Tableware & Drinks](tableware.html#bowl).

### How food cooks

Raw ingredients turn into cooked food using their **smoker / campfire recipes**. Anything that can be cooked in a smoker can be cooked in the pot. Cooking time is:

> **Time per portion = smoker cooking time ÷ (fuel heat × broth multiplier)**

For example, raw beef takes five seconds in a smoker. Coal + Clear Broth also takes five seconds; Spicy Red Broth (×1.3) takes about 3.8 seconds, while wood (×0.65) takes about 7.7 seconds. **Small ingredients** such as fatty beef rolls, potato slices, and meatballs take half as long as a whole cut of meat, making them quick to cook.

See the cooking section of the [Recipe Collection](recipes.html) for each ingredient's cooking times.

:::warn
**Only put food in the pot.** In 0.1.2-alpha, wood, ores, and iron items can also be added and “cooked” into charcoal or iron nuggets using furnace recipes. Avoid doing this. **From 0.1.3 onward, pots only accept food:** edible items or items with smoker / campfire recipes. Cooked food cannot be added again.
:::

### Ways to add and remove things

| What you are holding | Right-clicking the pot does this |
| --- | --- |
| Raw ingredient | Adds it to the large cell under your crosshair |
| A [plate](tableware.html) of food | Tips **raw** ingredients into randomly chosen large cells with space; cooked food stays on the plate |
| Chopsticks holding food | Places that food in the targeted large cell |
| Empty chopsticks | Opens the hotpot screen |
| Bucket | Adds broth to an empty pot, or removes broth with an empty bucket |
| Bowl | Collects cooked food or serves soup |
| Empty hand | Collects cooked food from the targeted cell |
| Empty hand + sneak (Shift) | Packs a stove-mounted pot, including broth and food, into your inventory (also works for standalone / campfire pots from 0.1.3) |

## The hotpot screen {#screen}

{{recipe:bamboo_chopsticks}}

**Hold chopsticks and right-click the pot** to open it:

- Every portion is visible, with different appearances for raw and cooked food.
- With chopsticks on your cursor, **left-click cooked food** to eat it, restoring hunger and saturation just like vanilla food.
- Double-click to collect matching cooked food onto your cursor, like double-clicking in the vanilla inventory.
- **Hover over food** to see its name. Dipped food also lists “Dipped: …”.
- Hold [dipping sauce](sauce.html) in your off hand to **automatically dip each bite** eaten in this screen.

### Cold pots

A pot is **cold** when it is **not on a burning stove or lit campfire**. The broth is still and ingredients float in it. Eating from a cold pot with chopsticks has a 30% chance of causing **Nausea for 15 seconds**. A [Plant Protein Drink](tableware.html#drink) clears Nausea.

## Cooking on a campfire {#campfire}{{since:0.1.1-alpha}}

You can cook without a stove by placing the pot **on a lit campfire or soul campfire**:

- In 0.1.2-alpha, the pot sits directly on the campfire. From 0.1.3, four slender iron supports rest on the campfire logs, so the pot no longer appears to float and flames no longer pass through it.
- Heat equals coal, **no fuel is consumed**, and nearby players are healed too.
- Extinguishing or breaking the campfire makes the pot cold.
- In 0.1.2-alpha, you must **mine** this pot to pick it up. From 0.1.3, **Shift + right-click with an empty hand** packs it with broth and food, just like a stove-mounted pot.

## Benefits of eating together {#heal}

A **heated pot** refreshes **Regeneration I** for **10 seconds** every **four seconds** on players **within six blocks**, like a beacon's area effect. Staying beside a pot keeps restoring health, and sharing a table makes it even livelier.

## Yuanyang and four-grid pots {#yuanyang}{{dev}}

:::dev
These pots are **coming in 0.1.3** and are not in 0.1.2-alpha. Gameplay and details, especially the screen, may still change before release.
:::

Craft a divided pot from a **nine-grid pot** and matching copper / iron ingots: **ingot to the left = S-shaped Yuanyang; below = O-shaped; above = straight divider; one on each side = four-grid pot**. Each compartment holds **its own broth**. Aim at a compartment to add or remove its broth. Each food slot cooks with **that compartment's broth**; a compartment without broth will not cook. Like nine-grid pots, these work on the ground, campfires, short stoves, and tall stoves.

| Pot | Divider | Broths | Capacity per compartment |
| --- | --- | --- | --- |
| S-shaped Yuanyang | S-shaped, like a yin-yang symbol | 2 | 28 on each side |
| O-shaped Yuanyang | A ring in the middle | 2 | 44 outside, 9 inside |
| Straight-divider Yuanyang | One straight central divider | 2 | 36 on each side |
| Four-grid hotpot | A cross dividing four compartments | 4 | 16 per compartment |

Open the screen with chopsticks. Small slots fill the pot from the edges inward, as with a nine-grid pot. **Spaces too close to a divider to fit a full slot are left empty**, showing only broth and the pot bottom. Food floats at several positions in each compartment (S: four per side; O: six outside and two inside; straight: three per side; four-grid: two per compartment). Items sharing a position take turns appearing.

In the screen, **double-clicking matching cooked food or collecting it with a bowl only combines food from the same broth**. Compartments with matching broths can be collected together; otherwise, only the selected side is collected.

{{recipe:copper_s_yuanyang_hotpot,copper_o_yuanyang_hotpot,copper_split_yuanyang_hotpot,copper_four_grid_hotpot}}

Iron recipes are the same: replace copper ingots and the copper pot with iron equivalents.

## Mining and removal

- **Mine the pot:** only the pot is removed. Broth and food are packed into its item and restored when placed again.
- **Mine the stove's brick body:** the whole stove is removed, and the packed pot drops too.
- The pot item's tooltip lists its **broth**, **food portions**, and how many are cooked.
- To remove a pot without mining the stove, **Shift + right-click the pot with an empty hand**.
