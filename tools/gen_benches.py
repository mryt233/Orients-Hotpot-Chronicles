"""生成每种原版木板的板凳与高条凳预览。作者：mryt233

用法（在 mod/ 目录）：
    python tools/gen_benches.py
形状（朝北放的条凳，长边沿 x，与 BenchBlock 的常量一致）：
  - 凳面：y 5..7（坐面 7/16），z 4..12；接着别的条凳的一头铺到方块边界，没接的一头收进 0.5 像素。
  - 凳腿：只在没接的那一头，前后两条 y 0..5，中间一根横撑。
  - left / right：站在凳子后面面朝 facing 时的左手边 / 右手边（朝北时 = 西 / 东）。
"""
import json


# mryt233 改（2026-11-10）：共用路径、保留原资源格式。
from pixkit.paths import MODID, RES
from pixkit.io import write_legacy_json
from pixkit.templates import write_shared_model as write
from pixkit.materials import ids

ASSET_ROOTS = [RES / "assets" / MODID]
DATA = RES / "data"
WOODS = ids("woods")


def generate_previews():
    """mryt233 改（2026-11-10）：高条凳额外生成完整预览，游戏模型只加说明。"""
    from bench_preview import VARIANTS, complete_model, explanation, java_parameters, preview_root
    from model_bake import ModelResolver
    from pixkit.manifest import declare_merge
    resolver = ModelResolver(RES)
    parameters = java_parameters()
    for wood in WOODS:
        for (left, right), tail in VARIANTS.items():
            name = f"{wood}_bench{tail}"
            relative = f"assets/{MODID}/models/block/{name}.json"
            file = RES / relative
            source = json.loads(file.read_text(encoding="utf-8"))
            source["_说明"] = explanation(name)
            write_legacy_json(file, source, ensure_ascii=False)
            declare_merge(file, ["tools/gen_tall.py", "tools/gen_benches.py"],
                          "先生成游戏凳面和横撑，再补预览说明；不改变游戏烘焙结果")
            base = resolver.merged(f"{MODID}:block/{name}")
            write_legacy_json(preview_root() / relative,
                              complete_model(base, left, right, parameters), ensure_ascii=False)
        item = resolver.merged(f"{MODID}:item/{wood}_bench")
        base = resolver.merged(f"{MODID}:block/{wood}_bench")
        base["display"] = item["display"]
        write_legacy_json(preview_root() / f"assets/{MODID}/models/item/{wood}_bench.json",
                          complete_model(base, False, False, parameters), ensure_ascii=False)

SEAT_Y0, SEAT_Y1 = 5, 7
SEAT_Z0, SEAT_Z1 = 4, 12
END_INSET = 0.5
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def box(f, t, skip=(), cull_down=False):
    faces = {}
    for d in ["north", "east", "south", "west", "up", "down"]:
        if d in skip:
            continue
        faces[d] = {"texture": "#planks"}
        if d == "down" and cull_down:
            faces[d]["cullface"] = "down"
    return {"from": f, "to": t, "faces": faces}


def legs(x0, x1):
    """一头的两条腿 + 横撑（x0..x1 是腿的宽度范围）。"""
    mid = (x0 + x1) / 2
    return [
        box([x0, 0, 4.5], [x1, SEAT_Y0, 6.5], cull_down=True),
        box([x0, 0, 9.5], [x1, SEAT_Y0, 11.5], cull_down=True),
        box([mid - 0.5, 2, 6.5], [mid + 0.5, 3, 9.5]),
    ]


def model(left, right, planks, all_faces=False):
    x0 = 0 if left else END_INSET
    x1 = 16 if right else 16 - END_INSET
    skip = set() if all_faces else ({"west"} if left else set()) | ({"east"} if right else set())
    els = [box([x0, SEAT_Y0, SEAT_Z0], [x1, SEAT_Y1, SEAT_Z1], skip)]
    if not left:
        els += legs(1.5, 3.5)
    if not right:
        els += legs(12.5, 14.5)
    return {"parent": "minecraft:block/block", "textures": {"planks": planks, "particle": planks}, "elements": els}


def suffix(left, right):
    return {(False, False): "", (True, False): "_left", (False, True): "_right", (True, True): "_middle"}[(left, right)]



def main():
    for root in ASSET_ROOTS:
        for wood in WOODS:
            name = f"{wood}_stool"   # 模型名和 ID 一致（2026-10-24 用户：alpha 名字对不上的都改；以前板凳模型叫 <木头>_bench）
            planks = f"minecraft:block/{wood}_planks"
            variants = {}
            for left in (False, True):
                for right in (False, True):
                    write(root / f"models/block/{name}{suffix(left, right)}.json", model(left, right, planks))
                    for facing, y in FACING_Y.items():
                        v = {"model": f"{MODID}:block/{name}{suffix(left, right)}"}
                        if y:
                            v["y"] = y
                            # 贴图锁定：转了方向木纹也按世界方向贴，正着、反着放的凳子拼在一起木纹接得上（用户 2026-10-26，调整）
                            v["uvlock"] = True
                        variants[f"facing={facing},left={str(left).lower()},right={str(right).lower()}"] = v
            ident = f"{wood}_stool"
            write(root / f"blockstates/{ident}.json", {"variants": variants})
            write(root / f"models/item/{ident}.json", model(False, False, planks, all_faces=True))

    for wood in WOODS:
        name = f"{wood}_stool"
        write(DATA / MODID / f"recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "group": f"{MODID}:stool",
            "pattern": ["PPP", "S S"],
            "key": {"P": {"item": f"minecraft:{wood}_planks"}, "S": {"item": "minecraft:stick"}},
            "result": {"id": f"{MODID}:{name}", "count": 3},
        })
        write(DATA / MODID / f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "conditions": [{"condition": "minecraft:survives_explosion"}],
                       "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}]}],
        })
    axe = DATA / "minecraft/tags/block/mineable/axe.json"
    tag = json.loads(axe.read_text(encoding="utf-8")) if axe.exists() else {"replace": False, "values": []}
    for wood in WOODS:
        ident = f"{MODID}:{wood}_stool"
        if ident not in tag["values"]:
            tag["values"].append(ident)
    write(axe, tag)
    generate_previews()
    print(f"已生成 {len(WOODS)} 种板凳，以及条凳 44 个方块 / 11 个物品预览")


if __name__ == "__main__":
    main()
