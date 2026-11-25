<!-- 作者：mryt233；英文翻译，原文：mryt233。 -->
<!-- mryt233 改（2026-11-26）：按 0.3.2-alpha 更新三种备菜架通用摆放、分物品叠放与潜行换材质分流说明。 -->
# Tables, Prep Racks & Seating

Hotpot belongs around a table. The mod provides **low hotpot tables** ({{i:hotpot_table}}, ground height) and **tall hotpot tables** ({{i:tall_hotpot_table}}, 1.5 blocks tall), **three prep racks** (floor, desktop, and hanging), plus **two styles of wooden seats** across 11 woods:

{{icons:hotpot_table,tall_hotpot_table,multi_tier_prep_rack,desktop_prep_rack,hanging_prep_rack}}

- **40 legacy tables consolidated into 2 universal tables**{{since:0.3.1-alpha}}: the earlier 11 wood low tables, 11 wood tall tables, and 18 stone/metal tables have been unified into **{{i:hotpot_table}}** and **{{i:tall_hotpot_table}}**, crafted with any vanilla planks (`#minecraft:planks`) and styled freely by **swapping materials**.
- **All hotpot tables are uniformly non-flammable**: even wooden-styled tables never catch fire from nearby heat.
<!--guide-collapse-->
- **Lossless legacy save migration**: loading older worlds seamlessly upgrades **placed legacy table blocks** (on chunk load) and **inventory legacy table items** (on join) while preserving their original look; **legacy tables in chests** remain intact and automatically upgrade the moment they are placed or enter a player's inventory — no lost blocks or purple-black textures.
<!--/guide-collapse-->

## Low hotpot tables

{{img:hotpot-low-table.webp|Low hotpot table + short stove + stools}}

{{recipe:hotpot_table}}

- Tabletop sits flush with a **short stove's bricks**, letting the stove nest seamlessly inside a table ring.
- Stepped underside recesses 4 pixels along outer edges for knee room.
- **Auto-connects**: joins adjacent tables (low or tall) and stoves automatically, and seals inner brick legs and tabletop edges cleanly on 3×3 rings or inside corners with zero hollow gaps.
- **Uniformly non-flammable** with hardness **2.0** (breakable empty-handed), while mining speed, break particles, and step/hit sounds dynamically match the applied part material.

## Tall hotpot tables

{{img:hotpot-tall-table.webp|Tall hotpot table + tall stove + benches}}

{{icons:tall_hotpot_table}}

{{recipe:tall_hotpot_table}}

- **1.5 blocks tall**, with an overhanging top and 4-pixel recessed legs for legroom.
- Built on a lower **brick** section topped with planks (crafted from 3 vanilla bricks + 3 any planks).
- Pairs with a **tall stove**: facing a stove's firebox automatically removes bricks on that side so flames stay visible.
- Connects seamlessly to adjacent low tables, tall tables, and stoves, and stays **non-flammable** (hardness 2.0, empty-hand breakable).
- Taller hitbox lets you **directly target tableware resting on top** without misclicking behind the table.

### Placing items on tables {#decor}

- **Plates, bowls, drinks, and sauce bowls** sit neatly on tabletops — see [Tableware & Drinks](tableware.html).
- **Small decorations** (lanterns, flower pots, candles, cakes, and `table_decorations` blocks such as {{i:desktop_prep_rack}}) snap flush to the tabletop instead of floating.
- Other full blocks float 5 pixels above low tables or embed 7 pixels into tall tables for decorative variety.

## Swapping tabletop and base materials {#swap_materials}{{since:0.3.1-alpha}}

{{icons:minecraft:oak_planks,minecraft:bricks,minecraft:quartz_block,minecraft:smooth_stone}}

Once you craft a universal **Hotpot Table** or **Tall Hotpot Table**, customize its **tabletop** and **base** independently on the spot (**works with opaque solid blocks from other mods too**):

