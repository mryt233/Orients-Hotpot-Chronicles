"""0.3.1-alpha Phase 4：统一火锅桌（hotpot_table / tall_hotpot_table）、三种备菜架（multi_tier_prep_rack /
desktop_prep_rack / hanging_prep_rack）模型、方块状态、标签、配方、战利品表与默认槽位贴图生成。

用法：
    python mod/tools/gen_phase4_furniture.py

作者：mryt233
"""
from __future__ import annotations

import json
from pathlib import Path
from PIL import Image, ImageDraw

from pixkit.io import save_png, write_json
from pixkit.paths import ASSETS, DATA, MOD, MODID

TEX_BLOCK = ASSETS / "textures/block"
MODELS_BLOCK = ASSETS / "models/block"
MODELS_ITEM = ASSETS / "models/item"
BLOCKSTATES = ASSETS / "blockstates"
DATA_MOD = DATA
ICONS_DIR = MOD / "tutorial/img/icons"
OUT_DIR = MOD / "tools/out/phase4_furniture"

BITS = [("n", 1), ("e", 2), ("s", 4), ("w", 8)]
DIRS = ["north", "east", "south", "west"]

BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}

PLATE_MATERIALS = ["quartz", "bamboo", "terracotta", "copper", "iron"]
PLATE_SHAPES = [
    "small_dish",
    "two_slot_plate",
    "three_slot_long_plate",
    "four_slot_square_plate",
    "five_slot_long_plate",
    "eight_slot_platter",
]


