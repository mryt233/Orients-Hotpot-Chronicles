"""新汤底的资源（2026-10-25：番茄锅底 tomato、三鲜锅底 three_fresh）。以后再加汤底，往 NEW 里加一行再跑一遍。

    python tools/gen_new_soups.py            # 汤面贴图 + 模型 + 方块状态 + 锅底料 / 番茄物品贴图，写两份资源
做法（照红汤 spicy 那一套抄）：
  1. 汤面贴图 hot_pot_top_<汤>.png：拿已有汤面贴图（番茄照红汤、三鲜照高汤）取明暗，换成这锅汤的颜色，再撒几样料的小点；
     沸腾动画 hot_pot_top_<汤>_boiling.png 由 tools/gen_boiling.py 生成（那边的 SOUPS 里也要加这锅汤的气泡色）。
  2. 方块模型：所有名字里带 spicy 的汤面模型（单独的锅、灶台、高灶台、碗）复制一份，spicy 换成新汤名。
  3. 方块状态：碗、两种灶台、两种九宫格锅里 soup=spicy 的条目各复制一条给新汤。
  4. 锅的物品模型 / 汤碗物品模型由 tools/gen_pot_items.py、tools/gen_bowls.py 生成（那边的 SOUPS 里也要加）。
重跑结果不变。作者：mryt233
"""
import json
import random
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "common/src/main/resources"
ROOTS = [RES / "assets" / MODID]

# 新汤：(照哪锅汤的明暗, 汤色 暗 → 亮, 撒的料 [(颜色, 个数)])
NEW = {
    "tomato": ("spicy", ((150, 34, 18), (226, 84, 46), (255, 150, 96)),
               [((242, 196, 74), 7), ((190, 40, 24), 6), ((104, 170, 58), 3)]),          # 蛋花、番茄块、葱
    "three_fresh": ("rich_stock", ((178, 150, 96), (222, 204, 156), (248, 238, 206)),
                    [((138, 90, 52), 5), ((236, 136, 48), 5), ((62, 122, 58), 5)]),       # 蘑菇、胡萝卜、海带
}
BLOCKSTATES = ["bowl", "brick_stove", "tall_brick_stove", "iron_nine_grid_hotpot", "copper_nine_grid_hotpot"]


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def soup_texture(name, src_name, ramp, bits):
    src = Image.open(ROOTS[0] / f"textures/block/hot_pot_top_{src_name}.png").convert("RGBA")
    w, h = src.size
    lum = [[sum(src.getpixel((x, y))[:3]) / 3 for x in range(w)] for y in range(h)]
    lo = min(min(r) for r in lum)
    hi = max(max(r) for r in lum)
    out = Image.new("RGBA", (w, h))
    px = out.load()
    for y in range(h):
        for x in range(w):
            t = (lum[y][x] - lo) / max(1, hi - lo)
            c = lerp(ramp[0], ramp[1], t / 0.6) if t < 0.6 else lerp(ramp[1], ramp[2], (t - 0.6) / 0.4)
            px[x, y] = c + (255,)
    rnd = random.Random(name)
    for color, n in bits:
        for _ in range(n * w // 16):
            x, y = rnd.randrange(1, w - 2), rnd.randrange(1, h - 2)
            px[x, y] = color + (255,)
            if rnd.random() < 0.5:
                px[x + 1, y] = color + (255,)
    return out


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def main():
    for root in ROOTS:
        for name, (src_name, ramp, bits) in NEW.items():
            soup_texture(name, src_name, ramp, bits).save(root / f"textures/block/hot_pot_top_{name}.png")
            for f in sorted((root / "models/block").glob("*spicy*.json")):
                text = f.read_text(encoding="utf-8").replace("spicy", name)
                (f.parent / f.name.replace("spicy", name)).write_text(text, encoding="utf-8")
        for bs in BLOCKSTATES:
            f = root / "blockstates" / f"{bs}.json"
            d = json.loads(f.read_text(encoding="utf-8"))
            if "variants" in d:
                for key in [k for k in d["variants"] if "soup=spicy" in k]:
                    for name in NEW:
                        d["variants"][key.replace("spicy", name)] = json.loads(
                            json.dumps(d["variants"][key]).replace("spicy", name))
            else:
                parts = d["multipart"]
                have = {json.dumps(p, sort_keys=True) for p in parts}
                for p in [p for p in parts if p.get("when", {}).get("soup") == "spicy"]:
                    for name in NEW:
                        q = json.loads(json.dumps(p).replace("spicy", name))
                        if json.dumps(q, sort_keys=True) not in have:
                            parts.append(q)
            write(f, d)
    print("ok")


if __name__ == "__main__":
    main()
