"""锅放在篝火上（0.1.2-alpha 后用户报：锅浮在篝火上方）。

篝火只有 7 像素高，独立摆放的锅坐在自己那一格的底面，离篝火顶还差 9 像素。方块状态加 `on_campfire`
（下面是篝火 / 灵魂篝火时为 true，HotPotBlock 自己维护）。第一版为 true 时整口锅下沉 9 像素，但原版篝火火苗
有 17 像素高，火从锅底穿进锅里、锅也不好挖（用户报）；现在不下沉（DROP = 0），改成锅底四角加细铁支架
踩到篝火木头上，锅底正好压住火苗尖。

做的事：
  1. 把方块状态里用到的模型（锅壳、各种汤面）各复制一份 `<名字>_on_campfire.json`，所有元素 y 减 9；
     没写 UV 的面先按原位置算好默认 UV 再写死，免得挪了位置贴图跟着错位。
  2. 重写两口锅的方块状态：原来的每条加 `on_campfire=false`，再复制一条 `on_campfire=true` 换成下沉模型。
重跑安全（从不带 on_campfire 的原始条目重建）。

    python tools/gen_pot_on_campfire.py
作者：
"""
import copy
import json
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]
POTS = ["copper_nine_grid_hotpot", "iron_nine_grid_hotpot"] + [   # 鸳鸯锅 / 田字锅（gen_yuanyang.py，新增）
    f"{m}_{k}" for m in ("copper", "iron")
    for k in ("s_yuanyang_hotpot", "o_yuanyang_hotpot", "split_yuanyang_hotpot", "four_grid_hotpot")]
DROP = 0.0   # 不下沉了：原版篝火火苗高 17 像素，锅压下去火会穿进锅里（用户报）；锅底正好压住火苗尖
LEG_TOP, LEG_BOTTOM = 0.0, -9.0   # 支架：从锅底一直到篝火木头顶（篝火 7 像素高）
SUFFIX = "_on_campfire"


def default_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {"down": [x0, 16 - z1, x1, 16 - z0], "up": [x0, z0, x1, z1],
            "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "west": [z0, 16 - y1, z1, 16 - y0], "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0]}[face]


def lowered(model):
    m = copy.deepcopy(model)
    for e in m.get("elements", []):
        for face, fc in e["faces"].items():
            if "uv" not in fc:
                fc["uv"] = default_uv(face, e["from"], e["to"])
        e["from"][1] -= DROP
        e["to"][1] -= DROP
        if "rotation" in e:
            e["rotation"]["origin"][1] -= DROP
    return m


def legs():
    """锅底四角各一根 1×1 的细支架，往下到篝火木头上，用锅自己的侧面贴图（铜锅铜色、铁锅铁色）。"""
    els = []
    for x in (2, 13):
        for z in (2, 13):
            faces = {d: {"texture": "#pot_side", "uv": [7, 0, 8, 9]} for d in ("north", "south", "east", "west")}
            faces["down"] = {"texture": "#pot_side", "uv": [7, 0, 8, 1]}
            els.append({"from": [x, LEG_BOTTOM, z], "to": [x + 1, LEG_TOP, z + 1], "faces": faces})
    return els


def main():
    for root in ROOTS:
        for pot in POTS:
            path = root / f"blockstates/{pot}.json"
            parts = [p for p in json.loads(path.read_text(encoding="utf-8"))["multipart"]
                     if p.get("when", {}).get("on_campfire") != "true"]
            out = []
            for p in parts:
                when = {k: v for k, v in p.get("when", {}).items() if k != "on_campfire"}
                apply = p["apply"]
                name = apply["model"].split("/")[-1]
                src = root / f"models/block/{name}.json"
                low = lowered(json.loads(src.read_text(encoding="utf-8")))
                if name.endswith("_shell"):
                    low["elements"] = low["elements"] + legs()
                (root / f"models/block/{name}{SUFFIX}.json").write_text(json.dumps(low, indent=2) + "\n", encoding="utf-8")
                out.append({"when": {**when, "on_campfire": "false"}, "apply": apply})
                out.append({"when": {**when, "on_campfire": "true"},
                            "apply": {**apply, "model": apply["model"] + SUFFIX}})
            path.write_text(json.dumps({"multipart": out}, indent=2) + "\n", encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    main()
