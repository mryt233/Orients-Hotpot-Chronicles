"""鸳鸯锅 / 田字锅（2026-10-23 用户要，照用户发的照片：S 形太极隔板、O 形中间一圈、中间一道直隔板、田字十字隔板）。

锅身照现有方形九宫格锅（同尺寸、同贴图，能放灶台、高灶台、篝火），去掉九宫格的四根隔板换成对应形状的隔板。
汤面不在模型里：每边一种汤（田字锅四边），由 client/HotPotBlockEntityRenderer 按 PotLayout.regions 画，
贴图按汤底名取，所以任何汤底、以后新加的汤底都能放任何一边，不用为每种汤底组合出模型 / 方块状态。

形状 classify 和 Java 的 PotLayout.sideAt 一致（改一边要改另一边）；隔板按半像素格子算、一行一行合并成长方块。

输出（两份资源目录都写）：
  models/block/<锅名>_hot_pot_shell.json          独立摆放的锅身 + 隔板（锅名 = copper_yuanyang_s / iron_four_grid ……，同 HotPotMaterial.modelKey）
  models/block/stove_<锅名>_hot_pot_shell.json    坐在灶台上
  models/block/tall_stove_<锅名>_hot_pot_shell.json 坐在高灶台上
  models/item/<方块ID>.json                       物品（同九宫格锅的摆放角度）
  blockstates/<方块ID>.json                       只有锅身（axis），篝火支架由 gen_pot_on_campfire.py 接着加
  blockstates/brick_stove.json / tall_brick_stove.json 里这几种锅的条目（先删旧的再加，重跑安全）
  data：战利品表（连汤带食材掉锅物品）、配方、镐子 / 石镐标签
    python tools/gen_yuanyang.py      （会接着跑 gen_pot_on_campfire 加篝火支架）
作者：
"""
import json
import math
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]
DATA = RES / "data"
METALS = ["copper", "iron"]
# 形状代号, 锅名后缀（模型名）, 方块 ID 模板, 配方摆法（I = 同金属锭，P = 同金属九宫格锅）
SHAPES = [
    ("s", "yuanyang_s", "{m}_s_yuanyang_hotpot", ["IP"]),
    ("o", "yuanyang_o", "{m}_o_yuanyang_hotpot", ["P", "I"]),
    ("split", "yuanyang_split", "{m}_split_yuanyang_hotpot", ["I", "P"]),
    ("four", "four_grid", "{m}_four_grid_hotpot", ["IPI"]),
]

STEP = 0.5          # 半像素格子
HALF_W = 0.5        # 隔板半宽（像素）→ 隔板约 1 像素厚
INNER = 13.0        # 锅腔边长（像素，模型里独立锅 1.5..14.5）
# 三种锅身：锅腔起点、边长、隔板底 / 顶（九宫格隔板顶：独立 6.5、灶上 15.4、高灶台 25.4）
BODIES = {
    "": (1.5, 13.0, 1.0, 6.5),
    "stove_": (1.837, 12.326, 8.8, 15.4),
    "tall_stove_": (1.837, 12.326, 18.8, 25.4),
}


def classify(shape, x, z):
    """锅腔里的一点 (x, z)（0..13 像素，模型坐标）属于哪边：None = 隔板，否则边号 0..3。同 PotLayout.sideAt。"""
    c = INNER / 2
    if shape == "split":
        return None if abs(x - c) < HALF_W else (0 if x < c else 1)
    if shape == "four":
        if abs(x - c) < HALF_W or abs(z - c) < HALF_W:
            return None
        return (0 if x < c else 1) if z < c else (3 if x < c else 2)
    if shape == "o":
        d = math.hypot(x - c, z - c)
        if abs(d - 3.4) < HALF_W:
            return None
        return 1 if d < 3.4 else 0
    r = c / 2
    d_up = abs(math.hypot(x - c, z - r) - r) if x >= c else math.hypot(x - c, min(abs(z), abs(z - c)))
    d_dn = abs(math.hypot(x - c, z - c - r) - r) if x <= c else math.hypot(x - c, min(abs(z - c), abs(z - 2 * c)))
    if min(d_up, d_dn) < HALF_W:
        return None
    if z < c:
        left = x < c + math.sqrt(max(0.0, r * r - (z - r) ** 2))
    else:
        left = x < c - math.sqrt(max(0.0, r * r - (z - c - r) ** 2))
    return 0 if left else 1


def wall_runs(shape):
    """隔板格子一行一行合并：(i0, i1, j)，单位半像素格。"""
    n = int(INNER / STEP)
    out = []
    for j in range(n):
        i = 0
        while i < n:
            if classify(shape, (i + 0.5) * STEP, (j + 0.5) * STEP) is not None:
                i += 1
                continue
            k = i
            while k < n and classify(shape, (k + 0.5) * STEP, (j + 0.5) * STEP) is None:
                k += 1
            out.append((i, k, j))
            i = k
    return out


