"""0.3.1-alpha Phase 2：独立橙子树与 9 种独立作物（及野生植株）资产与数据包生成脚本。

用法：
    python mod/tools/gen_botanicals.py          # 生成 16x16 预览图版 tools/out/botanicals/sheet.png
    python mod/tools/gen_botanicals.py install  # 写入 common/ 与 neoforge/ 全部贴图、模型、方块状态、战利品表、世界生成、标签与配方

说明：不改动 tools/gen_tables.py 等已登记哈希的 mineable/axe.json；橙木靠加入 minecraft:logs_that_burn
自动继承原版 #minecraft:logs 的斧头挖掘与木炭烧炼规则。
作者：mryt233
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path
from PIL import Image

TOOLS = Path(__file__).resolve().parent
MOD = TOOLS.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "common/src/main/resources"
ASSETS = RES / "assets" / MODID
DATA = RES / "data" / MODID
MC_DATA = RES / "data/minecraft"
NEO_DATA = MOD / "neoforge/src/main/resources/data" / MODID
OUT = TOOLS / "out/botanicals"

T = (0, 0, 0, 0)
# mryt233 改：树叶镂空像素在 alpha=0 下填充深林荫绿 RGB，防止高品质 Mipmap 黑边并让原版流畅(Fast)不透明模式呈现自然深绿树荫底。
LEAF_HOLE_RGBA = (18, 48, 14, 0)
ORANGE_LEAVES_STYLE = "azalea"  # 可选 "azalea"（方案A 杜鹃叶风 ~23%透明） / "jungle"（方案B 丛林叶风 ~31%透明）

CROPS = [
    "scallion",
    "cilantro",
    "soybean",
    "garlic",
    "tomato",
    "chili_pepper",
    "peanut",
    "greens",
    "zhe_er_gen",
]

TEMPERATE_CROPS = ["scallion", "cilantro", "soybean", "garlic", "greens"]
WARM_CROPS = ["tomato", "chili_pepper", "peanut"]
HUMID_CROPS = ["zhe_er_gen"]


def blank() -> Image.Image:
    return Image.new("RGBA", (16, 16), T)


def write_json(path: Path, obj: dict | list) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ------------------------------------------------------------------ 橙木与树叶 / 树苗贴图（mryt233 改：写实柑橘果树风格全面重绘）
def tex_orange_log() -> Image.Image:
    """光滑暖灰褐柑橘树皮（浅竖向皮孔与细润青灰地衣纹理，区分橡木粗裂纹）。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    bark = [
        (72, 62, 50, 255),
        (92, 80, 66, 255),
        (110, 96, 80, 255),
        (128, 114, 96, 255),
        (100, 96, 74, 255),  # 浅橄榄苔斑点缀
    ]
    for y in range(16):
        for x in range(16):
            h = (x * 11 + y * 5 + (x // 3) * 7) % 13
            idx = 1 if h < 4 else (2 if h < 9 else (3 if h < 12 else 0))
            if (x in (2, 7, 13)) and (y + x) % 6 in (0, 1):
                idx = 0
            elif (x in (4, 9, 14)) and (y + x * 2) % 7 == 0:
                idx = 4
            elif x % 5 == 1 and y % 5 in (1, 2):
                idx = 3
            px[x, y] = bark[idx]
    return im


def tex_orange_log_top() -> Image.Image:
    """柑橘原木端面：外圈光滑灰褐树皮 + 温润浅蜜色细密年轮心材。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    bark_out = (72, 62, 50, 255)
    bark_in = (104, 90, 74, 255)
    rings = [
        (232, 206, 154, 255),
        (214, 184, 128, 255),
        (194, 160, 106, 255),
        (168, 134, 86, 255),
    ]
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.6:
                px[x, y] = bark_out if (x + y) % 3 else bark_in
            elif d > 5.6:
                px[x, y] = bark_in
            elif d > 4.2:
                px[x, y] = rings[0] if (x + y) % 4 else rings[1]
            elif d > 2.8:
                px[x, y] = rings[2] if (x * y) % 3 else rings[1]
            elif d > 1.3:
                px[x, y] = rings[0]
            else:
                px[x, y] = rings[3]
    return im


def tex_stripped_orange_log() -> Image.Image:
    """去皮橙木侧面：细腻柔滑的浅蜜金柑橘边材。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    wood = [
        (192, 160, 108, 255),
        (210, 178, 122, 255),
        (226, 196, 140, 255),
        (238, 212, 160, 255),
    ]
    for y in range(16):
        for x in range(16):
            streak = (x * 5 + y * 3 + (x % 4)) % 9
            idx = 1 if streak < 4 else (2 if streak < 7 else (0 if streak == 7 else 3))
            if x % 5 == 2 and y % 4 != 1:
                idx = 3
            px[x, y] = wood[idx]
    return im


def tex_stripped_orange_log_top() -> Image.Image:
    im = tex_orange_log_top()
    px = im.load()
    edge = (192, 160, 108, 255)
    edge_hi = (212, 180, 124, 255)
    for y in range(16):
        for x in range(16):
            if max(abs(x - 7.5), abs(y - 7.5)) > 5.6:
                px[x, y] = edge if (x + y) % 2 == 0 else edge_hi
    return im


# mryt233 改（2026-11-22）：参考原版杜鹃叶(~23%透明)与丛林叶(~31%透明)重绘双方案高透明橙叶。
_AZALEA_GRID = [
    "1.0233210..21..2",
    "2..12321.1232..1",
    "0.233201223210.1",
    "2232102.232101.0",
    "21.12....12.21..",
    "...22...01..3221",
    "210.01232210.232",
    "321..02332101221",
    "0..1233211021.22",
    "1202332212101.11",
    "23210.221..21.23",
    "1..01231...232.1",
    ".012012.232210.1",
    ".23210.233210101",
    "2321010..120232.",
    "12012..0...01231",
]

_JUNGLE_GRID = [
    "..22.23.3321.12.",
    "2211.2323321.012",
    "3221..232.....12",
    "1321..10...232.1",
    "221.1232..12332.",
    "2..2101....2321.",
    "..2321..123.2212",
    "..1220.2321.0231",
    ".12332212...012.",
    "123.321102..1.0.",
    "2321.2321..12321",
    "21.1..2321.1231.",
    "20..12.232...022",
    "12..21...1..1.21",
    "02.1212...2321.1",
    "1.0.1232.332211.",
]

_CITRUS_LEAF_PALETTE = {
    ".": LEAF_HOLE_RGBA,
    "0": (30, 78, 22, 255),
    "1": (48, 114, 34, 255),
    "2": (74, 148, 46, 255),
    "3": (110, 182, 60, 255),
}


def _citrus_leaf_base(style: str = ORANGE_LEAVES_STYLE) -> Image.Image:
    """高透明无缝柑橘树叶底图：支持杜鹃叶风('azalea')与丛林叶风('jungle')。"""
    grid = _JUNGLE_GRID if style == "jungle" else _AZALEA_GRID
    im = blank()
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            px[x, y] = _CITRUS_LEAF_PALETTE[ch]
    return im


def tex_orange_leaves(stage: int, style: str = ORANGE_LEAVES_STYLE) -> Image.Image:
    """四阶段橙叶（0 绿叶 / 1 五瓣雪白橘花 / 2 饱满青橘 / 3 圆润大颗金熟橙），保留高透明树荫缝隙。"""
    im = _citrus_leaf_base(style)
    px = im.load()
    leaf_dk = (30, 78, 22, 255)
    is_jungle = style == "jungle"

    if stage == 1:
        blossoms = [(3, 2), (10, 1), (4, 8), (12, 10)] if is_jungle else [(4, 2), (11, 2), (5, 8), (11, 13)]
        for bx, by in blossoms:
            px[bx % 16, by % 16] = (248, 208, 66, 255)
            for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                px[(bx + dx) % 16, (by + dy) % 16] = (255, 255, 252, 255)
            for dx, dy in [(-1, -1), (1, 1)]:
                nx, ny = (bx + dx) % 16, (by + dy) % 16
                if px[nx, ny][3] > 0:
                    px[nx, ny] = (242, 240, 228, 255)
    elif stage == 2:
        # mryt233 改：提高青橘幼果黄绿高光与暗叶环对比度，避免和树叶混在一起。
        grid = [
            [(228, 250, 132, 255), (190, 232, 82, 255), (154, 206, 52, 255)],
            [(190, 232, 82, 255), (168, 218, 64, 255), (110, 162, 30, 255)],
            [(154, 206, 52, 255), (110, 162, 30, 255), (82, 132, 24, 255)],
        ]
        rim = (26, 68, 18, 255)
        fruits = [(2, 1), (10, 4), (4, 9)] if is_jungle else [(3, 1), (9, 6), (2, 11)]
        for fx, fy in fruits:
            for ox, oy in ((fx + 1, fy - 1), (fx + 1, fy + 3), (fx - 1, fy + 1), (fx + 3, fy + 1)):
                if 0 <= ox < 16 and 0 <= oy < 16 and px[ox, oy][3] > 0:
                    px[ox, oy] = rim
            for dy in range(3):
                for dx in range(3):
                    px[(fx + dx) % 16, (fy + dy) % 16] = grid[dy][dx]
    elif stage == 3:
        hi = (255, 224, 118, 255)
        lit = (252, 176, 46, 255)
        mid = (242, 134, 18, 255)
        sh = (196, 76, 8, 255)
        dk = (144, 48, 6, 255)
        fruit_map = [
            [(1, 0, leaf_dk), (2, 0, lit)],
            [(0, 1, lit), (1, 1, hi), (2, 1, mid), (3, 1, sh)],
            [(0, 2, mid), (1, 2, lit), (2, 2, sh), (3, 2, dk)],
            [(1, 3, sh), (2, 3, dk)],
        ]
        fruits = [(1, 0), (11, 3), (3, 8)] if is_jungle else [(2, 0), (8, 6), (2, 10)]
        for fx, fy in fruits:
            for row in fruit_map:
                for dx, dy, col in row:
                    if 0 <= fx + dx < 16 and 0 <= fy + dy < 16:
                        px[fx + dx, fy + dy] = col
    return im


# ------------------------------------------------------------------ 作物与树苗公共原版苔绿调色板（mryt233 改）
P_MOSS = {
    "d": (38, 98, 36, 255),
    "m": (48, 120, 44, 255),
    "l": (76, 148, 44, 255),
    "h": (92, 174, 52, 255),
    "s": (64, 104, 40, 255),
}
P_CHIVE = {
    "d": (38, 94, 42, 255),
    "m": (52, 120, 48, 255),
    "l": (76, 148, 52, 255),
    "h": (94, 174, 60, 255),
}
P_FOREST = {
    "d": (34, 92, 34, 255),
    "m": (46, 118, 42, 255),
    "l": (72, 146, 44, 255),
    "h": (90, 172, 54, 255),
    "s": (56, 94, 34, 255),
}


def _sprite(rows: list[str], pal: dict[str, tuple[int, int, int, int]]) -> Image.Image:
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), "Invalid 16x16 sprite grid"
    im = blank()
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = pal[ch]
    return im


def tex_orange_sapling() -> Image.Image:
    """仿原版橡树苗剪影的分叉柑橘果苗：暖灰褐幼干 + 杜鹃风镂空叶簇 + 零星橘花小果（根部贴齐 y=15）。"""
    pal = {
        "t": (70, 58, 46, 255),
        "T": (96, 82, 66, 255),
        "b": (122, 106, 86, 255),
        "d": (36, 98, 30, 255),
        "m": (52, 126, 38, 255),
        "l": (78, 156, 46, 255),
        "h": (98, 180, 56, 255),
        "w": (236, 240, 224, 255),
        "W": (248, 250, 242, 255),
        "o": (222, 116, 18, 255),
        "O": (248, 178, 52, 255),
        "R": (182, 74, 10, 255),
    }
    grid = [
        "................",
        "................",
        "......hlm.......",
        ".....hlmdd......",
        "....hl.md.hlm...",
        "...hmlm..hlmmd..",
        "...md.Oomdm.wW..",
        "....dmoR.md.d...",
        "..hl..Tbm.dm....",
        ".hlmd.tT.mmd....",
        "..mdd.tTbT......",
        "...d..bTt.......",
        "......tT........",
        "......tT........",
        ".....btTt.......",
        ".....ttTtb......",
    ]
    return _sprite(grid, pal)


# ------------------------------------------------------------------ 9 种耕地作物 (stage 0..3) 与野生植物贴图（mryt233 重绘）
def crop_scallion(stage: int) -> Image.Image:
    pal = {
        **P_CHIVE,
        "i": (146, 156, 120, 255),
        "w": (190, 196, 166, 255),
        "W": (220, 224, 198, 255),
        "q": (184, 194, 146, 255),
        "f": (224, 230, 196, 255),
        "F": (244, 246, 228, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "....h.....l.....", "....m..h..m..l..", "...d...m..d..m..", "...d..md.dm.d...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "..h.......h.....", "..l...h...l..h..", "...m..l..m...l..", "...m..m..m..m...",
            "...d.dm..d.dm...", "..dd.dm.dm.dm...", "..dd.im.im.di...", "..ii.wi.wi.ii...",
        ],
        [
            "................", "................", "................", "................",
            ".h......h.....h.", "..l..h..l..h..l.", "..m..l..m..l.m..", "...m.m..m..m.m..",
            "...d.m.dm..d.m..", "..dd.d.dm.dd.d..", "..dm.d.dm.dm.d..", "..dm.i.mi.dm.i..",
            "..ii.w.wi.ii.w..", "..wi.w.Wi.wi.w..", "..Wi.W.wi.Wi.W..", "..wW.w.wi.wW.w..",
        ],
        [
            "....FF....FF....", "...fFFq..fFFq...", ".h..qq..h.qq..h.", "..l.m..l..m..l..",
            "..m.l..m..l..m..", "...m.m.d..m..m..", "..hd.m.m.dd.md..", "..md.d.d.dm.dd..",
            "..md.d.d.dm.dm..", "..dd.i.m.ii.dm..", "..ii.w.i.wi.ii..", "..wi.w.w.Wi.wi..",
            "..Wi.W.W.wi.Wi..", "..wW.W.w.Wi.wW..", "..ww.w.W.ww.ww..", "..iw.i.w.ii.iw..",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_cilantro(stage: int) -> Image.Image:
    pal = {
        **P_MOSS,
        "u": (214, 222, 198, 255),
        "U": (244, 246, 232, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "....hl....hl....", "...lmd...lmd....", "....s..hl..s....", "...ds..ms.ds....",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            ".......hl.......", "...hl..md..hl...", "..lmd..s..lmd...", "...md.lmd.md....",
            "....s.dmd..s....", "..d.s..s..ds....", "..msd..s..sd....", "..dss..s..ss....",
        ],
        [
            "................", "................", "................", "................",
            "................", "....hl....hl....", "..hlmdh..lmdhl..", "..lmdml..mdmld..",
            "...dmdd..dmdm...", "..hlms.hl.smhl..", "..lmds.md.smdl..", "...sdd.sd.dds...",
            "..dss..sd..ssd..", "...ss..ss..ss...", "..dss..ds..ssd..", "..sss..ss..sss..",
        ],
        [
            "................", "....Uu....Uu....", "...UuuU..UuuU...", "..h.ss.hl.ss.h..",
            "..lmd..md..mdl..", "..dmd.hlmh.dmd..", "..hlm.lmdl.mlh..", "..lmd.ddmd.dml..",
            "...mdh.sm.hdmd..", "..dmdm.sd.mmd...", "..smdd.ss.dmds..", "..ss.d.ss.d.ss..",
            "..ss.s.sd.s.ss..", "..ds.s.ss.s.sd..", "..ss.s.ss.s.ss..", "..dd.s.ds.s.dd..",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_soybean(stage: int) -> Image.Image:
    pal = {
        **P_MOSS,
        "b": (222, 214, 228, 255),
        "B": (246, 242, 250, 255),
        "p": (146, 118, 48, 255),
        "P": (188, 160, 70, 255),
        "y": (218, 192, 96, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "...hl......hl...", "..lmd.....lmd...", "...sd......sd...", "...ss......ss...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", ".......hl.......",
            "...hl..md..hl...", "..lmd..sd..md...", "...sd.lmd..sd...", "..dm..dss..md...",
            "..ss...s...ss...", "..ss..ds...ss...", "..ds...s...ds...", "..ss...s...ss...",
        ],
        [
            "................", "................", "................", "................",
            "....hl....hl....", "..hlmdhl..mdhl..", "..lmdmld.Bbmdd..", "...dmdd.lmd.s...",
            "..Bbm.hl.dmdhl..", "..lmd.md.sm.md..", "...sd.sd.sd.sd..", "..dm..ss.dmd.s..",
            "..ss..ds..ss.d..", "..ss..ss..ss....", "..ds..ss..ds....", "..ss..ds..ss....",
        ],
        [
            "................", "................", "....hl....hl....", "..hlmdhl.lmdhl..",
            "..mdmldm.mdmdd..", "..ddmdd..ddmd...", ".hl.s.yP..s.hl..", ".md.yPps.yP.md..",
            ".dmdpPs..pPddm..", "..s.pP.yP.d.s...", "..d.s.yPps..d...", "..s.yP.pP.yP.s..",
            "..s.pP..s.pP.s..", "..d..s..d..s.d..", "..s..d..s..s.s..", "..s..s..s..d.s..",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_garlic(stage: int) -> Image.Image:
    pal = {
        **P_CHIVE,
        "c": (100, 162, 68, 255),
        "t": (148, 122, 138, 255),
        "v": (166, 118, 146, 255),
        "g": (176, 170, 174, 255),
        "G": (224, 218, 222, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "...h...h....h...", "...l...l....l...", "...m...m....m...", "..dm..dd...md...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", ".......h........",
            "..h....l....h...", "..l...hm....l...", "..m...lm....m...", "..dm..md...md...",
            "..dm..dd...dd...", "..dd..dm...dm...", "..dd..td...td...", "..tt..tt...tt...",
        ],
        [
            "................", "................", "................", "................",
            "..h....h.....h..", "..l....l..h..l..", "..m..h.m..l..m..", "..dm.l.m..m..dm.",
            "..dm.m.d..dm.dm.", "..dd.m.d..dd.dd.", "..dd.d.d..dd.dd.", "..td.d.t..td.td.",
            "..tt.t.t..tt.tt.", "..vt.t.v..vt.vt.", "..gv.v.g..gv.gv.", "..gg.g.g..gg.gg.",
        ],
        [
            ".......cc.......", "......c..h......", "..h...l..l...h..", "..l...m..m...l..",
            "..m.h.m..m.h.m..", "..m.l.d..d.l.m..", "..d.m.d..d.m.d..", "..d.d.d..d.d.d..",
            "..m.d.m..m.d.m..", "..d.d.d..d.d.d..", "..t.d.t..t.d.t..", "..t.t.t..t.t.t..",
            "..vGv.t..vGv.t..", ".gGGv.Gg.gGGv.g.", ".gvGg.GG.gvGg.G.", ".tggt.gt.tggt.t.",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_tomato(stage: int) -> Image.Image:
    pal = {
        **P_FOREST,
        "y": (222, 180, 46, 255),
        "Y": (246, 218, 78, 255),
        "u": (90, 148, 46, 255),
        "r": (144, 24, 18, 255),
        "R": (196, 42, 28, 255),
        "o": (228, 84, 54, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "...hl......hl...", "..lmd.....lmd...", "...sd......sd...", "...ss......ss...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", ".......hl.......",
            "...hl..md..hl...", "..lmd..sd..md...", "...sd.lmd..sd...", "..dm...s...md...",
            "..ss..ds...ss...", "..ds...s...ds...", "..ss...s...ss...", "..ds...s...ds...",
        ],
        [
            "................", "................", "................", ".....hl..hl.....",
            "..hl.md..md.hl..", "..md.sd.hlmdmd..", "..sd.yY.mdd.sd..", "..dm.sd.s..yY...",
            "..sd.lmdmd.sd...", "..ss..s.u.lmd...", "..ds.yY.s..sd...", "..ss..s.dm.ss...",
            "..ds..s.ss.ds...", "..ss..s.ds.ss...", "..ss..s.ss.ss...", "..ds..s.ds.ds...",
        ],
        [
            "....hl....hl....", "..hlmdhl..mdhl..", "..mdmldm.hlmdmd.", "..sd.dd..mdd.sd.",
            "..dm.oR..dd.oR..", "..sd.Rr.hl..Rr..", "..ss.dd.md.ddd..", "..ds.hl.dd.hl...",
            ".oR..md.oR.md...", ".Rr.dms.Rr..s...", ".dd..s..dd.oR...", "..s..d...s.Rr...",
            "..d..s...d.dd...", "..s..d...s..s...", "..s..s...s..d...", "..d..s...d..s...",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_chili_pepper(stage: int) -> Image.Image:
    pal = {
        **P_FOREST,
        "w": (226, 224, 214, 255),
        "W": (248, 246, 238, 255),
        "u": (78, 142, 46, 255),
        "r": (140, 22, 18, 255),
        "R": (190, 34, 24, 255),
        "o": (224, 66, 44, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "....hl....hl....", "...lmd...lmd....", "....sd....sd....", "....ss....ss....",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "....hl....hl....", "..hlmdhl..mdhl..", "..lmd.sd.lmd.s..", "...sd.dm..sd.d..",
            "...ss.ds..ss.d..", "...ds..s..ds....", "...ss..s..ss....", "...ds..s..ds....",
        ],
        [
            "................", "................", "................", "................",
            "................", "...hl..hl..hl...", "..lmdh.md.lmdh..", "..dmd.Wws.dmd...",
            "..sd.hlmd..sd...", "..dm.mdu.Wws.d..", "..ss..s.dm.ss...", "..ds..s.ds.ds...",
            "..ss..s..s.ss...", "..ds..s..s.ds...", "..ss..s..s.ss...", "..dd..s..s.dd...",
        ],
        [
            "................", "................", "................", "...hl..hl..hl...",
            "..hlmd.mdhlmdh..", "..mdm..dmdmld...", "..dmd.oR..mdm...", "..s.d.Rr..s.oR..",
            ".oR.s..r.oR.Rr..", ".Rr.s..d.Rr..r..", "..r.s..s..r..d..", "..s.d..s..s..s..",
            "..d.s..d..d..s..", "..s.s..s..s..d..", "..s.d..s..s..s..", "..d.s..s..d..s..",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_peanut(stage: int) -> Image.Image:
    pal = {
        **P_MOSS,
        "y": (218, 174, 42, 255),
        "Y": (246, 208, 68, 255),
        "n": (136, 98, 48, 255),
        "N": (180, 138, 72, 255),
        "k": (212, 174, 102, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "..hl.......hl...", "..md..hl...md...", "..sd..sd...sd...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "...hl......hl...", "..lmd..hl..mdh..",
            "..dmd.lmd..dmd..", "...s..dmd...s...", "..ds...s...ds...", "..ss...s...ss...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "..hl...hl...hl..",
            ".lmdh..md..hmdl.", ".dmd.yYmd.yYdmd.", "..s.hldmd.hl.s..", ".hl.md.s..md.hl.",
            ".md.sd.ds.sd.md.", ".ds..s.ss..s.ds.", "..s..s..s..s..s.", "..s..d..s..d..s.",
        ],
        [
            "................", "................", "................", "................",
            "................", "..hl...hl...hl..", ".hlmdh.mdh.hmdl.", ".mdmld.mld.dmdd.",
            ".dmd.hlmdhl.dmd.", "..s..mdmddm..s..", "..d..sd..sd..d..", "..s..ss..ss..s..",
            ".kNn.ds..sd.kNn.", ".Nkn.kNn.kN.Nkn.", ".nNn.Nkn.Nn.nNn.", "..n..nNn.n...n..",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_greens(stage: int) -> Image.Image:
    pal = {
        "d": (32, 92, 34, 255),
        "m": (44, 120, 42, 255),
        "l": (70, 148, 46, 255),
        "h": (90, 174, 56, 255),
        "s": (136, 162, 126, 255),
        "S": (172, 196, 158, 255),
        "w": (200, 218, 184, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "....hl....hl....", "....md....md....", "....sS....Ss....",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "...hl..hl..hl...", "..lmd..md..mdl..",
            "..dmd.lmdl.dmd..", "...sS.sSss.Ss...", "...wS..wS..Sw...", "...ss..ss..ss...",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "...hl.hlhl.hl...",
            "..lmd.mdmd.mdl..", ".hmld.lmdl.dlmh.", ".mdmd.dmdd.mdmd.", ".dmds.sSss.sdmd.",
            "..sS..wSsw..Ss..", "..wS..wSSs..Sw..", "..ss..sSSs..ss..", "...s..ssss..s...",
        ],
        [
            "................", "................", "................", "................",
            "....hl.hl.hl....", "..hlmd.md.mdlh..", ".hlmdmdlmldmdlh.", ".mdmdd.dm.ddmdm.",
            ".dmdm.hlmh.mdmd.", ".dsms.mdmd.smsd.", "..sSs.dmdd.sSs..", "..wSS.sSss.SSw..",
            "..wSw.wSSw.wSw..", "..sSs.wSSs.sSs..", "...ss.sSSs.ss...", "...ss.ssss.ss...",
        ],
    ]
    return _sprite(grids[stage], pal)


def crop_zhe_er_gen(stage: int) -> Image.Image:
    pal = {
        **P_FOREST,
        "p": (102, 48, 70, 255),
        "P": (136, 68, 94, 255),
        "w": (222, 218, 206, 255),
        "W": (246, 244, 234, 255),
        "y": (216, 194, 68, 255),
        "z": (164, 144, 106, 255),
        "Z": (202, 180, 140, 255),
    }
    grids = [
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            "...hl......hl...", "..dmd.....dmd...", "...P.......P....", "...p.......p....",
        ],
        [
            "................", "................", "................", "................",
            "................", "................", "................", "................",
            ".......hl.......", "...hl.dmd..hl...", "..dmd..P..dmd...", "...P..hlm..P....",
            "...p..dmd..p....", "..dp...P...pd...", "...p...p...p....", "...p...p...p....",
        ],
        [
            "................", "................", "................", "................",
            "................", "....hl....hl....", "..hlmdh..hmdhl..", "..dmdmd..dmdmd..",
            "...dmP.hl.Pmd...", "..hlP.dmd..Phl..", "..dmd..P..dmd...", "...P..pdp..P....",
            "...p...P...p....", "..dp...p...pd...", "...p...p...p....", "...p...p...p....",
        ],
        [
            "................", "...W......W.....", "..WyW....WyW....", "...w..W...w.....",
            "..hl.WyW..hl.hl.", "..dmd.w..dmd.md.", "..mP..hl..P..dm.", "..hlm.dmd.hlm.P.",
            "..dmd..P..dmd.p.", "...P..pdp..P..p.", "...p...P...p..p.", "..dp...p...p.dp.",
            "...p...p...p..p.", "..zp...p..zp..p.", ".Zzz..Zzp.Zzz.p.", "..z...zz...z..p.",
        ],
    ]
    return _sprite(grids[stage], pal)


CROP_PAINTERS = {
    "scallion": crop_scallion,
    "cilantro": crop_cilantro,
    "soybean": crop_soybean,
    "garlic": crop_garlic,
    "tomato": crop_tomato,
    "chili_pepper": crop_chili_pepper,
    "peanut": crop_peanut,
    "greens": crop_greens,
    "zhe_er_gen": crop_zhe_er_gen,
}


def tex_wild(crop_id: str) -> Image.Image:
    """基于重绘 stage3 底层叠加自然错落的原版色野草簇。"""
    im = CROP_PAINTERS[crop_id](3).copy()
    px = im.load()
    g_lo = (36, 90, 32, 255)
    g_mid = (52, 114, 38, 255)
    g_hi = (76, 142, 46, 255)
    tufts = [
        (0, 15, g_lo), (0, 14, g_mid), (1, 15, g_mid), (1, 13, g_hi),
        (4, 15, g_lo), (4, 14, g_hi),
        (10, 15, g_lo), (11, 15, g_mid), (11, 14, g_hi),
        (14, 15, g_lo), (14, 14, g_mid), (15, 15, g_lo), (15, 13, g_hi),
    ]
    for tx, ty, col in tufts:
        if px[tx, ty][3] == 0:
            px[tx, ty] = col
    return im


def generate_assets(style: str = ORANGE_LEAVES_STYLE) -> dict[str, list[str]]:
    summary = {"textures": [], "blockstates": [], "block_models": [], "item_models": []}

    def save_png(rel: str, im: Image.Image) -> None:
        p = ASSETS / "textures" / f"{rel}.png"
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p)
        summary["textures"].append(rel)

    def save_bs(name: str, data: dict) -> None:
        write_json(ASSETS / "blockstates" / f"{name}.json", data)
        summary["blockstates"].append(name)

    def save_bm(name: str, data: dict) -> None:
        write_json(ASSETS / "models/block" / f"{name}.json", data)
        summary["block_models"].append(name)

    def save_im(name: str, data: dict) -> None:
        write_json(ASSETS / "models/item" / f"{name}.json", data)
        summary["item_models"].append(name)

    # 橙子树方块贴图/模型/方块状态
    log_im = tex_orange_log()
    slog_im = tex_stripped_orange_log()
    leaves3_im = tex_orange_leaves(3, style)
    sapling_im = tex_orange_sapling()
    save_png("block/orange_log", log_im)
    save_png("block/orange_log_top", tex_orange_log_top())
    save_png("block/stripped_orange_log", slog_im)
    save_png("block/stripped_orange_log_top", tex_stripped_orange_log_top())
    save_png("block/orange_sapling", sapling_im)
    for stage in range(4):
        save_png(f"block/orange_leaves_stage{stage}", tex_orange_leaves(stage, style))

    # 32x32 教程图标
    icons_dir = MOD / "tutorial/img/icons"
    icons_dir.mkdir(parents=True, exist_ok=True)
    for icon_id, src_im in [
        ("orange_log", log_im),
        ("orange_wood", log_im),
        ("stripped_orange_log", slog_im),
        ("stripped_orange_wood", slog_im),
        ("orange_leaves", leaves3_im),
        ("orange_sapling", sapling_im),
    ]:
        src_im.resize((32, 32), Image.NEAREST).save(icons_dir / f"{MODID}__{icon_id}.png")

    for name, tex_side, tex_end in [
        ("orange_log", "block/orange_log", "block/orange_log_top"),
        ("stripped_orange_log", "block/stripped_orange_log", "block/stripped_orange_log_top"),
    ]:
        save_bm(name, {"parent": "minecraft:block/cube_column",
                       "textures": {"end": f"{MODID}:{tex_end}", "side": f"{MODID}:{tex_side}"}})
        save_bm(f"{name}_horizontal", {"parent": "minecraft:block/cube_column_horizontal",
                                       "textures": {"end": f"{MODID}:{tex_end}", "side": f"{MODID}:{tex_side}"}})
        save_bs(name, {"variants": {
            "axis=x": {"model": f"{MODID}:block/{name}_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": f"{MODID}:block/{name}"},
            "axis=z": {"model": f"{MODID}:block/{name}_horizontal", "x": 90},
        }})
        save_im(name, {"parent": f"{MODID}:block/{name}"})

    for name, tex in [("orange_wood", "block/orange_log"), ("stripped_orange_wood", "block/stripped_orange_log")]:
        save_bm(name, {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:{tex}"}})
        save_bs(name, {"variants": {
            "axis=x": {"model": f"{MODID}:block/{name}", "x": 90, "y": 90},
            "axis=y": {"model": f"{MODID}:block/{name}"},
            "axis=z": {"model": f"{MODID}:block/{name}", "x": 90},
        }})
        save_im(name, {"parent": f"{MODID}:block/{name}"})

    for stage in range(4):
        save_bm(f"orange_leaves_stage{stage}", {
            "parent": "minecraft:block/leaves",
            "textures": {"all": f"{MODID}:block/orange_leaves_stage{stage}"},
        })
    save_bs("orange_leaves", {
        "variants": {f"stage={s}": {"model": f"{MODID}:block/orange_leaves_stage{s}"} for s in range(4)}
    })
    save_im("orange_leaves", {"parent": f"{MODID}:block/orange_leaves_stage0"})

    save_bm("orange_sapling", {"parent": "minecraft:block/cross", "textures": {"cross": f"{MODID}:block/orange_sapling"}})
    save_bs("orange_sapling", {"variants": {"": {"model": f"{MODID}:block/orange_sapling"}}})
    save_im("orange_sapling", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:block/orange_sapling"}})

    # 9 种耕地作物 & 野生植株
    age_to_stage = {0: 0, 1: 0, 2: 1, 3: 1, 4: 2, 5: 2, 6: 2, 7: 3}
    for crop in CROPS:
        for stage in range(4):
            save_png(f"block/{crop}_stage{stage}", CROP_PAINTERS[crop](stage))
            save_bm(f"{crop}_stage{stage}", {
                "parent": "minecraft:block/crop",
                "textures": {"crop": f"{MODID}:block/{crop}_stage{stage}"},
            })
        save_bs(f"{crop}_crop", {
            "variants": {f"age={age}": {"model": f"{MODID}:block/{crop}_stage{st}"} for age, st in age_to_stage.items()}
        })

        wild_id = f"wild_{crop}"
        wild_im = tex_wild(crop)
        save_png(f"block/{wild_id}", wild_im)
        wild_im.resize((32, 32), Image.NEAREST).save(icons_dir / f"{MODID}__{wild_id}.png")
        save_bm(wild_id, {"parent": "minecraft:block/cross", "textures": {"cross": f"{MODID}:block/{wild_id}"}})
        save_bs(wild_id, {"variants": {"": {"model": f"{MODID}:block/{wild_id}"}}})
        save_im(wild_id, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:block/{wild_id}"}})

    return summary


# ------------------------------------------------------------------ 数据包：战利品表 / 世界生成 / 标签 / 配方
def silk_or_shears_condition() -> dict:
    return {
        "condition": "minecraft:any_of",
        "terms": [
            {
                "condition": "minecraft:match_tool",
                "predicate": {"items": "minecraft:shears"},
            },
            {
                "condition": "minecraft:match_tool",
                "predicate": {
                    "predicates": {
                        "minecraft:enchantments": [
                            {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                        ]
                    }
                },
            },
        ],
    }


def self_drop_loot(block_id: str) -> dict:
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{block_id}"}],
            }
        ],
    }


def orange_leaves_loot() -> dict:
    fortune_sapling = {
        "condition": "minecraft:table_bonus",
        "enchantment": "minecraft:fortune",
        "chances": [0.05, 0.0625, 0.083333336, 0.1],
    }
    fortune_stick = {
        "condition": "minecraft:table_bonus",
        "enchantment": "minecraft:fortune",
        "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1],
    }
    silk = silk_or_shears_condition()
    not_silk = {"condition": "minecraft:inverted", "term": silk}
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [
                    {
                        "type": "minecraft:alternatives",
                        "children": [
                            {"type": "minecraft:item", "name": f"{MODID}:orange_leaves", "conditions": [silk]},
                            {
                                "type": "minecraft:item",
                                "name": f"{MODID}:orange_sapling",
                                "conditions": [{"condition": "minecraft:survives_explosion"}, fortune_sapling],
                            },
                        ],
                    }
                ],
            },
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "conditions": [not_silk],
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": "minecraft:stick",
                        "conditions": [fortune_stick],
                        "functions": [
                            {
                                "function": "minecraft:set_count",
                                "count": {"type": "minecraft:uniform", "min": 1, "max": 2},
                                "add": False,
                            },
                            {"function": "minecraft:explosion_decay"},
                        ],
                    }
                ],
            },
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "conditions": [
                    not_silk,
                    {
                        "condition": "minecraft:block_state_property",
                        "block": f"{MODID}:orange_leaves",
                        "properties": {"stage": "3"},
                    },
                ],
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"{MODID}:orange",
                        "functions": [
                            {
                                "function": "minecraft:set_count",
                                "count": {"type": "minecraft:uniform", "min": 1, "max": 2},
                                "add": False,
                            },
                            {"function": "minecraft:explosion_decay"},
                        ],
                    }
                ],
            },
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "conditions": [
                    not_silk,
                    {
                        "condition": "minecraft:block_state_property",
                        "block": f"{MODID}:orange_leaves",
                        "properties": {"stage": "2"},
                    },
                    {"condition": "minecraft:random_chance", "chance": 0.25},
                ],
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"{MODID}:orange",
                        "functions": [{"function": "minecraft:explosion_decay"}],
                    }
                ],
            },
        ],
    }


def crop_loot(crop_id: str) -> dict:
    mature = {
        "condition": "minecraft:block_state_property",
        "block": f"{MODID}:{crop_id}_crop",
        "properties": {"age": "7"},
    }
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{crop_id}"}],
            },
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "conditions": [mature],
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"{MODID}:{crop_id}",
                        "functions": [
                            {
                                "function": "minecraft:set_count",
                                "count": {"type": "minecraft:uniform", "min": 1, "max": 3},
                                "add": False,
                            }
                        ],
                    }
                ],
            },
        ],
        "functions": [{"function": "minecraft:explosion_decay"}],
    }