# ------------------------------------------------------------------ 16x16 默认槽位与五金贴图
def _make_planks(variant: str) -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    if variant == "cap":
        base = [(162, 130, 78, 255), (150, 118, 69, 255), (176, 144, 90, 255), (124, 95, 52, 255)]
    else:
        base = [(154, 122, 72, 255), (140, 110, 64, 255), (168, 136, 82, 255), (114, 86, 46, 255)]
    for y in range(16):
        for x in range(16):
            if y in (0, 4, 8, 12):
                col = base[3]
            elif ((y // 4) == 0 and x == 11) or ((y // 4) == 1 and x == 3) or ((y // 4) == 2 and x == 14) or ((y // 4) == 3 and x == 6):
                col = base[3]
            else:
                h = (x * 11 + y * 5 + (y // 4) * 7) % 9
                col = base[2] if h >= 7 else (base[1] if h <= 2 else base[0])
            px[x, y] = col
    return im


def _make_bricks(variant: str) -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    mortar = (182, 176, 168, 255) if variant != "ring" else (164, 158, 150, 255)
    bricks = [(146, 77, 58, 255), (132, 68, 50, 255), (158, 88, 66, 255), (118, 60, 44, 255)]
    for y in range(16):
        row = y // 4
        offset = 4 if row % 2 else 0
        for x in range(16):
            if y % 4 == 3 or (x + offset) % 8 == 0:
                px[x, y] = mortar
            else:
                h = (x * 7 + y * 13 + row * 5) % 8
                px[x, y] = bricks[h % 4]
    return im


def _make_iron() -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    dark = (40, 44, 52, 255)
    mid = (58, 64, 74, 255)
    hi = (84, 92, 104, 255)
    rivet = (112, 122, 136, 255)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px[x, y] = dark
            elif (x + y) % 5 == 0:
                px[x, y] = hi
            else:
                px[x, y] = mid
    for rx, ry in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[rx, ry] = rivet
    return im


def install_textures() -> dict[str, Image.Image]:
    textures = {
        "mat_slot_top_cap": _make_planks("cap"),
        "mat_slot_top_side": _make_planks("side"),
        "mat_slot_base_cap": _make_bricks("cap"),
        "mat_slot_base_side": _make_bricks("side"),
        "mat_slot_tall_ring": _make_bricks("ring"),
        "prep_rack_iron": _make_iron(),
    }
    for name, im in textures.items():
        save_png(im, TEX_BLOCK / f"{name}.png")
    return textures


# ------------------------------------------------------------------ 几何辅助
def _suffix(mask: int) -> str:
    s = "".join(c for c, b in BITS if mask & b)
    return "_" + s if s else ""


def _spans(mask: int) -> tuple[int, int, int, int]:
    lo_x = 0 if (mask & 8) else 4
    hi_x = 16 if (mask & 2) else 12
    lo_z = 0 if (mask & 1) else 4
    hi_z = 16 if (mask & 4) else 12
    return lo_x, hi_x, lo_z, hi_z


def _side_uv(face: str, frm: list[float], to: list[float], rows: tuple[float, float]) -> list[float]:
    x0, _, z0 = frm
    x1, _, z1 = to
    v0, v1 = rows
    return {
        "north": [16 - x1, v0, 16 - x0, v1],
        "south": [x0, v0, x1, v1],
        "east": [16 - z1, v0, 16 - z0, v1],
        "west": [z0, v0, z1, v1],
    }[face]


def _box_faces(
    frm: list[float],
    to: list[float],
    cap_tex: str,
    side_tex: str,
    skip: set[str] = frozenset(),
    cull_down: bool = False,
    side_rows: tuple[float, float] | None = None,
) -> dict:
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if side_rows is None:
        side_rows = (16 - (y1 % 16 if y1 % 16 != 0 else 16), 16 - (y0 % 16))
        if y0 == 0 and y1 == 16:
            side_rows = (0, 16)
    faces = {}
    for d in ("north", "east", "south", "west", "up", "down"):
        if d in skip:
            continue
        if d in ("up", "down"):
            entry = {"texture": cap_tex, "uv": [x0, z0, x1, z1]}
            if d == "down" and cull_down:
                entry["cullface"] = "down"
        else:
            entry = {"texture": side_tex, "uv": _side_uv(d, frm, to, side_rows)}
        faces[d] = entry
    return faces


def _table_textures(tall: bool = False) -> dict[str, str]:
    tex = {
        "particle": f"{MODID}:block/mat_slot_top_cap",
        "top_cap": f"{MODID}:block/mat_slot_top_cap",
        "top_side": f"{MODID}:block/mat_slot_top_side",
        "base_cap": f"{MODID}:block/mat_slot_base_cap",
        "base_side": f"{MODID}:block/mat_slot_base_side",
    }
    if tall:
        tex["tall_ring"] = f"{MODID}:block/mat_slot_tall_ring"
    return tex


def _rack_textures() -> dict[str, str]:
    return {
        "particle": f"{MODID}:block/mat_slot_top_side",
        "top_cap": f"{MODID}:block/mat_slot_top_cap",
        "top_side": f"{MODID}:block/mat_slot_top_side",
        "metal": f"{MODID}:block/prep_rack_iron",
    }


def _solid_box(frm: list[float], to: list[float], cap_tex: str, side_tex: str) -> dict:
    x0, y0, z0 = frm
    x1, y1, z1 = to
    v0 = max(0.0, 16.0 - y1)
    v1 = min(16.0, 16.0 - y0)
    if v1 <= v0:
        v0, v1 = 0.0, max(1.0, y1 - y0)
    return {
        "from": frm,
        "to": to,
        "faces": _box_faces(frm, to, cap_tex, side_tex, side_rows=(v0, v1)),
    }


# ------------------------------------------------------------------ 统一火锅桌（矮 & 高）
def install_tables() -> None:
    low_variants = {}
    for mask in range(16):
        x0, x1, z0, z1 = _spans(mask)
        # mryt233 改（2026-11-20）：对齐高火锅桌，保留底座与桌面板水平侧面，避免转角 12px/8px 交界或高低桌相邻处漏面。
        model = {
            "parent": "minecraft:block/block",
            "textures": _table_textures(tall=False),
            "elements": [
                {
                    "from": [0, 7, 0],
                    "to": [16, 11, 16],
                    "faces": _box_faces([0, 7, 0], [16, 11, 16], "#top_cap", "#top_side", side_rows=(0, 4)),
                },
                {
                    "from": [x0, 0, z0],
                    "to": [x1, 7, z1],
                    "faces": _box_faces([x0, 0, z0], [x1, 7, z1], "#base_cap", "#base_side", skip={"up"}, cull_down=True, side_rows=(9, 16)),
                },
            ],
        }
        suffix = _suffix(mask)
        write_json(MODELS_BLOCK / f"hotpot_table{suffix}.json", model)
        key = ",".join(f"{d}={'true' if mask & b else 'false'}" for d, b in zip(DIRS, [1, 2, 4, 8]))
        low_variants[key] = {"model": f"{MODID}:block/hotpot_table{suffix}"}
    write_json(BLOCKSTATES / "hotpot_table.json", {"variants": low_variants})

    # 矮桌物品模型（东南西连接形态，6 面全画）
    ix0, ix1, iz0, iz1 = _spans(0b1110)
    write_json(MODELS_ITEM / "hotpot_table.json", {
        "parent": "minecraft:block/block",
        "textures": _table_textures(tall=False),
        "display": BLOCK_DISPLAY,
        "elements": [
            {
                "from": [0, 7, 0],
                "to": [16, 11, 16],
                "faces": _box_faces([0, 7, 0], [16, 11, 16], "#top_cap", "#top_side", side_rows=(0, 4)),
            },
            {
                "from": [ix0, 0, iz0],
                "to": [ix1, 7, iz1],
                "faces": _box_faces([ix0, 0, iz0], [ix1, 7, iz1], "#base_cap", "#base_side", skip={"up"}, side_rows=(9, 16)),
            },
        ],
    })

    # 高桌（open=false & open=true）
    tall_variants = {}
    for mask in range(16):
        x0, x1, z0, z1 = _spans(mask)
        suffix = _suffix(mask)
        key = ",".join(f"{d}={'true' if mask & b else 'false'}" for d, b in zip(DIRS, [1, 2, 4, 8]))
        top_el = {
            "from": [0, 19.0, 0],
            "to": [16, 23.0, 16],
            "faces": _box_faces([0, 19.0, 0], [16, 23.0, 16], "#top_cap", "#top_side", side_rows=(0, 4)),
        }
        ring_closed = {
            "from": [x0, 16, z0],
            "to": [x1, 19.0, z1],
            "faces": _box_faces([x0, 16, z0], [x1, 19.0, z1], "#tall_ring", "#tall_ring", skip={"up", "down"}, side_rows=(13, 16)),
        }
        base_el = {
            "from": [x0, 0, z0],
            "to": [x1, 16, z1],
            "faces": _box_faces([x0, 0, z0], [x1, 16, z1], "#base_cap", "#base_side", skip={"up"}, cull_down=True, side_rows=(0, 16)),
        }
        write_json(MODELS_BLOCK / f"tall_hotpot_table{suffix}.json", {
            "parent": "minecraft:block/block",
            "textures": _table_textures(tall=True),
            "elements": [top_el, base_el, ring_closed],
        })
        ring_open = {
            "from": [x0, 16, z0],
            "to": [x1, 19.0, z1],
            "faces": _box_faces([x0, 16, z0], [x1, 19.0, z1], "#tall_ring", "#tall_ring", skip={"up"}, side_rows=(13, 16)),
        }
        write_json(MODELS_BLOCK / f"tall_hotpot_table_open{suffix}.json", {
            "parent": "minecraft:block/block",
            "textures": _table_textures(tall=True),
            "elements": [top_el, ring_open],
        })
        tall_variants[f"{key},open=false"] = {"model": f"{MODID}:block/tall_hotpot_table{suffix}"}
        tall_variants[f"{key},open=true"] = {"model": f"{MODID}:block/tall_hotpot_table_open{suffix}"}

    write_json(BLOCKSTATES / "tall_hotpot_table.json", {"variants": tall_variants})
    tall_display = dict(BLOCK_DISPLAY, gui={"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]})
    write_json(MODELS_ITEM / "tall_hotpot_table.json", {
        "parent": f"{MODID}:block/tall_hotpot_table_esw",
        "display": tall_display,
    })


# ------------------------------------------------------------------ 三种备菜架（模型与 Blockstate）
def _rack_blockstate(wall_model: str, ceiling_model: str | None = None) -> dict:
    ceiling_model = ceiling_model or wall_model
    variants = {}
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for mount, model in (("wall", wall_model), ("ceiling", ceiling_model)):
            entry = {"model": f"{MODID}:block/{model}"}
            if y:
                entry["y"] = y
            variants[f"facing={facing},mount={mount}"] = entry
    return {"variants": variants}


def install_prep_racks() -> None:
    # 1. 多层落地菜架（6 格：3 层每层 2 格，facing=SOUTH 为基准朝向）
    multi_elements = [
        # 4 根铁质立柱
        _solid_box([1.0, 0.0, 2.0], [2.0, 16.0, 3.0], "#metal", "#metal"),
        _solid_box([14.0, 0.0, 2.0], [15.0, 16.0, 3.0], "#metal", "#metal"),
        _solid_box([1.0, 0.0, 13.0], [2.0, 16.0, 14.0], "#metal", "#metal"),
        _solid_box([14.0, 0.0, 13.0], [15.0, 16.0, 14.0], "#metal", "#metal"),
        # 两侧纵梁 + 顶部后护栏
        _solid_box([1.0, 0.5, 3.0], [1.8, 1.5, 13.0], "#metal", "#metal"),
        _solid_box([14.2, 0.5, 3.0], [15.0, 1.5, 13.0], "#metal", "#metal"),
        _solid_box([1.0, 6.5, 3.0], [1.8, 7.5, 13.0], "#metal", "#metal"),
        _solid_box([14.2, 6.5, 3.0], [15.0, 7.5, 13.0], "#metal", "#metal"),
        _solid_box([1.0, 12.0, 3.0], [1.8, 13.0, 13.0], "#metal", "#metal"),
        _solid_box([14.2, 12.0, 3.0], [15.0, 13.0, 13.0], "#metal", "#metal"),
        _solid_box([2.0, 14.8, 2.0], [14.0, 15.8, 2.8], "#metal", "#metal"),
        # 三层可换材质层板（严格落在 PrepRackRenderer y=4.35 / 9.85 / 15.35 之下）
        _solid_box([1.8, 0.5, 2.5], [14.2, 1.5, 13.5], "#top_cap", "#top_side"),
        _solid_box([1.8, 6.5, 2.5], [14.2, 7.5, 13.5], "#top_cap", "#top_side"),
        _solid_box([1.8, 12.0, 2.5], [14.2, 13.0, 13.5], "#top_cap", "#top_side"),
    ]
    write_json(MODELS_BLOCK / "multi_tier_prep_rack.json", {
        "parent": "minecraft:block/block",
        "textures": _rack_textures(),
        "elements": multi_elements,
    })
    write_json(BLOCKSTATES / "multi_tier_prep_rack.json", _rack_blockstate("multi_tier_prep_rack"))
    write_json(MODELS_ITEM / "multi_tier_prep_rack.json", {
        "parent": f"{MODID}:block/multi_tier_prep_rack",
        "display": BLOCK_DISPLAY,
    })

    # 2. 桌面备菜架（4 格：前下/后上两排阶梯式）
    desktop_elements = [
        # 铁质阶梯支架
        _solid_box([1.0, 0.0, 3.0], [1.8, 10.6, 4.0], "#metal", "#metal"),
        _solid_box([14.2, 0.0, 3.0], [15.0, 10.6, 4.0], "#metal", "#metal"),
        _solid_box([1.0, 0.0, 12.0], [1.8, 2.0, 13.0], "#metal", "#metal"),
        _solid_box([14.2, 0.0, 12.0], [15.0, 2.0, 13.0], "#metal", "#metal"),
        _solid_box([1.0, 0.5, 4.0], [1.8, 1.5, 12.0], "#metal", "#metal"),
        _solid_box([14.2, 0.5, 4.0], [15.0, 1.5, 12.0], "#metal", "#metal"),
        _solid_box([1.0, 5.2, 4.0], [1.8, 6.2, 8.4], "#metal", "#metal"),
        _solid_box([14.2, 5.2, 4.0], [15.0, 6.2, 8.4], "#metal", "#metal"),
        _solid_box([1.0, 1.5, 7.5], [1.8, 5.2, 8.4], "#metal", "#metal"),
        _solid_box([14.2, 1.5, 7.5], [15.0, 5.2, 8.4], "#metal", "#metal"),
        # 两层可换材质层板
        _solid_box([1.8, 0.5, 7.5], [14.2, 1.5, 12.8], "#top_cap", "#top_side"),
        _solid_box([1.8, 5.2, 3.2], [14.2, 6.2, 8.4], "#top_cap", "#top_side"),
    ]
    write_json(MODELS_BLOCK / "desktop_prep_rack.json", {
        "parent": "minecraft:block/block",
        "textures": _rack_textures(),
        "elements": desktop_elements,
    })
    write_json(BLOCKSTATES / "desktop_prep_rack.json", _rack_blockstate("desktop_prep_rack"))
    desktop_display = dict(BLOCK_DISPLAY, gui={"rotation": [30, 225, 0], "translation": [0, 2.46, 0], "scale": [0.625, 0.625, 0.625]})
    write_json(MODELS_ITEM / "desktop_prep_rack.json", {
        "parent": f"{MODID}:block/desktop_prep_rack",
        "display": desktop_display,
    })

    # 3. 挂架（双态：wall 靠墙 / ceiling 吊顶）
    wall_elements = [
        _solid_box([1.0, 12.6, 0.0], [15.0, 14.8, 4.5], "#top_cap", "#top_side"),
        _solid_box([1.5, 10.6, 0.0], [2.4, 15.0, 1.0], "#metal", "#metal"),
        _solid_box([13.6, 10.6, 0.0], [14.5, 15.0, 1.0], "#metal", "#metal"),
        _solid_box([1.6, 11.2, 1.0], [2.4, 12.6, 3.3], "#metal", "#metal"),
        _solid_box([13.6, 11.2, 1.0], [14.4, 12.6, 3.3], "#metal", "#metal"),
        _solid_box([1.5, 10.6, 2.3], [14.5, 11.4, 3.3], "#metal", "#metal"),
    ]
    for hx in (3.1, 6.1, 9.1, 12.1):
        wall_elements.append(_solid_box([hx, 9.4, 2.5], [hx + 0.8, 10.6, 3.1], "#metal", "#metal"))
    write_json(MODELS_BLOCK / "hanging_prep_rack_wall.json", {
        "parent": "minecraft:block/block",
        "textures": _rack_textures(),
        "elements": wall_elements,
    })

    ceiling_elements = [
        _solid_box([2.0, 10.8, 7.5], [3.0, 16.0, 8.5], "#metal", "#metal"),
        _solid_box([13.0, 10.8, 7.5], [14.0, 16.0, 8.5], "#metal", "#metal"),
        _solid_box([1.0, 11.4, 6.5], [15.0, 13.2, 9.5], "#top_cap", "#top_side"),
        _solid_box([1.5, 9.8, 7.5], [14.5, 10.6, 8.5], "#metal", "#metal"),
    ]
    for hx in (3.1, 6.1, 9.1, 12.1):
        ceiling_elements.append(_solid_box([hx, 8.8, 7.7], [hx + 0.8, 9.8, 8.3], "#metal", "#metal"))
    write_json(MODELS_BLOCK / "hanging_prep_rack_ceiling.json", {
        "parent": "minecraft:block/block",
        "textures": _rack_textures(),
        "elements": ceiling_elements,
    })
    write_json(BLOCKSTATES / "hanging_prep_rack.json", _rack_blockstate("hanging_prep_rack_wall", "hanging_prep_rack_ceiling"))
    hanging_display = dict(BLOCK_DISPLAY, gui={"rotation": [30, 225, 0], "translation": [-2.76, -1.45, 0], "scale": [0.68, 0.68, 0.68]})
    write_json(MODELS_ITEM / "hanging_prep_rack.json", {
        "parent": f"{MODID}:block/hanging_prep_rack_wall",
        "display": hanging_display,
    })


# ------------------------------------------------------------------ 标签、战利品表、配方
def install_data() -> None:
    write_json(DATA_MOD / "tags/block/table_materials_allow.json", {"replace": False, "values": []})
    write_json(DATA_MOD / "tags/block/table_materials_deny.json", {
        "replace": False,
        "values": [
            "minecraft:grass_block",
            "minecraft:tnt",
            "minecraft:short_grass",
            "minecraft:tall_grass",
        ],
    })

    # 追加 desktop_prep_rack 到桌面装饰物抬高标签（保留原有 17 条顺序）
    td_path = DATA_MOD / "tags/block/table_decorations.json"
    td = json.loads(td_path.read_text(encoding="utf-8"))
    desktop_ref = f"{MODID}:desktop_prep_rack"
    if desktop_ref not in td["values"]:
        td["values"].append(desktop_ref)
    write_json(td_path, td)

    rack_plates = [f"{MODID}:{mat}_{shape}" for mat in PLATE_MATERIALS for shape in PLATE_SHAPES] + ["minecraft:bowl"]
    write_json(DATA_MOD / "tags/item/rack_plates.json", {"replace": False, "values": rack_plates})
    write_json(DATA_MOD / "tags/item/rack_hangables.json", {
        "replace": False,
        "values": [
            f"#{MODID}:cloth_aprons",
            f"{MODID}:iron_kitchen_knife",
            f"{MODID}:rolling_pin",
            f"{MODID}:bamboo_chopsticks",
            f"{MODID}:wooden_press_lid",
            "minecraft:shears",
            "minecraft:bucket",
        ],
    })

    for block_id in (
        "hotpot_table",
        "tall_hotpot_table",
        "multi_tier_prep_rack",
        "desktop_prep_rack",
        "hanging_prep_rack",
    ):
        write_json(DATA_MOD / f"loot_table/blocks/{block_id}.json", {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1.0,
                    "bonus_rolls": 0.0,
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                    "entries": [
                        {
                            "type": "minecraft:item",
                            "name": f"{MODID}:{block_id}",
                            "functions": [
                                {
                                    "function": "minecraft:copy_components",
                                    "source": "block_entity",
                                    "include": [f"{MODID}:table_materials"],
                                }
                            ],
                        }
                    ],
                }
            ],
        })

    # 统一配方（40 种旧桌配方精简为 2 张通用桌配方；3 种备菜架统一木板+铁件）
    write_json(DATA_MOD / "recipe/hotpot_table.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["PPP", "PBP", "PPP"],
        "key": {"P": {"tag": "minecraft:planks"}, "B": {"item": "minecraft:brick"}},
        "result": {"id": f"{MODID}:hotpot_table", "count": 4},
    })
    write_json(DATA_MOD / "recipe/tall_hotpot_table.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["PPP", "BBB"],
        "key": {"P": {"tag": "minecraft:planks"}, "B": {"item": "minecraft:bricks"}},
        "result": {"id": f"{MODID}:tall_hotpot_table", "count": 3},
    })
    write_json(DATA_MOD / "recipe/multi_tier_prep_rack.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["P P", "III", "P P"],
        "key": {"P": {"tag": "minecraft:planks"}, "I": {"item": "minecraft:iron_ingot"}},
        "result": {"id": f"{MODID}:multi_tier_prep_rack", "count": 1},
    })
    write_json(DATA_MOD / "recipe/desktop_prep_rack.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["P P", "IPI"],
        "key": {"P": {"tag": "minecraft:planks"}, "I": {"item": "minecraft:iron_ingot"}},
        "result": {"id": f"{MODID}:desktop_prep_rack", "count": 1},
    })
    write_json(DATA_MOD / "recipe/hanging_prep_rack.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["I I", "PPP", "N N"],
        "key": {
            "P": {"tag": "minecraft:planks"},
            "I": {"item": "minecraft:iron_ingot"},
            "N": {"item": "minecraft:iron_nugget"},
        },
        "result": {"id": f"{MODID}:hanging_prep_rack", "count": 1},
    })


