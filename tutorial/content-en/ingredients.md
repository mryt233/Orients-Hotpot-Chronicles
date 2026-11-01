<!-- 作者：mryt233；英文翻译，原文：mryt233。发布前由 mryt233 同步并审核。 -->
<!-- mryt233 改，2026-10-30：同步正式版 #30–#71 和未发布 #72–#82；保留原译者署名。 -->
# Ingredients & Food Preparation

Ingredients make the meal. This chapter explains **where to find them**, **how to prepare them**, and **how much hunger they restore when cooked**.

{{icons:tripe,fatty_beef_roll,mutton_roll,beef_loin_slice,potato_slice,tofu_cube,beef_meatball,fish_ball,scallion,cilantro,soybean}}

:::tip
**All raw ingredients can be cooked in the pot**, or in a furnace, smoker, or campfire. The tables below list hunger restored per cooked portion. **Cooked food cannot go back into the pot.**
:::

## Tripe {#tripe}

{{icons:tripe,cooked_tripe}}

- **Source:** cows and mooshrooms drop it using the same quantity rules as vanilla beef: 1–3 pieces, plus 0–1 per Looting level. Animals killed while burning drop cooked tripe.
- **Raw Tripe** restores two hunger points; **Cooked Tripe** restores five.
- Great for hotpot: about 2.5 seconds to cook in the pot (half the time of a whole piece of beef).

{{recipe:cooked_tripe}}

## Beef Loin and Beef Loin Slices {#loin}{{since:0.1.3-alpha-hotfix1}}

{{icons:beef_loin,cooked_beef_loin,beef_loin_slice,cooked_beef_loin_slice}}

