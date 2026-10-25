"""1.5 格高的火锅桌、1.5 格高的灶台、一格高的条凳：模型、方块状态、物品模型、配方、战利品表、挖掘标签。
（r82 先做成预览给用户看，r83 用户确认后注册进游戏。）

用法（在 mod/ 目录）：
    python tools/gen_tall.py
  - 高火锅桌 `<木头>_tall_hotpot_table`：桌面板 y 20..24 铺满（1.5 格），下面 0..20 朝外收 4 像素，连接规则同矮桌；
    下面一格（0..16）是原版砖块、上面半格是木板。open=true（正对灶膛口的那张）去掉砖身只留木桌面。
  - 高灶台 `tall_stove`：灶膛（带火口的那一段）仍在最下面 y 0..7.6，中间加一段砖 7.6..18.6，
    上面的灶口、锅、汤全部照矮灶台上移 11 像素：砖面 22（比高桌面低 2 像素）、锅沿 27（露出桌面 3 像素）。
  - 条凳 `<木头>_bench`：一格高，凳面 y 14..16、z 5..11；每头两条腿，腿间上下两根横撑（像照片里的长条凳）。
"""
import copy
import json
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]
# 高火锅桌桌面板 y TOP0..TOP1：r82 定 20..24（1.5 格），r84 用户说桌子和灶台都再低一点 → 19..23
TOP0, TOP1 = 19.0, 23.0
# r82：先按砖面与 1.5 格桌面齐平上移 13（砖面 24、锅沿 29）；用户看了说高了，降 3 像素 → 上移 10；又说再高一点 → 上移 11（砖面 22、锅沿 27，露出桌面 3 像素）；r84 和桌子一起降 1 → 上移 10（砖面 21、锅沿 26，桌面 23）
LIFT = 10.0
BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}
# 条凳腿到地面各往外岔开的像素（上端在凳面下 y=14）
LEG_SPREAD = 2.0


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def faces(skip=(), cull_down=False):
    return {d: {"texture": "#planks", **({"cullface": "down"} if d == "down" and cull_down else {})}
            for d in ["north", "east", "south", "west", "up", "down"] if d not in skip}


# ---------------------------------------------------------------- 高火锅桌

def box_uv(frm, to, skip=(), cull_down=False, rows=None):
    """侧面的贴图竖向用 rows=(v0, v1)（贴图里的行），不用默认算法：方块高过 16 时默认 UV 会跑出贴图，纹路错开。
    横向照默认算法（跟着 x / z 位置），上下两面也照默认。"""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    v0, v1 = rows
    uv = {"north": [16 - x1, v0, 16 - x0, v1], "south": [x0, v0, x1, v1],
          "east": [16 - z1, v0, 16 - z0, v1], "west": [z0, v0, z1, v1]}
    out = {}
    for d in ["north", "east", "south", "west", "up", "down"]:
        if d in skip:
            continue
        f = {"texture": "#planks"}
        if d in uv:
            f["uv"] = uv[d]
        if d == "down" and cull_down:
            f["cullface"] = "down"
        out[d] = f
    return {"from": frm, "to": to, "faces": out}


def spans(mask):
    """接着桌子 / 灶台的一侧到方块边界，朝外一侧收进 4 像素。"""
    def span(lo_bit, hi_bit):
        lo_c, hi_c = bool(mask & lo_bit), bool(mask & hi_bit)
        return (0 if lo_c else 4), (16 if hi_c else 12)
    x0, x1 = span(8, 2)
    z0, z1 = span(1, 4)
    return x0, x1, z0, z1


