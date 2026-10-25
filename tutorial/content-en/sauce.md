<!-- 作者：；英文翻译。 -->
# Dipping Sauces

A good dipping sauce completes a hotpot meal. The mod uses a **self-service sauce station**: nine small bowls let you **mix your own sauce**. Each seasoning has its own effect, and **all effects stack**.

{{icons:oak_sauce_station,iron_sauce_station,dipping_sauce}}

## Sauce stations {#station}{{since:0.1.2-alpha}}

- **Appearance:** three ascending tiers, low at the front and high at the back, with three white porcelain bowls per tier: nine in total.
- **Height:** 1.5 blocks tall, occupying **two blocks**, including an invisible block above. You can **walk up the tiers**.
- **Materials:** 11 wood types plus iron, for 12 variants.
- **Facing:** the front faces the player who placed it.

### Recipes

Each recipe makes one station. Replace planks with iron ingots for iron, or use the corresponding planks for other woods.

{{recipe:oak_sauce_station,iron_sauce_station}}

### Restocking the bowls {#restock}

**Right-click the station with an empty hand** to open its restocking screen:

- **Nine slots** correspond to the nine bowls on the station.
- **Each slot accepts one ingredient type.** The ingredient placed in an empty slot determines its seasoning. Multiple slots can hold the same ingredient.
- **One ingredient = four servings** of seasoning. A slot holds up to 16 ingredients, or 64 servings.
- The **?** in the top-right opens an **ingredient reference**, showing which ingredient makes which seasoning.
- Stocked bowls visibly show their seasoning on the station.

## Nine seasonings {#seasoning}

| Seasoning | Ingredient | Effect when eaten |
| --- | --- | --- |
| MSG | {{i:minecraft:wheat}} | Restores two extra hunger points |
| Chili oil | {{i:minecraft:blaze_powder}} | Fire Resistance for 10 seconds |
| Sweet-and-sour sauce | {{i:minecraft:sweet_berries}} | Speed for 10 seconds |
| Honey | {{i:minecraft:honey_bottle}} | Clears spice-induced Hunger and Poison |
| Sesame oil | {{i:minecraft:sunflower}} or {{i:minecraft:pumpkin_seeds}} | Regeneration for five seconds |
| Vinegar | {{i:minecraft:apple}} or {{i:minecraft:beetroot}} | Clears Nausea and spice-induced Hunger |
| Sugar | {{i:minecraft:sugar}} | Haste for 10 seconds |
| Chopped scallions | {{i:scallion}} | Flavor only; no effect |
| Chopped cilantro | {{i:cilantro}} | Flavor only; no effect |

“Cooling the spice” means clearing the Hunger effect from Spicy Red Broth. **Every seasoning in your bowl contributes its effect when you eat dipped food.**

## Mixing a sauce {#make}

1. **Hold a vanilla bowl** ({{i:minecraft:bowl}}) and **right-click a stocked seasoning bowl** on the station. It takes one serving and becomes **Dipping Sauce**.
2. Want another seasoning? **Hold that sauce bowl and right-click a different seasoning bowl** to add it.
3. Rules:
   - **Each seasoning can be added only once per bowl.**
   - One sauce bowl lasts for **nine dips**.
   - After using some dips, you can still add **seasonings you have not added yet**. **The remaining dip count stays the same.**
   - An empty station bowl shows an “empty” message; restock it first.
4. The tooltip lists the seasonings and remaining dips.

:::tip
Try **sesame oil + vinegar + chopped scallions + chopped cilantro** for Regeneration, relief from spice and Nausea, and a fresh onion flavor. Pair Spicy Red Broth with **honey** or **vinegar** to clear its spice effect.
:::

## Dipping food {#dip}

There are three ways:

### ① Dip at the table

1. **Place your sauce bowl on a table** by right-clicking the tabletop, just like placing a plate.
2. **Hold food with chopsticks** and right-click the sauce bowl to dip it.
3. The food now carries the sauce's effects. Its tooltip lists **“Dipped: …”**.

### ② Automatic dipping at the pot

**Hold sauce in your off hand** and eat with chopsticks in the hotpot screen. Each bite is **automatically dipped**, without taking it out first.

### ③ Eat directly with chopsticks

Hold dipped food with chopsticks and **hold right-click while aiming into the air** to eat. Its effects still apply. **However you eat it**—in the screen, with chopsticks, or from the table—dipped food keeps its effects.

Each dip uses one serving. After nine dips, the sauce bowl becomes an **empty bowl**.
