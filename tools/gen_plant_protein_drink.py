"""维二（r101 预览，用户要的第二种饮料：植物蛋白饮料，外观照现实的白瓶紫标瓶子，名字避开真品牌）。

    python tools/gen_plant_protein_drink.py
大小和果橙多一样（总高 15 像素、粗 6 像素）；瓶身是圆的——MC 只能拼方块，每一层用两个交叉的长方块拼成
削掉四个角的「圆」（从上往下看是八边形）。从下往上：圆角瓶底、白色下半截（靠近瓶底一条红线）、
腰（收进去一圈）、紫色标签（白色「V」、一点黄）、白色上半截、圆圆的瓶肩、瓶颈、宽的淡紫色瓶盖。
贴图一条「环绕」的侧面图（四面都一样），一行 = 模型里 1 像素高。
作者：
"""
import json
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]

WH, WH_HI, WH_LO = (238, 234, 226, 255), (250, 248, 242, 255), (214, 208, 200, 255)   # 瓶身奶白
PU, PU_HI, PU_LO = (118, 66, 160, 255), (146, 96, 188, 255), (88, 46, 124, 255)       # 标签紫
CAP, CAP_HI, CAP_LO = (190, 156, 222, 255), (210, 184, 236, 255), (160, 124, 198, 255) # 瓶盖淡紫
YE, RD = (244, 204, 70, 255), (206, 44, 44, 255)


def texture():
    """侧面环绕图 0..6 × 0..15（第 0 行 = y 14..15）；顶 / 底 7..13 × 0..6；瓶盖侧面 7..13 × 7..9、瓶盖顶 7..11 × 10..14。"""
    im = Image.new("RGBA", (16, 16), WH)
    px = im.load()
    rows = {
        14: [WH_HI] * 6,
        13: [WH_HI, WH_HI, WH, WH, WH, WH_LO],                 # 瓶颈
        12: [WH_HI, WH, WH, WH, WH, WH_LO],                    # 瓶肩
        11: [WH_HI, WH_HI, WH, WH, WH, WH_LO],
        10: [WH_HI, WH_HI, WH_HI, WH, WH, WH_LO],              # 上半截（亮）
        9: [WH_HI, WH, WH, WH, WH, WH_LO],
        8: [PU_HI, PU_HI, PU, PU, PU, PU_LO],                  # 紫色标签上沿
        7: [PU, YE, WH_HI, PU, WH_HI, PU_LO],                  # 「V」+ 一点黄
        6: [PU, PU, WH_HI, WH_HI, PU, PU_LO],
        5: [PU_LO, PU, PU, PU, PU, PU_LO],                     # 标签下沿
        4: [WH_LO] * 6,                                        # 腰
        3: [WH_HI, WH, WH, WH, WH, WH_LO],                     # 下半截
        2: [WH_HI, WH, WH, WH, WH, WH_LO],
        1: [RD, RD, RD, RD, RD, RD],                           # 靠近瓶底的红线
        0: [WH, WH, WH, WH, WH_LO, WH_LO],                     # 瓶底
    }
    for y, row in rows.items():
        for x, c in enumerate(row):
            px[x, 14 - y] = c
    for y in range(0, 6):
        for x in range(7, 13):
            px[x, y] = WH_HI if (x + y) % 4 else WH
    for y in range(7, 9):
        for x in range(7, 13):
            px[x, y] = CAP_LO if (x % 2) else CAP          # 瓶盖侧面的竖纹
    for y in range(10, 14):
        for x in range(7, 11):
            px[x, y] = CAP_HI if (x + y) % 3 else CAP
    return im


def tier(y0, y1, d, kind="body", rows=None):
    """一层「圆」：直径 d（像素），两个交叉的长方块，四个角各削 1 像素（d ≥ 4）。
    rows=(下, 上)：这一层侧面用贴图里哪几行（按 y 算的行号，含下不含上）；不给就按自己的高度取。"""
    a, b = 8 - d / 2, 8 + d / 2
    c = 1 if d >= 4 else 0.5
    if kind == "cap":
        side = [7, 7, 13, 9]
        top = [7, 10, 11, 14]
    else:
        lo, hi = rows if rows else (y0, y1)
        side = [0, 15 - hi, 6, 15 - lo]
        top = [7, 0, 13, 6]
    faces = {k: {"uv": side, "texture": "#d"} for k in ["north", "south", "east", "west"]}
    faces["up"] = {"uv": top, "texture": "#d"}
    faces["down"] = {"uv": top, "texture": "#d"}
    return [{"from": [a, y0, a + c], "to": [b, y1, b - c], "faces": faces},
            # 第二块上下各收 0.01：两块顶面原来在同一高度重叠，进游戏会闪（0.1.2-alpha 后用户报瓶盖闪烁，调整）
            {"from": [a + c, y0 + 0.01, a], "to": [b - c, y1 - 0.01, b], "faces": json.loads(json.dumps(faces))}]


def model():
    els = []
    els += tier(0, 1, 5)                    # 圆角瓶底
    els += tier(1, 4.5, 6, rows=(1, 4))     # 下半截（红线在最下面一行）
    els += tier(4.5, 5, 5, rows=(4, 5))     # 腰：收进去一圈
    els += tier(5, 9, 6, rows=(5, 9))       # 紫色标签
    els += tier(9, 11, 6)                   # 上半截
    els += tier(11, 12, 5)          # 瓶肩（圆的）
    els += tier(12, 12.75, 4)
    els += tier(12.75, 13.25, 3)    # 瓶颈
    els += tier(13.25, 15, 4, "cap")  # 宽瓶盖
    return {"parent": "minecraft:block/block",
            "textures": {"d": f"{MODID}:block/plant_protein_drink", "particle": f"{MODID}:block/plant_protein_drink"},
            "elements": els}


def texture_empty(full):
    """喝完的空瓶：奶白的瓶身换成半透明感的淡灰白，标签和瓶盖留着。"""
    im = full.copy()
    px = im.load()
    pale = {WH: (226, 228, 230, 255), WH_HI: (240, 242, 244, 255), WH_LO: (204, 208, 212, 255)}
    for y in range(16):
        for x in range(16):
            if px[x, y] in pale:
                px[x, y] = pale[px[x, y]]
    return im


def main():
    tex = texture()
    for root in ASSET_ROOTS:
        tex.save(root / "textures/block/plant_protein_drink.png")
        texture_empty(tex).save(root / "textures/block/plant_protein_drink_empty.png")
        (root / "models/block/plant_protein_drink.json").write_text(json.dumps(model(), indent=2) + "\n", encoding="utf-8")
        empty = model()
        empty["textures"] = {"d": f"{MODID}:block/plant_protein_drink_empty", "particle": f"{MODID}:block/plant_protein_drink_empty"}
        (root / "models/block/plant_protein_drink_empty.json").write_text(json.dumps(empty, indent=2) + "\n", encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    main()