def wild_crop_loot(crop_id: str) -> dict:
    silk = silk_or_shears_condition()
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [
                    {
                        "type": "minecraft:alternatives",
                        "children": [
                            {"type": "minecraft:item", "name": f"{MODID}:wild_{crop_id}", "conditions": [silk]},
                            {
                                "type": "minecraft:item",
                                "name": f"{MODID}:{crop_id}",
                                "conditions": [{"condition": "minecraft:survives_explosion"}],
                                "functions": [
                                    {
                                        "function": "minecraft:set_count",
                                        "count": {"type": "minecraft:uniform", "min": 1, "max": 2},
                                        "add": False,
                                    }
                                ],
                            },
                        ],
                    }
                ],
            }
        ],
    }


def generate_datapack() -> None:
    # 战利品表
    for b in ("orange_log", "orange_wood", "stripped_orange_log", "stripped_orange_wood", "orange_sapling"):
        write_json(DATA / "loot_table/blocks" / f"{b}.json", self_drop_loot(b))
    write_json(DATA / "loot_table/blocks/orange_leaves.json", orange_leaves_loot())
    for crop in CROPS:
        write_json(DATA / "loot_table/blocks" / f"{crop}_crop.json", crop_loot(crop))
        write_json(DATA / "loot_table/blocks" / f"wild_{crop}.json", wild_crop_loot(crop))

    # 橙木相关配方
    for stripped in (False, True):
        log_id = "stripped_orange_log" if stripped else "orange_log"
        wood_id = "stripped_orange_wood" if stripped else "orange_wood"
        write_json(DATA / "recipe" / f"{wood_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "bark",
            "pattern": ["##", "##"],
            "key": {"#": {"item": f"{MODID}:{log_id}"}},
            "result": {"id": f"{MODID}:{wood_id}", "count": 3},
        })
    write_json(DATA / "recipe/oak_planks_from_orange_logs.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "building",
        "group": "planks",
        "ingredients": [{"tag": f"{MODID}:orange_logs"}],
        "result": {"id": "minecraft:oak_planks", "count": 4},
    })

    # 标签（仅新建与不冲突的标签；不动由其他脚本哈希锁定的 mineable/axe.json）
    orange_logs = [
        f"{MODID}:orange_log",
        f"{MODID}:orange_wood",
        f"{MODID}:stripped_orange_log",
        f"{MODID}:stripped_orange_wood",
    ]
    write_json(DATA / "tags/block/orange_logs.json", {"replace": False, "values": orange_logs})
    write_json(DATA / "tags/item/orange_logs.json", {"replace": False, "values": orange_logs})
    write_json(MC_DATA / "tags/block/logs_that_burn.json", {"replace": False, "values": [f"#{MODID}:orange_logs"]})
    write_json(MC_DATA / "tags/item/logs_that_burn.json", {"replace": False, "values": [f"#{MODID}:orange_logs"]})
    write_json(MC_DATA / "tags/block/leaves.json", {"replace": False, "values": [f"{MODID}:orange_leaves"]})
    write_json(MC_DATA / "tags/item/leaves.json", {"replace": False, "values": [f"{MODID}:orange_leaves"]})
    write_json(MC_DATA / "tags/block/saplings.json", {"replace": False, "values": [f"{MODID}:orange_sapling"]})
    write_json(MC_DATA / "tags/item/saplings.json", {"replace": False, "values": [f"{MODID}:orange_sapling"]})
    write_json(MC_DATA / "tags/block/mineable/hoe.json", {"replace": False, "values": [f"{MODID}:orange_leaves"]})
    crop_blocks = [f"{MODID}:{crop}_crop" for crop in CROPS]
    wild_blocks = [f"{MODID}:wild_{crop}" for crop in CROPS]
    write_json(MC_DATA / "tags/block/crops.json", {"replace": False, "values": crop_blocks})
    write_json(MC_DATA / "tags/block/bee_growables.json", {"replace": False, "values": crop_blocks})
    # mryt233 加：野生植株补充原版草花类标签，确保按标签识别的光影包与蜂类交互正常生效。
    write_json(MC_DATA / "tags/block/small_flowers.json", {"replace": False, "values": wild_blocks})

    # 世界生成：橙子树
    write_json(DATA / "worldgen/configured_feature/orange_tree.json", {
        "type": "minecraft:tree",
        "config": {
            "ignore_vines": True,
            "force_dirt": False,
            "dirt_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dirt"}},
            "trunk_provider": {
                "type": "minecraft:simple_state_provider",
                "state": {"Name": f"{MODID}:orange_log", "Properties": {"axis": "y"}},
            },
            "foliage_provider": {
                "type": "minecraft:weighted_state_provider",
                "entries": [
                    {"weight": 5, "data": {"Name": f"{MODID}:orange_leaves",
                                           "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false", "stage": "0"}}},
                    {"weight": 2, "data": {"Name": f"{MODID}:orange_leaves",
                                           "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false", "stage": "1"}}},
                    {"weight": 2, "data": {"Name": f"{MODID}:orange_leaves",
                                           "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false", "stage": "2"}}},
                    {"weight": 3, "data": {"Name": f"{MODID}:orange_leaves",
                                           "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false", "stage": "3"}}},
                ],
            },
            "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 2, "height_rand_a": 0, "height_rand_b": 0},
            "foliage_placer": {
                "type": f"{MODID}:orange_foliage_placer",
                "radius": 1,
                "offset": 0,
                "height": 2,
                "top_leaf_chance": 0.65,
                "upper_corner_skip_chance": 0.45,
            },
            "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
            "decorators": [],
        },
    })
    write_json(DATA / "worldgen/placed_feature/orange_tree_checked.json", {
        "feature": f"{MODID}:orange_tree",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 24},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
            {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": "minecraft:biome"},
            {
                "type": "minecraft:block_predicate_filter",
                "predicate": {
                    "type": "minecraft:would_survive",
                    "state": {"Name": f"{MODID}:orange_sapling", "Properties": {"stage": "0"}},
                },
            },
        ],
    })

    # 世界生成：9 种野生植株
    for crop in CROPS:
        rarity = 18 if crop in TEMPERATE_CROPS else (22 if crop in WARM_CROPS else 16)
        write_json(DATA / "worldgen/configured_feature" / f"wild_{crop}.json", {
            "type": "minecraft:random_patch",
            "config": {
                "tries": 36,
                "xz_spread": 5,
                "y_spread": 2,
                "feature": {
                    "feature": {
                        "type": "minecraft:simple_block",
                        "config": {
                            "to_place": {
                                "type": "minecraft:simple_state_provider",
                                "state": {"Name": f"{MODID}:wild_{crop}"},
                            }
                        },
                    },
                    "placement": [
                        {
                            "type": "minecraft:block_predicate_filter",
                            "predicate": {
                                "type": "minecraft:all_of",
                                "predicates": [
                                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                                    {
                                        "type": "minecraft:would_survive",
                                        "state": {"Name": f"{MODID}:wild_{crop}"},
                                    },
                                ],
                            },
                        }
                    ],
                },
            },
        })
        write_json(DATA / "worldgen/placed_feature" / f"wild_{crop}.json", {
            "feature": f"{MODID}:wild_{crop}",
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": rarity},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                {"type": "minecraft:biome"},
            ],
        })

    # 生物群系标签（Fabric BiomeSelectors.tag 与 NeoForge biome_modifier 共用）
    write_json(DATA / "tags/worldgen/biome/has_orange_tree.json", {
        "replace": False,
        "values": [
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:plains",
            "minecraft:meadow",
            "minecraft:sparse_jungle",
            "minecraft:jungle",
        ],
    })
    write_json(DATA / "tags/worldgen/biome/has_wild_temperate_crops.json", {
        "replace": False,
        "values": [
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow",
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
        ],
    })
    write_json(DATA / "tags/worldgen/biome/has_wild_warm_crops.json", {
        "replace": False,
        "values": [
            "minecraft:savanna",
            "minecraft:savanna_plateau",
            "minecraft:windswept_savanna",
            "minecraft:plains",
            "minecraft:sparse_jungle",
            "minecraft:jungle",
            "minecraft:river",
        ],
    })
    write_json(DATA / "tags/worldgen/biome/has_wild_humid_crops.json", {
        "replace": False,
        "values": [
            "minecraft:forest",
            "minecraft:dark_forest",
            "minecraft:birch_forest",
            "minecraft:river",
            "minecraft:swamp",
            "minecraft:bamboo_jungle",
        ],
    })

    # NeoForge biome modifiers
    write_json(NEO_DATA / "neoforge/biome_modifier/add_orange_tree.json", {
        "type": "neoforge:add_features",
        "biomes": f"#{MODID}:has_orange_tree",
        "features": f"{MODID}:orange_tree_checked",
        "step": "vegetal_decoration",
    })
    write_json(NEO_DATA / "neoforge/biome_modifier/add_wild_temperate_crops.json", {
        "type": "neoforge:add_features",
        "biomes": f"#{MODID}:has_wild_temperate_crops",
        "features": [f"{MODID}:wild_{c}" for c in TEMPERATE_CROPS],
        "step": "vegetal_decoration",
    })
    write_json(NEO_DATA / "neoforge/biome_modifier/add_wild_warm_crops.json", {
        "type": "neoforge:add_features",
        "biomes": f"#{MODID}:has_wild_warm_crops",
        "features": [f"{MODID}:wild_{c}" for c in WARM_CROPS],
        "step": "vegetal_decoration",
    })
    write_json(NEO_DATA / "neoforge/biome_modifier/add_wild_humid_crops.json", {
        "type": "neoforge:add_features",
        "biomes": f"#{MODID}:has_wild_humid_crops",
        "features": [f"{MODID}:wild_{c}" for c in HUMID_CROPS],
        "step": "vegetal_decoration",
    })


