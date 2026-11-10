"""之晗可乐（2026-10-25 用户定名 sino 可乐，2026-10-26 改名之晗可乐，ID orient_cola）：贴图 + 模型（满瓶 / 空瓶）。

    python tools/gen_orient_cola.py
拼法借植物蛋白饮料（每层两个交叉长方块拼成削角的「圆」，tools/gen_plant_protein_drink.py 的 tier）。
样子（用户发玻璃可乐瓶照片后逐像素调定）：玻璃汽水瓶——可乐色瓶底、下段鼓起、一道收腰、红标签白波纹、瓶肩、
瓶颈（下半截可乐、上半截空玻璃更细）、小红皇冠盖；可乐段和瓶颈玻璃有反光亮条（标签上没有）；不做真品牌的竖向凹槽纹和字。
空瓶：可乐换成玻璃，标签留着，没有瓶盖。
作者：mryt233
"""
import json


# mryt233 改（2026-11-10）：共用路径、保留原资源格式。
from pixkit.paths import MODID, RES
from pixkit.io import write_legacy_json as write

from PIL import Image

import fizz
import gen_plant_protein_drink as ppd

ASSET_ROOTS = [RES / "assets" / MODID]

CO, CO_HI, CO_LO = (78, 40, 24, 255), (112, 62, 36, 255), (52, 26, 16, 255)       # 可乐深棕
RD, RD_HI, RD_LO = (200, 30, 36, 255), (226, 58, 60, 255), (150, 18, 26, 255)     # 标签红
WH = (246, 242, 236, 255)
CAP, CAP_HI, CAP_LO = (204, 34, 40, 255), (232, 70, 72, 255), (160, 22, 30, 255)   # 瓶盖红
FOAM = (196, 168, 140, 255)                                                       # 瓶颈里一点泡沫色


GL, GL_HI, GL_LO = (200, 214, 214, 255), (234, 242, 242, 255), (158, 176, 178, 255)   # 玻璃


def texture():
    """布局和 gen_plant_protein_drink.texture 一样：侧面环绕 0..6 × 0..15（第 0 行 = y 14..15），顶 / 底 7..13 × 0..6，瓶盖侧 / 顶。
    玻璃汽水瓶（2026-10-26 用户发照片要的样式）：厚玻璃瓶底、可乐色瓶身（第 1 列一道玻璃反光）、中间红标签带白波纹、
    瓶肩、细瓶颈（下半截可乐、上半截空玻璃）、红色皇冠瓶盖。"""
    im = Image.new("RGBA", (16, 16), CO)
    px = im.load()
    # 玻璃反光（用户 2026-10-26）：可乐和瓶颈玻璃上每面靠左一道竖亮条（SHEEN），旁边一列稍亮过渡；红标签上不加（用户）
    SHEEN, SHEEN_SOFT = (176, 128, 98, 255), (128, 78, 50, 255)
    body = [CO_LO, SHEEN, SHEEN_SOFT, CO, CO, CO_LO]
    body_dim = [CO_LO, SHEEN_SOFT, CO_HI, CO, CO, CO_LO]       # 靠瓶底、瓶肩的地方反光弱一点
    rows = {
        12: [GL_HI, (252, 254, 254, 255), GL, GL, GL, GL_LO],  # 瓶口
        11: [GL_HI, (252, 254, 254, 255), GL_HI, GL, GL, GL_LO],   # 瓶颈上半截：空的玻璃，亮条更白
        10: body_dim,                                          # 瓶颈下半截：可乐
        9: body_dim,                                           # 瓶肩
        8: [RD_HI, RD_HI, RD, RD, RD, RD_LO],                  # 红标签上沿
        7: [WH, WH, RD, RD, WH, WH],                           # 一道白波纹（上下错开，绕一圈像波浪）
        6: [RD, RD, WH, WH, RD, RD_LO],
        5: [RD_LO, RD, RD, RD, RD, RD_LO],                     # 标签下沿
        4: body, 3: body, 2: body, 1: body_dim,
        0: [CO_LO, CO, CO, CO, CO_LO, CO_LO],                  # 瓶底也是可乐色（用户 2026-10-26）；空瓶时随可乐一起换成玻璃
    }
    for y, row in rows.items():
        for x, c in enumerate(row):
            px[x, 14 - y] = c
    for y in range(0, 6):
        for x in range(7, 13):
            px[x, y] = CO_HI if (x + y) % 4 == 0 else CO
    for y in range(7, 9):
        for x in range(7, 13):
            px[x, y] = CAP_LO if (x % 2) else CAP
    for y in range(10, 14):
        for x in range(7, 11):
            px[x, y] = CAP_HI if (x + y) % 3 else CAP
    return im


