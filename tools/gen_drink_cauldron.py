"""饮料炼药锅（2026-10-27 用户：饮料能倒进炼药锅里）：生成贴图、方块模型、方块状态、战利品表。

外观就是原版装水的炼药锅：模型直接用原版 template_cauldron_level1 / level2 / full，
只把水面 content 换成按饮料颜色重新上色的原版 water_still（同样 32 帧、同样的流动动画）。
之晗可乐的锅面再加冒上来破掉的气泡（2026-10-27 用户：可乐的都要有气泡，和瓶子、杯子的气泡同色）。
一锅最多 12 口（= 3 满瓶，像原版 3 瓶水装满一锅）；方块状态 sips 1~12，每 4 口高一档。

    python tools/gen_drink_cauldron.py            写进模组
作者：mryt233
"""
import json
import random
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "common/src/main/resources"
ASSETS = RES / "assets" / MODID
DATA = RES / "data" / MODID
WATER = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures/block/water_still.png"

# 饮料颜色：暗、基色、亮（和 gen_cup.py 杯子里的一致）
COLORS = {
    "orange_drink": ((212, 118, 12), (247, 160, 20), (255, 196, 70)),
    "plant_protein_drink": ((212, 204, 190), (238, 232, 220), (250, 248, 240)),
    "orient_cola": ((52, 26, 16), (78, 40, 24), (112, 62, 36)),
}
SIPS = 12
PER_LEVEL = 4
LEVELS = {1: "level1", 2: "level2", 3: "full"}


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def texture(kind):
    """原版水面按亮度重新上色：最暗 → 饮料暗色，中间 → 基色，最亮（浪花）→ 亮色。不透明。"""
    lo, base, hi = COLORS[kind]
    src = Image.open(WATER).convert("RGBA")
    out = Image.new("RGBA", src.size)
    sp, op = src.load(), out.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            t = max(0.0, min(1.0, (sp[x, y][0] - 165) / 60))
            c = mix(lo, base, t / 0.5) if t < 0.5 else mix(base, hi, (t - 0.5) / 0.5)
            op[x, y] = c + (255,)
    return out


# 可乐气泡：和 gen_cup.py 杯子里的气泡、拖尾同色
BUBBLE = (212, 182, 152, 255)
GLINT = (150, 104, 76, 255)


def fizz(sheet, seed=5, count=14):
    """锅面上的气泡：冒出来（亮点两帧）→ 破开（四周一圈淡色一帧）→ 没了。各个气泡位置、起点、周期错开，
    周期取 32 的约数，动画首尾接得上。"""
    w = sheet.size[0]
    frames = sheet.size[1] // w
    rnd = random.Random(seed)
    px = sheet.load()
    for _ in range(count):
        x, y = rnd.randint(1, w - 2), rnd.randint(1, w - 2)
        period = rnd.choice((8, 16, 16, 32))
        start = rnd.randrange(period)
        for f in range(frames):
            age = (f - start) % period
            oy = f * w
            if age < 2:
                px[x, oy + y] = BUBBLE
            elif age == 2:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    px[x + dx, oy + y + dy] = GLINT
    return sheet


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(obj, indent=2, ensure_ascii=False) + "\n").encode())


def level_of(sips):
    return (sips + PER_LEVEL - 1) // PER_LEVEL


def install():
    for kind in COLORS:
        tex = ASSETS / f"textures/block/{kind}_cauldron_still.png"
        img = texture(kind)
        if kind == "orient_cola":
            img = fizz(img)
        img.save(tex)
        tex.with_name(tex.name + ".mcmeta").write_bytes(WATER.with_name(WATER.name + ".mcmeta").read_bytes())
        for lv, name in LEVELS.items():
            write(ASSETS / f"models/block/{kind}_cauldron_{name}.json", {
                "parent": f"minecraft:block/template_cauldron_{name}",
                "textures": {
                    "bottom": "minecraft:block/cauldron_bottom",
                    "content": f"{MODID}:block/{kind}_cauldron_still",
                    "inside": "minecraft:block/cauldron_inner",
                    "particle": "minecraft:block/cauldron_side",
                    "side": "minecraft:block/cauldron_side",
                    "top": "minecraft:block/cauldron_top",
                },
            })
    variants = {}
    for kind in COLORS:
        for s in range(1, SIPS + 1):
            variants[f"kind={kind},sips={s}"] = {
                "model": f"{MODID}:block/{kind}_cauldron_{LEVELS[level_of(s)]}"}
    write(ASSETS / "blockstates/drink_cauldron.json", {"variants": variants})
    # 拆了掉原版炼药锅（饮料不掉，同原版水炼药锅）
    write(DATA / "loot_table/blocks/drink_cauldron.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": "minecraft:cauldron"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/drink_cauldron",
    })
    tag = RES / "data/minecraft/tags/block/mineable/pickaxe.json"
    t = json.loads(tag.read_text(encoding="utf-8"))
    ref = f"{MODID}:drink_cauldron"
    if ref not in t["values"]:
        t["values"].append(ref)
        write(tag, t)


if __name__ == "__main__":
    install()
    print("饮料炼药锅：3 种饮料 × 3 档，sips 1~12")