def table_model(mask, planks):
    x0, x1, z0, z1 = spans(mask)
    connected = {d for d, b in zip(["north", "east", "south", "west"], [1, 2, 4, 8]) if mask & b}
    # 桌面板四个侧面都画：灶台砖面比桌面低 2 像素，朝灶台那一面省掉就会露出一圈透明缝（用户 r82 截图）。
    # 侧面用原版木板的一整条（贴图第 0..4 行），像原版木板方块的边；桌身木板条从下往上一条条对齐
    # （0..16 用整张贴图，16..20 再接一条），不再错位（用户 r82：「不连贯、下面做干净一点」）。
    # 下面一格高（0..16）用原版砖块，上面半格（16..24）用木板（用户 r82）
    # 侧面全都画：挨着灶膛口的那张桌子是空的，旁边桌子朝它那一面省掉就会漏（用户 r82 截图）
    skip_base = {"up"}
    bricks = box_uv([x0, 0, z0], [x1, 16, z1], skip=skip_base, cull_down=True, rows=(0, 16))
    for f in bricks["faces"].values():
        f["texture"] = "#bricks"
    # 桌面板 20..24 铺满整格（悬出来一圈），下面 16..20 的木板和砖身一起朝外收 4 像素
    return {"parent": "minecraft:block/block",
            "textures": {"planks": planks, "bricks": "minecraft:block/bricks", "particle": planks}, "elements": [
        box_uv([0, TOP0, 0], [16, TOP1, 16], rows=(0, 4)),
        bricks,
        box_uv([x0, 16, z0], [x1, TOP0, z1], skip={"up", "down"}, rows=(16 - (TOP0 - 16), 16)),
    ]}


def table_open_model(mask, planks):
    """灶膛口正对着的那张桌子：下面的砖身去掉，只留木头部分，样子和旁边桌子的木头部分完全一样
    （桌面板 20..24 铺满，16..20 那层朝外收 4 像素；之前这层是满宽的，比旁边多伸出来一截，用户 r82 指出）。"""
    x0, x1, z0, z1 = spans(mask)
    return {"parent": "minecraft:block/block", "textures": {"planks": planks, "particle": planks}, "elements": [
        box_uv([0, TOP0, 0], [16, TOP1, 16], rows=(0, 4)),
        box_uv([x0, 16, z0], [x1, TOP0, z1], skip=("up",), rows=(16 - (TOP0 - 16), 16)),
    ]}


def suffix(mask):
    s = "".join(c for c, b in [("n", 1), ("e", 2), ("s", 4), ("w", 8)] if mask & b)
    return "_" + s if s else ""


# ---------------------------------------------------------------- 条凳（一格高）

def bench_model(left, right, planks, all_faces=False):
    x0 = 0 if left else 0.5
    x1 = 16 if right else 15.5
    skip = set() if all_faces else ({"west"} if left else set()) | ({"east"} if right else set())
    els = [{"from": [x0, 14, 5], "to": [x1, 16, 11], "faces": faces(skip)}]

    def end(lx0, lx1):
        """一头的两根横撑。两条腿是「A」字斜着岔开的（上端 z 6.5 / 9.5，到地面各往外 2 像素，约 8°）：
        MC 方块模型只能转 22.5° 的倍数，做不出这么小的连贯斜度，所以腿不放在模型里，
        由渲染器按任意角度画（查看器 animations.js 的 benchLegs，游戏里以后用方块实体渲染器）。"""
        mid = (lx0 + lx1) / 2
        legs = []
        for y in (2.5, 7.0):
            out = (14 - (y + 0.5)) * LEG_SPREAD / 14
            legs.append({"from": [mid - 0.5, y, 6.5 - out], "to": [mid + 0.5, y + 1, 9.5 + out], "faces": faces()})
        return legs
    # 单独一张：腿靠两头；连成长凳时两头的腿往里收，凳面在腿外面多伸出一截（照用户给的长条凳照片）
    single = not left and not right
    if not left:
        els += end(1.5, 3.5) if single else end(4.5, 6.5)
    if not right:
        els += end(12.5, 14.5) if single else end(9.5, 11.5)
    return {"parent": "minecraft:block/block", "textures": {"planks": planks, "particle": planks}, "elements": els}


# ---------------------------------------------------------------- 高灶台

def lifted(model, dy):
    m = copy.deepcopy(model)
    for e in m.get("elements", []):
        e["from"][1] += dy
        e["to"][1] += dy
        if "rotation" in e:
            e["rotation"]["origin"][1] += dy
    return m