# 尺寸（2026-10-26 用户：尺寸要合理）：真瓶约 20 厘米高、6 厘米粗；橙子饮料的 2 升大瓶 15 像素高 ≈ 30 厘米，
# 按同样比例玻璃汽水瓶 ≈ 11.75 像素高（含瓶盖）、瓶身 4 像素粗、瓶颈 2 像素。和 DrinkKind.bottleHeight 一致。
HEIGHT = 11.375  # 2026-10-26 用户调瓶盖厚度：0.75 → 0.5 →（0.25 太薄）→ 0.375，整瓶 11.75 → 11.375


def model(tex, cap=True):
    els = []
    # 用户 2026-10-26 先后调过两次比例（收腰上移、标签上移、瓶颈变短），看着不像，回退到这一版
    els += ppd.tier(0, 0.75, 3, rows=(0, 1), square=True)            # 厚瓶底，收一点
    els += ppd.tier(0.75, 3.25, 4, rows=(1, 3), square=True)         # 瓶身下段鼓起来
    els += ppd.tier(3.25, 4.25, 3, rows=(3, 4), square=True)         # 收腰（用户：参考图下半截收腰；回退后又上移 1 格）
    els += ppd.tier(4.25, 4.75, 4, rows=(4, 5), square=True)         # 再放宽到标签
    els += ppd.tier(4.75, 7.75, 4, rows=(5, 9), square=True)         # 红标签（用户：往上 0.5 太多，往下 0.2，再往下 0.05）
    els += ppd.tier(7.75, 8.25, 4, rows=(9, 10), square=True)
    els += ppd.tier(8.25, 9.25, 3, rows=(9, 10), square=True)        # 瓶肩
    els += ppd.tier(9.25, 10, 2, rows=(10, 11), square=True)         # 瓶颈：下半截可乐，宽 2（用户：有可乐颜色的这段不改）
    els += ppd.tier(10, 11, 1.5, rows=(11, 13), square=True)         # 上半截空玻璃 + 瓶口，削到 1.5（用户）
    if cap:
        els += ppd.tier(11, HEIGHT, 1.75, "cap", square=True, square_side=1.25)   # 2026-10-26 用户：盖子再小一圈（1.5 → 1.25）        # 皇冠瓶盖：只比 1.5 的瓶口宽一点（用户：瓶盖太大，又说再小一点）
    return {"parent": "minecraft:block/block",
            "textures": {"d": f"{MODID}:block/{tex}", "particle": f"{MODID}:block/{tex}"},
            "elements": els}


def texture_empty(full):
    """喝完的可乐空瓶：可乐换成玻璃，红标签留着，没有瓶盖（模型里去掉）。"""
    im = full.copy()
    px = im.load()
    glass = {CO: GL, CO_HI: GL_HI, CO_LO: GL_LO, (176, 128, 98, 255): (250, 252, 252, 255), (128, 78, 50, 255): GL_HI}
    for y in range(16):
        for x in range(16):
            if px[x, y] in glass:
                px[x, y] = glass[px[x, y]]
    return im


def fizzing(tex):
    """满瓶贴图放大到 32×32（一格 2 个贴图像素），可乐看得见的两段冒气泡（tools/fizz.py；2026-10-26 用户：要有自下而上的气泡）：
    瓶身下段（标签下面）和瓶颈下半截；避开左边的反光亮条和右边的暗边，标签、瓶盖不动。"""
    big = tex.resize((32, 32), Image.NEAREST)
    return fizz.animate(big, [(range(6, 10), 27, 20), (range(6, 10), 11, 8)],
                        bubble=(176, 128, 96, 255), glint=CO_HI, seed=3)


def main():
    tex = texture()
    for root in ASSET_ROOTS:
        sheet, meta = fizzing(tex)
        fizz.save(sheet, meta, root / "textures/block/orient_cola.png")
        texture_empty(tex).save(root / "textures/block/orient_cola_empty.png")
        write(root / "models/block/orient_cola.json", model("orient_cola"), ensure_ascii=True)
        write(root / "models/block/orient_cola_empty.json", model("orient_cola_empty", cap=False), ensure_ascii=True)
    print("ok")


if __name__ == "__main__":
    main()