1. **Per-part replacement (1-in-1-out refund)**: hold any eligible solid block (full opaque cube, non-emissive, no block entity, not blacklisted) and **right-click without sneaking**:
   - Aim at the **upper section** (top surface & apron rim) to replace the **tabletop** material;
   - Aim at the **lower section** (legs / base) to replace the **base** material (on tall tables, swaps both middle band and bottom brick base together);
   - Consumes 1 held block; if that part already carried a custom material, the previous block is **returned 1-for-1 losslessly** (default built-in oak planks / bricks do not drop extra blocks).
2. **Sneak controls**:
   - **Sneak + empty-hand right-click** a part: **ejects** its custom material block back to you and restores the default look;
   - **Sneak + block right-click**: bypasses material swapping so you can build against the table normally.
3. **Mining feedback & retention**: mining speed, break particles, and sounds match the targeted part's material — mining the top spills tabletop particles while mining the base spills base particles — while retaining hardness **2.0** (breakable empty-handed); breaking or middle-clicking preserves swapped appearances.
<!--guide-collapse-->
4. **Migrated legacy tables**: appearances migrated from the 40 older preset tables remain intact, but **cannot be popped out with sneak + empty hand** (to prevent item duplication); hold any new solid block and right-click directly to override.
<!--/guide-collapse-->
5. **Material scope config**: adjust `table_materials.mode` in Config ("Tables & Rack Materials") or JSON:
   - `any_solid` (default): any full opaque non-container block matching allow/deny tags (`table_materials_allow` / `table_materials_deny`);
   - `tag_only`: restrict strictly to the allowlist tag;
   - `off`: disable material swapping.

## Stone and metal hotpot tables {#stone}{{since:0.1.4-alpha}}

{{icons:minecraft:smooth_stone,minecraft:polished_granite,minecraft:polished_deepslate,minecraft:quartz_block,minecraft:cut_copper}}

The **9 stone and metal styles** introduced in 0.1.4-alpha (smooth stone, polished andesite / diorite / granite / deepslate / blackstone, quartz, cut copper, and iron — formerly 18 separate tables) are now unified into the **per-part material swapping** system in **0.3.1-alpha**: craft {{i:hotpot_table}} or {{i:tall_hotpot_table}} and right-click the tabletop with the desired stone or metal block. Legacy stone/metal tables already placed or stored in old saves upgrade automatically with their looks intact.

## Three prep racks {#prep_racks}{{since:0.3.1-alpha}}

{{icons:multi_tier_prep_rack,desktop_prep_rack,hanging_prep_rack}}

All three prep racks support **GUI-less slot-targeted direct placement and retrieval**, and starting in **0.3.2-alpha** they are no longer restricted to ingredients and plates — **any non-empty item** can be placed on display with per-category stacking styles:

