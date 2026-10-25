"""蘸料台（r103 预览；2026-10-23 改阶梯式 1.5 格高，三层每层 3 个碗）：原先是一格高的台子，台面上 3×3 九个小白瓷碗，对应 9 种调料（用户定：自己调，最多 3 种叠加）。

    python tools/gen_sauce_station.py
用户选了「每种木头都做一个 + 铁台」：11 种木头（台身木板 + 白色石英台面）+ 铁（原版铁块）；每种有「九碗都装满」和「空碗」两个模型。
调料面贴图 sauce_<调料>.png 是纯色加一点深浅；葱花、香菜碎末用 tools/gen_garnish.py 画的方块贴图，直接放在白瓷碗里。
九碗顺序（从北往南、从西往东）：麻酱 辣油 酸甜酱 / 蜂蜜 香油 醋 / 白糖 葱花 香菜碎末。
作者：
"""
import json
import random
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]

# 调料 ID（英文名）→ 颜色（基色）；葱花 / 香菜碎末用画好的贴图
SAUCES = [
    ("msg", (236, 236, 240)),             # 味精（小麦；2026-10-23 替换麻酱）
    ("chili_oil", (196, 40, 22)),         # 辣油（烈焰粉）
    ("sweet_sour_sauce", (200, 90, 60)),  # 酸甜酱（甜浆果）
    ("honey", (236, 168, 38)),            # 蜂蜜（蜂蜜瓶）
    ("sesame_oil", (204, 138, 40)),       # 香油（向日葵 / 南瓜种子）
    ("vinegar", (70, 36, 24)),            # 醋（苹果 / 甜菜根；原发酵蛛眼）
    ("sugar", (240, 238, 232)),           # 白糖（糖）
    ("chopped_scallion", None),           # 葱花（小葱）
    ("minced_cilantro", None),            # 香菜碎末（香菜）
]
# 用户 2026-10-23 定：每种原版木头一个（台身该木头的木板、台面白色石英）+ 一个铁的（原版铁块）。ID = <木头>_sauce_station / iron_sauce_station
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]
BODIES = {w: {"side": f"minecraft:block/{w}_planks", "top": "minecraft:block/quartz_block_top"} for w in WOODS}
BODIES["iron"] = {"side": "minecraft:block/iron_block", "top": "minecraft:block/iron_block"}
# 2026-10-23 用户改成阶梯式（照自助调料台照片）：1.5 格高，三层台阶从前往后一层比一层高，每层一横排 3 个小碗。
# 模型正面朝北（z=0 是最矮的前层），方块状态按 facing 转。层顶高度（像素，前 → 后）：
TIER_TOPS = [11.0, 15.0, 19.0]   # 用户 2026-10-23：前两层再高一点（原 10 / 16 → 14 / 18 / 22）；0.1.2 后用户：整体降一个活板门高（3 像素）
TIER_D = 16 / 3    # 每层进深
CAP = 1.0          # 每层顶上 1 像素白色台面
# 小碗：用户 2026-10-23 要换成碗的 a 款（直壁碗，gen_bowls.py 的弃案），按每格 5.33 像素缩到外边长 5：
# 碗脚 3×3 高 0.5、碗底板 5×5 高 0.5、直壁厚 0.75 高 1.5；各块不重叠（旧版碗壁和碗底叠着，转动时贴图打架闪烁）
BOWL = 5.0         # 小碗外边长
FOOT = 3.0
WALL = 0.75
BOWL_H = 2.5
FACES = ["north", "east", "south", "west", "up", "down"]


def oil_texture():
    """香油（用户 2026-10-23：看着不像油）：清亮的金褐色，中间亮、边上深，一道斜的反光高光，几乎没有颗粒。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 / 10.6       # 0 中心 → 1 角
            base = (212, 150, 44)
            edge = (150, 88, 22)
            c = tuple(int(base[i] + (edge[i] - base[i]) * d) for i in range(3))
            px[x, y] = c + (255,)
    for x, y in [(3, 4), (4, 4), (4, 3), (5, 3), (6, 3), (7, 2), (8, 2)]:   # 反光：一道亮的斜线
        px[x, y] = (250, 222, 140, 255)
    for x, y in [(5, 4), (3, 5), (9, 2)]:
        px[x, y] = (236, 192, 96, 255)
    for x, y in [(11, 11), (12, 11)]:                                          # 小油花
        px[x, y] = (232, 184, 84, 255)
    return im


def honey_texture():
    """蜂蜜（用户：看着不像蜂蜜）：照原版蜂蜜块的橙黄，稠的一圈圈纹路 + 白亮高光点。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            r = ((x - 8) ** 2 + (y - 8) ** 2) ** 0.5
            ring = int(r) % 4
            c = [(251, 185, 52), (246, 170, 40), (251, 185, 52), (255, 204, 84)][ring]
            px[x, y] = c + (255,)
    for x, y in [(4, 4), (5, 4), (4, 5)]:
        px[x, y] = (255, 246, 196, 255)
    for x, y in [(6, 5), (5, 6), (11, 10)]:
        px[x, y] = (255, 228, 140, 255)
    return im


