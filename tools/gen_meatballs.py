"""丸子（2026-10-23 用户定，预览阶段）：生肉 / 生鱼放砧板上，用「木棒」捣三下出 3 个生丸子（方案 A）；
每捣一下砧板上的肉都要变样：原样 → 捶碎（第 1 下）→ 肉浆（第 2 下）→ 第 3 下掉出 3 个丸子。
熟丸子每个回 3；香菜丸子（2 生丸子 + 1 香菜 → 2 个）多回 1。鱼丸用生鳕鱼 / 生鲑鱼，也有香菜鱼丸。

  擀面杖 rolling_pin（原叫木棒 wooden_pounder，用户改）
  <beef|pork|mutton>_meatball、fish_ball，前面加 cilantro_ 是香菜版，再加 cooked_ 是熟的（共 16 个）
  砧板上的阶段贴图 block/pounding/<beef|pork|mutton|fish>_<minced|paste>.png（由砧板渲染器画在板面上）

    python tools/gen_meatballs.py
作者：
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, k):
    return clamp(tuple(v * k for v in c))


def outline(im, color):
    px = im.load()
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = color + (255,)


# ---------------------------------------------------------------- 丸子
# (生的颜色, 熟的颜色)
MEATS = {
    "beef": ((200, 98, 96), (132, 100, 84)),
    "pork": ((234, 172, 162), (214, 194, 166)),
    "mutton": ((214, 132, 128), (172, 138, 110)),
    "fish": ((238, 234, 226), (248, 246, 238)),
}
CILANTRO = [(78, 150, 50), (110, 178, 64), (58, 120, 40)]


def ball_texture(color, cilantro, seed):
    """一个圆丸子：直径 11，左上高光、右下暗，表面一点点颗粒；香菜版撒几点绿。"""
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    cx, cy, r = 8.0, 8.5, 5.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r:
                light = 1.12 - 0.32 * ((dx + dy) / (2 * r) + 0.5)
                c = shade(color, light * rnd.choice([0.95, 1.0, 1.0, 1.04]))
                px[x, y] = c + (255,)
    for x, y in [(5, 5), (6, 5), (5, 6)]:
        px[x, y] = shade(color, 1.3) + (255,)                 # 高光
    if cilantro:
        for x, y in [(9, 6), (6, 9), (10, 10), (8, 12), (11, 8), (7, 7)]:
            px[x, y] = rnd.choice(CILANTRO) + (255,)
    outline(im, shade(color, 0.55))
    return im


# ---------------------------------------------------------------- 擀面杖
# 用户 2026-10-23：木棒做成擀面杖的样子，名字和 ID 一起改（rolling_pin）。斜 45° 摆，中间略粗、两头细（0.1.2-alpha 后改），
# 上沿一道亮的反光、下沿暗，两头圆一点、颜色深一点；摆在格子正中。
def rolling_pin_texture():
    """照用户发的两张参考图（带把手的擀面杖）：中间浅色圆筒约占全长一半多（两端平、一圈深色接缝），
    两头深一点的木把手各约占四分之一——靠圆筒是一段细脖子，后面是粗一点的握把、尾端圆头。
    上沿亮、下沿暗；斜 45° 从左下角到右上角（用户：中间太短加长；又说把手太短，按第二张图的比例）。"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    s2 = math.sqrt(0.5)
    ox, oy = 0.3, 15.7
    barrel = ((236, 210, 168), (222, 192, 146), (188, 156, 110))     # 圆筒：亮 / 中 / 暗
    handle = ((196, 138, 80), (172, 116, 62), (138, 90, 46))         # 把手：深一点的木色
    seam = (150, 112, 70)
    B0, B1, BR = 4.7, 16.3, 2.2          # 圆筒沿杆方向的起止、半径（用户：中间再加长一点点）
    NECK = 1.1                           # 细脖子长度
    END = 21.0                           # 全长
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5 - ox, y + 0.5 - oy
            u = (cx - cy) * s2
            v = (cx + cy) * s2
            c = None
            if B0 <= u <= B1 and abs(v) <= BR:
                hi, mid, lo = barrel
                c = hi if v < -BR + 0.9 else lo if v > BR - 0.9 else mid
                if u < B0 + 0.6 or u > B1 - 0.6:
                    c = seam
            elif 0.0 <= u < B0 or B1 < u <= END:
                d = B0 - u if u < B0 else u - B1          # 离圆筒多远
                r = 1.2 if d < NECK else 1.85             # 细脖子 → 握把（斜着画，半径要过 1.5 才有两格宽）
                if d > (B0 - 0.0) - 0.8:                  # 尾端收圆
                    r = 1.4
                if abs(v) <= r:
                    hi, mid, lo = handle
                    c = hi if v < -0.5 else lo if v > 0.5 else mid
            if c:
                px[x, y] = c + (255,)
    outline(im, (92, 60, 32))
    box = im.getbbox()
    w, h = box[2] - box[0], box[3] - box[1]
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out.paste(im.crop(box), ((16 - w) // 2, (16 - h) // 2))
    # 上下两端要一模一样（用户：两端样式不统一）：斜着栅格化两头会差半格，这里把左下半截沿 x=y 对称抄到右上半截
    op = out.load()
    for y in range(16):
        for x in range(16):
            if x > y:
                op[x, y] = op[y, x]   # mirror
    return out


# ---------------------------------------------------------------- 砧板上的两个阶段（俯视，画在板面上 8×8 像素大）
# 生肉原色（捶碎时露出的肉块颜色）
RAW = {"beef": (196, 52, 52), "pork": (230, 140, 130), "mutton": (206, 96, 92), "fish": (232, 206, 186)}


def minced_texture(meat, seed):
    """捶碎：摊开的一片不规则碎肉，一块块大小不一、颜色深浅不一。"""
    rnd = random.Random(seed)
    base = RAW[meat]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, (y + 0.5 - 8) * 1.1) + rnd.uniform(-1.2, 1.2)
            if d < 7.0:
                px[x, y] = shade(base, rnd.choice([0.8, 0.9, 1.0, 1.0, 1.1, 1.2])) + (255,)
    for _ in range(14):                                     # 一块块碎肉之间的缝
        x, y = rnd.randrange(2, 14), rnd.randrange(2, 14)
        if px[x, y][3]:
            px[x, y] = shade(base, 0.62) + (255,)
    outline(im, shade(base, 0.5))
    return im


def paste_texture(meat):
    """肉浆：一团圆圆的、光滑的浆（颜色 = 生丸子色），中间亮一点带一道反光。"""
    color = MEATS[meat][0]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 6.6:
                px[x, y] = shade(color, 1.08 - 0.15 * d / 6.6) + (255,)
    for x, y in [(5, 5), (6, 5), (7, 4), (5, 6)]:
        px[x, y] = shade(color, 1.28) + (255,)
    outline(im, shade(color, 0.6))
    return im


# 3D 丸子（2026-10-23 用户：丸子在锅里浮起来的样子做成 3D）：物品模型用几块长方体拼成一个 8 像素的圆球，
# 贴图就用这颗丸子的物品贴图中间那块（香菜点也带上）。锅里、手里、盘子里、物品栏都是立体的小球。
# r106 用户：太大、不圆 → 直径 6 像素，三根十字 + 一个削角的芯，棱角少一点
BALL_BOXES = [([6, 5, 6], [10, 11, 10]), ([5, 6, 6], [11, 10, 10]), ([6, 6, 5], [10, 10, 11]),
              ([5.5, 5.5, 5.5], [10.5, 10.5, 10.5])]
BALL_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [1.6, 1.6, 1.6]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.7, 0.7, 0.7]},
}


