"""0.3.1-alpha Phase 3：小石磨（stone_mill）、开口大木桶（wooden_vat）及豆腐加工资产与数据包生成脚本。

用法：
    python mod/tools/gen_processing.py          # 生成预览图版 mod/tools/out/processing/sheet.png
    python mod/tools/gen_processing.py install  # 写入全部贴图、模型、方块状态、配方、标签、战利品表与教程图标
    python mod/tools/gen_processing.py stone-mill  # 仅生成小石磨模型、客户端几何与教程图标

作者：mryt233
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

from export_bbmodel import default_uv
from pixkit import canvas, io, manifest, model
from pixkit.paths import ASSETS, DATA, MOD, MODID, RES, VANILLA

TOOLS = Path(__file__).resolve().parent
ICONS = MOD / "tutorial/img/icons"
OUT = TOOLS / "out/processing"

T = (0, 0, 0, 0)


# mryt233 改：复用公共画布、描边和 JSON 写入，未改资源保留原格式。
blank = canvas.blank
add_outline = canvas.outline_outer


def write_json(path: Path, obj: dict | list) -> None:
    io.write_legacy_json(path, obj, ensure_ascii=False)


def fill_cells(im: Image.Image, cells: dict[tuple[int, int], tuple[int, int, int, int]]) -> Image.Image:
    px = im.load()
    for (x, y), col in cells.items():
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = col
    return im


# ------------------------------------------------------------------ 方块贴图
def tex_stone_mill_wood() -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    cols = [(76, 50, 30, 255), (98, 66, 40, 255), (120, 82, 50, 255), (140, 98, 62, 255)]
    for y in range(16):
        for x in range(16):
            seam = (y % 4 == 0)
            v = ((x * 5 + y * 2) % 7)
            idx = 0 if seam else (1 if v < 3 else (2 if v < 6 else 3))
            px[x, y] = cols[idx]
    return im


def tex_wooden_vat_side() -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    cols = [(82, 56, 32, 255), (108, 76, 44, 255), (134, 96, 58, 255), (156, 114, 70, 255)]
    hoop = (48, 52, 58, 255)
    hoop_hi = (74, 80, 88, 255)
    for y in range(16):
        for x in range(16):
            if y in (3, 4, 11, 12):
                px[x, y] = hoop_hi if (x % 4 == 1 and y in (3, 11)) else hoop
            elif x % 4 == 0:
                px[x, y] = cols[0]
            else:
                stave_phase = x % 4
                grain = ((x * 3 + y * 5) % 9)
                idx = 1 if stave_phase == 3 or grain < 2 else (3 if stave_phase == 1 and grain > 5 else 2)
                px[x, y] = cols[idx]
    return im


def tex_wooden_vat_planks() -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    cols = [(74, 50, 28, 255), (96, 68, 38, 255), (118, 84, 48, 255), (138, 100, 60, 255)]
    for y in range(16):
        for x in range(16):
            if y in (0, 5, 10, 15):
                px[x, y] = cols[0]
            else:
                v = (x * 7 + y * 3) % 10
                px[x, y] = cols[1 if v < 3 else (2 if v < 8 else 3)]
    return im


def tex_wooden_vat_hoop() -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    c = [(38, 40, 46, 255), (56, 60, 68, 255), (78, 82, 92, 255), (104, 110, 122, 255)]
    for y in range(16):
        for x in range(16):
            if x % 5 == 2 and y % 5 == 2:
                px[x, y] = c[3]
            else:
                px[x, y] = c[1 if (x + y) % 3 else (2 if y % 4 == 0 else 0)]
    return im


def tex_wooden_vat_lid() -> Image.Image:
    """mryt233 改：3D 厚实拼板木压盖纹理（直纹木板缝、斜倒角高光、榫卯木销）。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    seam = (104, 72, 38, 255)
    dk = (146, 106, 60, 255)
    mid = (182, 138, 84, 255)
    hi = (212, 168, 108, 255)
    peg = (88, 60, 32, 255)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15) or x in (5, 10):
                px[x, y] = seam
            elif x in (1, 6, 11) or y == 1:
                px[x, y] = hi if (x + y) % 3 else mid
            elif y == 14 or x in (4, 9, 14):
                px[x, y] = dk
            else:
                v = (x * 5 + y * 3 + (x // 5) * 2) % 9
                px[x, y] = hi if v == 0 else (dk if v >= 7 else mid)
    for px_x, px_y in [(2, 4), (7, 4), (12, 4), (2, 11), (7, 11), (12, 11)]:
        px[px_x, px_y] = peg
    return im


def tex_soymilk_still() -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    cols = [
        (224, 214, 190, 255),
        (238, 230, 208, 255),
        (248, 242, 224, 255),
        (255, 251, 238, 255),
    ]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            idx = 3 if (int(r * 2) + (x + y) % 3) % 5 == 0 else (2 if r < 4.5 else (1 if (x * 5 + y * 3) % 4 else 0))
            px[x, y] = cols[idx]
    return im


# ------------------------------------------------------------------ 物品贴图 (16x16, 严格居中+描边)
def tex_soymilk_bucket() -> Image.Image:
    """mryt233 改：复用原版 water_bucket.png 的完整铁桶外观与桶口边缘，只把蓝色水面像素按明暗换成醇暖豆浆（保留 >30 色度以适配 LabPBR bucket_soup 分色）。"""
    water = Image.open(VANILLA / "textures/item/water_bucket.png").convert("RGBA")
    im = water.copy()
    px, wp = im.load(), water.load()
    liquid_cells = [
        (x, y)
        for y in range(16)
        for x in range(16)
        if wp[x, y][3] and not (wp[x, y][0] == wp[x, y][1] == wp[x, y][2])
    ]
    ramp = ((214, 198, 162), (238, 224, 190), (253, 244, 212))
    lum = {c: (wp[c][0] + wp[c][1] + wp[c][2]) / 3.0 for c in liquid_cells}
    lo, hi = min(lum.values()), max(lum.values())
    for c in liquid_cells:
        t = (lum[c] - lo) / max(1.0, hi - lo)
        k = 0 if t < 0.34 else (1 if t < 0.72 else 2)
        px[c] = ramp[k] + (255,)
    for x, y in sorted(liquid_cells, key=lambda c: (-lum[c], c[1], c[0]))[:2]:
        px[x, y] = (255, 248, 218, 255)
    return im


def tex_brine_bottle() -> Image.Image:
    im = blank()
    px = im.load()
    cork = (158, 114, 70, 255)
    cork_dk = (118, 80, 44, 255)
    glass_hi = (212, 236, 246, 255)
    glass = (166, 204, 224, 255)
    brine_hi = (224, 246, 252, 255)
    brine = (176, 224, 238, 255)
    brine_dk = (128, 184, 206, 255)
    out = (48, 72, 92, 255)

    for x in range(7, 9):
        px[x, 2] = cork
        px[x, 3] = cork_dk
    for x in range(6, 10):
        px[x, 4] = glass_hi if x < 8 else glass
    for x in range(7, 9):
        px[x, 5] = glass
    for x in range(5, 11):
        px[x, 6] = glass_hi if x == 5 else glass
    for y in range(7, 12):
        for x in range(4, 12):
            if x == 4:
                px[x, y] = glass_hi
            elif x == 11:
                px[x, y] = glass
            elif y == 7:
                px[x, y] = brine_hi
            elif x < 7:
                px[x, y] = brine_hi if y < 10 else brine
            elif x > 9:
                px[x, y] = brine_dk
            else:
                px[x, y] = brine
    for x in range(5, 11):
        px[x, 12] = glass if x in (5, 10) else brine_dk
    for x in range(6, 10):
        px[x, 13] = glass
    return add_outline(im, out)


# ------------------------------------------------------------------ 物品与方块模型
def item_generated(name: str) -> dict:
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{name}"}}


def faces_all(tex: str) -> dict:
    return {d: {"texture": tex} for d in ("north", "south", "west", "east", "up", "down")}


# mryt233 改：游戏模型、动态上盘和查看器共用用户确认的整像素尺寸。
MILL_GEOMETRY = MOD / "viewer/stone_mill_pixel_preview.json"
MILL_CLIENT = MOD / "common/src/client/java/cn/blockforge/generated/orientshotpotchronicles/client"


def mill_elements(rows):
    out = []
    for row in rows:
        f, t, tex = row[:3], row[3:6], "#" + row[6]
        uv = {d: default_uv(d, f, t) for d in ("north", "east", "south", "west", "up", "down")} if row[4] > 16 else None
        out.append(model.box(f, t, tex, uv=uv))
    return out


def mill_client_geometry(data):
    floor = data["slot"]["floor"]
    seam = data["slot"]["seam"]
    outer_left, inner_left, inner_right, outer_right = data["slot"]["ring"]
    mouth_left, mouth_right, mouth_z = data["slot"]["mouth"]
    return """// 作者：mryt233；由 tools/gen_processing.py 生成，请勿手改。
package cn.blockforge.generated.orientshotpotchronicles.client;

final class StoneMillGeometry {
    private StoneMillGeometry() {}
""" + "".join(
        f"    static final float {name} = {value:g}F;" + chr(10)
        for name, value in {
            "FLOOR": floor, "SEAM": seam, "OUTER_LEFT": outer_left, "OUTER_RIGHT": outer_right,
            "INNER_LEFT": inner_left, "INNER_RIGHT": inner_right,
            "MOUTH_LEFT": mouth_left, "MOUTH_RIGHT": mouth_right, "MOUTH_Z": mouth_z,
        }.items()
    ) + "    // x0, y0, z0, x1, y1, z1, wood flag." + chr(10) + "    static final int[][] TOP = {" + chr(10) + "".join(
        "        {" + ", ".join(str(n) for n in [*row[:6], int(row[6] == "wood")]) + "}," + chr(10)
        for row in data["top"]
    ) + "    };" + chr(10) + "}" + chr(10)


def stone_mill_icon(full_model):
    # 离线渲染器不补默认 UV，显式复制原版裁图规则，避免每个小面贴整张圆石。
    from render_preview import SCALE, draw_model
    textured = {**full_model, "elements": [
        {**el, "faces": {d: {**face, "uv": default_uv(d, el["from"], el["to"])}
                        for d, face in el["faces"].items()}}
        for el in full_model["elements"]
    ]}
    img = Image.new("RGBA", (36 * SCALE, 36 * SCALE))
    draw_model(img, textured, 18 * SCALE, 26 * SCALE)
    icon = img.crop(img.getbbox())
    icon.thumbnail((88, 88), Image.Resampling.NEAREST)
    out = Image.new("RGBA", (96, 96))
    out.paste(icon, ((96 - icon.width) // 2, (96 - icon.height) // 2))
    io.save_png(out, ICONS / f"{MODID}__stone_mill.png")


def stone_mill_models() -> None:
    data = json.loads(MILL_GEOMETRY.read_bytes())
    tex = {**data["textures"], "particle": data["textures"]["stone"]}
    base, top = mill_elements(data["base"]), mill_elements(data["top"])
    full = model.block_model(base + top, tex)
    manifest.start_session(MOD, RES)
    for suffix, contents in (("_base", model.block_model(base, tex)), ("_top", model.block_model(top, tex)), ("", full)):
        write_json(ASSETS / f"models/block/stone_mill{suffix}.json", contents)
    write_json(ASSETS / "models/item/stone_mill.json", model.item_from_block("stone_mill", display={
        "gui": {"rotation": [30, 225, 0], "translation": [0, -0.9, 0], "scale": [0.55, 0.55, 0.55]}
    }))
    write_json(ASSETS / "blockstates/stone_mill.json", {
        "variants": {
            "facing=south": {"model": f"{MODID}:block/stone_mill_base"},
            "facing=west": {"model": f"{MODID}:block/stone_mill_base", "y": 90},
            "facing=north": {"model": f"{MODID}:block/stone_mill_base", "y": 180},
            "facing=east": {"model": f"{MODID}:block/stone_mill_base", "y": 270},
        }
    })
    io.write_text(MILL_CLIENT / "StoneMillGeometry.java", mill_client_geometry(data))
    stone_mill_icon(full)


def wooden_press_lid_icon(lid_model: dict) -> None:
    """用统一 3D 模型与现生成方块贴图渲染 96×96 木压板教程图标。"""
    import render_preview
    from render_preview import SCALE, draw_model

    render_preview._tex_cache.clear()
    textured = {
        **lid_model,
        "elements": [
            {
                **el,
                "faces": {
                    d: {**face, "uv": default_uv(d, el["from"], el["to"])}
                    for d, face in el["faces"].items()
                },
            }
            for el in lid_model["elements"]
        ],
    }
    img = Image.new("RGBA", (30 * SCALE, 30 * SCALE))
    draw_model(img, textured, 15 * SCALE, 20 * SCALE)
    icon = img.crop(img.getbbox())
    icon.thumbnail((88, 88), Image.Resampling.NEAREST)
    out = Image.new("RGBA", (96, 96))
    out.paste(icon, ((96 - icon.width) // 2, (96 - icon.height) // 2))
    out.save(ICONS / f"{MODID}__wooden_press_lid.png")


def wooden_vat_models() -> None:
    tex = {
        "side": f"{MODID}:block/wooden_vat_side",
        "planks": f"{MODID}:block/wooden_vat_planks",
        "hoop": f"{MODID}:block/wooden_vat_hoop",
        "particle": f"{MODID}:block/wooden_vat_side",
    }
    elements = [
        # 桶底内木板 + 地脚延边 (y 0..3)
        {"from": [1.5, 0, 1.5], "to": [14.5, 3, 14.5], "faces": {
            "north": {"texture": "#side"}, "south": {"texture": "#side"},
            "west": {"texture": "#side"}, "east": {"texture": "#side"},
            "up": {"texture": "#planks"}, "down": {"texture": "#planks"},
        }},
        # 四面敞口橡木桶壁 (y 3..14)
        {"from": [1.0, 3, 1.0], "to": [15.0, 14, 2.5], "faces": {
            "north": {"texture": "#side"}, "south": {"texture": "#planks"},
            "west": {"texture": "#side"}, "east": {"texture": "#side"},
            "up": {"texture": "#planks"}, "down": {"texture": "#side"},
        }},
        {"from": [1.0, 3, 13.5], "to": [15.0, 14, 15.0], "faces": {
            "north": {"texture": "#planks"}, "south": {"texture": "#side"},
            "west": {"texture": "#side"}, "east": {"texture": "#side"},
            "up": {"texture": "#planks"}, "down": {"texture": "#side"},
        }},
        {"from": [1.0, 3, 2.5], "to": [2.5, 14, 13.5], "faces": {
            "north": {"texture": "#side"}, "south": {"texture": "#side"},
            "west": {"texture": "#side"}, "east": {"texture": "#planks"},
            "up": {"texture": "#planks"}, "down": {"texture": "#side"},
        }},
        {"from": [13.5, 3, 2.5], "to": [15.0, 14, 13.5], "faces": {
            "north": {"texture": "#side"}, "south": {"texture": "#side"},
            "west": {"texture": "#planks"}, "east": {"texture": "#side"},
            "up": {"texture": "#planks"}, "down": {"texture": "#side"},
        }},
        # 两道锻铁木桶外箍圈：拆为四条贴外壁环条（补全上下封闭面，避免实心贯通桶壁与漏面，mryt233 改）
        *[
            box_el
            for y0, y1 in ((3.2, 4.4), (10.2, 11.4))
            for box_el in (
                {"from": [0.8, y0, 0.8], "to": [15.2, y1, 1.0], "faces": faces_all("#hoop")},
                {"from": [0.8, y0, 15.0], "to": [15.2, y1, 15.2], "faces": faces_all("#hoop")},
                {"from": [0.8, y0, 1.0], "to": [1.0, y1, 15.0], "faces": faces_all("#hoop")},
                {"from": [15.0, y0, 1.0], "to": [15.2, y1, 15.0], "faces": faces_all("#hoop")},
            )
        ],
    ]
    write_json(ASSETS / "models/block/wooden_vat.json", {
        "parent": "minecraft:block/block",
        "textures": tex,
        "elements": elements,
    })
    lid_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "lid": f"{MODID}:block/wooden_vat_lid",
            "wood": f"{MODID}:block/wooden_vat_planks",
            "handle": f"{MODID}:block/stone_mill_wood",
            "particle": f"{MODID}:block/wooden_vat_lid",
        },
        "elements": [
            {"from": [3.0, 0.0, 4.0], "to": [13.0, 1.5, 12.0], "faces": faces_all("#lid")},
            {"from": [4.0, 0.0, 3.0], "to": [12.0, 1.5, 4.0], "faces": faces_all("#lid")},
            {"from": [4.0, 0.0, 12.0], "to": [12.0, 1.5, 13.0], "faces": faces_all("#lid")},
            {"from": [3.5, 1.5, 4.75], "to": [12.5, 2.5, 6.25], "faces": faces_all("#wood")},
            {"from": [3.5, 1.5, 9.75], "to": [12.5, 2.5, 11.25], "faces": faces_all("#wood")},
            {"from": [7.25, 2.5, 4.9], "to": [8.75, 3.5, 6.1], "faces": faces_all("#handle")},
            {"from": [7.25, 2.5, 9.9], "to": [8.75, 3.5, 11.1], "faces": faces_all("#handle")},
            {"from": [7.0, 3.5, 4.75], "to": [9.0, 4.5, 11.25], "faces": faces_all("#handle")},
        ],
    }
    write_json(ASSETS / "models/block/wooden_vat_lid.json", lid_model)
    write_json(ASSETS / "models/item/wooden_press_lid.json", {
        "parent": f"{MODID}:block/wooden_vat_lid",
        "gui_light": "side",
        "display": {
            "gui": {"rotation": [35, 35, 0], "translation": [0, 5.33, 0], "scale": [0.95, 0.95, 0.95]},
            "fixed": {"rotation": [90, 0, 0], "translation": [0, 0, 4], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [15, 45, 0], "translation": [0, 5, 0], "scale": [0.85, 0.85, 0.85]},
            "firstperson_lefthand": {"rotation": [15, 225, 0], "translation": [0, 5, 0], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 2], "scale": [0.6, 0.6, 0.6]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
        },
    })
    wooden_press_lid_icon(lid_model)
    write_json(ASSETS / "models/item/wooden_vat.json", {
        "parent": f"{MODID}:block/wooden_vat",
        "display": {
            "gui": {
                "rotation": [30, 225, 0],
                "translation": [0, 0.65, 0],
                "scale": [0.625, 0.625, 0.625],
            }
        },
    })
    write_json(ASSETS / "blockstates/wooden_vat.json", {
        "variants": {
            f"facing={d},has_lid={lid}": {"model": f"{MODID}:block/wooden_vat", **({"y": rot} if rot else {})}
            for d, rot in [("south", 0), ("west", 90), ("north", 180), ("east", 270)]
            for lid in ("false", "true")
        }
    })


# ------------------------------------------------------------------ 战利品表、标签与配方
def write_data() -> int:
    count = 0
    for blk in ("stone_mill", "wooden_vat"):
        write_json(DATA / f"loot_table/blocks/{blk}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{blk}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })
        count += 1

    rec = DATA / "recipe"
    # 工作台配方
    write_json(rec / "stone_mill.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["SCS", "SSS", "P P"],
        "key": {
            "S": {"tag": "minecraft:stone_crafting_materials"},
            "C": {"item": "minecraft:stick"},
            "P": {"tag": "minecraft:planks"},
        },
        "result": {"id": f"{MODID}:stone_mill", "count": 1},
    })
    write_json(rec / "wooden_vat.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["P P", "IPI", "PPP"],
        "key": {
            "P": {"tag": "minecraft:planks"},
            "I": {"item": "minecraft:iron_ingot"},
        },
        "result": {"id": f"{MODID}:wooden_vat", "count": 1},
    })
    write_json(rec / "wooden_press_lid.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" T ", "WWW"],
        "key": {
            "T": {"item": "minecraft:stick"},
            "W": {"tag": "minecraft:wooden_slabs"},
        },
        "result": {"id": f"{MODID}:wooden_press_lid", "count": 1},
    })
    write_json(rec / "brine_bottle.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [
            {"item": "minecraft:water_bucket"},
            {"item": "minecraft:kelp"},
            {"item": "minecraft:glass_bottle"},
            {"item": "minecraft:glass_bottle"},
        ],
        "result": {"id": f"{MODID}:brine_bottle", "count": 2},
    })
    write_json(rec / "brine_bottle_from_dried_kelp.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [
            {"item": "minecraft:water_bucket"},
            {"item": "minecraft:dried_kelp"},
            {"item": "minecraft:glass_bottle"},
        ],
        "result": {"id": f"{MODID}:brine_bottle", "count": 1},
    })
    write_json(rec / "dried_tofu_from_stonecutting.json", {
        "type": "minecraft:stonecutting",
        "ingredient": {"item": f"{MODID}:cold_tofu"},
        "result": {"id": f"{MODID}:dried_tofu", "count": 2},
    })
    # 完整模式隐藏工作台直合冷豆腐，关闭细分开关 / 精简模式时开启快捷直合
    write_json(rec / "cold_tofu.json", {
        "type": f"{MODID}:conditional_crafting",
        "flag": "mode.detailed_tofu_processing",
        "expected": False,
        "recipe": {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [{"item": f"{MODID}:soybean"}] * 3 + [{"item": "minecraft:water_bucket"}],
            "result": {"id": f"{MODID}:cold_tofu", "count": 1},
        },
    })
    count += 7

    # 小石磨自定义配方（data/.../recipe/stone_mill/）
    mill_recipes = [
        ("soymilk", {
            "type": f"{MODID}:stone_mill",
            "ingredient": {"item": f"{MODID}:soybean"},
            "ingredient_count": 2,
            "requires_water": True,
            "container": "minecraft:bucket",
            "result": {"id": f"{MODID}:soymilk_bucket", "count": 1},
            "turns": 4,
            "color": "#F3ECD8",
        }),
        ("crushed_peanuts", {
            "type": f"{MODID}:stone_mill",
            "ingredient": {"item": f"{MODID}:peanut"},
            "ingredient_count": 1,
            "requires_water": False,
            "result": {"id": f"{MODID}:crushed_peanuts", "count": 2},
            "turns": 3,
            "color": "#D8B26E",
        }),
        ("millet_chili", {
            "type": f"{MODID}:stone_mill",
            "ingredient": {"item": f"{MODID}:chili_pepper"},
            "ingredient_count": 1,
            "requires_water": False,
            "result": {"id": f"{MODID}:millet_chili", "count": 2},
            "turns": 2,
            "color": "#C42818",
        }),
        ("garlic_paste", {
            "type": f"{MODID}:stone_mill",
            "ingredient": {"item": f"{MODID}:garlic"},
            "ingredient_count": 1,
            "requires_water": False,
            "result": {"id": f"{MODID}:garlic_paste", "count": 2},
            "turns": 3,
            "color": "#E8D696",
        }),
    ]
    for name, body in mill_recipes:
        write_json(rec / f"stone_mill/{name}.json", body)
        count += 1

    # 开口大木桶自定义配方（data/.../recipe/wooden_vat/）
    vat_recipes = [
        ("cold_tofu", {
            "type": f"{MODID}:wooden_vat",
            "mode": "coagulate",
            "fluid": "soymilk",
            "ingredient": {"tag": f"{MODID}:tofu_coagulants"},
            "ingredient_count": 1,
            "needs_lid": True,
            "ticks": 80,
            "result": {"id": f"{MODID}:cold_tofu", "count": 2},
            "color": "#FAF3DF",
        }),
    ]
    for name, body in vat_recipes:
        write_json(rec / f"wooden_vat/{name}.json", body)
        count += 1

    # 标签
    write_json(DATA / "tags/item/tofu_coagulants.json", {
        "replace": False,
        "values": [f"{MODID}:brine_bottle"],
    })
    count += 1
    return count