def berry_texture():
    """酸甜酱（用户：加些碎甜浆果，能看出是甜浆果）：红色酱底 + 一颗颗原版甜浆果那种红果粒（深红边、亮红肉、一点高光）。"""
    rnd = random.Random(31)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            k = rnd.choice([0.92, 1.0, 1.0, 1.06])
            px[x, y] = tuple(int(c * k) for c in (214, 96, 62)) + (255,)   # 酱底偏橙，衬出红果粒
    for bx, by in [(3, 3), (9, 2), (12, 7), (5, 9), (10, 12), (2, 13), (7, 6)]:
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1)]:
            px[bx + dx, by + dy] = (210, 28, 46, 255)
        px[bx + 1, by + 1] = (118, 10, 26, 255)      # 果粒暗面
        px[bx, by] = (255, 120, 120, 255)            # 高光
    for x, y in [(6, 3), (13, 3), (8, 10), (3, 7), (13, 13)]:   # 碎果皮
        px[x, y] = (134, 16, 32, 255)
    return im


def msg_texture():
    """味精（用户 2026-10-23 加，替换麻酱）：一根根亮白的小晶体，和细粉一样的白糖区分开——白底偏冷、晶体带浅灰描边和亮点。"""
    rnd = random.Random(77)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rnd.choice([(214, 216, 224), (206, 208, 218), (220, 222, 230)]) + (255,)
    for cx, cy, horiz in [(2, 2, True), (8, 1, False), (12, 4, True), (4, 6, False), (9, 7, True), (1, 11, True),
                          (6, 11, False), (11, 11, True), (13, 9, False), (3, 14, True), (9, 14, True)]:
        cells = [(cx + k, cy) if horiz else (cx, cy + k) for k in range(3)]
        for i, (x, y) in enumerate(cells):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (255, 255, 255, 255) if i == 0 else (244, 246, 252, 255)
        sx, sy = (cx, cy + 1) if horiz else (cx + 1, cy)
        for k in range(3):
            x, y = (sx + k, sy) if horiz else (sx, sy + k)
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (170, 172, 186, 255)
    return im


def vinegar_texture():
    """醋（用户 2026-10-23：不像液体也不像醋）：陈醋那种深红褐、透亮的液面——中间稍亮带红、边上深，
    一道斜反光 + 一圈淡淡的波纹，不加颗粒噪点（颗粒看着像粉）。"""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 8.5) ** 2 + (y - 9) ** 2) ** 0.5 / 10.6
            mid, edge = (122, 52, 30), (58, 24, 14)
            px[x, y] = tuple(int(mid[i] + (edge[i] - mid[i]) * min(1, d)) for i in range(3)) + (255,)
    for x in range(16):                                   # 波纹：一圈稍亮的环
        for y in range(16):
            r = ((x - 9) ** 2 + (y - 10) ** 2) ** 0.5
            if 4.6 < r < 5.4:
                c = px[x, y]
                px[x, y] = (min(255, c[0] + 22), min(255, c[1] + 10), min(255, c[2] + 6), 255)
    for x, y in [(3, 4), (4, 4), (4, 3), (5, 3), (6, 3), (7, 2)]:   # 斜反光
        px[x, y] = (214, 150, 120, 255)
    for x, y in [(5, 4), (3, 5), (8, 2)]:
        px[x, y] = (168, 92, 64, 255)
    return im


CUSTOM = {"msg": msg_texture, "vinegar": vinegar_texture, "sesame_oil": oil_texture, "honey": honey_texture, "sweet_sour_sauce": berry_texture}


