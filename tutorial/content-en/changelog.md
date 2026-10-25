<!-- 作者：；英文翻译。 -->
# Changelog

This page is updated with each release, **newest first**. The “0.1.3 Preview” section lists completed work that has not been released. **Gameplay in this guide is based on the released 0.1.2-alpha.**

:::tip
**Maintenance note (players can skip this):** each game change gets a numbered entry under “Unreleased” in `docs/移植同步清单.md`. The `<!--清单#N-->` markers here refer to those entries, and `python tools/build_tutorial.py` checks for omissions. At release time, rename the whole preview section to the release version and start a new preview section.
:::

## 0.1.3 Preview {{dev}} {#unreleased}

Completed work following 0.1.2-alpha, **coming in 0.1.3**. It is not released yet, and details may still change.

### New content

- **Beef Loin and Beef Loin Slices:** cows and mooshrooms have a 25% chance to drop loin. Cooked loin restores eight hunger points. Slice it into three on a board or in a stonecutter; these are the fastest-cooking hotpot slices. ([Ingredients & Food Preparation](ingredients.html#loin))<!--清单#13-->
- **Yuanyang and four-grid pots:** S-shaped, O-shaped, and straight-divider Yuanyang pots, plus a four-compartment pot, in copper and iron. Craft using an ingot and a nine-grid pot. Each compartment has its own broth; aim at a compartment to add or remove broth, and its food cooks in that broth. ([Stoves & Hotpots](hotpot.html#yuanyang))<!--清单#20--><!--清单#21-->

### Changes

- **Milky broth renamed to Rich Stock**, including translated names. This is the broth made with a milk bucket.<!--清单#23-->
- **Packed Yuanyang / four-grid pots:** item icons show each compartment's broth, and tooltips list broths by compartment.<!--清单#22-->
- **Pots accept only food.** Wood and iron items previously cooked into charcoal and iron nuggets; now only food can be added.<!--清单#7-->
- **Sauce stations lowered by one trapdoor's height**, making all three tiers easier to reach.<!--清单#1-->
- **Cutting boards have a facing direction**, with food facing the player placing it.<!--清单#5-->
- **Mining food on a board removes only the food.** The board remains; food and board directions are stored separately.<!--清单#11-->
- **Cutting boards are visible in hand**, and inventory icons are no longer dark.<!--清单#9--><!--清单#19-->
- **Rolling Pins** have a new design with handles.<!--清单#10-->
- **Campfire pots** have iron supports at their four corners, so they no longer appear to float and flames no longer pass through them.<!--清单#2-->
- **Food in cold pots** no longer flickers; it slowly bobs up and down on a roughly six-second cycle.<!--清单#16-->
- **Benches connect even when facing opposite ways**, as long as they are along the same line.<!--清单#17-->
- **Sauce station selection outlines** surround only the bowl under the crosshair.<!--清单#8-->
- **Standalone / campfire pots** can be picked up whole with an empty hand and Shift + right-click.<!--清单#15-->

### Fixes

- Organized recipe-book groups for stools, benches, and plates to match current item names; removed unused old files.<!--清单#24-->
- Aligned internal drink names with item names. No visible change, but **drinks placed in old test worlds may disappear**.<!--清单#25-->
- Aligned internal names for Rich Stock, seating, plate models, and other assets; removed compatibility for old world IDs. **Renamed items in old test worlds may disappear.**<!--清单#26-->
- The cutting board can process **potatoes and tofu** again; they could not be placed in 0.1.2-alpha.<!--清单#18-->
- Food no longer leaves a ghost image after a cutting board is emptied.<!--清单#6-->
- Eating with chopsticks creates food-colored crumbs instead of purple-and-black missing-texture particles.<!--清单#4-->
- Breaking a Plant Protein Drink creates particles in its own colors rather than those of Orange Drink.<!--清单#14-->
- Added the square “回” motif to the upper half of the Orange Drink bottle.<!--清单#3-->
- The top of the Plant Protein Drink bottle cap no longer flickers.<!--清单#12-->

## 0.1.2-alpha{{since:0.1.2-alpha}} {#v012}

Released on 2026-10-23. GitHub release pages group Fabric and Forge / NeoForge builds together.

### New content

- **Sauce stations** in 11 woods and iron: three tiers with nine bowls, 1.5 blocks tall. Right-click to restock; each bowl's seasoning depends on its ingredient.
- **Nine seasonings:** MSG, chili oil, sweet-and-sour sauce, honey, sesame oil, vinegar, sugar, chopped scallions, and chopped cilantro. Their effects stack. Use a bowl on the station to mix sauce; one bowl lasts nine dips.
- **Automatic dipping:** hold sauce in the off hand when eating in the hotpot screen or with chopsticks to dip automatically.
- **Scallions, cilantro, and soybeans** drop from grass.
- **Cutting Board, Iron Kitchen Knife, and Rolling Pin.**
- **Cutting:** raw beef / mutton become fatty beef / mutton rolls; potatoes become slices; tofu becomes cubes. A stonecutter works too.
- **Pounding meatballs:** use a rolling pin three times on a cutting board to make beef, pork, mutton, or fish balls, with cilantro variants too. Meatballs are small 3D spheres.
- **Craft tofu with soybeans and a water bucket.**

### Changes

- Smaller stove openings, short-stove brick textures aligned with vanilla bricks, and front-facing stove inventory icons.
- Clear Broth now has a clear, light color.
- Tall hotpot tables, wooden sauce stations, and cutting boards are now flammable.

### Fixes

- Stoves, plates, and seats missing from 0.1.0-alpha.1 worlds can now be recognized again; old stools become benches.
- Fixed middle-click copying of drinks and food on hotpot tables, and pots on stoves.
- Selection outlines now surround only the targeted object when several share a block.
- Item names and properties now appear when hovering in the hotpot and firebox screens.

## 0.1.1-alpha{{since:0.1.1-alpha}} {#v011}

Released on 2026-10-23.

### New content

- **New bowl uses:** vanilla bowls can hold food on tables, collect cooked food from pots, or serve soup. Clear Broth, Rich Stock, Mushroom Broth, and Spicy Red Broth have different effects.
- **Drinks:** Orange Drink and Plant Protein Drink. Each bottle has four sips and can be placed on tables.
- **Campfire hotpot:** pots cook on lit campfires and soul campfires at the same speed as stoves, without consuming fuel, and heal nearby players.
- **Small decorations** such as lanterns, flower pots, candles, and cakes sit flush with hotpot tabletops.

### Fixes

- Fixed jittering when standing on plates on low tables, and stuttering ingredient bobbing in old worlds.

### Notes

- Several item / block IDs were aligned with English names, including `brick_stove`, `oak_stool`, and `oak_bench`. **Items from this mod may disappear or change in worlds from 0.1.0-alpha.1. Back up first; a new world is recommended.**

## 0.1.0-alpha.1{{since:0.1.0-alpha.1}} {#v010}

Released on 2026-10-22. First public version, including:

- **Short and tall stoves, copper and iron pots**, four broths, fuel and heat rules, hotpot and firebox screens, and eating cooked food with chopsticks.
- **Healing around the pot:** heated pots refresh Regeneration I within six blocks; eating from cold pots may cause Nausea.
- **Pack the whole pot:** broth and food stay inside the pot item when picked up.
- **Low and tall hotpot tables, stools, and benches in 11 woods**, with automatic connections and seating.
- **Plates:** five materials × six sizes; multiple plates can share one block on a table.
- **Tripe:** drops from cows and mooshrooms and cooks faster than beef.
- Creative inventory categories and smaller category tabs on the left.
- Ten languages.
