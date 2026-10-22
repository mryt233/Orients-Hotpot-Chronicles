# 材质贴图生成手册

用 [gen_image.py](gen_image.py) 调用中转站的 GPT 图像模型，为本模组生成 Minecraft 风格贴图。

## 1. 提示词结构

每次发给模型的提示词由三段拼成，前两段是固定文件，**只有第三段需要每次写**：

```text
通用提示词            tools/prompts/base.txt      所有贴图共用的 MC 像素风规则，自动加上
+ 类型补充（可选）     tools/prompts/<类型>.txt     按 --type 选择，自动加上
+ 单独补充            命令行里写的那句话            这个物品/方块具体长什么样
```

`base.txt` 里的 `{size}` 会自动替换成 `--pixel` 的值（默认 16）。

想看最终拼出来的完整提示词、但不调用 API（不花钱）：

```bash
python tools/gen_image.py "<单独补充>" --type item --pixel 32 -o x.png --show-prompt
```

### 1.1 通用提示词（base.txt）

```text
Minecraft texture, authentic vanilla Minecraft art style, designed on an exact {size}x{size} pixel grid where every pixel is a large crisp square, the whole image is the {size}x{size} grid scaled up evenly. Flat colors, limited palette of about 12 colors, simple 2-3 tone shading with light from the top-left. Bold readable shapes, main parts at least 2 pixels thick, separate parts divided by at least 2 pixels of space. No gradients, no anti-aliasing, no blur, no dithering noise, no text, no watermark, no border, no perspective, orthographic flat 2D view.
```

要调整全局风格（比如整体更暗、颜色更少），只改这个文件。

### 1.2 类型补充

| `--type` | 用途 | 内容要点 |
| --- | --- | --- |
| `tile` | 可平铺的方块面：砖、木纹、石头 | 铺满画面、四边无缝平铺、没有中心主体 |
| `face` | 有构图的方块面：灶门、锅侧面 | 铺满画面、最外一圈用基础材质，方便和相邻方块衔接 |
| `top` | 俯视顶面：锅口、汤面 | 正上方俯视、内容是 1–2 像素的简单色块 |
| `item` | 物品图标、食材 | 单个物体居中、深色描边、透明背景（自动开启 `--transparent`） |

要新增类型，在 `tools/prompts/` 下放一个新的 `.txt` 文件即可，`--type` 会自动认出来。

### 1.3 单独补充怎么写

只写**这个东西本身**：形状、材质、颜色、朝向、关键特征。风格规则已经在通用提示词里，不要重复。用英文写，短句即可。

| 要做的东西 | 命令 |
| --- | --- |
| 筷子 | `"a pair of wooden chopsticks with red lacquered tips, lying diagonally from bottom-left to top-right like vanilla tools" --type item --pixel 32` |
| 灶台侧面 | `"red clay bricks with light grey mortar lines, slightly soot-stained" --type tile --pixel 16` |
| 灶台正面（点燃） | `"front of a brick stove with an arched firebox opening in the lower half, bright orange flames and embers inside" --type face --pixel 16` |
| 铜锅侧面 | `"polished copper pot wall with a darker horizontal rim band at the top and two small riveted handles" --type face --pixel 16` |
| 红汤汤面 | `"round copper pot rim filled with bright red spicy broth, floating dried chili pieces and orange oil droplets" --type top --pixel 16` |
| 新食材：肥牛卷 | `"a rolled slice of raw beef, red meat with white fat marbling stripes" --type item --pixel 16` |
| 新食材：熟肥牛卷 | `"same item, cooked version: grey-brown meat, slightly shrunk" --type item --pixel 16 --ref tools/out/beef_raw.png` |

## 2. 环境配置

脚本只从环境变量读取配置，密钥不进仓库：

| 变量 | 示例 | 说明 |
| --- | --- | --- |
| `IMAGE_API_BASE_URL` | `https://xxx.com/v1` | 中转站地址，通常带 `/v1` |
| `IMAGE_API_KEY` | `sk-...` | 中转站密钥 |
| `IMAGE_MODEL` | `gpt-image-2.5-flare` | 不填默认 `gpt-image-1` |

在 Windows「编辑账户的环境变量」里添加，改完需要重启 Claude / 终端。

## 3. 脚本参数

| 参数 | 作用 |
| --- | --- |
| `--type` | 选择类型补充：`tile` / `face` / `top` / `item` |
| `--pixel N` | 缩成 N×N（方块一般 16，本模组筷子物品是 32） |
| `--colors N` | 缩小后限制调色板数量，12–24 比较像原版 |
| `--ref a.png b.png` | 参考图（走 `/images/edits`），让风格和配色贴近现有贴图 |
| `--transparent` | 透明背景（`--type item` 时自动开启） |
| `--show-prompt` | 只打印拼好的完整提示词，不调用 API |
| `--no-base` | 不加通用提示词，原样发送命令行里的提示词 |
| `--size` | 请求给 API 的尺寸，默认 `1024x1024` |

缩小前的原图保存在 `tools/out/<名字>.raw.png`（已被 git 忽略），效果不满意可以离线重新缩放，不用重新花钱生成。

## 4. 工作流程

1. **先输出到 `tools/out/`**，不要直接写进 `src/`。
2. 放大预览（最近邻放大 8–16 倍）检查：轮廓是否清晰、相邻部件有没有粘连、平铺有没有接缝。
3. 不满意就改「单独补充」，或在末尾追加第 6 节的反面提示后重新生成；多次出现的共性问题再改 `base.txt`。
4. 满意后复制到资源目录。**本模组的客户端资源有两份，必须同步**：
   - `src/main/resources/assets/orients_hotpot_chronicles/textures/...`
   - `src/main/resources/resourcepacks/hotpot_assets/assets/orients_hotpot_chronicles/textures/...`（若该路径下存在同名文件）
5. `./gradlew build` 后进游戏看实际效果；方块贴图在游戏光照下会比预览暗一些。

## 5. 现有贴图规格

| 类别 | 尺寸 | 备注 |
| --- | --- | --- |
| 方块面（锅侧面、灶台、桌子） | 16×16 | 不透明 |
| 锅口汤面（静止） | 16×16 | |
| 锅口汤面（沸腾动画） | 16×128 | 8 帧竖排，配 `.png.mcmeta`：`frametime: 3`、`interpolate: true` |
| 筷子物品 | 32×32 | 透明背景 |

动画帧不建议让模型直接生成（帧与帧之间很难对齐）：先用 `--type top` 生成一张静止帧，气泡翻滚由脚本在它上面逐帧绘制，再拼成 16×128。

## 6. 反面提示（出问题时追加到单独补充末尾）

| 问题 | 追加 |
| --- | --- |
| 缩小后部件粘连、细节糊掉 | `fewer details, larger shapes, as simple as vanilla Minecraft` |
| 有阴影或背景 | `no drop shadow, no background, no floor` |
| 出现 3D 透视 | `strictly flat 2D, no isometric, no 3D` |
| 颜色太艳 | `muted earthy colors like vanilla Minecraft` |
| 出现文字或签名 | `absolutely no letters, numbers or signatures` |