def walls(shape, body):
    x0, size, y0, y1 = BODIES[body]
    s = size / INNER * STEP
    els = []
    for i0, i1, j in wall_runs(shape):
        faces = {f: {"texture": "#pot_side", "uv": [7, 2, 8, 7.5] if f != "up" else [7, 2, 8, 3]}
                 for f in ("north", "south", "east", "west", "up")}
        els.append({"from": [round(x0 + i0 * s, 4), y0, round(x0 + j * s, 4)],
                    "to": [round(x0 + i1 * s, 4), y1, round(x0 + (j + 1) * s, 4)], "faces": faces})
    return els


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def assets():
    for root in ROOTS:
        mb = root / "models/block"
        for f in list(mb.glob("*_yuanyang_*_shell.json")) + list(mb.glob("yuanyang_*_[ab]_*.json")):
            if "hot_pot_shell" not in f.name:      # 第一版预览留下的（锅身 / 汤面单独的模型），现在汤面由渲染器画
                f.unlink()
        stove_parts = {"brick_stove": [], "tall_brick_stove": []}
        for shape, key, block_tpl, _ in SHAPES:
            for metal in METALS:
                name = f"{metal}_{key}"
                block = block_tpl.format(m=metal)
                for body in BODIES:
                    base = json.loads((mb / f"{body}{metal}_hot_pot_shell.json").read_text(encoding="utf-8"))
                    # 九宫格锅壳最后四个元素是隔板，换成这种锅的隔板
                    base["elements"] = base["elements"][:-4] + walls(shape, body)
                    write(mb / f"{body}{name}_hot_pot_shell.json", base)
                item = json.loads((root / f"models/item/{metal}_nine_grid_hotpot.json").read_text(encoding="utf-8"))
                # 物品交给程序画（client/HotPotClient：锅身方块模型 + 按存的每边汤底画汤面），任意汤底组合都对；
                # 九宫格锅那种「每种汤一个物品模型」在这里组合太多（田字 5^4 种）。显示角度沿用九宫格锅物品
                item["parent"] = "minecraft:builtin/entity"
                item["textures"] = {"particle": f"{MODID}:block/hot_pot_{metal}_side"}
                item.pop("overrides", None)
                write(root / f"models/item/{block}.json", item)
                write(root / f"blockstates/{block}.json", {"multipart": [
                    {"when": {"axis": "x"}, "apply": {"model": f"{MODID}:block/{name}_hot_pot_shell"}},
                    {"when": {"axis": "z"}, "apply": {"model": f"{MODID}:block/{name}_hot_pot_shell", "y": 90}}]})
                for stove, prefix in (("brick_stove", "stove_"), ("tall_brick_stove", "tall_stove_")):
                    for soup in ("", "_soup"):
                        for axis, y in (("x", 0), ("z", 90)):
                            apply = {"model": f"{MODID}:block/{prefix}{name}_hot_pot_shell"}
                            if y:
                                apply["y"] = y
                            stove_parts[stove].append({"when": {"pot": f"{name}{soup}", "pot_axis": axis}, "apply": apply})
        for stove, parts in stove_parts.items():
            path = root / f"blockstates/{stove}.json"
            d = json.loads(path.read_text(encoding="utf-8"))
            mine = {p["when"]["pot"] for p in parts}
            d["multipart"] = [p for p in d["multipart"] if p.get("when", {}).get("pot") not in mine] + parts
            write(path, d)


def data():
    pick = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    stone = DATA / "minecraft/tags/block/needs_stone_tool.json"
    ids = []
    for shape, key, block_tpl, pattern in SHAPES:
        for metal in METALS:
            block = block_tpl.format(m=metal)
            ids.append(f"{MODID}:{block}")
            loot = json.loads((DATA / MODID / f"loot_table/blocks/{metal}_nine_grid_hotpot.json").read_text(encoding="utf-8"))
            loot["pools"][0]["entries"][0]["name"] = f"{MODID}:{block}"
            write(DATA / MODID / f"loot_table/blocks/{block}.json", loot)
            # 同金属九宫格锅 + 同金属锭（用户定），四种锅靠摆法区分
            write(DATA / MODID / f"recipe/{block}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                  "key": {"I": {"item": f"minecraft:{metal}_ingot"}, "P": {"item": f"{MODID}:{metal}_nine_grid_hotpot"}},
                  "result": {"id": f"{MODID}:{block}", "count": 1}})
    for tag in (pick, stone):
        t = json.loads(tag.read_text(encoding="utf-8"))
        for i in ids:
            if i not in t["values"]:
                t["values"].append(i)
        write(tag, t)


def main():
    assets()
    data()
    # 篝火支架（on_campfire 的锅身模型和方块状态条目）接着加上，单独跑本脚本结果就是完整的（2026-10-24，）
    import gen_pot_on_campfire
    gen_pot_on_campfire.main()
    for shape, *_ in SHAPES:     # 终端里画一张草图核对
        print(shape)
        n = int(INNER / STEP)
        for j in range(0, n, 2):
            print("".join("#" if (c := classify(shape, (i + 0.5) * STEP, (j + 0.5) * STEP)) is None else ".oxv"[c]
                          for i in range(n)))
    print("ok")


if __name__ == "__main__":
    main()
