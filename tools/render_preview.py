"""把方块 / 物品模型（JSON，长方块拼的）画成一张斜视预览图，不用开游戏、不用 WebGL（2026-10-25）。

    python tools/render_preview.py 输出.png 模型1 模型2 ...      模型写 orients_hotpot_chronicles:block/orient_cola 或 minecraft:block/xxx
只画 elements 里的长方块（不支持 rotation 字段的斜面）；从右前上方看（看得见 上 / 南 / 东 三面），三面各一个亮度。
贴图按像素画，放大 SCALE 倍。原版模型 / 贴图从查看器缓存 viewer/cache/1.21.1 读。
作者：mryt233
"""
import json
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
ROOTS = {MODID: MOD / "common/src/main/resources/assets" / MODID,
         "minecraft": MOD.parent / "viewer/cache/1.21.1/assets/minecraft"}
SCALE = 14
# 预览还没装进模组的新模型：设环境变量 PREVIEW_ROOT=某个 assets/<模组> 目录，先从那里找模型 / 贴图
import os
PREVIEW = Path(os.environ["PREVIEW_ROOT"]) if os.environ.get("PREVIEW_ROOT") else None


def asset(ns, sub):
    if PREVIEW is not None and ns == MODID and (PREVIEW / sub).exists():
        return PREVIEW / sub
    return ROOTS[ns] / sub
SHADE = {"up": 1.0, "south": 0.82, "east": 0.64}


def split(ref, default_ns="minecraft"):
    ns, _, path = ref.partition(":") if ":" in ref else (default_ns, "", ref)
    return ns, path


def load_model(ref):
    ns, path = split(ref)
    m = json.loads(asset(ns, "models/" + path + ".json").read_text(encoding="utf-8"))
    textures, elements = {}, None
    if "parent" in m:
        p = load_model(m["parent"])
        textures.update(p["textures"])
        elements = p["elements"]
    textures.update(m.get("textures", {}))
    if "elements" in m:
        elements = m["elements"]
    return {"textures": textures, "elements": elements or []}


def resolve(textures, key):
    seen = 0
    while key.startswith("#") and seen < 10:
        key = textures.get(key[1:], "")
        seen += 1
    return key


_tex_cache = {}


def texture(ref):
    if ref not in _tex_cache:
        ns, path = split(ref)
        f = asset(ns, "textures/" + path + ".png")
        im = Image.open(f).convert("RGBA")
        if im.height > im.width:          # 动画贴图取第一帧
            im = im.crop((0, 0, im.width, im.width))
        _tex_cache[ref] = im
    return _tex_cache[ref]


COS, SIN = math.cos(math.radians(30)), math.sin(math.radians(30))


def project(p, ox, oy):
    x, y, z = p
    return (ox + (x - z) * COS * SCALE, oy + ((x + z) * SIN - y) * SCALE)


def face_points(el, face):
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    # 每个面四个角：左上、右上、右下、左下（和贴图 uv 的方向对应）
    if face == "up":
        return [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    if face == "south":
        return [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]
    if face == "east":
        return [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]
    raise ValueError(face)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def draw_model(img, model, ox, oy):
    draw = ImageDraw.Draw(img)
    els = sorted(model["elements"], key=lambda e: (e["from"][0] + e["to"][0] + e["from"][2] + e["to"][2]
                 + (e["from"][1] + e["to"][1]) * 1.5))
    for el in els:
        for face in ("up", "south", "east"):
            f = el.get("faces", {}).get(face)
            if not f:
                continue
            tex = texture(resolve(model["textures"], f["texture"]))
            tw = tex.width / 16
            u0, v0, u1, v1 = f.get("uv", [0, 0, 16, 16])
            tl, tr, br, bl = face_points(el, face)
            nu = max(1, round(abs(u1 - u0) * tw))     # 按贴图像素采样（32 像素贴图一格 2 个像素）
            nv = max(1, round(abs(v1 - v0) * tw))
            px = tex.load()
            for j in range(nv):
                for i in range(nu):
                    tu = u0 + (u1 - u0) * (i + 0.5) / nu
                    tv = v0 + (v1 - v0) * (j + 0.5) / nv
                    c = px[min(tex.width - 1, int(tu * tw)), min(tex.height - 1, int(tv * tw))]
                    if c[3] < 10:
                        continue
                    a, b = i / nu, (i + 1) / nu
                    cc, d = j / nv, (j + 1) / nv
                    top_a, top_b = lerp(tl, tr, a), lerp(tl, tr, b)
                    bot_a, bot_b = lerp(bl, br, a), lerp(bl, br, b)
                    quad = [lerp(top_a, bot_a, cc), lerp(top_b, bot_b, cc), lerp(top_b, bot_b, d), lerp(top_a, bot_a, d)]
                    s = SHADE[face]
                    draw.polygon([project(q, ox, oy) for q in quad],
                                 fill=(round(c[0] * s), round(c[1] * s), round(c[2] * s), 255))


def main():
    out, refs = sys.argv[1], sys.argv[2:]
    w = 30 * SCALE
    img = Image.new("RGBA", (w * len(refs), 30 * SCALE), (139, 139, 139, 255))
    for k, ref in enumerate(refs):
        draw_model(img, load_model(ref), k * w + w / 2, 20 * SCALE)
    img.save(out)
    print(out)


if __name__ == "__main__":
    main()