BLOCK_TEXTURES = {
    "stone_mill_wood": tex_stone_mill_wood,
    "wooden_vat_side": tex_wooden_vat_side,
    "wooden_vat_planks": tex_wooden_vat_planks,
    "wooden_vat_hoop": tex_wooden_vat_hoop,
    "wooden_vat_lid": tex_wooden_vat_lid,
    "soymilk_still": tex_soymilk_still,
}

ITEM_TEXTURES = {
    "soymilk_bucket": tex_soymilk_bucket,
    "brine_bottle": tex_brine_bottle,
}


def icon_wooden_vat() -> Image.Image:
    im = blank()
    px = im.load()
    rim = (156, 112, 68, 255)
    side = (122, 84, 46, 255)
    dk = (86, 58, 30, 255)
    hoop = (56, 60, 68, 255)
    milk = (244, 236, 214, 255)
    out = (48, 32, 18, 255)
    for x in range(4, 12):
        px[x, 4] = rim
        px[x, 5] = milk
        px[x, 6] = rim
    for y in range(6, 14):
        for x in range(3, 13):
            if y in (8, 11):
                px[x, y] = hoop
            elif x in (3, 12):
                px[x, y] = dk
            elif x % 3 == 0:
                px[x, y] = dk
            else:
                px[x, y] = rim if x < 7 else side
    return add_outline(im, out)


