"""玻璃杯（2026-10-25 用户：杯子，碰杯先不做）：贴图、模型、方块状态（摆在桌上）、物品模型、配方。

    python tools/gen_cup.py            # 写进两份资源
    python tools/gen_cup.py preview    # 只出预览图 build/cup_preview.png（不改模组），给用户看造型
2026-10-26 用户：重新做造型，分辨率高一些。现在是空心的玻璃杯：
  - 四面杯壁各是薄片，杯底加厚，杯里一块饮料（顶面略低于杯口），斜着往下看得见杯口一圈、杯里的饮料面；
  - 贴图 32×32（一个模型像素 = 2 个贴图像素），杯壁画出左上窗口式柔和高光、冰晶折射厚底、
    透过玻璃看到的鲜亮饮料（液面一道亮线）；空杯是清透淡冰蓝玻璃。
2026-11-06 mryt233 改（用户选定 4 号方案）：
  - 改为圆润八角削角杯造型（0.25 纤薄杯壁 + 四角斜向过渡柱，无死角漏缝）；
  - 去掉旧版粗白框、竖条纹与饮品上沿灰暗宽带，液面提高至 5.35，采用窗口式柔光与冰晶折射厚底。
杯子占 4×4、6 像素高，摆在盘子格中间（模型以 x/z = 8 为中心，和饮料瓶一样由盘子格渲染器挪位置）。
贴图布局（单位 = 模型像素，×2 = 贴图像素）：外壁 0..4 × 0..6，内壁 4..8 × 0..6，饮料面 8..11 × 0..3，
杯底内面 8..11 × 4..7，杯口 14..16 × 0..4，杯底外面 12..16 × 4..8。
作者：mryt233（2026-11-01 mryt233 改：改用 tools/pixkit 公共库；2026-11-06 mryt233 改：4 号圆润八角削角杯重绘）
"""
import os
import subprocess
import sys
from PIL import Image

# mryt233 改（2026-11-10）：材质名单与输出目录统一。
from pixkit.materials import materials, tuples

from pixkit import anim, io, model as pm
from pixkit.color import lerp, rgba
from pixkit.paths import MOD, MODID, RES, ROOTS as ASSET_ROOTS

G_TOP = (210, 224, 230, 255)
G_BOT = (176, 198, 208, 255)
G_HI = (244, 252, 255, 255)
G_LO = (144, 168, 180, 255)
G_IN = (182, 202, 212, 255)
RIM_MAIN = (196, 216, 226, 255)
RIM_HI = (232, 246, 252, 255)

# 饮料颜色：基色、亮、暗（通透鲜亮）
LIQUID = {e["id"]: tuples(e["cup_colors"]) for e in materials()["drinks"]}
KINDS = list(LIQUID)
LEVEL = 5.35       # 饮料面高度（模型像素，杯高 6）
WALL_T = 0.25      # 纤薄杯壁厚度
BOTTOM_H = 0.85    # 冰晶厚底高度
H = 6