def ball_model(name):
    els = []
    for f, t in BALL_BOXES:
        faces = {}
        for d in ("north", "south", "east", "west", "up", "down"):
            if d in ("north", "south"):
                uv = [f[0], 16 - t[1], t[0], 16 - f[1]]
            elif d in ("east", "west"):
                uv = [f[2], 16 - t[1], t[2], 16 - f[1]]
            else:
                uv = [f[0], f[2], t[0], t[2]]
            faces[d] = {"texture": "#ball", "uv": uv}
        els.append({"from": f, "to": t, "faces": faces})
    return {"parent": "minecraft:block/block", "textures": {"ball": f"{MODID}:item/{name}", "particle": f"{MODID}:item/{name}"},
            "elements": els, "display": BALL_DISPLAY}


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def ball_id(meat, cilantro, cooked):
    name = "fish_ball" if meat == "fish" else f"{meat}_meatball"
    return ("cooked_" if cooked else "") + ("cilantro_" if cilantro else "") + name


def main():
    for root in ROOTS:
        tex = root / "textures"
        (tex / "item").mkdir(parents=True, exist_ok=True)
        (tex / "block/pounding").mkdir(parents=True, exist_ok=True)
        for i, (meat, (raw, cooked)) in enumerate(MEATS.items()):
            for cil in (False, True):
                for ck in (False, True):
                    name = ball_id(meat, cil, ck)
                    ball_texture(cooked if ck else raw, cil, i).save(tex / f"item/{name}.png")
                    write(root / f"models/item/{name}.json", ball_model(name))
            minced_texture(meat, i).save(tex / f"block/pounding/{meat}_minced.png")
            paste_texture(meat).save(tex / f"block/pounding/{meat}_paste.png")
            for k, stage in enumerate(("minced", "paste")):
                write(root / f"models/item/pounded_meat_{meat}_{stage}.json",
                      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:block/pounding/{meat}_{stage}"}})
        # 砧板上捣到一半的肉：一个不进创造栏的物品，按自定义模型数据换成 8 个样子之一（顺序同 Pounding.stageModel）
        overrides = []
        for i, meat in enumerate(MEATS):
            for k, stage in enumerate(("minced", "paste")):
                overrides.append({"predicate": {"custom_model_data": i * 2 + k + 1}, "model": f"{MODID}:item/pounded_meat_{meat}_{stage}"})
        write(root / "models/item/pounded_meat.json", {"parent": "minecraft:item/generated",
              "textures": {"layer0": f"{MODID}:block/pounding/beef_paste"}, "overrides": overrides})
        rolling_pin_texture().save(tex / "item/rolling_pin.png")
        write(root / "models/item/rolling_pin.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/rolling_pin"}})
        for old in (tex / "item/wooden_pounder.png", root / "models/item/wooden_pounder.json"):
            if old.exists():
                old.unlink()
    print("ok")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- 进游戏的数据（用户 2026-10-23「ok」）
