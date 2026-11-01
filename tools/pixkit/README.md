# pixkit：贴图 / 模型生成公共库

作者：mryt233（2026-11-01）

以前每个 `gen_*.py` 各写一份 `mix` / `shade` / `outline` / `write`，同名函数的算法还不完全一样。pixkit 把它们收在一起，**每种算法单独命名**，迁移旧脚本时选对应的那个，生成结果逐字节不变。

脚本放在 `tools/` 下时直接 `from pixkit import ...`（Python 会从脚本所在目录找）。

## 模块一览

### `pixkit.paths`

| 名字 | 是什么 |
| --- | --- |
| `MOD` / `MODID` / `RES` | `mod/` 目录、模组 ID、`common/src/main/resources` |
| `ASSETS` / `DATA` / `ROOTS` | `assets/<modid>`（资源只有这一份）、`data/<modid>`、`[ASSETS]` |
| `VANILLA` | 原版资源（查看器缓存 `viewer/cache/1.21.1/assets/minecraft`） |
| `PREVIEW` / `OUT` | 预览目录（配合 `PREVIEW_ROOT`，不装进模组）、`tools/out` |

### `pixkit.color`

| 函数 | 说明 |
| --- | --- |
| `shade(c, k)` | 乘系数变亮/变暗，**四舍五入** |
| `shade_floor(c, k)` / `clamp(c)` | 同上但**向零取整**（gen_ingredients / cutting / meatballs 的旧写法） |
| `mix(a, b, t)` | `a + (b-a)*t`，不截 t（gen_cup / drink_cauldron / guide_gui 的旧写法） |
| `lerp(a, b, t)` | `a*(1-t) + b*t`，t 截到 0..1（gen_boiling / stove_fire 的旧写法） |
| `rgba(c, a=255)` / `hexc("#3F76E4")` | 补透明度 / 十六进制转颜色 |
| `luma(c)` / `ramp(base, steps)` | 亮度 / 从基色生成由暗到亮的色阶 |
| `ease_out_cubic(x)` | 缓动 |

> `mix` 和 `lerp` 在 t 接近 .5 时取整可能差 1，新脚本随便选一个，迁移旧脚本必须照原写法选。

### `pixkit.canvas`

| 函数 | 说明 |
| --- | --- |
| `blank(w=16, h=None, fill=透明)` | 新画布 |
| `outline_inner(im, rgb)` | 不透明区域**最外一圈**改成描边色（形状不变大） |
| `outline_outer(im, rgba)` | 在不透明区域**外面加一圈**（形状大 1 像素） |
| `sphere_level(x, y, cx, cy, r)` | 球面明暗 0～3，光从左上（gen_garden / shrimp 的旧 `shade(x, y, ...)`） |
| `speckle(im, rnd, p, k, fn=shade)` | 随机颗粒 / 气孔，固定种子可复现 |
| `iso_cube(draw, cx, ty, w, h, top, left, right)` | 等角小方块（豆腐那种） |
| `from_rows(rows, palette)` | 字符画画图 |
| `recolor_by_luma(im, colors)` | 按亮度把原版贴图换色（适合「照原版改色」） |

### `pixkit.model`

| 函数 | 说明 |
| --- | --- |
| `box(frm, to, tex, faces=FACES, uv=None)` | 长方块元素 |
| `block_model(elements, textures)` | 方块模型，自动补 `particle` |
| `item_generated(name)` / `item_from_block(model, display)` | 薄物品模型 / 物品借用方块模型 |
| `VANILLA_BLOCK_DISPLAY` / `small_block_display(height)` | 原版方块持握姿势 / 矮小方块（杯、碗）放物品栏居中 |

### `pixkit.io`

`write_json(path, obj)`（缩进 2、**LF**、自动建目录）、`save_png(im, path)`、`install_item(root, name, im)`（贴图 + item/generated 模型一起装）。

### `pixkit.anim`

`strip(frames)`（多帧竖叠）、`mcmeta(frametime, interpolate)`、`save(sheet, meta, path)`（贴图 + `.png.mcmeta`）、`fizz_animate(...)`（可乐气泡，转调 `tools/fizz.py`）。

### `pixkit.texstats`

`stats(im)`：一张贴图的颜色数、色板、主色、明暗级数、对比度、可见范围与居中偏移、半透明像素数、有没有深色描边。原版参考库和贴图自检都用它。

### `pixkit.text`

`font(size, bold=False)`：预览图 / 对比板上写中文（微软雅黑）。

## 小例子：画一个新食材

```python
import random
from pixkit import canvas, color, io
from pixkit.paths import ASSETS

base = (150, 30, 30)                       # 鸭血的基色
lv = color.ramp(base, 4)                   # 暗 → 亮四级
im = canvas.blank()
px = im.load()
for y in range(4, 12):
    for x in range(4, 12):
        px[x, y] = color.rgba(lv[canvas.sphere_level(x, y, 8, 8, 4)])
canvas.speckle(im, random.Random(1), 0.08, 0.85)
canvas.outline_outer(im, color.rgba(color.shade(base, 0.45)))
io.install_item(ASSETS, "duck_blood", im)  # 写贴图 + 物品模型
```

## 迁移旧脚本的规矩

1. 迁移前用 `tools/out` 外的任意位置记下资源指纹（PNG 原字节、文本去掉 CR 后的内容）；
2. 改成 pixkit 后重跑，指纹必须**全部一致**（只允许 CRLF → LF 的换行变化）；
3. 测试：`python tools/tests/test_pixkit.py`（每个函数都和迁移前的旧写法对照）。

已迁移：`gen_cup.py`、`gen_boiling.py`、`gen_ingredients.py`（2026-11-01，2040 个资源文件内容全部一致）。其余脚本改到哪个再迁哪个。
