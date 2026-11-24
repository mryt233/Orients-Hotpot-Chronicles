<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：按 0.3.1-alpha 正式版整理英文餐具与饮料页，修复围裙表格语法。 -->
# Tableware & Drinks

Fill your hotpot table with plates of raw ingredients, bowls of soup, drinks to cool the spice, protective aprons, and chopsticks for picking up food.

## Plates {#plate}

### Types

**5 materials × 6 sizes = 30 plates:**

| Material | Small dish ingredient |
| --- | --- |
| Quartz | {{i:minecraft:quartz}} |
| Woven bamboo | {{i:minecraft:bamboo}} |
| Terracotta | {{i:minecraft:brick}} |
| Copper | {{i:minecraft:copper_ingot}} |
| Iron | {{i:minecraft:iron_ingot}} |

| Size | Shape | Food capacity |
| --- | --- | --- |
| Small dish (1×1) | One slot | 3 portions |
| Two-slot plate (1×2) | Two slots long | 6 portions |
| Three-slot long plate (1×3) | Three slots long | 9 portions |
| Four-slot square plate (2×2) | Square | 12 portions |
| Five-slot long plate (1×5) | Five slots long | 15 portions |
| Eight-slot platter (2×4) | Large | 24 portions |

Each **slot** holds up to 3 portions of any food, so capacity = slots × 3.

### Crafting

**Start with small dishes**: arrange **“material + bowl + material” horizontally** to craft 2 small dishes, then combine matching small dishes into larger plates (quartz shown below):

{{recipe:quartz_small_dish,bamboo_small_dish,terracotta_small_dish,copper_small_dish,iron_small_dish}}

{{recipe:quartz_two_slot_plate,quartz_three_slot_long_plate,quartz_four_slot_square_plate,quartz_five_slot_long_plate,quartz_eight_slot_platter}}

### Placement {#place}

- Hold a plate and **right-click a tabletop or the ground** to place it.
- **Multiple plates share one block** without overlapping: up to 5 small dishes, 2 long plates, or 1 square plate per block (mixed as space permits).
- Plates hug the tabletop automatically (5 px lower on low tables, 7 px higher on tall tables), and items placed on every plate size — including five-slot long plates, eight-slot platters, and small dishes — render cleanly layered without z-fighting or sinking under shaders.

### Adding and removing food {#use}

Actions apply to **only the plate under your crosshair**:

| What you are holding | Right-clicking the plate does this |
| --- | --- |
| Plate | Places another plate |
| Food | Adds 1 portion (up to 3 per slot) |
| Bamboo chopsticks | Picks up the **last-added** portion; clicking while already holding cooked food = **eats it** (sneak + right-click to put back) |
| Empty hand | **Picks up the whole plate with food** |
| Empty hand + sneak | Pops the last-added portion into your inventory, leaving the plate |

- **Mine a plate**: breaks only the targeted plate and drops its food.
- **Middle-click**: copies the plate **including its contents**.
- Filled plates render their contents in inventory icons and in-hand, with bundle-style "×quantity" tooltips.
- **Tip into the pot**: right-click the pot with a filled plate to pour **raw** ingredients in while cooked food stays on the plate.

## Bowls and soup bowls {#bowl}

### Vanilla bowls work too

**Vanilla bowls** ({{i:minecraft:bowl}}) can sit on tables too:

- **Place on a table** like a plate: holds **3 portions** cleanly layered without flicker.
- **Right-click a pot with a bowl**: collects **cooked food** first (up to 3 portions), or ladles **soup** if none is cooked (without draining pot broth).
- Right-click a pot with a food-filled bowl to tip everything inside.

### Six soup bowls

Ladled soup becomes a dedicated **soup bowl** item restoring **4 hunger** plus its broth effect (custom broths without a dedicated bowl item fill the generic {{i:soup_bowl}}):