- **{{i:multi_tier_prep_rack}} (6-slot floor rack)**: 3-tier iron-and-wood floor frame with 2 slots per shelf (**6 slots total**). Ingredients stack up to **16** per slot by default (arranged compactly front-to-back), plates/bowls stack upward (default 4), drinks and seasoning bottles stand upright side-by-side (default 4), and blocks/large items display singly facing forward.
- **{{i:desktop_prep_rack}} (4-slot desktop rack)**: two-step tabletop organizer (2 lower front slots + 2 upper rear slots, **4 slots total**). Tagged under `table_decorations`, so placing it on a low or tall hotpot table automatically **raises or lowers it flush with the tabletop**; ingredients stack up to **12** per slot by default.
- **{{i:hanging_prep_rack}} (4-slot dual-mode hanging rack)**: **4 iron hooks** that place as **`wall` (wall-mounted)** on block sides or **`ceiling` (ceiling-mounted)** under ceilings. `#rack_hangables` kitchenware (iron kitchen knife, rolling pin, bamboo chopsticks, wooden press lid, shears, buckets) and **all 3 cloth aprons** ({{i:cloth_apron}}, {{i:blue_print_cloth_apron}}, {{i:brown_trim_cloth_apron}}) still hang vertically tucked against the hooks, while all other items scale down and display upright as single items (large blocks won't hang upside down).

**Controls & interaction routing (0.3.2-alpha+)**{{since:0.3.2-alpha}}:
1. **Placing & taking items**: **normal right-click** places **1 item** into the targeted slot (merging automatically when the item and its data match; if the targeted slot is full or holds a different item, it looks for the nearest mergeable slot first, then the nearest empty slot); **empty main-hand right-click** takes **1 item** back (even if your off hand holds a shield, torch, etc., without accidentally storing your off-hand item; when the entire rack is empty, off-hand interactions pass through as usual).
2. **Material swapping & building against racks**: **sneak + right-click with an eligible solid block** swaps the rack frame material (1-in-1-out lossless refund, controlled by `mode.rack_swap_materials` and `table_materials.mode`); **sneak + right-click with both hands empty** ejects the custom material; **sneak + right-click with non-material items** such as torches, lanterns, or another prep rack places them against the rack normally instead of storing them inside.
3. **Middle-click & breaking**: middle-clicking an item in a slot copies that item directly, while middle-clicking the frame copies the rack itself with its custom material intact; breaking a rack clears its slots before dropping all stored items in valid stacks alongside the rack.
4. **Datapack & config customization (3-layer data separation)**: datapacks at `data/<namespace>/prep_rack_rules/*.json` can fine-tune stack limits, display modes (`row_depth` / `flat_pile` / `upright` / `single` / `hanging`), visual counts, and scale/rotation by exact item, item tag (`#rack_foods`, `#rack_plates`, `#rack_bottles`, `#rack_hangables`, etc.), or category (hot-reloaded via `/reload` and synced to multiplayer clients; lowering limits keeps existing items in older saves untouched). In Config under "Advanced → Prep Racks", you can also adjust the fallback slot capacity `rack.rack_default_max_stack` (default 12) and max items drawn per slot `rack.rack_max_visual_items` (default 8, range 1–16; lower values improve performance without reducing actual capacity).

<!--guide-fallback:rack_swap_off-->

{{recipe:multi_tier_prep_rack,desktop_prep_rack,hanging_prep_rack}}

## Stools and benches

| Seat | Height | Output per craft |
| --- | --- | --- |
| {{i:oak_stool}} | Low (~7/16 block seat height) | 3 |
| {{i:oak_bench}} | One block tall, level with tabletop (1.25 seat height) | 3 |

{{recipe:oak_stool,oak_bench}}

Both come in all 11 wood types by swapping the matching plank species.

### Placement

- **Facing**: placed facing the same direction as the player.
- **Bench rows**: two aligned seats of the same height **auto-join into a long bench** (removing inner legs and inset outer legs; perpendicular seats stay separate), up to **3 blocks long**, regardless of placement direction.
- Benches feature tilted "A"-frame legs with a clean full-width hitbox so walking beside them never snags.

### Stacking benches into ladders {#climb}{{since:0.2.2-alpha}}

Each bench is 1 block tall, so stacked benches double as climbable stairs.

1. **Right-click an existing bench while holding another bench (no sneaking)** to automatically stack onto the **very top** of that column, consuming 1 bench.
   - Aiming directly at the **top face** does the same.
   - Stacks up to **16 layers high**.
2. Stacks of **2 or more layers** turn into a **climbable ladder** from any side by walking forward or jumping (single benches never climb accidentally).
3. **Top bench remains sittable** with an empty hand; covered middle benches cannot be sat on.
4. Stacking onto an occupied bench gently dismounts the seated player first.
5. Mining removes one bench at a time and drops anyone sitting on it safely.

{{icons:oak_bench,bamboo_bench}}

:::tip
Stack and break freely: build high shelves or cozy "upper bunks" beside the pot. Stacking benches onto benches or climbing a bench tower unlocks the "**Climbing Higher**" advancement.
:::

### Sitting {#sit}

1. **Right-click a seat with an empty hand** (or any non-block item) to sit down.
2. Click the **center** to sit centered, or click the **seam between two joined seats** to sit astride the joint.
3. Players leave **at least one full block** of spacing so they never clip together.
4. Press **Shift (sneak)** to dismount behind the bench.

:::tip
Recommended pairings: **benches + tall tables + tall stoves**, and **stools + low tables + short stoves** so heights align cleanly.
:::