def texture(kind=None):
    # 底色用不透明的玻璃色：贴图里没用到的地方也不留透明，不然带动画的一杯可乐在查看器里会被当成半透明、杯壁看穿
    im = Image.new("RGBA", (32, 32), G_TOP)
    px = im.load()
    liq = LIQUID.get(kind)

    # 窗口式高光：左列 y=2..6 一道柔亮条 + 次列 y=2..4 短伴随反光
    hi_strong = {(1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (2, 2)}
    hi_soft = {(1, 7), (2, 3), (2, 4)}

    for y in range(12):
        grad_g = lerp(G_TOP, G_BOT, y / 11.0)
        for x in range(8):
            is_hi = (x, y) in hi_strong
            is_soft = (x, y) in hi_soft
            if y == 0:
                if liq:
                    c = lerp(RIM_MAIN, liq[1], 0.25)
                else:
                    c = RIM_HI if x in (1, 2) else (G_LO if x in (0, 7) else RIM_MAIN)
            elif y >= 10:
                # 冰晶厚底：第 10 行中部折射亮垫，第 11 行深色底沿
                if y == 10 and x in (1, 2, 3, 4, 5, 6):
                    c = lerp(G_HI, liq[1], 0.35) if liq else lerp(G_TOP, G_HI, 0.55)
                else:
                    c = lerp(G_LO, liq[2], 0.35) if liq else G_LO
            elif liq and y >= 1:
                if y == 1:
                    c = lerp(liq[1], G_HI, 0.45 if x in (1, 2, 3) else 0.22)
                else:
                    c = lerp(liq[0], liq[2], (y - 1) / 9.0 * 0.35)
                    if is_hi:
                        c = lerp(liq[1], G_HI, 0.48)
                    elif is_soft:
                        c = lerp(c, liq[1], 0.55)
                    elif x in (0, 7):
                        c = lerp(c, liq[2], 0.45)
            else:
                c = grad_g
                if is_hi:
                    c = G_HI
                elif is_soft:
                    c = lerp(grad_g, G_HI, 0.55)
                elif x in (0, 7):
                    c = lerp(grad_g, G_LO, 0.65)
            px[x, y] = rgba(c)

    # 内壁：8 × 12
    for y in range(12):
        for x in range(8, 16):
            if y == 0:
                px[x, y] = RIM_MAIN
            elif liq and y >= 1:
                px[x, y] = lerp(liq[0], G_IN, 0.35)
            else:
                px[x, y] = lerp(G_IN, G_LO, y / 14.0)

    # 饮料面：6 × 6，左上柔光，四边微深
    for y in range(6):
        for x in range(16, 22):
            if liq:
                u, v = x - 16, y
                if (u, v) in ((1, 1), (2, 1), (1, 2)):
                    px[x, y] = liq[1]
                elif (u, v) in ((2, 2), (3, 1), (1, 3)):
                    px[x, y] = lerp(liq[0], liq[1], 0.55)
                elif u in (0, 5) or v in (0, 5):
                    px[x, y] = lerp(liq[0], liq[2], 0.45)
                else:
                    px[x, y] = liq[0]

    # 杯底内面（空杯从上面看到的）：6 × 6
    for y in range(8, 14):
        for x in range(16, 22):
            u, v = x - 16, y - 8
            if (u, v) in ((1, 1), (2, 1), (1, 2)):
                px[x, y] = G_HI
            elif u in (0, 5) or v in (0, 5):
                px[x, y] = G_LO
            else:
                px[x, y] = G_IN

    # 杯口顶面：4 × 8 柔和冰蓝玻璃边 + 一点高光
    for y in range(8):
        for x in range(28, 32):
            if x == 29 and y in (1, 2):
                px[x, y] = RIM_HI
            elif x in (28, 31) or y in (0, 7):
                px[x, y] = lerp(RIM_MAIN, G_LO, 0.3)
            else:
                px[x, y] = RIM_MAIN

    # 杯底外面：8 × 8
    for y in range(8, 16):
        for x in range(24, 32):
            px[x, y] = G_LO
    return im


def model(tex, kind=None):
    t = "#d"
    wall_out = [0, 0, 4, H]
    wall_in = [4, 0, 8, H]
    rim_top = [14, 0, 16, 4]
    bottom = [12, 4, 16, 8]
    edge = [3.5, 0, 4, H]

    def wall(frm, to, outer):
        inner = {"north": "south", "south": "north", "east": "west", "west": "east"}[outer]
        f = {d: {"uv": edge, "texture": t} for d in ("north", "south", "east", "west")}
        f.update({
            "up": {"uv": rim_top, "texture": t},
            "down": {"uv": bottom, "texture": t},
            outer: {"uv": wall_out, "texture": t},
            inner: {"uv": wall_in, "texture": t},
        })
        return {"from": frm, "to": to, "faces": f}

    x0, x1 = 6.0, 10.0
    z0, z1 = 6.0, 10.0
    c = 0.45

    els = [
        # 四面削角主杯壁（0.25 纤薄壁厚，两端各收 0.45 形成八边形圆杯）
        wall([x0 + c, 0, z0], [x1 - c, H, z0 + WALL_T], "north"),
        wall([x0 + c, 0, z1 - WALL_T], [x1 - c, H, z1], "south"),
        wall([x0, 0, z0 + c], [x0 + WALL_T, H, z1 - c], "west"),
        wall([x1 - WALL_T, 0, z0 + c], [x1, H, z1 - c], "east"),
        # 四个斜角过渡柱（严丝合缝连接四面主壁，高度微收 0.01 防顶面重叠闪烁）
        wall([x0 + 0.2, 0, z0 + 0.2], [x0 + c + 0.1, H - 0.01, z0 + c + 0.1], "north"),
        wall([x1 - c - 0.1, 0, z0 + 0.2], [x1 - 0.2, H - 0.01, z0 + c + 0.1], "east"),
        wall([x0 + 0.2, 0, z1 - c - 0.1], [x0 + c + 0.1, H - 0.01, z1 - 0.2], "west"),
        wall([x1 - c - 0.1, 0, z1 - c - 0.1], [x1 - 0.2, H - 0.01, z1 - 0.2], "south"),
        # 杯底（外面朝下，里面朝上看得见：空杯是玻璃，满杯被饮料盖住）
        {"from": [x0 + WALL_T, 0, z0 + WALL_T], "to": [x1 - WALL_T, BOTTOM_H, z1 - WALL_T],
         "faces": {"up": {"uv": [8, 4, 11, 7], "texture": t}, "down": {"uv": bottom, "texture": t}}},
    ]
    if kind:
        els.append({
            "from": [x0 + WALL_T, BOTTOM_H, z0 + WALL_T],
            "to": [x1 - WALL_T, LEVEL, z1 - WALL_T],
            "faces": {"up": {"uv": [8, 0, 11, 3], "texture": t}},
        })
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
            sheet, meta = anim.fizz_animate(texture(kind), [((3, 4, 5), 9, 2), ((17, 18, 19, 20), 4, 1)],
                                            bubble=(212, 182, 152, 255), glint=(150, 104, 76, 255), seed=5)
            anim.save(sheet, meta, root / f"textures/block/cup_{kind}.png")
        else:
            io.save_png(texture(kind), root / f"textures/block/cup_{kind}.png")
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