# ------------------------------------------------------------------ 教程图标与预览图版
def _draw_tutorial_icon(kind: str) -> Image.Image:
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    wood_top = (176, 144, 90, 255)
    wood_side = (138, 108, 62, 255)
    brick = (142, 74, 56, 255)
    iron = (62, 68, 80, 255)
    iron_hi = (104, 114, 128, 255)
    outline = (36, 28, 22, 255)

    if kind == "hotpot_table":
        d.rectangle([5, 16, 26, 26], fill=brick, outline=outline)
        d.rectangle([3, 10, 28, 16], fill=wood_top, outline=outline)
        d.rectangle([4, 13, 27, 15], fill=wood_side)
    elif kind == "tall_hotpot_table":
        d.rectangle([6, 14, 25, 29], fill=brick, outline=outline)
        d.rectangle([6, 10, 25, 14], fill=wood_side, outline=outline)
        d.rectangle([3, 5, 28, 10], fill=wood_top, outline=outline)
    elif kind == "multi_tier_prep_rack":
        d.rectangle([5, 3, 7, 29], fill=iron, outline=outline)
        d.rectangle([24, 3, 26, 29], fill=iron, outline=outline)
        for y in (6, 14, 22):
            d.rectangle([6, y, 25, y + 3], fill=wood_top, outline=outline)
            d.rectangle([7, y + 2, 24, y + 3], fill=wood_side)
    elif kind == "desktop_prep_rack":
        d.rectangle([5, 7, 7, 26], fill=iron, outline=outline)
        d.rectangle([24, 7, 26, 26], fill=iron, outline=outline)
        d.rectangle([6, 11, 23, 15], fill=wood_top, outline=outline)
        d.rectangle([8, 20, 26, 24], fill=wood_top, outline=outline)
    elif kind == "hanging_prep_rack":
        d.rectangle([4, 6, 27, 10], fill=wood_top, outline=outline)
        d.rectangle([5, 12, 26, 14], fill=iron_hi, outline=outline)
        for hx in (7, 12, 18, 23):
            d.rectangle([hx, 14, hx + 2, 19], fill=iron)
    return im


