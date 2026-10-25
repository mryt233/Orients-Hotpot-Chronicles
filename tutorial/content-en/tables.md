<!-- 作者：；英文翻译。 -->
# Tables & Seating

Hotpot is best shared around a table. There are **two matching sets**: a low set close to the ground, like a home hotpot table, and a tall set with 1.5-block-high tables, like a restaurant. Both come in **11 wood types**:

{{icons:oak_hotpot_table,spruce_hotpot_table,birch_hotpot_table,jungle_hotpot_table,acacia_hotpot_table,dark_oak_hotpot_table,mangrove_hotpot_table,cherry_hotpot_table,bamboo_hotpot_table,crimson_hotpot_table,warped_hotpot_table}}

Oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson, and warped. Tables use **vanilla plank textures**, and their recipes simply swap in the matching planks.

## Low hotpot tables

{{img:hotpot-low-table.webp|Low hotpot tables, a stove, and stools}}

{{recipe:oak_hotpot_table}}

- The tabletop is **level with a short stove's brick surface**, so a stove can fit seamlessly into a row of tables.
- The underside resembles an upside-down stair, recessed four pixels on the outer side to leave knee room.
- **Automatic connections:** any side next to another low or tall table, or a stove, fills in to create long tables.
- Wooden tables are flammable and can be mined quickly with an axe.

## Tall hotpot tables

{{img:hotpot-tall-table.webp|Tall hotpot tables, a tall stove, and benches}}

{{icons:oak_tall_hotpot_table}}

{{recipe:oak_tall_hotpot_table}}

- **1.5 blocks high**, with an overhanging tabletop. The legs occupy pixels 0–19 and are recessed four pixels on the outside too.
- The lower block is **vanilla bricks**, with wooden planks above it; this is why the recipe uses three brick blocks.
- Pair them with a **tall stove**. When the stove opening faces a table, the table **removes the bricks on that side and keeps the wooden part**, leaving the firebox visible.
- They also connect automatically to low tables and stoves.
- Targeting accounts for their extra height, so **plates and bowls on the tabletop can still be selected**.

### Decorating the table {#decor}

- **Plates, bowls, drinks, and sauce bowls** sit on the tabletop; see [Tableware & Drinks](tableware.html).
- **Small decorations** such as lanterns, flower pots, candles, and cakes **sit flush with the tabletop** instead of floating.
- Other regular blocks follow vanilla placement: they float five pixels above low tables or sink seven pixels into tall tables. You can still use them as decorations.

## Stools and benches

| Seating | Height | Recipe yield |
| --- | --- | --- |
| {{i:oak_stool}} | Low; sitting height about 7/16 of a block | 3 |
| {{i:oak_bench}} | One block high, with its top level with the table; sitting position raised to 1.25 blocks | 3 |

{{recipe:oak_stool,oak_bench}}

Both come in 11 wood types. Use the corresponding planks in the recipes.

### Placement

- **Facing:** seating and seated players face the direction you were looking when placing it.
- **Connecting:** adjacent seats in a straight line connect automatically when they are the same height, even with different woods. Middle legs disappear and end legs move inward. Perpendicular seats do not connect. In 0.1.2-alpha, seats must **face the same direction**; placing from the opposite side leaves them offset. From 0.1.3, they connect along the same line regardless of which side you stand on.
- Tall benches have angled A-frame legs, drawn by code because vanilla models cannot rotate at such small angles.

### Sitting down {#sit}

1. **Right-click seating with an empty hand**, or while holding something that is not a block.
2. Click the **middle of a block** to sit in the middle; click the **join between two blocks** to sit between them.
3. Leave **at least one full block between players** so they do not overlap.
4. Press **Shift (sneak)** to get up behind the seat.

:::tip
Match the heights for a comfortable meal: **bench + tall table + tall stove**, or **stool + low table + short stove**.
:::
