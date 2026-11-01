<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-10-30：同步正式版 #30–#71 和未发布 #72–#82；保留原译者署名。 -->
# Tableware & Drinks

Fill your hotpot table with plates of raw ingredients, bowls of soup, drinks to cool the spice, and chopsticks for picking up food.

## Plates {#plate}

### Types

**Five materials × six sizes = 30 plates:**

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

Each **slot** holds up to three portions, including a small dish, so plate capacity = slots × 3.

### Crafting

**Start with small dishes:** place **“material bowl material” in a horizontal row** (with a vanilla bowl in the middle) to craft 2 small dishes. **Combine matching small dishes to make larger plates.** Quartz examples follow; use the corresponding dishes for other materials:

{{recipe:quartz_small_dish,bamboo_small_dish,terracotta_small_dish,copper_small_dish,iron_small_dish}}

{{recipe:quartz_two_slot_plate,quartz_three_slot_long_plate,quartz_four_slot_square_plate,quartz_five_slot_long_plate,quartz_eight_slot_platter}}

### Placement {#place}

- Hold a plate and **right-click a tabletop or the ground** to place it.
- **Multiple plates fit in one block**, without overlapping. Types can be mixed: up to five small dishes, two long plates, or one square plate per block.
- Plates align automatically with the tabletop: five pixels lower on low tables, seven pixels higher on tall tables.

### Adding and removing food {#use}

These actions affect **only the plate under your crosshair**:

| What you are holding | Right-clicking the plate does this |
| --- | --- |
| Plate | Places another plate |
| Food | Adds one portion (up to three per slot) |
| Bamboo chopsticks | Takes the **last-added** portion; right-clicking the plate again while chopsticks already hold cooked food = **eats it** (returns it only when too full to eat) |
| Empty hand | **Picks up the whole plate with its food** |
| Empty hand + sneak | Takes the last-added portion into your inventory, leaving the plate |

- **Mine a plate:** only the targeted plate breaks, and its food drops too.
- **Middle-click:** copies the targeted plate **including its food**.
- Filled plates show their food **in inventory icons and while held**. Their tooltips work like vanilla bundles, combining matching items into “×quantity”.
- **Tip food into the pot:** right-click the pot with a filled plate. **Raw** ingredients go in; cooked food stays on the plate.

## Bowls and soup bowls {#bowl}

### Vanilla bowls work too

**Vanilla bowls** ({{i:minecraft:bowl}}) gain new uses:

- **Place one on a table** like a plate. It holds **three portions** and works as a small dish.
- **Right-click a pot with a bowl:** it first collects **cooked food** (up to three portions). If there is none, it serves **soup**. Soup servings do not reduce the broth in the pot.
- Right-click a pot with a bowl already holding food to tip the whole bowl into it.

### Six soup bowls

Served soup becomes a modded **soup bowl** item. All restore **four hunger points**, with different effects:

| Soup bowl | Effect |
| --- | --- |
| {{i:clear_broth_bowl}} | Clears Nausea, including that caused by a cold pot |
| {{i:rich_stock_bowl}} | High saturation; keeps you full longer |
| {{i:mushroom_broth_bowl}} | Regeneration I for six seconds |
| {{i:spicy_red_broth_bowl}} | Strength I for nine seconds, but Hunger for three seconds from the spice |
| {{i:tomato_soup_bowl}} | Regeneration I for 6 seconds; clears Nausea |
| {{i:three_fresh_soup_bowl}} | Water Breathing for 15 seconds |

Soup bowls return an empty vanilla bowl when consumed and can also be placed on tables.

:::tip
To empty the pot and change its broth, **scoop it out with an empty bucket**, regardless of the broth type. Serving mushroom broth in a drinking bowl is a separate action.
:::

## Drinks {#drink}

What's hotpot without something to drink?

{{icons:orange_drink,plant_protein_drink,orient_cola,glass_cup}}

