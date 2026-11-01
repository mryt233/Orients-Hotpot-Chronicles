"""玻璃杯（2026-10-25 用户：杯子，碰杯先不做）：贴图、模型、方块状态（摆在桌上）、物品模型、配方。

    python tools/gen_cup.py            # 写进两份资源
    python tools/gen_cup.py preview    # 只出预览图 build/cup_preview.png（不改模组），给用户看造型
2026-10-26 用户：重新做造型，分辨率高一些。现在是空心的玻璃杯：
  - 四面杯壁各是 0.5 像素厚的薄片，杯底 0.75 像素厚，杯里一块饮料（顶面低于杯口），斜着往下看得见杯口一圈、杯里的饮料面；
  - 贴图 32×32（一个模型像素 = 2 个贴图像素），杯壁画出左边一道竖的高光、右边暗边、杯口一道白边、
    透过玻璃看到的饮料（液面一道亮线）、加厚的杯底；空杯是淡蓝灰的玻璃。
杯子占 4×4、6 像素高，摆在盘子格中间（模型以 x/z = 8 为中心，和饮料瓶一样由盘子格渲染器挪位置）。
贴图布局（单位 = 模型像素，×2 = 贴图像素）：外壁 0..4 × 0..6，内壁 4..8 × 0..6，饮料面 8..11 × 0..3，
杯底内面 8..11 × 4..7，杯口 14..16 × 0..4，杯底外面 12..16 × 4..8。
作者：mryt233（2026-11-01 mryt233 改：改用 tools/pixkit 公共库，输出逐字节不变）
"""
import os
import subprocess
import sys
from PIL import Image

from pixkit import anim, io, model as pm
from pixkit.color import mix, rgba
from pixkit.paths import MOD, MODID, RES, ROOTS as ASSET_ROOTS

GLASS, GLASS_HI, GLASS_LO = (206, 226, 232, 255), (240, 249, 252, 255), (164, 190, 200, 255)
GLASS_EDGE = (140, 168, 180, 255)
GLASS_IN = (186, 208, 216, 255)
RIM = (250, 254, 255, 255)
# 饮料颜色：基色、亮、暗（和瓶子里的颜色一致）
LIQUID = {
    "orange_drink": ((247, 160, 20, 255), (255, 196, 70, 255), (212, 118, 12, 255)),
    "plant_protein_drink": ((238, 232, 220, 255), (250, 248, 240, 255), (212, 204, 190, 255)),
    "orient_cola": ((78, 40, 24, 255), (112, 62, 36, 255), (52, 26, 16, 255)),
    # 一杯水 / 一杯牛奶（2026-10-27 用户，新增）：水用原版默认水色 #3F76E4，牛奶照原版牛奶桶的白
    "water": ((63, 118, 228, 255), (110, 160, 244, 255), (42, 88, 192, 255)),
    "milk": ((244, 244, 238, 255), (255, 255, 255, 255), (218, 218, 210, 255)),
}
KINDS = list(LIQUID)
LEVEL = 4.5        # 饮料面高度（模型像素，杯高 6）
H = 6


def texture(kind=None):
    # 底色用不透明的玻璃色：贴图里没用到的地方也不留透明，不然带动画的一杯可乐在查看器里会被当成半透明、杯壁看穿（用户报，调整）
    im = Image.new("RGBA", (32, 32), GLASS)
    px = im.load()
    liq = LIQUID.get(kind)
    # 外壁：8 × 12 像素。第 0 行杯口白边；左 1~2 列一道竖高光，右边两列暗；饮料那段是玻璃透出的饮料色，液面一道亮线；
    # 最下面 2 行是加厚的杯底（玻璃更厚、颜色更深）
    level_row = round((H - LEVEL) * 2)          # 液面在第几行
    for y in range(12):
        for x in range(8):
            base = GLASS_HI if x in (1, 2) else GLASS_EDGE if x == 7 else GLASS_LO if x == 6 else GLASS
            if liq and level_row <= y < 10:
                c = mix(liq[0], base, 0.28 if x not in (1, 2) else 0.5)
                if y == level_row:
                    c = mix(liq[1], RIM, 0.35)
                elif x == 7:
                    c = mix(liq[2], GLASS_EDGE, 0.3)
            elif y >= 10:
                c = mix(base, GLASS_EDGE, 0.45) if not liq else mix(mix(liq[2], base, 0.4), GLASS_EDGE, 0.3)
            else:
                c = base
            if y == 0:
                c = RIM
            px[x, y] = rgba(c)
    # 内壁：8 × 12，比外壁暗一点，没有高光
    for y in range(12):
        for x in range(8, 16):
            px[x, y] = RIM if y == 0 else GLASS_IN
    # 饮料面：6 × 6，中间一点反光；空杯不用
    for y in range(6):
        for x in range(16, 22):
            if liq:
                px[x, y] = liq[1] if (x, y) in ((17, 1), (18, 1), (17, 2)) else (liq[2] if x == 21 or y == 5 else liq[0])
    # 杯底内面（空杯从上面看到的）：6 × 6
    for y in range(8, 14):
        for x in range(16, 22):
            px[x, y] = GLASS_HI if (x, y) in ((17, 9), (18, 9)) else GLASS_IN
    # 杯口：4 × 8 白
    for y in range(8):
        for x in range(28, 32):
            px[x, y] = RIM
    # 杯底外面：8 × 8
    for y in range(8, 16):
        for x in range(24, 32):
            px[x, y] = GLASS_LO
    return im


