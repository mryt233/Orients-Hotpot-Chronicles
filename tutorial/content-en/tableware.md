<!-- 作者：；英文翻译。 -->
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

**Start with small dishes:** place two units of material side by side to get two dishes. **Combine matching small dishes to make larger plates.** Quartz examples follow; use the corresponding dishes for other materials.

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
| Chopsticks | Takes the **last-added** portion; right-click again while holding food to return it |
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

### Four soup bowls

Served soup becomes a modded **soup bowl** item. All restore **four hunger points**, with different effects:

| Soup bowl | Effect |
| --- | --- |
| {{i:clear_broth_bowl}} | Clears Nausea, including that caused by a cold pot |
| {{i:rich_stock_bowl}} | High saturation; keeps you full longer |
| {{i:mushroom_broth_bowl}} | Regeneration I for six seconds |
| {{i:spicy_red_broth_bowl}} | Strength I for nine seconds, but Hunger for three seconds from the spice |

Soup bowls return an empty vanilla bowl when consumed and can also be placed on tables.

:::tip
To empty the pot and change its broth, **scoop it out with an empty bucket**, regardless of the broth type. Serving mushroom broth in a drinking bowl is a separate action.
:::

## Drinks {#drink}

What's hotpot without something to drink?

{{icons:orange_drink,plant_protein_drink,empty_orange_drink_bottle,empty_plant_protein_drink_bottle}}

| Drink | Effect per sip |
| --- | --- |
| {{i:orange_drink}} | Restores two hunger points; Speed for 15 seconds; **cools the spice** by clearing Hunger |
| {{i:plant_protein_drink}} | Restores two hunger points; **cools the spice** and clears Nausea |

- Each bottle has **four sips**. You can drink even with a full hunger bar. An **empty bottle** remains afterward and can also sit on the table.
- **Place drinks on a table** and right-click to drink. Their tooltips show the remaining sips.
- Drinks currently have **no crafting recipes**. Get them from the Creative inventory's Food category or with `/give`.

## Chopsticks {#chopsticks}

{{recipe:bamboo_chopsticks}}

Chopsticks are your everyday tool:

- **Open the hotpot screen:** hold chopsticks and right-click a pot.
- **Pick up food:** right-click a plate to take its last-added portion. You can see it at the chopstick tips.
- **Add food to a pot:** right-click while holding food with chopsticks to place it in the targeted large cell.
- **Eat:** with edible food held, **hold right-click while aiming into the air**. Any dipping-sauce effects apply too.
- **Dip:** use chopsticks holding food on a sauce bowl on the table; see [Dipping Sauces](sauce.html).
