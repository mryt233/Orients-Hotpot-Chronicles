"""切菜（2026-10-23 用户定，预览阶段）：砧板（只做一种）+ 铁菜刀；生牛肉 / 生羊肉在砧板上用菜刀点一下切成 3 卷（切石机也能切），
每卷煮熟回 3 饥饿值。这里画贴图和模型：

  肥牛卷 fatty_beef_roll / 熟肥牛卷 cooked_fatty_beef_roll
  羊肉卷 mutton_roll / 熟羊肉卷 cooked_mutton_roll
  铁菜刀 iron_kitchen_knife
  砧板 cutting_board（方块：圆木墩，直径 12、高 3 像素；顶面年轮，侧面带一圈树皮）

    python tools/gen_cutting.py
作者：
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, k):
    return clamp(tuple(v * k for v in c))


# ---------------------------------------------------------------- 肉卷
# 照用户发的肥牛卷照片（2026-10-23）：卷子横躺，左端是开口的截面——一圈卷起来的肉片（切口边发白），
# 中间一个看得见的空洞；卷身鲜红，满是细碎的白色雪花纹（不是一条条的白杠）。
MEATS = {
    # 名字: (瘦肉色, 肥肉 / 雪花色, 雪花多少 0..1, 空洞色)
    "fatty_beef_roll": ((206, 32, 52), (250, 228, 232), 0.42, (96, 8, 24)),
    "cooked_fatty_beef_roll": ((150, 100, 88), (226, 206, 186), 0.42, (70, 44, 36)),
    "mutton_roll": ((214, 86, 98), (252, 238, 236), 0.55, (110, 30, 40)),
    "cooked_mutton_roll": ((172, 122, 96), (234, 216, 192), 0.55, (82, 54, 40)),
}
# 用户又要 45 度斜放（同原版物品的斜向）：开口截面在左下，卷身往右上伸。
# 坐标换到卷子上：u 沿轴线（从截面中心往右上），v 垂直轴线（正向 = 右下 = 背光一侧）
CAP = (4.7, 11.3)         # 截面中心
LENGTH = 8.6              # 截面中心到右上圆头
R = 3.6                   # 半径
FLAP = 3.6                # 没卷进去的那截肉片（用户：再加长一点）：从卷身右下侧平摊出来的宽度（用户：长边留长一点更像）
CAP_RX = 2.7              # 截面椭圆沿轴线方向的半径（斜着看压扁）
UX, UY = math.sqrt(0.5), -math.sqrt(0.5)
NX, NY = -UY, UX


def to_roll(x, y):
    cx, cy = x + 0.5 - CAP[0], y + 0.5 - CAP[1]
    return cx * UX + cy * UY, cx * NX + cy * NY


def roll_texture(lean, fat, marble, hole, seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    noise = {(x, y): rnd.random() for y in range(16) for x in range(16)}
    cap = set()
    for y in range(16):
        for x in range(16):
            u, v = to_roll(x, y)
            dv = v / R
            if dv > 1 and v <= R + FLAP and 0.4 + (v - R) * 0.35 <= u <= LENGTH + 0.6 - (v - R) * 0.2:
                # 平摊出来的肉片：比卷身暗一点（贴着地、在卷子影子里），最外沿一道白色切口边
                if v > R + FLAP - 1:
                    c = shade(fat, 0.92)
                else:
                    c = fat if noise[(x, y)] < marble * 0.45 else lean
                    c = shade(c, 0.96)
                px[x, y] = c + (255,)
                continue
            if abs(dv) > 1:
                continue
            half = math.sqrt(1 - dv * dv)
            r_cap = math.hypot(u / CAP_RX, dv)
            if r_cap <= 1:
                cap.add((x, y))
                # 截面：外圈卷起来的肉片切口（白边 + 一圈红 + 里层白边），中间空洞
                if r_cap > 0.8:
                    c = shade(fat, 0.97)
                elif r_cap > 0.6:
                    c = shade(lean, 1.0)
                elif r_cap > 0.45:
                    c = shade(fat, 0.9)
                else:
                    c = hole if dv > -0.2 else shade(hole, 1.35)
                px[x, y] = c + (255,)
            elif 0 <= u <= LENGTH + 1.4 * half:
                # 卷身：鲜红 + 细碎的白色雪花纹
                vein = math.sin(x * 1.7 + y * 2.3 + seed) * math.sin(x * 0.9 - y * 1.6)
                c = fat if (noise[(x, y)] < marble * 0.5 or vein > 0.93 - marble * 0.1) else lean
                k = 1.15 - 0.35 * (dv + 1) / 2 + (0.08 if dv < -0.75 else 0)
                if dv > 0.72 and u > 0.6:
                    c, k = lean, 0.5                 # 卷身底下一道暗线：和摊开的那片分开
                px[x, y] = shade(c, k) + (255,)
    # 描边：卷身外缘深色；截面那一圈白边不描
    edge = shade(lean, 0.5)
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for (x, y) in solid:
        if (x, y) not in cap and any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = edge + (255,)
    return im


# ---------------------------------------------------------------- 牛脊肉 / 吊龙（用户 2026-10-23：加吊龙，牛掉一块牛脊肉来切）
# 吊龙 = 潮汕牛肉火锅的牛背脊长条肉，现切鲜肉片，平铺不卷，瘦肉为主、边上一条白油花。
def body(x, y, w=7.4, h=5.0, cy=9.0):
    """鼓起来的肉块轮廓：底宽上圆，边缘微微起伏。"""
    dx, dy = (x + 0.5 - 8) / w, (y + 0.5 - cy) / h
    if dy > 0:
        dy *= 0.75
    return math.hypot(dx, dy) + 0.06 * math.sin(x * 1.7 + y * 0.9)


RAW_LOIN = {"MEAT": (168, 58, 72), "MEAT_D": (132, 38, 54), "MEAT_L": (192, 86, 98), "CUT": (182, 44, 58),
            "MARBLE": (236, 214, 214), "FILM": (206, 210, 226), "FILM_L": (230, 232, 240),
            "FAT": (240, 222, 188), "FAT_D": (220, 196, 156), "EDGE": (90, 22, 36)}
# 熟牛脊肉（用户要，新增）：同一张图换成烤熟的颜色，配色向原版熟牛排靠
COOKED_LOIN = {"MEAT": (126, 70, 46), "MEAT_D": (96, 50, 32), "MEAT_L": (150, 92, 62), "CUT": (140, 84, 60),
               "MARBLE": (206, 172, 136), "FILM": (168, 128, 94), "FILM_L": (190, 152, 114),
               "FAT": (232, 204, 150), "FAT_D": (208, 174, 120), "EDGE": (70, 38, 24)}


def loin_texture(pal=RAW_LOIN):
    """牛脊肉（用户定 D，照用户发的牛脊肉照片）：扁宽的一大块暗紫红肉。左前一个斜切面（深红 + 白色雪花线，
    和表面之间一道深色分界），表面一道道斜的白蓝筋膜，右边和上沿几团米黄油脂。pal 换颜色（生 / 熟）。"""
    MEAT, MEAT_D, MEAT_L, CUT, MARBLE = pal["MEAT"], pal["MEAT_D"], pal["MEAT_L"], pal["CUT"], pal["MARBLE"]
    FILM, FILM_L, FAT, FAT_D, EDGE = pal["FILM"], pal["FILM_L"], pal["FAT"], pal["FAT_D"], pal["EDGE"]
    rnd = random.Random(11)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(16):
            if body(x, y) > 1:
                continue
            k = x + 1.4 * (15 - y)
            if k < 11.5:                              # 左前切面
                c = MARBLE if (x * 2 + y * 3) % 7 == 0 or (y - x) % 6 == 0 and rnd.random() < 0.6 else CUT
            elif k < 12.6:
                c = EDGE                              # 切面和表面之间的分界
            elif (x - y) % 5 in (0, 1) and x > 5:
                c = FILM_L if (x - y) % 5 == 0 else FILM
            else:
                c = MEAT_L if rnd.random() < 0.1 else (MEAT_D if y > 12 else MEAT)
            for fx, fy in [(12, 6), (13, 9), (9, 4), (12, 11)]:   # 成团的油脂
                if abs(x - fx) + abs(y - fy) <= 1 and k >= 12.6:
                    c = FAT if (x + y) % 2 else FAT_D
            px[x, y] = c + (255,)
    outline(im, EDGE)
    # 肉块画在 cy=9 偏下（上空 4、下贴底），整张往上挪 2 像素居中（用户报：物品栏里不在正中间）
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out.alpha_composite(im, (0, -2))
    return out


def slice_texture(lean, lean_dark, lean_light, fat, outline_c):
    """一片吊龙（用户定 C）：软软的、呈波浪形塌下来的薄肉片，一侧（上沿）一条油边，肉上几点浅色。"""
    rnd = random.Random(3)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for x in range(1, 15):
        cy = 8 + 2.4 * math.sin((x - 1) * 0.55)
        for y in range(16):
            d = y - cy
            if -3.5 <= d <= 2.5:
                c = fat if d < -1.0 else (lean_light if rnd.random() < 0.07 else lean_dark if d > 1.3 else lean)
                px[x, y] = c + (255,)
    outline(im, outline_c)
    return im


def outline(im, color):
    px = im.load()
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = color + (255,)


# ---------------------------------------------------------------- 铁菜刀
# 用户 2026-10-23：长方形中式菜刀，柄和刀背在一条线上、从刀身后端直着伸出去，整把刀斜 45 度
# （柄在左下、刀头在右上，同原版工具）。按刀的坐标画：u 沿刀（柄尾 → 刀头），v 垂直（0 = 刀背线，正向 = 刃口一侧）。
KNIFE_ORIGIN = (1.6, 14.4)
BLADE_U, BLADE_V = (5.6, 14.8), (-5.2, 0.6)    # 刀身（用户：拿在手里方向反了 → 刃口改到左上一侧，同原版斧头的刃朝向）
HANDLE_U, HANDLE_V = (-1.4, 5.0), (-1.3, 0.6)   # 木柄（贴着刀背那一侧；用户：再长一个像素）


def knife_texture():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    s2 = math.sqrt(0.5)
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5 - KNIFE_ORIGIN[0], y + 0.5 - KNIFE_ORIGIN[1]
            u = (cx - cy) * s2
            v = (cx + cy) * s2
            c = None
            if BLADE_U[0] <= u <= BLADE_U[1] and BLADE_V[0] <= v <= BLADE_V[1]:
                if v > -0.4:
                    c = (132, 136, 144)                    # 刀背（贴着柄那条线）
                elif v < BLADE_V[0] + 1.1:
                    c = (238, 242, 246)                    # 刃口亮边
                else:
                    c = (214, 218, 224) if abs(u + v - 6.5) < 0.9 else (190, 194, 200)   # 一道斜反光
            elif HANDLE_U[1] <= u < BLADE_U[0] and -1.6 <= v <= 0.9:
                c = (120, 122, 128)                        # 刀箍
            elif HANDLE_U[0] <= u < HANDLE_U[1] and HANDLE_V[0] <= v <= HANDLE_V[1]:
                c = (124, 84, 46) if v < -0.5 else (92, 60, 32)   # 木柄（上亮下暗）
            if c:
                px[x, y] = c + (255,)
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            r, g, b, _ = px[x, y]
            px[x, y] = shade((r, g, b), 0.45) + (255,)
    # 挂孔：刀头靠刀背处只点一个深色像素（用户：那里只留一个点，像中式菜刀的洞）
    hu, hv = BLADE_U[1] - 2.4, -1.6
    hx = int(KNIFE_ORIGIN[0] + (hu + hv) * s2)
    hy = int(KNIFE_ORIGIN[1] + (hv - hu) * s2)
    px[hx, hy] = (70, 72, 78, 255)
    return centered(im)


def centered(im):
    """整把刀挪到格子正中（用户：物品栏里没在正中心）。"""
    box = im.getbbox()
    w, h = box[2] - box[0], box[3] - box[1]
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out.paste(im.crop(box), ((16 - w) // 2, (16 - h) // 2))
    return out


# ---------------------------------------------------------------- 砧板
def board_top():
    """顶面：横切的木墩，一圈圈年轮，外圈一道树皮（圆以外的像素模型里看不到）。"""
    rnd = random.Random(5)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 8) + rnd.uniform(-0.25, 0.25)
            if r > 6.3:
                c = (92, 64, 38)                                       # 树皮
            elif int(r * 1.15) % 2:
                c = (196, 156, 104)
            else:
                c = (178, 136, 86)
            if r < 1.0:
                c = (160, 118, 72)                                     # 树心
            px[x, y] = c + (255,)
    for x, y in [(9, 6), (10, 7), (6, 10)]:                            # 刀痕
        px[x, y] = (150, 112, 70, 255)
    return im


def board_side():
    """侧面：深色树皮，上下各一道浅色切面边。"""
    rnd = random.Random(6)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            c = rnd.choice([(92, 64, 38), (84, 58, 34), (102, 72, 44)])
            if x % 4 == 1:
                c = (74, 50, 28)
            px[x, y] = c + (255,)
    return im


def board_model():
    """圆木墩：三个交叉的长方块拼成近似的圆（直径 12），高 3 像素。"""
    h = 3
    els = []
    for f, t in [([2, 0, 4], [14, h, 12]), ([4, 0, 2], [12, h, 14]), ([3, 0, 3], [13, h, 13])]:
        faces = {d: {"texture": "#side", "uv": [f[0], 16 - h, t[0], 16] if d in ("north", "south") else [f[2], 16 - h, t[2], 16]}
                 for d in ("north", "south", "east", "west")}
        faces["up"] = {"texture": "#top", "uv": [f[0], f[2], t[0], t[2]]}
        faces["down"] = {"texture": "#side", "uv": [f[0], f[2], t[0], t[2]]}
        els.append({"from": f, "to": t, "faces": faces})
    return {"parent": "minecraft:block/block",
            "textures": {"top": f"{MODID}:block/cutting_board_top", "side": f"{MODID}:block/cutting_board_side",
                         "particle": f"{MODID}:block/cutting_board_top"},
            "elements": els}


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def main():
    for root in ROOTS:
        tex = root / "textures"
        (tex / "item").mkdir(parents=True, exist_ok=True)
        (tex / "block").mkdir(parents=True, exist_ok=True)
        for i, (name, (lean, fat, marble, hole)) in enumerate(MEATS.items()):
            roll_texture(lean, fat, marble, hole, i // 2).save(tex / f"item/{name}.png")
            write(root / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{name}"}})
        loin_texture().save(tex / "item/beef_loin.png")
        loin_texture(COOKED_LOIN).save(tex / "item/cooked_beef_loin.png")
        slice_texture((200, 40, 50), (160, 24, 36), (226, 92, 96), (246, 234, 218), (104, 16, 24)).save(tex / "item/beef_loin_slice.png")
        slice_texture((156, 108, 92), (130, 86, 72), (180, 132, 112), (232, 214, 194), (80, 52, 42)).save(tex / "item/cooked_beef_loin_slice.png")
        for name in ("beef_loin", "cooked_beef_loin", "beef_loin_slice", "cooked_beef_loin_slice"):
            write(root / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{name}"}})
        knife_texture().save(tex / "item/iron_kitchen_knife.png")
        # 物品栏里（gui）沿对角线翻一面，看着和最早那版一样（刃口在右下）；拿在手里仍是改过的方向（用户 2026-10-23）
        write(root / "models/item/iron_kitchen_knife.json",
              {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/iron_kitchen_knife"},
               "display": {"gui": {"rotation": [180, 0, -90], "translation": [0, 0, 0], "scale": [1, 1, 1]}}})
        board_top().save(tex / "block/cutting_board_top.png")
        board_side().save(tex / "block/cutting_board_side.png")
        write(root / "models/block/cutting_board.json", board_model())
        # 物品栏图标：俯视（用户：斜着看太小）；拿在手里照原版方块。
        # gui_light = front（调整，用户报：物品栏里的砧板比放下的暗、颜色不一样）：方块模型默认按「斜着看的方块」打光，
        # 正对屏幕的板面只吃到一半的光，看着发褐；front 是原版平面物品的打光，板面按贴图原色显示
        write(root / "models/item/cutting_board.json", {"parent": f"{MODID}:block/cutting_board", "gui_light": "front", "display": {
            "gui": {"rotation": [90, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
            "fixed": {"rotation": [90, 0, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            # 0.1.2-alpha 后用户：第一人称看不见，要和放下后一样大 → 1 倍，抬高、微微向自己倾斜露出板面
            "firstperson_righthand": {"rotation": [15, 45, 0], "translation": [0, 5, 0], "scale": [1.0, 1.0, 1.0]},
            "firstperson_lefthand": {"rotation": [15, 225, 0], "translation": [0, 5, 0], "scale": [1.0, 1.0, 1.0]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]}}})
        # 有朝向（用户 2026-10-23）：facing = 放的人站的那一边，模型（刀痕）跟着转
        write(root / "blockstates/cutting_board.json", {"variants": {
            f"facing={f}": {"model": f"{MODID}:block/cutting_board", **({"y": y} if y else {})}
            for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270))}})
    print("ok")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- 进游戏的数据（2026-10-23 用户说「可以，合理了」后加）
DATA = RES / "data"


def data_files():
    rec = DATA / MODID / "recipe"
    rec.mkdir(parents=True, exist_ok=True)
    for raw, roll in [("beef", "fatty_beef_roll"), ("mutton", "mutton_roll")]:
        # 切石机：1 块生肉 → 3 卷（砧板也照这条配方切）
        write(rec / f"{roll}_from_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"minecraft:{raw}"},
                                                      "result": {"id": f"{MODID}:{roll}", "count": 3}})
        # 熟肉卷：熔炉 100 tick（薄片熟得快，锅里也按这个算）、烟熏炉 50、营火 300
        for kind, time, suffix in [("smelting", 100, ""), ("smoking", 50, "_from_smoking"), ("campfire_cooking", 300, "_from_campfire_cooking")]:
            write(rec / f"cooked_{roll}{suffix}.json", {"type": f"minecraft:{kind}", "category": "food", "ingredient": {"item": f"{MODID}:{roll}"},
                                                         "result": {"id": f"{MODID}:cooked_{roll}"}, "experience": 0.35, "cookingtime": time})
    # 牛脊肉 → 3 片吊龙（砧板也照这条切）；熟吊龙熔炉 50 / 烟熏 25 / 营火 150（涮得最快，肥牛卷的一半）
    write(rec / "beef_loin_slice_from_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"{MODID}:beef_loin"},
                                                          "result": {"id": f"{MODID}:beef_loin_slice", "count": 3}})
    for kind, time, suffix in [("smelting", 50, ""), ("smoking", 25, "_from_smoking"), ("campfire_cooking", 150, "_from_campfire_cooking")]:
        write(rec / f"cooked_beef_loin_slice{suffix}.json", {"type": f"minecraft:{kind}", "category": "food",
              "ingredient": {"item": f"{MODID}:beef_loin_slice"}, "result": {"id": f"{MODID}:cooked_beef_loin_slice"},
              "experience": 0.35, "cookingtime": time})
        # 整块牛脊肉直接烤 = 熟牛脊肉（用户要；时间同生牛肉）
        (rec / f"cooked_beef_from_beef_loin{suffix}.json").unlink(missing_ok=True)
        write(rec / f"cooked_beef_loin{suffix}.json", {"type": f"minecraft:{kind}", "category": "food",
              "ingredient": {"item": f"{MODID}:beef_loin"}, "result": {"id": f"{MODID}:cooked_beef_loin"},
              "experience": 0.35, "cookingtime": time * 4})
    write(rec / "iron_kitchen_knife.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["II", "S "],
                                             "key": {"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "minecraft:stick"}},
                                             "result": {"id": f"{MODID}:iron_kitchen_knife", "count": 1}})
    # 两个原木横排（调整 r117：原来单个原木无序合成，和原版「原木 → 木板」冲突）
    write(rec / "cutting_board.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["LL"],
                                        "key": {"L": {"tag": "minecraft:logs"}},
                                        "result": {"id": f"{MODID}:cutting_board", "count": 2}})
    # 能放上砧板切的东西（照切石机配方切出什么）
    f = DATA / MODID / "tags/item/cuttable.json"
    tag = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"replace": False, "values": []}
    # 合并写、不覆盖：土豆 / 豆腐由 gen_ingredients.py 加。r106～r110 这里整份覆盖，跑完把土豆、豆腐挤掉了，
    # 砧板切不了土豆片、豆腐块（用户报，调整）；这里也列上，两个脚本谁后跑都齐全
    for v in ["minecraft:beef", "minecraft:mutton", "minecraft:potato", f"{MODID}:tofu", f"{MODID}:beef_loin"]:
        if v not in tag["values"]:
            tag["values"].append(v)
    write(f, tag)
    write(DATA / MODID / "loot_table/blocks/cutting_board.json",
          {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                                                 "entries": [{"type": "minecraft:item", "name": f"{MODID}:cutting_board"}]}]})
    f = DATA / "minecraft/tags/block/mineable/axe.json"
    tag = json.loads(f.read_text(encoding="utf-8"))
    if f"{MODID}:cutting_board" not in tag["values"]:
        tag["values"].append(f"{MODID}:cutting_board")
    write(f, tag)
    # 肉卷算肉（狼能吃、原版「肉」相关的判断都认）
    f = DATA / "minecraft/tags/item/meat.json"
    tag = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"replace": False, "values": []}
    for i in ["fatty_beef_roll", "cooked_fatty_beef_roll", "mutton_roll", "cooked_mutton_roll",
              "beef_loin", "cooked_beef_loin", "beef_loin_slice", "cooked_beef_loin_slice"]:
        if f"{MODID}:{i}" not in tag["values"]:
            tag["values"].append(f"{MODID}:{i}")
    write(f, tag)


if __name__ == "__main__":
    data_files()
