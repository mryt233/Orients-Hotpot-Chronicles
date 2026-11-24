<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-22）：按 0.3.1-alpha 正式版整理灶台与火锅英文页，清除开发预览框并接入指南模式折叠。 -->
# Stoves & Hotpots

Hotpot has three essentials: a **stove** for heat, a **pot** for broth and ingredients, and a **broth base** that determines cooking speed and flavor. Here is how they all work.

## Stoves

{{icons:brick_stove,tall_brick_stove}}

### Short and tall stoves

- {{i:brick_stove}}: the short version. Its brick surface sits flush with a low hotpot table.
{{recipe:brick_stove}}
- {{i:tall_brick_stove}}: raises the firebox and pot by 10 pixels to match a **tall hotpot table** (its brick surface sits two pixels below the tabletop while the pot rim rises three pixels above for easy access).
{{recipe:tall_brick_stove}}

A tall stove also occupies the block above it with an invisible block — **leave that space empty when placing one**. Once mounted with a pot, if a click misses the pot slot you can right-click the **lower brick body** to reliably open the firebox, and the upper hitbox updates immediately whenever a pot is placed or removed. Both short and tall stoves match adjacent vanilla bricks cleanly on all four sides whether lit or unlit, leaving only firebox flames and embers glowing warmly.

{{img:stoves-boiling.webp|Pots on short and tall stoves, with lit fires and boiling broth}}

### Fuel and heat {#fuel}

Right-click the stove's brick body (or the stove itself when no pot is attached) to open the **firebox screen** and add fuel. **Any vanilla furnace fuel works**, with the same burn time as a furnace. There are three heat levels:

| Fuel | Heat | Burn time (same as a furnace) |
| --- | --- | --- |
| Wood items (planks, logs, sticks, etc.) | ×0.65 | For example, planks: 15 seconds |
| Coal or charcoal | ×1.0 | 80 seconds |
| Lava bucket | ×1.6 | 1000 seconds |

:::tip
**Fuel is consumed only while raw ingredients need cooking.**
If the pot has broth and the firebox has fuel but there is no raw food, the fire stays lit and the broth keeps bubbling evenly **without consuming fuel** — take your time setting the table without wasting coal. An empty pot without broth will not light the stove.
:::

The firebox screen has no temperature bar or countdown timer — watch the flame size to see how strongly it burns.

## Pots {#pot}

### Types

