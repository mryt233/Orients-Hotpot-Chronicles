"""从动画共用的整像素几何导出查看器静态小石磨，不写入任何游戏资源。作者：mryt233"""
import json
from itertools import combinations
from pathlib import Path

import export_bbmodel as export
from pixkit.model import box

MOD = Path(__file__).resolve().parents[1]
DATA = MOD / "viewer/stone_mill_pixel_preview.json"
OUT = MOD / "model-previews/stone_mill_pixel_grid_review.bbmodel"


def intersects(a, b):
    return all(a[k] < b[k + 3] and b[k] < a[k + 3] for k in range(3))


def main():
    data = json.loads(DATA.read_bytes())
    rows = data["base"] + data["top"]
    for row in rows:
        assert row[6] in data["textures"], row
        assert all(type(v) is int for v in row[:6]), row
        assert 0 <= row[0] < row[3] <= 16 and 0 <= row[1] < row[4] <= 24 and 0 <= row[2] < row[5] <= 16, row
        for face in ("up", "down", "north", "south", "east", "west"):
            assert all(type(v) is int for v in export.default_uv(face, row[:3], row[3:6]))
    assert all(not intersects(a, b) for a, b in combinations(rows, 2)), "模型有重叠体积"
    _, _, hub_front, rim_front = data["slot"]["ring"]
    left, right, end = data["slot"]["mouth"]
    floor = data["slot"]["floor"]
    assert not any(intersects(row, [left, floor, hub_front, right, floor + 2, end]) for row in rows), "导流嘴堵塞"
    for z in (hub_front + .5, rim_front + .5, end - .5):
        assert any(a < 8 < d and c < z < f and e == floor for a, b, c, d, e, f, _ in rows), "槽底断开"

    elements = lambda key: [box(row[:3], row[3:6], "#" + row[6]) for row in data[key]]
    export.PARTS = [("固定下盘与木架", "base"), ("转动上盘与木柄", "top")]
    export.NAME = "小石磨｜整像素圆石（外观预览）"
    export.OUT = OUT
    export.load_model = lambda key: {"textures": data["textures"], "elements": elements(key)}
    export.main()
    bb = json.loads(OUT.read_bytes())
    bb["credit"] = "作者：mryt233"
    OUT.write_bytes((json.dumps(bb, ensure_ascii=False, indent=1) + "\n").encode("utf-8"))
    assert {t["name"] for t in bb["textures"]} == {"cobblestone.png", "stone_mill_wood.png"}
    print("整数顶点、整数 UV、无相交体积、导流槽贯通：通过。")


if __name__ == "__main__":
    main()