def model(tex, kind=None):
    t = "#d"
    wall_out = [0, 0, 4, H]
    wall_in = [4, 0, 8, H]
    rim_top = [14, 0, 16, 4]
    bottom = [12, 4, 16, 8]
    edge = [3.5, 0, 4, H]          # 杯壁两头的窄面：用外壁最右那条暗边

    def wall(frm, to, outer):
        # 六个面都要有：薄壁两头的窄面（0.5 宽）在杯角露得出来，以前没画，进游戏杯角漏了缝（用户截图，补充）
        inner = {"north": "south", "south": "north", "east": "west", "west": "east"}[outer]
        f = {d: {"uv": edge, "texture": t} for d in ("north", "south", "east", "west")}
        f.update({"up": {"uv": rim_top, "texture": t}, "down": {"uv": bottom, "texture": t},
                  outer: {"uv": wall_out, "texture": t}, inner: {"uv": wall_in, "texture": t}})
        return {"from": frm, "to": to, "faces": f}

    els = [
        # 四面杯壁（南北两片满宽，东西两片夹在中间）
        wall([6, 0, 6], [10, H, 6.5], "north"),
        wall([6, 0, 9.5], [10, H, 10], "south"),
        wall([6, 0, 6.5], [6.5, H, 9.5], "west"),
        wall([9.5, 0, 6.5], [10, H, 9.5], "east"),
        # 杯底（外面朝下，里面朝上看得见：空杯是玻璃，满杯被饮料盖住）
        {"from": [6.5, 0, 6.5], "to": [9.5, 0.75, 9.5],
         "faces": {"up": {"uv": [8, 4, 11, 7], "texture": t}, "down": {"uv": bottom, "texture": t}}},
    ]
    if kind:
        els.append({"from": [6.5, 0.75, 6.5], "to": [9.5, LEVEL, 9.5],
                    "faces": {"up": {"uv": [8, 0, 11, 3], "texture": t}}})
    return pm.block_model(els, {"d": f"{MODID}:block/{tex}"})


# 杯子只有 6 高，物品栏里往上挪 6 才在格子正中（用户：物品栏里偏了）
CUP_DISPLAY = pm.small_block_display(H)
write = io.write_json


def assets(root):
    io.save_png(texture(), root / "textures/block/cup.png")
    write(root / "models/block/cup.json", model("cup"))
    write(root / "models/item/glass_cup.json", {"parent": f"{MODID}:block/cup", "display": CUP_DISPLAY})
    for kind in KINDS:
        if kind == "orient_cola":
            # 一杯可乐冒气泡（2026-10-26 用户）：杯壁透出的可乐那段往上冒，液面上一闪一闪的小泡（tools/fizz.py）
            lo = round((H - LEVEL) * 2)
            sheet, meta = anim.fizz_animate(texture(kind), [((3, 4, 5), 9, lo + 1), ((17, 18, 19, 20), 4, 1)],
                                       bubble=(212, 182, 152, 255), glint=(150, 104, 76, 255), seed=5)
            anim.save(sheet, meta, root / f"textures/block/cup_{kind}.png")
        else:
            io.save_png(texture(kind), root / f"textures/block/cup_{kind}.png")
        # 一杯可乐以前在这里 continue 掉了，模型没跟着重写，进游戏还是旧模型（杯角漏缝、物品栏偏下；用户报，调整）
        write(root / f"models/block/cup_{kind}.json", model(f"cup_{kind}", kind))
        write(root / f"models/item/{kind}_cup.json", {"parent": f"{MODID}:block/cup_{kind}", "display": CUP_DISPLAY})


def main():
    if "preview" in sys.argv:
        root = MOD / "build/preview_assets" / MODID
        assets(root)
        env = dict(os.environ, PREVIEW_ROOT=str(root))
        refs = [f"{MODID}:block/cup"] + [f"{MODID}:block/cup_{k}" for k in KINDS]
        subprocess.run([sys.executable, str(MOD / "tools/render_preview.py"), str(MOD / "build/cup_preview.png")] + refs,
                       env=env, check=True)
        return
    for root in ASSET_ROOTS:
        assets(root)
        # 方块状态 cup：axis × empty × kind（empty=true 时 kind 不起作用，都是空杯）
        variants = {}
        for axis in ("x", "z"):
            for kind in KINDS:
                for empty in ("false", "true"):
                    v = {"model": f"{MODID}:block/cup" + ("" if empty == "true" else f"_{kind}")}
                    if axis == "z":
                        v["y"] = 90
                    variants[f"axis={axis},empty={empty},kind={kind}"] = v
        write(root / "blockstates/cup.json", {"variants": variants})
    # 配方：玻璃摆成杯子样（两边 + 底），出 4 个
    write(RES / "data" / MODID / "recipe/glass_cup.json",
          {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["G G", "GGG"],
           "key": {"G": {"item": "minecraft:glass"}}, "result": {"id": f"{MODID}:glass_cup", "count": 4}})
    print("ok")


if __name__ == "__main__":
    main()