def aligned_uv(face, frm, to):
    """侧面按世界位置一比一取贴图（砖缝和旁边原版砖块对齐）；元素不能跨过 16 的整格线。"""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    band = 16 * int(y0 // 16)
    v0, v1 = 16 - (y1 - band), 16 - (y0 - band)
    return {"north": [16 - x1, v0, 16 - x0, v1], "south": [x0, v0, x1, v1],
            "east": [16 - z1, v0, 16 - z0, v1], "west": [z0, v0, z1, v1]}[face]


FIREBOX_TOP = 11.0   # 带火口的那一段：贴图里火口拱门占 5..16 行，按原尺寸显示（矮灶台是压扁到 7.6 的）


def tall_stove_base(src):
    """高灶台：最下面一段带火口（0..11，火口贴图一比一），中间补砖到灶口，上面的灶口墙整体上移；
    四个侧面的砖都按世界位置一比一对齐，和旁边高桌的原版砖块接得上（用户 r82）。"""
    m = copy.deepcopy(src)
    body, walls = m["elements"][0], m["elements"][1:]
    socket_up = body["faces"].pop("up")
    body["to"][1] = FIREBOX_TOP
    top = 7.6 + LIFT
    middles = []
    for y0, y1 in ((FIREBOX_TOP, 16.0), (16.0, top)):
        if y1 > y0:
            middles.append({"from": [0, y0, 0], "to": [16, y1, 16],
                            "faces": {d: {"texture": "#side"} for d in ["north", "east", "south", "west"]}})
    middles[-1]["faces"]["up"] = socket_up
    els = [body] + middles + lifted({"elements": walls}, LIFT)["elements"]
    for e in els:
        for d, f in e["faces"].items():
            if e is not body and f["texture"] == "#front":
                f["texture"] = "#side"   # 火口只在最下面那段；上面的墙用普通砖，免得对齐后又露出拱门
            if d in ("north", "east", "south", "west") and f["texture"] in ("#side", "#front"):
                f["uv"] = aligned_uv(d, e["from"], e["to"])
    m["elements"] = els
    return m


def main():
    for root in ASSET_ROOTS:
        blocks = root / "models/block"
        for wood in WOODS:
            planks = f"minecraft:block/{wood}_planks"
            name = f"{wood}_tall_hotpot_table"
            variants = {}
            for mask in range(16):
                write(blocks / f"{name}{suffix(mask)}.json", table_model(mask, planks))
                key = ",".join(f"{d}={'true' if mask & b else 'false'}"
                               for d, b in zip(["north", "east", "south", "west"], [1, 2, 4, 8]))
                variants[key] = {"model": f"{MODID}:block/{name}{suffix(mask)}"}
            # open=true：这张桌子正对着灶膛口，不管怎么连接都只留木桌面
            variants = {f"{k},open=false": v for k, v in variants.items()}
            for mask in range(16):
                write(blocks / f"{name}_open{suffix(mask)}.json", table_open_model(mask, planks))
                key = ",".join(f"{d}={'true' if mask & b else 'false'}"
                               for d, b in zip(["north", "east", "south", "west"], [1, 2, 4, 8]))
                variants[f"{key},open=true"] = {"model": f"{MODID}:block/{name}_open{suffix(mask)}"}
            write(root / f"blockstates/{name}.json", {"variants": variants})

            name = f"{wood}_bench"   # 模型名和 ID 一致（2026-10-24；以前条凳模型叫 <木头>_tall_bench）
            variants = {}
            sfx = {(False, False): "", (True, False): "_left", (False, True): "_right", (True, True): "_middle"}
            for left in (False, True):
                for right in (False, True):
                    write(blocks / f"{name}{sfx[(left, right)]}.json", bench_model(left, right, planks))
                    for facing, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items():
                        v = {"model": f"{MODID}:block/{name}{sfx[(left, right)]}"}
                        if y:
                            v["y"] = y
                        variants[f"facing={facing},left={str(left).lower()},right={str(right).lower()}"] = v
            write(root / f"blockstates/{wood}_bench.json", {"variants": variants})

        # 高灶台：方块状态照抄矮灶台，把每个模型换成上移版本
        stove_state = json.loads((root / "blockstates/brick_stove.json").read_text(encoding="utf-8"))
        made = set()
        for part in stove_state["multipart"]:
            mid = part["apply"]["model"].split("/")[-1]
            tall = "tall_" + mid
            if tall not in made:
                src = json.loads((blocks / f"{mid}.json").read_text(encoding="utf-8"))
                write(blocks / f"{tall}.json", tall_stove_base(src) if mid in ("stove", "stove_lit") else lifted(src, LIFT))
                made.add(tall)
            part["apply"]["model"] = f"{MODID}:block/{tall}"
        write(root / "blockstates/tall_brick_stove.json", stove_state)
        # 物品模型：高的东西在物品栏里缩小一点、往下挪，免得顶出格子
        tall_display = dict(BLOCK_DISPLAY, gui={"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]})
        write(root / "models/item/tall_brick_stove.json", {"parent": f"{MODID}:block/tall_stove", "display": tall_display})
        for wood in WOODS:
            write(root / f"models/item/{wood}_tall_hotpot_table.json",
                  {"parent": f"{MODID}:block/{wood}_tall_hotpot_table_esw", "display": tall_display})
            # 条凳的斜腿不在方块模型里，物品交给 builtin/entity（客户端 BuiltinItemRenderer 画凳面 + 斜腿）
            write(root / f"models/item/{wood}_bench.json",
                  {"parent": "minecraft:builtin/entity", "display": BLOCK_DISPLAY,
                   "textures": {"particle": f"minecraft:block/{wood}_planks"}})
    data = RES / "data"
    loot = lambda name: {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                         "conditions": [{"condition": "minecraft:survives_explosion"}],
                         "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}]}]}
    write(data / MODID / "recipe/tall_brick_stove.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": f"{MODID}:brick_stove"}, {"item": "minecraft:bricks"}],
        "result": {"id": f"{MODID}:tall_brick_stove", "count": 1}})
    write(data / MODID / "loot_table/blocks/tall_brick_stove.json", loot("tall_brick_stove"))
    for wood in WOODS:
        write(data / MODID / f"recipe/{wood}_tall_hotpot_table.json", {
            "type": "minecraft:crafting_shaped", "group": f"{MODID}:tall_hotpot_table", "pattern": ["PPP", "BBB"],
            "key": {"P": {"item": f"minecraft:{wood}_planks"}, "B": {"item": "minecraft:bricks"}},
            "result": {"id": f"{MODID}:{wood}_tall_hotpot_table", "count": 3}})
        write(data / MODID / f"recipe/{wood}_bench.json", {
            "type": "minecraft:crafting_shaped", "group": f"{MODID}:bench", "pattern": ["PPP", "S S", "S S"],
            "key": {"P": {"item": f"minecraft:{wood}_planks"}, "S": {"item": "minecraft:stick"}},
            "result": {"id": f"{MODID}:{wood}_bench", "count": 3}})
        write(data / MODID / f"loot_table/blocks/{wood}_tall_hotpot_table.json", loot(f"{wood}_tall_hotpot_table"))
        write(data / MODID / f"loot_table/blocks/{wood}_bench.json", loot(f"{wood}_bench"))
    for tag_file, ids in [("mineable/pickaxe.json", ["tall_brick_stove"]),
                          ("mineable/axe.json", [f"{w}_tall_hotpot_table" for w in WOODS] + [f"{w}_bench" for w in WOODS])]:
        f = data / "minecraft/tags/block" / tag_file
        tag = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"replace": False, "values": []}
        for i in ids:
            if f"{MODID}:{i}" not in tag["values"]:
                tag["values"].append(f"{MODID}:{i}")
        write(f, tag)
    print("已生成：高灶台、11 种高火锅桌、11 种条凳")


if __name__ == "__main__":
    main()
