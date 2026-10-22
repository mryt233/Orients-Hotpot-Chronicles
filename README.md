<!-- （2026-10-22，按 promo/GitHub编写手册.md 编写，对应 0.1.0-alpha.1）。由 放到仓库根目录。 -->

<div align="center">

<img src="src/main/resources/assets/orients_hotpot_chronicles/icon.png" width="128" alt="icon">

# 之晗的火锅物语 · Orient's Hotpot Chronicles

在 Minecraft 里吃一顿九宫格火锅：砌灶台、坐上锅、倒汤底、点火，把食材下锅涮熟，再用筷子夹着吃。

Cook a nine-grid hotpot in Minecraft: build a stove, seat a pot on it, pour in a broth, light the fire, cook your food and eat it with chopsticks.

Minecraft 1.21.1 · Fabric · 作者 / Author：蘧白川 / Orient（GitHub：[@mryt233](https://github.com/mryt233)）

</div>

> [!WARNING]
> **测试版（alpha）**：这是第一个公开测试版，可能有 bug。请先在新存档里试玩，并备份你的存档。
>
> **Alpha version**: this is the first public test build and may contain bugs. Please try it in a new world first and back up your saves.

## 截图 · Screenshots

![一桌火锅：高灶台、高火锅桌、条凳和摆满菜的盘子 · A full hotpot table with a tall stove, tall tables, benches and plates of food](docs/images/hotpot-tall-table.webp)

![矮火锅桌、板凳、条凳和各种盘子 · Low hotpot table with a stool, a bench and plates](docs/images/hotpot-low-table.webp)

![灶台上的铁锅和高灶台上的铜锅，红汤正在沸腾 · Iron pot on a stove and copper pot on a tall stove, spicy broth boiling](docs/images/stoves-boiling.webp)

<!-- 截图：九宫格锅界面 -->
<!-- 截图：盘子里叠着的菜（特写） -->
<!-- 截图：创造模式物品栏分类页 -->

## 特色 · Features

- **灶台和锅**：灶台用原版熔炉燃料烧火，铁锅、铜锅坐在灶台上煮。还有 1.5 格高的高灶台，配高火锅桌用。
- **四种汤底**：清汤（水桶）、浓汤（牛奶桶）、菌汤（蘑菇煲）、红汤（岩浆桶）。汤底不同，煮的快慢也不同：红汤最快，浓汤最慢。
- **9×9 = 81 格的九宫格锅**：生食材下锅，按熔炉配方的时间煮熟，熟了拿出来能得到和熔炉一样的经验。
- **竹筷**：在锅界面里用筷子左键点熟食，直接吃掉；也能从盘子里夹菜、把菜放进锅里。
- **趁热吃**：锅不在烧着的灶台上就是冷锅，吃冷锅里的东西有概率反胃。
- **围锅取暖**：灶台烧着火、上面有锅时，6 格内的玩家获得生命恢复 I。
- **不白烧燃料**：只有在煮生食材时才消耗燃料，光烧着保温不耗。
- **整锅带走**：挖下来的锅连汤带菜，放下来原样都在。
- **火锅桌**：11 种原版木头，各有矮桌和高桌，互相拼接，也能和灶台拼成一整张大桌。
- **板凳和条凳**：11 种木头，同方向挨着放会连成长凳，能坐。
- **盘子**：石英、竹编、红陶、铜、铁 5 种材质 × 6 种大小，摆在桌上放菜，一格能叠 3 样。
- **毛肚**：牛和哞菇会掉生毛肚，在锅里涮得比牛肉快。
- **10 种语言**：简体中文、繁体中文、英语、日语、韩语、俄语、德语、法语、西班牙语、葡萄牙语（巴西）。

---

- **Stove and pots**: the stove burns vanilla furnace fuel; seat an iron or copper pot on it to cook. A 1.5-block Tall Brick Stove matches the tall hotpot tables.
- **Four broths**: Clear (water bucket), Milky (milk bucket), Mushroom (mushroom stew), Spicy (lava bucket). Each broth cooks at a different speed: Spicy is the fastest, Milky the slowest.
- **A 9×9 = 81-slot nine-grid pot**: raw food cooks in the time its furnace recipe takes, and gives the same XP as a furnace.
- **Bamboo Chopsticks**: in the pot screen, left-click cooked food with the chopsticks to eat it on the spot. They also pick food up from plates and put food into the pot.
- **Eat it hot**: a pot that is not on a lit stove is a cold pot. Eating from a cold pot may give you Nausea.
- **Warm up around the pot**: a lit stove with a pot on it gives Regeneration I to players within 6 blocks.
- **No wasted fuel**: fuel is only used while raw food is cooking. Keeping the pot hot costs nothing.
- **Take the whole pot with you**: a mined pot keeps its broth and food. Place it down and everything is still there.
- **Hotpot tables**: 11 vanilla wood types, each in a low and a tall version. Tables connect to each other and to the stove into one big table.
- **Stools and benches**: 11 wood types. Placed side by side facing the same way, they join into a long bench you can sit on.
- **Plates**: 5 materials (quartz, bamboo, terracotta, copper, iron) × 6 sizes. Put them on the table and stack up to 3 foods in each slot.
- **Tripe**: cows and mooshrooms drop Raw Tripe, which cooks in the pot faster than beef.
- **10 languages**: English, Simplified Chinese, Traditional Chinese, Japanese, Korean, Russian, German, French, Spanish, Portuguese (Brazil).

## 版本支持 · Compatibility

| 项目 · Item | 要求 · Required | 作者测试用的版本 · Tested with |
| --- | --- | --- |
| Minecraft | 只支持 **1.21.1** · **1.21.1** only | 1.21.1 |
| 加载器 · Loader | **Fabric**（没有 Forge / NeoForge 版 · no Forge / NeoForge version） | — |
| Fabric Loader | **0.15.11** 或更新 · or newer | 0.19.3 |
| Fabric API | **1.21.1 版**，建议 0.116.14+1.21.1 或更新 · for 1.21.1, 0.116.14+1.21.1 or newer recommended | 0.116.14+1.21.1 |
| Java | **21** 或更新 · or newer | 21 |
| 安装位置 · Side | **客户端和服务端都要装** · **both client and server** | — |

### 和其他模组的兼容性 · Compatibility with other mods

**不详。** 目前还在适配，还没有专门测试过和其他模组一起装的情况。如果你发现和某个模组冲突（崩溃、显示不对、功能失效），欢迎[反馈](#反馈--feedback)！

**Unknown.** Still being worked on; it has not been tested together with other mods yet. If you run into a conflict with another mod (crash, visual glitch, broken feature), please [let me know](#反馈--feedback)!

## 安装 · Installation

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（Minecraft 1.21.1，Loader 0.15.11 或更新）。
2. 下载 [Fabric API](https://modrinth.com/mod/fabric-api)（1.21.1 版），放进 `mods` 文件夹。
3. 从 [Releases](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) 下载本模组的 jar，放进 `mods` 文件夹。

需要 **Java 21**。**客户端和服务端都要装。**

---

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1 (Loader 0.15.11 or newer).
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1 and put it in your `mods` folder.
3. Download this mod's jar from [Releases](https://github.com/mryt233/Orients-Hotpot-Chronicles/releases) and put it in your `mods` folder.

Requires **Java 21**. **Install it on both the client and the server.**

## 怎么玩 · How to play

<details>
<summary><b>展开中文玩法</b></summary>

1. **摆灶台、坐锅**：拿锅右键灶台顶面，锅的两只耳朵会朝向你的左右两边。
2. **倒汤底**：拿着水桶、牛奶桶、蘑菇煲或岩浆桶右键锅。倒进去之后不能直接换，要先用空桶右键把汤舀回来（菌汤用碗舀，而且锅里要先没有菜）。锅里有菜时舀汤，菜会掉出来。
3. **点火**：打开灶膛放燃料。锅里有汤、灶膛有燃料就会点着、沸腾。只有在煮生食材时才消耗燃料。
4. **下菜**：
   - 拿食材右键锅；或者用筷子打开锅界面，往 81 格里放；
   - 筷子夹着菜右键锅，菜会放进准星对着的那个大格；
   - 拿着装了菜的盘子右键锅，生食材会整盘倒进去，随机分到各个大格，熟的留在盘子里。

   能下锅的是生的、能吃的东西，熟了的不能再下锅。
5. **吃**：在锅界面里把筷子拿到鼠标上，左键点熟食就能吃掉，饱食度和原版一样。空手或拿碗右键锅，也能把熟食捞出来。
6. **趁热吃**：锅不在烧着的灶台上就是冷锅，吃冷锅里的东西有概率反胃。
7. **围锅取暖**：灶台烧着火、上面有锅时，6 格内的玩家获得生命恢复 I。
8. **整锅带走**：对着锅挖只挖锅，对着灶身挖会挖掉整个灶台。汤和菜都装在锅物品里，放下来原样都在，物品提示里写着汤底和份数。
9. **盘子**：
   - 拿盘子右键桌面或地面摆上去，一格能摆好几个（比如 5 个小碟、2 个长盘，或者混着摆）；
   - 拿食物右键盘子放菜，每一格能叠 3 样；
   - 筷子右键盘子，夹走最后放上去的那样，再右键放回盘子或放进锅；
   - 空手右键拿起整个盘子，菜跟着走；潜行 + 空手右键一次取出一样菜；
   - 挖只挖准星对着的那个盘子；鼠标中键能连菜一起复制盘子；
   - 装了菜的盘子，物品提示里会像收纳袋一样列出里面的菜。
10. **坐**：空手右键板凳或条凳坐下，点两块凳子的交界处能坐在中间，潜行起身。两个人之间至少隔一整格。
11. **毛肚**：打牛或哞菇掉 1～3 个生毛肚（和原版牛肉一样：抢夺能多掉，着火时掉熟的）。

创造模式里有单独的分类页「之晗的火锅物语」，左边能切换子类：全部、厨具、餐具、桌子、凳子、食物。

</details>

<details>
<summary><b>How to play (English)</b></summary>

1. **Stove and pot**: right-click the top of a stove with a pot. The pot's two handles face your left and right.
2. **Broth**: right-click the pot with a water bucket, milk bucket, mushroom stew or lava bucket. You can't swap a broth directly: scoop it out first by right-clicking with an empty bucket (use a bowl for mushroom broth, and only when the pot has no food in it). If you scoop broth while there is food in the pot, the food drops out.
3. **Fire**: open the stove and put in fuel. With broth in the pot and fuel in the stove, it lights and boils. Fuel is only used while raw food is cooking.
4. **Adding food**:
   - right-click the pot with food, or open the pot screen with chopsticks and put food into the 81 slots;
   - right-click the pot with chopsticks holding food to put it into the big cell you are aiming at;
   - right-click the pot with a plate of food to pour all the raw food in, spread randomly across the big cells. Cooked food stays on the plate.

   Only raw, edible items can go in. Cooked food can't go back in.
5. **Eating**: in the pot screen, hold the chopsticks on your cursor and left-click cooked food to eat it, with the same hunger and saturation as vanilla. Right-clicking the pot with an empty hand or a bowl also takes cooked food out.
6. **Eat it hot**: a pot that is not on a lit stove is a cold pot. Eating from a cold pot may give you Nausea.
7. **Warm up**: a lit stove with a pot on it gives Regeneration I to players within 6 blocks.
8. **Take the whole pot**: aim at the pot to mine only the pot; aim at the stove body to mine the whole stove. The broth and food are kept in the pot item and come back when you place it. The tooltip shows the broth and the number of servings.
9. **Plates**:
   - right-click a table top or the ground with a plate to place it. One block can hold several plates (for example 5 small dishes, 2 long plates, or a mix);
   - right-click a plate with food to put it on. Each slot stacks up to 3 foods;
   - right-click a plate with chopsticks to pick up the last food placed, then right-click to put it back or into the pot;
   - right-click with an empty hand to pick up the whole plate with its food; sneak + right-click with an empty hand to take out one food at a time;
   - mining only breaks the plate you are aiming at; middle-click copies a plate together with its food;
   - a plate with food lists its contents in the tooltip, like a bundle.
10. **Sitting**: right-click a stool or bench with an empty hand to sit. Click where two seats meet to sit in the middle. Sneak to stand up. Two players sit at least one full block apart.
11. **Tripe**: cows and mooshrooms drop 1–3 Raw Tripe (like vanilla beef: Looting adds more, and they drop cooked if on fire).

In Creative mode, the mod has its own tab, "Orient's Hotpot Chronicles", with sub-categories on the left: All, Cook, Dish, Table, Seat, Food.

</details>

## 合成表 · Recipes

除了熟毛肚，都在工作台合成。表里的「空」表示那一格不放东西。

All recipes use a crafting table except Cooked Tripe. "空 / empty" means leave that slot empty.

| 物品 · Item | 配方 · Recipe | 产出 · Output |
| --- | --- | --- |
| 灶台 · Brick Stove | 上排：圆石 ×3；下排：圆石、熔炉、圆石<br>Top: 3 cobblestone; bottom: cobblestone, furnace, cobblestone | 1 |
| 高灶台 · Tall Brick Stove | 灶台 + 红砖块（无序）<br>Stove + bricks (shapeless) | 1 |
| 铁制九宫格火锅 · Iron Nine-Grid Hotpot | 一排：铁锭、炼药锅、铁锭<br>One row: iron ingot, cauldron, iron ingot | 1 |
| 铜制九宫格火锅 · Copper Nine-Grid Hotpot | 上排：铜锭、空、铜锭；下排：铜锭、炼药锅、铜锭<br>Top: copper ingot, empty, copper ingot; bottom: copper ingot, cauldron, copper ingot | 1 |
| 竹筷 · Bamboo Chopsticks | 2×2 竹子<br>2×2 bamboo | 1 |
| 火锅桌 · Hotpot Table | 同种木板 ×8 围一圈，中间放 1 个红砖<br>8 planks of one wood type around 1 brick | 4 |
| 高火锅桌 · Tall Hotpot Table | 上排：同种木板 ×3；下排：红砖块 ×3<br>Top: 3 planks; bottom: 3 bricks | 3 |
| 板凳 · Stool | 上排：同种木板 ×3；下排：木棍、空、木棍<br>Top: 3 planks; bottom: stick, empty, stick | 3 |
| 条凳 · Bench | 上排：同种木板 ×3；下面两排：木棍、空、木棍<br>Top: 3 planks; next two rows: stick, empty, stick | 3 |
| 小碟 · Small Dish | 一排 2 个：下界石英 / 竹子 / 红砖 / 铜锭 / 铁锭<br>2 in a row: nether quartz / bamboo / brick / copper ingot / iron ingot | 2 |
| 双格盘 · Two-Slot Plate | 同材质小碟 ×2 横排<br>2 small dishes of the same material in a row | 1 |
| 三格长盘 · Three-Slot Long Plate | 同材质小碟 ×3 横排<br>3 small dishes in a row | 1 |
| 四格方盘 · Four-Slot Square Plate | 同材质小碟 2×2<br>2×2 small dishes | 1 |
| 五格长盘 · Five-Slot Long Plate | 上排：小碟、空、小碟；下排：小碟 ×3<br>Top: dish, empty, dish; bottom: 3 dishes | 1 |
| 八格大盘 · Eight-Slot Platter | 同材质小碟 ×8 围一圈，中间空<br>8 small dishes in a ring, center empty | 1 |
| 熟毛肚 · Cooked Tripe | 熔炉、烟熏炉或营火烤生毛肚<br>Smelt Raw Tripe in a furnace, smoker or campfire | 1 |

## 许可证 · License

**保留所有权利（All Rights Reserved），源码公开。** 本模组不是开源软件，源码公开只是方便大家查看和学习。

可以直接做（不用问作者）：
- 自己玩；
- 放进免费整合包：注明作者和下载页，只放未修改的官方 jar；
- 在免费服务器上使用：注明作者和下载页；
- 做视频、直播，可以有收益；
- 看源码学习；
- 做联动模组或附属模组（不复制本模组的代码和素材）；
- 做材质包、翻译包（只放你自己做的文件）。

要先取得作者书面同意：
- 移植到其他加载器或其他 Minecraft 版本；
- 修改后再发布；
- 在其他项目里使用本模组的代码或素材；
- 出售，或放进付费内容（包括付费整合包）；
- 在收费服务器上使用。

以上只是要点，以 [LICENSE](LICENSE) 原文为准。

---

**All Rights Reserved, source available.** This mod is not open source. The source is public for reference and learning.

You may, without asking:
- play it yourself;
- include it in free modpacks: credit the author and the download page, and include only the unmodified official jar;
- use it on free servers: credit the author and the download page;
- make videos and streams, including monetized ones;
- read the source to learn from it;
- make integration mods or add-ons (without copying this mod's code or assets);
- make resource packs and translation packs (containing only files you made).

You need the author's written permission to:
- port it to another loader or Minecraft version;
- distribute a modified version;
- use its code or assets in another project;
- sell it or include it in paid content (including paid modpacks);
- use it on a paid server.

This is only a summary. The [LICENSE](LICENSE) file is the binding text.

## 反馈 · Feedback

遇到 bug、有疑问或建议，欢迎告诉我：

- **GitHub Issues**：[提交反馈](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose)（有报 bug、提建议、提问三种表单）
- **B 站**：蘧白川
- **邮箱**：orientcheung233@gmail.com
- **QQ 交流群**：248314791

Found a bug, or have a question or an idea? Let me know:

- **GitHub Issues**: [open an issue](https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose) (bug report, suggestion, or question)
- **Bilibili**: 蘧白川
- **Email**: orientcheung233@gmail.com
- **QQ group**: 248314791

<!-- 其他下载平台：Modrinth / CurseForge / MC百科 上架后在这里补链接 -->

## 作者 · Credits

蘧白川 / Orient · GitHub：[@mryt233](https://github.com/mryt233)

<!-- 2026-10-22 按用户要求补充 AI 协作致谢，保留上方原作者署名。 -->

### AI 协作致谢 · AI assistance