def install_icons_and_preview(textures: dict[str, Image.Image]) -> None:
    ICONS_DIR.mkdir(parents=True, exist_ok=True)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (32 * 6, 64), (30, 32, 36, 255))
    for i, (name, tex) in enumerate(textures.items()):
        sheet.paste(tex.resize((32, 32), Image.Resampling.NEAREST), (i * 32, 0))
    for i, kind in enumerate((
        "hotpot_table",
        "tall_hotpot_table",
        "multi_tier_prep_rack",
        "desktop_prep_rack",
        "hanging_prep_rack",
    )):
        icon = _draw_tutorial_icon(kind)
        save_png(icon, ICONS_DIR / f"{MODID}__{kind}.png")
        sheet.paste(icon, (i * 32, 32), icon)
    vanilla_nugget = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures/item/iron_nugget.png"
    dst_nugget = ICONS_DIR / "minecraft__iron_nugget.png"
    if vanilla_nugget.is_file() and not dst_nugget.is_file():
        nug = Image.open(vanilla_nugget).convert("RGBA").resize((32, 32), Image.Resampling.NEAREST)
        save_png(nug, dst_nugget)
    save_png(sheet, OUT_DIR / "sheet.png")


def main() -> None:
    textures = install_textures()
    install_tables()
    install_prep_racks()
    install_data()
    install_icons_and_preview(textures)
    print("Phase 4 统一家具与备菜架资源已生成。")


if __name__ == "__main__":
    main()