| Pot | Description |
| --- | --- |
| {{i:iron_nine_grid_hotpot}} / {{i:copper_nine_grid_hotpot}} | Nine-grid pot: one broth across nine large cells. Copper and iron differ only in appearance. |
| Yuanyang and four-grid pots | Available since 0.1.3-alpha-hotfix1; see [Yuanyang and four-grid pots](#yuanyang) below. |

Choose copper or iron, then select a pot shape below:

{{potviewer}}

{{recipe:iron_nine_grid_hotpot,copper_nine_grid_hotpot}}

A pot is an independent block: it can sit **on a stove**, **on the ground**, or **on a lit campfire**.

### Capacity

A nine-grid pot has 9 large cells, each containing a 3×3 grid of 9 smaller slots — **up to 81 portions per pot**. Food enters the large cell under your crosshair; once full, no more fits there.

### Broth bases {#soup}

| Add this | Broth | Cooking speed | Notes |
| --- | --- | --- | --- |
| {{i:minecraft:water_bucket}} | Clear Broth | ×1.00 | Clear and light |
| {{i:minecraft:milk_bucket}} | Rich Stock | ×0.85 | Milky white; slowest |
| {{i:minecraft:mushroom_stew}} | Mushroom Broth | ×1.15 | Returns the bowl when poured |
| {{i:minecraft:lava_bucket}} | Spicy Red Broth | ×1.30 | Bright red; fastest |
| {{i:tomato_soup_bucket}} | Tomato Broth | ×1.10 | Craft a broth bucket from hotpot base + water bucket, or add base to Clear Broth |
| {{i:three_fresh_soup_bucket}} | Three Delicacies Broth | ×1.00 | Same method with Three Delicacies Hotpot Base |

**Hotpot bases**: {{i:tomato_hotpot_base}} uses tomatoes and {{i:three_fresh_hotpot_base}} uses Three Delicacies ingredients (see [Recipe Collection](recipes.html)). Dropping a base into Clear Broth converts the pot directly, just like pouring a crafted broth bucket.

- **Add broth**: hold a bucket (or mushroom stew) and right-click the pot. Standard pots hold one broth at a time (divided pots hold multiple).
- **Remove broth**: **right-click with an empty bucket** to scoop broth back out. Mushroom broth requires a **bowl** and **no food left in the pot** — take out all food first.
- **Serve soup to drink**: right-click with a bowl to collect cooked food first, or soup if none is cooked; see [Tableware & Drinks](tableware.html#bowl).

### Four heat levels for Spicy Red Broth {#spicy_level}{{since:0.2.2-alpha}}

Spicy Red Broth can be dialed up — not with powder, by **throwing real chili peppers into the pot**.

| Level | Effect on food cooked in that pot |
| --- | --- |
| **Mild** (just poured) | Strength I for 6 s, Speed I for 6 s, no Hunger |
| **Medium Spicy** | Strength II for 9 s, Speed I for 9 s |
| **Extra Spicy** | Strength II for 12 s, Speed I for 10 s, Fire Resistance 15 s, and Hunger I 6 s (spice opens your appetite) |
| **Level 3 Extreme Spicy** | Strength II for 15 s, Speed I for 12 s, Fire Resistance 30 s, and Hunger I 8 s; your mouth bursts into harmless flame for 2 s (prevented by a cloth apron) |

How to adjust it:

1. **Hold a {{i:chili_pepper}} chili pepper and right-click the red broth area**: heat goes up one level with a burst of flame and an action-bar message. At Extreme Spicy it notifies you without consuming the pepper.
2. **Cool down with ice**: regular **Ice** drops 1 level, **Packed Ice** drops 2, and **Blue Ice** resets straight to Mild with white steam and splashes.
3. Clicking **any other broth** with chili or ice warns that only Spicy Red Broth can be adjusted and will not insert them as food.
4. Yuanyang and four-grid pots **track each compartment separately** — only the compartment you click changes. Pouring, scooping, swapping broths, or emptying resets heat back to Mild.

A bowl of {{i:spicy_red_broth_bowl}} scooped from the pot keeps **the heat level at the moment it was scooped**. Cooked food also shows its heat level, such as "Broth: Spicy Red Broth · Extra Spicy".

<!--guide-fallback:penalties_red_off-->

<!--guide-collapse-->
:::tip
Pair spice levels with **chili oil**, **honey**, and **vinegar**: chili oil grants Fire Resistance, while honey and vinegar clear the Hunger debuff and suppress the 2-second mouth flame. Spicy step durations, Fire Resistance, Hunger, and mouth-flame thresholds can also be tuned in [Configurable values](faq.html#config) under "Spicy Broth" (Simple Mode or disabling `mode.penalties_enabled` skips the Hunger and mouth-flame penalties).
:::
<!--/guide-collapse-->

### What the broth adds to your food {#dish_flavor}{{since:0.2.2-alpha}}

Food cooked in a pot **carries that broth's signature flavor** — tooltips gain a line such as "**Broth: Mushroom Broth**" (plus heat level for Spicy Red Broth) and grant an extra effect when eaten:

| Broth | Extra effect from food cooked in it |
| --- | --- |
| **Clear Broth** | Speed I for 15 s (clean and light) + 1 tick of Saturation |
| **Rich Stock** | Resistance I for 15 s + Absorption I for 30 s (beef bones and old hen, hearty and protective) |
| **Mushroom Broth** | Regeneration I for 10 s (mountain mushrooms, restorative) |
| **Spicy Red Broth** | Strength + Speed scaled by that compartment's heat level; Extra Spicy+ adds Fire Resistance and Hunger (see above) |
| **Tomato Broth** | Haste I for 15 s (sweet and tangy energy boost), also clears Nausea |
| **Three Delicacies Broth** | Water Breathing for 20 s + Luck I for 30 s (scallops and shrimp, fortune smiles) |

It counts whether you pick food up by hand, eat with chopsticks, or ladle into a bowl. Simmer a batch in Mushroom Broth to heal up, or brew Three Delicacies Broth before diving.

Hovering over a dish or soup bowl indents and lists **every status effect and duration** right below the broth line (e.g., "Strength II (0:15)", "Speed I (0:12)") — blue for buffs, red for debuffs. Dipping in honey or vinegar suppresses Hunger so it disappears from the tooltip too. Soup bowls list their effects the exact same way.

### How food cooks

Raw ingredients turn into cooked dishes using their **smoker / campfire recipes** or **pot-only boiling recipes** (anything a smoker cooks can simmer in the pot):

> **Time per portion = base cooking time ÷ (fuel heat × broth multiplier)**

For example, raw beef takes 5 seconds in a smoker, so Coal + Clear Broth takes 5 seconds; Spicy Red Broth (×1.3) takes ~3.8 seconds, while wood (×0.65) takes ~7.7 seconds. **Small cuts** like fatty beef rolls, potato slices, meatballs, and boneless chicken feet take half as long as whole meat.

See the "Cooking" and "Pot-Only Boiling" sections of the [Recipe Collection](recipes.html) for exact times.

:::warn
**Only put food in the pot**: only raw cookable foods can be inserted; already-cooked food, chili peppers, milled spices, and items tagged `orients_hotpot_chronicles:not_pot_ingredient` are rejected.
:::

### Ways to add and remove things

| What you are holding | Right-clicking the pot does this |
| --- | --- |
| Raw ingredient | Adds it to the large cell under your crosshair |
| A [plate](tableware.html) of food | Tips **raw** ingredients into available large cells; cooked food stays on the plate |
| Bamboo chopsticks holding **raw** food | Drops that ingredient into the targeted cell |
| Bamboo chopsticks holding **cooked** food | Eats it (even when full; sneak + right-click a plate/bowl to put back) |
| Bamboo chopsticks (empty) | **Picks up a cooked portion** from that cell (notifies if none); hold right-click to eat |
| Bamboo chopsticks + sneak (Shift) | **Opens the hotpot screen** |
| Bucket | Pours broth in, or scoops broth out with an empty bucket |
| Bowl | Collects cooked food first, or serves soup |
| Empty hand | Collects cooked food from the targeted cell |
| Empty hand + sneak (Shift) | Packs the pot (with broth and food intact) into your inventory |

:::tip
**Chopsticks and empty hands pick up whichever portion is currently visible at the surface**; if it is still raw, a message warns you. A bowl collects the visible cooked portion plus other cooked food, up to 3 portions total.
:::

<!--guide-collapse-->
### Hoppers and pots {#hopper}{{since:0.2.0-alpha}}

- **A hopper above the pot** inserts only **raw cookable ingredients**, one per slot; everything else remains in the hopper.
- **A hopper below the pot (or stove)** extracts only **cooked dishes**, leaving raw food to finish cooking.
<!--/guide-collapse-->

## The hotpot screen {#screen}

{{recipe:bamboo_chopsticks}}

**Hold bamboo chopsticks and Shift + right-click the pot** to open it (plain right-click picks up food instead):

- Every portion is rendered individually (raw vs cooked appearances), and bubbles, steam, and floating ingredients maintain a steady, smooth phase throughout burning with smooth ignition/extinguish transitions.
- With chopsticks on your cursor, **left-click cooked food** to eat it in one bite (restores hunger and saturation, works even when full).
- Double-click to gather matching cooked portions onto your cursor like a vanilla inventory.
- **Hover over ingredients** for names and "Dipped: ..." tooltips.
- Holding [dipping sauce](sauce.html) in your **main or off hand** automatically dips each bite eaten in this screen.

### Cold pots

A pot is **cold** when it is **not on a lit stove or lit campfire**. Broth sits still and food floats uncooked. Eating from a cold pot has a 30% chance of causing **Nausea for 15 seconds** (adjust chance/duration under "Cold-Pot Nausea" in [Configurable values](faq.html#config), or skip completely in Simple Mode / with `mode.penalties_enabled` off). Drink a [Plant Protein Drink](tableware.html#drink), Clear Broth bowl, or dip in vinegar to clear Nausea.

<!--guide-fallback:penalties_cold_off-->

## Cooking on a campfire {#campfire}{{since:0.1.1-alpha}}

Skip building a stove by placing the pot **on a lit campfire or soul campfire**:

- Slender iron supports extend automatically under the pot to rest on the campfire logs cleanly without floating or flame clipping.
- Heat equals coal with **zero fuel cost**, and still heals nearby players; the screen indicates active heating.
- Extinguishing or breaking the campfire turns the pot cold; **empty-hand Shift + right-click** packs the whole pot cleanly just like on a stove.

## Benefits of eating together {#heal}

A **heated pot** refreshes **Regeneration I** for **10 seconds** every **4 seconds** on players **within 6 blocks**. Staying by the table keeps hearts topped up. (Adjust interval, radius, duration, and level under "Pot Warmth" in [Configurable values](faq.html#config); set duration to `0` to disable.)

## Real-time minigames: Speed Eating and 4-Track Rhythm Battle {#minigames}{{since:0.3.1-alpha}}

<!--guide-flag:mode.minigames_enabled-->
Aim at a pot containing cooked food and press **[G]** (or click "**Eat Challenge**" top-right inside the hotpot screen) to launch a solo high-score run or send a **15-second duel invite** to tablemates within `8` blocks (`[G]` accepts / `[N]` declines popups; `/orientshotpot minigame` can also start solo runs, invite, accept/decline, or list tracks).

- **Food source**: by default, consumes real cooked food from the pot and triggers dipping sauces, broth buffs, hunger/saturation, and the "Gather Around the Stove" / "Table Feast" advancements; toggle "**Virtual Practice Platter**" on the challenge panel to practice without consuming food (hosts can disable this option).
- **Mode 1 · Speed Eating (Rush)**: a `10`-second frenzy hammering **Spacebar**; every `5` valid hits eats `1` cooked portion with a burst of steam. Emptying the pot awards a "Clean Plate" bonus and ends early; the server enforces a sliding-window limit via `minigame.rush_max_cps` (default `16` CPS) to block macros.
- **Mode 2 · 4-Track Rhythm Battle**: a translucent 4-track falling-note overlay using **`D / F / J / K`** by default (arrow keys also work; fully rebindable in vanilla Key Binds), judged by `rhythm_perfect_ms` (60ms) / `rhythm_good_ms` (130ms) / `rhythm_miss_ms` (220ms). Includes **3 built-in tracks** (`midnight_boil`, `bamboo_chopsticks`, `red_soup_rush`) and supports custom datapack charts under **`data/<ns>/hotpot_tracks/*.json`** (`/reload` applies immediately and syncs to multiplayer clients).
<!--/guide-flag-->
<!--guide-fallback:minigames_off-->

## Yuanyang and four-grid pots {#yuanyang}{{since:0.1.3-alpha-hotfix1}}

Combine a matching copper or iron ingot with a **nine-grid pot** of the same metal to craft a divided pot: **ingot on the left = Taiji Yuanyang; below = Round-Center Yuanyang; above = Straight-Divider Yuanyang; one on each side = Four-Grid Pot; one above and one below = Square-Center Yuanyang**. Each compartment holds **its own broth**, fills/scoops independently where you aim, and cooks using its own broth (empty compartments do not cook). They sit on the ground, campfires, short stoves, and tall stoves alike.

| Pot | Divider | Broths | Capacity per compartment |
| --- | --- | --- | --- |
| Taiji Yuanyang | S-shaped yin-yang curve | 2 | 28 each side |
| Round-Center Yuanyang | Circular center ring | 2 | 44 outer, 9 center |
| Straight-Divider Yuanyang | Straight center partition | 2 | 36 each side |
| Four-Grid Pot | Cross partition (4 zones) | 4 | 16 per zone |
| Square-Center Yuanyang | Circular ring with square center | 2 | 44 outer, 9 center |

<!--guide-collapse-->
:::warn
In 0.1.4-alpha, S-shaped and O-shaped pots were renamed **Taiji Yuanyang** and **Round-Center Yuanyang** along with their item IDs. **Those four renamed pots already placed in 0.1.3 or earlier worlds will disappear** (two each in copper and iron).
:::
<!--/guide-collapse-->

Open the screen with bamboo chopsticks (Shift + right-click). Small slots fill outward from the pot walls; **gaps near dividers too small for full slots remain open** showing simmering broth and pot bottom. Each compartment floats ingredients across dedicated spots (Taiji: 4 per side; Round/Square-Center: 6 outer + 2 inner; Straight: 3 per side; Four-Grid: 2 per zone), cycling turns to float to the surface.

Inside the screen, **double-clicking matching dishes or ladling with a bowl collects across compartments sharing the same broth** — if broths differ, only the side you clicked is collected.

{{recipe:copper_taiji_yuanyang_hotpot,copper_round_yuanyang_hotpot,copper_split_yuanyang_hotpot,copper_four_grid_hotpot,copper_square_yuanyang_hotpot}}

Iron divided pots use identical patterns with iron ingots and an iron nine-grid pot.

## Mining and removal

- **Mine the pot**: only the pot breaks, packing broth and contents into one item restored upon placement; break debris, dropped pot, and spilled items spawn at the pot's true height rather than the stove base, and mining the upper section of a tall stove emits matching pot or brick particles. Middle-clicking in Creative also copies the packed pot intact.
- Standalone pots **stay suspended even over air** without popping off.
- **Mine the stove bricks**: mines the whole stove and drops the packed pot too.
- Pot tooltips display stored **broth** and **portion counts** (how many are cooked).
- To lift a pot off a stove without breaking bricks: **Shift + right-click the pot with an empty hand**.