| Soup bowl | Effect |
| --- | --- |
| {{i:clear_broth_bowl}} | Speed I for 15 s + Saturation 1 tick; **clears Nausea** (clean and light) |
| {{i:rich_stock_bowl}} | Resistance I for 15 s + Absorption I for 30 s (high saturation, beef bones and old hen) |
| {{i:mushroom_broth_bowl}} | Regeneration I for 10 s (mountain mushrooms, restorative) |
| {{i:spicy_red_broth_bowl}} | Strength + Speed (Mild: Strength I 6 s + Speed I 6 s; Medium Spicy: Strength II 9 s + Speed I 9 s; Extra Spicy: Strength II 12 s + Speed I 10 s + Fire Resistance 15 s + Hunger I 6 s; Level 3 Extreme Spicy: Strength II 15 s + Speed I 12 s + Fire Resistance 30 s + Hunger I 8 s). **Keeps the pot's heat level when ladled**; see [heat levels](hotpot.html#spicy_level) |
| {{i:tomato_soup_bowl}} | Haste I for 15 s; clears Nausea (sweet and tart) |
| {{i:three_fresh_soup_bowl}} | Water Breathing for 20 s + Luck I for 30 s (scallops and shrimp) |

Finished soup bowls return empty vanilla bowls and can also sit on tables.

:::tip
Always **use an empty bucket** to change broth in the pot — ladling bowls is solely for drinking.
:::

## Drinks {#drink}

{{icons:orange_drink,plant_protein_drink,orient_cola,glass_cup}}

| Drink | Effect per sip |
| --- | --- |
| {{i:orange_drink}} | Restores 2 hunger; Speed 15 s; **cools spice** (clears Hunger) |
| {{i:plant_protein_drink}} | Restores 2 hunger; **cools spice** and clears Nausea |
| {{i:orient_cola}} | Restores 2 hunger; Speed and Haste 15 s; fizzes with bubbles |