DATA = RES / "data"


def data_files():
    rec = DATA / MODID / "recipe"
    for meat in MEATS:
        for cil in (False, True):
            raw, cooked = ball_id(meat, cil, False), ball_id(meat, cil, True)
            for kind, time, suffix in [("smelting", 100, ""), ("smoking", 50, "_from_smoking"), ("campfire_cooking", 300, "_from_campfire_cooking")]:
                write(rec / f"{cooked}{suffix}.json", {"type": f"minecraft:{kind}", "category": "food", "ingredient": {"item": f"{MODID}:{raw}"},
                                                      "result": {"id": f"{MODID}:{cooked}"}, "experience": 0.35, "cookingtime": time})
        # 香菜丸子：2 个生丸子 + 1 颗香菜 → 2 个
        write(rec / f"{ball_id(meat, True, False)}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
              "ingredients": [{"item": f"{MODID}:{ball_id(meat, False, False)}"}] * 2 + [{"item": f"{MODID}:cilantro"}],
              "result": {"id": f"{MODID}:{ball_id(meat, True, False)}", "count": 2}})
    # 擀面杖：原木在上、木棍在下（原叫木棒，2026-10-23 改名）
    (rec / "wooden_pounder.json").unlink(missing_ok=True)
    write(rec / "rolling_pin.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["L", "S"],
                                     "key": {"L": {"tag": "minecraft:logs"}, "S": {"item": "minecraft:stick"}},
                                     "result": {"id": f"{MODID}:rolling_pin", "count": 1}})
    f = DATA / "minecraft/tags/item/meat.json"
    tag = json.loads(f.read_text(encoding="utf-8"))
    for meat in ("beef", "pork", "mutton"):
        for cil in (False, True):
            for ck in (False, True):
                v = f"{MODID}:{ball_id(meat, cil, ck)}"
                if v not in tag["values"]:
                    tag["values"].append(v)
    write(f, tag)


if __name__ == "__main__":
    data_files()