def preview_sheet() -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    items = [(k, fn()) for k, fn in list(BLOCK_TEXTURES.items()) + list(ITEM_TEXTURES.items())]
    cols = 5
    rows = (len(items) + cols - 1) // cols
    cell = 96
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (32, 34, 38, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(items):
        cx, cy = (i % cols) * cell, (i // cols) * cell
        sheet.alpha_composite(im.resize((64, 64), Image.Resampling.NEAREST), (cx + 16, cy + 8))
        draw.text((cx + 4, cy + 78), name[:16], fill=(230, 230, 230, 255))
    out = OUT / "sheet.png"
    sheet.save(out)
    return out


def install() -> None:
    tex_b = ASSETS / "textures/block"
    tex_i = ASSETS / "textures/item"
    tex_b.mkdir(parents=True, exist_ok=True)
    tex_i.mkdir(parents=True, exist_ok=True)
    ICONS.mkdir(parents=True, exist_ok=True)

    for name, fn in BLOCK_TEXTURES.items():
        fn().save(tex_b / f"{name}.png")
    for name, fn in ITEM_TEXTURES.items():
        im = fn()
        im.save(tex_i / f"{name}.png")
        im.resize((32, 32), Image.Resampling.NEAREST).save(ICONS / f"{MODID}__{name}.png")

    icon_wooden_vat().resize((32, 32), Image.Resampling.NEAREST).save(ICONS / f"{MODID}__wooden_vat.png")
    vanilla_kelp = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures/item/dried_kelp.png"
    if vanilla_kelp.is_file():
        with Image.open(vanilla_kelp) as vim:
            vim.convert("RGBA").resize((32, 32), Image.Resampling.NEAREST).save(ICONS / "minecraft__dried_kelp.png")

    wooden_vat_models()

    for name in ITEM_TEXTURES:
        write_json(ASSETS / f"models/item/{name}.json", item_generated(name))

    n_data = write_data()
    stone_mill_models()
    print(f"已写入 {len(BLOCK_TEXTURES)} 张方块贴图、{len(ITEM_TEXTURES)} 张物品贴图、小石磨/木桶/3D木压板模型与 {n_data} 份数据文件。")


def main() -> int:
    if sys.argv[1:] == ["stone-mill"]:
        stone_mill_models()
        return 0
    if len(sys.argv) > 1 and sys.argv[1] == "install":
        install()
    sheet = preview_sheet()
    print(f"预览图版已生成：{sheet}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