- One bottle gives **4 sips** even when full, leaving an **empty bottle** placeable on tables.
- Place drinks on tables and right-click to sip; tooltips show remaining sips.
- Craft Orange Drink from oranges, Plant Protein Drink from soybeans, and Orient Cola from cocoa beans and honey.
- Starting in `0.3.1-alpha`, pick ripe oranges directly off [standalone orange trees](ingredients.html#crops_trees) in forests and plains (vanilla oak and dark oak leaves no longer drop oranges).

{{recipe:orange_drink,plant_protein_drink,orient_cola}}

### Glass cups {#cup}

{{recipe:glass_cup}}

- Right-click an empty cup with a drink bottle to pour ({{i:orange_drink_cup}}{{i:plant_protein_drink_cup}}{{i:orient_cola_cup}}); **1 cup = 1 sip** and returns the empty cup.
- Finished bottles leave **empty bottles** ({{i:empty_orange_drink_bottle}}{{i:empty_plant_protein_drink_bottle}}) for decoration or refilling from cauldrons.
- **Cup of Water / Cup of Milk** ({{i:water_cup}}{{i:milk_cup}}): right-click an empty cup with a water or milk bucket (**bucket stays full**), or scoop water sources directly; neither restores hunger, and milk clears effects.

### Toasting {#cheers}{{since:0.2.2-alpha}}

{{icons:glass_cup,orange_drink_cup,orient_cola_cup}}

Hold any **filled glass cup** (orange drink, plant protein drink, Orient Cola, water, or milk) and toast in three ways:

1. **Right-click a friend** also holding a filled cup in main or off hand to clink together.
2. **Solo or casual toast**: hold **Shift** and right-click air (prioritizes any cup-holding player within 4 blocks).
3. **Right-click a filled cup placed on a table**: clinks on the tabletop too.

A crisp double chime plays alongside rim splashes and cheerful music notes. Both players receive **Luck I for 30 s + Regeneration I for 5 s**, and the cup records **+1 bonus hunger on its next sip** (tooltip reads "Toasted · Restores +1 bonus Hunger when drunk").

Drinking resets the cup cleanly; toasting unlocks the "**Cheers!**" advancement. (Cooldown, buff durations/levels, and bonus hunger/saturation are tunable under "Cheers" in [Configurable values](faq.html#config).)

:::tip
Start hotpot nights with a toast — boosted Luck helps you snag meatballs, and Shift + right-click air works solo anytime!
:::

### Drinks in cauldrons {#cauldron}

- Right-click a vanilla **cauldron** with a drink bottle or filled cup to pour it in (one drink type per cauldron, up to **12 sips** / 3 bottles; level rises every 4 sips; cola bubbles).
- **Empty cup scoops 1 sip**; vanilla glass bottle or matching empty bottle scoops **up to 4 sips**. Mixing drinks is blocked safely.


## Cloth apron {#apron}{{since:0.2.2-alpha}}

{{icons:cloth_apron}}

Protect yourself from hot oil splashes and Level 3 Extreme Spicy lip-flames with a **cloth apron**:

- **Equip**: wears in the chestplate slot with real leather-tier protection (**3 armor, 80 durability**), taking normal wear and enchantments.
- **Craft**: in 3×3, **1 string at top-center** + **6 white wool** across the middle and bottom rows.
- **Repair**: repair on an anvil using **white wool** or **string**.

{{recipe:cloth_apron}}

### Three styles and recoloring {#apron_styles}{{since:0.2.2-alpha}}

{{icons:cloth_apron,blue_print_cloth_apron,brown_trim_cloth_apron}}

All **three styles** share identical armor, durability, enchantability, and repair materials — only the pattern changes:

| Style | Look |
| --- | --- |
| {{i:cloth_apron}} | **Plain off-white linen** (default) |
| {{i:blue_print_cloth_apron}} | Indigo dyed cloth with tiny white floral print |
| {{i:brown_trim_cloth_apron}} | Off-white apron with dark brown trim and a hem pocket |

**Recoloring**: combine **any apron style + dye** shapelessly —

- Any style + **white dye** → **linen**
- Any style + **blue dye** → **blue print**
- Any style + **brown dye** → **brown trim**

Recoloring updates **the same apron** while keeping **durability, enchantments, and custom names**; matching dye returns it untouched.

{{recipe:cloth_apron,blue_print_cloth_apron,brown_trim_cloth_apron}}

:::tip
**How it looks worn**: hugs the front torso down to just above the knees, swinging naturally with your steps and resting flat across your lap when seated, with transparent sleeves/back and just the neck strap behind.
:::

While worn:

1. **No lip flames on Extreme Spice**: eating Level 3 Extreme Spicy food or broth **no longer lights the 2-second mouth flame** (purely visual when unarmored, never deals damage).
2. **Splash shield within 4 blocks**: standing within 4 blocks of a **lit stove, lit campfire, fire block, or lava** grants immunity to close-range burn/lava/hot-floor splash ticks and extinguishes flames on you (direct lava wading, mobs, and falls still hurt normally).

:::tip
**Stay close to the heat**: splash protection requires an active heat source within 4 blocks; step away and it behaves like ordinary armor.
:::

## Chopsticks {#chopsticks}

{{recipe:bamboo_chopsticks}}

- **Open hotpot screen**: hold chopsticks and **Shift + right-click pot**.
- **Pick up cooked food from pot**: **plain right-click pot** to grab a cooked item from that cell.
- **Pick up from bowls/plates**: right-click to lift the last-added item.
- **Add raw food to pot**: right-click pot while holding **raw** food on chopsticks.
- **Eat**: hold right-click with cooked food on chopsticks (**works even when full**, adding saturation and sauce buffs).
- **Return to plate/bowl**: **sneak + right-click center** while holding food.
- **Dip**: right-click a placed sauce bowl while holding food; see [Dipping Sauces](sauce.html).
- **Rest on bowl or plate rims**{{since:0.3.0-alpha}}: **Shift + right-click the rim** to perch chopsticks inward from that side (carrying held food along); clicking the center empty-handed rests on your facing side, snapped to 22.5° steps (**empty-hand right-click** picks back up; breaking drops both).
- **Rest flat on tabletops**{{since:0.2.2-alpha}}: aim at an **empty spot on any low/tall hotpot table** and **Shift + right-click** to place chopsticks intact with a porcelain mini chopstick rest — two parallel 8.5 px bamboo sticks resting tips across the saddle and ends facing you across all 4 cardinal directions (occupying 9.5×4.5 px near the clicked spot; empty-hand right-click or mining retrieves chopsticks + held food cleanly without dropping a separate rest).

{{icons:bamboo_chopsticks,chopstick_rest}}

:::tip
Rim resting vs tabletop resting are separate placements: **bowls/plates** hold chopsticks on their rims, while **empty tabletops** add the porcelain rest. Pick up empty-handed before moving.
:::

:::tip
**Eat even when full**{{since:0.2.0-alpha}}: every mod food item can be eaten on a full hunger bar, topping up saturation up to 20; compatible with Always Eat.
:::