- **Beef Loin:** cows / mooshrooms have a **25%** chance to drop an extra piece (+10% per Looting level; burning animals drop Cooked Beef Loin). Eaten raw, it restores three hunger points; cooking it whole produces **Cooked Beef Loin**, restoring eight, just like vanilla steak.
- **Beef Loin Slices**, the hotpot cut known as *diaolong*: cut a loin with a kitchen knife on a [cutting board](#board), or use a **stonecutter**, to get **three slices**. Each restores one hunger point raw or three cooked.
- Loin slices **cook fastest in the pot** (twice as fast as fatty beef rolls), finishing in just a few seconds.
- Cooked Beef Loin cannot be sliced.

{{recipe:beef_loin_slice,cooked_beef_loin}}

## Cutting board, kitchen knife, and rolling pin {#board}{{since:0.1.2-alpha}}

Craft these three tools before preparing ingredients:

### Cutting board

:::tip
**Directional placement**, **visibility while held**, **mining only the food on top**, and **brighter inventory icons** are all improvements introduced in 0.1.3-alpha-hotfix1. In 0.1.2-alpha, the board has no facing direction, is barely visible in hand, and breaks together with the food on it.
:::

{{recipe:cutting_board}}

- **Two logs of any type placed side by side horizontally** yield **two cutting boards** (any wood type).
- **Directional placement:** the board follows your placement direction and food faces you.
- **Hold something that can be cut or pounded and right-click the board** to place one item. **Right-click with an empty hand** to take it back.
- **Mine the food on top** to remove just that item in one hit, leaving the board.

### Iron Kitchen Knife

{{recipe:iron_kitchen_knife}}

A square-tipped Chinese cleaver with 250 durability. **It also works as a weapon**, dealing four damage.

### Rolling Pin

{{recipe:rolling_pin}}

Has 120 durability and is used to **pound meatballs**.

## Cutting ingredients {#cut}{{since:0.1.2-alpha}}

Place an ingredient on the board, then **right-click with the Iron Kitchen Knife** to cut:

| On the board | Cuts needed | Output | Hunger per cooked portion |
| --- | --- | --- | --- |
| {{i:minecraft:beef}} Raw Beef | 1 | 3 {{i:fatty_beef_roll}} | 3 |
| {{i:minecraft:mutton}} Raw Mutton | 1 | 3 {{i:mutton_roll}} | 3 |
| {{i:beef_loin}} | 1 | 3 {{i:beef_loin_slice}} | 3 |
| {{i:minecraft:potato}} Potato | 3 cuts, 1 portion per cut | 3 portions of {{i:potato_slice}} (three slices in each portion) | 2 |
| {{i:tofu}} Tofu | 1 | 3 portions of {{i:tofu_cube}} | 2 |

**Shortcut:** use a **stonecutter** to prepare the whole batch at once.

:::warn
In 0.1.2-alpha, **potatoes and tofu cannot be placed on the cutting board** due to a known issue (use a stonecutter instead). This is fixed in 0.1.3-alpha-hotfix1.
:::

{{recipe:fatty_beef_roll,mutton_roll,potato_slice,tofu_cube}}

## Pounding meatballs {#meatball}{{since:0.1.2-alpha}}

**Suitable ingredients:** raw beef, porkchops, mutton, cod, and salmon.

1. Place the meat on the board.
2. **Right-click the board with a Rolling Pin three times:**
   - First hit: crush the meat.
   - Second hit: turn it into a paste.
   - Third hit: produce **three raw meatballs**.
3. The meatballs are small 3D spheres. Cook them in the pot; each cooked meatball restores **three hunger points**.

| Ingredient | Raw meatball |
| --- | --- |
| Raw beef | {{i:beef_meatball}} |
| Raw porkchop | {{i:pork_meatball}} |
| Raw mutton | {{i:mutton_meatball}} |
| Raw cod / salmon | {{i:fish_ball}} |

### Cilantro meatballs

Combine two raw meatballs with one {{i:cilantro}} in a shapeless crafting recipe to get **two** cilantro meatballs. All four meatball types have cilantro variants. Each cooked one restores **four hunger points**, more than a plain meatball.

{{recipe:cilantro_beef_meatball}}

## Shrimp, peeled shrimp, and shrimp paste {#shrimp}{{since:0.1.4-alpha}}

{{icons:shrimp,peeled_shrimp,shrimp_paste}}

- **Shrimp:** caught while fishing, roughly one in six fish catches.
- **Peeled Shrimp:** cut a shrimp with a knife on the board, or use a stonecutter.
- **Shrimp Paste:** pound peeled shrimp on the board with a rolling pin 3 times.
- All three can be cooked in a pot or roasted. Cooked portions restore **3 / 3 / 4** hunger points respectively. Cooked Shrimp and Cooked Peeled Shrimp also have a small flavor line in their tooltips.

{{recipe:peeled_shrimp,cooked_shrimp_paste}}

## Minced pork and ham sausages {#sausage}{{dev}}

{{icons:minced_pork,raw_ham_sausage,ham_sausage,ham_sausage_slice,cooked_ham_sausage_slice}}

:::dev
Not released yet: **cut a raw porkchop with a knife → 1 Minced Pork; Minced Pork + 1 vanilla wheat → 3 Raw Ham Sausages; heat → Ham Sausage; cut once → 3 Ham Sausage Slices; heat slices again → Cooked Ham Sausage Slices**. Both cutting steps also work in a stonecutter. Raw sausages cannot be sliced on the board; cooked slices cannot go back into the pot. Pounding pork meatballs is unchanged.

Default hunger restored: Raw Ham Sausage **1**, Ham Sausage **3**, Ham Sausage Slice **1**, Cooked Ham Sausage Slice **2**. Minced Pork is a crafting ingredient, not directly edible. Full instructions and recipes are in the [Development Preview](preview.html#sausage).
:::

## Tofu and soybeans {#tofu}{{since:0.1.2-alpha}}

- **Soybeans:** breaking **short or tall grass** without shears has a **15%** chance to drop one.
- **Tofu:** shapelessly craft three soybeans and one water bucket into a block of tofu. **The bucket is returned.**
- One knife cut turns tofu on a board into three **Tofu Cubes**, each restoring two hunger points when cooked.

{{recipe:tofu}}

## Scallions and cilantro {#garnish}{{since:0.1.2-alpha}}

- Break **short grass or the lower half of tall grass** without shears. Scallions and cilantro each have a **25%** drop chance.
- Both **can be eaten raw** for one hunger point. They are also [dipping sauce](sauce.html) ingredients, and cilantro can make cilantro meatballs.

- Grass also drops **tomatoes** for hotpot base, **garlic** for garlic paste, and **soybeans** for tofu.
- **Cooked Kelp:** cook vanilla kelp in a hotpot to restore 2 hunger points. Furnaces, smokers, and campfires still produce dried kelp.

## Hunger reference

| Food | Raw | Cooked |
| --- | --- | --- |
| Tripe | 2 | 5 |
| Beef Loin | 3 | 8 |
| Beef Loin Slices | 1 | 3 |
| Fatty Beef Rolls / Mutton Rolls | 1 | 3 |
| Potato Slices | 1 | 2 |
| Tofu (whole) / Tofu Cubes | 2 / 1 | — / 2 |
| Beef / Pork / Mutton Meatballs / Fish Balls | 1 | 3 |
| Cilantro meatballs (four types) | 1 | 4 |
| Soybeans | 1 | — |
| Scallions / Cilantro | 1 | — |
| Shrimp / Peeled Shrimp / Shrimp Paste | — | 3 / 3 / 4 |
| Cooked Kelp | — | 2 |

:::tip
Duck blood, luncheon meat, aorta, wide noodles, lotus root slices, enoki mushrooms, dried tofu skin, leafy greens, and other hotpot favorites are still planned. As they are added, this guide will be updated too.
:::
