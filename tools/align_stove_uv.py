"""矮灶台（stove / stove_lit）的砖纹按世界位置一比一对齐，和旁边的原版砖块接得上（用户 2026-10-23：高灶台接得上、矮的接不上）。

原来矮灶台把整张 16 像素的砖贴图压扁到 11 像素高，砖缝比原版密、位置也对不上。这里把四个侧面上
#front / #side 的 UV 改成和 gen_tall.py 的 aligned_uv 一样：第 y 像素取贴图第 16-y 行。灶台高 11，正面正好取
贴图 5..16 行 = 整个火口拱门，火口不会被切掉。重跑安全。

    python tools/align_stove_uv.py
作者：
"""
import json
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
ROOTS = [RES / "assets/orients_hotpot_chronicles", RES / "resourcepacks/hotpot_assets/assets/orients_hotpot_chronicles"]


def aligned_uv(face, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    v0, v1 = 16 - y1, 16 - y0
    return {"north": [16 - x1, v0, 16 - x0, v1], "south": [x0, v0, x1, v1],
            "east": [16 - z1, v0, 16 - z0, v1], "west": [z0, v0, z1, v1]}[face]


for root in ROOTS:
    for name in ("stove", "stove_lit"):
        p = root / f"models/block/{name}.json"
        m = json.loads(p.read_text(encoding="utf-8"))
        for e in m["elements"]:
            for d, f in e["faces"].items():
                if d in ("north", "east", "south", "west") and f["texture"] in ("#front", "#side"):
                    f["uv"] = aligned_uv(d, e["from"], e["to"])
        p.write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8")
print("ok")