def sauce_texture(rgb, seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            k = rnd.choice([0.9, 1.0, 1.0, 1.0, 1.08])
            px[x, y] = tuple(max(0, min(255, int(c * k))) for c in rgb) + (255,)
    return im


def box(f, t, tex):
    """一个长方块，六个面都写明 UV：高过 16 像素的部分按 y-16 取贴图（默认算法会跑出贴图）。不能跨过 y=16，跨的用 boxes()。"""
    x0, y0, z0 = f
    x1, y1, z1 = t
    off = 16 if y0 >= 16 else 0
    v0, v1 = 16 - (y1 - off), 16 - (y0 - off)
    uv = {"north": [16 - x1, v0, 16 - x0, v1], "south": [x0, v0, x1, v1],
          "east": [16 - z1, v0, 16 - z0, v1], "west": [z0, v0, z1, v1],
          "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    return {"from": f, "to": t, "faces": {d: {"texture": tex, "uv": uv[d]} for d in FACES}}


def boxes(f, t, tex):
    """跨过 y=16 的长方块切成上下两段。"""
    if f[1] < 16 < t[1]:
        return [box(f, [t[0], 16, t[2]], tex), box([f[0], 16, f[2]], t, tex)]
    return [box(f, t, tex)]


def bowl_center(i):
    """第 i 种调料的小碗中心（模型坐标，正面朝北）。row 0 = 最后最高那层；col 0 = 站在正面看的最左边（东边）。"""
    row, col = divmod(i, 3)
    tier = 2 - row
    cell = 16 / 3
    return 16 - cell * (col + 0.5), TIER_D * (tier + 0.5), TIER_TOPS[tier]


def model(body, filled):
    tex = {"side": BODIES[body]["side"], "top": BODIES[body]["top"], "bowl": f"{MODID}:block/bowl_wood", "inner": f"{MODID}:block/bowl_wood_inner",   # 用户 2026-10-23：碗换原版碗的木头材质（同摆桌上的碗）
           "particle": BODIES[body]["side"]}
    els = []
    for tier, top in enumerate(TIER_TOPS):
        z0, z1 = TIER_D * tier, TIER_D * (tier + 1)
        els += boxes([0, 0, z0], [16, top - CAP, z1], "#side")          # 台身
        els += boxes([0, top - CAP, z0], [16, top, z1], "#top")         # 白色台面
    for i, (name, rgb) in enumerate(SAUCES):
        cx, cz, y0 = bowl_center(i)
        a0, a1 = cx - BOWL / 2, cx + BOWL / 2
        b0, b1 = cz - BOWL / 2, cz + BOWL / 2
        y1 = y0 + BOWL_H
        t = WALL
        f = FOOT / 2
        els += [box([cx - f, y0, cz - f], [cx + f, y0 + 0.5, cz + f], "#bowl"),           # 碗脚
                box([a0, y0 + 0.5, b0], [a1, y0 + 1, b1], "#inner"),                   # 碗底板
                box([a0, y0 + 1, b0], [a1, y1, b0 + t], "#bowl"), box([a0, y0 + 1, b1 - t], [a1, y1, b1], "#bowl"),
                box([a0, y0 + 1, b0 + t], [a0 + t, y1, b1 - t], "#bowl"), box([a1 - t, y0 + 1, b0 + t], [a1, y1, b1 - t], "#bowl")]
        if filled:
            key = f"s{i}"
            surface_y = y1 - 0.5
            if rgb is None:
                # 葱花 / 香菜碎末：切好的贴图（带透明）直接放在白瓷碗里，空隙露出碗底（用户：底下不要垫油）
                tex[key] = f"{MODID}:block/{name}"
            else:
                tex[key] = f"{MODID}:block/sauce_{name}"
            if rgb is None:
                # 葱花 / 香菜碎末堆得满一点（用户 2026-10-23：看着只有薄薄一层）：三层错开的碎末叠起来，
                # 层层错开显得厚（）
                # 用户：侧面看最上一层冒出碗口悬空了 → 四层都收在碗口以内
                layers = [(y1 - 1.3, 0, [2, 2, 14, 14], 0), (y1 - 0.95, 0, [0, 4, 12, 16], 90),
                          (y1 - 0.6, 0, [4, 0, 16, 12], 180), (y1 - 0.25, 0, [3, 3, 13, 13], 270)]
                for ly, inset, uv, rot in layers:
                    face = {"texture": f"#{key}", "uv": uv}
                    if rot:
                        face["rotation"] = rot
                    els.append({"from": [a0 + t + inset, ly, b0 + t + inset], "to": [a1 - t - inset, ly + 0.01, b1 - t - inset],
                                "faces": {"up": face}})
            else:
                els.append({"from": [a0 + t, surface_y, b0 + t], "to": [a1 - t, surface_y + 0.01, b1 - t],
                            "faces": {"up": {"texture": f"#{key}", "uv": [2, 2, 14, 14]}}})
    return {"parent": "minecraft:block/block", "textures": tex, "elements": els}


def main():
    for root in ROOTS:
        (root / "textures/block").mkdir(parents=True, exist_ok=True)
        for i, (name, rgb) in enumerate(SAUCES):
            if rgb is not None:
                (CUSTOM[name]() if name in CUSTOM else sauce_texture(rgb, i)).save(root / f"textures/block/sauce_{name}.png")
        for old in ("sauce_station_wood", "sauce_station_steel", "sauce_station_wood_empty", "sauce_station_steel_empty"):
            (root / f"models/block/{old}.json").unlink(missing_ok=True)   # 第一版预览（wood / steel）；只删这几个，别误删 sauce_station_top
        # 看不见的上半截 sauce_station_top：方块状态 + 只有粒子贴图的空模型（以前手写，被上面的清理误删过，改由这里生成）
        (root / "models/block/sauce_station_top.json").write_text(json.dumps(
            {"textures": {"particle": "minecraft:block/oak_planks"}}, indent=2) + "\n", encoding="utf-8")
        (root / "blockstates/sauce_station_top.json").write_text(json.dumps(
            {"variants": {"": {"model": f"{MODID}:block/sauce_station_top"}}}, indent=2) + "\n", encoding="utf-8")
        for body in BODIES:
            for filled in (True, False):
                name = f"{body}_sauce_station" + ("" if filled else "_empty")
                (root / f"models/block/{name}.json").write_text(json.dumps(model(body, filled), indent=2) + "\n",
                                                                encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- r103 进游戏：方块状态 / 物品 / 掉落 / 挖掘工具
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}   # 正面朝向 → 模型绕 y 转（顺时针）
BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    # 拿在手里：正面（最矮那层）朝着自己（2026-10-23 用户要求；原版方块是 45 / 225，这里各转半圈）
    # r106 进游戏看还是偏了 90°，用户：再顺时针转 90°
    "thirdperson_righthand": {"rotation": [75, 135, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 315, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def game_files():
    data = RES / "data"
    for root in ROOTS:
        # 2026-10-23 碗不固定后，调料面改由 client/SauceStationRenderer 画，删掉按位置叠加的 sauce_fill_* 模型（）
        for old in (root / "models/block").glob("sauce_fill_*.json"):
            old.unlink()
        for body in BODIES:
            ident = f"{body}_sauce_station"
            variants = {f"facing={facing}": {"model": f"{MODID}:block/{ident}_empty", **({"y": y} if y else {})}
                        for facing, y in ROT.items()}
            (root / f"blockstates/{ident}.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8")
            (root / f"models/item/{ident}.json").write_text(json.dumps(
                {"parent": f"{MODID}:block/{ident}", "display": BLOCK_DISPLAY}, indent=2) + "\n", encoding="utf-8")
    for body in BODIES:
        ident = f"{body}_sauce_station"
        loot = {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{ident}"}]}]}
        f = data / MODID / f"loot_table/blocks/{ident}.json"
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(json.dumps(loot, indent=2) + "\n", encoding="utf-8")
    for tag_file, ids in [("mineable/axe.json", [f"{w}_sauce_station" for w in WOODS]),
                          ("mineable/pickaxe.json", ["iron_sauce_station"])]:
        f = data / "minecraft/tags/block" / tag_file
        tag = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"replace": False, "values": []}
        for i in ids:
            if f"{MODID}:{i}" not in tag["values"]:
                tag["values"].append(f"{MODID}:{i}")
        f.write_text(json.dumps(tag, indent=2) + "\n", encoding="utf-8")


game_files()


# ---------------------------------------------------------------- 配方（2026-10-23 用户定：台阶样式，3 碗斜线 + 3 木板；铁的用铁锭）（）


def recipes():
    for body in BODIES:
        ident = f"{body}_sauce_station"
        leg = "minecraft:iron_ingot" if body == "iron" else f"minecraft:{body}_planks"
        recipe = {"type": "minecraft:crafting_shaped", "group": f"{MODID}:sauce_station", "category": "misc",
                  "pattern": ["  B", " BP", "BPP"],   # 用户 2026-10-23 改成台阶样式（原：碗 / 台阶 / 木板三排）
                  "key": {"B": {"item": "minecraft:bowl"}, "P": {"item": leg}},
                  "result": {"id": f"{MODID}:{ident}", "count": 1}}
        (RES / "data" / MODID / f"recipe/{ident}.json").write_text(json.dumps(recipe, indent=2) + "\n", encoding="utf-8")


recipes()


# ---------------------------------------------------------------- 一碗蘸料的表面（2026-10-23 用户：碗里的东西不像蘸料）（）
# 灰度的「酱面」：中间亮四周暗、一道斜反光、一圈淡波纹；游戏里按碗里几种调料的平均色染色（client/SauceRender），
# 再按放了哪几种撒对应颜色的小点（辣油红点、葱花绿点……），一眼看得出放了什么。
def bowl_surface_texture():
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            v = 235 - d * 7
            if 5.2 < d < 6.2:
                v += 12                                   # 波纹
            px[x, y] = (int(v), int(v), int(v), 255)
    for x, y in [(4, 5), (5, 4), (6, 4), (7, 3), (5, 5)]:
        px[x, y] = (255, 255, 255, 255)                   # 反光
    return im


def bowl_surface():
    for root in ROOTS:
        (root / "textures/misc").mkdir(parents=True, exist_ok=True)
        bowl_surface_texture().save(root / "textures/misc/sauce_surface.png")


bowl_surface()