def sheet(style: str = ORANGE_LEAVES_STYLE) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    tiles: list[tuple[str, Image.Image]] = [
        ("orange_log", tex_orange_log()),
        ("orange_log_top", tex_orange_log_top()),
        ("stripped_orange_log", tex_stripped_orange_log()),
        ("stripped_orange_log_top", tex_stripped_orange_log_top()),
        ("orange_sapling", tex_orange_sapling()),
    ]
    tiles += [(f"orange_leaves_s{s}", tex_orange_leaves(s, style)) for s in range(4)]
    for crop in CROPS:
        for s in range(4):
            tiles.append((f"{crop}_s{s}", CROP_PAINTERS[crop](s)))
        tiles.append((f"wild_{crop}", tex_wild(crop)))

    cols = 10
    rows = (len(tiles) + cols - 1) // cols
    scale = 4
    cell = 16 * scale + 8
    out = Image.new("RGBA", (cols * cell + 8, rows * cell + 8), (112, 118, 126, 255))
    for i, (_, im) in enumerate(tiles):
        r, c = divmod(i, cols)
        out.alpha_composite(im.resize((16 * scale, 16 * scale), Image.NEAREST), (8 + c * cell, 8 + r * cell))
    out.save(OUT / "sheet.png")
    print(f"preview sheet: {OUT / 'sheet.png'} ({len(tiles)} tiles)")


def main() -> int:
    style = ORANGE_LEAVES_STYLE
    if "--style" in sys.argv:
        idx = sys.argv.index("--style")
        if idx + 1 < len(sys.argv):
            style = sys.argv[idx + 1]
    sheet(style)
    if "install" in sys.argv:
        summary = generate_assets(style)
        generate_datapack()
        print(
            f"installed botanicals (style={style}): textures={len(summary['textures'])}, "
            f"blockstates={len(summary['blockstates'])}, "
            f"block_models={len(summary['block_models'])}, "
            f"item_models={len(summary['item_models'])}"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