| Drink | Effect per sip |
| --- | --- |
| {{i:orange_drink}} | Restores two hunger points; Speed for 15 seconds; **cools the spice** by clearing Hunger |
| {{i:plant_protein_drink}} | Restores two hunger points; **cools the spice** and clears Nausea |
| {{i:orient_cola}} | Restores 2 hunger points; Speed and Haste for 15 seconds; the bottle bubbles |

- Each bottle has **four sips**. You can drink even with a full hunger bar. An **empty bottle** remains afterward and can also sit on the table.
- **Place drinks on a table** and right-click to drink. Their tooltips show the remaining sips.
- All three drinks have recipes: oranges for Orange Drink, soybeans for Plant Protein Drink, and cocoa beans and honey for Orient Cola. **Oranges** drop from oak and dark oak leaves.

{{recipe:orange_drink,plant_protein_drink,orient_cola}}

### Glass cups {#cup}

{{recipe:glass_cup}}

- Right-click an empty cup with a drink bottle to fill it ({{i:orange_drink_cup}}{{i:plant_protein_drink_cup}}{{i:orient_cola_cup}}). **One cup holds one sip**; drinking leaves an empty cup. Cups can be placed on tables.
- Finished bottles leave **empty bottles** ({{i:empty_orange_drink_bottle}}{{i:empty_plant_protein_drink_bottle}}), which can sit on tables or scoop drinks from cauldrons.
- **Cup of Water / Cup of Milk** ({{i:water_cup}}{{i:milk_cup}}): right-click an empty cup with a water or milk bucket; **the bucket stays full**. An empty cup can also scoop from a water source. Water has no effect, milk clears all effects, and neither restores hunger.

### Drinks in cauldrons {#cauldron}

- Right-click a vanilla **cauldron** with a drink bottle or filled cup to pour it in. Each cauldron holds one drink type, up to **12 sips** (3 full bottles); the surface rises once per 4 sips and takes the drink's color. Cola bubbles.
- **An empty cup scoops one sip**. A vanilla glass bottle or an empty bottle of that drink scoops **one bottle**, up to 4 sips. Scooping the last sip leaves an empty cauldron. A different drink is rejected with a message.

## Chopsticks {#chopsticks}

{{recipe:bamboo_chopsticks}}

Chopsticks are the most commonly used tool in this mod:

- **Open the hotpot screen:** hold bamboo chopsticks and **Shift + right-click the pot**.
- **Pick up cooked food from the pot:** **right-click the pot directly** to pick up a cooked portion from the targeted cell (it will notify you if there is no cooked food in that cell).
- **Pick up food from bowls and plates:** right-click a bowl or plate to pick up the last-added portion (you can see it at the chopstick tips while held).
- **Add food to the pot:** right-click the pot while holding a **raw** ingredient to place it in the targeted large cell (into the pot).
- **Eat:** while holding cooked food, **hold right-click** to eat it (aiming at the air, the pot, a bowl, or a plate all work; if you are too full to eat, right-clicking a bowl or plate will return the food). Any dipping-sauce effects apply too.
- **Dip:** right-click a sauce bowl on the table while holding food with chopsticks to take a dip; see [Dipping Sauces](sauce.html).
- **Rest chopsticks on bowls and plates:** **Shift + right-click** a placed bowl or plate with chopsticks, even while holding food. The chopsticks rest across its rim with the food above the center. **Right-click with an empty hand** to retrieve them, keeping the food. Breaking the bowl drops the chopsticks too.

:::dev
**Development-build chopsticks:** you can eat even when full; the hunger bar stays full while saturation is still added. Right-clicking a bowl or plate still eats cooked food. Returning it uses **sneak + right-click** — **food is returned first** (the chopsticks stay in hand), and **only empty chopsticks rest on the rim**, so the two actions do not conflict; normal right-click eating is unchanged. See the [Development Preview](preview.html#chopsticks). The “return when full” behavior above still describes released 0.1.4-alpha.
:::
